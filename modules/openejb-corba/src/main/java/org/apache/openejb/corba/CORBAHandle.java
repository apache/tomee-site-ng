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
package org.apache.openejb.corba;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.rmi.RemoteException;
import javax.ejb.EJBObject;
import javax.ejb.Handle;
import javax.ejb.spi.HandleDelegate;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;

import org.omg.CORBA.ORB;


/**
 * EJB v2.1 spec, section 19.5.5.1
 * <p/>
 * The <code>javax.ejb.spi.HandleDelegate</code> service provider interface
 * defines methods that enable portable implementations of <code>Handle</code>
 * and <code>HomeHandle</code> that are instantiated in a different vendor’s
 * container to serialize and deserialize EJBObject and EJBHome references.
 * The <code>HandleDelegate</code> interface is not used by enterprise beans
 * or J2EE application components directly.
 *
 * @version $Revision$ $Date$
*/
public class CORBAHandle implements Handle, Serializable {

    private static final long serialVersionUID = -3390719015323727224L;

    private String ior;
    private Object primaryKey;
    // the remote interface this EJBObject implements
    private Class  remoteInterface; 

    public CORBAHandle(String ior, Object primaryKey, Class remoteInterface) {
        this.ior = ior;
        this.primaryKey = primaryKey;
        this.remoteInterface = remoteInterface; 
    }

    public EJBObject getEJBObject() throws RemoteException {
        try {
            return (EJBObject) PortableRemoteObject.narrow(getOrb().string_to_object(ior), remoteInterface);
        } catch (Exception e) {
            throw new RemoteException("Unable to convert IOR into object", e);
        }
    }

    public Object getPrimaryKey() {
        return primaryKey;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        HandleDelegate handleDelegate = getHandleDelegate();
        handleDelegate.writeEJBObject(getEJBObject(), out);
        out.writeObject(primaryKey);
        out.writeObject(remoteInterface); 
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        HandleDelegate handleDelegate = getHandleDelegate();
        EJBObject obj = handleDelegate.readEJBObject(in);
        primaryKey = in.readObject();
        remoteInterface = (Class)in.readObject(); 

        try {
            ior = getOrb().object_to_string((org.omg.CORBA.Object) obj);
        } catch (Exception e) {
            throw new RemoteException("Unable to convert object to IOR", e);
        }
    }

    private static ORB getOrb() {
        try {
            Context context = new InitialContext();
            ORB orb = (ORB) context.lookup("java:comp/ORB");
            return orb;
        } catch (Throwable e) {
            throw new org.omg.CORBA.MARSHAL("Could not find ORB in jndi at java:comp/ORB", 0, org.omg.CORBA.CompletionStatus.COMPLETED_YES);
        }
    }

    private static HandleDelegate getHandleDelegate() {
        try {
            Context context = new InitialContext();
            HandleDelegate handleDelegate = (HandleDelegate) context.lookup("java:comp/HandleDelegate");
            return handleDelegate;
        } catch (Throwable e) {
            throw new org.omg.CORBA.MARSHAL("Could not find handle delegate in jndi at java:comp/HandleDelegate", 0, org.omg.CORBA.CompletionStatus.COMPLETED_YES);
        }
    }
}
