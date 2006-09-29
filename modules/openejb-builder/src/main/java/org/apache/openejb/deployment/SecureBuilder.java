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

package org.apache.openejb.deployment;

import javax.security.auth.Subject;

import org.apache.geronimo.naming.deployment.ResourceEnvironmentBuilder;
import org.apache.geronimo.security.deploy.DefaultPrincipal;


/**
 * An interface for secure inteceptor builders.
 *
 * @version $Revision$ $Date$
 */
public interface SecureBuilder extends ResourceEnvironmentBuilder {
    DefaultPrincipal getDefaultPrincipal();

    void setDefaultPrincipal(DefaultPrincipal defaultSubject);

    Subject getRunAs();

    void setRunAs(Subject runAs);

    boolean isSecurityEnabled();

    void setSecurityEnabled(boolean securityEnabled);

    void setPolicyContextId(String policyContextId);
}
