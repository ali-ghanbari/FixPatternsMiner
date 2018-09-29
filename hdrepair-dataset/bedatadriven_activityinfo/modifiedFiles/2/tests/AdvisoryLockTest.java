package org.activityinfo.server.command;
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

import com.google.api.client.repackaged.com.google.common.base.Joiner;
import com.google.inject.Inject;
import org.activityinfo.fixtures.InjectionSupport;
import org.activityinfo.legacy.shared.exception.CommandException;
import org.activityinfo.server.endpoint.gwtrpc.AdvisoryLock;
import org.hibernate.Query;
import org.hibernate.ejb.HibernateEntityManager;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.EntityManagerFactory;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author yuriyz on 10/13/2014.
 */
@RunWith(InjectionSupport.class)
public class AdvisoryLockTest extends CommandTestCase2 {

    @Inject
    protected EntityManagerFactory serverEntityManagerFactory;

    public void reset() {

        String sql = String.format("SELECT RELEASE_LOCK('%s')", AdvisoryLock.ADVISORY_LOCK_NAME);

        Query query = createEntityManager().getSession().createSQLQuery(sql);
        query.uniqueResult();

    }

    @Test
    @Ignore
    public void lockTest() throws CommandException, InterruptedException {
        int workCount = 25;

        reset();

        final List<Integer> startedWork = new CopyOnWriteArrayList<>();
        final List<Integer> finishedWork = new CopyOnWriteArrayList<>();

        for (int i = 0; i < workCount; i++) {
            final int workNumber = i;
            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    try (AdvisoryLock lock = new AdvisoryLock(createEntityManager())) {
                        // if added to started then lock is obtained
                        startedWork.add(workNumber);
                        System.out.println("Started work: " + Joiner.on(",").join(startedWork));
                        Thread.sleep(2000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    // if added to finished then lock is released
                    finishedWork.add(workNumber);
                    System.out.println("Finished work: " + Joiner.on(",").join(finishedWork));

                    // assert all previous are finished
                    for (int i = 0; i < startedWork.indexOf(workNumber); i++) {
                        if (!finishedWork.contains(startedWork.get(i))) {
                            throw new AssertionError("Advisory lock failed to lock workNumber:" + workNumber);
                        }
                    }
                }
            });
        }
        Thread.sleep(TimeUnit.SECONDS.toMillis(workCount));
        Assert.assertEquals(finishedWork.size(), workCount);
    }

    private HibernateEntityManager createEntityManager() {
        return (HibernateEntityManager) serverEntityManagerFactory.createEntityManager();
    }

}
