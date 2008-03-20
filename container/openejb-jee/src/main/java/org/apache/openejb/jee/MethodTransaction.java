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
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.openejb.jee;

/**
 * @version $Rev$ $Date$
 */
public class MethodTransaction {
    private final Method method;
    private final TransAttribute attribute;

    public MethodTransaction(TransAttribute attribute, Method method) {
        this.attribute = attribute;
        this.method = method;
    }

    public TransAttribute getAttribute() {
        return attribute;
    }

    public String getEjbName() {
        return method.getEjbName();
    }

    public MethodIntf getMethodIntf() {
        return method.getMethodIntf();
    }

    public String getMethodName() {
        return method.getMethodName();
    }

    public MethodParams getMethodParams() {
        return method.getMethodParams();
    }

    public Method getMethod() {
        return method;
    }

    public String getClassName() {
        return method.getClassName();
    }
}
