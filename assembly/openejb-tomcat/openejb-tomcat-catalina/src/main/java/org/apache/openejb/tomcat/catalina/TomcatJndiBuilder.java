/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.openejb.tomcat.catalina;

import org.apache.catalina.core.NamingContextListener;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.deploy.ContextEjb;
import org.apache.catalina.deploy.ContextEnvironment;
import org.apache.catalina.deploy.ContextResource;
import org.apache.catalina.deploy.ContextResourceEnvRef;
import org.apache.catalina.deploy.ContextTransaction;
import org.apache.catalina.deploy.NamingResources;
import org.apache.naming.ContextAccessController;
import org.apache.naming.factory.Constants;
import org.apache.openejb.Injection;
import org.apache.openejb.OpenEJBException;
import org.apache.openejb.assembler.classic.EjbLocalReferenceInfo;
import org.apache.openejb.assembler.classic.EjbReferenceInfo;
import org.apache.openejb.assembler.classic.EnvEntryInfo;
import org.apache.openejb.assembler.classic.PersistenceContextReferenceInfo;
import org.apache.openejb.assembler.classic.PersistenceUnitReferenceInfo;
import org.apache.openejb.assembler.classic.PortRefInfo;
import org.apache.openejb.assembler.classic.ResourceEnvReferenceInfo;
import org.apache.openejb.assembler.classic.ResourceReferenceInfo;
import org.apache.openejb.assembler.classic.ServiceReferenceInfo;
import org.apache.openejb.assembler.classic.WebAppInfo;
import org.apache.openejb.assembler.classic.WsBuilder;
import org.apache.openejb.core.webservices.HandlerChainData;
import org.apache.openejb.core.webservices.PortRefData;
import org.apache.openejb.loader.SystemInstance;
import org.apache.openejb.persistence.JtaEntityManager;
import org.apache.openejb.persistence.JtaEntityManagerRegistry;
import org.apache.openejb.spi.ContainerSystem;
import org.apache.openejb.tomcat.common.EjbFactory;
import org.apache.openejb.tomcat.common.NamingUtil;
import static org.apache.openejb.tomcat.common.NamingUtil.DEPLOYMENT_ID;
import static org.apache.openejb.tomcat.common.NamingUtil.EXTENDED;
import static org.apache.openejb.tomcat.common.NamingUtil.EXTERNAL;
import static org.apache.openejb.tomcat.common.NamingUtil.JNDI_NAME;
import static org.apache.openejb.tomcat.common.NamingUtil.JNDI_PROVIDER_ID;
import static org.apache.openejb.tomcat.common.NamingUtil.LOCAL;
import static org.apache.openejb.tomcat.common.NamingUtil.NAME;
import static org.apache.openejb.tomcat.common.NamingUtil.RESOURCE_ID;
import static org.apache.openejb.tomcat.common.NamingUtil.UNIT;
import static org.apache.openejb.tomcat.common.NamingUtil.WSDL_URL;
import static org.apache.openejb.tomcat.common.NamingUtil.WS_CLASS;
import static org.apache.openejb.tomcat.common.NamingUtil.WS_ID;
import static org.apache.openejb.tomcat.common.NamingUtil.WS_PORT_QNAME;
import static org.apache.openejb.tomcat.common.NamingUtil.WS_QNAME;
import static org.apache.openejb.tomcat.common.NamingUtil.setStaticValue;
import org.apache.openejb.tomcat.common.PersistenceContextFactory;
import org.apache.openejb.tomcat.common.PersistenceUnitFactory;
import org.apache.openejb.tomcat.common.ResourceFactory;
import org.apache.openejb.tomcat.common.UserTransactionFactory;
import org.apache.openejb.tomcat.common.WsFactory;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.transaction.UserTransaction;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class TomcatJndiBuilder {
    private final StandardContext standardContext;
    private final WebAppInfo webAppInfo;
    private final List<Injection> injections;
    private final boolean replaceEntry;
    private boolean useCrossClassLoaderRef = true;
    private NamingContextListener namingContextListener;

    public TomcatJndiBuilder(StandardContext standardContext, WebAppInfo webAppInfo, List<Injection> injections) {
        this.injections = injections;
        this.standardContext = standardContext;
        this.namingContextListener = BackportUtil.getNamingContextListener(standardContext);
        this.webAppInfo = webAppInfo;

        String parameter = standardContext.findParameter("openejb.start.late");
        replaceEntry = Boolean.parseBoolean(parameter);
    }

    public boolean isUseCrossClassLoaderRef() {
        return useCrossClassLoaderRef;
    }

    public void setUseCrossClassLoaderRef(boolean useCrossClassLoaderRef) {
        this.useCrossClassLoaderRef = useCrossClassLoaderRef;
    }

    public void mergeJndi() throws OpenEJBException {

        NamingResources naming = standardContext.getNamingResources();

        URI moduleUri;
        try {
            moduleUri = new URI(webAppInfo.moduleId);
        } catch (URISyntaxException e) {
            throw new OpenEJBException(e);
        }

        for (EnvEntryInfo ref : webAppInfo.jndiEnc.envEntries) {
            mergeRef(naming, ref);
        }
        for (EjbReferenceInfo ref : webAppInfo.jndiEnc.ejbReferences) {
            mergeRef(naming, ref);
        }
        for (EjbLocalReferenceInfo ref : webAppInfo.jndiEnc.ejbLocalReferences) {
            mergeRef(naming, ref);
        }
        for (PersistenceContextReferenceInfo ref : webAppInfo.jndiEnc.persistenceContextRefs) {
            mergeRef(naming, ref, moduleUri);
        }
        for (PersistenceUnitReferenceInfo ref : webAppInfo.jndiEnc.persistenceUnitRefs) {
            mergeRef(naming, ref, moduleUri);
        }
        for (ResourceReferenceInfo ref : webAppInfo.jndiEnc.resourceRefs) {
            mergeRef(naming, ref);
        }
        for (ResourceEnvReferenceInfo ref : webAppInfo.jndiEnc.resourceEnvRefs) {
            mergeRef(naming, ref);
        }
        for (ServiceReferenceInfo ref : webAppInfo.jndiEnc.serviceRefs) {
            mergeRef(naming, ref);
        }
        ContextTransaction contextTransaction = new ContextTransaction();
        contextTransaction.setProperty(Constants.FACTORY, UserTransactionFactory.class.getName());
        naming.setTransaction(contextTransaction);
    }

    public void mergeRef(NamingResources naming, EnvEntryInfo ref) {
        ContextEnvironment environment = naming.findEnvironment(ref.name);
        boolean addEntry = false;
        if (environment == null) {
            environment = new ContextEnvironment();
            environment.setName(ref.name);
            addEntry = true;
        }

        environment.setType(ref.type);
        environment.setValue(ref.value);

        if (addEntry) {
            naming.addEnvironment(environment);
        }

        if (replaceEntry) {
            ContextAccessController.setWritable(namingContextListener.getName(), standardContext);
            if (!addEntry) namingContextListener.removeEnvironment(environment.getName());
            namingContextListener.addEnvironment(environment);
            ContextAccessController.setReadOnly(namingContextListener.getName());
        }
    }

    public void mergeRef(NamingResources naming, EjbReferenceInfo ref) {
        ContextEjb ejb = naming.findEjb(ref.referenceName);
        boolean addEntry = false;
        if (ejb == null) {
            ejb = new ContextEjb();
            ejb.setName(ref.referenceName);
            addEntry = true;
        }

        ejb.setProperty(Constants.FACTORY, EjbFactory.class.getName());
        ejb.setProperty(NAME, ref.referenceName);
        ejb.setHome(ref.homeClassName);
        ejb.setRemote(ref.interfaceClassName);
        ejb.setLink(null);
        ejb.setType(ref.interfaceClassName);
        if (useCrossClassLoaderRef) {
            ejb.setProperty(EXTERNAL, "" + ref.externalReference);
        }

        if (ref.ejbDeploymentId != null) {
            ejb.setProperty(DEPLOYMENT_ID, ref.ejbDeploymentId);
        }

        if (ref.location != null) {
            ejb.setProperty(JNDI_NAME, ref.location.jndiName);
            ejb.setProperty(JNDI_PROVIDER_ID, ref.location.jndiProviderId);
        }

        if (addEntry) {
            naming.addEjb(ejb);
        }

        if (replaceEntry) {
            ContextAccessController.setWritable(namingContextListener.getName(), standardContext);
            if (!addEntry) namingContextListener.removeEjb(ejb.getName());
            namingContextListener.addEjb(ejb);
            ContextAccessController.setReadOnly(namingContextListener.getName());
        }
    }

    public void mergeRef(NamingResources naming, EjbLocalReferenceInfo ref) {
        // NamingContextListener.addLocalEjb is empty so we'll just use an ejb ref
        ContextEjb ejb = naming.findEjb(ref.referenceName);
        boolean addEntry = false;
        if (ejb == null) {
            ejb = new ContextEjb();
            ejb.setName(ref.referenceName);
            addEntry = true;
        }

        ejb.setProperty(Constants.FACTORY, EjbFactory.class.getName());
        ejb.setProperty(NAME, ref.referenceName);
        ejb.setHome(ref.homeClassName);
        ejb.setRemote(null);
        ejb.setProperty(LOCAL, ref.interfaceClassName);
        ejb.setLink(null);
        ejb.setType(ref.interfaceClassName);

        if (ref.ejbDeploymentId != null) {
            ejb.setProperty(DEPLOYMENT_ID, ref.ejbDeploymentId);
        }

        if (ref.location != null) {
            ejb.setProperty(JNDI_NAME, ref.location.jndiName);
            ejb.setProperty(JNDI_PROVIDER_ID, ref.location.jndiProviderId);
        }

        if (addEntry) {
            naming.addEjb(ejb);
        }

        if (replaceEntry) {
            ContextAccessController.setWritable(namingContextListener.getName(), standardContext);
            if (!addEntry) namingContextListener.removeEjb(ejb.getName());
            namingContextListener.addEjb(ejb);
            ContextAccessController.setReadOnly(namingContextListener.getName());
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void mergeRef(NamingResources naming, PersistenceContextReferenceInfo ref, URI moduleUri) {
        ContextResource resource = naming.findResource(ref.referenceName);
        boolean addEntry = false;
        if (resource == null) {
            resource = new ContextResource();
            resource.setName(ref.referenceName);
            addEntry = true;
        }

        resource.setProperty(Constants.FACTORY, PersistenceContextFactory.class.getName());
        resource.setProperty(NAME, ref.referenceName);
        resource.setType(EntityManager.class.getName());

        if (ref.persistenceUnitName != null) {
            resource.setProperty(UNIT, ref.persistenceUnitName);
        }
        resource.setProperty(EXTENDED, "" + ref.extended);
        if (ref.properties != null) {
            // resource.setProperty(NamingConstants.PROPERTIES, ref.properties);
        }

        if (ref.location != null) {
            resource.setProperty(JNDI_NAME, ref.location.jndiName);
            resource.setProperty(JNDI_PROVIDER_ID, ref.location.jndiProviderId);
        } else {
            Context context = SystemInstance.get().getComponent(ContainerSystem.class).getJNDIContext();
            EntityManagerFactory factory;
            try {
                factory = (EntityManagerFactory) context.lookup("openejb/PersistenceUnit/" + ref.unitId);
            } catch (NamingException e) {
                throw new IllegalStateException("PersistenceUnit '" + ref.unitId + "' not found for EXTENDED ref '" + ref.referenceName + "'");
            }

            JtaEntityManagerRegistry jtaEntityManagerRegistry = SystemInstance.get().getComponent(JtaEntityManagerRegistry.class);
            JtaEntityManager jtaEntityManager = new JtaEntityManager(ref.persistenceUnitName, jtaEntityManagerRegistry, factory, ref.properties, ref.extended);
            Object object = jtaEntityManager;
            setResource(resource, object);
        }

        if (addEntry) {
            naming.addResource(resource);
        }

        if (replaceEntry) {
            ContextAccessController.setWritable(namingContextListener.getName(), standardContext);
            if (!addEntry) namingContextListener.removeResource(resource.getName());
            namingContextListener.addResource(resource);
            ContextAccessController.setReadOnly(namingContextListener.getName());
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void mergeRef(NamingResources naming, PersistenceUnitReferenceInfo ref, URI moduleUri) {
        ContextResource resource = naming.findResource(ref.referenceName);
        boolean addEntry = false;
        if (resource == null) {
            resource = new ContextResource();
            resource.setName(ref.referenceName);
            addEntry = true;
        }

        resource.setProperty(Constants.FACTORY, PersistenceUnitFactory.class.getName());
        resource.setProperty(NAME, ref.referenceName);
        resource.setType(EntityManagerFactory.class.getName());

        if (ref.persistenceUnitName != null) {
            resource.setProperty(UNIT, ref.persistenceUnitName);
        }

        if (ref.location != null) {
            resource.setProperty(JNDI_NAME, ref.location.jndiName);
            resource.setProperty(JNDI_PROVIDER_ID, ref.location.jndiProviderId);
        } else {
            // TODO: This will not work if webapps don't use AutoConfi
            Context context = SystemInstance.get().getComponent(ContainerSystem.class).getJNDIContext();
            EntityManagerFactory factory;
            try {
                factory = (EntityManagerFactory) context.lookup("openejb/PersistenceUnit/" + ref.unitId);
            } catch (NamingException e) {
                throw new IllegalStateException("PersistenceUnit '" + ref.unitId + "' not found for EXTENDED ref '" + ref.referenceName + "'");
            }
            setResource(resource, factory);
        }

        if (addEntry) {
            naming.addResource(resource);
        }

        if (replaceEntry) {
            ContextAccessController.setWritable(namingContextListener.getName(), standardContext);
            if (!addEntry) namingContextListener.removeResource(resource.getName());
            namingContextListener.addResource(resource);
            ContextAccessController.setReadOnly(namingContextListener.getName());
        }
    }

    public void mergeRef(NamingResources naming, ResourceReferenceInfo ref) {
        ContextResource resource = naming.findResource(ref.referenceName);
        boolean addEntry = false;
        if (resource == null) {
            resource = new ContextResource();
            resource.setName(ref.referenceName);
            addEntry = true;
        }

        resource.setProperty(Constants.FACTORY, ResourceFactory.class.getName());
        resource.setProperty(NAME, ref.referenceName);
        resource.setType(ref.referenceType);
        resource.setAuth(ref.referenceAuth);

        if (ref.resourceID != null) {
            resource.setProperty(RESOURCE_ID, ref.resourceID);
        }

        if (ref.properties != null) {
            // resource.setProperty(NamingConstants.PROPERTIES, ref.properties);
        }

        if (ref.location != null) {
            resource.setProperty(JNDI_NAME, ref.location.jndiName);
            resource.setProperty(JNDI_PROVIDER_ID, ref.location.jndiProviderId);
        }

        if (addEntry) {
            naming.addResource(resource);
        }

        if (replaceEntry) {
            ContextAccessController.setWritable(namingContextListener.getName(), standardContext);
            if (!addEntry) namingContextListener.removeResource(resource.getName());
            namingContextListener.addResource(resource);
            ContextAccessController.setReadOnly(namingContextListener.getName());
        }
    }

    public void mergeRef(NamingResources naming, ResourceEnvReferenceInfo ref) {
        ContextResourceEnvRef resourceEnv = naming.findResourceEnvRef(ref.resourceEnvRefName);
        boolean addEntry = false;
        if (resourceEnv == null) {
            resourceEnv = new ContextResourceEnvRef();
            resourceEnv.setName(ref.resourceEnvRefName);
            addEntry = true;
        }

        if (UserTransaction.class.getName().equals(ref.resourceEnvRefType)) {
            resourceEnv.setProperty(Constants.FACTORY, UserTransactionFactory.class.getName());
            resourceEnv.setType(ref.resourceEnvRefType);
        } else {
            resourceEnv.setProperty(Constants.FACTORY, ResourceFactory.class.getName());
            resourceEnv.setProperty(NAME, ref.resourceEnvRefName);
            resourceEnv.setType(ref.resourceEnvRefType);

            if (ref.resourceID != null) {
                resourceEnv.setProperty(RESOURCE_ID, ref.resourceID);
            }

            if (ref.location != null) {
                resourceEnv.setProperty(JNDI_NAME, ref.location.jndiName);
                resourceEnv.setProperty(JNDI_PROVIDER_ID, ref.location.jndiProviderId);
            }
        }

        if (addEntry) {
            naming.addResourceEnvRef(resourceEnv);
        }

        if (replaceEntry) {
            ContextAccessController.setWritable(namingContextListener.getName(), standardContext);
            if (!addEntry) namingContextListener.removeResourceEnvRef(resourceEnv.getName());
            namingContextListener.addResourceEnvRef(resourceEnv);
            ContextAccessController.setReadOnly(namingContextListener.getName());
        }
    }

    public void mergeRef(NamingResources naming, ServiceReferenceInfo ref) {
        ContextResource resource = naming.findResource(ref.referenceName);
        boolean addEntry = false;
        if (resource == null) {
            resource = new ContextResource();
            resource.setName(ref.referenceName);
            addEntry = true;
        }

        resource.setProperty(Constants.FACTORY, WsFactory.class.getName());
        resource.setProperty(NAME, ref.referenceName);
        if (ref.referenceType != null) {
            resource.setType(ref.referenceType);
        } else {
            resource.setType(ref.serviceType);
        }

        if (ref.location != null) {
            resource.setProperty(JNDI_NAME, ref.location.jndiName);
            resource.setProperty(JNDI_PROVIDER_ID, ref.location.jndiProviderId);
        } else {
            // ID
            if (ref.id != null) {
                resource.setProperty(WS_ID, ref.id);
            }
            // Service QName
            if (ref.serviceQName != null) {
                resource.setProperty(WS_QNAME, ref.serviceQName.toString());
            }
            // Service Class
            resource.setProperty(WS_CLASS, ref.serviceType);

            // Port QName
            if (ref.portQName != null) {
                resource.setProperty(WS_PORT_QNAME, ref.portQName.toString());
            }

            // add the wsdl url
            URL wsdlURL = getWsdlUrl(ref);
            if (wsdlURL != null) {
                resource.setProperty(WSDL_URL, wsdlURL.toString());
            }

            // add port refs
            if (!ref.portRefs.isEmpty()) {
                List<PortRefData> portRefs = new ArrayList<PortRefData>(ref.portRefs.size());
                for (PortRefInfo portRefInfo : ref.portRefs) {
                    PortRefData portRef = new PortRefData();
                    portRef.setQName(portRefInfo.qname);
                    portRef.setServiceEndpointInterface(portRefInfo.serviceEndpointInterface);
                    portRef.setEnableMtom(portRefInfo.enableMtom);
                    portRef.getProperties().putAll(portRefInfo.properties);
                    portRefs.add(portRef);
                }
                setResource(resource, "port-refs", portRefs);
            }

            // add the handle chains
            if (!ref.handlerChains.isEmpty()) {
                try {
                    List<HandlerChainData> handlerChains = WsBuilder.toHandlerChainData(ref.handlerChains, standardContext.getLoader().getClassLoader());
                    setResource(resource, "handler-chains", handlerChains);
                    setResource(resource, "injections", injections);
                } catch (OpenEJBException e) {
                    throw new IllegalArgumentException("Error creating handler chain for web service-ref " + ref.referenceName);
                }
            }
        }

        // if there was a service entry, remove it
        String serviceName = BackportUtil.findServiceName(naming, ref.referenceName);
        if (serviceName != null) {
            ContextAccessController.setWritable(namingContextListener.getName(), standardContext);
            if (!addEntry) BackportUtil.removeService(namingContextListener, serviceName);
            ContextAccessController.setReadOnly(namingContextListener.getName());
        }

        // add the new resource entry
        if (addEntry) {
            naming.addResource(resource);
        }

        // or replace the exisitng resource entry
        if (replaceEntry) {
            ContextAccessController.setWritable(namingContextListener.getName(), standardContext);
            if (!addEntry) namingContextListener.removeResource(resource.getName());
            namingContextListener.addResource(resource);
            ContextAccessController.setReadOnly(namingContextListener.getName());
        }
    }

    private URL getWsdlUrl(ServiceReferenceInfo ref) {
        if (ref.wsdlFile == null) return null;

        URL wsdlUrl = null;
        try {
            wsdlUrl = new URL(ref.wsdlFile);
        } catch (MalformedURLException e) {
        }

        if (wsdlUrl == null) {
            wsdlUrl = standardContext.getLoader().getClassLoader().getResource(ref.wsdlFile);
        }

        if (wsdlUrl == null) {
            try {
                wsdlUrl = standardContext.getServletContext().getResource("/" + ref.wsdlFile);
            } catch (MalformedURLException e) {
            }
        }

        if (wsdlUrl == null) {
            throw new IllegalArgumentException("WSDL file " + ref.wsdlFile + " for web service-ref " + ref.referenceName + " not found");
        }

        return wsdlUrl;
    }

    private void setResource(ContextResource resource, String name, Object object) {
        setStaticValue(new Resource(resource), name, object);
    }

    private void setResource(ContextResource resource, Object object) {
        setStaticValue(new Resource(resource), object);
    }

    private static class Resource implements NamingUtil.Resource {
        private final ContextResource contextResource;

        public Resource(ContextResource contextResource) {
            this.contextResource = contextResource;
        }

        public void setProperty(String name, Object value) {
            contextResource.setProperty(name, value);
        }
    }
}
