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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import javax.ejb.spi.HandleDelegate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.gbean.AbstractName;
import org.apache.geronimo.gbean.GBeanLifecycle;
import org.apache.openejb.corba.security.config.ConfigAdapter;
import org.apache.openejb.corba.security.config.ssl.SSLConfig;
import org.apache.openejb.corba.security.config.tss.TSSConfig;
import org.apache.openejb.corba.security.config.tss.TSSSSLTransportConfig;
import org.apache.openejb.corba.security.config.tss.TSSTransportMechConfig;
import org.apache.openejb.corba.util.Util;
import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;


/**
 * A CORBABean is a main CORBA server configuration.  The
 * CORBABean is the hosting ORB to which additional TSSBeans
 * attach to export EJBs.  The CORBABean may be configured
 * to use either plain socket listeners or SSL listeners, based
 * on the bean specification.  All TSSBean objects attached
 * to this Bean instance will share the same listener
 * endpoint and transport-level security.
 * @version $Revision$ $Date$
 */
public class CORBABean implements GBeanLifecycle, ORBRef, ORBConfiguration {
    private final Log log = LogFactory.getLog(CORBABean.class);

    private final ClassLoader classLoader;
    private final ConfigAdapter configAdapter;
    // the initial listener port
    private int listenerPort;
    // the host name we expose in IORs
    private String host;
    private TSSConfig tssConfig;
    private SSLConfig sslConfig;
    private ORB orb;
    private POA rootPOA;
    private NameService nameService;
    private AbstractName abstractName;

    public CORBABean() {
        this.classLoader = null;
        this.configAdapter = null;
        this.sslConfig = null;
        this.listenerPort = -1;
        this.host = null;
        this.abstractName = null;
    }

    /**
     * Instantiate a CORBABean instance.
     *
     * @param abstractName
     *               The server-created abstract name for this bean instance.
     * @param configAdapter
     *               The ORB ConfigAdapter used to interface with the
     *               JVM-configured ORB instance.
     * @param host   The hostname we publish ourselves under.
     * @param listenerPort
     *               The initial listener port to use.
     * @param classLoader
     *               The ClassLoader used for ORB context class loading.
     * @param nameService
     *               The initial name service the created ORB will use
     *               for object resolution.
     * @param ssl    The SSL configuration, including the KeystoreManager.
     *
     */
    public CORBABean(AbstractName abstractName, ConfigAdapter configAdapter, String host, int listenerPort, ClassLoader classLoader, NameService nameService, SSLConfig ssl) {
        this.abstractName = abstractName;
        this.classLoader = classLoader;
        this.configAdapter = configAdapter;
        sslConfig = ssl;
        this.nameService = nameService;
        this.host = host;
        this.listenerPort = listenerPort;
    }

    /**
     * Retrieve the NameService this CORBA server depends upon.
     *
     * @return The configured NameService instance.
     */
    public NameService getNameService() {
        return nameService;
    }

    /**
     * Setter attribute for the NameService.
     *
     * @param s      The new target name service.
     */
    public void setNameService(NameService s) {
        nameService = s;
    }

    /**
     * Get the optional TSSConfig object specified for this
     * CORBABean server.
     *
     * @return The TSSConfig object (if any).
     */
    public TSSConfig getTssConfig() {
        // if nothing has been explicitly set, ensure we return
        // a default one.
        if (tssConfig == null) {
            tssConfig = new TSSConfig();
        }
        return tssConfig;
    }

    /**
     * Set a TSSConfig value for this CORBA instance.
     *
     * @param config The required TSSConfig information.
     */
    public void setTssConfig(TSSConfig config) {
        this.tssConfig = config;
    }

    /**
     * Return the SSLConfig used for this ORB instance.
     * if one has not been configured, this returns
     * a default configuration.
     *
     * @return The SSLConfig object use to manage transport-level
     *         security.
     */
    public SSLConfig getSslConfig() {
        if (sslConfig == null) {
            sslConfig = new SSLConfig();
        }
        return sslConfig;
    }

    /**
     * Attribute setter for the SSL configuration.
     *
     * @param c      The new SSLConfig object used for secure communications.
     */
    public void setSslConfing(SSLConfig c) {
        sslConfig = c;
    }


    /**
     * Return the ORB instance created for this CORBABean.
     *
     * @return The ORB instance backing this bean.
     */
    public ORB getORB() {
        return orb;
    }

    public HandleDelegate getHandleDelegate() {
        return new CORBAHandleDelegate();
    }

    /**
     * Get the root POA() instance associated with the ORB.
     *
     * @return The rootPOA instance obtained from the ORB.
     */
    public POA getRootPOA() {
        return rootPOA;
    }

    /**
     * Retrieve the listener address (host/port combo) used
     * by the ORB.
     *
     * @return An InetSocketAddress item identifying the end point
     *         for the ORB.
     */
    public InetSocketAddress getListenAddress() {
        return new InetSocketAddress(host, listenerPort);
    }

    /**
     * Start the ORB associated with this bean instance.
     *
     * @exception Exception
     */
    public void doStart() throws Exception {

        ClassLoader savedLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);

            // make sure we've decided how the listener should be configured.
            resolveListenerAddress();

            log.debug("CORBABean " + getURI() + " creating listener on port " + listenerPort);
            // the config adapter creates the actual ORB instance for us.
            orb = configAdapter.createServerORB(this);

            // we set this ORB value into the Util.  The Util ORB is used for a lot of utility things, so
            // we'll cache the first instance created.
            Util.setORB(orb);

            // TSSBeans are going to need our rootPOA instance, so resolve this now.
            org.omg.CORBA.Object obj = orb.resolve_initial_references("RootPOA");
            rootPOA = POAHelper.narrow(obj);
        } finally {
            Thread.currentThread().setContextClassLoader(savedLoader);
        }

    }

    public void doStop() throws Exception {
        orb.destroy();
        log.debug("Stopped CORBABean");
    }

    public void doFail() {
        log.warn("Failed CORBABean");
    }

    /**
     * Process the specified host/port information on
     * both the bean and the TSSConfig to arrive at a
     * target port.  This must be called prior to creating
     * the ORB.
     */
    private void resolveListenerAddress() {
        // if we have a config with a TSSSSLTransportConfig defined, the
        // host and port from the config override bean-configured values.
        if (tssConfig != null) {
            TSSTransportMechConfig transportMech = tssConfig.getTransport_mech();
            if (transportMech != null) {
                if (transportMech instanceof TSSSSLTransportConfig) {
                    TSSSSLTransportConfig transportConfig = (TSSSSLTransportConfig) transportMech;
                    host = transportConfig.getHostname();
                    listenerPort = transportConfig.getPort();
                }
            }
        }
        // now provide defaults for anything still needing resolving
        if (host == null) {
            try {
                host = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                // just punt an use localhost as an absolute fallback.
                host = "localhost";
            }
        }

        // if nothing has been explicitly specified, we use a port value of -1, which
        // allows the ORB to allocate the address.
    }

    /**
     * Return the retrieval URI for this bean.
     *
     * @return The URI for the bean AbstractName;
     */
    public String getURI() {
        return abstractName.toString();
    }

    /**
     * Get the configured listener port.
     *
     * @return The configeration port value.
     */
    public int getPort() {
        return listenerPort;
    }

    /**
     * Get the configuration host name.
     *
     * @return The configuration host name.  The default is "localhost".
     */
    public String getHost() {
        return host;
    }
}
