/*
 *  Copyright 2013, 2014 Deutsche Nationalbibliothek
 *
 *  Licensed under the Apache License, Version 2.0 the "License";
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.culturegraph.mf.morph.collectors;

import org.culturegraph.mf.test.TestSuite;
import org.culturegraph.mf.test.TestSuite.TestDefinitions;
import org.junit.runner.RunWith;


/**
 * @author Markus Michael Geipel
 */
@RunWith(TestSuite.class)
@TestDefinitions({ "AllTest.xml", "AnyTest.xml", "NoneTest.xml", "CombineTest.xml", "GroupTest.xml", "ChooseTest.xml", "EntityTest.xml", "ConcatTest.xml",
		"Nested.xml", "NestedEntity.xml", "TuplesTest.xml", "Misc.xml", "SquareTest.xml", "RangeTest.xml", "EqualsFilterTest.xml", "If.xml" })
public final class CollectorTest {/* bind to xml test */
}
