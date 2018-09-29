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

package com.hazelcast.queue;

import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.TransactionalQueue;
import com.hazelcast.test.ParallelTestSupport;
import com.hazelcast.test.RandomBlockJUnit4ClassRunner;
import com.hazelcast.test.StaticNodeFactory;
import com.hazelcast.transaction.TransactionException;
import com.hazelcast.transaction.TransactionOptions;
import com.hazelcast.transaction.TransactionalTask;
import com.hazelcast.transaction.TransactionalTaskContext;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * @ali 3/11/13
 */
@RunWith(RandomBlockJUnit4ClassRunner.class)
public class TransactionQueueTest extends ParallelTestSupport {

    @Test
    public void testTransactionalOfferPoll1() throws Exception {
        Config config = new Config();
        final int insCount = 4;
        final String name = "defQueue";
        StaticNodeFactory factory = createNodeFactory(insCount);
        final HazelcastInstance[] instances = factory.newInstances(config);

        boolean b = instances[0].executeTransaction(new TransactionOptions().setTransactionType(TransactionOptions.TransactionType.LOCAL),
                new TransactionalTask<Boolean>() {
                    public Boolean execute(TransactionalTaskContext context) throws TransactionException {
                        TransactionalQueue<String> q = context.getQueue(name);
                        assertTrue(q.offer("ali"));
                        String s = q.poll();
                        assertNull(s);
                        return true;
                    }
                });
        assertTrue(b);
        assertEquals(1, getQueue(instances, name).size());
    }

    @Test
    public void testTransactionalOfferPoll2() throws Exception {
        Config config = new Config();
        final int insCount = 4;
        final String name0 = "defQueue0";
        final String name1 = "defQueue1";
        StaticNodeFactory factory = createNodeFactory(insCount);
        final HazelcastInstance[] instances = factory.newInstances(config);
        final CountDownLatch latch = new CountDownLatch(1);
        new Thread() {
            public void run() {
                try {
                    latch.await(5, TimeUnit.SECONDS);
                    sleep(3000);
                    getQueue(instances, name0).offer("item0");
                } catch (InterruptedException ignored) {
                }
            }
        }.start();

        boolean b = instances[0].executeTransaction(new TransactionOptions().setTransactionType(TransactionOptions.TransactionType.LOCAL),
                new TransactionalTask<Boolean>() {
                    public Boolean execute(TransactionalTaskContext context) throws TransactionException {
                        TransactionalQueue<String> q0 = context.getQueue(name0);
                        TransactionalQueue<String> q1 = context.getQueue(name1);
                        String s = null;
                        latch.countDown();
                        try {
                            s = q0.poll(10, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            fail(e.getMessage());
                        }
                        assertEquals("item0", s);
                        q1.offer(s);
                        return true;
                    }
                });
        assertTrue(b);
        assertEquals(0, getQueue(instances, name0).size());
        assertEquals("item0", getQueue(instances, name1).poll());
    }

    @Test
    public void testQueueWithMap() throws Exception {
        Config config = new Config();
        final int insCount = 4;
        final String queueName = "defQueue";
        final String mapName = "defMap";
        StaticNodeFactory factory = createNodeFactory(insCount);
        final HazelcastInstance[] instances = factory.newInstances(config);
        instances[0].getMap(mapName).lock("lock1");

        try {
            instances[1].executeTransaction(new TransactionOptions().setTimeout(5, TimeUnit.SECONDS)
                    .setTransactionType(TransactionOptions.TransactionType.LOCAL),
                    new TransactionalTask<Object>() {
                        public Object execute(TransactionalTaskContext context) throws TransactionException {
                            boolean offered = context.getQueue(queueName).offer("item1");
                            assertTrue(offered);
                            context.getMap(mapName).put("lock1", "value1");
                            fail();
                            return null;
                        }
                    });
        } catch (TransactionException ex) {
            // expected
        }
        assertEquals(0, instances[0].getQueue(queueName).size());
        assertNull(instances[0].getMap(mapName).get("lock1"));
    }

    private IQueue getQueue(HazelcastInstance[] instances, String name) {
        final Random rnd = new Random(System.currentTimeMillis());
        return instances[rnd.nextInt(instances.length)].getQueue(name);
    }
}
