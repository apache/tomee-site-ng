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
package org.apache.openejb.corba.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.gbean.GBeanLifecycle;
import org.apache.openejb.corba.CORBABean;


/**
 * OpenORB specific startup GBean
 *
 * @version $Revision$ $Date$
 */
public class OpenORBUtil implements GBeanLifecycle {

    private final Log log = LogFactory.getLog(OpenORBUtil.class);

    private final CORBABean server;

    public OpenORBUtil() {
        server = null;
    }

    public OpenORBUtil(CORBABean server) {
        this.server = server;
    }

    public CORBABean getServer() {
        return server;
    }

    public void doStart() throws Exception {

//        DefaultORB.setORB(server.getORB());

        log.debug("Started OpenORBUtil");
    }

    public void doStop() throws Exception {
        log.debug("Stopped OpenORBUtil");
    }

    public void doFail() {
        log.warn("Failed OpenORBUtil");
    }

}
