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
package org.apache.openejb.core.singleton;

import java.util.Map;

import org.apache.openejb.core.BaseSessionContext;
import org.apache.openejb.core.Operation;
import org.apache.openejb.core.ThreadContext;
import org.apache.openejb.spi.SecurityService;

/**
 * @version $Rev$ $Date$
 */
public class SingletonContext extends BaseSessionContext {
    protected final static State[] states = new State[Operation.values().length];

    public static State[] getStates() {
        return states;
    }

    public SingletonContext(SecurityService securityService) {
        super(securityService);
    }

    protected State getState() {
        Operation operation = ThreadContext.getThreadContext().getCurrentOperation();
        State state = states[operation.ordinal()];

        if (state == null) throw new IllegalArgumentException("Invalid operation " + operation + " for this context");

        return state;
    }

    /**
     * Business method from web service endpoint
     */
    private static class BusinessWsStatelessState extends SessionState {
        public Class getInvokedBusinessInterface() {
            throw new IllegalStateException();
        }
    }

    static {
        states[Operation.INJECTION.ordinal()] = new InjectionSessionState();
        states[Operation.CREATE.ordinal()] = new LifecycleSessionState();
        states[Operation.BUSINESS.ordinal()] = new BusinessSessionState();
        states[Operation.BUSINESS_WS.ordinal()] = new BusinessWsStatelessState();
        states[Operation.TIMEOUT.ordinal()] = new TimeoutSessionState();
        states[Operation.POST_CONSTRUCT.ordinal()] = new PostConstructSessionState();
        states[Operation.PRE_DESTROY.ordinal()] = new LifecycleSessionState();
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
