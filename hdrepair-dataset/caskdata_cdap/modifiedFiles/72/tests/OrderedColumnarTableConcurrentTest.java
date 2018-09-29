package com.continuuity.data2.dataset.lib.table;

import com.continuuity.api.common.Bytes;
import com.continuuity.api.data.OperationResult;
import com.continuuity.data2.transaction.DefaultTransactionExecutor;
import com.continuuity.data2.transaction.TransactionAware;
import com.continuuity.data2.transaction.TransactionExecutor;
import com.continuuity.data2.transaction.TransactionExecutorFactory;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;

/**
 * This test emulates usage table by multiple concurrent clients.
 * @param <T> table type
 */
public abstract class OrderedColumnarTableConcurrentTest<T extends OrderedColumnarTable>
  extends OrderedColumnarTableTest<T> {

  private static final Logger LOG = LoggerFactory.getLogger(OrderedColumnarTableConcurrentTest.class);

  private static final byte[] ROW_TO_INCREMENT = Bytes.toBytes("row_to_increment");
  private static final byte[] COLUMN_TO_INCREMENT = Bytes.toBytes("column_to_increment");

  private static final byte[][] ROWS_TO_APPEND_TO;

  static {
    ROWS_TO_APPEND_TO = new byte[6][];
    ROWS_TO_APPEND_TO[0] = ROW_TO_INCREMENT;
    for (int i = 1; i < ROWS_TO_APPEND_TO.length; i++) {
      ROWS_TO_APPEND_TO[i] = Bytes.toBytes("row_to_append_to_" + i);
    }
  }

  protected TransactionExecutorFactory txExecutorFactory;

  @Before
  public void before() {
    super.before();
    txExecutorFactory = new TransactionExecutorFactory() {
      @Override
      public DefaultTransactionExecutor createExecutor(Iterable<TransactionAware> txAwares) {
        return new DefaultTransactionExecutor(txClient, txAwares);
      }
    };

  }

  @Test(timeout = 120000)
  public void testConcurrentOnSingleTable() throws Exception {
    // Set of clients read and write data concurrently.
    // * n clients increment a value with increasing values (+1, +2, ...) at specific row:column 100 times
    // * n clients append 100 columns to a set of 4 rows which includes the row that gets incremented (2 at a time).
    //   Append is: read all columns, add <last_column+1>
    int n = 5;

    getTableManager().create("myTable");
    try {
      final Thread[] incrementingClients = new Thread[n];
      final Thread[] appendingClients = new Thread[n];
      for (int i = 0; i < incrementingClients.length; i++) {
        incrementingClients[i] = new Thread(new IncrementingClient(txExecutorFactory));
        appendingClients[i] = new Thread(new AppendingClient(txExecutorFactory));
      }

      // start threads
      for (int i = 0; i < incrementingClients.length; i++) {
        incrementingClients[i].start();
        appendingClients[i].start();
      }

      // wait for finish
      for (int i = 0; i < incrementingClients.length; i++) {
        incrementingClients[i].join();
        appendingClients[i].join();
      }

      // verify result
      final T table = getTable("myTable");
      DefaultTransactionExecutor txExecutor =
        txExecutorFactory.createExecutor(Lists.newArrayList((TransactionAware) table));
      txExecutor.execute(new TransactionExecutor.Subroutine() {
        @Override
        public void apply() throws Exception {
          verifyIncrements();
          verifyAppends();
        }

        private void verifyAppends() throws Exception {
          for (byte[] row : ROWS_TO_APPEND_TO) {
            OperationResult<Map<byte[], byte[]>> cols = table.get(row, null);
            Assert.assertFalse(cols.isEmpty());

            // +1 because there was one extra column that we incremented
            boolean isIncrementedColumn = Arrays.equals(ROW_TO_INCREMENT, row);
            Assert.assertEquals(appendingClients.length * 100 + (isIncrementedColumn ? 1 : 0), cols.getValue().size());

            for (int i = isIncrementedColumn ? 1 : 0;
                 i < appendingClients.length * 100 + (isIncrementedColumn ? 1 : 0);
                 i++) {
              Assert.assertArrayEquals(Bytes.toBytes("foo" + i), cols.getValue().get(Bytes.toBytes("column" + i)));
            }
          }
        }

        private void verifyIncrements() throws Exception {
          OperationResult<Map<byte[], byte[]>> result = table.get(ROW_TO_INCREMENT,
                                                                  new byte[][]{COLUMN_TO_INCREMENT});
          Assert.assertFalse(result.isEmpty());
          byte[] val = result.getValue().get(COLUMN_TO_INCREMENT);
          long sum1to100 = ((1 + 99) * 99 / 2);
          Assert.assertEquals(incrementingClients.length * sum1to100, Bytes.toLong(val));
        }
      });

    } finally {
      getTableManager().drop("myTable");
    }
  }

  private class IncrementingClient implements Runnable {
    private final TransactionExecutorFactory txExecutorFactory;
    private final T table;

    public IncrementingClient(TransactionExecutorFactory txExecutorFactory) throws Exception {
      this.txExecutorFactory = txExecutorFactory;
      this.table = getTable("myTable");
    }

    @Override
    public void run() {
      final int[] executed = {0};
      while (executed[0] < 100) {
        DefaultTransactionExecutor txExecutor =
          txExecutorFactory.createExecutor(Lists.newArrayList((TransactionAware) table));
        try {
          txExecutor.execute(new TransactionExecutor.Subroutine() {
            @Override
            public void apply() throws Exception {
              table.increment(ROW_TO_INCREMENT,
                              new byte[][]{COLUMN_TO_INCREMENT},
                              new long[]{(long) executed[0]});
            }
          });
        } catch (Throwable t) {
          LOG.warn("failed to increment, will retry again", t);
          // do nothing: we'll retry execution
          t.printStackTrace();
          continue;
        }
        executed[0]++;
      }
    }
  }

  private class AppendingClient implements Runnable {
    private final TransactionExecutorFactory txExecutorFactory;
    private final T table;

    public AppendingClient(TransactionExecutorFactory txExecutorFactory) throws Exception {
      this.txExecutorFactory = txExecutorFactory;
      this.table = getTable("myTable");
    }

    @Override
    public void run() {
      // append to ith and (i+1)th rows at the same time
      DefaultTransactionExecutor txExecutor =
        txExecutorFactory.createExecutor(Lists.newArrayList((TransactionAware) table));
      for (int k = 0; k < 100; k++) {
        for (int i = 0; i < ROWS_TO_APPEND_TO.length / 2; i++) {
          final byte[] row1 = ROWS_TO_APPEND_TO[i * 2];
          final byte[] row2 = ROWS_TO_APPEND_TO[i * 2 + 1];
          boolean appended = false;
          while (!appended) {
            try {
              txExecutor.execute(new TransactionExecutor.Subroutine() {
                @Override
                public void apply() throws Exception {
                  appendColumn(row1);
                  appendColumn(row2);
                }

                private void appendColumn(byte[] row) throws Exception {
                  OperationResult<Map<byte[], byte[]>> columns = table.get(row, null);
                  int columnsCount = columns.isEmpty() ? 0 : columns.getValue().size();
                  byte[] columnToAppend = Bytes.toBytes("column" + columnsCount);
                  table.put(row, new byte[][]{columnToAppend}, new byte[][]{Bytes.toBytes("foo" + columnsCount)});
                }
              });
            } catch (Throwable t) {
              LOG.warn("failed to append, will retry again", t);
              // do nothing: we'll retry
              appended = false;
              continue;
            }

            appended = true;
          }
        }
      }
    }
  }

}
