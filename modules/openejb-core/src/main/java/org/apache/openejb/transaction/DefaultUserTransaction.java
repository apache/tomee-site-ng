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
package org.apache.openejb.transaction;

import java.io.Serializable;
import javax.transaction.UserTransaction;
import javax.transaction.TransactionManager;
import javax.transaction.SystemException;
import javax.transaction.NotSupportedException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;

public final class DefaultUserTransaction implements UserTransaction, Serializable {
    private static final long serialVersionUID = -7524804683512228998L;
    private final transient TransactionManager transactionManager;

    public DefaultUserTransaction(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }


    public int getStatus() throws SystemException {
        return transactionManager.getStatus();
    }

    public void setRollbackOnly() throws IllegalStateException, SystemException {
        transactionManager.setRollbackOnly();
    }

    public void setTransactionTimeout(int seconds) throws SystemException {
        if (seconds < 0) {
            throw new SystemException("transaction timeout must be positive or 0, not " + seconds);
        }
        transactionManager.setTransactionTimeout(seconds);
    }

    public void begin() throws NotSupportedException, SystemException {
        transactionManager.begin();
    }

    public void commit() throws HeuristicMixedException, HeuristicRollbackException, IllegalStateException, RollbackException, SecurityException, SystemException {
        transactionManager.commit();
    }

    public void rollback() throws IllegalStateException, SecurityException, SystemException {
        transactionManager.rollback();
    }
}
