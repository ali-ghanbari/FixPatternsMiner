package brooklyn.management.internal;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import brooklyn.entity.basic.BasicGroup;
import brooklyn.event.SensorEvent;
import brooklyn.event.SensorEventListener;
import brooklyn.management.SubscriptionHandle;
import brooklyn.management.SubscriptionManager;
import brooklyn.test.entity.TestApplication;
import brooklyn.test.entity.TestEntity;

/**
 * testing the {@link SubscriptionManager} and associated classes.
 */
public class LocalSubscriptionManagerTest {
    
    private static final int TIMEOUT_MS = 5000;
    
    private TestApplication app;
    
    @BeforeMethod
    public void setUp() throws Exception {
        app = new TestApplication();
    }
    
    @Test
    public void testSubscribeToEntityAttributeChange() throws Exception {
        TestEntity entity = new TestEntity(app);
        final CountDownLatch latch = new CountDownLatch(1);
        app.subscribe(entity, TestEntity.SEQUENCE, new SensorEventListener<Object>() {
                @Override public void onEvent(SensorEvent<Object> event) {
                    latch.countDown();
                }});
        entity.setSequenceValue(1234);
        if (!latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            fail("Timeout waiting for Event on TestEntity listener");
        }
    }
    
    @Test
    public void testSubscribeToEntityWithAttributeWildcard() throws Exception {
        TestEntity entity = new TestEntity(app);
        final CountDownLatch latch = new CountDownLatch(1);
        app.subscribe(entity, null, new SensorEventListener<Object>() {
            @Override public void onEvent(SensorEvent<Object> event) {
                latch.countDown();
            }});
        entity.setSequenceValue(1234);
        if (!latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            fail("Timeout waiting for Event on TestEntity listener");
        }
    }
    
    @Test
    public void testSubscribeToAttributeChangeWithEntityWildcard() throws Exception {
        TestEntity entity = new TestEntity(app);
        final CountDownLatch latch = new CountDownLatch(1);
        app.subscribe(null, TestEntity.SEQUENCE, new SensorEventListener<Object>() {
                @Override public void onEvent(SensorEvent<Object> event) {
                    latch.countDown();
                }});
        entity.setSequenceValue(1234);
        if (!latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            fail("Timeout waiting for Event on TestEntity listener");
        }
    }
    
    @Test
    public void testSubscribeToChildAttributeChange() throws Exception {
        TestEntity child = new TestEntity(app);
        final CountDownLatch latch = new CountDownLatch(1);
        app.subscribeToChildren(app, TestEntity.SEQUENCE, new SensorEventListener<Object>() {
            @Override public void onEvent(SensorEvent<Object> event) {
                latch.countDown();
            }});
        child.setSequenceValue(1234);
        if (!latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            fail("Timeout waiting for Event on child TestEntity listener");
        }
    }
    
    @Test
    public void testSubscribeToMemberAttributeChange() throws Exception {
        BasicGroup group = new BasicGroup(app);
        TestEntity member = new TestEntity(app);
        group.addMember(member);

        final List<SensorEvent<Integer>> events = new CopyOnWriteArrayList<SensorEvent<Integer>>();
        final CountDownLatch latch = new CountDownLatch(1);
        app.subscribeToMembers(group, TestEntity.SEQUENCE, new SensorEventListener<Integer>() {
            @Override public void onEvent(SensorEvent<Integer> event) {
                events.add(event);
                latch.countDown();
            }});
        member.setAttribute(TestEntity.SEQUENCE, 123);

        if (!latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            fail("Timeout waiting for Event on parent TestEntity listener");
        }
        assertEquals(events.size(), 1);
        assertEquals(events.get(0).getValue(), (Integer)123);
        assertEquals(events.get(0).getSensor(), TestEntity.SEQUENCE);
        assertEquals(events.get(0).getSource().getId(), member.getId());
    }
    
    // Regression test for ConcurrentModificationException in issue #327
    @Test(groups="Integration")
    public void testConcurrentSubscribingAndPublishing() throws Exception {
        final AtomicReference<Exception> threadException = new AtomicReference<Exception>();
        TestEntity entity = new TestEntity(app);
        
        // Repeatedly subscribe and unsubscribe, so listener-set constantly changing while publishing to it.
        // First create a stable listener so it is always the same listener-set object.
        Thread thread = new Thread() {
            public void run() {
                try {
                    SensorEventListener<Object> noopListener = new SensorEventListener<Object>() {
                        @Override public void onEvent(SensorEvent<Object> event) {
                        }
                    };
                    app.getSubscriptionContext().subscribe(null, TestEntity.SEQUENCE, noopListener);
                    while (!Thread.currentThread().isInterrupted()) {
                        SubscriptionHandle handle = app.getSubscriptionContext().subscribe(null, TestEntity.SEQUENCE, noopListener);
                        app.getSubscriptionContext().unsubscribe(handle);
                    }
                } catch (Exception e) {
                    threadException.set(e);
                }
            }
        };
        
        try {
            thread.start();
            for (int i = 0; i < 10000; i++) {
                entity.setAttribute(TestEntity.SEQUENCE, i);
            }
        } finally {
            thread.interrupt();
        }

        if (threadException.get() != null) throw threadException.get();
    }
}
