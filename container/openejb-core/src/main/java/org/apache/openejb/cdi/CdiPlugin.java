/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openejb.cdi;

import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import org.apache.openejb.DeploymentInfo;
import org.apache.openejb.OpenEJBException;
import org.apache.openejb.assembler.classic.ProxyInterfaceResolver;
import org.apache.openejb.core.AppContext;
import org.apache.openejb.core.CoreUserTransaction;
import org.apache.openejb.loader.SystemInstance;
import org.apache.openejb.spi.ContainerSystem;
import org.apache.openejb.util.LogCategory;
import org.apache.openejb.util.Logger;
import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.container.InjectionResolver;
import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.ee.event.TransactionalEventNotifier;
import org.apache.webbeans.ejb.common.component.BaseEjbBean;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.jms.JMSManager;
import org.apache.webbeans.plugins.PluginLoader;
import org.apache.webbeans.portable.AnnotatedElementFactory;
import org.apache.webbeans.portable.events.ExtensionLoader;
import org.apache.webbeans.portable.events.discovery.BeforeShutdownImpl;
import org.apache.webbeans.proxy.JavassistProxyFactory;
import org.apache.webbeans.spi.SecurityService;
import org.apache.webbeans.spi.TransactionService;
import org.apache.webbeans.spi.plugins.AbstractOwbPlugin;
import org.apache.webbeans.spi.plugins.OpenWebBeansEjbPlugin;
import org.apache.webbeans.spi.plugins.OpenWebBeansJavaEEPlugin;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.WebBeansUtil;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.naming.NamingException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import java.lang.annotation.Annotation;
import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

public class CdiPlugin extends AbstractOwbPlugin implements OpenWebBeansJavaEEPlugin, OpenWebBeansEjbPlugin, TransactionService, SecurityService {

    private static final Logger logger = Logger.getInstance(LogCategory.OPENEJB.createChild("cdi"), CdiPlugin.class);
    private AppContext appContext;
    private Set<Class<?>> beans;

    private CdiAppContextsService contexsServices;

    @Override
    public void shutDown() throws Exception {
        super.shutDown();
        this.beans.clear();
    }

    public void setAppContext(AppContext appContext) {
        this.appContext = appContext;
    }

    public void configureDeployments(List<DeploymentInfo> ejbDeployments) {
        WeakHashMap<Class<?>, Object> beans = new WeakHashMap<Class<?>, Object>();
        for (DeploymentInfo deployment : ejbDeployments) {
            if (deployment.getComponentType().isSession()) {
                beans.put(deployment.getBeanClass(), null);
            }
        }
        this.beans = beans.keySet();
    }

    public CdiAppContextsService getContexsServices() {
        return contexsServices;
    }

    public void startup() {
        this.contexsServices = (CdiAppContextsService) WebBeansFinder.getSingletonInstance("org.apache.openejb.cdi.CdiAppContextsService", appContext.getClassLoader());
        this.contexsServices.init(null);
    }

    public void stop() throws OpenEJBException {
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        try {
            //Setting context class loader for cleaning
            Thread.currentThread().setContextClassLoader(appContext.getClassLoader());

            //Fire shut down
            appContext.getBeanManager().fireEvent(new BeforeShutdownImpl(), new Annotation[0]);

            //Destroys context
            this.contexsServices.destroy(null);

            //Free all plugin resources
            PluginLoader.getInstance().shutDown();

            //Clear extensions
            ExtensionLoader.getInstance().clear();

            //Delete Resolutions Cache
            InjectionResolver.getInstance().clearCaches();

            //Delte proxies
            JavassistProxyFactory.getInstance().clear();

            //Delete AnnotateTypeCache
            AnnotatedElementFactory.getInstance().clear();

            //JMs Manager clear
            JMSManager.getInstance().clear();

            //Clear the resource injection service
            CdiResourceInjectionService injectionServices = (CdiResourceInjectionService) WebBeansFinder.getSingletonInstance("org.apache.openejb.cdi.CdiResourceInjectionService", appContext.getClassLoader());
            injectionServices.clear();

            //ContextFactory cleanup
            ContextFactory.cleanUpContextFactory();

            //Clear singleton list
            WebBeansFinder.clearInstances(WebBeansUtil.getCurrentClassLoader());


        } catch (Exception e) {
            throw new OpenEJBException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
        }
    }

    @Override
    public <T> T getSupportedService(Class<T> serviceClass) {
        return supportService(serviceClass) ? serviceClass.cast(this) : null;
    }

    @Override
    public void isManagedBean(Class<?> clazz) throws Exception {
    }

    @Override
    public boolean supportService(Class<?> serviceClass) {
        return serviceClass == TransactionService.class || serviceClass == SecurityService.class;
    }

    @Override
    public Object getSessionBeanProxy(Bean<?> bean, Class<?> interfce, CreationalContext<?> creationalContext) {
        final CdiEjbBean ejbBean = (CdiEjbBean) bean;
        final DeploymentInfo deployment = ejbBean.getDeploymentInfo();

        final Class beanClass = deployment.getBeanClass();
        final List<Class> localInterfaces = deployment.getBusinessLocalInterfaces();

        List<Class> interfaces = ProxyInterfaceResolver.getInterfaces(beanClass, interfce, localInterfaces);
        DeploymentInfo.BusinessLocalHome home = deployment.getBusinessLocalHome(interfaces);
        return home.create();

//        try {
//
//            ((CdiEjbBean<Object>) bean).setIface(iface);
//
//            Class<?> clazz = JavassistProxyFactory.getInstance().getEjbBeanProxyClass((BaseEjbBean<Object>) (CdiEjbBean<Object>) bean);
//
//            if (clazz == null) {
//                ProxyFactory factory = new ProxyFactory();
//                factory.setInterfaces(new Class[]{(Class<?>) iface});
//                clazz = JavassistProxyFactory.getInstance().defineEjbBeanProxyClass((BaseEjbBean<Object>) (CdiEjbBean<Object>) bean, factory);
//            }
//
//            Object proxyInstance = (Object) ClassUtil.newInstance(clazz);
//
//            EjbBeanProxyHandler handler = new EjbBeanProxyHandler((CdiEjbBean<Object>) bean, creationalContext);
//
//            ((ProxyObject) proxyInstance).setHandler(handler);
//
//            return proxyInstance;
//
//        } catch (Exception e) {
//            throw new WebBeansException(e);
//        }
    }

    @Override
    public boolean isSessionBean(Class<?> clazz) {
        return beans.contains(clazz);
    }

    @Override
    public <T> Bean<T> defineSessionBean(Class<T> clazz, ProcessAnnotatedType<T> processAnnotateTypeEvent) {
        throw new IllegalStateException("Statement should never be reached");
    }

    @Override
    public boolean isSingletonBean(Class<?> clazz) {
        throw new IllegalStateException("Statement should never be reached");
    }

    @Override
    public boolean isStatefulBean(Class<?> clazz) {
        throw new IllegalStateException("Statement should never be reached");
    }

    @Override
    public boolean isStatelessBean(Class<?> clazz) {
        throw new IllegalStateException("Statement should never be reached");
    }

    @Override
    public Transaction getTransaction() {
        TransactionManager manager = getTransactionManager();
        if (manager != null) {
            try {
                return manager.getTransaction();
            } catch (SystemException e) {
                logger.error("Error is occured while getting transaction instance from system", e);
            }
        }

        return null;
    }

    @Override
    public TransactionManager getTransactionManager() {
        // TODO Convert to final field
        return SystemInstance.get().getComponent(TransactionManager.class);
    }

    @Override
    public UserTransaction getUserTransaction() {
        UserTransaction ut = null;

        // TODO Convert to final field
        ContainerSystem containerSystem = SystemInstance.get().getComponent(ContainerSystem.class);
        
        try {
            ut = (UserTransaction) containerSystem.getJNDIContext().lookup("comp/UserTransaction");
        } catch (NamingException e) {
            logger.debug("User transaction is not bound to context, lets create it");
            ut = new CoreUserTransaction(getTransactionManager());

        }
        return ut;
    }

    @Override
    public void registerTransactionSynchronization(TransactionPhase phase, ObserverMethod<? super Object> observer, Object event) throws Exception {
        TransactionalEventNotifier.registerTransactionSynchronization(phase, observer, event);
    }

    @Override
    public Principal getCurrentPrincipal() {

        // TODO Convert to final field
        org.apache.openejb.spi.SecurityService<?> service = SystemInstance.get().getComponent(org.apache.openejb.spi.SecurityService.class);
        if (service != null) {
            return service.getCallerPrincipal();
        }

        return null;
    }
}
