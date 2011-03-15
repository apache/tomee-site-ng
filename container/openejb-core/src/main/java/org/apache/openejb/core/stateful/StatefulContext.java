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
package org.apache.openejb.core.stateful;

import java.util.Map;

import javax.transaction.UserTransaction;
import javax.xml.rpc.handler.MessageContext;

import org.apache.openejb.core.BaseSessionContext;
import org.apache.openejb.core.Operation;
import org.apache.openejb.core.ThreadContext;
import org.apache.openejb.spi.SecurityService;


/**
 * @version $Rev$ $Date$
 */
public class StatefulContext extends BaseSessionContext {

    protected final static State[] states = new State[Operation.values().length];
    
    public static State[] getStates() {
        return states;
    }

    public StatefulContext(SecurityService securityService, UserTransaction userTransaction) {
        super(securityService, userTransaction);
    }

    protected State getState() {
        Operation operation = ThreadContext.getThreadContext().getCurrentOperation();
        State state = states[operation.ordinal()];

        if (state == null) throw new IllegalArgumentException("Invalid operation " + operation + " for this context");

        return state;
    }

    /**
     * PostConstruct, Pre-Destroy lifecycle callback interceptor methods
     */
    public static class LifecycleStatefulSessionState extends SessionState {

        public MessageContext getMessageContext() throws IllegalStateException {
            throw new IllegalStateException();
        }

        public Class getInvokedBusinessInterface() {
            throw new IllegalStateException();
        }

        public void setRollbackOnly() throws IllegalStateException {
            throw new IllegalStateException();
        }

        public boolean getRollbackOnly() throws IllegalStateException {
            throw new IllegalStateException();
        }

        public boolean isMessageContextAccessAllowed() {
            return false;
        }

        public boolean isTimerAccessAllowed() {
            return false;
        }

        public boolean isTimerMethodAllowed() {
            return false;
        }
    }

    /**
     * afterBegin
     * beforeCompletion
     */
    public static class BeforeCompletion extends SessionState {

        public Class getInvokedBusinessInterface() {
            throw new IllegalStateException();
        }

        public MessageContext getMessageContext() throws IllegalStateException {
            throw new IllegalStateException();
        }

        public boolean isMessageContextAccessAllowed() {
            return false;
        }
    }

    /**
     * afterCompletion
     */
    public static class AfterCompletion extends SessionState {
        public MessageContext getMessageContext() throws IllegalStateException {
            throw new IllegalStateException();
        }

        public Class getInvokedBusinessInterface() {
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

        public boolean isJNDIAccessAllowed() {
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

        public boolean isTimerMethodAllowed() {
            return false;
        }
    }

    static {
        states[Operation.INJECTION.ordinal()] = new InjectionSessionState();
        states[Operation.CREATE.ordinal()] = new LifecycleStatefulSessionState();
        states[Operation.BUSINESS.ordinal()] = new BusinessSessionState();
        states[Operation.AFTER_BEGIN.ordinal()] = new BeforeCompletion();
        states[Operation.BEFORE_COMPLETION.ordinal()] = new BeforeCompletion();
        states[Operation.AFTER_COMPLETION.ordinal()] = new AfterCompletion();
        states[Operation.TIMEOUT.ordinal()] = new TimeoutSessionState();
        states[Operation.PRE_DESTROY.ordinal()] = new LifecycleStatefulSessionState();
        states[Operation.REMOVE.ordinal()] = new LifecycleStatefulSessionState();
        states[Operation.POST_CONSTRUCT.ordinal()] = new LifecycleStatefulSessionState();
    }

	public boolean wasCancelCalled() {
		//TODO: next openejb version
		throw new UnsupportedOperationException("Method not implemented: boolean wasCancelCalled()");
	}

	public Map<String, Object> getContextData() {
		//TODO: next openejb version
		throw new UnsupportedOperationException("Method not implemented: Map<String, Object> getContextData()");
	}

}
