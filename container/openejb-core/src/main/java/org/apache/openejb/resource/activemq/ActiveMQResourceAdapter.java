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
package org.apache.openejb.resource.activemq;

import org.apache.openejb.util.URISupport;

import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapterInternalException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import org.apache.openejb.util.LogCategory;

public class ActiveMQResourceAdapter extends org.apache.activemq.ra.ActiveMQResourceAdapter {

    private String dataSource;
    private String useDatabaseLock;

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public void setUseDatabaseLock(String useDatabaseLock) {
        this.useDatabaseLock = useDatabaseLock;
    }

//   DMB:  Work in progress.  These all should go into the service-jar.xml
//   Sources of info:
//         - http://activemq.apache.org/resource-adapter-properties.html
//         - http://activemq.apache.org/camel/maven/camel-core/apidocs/org/apache/camel/processor/RedeliveryPolicy.html
//
//    /**
//     * 100	 The maximum number of messages sent to a consumer on a durable topic until acknowledgements are received
//     * @param integer
//     */
//    public void setDurableTopicPrefetch(Integer integer) {
//        super.setDurableTopicPrefetch(integer);
//    }
//
//    /**
//     * 1000	 The delay before redeliveries start. Also configurable on the ActivationSpec.
//     * @param aLong
//     */
//    public void setInitialRedeliveryDelay(Long aLong) {
//        super.setInitialRedeliveryDelay(aLong);
//    }
//
//    /**
//     * 100	 The maximum number of messages sent to a consumer on a JMS stream until acknowledgements are received
//     * @param integer
//     */
//    public void setInputStreamPrefetch(Integer integer) {
//        super.setInputStreamPrefetch(integer);
//    }
//
//    /**
//     * 5	 The maximum number of redeliveries or -1 for no maximum. Also configurable on the ActivationSpec.
//     * @param integer
//     */
//    public void setMaximumRedeliveries(Integer integer) {
//        super.setMaximumRedeliveries(integer);
//    }
//
//    public void setQueueBrowserPrefetch(Integer integer) {
//        super.setQueueBrowserPrefetch(integer);
//    }
//
//    /**
//     * 1000	 The maximum number of messages sent to a consumer on a queue until acknowledgements are received
//     * @param integer
//     */
//    public void setQueuePrefetch(Integer integer) {
//        super.setQueuePrefetch(integer);
//    }
//
//    /**
//     * 5	 The multiplier to use if exponential back off is enabled. Also configurable on the ActivationSpec.
//     * @param aShort
//     */
//    public void setRedeliveryBackOffMultiplier(Short aShort) {
//        super.setRedeliveryBackOffMultiplier(aShort);
//    }
//
//    public void setRedeliveryUseExponentialBackOff(Boolean aBoolean) {
//        super.setRedeliveryUseExponentialBackOff(aBoolean);
//    }
//
//    /**
//     * 32766  The maximum number of messages sent to a consumer on a non-durable topic until acknowledgements are received
//     * @param integer
//     */
//    public void setTopicPrefetch(Integer integer) {
//        super.setTopicPrefetch(integer);
//    }
    
    @Override
    public void start(final BootstrapContext bootstrapContext) throws ResourceAdapterInternalException {
        final Properties properties = new Properties();

        //Add data source property
        if (null != this.dataSource) {
            properties.put("DataSource", this.dataSource);
        }

        if (null != this.useDatabaseLock) {
            properties.put("UseDatabaseLock", this.useDatabaseLock);
        }

        // prefix server uri with openejb: so our broker factory is used
        final String brokerXmlConfig = getBrokerXmlConfig();
        if (brokerXmlConfig != null && brokerXmlConfig.startsWith("broker:")) {
            try {
                final URISupport.CompositeData compositeData = URISupport.parseComposite(new URI(brokerXmlConfig));
                compositeData.getParameters().put("persistent", "false");
                setBrokerXmlConfig(ActiveMQFactory.getBrokerMetaFile() + compositeData.toURI());
            } catch (URISyntaxException e) {
                throw new ResourceAdapterInternalException("Invalid BrokerXmlConfig", e);
            }
        }

        ActiveMQFactory.setThreadProperties(properties);

        try {
            super.start(bootstrapContext);
        } finally {
            ActiveMQFactory.setThreadProperties(null);

            // reset brokerXmlConfig
            if (brokerXmlConfig != null) {
                setBrokerXmlConfig(brokerXmlConfig);
            }
        }
    }

    @Override
    public void stop() {

        final ActiveMQResourceAdapter ra = this;

        final Thread stopThread = new Thread("ActiveMQResourceAdapter stop") {

            @Override
            public void run() {
                ra.stopImpl();
            }
        };

        stopThread.setDaemon(true);
        stopThread.start();

        try {
            //Block for a maximum of 5 seconds waiting for this thread to die.
            stopThread.join(5000);
        } catch (InterruptedException ex) {
            org.apache.openejb.util.Logger.getInstance(LogCategory.OPENEJB, "org.apache.openejb.util.resources").warning("Gave up on ActiveMQ shutdown", ex);
            return;
        }
    }

    private void stopImpl() {
        super.stop();
        org.apache.openejb.util.Logger.getInstance(LogCategory.OPENEJB, "org.apache.openejb.util.resources").info("Stopped ActiveMQ");
    }
}
