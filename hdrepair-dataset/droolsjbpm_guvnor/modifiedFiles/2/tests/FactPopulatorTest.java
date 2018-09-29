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

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.drools.base.ClassTypeResolver;
import org.drools.base.TypeResolver;
import org.drools.guvnor.models.testscenarios.shared.CollectionFieldData;
import org.drools.guvnor.models.testscenarios.shared.FactData;
import org.drools.guvnor.models.testscenarios.shared.Field;
import org.drools.guvnor.models.testscenarios.shared.FieldData;
import org.junit.Before;
import org.junit.Test;
import org.drools.guvnor.models.testscenarios.backend.Cheese;
import org.drools.guvnor.models.testscenarios.backend.CheeseType;
import org.drools.guvnor.models.testscenarios.backend.Cheesery;
import org.drools.guvnor.models.testscenarios.backend.MyCollectionWrapper;
import org.drools.guvnor.models.testscenarios.backend.OuterFact;
import org.drools.guvnor.models.testscenarios.backend.Person;
import org.drools.guvnor.models.testscenarios.backend.SqlDateWrapper;
import org.kie.runtime.KieSession;

public class FactPopulatorTest {

    static {
        try {
            Class.forName("org.drools.base.mvel.MVELCompilationUnit");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private KieSession workingMemory;
    private Map<String, Object> populatedData;
    private FactPopulator factPopulator;

    @Before
    public void setUp() throws Exception {
        workingMemory = mock( KieSession.class );
        populatedData = new HashMap<String, Object>();
        factPopulator = new FactPopulator(workingMemory, populatedData);
    }

    @Test
    public void testPopulateFacts() throws Exception {
        FactData factData = new FactData(
                "Person",
                "p1",
                Arrays.<Field>asList(
                        new FieldData(
                                "name",
                                "mic"),
                        new FieldData(
                                "age",
                                "=30 + 3")),
                false);

        factPopulator.add(new NewFactPopulator(populatedData, getTypeResolver(), getClassLoader(), factData));

        factPopulator.populate();

        assertTrue(populatedData.containsKey("p1"));

        Person person = (Person) populatedData.get("p1");
        assertEquals("mic",
                person.getName());
        assertEquals(33,
                person.getAge());
    }

    @Test
    public void testPopulateEnum() throws Exception {

        FieldData fieldData = new FieldData(
                "cheeseType",
                "CheeseType.CHEDDAR");
        fieldData.setNature(FieldData.TYPE_ENUM);
        FactData factData = new FactData("Cheese",
                "c1",
                asList((Field) fieldData),
                false);

        factPopulator.add(new NewFactPopulator(populatedData, getTypeResolver(), getClassLoader(), factData));

        factPopulator.populate();

        assertTrue(populatedData.containsKey("c1"));

        Cheese cheese = (Cheese) populatedData.get("c1");
        assertEquals(CheeseType.CHEDDAR, cheese.getCheeseType());
    }

    @Test
    public void testPopulateNested() throws Exception {

        TypeResolver typeResolver = getTypeResolver();

        FactData cheeseFactData = new FactData(
                "Cheese",
                "c1",
                Arrays.<Field>asList(
                        new FieldData(
                                "type",
                                "cheddar"),
                        new FieldData(
                                "price",
                                "42")),
                false);

        factPopulator.add(new NewFactPopulator(populatedData, typeResolver, getClassLoader(), cheeseFactData));

        FactData outerFactData = new FactData(
                "OuterFact",
                "p1",
                Arrays.<Field>asList(
                        new FieldData(
                                "name",
                                "mic"),
                        new FieldData(
                                "innerFact",
                                "=c1")),
                false);

        factPopulator.add(new NewFactPopulator(populatedData, typeResolver, getClassLoader(), outerFactData));

        factPopulator.populate();

        assertTrue(populatedData.containsKey("c1"));
        assertTrue(populatedData.containsKey("p1"));

        OuterFact o = (OuterFact) populatedData.get("p1");
        assertEquals(populatedData.get("c1"), o.getInnerFact());
    }

    @Test
    public void testPopulateNestedWrongOrder() throws Exception {

        TypeResolver typeResolver = getTypeResolver();

        FactData outerFactData = new FactData(
                "OuterFact",
                "p1",
                Arrays.<Field>asList(
                        new FieldData(
                                "name",
                                "mic"),
                        new FieldData(
                                "innerFact",
                                "=c1")),
                false);

        factPopulator.add(new NewFactPopulator(populatedData, typeResolver, getClassLoader(), outerFactData));

        FactData cheeseFactData = new FactData(
                "Cheese",
                "c1",
                Arrays.<Field>asList(
                        new FieldData(
                                "type",
                                "cheddar"),
                        new FieldData(
                                "price",
                                "42")),
                false);

        factPopulator.add(new NewFactPopulator(populatedData, typeResolver, getClassLoader(), cheeseFactData));

        factPopulator.populate();

        assertTrue(populatedData.containsKey("c1"));
        assertTrue(populatedData.containsKey("p1"));

        OuterFact o = (OuterFact) populatedData.get("p1");
        assertEquals(populatedData.get("c1"), o.getInnerFact());
    }

    @Test
    public void testPopulateFactWithoutFields() throws Exception {

        factPopulator.add(
                new NewFactPopulator(
                        populatedData,
                        getTypeResolver(),
                        getClassLoader(),
                        new FactData("Cheese",
                                "c1",
                                new ArrayList<Field>(),
                                false)));

        factPopulator.populate();

        assertTrue(populatedData.containsKey("c1"));
        assertTrue(populatedData.get("c1") instanceof Cheese);
    }

    @Test
    public void testPopulateEmptyIntegerField() throws Exception {

        factPopulator.add(
                new NewFactPopulator(
                        populatedData,
                        getTypeResolver(),
                        getClassLoader(),
                        new FactData("Cheese",
                                "c1",
                                Arrays.<Field>asList(
                                        new FieldData(
                                                "price",
                                                "")),
                                false)));

        factPopulator.populate();

        assertTrue(populatedData.containsKey("c1"));
        assertTrue(populatedData.get("c1") instanceof Cheese);
    }

    @Test
    public void testPopulatingExistingFact() throws Exception {
        Cheese cheese = new Cheese();
        cheese.setType("whee");
        cheese.setPrice(1);

        Map<String, Object> populatedData = new HashMap<String, Object>();
        populatedData.put(
                "x",
                cheese);

        factPopulator.add(
                new ExistingFactPopulator(
                        populatedData,
                        getTypeResolver(),
                        getClassLoader(),
                        new FactData(
                                "Cheese",
                                "x",
                                Arrays.<Field>asList(
                                        new FieldData(
                                                "type",
                                                null),
                                        new FieldData(
                                                "price",
                                                "42")),
                                false)));

        factPopulator.populate();

        assertEquals("whee", cheese.getType());
        assertEquals(42, cheese.getPrice());
    }

    @Test
    public void testDateField() throws Exception {

        factPopulator.add(
                new NewFactPopulator(
                        populatedData,
                        getTypeResolver(),
                        getClassLoader(),
                        new FactData(
                                "Cheese",
                                "c1",
                                Arrays.<Field>asList(
                                        new FieldData(
                                                "type",
                                                "cheddar"),
                                        new FieldData(
                                                "usedBy",
                                                "10-Jul-2008")),
                                false)));
        factPopulator.add(
                new NewFactPopulator(
                        populatedData,
                        getTypeResolver(),
                        getClassLoader(),
                        new FactData(
                                "OuterFact",
                                "p1",
                                Arrays.<Field>asList(
                                        new FieldData(
                                                "name",
                                                "mic"),
                                        new FieldData(
                                                "innerFact",
                                                "=c1")),
                                false)));

        factPopulator.populate();

        assertTrue(populatedData.containsKey("c1"));
        assertTrue(populatedData.containsKey("p1"));

        Cheese c = (Cheese) populatedData.get("c1");
        assertNotNull(c.getUsedBy());

    }


    @Test
    public void testSQLDateField() throws Exception {

        factPopulator.add(
                new NewFactPopulator(
                        populatedData,
                        getTypeResolver(),
                        getClassLoader(),
                        new FactData(
                                "SqlDateWrapper",
                                "c1",
                                Arrays.<Field>asList(
                                        new FieldData(
                                                "sqlDate",
                                                "10-Jul-2008")),
                                false)));

        factPopulator.populate();

        assertTrue(populatedData.containsKey("c1"));

        SqlDateWrapper sqlDateWrapper = (SqlDateWrapper) populatedData.get("c1");
        assertNotNull(sqlDateWrapper.getSqlDate());

    }


    @Test
    public void testPopulateFactsWithExpressions() throws Exception {

        factPopulator.add(
                new NewFactPopulator(
                        populatedData,
                        getTypeResolver(),
                        getClassLoader(),
                        new FactData("Cheese",
                                "c1",
                                Arrays.<Field>asList(
                                        new FieldData(
                                                "type",
                                                "cheddar"),
                                        new FieldData(
                                                "price",
                                                "42")),
                                false)));
        factPopulator.add(
                new NewFactPopulator(
                        populatedData,
                        getTypeResolver(),
                        getClassLoader(),
                        new FactData(
                                "Cheese",
                                "c2",
                                Arrays.<Field>asList(
                                        new FieldData(
                                                "type",
                                                "= c1.type")),
                                false)));

        factPopulator.populate();

        assertTrue(populatedData.containsKey("c1"));
        assertTrue(populatedData.containsKey("c2"));

        Cheese c = (Cheese) populatedData.get("c1");
        assertEquals("cheddar", c.getType());
        assertEquals(42, c.getPrice());

        Cheese c2 = (Cheese) populatedData.get("c2");
        assertEquals(c.getType(), c2.getType());
    }

    @Test
    public void testPopulateEmptyString() throws Exception {
        Cheese cheese = new Cheese();
        cheese.setType("whee");
        cheese.setPrice(1);
        populatedData.put("x", cheese);

        assertEquals(1, cheese.getPrice());

        //An empty String is a 'value' as opposed to null
        factPopulator.add(
                new ExistingFactPopulator(
                        populatedData,
                        getTypeResolver(),
                        getClassLoader(),
                        new FactData(
                                "Cheese",
                                "x",
                                Arrays.<Field>asList(
                                        new FieldData(
                                                "type",
                                                ""),
                                        new FieldData(
                                                "price",
                                                "42")),
                                false)));

        factPopulator.populate();

        assertEquals("", cheese.getType());
        assertEquals(42, cheese.getPrice());
    }

    @Test
    public void testCollectionFieldInFacts() throws Exception {

        TypeResolver typeResolver = getTypeResolver();

        FactData fd1 = new FactData("Cheese",
                "f1",
                Arrays.<Field>asList(
                        new FieldData("type",
                                ""),
                        new FieldData("price",
                                "42")),
                false);

        factPopulator.add(new NewFactPopulator(populatedData, typeResolver, getClassLoader(), fd1));

        FactData fd2 = new FactData("Cheese",
                "f2",
                Arrays.<Field>asList(
                        new FieldData("type",
                                ""),
                        new FieldData("price",
                                "43")),
                false);
        factPopulator.add(new NewFactPopulator(populatedData, typeResolver, getClassLoader(), fd2));

        FactData fd3 = new FactData("Cheese",
                "f3",
                Arrays.<Field>asList(
                        new FieldData("type",
                                ""),
                        new FieldData("price",
                                "45")),
                false);

        factPopulator.add(new NewFactPopulator(populatedData, typeResolver, getClassLoader(), fd3));

        FieldData field = new FieldData();
        field.setName("cheeses");
        field.setNature(FieldData.TYPE_COLLECTION);
        field.setValue("=[f1,f2,f3]");
        List<Field> lstField = new ArrayList<Field>();
        lstField.add(field);
        FactData lst = new FactData("Cheesery",
                "listChesse",
                lstField,
                false);
        factPopulator.add(new NewFactPopulator(populatedData, typeResolver, getClassLoader(), lst));

        factPopulator.populate();


        Cheesery listChesse = (Cheesery) populatedData.get("listChesse");
        Cheese f1 = (Cheese) populatedData.get("f1");
        Cheese f2 = (Cheese) populatedData.get("f2");
        Cheese f3 = (Cheese) populatedData.get("f3");

        assertEquals(3, listChesse.getCheeses().size());
        assertTrue(listChesse.getCheeses().contains(f1));
        assertTrue(listChesse.getCheeses().contains(f2));
        assertTrue(listChesse.getCheeses().contains(f3));

    }

    @Test
    public void testCollection() throws Exception {
        TypeResolver typeResolver = getTypeResolver();

        List<Field> fieldData = new ArrayList<Field>();
        CollectionFieldData collectionFieldData = new CollectionFieldData();
        collectionFieldData.setName("cheeses");
        fieldData.add(collectionFieldData);
        collectionFieldData.getCollectionFieldList().add(new FieldData("cheeses", "=cheese1"));
        collectionFieldData.getCollectionFieldList().add(new FieldData("cheeses", "=cheese2"));

        FactData cheeseryFactData = new FactData(
                "Cheesery",
                "cheesery",
                fieldData,
                false);

        FactData cheeseFactData1 = new FactData(
                "Cheese",
                "cheese1",
                Collections.<Field> emptyList(),
                false
        );
        FactData cheeseFactData2 = new FactData(
                "Cheese",
                "cheese2",
                Collections.<Field> emptyList(),
                false
        );

        factPopulator.add(new NewFactPopulator(
                populatedData,
                typeResolver,
                Thread.currentThread().getContextClassLoader(),
                cheeseryFactData));

        factPopulator.add(new NewFactPopulator(
                populatedData,
                typeResolver,
                Thread.currentThread().getContextClassLoader(),
                cheeseFactData1));

        factPopulator.add(new NewFactPopulator(
                populatedData,
                typeResolver,
                Thread.currentThread().getContextClassLoader(),
                cheeseFactData2));

        factPopulator.populate();

        assertTrue(populatedData.containsKey("cheesery"));
        Cheesery cheesery = (Cheesery) populatedData.get("cheesery");
        assertNotNull(cheesery);

        assertEquals(2, cheesery.getCheeses().size());
        assertNotNull(cheesery.getCheeses().get(0));
        assertTrue(cheesery.getCheeses().get(0) instanceof Cheese);
        assertNotNull(cheesery.getCheeses().get(1));
        assertTrue(cheesery.getCheeses().get(1) instanceof Cheese);
    }

    @Test
    public void testCollectionSums() throws Exception {
        TypeResolver typeResolver = getTypeResolver();

        List<Field> fieldData = new ArrayList<Field>();
        CollectionFieldData collectionFieldData = new CollectionFieldData();
        collectionFieldData.setName("list");
        fieldData.add(collectionFieldData);
        collectionFieldData.getCollectionFieldList().add(new FieldData("list", "=1+3"));

        FactData wrapperFactData = new FactData(
                "MyCollectionWrapper",
                "wrapper",
                fieldData,
                false);

        factPopulator.add(new NewFactPopulator(
                populatedData,
                typeResolver,
                Thread.currentThread().getContextClassLoader(),
                wrapperFactData));


        factPopulator.populate();

        assertTrue(populatedData.containsKey("wrapper"));
        MyCollectionWrapper wrapper = (MyCollectionWrapper) populatedData.get("wrapper");
        assertNotNull(wrapper);

        assertEquals(1, wrapper.getList().size());
        assertNotNull(wrapper.getList().get(0));
        assertEquals(4, wrapper.getList().get(0));
    }

    private TypeResolver getTypeResolver() {
        TypeResolver resolver = new ClassTypeResolver(new HashSet<String>(), getClassLoader());
        resolver.addImport("org.drools.guvnor.models.testscenarios.backend.MyCollectionWrapper");
        resolver.addImport("org.drools.guvnor.models.testscenarios.backend.Cheesery");
        resolver.addImport("org.drools.guvnor.models.testscenarios.backend.Cheese");
        resolver.addImport("org.drools.guvnor.models.testscenarios.backend.SqlDateWrapper");
        resolver.addImport("org.drools.guvnor.models.testscenarios.backend.CheeseType");
        resolver.addImport("org.drools.guvnor.models.testscenarios.backend.Person");
        resolver.addImport("org.drools.guvnor.models.testscenarios.backend.OuterFact");
        return resolver;
    }

    private ClassLoader getClassLoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return classLoader;
    }

}
