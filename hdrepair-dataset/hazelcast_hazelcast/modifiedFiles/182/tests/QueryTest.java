/*
 * Copyright (c) 2008-2013, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.map.finalTest;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MapIndexConfig;
import com.hazelcast.core.*;
import com.hazelcast.impl.GroupProperties;
import com.hazelcast.instance.StaticNodeFactory;
import com.hazelcast.map.*;
import com.hazelcast.query.*;
import com.hazelcast.util.Clock;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.hazelcast.instance.TestUtil;


import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class QueryTest extends TestUtil {

    @BeforeClass
    public static void init() throws Exception {
        System.setProperty(GroupProperties.PROP_WAIT_SECONDS_BEFORE_JOIN, "1");
        Hazelcast.shutdownAll();
    }

    @After
    public void cleanUp() {
        Hazelcast.shutdownAll();
    }

    @Test
    public void issue393() {
        StaticNodeFactory nodeFactory = new StaticNodeFactory(1);
        HazelcastInstance instance = nodeFactory.newHazelcastInstance(new Config());
        final IMap<String, Value> map = instance.getMap("default");
        map.addIndex("name", true);
        for (int i = 0; i < 4; i++) {
            final Value v = new Value("name" + i);
            map.put("" + i, v);
        }
        final Predicate predicate = new PredicateBuilder().getEntryObject().get("name").in("name0", "name2");
        final Collection<Value> values = map.values(predicate);
        final String[] expectedValues = new String[]{"name0", "name2"};
        assertEquals(expectedValues.length, values.size());
        final List<String> names = new ArrayList<String>();
        for (final Value configObject : values) {
            names.add(configObject.name);
        }
        final String[] array = names.toArray(new String[0]);
        Arrays.sort(array);
        assertArrayEquals(names.toString(), expectedValues, array);
    }

    @Test
    public void issue393Fail() {
        StaticNodeFactory nodeFactory = new StaticNodeFactory(1);
        HazelcastInstance instance = nodeFactory.newHazelcastInstance(new Config());
        final IMap<String, Value> map = instance.getMap("default");
        map.addIndex("qwe", true);
        final Value v = new Value("name");
        try {
            map.put("0", v);
            fail();
        } catch (Throwable e) {
            assertTrue(e.getMessage().contains("There is no suitable accessor for 'qwe'"));
        }
    }

    @Test
    public void negativeDouble() {
        StaticNodeFactory nodeFactory = new StaticNodeFactory(1);
        HazelcastInstance instance = nodeFactory.newHazelcastInstance(new Config());
        final IMap<String, Employee> map = instance.getMap("default");
        map.addIndex("salary", false);
        map.put("" + 4, new Employee(1, "default", 1, true, -70D));
        map.put("" + 3, new Employee(1, "default", 1, true, -60D));
        map.put("" + 1, new Employee(1, "default", 1, true, -10D));
        map.put("" + 2, new Employee(2, "default", 2, true, 10D));
        Predicate predicate = new SqlPredicate("salary >= -60");
        Collection<Employee> values = map.values(predicate);
        assertEquals(3, values.size());
        predicate = new SqlPredicate("salary between -20 and 20");
        values = map.values(predicate);
        assertEquals(2, values.size());
    }

    @Test
    public void issue393SqlEq() {
        StaticNodeFactory nodeFactory = new StaticNodeFactory(1);
        HazelcastInstance instance = nodeFactory.newHazelcastInstance(new Config());
        final IMap<String, Value> map = instance.getMap("default");
        map.addIndex("name", true);
        for (int i = 0; i < 4; i++) {
            final Value v = new Value("name" + i);
            map.put("" + i, v);
        }
        final Predicate predicate = new SqlPredicate("name='name0'");
        final Collection<Value> values = map.values(predicate);
        final String[] expectedValues = new String[]{"name0"};
        assertEquals(expectedValues.length, values.size());
        final List<String> names = new ArrayList<String>();
        for (final Value configObject : values) {
            names.add(configObject.name);
        }
        final String[] array = names.toArray(new String[0]);
        Arrays.sort(array);
        assertArrayEquals(names.toString(), expectedValues, array);
    }

    @Test
    public void issue393SqlIn() {
        StaticNodeFactory nodeFactory = new StaticNodeFactory(1);
        HazelcastInstance instance = nodeFactory.newHazelcastInstance(new Config());
        final IMap<String, Value> map = instance.getMap("default");
        map.addIndex("name", true);
        for (int i = 0; i < 4; i++) {
            final Value v = new Value("name" + i);
            map.put("" + i, v);
        }
        final Predicate predicate = new SqlPredicate("name IN ('name0', 'name2')");
        final Collection<Value> values = map.values(predicate);
        final String[] expectedValues = new String[]{"name0", "name2"};
        assertEquals(expectedValues.length, values.size());
        final List<String> names = new ArrayList<String>();
        for (final Value configObject : values) {
            names.add(configObject.name);
        }
        final String[] array = names.toArray(new String[0]);
        Arrays.sort(array);
        assertArrayEquals(names.toString(), expectedValues, array);
    }

    @Test
    public void issue393SqlInInteger() {
        StaticNodeFactory nodeFactory = new StaticNodeFactory(1);
        HazelcastInstance instance = nodeFactory.newHazelcastInstance(new Config());
        final IMap<String, Value> map = instance.getMap("default");
        map.addIndex("index", false);
        for (int i = 0; i < 4; i++) {
            final Value v = new Value("name" + i, new ValueType("type" + i), i);
            map.put("" + i, v);
        }
        final Predicate predicate = new SqlPredicate("index IN (0, 2)");
        final Collection<Value> values = map.values(predicate);
        final String[] expectedValues = new String[]{"name0", "name2"};
        assertEquals(expectedValues.length, values.size());
        final List<String> names = new ArrayList<String>();
        for (final Value configObject : values) {
            names.add(configObject.name);
        }
        final String[] array = names.toArray(new String[0]);
        Arrays.sort(array);
        assertArrayEquals(names.toString(), expectedValues, array);
    }

    @Test
    public void testIteratorContract() {
        StaticNodeFactory nodeFactory = new StaticNodeFactory(1);
        HazelcastInstance instance = nodeFactory.newHazelcastInstance(new Config());
        final IMap<String, ValueType> map = instance.getMap("testIteratorContract");
        map.put("1", new ValueType("one"));
        map.put("2", new ValueType("two"));
        map.put("3", new ValueType("three"));
        final Predicate predicate = new SqlPredicate("typeName in ('one','two')");
//        testIterator(map.keySet().iterator(), 3);
        System.out.println("sz keyset:" + map.keySet(predicate).size());
        System.out.println("sz entryset:" + map.entrySet(predicate).size());
        System.out.println("sz values:" + map.values(predicate).size());

        System.out.println("sz keyset iter:" + map.keySet(predicate).iterator());

        testIterator(map.keySet(predicate).iterator(), 2);
        testIterator(map.entrySet().iterator(), 3);
        testIterator(map.entrySet(predicate).iterator(), 2);
        testIterator(map.values().iterator(), 3);
        testIterator(map.values(predicate).iterator(), 2);
    }

    private void testIterator(final Iterator it, int size) {
        for (int i = 0; i < size * 2; i++) {
            assertTrue("i is " + i, it.hasNext());
        }
        for (int i = 0; i < size; i++) {
            assertTrue(it.hasNext());
            assertNotNull(it.next());
        }
        assertFalse(it.hasNext());
        assertFalse(it.hasNext());
    }

    @Test
    public void testInnerIndex() {
        StaticNodeFactory nodeFactory = new StaticNodeFactory(1);
        HazelcastInstance instance = nodeFactory.newHazelcastInstance(new Config());
        final IMap<String, Value> map = instance.getMap("default");
        map.addIndex("name", false);
        map.addIndex("type.typeName", false);
        for (int i = 0; i < 10; i++) {
            final Value v = new Value("name" + i, i < 5 ? null : new ValueType("type" + i), i);
            map.put("" + i, v);
        }
        final Predicate predicate = new PredicateBuilder().getEntryObject().get("type.typeName").in("type8", "type6");
        final Collection<Value> values = map.values(predicate);
        assertEquals(2, values.size());
        final List<String> typeNames = new ArrayList<String>();
        for (final Value configObject : values) {
            typeNames.add(configObject.getType().getTypeName());
        }
        final String[] array = typeNames.toArray(new String[0]);
        Arrays.sort(array);
        assertArrayEquals(typeNames.toString(), new String[]{"type6", "type8"}, array);
    }

    @Test
    public void testInnerIndexSql() {
        StaticNodeFactory nodeFactory = new StaticNodeFactory(1);
        HazelcastInstance instance = nodeFactory.newHazelcastInstance(new Config());
        final IMap<String, Value> map = instance.getMap("default");
        map.addIndex("name", false);
        map.addIndex("type.typeName", false);
        for (int i = 0; i < 4; i++) {
            final Value v = new Value("name" + i, new ValueType("type" + i), i);
            map.put("" + i, v);
        }
        final Predicate predicate = new SqlPredicate("type.typeName='type1'");
        final Collection<Value> values = map.values(predicate);
        assertEquals(1, values.size());
        final List<String> typeNames = new ArrayList<String>();
        for (final Value configObject : values) {
            typeNames.add(configObject.getType().getTypeName());
        }
        assertArrayEquals(typeNames.toString(), new String[]{"type1"}, typeNames.toArray(new String[0]));
    }

    @Test
    public void testQueryWithTTL() throws Exception {
        StaticNodeFactory nodeFactory = new StaticNodeFactory(2);
        Config cfg = new Config();
        MapConfig mapConfig = new MapConfig();
        int TTL = 2;
        mapConfig.setTimeToLiveSeconds(TTL);
        mapConfig.setName("employees");
        cfg.addMapConfig(mapConfig);
        HazelcastInstance h1 = nodeFactory.newHazelcastInstance(cfg);
        HazelcastInstance h2 = nodeFactory.newHazelcastInstance(cfg);
        IMap imap = h1.getMap("employees");
        imap.addIndex("name", false);
        imap.addIndex("age", true);
        imap.addIndex("active", false);
        int expectedCount = 0;
        for (int i = 0; i < 1000; i++) {
            Employee employee = new Employee("joe" + i, i % 60, ((i & 1) == 1), Double.valueOf(i));
            if (employee.getName().startsWith("joe15") && employee.isActive()) {
                expectedCount++;
                System.out.println(employee);
            }
            imap.put(String.valueOf(i), employee);
        }
        Collection<Employee> values = imap.values(new SqlPredicate("active and name LIKE 'joe15%'"));
        for (Employee employee : values) {
//            System.out.println(employee);
            assertTrue(employee.isActive());
        }
        assertEquals(expectedCount, values.size());
        Thread.sleep((TTL + 1) * 1000);
        assertEquals(0, imap.size());
        values = imap.values(new SqlPredicate("active and name LIKE 'joe15%'"));
        assertEquals(0, values.size());
        Thread.sleep(5000);
        values = imap.values(new SqlPredicate("active and name LIKE 'joe15%'"));
        assertEquals(0, values.size());
    }

    @Test
    public void testOneIndexedFieldsWithTwoCriteriaField() throws Exception {
        StaticNodeFactory nodeFactory = new StaticNodeFactory(1);
        HazelcastInstance h1 = nodeFactory.newHazelcastInstance(new Config());
        IMap imap = h1.getMap("employees");
        imap.addIndex("name", false);
//        imap.addIndex("age", false);
        imap.put("1", new Employee(1L, "joe", 30, true, 100D));
        EntryObject e = new PredicateBuilder().getEntryObject();
        PredicateBuilder a = e.get("name").equal("joe");
        Predicate b = e.get("age").equal("30");
        final Collection<Object> actual = imap.values(a.and(b));
        assertEquals(1, actual.size());
    }

    @Test
    public void testQueryDuringAndAfterMigrationWithIndex() throws Exception {
        Config cfg = new Config();
        StaticNodeFactory nodeFactory = new StaticNodeFactory(4);
        HazelcastInstance h1 = nodeFactory.newHazelcastInstance(cfg);
        IMap imap = h1.getMap("employees");
        imap.addIndex("name", false);
        imap.addIndex("age", true);
        imap.addIndex("active", false);
        for (int i = 0; i < 10000; i++) {
            imap.put(String.valueOf(i), new Employee("joe" + i, i % 60, ((i & 1) == 1), Double.valueOf(i)));
        }
        HazelcastInstance h2 = nodeFactory.newHazelcastInstance(cfg);
        HazelcastInstance h3 = nodeFactory.newHazelcastInstance(cfg);
        HazelcastInstance h4 = nodeFactory.newHazelcastInstance(cfg);
        long startNow = Clock.currentTimeMillis();
        while ((Clock.currentTimeMillis() - startNow) < 50000) {
            Collection<Employee> values = imap.values(new SqlPredicate("active and name LIKE 'joe15%'"));
            for (Employee employee : values) {
                assertTrue(employee.isActive());
            }
            assertEquals(56, values.size());
        }
    }

    @Test
    public void testQueryDuringAndAfterMigration() throws Exception {
        Config cfg = new Config();
        StaticNodeFactory nodeFactory = new StaticNodeFactory(4);
        HazelcastInstance h1 = nodeFactory.newHazelcastInstance(cfg);
        int count = 100000;
        IMap imap = h1.getMap("values");
        for (int i = 0; i < count; i++) {
            imap.put(i, i);
        }
        HazelcastInstance h2 = nodeFactory.newHazelcastInstance(cfg);
        HazelcastInstance h3 = nodeFactory.newHazelcastInstance(cfg);
        HazelcastInstance h4 = nodeFactory.newHazelcastInstance(cfg);
        long startNow = Clock.currentTimeMillis();
        while ((Clock.currentTimeMillis() - startNow) < 50000) {
            Collection<Employee> values = imap.values();
            assertEquals(count, values.size());
        }
    }

    @Test
    public void testTwoNodesWithPartialIndexes() throws Exception {
        Config cfg = new Config();
        StaticNodeFactory nodeFactory = new StaticNodeFactory(2);
        HazelcastInstance h1 = nodeFactory.newHazelcastInstance(cfg);
        HazelcastInstance h2 = nodeFactory.newHazelcastInstance(cfg);
        IMap imap = h1.getMap("employees");
        imap.addIndex("name", false);
        imap.addIndex("age", true);
        imap.addIndex("active", false);
        for (int i = 0; i < 5000; i++) {
            Employee employee = new Employee(i, "name" + i % 100, "city" + (i % 100), i % 60, ((i & 1) == 1), Double.valueOf(i));
            imap.put(String.valueOf(i), employee);
        }
        assertEquals(2, h1.getCluster().getMembers().size());
        assertEquals(2, h2.getCluster().getMembers().size());
        imap = h2.getMap("employees");
        imap.addIndex("name", false);
        imap.addIndex("age", true);
        imap.addIndex("active", false);
        Collection<Employee> entries = imap.values(new SqlPredicate("name='name3' and city='city3' and age > 2"));
        assertEquals(50, entries.size());
        for (Employee e : entries) {
            assertEquals("name3", e.getName());
            assertEquals("city3", e.getCity());
        }
        entries = imap.values(new SqlPredicate("name LIKE '%name3' and city like '%city3' and age > 2"));
        assertEquals(50, entries.size());
        for (Employee e : entries) {
            assertEquals("name3", e.getName());
            assertEquals("city3", e.getCity());
            assertTrue(e.getAge() > 2);
        }
        entries = imap.values(new SqlPredicate("name LIKE '%name3%' and city like '%city30%'"));
        assertEquals(50, entries.size());
        for (Employee e : entries) {
            assertTrue(e.getName().startsWith("name3"));
            assertTrue(e.getCity().startsWith("city3"));
        }
    }

    @Test
    public void testTwoNodesWithIndexes() throws Exception {
        Config cfg = new Config();
        StaticNodeFactory nodeFactory = new StaticNodeFactory(2);
        HazelcastInstance h1 = nodeFactory.newHazelcastInstance(cfg);
        HazelcastInstance h2 = nodeFactory.newHazelcastInstance(cfg);
        IMap imap = h1.getMap("employees");
        imap.addIndex("name", false);
        imap.addIndex("city", false);
        imap.addIndex("age", true);
        imap.addIndex("active", false);
        for (int i = 0; i < 5000; i++) {
            Employee employee = new Employee(i, "name" + i % 100, "city" + (i % 100), i % 60, ((i & 1) == 1), Double.valueOf(i));
            imap.put(String.valueOf(i), employee);
        }
        assertEquals(2, h1.getCluster().getMembers().size());
        assertEquals(2, h2.getCluster().getMembers().size());
        imap = h2.getMap("employees");
        imap.addIndex("name", false);
        imap.addIndex("city", false);
        imap.addIndex("age", true);
        imap.addIndex("active", false);
        Collection<Employee> entries = imap.values(new SqlPredicate("name='name3' and city='city3' and age > 2"));
        assertEquals(50, entries.size());
        for (Employee e : entries) {
            assertEquals("name3", e.getName());
            assertEquals("city3", e.getCity());
        }
        entries = imap.values(new SqlPredicate("name LIKE '%name3' and city like '%city3' and age > 2"));
        assertEquals(50, entries.size());
        for (Employee e : entries) {
            assertEquals("name3", e.getName());
            assertEquals("city3", e.getCity());
            assertTrue(e.getAge() > 2);
        }
        entries = imap.values(new SqlPredicate("name LIKE '%name3%' and city like '%city30%'"));
        assertEquals(50, entries.size());
        for (Employee e : entries) {
            assertTrue(e.getName().startsWith("name3"));
            assertTrue(e.getCity().startsWith("city3"));
        }
    }

    @Test
    public void testQueryWithIndexesWhileMigrating() throws Exception {
        Config cfg = new Config();
        StaticNodeFactory nodeFactory = new StaticNodeFactory(4);
        HazelcastInstance h1 = nodeFactory.newHazelcastInstance(cfg);
        IMap imap = h1.getMap("employees");
        imap.addIndex("name", false);
        imap.addIndex("age", true);
        imap.addIndex("active", false);
        for (int i = 0; i < 500; i++) {
            Map temp = new HashMap(100);
            for (int j = 0; j < 100; j++) {
                String key = String.valueOf((i * 100000) + j);
                temp.put(key, new Employee("name" + key, i % 60, ((i & 1) == 1), Double.valueOf(i)));
            }
            imap.putAll(temp);
        }
        assertEquals(50000, imap.size());
        HazelcastInstance h2 = nodeFactory.newHazelcastInstance(cfg);
        HazelcastInstance h3 = nodeFactory.newHazelcastInstance(cfg);
        HazelcastInstance h4 = nodeFactory.newHazelcastInstance(cfg);
        for (int i = 0; i < 1; i++) {
            Set<Map.Entry> entries = imap.entrySet(new SqlPredicate("active=true and age>44"));
            assertEquals(6400, entries.size());
        }
    }


    @Test
    public void testOneMemberWithoutIndex() {
        Config cfg = new Config();
        StaticNodeFactory nodeFactory = new StaticNodeFactory(4);
        HazelcastInstance h1 = nodeFactory.newHazelcastInstance(cfg);
        IMap imap = h1.getMap("employees");
        doFunctionalQueryTest(imap);
    }

    @Test
    public void testOneMemberWithIndex() {
        Config cfg = new Config();
        StaticNodeFactory nodeFactory = new StaticNodeFactory(1);
        HazelcastInstance instance = nodeFactory.newHazelcastInstance(cfg);
        IMap imap = instance.getMap("employees");
        imap.addIndex("name", false);
        imap.addIndex("age", true);
        imap.addIndex("active", false);
        doFunctionalQueryTest(imap);
    }

    @Test
    public void testOneMemberSQLWithoutIndex() {
        Config cfg = new Config();
        StaticNodeFactory nodeFactory = new StaticNodeFactory(4);
        HazelcastInstance h1 = nodeFactory.newHazelcastInstance(cfg);
        IMap imap = h1.getMap("employees");
        doFunctionalSQLQueryTest(imap);
        Set<Map.Entry> entries = imap.entrySet(new SqlPredicate("active and age>23"));
        assertEquals(27, entries.size());
    }

    @Test
    public void testOneMemberSQLWithIndex() {
        Config cfg = new Config();
        StaticNodeFactory nodeFactory = new StaticNodeFactory(4);
        HazelcastInstance h1 = nodeFactory.newHazelcastInstance(cfg);
        IMap imap = h1.getMap("employees");
        imap.addIndex("name", false);
        imap.addIndex("age", true);
        imap.addIndex("active", false);
        doFunctionalSQLQueryTest(imap);
    }

    @Test
    public void testIndexSQLPerformance() {
        Config cfg = new Config();
        StaticNodeFactory nodeFactory = new StaticNodeFactory(4);
        HazelcastInstance h1 = nodeFactory.newHazelcastInstance(cfg);
        IMap imap = h1.getMap("employees");
        for (int i = 0; i < 5000; i++) {
            imap.put(String.valueOf(i), new Employee("name" + i, i % 60, ((i & 1) == 1), Double.valueOf(i)));
        }
        long start = Clock.currentTimeMillis();
        Set<Map.Entry> entries = imap.entrySet(new SqlPredicate("active=true and age=23"));
        long tookWithout = (Clock.currentTimeMillis() - start);
        assertEquals(83, entries.size());
        for (Map.Entry entry : entries) {
            Employee c = (Employee) entry.getValue();
            assertEquals(c.getAge(), 23);
            assertTrue(c.isActive());
        }
        imap.clear();
        imap = h1.getMap("employees2");
        imap.addIndex("name", false);
        imap.addIndex("age", true);
        imap.addIndex("active", false);
        for (int i = 0; i < 5000; i++) {
            imap.put(String.valueOf(i), new Employee("name" + i, i % 60, ((i & 1) == 1), Double.valueOf(i)));
        }
        start = Clock.currentTimeMillis();
        entries = imap.entrySet(new SqlPredicate("active and age=23"));
        long tookWithIndex = (Clock.currentTimeMillis() - start);
        assertEquals(83, entries.size());
        for (Map.Entry entry : entries) {
            Employee c = (Employee) entry.getValue();
            assertEquals(c.getAge(), 23);
            assertTrue(c.isActive());
        }
        assertTrue(tookWithIndex < (tookWithout / 2));
    }

    // todo fails
    @Test
    public void testRangeIndexSQLPerformance() {
        Config cfg = new Config();
        StaticNodeFactory nodeFactory = new StaticNodeFactory(4);
        HazelcastInstance h1 = nodeFactory.newHazelcastInstance(cfg);
        IMap imap = h1.getMap("employees");
        for (int i = 0; i < 50000; i++) {
            imap.put(String.valueOf(i), new Employee("name" + i, i % 60, ((i & 1) == 1), Double.valueOf(i)));
        }
        long start = Clock.currentTimeMillis();
        Set<Map.Entry> entries = imap.entrySet(new SqlPredicate("active and salary between 4010.99 and 4032.01"));
        long tookWithout = (Clock.currentTimeMillis() - start);
        assertEquals(11, entries.size());
        for (Map.Entry entry : entries) {
            Employee c = (Employee) entry.getValue();
            assertTrue(c.getAge() < 4033);
            assertTrue(c.isActive());
        }
        imap.clear();
        imap = h1.getMap("employees2");
        imap.addIndex("name", false);
        imap.addIndex("salary", false);
        imap.addIndex("active", false);
        for (int i = 0; i < 50000; i++) {
            imap.put(String.valueOf(i), new Employee("name" + i, i % 60, ((i & 1) == 1), Double.valueOf(i)));
        }
        imap.put(String.valueOf(10), new Employee("name" + 10, 10, true, 44010.99D));
        imap.put(String.valueOf(11), new Employee("name" + 11, 11, true, 44032.01D));
        start = Clock.currentTimeMillis();
        entries = imap.entrySet(new SqlPredicate("active and salary between 44010.99 and 44032.01"));
        long tookWithIndex = (Clock.currentTimeMillis() - start);
        assertEquals(13, entries.size());
        boolean foundFirst = false;
        boolean foundLast = false;
        for (Map.Entry entry : entries) {
            Employee c = (Employee) entry.getValue();
            assertTrue(c.getAge() < 44033);
            assertTrue(c.isActive());
            if (c.getSalary() == 44010.99D) {
                foundFirst = true;
            } else if (c.getSalary() == 44032.01D) {
                foundLast = true;
            }
        }
        assertTrue(foundFirst);
        assertTrue(foundLast);
        System.out.println(tookWithIndex + " vs. " + tookWithout);
        assertTrue(tookWithIndex < (tookWithout / 2));
        for (int i = 0; i < 50000; i++) {
            imap.put(String.valueOf(i), new Employee("name" + i, i % 60, ((i & 1) == 1), 100.25D));
        }
        entries = imap.entrySet(new SqlPredicate("salary between 99.99 and 100.25"));
        assertEquals(50000, entries.size());
        for (Map.Entry entry : entries) {
            Employee c = (Employee) entry.getValue();
            assertTrue(c.getSalary() == 100.25D);
        }
    }

    @Test
    public void testIndexPerformance() {
        Config cfg = new Config();
        StaticNodeFactory nodeFactory = new StaticNodeFactory(4);
        HazelcastInstance h1 = nodeFactory.newHazelcastInstance(cfg);
        IMap imap = h1.getMap("employees");
        for (int i = 0; i < 5000; i++) {
            imap.put(String.valueOf(i), new Employee("name" + i, i % 60, ((i & 1) == 1), Double.valueOf(i)));
        }
        EntryObject e = new PredicateBuilder().getEntryObject();
        Predicate predicate = e.is("active").and(e.get("age").equal(23));
        long start = Clock.currentTimeMillis();
        Set<Map.Entry> entries = imap.entrySet(predicate);
        long tookWithout = (Clock.currentTimeMillis() - start);
        assertEquals(83, entries.size());
        for (Map.Entry entry : entries) {
            Employee c = (Employee) entry.getValue();
            assertEquals(c.getAge(), 23);
            assertTrue(c.isActive());
        }
        imap.clear();
        imap = h1.getMap("employees2");
        imap.addIndex("name", false);
        imap.addIndex("age", true);
        imap.addIndex("active", false);
        for (int i = 0; i < 5000; i++) {
            imap.put(String.valueOf(i), new Employee("name" + i, i % 60, ((i & 1) == 1), Double.valueOf(i)));
        }
        e = new PredicateBuilder().getEntryObject();
        predicate = e.is("active").and(e.get("age").equal(23));
        start = Clock.currentTimeMillis();
        entries = imap.entrySet(predicate);
        long tookWithIndex = (Clock.currentTimeMillis() - start);
        assertEquals(83, entries.size());
        for (Map.Entry entry : entries) {
            Employee c = (Employee) entry.getValue();
            assertEquals(c.getAge(), 23);
            assertTrue(c.isActive());
        }
        assertTrue(tookWithIndex < (tookWithout / 2));
    }

    @Test
    public void testNullIndexing() {
        Config cfg = new Config();
        StaticNodeFactory nodeFactory = new StaticNodeFactory(2);
        HazelcastInstance h1 = nodeFactory.newHazelcastInstance(cfg);
        HazelcastInstance h2 = nodeFactory.newHazelcastInstance(cfg);
        IMap imap1 = h1.getMap("employees");
        IMap imap2 = h2.getMap("employees");
        for (int i = 0; i < 5000; i++) {
            imap1.put(String.valueOf(i), new Employee((i % 2 == 0) ? null : "name" + i, i % 60, true, Double.valueOf(i)));
        }
        EntryObject e = new PredicateBuilder().getEntryObject();
        Predicate predicate = e.is("active").and(e.get("name").equal(null));
        long start = Clock.currentTimeMillis();
        Set<Map.Entry> entries = imap2.entrySet(predicate);
        long tookWithout = (Clock.currentTimeMillis() - start);
        assertEquals(2500, entries.size());
        for (Map.Entry entry : entries) {
            Employee c = (Employee) entry.getValue();
            assertNull(c.getName());
        }
        imap1.destroy();
        imap1 = h1.getMap("employees2");
        imap2 = h2.getMap("employees2");
        imap1.addIndex("name", false);
        imap1.addIndex("age", true);
        imap1.addIndex("active", false);
        for (int i = 0; i < 5000; i++) {
            imap1.put(String.valueOf(i), new Employee((i % 2 == 0) ? null : "name" + i, i % 60, true, Double.valueOf(i)));
        }
        e = new PredicateBuilder().getEntryObject();
        predicate = e.is("active").and(e.get("name").equal(null));
        start = Clock.currentTimeMillis();
        entries = imap2.entrySet(predicate);
        long tookWithIndex = (Clock.currentTimeMillis() - start);
        assertEquals(2500, entries.size());
        for (Map.Entry entry : entries) {
            Employee c = (Employee) entry.getValue();
            assertNull(c.getName());
        }
        assertTrue("WithIndex: " + tookWithIndex + ", without: " + tookWithout, tookWithIndex < tookWithout);
    }

    @Test
    public void testIndexPerformanceUsingPredicate() {
        Config cfg = new Config();
        StaticNodeFactory nodeFactory = new StaticNodeFactory(2);
        HazelcastInstance h1 = nodeFactory.newHazelcastInstance(cfg);
        IMap imap = h1.getMap("employees");
        for (int i = 0; i < 5000; i++) {
            imap.put(String.valueOf(i), new Employee("name" + i, i % 60, ((i & 1) == 1), Double.valueOf(i)));
        }
        EntryObject e = new PredicateBuilder().getEntryObject();
        Predicate predicate = e.is("active").and(e.get("age").equal(23));
        long start = Clock.currentTimeMillis();
        Set<Map.Entry> entries = imap.entrySet(predicate);
        long tookWithout = (Clock.currentTimeMillis() - start);
        assertEquals(83, entries.size());
        for (Map.Entry entry : entries) {
            Employee c = (Employee) entry.getValue();
            assertEquals(c.getAge(), 23);
            assertTrue(c.isActive());
        }
        imap.clear();
        imap = h1.getMap("employees2");
        imap.addIndex("name", false);
        imap.addIndex("active", false);
        imap.addIndex("age", true);
        for (int i = 0; i < 5000; i++) {
            imap.put(String.valueOf(i), new Employee("name" + i, i % 60, ((i & 1) == 1), Double.valueOf(i)));
        }
        e = new PredicateBuilder().getEntryObject();
        predicate = e.is("active").and(e.get("age").equal(23));
        start = Clock.currentTimeMillis();
        entries = imap.entrySet(predicate);
        long tookWithIndex = (Clock.currentTimeMillis() - start);
        assertEquals(83, entries.size());
        for (Map.Entry entry : entries) {
            Employee c = (Employee) entry.getValue();
            assertEquals(c.getAge(), 23);
            assertTrue(c.isActive());
        }
        assertTrue(tookWithIndex < (tookWithout / 2));
    }

    @Test
    public void testTwoMembers() {
        Config cfg = new Config();
        StaticNodeFactory nodeFactory = new StaticNodeFactory(2);
        HazelcastInstance h1 = nodeFactory.newHazelcastInstance(cfg);
        HazelcastInstance h2 = nodeFactory.newHazelcastInstance(cfg);
        IMap imap = h1.getMap("employees");
        doFunctionalQueryTest(imap);
    }

    @Test
    public void testTwoMembersWithIndexes() {
        Config cfg = new Config();
        StaticNodeFactory nodeFactory = new StaticNodeFactory(2);
        HazelcastInstance h1 = nodeFactory.newHazelcastInstance(cfg);
        HazelcastInstance h2 = nodeFactory.newHazelcastInstance(cfg);
        IMap imap = h1.getMap("employees");
        imap.addIndex("name", false);
        imap.addIndex("age", true);
        imap.addIndex("active", false);
        doFunctionalQueryTest(imap);
    }

    @Test
    public void testTwoMembersWithIndexesAndShutdown() {
        Config cfg = new Config();
        StaticNodeFactory nodeFactory = new StaticNodeFactory(2);
        HazelcastInstance h1 = nodeFactory.newHazelcastInstance(cfg);
        HazelcastInstance h2 = nodeFactory.newHazelcastInstance(cfg);
        IMap imap = h1.getMap("employees");
        imap.addIndex("name", false);
        imap.addIndex("age", true);
        imap.addIndex("active", false);
        doFunctionalQueryTest(imap);
        assertEquals(101, imap.size());
        h2.getLifecycleService().shutdown();
        assertEquals(101, imap.size());
        Set<Map.Entry> entries = imap.entrySet(new SqlPredicate("active and age=23"));
        assertEquals(2, entries.size());
        for (Map.Entry entry : entries) {
            Employee c = (Employee) entry.getValue();
            assertEquals(c.getAge(), 23);
            assertTrue(c.isActive());
        }
    }

    @Test
    public void testTwoMembersWithIndexesAndShutdown2() {
        Config cfg = new Config();
        StaticNodeFactory nodeFactory = new StaticNodeFactory(2);
        HazelcastInstance h1 = nodeFactory.newHazelcastInstance(cfg);
        HazelcastInstance h2 = nodeFactory.newHazelcastInstance(cfg);
        IMap imap = h1.getMap("employees");
        imap.addIndex("name", false);
        imap.addIndex("age", true);
        imap.addIndex("active", false);
        doFunctionalQueryTest(imap);
        assertEquals(101, imap.size());
        h1.getLifecycleService().shutdown();
        imap = h2.getMap("employees");
        assertEquals(101, imap.size());
        Set<Map.Entry> entries = imap.entrySet(new SqlPredicate("active and age=23"));
        assertEquals(2, entries.size());
        for (Map.Entry entry : entries) {
            Employee c = (Employee) entry.getValue();
            assertEquals(c.getAge(), 23);
            assertTrue(c.isActive());
        }
    }

    @Test
    public void testTwoMembersWithIndexesAndShutdown3() {
        Config cfg = new Config();
        StaticNodeFactory nodeFactory = new StaticNodeFactory(2);
        HazelcastInstance h1 = nodeFactory.newHazelcastInstance(cfg);
        IMap imap = h1.getMap("employees");
        imap.addIndex("name", false);
        imap.addIndex("age", true);
        imap.addIndex("active", false);
        doFunctionalQueryTest(imap);
        assertEquals(101, imap.size());
        HazelcastInstance h2 = nodeFactory.newHazelcastInstance(cfg);
        assertEquals(101, imap.size());
        h1.getLifecycleService().shutdown();
        imap = h2.getMap("employees");
        assertEquals(101, imap.size());
        Set<Map.Entry> entries = imap.entrySet(new SqlPredicate("active and age=23"));
        assertEquals(2, entries.size());
        for (Map.Entry entry : entries) {
            Employee c = (Employee) entry.getValue();
            assertEquals(c.getAge(), 23);
            assertTrue(c.isActive());
        }
    }

    @Test
    public void testSecondMemberAfterAddingIndexes() {
        Config cfg = new Config();
        StaticNodeFactory nodeFactory = new StaticNodeFactory(2);
        HazelcastInstance h1 = nodeFactory.newHazelcastInstance(cfg);
        IMap imap = h1.getMap("employees");
        imap.addIndex("name", false);
        imap.addIndex("age", true);
        imap.addIndex("active", false);
        HazelcastInstance h2 = nodeFactory.newHazelcastInstance(cfg);
        doFunctionalQueryTest(imap);
    }

    @Test
    public void testWithDashInTheNameAndSqlPredicate() {
        Config cfg = new Config();
        StaticNodeFactory nodeFactory = new StaticNodeFactory(1);
        HazelcastInstance h1 = nodeFactory.newHazelcastInstance(cfg);
        IMap<String, Employee> map = h1.getMap("employee");
        Employee toto = new Employee("toto", 23, true, 165765.0);
        map.put("1", toto);
        Employee toto2 = new Employee("toto-super+hero", 23, true, 165765.0);
        map.put("2", toto2);
        //Works well
        Set<Map.Entry<String, Employee>> entries = map.entrySet(new SqlPredicate("name='toto-super+hero'"));
        assertTrue(entries.size() > 0);
        for (Map.Entry<String, Employee> entry : entries) {
            Employee e = entry.getValue();
            System.out.println(e);
            assertEquals(e, toto2);
        }
    }

    @Test
    public void queryWithThis() {
        Config cfg = new Config();
        StaticNodeFactory nodeFactory = new StaticNodeFactory(1);
        HazelcastInstance instance = nodeFactory.newHazelcastInstance(cfg);
        IMap<String, String> map = instance.getMap("queryWithThis");
        map.addIndex("this", false);
        for (int i = 0; i < 1000; i++) {
            map.put("" + i, "" + i);
        }
        final Predicate predicate = new PredicateBuilder().getEntryObject().get("this").equal("10");
        Collection<String> set = map.values(predicate);
        assertEquals(1, set.size());
        assertEquals(1, map.values(new SqlPredicate("this=15")).size());
    }

    /**
     * Test for issue 711
     */
    @Test
    public void testPredicateWithEntryKeyObject() {
        Config cfg = new Config();
        StaticNodeFactory nodeFactory = new StaticNodeFactory(1);
        HazelcastInstance instance = nodeFactory.newHazelcastInstance(cfg);
        IMap map = instance.getMap("testPredicateWithEntryKeyObject");
        map.put("1", 11);
        map.put("2", 22);
        map.put("3", 33);
        Predicate predicate = new PredicateBuilder().getEntryObject().key().equal("1");
        assertEquals(1, map.values(predicate).size());
        predicate = new PredicateBuilder().getEntryObject().key().in("2", "3");
        assertEquals(2, map.keySet(predicate).size());
    }

    /**
     * Github issues 98 and 131
     */
    @Test
    public void testPredicateStringAttribute() {
        Config cfg = new Config();
        StaticNodeFactory nodeFactory = new StaticNodeFactory(1);
        HazelcastInstance instance = nodeFactory.newHazelcastInstance(cfg);
        IMap map = instance.getMap("testPredicateStringWithString");
        testPredicateStringAttribute(map);
    }

    /**
     * Github issues 98 and 131
     */
    // todo fails
    @Test
    public void testPredicateStringAttributesWithIndex() {
        Config cfg = new Config();
        StaticNodeFactory nodeFactory = new StaticNodeFactory(1);
        HazelcastInstance instance = nodeFactory.newHazelcastInstance(cfg);
        IMap map = instance.getMap("testPredicateStringWithStringIndex");
        map.addIndex("name", false);
        testPredicateStringAttribute(map);
    }

    private void testPredicateStringAttribute(IMap map) {
        map.put(1, new Value("abc"));
        map.put(2, new Value("xyz"));
        map.put(3, new Value("aaa"));
        map.put(4, new Value("zzz"));
        map.put(5, new Value("klm"));
        map.put(6, new Value("prs"));
        map.put(7, new Value("prs"));
        map.put(8, new Value("def"));
        map.put(9, new Value("qwx"));
        assertEquals(8, map.values(new SqlPredicate("name > 'aac'")).size());
        assertEquals(9, map.values(new SqlPredicate("name between 'aaa' and 'zzz'")).size());
        assertEquals(7, map.values(new SqlPredicate("name < 't'")).size());
        assertEquals(6, map.values(new SqlPredicate("name >= 'gh'")).size());
        assertEquals(8, map.values(new PredicateBuilder().getEntryObject().get("name").greaterThan("aac")).size());
        assertEquals(9, map.values(new PredicateBuilder().getEntryObject().get("name").between("aaa", "zzz")).size());
        assertEquals(7, map.values(new PredicateBuilder().getEntryObject().get("name").lessThan("t")).size());
        assertEquals(6, map.values(new PredicateBuilder().getEntryObject().get("name").greaterEqual("gh")).size());
    }

    @Test
    public void testPredicateDateAttribute() {
        Config cfg = new Config();
        StaticNodeFactory nodeFactory = new StaticNodeFactory(1);
        HazelcastInstance instance = nodeFactory.newHazelcastInstance(cfg);
        IMap map = instance.getMap("testPredicateDateAttribute");
        testPredicateDateAttribute(map);
    }

    @Test
    public void testPredicateDateAttributeWithIndex() {
        Config cfg = new Config();
        StaticNodeFactory nodeFactory = new StaticNodeFactory(1);
        HazelcastInstance instance = nodeFactory.newHazelcastInstance(cfg);
        IMap map = instance.getMap("testPredicateDateAttribute");
        map.addIndex("this", true);
        testPredicateDateAttribute(map);
    }

    private void testPredicateDateAttribute(IMap map) {
        Calendar cal = Calendar.getInstance();
        cal.set(2012, 5, 5);
        map.put(1, cal.getTime());
        cal.set(2011, 10, 10);
        map.put(2, cal.getTime());
        cal.set(2011, 1, 1);
        map.put(3, cal.getTime());
        cal.set(2010, 8, 5);
        map.put(4, cal.getTime());
        cal.set(2000, 5, 5);
        map.put(5, cal.getTime());
        cal.set(2011, 0, 1);
        assertEquals(3, map.values(new PredicateBuilder().getEntryObject().get("this").greaterThan(cal.getTime())).size());
        assertEquals(3, map.values(new SqlPredicate("this > 'Sat Jan 01 11:43:05 EET 2011'")).size());
        assertEquals(2, map.values(new PredicateBuilder().getEntryObject().get("this").lessThan(cal.getTime())).size());
        assertEquals(2, map.values(new SqlPredicate("this < 'Sat Jan 01 11:43:05 EET 2011'")).size());
        cal.set(2003, 10, 10);
        Date d1 = cal.getTime();
        cal.set(2012, 1, 10);
        Date d2 = cal.getTime();
        assertEquals(3, map.values(new PredicateBuilder().getEntryObject().get("this").between(d1, d2)).size());
        assertEquals(3, map.values(new SqlPredicate("this between 'Mon Nov 10 11:43:05 EET 2003'" +
                " and 'Fri Feb 10 11:43:05 EET 2012'")).size());
    }

    // todo fails
    @Test
    public void testPredicateEnumAttribute() {
        Config cfg = new Config();
        StaticNodeFactory nodeFactory = new StaticNodeFactory(1);
        HazelcastInstance instance = nodeFactory.newHazelcastInstance(cfg);
        IMap map = instance.getMap("testPredicateEnumAttribute");
        testPredicateEnumAttribute(map);
    }

    @Test
    public void testPredicateEnumAttributeWithIndex() {
        Config cfg = new Config();
        StaticNodeFactory nodeFactory = new StaticNodeFactory(1);
        HazelcastInstance instance = nodeFactory.newHazelcastInstance(cfg);
        IMap map = instance.getMap("testPredicateEnumAttribute");
        map.addIndex("this", true);
        testPredicateDateAttribute(map);
    }

    private void testPredicateEnumAttribute(IMap map) {
        map.put(1, NodeType.MEMBER);
        map.put(2, NodeType.LITE_MEMBER);
        map.put(3, NodeType.JAVA_CLIENT);
        assertEquals(NodeType.MEMBER, map.values(new SqlPredicate("this=MEMBER")).iterator().next());
        assertEquals(2, map.values(new SqlPredicate("this in (MEMBER, LITE_MEMBER)")).size());
        assertEquals(NodeType.JAVA_CLIENT,
                map.values(new PredicateBuilder().getEntryObject()
                        .get("this").equal(NodeType.JAVA_CLIENT)).iterator().next());
        assertEquals(0, map.values(new PredicateBuilder().getEntryObject()
                .get("this").equal(NodeType.CSHARP_CLIENT)).size());
        assertEquals(2, map.values(new PredicateBuilder().getEntryObject()
                .get("this").in(NodeType.LITE_MEMBER, NodeType.MEMBER)).size());
    }


    public enum NodeType {
        MEMBER(1),
        LITE_MEMBER(2),
        JAVA_CLIENT(3),
        CSHARP_CLIENT(4);

        private int value;

        private NodeType(int type) {
            this.value = type;
        }

        public int getValue() {
            return value;
        }

        public static NodeType create(int value) {
            switch (value) {
                case 1:
                    return MEMBER;
                case 2:
                    return LITE_MEMBER;
                case 3:
                    return JAVA_CLIENT;
                case 4:
                    return CSHARP_CLIENT;
                default:
                    return null;
            }
        }
    }

    @Test
    public void testPredicateNotEqualWithIndex() {
        Config cfg = new Config();
        StaticNodeFactory nodeFactory = new StaticNodeFactory(1);
        HazelcastInstance instance = nodeFactory.newHazelcastInstance(cfg);
        IMap map1 = instance.getMap("testPredicateNotEqualWithIndex-ordered");
        IMap map2 = instance.getMap("testPredicateNotEqualWithIndex-unordered");
        testPredicateNotEqualWithIndex(map1, true);
        testPredicateNotEqualWithIndex(map2, false);
    }

    private void testPredicateNotEqualWithIndex(final IMap map, boolean ordered) {
        map.addIndex("name", ordered);
        map.put(1, new Value("abc", 1));
        map.put(2, new Value("xyz", 2));
        map.put(3, new Value("aaa", 3));
        assertEquals(3, map.values(new SqlPredicate("name != 'aac'")).size());
        assertEquals(2, map.values(new SqlPredicate("index != 2")).size());
        assertEquals(3, map.values(new PredicateBuilder().getEntryObject().get("name").notEqual("aac")).size());
        assertEquals(2, map.values(new PredicateBuilder().getEntryObject().get("index").notEqual(2)).size());
    }

    public void doFunctionalSQLQueryTest(IMap imap) {
        imap.put("1", new Employee("joe", 33, false, 14.56));
        imap.put("2", new Employee("ali", 23, true, 15.00));
        for (int i = 3; i < 103; i++) {
            imap.put(String.valueOf(i), new Employee("name" + i, i % 60, ((i & 1) == 1), Double.valueOf(i)));
        }
        Set<Map.Entry> entries = imap.entrySet();
        assertEquals(102, entries.size());
        int itCount = 0;
        for (Map.Entry entry : entries) {
            Employee c = (Employee) entry.getValue();
            itCount++;
        }
        assertEquals(102, itCount);
        entries = imap.entrySet(new SqlPredicate("active=true and age=23"));
        assertEquals(3, entries.size());
        for (Map.Entry entry : entries) {
            Employee c = (Employee) entry.getValue();
            assertEquals(c.getAge(), 23);
            assertTrue(c.isActive());
        }
        imap.remove("2");
        entries = imap.entrySet(new SqlPredicate("active=true and age=23"));
        assertEquals(2, entries.size());
        for (Map.Entry entry : entries) {
            Employee c = (Employee) entry.getValue();
            assertEquals(c.getAge(), 23);
            assertTrue(c.isActive());
        }
        entries = imap.entrySet(new SqlPredicate("age!=33"));
        for (Map.Entry entry : entries) {
            Employee c = (Employee) entry.getValue();
            assertTrue(c.getAge() != 33);
        }
        entries = imap.entrySet(new SqlPredicate("active!=false"));
        for (Map.Entry entry : entries) {
            Employee c = (Employee) entry.getValue();
            assertTrue(c.isActive());
        }
    }

    public void doFunctionalQueryTest(IMap imap) {
        imap.put("1", new Employee("joe", 33, false, 14.56));
        imap.put("2", new Employee("ali", 23, true, 15.00));
        for (int i = 3; i < 103; i++) {
            imap.put(String.valueOf(i), new Employee("name" + i, i % 60, ((i & 1) == 1), Double.valueOf(i)));
        }
        Set<Map.Entry> entries = imap.entrySet();
        assertEquals(102, entries.size());
        int itCount = 0;
        for (Map.Entry entry : entries) {
            Employee c = (Employee) entry.getValue();
            itCount++;
        }
        assertEquals(102, itCount);
        EntryObject e = new PredicateBuilder().getEntryObject();
        Predicate predicate = e.is("active").and(e.get("age").equal(23));
        entries = imap.entrySet(predicate);
//        assertEquals(3, entries.size());
        for (Map.Entry entry : entries) {
            Employee c = (Employee) entry.getValue();
            assertEquals(c.getAge(), 23);
            assertTrue(c.isActive());
        }
        imap.remove("2");
        entries = imap.entrySet(predicate);
        assertEquals(2, entries.size());
        for (Map.Entry entry : entries) {
            Employee c = (Employee) entry.getValue();
            assertEquals(c.getAge(), 23);
            assertTrue(c.isActive());
        }
        entries = imap.entrySet(new SqlPredicate(" (age >= " + 30 + ") AND (age <= " + 40 + ")"));
        assertEquals(23, entries.size());
        for (Map.Entry entry : entries) {
            Employee c = (Employee) entry.getValue();
            System.out.println(c);
            assertTrue(c.getAge() >= 30);
            assertTrue(c.getAge() <= 40);
        }
    }

    // todo fails
    @Test
    public void testInvalidSqlPredicate() {
        Config cfg = new Config();
        StaticNodeFactory nodeFactory = new StaticNodeFactory(1);
        HazelcastInstance instance = nodeFactory.newHazelcastInstance(cfg);
        IMap map = instance.getMap("employee");
        map.put(1, new Employee("e", 1, false, 0));
        map.put(2, new Employee("e2", 1, false, 0));
        try {
            map.values(new SqlPredicate("invalid_sql"));
            fail("Should fail because of invalid SQL!");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("There is no suitable accessor for 'invalid_sql'"));
        }
        try {
            map.values(new SqlPredicate("invalid sql"));
            fail("Should fail because of invalid SQL!");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Invalid SQL: [invalid sql]"));
        }
        try {
            map.values(new SqlPredicate("invalid and sql"));
            fail("Should fail because of invalid SQL!");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("There is no suitable accessor for 'invalid'"));
        }
        try {
            map.values(new SqlPredicate("invalid sql and"));
            fail("Should fail because of invalid SQL!");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("There is no suitable accessor for 'invalid'"));
        }
        try {
            map.values(new SqlPredicate(""));
            fail("Should fail because of invalid SQL!");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Invalid SQL: []"));
        }
        assertEquals(2, map.values(new SqlPredicate("age=1 and name like 'e%'")).size());
    }

    /**
     * test for issue #359
     */
    // todo fails
    @Test
    public void testIndexCleanupOnMigration() throws InterruptedException {
        Config cfg = new Config();
        final int n = 6;
        final int runCount = 500;
        final StaticNodeFactory nodeFactory = new StaticNodeFactory(n);
        HazelcastInstance instance = nodeFactory.newHazelcastInstance(cfg);
        final Config config = new Config();
        config.setProperty(GroupProperties.PROP_WAIT_SECONDS_BEFORE_JOIN, "0");
        final String mapName = "testIndexCleanupOnMigration";
        config.getMapConfig(mapName).addMapIndexConfig(new MapIndexConfig("name", false));
        ExecutorService ex = Executors.newFixedThreadPool(n);
        final CountDownLatch latch = new CountDownLatch(n);
        final AtomicInteger countdown = new AtomicInteger(n * runCount);
        final Random rand = new Random();
        for (int i = 0; i < n; i++) {
            Thread.sleep(rand.nextInt((i + 1) * 100) + 10);
            ex.execute(new Runnable() {
                public void run() {
                    final HazelcastInstance hz = nodeFactory.newHazelcastInstance(config);
                    final String name = UUID.randomUUID().toString();
                    final IMap<Object, Value> map = hz.getMap(mapName);
                    map.put(name, new Value(name, 0));
                    try {
                        for (int j = 1; j <= runCount; j++) {
                            Value v = map.get(name);
                            v.setIndex(j);
                            map.put(name, v);

                            try {
                                Thread.sleep(rand.nextInt(100) + 1);
                            } catch (InterruptedException e) {
                                break;
                            }
                            EntryObject e = new PredicateBuilder().getEntryObject();
                            Predicate<?, ?> predicate = e.get("name").equal(name);
                            final Collection<Value> values = map.values(predicate);
                            assertEquals(1, values.size());
                            Value v1 = values.iterator().next();
                            Value v2 = map.get(name);
                            assertEquals(v1, v2);
                            countdown.decrementAndGet();
                        }
                    } catch (AssertionError e) {
                        e.printStackTrace();
                    } catch (Throwable e) {
                        System.err.println(e.getClass().getName() + "-> " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }
        try {
            assertTrue(latch.await(60, TimeUnit.SECONDS));
            assertEquals(0, countdown.get());
        } finally {
            ex.shutdownNow();
        }
    }

}
