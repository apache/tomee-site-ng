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
package org.apache.openejb.corba.security.config.css;

import java.util.Iterator;
import java.util.Set;
import javax.security.auth.Subject;

import org.apache.geronimo.security.jaas.NamedUsernamePasswordCredential;
import org.apache.geronimo.security.ContextManager;

import org.apache.openejb.corba.security.config.tss.TSSASMechConfig;
import org.apache.openejb.corba.security.config.tss.TSSGSSUPMechConfig;
import org.apache.openejb.corba.util.Util;


/**
 * This GSSUP mechanism obtains its username and password from a named username
 * password credential that is stored in the subject associated w/ the call
 * stack.
 *
 * @version $Revision$ $Date$
 */
public class CSSGSSUPMechConfigDynamic implements CSSASMechConfig {

    private final String domain;
    private transient byte[] encoding;

    public CSSGSSUPMechConfigDynamic(String domain) {
        this.domain = domain;
    }

    public short getSupports() {
        return 0;
    }

    public short getRequires() {
        return 0;
    }

    public boolean canHandle(TSSASMechConfig asMech) {
        if (asMech instanceof TSSGSSUPMechConfig) return true;
        if (asMech.getRequires() == 0) return true;

        return false;
    }

    public byte[] encode() {
        if (encoding == null) {
            NamedUsernamePasswordCredential credential = null;
            Subject subject = ContextManager.getNextCaller();

            Set creds = subject.getPrivateCredentials(NamedUsernamePasswordCredential.class);

            if (creds.size() != 0) {
                for (Iterator iter = creds.iterator(); iter.hasNext();) {
                    NamedUsernamePasswordCredential temp = (NamedUsernamePasswordCredential) iter.next();
                    if (temp.getName().equals(domain)) {
                        credential = temp;
                        break;
                    }
                }
                if(credential != null) {
                    encoding = Util.encodeGSSUPToken(Util.getORB(), Util.getCodec(), credential.getUsername(), new String(credential.getPassword()), domain);
                }
            }

            if (encoding == null) encoding = new byte[0];
        }
        return encoding;
    }
}
