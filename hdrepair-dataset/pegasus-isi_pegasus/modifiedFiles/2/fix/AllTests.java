/*
 * Copyright 2007-2014 University Of Southern California
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.isi.pegasus.planner.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    edu.isi.pegasus.common.util.VersionTest.class,
    edu.isi.pegasus.common.util.PegasusURLTest.class,
    edu.isi.pegasus.planner.namespace.PegasusTest.class,
    edu.isi.pegasus.planner.catalog.replica.impl.RegexRCTest.class,
    edu.isi.pegasus.planner.catalog.replica.impl.JDBCRCTest.class,
    edu.isi.pegasus.planner.cluster.RuntimeClusteringTest.class,
    edu.isi.pegasus.planner.code.generator.condor.style.GliteTest.class,
    edu.isi.pegasus.planner.transfer.mapper.FlatOutputMapperTest.class,
    edu.isi.pegasus.planner.transfer.mapper.HashedOutputMapperTest.class,
    edu.isi.pegasus.planner.transfer.mapper.ReplicaOutputMapperTest.class,
    edu.isi.pegasus.planner.transfer.mapper.FixedOutputMapperTest.class,
    edu.isi.pegasus.planner.refiner.DataReuseEngineTest.class,
    edu.isi.pegasus.common.util.GLiteEscapeTest.class
})
public class AllTests {
}

