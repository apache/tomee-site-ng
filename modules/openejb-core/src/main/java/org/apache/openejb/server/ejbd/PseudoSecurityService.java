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

import org.apache.openejb.spi.SecurityService;
import org.apache.openejb.util.FastThreadLocal;

public class PseudoSecurityService implements SecurityService {
    
    private FastThreadLocal threadStorage = new FastThreadLocal( );
    
    public void init(java.util.Properties props ) {
        props = props;
    }
    
    public Object getSecurityIdentity(){
        return threadStorage.get();
    }
    public void setSecurityIdentity(Object securityIdentity){
        threadStorage.set(securityIdentity);
    }
    
    public boolean isCallerAuthorized(Object securityIdentity,String [] roleNames){
        // use this principal to validate against roles
        return true;
    }
    
    public Object translateTo(Object securityIdentity, Class type){
        if(type == java.security.Principal.class){
            return new java.security.Principal(){
                public String getName(){return "TestRole";}
            };
        }else if(type == javax.security.auth.Subject.class){
            return new javax.security.auth.Subject();   
        }else{
            return null;
        }
    }
}