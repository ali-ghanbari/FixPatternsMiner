/*
Copyright 2011 Selenium committers
Copyright 2011 Software Freedom Conservancy

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package org.openqa.grid.internal;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.openqa.grid.internal.configuration.Grid1ConfigurationLoaderTest;
import org.openqa.grid.internal.listener.RegistrationListenerTest;
import org.openqa.grid.internal.listener.SessionListenerTest;
import org.openqa.grid.internal.utils.DefaultCapabilityMatcherTest;
import org.openqa.grid.plugin.RemoteProxyInheritanceTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    RemoteProxyInheritanceTest.class,
    SmokeTest.class,
    SessionTimesOutTest.class,
    BaseRemoteProxyTest.class,
    RemoteProxySlowSetup.class,
    RegistryTest.class,
    RegistryStateTest.class,
    PriorityTestLoad.class,
    PriorityTest.class,
    ParallelTest.class,
    LoadBalancedTests.class,
//    DefaultToFIFOPriorityTest.class,
    ConcurrencyLockTest.class,
    AddingProxyAgainFreesResources.class,
    DefaultCapabilityMatcherTest.class,
    SessionListenerTest.class,
    RegistrationListenerTest.class,
    StatusServletTests.class,
    Grid1ConfigurationLoaderTest.class,
    UserDefinedCapabilityMatcherTests.class,
    GridShutdownTest.class
})
public class GridInternalTestSuite {
// 186 sec
}
