/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *     contributor license agreements.  See the NOTICE file distributed with
 *     this work for additional information regarding copyright ownership.
 *     The ASF licenses this file to You under the Apache License, Version 2.0
 *     (the "License"); you may not use this file except in compliance with
 *     the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package org.apache.openejb.server.rest;

import org.apache.openejb.BeanContext;
import org.apache.openejb.Injection;
import org.apache.openejb.assembler.classic.AppInfo;
import org.apache.openejb.assembler.classic.Assembler;
import org.apache.openejb.assembler.classic.DeploymentListener;
import org.apache.openejb.assembler.classic.EjbJarInfo;
import org.apache.openejb.assembler.classic.EnterpriseBeanInfo;
import org.apache.openejb.assembler.classic.WebAppInfo;
import org.apache.openejb.core.CoreContainerSystem;
import org.apache.openejb.core.WebContext;
import org.apache.openejb.loader.SystemInstance;
import org.apache.openejb.server.SelfManaging;
import org.apache.openejb.server.ServerService;
import org.apache.openejb.server.ServiceException;
import org.apache.openejb.server.httpd.HttpListener;
import org.apache.openejb.server.httpd.HttpListenerRegistry;
import org.apache.openejb.spi.ContainerSystem;
import org.apache.openejb.util.LogCategory;
import org.apache.openejb.util.Logger;

import javax.naming.Context;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.webbeans.config.WebBeansContext;

public abstract class RESTService implements ServerService, SelfManaging, DeploymentListener {
    public static final Logger LOGGER = Logger.getInstance(LogCategory.OPENEJB_RS, RESTService.class);
    private static final boolean OLD_WEBSERVICE_DEPLOYMENT = SystemInstance.get().getOptions().get("openejb.webservice.old-deployment", false);

    private static final String IP = "n/a";
    private static final int PORT = -1;
    public static final String NOPATH_PREFIX = "http://nopath/";

    private final Set<AppInfo> deployedApplications = new HashSet<AppInfo>();
    private final Set<WebAppInfo> deployedWebApps = new HashSet<WebAppInfo>();
    private Assembler assembler;
    private CoreContainerSystem containerSystem;
    private RsRegistry rsRegistry;
    private List<String> services = new ArrayList<String>();
    private String virtualHost;

    public void afterApplicationCreated(final AppInfo appInfo, final WebAppInfo webApp) {
        final Map<String, EJBRestServiceInfo> restEjbs = getRestEjbs(appInfo);

        final WebContext webContext = containerSystem.getWebContext(webApp.moduleId);
        if (webContext == null) {
            return;
        }

        if (!deployedWebApps.add(webApp)) {
            return;
        }

        final ClassLoader classLoader = getClassLoader(webContext.getClassLoader());
        final Collection<Injection> injections = webContext.getInjections();
        final WebBeansContext owbCtx = webContext.getAppContext().getWebBeansContext();
        Context context = webContext.getJndiEnc();
        if (context == null) { // usually true since it is set in org.apache.tomee.catalina.TomcatWebAppBuilder.afterStart() and lookup(comp) fails
            context = webContext.getAppContext().getAppJndiContext();
        }

        // The spec says:
        //
        // "The resources and providers that make up a JAX-RS application are configured via an application-supplied
        // subclass of Application. An implementation MAY provide alternate mechanisms for locating resource
        // classes and providers (e.g. runtime class scanning) but use of Application is the only portable means of
        //  configuration."
        //
        //  The choice here is to deploy using the Application if it exists or to use the scanned classes
        //  if there is no Application.
        //
        //  Like this providing an Application subclass user can totally control deployed services.

        boolean useApp = false;
        String appPrefix = webApp.contextRoot;
        for (String app : webApp.restApplications) { // normally a unique one but we support more
            appPrefix = webApp.contextRoot; // if multiple application classes reinit it
            if (!appPrefix.endsWith("/")) {
                appPrefix += "/";
            }

            Application appInstance;
            Class<?> appClazz;
            try {
                appClazz = classLoader.loadClass(app);
                appInstance = Application.class.cast(appClazz.newInstance());
            } catch (Exception e) {
                throw new OpenEJBRestRuntimeException("can't create class " + app, e);
            }

            ApplicationPath path = appClazz.getAnnotation(ApplicationPath.class);
            if (path != null) {
                String appPath = path.value();
                if (appPath.startsWith("/")) {
                    appPrefix += appPath.substring(1);
                } else {
                    appPrefix += appPath;
                }
            }

            Set<Object> singletons = appInstance.getSingletons();
            for (Object o : singletons) {
                if (o == null) {
                    continue;
                }

                if (restEjbs.containsKey(o.getClass().getName())) {
                    // no more a singleton if the ejb i not a singleton...but it is a weird case
                    deployEJB(appPrefix, restEjbs.get(o.getClass().getName()).context);
                } else {
                    deploySingleton(appPrefix, o, appInstance, classLoader);
                }
            }
            Set<Class<?>> classes = appInstance.getClasses();
            for (Class<?> clazz : classes) {
                if (restEjbs.containsKey(clazz.getName())) {
                    deployEJB(appPrefix, restEjbs.get(clazz.getName()).context);
                } else {
                    deployPojo(appPrefix, clazz, appInstance, classLoader, injections, context, owbCtx);
                }
            }

            useApp = useApp || classes.size() + singletons.size() > 0;
            LOGGER.info("REST application deployed: " + app);
        }

        if (!useApp) {
            final Set<String> restClasses = new HashSet<String>(webApp.restClass);
            restClasses.addAll(webApp.ejbRestServices);

            for (String clazz : restClasses) {
                if (restEjbs.containsKey(clazz)) {
                    deployEJB(appPrefix, restEjbs.get(clazz).context);
                } else {
                    try {
                        Class<?> loadedClazz = classLoader.loadClass(clazz);
                        deployPojo(appPrefix, loadedClazz, null, classLoader, injections, context, owbCtx);
                    } catch (ClassNotFoundException e) {
                        throw new OpenEJBRestRuntimeException("can't find class " + clazz, e);
                    }
                }
            }
        }

        restEjbs.clear();
    }

    @Override public void afterApplicationCreated(final AppInfo appInfo) {
        if (deployedApplications.add(appInfo)) {
            if (appInfo.webApps.size() == 0) {
                Map<String, EJBRestServiceInfo> restEjbs = getRestEjbs(appInfo);
                for (Map.Entry<String, EJBRestServiceInfo> ejb : restEjbs.entrySet()) {
                    deployEJB(ejb.getValue().path, ejb.getValue().context);
                }
                restEjbs.clear();
            } else {
                for (final WebAppInfo webApp : appInfo.webApps) {
                    afterApplicationCreated(appInfo, webApp);
                }
            }
        }
    }

    protected Map<String,EJBRestServiceInfo> getRestEjbs(AppInfo appInfo) {
        Map<String, BeanContext> beanContexts = new HashMap<String, BeanContext>();
        for (EjbJarInfo ejbJar : appInfo.ejbJars) {
            for (EnterpriseBeanInfo bean : ejbJar.enterpriseBeans) {
                if (bean.restService) {
                    BeanContext beanContext = containerSystem.getBeanContext(bean.ejbDeploymentId);
                    if (beanContext == null) {
                        continue;
                    }

                    beanContexts.put(bean.ejbClass, beanContext);
                }
            }
        }

        Map<String, EJBRestServiceInfo> restEjbs = new HashMap<String, EJBRestServiceInfo>();
        for (WebAppInfo webApp : appInfo.webApps) {
            for (String ejb : webApp.ejbRestServices) {
                restEjbs.put(ejb, new EJBRestServiceInfo(webApp.contextRoot, beanContexts.get(ejb)));
            }
        }
        for (Map.Entry<String, BeanContext> ejbs : beanContexts.entrySet()) {
            final String clazz = ejbs.getKey();
            if (!restEjbs.containsKey(clazz)) {
                // null is important, it means there is no webroot path in standalone
                String context = null;
                if (!OLD_WEBSERVICE_DEPLOYMENT) {
                    if (appInfo.appId != null && !appInfo.appId.isEmpty()) {
                        context = appInfo.appId;
                    } else {
                        context = ejbs.getValue().getModuleName();
                    }
                }
                restEjbs.put(clazz, new EJBRestServiceInfo(context, beanContexts.get(clazz)));
            }
        }
        beanContexts.clear();

        return restEjbs;
    }

    private void deploySingleton(String contextRoot, Object o, Application appInstance, ClassLoader classLoader) {
        final String nopath = getAddress(contextRoot, o.getClass()) + "/.*";
        final RsHttpListener listener = createHttpListener();
        final RsRegistry.AddressInfo address = rsRegistry.createRsHttpListener(contextRoot, listener, classLoader, nopath.substring(NOPATH_PREFIX.length() - 1), virtualHost);

        services.add(address.complete);
        listener.deploySingleton(getFullContext(address.base, contextRoot), o, appInstance);

        LOGGER.info("deployed REST singleton: " + o);
    }

    private static String baseAddress(final String address, final String contextRoot) {
        if (contextRoot == null || contextRoot.isEmpty()) {
            return address;
        }
        int idx = address.indexOf(contextRoot);
        return address.substring(0, idx) + contextRoot;
    }

    private void deployPojo(String contextRoot, Class<?> loadedClazz, Application app, ClassLoader classLoader, Collection<Injection> injections, Context context, WebBeansContext owbCtx) {
        if (loadedClazz.isInterface()) {
            return;
        }

        final String nopath = getAddress(contextRoot, loadedClazz) + "/.*";
        final RsHttpListener listener = createHttpListener();
        final RsRegistry.AddressInfo address = rsRegistry.createRsHttpListener(contextRoot, listener, classLoader, nopath.substring(NOPATH_PREFIX.length() - 1), virtualHost);

        services.add(address.complete);
        listener.deployPojo(getFullContext(address.base, contextRoot), loadedClazz, app, injections, context, owbCtx);

        LOGGER.info("REST Service: " + address.complete + "  -> Pojo " + loadedClazz.getName());
    }

    private void deployEJB(String context, BeanContext beanContext) {
        final String nopath = getAddress(context, beanContext.getBeanClass()) + "/.*";
        final RsHttpListener listener = createHttpListener();
        final RsRegistry.AddressInfo address = rsRegistry.createRsHttpListener(context, listener, beanContext.getClassLoader(), nopath.substring(NOPATH_PREFIX.length() - 1), virtualHost);

        services.add(address.complete);
        listener.deployEJB(getFullContext(address.base, context), beanContext);

        LOGGER.info("REST Service: " + address.complete + "  -> EJB " + beanContext.getEjbName());
    }

    /**
     * It creates the service container (http listener).
     *
     * @return the service container
     */
    protected abstract RsHttpListener createHttpListener();

    private static String getFullContext(String address, String context) {
        if (context == null) {
            return address;
        }
        if (context.isEmpty() && address.contains("/")) {
            return address.substring(0, address.lastIndexOf("/"));
        }

        String webCtx = context; // context can get the app path too
        if (webCtx.contains("/")) {
            webCtx = webCtx.substring(0, webCtx.indexOf("/"));
        }
        int idx = address.indexOf(webCtx);
        String base = address.substring(0, idx);
        if (!base.endsWith("/") && !webCtx.startsWith("/")) {
            base = base + '/';
        }
        return base + context;
    }

    private String getAddress(String context, Class<?> clazz) {
        String root = NOPATH_PREFIX;
        if (context != null) {
            root += context;
        }

        Class<?> usedClass = clazz;
        while (usedClass.getAnnotation(Path.class) == null && usedClass.getSuperclass() != null) {
            usedClass = usedClass.getSuperclass();
        }
        if (usedClass == null || usedClass.getAnnotation(Path.class) == null) {
            throw new IllegalArgumentException("no @Path annotation on " + clazz.getName());
        }

        try {
            return UriBuilder.fromUri(new URI(root)).path(usedClass).build().toURL().toString();
        } catch (MalformedURLException e) {
            throw new OpenEJBRestRuntimeException("url is malformed", e);
        } catch (URISyntaxException e) {
            throw new OpenEJBRestRuntimeException("uri syntax is not correct", e);
        }
    }

    private void undeployRestObject(String context) {
        HttpListener listener = rsRegistry.removeListener(context);
        if (listener != null) {
            RsHttpListener.class.cast(listener).undeploy();
        }
    }

    private static ClassLoader getClassLoader(ClassLoader classLoader) {
        ClassLoader cl = classLoader;
        if (cl == null) {
            cl = Thread.currentThread().getContextClassLoader();
        }
        if (cl == null) {
            cl = RESTService.class.getClassLoader();
        }
        return cl;
    }

    @Override public void beforeApplicationDestroyed(AppInfo appInfo) {
        if (deployedApplications.contains(appInfo)) {
            for (WebAppInfo webApp : appInfo.webApps) {
                for (String address : services) {
                    if (address.endsWith(webApp.contextRoot)) {
                        undeployRestObject(address);
                    }
                }
                deployedWebApps.remove(webApp);
            }
        }
    }

    @Override public void start() throws ServiceException {
        SystemInstance.get().setComponent(RESTService.class, this);

        beforeStart();

        containerSystem = (CoreContainerSystem) SystemInstance.get().getComponent(ContainerSystem.class);
        assembler = SystemInstance.get().getComponent(Assembler.class);
        if (assembler != null) {
            assembler.addDeploymentListener(this);
            for (AppInfo appInfo : assembler.getDeployedApplications()) {
                afterApplicationCreated(appInfo);
            }
        }
    }

    protected void beforeStart() {
        rsRegistry = SystemInstance.get().getComponent(RsRegistry.class);
        if (rsRegistry == null && SystemInstance.get().getComponent(HttpListenerRegistry.class) != null) {
            rsRegistry = new RsRegistryImpl();
        }
    }

    @Override public void stop() throws ServiceException {
        for (String address : services) {
            undeployRestObject(address);
        }

        if (assembler != null) {
            assembler.removeDeploymentListener(this);
            for (AppInfo appInfo : new ArrayList<AppInfo>(deployedApplications)) {
                beforeApplicationDestroyed(appInfo);
            }
        }
    }

    @Override public void service(InputStream in, OutputStream out) throws ServiceException, IOException {
        throw new UnsupportedOperationException(getClass().getName() + " cannot be invoked directly");
    }

    @Override public void service(Socket socket) throws ServiceException, IOException {
        throw new UnsupportedOperationException(getClass().getName() + " cannot be invoked directly");
    }

    @Override public String getIP() {
        return IP;
    }

    @Override public int getPort() {
        return PORT;
    }

    @Override public void init(Properties props) throws Exception {
        virtualHost = props.getProperty("virtualHost");
    }

    public String getVirtualHost() {
        return virtualHost;
    }

    public void setVirtualHost(String virtualHost) {
        this.virtualHost = virtualHost;
    }

    public static class EJBRestServiceInfo {
        public String path;
        public BeanContext context;

        public EJBRestServiceInfo(String path, BeanContext context) {
            if (context == null) {
                throw new OpenEJBRestRuntimeException("can't find context");
            }

            this.path = path;
            this.context = context;
        }
    }
}
