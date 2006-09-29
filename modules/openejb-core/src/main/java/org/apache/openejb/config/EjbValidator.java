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
package org.apache.openejb.config;

import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.Vector;

import org.apache.openejb.OpenEJBException;
import org.apache.openejb.config.ejb11.EjbJar;
import org.apache.openejb.config.rules.CheckClasses;
import org.apache.openejb.config.rules.CheckMethods;
import org.apache.openejb.util.JarUtils;
import org.apache.openejb.util.Messages;


/**
 * @author <a href="mailto:david.blevins@visi.com">David Blevins</a>
 */
public class EjbValidator {

    static protected Messages _messages = new Messages("org.apache.openejb.util.resources");

    int LEVEL = 2;
    boolean PRINT_DETAILS = false;
    boolean PRINT_XML = false;
    boolean PRINT_WARNINGS = true;
    boolean PRINT_COUNT = false;

    private Vector sets = new Vector();

    /*------------------------------------------------------*/
    /*    Constructors                                      */
    /*------------------------------------------------------*/
    public EjbValidator() throws OpenEJBException {
        JarUtils.setHandlerSystemProperty();
    }

    public void addEjbSet(EjbSet set) {
        sets.add(set);
    }

    public EjbSet[] getEjbSets() {
        EjbSet[] ejbSets = new EjbSet[sets.size()];
        sets.copyInto(ejbSets);
        return ejbSets;
    }


    public EjbSet validateJar(String jarLocation) {
        EjbSet set = new EjbSet(jarLocation);

        try {
            set.setEjbJar(EjbJarUtils.readEjbJar(jarLocation));
            validateJar(set);
        } catch (Throwable e) {
            e.printStackTrace(System.out);
            ValidationError err = new ValidationError("cannot.validate");
            err.setDetails(e.getMessage());
            set.addError(err);
        }
        return set;
    }

    public EjbSet validateJar(EjbJar ejbJar, String jarLocation) {
        // Create the EjbSet
        EjbSet set = new EjbSet(jarLocation);
        set.setEjbJar(ejbJar);
        return validateJar(set);
    }

    public EjbSet validateJar(EjbSet set) {
        try {
            //System.out.println("[] validating "+ set.getJarPath());        
            // Run the validation rules
            ValidationRule[] rules = getValidationRules();
            for (int i = 0; i < rules.length; i++) {
                rules[i].validate(set);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            ValidationError err = new ValidationError("cannot.validate");
            err.setDetails(e.getMessage());
            set.addError(err);
        }
        return set;
    }

    protected ValidationRule[] getValidationRules() {
        ValidationRule[] rules = new ValidationRule[]{
            new CheckClasses(),
            new CheckMethods()
        };
        return rules;
    }
    
    //Validate all classes are present
    //Validate classes are the correct type
    //Validate ejb references
    //Validate resource references
    //Validate security references

    
    public void printResults(EjbSet set) {
        if (!set.hasErrors() && !set.hasFailures() && (!PRINT_WARNINGS || !set.hasWarnings())) {
            return;
        }
        System.out.println("------------------------------------------");
        System.out.println("JAR " + set.getJarPath());
        System.out.println("                                          ");

        printValidationExceptions(set.getErrors());
        printValidationExceptions(set.getFailures());

        if (PRINT_WARNINGS) {
            printValidationExceptions(set.getWarnings());
        }
    }

    protected void printValidationExceptions(ValidationException[] exceptions) {
        for (int i = 0; i < exceptions.length; i++) {
            System.out.print(" ");
            System.out.print(exceptions[i].getPrefix());
            System.out.print(" ... ");
            if (!(exceptions[i] instanceof ValidationError)) {
                System.out.print(exceptions[i].getBean().getEjbName());
                System.out.print(": ");
            }
            if (LEVEL > 2) {
                System.out.println(exceptions[i].getMessage(1));
                System.out.println();
                System.out.print('\t');
                System.out.println(exceptions[i].getMessage(LEVEL));
                System.out.println();
            } else {
                System.out.println(exceptions[i].getMessage(LEVEL));
            }
        }
        if (PRINT_COUNT && exceptions.length > 0) {
            System.out.println();
            System.out.print(" " + exceptions.length + " ");
            System.out.println(exceptions[0].getCategory());
            System.out.println();
        }

    }

    public void printResultsXML(EjbSet set) {
        if (!set.hasErrors() && !set.hasFailures() && (!PRINT_WARNINGS || !set.hasWarnings())) {
            return;
        }

        System.out.println("<jar>");
        System.out.print("  <path>");
        System.out.print(set.getJarPath());
        System.out.println("</path>");

        printValidationExceptionsXML(set.getErrors());
        printValidationExceptionsXML(set.getFailures());

        if (PRINT_WARNINGS) {
            printValidationExceptionsXML(set.getWarnings());
        }
        System.out.println("</jar>");
    }

    protected void printValidationExceptionsXML(ValidationException[] exceptions) {
        for (int i = 0; i < exceptions.length; i++) {
            System.out.print("    <");
            System.out.print(exceptions[i].getPrefix());
            System.out.println(">");
            if (!(exceptions[i] instanceof ValidationError)) {
                System.out.print("      <ejb-name>");
                System.out.print(exceptions[i].getBean().getEjbName());
                System.out.println("</ejb-name>");
            }
            System.out.print("      <summary>");
            System.out.print(exceptions[i].getMessage(1));
            System.out.println("</summary>");
            System.out.println("      <description><![CDATA[");
            System.out.println(exceptions[i].getMessage(3));
            System.out.println("]]></description>");
            System.out.print("    </");
            System.out.print(exceptions[i].getPrefix());
            System.out.println(">");
        }
    }

    public void displayResults(EjbSet[] sets) {
        if (PRINT_XML) {
            System.out.println("<results>");
            for (int i = 0; i < sets.length; i++) {
                printResultsXML(sets[i]);
            }
            System.out.println("</results>");
        } else {
            for (int i = 0; i < sets.length; i++) {
                printResults(sets[i]);
            }
            for (int i = 0; i < sets.length; i++) {
                if (sets[i].hasErrors() || sets[i].hasFailures()) {
                    if (LEVEL < 3) {
                        System.out.println();
                        System.out.println("For more details, use the -vvv option");
                    }
                    i = sets.length;
                }
            }
        }

    }

    /*------------------------------------------------------*/
    /*    Static methods                                    */
    /*------------------------------------------------------*/

    private static void printVersion() {
        /*
         * Output startup message
         */
        Properties versionInfo = new Properties();

        try {
            JarUtils.setHandlerSystemProperty();
            versionInfo.load(new URL("resource:/openejb-version.properties").openConnection().getInputStream());
        } catch (java.io.IOException e) {
        }

        System.out.println("OpenEJB EJB Validation Tool " + versionInfo.get("version") + "    build: " + versionInfo.get("date") + "-" + versionInfo.get("time"));
        System.out.println("" + versionInfo.get("url"));
    }

    private static void printHelp() {
        String header = "OpenEJB EJB Validation Tool ";
        try {
            JarUtils.setHandlerSystemProperty();
            Properties versionInfo = new Properties();
            versionInfo.load(new URL("resource:/openejb-version.properties").openConnection().getInputStream());
            header += versionInfo.get("version");
        } catch (java.io.IOException e) {
        }

        System.out.println(header);
        
        // Internationalize this
        try {
            InputStream in = new URL("resource:/openejb/validate.txt").openConnection().getInputStream();

            int b = in.read();
            while (b != -1) {
                System.out.write(b);
                b = in.read();
            }
        } catch (java.io.IOException e) {
        }
    }

    private static void printExamples() {
        String header = "OpenEJB EJB Validation Tool ";
        try {
            JarUtils.setHandlerSystemProperty();
            Properties versionInfo = new Properties();
            versionInfo.load(new URL("resource:/openejb-version.properties").openConnection().getInputStream());
            header += versionInfo.get("version");
        } catch (java.io.IOException e) {
        }

        System.out.println(header);
        
        // Internationalize this
        try {
            InputStream in = new URL("resource:/openejb/validate-examples.txt").openConnection().getInputStream();

            int b = in.read();
            while (b != -1) {
                System.out.write(b);
                b = in.read();
            }
        } catch (java.io.IOException e) {
        }
    }


    public static void main(String args[]) {
        try {
            org.apache.openejb.util.ClasspathUtils.addJarsToPath("lib");
            org.apache.openejb.util.ClasspathUtils.addJarsToPath("dist");
        } catch (Exception e) {
            // ignore it
        }
        try {
            EjbValidator v = new EjbValidator();

            if (args.length == 0) {
                printHelp();
                return;
            }

            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-v")) {
                    v.LEVEL = 1;
                } else if (args[i].equals("-vv")) {
                    v.LEVEL = 2;
                } else if (args[i].equals("-vvv")) {
                    v.LEVEL = 3;
                } else if (args[i].equals("-nowarn")) {
                    v.PRINT_WARNINGS = false;
                } else if (args[i].equals("-xml")) {
                    v.PRINT_XML = true;
                } else if (args[i].equals("-help")) {
                    printHelp();
                } else if (args[i].equals("-examples")) {
                    printExamples();
                } else if (args[i].equals("-version")) {
                    printVersion();
                } else {
                    // We must have reached the jar list
                    for (; i < args.length; i++) {
                        try {
                            EjbSet set = v.validateJar(args[i]);
                            v.addEjbSet(set);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            EjbSet[] sets = v.getEjbSets();
            v.displayResults(sets);

            for (int i = 0; i < sets.length; i++) {
                if (sets[i].hasErrors() || sets[i].hasFailures()) {
                    System.exit(1);
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            //e.printStackTrace();
        }
    }

}
