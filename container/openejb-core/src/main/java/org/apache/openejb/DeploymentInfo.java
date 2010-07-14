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
package org.apache.openejb;

import org.apache.openejb.core.interceptor.InterceptorData;
import org.apache.openejb.core.timer.EjbTimerService;
import org.apache.openejb.core.ExceptionType;
import org.apache.openejb.core.transaction.TransactionType;
import org.apache.openejb.core.transaction.TransactionPolicyFactory;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.Properties;
import javax.naming.Context;
import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;

public interface DeploymentInfo {

    public BeanType getComponentType();

    public InterfaceType getInterfaceType(Class clazz);

    public TransactionType getTransactionType(Method method);

    public Container getContainer();

    public Object getDeploymentID();

    public String getEjbName();

    public String getModuleID();

    public String getRunAs();

    public boolean isBeanManagedTransaction();

    public Class getHomeInterface();

    public Class getLocalHomeInterface();

    public Class getLocalInterface();

    public Class getRemoteInterface();

    public Class getBeanClass();

    public Class getPrimaryKeyClass();

    public Class getBusinessLocalInterface();

    public Class getBusinessLocalBeanInterface();

    public Class getBusinessRemoteInterface();

    public List<Class> getBusinessLocalInterfaces();

    public List<Class> getBusinessRemoteInterfaces();

    public Class getServiceEndpointInterface();

    public String getPrimaryKeyField();

    public Context getJndiEnc();

    public boolean isReentrant();

    public Class getInterface(InterfaceType interfaceType);

    public List<Class> getInterfaces(InterfaceType interfaceType);

    public Class getMdbInterface();

    public Map<String, String> getActivationProperties();

    public ClassLoader getClassLoader();

    public List<Method> getRemoveMethods();

    public List<Injection> getInjections();

    public List<InterceptorData> getMethodInterceptors(Method method);

    public void setContainer(Container container);

    public Method getEjbTimeout();

    public EjbTimerService getEjbTimerService();

    public ExceptionType getExceptionType(Throwable e);

    EJBHome getEJBHome();

    EJBLocalHome getEJBLocalHome();

    BusinessLocalHome getBusinessLocalHome();

    BusinessLocalBeanHome getBusinessLocalBeanHome();

    BusinessLocalHome getBusinessLocalHome(List<Class> interfaces);

    BusinessRemoteHome getBusinessRemoteHome();

    BusinessRemoteHome getBusinessRemoteHome(List<Class> interfaces);

    String getDestinationId();

    boolean isDestroyed();

    boolean isBeanManagedConcurrency();

    List<Class> getObjectInterface(Class homeInterface);

    TransactionPolicyFactory getTransactionPolicyFactory();

    public interface BusinessLocalHome extends javax.ejb.EJBLocalHome {
        Object create();
    }

    public interface BusinessLocalBeanHome extends javax.ejb.EJBLocalHome {
        Object create();
    }

    public interface BusinessRemoteHome extends javax.ejb.EJBHome {
        Object create();
    }

    public interface ServiceEndpoint {
    }

    public interface Timeout {
    }

    public <T> T get(Class<T> type);

    public <T> T set(Class<T> type, T value);

    public Properties getProperties();

    public boolean retainIfExeption(Method removeMethod);

    public boolean isLoadOnStartup();

    public Set<String> getDependsOn();

    public boolean isSessionSynchronized();
}
