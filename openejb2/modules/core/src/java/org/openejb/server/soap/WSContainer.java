/**
 * Redistribution and use of this software and associated documentation
 * ("Software"), with or without modification, are permitted provided
 * that the following conditions are met:
 *
 * 1. Redistributions of source code must retain copyright
 *    statements and notices.  Redistributions must also contain a
 *    copy of this document.
 *
 * 2. Redistributions in binary form must reproduce the
 *    above copyright notice, this list of conditions and the
 *    following disclaimer in the documentation and/or other
 *    materials provided with the distribution.
 *
 * 3. The name "OpenEJB" must not be used to endorse or promote
 *    products derived from this Software without prior written
 *    permission of The OpenEJB Group.  For written permission,
 *    please contact openejb@openejb.org.
 *
 * 4. Products derived from this Software may not be called "OpenEJB"
 *    nor may "OpenEJB" appear in their names without prior written
 *    permission of The OpenEJB Group. OpenEJB is a registered
 *    trademark of The OpenEJB Group.
 *
 * 5. Due credit should be given to the OpenEJB Project
 *    (http://openejb.org/).
 *
 * THIS SOFTWARE IS PROVIDED BY THE OPENEJB GROUP AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL
 * THE OPENEJB GROUP OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Copyright 2001 (C) The OpenEJB Group. All Rights Reserved.
 *
 * $Id$
 */
package org.openejb.server.soap;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import javax.wsdl.Definition;

import org.codehaus.xfire.MessageContext;
import org.codehaus.xfire.XFireRuntimeException;
import org.codehaus.xfire.fault.Soap11FaultHandler;
import org.codehaus.xfire.fault.XFireFault;
import org.codehaus.xfire.handler.SoapHandler;
import org.codehaus.xfire.java.DefaultJavaService;
import org.codehaus.xfire.java.Invoker;
import org.codehaus.xfire.java.JavaServiceHandler;
import org.codehaus.xfire.soap.Soap11;
import org.openejb.EJBContainer;
import org.openejb.EJBInterfaceType;
import org.openejb.EJBInvocation;
import org.openejb.EJBInvocationImpl;
import org.openejb.proxy.ProxyInfo;
import org.apache.geronimo.core.service.InvocationKey;
import org.apache.geronimo.webservices.MessageContextInvocationKey;

public class WSContainer implements Invoker {

    private final EJBContainer ejbContainer;
    private final URI location;
    private final URL wsdlURL;
    private final DefaultJavaService service;

    protected WSContainer() {
        this.ejbContainer = null;
        this.location = null;
        this.wsdlURL = null;
        this.service = null;
    }

    public WSContainer(EJBContainer ejbContainer, Definition definition, URI location, URL wsdlURL, String namespace, String encoding, String style) {
        this.ejbContainer = ejbContainer;
        this.location = location;
        this.wsdlURL = wsdlURL;

        ProxyInfo proxyInfo = ejbContainer.getProxyInfo();
        Class serviceEndpointInterface = proxyInfo.getServiceEndpointInterface();

        service = new DefaultJavaService();
        service.setName(ejbContainer.getEJBName());
        service.setDefaultNamespace(namespace);
        service.setServiceClass(serviceEndpointInterface);
        service.setSoapVersion(Soap11.getInstance());
        service.setStyle(style);
        service.setUse(encoding);
        service.setWSDLURL(wsdlURL);
        service.setServiceHandler(new SoapHandler(new JavaServiceHandler(this)));
        service.setFaultHandler(new Soap11FaultHandler());

        LightWeightServiceConfigurator configurator = new LightWeightServiceConfigurator(definition, service);
        configurator.configure();
    }

    public void invoke(MessageContext context) {
        SoapHandler handler = null;
        try {
            context.setService(service);

            handler = (SoapHandler) service.getServiceHandler();
            handler.invoke(context);
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof XFireRuntimeException) {
                throw (XFireRuntimeException) e;
            } else if (handler != null) {
                XFireFault fault = XFireFault.createFault(e);
                handler.handleFault(fault, context);
            } else {
                throw new XFireRuntimeException("Couldn't process message.", e);
            }
        }
    }

    public Object invoke(Method m, Object[] params, MessageContext context) throws XFireFault {
        try {
            int index = ejbContainer.getProxyFactory().getMethodIndex(m);
            EJBInvocation invocation = new EJBInvocationImpl(EJBInterfaceType.WEB_SERVICE, null, index, params);
            //I give up, why would you use xfire?
            Map properties = new HashMap();
            javax.xml.rpc.handler.MessageContext messageContext = new SimpleMessageContext(properties);
            invocation.put(MessageContextInvocationKey.INSTANCE, messageContext);
            return ejbContainer.invoke(invocation);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new XFireFault("Error invoking EJB", throwable, XFireFault.RECEIVER);
        }
    }

    public URI getLocation() {
        return location;
    }

    public URL getWsdlURL() {
        return wsdlURL;
    }

    private static class SimpleMessageContext implements javax.xml.rpc.handler.MessageContext {

        private final Map properties;

        public SimpleMessageContext(Map properties) {
            this.properties = new HashMap(properties);
        }

        public boolean containsProperty(String name) {
            return properties.containsKey(name);
        }

        public Object getProperty(String name) {
            return properties.get(name);
        }

        public Iterator getPropertyNames() {
            return properties.keySet().iterator();
        }

        public void removeProperty(String name) {
            properties.remove(name);
        }

        public void setProperty(String name, Object value) {
            properties.put(name, value);
        }
    }
}
