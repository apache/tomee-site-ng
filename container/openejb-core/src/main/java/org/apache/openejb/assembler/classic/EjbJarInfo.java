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
package org.apache.openejb.assembler.classic;

import java.util.List;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

public class EjbJarInfo extends ValidationInfoObject {
    public final Properties properties = new Properties();

    public String moduleId;
    public String jarPath;
    public final List<EnterpriseBeanInfo> enterpriseBeans = new ArrayList<EnterpriseBeanInfo>();

    public final List<SecurityRoleInfo> securityRoles = new ArrayList<SecurityRoleInfo>();
    public final List<MethodPermissionInfo> methodPermissions= new ArrayList<MethodPermissionInfo>();
    public final List<MethodTransactionInfo> methodTransactions = new ArrayList<MethodTransactionInfo>();
    public final List<MethodConcurrencyInfo> methodConcurrency = new ArrayList<MethodConcurrencyInfo>();
    public final List<MethodScheduleInfo> methodSchedules = new ArrayList<MethodScheduleInfo>();
    public final List<InterceptorInfo> interceptors = new ArrayList<InterceptorInfo>();
    public final List<InterceptorBindingInfo> interceptorBindings = new ArrayList<InterceptorBindingInfo>();
    public final List<MethodInfo> excludeList = new ArrayList<MethodInfo>();
    public final List<ApplicationExceptionInfo> applicationException = new ArrayList<ApplicationExceptionInfo>();
    public final List<PortInfo> portInfos = new ArrayList<PortInfo>();
    public final Set<String> watchedResources = new TreeSet<String>();
}
