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

import javax.security.auth.Subject;

import org.omg.CORBA.ORB;
import org.omg.CSI.EstablishContext;
import org.omg.CSIIOP.AS_ContextSec;
import org.omg.GSSUP.GSSUPMechOID;
import org.omg.IOP.Codec;

import org.apache.openejb.corba.security.SASException;
import org.apache.openejb.corba.util.Util;


/**
 * @version $Rev$ $Date$
 */
public class TSSNULLASMechConfig extends TSSASMechConfig {

    public short getSupports() {
        return 0;
    }

    public short getRequires() {
        return 0;
    }

    /**
     * Encode a virtually null AS context.  Since supports is zero, everything
     * else should be ignored.
     *
     * @param orb
     * @param codec
     * @return
     * @throws Exception
     */
    public AS_ContextSec encodeIOR(ORB orb, Codec codec) throws Exception {
        AS_ContextSec result = new AS_ContextSec();

        result.target_supports = 0;
        result.target_requires = 0;
        result.client_authentication_mech = Util.encodeOID(GSSUPMechOID.value);
        result.target_name = Util.encodeGSSExportName(GSSUPMechOID.value, "");

        return result;
    }

    public Subject check(EstablishContext msg) throws SASException {
        return null;
    }
}
