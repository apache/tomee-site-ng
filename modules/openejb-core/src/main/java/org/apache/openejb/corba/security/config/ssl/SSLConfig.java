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
package org.apache.openejb.corba.security.config.ssl;

import org.apache.geronimo.management.geronimo.KeystoreManager;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
import org.apache.geronimo.management.geronimo.KeyIsLocked;
import org.apache.geronimo.management.geronimo.KeystoreIsLocked;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;

import org.apache.openejb.corba.security.config.tss.TSSConfig;

/**
 * An active SSL configuration.  The SSL configuration
 * identifies the KeystoreManager instance to be used
 * for SSL connections, as well as the specifics
 * of the certificates to be used for the connections.
 *
 * The socket factories attached to the CORBA ORBs
 * used the SSLConfig to retrieve SocketFactory instances
 * for creating the secure sockets.
 * @version $Rev$ $Date$
 */
public class SSLConfig {
    private KeystoreManager manager;
    private String provider;
    private String keyStore;
    private String trustStore;
    private String keyAlias;
    private String algorithm = "default";
    private String protocol = "SSL";

    /**
     * Default GBean constructor.
     */
    public SSLConfig() {
        manager = null;
    }

    /**
     * "Normal" constructor for config items.
     *
     * @param keystoreManager
     *               The keystoreManager instance used to create SSL sockets
     *               for this configuration.
     */
    public SSLConfig(KeystoreManager keystoreManager) {
        manager = keystoreManager;
    }


    /**
     * Create an SSLServerSocketFactory instance for creating
     * server-side SSL connections.
     *
     * @param loader The class loader used to resolve classes required
     *               by the KeystoreManager.
     *
     * @return An SSLServerSocketFactory instance created with the
     *         SSLConfig specifices.
     * @throws KeystoreIsLocked
     *                Occurs when the requested key keystore cannot
     *                be used because it has not been unlocked.
     * @throws KeyIsLocked
     *                Occurs when the requested private key in the key
     *                keystore cannot be used because it has not been
     *                unlocked.
     * @throws NoSuchAlgorithmException
     * @throws UnrecoverableKeyException
     * @throws KeyStoreException
     * @throws KeyManagementException
     * @throws NoSuchProviderException
     */
    public SSLSocketFactory createSSLFactory(ClassLoader loader) throws KeystoreIsLocked, KeyIsLocked, NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException, KeyManagementException, NoSuchProviderException {
        if (manager != null) {
            // fix up the default algorithm now. 
            if ("default".equalsIgnoreCase(algorithm)) {
                this.algorithm = KeyManagerFactory.getDefaultAlgorithm();
            }
            // the keystore manager does all of the heavy lifting
            return manager.createSSLFactory(provider, protocol, algorithm, keyStore, keyAlias, trustStore, loader);
        }
        else {
            return (SSLSocketFactory) SSLSocketFactory.getDefault();
        }
    }

    /**
     * Create an SSLSocketFactory instance for creating
     * client-side SSL connections.
     *
     * @param loader The class loader used to resolve classes required
     *               by the KeystoreManager.
     *
     * @return An SSLSocketFactory instance created with the
     *         SSLConfig specifices.
     * @throws KeystoreIsLocked
     *                Occurs when the requested key keystore cannot
     *                be used because it has not been unlocked.
     * @throws KeyIsLocked
     *                Occurs when the requested private key in the key
     *                keystore cannot be used because it has not been
     *                unlocked.
     * @throws NoSuchAlgorithmException
     * @throws UnrecoverableKeyException
     * @throws KeyStoreException
     * @throws KeyManagementException
     * @throws NoSuchProviderException
     */
    public SSLServerSocketFactory createSSLServerFactory(ClassLoader loader) throws KeystoreIsLocked, KeyIsLocked, NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException, KeyManagementException, NoSuchProviderException {
        if (manager != null) {
            // fix up the default algorithm now. 
            if ("default".equalsIgnoreCase(algorithm)) {
                this.algorithm = KeyManagerFactory.getDefaultAlgorithm();
            }
            // the keystore manager does all of the heavy lifting
            return manager.createSSLServerFactory(provider, protocol, algorithm, keyStore, keyAlias, trustStore, loader);
        }
        else {
            return (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        }
    }

    /**
     * Get the protocol to be used by this SSL configuration.
     * Normally, this is just "SSL".
     *
     * @return The String name of the configuration protocol.
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Set the protocol to be used by this configuration.
     *
     * @param protocol The new protocol name.
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }


    /**
     * Retrieve the encryption provider to be used for
     * these connnections.
     *
     * @return The current provider name.
     */
    public String getProvider() {
        return provider;
    }

    /**
     * Set a new encryption provider for the SSL access.
     *
     * @param provider The new provider name.
     */
    public void setProvider(String provider) {
        this.provider = provider;
    }

    /**
     * The encryption algorithm to use.
     *
     * @return The current encryption algorithm.
     */
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * Algorithm to use.
     * As different JVMs have different implementations available, the default algorithm can be used by supplying the value "Default".
     *
     * @param algorithm the algorithm to use, or "Default" to use the default from {@link javax.net.ssl.KeyManagerFactory#getDefaultAlgorithm()}
     */
    public void setAlgorithm(String algorithm) {
        // cache the value so the null
        this.algorithm = algorithm;
    }

    /**
     * Set the name of the keystore to be used for this
     * connection.  This must be the name of a keystore
     * stored within the KeystoreManager instance.
     *
     * @param keyStore The key store String name.
     */
    public void setKeyStore(String keyStore) {
        this.keyStore = keyStore;
    }

    /**
     * Retrieve the name of the keystore.
     *
     * @return The String key store name.
     */
    public String getKeyStore() {
        return keyStore;
    }

    /**
     * Set the name of the truststore to be used for
     * connections.  The truststore must map to one
     * managed by the KeystoreManager instance.
     *
     * @param trustStore The new trustStore name.
     */
    public void setTrustStore(String trustStore) {
        this.trustStore = trustStore;
    }

    /**
     * Retrieve the in-use truststore name.
     *
     * @return The String name of the trust store.
     */
    public String getTrustStore() {
        return trustStore;
    }

    /**
     * Set the key alias to be used for the connection.
     *
     * @param keyAlias The String name of the key alias.
     */
    public void setKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
    }

    /**
     * Retrieve the key alias name to use.
     *
     * @return The String name of the key alias.
     */
    public String getKeyAlias() {
        return keyAlias;
    }
}

