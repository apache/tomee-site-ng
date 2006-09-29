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
package org.apache.openejb.dispatch;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.apache.geronimo.kernel.ClassLoading;

/**
 *
 *
 * @version $Revision$ $Date$
 */
public final class MethodSignature implements Serializable, Comparable {
    private static final String[] NOARGS = {};
    private final String methodName;
    private final String[] parameterTypes;

    public MethodSignature(Method method) {
        methodName = method.getName();
        Class[] params = method.getParameterTypes();
        parameterTypes = new String[params.length];
        for (int i = 0; i < params.length; i++) {
            parameterTypes[i] = params[i].getName();
        }
    }

    public MethodSignature(String methodName) {
        this.methodName = methodName;
        parameterTypes = NOARGS;
    }

    public MethodSignature(String methodName, String[] parameterTypes) {
        this.methodName = methodName;
        this.parameterTypes = parameterTypes != null ? parameterTypes : NOARGS;
    }

    public MethodSignature(String methodName, Class[] params) {
        this.methodName = methodName;
        parameterTypes = new String[params.length];
        for (int i = 0; i < params.length; i++) {
            parameterTypes[i] = params[i].getName();
        }
    }

    public String getMethodName() {
        return methodName;
    }

    public String[] getParameterTypes() {
        return parameterTypes;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(methodName).append('(');
        if (parameterTypes != null) {
            for (int i = 0; i < parameterTypes.length; i++) {
                String parameterType = parameterTypes[i];
                if (i > 0) {
                    buffer.append(',');
                }
                buffer.append(parameterType);
            }
        }
        buffer.append(')');
        return buffer.toString();
    }

    public boolean match(Method method) {
        if(!methodName.equals(method.getName())) {
            return false;
        }
        Class[] types = method.getParameterTypes();
        if (types.length != parameterTypes.length) {
            return false;
        }
        for (int i = 0; i < parameterTypes.length; i++) {
            if(!types[i].getName().equals(parameterTypes[i])) {
                return false;
            }
        }
        return true;
    }

    public Method getMethod(Class clazz) {
        try {
            ClassLoader classLoader = clazz.getClassLoader();
            Class[] args = new Class[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                args[i] = ClassLoading.loadClass(parameterTypes[i], classLoader);
            }
            return clazz.getMethod(methodName, args);
        } catch (Exception e) {
            return null;
        }
    }

    public int hashCode() {
        return toString().hashCode();
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof MethodSignature == false) {
            return false;
        }
        MethodSignature other = (MethodSignature) obj;
        return methodName.equals(other.methodName) && Arrays.equals(parameterTypes, other.parameterTypes);
    }

    public int compareTo(Object object) {
        MethodSignature methodSignature = (MethodSignature) object;

        // alphabetic compare of method names
        int value = methodName.compareTo(methodSignature.methodName);
        if (value != 0) {
            return value;
        }

        // shorter parameter list comes before longer parameter lists
        if (parameterTypes.length < methodSignature.parameterTypes.length) {
            return -1;
        }
        if (parameterTypes.length > methodSignature.parameterTypes.length) {
            return 1;
        }

        // alphabetic compare of each parameter type
        for (int i = 0; i < parameterTypes.length; i++) {
            value = parameterTypes[i].compareTo(methodSignature.parameterTypes[i]);
            if (value != 0) {
                return value;
            }
        }

        // they are the same
        return 0;
    }
}
