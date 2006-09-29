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
package org.openejb.test.entity.cmp2.prefetch;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.ejb.SessionContext;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

import org.openejb.test.TestFailureException;
import org.openejb.test.entity.cmp2.model.AddressLocal;
import org.openejb.test.entity.cmp2.model.AddressLocalHome;
import org.openejb.test.entity.cmp2.model.LineItemLocal;
import org.openejb.test.entity.cmp2.model.LineItemLocalHome;
import org.openejb.test.entity.cmp2.model.OrderLocal;
import org.openejb.test.entity.cmp2.model.OrderLocalHome;
import org.openejb.test.entity.cmp2.model.ProductLocal;
import org.openejb.test.entity.cmp2.model.ProductLocalHome;

/**
 * @version $Revision$ $Date$
 */
public class PrefetchFacadeBean implements javax.ejb.SessionBean {
    private InitialContext jndiContext;
    private SessionContext ctx;

    public void testDoesNotOverwriteUpdates() throws TestFailureException {
        TestAction action = new TestAction() {
            public void executeTest(UserTransaction userTransaction) throws Exception {
                userTransaction.begin();
                try {
                    OrderLocalHome home = (OrderLocalHome) javax.rmi.PortableRemoteObject.narrow(jndiContext.lookup("java:comp/env/ejb/Order"), OrderLocalHome.class);
                    Integer orderId = new Integer(1);
                    OrderLocal order = home.findByPrimaryKey(orderId);
                    order.setReference("new Reference");
                    order.setBillingAddress(null);
                    order.setShippingAddress(null);
                    order.setLineItems(new HashSet());
                    
                    order = home.findPrefetchAll(orderId);
                    Assert.assertEquals("new Reference", order.getReference());
                    Assert.assertNull(order.getBillingAddress());
                    Assert.assertNull(order.getShippingAddress());
                    Assert.assertEquals(0, order.getLineItems().size());
                } finally {
                    userTransaction.commit();
                }
            }
        };
        
        TestTemplate template = new TestTemplate(action);
        template.execute();
    }

    public void testFinderPrefetch() throws TestFailureException {
        TestAction action = new TestAction() {
            public void executeTest(UserTransaction userTransaction) throws Exception {
                userTransaction.begin();
                try {
                    OrderLocalHome home = (OrderLocalHome) javax.rmi.PortableRemoteObject.narrow(jndiContext.lookup("java:comp/env/ejb/Order"), OrderLocalHome.class);
                    Integer orderId = new Integer(1);
                    OrderLocal order = home.findPrefetchAll(orderId);
                    
                    validatePrefetch(order);
                } finally {
                    userTransaction.commit();
                }
            }
        };
        
        TestTemplate template = new TestTemplate(action);
        template.execute();
    }

    public void testEJBPrefetch() throws TestFailureException {
        TestAction action = new TestAction() {
            public void executeTest(UserTransaction userTransaction) throws Exception {
                userTransaction.begin();
                try {
                    LineItemLocalHome home = (LineItemLocalHome) javax.rmi.PortableRemoteObject.narrow(jndiContext.lookup("java:comp/env/ejb/LineItem"), LineItemLocalHome.class);
                    Integer orderId = new Integer(1);
                    LineItemLocal lineItem = home.findByPrimaryKey(orderId);
                    Assert.assertEquals(10, lineItem.getQuantity());
                    
                    DataSource ds = (DataSource) jndiContext.lookup("java:comp/env/jdbc/CMPDatasource");
                    Connection con = ds.getConnection();
                    Statement statement = con.createStatement();
                    int nbUpdates = statement.executeUpdate("UPDATE product SET name = ''");
                    Assert.assertEquals(1, nbUpdates);
                    
                    ProductLocal product = lineItem.getProduct();
                    Assert.assertEquals("product_name1", product.getName());
                    Assert.assertEquals("product_type1", product.getProductType());
                } finally {
                    userTransaction.commit();
                }
            }
        };
        
        TestTemplate template = new TestTemplate(action);
        template.execute();
    }

    public void testCMPPrefetch() throws TestFailureException {
        TestAction action = new TestAction() {
            public void executeTest(UserTransaction userTransaction) throws Exception {
                userTransaction.begin();
                try {
                    OrderLocalHome home = (OrderLocalHome) javax.rmi.PortableRemoteObject.narrow(jndiContext.lookup("java:comp/env/ejb/Order"), OrderLocalHome.class);
                    Integer orderId = new Integer(1);
                    OrderLocal order = home.findByPrimaryKey(orderId);
                    Assert.assertEquals("order1", order.getReference());
                    
                    validatePrefetch(order);
                } finally {
                    userTransaction.commit();
                }
            }
        };
        
        TestTemplate template = new TestTemplate(action);
        template.execute();
    }

    public void testCMRPrefetch() throws TestFailureException {
        TestAction action = new TestAction() {
            public void executeTest(UserTransaction userTransaction) throws Exception {
                userTransaction.begin();
                try {
                    OrderLocalHome home = (OrderLocalHome) javax.rmi.PortableRemoteObject.narrow(jndiContext.lookup("java:comp/env/ejb/Order"), OrderLocalHome.class);
                    Integer orderId = new Integer(1);
                    OrderLocal order = home.findByPrimaryKey(orderId);
                    order.getBillingAddress();
                    
                    validatePrefetch(order);
                } finally {
                    userTransaction.commit();
                }
            }
        };
        
        TestTemplate template = new TestTemplate(action);
        template.execute();
    }

    public void ejbCreate() throws javax.ejb.CreateException{
        try {
            jndiContext = new InitialContext();
        } catch (Exception e){
            throw new CreateException("Can not get the initial context: "+e.getMessage());
        }
    }

    public void setSessionContext(SessionContext ctx) throws EJBException,RemoteException {
        this.ctx = ctx;
    }

    public void ejbRemove() throws EJBException,RemoteException {
    }

    public void ejbActivate() throws EJBException,RemoteException {
    }

    public void ejbPassivate() throws EJBException,RemoteException {
    }
    
    private void setUp() throws AssertionFailedError {
        try {
            ProductLocalHome productHome = (ProductLocalHome) javax.rmi.PortableRemoteObject.narrow(jndiContext.lookup("java:comp/env/ejb/Product"), ProductLocalHome.class);
            ProductLocal product1 = productHome.create(new Integer(1), "product_name1", "product_type1");
            
            LineItemLocalHome lineItemHome = (LineItemLocalHome) javax.rmi.PortableRemoteObject.narrow(jndiContext.lookup("java:comp/env/ejb/LineItem"), LineItemLocalHome.class);
            LineItemLocal lineItem1 = lineItemHome.create(new Integer(1), 10);
            LineItemLocal lineItem2 = lineItemHome.create(new Integer(2), 20);
            
            OrderLocalHome orderHome = (OrderLocalHome) javax.rmi.PortableRemoteObject.narrow(jndiContext.lookup("java:comp/env/ejb/Order"), OrderLocalHome.class);
            OrderLocal order1 = orderHome.create(new Integer(1), "order1");
            
            AddressLocalHome addressHome = (AddressLocalHome) javax.rmi.PortableRemoteObject.narrow(jndiContext.lookup("java:comp/env/ejb/Address"), AddressLocalHome.class);
            AddressLocal address1 = addressHome.create(new Integer(1), "street1", "city1");
            AddressLocal address2 = addressHome.create(new Integer(2), "street2", "city2");
            
            Set productLineItems = product1.getLineItems();
            productLineItems.add(lineItem1);
            productLineItems.add(lineItem2);
            
            Set order1LineItems = order1.getLineItems();
            order1LineItems.add(lineItem1);
            order1LineItems.add(lineItem2);
            
            order1.setBillingAddress(address1);
            order1.setShippingAddress(address2);
        } catch (Exception e) {
            throw (AssertionFailedError) new AssertionFailedError(e.getMessage()).initCause(e);
        }
    }
    
    private void cleanUp() throws AssertionFailedError {
        try {
            ProductLocalHome productHome = (ProductLocalHome) javax.rmi.PortableRemoteObject.narrow(jndiContext.lookup("java:comp/env/ejb/Product"), ProductLocalHome.class);
            productHome.findByPrimaryKey(new Integer(1)).remove();
            
            LineItemLocalHome lineItemHome = (LineItemLocalHome) javax.rmi.PortableRemoteObject.narrow(jndiContext.lookup("java:comp/env/ejb/LineItem"), LineItemLocalHome.class);
            lineItemHome.findByPrimaryKey(new Integer(1)).remove();
            lineItemHome.findByPrimaryKey(new Integer(2)).remove();
            
            OrderLocalHome orderHome = (OrderLocalHome) javax.rmi.PortableRemoteObject.narrow(jndiContext.lookup("java:comp/env/ejb/Order"), OrderLocalHome.class);
            orderHome.findByPrimaryKey(new Integer(1)).remove();
            
            AddressLocalHome addressHome = (AddressLocalHome) javax.rmi.PortableRemoteObject.narrow(jndiContext.lookup("java:comp/env/ejb/Address"), AddressLocalHome.class);
            addressHome.findByPrimaryKey(new Integer(1)).remove();
            addressHome.findByPrimaryKey(new Integer(2)).remove();
        } catch (FinderException e) {
            throw (AssertionFailedError) new AssertionFailedError(e.getMessage()).initCause(e);
        } catch (NamingException e) {
            throw (AssertionFailedError) new AssertionFailedError(e.getMessage()).initCause(e);
        } catch (RemoveException e) {
            throw (AssertionFailedError) new AssertionFailedError(e.getMessage()).initCause(e);
        }
    }
    
    private void validatePrefetch(OrderLocal order) throws NamingException, SQLException {
        DataSource ds = (DataSource) jndiContext.lookup("java:comp/env/jdbc/CMPDatasource");
        Connection con = ds.getConnection();
        Statement statement = con.createStatement();
        int nbUpdates = statement.executeUpdate("UPDATE order_table SET reference = ''");
        Assert.assertEquals(1, nbUpdates);
        nbUpdates = statement.executeUpdate("UPDATE address SET street = ''");
        Assert.assertEquals(2, nbUpdates);
        nbUpdates = statement.executeUpdate("UPDATE line_item SET quantity = 0");
        Assert.assertEquals(2, nbUpdates);
        nbUpdates = statement.executeUpdate("UPDATE product SET name = ''");
        Assert.assertEquals(1, nbUpdates);
        
        Assert.assertEquals("order1", order.getReference());
        AddressLocal address = order.getBillingAddress();
        Assert.assertEquals("street1", address.getStreet());
        Assert.assertEquals("city1", address.getCity());
        address = order.getShippingAddress();
        Assert.assertEquals("street2", address.getStreet());
        Assert.assertEquals("city2", address.getCity());
        int cpt = 0;
        Set lineItems = order.getLineItems();
        for (Iterator iter = lineItems.iterator(); iter.hasNext();) {
            LineItemLocal lineItem = (LineItemLocal) iter.next();
            int quantity = lineItem.getQuantity();
            Assert.assertTrue(10 == lineItem.getQuantity() || 20 == lineItem.getQuantity());
            ProductLocal product = lineItem.getProduct();
            Assert.assertEquals("product_name1", product.getName());
            Assert.assertEquals("product_type1", product.getProductType());
            cpt++;
        }
        Assert.assertEquals(2, cpt);
    }
    
    private interface TestAction {
        public void executeTest(UserTransaction userTransaction) throws Exception;
    }
    
    private class TestTemplate {
        private final TestAction action;
        private TestTemplate(TestAction action) {
            this.action = action;
        }
        public void execute() throws TestFailureException {
            UserTransaction userTransaction = ctx.getUserTransaction();
            try {
                userTransaction.begin();
                setUp();
                userTransaction.commit();
            } catch (Exception e) {
                Assert.fail("Received Exception " + e.getClass() + " : " + e.getMessage());
            }
            
            try {
                action.executeTest(userTransaction);
            } catch (AssertionFailedError e) {
                throw new TestFailureException(e);
            } catch (Throwable e) {
                Assert.fail("Received Exception " + e.getClass() + " : " + e.getMessage());
            } finally {
                try {
                    userTransaction.begin();
                    cleanUp();
                    userTransaction.commit();
                } catch (Exception e) {
                    Assert.fail("Received Exception " + e.getClass() + " : " + e.getMessage());
                }
            }
        }
    }
}
