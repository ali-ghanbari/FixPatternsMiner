/*
 * Copyright 2011 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.guvnor.models.testscenarios.backend.populators;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.drools.base.ClassTypeResolver;
import org.drools.base.TypeResolver;
import org.drools.guvnor.models.testscenarios.shared.FactData;
import org.drools.guvnor.models.testscenarios.shared.Field;
import org.drools.guvnor.models.testscenarios.shared.FieldData;
import org.junit.Before;
import org.junit.Test;
import org.kie.runtime.KieSession;
import org.kie.runtime.rule.FactHandle;

public class NewFactPopulatorTest {

    private TypeResolver            typeResolver;
    private HashMap<String, Object> populatedData;
    private KieSession              workingMemory;

    @Before
    public void setUp() throws Exception {
        typeResolver = new ClassTypeResolver( new HashSet<String>(),
                                              Thread.currentThread().getContextClassLoader() );
        populatedData = new HashMap<String, Object>();
        workingMemory = mock( KieSession.class );
    }

    @Test
    public void testDummyRunNoRules() throws Exception {
        typeResolver.addImport( "org.drools.guvnor.models.testscenarios.backend.Cheese" );

        List<Field> fieldData = new ArrayList<Field>();
        fieldData.add( new FieldData( "type",
                                      "cheddar" ) );
        fieldData.add( new FieldData( "price",
                                      "42" ) );
        FactData fact = new FactData( "Cheese",
                                      "c1",
                                      fieldData,
                                      false );

        NewFactPopulator newFactPopulator = new NewFactPopulator(
                                                                  populatedData,
                                                                  typeResolver,
                                                                  Thread.currentThread().getContextClassLoader(),
                                                                  fact );

        newFactPopulator.populate( workingMemory, new HashMap<String, FactHandle>() );

        assertTrue( populatedData.containsKey( "c1" ) );
        assertNotNull( populatedData.get( "c1" ) );
        
        verify( workingMemory ).insert( populatedData.get( "c1" ) );
    }

}
