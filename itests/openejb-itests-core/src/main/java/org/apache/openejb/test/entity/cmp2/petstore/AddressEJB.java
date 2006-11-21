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
package org.apache.openejb.test.entity.cmp2.petstore;

import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import javax.ejb.RemoveException;
import javax.ejb.CreateException;
import javax.naming.NamingException;
import javax.naming.InitialContext;

/**
 * @version $Revision$ $Date$
 */
public abstract class AddressEJB implements EntityBean {

    private EntityContext context = null;

    public abstract String getStreet();

    public abstract void setStreet(String street);

    public abstract String getCity();

    public abstract void setCity(String city);

    public Object ejbCreate() throws CreateException {
        return null;
    }

    public void ejbPostCreate() throws CreateException {
    }

    public void setEntityContext(EntityContext c) {
        context = c;
    }

    public void unsetEntityContext() {
        context = null;
    }

    public void ejbRemove() throws RemoveException {
    }

    public void ejbActivate() {
    }

    public void ejbPassivate() {
    }

    public void ejbStore() {
    }

    public void ejbLoad() {
    }
}
