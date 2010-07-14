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
package org.apache.openejb.config;

import org.apache.openejb.jee.Connector;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * @version $Rev$ $Date$
 */
public class ConnectorModule implements DeploymentModule {
    private final ValidationContext validation;
    private final Map<String,Object> altDDs = new HashMap<String,Object>();

    private Connector connector;
    private ClassLoader classLoader;
    private String jarLocation;
    private final String moduleId;
    private final List<URL> libraries = new ArrayList<URL>();
    private final Set<String> watchedResources = new TreeSet<String>();

    public ConnectorModule(Connector connector) {
        this(connector, Thread.currentThread().getContextClassLoader(), null, null);
    }

    public ConnectorModule(Connector connector, ClassLoader classLoader, String jarLocation, String moduleId) {
        this.connector = connector;
        this.classLoader = classLoader;
        this.jarLocation = jarLocation;

        if (moduleId == null){
            if (connector != null && connector.getId() != null){
                moduleId = connector.getId();
            } else {
                File file = new File(jarLocation);
                moduleId = file.getName();
                if (moduleId.endsWith(".unpacked")) {
                    moduleId = moduleId.substring(0, moduleId.length() - ".unpacked".length());
                }
                if (moduleId.endsWith(".rar")) {
                    moduleId = moduleId.substring(0, moduleId.length() - ".rar".length());
                }
            }
        }

        this.moduleId = moduleId;
        validation = new ValidationContext(ConnectorModule.class, jarLocation);
    }

    public ValidationContext getValidation() {
        return validation;
    }

    public String getModuleId() {
        return moduleId;
    }

    public Map<String, Object> getAltDDs() {
        return altDDs;
    }

    public Connector getConnector() {
        return connector;
    }

    public void setConnector(Connector connector) {
        this.connector = connector;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public String getJarLocation() {
        return jarLocation;
    }

    public void setJarLocation(String jarLocation) {
        this.jarLocation = jarLocation;
    }

    public List<URL> getLibraries() {
        return libraries;
    }

    public Set<String> getWatchedResources() {
        return watchedResources;
    }
}
