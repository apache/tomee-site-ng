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

import org.apache.geronimo.interceptor.Interceptor;
import org.apache.geronimo.interceptor.Invocation;
import org.apache.geronimo.interceptor.InvocationResult;
import org.apache.openejb.CallbackMethod;
import org.apache.openejb.EjbCallbackInvocation;
import org.apache.openejb.EjbInvocation;
import org.apache.openejb.EJBOperation;
import org.apache.openejb.dispatch.AbstractCallbackOperation;
import org.apache.openejb.slsb.EjbCreateMethod;
import org.apache.openejb.slsb.RemoveMethod;

import javax.ejb.EnterpriseBean;
import javax.ejb.MessageDrivenBean;
import javax.ejb.MessageDrivenContext;

/**
 * @version $Revision$ $Date$
 */
public class MdbCallbackInterceptor implements Interceptor {
    private static final SetMessageDrivenContextOperation SET_MESSAGE_DRIVEN_CONTEXT = new SetMessageDrivenContextOperation();

    public InvocationResult invoke(Invocation invocation) throws Throwable {
        EjbCallbackInvocation ejbCallbackInvocation = (EjbCallbackInvocation) invocation;

        CallbackMethod callbackMethod = ejbCallbackInvocation.getCallbackMethod();
        if (callbackMethod == CallbackMethod.SET_CONTEXT) {
            InvocationResult result = SET_MESSAGE_DRIVEN_CONTEXT.execute(ejbCallbackInvocation);
            return result;
        } else if (callbackMethod == CallbackMethod.CREATE) {
            InvocationResult result = EjbCreateMethod.INSTANCE.execute(ejbCallbackInvocation);
            return result;
        } else if (callbackMethod == CallbackMethod.REMOVE) {
            InvocationResult result = RemoveMethod.INSTANCE.execute(ejbCallbackInvocation);
            return result;
        } else {
            throw new AssertionError("Unknown callback method " + callbackMethod);
        }
    }

    private static final class SetMessageDrivenContextOperation extends AbstractCallbackOperation {

        public InvocationResult execute(EjbInvocation invocation) throws Throwable {
            return invoke(invocation, EJBOperation.SETCONTEXT);
        }

        protected Object doOperation(EnterpriseBean instance, Object[] arguments) throws Throwable {
            ((MessageDrivenBean)instance).setMessageDrivenContext((MessageDrivenContext)arguments[0]);
            return null;
        }
    }

}
