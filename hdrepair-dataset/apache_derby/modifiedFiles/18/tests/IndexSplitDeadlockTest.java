/*
 * Derby - Class org.apache.derbyTesting.functionTests.tests.store.IndexSplitDeadlockTest
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.apache.derbyTesting.functionTests.tests.store;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import junit.framework.Test;
import org.apache.derbyTesting.junit.BaseJDBCTestCase;
import org.apache.derbyTesting.junit.CleanDatabaseTestSetup;
import org.apache.derbyTesting.junit.DatabasePropertyTestSetup;
import org.apache.derbyTesting.junit.JDBC;
import org.apache.derbyTesting.junit.TestConfiguration;

/**
 * Test that executes the code paths changed by the fix for the index split
 * deadlock (DERBY-2991). The main purpose is to test that index scans are
 * able to reposition in cases where they release the latch on the leaf page
 * on which they are positioned (typically because they had to wait for a
 * lock, or because they returned control to the caller after fetching a
 * bulk of rows).
 */
public class IndexSplitDeadlockTest extends BaseJDBCTestCase {

    /** List of obstructor threads to wait for after running the test. */
    private List obstructors;

    public IndexSplitDeadlockTest(String name) {
        super(name);
    }

    public static Test suite() {
        Test test = TestConfiguration.embeddedSuite(
                IndexSplitDeadlockTest.class);
        test = new CleanDatabaseTestSetup(test);
        test = DatabasePropertyTestSetup.setLockTimeouts(test, 2, 4);
        return test;
    }

    protected void tearDown() throws Exception {
        rollback();
        getConnection().setAutoCommit(false); // required by JDBC.dropSchema()
        JDBC.dropSchema(getConnection().getMetaData(), "APP");

        // Go through all the obstructors and call waitFor() so that we
        // detect errors that happened in another thread.
        if (obstructors != null) {
            for (Iterator it = obstructors.iterator(); it.hasNext(); ) {
                Obstructor o = (Obstructor) it.next();
                o.waitFor();
            }
        }
        obstructors = null;

        super.tearDown();
    }

    // --------------------------------------------------------------------
    // Test cases for calls to BTreeScan.reposition() in BTreeMaxScan
    // --------------------------------------------------------------------

    // NOTE: There is a call in fetchMax() that cannot be reached because the
    // scan state is alway SCAN_INIT when that method is called, and it only
    // calls reposition() if the scan state is SCAN_INPROGRESS. Therefore,
    // there's no test case for fetchMax().

    public void testBTreeMaxScan_fetchMaxRowFromBeginning() throws Exception {
        getConnection().setAutoCommit(false);

        Statement s = createStatement();
        s.executeUpdate("create table max_scan(x int)");
        s.executeUpdate("create index idx on max_scan(x)");

        // We need to make sure that we have at least two leaf pages. Each
        // 4K index page can hold ~200 rows.
        PreparedStatement ins = prepareStatement(
                "insert into max_scan values ?");
        for (int i = 0; i < 500; i++) {
            ins.setInt(1, i * 2);
            ins.executeUpdate();
        }
        commit();

        // Now make sure that the right-most leaf is empty, so that we must
        // fetch the max value from the beginning.
        s.executeUpdate("delete from max_scan where x > 50");

        // Obtain lock in another thread to block scans. Release lock after
        // two seconds.
        obstruct("update max_scan set x = x where x = 10", 2000);

        // Give the other thread time to obtain the lock.
        Thread.sleep(1000);

        // Perform a max scan (from beginning because last page is empty).
        // Will force repositioning because we must wait for the lock and
        // release the latch.
        JDBC.assertSingleValueResultSet(s.executeQuery(
                "select max(x) from max_scan --DERBY-PROPERTIES index=IDX"),
                "50");
    }

    // --------------------------------------------------------------------
    // Test cases for calls to BTreeScan.reposition() in BTreeForwardScan
    // --------------------------------------------------------------------

    /**
     * Test first call to reposition() in BTreeForwardScan.fetchRows().
     * This call happens when a new batch of rows is requested from a scan
     * that's in progress.
     */
    public void testBTreeForwardScan_fetchRows1() throws SQLException {

        // Create a table and an index and populate them
        Statement s = createStatement();
        s.executeUpdate("create table t (x int)");
        s.executeUpdate("create index idx on t(x)");
        PreparedStatement ins = prepareStatement("insert into t values ?");
        for (int i = 0; i < 400; i++) {
            ins.setInt(1, i);
            ins.executeUpdate();
        }

        // Start an index scan and fetch some rows so that it's in the
        // INPROGRESS state. Just fetch a small number of rows so that we
        // are still positioned on the left-most leaf page.
        ResultSet rs = s.executeQuery(
                "select * from t --DERBY-PROPERTIES index=IDX");
        for (int i = 0; i < 30; i++) {
            assertTrue(rs.next());
            assertEquals(i, rs.getInt(1));
        }

        // In another transaction, insert values smaller than the values
        // currently in the index. This causes a split of the left-most leaf.
        // Before DERBY-2991 we'd get a lock timeout here.
        Connection c2 = openDefaultConnection();
        Statement s2 = c2.createStatement();
        for (int i = 0; i < 300; i++) {
            s2.executeUpdate("insert into t values -1");
        }
        s2.close();
        c2.close();

        // Continue the index scan. This will trigger a full repositioning
        // from the root of the B-tree since the page on which we were
        // positioned has been split.
        for (int i = 30; i < 400; i++) {
            assertTrue(rs.next());
            assertEquals(i, rs.getInt(1));
        }
        assertFalse(rs.next());
    }

    // --------------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------------

    /**
     * <p>
     * In a separate thread, and in a separate transaction, execute the
     * SQL text and wait for the specified period of time, before the
     * transaction is rolled back. This method can be used to hold locks
     * and thereby block the main thread for a certain amount of time.
     * </p>
     *
     * <p>
     * If an exception is thrown while executing the SQL, the exception is
     * stored and rethrown from the tearDown() method in the main execution
     * thread, so that it is detected by the JUnit framework.
     * </p>
     *
     * @param sql the SQL text to execute
     * @param blockMillis how many milliseconds to wait until the transaction
     * is rolled back
     */
    private void obstruct(String sql, long blockMillis) {
        if (obstructors == null) {
            obstructors = new ArrayList();
        }
        obstructors.add(new Obstructor(sql, blockMillis));
    }

    /**
     * Helper class for the obstruct() method. Executes SQL in a separate
     * thread and stores any exceptions thrown.
     */
    private class Obstructor implements Runnable {
        private final String sql;
        private final long blockMillis;
        private final Thread thread;
        private Exception error;

        /**
         * Create and start an obstructor thread.
         * @param sql the SQL text to execute
         * @param blockMillis the time in milliseconds to keep the
         * transaction active
         */
        Obstructor(String sql, long blockMillis) {
            this.sql = sql;
            this.blockMillis = blockMillis;
            thread = new Thread(this);
            thread.start();
        }

        /**
         * Run the SQL in a separate transaction and block for the specified
         * amount of time.
         */
        public void run() {
            try {
                Connection c = openDefaultConnection();
                try {
                    c.setAutoCommit(false);
                    Statement s = c.createStatement();
                    s.execute(sql);
                    s.close();
                    Thread.sleep(blockMillis);
                } finally {
                    c.rollback();
                    c.close();
                }
            } catch (Exception e) {
                error = e;
            }
        }

        /**
         * Wait for the obstructor thread to complete. If an error occurred
         * while the thread was running, the exception will be rethrown by
         * this method.
         *
         * @throws Exception if an error occurred while the thread was running
         */
        void waitFor() throws Exception {
            thread.join();
            Exception e = error;
            error = null;
            if (e != null) {
                throw e;
            }
        }

    }

}
