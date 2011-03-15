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
package org.apache.openejb.core.entity;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Map;

import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;
import javax.ejb.TimerService;
import javax.transaction.UserTransaction;

import org.apache.openejb.DeploymentInfo;
import org.apache.openejb.InterfaceType;
import org.apache.openejb.InternalErrorException;
import org.apache.openejb.core.BaseContext;
import org.apache.openejb.core.Operation;
import org.apache.openejb.core.ThreadContext;
import org.apache.openejb.core.ivm.EjbObjectProxyHandler;
import org.apache.openejb.core.ivm.IntraVmProxy;
import org.apache.openejb.spi.SecurityService;
import org.apache.openejb.util.proxy.ProxyManager;

/**
 * @version $Rev$ $Date$
 */
public class EntityContext extends BaseContext implements javax.ejb.EntityContext {
    protected final static State[] states = new State[Operation.values().length];

    public static State[] getStates() {
        return states;
    }

    public EntityContext(SecurityService securityService) {
        super(securityService);
    }

    protected State getState() {
        Operation operation = ThreadContext.getThreadContext().getCurrentOperation();
        State state = states[operation.ordinal()];

        if (state == null) throw new IllegalArgumentException("Invalid operation " + operation + " for this context");

        return state;
    }

    public EJBLocalObject getEJBLocalObject() throws IllegalStateException {
        return ((EntityState) getState()).getEJBLocalObject();
    }

    public EJBObject getEJBObject() throws IllegalStateException {
        return ((EntityState) getState()).getEJBObject();
    }

    public Object getPrimaryKey() throws IllegalStateException {
        return ((EntityState) getState()).getPrimaryKey();
    }

    private static class EntityState extends State {

        public EJBLocalObject getEJBLocalObject() throws IllegalStateException {
            ThreadContext threadContext = ThreadContext.getThreadContext();
            DeploymentInfo di = threadContext.getDeploymentInfo();

            if (di.getLocalInterface() == null) {
                throw new IllegalStateException("EJB " + di.getDeploymentID() + " does not have a local interface");
            }

            EjbObjectProxyHandler handler = new EntityEjbObjectHandler(di, threadContext.getPrimaryKey(), InterfaceType.EJB_LOCAL, new ArrayList<Class>());

            try {
                Class[] interfaces = new Class[]{di.getLocalInterface(), IntraVmProxy.class};
                return (EJBLocalObject) ProxyManager.newProxyInstance(interfaces, handler);
            } catch (IllegalAccessException iae) {
                throw new InternalErrorException("Could not create IVM proxy for " + di.getLocalInterface() + " interface", iae);
            }
        }

        public EJBObject getEJBObject() throws IllegalStateException {
            ThreadContext threadContext = ThreadContext.getThreadContext();
            DeploymentInfo di = threadContext.getDeploymentInfo();

            if (di.getRemoteInterface() == null) {
                throw new IllegalStateException("EJB " + di.getDeploymentID() + " does not have a remote interface");
            }

            EjbObjectProxyHandler handler = new EntityEjbObjectHandler(di.getContainer().getDeploymentInfo(di.getDeploymentID()), threadContext.getPrimaryKey(), InterfaceType.EJB_OBJECT, new ArrayList<Class>());
            try {
                Class[] interfaces = new Class[]{di.getRemoteInterface(), IntraVmProxy.class};
                return (EJBObject) ProxyManager.newProxyInstance(interfaces, handler);
            } catch (IllegalAccessException iae) {
                throw new InternalErrorException("Could not create IVM proxy for " + di.getRemoteInterface() + " interface", iae);
            }
        }

        public Object getPrimaryKey() throws IllegalStateException {
            ThreadContext threadContext = ThreadContext.getThreadContext();
            return threadContext.getPrimaryKey();
        }
        
        public boolean isTimerMethodAllowed() {
            return false;
        }
    }

    protected static class ContextEntityState extends EntityState {

        public EJBLocalObject getEJBLocalObject() throws IllegalStateException {
            throw new IllegalStateException();
        }

        public EJBObject getEJBObject() throws IllegalStateException {
            throw new IllegalStateException();
        }

        public Object getPrimaryKey() throws IllegalStateException {
            throw new IllegalStateException();
        }

        public Principal getCallerPrincipal(SecurityService securityService) {
            throw new IllegalStateException();
        }

        public boolean isCallerInRole(SecurityService securityService, String roleName) {
            throw new IllegalStateException();
        }

        public UserTransaction getUserTransaction(UserTransaction userTransaction) throws IllegalStateException {
            throw new IllegalStateException();
        }

        public void setRollbackOnly() throws IllegalStateException {
            throw new IllegalStateException();
        }

        public boolean getRollbackOnly() throws IllegalStateException {
            throw new IllegalStateException();
        }

        public TimerService getTimerService() throws IllegalStateException {
            throw new IllegalStateException();
        }

        public boolean isUserTransactionAccessAllowed() {
            return false;
        }

        public boolean isMessageContextAccessAllowed() {
            return false;
        }

        public boolean isEntityManagerFactoryAccessAllowed() {
            return false;
        }

        public boolean isEntityManagerAccessAllowed() {
            return false;
        }

        public boolean isTimerAccessAllowed() {
            return false;
        }
    }

    protected static class CreateEntityState extends EntityState {

        public EJBLocalObject getEJBLocalObject() throws IllegalStateException {
            throw new IllegalStateException();
        }

        public EJBObject getEJBObject() throws IllegalStateException {
            throw new IllegalStateException();
        }

        public Object getPrimaryKey() throws IllegalStateException {
            throw new IllegalStateException();
        }

        public UserTransaction getUserTransaction(UserTransaction userTransaction) throws IllegalStateException {
            throw new IllegalStateException();
        }

        public boolean isUserTransactionAccessAllowed() {
            return false;
        }

        public boolean isMessageContextAccessAllowed() {
            return false;
        }

        public boolean isTimerAccessAllowed() {
            return false;
        }
    }

    protected static class LifecycleBusinessTimeoutEntityState extends EntityState {

        public UserTransaction getUserTransaction(UserTransaction userTransaction) throws IllegalStateException {
            throw new IllegalStateException();
        }

        public boolean isUserTransactionAccessAllowed() {
            return false;
        }

        public boolean isMessageContextAccessAllowed() {
            return false;
        }

        public boolean isTimerMethodAllowed() {
            return true;
        }
    }

    protected static class FindEntityState extends EntityState {

        public EJBLocalObject getEJBLocalObject() throws IllegalStateException {
            throw new IllegalStateException();
        }

        public EJBObject getEJBObject() throws IllegalStateException {
            throw new IllegalStateException();
        }

        public Object getPrimaryKey() throws IllegalStateException {
            throw new IllegalStateException();
        }

        public UserTransaction getUserTransaction(UserTransaction userTransaction) throws IllegalStateException {
            throw new IllegalStateException();
        }

        public TimerService getTimerService() throws IllegalStateException {
            throw new IllegalStateException();
        }

        public boolean isUserTransactionAccessAllowed() {
            return false;
        }

        public boolean isMessageContextAccessAllowed() {
            return false;
        }

        public boolean isTimerAccessAllowed() {
            return false;
        }
    }

    protected static class HomeEntityState extends EntityState {

        public EJBLocalObject getEJBLocalObject() throws IllegalStateException {
            throw new IllegalStateException();
        }

        public EJBObject getEJBObject() throws IllegalStateException {
            throw new IllegalStateException();
        }

        public Object getPrimaryKey() throws IllegalStateException {
            throw new IllegalStateException();
        }

        public UserTransaction getUserTransaction(UserTransaction userTransaction) throws IllegalStateException {
            throw new IllegalStateException();
        }

        public boolean isUserTransactionAccessAllowed() {
            return false;
        }

        public boolean isMessageContextAccessAllowed() {
            return false;
        }

        public boolean isTimerAccessAllowed() {
            return false;
        }
    }

    protected static class ActivatePassivateEntityState extends EntityState {

        public Principal getCallerPrincipal(SecurityService securityService) {
            throw new IllegalStateException();
        }

        public boolean isCallerInRole(SecurityService securityService, String roleName) {
            throw new IllegalStateException();
        }

        public UserTransaction getUserTransaction(UserTransaction userTransaction) throws IllegalStateException {
            throw new IllegalStateException();
        }

        public void setRollbackOnly() throws IllegalStateException {
            throw new IllegalStateException();
        }

        public boolean getRollbackOnly() throws IllegalStateException {
            throw new IllegalStateException();
        }

        public boolean isUserTransactionAccessAllowed() {
            return false;
        }

        public boolean isMessageContextAccessAllowed() {
            return false;
        }

        public boolean isEntityManagerFactoryAccessAllowed() {
            return false;
        }

        public boolean isEntityManagerAccessAllowed() {
            return false;
        }

        public boolean isTimerAccessAllowed() {
            return false;
        }
    }

    static {
        states[Operation.SET_CONTEXT.ordinal()] = new ContextEntityState();
        states[Operation.UNSET_CONTEXT.ordinal()] = new ContextEntityState();
        states[Operation.CREATE.ordinal()] = new CreateEntityState();
        states[Operation.POST_CREATE.ordinal()] = new LifecycleBusinessTimeoutEntityState();
        states[Operation.REMOVE.ordinal()] = new LifecycleBusinessTimeoutEntityState();
        states[Operation.FIND.ordinal()] = new FindEntityState();
        states[Operation.HOME.ordinal()] = new HomeEntityState();
        states[Operation.ACTIVATE.ordinal()] = new ActivatePassivateEntityState();
        states[Operation.PASSIVATE.ordinal()] = new ActivatePassivateEntityState();
        states[Operation.LOAD.ordinal()] = new LifecycleBusinessTimeoutEntityState();
        states[Operation.STORE.ordinal()] = new LifecycleBusinessTimeoutEntityState();
        states[Operation.BUSINESS.ordinal()] = new LifecycleBusinessTimeoutEntityState();
        states[Operation.TIMEOUT.ordinal()] = new LifecycleBusinessTimeoutEntityState();
    }

	public Map<String, Object> getContextData() {
		//TODO: next openejb version
		throw new UnsupportedOperationException("Method not implemented: Map<String, Object> getContextData()");
	}
}
