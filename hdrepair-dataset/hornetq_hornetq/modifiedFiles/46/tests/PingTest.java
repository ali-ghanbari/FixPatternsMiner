/*
 * Copyright 2009 Red Hat, Inc.
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.hornetq.tests.integration.remoting;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.hornetq.core.client.ClientSession;
import org.hornetq.core.client.ClientSessionFactory;
import org.hornetq.core.client.SessionFailureListener;
import org.hornetq.core.client.impl.ClientSessionFactoryImpl;
import org.hornetq.core.client.impl.ClientSessionFactoryInternal;
import org.hornetq.core.client.impl.FailoverManagerImpl;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.TransportConfiguration;
import org.hornetq.core.exception.HornetQException;
import org.hornetq.core.logging.Logger;
import org.hornetq.core.remoting.CloseListener;
import org.hornetq.core.remoting.Interceptor;
import org.hornetq.core.remoting.Packet;
import org.hornetq.core.remoting.RemotingConnection;
import org.hornetq.core.remoting.impl.wireformat.PacketImpl;
import org.hornetq.core.remoting.server.impl.RemotingServiceImpl;
import org.hornetq.core.server.HornetQServer;
import org.hornetq.tests.util.ServiceTestBase;

/**
 * @author <a href="mailto:jmesnil@redhat.com">Jeff Mesnil</a>
 * 
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 * 
 * @version <tt>$Revision$</tt>
 */
public class PingTest extends ServiceTestBase
{
   // Constants -----------------------------------------------------

   private static final Logger log = Logger.getLogger(PingTest.class);

   private static final long CLIENT_FAILURE_CHECK_PERIOD = 500;

   // Attributes ----------------------------------------------------

   private HornetQServer server;

   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   // Public --------------------------------------------------------

   @Override
   protected void setUp() throws Exception
   {
      super.setUp();
      Configuration config = createDefaultConfig(true);
      server = createServer(false, config);
      server.start();
   }

   @Override
   protected void tearDown() throws Exception
   {
      server.stop();
      server = null;
      super.tearDown();
   }

   class Listener implements SessionFailureListener
   {
      volatile HornetQException me;

      public void connectionFailed(HornetQException me)
      {
         this.me = me;
      }

      public HornetQException getException()
      {
         return me;
      }
      
      public void beforeReconnect(HornetQException exception)
      {               
      }
   };

   /*
    * Test that no failure listeners are triggered in a non failure case with pinging going on
    */
   public void testNoFailureWithPinging() throws Exception
   {
      TransportConfiguration transportConfig = new TransportConfiguration("org.hornetq.integration.transports.netty.NettyConnectorFactory");

      ClientSessionFactory csf = new ClientSessionFactoryImpl(transportConfig);

      csf.setClientFailureCheckPeriod(CLIENT_FAILURE_CHECK_PERIOD);
      csf.setConnectionTTL(CLIENT_FAILURE_CHECK_PERIOD * 2);

      ClientSession session = csf.createSession(false, true, true);
      
      log.info("Created session");

      assertEquals(1, ((ClientSessionFactoryInternal)csf).numConnections());

      Listener clientListener = new Listener();

      session.addFailureListener(clientListener);

      RemotingConnection serverConn = null;
      while (serverConn == null)
      {
         Set<RemotingConnection> conns = server.getRemotingService().getConnections();

         if (!conns.isEmpty())
         {
            serverConn = server.getRemotingService().getConnections().iterator().next();
         }
         else
         {
            // It's async so need to wait a while
            Thread.sleep(10);
         }
      }

      Listener serverListener = new Listener();

      serverConn.addFailureListener(serverListener);

      Thread.sleep(CLIENT_FAILURE_CHECK_PERIOD * 10);

      assertNull(clientListener.getException());

      assertNull(serverListener.getException());

      RemotingConnection serverConn2 = server.getRemotingService().getConnections().iterator().next();
      
      log.info("Server conn2 is " + serverConn2);

      assertTrue(serverConn == serverConn2);

      session.close();
      
      csf.close();
   }

   /*
    * Test that no failure listeners are triggered in a non failure case with no pinging going on
    */
   public void testNoFailureNoPinging() throws Exception
   {
      TransportConfiguration transportConfig = new TransportConfiguration("org.hornetq.integration.transports.netty.NettyConnectorFactory");

      ClientSessionFactory csf = new ClientSessionFactoryImpl(transportConfig);
      csf.setClientFailureCheckPeriod(-1);
      csf.setConnectionTTL(-1);

      ClientSession session = csf.createSession(false, true, true);

      assertEquals(1, ((ClientSessionFactoryInternal)csf).numConnections());

      Listener clientListener = new Listener();

      session.addFailureListener(clientListener);

      RemotingConnection serverConn = null;
      while (serverConn == null)
      {
         Set<RemotingConnection> conns = server.getRemotingService().getConnections();

         if (!conns.isEmpty())
         {
            serverConn = server.getRemotingService().getConnections().iterator().next();
         }
         else
         {
            // It's async so need to wait a while
            Thread.sleep(10);
         }
      }

      Listener serverListener = new Listener();

      serverConn.addFailureListener(serverListener);

      Thread.sleep(ClientSessionFactoryImpl.DEFAULT_CLIENT_FAILURE_CHECK_PERIOD);

      assertNull(clientListener.getException());

      assertNull(serverListener.getException());

      RemotingConnection serverConn2 = server.getRemotingService().getConnections().iterator().next();
      
      log.info("Serverconn2 is " + serverConn2);

      assertTrue(serverConn == serverConn2);

      session.close();
      
      csf.close();
   }

   /*
    * Test the server timing out a connection since it doesn't receive a ping in time
    */
   public void testServerFailureNoPing() throws Exception
   {
      TransportConfiguration transportConfig = new TransportConfiguration("org.hornetq.integration.transports.netty.NettyConnectorFactory");

      ClientSessionFactoryImpl csf = new ClientSessionFactoryImpl(transportConfig);

      csf.setClientFailureCheckPeriod(CLIENT_FAILURE_CHECK_PERIOD);
      csf.setConnectionTTL(CLIENT_FAILURE_CHECK_PERIOD * 2);

      Listener clientListener = new Listener();

      ClientSession session = csf.createSession(false, true, true);

      assertEquals(1, ((ClientSessionFactoryInternal)csf).numConnections());

      session.addFailureListener(clientListener);

      // We need to get it to stop pinging after one

      ((FailoverManagerImpl)csf.getFailoverManagers()[0]).stopPingingAfterOne();

      RemotingConnection serverConn = null;

      while (serverConn == null)
      {
         Set<RemotingConnection> conns = server.getRemotingService().getConnections();

         if (!conns.isEmpty())
         {
            serverConn = server.getRemotingService().getConnections().iterator().next();
         }
         else
         {
            // It's async so need to wait a while
            Thread.sleep(10);
         }
      }

      Listener serverListener = new Listener();

      serverConn.addFailureListener(serverListener);

      for (int i = 0; i < 1000; i++)
      {
         // a few tries to avoid a possible race caused by GCs or similar issues
         if (server.getRemotingService().getConnections().isEmpty() && clientListener.getException() != null)
         {
            break;
         }

         Thread.sleep(10);
      }
      
      if (!server.getRemotingService().getConnections().isEmpty())
      {
         RemotingConnection serverConn2 = server.getRemotingService().getConnections().iterator().next();
         
         log.info("Serverconn2 is " + serverConn2);
      }

      assertTrue(server.getRemotingService().getConnections().isEmpty());
            
      // The client listener should be called too since the server will close it from the server side which will result
      // in the
      // netty detecting closure on the client side and then calling failure listener
      assertNotNull(clientListener.getException());

      assertNotNull(serverListener.getException());

      session.close();
      
      csf.close();
   }
   
   /*
   * Test the client triggering failure due to no ping from server received in time
   */
   public void testClientFailureNoServerPing() throws Exception
   {
      // server must received at least one ping from the client to pass
      // so that the server connection TTL is configured with the client value
      final CountDownLatch pingOnServerLatch = new CountDownLatch(1);
      server.getRemotingService().addInterceptor(new Interceptor()
      {
         
         public boolean intercept(Packet packet, RemotingConnection connection) throws HornetQException
         {
            if (packet.getType() == PacketImpl.PING)
            {
               pingOnServerLatch.countDown();
            }
            return true;
         }
      });

      TransportConfiguration transportConfig = new TransportConfiguration("org.hornetq.integration.transports.netty.NettyConnectorFactory");

      ClientSessionFactory csf = new ClientSessionFactoryImpl(transportConfig);

      csf.setClientFailureCheckPeriod(CLIENT_FAILURE_CHECK_PERIOD);
      csf.setConnectionTTL(CLIENT_FAILURE_CHECK_PERIOD * 2);

      ClientSession session = csf.createSession(false, true, true);

      assertEquals(1, ((ClientSessionFactoryInternal)csf).numConnections());

      final CountDownLatch clientLatch = new CountDownLatch(1);
      SessionFailureListener clientListener = new SessionFailureListener()
      {
         public void connectionFailed(HornetQException me)
         {
            clientLatch.countDown();
         }

         public void beforeReconnect(HornetQException exception)
         {
         }
      };
      
      final CountDownLatch serverLatch = new CountDownLatch(1);
      CloseListener serverListener = new CloseListener()
      {         
         public void connectionClosed()
         {
            serverLatch.countDown();
         }
      };

      session.addFailureListener(clientListener);

      RemotingConnection serverConn = null;
      while (serverConn == null)
      {
         Set<RemotingConnection> conns = server.getRemotingService().getConnections();

         if (!conns.isEmpty())
         {
            serverConn = server.getRemotingService().getConnections().iterator().next();
         }
         else
         {
            // It's async so need to wait a while
            Thread.sleep(10);
         }
      }
      
      
      serverConn.addCloseListener(serverListener);
      assertTrue("server has not received any ping from the client" , pingOnServerLatch.await(2000, TimeUnit.MILLISECONDS));

      // we let the server receives at least 1 ping (so that it uses the client ConnectionTTL value)
      
      //Setting the handler to null will prevent server sending pings back to client
      serverConn.getChannel(0, -1).setHandler(null);

      assertTrue(clientLatch.await(4 * CLIENT_FAILURE_CHECK_PERIOD, TimeUnit.MILLISECONDS));
      
      //Server connection will be closed too, when client closes client side connection after failure is detected
      assertTrue(serverLatch.await(2 * RemotingServiceImpl.CONNECTION_TTL_CHECK_INTERVAL, TimeUnit.MILLISECONDS));

      long start = System.currentTimeMillis();
      while (true)
      {
         if (!server.getRemotingService().getConnections().isEmpty() && System.currentTimeMillis() - start < 10000)
         {
            Thread.sleep(500);
         }
         else
         {
            break;
         }
      }
      assertTrue(server.getRemotingService().getConnections().isEmpty());

      session.close();

      csf.close();
   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------

}