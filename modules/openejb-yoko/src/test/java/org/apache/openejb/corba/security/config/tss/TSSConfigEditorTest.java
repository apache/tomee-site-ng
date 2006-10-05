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

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Properties;

import junit.framework.TestCase;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;

import org.apache.geronimo.common.DeploymentException;

import org.apache.geronimo.gbean.AbstractName;
import org.apache.geronimo.kernel.Jsr77Naming;
import org.apache.geronimo.j2ee.j2eeobjectnames.NameFactory;
import org.apache.geronimo.kernel.Naming;
import org.apache.geronimo.kernel.repository.Artifact;

import org.omg.CORBA.SystemException;
import org.apache.openejb.corba.CORBABean;
import org.apache.openejb.corba.security.config.ConfigAdapter;


/**
 * @version $Revision$ $Date$
 */
public class TSSConfigEditorTest extends TestCase {

    private XmlObject getXmlObject(String xmlString) throws XmlException {
        XmlObject xmlObject = XmlObject.Factory.parse(xmlString);
        XmlCursor xmlCursor = xmlObject.newCursor();
        try {
            xmlCursor.toFirstChild();
            return xmlCursor.getObject();
        } finally {
            xmlCursor.dispose();
        }
    }

    private static final String TEST_XML4 = "            <tss:tss xmlns:tss=\"http://openejb.apache.org/xml/ns/corba-tss-config-2.1\" xmlns:sec=\"http://geronimo.apache.org/xml/ns/security-1.2\">\n" +
                                            "                <tss:default-principal>\n" +
                                            "                    <sec:principal class=\"org.apache.geronimo.security.realm.providers.GeronimoUserPrincipal\" name=\"guest\"/>\n" +
                                            "                </tss:default-principal>\n" +
                                            "                <tss:SSL port=\"6685\" hostname=\"localhost\">\n" +
                                            "                    <tss:supports>Integrity Confidentiality EstablishTrustInTarget EstablishTrustInClient</tss:supports>\n" +
                                            "                    <tss:requires>Integrity Confidentiality EstablishTrustInClient</tss:requires>\n" +
                                            "                </tss:SSL>\n" +
                                            "                <tss:compoundSecMechTypeList>\n" +
                                            "                    <tss:compoundSecMech>\n" +
                                            "                        <tss:GSSUP targetName=\"geronimo-properties-realm\"/>\n" +
                                            "                        <tss:sasMech>\n" +
                                            "                            <tss:identityTokenTypes><tss:ITTAnonymous/><tss:ITTPrincipalNameGSSUP principal-class=\"org.apache.geronimo.security.realm.providers.GeronimoUserPrincipal\" domain=\"foo\"/><tss:ITTDistinguishedName domain=\"foo\"/><tss:ITTX509CertChain domain=\"foo\"/></tss:identityTokenTypes>\n" +
                                            "                        </tss:sasMech>\n" +
                                            "                    </tss:compoundSecMech>\n" +
                                            "                </tss:compoundSecMechTypeList>\n" +
                                            "            </tss:tss>";

    public void testCORBABean() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        Naming naming = new Jsr77Naming();
        AbstractName testName = naming.createRootName(new Artifact("test", "stuff", "", "ear"), "gbean", NameFactory.CORBA_SERVICE) ;
        ConfigAdapter configAdapter = new org.apache.openejb.yoko.ORBConfigAdapter();
        CORBABean corbaBean = new CORBABean(testName, configAdapter, "localhost", 8050, classLoader, null, null);
        XmlObject xmlObject = getXmlObject(TEST_XML4);
        TSSConfigEditor editor = new TSSConfigEditor();
        Object o = editor.getValue(xmlObject, null, classLoader);
        TSSConfig tss = (TSSConfig) o;

        corbaBean.setTssConfig(tss);

        try {
            corbaBean.doStart();
        } catch(SystemException se) {
            se.printStackTrace();
            fail(se.getCause().getMessage());
        } finally {
            try {
                corbaBean.doStop();
            } catch (Throwable e) {

            }
        }
    }
}

