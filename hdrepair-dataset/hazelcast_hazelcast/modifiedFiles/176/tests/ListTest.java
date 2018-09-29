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

package com.hazelcast.collection;

import com.hazelcast.config.Config;
import com.hazelcast.core.*;
import com.hazelcast.test.HazelcastJUnit4ClassRunner;
import com.hazelcast.test.HazelcastTestSupport;
import com.hazelcast.test.TestHazelcastInstanceFactory;
import com.hazelcast.test.annotation.ClientCompatibleTest;
import com.hazelcast.test.annotation.ParallelTest;
import com.hazelcast.transaction.TransactionContext;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * @author ali 3/6/13
 */
@RunWith(HazelcastJUnit4ClassRunner.class)
@Category(ParallelTest.class)
public class ListTest extends HazelcastTestSupport {

    @Test
    @ClientCompatibleTest
    public void testListMethods() throws Exception {
        Config config = new Config();
        final String name = "defList";
        final int count = 100;
        final int insCount = 2;
        TestHazelcastInstanceFactory factory = createHazelcastInstanceFactory(insCount);
        final HazelcastInstance[] instances = factory.newInstances(config);

        for (int i=0; i<count; i++){
            assertTrue(getList(instances, name).add("item"+i));
        }

//        Iterator iter = getList(instances, name).iterator();
//        int item = 0;
//        while (iter.hasNext()){
//            assertEquals("item"+item++, iter.next());
//        }

//        assertEquals(count, getList(instances, name).size());

        assertEquals("item0", getList(instances, name).get(0));
        assertEquals(count, getList(instances, name).size());
        getList(instances, name).add(0, "item");
        assertEquals(count+1, getList(instances, name).size());
        assertEquals("item", getList(instances, name).get(0));
        assertEquals("item0", getList(instances, name).get(1));
        assertTrue(getList(instances, name).remove("item99"));
        assertFalse(getList(instances, name).remove("item99"));
        assertEquals(count, getList(instances, name).size());
        assertEquals("item",getList(instances, name).set(0, "newItem"));
        assertEquals("newItem",getList(instances, name).get(0));

        getList(instances, name).clear();
        assertEquals(0, getList(instances, name).size());

        List list = new ArrayList();
        list.add("item-1");
        list.add("item-2");

        assertTrue(getList(instances, name).addAll(list));
        assertEquals("item-1", getList(instances, name).get(0));
        assertEquals("item-2", getList(instances, name).get(1));

        assertTrue(getList(instances, name).addAll(1,list));
        assertEquals("item-1", getList(instances, name).get(0));
        assertEquals("item-1", getList(instances, name).get(1));
        assertEquals("item-2", getList(instances, name).get(2));
        assertEquals("item-2", getList(instances, name).get(3));
        assertEquals(4, getList(instances, name).size());
        assertEquals(0, getList(instances, name).indexOf("item-1"));
        assertEquals(1, getList(instances, name).lastIndexOf("item-1"));
        assertEquals(2, getList(instances, name).indexOf("item-2"));
        assertEquals(3, getList(instances, name).lastIndexOf("item-2"));

        assertEquals(4, getList(instances, name).size());





        assertTrue(getList(instances, name).containsAll(list));
        list.add("asd");
        assertFalse(getList(instances, name).containsAll(list));
        assertTrue(getList(instances, name).contains("item-1"));
        assertFalse(getList(instances, name).contains("item"));
    }

    @Test
    public void testListener() throws Exception {
        Config config = new Config();
        final String name = "defList";
        final int count = 10;
        final int insCount = 4;
        TestHazelcastInstanceFactory factory = createHazelcastInstanceFactory(insCount);
        final HazelcastInstance[] instances = factory.newInstances(config);
        final CountDownLatch latchAdd = new CountDownLatch(count);
        final CountDownLatch latchRemove = new CountDownLatch(count);

        ItemListener listener = new ItemListener() {
            public void itemAdded(ItemEvent item) {
                latchAdd.countDown();
            }

            public void itemRemoved(ItemEvent item) {
                latchRemove.countDown();
            }
        };

        getList(instances, name).addItemListener(listener, true);

        for (int i = 0; i < count; i++) {
            getList(instances, name).add("item" + i);
        }
        for (int i = 0; i < count; i++) {
            getList(instances, name).remove("item"+i);
        }
        assertTrue(latchAdd.await(5, TimeUnit.SECONDS));
        assertTrue(latchRemove.await(5, TimeUnit.SECONDS));

    }

    @Test
    public void testPutRemoveList(){
        Config config = new Config();
        final String name = "defList";

        final int insCount = 4;
        TestHazelcastInstanceFactory factory = createHazelcastInstanceFactory(insCount);
        final HazelcastInstance[] instances = factory.newInstances(config);
        TransactionContext context = instances[0].newTransactionContext();
        try {
            context.beginTransaction();

            TransactionalList mm = context.getList(name);
            assertEquals(0, mm.size());
            assertTrue(mm.add("value1"));
            assertTrue(mm.add("value1"));
            assertEquals(2, mm.size());
            assertFalse(mm.remove("value2"));
            assertTrue(mm.remove("value1"));

            context.commitTransaction();
        } catch (Exception e){
            fail(e.getMessage());
            context.rollbackTransaction();
        }

        assertEquals(1, instances[1].getList(name).size());
        assertTrue(instances[2].getList(name).add("value1"));
    }

    private IList getList(HazelcastInstance[] instances, String name){
        final Random rnd = new Random(System.currentTimeMillis());
        return instances[rnd.nextInt(instances.length)].getList(name);
    }


}
