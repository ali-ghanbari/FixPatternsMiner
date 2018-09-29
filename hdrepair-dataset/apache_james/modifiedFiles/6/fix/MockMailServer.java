/***********************************************************************
 * Copyright (c) 2000-2006 The Apache Software Foundation.             *
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
package org.apache.james.test.mock.james;

import org.apache.james.core.MimeMessageCopyOnWriteProxy;
import org.apache.james.services.MailRepository;
import org.apache.james.services.MailServer;
import org.apache.james.smtpserver.MessageSizeException;
import org.apache.james.userrepository.MockUsersRepository;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.InputStream;
import java.util.*;

public class MockMailServer implements MailServer {

    private final MockUsersRepository m_users = new MockUsersRepository();

    private int m_counter = 0;
    private int m_maxMessageSizeBytes = 0;

    private final ArrayList mails = new ArrayList();

    private HashMap inboxes;
    
    public MockUsersRepository getUsersRepository() {
        return m_users;
    }

    public void sendMail(MailAddress sender, Collection recipients, MimeMessage msg) throws MessagingException {
        Object[] mailObjects = new Object[]{sender, recipients, new MimeMessageCopyOnWriteProxy(msg)};
        mails.add(mailObjects);
    }

    public void sendMail(MailAddress sender, Collection recipients, InputStream msg) throws MessagingException {
        Object[] mailObjects = new Object[]{sender, recipients, msg};
        mails.add(mailObjects);
    }

    public void sendMail(Mail mail) throws MessagingException {
        int bodySize = mail.getMessage().getSize();
        try {
            if (m_maxMessageSizeBytes != 0 && m_maxMessageSizeBytes*1024 < bodySize) throw new MessageSizeException();
        } catch (MessageSizeException e) {
            throw new MessagingException("message size exception is nested", e);
        }
        sendMail(mail.getSender(), mail.getRecipients(), mail.getMessage());
    }

    public void sendMail(MimeMessage message) throws MessagingException {
        // taken from class org.apache.james.James 
        MailAddress sender = new MailAddress((InternetAddress)message.getFrom()[0]);
        Collection recipients = new HashSet();
        Address addresses[] = message.getAllRecipients();
        if (addresses != null) {
            for (int i = 0; i < addresses.length; i++) {
                // Javamail treats the "newsgroups:" header field as a
                // recipient, so we want to filter those out.
                if ( addresses[i] instanceof InternetAddress ) {
                    recipients.add(new MailAddress((InternetAddress)addresses[i]));
                }
            }
        }
        sendMail(sender, recipients, message);
    }

    public MailRepository getUserInbox(String userName) {
        if (inboxes==null) {
            return null;
        } else {
            return (MailRepository) inboxes.get(userName);
        }
        
    }
    
    public void setUserInbox(String userName, MailRepository inbox) {
        if (inboxes == null) {
            inboxes = new HashMap();
        }
        inboxes.put(userName,inbox);
    }

    public Map getRepositoryCounters() {
        return null; // trivial implementation 
    }

    public synchronized String getId() {
        m_counter++;
        return "MockMailServer-ID-" + m_counter;
    }

    public boolean addUser(String userName, String password) {
        m_users.addUser(userName, password);
        return true;
    }

    public boolean isLocalServer(String serverName) {
        return "localhost".equals(serverName);
    }

    public Object[] getLastMail()
    {
        if (mails.size() == 0) return null;
        return (Object[])mails.get(mails.size()-1);
    }

    public void setMaxMessageSizeBytes(int maxMessageSizeBytes) {
        m_maxMessageSizeBytes = maxMessageSizeBytes;
    }
}


