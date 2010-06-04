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
package org.apache.openejb.core;

import java.io.Serializable;
import java.io.ObjectStreamException;
import java.security.Identity;
import java.security.Principal;
import java.util.Properties;
import javax.ejb.EJBContext;
import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.TimerService;
import javax.naming.NamingException;
import javax.naming.Context;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import javax.transaction.NotSupportedException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;

import org.apache.openejb.DeploymentInfo;
import org.apache.openejb.core.timer.EjbTimerService;
import org.apache.openejb.core.timer.TimerServiceImpl;
import org.apache.openejb.core.ivm.IntraVmArtifact;
import org.apache.openejb.core.transaction.EjbUserTransaction;
import org.apache.openejb.core.transaction.TransactionPolicy;
import org.apache.openejb.spi.SecurityService;


/**
 * @version $Rev$ $Date$
 */
public abstract class BaseContext implements EJBContext, Serializable {

    private final SecurityService securityService;
    private final UserTransaction userTransaction;

    protected BaseContext(SecurityService securityService) {
        this(securityService, new EjbUserTransaction());
    }

    protected BaseContext(SecurityService securityService, UserTransaction userTransaction) {
        this.securityService = securityService;
        this.userTransaction = new UserTransactionWrapper(userTransaction);
    }

    protected static State[] states;
    
    public static State[] getStates() {
        return states;
    }
    
    protected abstract State getState();

    public EJBHome getEJBHome() {
        return getState().getEJBHome();
    }

    public EJBLocalHome getEJBLocalHome() {
        return getState().getEJBLocalHome();
    }

    public final Properties getEnvironment() {
        throw new UnsupportedOperationException();
    }

    public final Identity getCallerIdentity() {
        throw new UnsupportedOperationException();
    }

    public Principal getCallerPrincipal() {
        Principal callerPrincipal = getState().getCallerPrincipal(securityService);
        if (callerPrincipal == null) callerPrincipal = UnauthenticatedPrincipal.INSTANCE;
        return callerPrincipal;
    }

    public final boolean isCallerInRole(Identity identity) {
        throw new UnsupportedOperationException();
    }

    public boolean isCallerInRole(String roleName) {
        return getState().isCallerInRole(securityService, roleName);
    }

    public UserTransaction getUserTransaction() throws IllegalStateException {
        return getState().getUserTransaction(userTransaction);
    }

    public void setRollbackOnly() throws IllegalStateException {
        getState().setRollbackOnly();
    }

    public boolean getRollbackOnly() throws IllegalStateException {
        return getState().getRollbackOnly();
    }

    public TimerService getTimerService() throws IllegalStateException {
        return getState().getTimerService();
    }

    public Object lookup(String name) {
        ThreadContext threadContext = ThreadContext.getThreadContext();
        DeploymentInfo deploymentInfo = threadContext.getDeploymentInfo();
        Context jndiEnc = deploymentInfo.getJndiEnc();
        try {
            jndiEnc = (Context) jndiEnc.lookup("java:comp/env");
            return jndiEnc.lookup(name);
        } catch (NamingException e) {
            throw new IllegalArgumentException(e);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(e);
        }

//        try {
//            InitialContext initialContext = new InitialContext();
//            Context ctx = (Context) initialContext.lookup("java:comp/env");
//            return ctx.lookup(name);
//        } catch (NamingException e) {
//            throw new IllegalArgumentException(e);
//        } catch (RuntimeException e) {
//            throw new IllegalArgumentException(e);
//        }
    }

    public boolean isUserTransactionAccessAllowed() {
        return getState().isUserTransactionAccessAllowed();
    }

    public boolean isMessageContextAccessAllowed() {
        return getState().isMessageContextAccessAllowed();
    }

    public boolean isJNDIAccessAllowed() {
        return getState().isJNDIAccessAllowed();
    }

    public boolean isEntityManagerFactoryAccessAllowed() {
        return getState().isEntityManagerFactoryAccessAllowed();
    }

    public boolean isEntityManagerAccessAllowed() {
        return getState().isEntityManagerAccessAllowed();
    }

    public boolean isTimerAccessAllowed() {
        return getState().isTimerAccessAllowed();
    }

    public static boolean isTimerMethodAllowed() {
        State[] currentStates = ThreadContext.getThreadContext().getCurrentAllowedStates();
        State currentState = currentStates[ThreadContext.getThreadContext().getCurrentOperation().ordinal()];
        
        return currentState.isTimerMethodAllowed();
    }

    public class UserTransactionWrapper implements UserTransaction {
        private UserTransaction userTransaction;

        public UserTransactionWrapper(UserTransaction userTransaction) {
            this.userTransaction = userTransaction;
        }

        public void begin() throws NotSupportedException, SystemException {
            if (!isUserTransactionAccessAllowed()) {
                throw new IllegalStateException();
            }
            userTransaction.begin();
        }

        public void commit() throws HeuristicMixedException, HeuristicRollbackException, IllegalStateException, RollbackException, SecurityException, SystemException {
            if (!isUserTransactionAccessAllowed()) {
                throw new IllegalStateException();
            }
            userTransaction.commit();
        }

        public int getStatus() throws SystemException {
            if (!isUserTransactionAccessAllowed()) {
                throw new IllegalStateException();
            }
            return userTransaction.getStatus();
        }

        public void rollback() throws IllegalStateException, SecurityException, SystemException {
            if (!isUserTransactionAccessAllowed()) {
                throw new IllegalStateException();
            }
            userTransaction.rollback();
        }

        public void setRollbackOnly() throws IllegalStateException, SystemException {
            if (!isUserTransactionAccessAllowed()) {
                throw new IllegalStateException();
            }
            userTransaction.setRollbackOnly();
        }

        public void setTransactionTimeout(int i) throws SystemException {
            if (!isUserTransactionAccessAllowed()) {
                throw new IllegalStateException();
            }
            userTransaction.setTransactionTimeout(i);
        }
    }
    
    public static class State {

        public EJBHome getEJBHome() {
            ThreadContext threadContext = ThreadContext.getThreadContext();
            DeploymentInfo di = threadContext.getDeploymentInfo();

            return di.getEJBHome();
        }

        public EJBLocalHome getEJBLocalHome() {
            ThreadContext threadContext = ThreadContext.getThreadContext();
            DeploymentInfo di = threadContext.getDeploymentInfo();

            return di.getEJBLocalHome();
        }

        public Principal getCallerPrincipal(SecurityService securityService) {
            return securityService.getCallerPrincipal();
        }

        public boolean isCallerInRole(SecurityService securityService, String roleName) {
            return securityService.isCallerInRole(roleName);
        }

        public UserTransaction getUserTransaction(UserTransaction userTransaction) throws IllegalStateException {
            ThreadContext threadContext = ThreadContext.getThreadContext();
            DeploymentInfo di = threadContext.getDeploymentInfo();

            if (di.isBeanManagedTransaction()) {
                return userTransaction;
            } else {
                throw new IllegalStateException("container-managed transaction beans can not access the UserTransaction");
            }
        }

        public void setRollbackOnly() throws IllegalStateException {
            ThreadContext threadContext = ThreadContext.getThreadContext();
            DeploymentInfo di = threadContext.getDeploymentInfo();

            if (di.isBeanManagedTransaction()) {
                throw new IllegalStateException("bean-managed transaction beans can not access the setRollbackOnly() method");
            }

            TransactionPolicy txPolicy = threadContext.getTransactionPolicy();
            if (txPolicy == null) {
                throw new IllegalStateException("ThreadContext does not contain a TransactionEnvironment");
            }
            if (!txPolicy.isTransactionActive()) {
                // this would be true for Supports tx attribute where no tx was propagated
                throw new IllegalStateException("No current transaction");
            }
            txPolicy.setRollbackOnly();
        }

        public boolean getRollbackOnly() throws IllegalStateException {
            ThreadContext threadContext = ThreadContext.getThreadContext();
            DeploymentInfo di = threadContext.getDeploymentInfo();

            if (di.isBeanManagedTransaction()) {
                throw new IllegalStateException("bean-managed transaction beans can not access the getRollbackOnly() method: deploymentId=" + di.getDeploymentID());
            }

            TransactionPolicy transactionPolicy = threadContext.getTransactionPolicy();
            if (transactionPolicy == null) {
                throw new IllegalStateException("ThreadContext does not contain a TransactionEnvironment");
            }
            if (!transactionPolicy.isTransactionActive()) {
                // this would be true for Supports tx attribute where no tx was propagated
                throw new IllegalStateException("No current transaction");
            }
            return transactionPolicy.isRollbackOnly();
        }

        public TimerService getTimerService() throws IllegalStateException {
            ThreadContext threadContext = ThreadContext.getThreadContext();
            DeploymentInfo deploymentInfo = threadContext.getDeploymentInfo();
            EjbTimerService timerService = deploymentInfo.getEjbTimerService();
            if (timerService == null) {
                throw new IllegalStateException("This ejb does not support timers " + deploymentInfo.getDeploymentID());
            }
            return new TimerServiceImpl(timerService, threadContext.getPrimaryKey());
        }

        public boolean isTimerAccessAllowed() {
            return true;
        }

        public boolean isTimerMethodAllowed() {
            return true;
        }

        public boolean isUserTransactionAccessAllowed() {
            ThreadContext threadContext = ThreadContext.getThreadContext();
            DeploymentInfo di = threadContext.getDeploymentInfo();

            return di.isBeanManagedTransaction();
        }

        public boolean isMessageContextAccessAllowed() {
            return true;
        }

        public boolean isJNDIAccessAllowed() {
            return true;
        }

        public boolean isEntityManagerFactoryAccessAllowed() {
            return true;
        }

        public boolean isEntityManagerAccessAllowed() {
            return true;
        }

    }

    protected Object writeReplace() throws ObjectStreamException {
        return new IntraVmArtifact(this, true);
    }
}
