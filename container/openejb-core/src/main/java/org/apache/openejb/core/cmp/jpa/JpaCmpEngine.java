/**
 *
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
package org.apache.openejb.core.cmp.jpa;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Method;
import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.EJBObject;
import javax.ejb.EntityBean;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.ejb.EJBLocalObject;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.transaction.TransactionManager;
import javax.transaction.Status;
import javax.transaction.TransactionSynchronizationRegistry;

import org.apache.openejb.OpenEJBException;
import org.apache.openejb.loader.SystemInstance;
import org.apache.openejb.core.CoreDeploymentInfo;
import org.apache.openejb.core.ThreadContext;
import org.apache.openejb.core.cmp.CmpCallback;
import org.apache.openejb.core.cmp.CmpEngine;
import org.apache.openejb.core.cmp.ComplexKeyGenerator;
import org.apache.openejb.core.cmp.KeyGenerator;
import org.apache.openejb.core.cmp.SimpleKeyGenerator;
import org.apache.openejb.core.cmp.cmp2.Cmp2KeyGenerator;
import org.apache.openejb.core.cmp.cmp2.Cmp2Util;
import org.apache.openjpa.event.AbstractLifecycleListener;
import org.apache.openjpa.event.LifecycleEvent;
import org.apache.openjpa.persistence.OpenJPAEntityManagerSPI;

public class JpaCmpEngine implements CmpEngine {
    private static final Object[] NO_ARGS = new Object[0];
    public static final String CMP_PERSISTENCE_CONTEXT_REF_NAME = "openejb/cmp";

    private final CmpCallback cmpCallback;
    private final TransactionManager transactionManager;
    private final TransactionSynchronizationRegistry synchronizationRegistry = SystemInstance.get().getComponent(TransactionSynchronizationRegistry.class);
    private final WeakHashMap<EntityManager,Object> entityManagerListeners = new WeakHashMap<EntityManager,Object>();

    private final Map<Object, CoreDeploymentInfo> deployments = new HashMap<Object, CoreDeploymentInfo>();
    private final ThreadLocal<Set<EntityBean>> creating = new ThreadLocal<Set<EntityBean>>() {
        protected Set<EntityBean> initialValue() {
            return new HashSet<EntityBean>();
        }
    };

    public JpaCmpEngine(CmpCallback cmpCallback, TransactionManager transactionManager) {
        this.cmpCallback = cmpCallback;
        this.transactionManager = transactionManager;
    }

    public void deploy(CoreDeploymentInfo deploymentInfo) throws OpenEJBException {
        deployments.put(deploymentInfo.getDeploymentID(), deploymentInfo);
        if (deploymentInfo.getCmpImplClass() == null) {
            throw new OpenEJBException("Deployment info does not define a CMP implementation class " + deploymentInfo.getDeploymentID());
        }
        configureKeyGenerator(deploymentInfo);
    }

    public void undeploy(CoreDeploymentInfo deploymentInfo) throws OpenEJBException {
        deploymentInfo.setKeyGenerator(null);
        deployments.remove(deploymentInfo.getDeploymentID());
        if (deployments.size() == 0){
            entityManagerListeners.clear();
        }
    }

    public boolean isEmpty() {
        return deployments.size() == 0;
    }

    private EntityManager getEntityManager(CoreDeploymentInfo deploymentInfo) {
        EntityManager entityManager = null;
        try {
            entityManager = (EntityManager) deploymentInfo.getJndiEnc().lookup("java:comp/env/" + CMP_PERSISTENCE_CONTEXT_REF_NAME);
        } catch (NamingException ignroed) {
        }

        if (entityManager == null) {
            throw new EJBException("Entity manager not found at \"openejb/cmp\" in jndi ejb " + deploymentInfo.getDeploymentID());
        }

        registerListener(entityManager);

        return entityManager;
    }

    private synchronized void registerListener(EntityManager entityManager) {
        // check if listener is already registered
        if (entityManagerListeners.containsKey(entityManager)) {
            return;
        }

        if (entityManager instanceof OpenJPAEntityManagerSPI) {
            OpenJPAEntityManagerSPI openjpaEM = (OpenJPAEntityManagerSPI) entityManager;
            OpenJPALifecycleListener listener = new OpenJPALifecycleListener();
            openjpaEM.addLifecycleListener(listener, (Class[])null);
            entityManagerListeners.put(entityManager,  listener);
            return;
        }

        Object delegate = entityManager.getDelegate();
        if (delegate != entityManager && delegate instanceof EntityManager) {
            registerListener((EntityManager) delegate);
        }
    }

    public Object createBean(EntityBean bean, ThreadContext callContext) throws CreateException {
        // TODO verify that extract primary key requires a flush followed by a merge
        boolean startedTx = startTransaction("persist");
        creating.get().add(bean);
        try {
            CoreDeploymentInfo deploymentInfo = callContext.getDeploymentInfo();
            EntityManager entityManager = getEntityManager(deploymentInfo);

            entityManager.persist(bean);
            entityManager.flush();
            bean = entityManager.merge(bean);

            // extract the primary key from the bean
            KeyGenerator kg = deploymentInfo.getKeyGenerator();
            Object primaryKey = kg.getPrimaryKey(bean);

            // add to transaction cache
            getTransactionCache().put(deploymentInfo.getCmpImplClass(), primaryKey, bean);

            return primaryKey;
        } finally {
            creating.get().remove(bean);
            commitTransaction(startedTx, "persist");
        }
    }

    public Object loadBean(ThreadContext callContext, Object primaryKey) {
        boolean startedTx = startTransaction("load");
        try {
            CoreDeploymentInfo deploymentInfo = callContext.getDeploymentInfo();
            Class<?> beanClass = deploymentInfo.getCmpImplClass();

            // First check the transaction cache
            Object bean = getTransactionCache().get(beanClass, primaryKey);
            if (bean == null) {
                // Try to load it from the entity manager
                EntityManager entityManager = getEntityManager(deploymentInfo);
                bean = entityManager.find(beanClass, primaryKey);
            }
            return bean;
        } finally {
            commitTransaction(startedTx, "load");
        }
    }

    public void storeBeanIfNoTx(ThreadContext callContext, Object bean) {
        boolean startedTx = startTransaction("store");
        if (startedTx) {
            CoreDeploymentInfo deploymentInfo = callContext.getDeploymentInfo();

            try {
                EntityManager entityManager = getEntityManager(deploymentInfo);
                entityManager.merge(bean);
            } finally {
                commitTransaction(startedTx, "store");
            }
        }
    }

    public void removeBean(ThreadContext callContext) {
        boolean startedTx = startTransaction("remove");
        try {
            CoreDeploymentInfo deploymentInfo = callContext.getDeploymentInfo();
            Class<?> beanClass = deploymentInfo.getCmpImplClass();

            EntityManager entityManager = getEntityManager(deploymentInfo);
            Object primaryKey = callContext.getPrimaryKey();

            // First check the transaction cache
            Object bean = getTransactionCache().get(beanClass, primaryKey);
            if (bean == null) {
                // Try to load it from the entity manager
                bean = entityManager.find(beanClass, primaryKey);
            }

            // remove the bean
            entityManager.remove(bean);
            getTransactionCache().remove(beanClass, primaryKey);
        } finally {
            commitTransaction(startedTx, "remove");
        }
    }

    public List<Object> queryBeans(ThreadContext callContext, Method queryMethod, Object[] args) throws FinderException {
        CoreDeploymentInfo deploymentInfo = callContext.getDeploymentInfo();
        EntityManager entityManager = getEntityManager(deploymentInfo);

        StringBuilder queryName = new StringBuilder();
        queryName.append(deploymentInfo.getAbstractSchemaName()).append(".").append(queryMethod.getName());
        String shortName = queryName.toString();
        if (queryMethod.getParameterTypes().length > 0) {
            queryName.append('(');
            boolean first = true;
            for (Class<?> parameterType : queryMethod.getParameterTypes()) {
                if (!first) queryName.append(',');
                queryName.append(parameterType.getCanonicalName());
                first = false;
            }
            queryName.append(')');

        }

        String fullName = queryName.toString();
        Query query = createNamedQuery(entityManager, fullName);
        if (query == null) {
            query = createNamedQuery(entityManager, shortName);
            if (query == null) {
                throw new FinderException("No query defined for method " + fullName);
            }
        }
        return executeQuery(query, args);
    }

    public List<Object> queryBeans(CoreDeploymentInfo deploymentInfo, String signature, Object[] args) throws FinderException {
        EntityManager entityManager = getEntityManager(deploymentInfo);

        Query query = createNamedQuery(entityManager, signature);
        if (query == null) {
            int parenIndex = signature.indexOf('(');
            if (parenIndex > 0) {
                String shortName = signature.substring(0, parenIndex);
                query = createNamedQuery(entityManager, shortName);
            }
            if (query == null) {
                throw new FinderException("No query defined for method " + signature);
            }
        }
        return executeQuery(query, args);
    }

    private List<Object> executeQuery(Query query, Object[] args) {
        // process args
        if (args == null) {
            args = NO_ARGS;
        }
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            // ejb proxies need to be swapped out for real instance classes
            if (arg instanceof EJBObject) {
                arg = Cmp2Util.getEntityBean(((EJBObject) arg));
            }
            if (arg instanceof EJBLocalObject) {
                arg = Cmp2Util.getEntityBean(((EJBLocalObject) arg));
            }
            query.setParameter(i + 1, arg);
        }

        // todo results should not be iterated over, but should instead
        // perform all work in a wrapper list on demand by the application code
        List results = query.getResultList();
        for (Object value : results) {
            if (value instanceof EntityBean) {
                // todo don't activate beans already activated
                EntityBean entity = (EntityBean) value;
                cmpCallback.setEntityContext(entity);
                cmpCallback.ejbActivate(entity);
            }
        }
        //noinspection unchecked
        return results;
    }

    private Query createNamedQuery(EntityManager entityManager, String name) {
        try {
            return entityManager.createNamedQuery(name);
        } catch (IllegalArgumentException ignored) {
            // soooo lame that jpa throws an exception instead of returning null....
            ignored.printStackTrace();
            return null;
        }
    }

    private boolean startTransaction(String operation) {
        try {
            if (Status.STATUS_NO_TRANSACTION == transactionManager.getStatus()) {
                transactionManager.begin();
                return true;
            }
            return false;
        } catch (Exception e) {
            throw new EJBException("Unable to start transaction for " + operation + " operation", e);
        }
    }

    private void commitTransaction(boolean startedTx, String operation) {
        try {
            if (startedTx) {
                transactionManager.commit();
            }
        } catch (Exception e) {
            throw new EJBException("Unable to complete transaction for " + operation + " operation", e);
        }
    }

    private void configureKeyGenerator(CoreDeploymentInfo di) throws OpenEJBException {
        if (di.isCmp2()) {
            di.setKeyGenerator(new Cmp2KeyGenerator());
        } else {
            String primaryKeyField = di.getPrimaryKeyField();
            Class cmpBeanImpl = di.getCmpImplClass();
            if (primaryKeyField != null) {
                di.setKeyGenerator(new SimpleKeyGenerator(cmpBeanImpl, primaryKeyField));
            } else if (Object.class.equals(di.getPrimaryKeyClass())) {
                di.setKeyGenerator(new SimpleKeyGenerator(cmpBeanImpl, "OpenEJB_pk"));
            } else {
                di.setKeyGenerator(new ComplexKeyGenerator(cmpBeanImpl, di.getPrimaryKeyClass()));
            }
        }
    }

    // todo remove when OpenJPA fixes the new-remove-new-find bug
    private TransactionCache getTransactionCache() {
        TransactionCache transactionCache = (TransactionCache) synchronizationRegistry.getResource(TransactionCache.class);
        if (transactionCache == null) {
            transactionCache = new TransactionCache();
            synchronizationRegistry.putResource(TransactionCache.class, transactionCache);
        }
        return transactionCache;
    }

    private static class TransactionCache {
        private final Map<Class,Map<Object,Object>> cache = new HashMap<Class,Map<Object,Object>>();

        public Object get(Class clazz, Object primaryKey) {
            Map<Object, Object> instances = cache.get(clazz);
            if (instances == null) return null;
            return instances.get(primaryKey);
        }

        public void put(Class clazz, Object primaryKey, Object value) {
            Map<Object, Object> instances = cache.get(clazz);
            if (instances == null) {
                instances = new HashMap<Object, Object>();
                cache.put(clazz, instances);
            }
            instances.put(primaryKey, value);
        }

        public Object remove(Class clazz, Object primaryKey) {
            Map<Object, Object> instances = cache.get(clazz);
            if (instances == null) return null;
            return instances.remove(primaryKey);
        }
    }

    private class OpenJPALifecycleListener extends AbstractLifecycleListener {
//        protected void eventOccurred(LifecycleEvent event) {
//            int type = event.getType();
//            switch (type) {
//                case LifecycleEvent.BEFORE_PERSIST:
//                    System.out.println("BEFORE_PERSIST");
//                    break;
//                case LifecycleEvent.AFTER_PERSIST:
//                    System.out.println("AFTER_PERSIST");
//                    break;
//                case LifecycleEvent.AFTER_LOAD:
//                    System.out.println("AFTER_LOAD");
//                    break;
//                case LifecycleEvent.BEFORE_STORE:
//                    System.out.println("BEFORE_STORE");
//                    break;
//                case LifecycleEvent.AFTER_STORE:
//                    System.out.println("AFTER_STORE");
//                    break;
//                case LifecycleEvent.BEFORE_CLEAR:
//                    System.out.println("BEFORE_CLEAR");
//                    break;
//                case LifecycleEvent.AFTER_CLEAR:
//                    System.out.println("AFTER_CLEAR");
//                    break;
//                case LifecycleEvent.BEFORE_DELETE:
//                    System.out.println("BEFORE_DELETE");
//                    break;
//                case LifecycleEvent.AFTER_DELETE:
//                    System.out.println("AFTER_DELETE");
//                    break;
//                case LifecycleEvent.BEFORE_DIRTY:
//                    System.out.println("BEFORE_DIRTY");
//                    break;
//                case LifecycleEvent.AFTER_DIRTY:
//                    System.out.println("AFTER_DIRTY");
//                    break;
//                case LifecycleEvent.BEFORE_DIRTY_FLUSHED:
//                    System.out.println("BEFORE_DIRTY_FLUSHED");
//                    break;
//                case LifecycleEvent.AFTER_DIRTY_FLUSHED:
//                    System.out.println("AFTER_DIRTY_FLUSHED");
//                    break;
//                case LifecycleEvent.BEFORE_DETACH:
//                    System.out.println("BEFORE_DETACH");
//                    break;
//                case LifecycleEvent.AFTER_DETACH:
//                    System.out.println("AFTER_DETACH");
//                    break;
//                case LifecycleEvent.BEFORE_ATTACH:
//                    System.out.println("BEFORE_ATTACH");
//                    break;
//                case LifecycleEvent.AFTER_ATTACH:
//                    System.out.println("AFTER_ATTACH");
//                    break;
//                case LifecycleEvent.AFTER_REFRESH:
//                    System.out.println("AFTER_REFRESH");
//                    break;
//                default:
//                    System.out.println("default");
//                    break;
//            }
//            super.eventOccurred(event);
//        }

        public void afterLoad(LifecycleEvent lifecycleEvent) {
            eventOccurred(lifecycleEvent);
            Object bean = lifecycleEvent.getSource();
            // This may seem a bit strange to call ejbActivate immedately followed by ejbLoad,
            // but it is completely legal.  Since the ejbActivate method is not allowed to access
            // persistent state of the bean (EJB 3.0fr 8.5.2) there should be no concern that the
            // call back method clears the bean state before ejbLoad is called.
            cmpCallback.setEntityContext((EntityBean) bean);
            cmpCallback.ejbActivate((EntityBean) bean);
            cmpCallback.ejbLoad((EntityBean) bean);
        }

        public void beforeStore(LifecycleEvent lifecycleEvent) {
            eventOccurred(lifecycleEvent);
            EntityBean bean = (EntityBean) lifecycleEvent.getSource();
            if (!creating.get().contains(bean)) {
                cmpCallback.ejbStore(bean);
            }
        }

        public void afterAttach(LifecycleEvent lifecycleEvent) {
            eventOccurred(lifecycleEvent);
            Object bean = lifecycleEvent.getSource();
            cmpCallback.setEntityContext((EntityBean) bean);
        }

        public void beforeDelete(LifecycleEvent lifecycleEvent) {
            eventOccurred(lifecycleEvent);
            try {
                Object bean = lifecycleEvent.getSource();
                cmpCallback.ejbRemove((EntityBean) bean);
            } catch (RemoveException e) {
                throw new PersistenceException(e);
            }
        }

        public void afterDetach(LifecycleEvent lifecycleEvent) {
            eventOccurred(lifecycleEvent);
            // todo detach is called after ejbRemove which does not need ejbPassivate
            Object bean = lifecycleEvent.getSource();
            cmpCallback.ejbPassivate((EntityBean) bean);
            cmpCallback.unsetEntityContext((EntityBean) bean);
        }

        public void beforePersist(LifecycleEvent lifecycleEvent) {
            eventOccurred(lifecycleEvent);
        }

        public void afterRefresh(LifecycleEvent lifecycleEvent) {
            eventOccurred(lifecycleEvent);
        }

        public void beforeDetach(LifecycleEvent lifecycleEvent) {
            eventOccurred(lifecycleEvent);
        }

        public void beforeAttach(LifecycleEvent lifecycleEvent) {
            eventOccurred(lifecycleEvent);
        }
    }
}
