/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.itest.osgi;
import java.net.URL;
import org.apache.camel.CamelContext;
import org.apache.camel.osgi.CamelContextFactory;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.karaf.testing.Helper;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;
import org.ops4j.pax.exam.options.UrlReference;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.scanFeatures;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.workingDirectory;

public class OSGiIntegrationTestSupport extends CamelTestSupport {
    protected static final transient Logger LOG = LoggerFactory.getLogger(OSGiIntegrationTestSupport.class);
    @Inject
    protected BundleContext bundleContext;
    
    protected Bundle getInstalledBundle(String symbolicName) {
        for (Bundle b : bundleContext.getBundles()) {
            if (b.getSymbolicName().equals(symbolicName)) {
                return b;
            }
        }
        for (Bundle b : bundleContext.getBundles()) {
            System.err.println("Bundle: " + b.getSymbolicName());
        }
        throw new RuntimeException("Bundle " + symbolicName + " does not exist");
    }
        
    protected CamelContext createCamelContext() throws Exception {
        LOG.info("Get the bundleContext is " + bundleContext);
        LOG.info("Application installed as bundle id: " + bundleContext.getBundle().getBundleId());

        setThreadContextClassLoader();

        CamelContextFactory factory = new CamelContextFactory();
        factory.setBundleContext(bundleContext);
        factory.setRegistry(createRegistry());
        return factory.createContext();
    }
    
    protected void setThreadContextClassLoader() {
        // set the thread context classloader current bundle classloader
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
    }
    
    public static UrlReference getCamelKarafFeatureUrl() {
        return getCamelKarafFeatureUrl(null);
    }

    public static UrlReference getCamelKarafFeatureUrl(String version) {

        String type = "xml/features";
        MavenArtifactProvisionOption mavenOption = mavenBundle().groupId("org.apache.camel.karaf").artifactId("apache-camel");
        if (version == null) {
            return mavenOption.versionAsInProject().type(type);
        } else {
            return mavenOption.version(version).type(type);
        }
    }
    
    public static UrlReference getKarafFeatureUrl() {
        String karafVersion = System.getProperty("karafVersion");
        System.out.println("*** The karaf version is " + karafVersion + " ***");

        String type = "xml/features";
        return mavenBundle().groupId("org.apache.karaf.assemblies.features").
            artifactId("standard").version(karafVersion).type(type);
    }

    public static UrlReference getKarafEnterpriseFeatureUrl() {
        String karafVersion = System.getProperty("karafVersion");
        System.out.println("*** The karaf version is " + karafVersion + " ***");

        String type = "xml/features";
        return mavenBundle().groupId("org.apache.karaf.assemblies.features").
            artifactId("enterprise").version(karafVersion).type(type);
    }
    
    private static URL getResource(String location) {
        URL url = null;
        if (Thread.currentThread().getContextClassLoader() != null) {
            url = Thread.currentThread().getContextClassLoader().getResource(location);
        }
        if (url == null) {
            url = Helper.class.getResource(location);
        }
        if (url == null) {
            throw new RuntimeException("Unable to find resource " + location);
        }
        return url;
    }
    
    public static Option[] getDefaultCamelKarafOptions() {
        Option[] options = combine(
            // Set the karaf environment with some customer configuration
            combine(
                Helper.getDefaultConfigOptions(
                    Helper.getDefaultSystemOptions(),
                    getResource("/org/apache/camel/itest/karaf/config.properties"),
                    // this is how you set the default log level when using pax logging (logProfile)
                    Helper.setLogLevel("WARN")),
                Helper.getDefaultProvisioningOptions()),                             
            // install the spring, http features first
            scanFeatures(getKarafFeatureUrl(), "spring", "spring-dm", "jetty"),

            // using the features to install the camel components             
            scanFeatures(getCamelKarafFeatureUrl(),                         
                "camel-core", "camel-spring", "camel-test"),
                                   
            workingDirectory("target/paxrunner/"));

            //equinox(),
            //felix());
        return options;
    }

    @Configuration
    public static Option[] configure() throws Exception {
        Option[] options = combine(
            getDefaultCamelKarafOptions());

        // for remote debugging
        // vmOption("-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5008"),

        return options;
    }

}
