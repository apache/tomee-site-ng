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
package org.apache.openejb.corba.security.config.tss;

import java.io.Serializable;


/**
 * @version $Revision$ $Date$
 */
public class TSSTransportAddressConfig implements Serializable {
    private short port;
    private String hostname;

    public TSSTransportAddressConfig() {
    }

    public TSSTransportAddressConfig(short port, String hostname) {
        this.port = port;
        this.hostname = hostname;
    }

    public short getPort() {
        return port;
    }

    public void setPort(short port) {
        this.port = port;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        toString("", buf);
        return buf.toString();
    }

    void toString(String spaces, StringBuffer buf) {
        String moreSpaces = spaces + "  ";
        buf.append(spaces).append("TSSTransportAddressConfig: [\n");
        buf.append(moreSpaces).append("port    : ").append(port).append("\n");
        buf.append(moreSpaces).append("hostName: ").append(hostname).append("\n");
        buf.append(spaces).append("]\n");
    }

}
