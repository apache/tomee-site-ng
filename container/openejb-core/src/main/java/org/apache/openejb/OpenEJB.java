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
package org.apache.openejb;

import java.util.Date;
import java.util.Properties;

import javax.transaction.TransactionManager;

import org.apache.openejb.loader.SystemInstance;
import org.apache.openejb.spi.ApplicationServer;
import org.apache.openejb.spi.Assembler;
import org.apache.openejb.spi.ContainerSystem;
import org.apache.openejb.spi.SecurityService;
import org.apache.openejb.util.LogCategory;
import org.apache.openejb.util.Logger;
import org.apache.openejb.util.Messages;
import org.apache.openejb.util.OpenEjbVersion;
import org.apache.openejb.util.SafeToolkit;

/**
 * @version $Rev$ $Date$
 */
public final class OpenEJB {

    private static Instance instance;

    public static ApplicationServer getApplicationServer() {
        return SystemInstance.get().getComponent(ApplicationServer.class);
    }

    public static TransactionManager getTransactionManager(){
        return SystemInstance.get().getComponent(TransactionManager.class);
    }
    
    public static class Instance {
        private static Messages messages = new Messages("org.apache.openejb.util.resources");
        private final Throwable initialized;

        /**
         * 1 usage
         * org.apache.openejb.core.ivm.naming.InitContextFactory
         */
        public Instance(Properties props) throws OpenEJBException {
            this(props, null);
        }

        /**
         * 2 usages
         */
        public Instance(Properties initProps, ApplicationServer appServer) throws OpenEJBException {
            initialized = new Exception("Initialized at "+new Date()).fillInStackTrace();

            Logger logger = Logger.getInstance(LogCategory.OPENEJB_STARTUP, "org.apache.openejb.util.resources");

            try {
                SystemInstance.init(initProps);
            } catch (Exception e) {
                throw new OpenEJBException(e);
            }
            SystemInstance system = SystemInstance.get();

            SafeToolkit toolkit = SafeToolkit.getToolkit("OpenEJB");

            if (appServer == null) {
                ApplicationServer defaultServer = (ApplicationServer) toolkit.newInstance("org.apache.openejb.core.ServerFederation");
                appServer = defaultServer;
            }
            system.setComponent(ApplicationServer.class, appServer);

            /*
            * Output startup message
            */
            OpenEjbVersion versionInfo = OpenEjbVersion.get();

            if (initProps.getProperty("openejb.nobanner") == null) {
                System.out.println("Apache OpenEJB " + versionInfo.getVersion() + "    build: " + versionInfo.getDate() + "-" + versionInfo.getTime());
                System.out.println("" + versionInfo.getUrl());
            }

            Logger logger2 = Logger.getInstance(LogCategory.OPENEJB, "org.apache.openejb.util.resources");
            logger2.info("startup.banner", versionInfo.getUrl(), new Date(), versionInfo.getCopyright(),
                    versionInfo.getVersion(), versionInfo.getDate(), versionInfo.getTime());

            logger.info("openejb.home = " + SystemInstance.get().getHome().getDirectory().getAbsolutePath());
            logger.info("openejb.base = " + SystemInstance.get().getBase().getDirectory().getAbsolutePath());

            Properties props = new Properties(SystemInstance.get().getProperties());

            if (initProps == null) {
                logger.debug("startup.noInitializationProperties");
            } else {
                props.putAll(initProps);
            }



            /* Uses the EnvProps.ASSEMBLER property to obtain the Assembler impl.
               Default is org.apache.openejb.assembler.classic.Assembler */
            String className = props.getProperty(EnvProps.ASSEMBLER);
            if (className == null) {
                className = props.getProperty("openejb.assembler", "org.apache.openejb.assembler.classic.Assembler");
            } else {
                logger.warning("startup.deprecatedPropertyName", EnvProps.ASSEMBLER);
            }

            logger.debug("startup.instantiatingAssemblerClass", className);
            Assembler assembler = null;

            try {
                assembler = (Assembler) toolkit.newInstance(className);
            } catch (OpenEJBException oe) {
                logger.fatal("startup.assemblerCannotBeInstantiated", oe);
                throw oe;
            } catch (Throwable t) {
                String msg = messages.message("startup.openejbEncounteredUnexpectedError");
                logger.fatal(msg, t);
                throw new OpenEJBException(msg, t);
            }

            SystemInstance.get().setComponent(Assembler.class, assembler);

            try {
                assembler.init(props);
            } catch (OpenEJBException oe) {
                logger.fatal("startup.assemblerFailedToInitialize", oe);
                throw oe;
            } catch (Throwable t) {
                String msg = messages.message("startup.assemblerEncounteredUnexpectedError");
                logger.fatal(msg, t);
                throw new OpenEJBException(msg, t);
            }

            try {
                assembler.build();
            } catch (OpenEJBException oe) {
                logger.fatal("startup.assemblerFailedToBuild", oe);
                throw oe;
            } catch (Throwable t) {
                String msg = messages.message("startup.assemblerEncounterUnexpectedBuildError");
                logger.fatal(msg, t);
                throw new OpenEJBException(msg, t);
            }

            ContainerSystem containerSystem = assembler.getContainerSystem();

            if (containerSystem == null) {
                String msg = messages.message("startup.assemblerReturnedNullContainer");
                logger.fatal(msg);
                throw new OpenEJBException(msg);
            }

            system.setComponent(ContainerSystem.class, containerSystem);

            if (logger.isDebugEnabled()) {
                logger.debug("startup.debugContainers", containerSystem.containers().length);

                if (containerSystem.containers().length > 0) {
                    Container[] c = containerSystem.containers();
                    logger.debug("startup.debugContainersType");
                    for (int i = 0; i < c.length; i++) {
                        String entry = "   ";
                        switch (c[i].getContainerType()) {
                            case BMP_ENTITY:
                                entry += "BMP ENTITY  ";
                                break;
                            case CMP_ENTITY:
                                entry += "CMP ENTITY  ";
                                break;
                            case STATEFUL:
                                entry += "STATEFUL    ";
                                break;
                            case STATELESS:
                                entry += "STATELESS   ";
                                break;
                            case MESSAGE_DRIVEN:
                                entry += "MESSAGE     ";
                                break;
                        }
                        entry += c[i].getContainerID();
                        logger.debug("startup.debugEntry", entry);
                    }
                }

                logger.debug("startup.debugDeployments", containerSystem.deployments().length);
                if (containerSystem.deployments().length > 0) {
                    logger.debug("startup.debugDeploymentsType");
                    DeploymentInfo[] d = containerSystem.deployments();
                    for (int i = 0; i < d.length; i++) {
                        String entry = "   ";
                        switch (d[i].getComponentType()) {
                            case BMP_ENTITY:
                                entry += "BMP_ENTITY  ";
                                break;
                            case CMP_ENTITY:
                                entry += "CMP_ENTITY  ";
                                break;
                            case STATEFUL:
                                entry += "STATEFUL    ";
                                break;
                            case STATELESS:
                                entry += "STATELESS   ";
                                break;
                            case MESSAGE_DRIVEN:
                                entry += "MESSAGE     ";
                                break;
                        }
                        entry += d[i].getDeploymentID();
                        logger.debug("startup.debugEntry", entry);
                    }
                }
            }

            SecurityService securityService = assembler.getSecurityService();
            if (securityService == null) {
                String msg = messages.message("startup.assemblerReturnedNullSecurityService");
                logger.fatal(msg);
                throw new OpenEJBException(msg);
            } else {
                logger.debug("startup.securityService", securityService.getClass().getName());
            }
            system.setComponent(SecurityService.class, securityService);

            TransactionManager transactionManager = assembler.getTransactionManager();
            if (transactionManager == null) {
                String msg = messages.message("startup.assemblerReturnedNullTransactionManager");
                logger.fatal(msg);
                throw new OpenEJBException(msg);
            } else {
                logger.debug("startup.transactionManager", transactionManager.getClass().getName());
            }
            SystemInstance.get().setComponent(TransactionManager.class, transactionManager);

            logger.debug("startup.ready");

        }

        public Throwable getInitialized() {
            return initialized;
        }
    }

    public static void destroy() {
        instance = null;
        SystemInstance.get().removeComponent(ContainerSystem.class);
    }

    /**
     * 1 usage
     * org.apache.openejb.core.ivm.naming.InitContextFactory
     */
    public static void init(Properties props) throws OpenEJBException {
        init(props, null);
    }

    private static Messages messages = new Messages("org.apache.openejb.util.resources");
    private static Logger logger = Logger.getInstance(LogCategory.OPENEJB_STARTUP, "org.apache.openejb.util.resources");

    /**
     * 2 usages
     */
    public static void init(Properties initProps, ApplicationServer appServer) throws OpenEJBException {
        if (isInitialized()) {
            if (instance != null){
                String msg = messages.message("startup.alreadyInitialized");
                logger.error(msg, instance.initialized);
                throw new OpenEJBException(msg, instance.initialized);
            } else {
                String msg = messages.message("startup.alreadyInitialized");
                logger.error(msg);
                throw new OpenEJBException(msg);
            }
        } else {
            instance = new Instance(initProps, appServer);
        }
    }

    /**
     * 1 usages
     */
    public static boolean isInitialized() {
        return instance != null || SystemInstance.get().getComponent(ContainerSystem.class) != null;
    }
}
