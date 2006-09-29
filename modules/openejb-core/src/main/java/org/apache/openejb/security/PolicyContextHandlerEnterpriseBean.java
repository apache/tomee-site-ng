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
package org.apache.openejb.security;

import javax.security.jacc.PolicyContextHandler;
import javax.security.jacc.PolicyContextException;


/**
 *
 * @version $Revision$ $Date$
 * @see "JSR 115" section 4.6
 */
public class PolicyContextHandlerEnterpriseBean implements PolicyContextHandler {
    public static final String HANDLER_KEY = "javax.ejb.EnterpriseBean";

    public boolean supports(String key) throws PolicyContextException {
        return HANDLER_KEY.equals(key);
    }

    public String[] getKeys() throws PolicyContextException {
        return new String[]{HANDLER_KEY};
    }

    public Object getContext(String key, Object d) throws PolicyContextException {
        if (HANDLER_KEY.equals(key) && d != null) {
            PolicyContextHandlerDataEJB data = (PolicyContextHandlerDataEJB) d;

            return data.bean;
        }

        return null;
    }
}
