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
package org.apache.openejb.mdb;

import java.security.Principal;
import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;
import javax.ejb.MessageDrivenContext;
import javax.ejb.TimerService;
import javax.transaction.UserTransaction;
import javax.transaction.TransactionManager;
import javax.xml.rpc.handler.MessageContext;
import javax.security.auth.Subject;

import org.apache.openejb.EJBContextImpl;
import org.apache.openejb.EJBInstanceContext;
import org.apache.openejb.EJBOperation;
import org.apache.openejb.timer.TimerState;

/**
 * Implementation of MessageDrivenContext using the State pattern to determine
 * which methods can be called given the current state of the Session bean.
 *
 * @version $Revision$ $Date$
 */
public class MdbContext extends EJBContextImpl implements MessageDrivenContext {
    public MdbContext(MdbInstanceContext context, TransactionManager transactionManager, UserTransaction userTransaction) {
        super(context, transactionManager, userTransaction);
        state = MdbContext.INACTIVE;
    }

    void setState(EJBOperation operation) {
        state = states[operation.getOrdinal()];
        assert (state != null) : "Invalid EJBOperation for MDB, ordinal=" + operation.getOrdinal();

        context.setTimerServiceAvailable(timerServiceAvailable[operation.getOrdinal()]);
    }

    public boolean setTimerState(EJBOperation operation) {
        boolean oldTimerState = TimerState.getTimerState();
        TimerState.setTimerState(timerMethodsAvailable[operation.getOrdinal()]);
        return oldTimerState;
    }

    public MessageContext getMessageContext() throws IllegalStateException {
        return ((MdbContextState) state).getMessageContext();
    }

    public abstract static class MdbContextState extends EJBContextState {
        protected MessageContext getMessageContext() {
            throw new UnsupportedOperationException();
        }
    }

    public static final MdbContextState INACTIVE = new MdbContextState() {
        public EJBHome getEJBHome(EJBInstanceContext context) {
            throw new IllegalStateException("getEJBHome() cannot be called when inactive");
        }

        public EJBLocalHome getEJBLocalHome(EJBInstanceContext context) {
            throw new IllegalStateException("getEJBLocalHome() cannot be called when inactive");
        }

        public EJBObject getEJBObject(EJBInstanceContext context) {
            throw new IllegalStateException("getEJBObject() cannot be called when inactive");
        }

        public EJBLocalObject getEJBLocalObject(EJBInstanceContext context) {
            throw new IllegalStateException("getEJBLocalObject() cannot be called when inactive");
        }

        public Principal getCallerPrincipal(Subject callerSubject) {
            throw new IllegalStateException("getCallerPrincipal() cannot be called when inactive");
        }

        public boolean isCallerInRole(String s, EJBInstanceContext context) {
            throw new IllegalStateException("isCallerInRole(String) cannot be called when inactive");
        }

        public UserTransaction getUserTransaction(UserTransaction userTransaction) {
            throw new IllegalStateException("getUserTransaction() cannot be called when inactive");
        }

        public void setRollbackOnly(EJBInstanceContext context, TransactionManager transactionManager) {
            throw new IllegalStateException("setRollbackOnly() cannot be called when inactive");
        }

        public boolean getRollbackOnly(EJBInstanceContext context, TransactionManager transactionManager) {
            throw new IllegalStateException("getRollbackOnly() cannot be called when inactive");
        }

        public MessageContext getMessageContext() {
            throw new IllegalStateException("getMessageContext() cannot be called when inactive");
        }

        public TimerService getTimerService(EJBInstanceContext context) {
            throw new IllegalStateException("getTimerService() cannot be called when inactive");
        }
    };

    public static final MdbContextState SETSESSIONCONTEXT = new MdbContextState() {
        public EJBObject getEJBObject(EJBInstanceContext context) {
            throw new IllegalStateException("getEJBObject() cannot be called from setMessageDrivenContext(MessageDrivenContext)");
        }

        public EJBLocalObject getEJBLocalObject(EJBInstanceContext context) {
            throw new IllegalStateException("getEJBLocalObject() cannot be called from setMessageDrivenContext(MessageDrivenContext)");
        }

        public Principal getCallerPrincipal(Subject callerSubject) {
            throw new IllegalStateException("getCallerPrincipal() cannot be called from setMessageDrivenContext(MessageDrivenContext)");
        }

        public boolean isCallerInRole(String s, EJBInstanceContext context) {
            throw new IllegalStateException("isCallerInRole(String) cannot be called from setMessageDrivenContext(MessageDrivenContext)");
        }

        public UserTransaction getUserTransaction(UserTransaction userTransaction) {
            throw new IllegalStateException("getUserTransaction() cannot be called from setMessageDrivenContext(MessageDrivenContext)");
        }

        public void setRollbackOnly(EJBInstanceContext context, TransactionManager transactionManager) {
            throw new IllegalStateException("setRollbackOnly() cannot be called from setMessageDrivenContext(MessageDrivenContext)");
        }

        public boolean getRollbackOnly(EJBInstanceContext context, TransactionManager transactionManager) {
            throw new IllegalStateException("getRollbackOnly() cannot be called from setMessageDrivenContext(MessageDrivenContext)");
        }

        public MessageContext getMessageContext() {
            throw new IllegalStateException("getMessageContext() cannot be called from setMessageDrivenContext(MessageDrivenContext)");
        }

        public TimerService getTimerService(EJBInstanceContext context) {
            throw new IllegalStateException("getTimerService() cannot be called from setMessageDrivenContext(MessageDrivenContext)");
        }
    };

    public static final MdbContextState EJBCREATEREMOVE = new MdbContextState() {
        public Principal getCallerPrincipal(Subject callerSubject) {
            throw new IllegalStateException("getCallerPrincipal() cannot be called from ejbCreate/ejbRemove");
        }

        public boolean isCallerInRole(String s, EJBInstanceContext context) {
            throw new IllegalStateException("isCallerInRole(String) cannot be called from ejbCreate/ejbRemove");
        }

        public void setRollbackOnly(EJBInstanceContext context, TransactionManager transactionManager) {
            throw new IllegalStateException("setRollbackOnly() cannot be called from ejbCreate/ejbRemove");
        }

        public boolean getRollbackOnly(EJBInstanceContext context, TransactionManager transactionManager) {
            throw new IllegalStateException("getRollbackOnly() cannot be called from ejbCreate/ejbRemove");
        }

        public MessageContext getMessageContext() {
            throw new IllegalStateException("getMessageContext() cannot be called from ejbCreate/ejbRemove");
        }
    };

    public static final MdbContextState BIZ_INTERFACE = new MdbContextState() {
        public MessageContext getMessageContext() {
            throw new IllegalStateException("getMessageContext() cannot be called in a business method invocation from component interface)");
        }
    };


    public static final MdbContextState BIZ_WSENDPOINT = new MdbContextState() {
        public boolean isCallerInRole(String s, EJBInstanceContext context) {
            throw new IllegalStateException("isCallerInRole(String) cannot be called in a business method invocation from a web-service endpoint");
        }
    };

    public static final MdbContextState EJBTIMEOUT = new MdbContextState() {
        public boolean isCallerInRole(String s, EJBInstanceContext context) {
            throw new IllegalStateException("isCallerInRole(String) cannot be called from ejbTimeout");
        }

        public MessageContext getMessageContext() {
            throw new IllegalStateException("getMessageContext() cannot be called from ejbTimeout");
        }
    };

    private static final MdbContextState states[] = new MdbContextState[EJBOperation.MAX_ORDINAL];

    static {
        states[EJBOperation.INACTIVE.getOrdinal()] = INACTIVE;
        states[EJBOperation.SETCONTEXT.getOrdinal()] = SETSESSIONCONTEXT;
        states[EJBOperation.EJBCREATE.getOrdinal()] = EJBCREATEREMOVE;
        states[EJBOperation.EJBREMOVE.getOrdinal()] = EJBCREATEREMOVE;
        states[EJBOperation.BIZMETHOD.getOrdinal()] = BIZ_INTERFACE;
        states[EJBOperation.ENDPOINT.getOrdinal()] = BIZ_WSENDPOINT;
        states[EJBOperation.TIMEOUT.getOrdinal()] = EJBTIMEOUT;
    }
    //spec p 344
    private static final boolean timerServiceAvailable[] = new boolean[EJBOperation.MAX_ORDINAL];

    static {
        timerServiceAvailable[EJBOperation.EJBCREATE.getOrdinal()] = true;
        timerServiceAvailable[EJBOperation.EJBREMOVE.getOrdinal()] = true;
        timerServiceAvailable[EJBOperation.BIZMETHOD.getOrdinal()] = true;
        timerServiceAvailable[EJBOperation.ENDPOINT.getOrdinal()] = true;
        timerServiceAvailable[EJBOperation.TIMEOUT.getOrdinal()] = true;
    }

    private static final boolean timerMethodsAvailable[] = new boolean[EJBOperation.MAX_ORDINAL];

    static {
        timerMethodsAvailable[EJBOperation.BIZMETHOD.getOrdinal()] = true;
        timerMethodsAvailable[EJBOperation.TIMEOUT.getOrdinal()] = true;
    }

}
