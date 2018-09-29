/***********************************************************************
 * Copyright (c) 1999-2006 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/
package org.apache.james.smtpserver;

import org.apache.avalon.cornerstone.services.sockets.SocketManager;
import org.apache.avalon.cornerstone.services.store.Store;
import org.apache.avalon.cornerstone.services.threads.ThreadManager;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.commons.net.smtp.SMTPClient;
import org.apache.commons.net.smtp.SMTPReply;
import org.apache.james.Constants;
import org.apache.james.core.MailImpl;
import org.apache.james.services.DNSServer;
import org.apache.james.services.JamesConnectionManager;
import org.apache.james.services.MailServer;
import org.apache.james.services.UsersRepository;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.test.mock.avalon.MockServiceManager;
import org.apache.james.test.mock.avalon.MockSocketManager;
import org.apache.james.test.mock.avalon.MockStore;
import org.apache.james.test.mock.avalon.MockThreadManager;
import org.apache.james.test.mock.james.InMemorySpoolRepository;
import org.apache.james.test.mock.james.MockMailServer;
import org.apache.james.test.mock.mailet.MockMailContext;
import org.apache.james.test.mock.mailet.MockMailetConfig;
import org.apache.james.test.util.Util;
import org.apache.james.transport.mailets.RemoteDelivery;
import org.apache.james.userrepository.MockUsersRepository;
import org.apache.james.util.Base64;
import org.apache.james.util.connection.SimpleConnectionManager;
import org.apache.mailet.MailAddress;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

/**
 * Tests the org.apache.james.smtpserver.SMTPServer unit 
 */
public class SMTPServerTest extends TestCase {
    private int m_smtpListenerPort = Util.getNonPrivilegedPort();
    private MockMailServer m_mailServer;
    private SMTPTestConfiguration m_testConfiguration;
    private SMTPServer m_smtpServer;
    private MockUsersRepository m_usersRepository = new MockUsersRepository();

    public SMTPServerTest() {
        super("SMTPServerTest");
    }

    public void verifyLastMail(String sender, String recipient, MimeMessage msg) throws IOException, MessagingException {
        Object[] mailData = m_mailServer.getLastMail();
        assertNotNull("mail received by mail server", mailData);

        if (sender == null && recipient == null && msg == null) fail("no verification can be done with all arguments null");

        if (sender != null) assertEquals("sender verfication", sender, ((MailAddress)mailData[0]).toString());
        if (recipient != null) assertTrue("recipient verfication", ((Collection) mailData[1]).contains(new MailAddress(recipient)));
        if (msg != null) {
            ByteArrayOutputStream bo1 = new ByteArrayOutputStream();
            msg.writeTo(bo1);
            ByteArrayOutputStream bo2 = new ByteArrayOutputStream();
            ((MimeMessage) mailData[2]).writeTo(bo2);
            assertEquals(bo1.toString(),bo2.toString());
            assertEquals("message verification", msg, ((MimeMessage) mailData[2]));
        }
    }
    
    protected void setUp() throws Exception {
        m_smtpServer = new SMTPServer();
        ContainerUtil.enableLogging(m_smtpServer,new MockLogger());
        m_serviceManager = setUpServiceManager();
        ContainerUtil.service(m_smtpServer, m_serviceManager);
        m_testConfiguration = new SMTPTestConfiguration(m_smtpListenerPort);
    }

    private void finishSetUp(SMTPTestConfiguration testConfiguration) throws Exception {
        testConfiguration.init();
        ContainerUtil.configure(m_smtpServer, testConfiguration);
        ContainerUtil.initialize(m_smtpServer);
        m_mailServer.setMaxMessageSizeBytes(m_testConfiguration.getMaxMessageSize());
    }

    private MockServiceManager setUpServiceManager() throws Exception {
        m_serviceManager = new MockServiceManager();
        SimpleConnectionManager connectionManager = new SimpleConnectionManager();
        ContainerUtil.enableLogging(connectionManager, new MockLogger());
        m_serviceManager.put(JamesConnectionManager.ROLE, connectionManager);
        m_serviceManager.put("org.apache.mailet.MailetContext", new MockMailContext());
        m_mailServer = new MockMailServer();
        m_serviceManager.put(MailServer.ROLE, m_mailServer);
        m_serviceManager.put(UsersRepository.ROLE, m_usersRepository);
        m_serviceManager.put(SocketManager.ROLE, new MockSocketManager(m_smtpListenerPort));
        m_serviceManager.put(ThreadManager.ROLE, new MockThreadManager());
        // Mock DNS Server
        DNSServer dns = new DNSServer() {

            public Collection findMXRecords(String hostname) {
                List res = new ArrayList();
                if (hostname == null) {
                    return res;
                };
                if ("james.apache.org".equals(hostname)) {
                    res.add("nagoya.apache.org");
                }
                return res;
            }

            public Iterator getSMTPHostAddresses(String domainName) {
                throw new UnsupportedOperationException("Unimplemented mock service");
            }
            

            public InetAddress[] getAllByName(String host) throws UnknownHostException
            {
                return new InetAddress[] {getByName(host)};
//                throw new UnsupportedOperationException("getByName not implemented in mock for host: "+host);
            }


            public InetAddress getByName(String host) throws UnknownHostException
            {
                return InetAddress.getByName(host);
//                throw new UnsupportedOperationException("getByName not implemented in mock for host: "+host);
            }
            
            public Collection findTXTRecords(String hostname) {
                List res = new ArrayList();
                if (hostname == null) {
                    return res;
                };
                if ("2.0.0.127.bl.spamcop.net".equals(hostname)) {
                    res.add("Blocked - see http://www.spamcop.net/bl.shtml?127.0.0.2");
                }
                return res;
            }
            
        };
        m_serviceManager.put(DNSServer.ROLE, dns);
        m_serviceManager.put(Store.ROLE, new MockStore());
        return m_serviceManager;
    }

    public void testSimpleMailSendWithEHLO() throws Exception {
        finishSetUp(m_testConfiguration);
        
        SMTPClient smtpProtocol = new SMTPClient();
        smtpProtocol.connect("127.0.0.1", m_smtpListenerPort);

        // no message there, yet
        assertNull("no mail received by mail server", m_mailServer.getLastMail());

        smtpProtocol.sendCommand("EHLO "+InetAddress.getLocalHost());
        String[] capabilityRes = smtpProtocol.getReplyStrings();
        
        List capabilitieslist = new ArrayList();
        for (int i = 1; i < capabilityRes.length; i++) {
            capabilitieslist.add(capabilityRes[i].substring(4));
        }
        
        assertEquals("capabilities", 2, capabilitieslist.size());
        assertTrue("capabilities present PIPELINING", capabilitieslist.contains("PIPELINING"));
        assertTrue("capabilities present ENHANCEDSTATUSCODES", capabilitieslist.contains("ENHANCEDSTATUSCODES"));
        //assertTrue("capabilities present 8BITMIME", capabilitieslist.contains("8BITMIME"));

        smtpProtocol.setSender("mail@localhost");
        smtpProtocol.addRecipient("mail@localhost");

        smtpProtocol.sendShortMessageData("Subject: test\r\n\r\nBody\r\n\r\n.\r\n");
        smtpProtocol.quit();
        smtpProtocol.disconnect();

        // mail was propagated by SMTPServer
        assertNotNull("mail received by mail server", m_mailServer.getLastMail());
    }

    public void testEmptyMessage() throws Exception {
        finishSetUp(m_testConfiguration);

        SMTPClient smtp = new SMTPClient();
        smtp.connect("127.0.0.1", m_smtpListenerPort);

        // no message there, yet
        assertNull("no mail received by mail server", m_mailServer.getLastMail());

        smtp.helo(InetAddress.getLocalHost().toString());
        
        smtp.setSender("mail@localhost");
        
        smtp.addRecipient("mail@localhost");

        smtp.sendShortMessageData("");

        smtp.quit();
        
        smtp.disconnect();

        // mail was propagated by SMTPServer
        assertNotNull("mail received by mail server", m_mailServer.getLastMail());

        // added to check a NPE in the test (JAMES-474) due to MockMailServer
        // not cloning the message (added a MimeMessageCopyOnWriteProxy there)
        System.gc();

        int size = ((MimeMessage) m_mailServer.getLastMail()[2]).getSize();

        assertEquals(size, 2);
    }

    public void testSimpleMailSendWithHELO() throws Exception {
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol = new SMTPClient();
        smtpProtocol.connect("127.0.0.1", m_smtpListenerPort);

        // no message there, yet
        assertNull("no mail received by mail server", m_mailServer.getLastMail());

        smtpProtocol.helo(InetAddress.getLocalHost().toString());
        
        smtpProtocol.setSender("mail@localhost");
        
        smtpProtocol.addRecipient("mail@localhost");

        smtpProtocol.sendShortMessageData("Subject: test mail\r\n\r\nTest body\r\n.\r\n");

        smtpProtocol.quit();
        smtpProtocol.disconnect();

        // mail was propagated by SMTPServer
        assertNotNull("mail received by mail server", m_mailServer.getLastMail());
    }

    public void testTwoSimultaneousMails() throws Exception {
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol1 = new SMTPClient();
        smtpProtocol1.connect("127.0.0.1", m_smtpListenerPort);
        SMTPClient smtpProtocol2 = new SMTPClient();
        smtpProtocol2.connect("127.0.0.1", m_smtpListenerPort);

        assertTrue("first connection taken",smtpProtocol1.isConnected());
        assertTrue("second connection taken",smtpProtocol2.isConnected());

        // no message there, yet
        assertNull("no mail received by mail server", m_mailServer.getLastMail());

        smtpProtocol1.helo(InetAddress.getLocalHost().toString());
        smtpProtocol2.helo(InetAddress.getLocalHost().toString());

        String sender1 = "mail_sender1@localhost";
        String recipient1 = "mail_recipient1@localhost";
        smtpProtocol1.setSender(sender1);
        smtpProtocol1.addRecipient(recipient1);

        String sender2 = "mail_sender2@localhost";
        String recipient2 = "mail_recipient2@localhost";
        smtpProtocol2.setSender(sender2);
        smtpProtocol2.addRecipient(recipient2);

        smtpProtocol1.sendShortMessageData("Subject: test\r\n\r\nTest body\r\n.\r\n");
        verifyLastMail(sender1, recipient1, null);
            
        smtpProtocol2.sendShortMessageData("Subject: test\r\n\r\nTest body\r\n.\r\n");
        verifyLastMail(sender2, recipient2, null);

        smtpProtocol1.quit();
        smtpProtocol2.quit();
        
        smtpProtocol1.disconnect();
        smtpProtocol2.disconnect();
    }

    public void testTwoMailsInSequence() throws Exception {
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol1 = new SMTPClient();
        smtpProtocol1.connect("127.0.0.1", m_smtpListenerPort);

        assertTrue("first connection taken", smtpProtocol1.isConnected());

        // no message there, yet
        assertNull("no mail received by mail server", m_mailServer.getLastMail());

        smtpProtocol1.helo(InetAddress.getLocalHost().toString());

        String sender1 = "mail_sender1@localhost";
        String recipient1 = "mail_recipient1@localhost";
        smtpProtocol1.setSender(sender1);
        smtpProtocol1.addRecipient(recipient1);

        smtpProtocol1.sendShortMessageData("Subject: test\r\n\r\nTest body\r\n");
        verifyLastMail(sender1, recipient1, null);
            
        String sender2 = "mail_sender2@localhost";
        String recipient2 = "mail_recipient2@localhost";
        smtpProtocol1.setSender(sender2);
        smtpProtocol1.addRecipient(recipient2);

        smtpProtocol1.sendShortMessageData("Subject: test2\r\n\r\nTest body2\r\n");
        verifyLastMail(sender2, recipient2, null);

        smtpProtocol1.quit();
        smtpProtocol1.disconnect();
    }
    
    public void testHeloResolv() throws Exception {
        m_testConfiguration.setHeloResolv();
        m_testConfiguration.setAuthorizedAddresses("192.168.0.1");
        finishSetUp(m_testConfiguration);


        SMTPClient smtpProtocol1 = new SMTPClient();
        smtpProtocol1.connect("127.0.0.1", m_smtpListenerPort);

        assertTrue("first connection taken", smtpProtocol1.isConnected());

        // no message there, yet
        assertNull("no mail received by mail server", m_mailServer.getLastMail());

        String helo1 = "abgsfe3rsf.de";
        String helo2 = "james.apache.org";
        
        smtpProtocol1.sendCommand("helo",helo1);
        // this should give a 501 code cause the helo could not resolved
        assertEquals("expected error: helo could not resolved", 501, smtpProtocol1.getReplyCode());
            
        smtpProtocol1.sendCommand("helo", helo2);
        // helo is resolvable. so this should give a 250 code
        assertEquals("Helo accepted", 250, smtpProtocol1.getReplyCode());

        smtpProtocol1.quit();
    }
    
    public void testHeloResolvDefault() throws Exception {
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol1 = new SMTPClient();
        smtpProtocol1.connect("127.0.0.1", m_smtpListenerPort);
        
        smtpProtocol1.helo("abgsfe3rsf.de");
        // helo should not be checked. so this should give a 250 code
        assertEquals("Helo accepted", 250, smtpProtocol1.getReplyCode());

        smtpProtocol1.quit();
    }
    
    public void testSenderDomainResolv() throws Exception {
        m_testConfiguration.setSenderDomainResolv();
        m_testConfiguration.setAuthorizedAddresses("192.168.0.1/32");
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol1 = new SMTPClient();
        smtpProtocol1.connect("127.0.0.1", m_smtpListenerPort);

        
        assertTrue("first connection taken", smtpProtocol1.isConnected());

        // no message there, yet
        assertNull("no mail received by mail server", m_mailServer.getLastMail());

        smtpProtocol1.helo(InetAddress.getLocalHost().toString());

        String sender1 = "mail_sender1@xfwrqqfgfe.de";
        String sender2 = "mail_sender2@james.apache.org";
        
        smtpProtocol1.setSender(sender1);
        assertEquals("expected 501 error", 501, smtpProtocol1.getReplyCode());
    
        smtpProtocol1.setSender(sender2);

        smtpProtocol1.quit();
        
    }
 
    public void testSenderDomainResolvDefault() throws Exception {
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol1 = new SMTPClient();
        smtpProtocol1.connect("127.0.0.1", m_smtpListenerPort);
        
        smtpProtocol1.helo(InetAddress.getLocalHost().toString());
        
        String sender1 = "mail_sender1@xfwrqqfgfe.de";
        
        smtpProtocol1.setSender(sender1);

        smtpProtocol1.quit();
    }
    
    public void testSenderDomainResolvRelayClientDefault() throws Exception {
        m_testConfiguration.setSenderDomainResolv();
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol1 = new SMTPClient();
        smtpProtocol1.connect("127.0.0.1", m_smtpListenerPort);

        
        assertTrue("first connection taken", smtpProtocol1.isConnected());

        // no message there, yet
        assertNull("no mail received by mail server", m_mailServer.getLastMail());

        smtpProtocol1.helo(InetAddress.getLocalHost().toString());

        String sender1 = "mail_sender1@xfwrqqfgfe.de";
        
        // Both mail shold
        smtpProtocol1.setSender(sender1);

        smtpProtocol1.quit();
        
    }
    
    public void testSenderDomainResolvRelayClient() throws Exception {
        m_testConfiguration.setSenderDomainResolv();
        m_testConfiguration.setCheckAuthClients(true);
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol1 = new SMTPClient();
        smtpProtocol1.connect("127.0.0.1", m_smtpListenerPort);

        
        assertTrue("first connection taken", smtpProtocol1.isConnected());

        // no message there, yet
        assertNull("no mail received by mail server", m_mailServer.getLastMail());

        smtpProtocol1.helo(InetAddress.getLocalHost().toString());

        String sender1 = "mail_sender1@xfwrqqfgfe.de";
        String sender2 = "mail_sender2@james.apache.org";
        
        smtpProtocol1.setSender(sender1);
        assertEquals("expected 501 error", 501, smtpProtocol1.getReplyCode());
    
        smtpProtocol1.setSender(sender2);

        smtpProtocol1.quit();
        
    }
    
    public void testMaxRcpt() throws Exception {
        m_testConfiguration.setMaxRcpt(1);
        finishSetUp(m_testConfiguration);


        SMTPClient smtpProtocol1 = new SMTPClient();
        smtpProtocol1.connect("127.0.0.1", m_smtpListenerPort);

        assertTrue("first connection taken", smtpProtocol1.isConnected());

        // no message there, yet
        assertNull("no mail received by mail server", m_mailServer.getLastMail());

        smtpProtocol1.helo(InetAddress.getLocalHost().toString());

        String sender1 = "mail_sender1@james.apache.org";
        String rcpt1 = "test@localhost";
        String rcpt2 = "test2@localhost";
    
        smtpProtocol1.setSender(sender1);
        smtpProtocol1.addRecipient(rcpt1);

        smtpProtocol1.addRecipient(rcpt2);
        assertEquals("expected 452 error", 452, smtpProtocol1.getReplyCode());
        
        smtpProtocol1.sendShortMessageData("Subject: test\r\n\r\nTest body\r\n");
        
        // After the data is send the rcpt count is set back to 0.. So a new mail with rcpt should be accepted
        
        smtpProtocol1.setSender(sender1);
 
        smtpProtocol1.addRecipient(rcpt1);
        
        smtpProtocol1.sendShortMessageData("Subject: test\r\n\r\nTest body\r\n");
        
        smtpProtocol1.quit();
        
    }

    public void testMaxRcptDefault() throws Exception {
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol1 = new SMTPClient();
        smtpProtocol1.connect("127.0.0.1", m_smtpListenerPort);
        
        smtpProtocol1.helo(InetAddress.getLocalHost().toString());
        
        String sender1 = "mail_sender1@james.apache.org";
        String rcpt1 = "test@localhost";
        
        smtpProtocol1.setSender(sender1);
        
        smtpProtocol1.addRecipient(rcpt1);
        
        smtpProtocol1.sendShortMessageData("Subject: test\r\n\r\nTest body\r\n");
        
        smtpProtocol1.quit();
    }
  
    public void testEhloResolv() throws Exception {
        m_testConfiguration.setEhloResolv();
        m_testConfiguration.setAuthorizedAddresses("192.168.0.1");
        finishSetUp(m_testConfiguration);


        SMTPClient smtpProtocol1 = new SMTPClient();
        smtpProtocol1.connect("127.0.0.1", m_smtpListenerPort);

        assertTrue("first connection taken", smtpProtocol1.isConnected());

        // no message there, yet
        assertNull("no mail received by mail server", m_mailServer.getLastMail());

        String ehlo1 = "abgsfe3rsf.de";
        String ehlo2 = "james.apache.org";
        
        smtpProtocol1.sendCommand("ehlo", ehlo1);
        // this should give a 501 code cause the ehlo could not resolved
        assertEquals("expected error: ehlo could not resolved", 501, smtpProtocol1.getReplyCode());
            
        smtpProtocol1.sendCommand("ehlo", ehlo2);
        // ehlo is resolvable. so this should give a 250 code
        assertEquals("ehlo accepted", 250, smtpProtocol1.getReplyCode());

        smtpProtocol1.quit();
    }
    
    public void testEhloResolvDefault() throws Exception {
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol1 = new SMTPClient();
        smtpProtocol1.connect("127.0.0.1", m_smtpListenerPort);
        
        smtpProtocol1.sendCommand("ehlo","abgsfe3rsf.de");
        // ehlo should not be checked. so this should give a 250 code
        assertEquals("ehlo accepted", 250, smtpProtocol1.getReplyCode());

        smtpProtocol1.quit();
    }
    
    public void testEhloResolvIgnoreClientDisabled() throws Exception {
        m_testConfiguration.setEhloResolv();
        m_testConfiguration.setCheckAuthNetworks(true);
        finishSetUp(m_testConfiguration);


        SMTPClient smtpProtocol1 = new SMTPClient();
        smtpProtocol1.connect("127.0.0.1", m_smtpListenerPort);

        assertTrue("first connection taken", smtpProtocol1.isConnected());

        // no message there, yet
        assertNull("no mail received by mail server", m_mailServer.getLastMail());

        String ehlo1 = "abgsfe3rsf.de";
        String ehlo2 = "james.apache.org";
        
        smtpProtocol1.sendCommand("ehlo", ehlo1);
        // this should give a 501 code cause the ehlo could not resolved
        assertEquals("expected error: ehlo could not resolved", 501, smtpProtocol1.getReplyCode());
            
        smtpProtocol1.sendCommand("ehlo", ehlo2);
        // ehlo is resolvable. so this should give a 250 code
        assertEquals("ehlo accepted", 250, smtpProtocol1.getReplyCode());

        smtpProtocol1.quit();
    }
    
    public void testHeloEnforcement() throws Exception {
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol1 = new SMTPClient();
        smtpProtocol1.connect("127.0.0.1", m_smtpListenerPort);

        assertTrue("first connection taken", smtpProtocol1.isConnected());

        // no message there, yet
        assertNull("no mail received by mail server", m_mailServer.getLastMail());

        String sender1 = "mail_sender1@localhost";
        smtpProtocol1.setSender(sender1);
        assertEquals("expected 503 error", 503, smtpProtocol1.getReplyCode());
        
        smtpProtocol1.helo(InetAddress.getLocalHost().toString());
        
        smtpProtocol1.setSender(sender1);
        
        smtpProtocol1.quit();
    }
    
    public void testHeloEnforcementDisabled() throws Exception {
        m_testConfiguration.setHeloEhloEnforcement(false);
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol1 = new SMTPClient();
        smtpProtocol1.connect("127.0.0.1", m_smtpListenerPort);

        assertTrue("first connection taken", smtpProtocol1.isConnected());

        // no message there, yet
        assertNull("no mail received by mail server", m_mailServer.getLastMail());

        String sender1 = "mail_sender1@localhost";
        
        smtpProtocol1.setSender(sender1);
        
        smtpProtocol1.quit();
    }

    public void testAuth() throws Exception {
        m_testConfiguration.setAuthorizedAddresses("128.0.0.1/8");
        m_testConfiguration.setAuthorizingAnnounce();
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol = new SMTPClient();
        smtpProtocol.connect("127.0.0.1", m_smtpListenerPort);

        smtpProtocol.sendCommand("ehlo", InetAddress.getLocalHost().toString());
        String[] capabilityRes = smtpProtocol.getReplyStrings();
        
        List capabilitieslist = new ArrayList();
        for (int i = 1; i < capabilityRes.length; i++) {
            capabilitieslist.add(capabilityRes[i].substring(4));
        }
            
        assertTrue("anouncing auth required", capabilitieslist.contains("AUTH LOGIN PLAIN"));
        // is this required or just for compatibility? assertTrue("anouncing auth required", capabilitieslist.contains("AUTH=LOGIN PLAIN"));

        String userName = "test_user_smtp";
        String noexistUserName = "noexist_test_user_smtp";
        String sender ="test_user_smtp@localhost";
        smtpProtocol.sendCommand("AUTH FOO", null);
        assertEquals("expected error: unrecognized authentication type", 504, smtpProtocol.getReplyCode());

        smtpProtocol.setSender(sender);

        smtpProtocol.addRecipient("mail@sample.com");
        assertEquals("expected 530 error", 530, smtpProtocol.getReplyCode());

        assertFalse("user not existing", m_usersRepository.contains(noexistUserName));

        smtpProtocol.sendCommand("AUTH PLAIN");
        smtpProtocol.sendCommand(Base64.encodeAsString("\0"+noexistUserName+"\0pwd\0"));
//        smtpProtocol.sendCommand(noexistUserName+"pwd".toCharArray());
        assertEquals("expected error", 535, smtpProtocol.getReplyCode());

        m_usersRepository.addUser(userName, "pwd");

        smtpProtocol.sendCommand("AUTH PLAIN");
        smtpProtocol.sendCommand(Base64.encodeAsString("\0"+userName+"\0wrongpwd\0"));
        assertEquals("expected error", 535, smtpProtocol.getReplyCode());

        smtpProtocol.sendCommand("AUTH PLAIN");
        smtpProtocol.sendCommand(Base64.encodeAsString("\0"+userName+"\0pwd\0"));
        assertEquals("authenticated", 235, smtpProtocol.getReplyCode());

        smtpProtocol.sendCommand("AUTH PLAIN");
        assertEquals("expected error: User has previously authenticated.", 503, smtpProtocol.getReplyCode());

        smtpProtocol.addRecipient("mail@sample.com");
        smtpProtocol.sendShortMessageData("Subject: test\r\n\r\nTest body\r\n");

        smtpProtocol.quit();

        // mail was propagated by SMTPServer
        assertNotNull("mail received by mail server", m_mailServer.getLastMail());
    }

    public void testAuthWithEmptySender() throws Exception {
        m_testConfiguration.setAuthorizedAddresses("128.0.0.1/8");
        m_testConfiguration.setAuthorizingAnnounce();
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol = new SMTPClient();
        smtpProtocol.connect("127.0.0.1", m_smtpListenerPort);

        smtpProtocol.sendCommand("ehlo "+InetAddress.getLocalHost());

        String userName = "test_user_smtp";
        m_usersRepository.addUser(userName, "pwd");

        smtpProtocol.setSender("");

        smtpProtocol.sendCommand("AUTH PLAIN");
        smtpProtocol.sendCommand(Base64.encodeAsString("\0"+userName+"\0pwd\0"));
        assertEquals("authenticated", 235, smtpProtocol.getReplyCode());

        smtpProtocol.addRecipient("mail@sample.com");
        assertEquals("expected error", 503, smtpProtocol.getReplyCode());
        
        smtpProtocol.quit();
    }

    public void testNoRecepientSpecified() throws Exception {
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol = new SMTPClient();
        smtpProtocol.connect("127.0.0.1", m_smtpListenerPort);

        smtpProtocol.sendCommand("ehlo "+InetAddress.getLocalHost());

        smtpProtocol.setSender("mail@sample.com");

        // left out for test smtpProtocol.rcpt(new Address("mail@localhost"));

        smtpProtocol.sendShortMessageData("Subject: test\r\n\r\nTest body\r\n");
        assertTrue("sending succeeded without recepient", SMTPReply.isNegativePermanent(smtpProtocol.getReplyCode()));

        smtpProtocol.quit();

        // mail was propagated by SMTPServer
        assertNull("no mail received by mail server", m_mailServer.getLastMail());
    }

    public void testMultipleMailsAndRset() throws Exception {
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol = new SMTPClient();
        smtpProtocol.connect("127.0.0.1", m_smtpListenerPort);

        smtpProtocol.sendCommand("ehlo "+InetAddress.getLocalHost());

        smtpProtocol.setSender("mail@sample.com");
        
        smtpProtocol.reset();
        
        smtpProtocol.setSender("mail@sample.com");

        smtpProtocol.quit();

        // mail was propagated by SMTPServer
        assertNull("no mail received by mail server", m_mailServer.getLastMail());
    }

    public void testRelayingDenied() throws Exception {
        m_testConfiguration.setAuthorizedAddresses("128.0.0.1/8");
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol = new SMTPClient();
        smtpProtocol.connect("127.0.0.1", m_smtpListenerPort);

        smtpProtocol.sendCommand("ehlo "+InetAddress.getLocalHost());

        smtpProtocol.setSender("mail@sample.com");

        smtpProtocol.addRecipient("maila@sample.com");
        assertEquals("expected 550 error", 550, smtpProtocol.getReplyCode());
    }

    public void testHandleAnnouncedMessageSizeLimitExceeded() throws Exception {
        m_testConfiguration.setMaxMessageSize(1); // set message limit to 1kb
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol = new SMTPClient();
        smtpProtocol.connect("127.0.0.1", m_smtpListenerPort);

        smtpProtocol.sendCommand("ehlo "+InetAddress.getLocalHost());

        smtpProtocol.sendCommand("MAIL FROM:<mail@localhost> SIZE=1025", null);
        assertEquals("expected error: max msg size exceeded", 552, smtpProtocol.getReplyCode());

        smtpProtocol.addRecipient("mail@localhost");
        assertEquals("expected error", 503, smtpProtocol.getReplyCode());
    }

    public void testHandleMessageSizeLimitExceeded() throws Exception {
        m_testConfiguration.setMaxMessageSize(1); // set message limit to 1kb 
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol = new SMTPClient();
        smtpProtocol.connect("127.0.0.1", m_smtpListenerPort);

        smtpProtocol.sendCommand("ehlo "+InetAddress.getLocalHost());

        smtpProtocol.setSender("mail@localhost");
        smtpProtocol.addRecipient("mail@localhost");

        Writer wr = smtpProtocol.sendMessageData();
        // create Body with more than 1kb . 502
        wr.write("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        wr.write("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        wr.write("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        wr.write("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        wr.write("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100\r\n");
        // second line
        wr.write("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        wr.write("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        wr.write("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        wr.write("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        wr.write("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        wr.write("123456781012345678201\r\n"); // 521 + CRLF = 523 + 502 => 1025
        wr.close();
        
        assertFalse(smtpProtocol.completePendingCommand());

        assertEquals("expected 552 error", 552, smtpProtocol.getReplyCode());

    }

    public void testHandleMessageSizeLimitRespected() throws Exception {
        m_testConfiguration.setMaxMessageSize(1); // set message limit to 1kb 
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol = new SMTPClient();
        smtpProtocol.connect("127.0.0.1", m_smtpListenerPort);

        smtpProtocol.sendCommand("ehlo "+InetAddress.getLocalHost());

        smtpProtocol.setSender("mail@localhost");
        smtpProtocol.addRecipient("mail@localhost");

        Writer wr = smtpProtocol.sendMessageData();
        // create Body with less than 1kb
        wr.write("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        wr.write("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        wr.write("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        wr.write("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        wr.write("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        wr.write("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        wr.write("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        wr.write("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        wr.write("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        wr.write("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        wr.write("1234567810123456782012\r\n"); // 1022 + CRLF = 1024
        wr.close();
        
        assertTrue(smtpProtocol.completePendingCommand());

        assertEquals("expected 250 ok", 250, smtpProtocol.getReplyCode());

    }

    public void testConnectionLimitExceeded() throws Exception {
        m_testConfiguration.setConnectionLimit(1); // allow no more than one connection at a time 
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol1 = new SMTPClient();
        SMTPClient smtpProtocol2 = new SMTPClient();
        smtpProtocol1.connect("127.0.0.1", m_smtpListenerPort);
        assertTrue("first connection taken", smtpProtocol1.isConnected());

        try {
            smtpProtocol2.connect("127.0.0.1", m_smtpListenerPort);
            fail("second connection not taken1");
        } catch (Exception e) {
        }
        
        // disconnect the first
        smtpProtocol1.quit();
        smtpProtocol1.disconnect();
        
        Thread.sleep(100);
        
        // now the second should be able to connect
        smtpProtocol2.connect("127.0.0.1", m_smtpListenerPort);
        assertTrue(smtpProtocol2.isConnected());
    }
    
    // RemoteDelivery tests.
    
    InMemorySpoolRepository outgoingSpool;
    private MockServiceManager m_serviceManager;
    
    private Properties getStandardParameters() {
        Properties parameters = new Properties();
        parameters.put("delayTime", "500 msec, 500 msec, 500 msec"); // msec, sec, minute, hour
        parameters.put("maxRetries", "3");
        parameters.put("deliveryThreads", "1");
        parameters.put("debug", "true");
        parameters.put("sendpartial", "false");
        parameters.put("bounceProcessor", "bounce");
        parameters.put("outgoing", "mocked://outgoing/");
        return parameters;
    }
    

    /**
     * This has been created to test javamail 1.4 introduced bug.
     * http://issues.apache.org/jira/browse/JAMES-490
     */
    public void testDeliveryToSelfWithGatewayAndBind() throws Exception {
        finishSetUp(m_testConfiguration);
        outgoingSpool = new InMemorySpoolRepository();
        ((MockStore) m_serviceManager.lookup(Store.ROLE)).add("outgoing", outgoingSpool);
        
        RemoteDelivery rd = new RemoteDelivery();
        
        MockMailContext mmc = new MockMailContext();
        mmc.setAttribute(Constants.AVALON_COMPONENT_MANAGER,m_serviceManager);
        mmc.setAttribute(Constants.HELLO_NAME,"localhost");
        MockMailetConfig mci = new MockMailetConfig("Test",mmc,getStandardParameters());
        mci.setProperty("bind", "127.0.0.1");
        mci.setProperty("gateway","127.0.0.1");
        mci.setProperty("gatewayPort",""+m_smtpListenerPort);
        rd.init(mci);
        String sources = "Content-Type: text/plain;\r\nSubject: test\r\n\r\nBody";
        String sender = "test@localhost";
        String recipient = "test@localhost";
        MimeMessage mm = new MimeMessage(Session.getDefaultInstance(new Properties()),new ByteArrayInputStream(sources.getBytes()));
        MailImpl mail = new MailImpl("name",new MailAddress(sender),Arrays.asList(new MailAddress[] {new MailAddress(recipient)}),mm);
        
        rd.service(mail);
        
        while (outgoingSpool.size() > 0) {
            Thread.sleep(1000);
        }

        verifyLastMail(sender, recipient, null);
        
        assertEquals(((String) mm.getContent()).trim(),((String) ((MimeMessage) m_mailServer.getLastMail()[2]).getContent()).trim());
    }

    
    /**
     * This is useful code to run tests on javamail bugs 
     * http://issues.apache.org/jira/browse/JAMES-52
     * 
     * This 
     * @throws Exception
     */
    public void test8bitmimeFromStream() throws Exception {
        finishSetUp(m_testConfiguration);
        outgoingSpool = new InMemorySpoolRepository();
        ((MockStore) m_serviceManager.lookup(Store.ROLE)).add("outgoing", outgoingSpool);
        
        RemoteDelivery rd = new RemoteDelivery();
        
        MockMailContext mmc = new MockMailContext();
        mmc.setAttribute(Constants.AVALON_COMPONENT_MANAGER,m_serviceManager);
        mmc.setAttribute(Constants.HELLO_NAME,"localhost");
        MockMailetConfig mci = new MockMailetConfig("Test",mmc,getStandardParameters());
        mci.setProperty("gateway","127.0.0.1");
        mci.setProperty("gatewayPort",""+m_smtpListenerPort);
        rd.init(mci);
        
        String sources = "Content-Type: text/plain;\r\nContent-Transfer-Encoding: quoted-printable\r\nSubject: test\r\n\r\nBody=80\r\n";
        //String sources = "Content-Type: text/plain; charset=iso-8859-15\r\nContent-Transfer-Encoding: quoted-printable\r\nSubject: test\r\n\r\nBody=80\r\n";
        //String sources = "Content-Type: text/plain; charset=iso-8859-15\r\nContent-Transfer-Encoding: 8bit\r\nSubject: test\r\n\r\nBody\u20AC\r\n";
        String sender = "test@localhost";
        String recipient = "test@localhost";
        MimeMessage mm = new MimeMessage(Session.getDefaultInstance(new Properties()),new ByteArrayInputStream(sources.getBytes()));
        MailImpl mail = new MailImpl("name",new MailAddress(sender),Arrays.asList(new MailAddress[] {new MailAddress(recipient)}),mm);
        
        rd.service(mail);
        
        while (outgoingSpool.size() > 0) {
            Thread.sleep(1000);
        }

        // verifyLastMail(sender, recipient, mm);
        verifyLastMail(sender, recipient, null);
        
        // THIS WOULD FAIL BECAUSE OF THE JAVAMAIL BUG
        // assertEquals(mm.getContent(),((MimeMessage) m_mailServer.getLastMail()[2]).getContent());

    }
    
    
}
