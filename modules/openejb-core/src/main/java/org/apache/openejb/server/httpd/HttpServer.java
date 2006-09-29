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
package org.apache.openejb.server.httpd;

import org.activeio.xnet.ServerService;
import org.activeio.xnet.ServiceException;
import org.activeio.xnet.SocketService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.gbean.GBeanLifecycle;
import org.apache.openejb.DeploymentIndex;
import org.apache.openejb.OpenEJBException;
import sun.net.www.protocol.http.HttpURLConnection;

import javax.naming.Context;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.util.Properties;

/**
 * This is the main class for the web administration.  It takes care of the
 * processing from the browser, sockets and threading.
 *
 * @since 11/25/2001
 */
public class HttpServer implements SocketService, ServerService, GBeanLifecycle {

    private static Log log = LogFactory.getLog(HttpServer.class);
    private HttpListener listener;

    public HttpServer(DeploymentIndex index) {
    }

    public HttpServer() {
    }

    public HttpServer(HttpListener listener) {
        this.listener = listener;
    }

    public void service(Socket socket) throws ServiceException, IOException {
        /**
         * The InputStream used to receive incoming messages from the client.
         */
        InputStream in = socket.getInputStream();
        /**
         * The OutputStream used to send outgoing response messages to the client.
         */
        OutputStream out = socket.getOutputStream();

        try {
            //TODO: if ssl change to https
            URI socketURI = new URI("http://" + socket.getLocalAddress().getHostName() + ":" + socket.getLocalPort());
            processRequest(socketURI, in, out);
        } catch (Throwable e) {
            log.error("Unexpected error", e);
        } finally {
            try {
                if (out != null) {
                    out.flush();
                    out.close();
                }
                if (in != null)
                    in.close();
                if (socket != null)
                    socket.close();
            } catch (Throwable t) {
                log.error("Encountered problem while closing connection with client: "
                        + t.getMessage());
            }
        }
    }

    public void start() throws ServiceException {
    }

    public void stop() throws ServiceException {
    }

    public String getName() {
        return "webadmin";
    }

    public int getPort() {
        return 0;
    }

    public String getIP() {
        return "";
    }

    public HttpListener getListener() {
        return listener;
    }


    /**
     * Initalizes this instance and takes care of starting things up
     *
     * @param props a properties instance for system properties
     * @throws Exception if an exeption is thrown
     */
    public void init(Properties props) throws Exception {

        //props.putAll(System.getProperties());

        Properties properties = new Properties();
        properties.put(Context.INITIAL_CONTEXT_FACTORY,
                "org.apache.openejb.naming.GlobalInitialContextFactory");

    }

    /**
     * takes care of processing requests and creating the webadmin ejb's
     *
     * @param socket
     * @param in     the input stream from the browser
     * @param out    the output stream to the browser
     */
    private void processRequest(URI socketURI, InputStream in, OutputStream out) {
        HttpResponseImpl response = null;
        try {
            response = process(socketURI, in);

        } catch (Throwable t) {
            response = HttpResponseImpl.createError(t.getMessage(), t);
        } finally {
            try {
                response.writeMessage(out);
            } catch (Throwable t2) {
                log.error("Could not write response", t2);
            }
        }

    }

    private HttpResponseImpl process(URI socketURI, InputStream in) throws OpenEJBException {

        HttpRequestImpl req = new HttpRequestImpl(socketURI);
        HttpResponseImpl res = new HttpResponseImpl();

        try {
            req.readMessage(in);
            res.setRequest(req);
        } catch (Throwable t) {
            res.setCode(HttpURLConnection.HTTP_BAD_REQUEST);
            res.setResponseString("Could not read the request");
            res.getPrintWriter().println(t.getMessage());
            t.printStackTrace(res.getPrintWriter());
            log.error("BAD REQUEST", t);
            throw new OpenEJBException("Could not read the request.\n" + t.getClass().getName() + ":\n" + t.getMessage(), t);
        }

        URI uri = null;
        String location = null;
        try {
            uri = req.getURI();
            location = uri.getPath();
            int querry = location.indexOf("?");
            if (querry != -1) {
                location = location.substring(0, querry);
            }
        } catch (Throwable t) {
            throw new OpenEJBException("Could not determine the module " + location + "\n" + t.getClass().getName() + ":\n" + t.getMessage());
        }

        HttpListener httpListener = null;

        try {
            httpListener = getHttpListener(location);
        } catch (Throwable t) {
            throw new OpenEJBException("Could not load the module " + location + "\n" + t.getClass().getName() + ":\n" + t.getMessage(), t);
        }

        try {
            httpListener.onMessage(req, res);
        } catch (Throwable t) {
            throw new OpenEJBException("Error occurred while executing the module " + location + "\n" + t.getClass().getName() + ":\n" + t.getMessage(), t);
        }

        return res;
    }

    private HttpListener getHttpListener(String uri) {
        return listener;
    }

    public void doStart() throws Exception {
    }

    public void doStop() throws Exception {
    }

    public void doFail() {
    }
}
