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
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.openejb.jee;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.List;

/**
 * The security-permissionType specifies a security
 * permission that is required by the resource adapter code.
 * <p/>
 * The security permission listed in the deployment descriptor
 * are ones that are different from those required by the
 * default permission set as specified in the connector
 * specification. The optional description can mention specific
 * reason that resource adapter requires a given security
 * permission.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "security-permissionType", propOrder = {
        "description",
        "securityPermissionSpec"
})
public class SecurityPermission {

    protected List<Text> description;
    @XmlElement(name = "security-permission-spec", required = true)
    protected String securityPermissionSpec;
    @XmlAttribute
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    protected String id;

    public List<Text> getDescription() {
        if (description == null) {
            description = new ArrayList<Text>();
        }
        return this.description;
    }

    public String getSecurityPermissionSpec() {
        return securityPermissionSpec;
    }

    public void setSecurityPermissionSpec(String value) {
        this.securityPermissionSpec = value;
    }

    public String getId() {
        return id;
    }

    public void setId(String value) {
        this.id = value;
    }

}
