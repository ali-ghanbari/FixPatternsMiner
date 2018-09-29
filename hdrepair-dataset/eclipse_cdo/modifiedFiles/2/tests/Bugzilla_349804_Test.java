/**
 * Copyright (c) 2004 - 2011 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.tests.bugzilla;

import org.eclipse.emf.cdo.CDOLock;
import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.server.IRepository.WriteAccessHandler;
import org.eclipse.emf.cdo.server.IStoreAccessor.CommitContext;
import org.eclipse.emf.cdo.server.ITransaction;
import org.eclipse.emf.cdo.session.CDOSession;
import org.eclipse.emf.cdo.session.CDOSessionInvalidationEvent;
import org.eclipse.emf.cdo.tests.AbstractCDOTest;
import org.eclipse.emf.cdo.tests.model1.Company;
import org.eclipse.emf.cdo.transaction.CDOTransaction;
import org.eclipse.emf.cdo.util.CommitException;

import org.eclipse.net4j.util.event.IEvent;
import org.eclipse.net4j.util.event.IListener;
import org.eclipse.net4j.util.om.monitor.OMMonitor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Egidijus Vaisnora
 */
public class Bugzilla_349804_Test extends AbstractCDOTest
{
  public void testInvalidation() throws CommitException, InterruptedException
  {
    {
      CDOSession session = openSession();
      CDOTransaction transaction1 = session.openTransaction();

      transaction1.createResource(getResourcePath("test"));
      transaction1.commit();

      Failure handler = new Failure();
      getRepository().addHandler(handler);
      CDOTransaction failureTransaction = session.openTransaction();
      failureTransaction.createResource(getResourcePath("fail"));

      try
      {
        // Creating failure commit. It will change last update time on server TimeStampAuthority
        failureTransaction.commit();
        fail("CommitException expected");
      }
      catch (CommitException expected)
      {
      }

      getRepository().removeHandler(handler);
      session.close();
    }

    CDOSession session = openSession();
    CDOTransaction transaction = session.openTransaction();

    final CountDownLatch invalidationLatch = new CountDownLatch(1);
    session.addListener(new IListener()
    {
      public void notifyEvent(IEvent event)
      {
        if (event instanceof CDOSessionInvalidationEvent)
        {
          invalidationLatch.countDown();
        }
      }
    });

    Company createCompany = getModel1Factory().createCompany();
    CDOResource resource = transaction.getResource(getResourcePath("test"));
    resource.getContents().add(createCompany);

    // Invalidation shall fail, because it will use lastUpdateTime from TimeStampAuthority for commit result
    transaction.commit();

    invalidationLatch.await(500, TimeUnit.MILLISECONDS);
    assertEquals("Invalidation was not delivered", 0, invalidationLatch.getCount());
  }

  public void testDeadlockWithLocking() throws CommitException, InterruptedException, TimeoutException
  {
    {
      CDOSession session = openSession();
      CDOTransaction transaction1 = session.openTransaction();

      transaction1.createResource(getResourcePath("test"));
      transaction1.commit();

      Failure handler = new Failure();
      getRepository().addHandler(handler);

      CDOTransaction failureTransaction = session.openTransaction();
      failureTransaction.createResource(getResourcePath("fail"));

      try
      {
        // Creating failure commit. It will change last update time on server TimeStampAuthority
        failureTransaction.commit();
        fail("CommitException expected");
      }
      catch (CommitException expected)
      {
      }

      getRepository().removeHandler(handler);
      session.close();
    }

    CDOSession session = openSession();
    CDOTransaction transaction = session.openTransaction();
    CDOTransaction updaterTransaction = session.openTransaction();

    CDOResource resourceOnUpdater = updaterTransaction.getResource(getResourcePath("test"));
    // Resolve PROXY state
    resourceOnUpdater.getName();

    Company createCompany = getModel1Factory().createCompany();
    CDOResource resource = transaction.getResource(getResourcePath("test"));
    resource.getContents().add(createCompany);
    // Invalidation shall fail, because it will use lastUpdateTime from TimeStampAuthority for commit result
    transaction.commit();

    CDOLock cdoWriteLock = resourceOnUpdater.cdoWriteLock();
    // Waiting for commit which already happen
    cdoWriteLock.lock(1000);
  }

  /**
   * @author Egidijus Vaisnora
   */
  private class Failure implements WriteAccessHandler
  {
    public void handleTransactionBeforeCommitting(ITransaction transaction, CommitContext commitContext,
        OMMonitor monitor) throws RuntimeException
    {
      throw new IllegalArgumentException("Fail on purpose");
    }

    public void handleTransactionAfterCommitted(ITransaction transaction, CommitContext commitContext, OMMonitor monitor)
    {
      // Do nothing
    }
  }
}
