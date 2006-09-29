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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.openejb.corba.security.config.tss.TSSCompoundSecMechConfig;
import org.apache.openejb.corba.security.config.tss.TSSCompoundSecMechListConfig;


/**
 * @version $Rev$ $Date$
 */
public class CSSCompoundSecMechListConfig implements Serializable {

    private boolean stateful;
    private final ArrayList mechs = new ArrayList();

    public boolean isStateful() {
        return stateful;
    }

    public void setStateful(boolean stateful) {
        this.stateful = stateful;
    }

    public void add(CSSCompoundSecMechConfig mech) {
        mechs.add(mech);
    }

    public CSSCompoundSecMechConfig mechAt(int i) {
        return (CSSCompoundSecMechConfig) mechs.get(i);
    }

    public int size() {
        return mechs.size();
    }

    public List findCompatibleSet(TSSCompoundSecMechListConfig mechList) {
        List result = new ArrayList();

        for (Iterator availMechs = mechs.iterator(); availMechs.hasNext();) {
            CSSCompoundSecMechConfig aConfig = (CSSCompoundSecMechConfig) availMechs.next();

            int size = mechList.size();
            for (int i = 0; i < size; i++) {
                TSSCompoundSecMechConfig requirement = mechList.mechAt(i);

                if (aConfig.canHandle(requirement)) {
                    result.add(aConfig);
                }
            }

        }

        return result;
    }
}
