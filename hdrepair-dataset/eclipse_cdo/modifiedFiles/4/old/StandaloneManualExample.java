/**
 * Copyright (c) 2004 - 2010 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.examples;

import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.examples.company.CompanyFactory;
import org.eclipse.emf.cdo.examples.company.CompanyPackage;
import org.eclipse.emf.cdo.net4j.CDONet4jUtil;
import org.eclipse.emf.cdo.net4j.CDOSessionConfiguration;
import org.eclipse.emf.cdo.session.CDOSession;
import org.eclipse.emf.cdo.transaction.CDOTransaction;

import org.eclipse.net4j.FactoriesProtocolProvider;
import org.eclipse.net4j.Net4jUtil;
import org.eclipse.net4j.buffer.IBufferProvider;
import org.eclipse.net4j.protocol.IProtocolProvider;
import org.eclipse.net4j.util.lifecycle.LifecycleUtil;
import org.eclipse.net4j.util.om.OMPlatform;
import org.eclipse.net4j.util.om.log.PrintLogHandler;
import org.eclipse.net4j.util.om.trace.PrintTraceHandler;

import org.eclipse.emf.ecore.EObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * @author Eike Stepper
 */
public class StandaloneManualExample
{
  @SuppressWarnings("restriction")
  public static void main(String[] args)
  {
    // Enable logging and tracing
    OMPlatform.INSTANCE.setDebugging(true);
    OMPlatform.INSTANCE.addLogHandler(PrintLogHandler.CONSOLE);
    OMPlatform.INSTANCE.addTraceHandler(PrintTraceHandler.CONSOLE);

    // Prepare receiveExecutor
    final ThreadGroup threadGroup = new ThreadGroup("net4j"); //$NON-NLS-1$
    ExecutorService receiveExecutor = Executors.newCachedThreadPool(new ThreadFactory()
    {
      public Thread newThread(Runnable r)
      {
        Thread thread = new Thread(threadGroup, r);
        thread.setDaemon(true);
        return thread;
      }
    });

    // Prepare bufferProvider
    IBufferProvider bufferProvider = Net4jUtil.createBufferPool();
    LifecycleUtil.activate(bufferProvider);

    IProtocolProvider protocolProvider = new FactoriesProtocolProvider(
        new org.eclipse.emf.cdo.internal.net4j.protocol.CDOClientProtocolFactory());

    // Prepare selector
    org.eclipse.net4j.internal.tcp.TCPSelector selector = new org.eclipse.net4j.internal.tcp.TCPSelector();
    selector.activate();

    // Prepare connector
    org.eclipse.net4j.internal.tcp.TCPClientConnector connector = new org.eclipse.net4j.internal.tcp.TCPClientConnector();
    connector.getConfig().setBufferProvider(bufferProvider);
    connector.getConfig().setReceiveExecutor(receiveExecutor);
    connector.getConfig().setProtocolProvider(protocolProvider);
    connector.getConfig().setNegotiator(null);
    connector.setSelector(selector);
    connector.setHost("localhost"); //$NON-NLS-1$
    connector.setPort(2036);
    connector.activate();

    // Create configuration
    CDOSessionConfiguration configuration = CDONet4jUtil.createSessionConfiguration();
    configuration.setConnector(connector);
    configuration.setRepositoryName("repo1"); //$NON-NLS-1$

    // Open session
    CDOSession session = configuration.openSession();
    session.getPackageRegistry().putEPackage(CompanyPackage.eINSTANCE);

    // Open transaction
    CDOTransaction transaction = session.openTransaction();

    // Get or create resource
    CDOResource resource = transaction.getOrCreateResource("/path/to/my/resource"); //$NON-NLS-1$

    // Work with the resource and commit the transaction
    EObject object = CompanyFactory.eINSTANCE.createCompany();
    resource.getContents().add(object);
    transaction.commit();

    // Cleanup
    session.close();
    connector.deactivate();
  }
}
