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
package org.apache.openejb.core.ivm;

import java.io.ObjectStreamException;
import java.lang.reflect.Method;
import java.rmi.AccessException;
import java.rmi.RemoteException;
import java.util.List;

import javax.ejb.AccessLocalException;
import javax.ejb.EJBAccessException;
import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;

import org.apache.openejb.DeploymentInfo;
import org.apache.openejb.InterfaceType;
import org.apache.openejb.core.ServerFederation;
import org.apache.openejb.spi.ApplicationServer;
import org.apache.openejb.util.LogCategory;
import org.apache.openejb.util.Logger;

public abstract class EjbObjectProxyHandler extends BaseEjbProxyHandler {
    private static final Logger logger = Logger.getInstance(LogCategory.OPENEJB, "org.apache.openejb.util.resources");
    static final java.util.HashMap dispatchTable;

    static {
        dispatchTable = new java.util.HashMap();
        dispatchTable.put("getHandle", new Integer(1));
        dispatchTable.put("getPrimaryKey", new Integer(2));
        dispatchTable.put("isIdentical", new Integer(3));
        dispatchTable.put("remove", new Integer(4));
        dispatchTable.put("getEJBHome", new Integer(5));
        dispatchTable.put("getEJBLocalHome", new Integer(6));
        
    }

    public EjbObjectProxyHandler(DeploymentInfo deploymentInfo, Object pk, InterfaceType interfaceType, List<Class> interfaces) {
        super(deploymentInfo, pk, interfaceType, interfaces);
    }

    public abstract Object getRegistryId();

    public Object _invoke(Object p, Class interfce, Method m, Object[] a) throws Throwable {
        java.lang.Object retValue = null;
        java.lang.Throwable exc = null;

        try {
            if (logger.isInfoEnabled()) {
                logger.info("invoking method " + m.getName() + " on " + deploymentID + " with identity " + primaryKey);
            }
            Integer operation = (Integer) dispatchTable.get(m.getName());
            if(operation != null){
                if(operation.intValue() == 3){
                    if(m.getParameterTypes()[0] != EJBObject.class && m.getParameterTypes()[0] != EJBLocalObject.class ){
                        operation = null;  
                    }
                } else {
                    operation = (m.getParameterTypes().length == 0)?operation:null;
                }                
            }
            if (operation == null || !interfaceType.isComponent() ) {
                retValue = businessMethod(interfce, m, a, p);
            } else {
                switch (operation.intValue()) {
                    case 1:
                        retValue = getHandle(m, a, p);
                        break;
                    case 2:
                        retValue = getPrimaryKey(m, a, p);
                        break;
                    case 3:
                        retValue = isIdentical(m, a, p);
                        break;
                    case 4:
                        retValue = remove(interfce, m, a, p);
                        break;
                    case 5:
                        retValue = getEJBHome(m, a, p);
                        break;
                    case 6:
                        retValue = getEJBLocalHome(m, a, p);
                        break;
                    default:
                        throw new RuntimeException("Inconsistent internal state");
                }
            }

            return retValue;

            /*
            * The ire is thrown by the container system and propagated by
            * the server to the stub.
            */
        } catch (org.apache.openejb.InvalidateReferenceException ire) {
            invalidateAllHandlers(getRegistryId());
            exc = (ire.getRootCause() != null) ? ire.getRootCause() : ire;
            throw exc;
            /*
            * Application exceptions must be reported dirctly to the client. They
            * do not impact the viability of the proxy.
            */
        } catch (org.apache.openejb.ApplicationException ae) {
            exc = (ae.getRootCause() != null) ? ae.getRootCause() : ae;
            if (exc instanceof EJBAccessException) {
                if (interfaceType.isBusiness()) {
                    throw exc;
                } else {
                    if (interfaceType.isLocal()) {
                        throw new AccessLocalException(exc.getMessage()).initCause(exc.getCause());
                    } else {
                        throw new AccessException(exc.getMessage());
                    }
                }

            }
            throw exc;

            /*
            * A system exception would be highly unusual and would indicate a sever
            * problem with the container system.
            */
        } catch (org.apache.openejb.SystemException se) {
            invalidateReference();
            exc = (se.getRootCause() != null) ? se.getRootCause() : se;
            logger.error("The container received an unexpected exception: ", exc);
            throw new RemoteException("Container has suffered a SystemException", exc);
        } catch (org.apache.openejb.OpenEJBException oe) {
            exc = (oe.getRootCause() != null) ? oe.getRootCause() : oe;
            logger.warning("The container received an unexpected exception: ", exc);
            throw new RemoteException("Unknown Container Exception", oe.getRootCause());
        } finally {
            if (logger.isDebugEnabled()) {
                if (exc == null) {
                    logger.debug("finished invoking method " + m.getName() + ". Return value:" + retValue);
                } else {
                    logger.debug("finished invoking method " + m.getName() + " with exception " + exc);
                }
            } else if (logger.isInfoEnabled()) {
                if (exc == null) {
                    logger.debug("finished invoking method " + m.getName());
                } else {
                    logger.debug("finished invoking method " + m.getName() + " with exception " + exc);
                }
            }
        }
    }

    protected Object getEJBHome(Method method, Object[] args, Object proxy) throws Throwable {
        checkAuthorization(method);
        return getDeploymentInfo().getEJBHome();
    }

    protected Object getEJBLocalHome(Method method, Object[] args, Object proxy) throws Throwable {
        checkAuthorization(method);
        return getDeploymentInfo().getEJBLocalHome();
    }

    protected Object getHandle(Method method, Object[] args, Object proxy) throws Throwable {
        checkAuthorization(method);
        return new IntraVmHandle(proxy);
    }

    public org.apache.openejb.ProxyInfo getProxyInfo() {
        return new org.apache.openejb.ProxyInfo(getDeploymentInfo(), primaryKey, getInterfaces(), interfaceType);
    }

    protected Object _writeReplace(Object proxy) throws ObjectStreamException {
        /*
         * If the proxy is being  copied between bean instances in a RPC
         * call we use the IntraVmArtifact
         */
        if (IntraVmCopyMonitor.isIntraVmCopyOperation()) {
            return new IntraVmArtifact(proxy);
            /*
            * If the proxy is referenced by a stateful bean that is  being
            * passivated by the container we allow this object to be serialized.
            */
        } else if (IntraVmCopyMonitor.isStatefulPassivationOperation()) {
            return proxy;
            /*
            * If the proxy is being copied between class loaders
            * we allow this object to be serialized.
            */
        } else if (IntraVmCopyMonitor.isCrossClassLoaderOperation()) {
            return proxy;
            /*
            * If the proxy is serialized outside the core container system,
            * we allow the application server to handle it.
            */
        } else {
            ApplicationServer applicationServer = ServerFederation.getApplicationServer();
            if (interfaceType.isBusiness()){
                return applicationServer.getBusinessObject(this.getProxyInfo());
            } else {
                return applicationServer.getEJBObject(this.getProxyInfo());
            }
        }
    }

    protected abstract Object getPrimaryKey(Method method, Object[] args, Object proxy) throws Throwable;

    protected abstract Object isIdentical(Method method, Object[] args, Object proxy) throws Throwable;

    protected abstract Object remove(Class interfce, Method method, Object[] args, Object proxy) throws Throwable;

    protected Object businessMethod(Class interfce, Method method, Object[] args, Object proxy) throws Throwable {
//        checkAuthorization(method);
        return container.invoke(deploymentID, interfce, method, args, primaryKey);
    }

    public static Object createProxy(DeploymentInfo deploymentInfo, Object primaryKey, InterfaceType interfaceType) {
        return createProxy(deploymentInfo, primaryKey, interfaceType, null);
    }

    public static Object createProxy(DeploymentInfo deploymentInfo, Object primaryKey, InterfaceType interfaceType, List<Class> interfaces) {
        if (!interfaceType.isHome()){
            interfaceType = interfaceType.getCounterpart();
        }
        EjbHomeProxyHandler homeHandler = EjbHomeProxyHandler.createHomeHandler(deploymentInfo, interfaceType, interfaces);
        return homeHandler.createProxy(primaryKey);
    }
}
