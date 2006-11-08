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
package org.openejb.test.entity.cmp;

import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.openejb.test.TestManager;

/**
 * 
 */
public class CmpTestSuite extends org.openejb.test.TestSuite {

    public CmpTestSuite() {
        super();
        this.addTest(new CmpJndiTests());
        this.addTest(new CmpHomeIntfcTests());
        this.addTest(new CmpEjbHomeTests());
        this.addTest(new CmpEjbObjectTests());
        this.addTest(new CmpRemoteIntfcTests());
        this.addTest(new CmpHomeHandleTests());
        this.addTest(new CmpHandleTests());
        this.addTest(new CmpEjbMetaDataTests());
        this.addTest(new CmpAllowedOperationsTests());
        this.addTest(new CmpJndiEncTests());
        this.addTest(new CmpRmiIiopTests());
        this.addTest(new CmpTransactionTests());

    }

    public static junit.framework.Test suite() {
        return new CmpTestSuite();
    }

    /**
     * Sets up the fixture, for example, open a network connection.
     * This method is called before a test is executed.
     */
    protected void setUp() throws Exception {
        Properties props = TestManager.getServer().getContextEnvironment();
        props.put(Context.SECURITY_PRINCIPAL, "ENTITY_TEST_CLIENT");
        props.put(Context.SECURITY_CREDENTIALS, "ENTITY_TEST_CLIENT");
        InitialContext initialContext = new InitialContext(props);
        
        /*[2] Create database table */
        TestManager.getDatabase().createEntityTable();
        TestManager.getDatabase().createEntityExplicitePKTable();
    }

    /**
     * Tears down the fixture, for example, close a network connection.
     * This method is called after a test is executed.
     */
    protected void tearDown() throws Exception {
        /*[1] Drop database table */
        TestManager.getDatabase().dropEntityTable();
        TestManager.getDatabase().dropEntityExplicitePKTable();
    }
}
