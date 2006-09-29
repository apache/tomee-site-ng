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
package org.apache.openejb.server.ejbd;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.openejb.client.AuthenticationRequest;
import org.apache.openejb.client.AuthenticationResponse;
import org.apache.openejb.client.ClientMetaData;
import org.apache.openejb.client.RequestMethods;
import org.apache.openejb.client.ResponseCodes;

class AuthRequestHandler implements ResponseCodes, RequestMethods {
    AuthRequestHandler() {
    }

    public void processRequest(ObjectInputStream in, ObjectOutputStream out) {
        AuthenticationRequest req = new AuthenticationRequest();
        AuthenticationResponse res = new AuthenticationResponse();

        try {
            req.readExternal(in);

            // TODO: perform some real authentication here

            ClientMetaData client = new ClientMetaData();

            client.setClientIdentity(new String((String) req.getPrinciple()));

            res.setResponseCode(AUTH_GRANTED);

            res.writeExternal(out);
        } catch (Throwable t) {
            //replyWithFatalError
            //(out, t, "Error caught during request processing");
            return;
        }
    }
}