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
package org.apache.openejb.config.rules;

import javax.ejb.EJBLocalHome;
import javax.ejb.EJBLocalObject;

import org.apache.openejb.OpenEJBException;
import org.apache.openejb.config.Bean;
import org.apache.openejb.config.EjbSet;
import org.apache.openejb.config.ValidationFailure;
import org.apache.openejb.config.ValidationRule;
import org.apache.openejb.util.SafeToolkit;


/**
 * @author <a href="mailto:david.blevins@visi.com">David Blevins</a>
 */

public class CheckClasses implements ValidationRule {

    EjbSet set;

    public void validate(EjbSet set) {
        this.set = set;

        Bean[] beans = set.getBeans();
        Bean b = null;
        try {
            for (int i = 0; i < beans.length; i++) {
                b = beans[i];
                check_hasEjbClass(b);
                check_isEjbClass(b);
                if (b.getHome() != null) {
                    check_hasHomeClass(b);
                    check_hasRemoteClass(b);
                    check_isHomeInterface(b);
                    check_isRemoteInterface(b);
                }
                if (b.getLocalHome() != null) {
                    check_hasLocalHomeClass(b);
                    check_hasLocalClass(b);
                    check_isLocalHomeInterface(b);
                    check_isLocalInterface(b);
                }
            }
        } catch (RuntimeException e) {
            throw new RuntimeException(b.getEjbName(), e);
        }

        SafeToolkit.unloadTempCodebase(set.getJarPath());
    }

    private void check_hasLocalClass(Bean b) {
        lookForClass(b, b.getLocal(), "<local>");
    }

    private void check_hasLocalHomeClass(Bean b) {
        lookForClass(b, b.getLocalHome(), "<local-home>");
    }


    public void check_hasEjbClass(Bean b) {

        lookForClass(b, b.getEjbClass(), "<ejb-class>");

    }


    public void check_hasHomeClass(Bean b) {

        lookForClass(b, b.getHome(), "<home>");

    }


    public void check_hasRemoteClass(Bean b) {

        lookForClass(b, b.getRemote(), "<remote>");

    }


    public void check_isEjbClass(Bean b) {

        if (b instanceof org.apache.openejb.config.SessionBean) {

            compareTypes(b, b.getEjbClass(), javax.ejb.SessionBean.class);

        } else if (b instanceof org.apache.openejb.config.EntityBean) {

            compareTypes(b, b.getEjbClass(), javax.ejb.EntityBean.class);

        }

    }


    private void check_isLocalInterface(Bean b) {
        compareTypes(b, b.getLocal(), EJBLocalObject.class);
    }

    private void check_isLocalHomeInterface(Bean b) {
        compareTypes(b, b.getLocalHome(), EJBLocalHome.class);
    }


    public void check_isHomeInterface(Bean b) {

        compareTypes(b, b.getHome(), javax.ejb.EJBHome.class);

    }


    public void check_isRemoteInterface(Bean b) {

        compareTypes(b, b.getRemote(), javax.ejb.EJBObject.class);

    }


    private void lookForClass(Bean b, String clazz, String type) {
        try {
            SafeToolkit.loadTempClass(clazz, set.getJarPath());
        } catch (OpenEJBException e) {
/*
            # 0 - Class name
            # 1 - Element (home, ejb-class, remote)
            # 2 - Bean name
            */

            ValidationFailure failure = new ValidationFailure("missing.class");
            failure.setDetails(clazz, type, b.getEjbName());
            failure.setBean(b);

            set.addFailure(failure);

//set.addFailure( new ValidationFailure("missing.class", clazz, type, b.getEjbName()) );
        } catch (NoClassDefFoundError e) {
/*
             # 0 - Class name
             # 1 - Element (home, ejb-class, remote)
             # 2 - Bean name
             # 3 - Misslocated Class name
             */
            ValidationFailure failure = new ValidationFailure("misslocated.class");
            failure.setDetails(clazz, type, b.getEjbName(), e.getMessage());
            failure.setBean(b);

            set.addFailure(failure);
            throw e;
        }

    }

    private void compareTypes(Bean b, String clazz1, Class class2) {
        Class class1 = null;
        try {
            class1 = SafeToolkit.loadTempClass(clazz1, set.getJarPath());
        } catch (OpenEJBException e) {
        }

        if (class1 != null && !class2.isAssignableFrom(class1)) {
            ValidationFailure failure = new ValidationFailure("wrong.class.type");
            failure.setDetails(clazz1, class2.getName());
            failure.setBean(b);

            set.addFailure(failure);
            //set.addFailure( new ValidationFailure("wrong.class.type", clazz1, class2.getName()) );
        }
    }
}





