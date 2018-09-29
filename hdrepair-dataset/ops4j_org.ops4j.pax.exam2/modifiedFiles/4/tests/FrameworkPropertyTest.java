/*
 * Copyright (C) 2011 Harald Wellmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.exam.regression.multi.felix.options;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.regression.multi.RegressionConfiguration.isFelix;
import static org.ops4j.pax.exam.regression.multi.RegressionConfiguration.isNativeContainer;
import static org.ops4j.pax.exam.regression.multi.RegressionConfiguration.regressionDefaults;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.swissbox.framework.ServiceLookup;
import org.osgi.framework.BundleContext;
import org.osgi.service.startlevel.StartLevel;

@RunWith( JUnit4TestRunner.class )
@ExamReactorStrategy( PerMethod.class )
public class FrameworkPropertyTest
{

    @Inject
    private BundleContext bc;
    
    @Configuration
    public Option[] config()
    {
        return options(
            regressionDefaults(),
            frameworkProperty( "felix.startlevel.bundle" ).value( "4" ),
            junitBundles() );
    }

    @Test
    public void startLevel()
    {
        assumeTrue( isFelix() && !isNativeContainer() );

        // Framework properties are currently translated into vmOptions for Pax Runner.
        // Pax Runner does not clearly distinguish framework and system properties.
        assertThat( System.getProperty( "felix.startlevel.bundle" ), is( "4" ) );

        StartLevel startLevel = ServiceLookup.getService( bc, StartLevel.class );
        assertThat( startLevel, is( notNullValue() ) );
        assertThat( startLevel.getInitialBundleStartLevel(), is( equalTo( 4 ) ) );
    }
}
