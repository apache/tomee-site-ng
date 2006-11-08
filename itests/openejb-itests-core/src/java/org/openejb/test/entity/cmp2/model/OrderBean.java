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
package org.openejb.test.entity.cmp2.model;

import java.util.Set;

import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;


/**
 * @version $Revision$ $Date$
 */
public abstract class OrderBean implements EntityBean {

    // CMP
    public abstract Integer getId();
    public abstract void setId(Integer primaryKey);

    public abstract String getReference();
    public abstract void setReference(String reference);
    
    // CMR
    public abstract AddressLocal getShippingAddress();
    public abstract void setShippingAddress(AddressLocal address);

    public abstract AddressLocal getBillingAddress();
    public abstract void setBillingAddress(AddressLocal address);

    public abstract Set getLineItems();
    public abstract void setLineItems(Set lineItems);
    
    public Integer ejbCreate(Integer id, String reference) throws CreateException {
        setId(id);
        setReference(reference);
        return null;
    }

    public void ejbPostCreate(Integer id, String reference) {
    }
    
    public void ejbLoad() {
    }

    public void setEntityContext(EntityContext ctx) {
    }

    public void unsetEntityContext() {
    }

    public void ejbStore() {
    }

    public void ejbRemove() {
    }

    public void ejbActivate() {
    }

    public void ejbPassivate() {
    }
}
