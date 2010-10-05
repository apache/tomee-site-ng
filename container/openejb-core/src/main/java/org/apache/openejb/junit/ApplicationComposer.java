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
package org.apache.openejb.junit;

import org.apache.openejb.DeploymentInfo;
import org.apache.openejb.InjectionProcessor;
import org.apache.openejb.OpenEJBException;
import org.apache.openejb.assembler.classic.AppInfo;
import org.apache.openejb.assembler.classic.Assembler;
import org.apache.openejb.config.AppModule;
import org.apache.openejb.config.ConfigurationFactory;
import org.apache.openejb.config.ConnectorModule;
import org.apache.openejb.config.EjbModule;
import org.apache.openejb.config.PersistenceModule;
import org.apache.openejb.core.CoreDeploymentInfo;
import org.apache.openejb.core.Operation;
import org.apache.openejb.core.ThreadContext;
import org.apache.openejb.core.ivm.naming.InitContextFactory;
import org.apache.openejb.jee.Application;
import org.apache.openejb.jee.Connector;
import org.apache.openejb.jee.EjbJar;
import org.apache.openejb.jee.EnterpriseBean;
import org.apache.openejb.jee.ManagedBean;
import org.apache.openejb.jee.TransactionType;
import org.apache.openejb.jee.jpa.unit.Persistence;
import org.apache.openejb.jee.jpa.unit.PersistenceUnit;
import org.apache.openejb.jee.oejb3.EjbDeployment;
import org.apache.openejb.jee.oejb3.OpenejbJar;
import org.apache.openejb.loader.SystemInstance;
import org.apache.openejb.spi.ContainerSystem;
import org.apache.openejb.util.Join;
import org.apache.openejb.util.LogCategory;
import org.apache.openejb.util.Logger;
import org.apache.openejb.util.OptionsLog;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;

import javax.naming.Context;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @version $Rev$ $Date$
 */
public class ApplicationComposer extends BlockJUnit4ClassRunner {

    private final TestClass testClass;

    public ApplicationComposer(Class<?> klass) throws InitializationError {
        super(klass);
        testClass = new TestClass(klass);
        validate();
    }

    private void validate() throws InitializationError {
        List<Throwable> errors = new ArrayList<Throwable>();

        final List<FrameworkMethod> configs = testClass.getAnnotatedMethods(Configuration.class);
        if (configs.size() > 1) {
            final String gripe = "Test class should have no more than one @Configuration method";
            errors.add(new Exception(gripe));
        }

        for (FrameworkMethod method : configs) {
            final Class<?> type = method.getMethod().getReturnType();
            if (!Properties.class.isAssignableFrom(type)) {
                final String gripe = "@Configuration method must return " + Properties.class.getName();
                errors.add(new Exception(gripe));
            }
        }

        int appModules = 0;
        int modules = 0;
        Class[] moduleTypes = {EjbJar.class, EnterpriseBean.class, Persistence.class, PersistenceUnit.class, Connector.class};
        for (FrameworkMethod method : testClass.getAnnotatedMethods(Module.class)) {

            modules++;

            final Class<?> type = method.getMethod().getReturnType();

            if (Application.class.isAssignableFrom(type)) {

                appModules++;

            } else if (!isValidModuleType(type, moduleTypes)) {
                final String gripe = "@Module method must return " + Join.join(" or ", moduleTypes).replaceAll("(class|interface) ", "");
                errors.add(new Exception(gripe));
            }
        }

        if (appModules > 1) {
            final String gripe = "Test class should have no more than one @Module method that returns " + Application.class.getName();
            errors.add(new Exception(gripe));
        }

        if (modules < 1) {
            final String gripe = "Test class should have at least one @Module method";
            errors.add(new Exception(gripe));
        }

        if (!errors.isEmpty()) {
            throw new InitializationError(errors);
        }
    }

    private boolean isValidModuleType(Class<?> type, Class[] moduleTypes) {
        for (Class moduleType : moduleTypes) {
            if (moduleType.isAssignableFrom(type)) return true;
        }
        return false;
    }

    @Override
    protected Statement withAfters(FrameworkMethod method, Object target, Statement statement) {
        statement = super.withAfters(method, target, statement);
        statement = new DeployApplication(target, statement);
        return statement;
    }

    public class DeployApplication extends Statement {

        // The TestCase instance
        private final Object testInstance;

        private final Statement next;

        public DeployApplication(Object testInstance, Statement next) {
            this.testInstance = testInstance;
            this.next = next;
        }

        @Override
        public void evaluate() throws Throwable {
            final Class<?> javaClass = testClass.getJavaClass();
            final ClassLoader loader = javaClass.getClassLoader();
            AppModule appModule = new AppModule(loader, javaClass.getSimpleName());

            // Add the test case as an @ManagedBean
            {
                final EjbJar ejbJar = new EjbJar();
                final OpenejbJar openejbJar = new OpenejbJar();
                final ManagedBean bean = ejbJar.addEnterpriseBean(new ManagedBean(javaClass));
                bean.setTransactionType(TransactionType.BEAN);

                final EjbDeployment ejbDeployment = openejbJar.addEjbDeployment(bean);
                ejbDeployment.setDeploymentId(javaClass.getName());

                appModule.getEjbModules().add(new EjbModule(ejbJar, openejbJar));
            }

            Application application = null;

            // Invoke the @Module producer methods to build out the AppModule
            for (FrameworkMethod method : testClass.getAnnotatedMethods(Module.class)) {

                final Object obj = method.invokeExplosively(testInstance);

                if (obj instanceof EjbJar) {

                    final EjbJar ejbJar = (EjbJar) obj;
                    appModule.getEjbModules().add(new EjbModule(ejbJar));

                } else if (obj instanceof EnterpriseBean) {

                    final EnterpriseBean bean = (EnterpriseBean) obj;
                    final EjbJar ejbJar = new EjbJar();
                    ejbJar.addEnterpriseBean(bean);
                    appModule.getEjbModules().add(new EjbModule(ejbJar));

                } else if (obj instanceof Connector) {

                    final Connector connector = (Connector) obj;
                    appModule.getResourceModules().add(new ConnectorModule(connector));

                } else if (obj instanceof Persistence) {

                    final Persistence persistence = (Persistence) obj;
                    appModule.getPersistenceModules().add(new PersistenceModule("", persistence));

                } else if (obj instanceof PersistenceUnit) {

                    final PersistenceUnit unit = (PersistenceUnit) obj;
                    appModule.getPersistenceModules().add(new PersistenceModule("", new Persistence(unit)));

                }
            }

            // For the moment we just take the first @Configuration method
            // maybe later we can add something fancy to allow multiple configurations using a qualifier
            // as a sort of altDD/altConfig concept.  Say for example the altDD prefix might be "foo",
            // we can then imagine something like this:
            // @Foo @Configuration public Properties alternateConfig(){...}
            // @Foo @Module  public Properties alternateModule(){...}
            // anyway, one thing at a time ....

            final Properties configuration = new Properties();
            configuration.put("openejb.deployments.classpath", "false");

            final List<FrameworkMethod> methods = testClass.getAnnotatedMethods(Configuration.class);
            for (FrameworkMethod method : methods) {
                final Object o = method.invokeExplosively(testInstance);
                if (o instanceof Properties) {
                    Properties properties = (Properties) o;
                    configuration.putAll(properties);
                }
                break;
            }

            if (SystemInstance.isInitialized()) SystemInstance.reset();

            SystemInstance.init(configuration);
            SystemInstance.get().setProperty("openejb.embedded", "true");
            Logger.configure();
            OptionsLog.install();
            try {
                ConfigurationFactory config = new ConfigurationFactory();
                config.init(SystemInstance.get().getProperties());

                Assembler assembler = new Assembler();
                assembler.buildContainerSystem(config.getOpenEjbConfiguration());

                final AppInfo appInfo = config.configureApplication(appModule);

                assembler.createApplication(appInfo);

                try {
                    final ContainerSystem containerSystem = SystemInstance.get().getComponent(ContainerSystem.class);
                    final CoreDeploymentInfo context = (CoreDeploymentInfo) containerSystem.getDeploymentInfo(javaClass.getName());

                    Context jndi = (Context) context.getJndiEnc().lookup("comp/env");
                    
                    InjectionProcessor processor = new InjectionProcessor(testInstance, context.getInjections(), jndi);

                    processor.createInstance();

                    System.setProperty(javax.naming.Context.INITIAL_CONTEXT_FACTORY, InitContextFactory.class.getName());

                    final ThreadContext previous = ThreadContext.enter(new ThreadContext(context, null, Operation.BUSINESS));
                    try {
                        next.evaluate();
                    } finally {
                        ThreadContext.exit(previous);
                    }

                } finally {
//                    assembler.destroyApplication(appInfo.jarPath);
                }
            } finally {
                SystemInstance.reset();
            }
        }
    }
}
