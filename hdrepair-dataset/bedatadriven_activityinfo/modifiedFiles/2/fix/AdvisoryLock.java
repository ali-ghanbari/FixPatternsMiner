package org.activityinfo.server.endpoint.gwtrpc;
/*
 * #%L
 * ActivityInfo Server
 * %%
 * Copyright (C) 2009 - 2013 UNICEF
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import org.activityinfo.legacy.shared.exception.CommandTimeoutException;
import org.activityinfo.legacy.shared.exception.UnexpectedCommandException;
import org.hibernate.Query;
import org.hibernate.ejb.HibernateEntityManager;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author yuriyz on 10/10/2014.
 */
public class AdvisoryLock implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(AdvisoryLock.class.getName());

    public static final String ADVISORY_LOCK_NAME = "activityinfo.remote_execution_context";

    public static final int ADVISORY_GET_LOCK_TIMEOUT = 10;
    public static final int SUCCESS_CODE = 1;
    private static final int TIMEOUT_CODE = 0;

    private final HibernateEntityManager entityManager;

    public AdvisoryLock(HibernateEntityManager entityManager) {
        Preconditions.checkNotNull(entityManager);

        this.entityManager = entityManager;

        try {
            Stopwatch stopwatch = Stopwatch.createStarted();

            String sql = String.format("SELECT GET_LOCK('%s', %s)", ADVISORY_LOCK_NAME, ADVISORY_GET_LOCK_TIMEOUT);

            Query query = entityManager.getSession().createSQLQuery(sql);
            Object result = query.uniqueResult();

            if (result == null) {
                throw new UnexpectedCommandException("Error occurred while trying to obtain advisory lock.");
            }

            int resultCode = ((Number) result).intValue();
            if (resultCode == TIMEOUT_CODE) { // time out
                LOGGER.severe("Timed out waiting for write lock.");
                throw new CommandTimeoutException();
            }

            if (resultCode != SUCCESS_CODE) { // not success
                LOGGER.severe("Failed to acquire lock, result code: " + resultCode);
                throw new RuntimeException("Failed to acquire lock, result code: " + resultCode);
            }

            stopwatch.stop();
            LOGGER.finest("Acquiring advisory lock takes: " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + "ms");
        } catch (CommandTimeoutException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Internal error during acquiring advisory lock: " + e.getMessage(), e);
            throw new RuntimeException("Exception caught while trying to acquire update lock", e);
        }
    }

    @Override
    public void close() throws Exception {
        Stopwatch stopwatch = Stopwatch.createStarted();
        String sql = String.format("SELECT RELEASE_LOCK('%s')", ADVISORY_LOCK_NAME);

        Query query = entityManager.getSession().createSQLQuery(sql);
        Object result = query.uniqueResult();
        int resultCode = ((Number) result).intValue();
        if (resultCode != SUCCESS_CODE) {
            throw new RuntimeException("Failed to release lock, result code: " + resultCode);
        }

        stopwatch.stop();
        LOGGER.finest("Release lock takes: " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + "ms");
    }

    public void closeQuietly() {
        try {
            close();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to release lock.", e);
        }
    }

    public static void closeQuietly(AdvisoryLock lock) {
        if (lock != null) {
            lock.closeQuietly();
        }
    }
}
