/*
 * Copyright 2012 Red Hat
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.drools.karaf.itest;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

import org.apache.karaf.tooling.exam.options.LogLevelOption;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.runtime.KieContainer;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;
import org.ops4j.pax.exam.options.UrlReference;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.osgi.context.support.OsgiBundleXmlApplicationContext;

import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.*;
import static org.junit.Assert.*;
import static org.ops4j.pax.exam.CoreOptions.*;

@RunWith(JUnit4TestRunner.class)
public class KieSpringOnKarafTest {

    protected static final transient Logger LOG = LoggerFactory.getLogger(KieSpringOnKarafTest.class);
    protected static final String DroolsVersion = "6.0.0-SNAPSHOT";

    protected OsgiBundleXmlApplicationContext applicationContext;

    @Inject
    protected BundleContext bc;

    @Before
    public void init() {
        applicationContext = createApplicationContext();
        assertNotNull("Should have created a valid spring context", applicationContext);
    }

    protected void refresh() {
        applicationContext.setBundleContext(bc);
        applicationContext.refresh();
    }

    protected OsgiBundleXmlApplicationContext createApplicationContext() {
        return new OsgiBundleXmlApplicationContext(new String[]{"org/kie/spring/kie-beans.xml"});
    }

    @Test
    public void testKContainer() throws Exception {
        Thread.sleep(5000);
        refresh();
        KieContainer kieContainer = (KieContainer) applicationContext.getBean("defaultContainer");
        assertNotNull(kieContainer);
        System.out.println("kieContainer.getReleaseId() == "+kieContainer.getReleaseId());
    }

 /*   @Test
    public void testKieBase() throws Exception {
        KieBase kbase = (KieBase) context.getBean("drl_kiesample");
        assertNotNull(kbase);
    }

    @Test
    public void testReleaseId() throws Exception {
        ReleaseId releaseId = (ReleaseId) context.getBean("dummyReleaseId");
        assertNotNull(releaseId);
    }

    @Test
    public void testKieSessionRef() throws Exception {
        StatelessKieSession ksession = (StatelessKieSession) context.getBean("ksession1");
        assertNotNull(ksession);
    }

    @Test
    public void testKieSession() throws Exception {
        StatelessKieSession ksession = (StatelessKieSession) context.getBean("ksession9");
        assertNotNull(ksession);
    }

    @Test
    public void testKieSessionDefaultType() throws Exception {
        Object obj = context.getBean("ksession99");
        assertNotNull(obj);
        assertTrue(obj instanceof KieSession);
    }*/


    @Configuration
    public static Option[] configure() {
        return new Option[]{

                // Install Karaf Container
                karafDistributionConfiguration().frameworkUrl(
                        maven().groupId("org.apache.karaf").artifactId("apache-karaf").type("tar.gz").versionAsInProject())
                        .karafVersion(MavenUtils.getArtifactVersion("org.apache.karaf", "apache-karaf")).name("Apache Karaf")
                        .unpackDirectory(new File("target/exam/unpack/")),

                keepRuntimeFolder(),
                logLevel(LogLevelOption.LogLevel.INFO),

                // Option to be used to do remote debugging
                // debugConfiguration("5005", true),

                // Load Spring DM Karaf Feature
                scanFeatures(
                        maven().groupId("org.apache.karaf.assemblies.features").artifactId("standard").type("xml").classifier("features").versionAsInProject(),
                        "spring","spring-dm"
                ),

                // Load Drools + Kie
                loadDroolsKieFeatures("kie-spring")

        };

    }

    public static MavenArtifactProvisionOption getFeatureUrl(String groupId, String version) {
        return mavenBundle().groupId(groupId).artifactId(version);
    }

    public static UrlReference getCamelKarafFeatureUrl(String version) {
        String type = "xml/features";
        MavenArtifactProvisionOption mavenOption = getFeatureUrl("org.apache.camel.karaf", "apache-camel");
        if (version == null) {
            return mavenOption.versionAsInProject().type(type);
        } else {
            return mavenOption.version(version).type(type);
        }
    }

    public static Option loadDroolsKieFeatures(String... features) {
        List<String> result = new ArrayList<String>();
        result.add("drools-module");
        for (String feature : features) {
            result.add(feature);
        }
        return scanFeatures(getFeatureUrl("org.drools", "drools-karaf-features").type("xml/features").version(DroolsVersion), result.toArray(new String[4 + features.length]));
    }

}