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
package org.openejb.test.stateful;

import java.rmi.RemoteException;
import java.util.Properties;

import org.openejb.test.ApplicationException;
import org.openejb.test.object.OperationsPolicy;

/**
 * 
 */
public interface BasicStatefulObject extends javax.ejb.EJBObject{
    /**
     * Does nothing... used to initialize the allowed operations map
     */
    public void doNothing() throws RemoteException;

    /**
     * Reverses the string passed in then returns it
     * 
     * @return 
     */
    public String businessMethod(String text) throws RemoteException;
    
    /**
     * Throws an ApplicationException when invoked
     * 
     */
    public void throwApplicationException() throws RemoteException, ApplicationException;
    
    /**
     * Throws a java.lang.NullPointerException when invoked
     * This is a system exception and should result in the 
     * destruction of the instance and invalidation of the
     * remote reference.
     * 
     */
    public void throwSystemException_NullPointer() throws RemoteException;
    
    /**
     * Returns a report of the bean's 
     * runtime permissions
     * 
     * @return 
     */
    public Properties getPermissionsReport() throws RemoteException;
    
    /**
     * Returns a report of the allowed opperations
     * for one of the bean's methods.
     * 
     * @param methodName The method for which to get the allowed opperations report
     * @return 
     */
    public OperationsPolicy getAllowedOperationsReport(String methodName) throws RemoteException;
}
