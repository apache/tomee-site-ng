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

package org.openejb.server;

import java.net.InetAddress;
import javax.management.ObjectName;

import org.apache.geronimo.gbean.GBeanData;
import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoBuilder;
import org.apache.geronimo.kernel.GBeanAlreadyExistsException;
import org.apache.geronimo.kernel.GBeanNotFoundException;
import org.apache.geronimo.kernel.Kernel;
import org.apache.geronimo.kernel.jmx.JMXUtil;
import org.apache.geronimo.j2ee.j2eeobjectnames.NameFactory;

public class StandardServiceStackGBean {

    public static final GBeanInfo GBEAN_INFO;

    static {
        GBeanInfoBuilder infoFactory = new GBeanInfoBuilder(StandardServiceStack.class);

        infoFactory.addAttribute("name", String.class, true);
        infoFactory.addAttribute("port", int.class, true);
        infoFactory.addAttribute("soTimeout", int.class, true);
        infoFactory.addAttribute("address", InetAddress.class, true);
        infoFactory.addAttribute("allowHosts", InetAddress[].class, true);
        infoFactory.addAttribute("threads", int.class, true);
        infoFactory.addAttribute("priority", int.class, true);
        infoFactory.addAttribute("logOnSuccess", String[].class, true);
        infoFactory.addAttribute("logOnFailure", String[].class, true);
        infoFactory.addReference("Server", ServerService.class, NameFactory.GERONIMO_SERVICE);

        infoFactory.setConstructor(new String[]{
            "name",
            "port",
            "address",
            "allowHosts",
            "threads",
            "priority",
            "logOnSuccess",
            "logOnFailure",
            "Server"});

        GBEAN_INFO = infoFactory.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }

    public static ObjectName addGBean(Kernel kernel, String name, int port, InetAddress address, InetAddress[] allowHosts, int threads, int priority, String[] logOnSuccess, String[] logOnFailure, ObjectName server) throws GBeanAlreadyExistsException, GBeanNotFoundException {
        ClassLoader classLoader = StandardServiceStack.class.getClassLoader();
        ObjectName SERVICE_NAME = JMXUtil.getObjectName("openejb:type=StandardServiceStack,name=" + name);

        GBeanData gbean = new GBeanData(SERVICE_NAME, StandardServiceStackGBean.GBEAN_INFO);
        gbean.setAttribute("name", name);
        gbean.setAttribute("port", new Integer(port));
        gbean.setAttribute("address", address);
        gbean.setAttribute("allowHosts", allowHosts);
        gbean.setAttribute("threads", new Integer(threads));
        gbean.setAttribute("priority", new Integer(priority));
        gbean.setAttribute("logOnSuccess", logOnSuccess);
        gbean.setAttribute("logOnFailure", logOnFailure);
        gbean.setReferencePattern("Server", server);

        kernel.loadGBean(gbean, classLoader);
        kernel.startGBean(SERVICE_NAME);
        return SERVICE_NAME;
    }
}
