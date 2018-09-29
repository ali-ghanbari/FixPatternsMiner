package com.continuuity.data2.transaction.queue;

import com.continuuity.api.common.Bytes;
import com.continuuity.common.queue.QueueName;
import com.continuuity.data2.queue.ConsumerConfig;
import com.continuuity.data2.queue.DequeueResult;
import com.continuuity.data2.queue.DequeueStrategy;
import com.continuuity.data2.queue.Queue2Consumer;
import com.continuuity.data2.queue.Queue2Producer;
import com.continuuity.data2.queue.QueueClientFactory;
import com.continuuity.data2.queue.QueueEntry;
import com.continuuity.data2.transaction.Transaction;
import com.continuuity.data2.transaction.TransactionAware;
import com.continuuity.data2.transaction.TransactionContext;
import com.continuuity.data2.transaction.TransactionExecutor;
import com.continuuity.data2.transaction.TransactionExecutorFactory;
import com.continuuity.data2.transaction.TransactionFailureException;
import com.continuuity.data2.transaction.TransactionSystemClient;
import com.continuuity.data2.transaction.inmemory.InMemoryTransactionManager;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * tests for queues. Extend this class to run the tests against an implementation of the queues.
 */
public abstract class QueueTest {

  private static final Logger LOG = LoggerFactory.getLogger(QueueTest.class);

  private static final int ROUNDS = 1000;
  private static final long TIMEOUT_MS = 2 * 60 * 1000L;

  protected static TransactionSystemClient txSystemClient;
  protected static QueueClientFactory queueClientFactory;
  protected static QueueAdmin queueAdmin;
  protected static StreamAdmin streamAdmin;
  protected static InMemoryTransactionManager transactionManager;
  protected static TransactionExecutorFactory executorFactory;

  @AfterClass
  public static void shutdownTx() {
    if (transactionManager != null) {
      transactionManager.stopAndWait();
    }
  }

  @Test
  public void testCreateProducerWithMetricsEnsuresTableExists() throws Exception {
    QueueName queueName = QueueName.fromStream("me", "my_stream");
    final Queue2Producer producer = queueClientFactory.createProducer(queueName, new QueueMetrics() {
      @Override
      public void emitEnqueue(int count) {}

      @Override
      public void emitEnqueueBytes(int bytes) {}
    });

    Assert.assertNotNull(producer);
  }

  @Test
  public void testDropAllQueues() throws Exception {
    // create a queue and a stream and enqueue one entry each
    QueueName queueName = QueueName.fromFlowlet("myFlow", "myFlowlet", "tDAQ");
    QueueName streamName = QueueName.fromStream("tDAQ", "myStream");
    final Queue2Producer qProducer = queueClientFactory.createProducer(queueName);
    final Queue2Producer sProducer = queueClientFactory.createProducer(streamName);
    executorFactory.createExecutor(Lists.newArrayList((TransactionAware) qProducer, (TransactionAware) sProducer))
                   .execute(new TransactionExecutor.Subroutine() {
                     @Override
                     public void apply() throws Exception {
                       qProducer.enqueue(new QueueEntry(Bytes.toBytes("q42")));
                       sProducer.enqueue(new QueueEntry(Bytes.toBytes("s42")));
                     }
                   });
    // drop all queues
    queueAdmin.dropAll();
    // verify that queue is gone and stream is still there
    final Queue2Consumer qConsumer = queueClientFactory.createConsumer(
      queueName, new ConsumerConfig(0, 0, 1, DequeueStrategy.FIFO, null), 1);
    final Queue2Consumer sConsumer = queueClientFactory.createConsumer(
      streamName, new ConsumerConfig(0, 0, 1, DequeueStrategy.FIFO, null), 1);
    executorFactory.createExecutor(Lists.newArrayList((TransactionAware) qConsumer, (TransactionAware) sConsumer))
                   .execute(new TransactionExecutor.Subroutine() {
                     @Override
                     public void apply() throws Exception {
                       DequeueResult dequeue = qConsumer.dequeue();
                       Assert.assertTrue(dequeue.isEmpty());
                       dequeue = sConsumer.dequeue();
                       Assert.assertFalse(dequeue.isEmpty());
                       Iterator<byte[]> iterator = dequeue.iterator();
                       Assert.assertTrue(iterator.hasNext());
                       Assert.assertArrayEquals(Bytes.toBytes("s42"), iterator.next());
                     }
                   });
  }

  @Test
  public void testDropAllStreams() throws Exception {
    // create a queue and a stream and enqueue one entry each
    QueueName queueName = QueueName.fromFlowlet("myFlow", "myFlowlet", "tDAS");
    QueueName streamName = QueueName.fromStream("tDAS", "myStream");
    final Queue2Producer qProducer = queueClientFactory.createProducer(queueName);
    final Queue2Producer sProducer = queueClientFactory.createProducer(streamName);
    executorFactory.createExecutor(Lists.newArrayList((TransactionAware) qProducer, (TransactionAware) sProducer))
                   .execute(new TransactionExecutor.Subroutine() {
                     @Override
                     public void apply() throws Exception {
                       qProducer.enqueue(new QueueEntry(Bytes.toBytes("q42")));
                       sProducer.enqueue(new QueueEntry(Bytes.toBytes("s42")));
                     }
                   });
    // drop all queues
    streamAdmin.dropAll();
    // verify that queue is gone and stream is still there
    final Queue2Consumer qConsumer = queueClientFactory.createConsumer(
      queueName, new ConsumerConfig(0, 0, 1, DequeueStrategy.FIFO, null), 1);
    final Queue2Consumer sConsumer = queueClientFactory.createConsumer(
      streamName, new ConsumerConfig(0, 0, 1, DequeueStrategy.FIFO, null), 1);
    executorFactory.createExecutor(Lists.newArrayList((TransactionAware) qConsumer, (TransactionAware) sConsumer))
                   .execute(new TransactionExecutor.Subroutine() {
                     @Override
                     public void apply() throws Exception {
                       DequeueResult dequeue = sConsumer.dequeue();
                       Assert.assertTrue(dequeue.isEmpty());
                       dequeue = qConsumer.dequeue();
                       Assert.assertFalse(dequeue.isEmpty());
                       Iterator<byte[]> iterator = dequeue.iterator();
                       Assert.assertTrue(iterator.hasNext());
                       Assert.assertArrayEquals(Bytes.toBytes("q42"), iterator.next());
                     }
                   });
  }


  @Test
  public void testStreamQueue() throws Exception {
    QueueName queueName = QueueName.fromStream("me", "my_stream");
    final Queue2Producer producer = queueClientFactory.createProducer(queueName);
    executorFactory.createExecutor(Lists.newArrayList((TransactionAware) producer))
      .execute(new TransactionExecutor.Subroutine() {
      @Override
      public void apply() throws Exception {
        QueueEntry entry = new QueueEntry(Bytes.toBytes("my_data"));
        producer.enqueue(entry);
      }
    });

    final Queue2Consumer consumer =
      queueClientFactory.createConsumer(queueName,
                                        new ConsumerConfig(0, 0, 1, DequeueStrategy.FIFO, null), 1);
    executorFactory.createExecutor(Lists.newArrayList((TransactionAware) consumer))
      .execute(new TransactionExecutor.Subroutine() {
        @Override
        public void apply() throws Exception {
          DequeueResult dequeue = consumer.dequeue();
          Assert.assertTrue(dequeue != null && !dequeue.isEmpty());
          Iterator<byte[]> iterator = dequeue.iterator();
          Assert.assertTrue(iterator.hasNext());
          Assert.assertArrayEquals(Bytes.toBytes("my_data"), iterator.next());
          Assert.assertFalse(iterator.hasNext());
        }
      });
  }

  // Simple enqueue and dequeue with one consumer, no batch
  @Test(timeout = TIMEOUT_MS)
  public void testSingleFifo() throws Exception {
    QueueName queueName = QueueName.fromFlowlet("flow", "flowlet", "singlefifo");
    enqueueDequeue(queueName, ROUNDS, ROUNDS, 1, 1, DequeueStrategy.FIFO, 1);
  }

  // Simple enqueue and dequeue with three consumers, no batch
  @Test(timeout = TIMEOUT_MS)
  public void testMultiFifo() throws Exception {
    QueueName queueName = QueueName.fromFlowlet("flow", "flowlet", "multififo");
    enqueueDequeue(queueName, ROUNDS, ROUNDS, 1, 3, DequeueStrategy.FIFO, 1);
  }

  // Simple enqueue and dequeue with one consumer, no batch
  @Test(timeout = TIMEOUT_MS)
  public void testSingleHash() throws Exception {
    QueueName queueName = QueueName.fromFlowlet("flow", "flowlet", "singlehash");
    enqueueDequeue(queueName, 2 * ROUNDS, ROUNDS, 1, 1, DequeueStrategy.HASH, 1);
  }

  @Test(timeout = TIMEOUT_MS)
  public void testMultiHash() throws Exception {
    QueueName queueName = QueueName.fromStream("bingo", "bang");
    enqueueDequeue(queueName, 2 * ROUNDS, ROUNDS, 1, 3, DequeueStrategy.HASH, 1);
  }

  // Batch enqueue and batch dequeue with one consumer.
  @Test(timeout = TIMEOUT_MS)
  public void testBatchHash() throws Exception {
    QueueName queueName = QueueName.fromFlowlet("flow", "flowlet", "batchhash");
    enqueueDequeue(queueName, 2 * ROUNDS, ROUNDS, 10, 1, DequeueStrategy.HASH, 50);
  }

  @Test(timeout = TIMEOUT_MS)
  public void testQueueAbortRetrySkip() throws Exception {
    QueueName queueName = QueueName.fromFlowlet("flow", "flowlet", "queuefailure");
    configureGroups(queueName, ImmutableMap.of(0L, 1, 1L, 1));

    createEnqueueRunnable(queueName, 5, 1, null).run();

    Queue2Consumer fifoConsumer = queueClientFactory.createConsumer(
      queueName, new ConsumerConfig(0, 0, 1, DequeueStrategy.FIFO, null), 2);
    Queue2Consumer hashConsumer = queueClientFactory.createConsumer(
      queueName, new ConsumerConfig(1, 0, 1, DequeueStrategy.HASH, "key"), 2);

    TransactionContext txContext = createTxContext(fifoConsumer, hashConsumer);
    txContext.start();

    Assert.assertEquals(0, Bytes.toInt(fifoConsumer.dequeue().iterator().next()));
    Assert.assertEquals(0, Bytes.toInt(hashConsumer.dequeue().iterator().next()));

    // Abort the consumer transaction
    txContext.abort();

    // Dequeue again in a new transaction, should see the same entries
    txContext.start();
    Assert.assertEquals(0, Bytes.toInt(fifoConsumer.dequeue().iterator().next()));
    Assert.assertEquals(0, Bytes.toInt(hashConsumer.dequeue().iterator().next()));
    txContext.finish();

    // Dequeue again, now should get next entry
    txContext.start();
    Assert.assertEquals(1, Bytes.toInt(fifoConsumer.dequeue().iterator().next()));
    Assert.assertEquals(1, Bytes.toInt(hashConsumer.dequeue().iterator().next()));
    txContext.finish();

    // Dequeue a result and abort.
    txContext.start();
    DequeueResult fifoResult = fifoConsumer.dequeue();
    DequeueResult hashResult = hashConsumer.dequeue();

    Assert.assertEquals(2, Bytes.toInt(fifoResult.iterator().next()));
    Assert.assertEquals(2, Bytes.toInt(hashResult.iterator().next()));
    txContext.abort();

    // Now skip the result with a new transaction.
    txContext.start();
    fifoResult.reclaim();
    hashResult.reclaim();
    txContext.finish();

    // Dequeue again, it should see a new entry
    txContext.start();
    Assert.assertEquals(3, Bytes.toInt(fifoConsumer.dequeue().iterator().next()));
    Assert.assertEquals(3, Bytes.toInt(hashConsumer.dequeue().iterator().next()));
    txContext.finish();

    // Dequeue again, it should see a new entry
    txContext.start();
    Assert.assertEquals(4, Bytes.toInt(fifoConsumer.dequeue().iterator().next()));
    Assert.assertEquals(4, Bytes.toInt(hashConsumer.dequeue().iterator().next()));
    txContext.finish();

    txContext.start();
    if (fifoConsumer instanceof Closeable) {
      ((Closeable) fifoConsumer).close();
    }
    if (hashConsumer instanceof Closeable) {
      ((Closeable) hashConsumer).close();
    }
    txContext.finish();

    verifyQueueIsEmpty(queueName, 2);
  }

  @Test(timeout = TIMEOUT_MS)
  public void testRollback() throws Exception {
    QueueName queueName = QueueName.fromFlowlet("flow", "flowlet", "queuerollback");
    Queue2Producer producer = queueClientFactory.createProducer(queueName);
    Queue2Consumer consumer = queueClientFactory.createConsumer(
      queueName, new ConsumerConfig(0, 0, 1, DequeueStrategy.FIFO, null), 1);

    TransactionContext txContext = createTxContext(producer, consumer,
                                        new TransactionAware() {

      boolean canCommit = false;

      @Override
      public void startTx(Transaction tx) {
      }

      @Override
      public Collection<byte[]> getTxChanges() {
        return ImmutableList.of();
      }

      @Override
      public boolean commitTx() throws Exception {
        // Flip-flop between commit success/failure.
        boolean res = canCommit;
        canCommit = !canCommit;
        return res;
      }

      @Override
      public void postTxCommit() {
      }

      @Override
      public boolean rollbackTx() throws Exception {
        return true;
      }


      @Override
      public String getName() {
        return "test";
      }                                    
    });

    // First, try to enqueue and commit would fail
    txContext.start();
    try {
      producer.enqueue(new QueueEntry(Bytes.toBytes(1)));
      txContext.finish();
      // If reaches here, it's wrong, as exception should be thrown.
      Assert.assertTrue(false);
    } catch (TransactionFailureException e) {
      txContext.abort();
    }

    // Try to enqueue again. Within the same transaction, dequeue should be empty.
    txContext.start();
    producer.enqueue(new QueueEntry(Bytes.toBytes(1)));
    Assert.assertTrue(consumer.dequeue().isEmpty());
    txContext.finish();

    // This time, enqueue has been committed, dequeue would see the item
    txContext.start();
    try {
      Assert.assertEquals(1, Bytes.toInt(consumer.dequeue().iterator().next()));
      txContext.finish();
      // If reaches here, it's wrong, as exception should be thrown.
      Assert.assertTrue(false);
    } catch (TransactionFailureException e) {
      txContext.abort();
    }

    // Dequeue again, since last tx was rollback, this dequeue should see the item again.
    txContext.start();
    Assert.assertEquals(1, Bytes.toInt(consumer.dequeue().iterator().next()));
    txContext.finish();
  }

  @Test
  public void testOneFIFOEnqueueDequeue() throws Exception {
    testOneEnqueueDequeue(DequeueStrategy.FIFO);
  }

  @Test
  public void testOneRoundRobinEnqueueDequeue() throws Exception {
    testOneEnqueueDequeue(DequeueStrategy.ROUND_ROBIN);
  }

  @Test
  public void testReset() throws Exception {
    // NOTE: using different name of the queue from other unit-tests because this test leaves entries
    QueueName queueName = QueueName.fromFlowlet("flow", "flowlet", "queueReset");
    configureGroups(queueName, ImmutableMap.of(0L, 1, 1L, 1));
    Queue2Producer producer = queueClientFactory.createProducer(queueName);
    TransactionContext txContext = createTxContext(producer);
    txContext.start();
    producer.enqueue(new QueueEntry(Bytes.toBytes(0)));
    producer.enqueue(new QueueEntry(Bytes.toBytes(1)));
    producer.enqueue(new QueueEntry(Bytes.toBytes(2)));
    producer.enqueue(new QueueEntry(Bytes.toBytes(3)));
    producer.enqueue(new QueueEntry(Bytes.toBytes(4)));
    txContext.finish();

    Queue2Consumer consumer1 = queueClientFactory.createConsumer(
      queueName, new ConsumerConfig(0, 0, 1, DequeueStrategy.FIFO, null), 2);

    // Check that there's smth in the queue, but do not consume: abort tx after check
    txContext = createTxContext(consumer1);
    txContext.start();
    Assert.assertEquals(0, Bytes.toInt(consumer1.dequeue().iterator().next()));
    txContext.finish();

    System.out.println("Before drop");

    // Reset queues
    queueAdmin.dropAll();

    System.out.println("After drop");

    // we gonna need another one to check again to avoid caching side-affects
    Queue2Consumer consumer2 = queueClientFactory.createConsumer(
      queueName, new ConsumerConfig(1, 0, 1, DequeueStrategy.FIFO, null), 2);
    txContext = createTxContext(consumer2);
    // Check again: should be nothing in the queue
    txContext.start();
    Assert.assertTrue(consumer2.dequeue().isEmpty());
    txContext.finish();

    // add another entry
    txContext = createTxContext(producer);
    txContext.start();
    producer.enqueue(new QueueEntry(Bytes.toBytes(5)));
    txContext.finish();

    txContext = createTxContext(consumer2);
    // Check again: consumer should see new entry
    txContext.start();
    Assert.assertEquals(5, Bytes.toInt(consumer2.dequeue().iterator().next()));
    txContext.finish();
  }

  private void testOneEnqueueDequeue(DequeueStrategy strategy) throws Exception {
    QueueName queueName = QueueName.fromFlowlet("flow", "flowlet", "queue1");
    configureGroups(queueName, ImmutableMap.of(0L, 1));
    Queue2Producer producer = queueClientFactory.createProducer(queueName);
    TransactionContext txContext = createTxContext(producer);
    txContext.start();
    producer.enqueue(new QueueEntry(Bytes.toBytes(55)));
    txContext.finish();

    Queue2Consumer consumer = queueClientFactory.createConsumer(
      queueName, new ConsumerConfig(0, 0, 1, strategy, null), 1);

    // Check that there's smth in the queue, but do not consume: abort tx after check
    txContext = createTxContext(consumer);
    txContext.start();
    Assert.assertEquals(55, Bytes.toInt(consumer.dequeue().iterator().next()));
    txContext.finish();

    if (producer instanceof Closeable) {
      ((Closeable) producer).close();
    }
    if (consumer instanceof Closeable) {
      ((Closeable) consumer).close();
    }

    verifyQueueIsEmpty(queueName, 1);
  }


  private void enqueueDequeue(final QueueName queueName, int preEnqueueCount,
                              int concurrentCount, int enqueueBatchSize,
                              final int consumerSize, final DequeueStrategy dequeueStrategy,
                              final int dequeueBatchSize) throws Exception {

    configureGroups(queueName, ImmutableMap.of(0L, consumerSize));

    Preconditions.checkArgument(preEnqueueCount % enqueueBatchSize == 0, "Count must be divisible by enqueueBatchSize");
    Preconditions.checkArgument(concurrentCount % enqueueBatchSize == 0, "Count must be divisible by enqueueBatchSize");

    createEnqueueRunnable(queueName, preEnqueueCount, enqueueBatchSize, null).run();

    final CyclicBarrier startBarrier = new CyclicBarrier(consumerSize + 2);
    ExecutorService executor = Executors.newFixedThreadPool(consumerSize + 1);

    // Enqueue thread
    executor.submit(createEnqueueRunnable(queueName, concurrentCount, enqueueBatchSize, startBarrier));

    // Dequeue
    final long expectedSum = ((long) preEnqueueCount / 2 * ((long) preEnqueueCount - 1)) +
                             ((long) concurrentCount / 2 * ((long) concurrentCount - 1));
    final AtomicLong valueSum = new AtomicLong();
    final CountDownLatch completeLatch = new CountDownLatch(consumerSize);

    for (int i = 0; i < consumerSize; i++) {
      final int instanceId = i;
      executor.submit(new Runnable() {
        @Override
        public void run() {
          try {
            startBarrier.await();
            LOG.info("Consumer {} starts consuming {}", instanceId, queueName.getSimpleName());
            Queue2Consumer consumer = queueClientFactory.createConsumer(
              queueName, new ConsumerConfig(0, instanceId, consumerSize, dequeueStrategy, "key"), 1);
            try {
              TransactionContext txContext = createTxContext(consumer);

              Stopwatch stopwatch = new Stopwatch();
              stopwatch.start();

              int dequeueCount = 0;
              while (valueSum.get() < expectedSum) {
                txContext.start();

                try {
                  DequeueResult result = consumer.dequeue(dequeueBatchSize);
                  txContext.finish();

                  if (result.isEmpty()) {
                    continue;
                  }

                  for (byte[] data : result) {
                    valueSum.addAndGet(Bytes.toInt(data));
                    dequeueCount++;
                  }
                } catch (TransactionFailureException e) {
                  LOG.error("Operation error", e);
                  txContext.abort();
                  throw Throwables.propagate(e);
                }
              }

              long elapsed = stopwatch.elapsedTime(TimeUnit.MILLISECONDS);
              LOG.info("Dequeue {} entries in {} ms for {}", dequeueCount, elapsed, queueName.getSimpleName());
              LOG.info("Dequeue avg {} entries per seconds for {}",
                       (double) dequeueCount * 1000 / elapsed, queueName.getSimpleName());

              if (consumer instanceof Closeable) {
                txContext.start();
                ((Closeable) consumer).close();
                txContext.finish();
              }

              completeLatch.countDown();
            } finally {
              if (consumer instanceof Closeable) {

                ((Closeable) consumer).close();
              }
            }
          } catch (Exception e) {
            LOG.error(e.getMessage(), e);
          }
        }
      });
    }

    startBarrier.await();
    completeLatch.await();
    TimeUnit.SECONDS.sleep(2);

    Assert.assertEquals(expectedSum, valueSum.get());

    // Only check eviction for queue.
    if (!queueName.isStream()) {
      verifyQueueIsEmpty(queueName, 1);
    }
    executor.shutdownNow();
  }

  private TransactionContext createTxContext(Object... txAwares) {
    TransactionAware[] casted = new TransactionAware[txAwares.length];
    for (int i = 0; i < txAwares.length; i++) {
      casted[i] = (TransactionAware) txAwares[i];
    }
    return new TransactionContext(txSystemClient, casted);
  }
  
  private Runnable createEnqueueRunnable(final QueueName queueName, final int count,
                                         final int batchSize, final CyclicBarrier barrier) {
    return new Runnable() {

      @Override
      public void run() {
        try {
          if (barrier != null) {
            barrier.await();
          }
          Queue2Producer producer = queueClientFactory.createProducer(queueName);
          try {
            TransactionContext txContext = createTxContext(producer);

            LOG.info("Start enqueue {} entries.", count);

            Stopwatch stopwatch = new Stopwatch();
            stopwatch.start();

            // Pre-Enqueue
            int batches = count / batchSize;
            List<QueueEntry> queueEntries = Lists.newArrayListWithCapacity(batchSize);
            for (int i = 0; i < batches; i++) {
              txContext.start();

              try {
                queueEntries.clear();
                for (int j = 0; j < batchSize; j++) {
                  int val = i * batchSize + j;
                  byte[] queueData = Bytes.toBytes(val);
                  queueEntries.add(new QueueEntry("key", val, queueData));
                }

                producer.enqueue(queueEntries);
                txContext.finish();
              } catch (TransactionFailureException e) {
                LOG.error("Operation error", e);
                txContext.abort();
                throw Throwables.propagate(e);
              }
            }

            long elapsed = stopwatch.elapsedTime(TimeUnit.MILLISECONDS);
            LOG.info("Enqueue {} entries in {} ms for {}", count, elapsed, queueName.getSimpleName());
            LOG.info("Enqueue avg {} entries per seconds for {}",
                     (double) count * 1000 / elapsed, queueName.getSimpleName());
            stopwatch.stop();
          } finally {
            if (producer instanceof Closeable) {
              ((Closeable) producer).close();
            }
          }
        } catch (Exception e) {
          LOG.error(e.getMessage(), e);
        }
      }
    };
  }

  protected void configureGroups(QueueName queueName, Map<Long, Integer> groupInfo) throws Exception {
    // Do NOTHING by default
  }

  protected void verifyQueueIsEmpty(QueueName queueName, int numActualConsumers) throws Exception {
    // the queue has been consumed by n consumers. Use a consumerId greater than n to make sure it can dequeue.
    Queue2Consumer consumer = queueClientFactory.createConsumer(
      queueName, new ConsumerConfig(numActualConsumers + 1, 0, 1, DequeueStrategy.FIFO, null), -1);

    TransactionContext txContext = createTxContext(consumer);
    txContext.start();
    Assert.assertTrue("Entire queue should be evicted after test but dequeue succeeds.",
                       consumer.dequeue().isEmpty());
    txContext.abort();
  }
}
