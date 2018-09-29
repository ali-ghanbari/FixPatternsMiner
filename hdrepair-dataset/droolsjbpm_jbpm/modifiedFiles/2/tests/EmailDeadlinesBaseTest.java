/**
 * Copyright 2010 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jbpm.services.task;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;

import org.jbpm.services.task.impl.factories.TaskFactory;
import org.jbpm.services.task.impl.model.ContentDataImpl;
import org.jbpm.services.task.impl.model.ContentImpl;
import org.jbpm.services.task.impl.model.PeopleAssignmentsImpl;
import org.jbpm.services.task.impl.model.TaskImpl;
import org.jbpm.services.task.impl.model.UserImpl;
import org.jbpm.services.task.utils.ContentMarshallerHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kie.internal.task.api.model.Content;
import org.kie.internal.task.api.model.ContentData;
import org.kie.internal.task.api.model.OrganizationalEntity;
import org.kie.internal.task.api.model.PeopleAssignments;
import org.kie.internal.task.api.model.Status;
import org.kie.internal.task.api.model.Task;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;


/**
 * All users are defined as part of userinfo.propeties file that is utilized by PropertyUserInfoImpl
 *
 */
public abstract class EmailDeadlinesBaseTest extends HumanTaskServicesBaseTest {

    private Wiser wiser;
    
    @Before
    public void setup() {
        super.setUp();
        Properties conf = new Properties();
        conf.setProperty("mail.smtp.host", "localhost");
        conf.setProperty("mail.smtp.port", "2345");
        conf.setProperty("mail.from", "from@domain.com");
        conf.setProperty("mail.replyto", "replyTo@domain.com");
        
        wiser = new Wiser();
        wiser.setHostname(conf.getProperty("mail.smtp.host"));
        wiser.setPort(Integer.parseInt(conf.getProperty("mail.smtp.port")));        
        wiser.start();
        
    }
    
    @After
    public void tearDown(){
        if (wiser != null) {
            wiser.stop();
        }
        super.tearDown();
    }

    protected Wiser getWiser() {
        return this.wiser;
    }
    
    @Test
    public void testDelayedEmailNotificationOnDeadline() throws Exception {
        
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("now", new Date());

        Reader reader = new InputStreamReader(getClass().getResourceAsStream(MvelFilePath.DeadlineWithNotification));
        Task task = (TaskImpl) TaskFactory.evalTask(reader, vars);
        
        taskService.addTask(task, new HashMap<String, Object>());
        long taskId = task.getId();

        Content content = new ContentImpl();
        
        Map<String, String> params = fillMarshalSubjectAndBodyParams();
        ContentData marshalledObject = ContentMarshallerHelper.marshal(params, null);
        content.setContent(marshalledObject.getContent());
        taskService.addContent(taskId, content);
        long contentId = content.getId();
        
        content = taskService.getContentById(contentId);
        Object unmarshallObject = ContentMarshallerHelper.unmarshall(content.getContent(), null);
        checkContentSubjectAndBody(unmarshallObject);

        // emails should not be set yet
        assertEquals(0, getWiser().getMessages().size());
        Thread.sleep(100);

        // nor yet
        assertEquals(0, getWiser().getMessages().size());

        long time = 0;
        while (getWiser().getMessages().size() != 2 && time < 5000) {
            Thread.sleep(50);
            time += 50;
        }
        for (WiserMessage msg : getWiser().getMessages()) {
            System.out.println(msg.getEnvelopeReceiver());
        }
        // 1 email with two recipients should now exist
        assertEquals(2, getWiser().getMessages().size());

        List<String> list = new ArrayList<String>(2);
        list.add(getWiser().getMessages().get(0).getEnvelopeReceiver());
        list.add(getWiser().getMessages().get(1).getEnvelopeReceiver());

        assertTrue(list.contains("tony@domain.com"));
        assertTrue(list.contains("darth@domain.com"));


        MimeMessage msg = ((WiserMessage) getWiser().getMessages().get(0)).getMimeMessage();
        assertEquals(myBody, msg.getContent());
        assertEquals(mySubject, msg.getSubject());
        assertEquals("from@domain.com", ((InternetAddress) msg.getFrom()[0]).getAddress());
        assertEquals("replyTo@domain.com", ((InternetAddress) msg.getReplyTo()[0]).getAddress());
        assertEquals("tony@domain.com", ((InternetAddress) msg.getRecipients(RecipientType.TO)[0]).getAddress());
        assertEquals("darth@domain.com", ((InternetAddress) msg.getRecipients(RecipientType.TO)[1]).getAddress());
    }
    @Test
    public void testDelayedEmailNotificationOnDeadlineContentSingleObject() throws Exception {
        

        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("now", new Date());

        Reader reader = new InputStreamReader(getClass().getResourceAsStream(MvelFilePath.DeadlineWithNotificationContentSingleObject));
        Task task = (TaskImpl) TaskFactory.evalTask(reader, vars);
     
        taskService.addTask(task, new HashMap<String, Object>());
        long taskId = task.getId();

        Content content = new ContentImpl();
        ContentDataImpl marshalledObject = ContentMarshallerHelper.marshal("'singleobject'", null);
        content.setContent(marshalledObject.getContent());
       
        taskService.addContent(taskId, content);
        long contentId = content.getId();
      
        content = taskService.getContentById(contentId);
        Object unmarshallObject = ContentMarshallerHelper.unmarshall(content.getContent(), null);
        assertEquals("'singleobject'", unmarshallObject.toString());

        // emails should not be set yet
        assertEquals(0, getWiser().getMessages().size());
        Thread.sleep(100);
        // nor yet
        assertEquals(0, getWiser().getMessages().size());
        long time = 0;
        while (getWiser().getMessages().size() != 2 && time < 5000) {
            Thread.sleep(500);
            time = 500;
        }

        // 1 email with two recipients should now exist
        assertEquals(2, getWiser().getMessages().size());
        List<String> list = new ArrayList<String>(2);
        list.add(getWiser().getMessages().get(0).getEnvelopeReceiver());
        list.add(getWiser().getMessages().get(1).getEnvelopeReceiver());

        assertTrue(list.contains("tony@domain.com"));
        assertTrue(list.contains("darth@domain.com"));

        MimeMessage msg = ((WiserMessage) getWiser().getMessages().get(0)).getMimeMessage();
        assertEquals("'singleobject'", msg.getContent());
        assertEquals("'singleobject'", msg.getSubject());
        assertEquals("from@domain.com", ((InternetAddress) msg.getFrom()[0]).getAddress());
        assertEquals("replyTo@domain.com", ((InternetAddress) msg.getReplyTo()[0]).getAddress());
        assertEquals("tony@domain.com", ((InternetAddress) msg.getRecipients(RecipientType.TO)[0]).getAddress());
        assertEquals("darth@domain.com", ((InternetAddress) msg.getRecipients(RecipientType.TO)[1]).getAddress());

    }
    @Test
    public void testDelayedEmailNotificationOnDeadlineTaskCompleted() throws Exception {


         Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("now", new Date());

        Reader reader = new InputStreamReader(getClass().getResourceAsStream(MvelFilePath.DeadlineWithNotification));
        Task task = (TaskImpl) TaskFactory.evalTask(reader, vars);
        
        task.getTaskData().setSkipable(true);
        PeopleAssignments assignments = new PeopleAssignmentsImpl();
        List<OrganizationalEntity> ba = new ArrayList<OrganizationalEntity>();
        ba.add(new UserImpl("Administrator"));
        assignments.setBusinessAdministrators(ba);
        
        List<OrganizationalEntity> po = new ArrayList<OrganizationalEntity>();
        po.add(new UserImpl("Administrator"));
        assignments.setPotentialOwners(po);
        
        task.setPeopleAssignments(assignments);
        
        taskService.addTask(task, new HashMap<String, Object>());
        long taskId = task.getId();

        Content content = new ContentImpl();
        
        Map<String, String> params = fillMarshalSubjectAndBodyParams();
        ContentDataImpl marshalledObject = ContentMarshallerHelper.marshal(params, null);
        content.setContent(marshalledObject.getContent());
        taskService.addContent(taskId, content);
        long contentId = content.getId();
        
        content = taskService.getContentById(contentId);
        Object unmarshallObject = ContentMarshallerHelper.unmarshall(content.getContent(), null);
        checkContentSubjectAndBody(unmarshallObject);
        
        taskService.start(taskId, "Administrator");
        taskService.complete(taskId, "Administrator", null);
        // emails should not be set yet
        assertEquals(0, getWiser().getMessages().size());
        Thread.sleep(100);

        // nor yet
        assertEquals(0, getWiser().getMessages().size());

        long time = 0;
        while (getWiser().getMessages().size() != 2 && time < 5000) {
            Thread.sleep(500);
            time += 500;
        }

        // no email should ne sent as task was completed before deadline was triggered
        assertEquals(0, getWiser().getMessages().size());
        task = taskService.getTaskById(taskId);
        assertEquals(Status.Completed, task.getTaskData().getStatus());
        assertEquals(0, task.getDeadlines().getStartDeadlines().size());
        assertEquals(0, task.getDeadlines().getEndDeadlines().size());
        
        
    }
    @Test
    public void testDelayedEmailNotificationOnDeadlineTaskFailed() throws Exception {
        

        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("now", new Date());

        Reader reader = new InputStreamReader(getClass().getResourceAsStream(MvelFilePath.DeadlineWithNotification));
        Task task = (TaskImpl) TaskFactory.evalTask(reader, vars);
        
        task.getTaskData().setSkipable(true);
        PeopleAssignments assignments = new PeopleAssignmentsImpl();
        List<OrganizationalEntity> ba = new ArrayList<OrganizationalEntity>();
        ba.add(new UserImpl("Administrator"));
        assignments.setBusinessAdministrators(ba);
        
        List<OrganizationalEntity> po = new ArrayList<OrganizationalEntity>();
        po.add(new UserImpl("Administrator"));
        assignments.setPotentialOwners(po);
        
        task.setPeopleAssignments(assignments);
        
        
        taskService.addTask(task, new HashMap<String, Object>());
        long taskId = task.getId();

        Content content = new ContentImpl();
        
        Map<String, String> params = fillMarshalSubjectAndBodyParams();
        ContentDataImpl marshalledObject = ContentMarshallerHelper.marshal(params, null);
        content.setContent(marshalledObject.getContent());
        taskService.addContent(taskId, content);
        long contentId = content.getId();
        
        content = taskService.getContentById(contentId);
        Object unmarshallObject = ContentMarshallerHelper.unmarshall(content.getContent(), null);
        checkContentSubjectAndBody(unmarshallObject);
        
        taskService.start(taskId, "Administrator");
        taskService.fail(taskId, "Administrator", null);
     // emails should not be set yet
        assertEquals(0, getWiser().getMessages().size());
        Thread.sleep(100);

        // nor yet
        assertEquals(0, getWiser().getMessages().size());

        long time = 0;
        while (getWiser().getMessages().size() != 2 && time < 5000) {
            Thread.sleep(500);
            time += 500;
        }

        // no email should ne sent as task was completed before deadline was triggered
        assertEquals(0, getWiser().getMessages().size());
        task = taskService.getTaskById(taskId);
        assertEquals(Status.Failed, task.getTaskData().getStatus());
        assertEquals(0, task.getDeadlines().getStartDeadlines().size());
        assertEquals(0, task.getDeadlines().getEndDeadlines().size());
    }
    @Test
    public void testDelayedEmailNotificationOnDeadlineTaskSkipped() throws Exception {

        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("now", new Date());

        Reader reader = new InputStreamReader(getClass().getResourceAsStream(MvelFilePath.DeadlineWithNotification));
        Task task = (TaskImpl) TaskFactory.evalTask(reader, vars);
        
        task.getTaskData().setSkipable(true);
        PeopleAssignments assignments = new PeopleAssignmentsImpl();
        List<OrganizationalEntity> ba = new ArrayList<OrganizationalEntity>();
        ba.add(new UserImpl("Administrator"));
        assignments.setBusinessAdministrators(ba);
        
        List<OrganizationalEntity> po = new ArrayList<OrganizationalEntity>();
        po.add(new UserImpl("Administrator"));
        assignments.setPotentialOwners(po);
        
        task.setPeopleAssignments(assignments);
        taskService.addTask(task, new HashMap<String, Object>());
        long taskId = task.getId();

        Content content = new ContentImpl();
        
        Map<String, String> params = fillMarshalSubjectAndBodyParams();
        ContentDataImpl marshalledObject = ContentMarshallerHelper.marshal(params, null);
        content.setContent(marshalledObject.getContent());
        taskService.addContent(taskId, content);
        long contentId = content.getId();
        
        content = taskService.getContentById(contentId);
        Object unmarshallObject = ContentMarshallerHelper.unmarshall(content.getContent(), null);
        checkContentSubjectAndBody(unmarshallObject);
        
        taskService.skip(taskId, "Administrator");
     // emails should not be set yet
        assertEquals(0, getWiser().getMessages().size());
        Thread.sleep(100);

        // nor yet
        assertEquals(0, getWiser().getMessages().size());

        long time = 0;
        while (getWiser().getMessages().size() != 2 && time < 5000) {
            Thread.sleep(500);
            time += 500;
        }

        // no email should ne sent as task was completed before deadline was triggered
        assertEquals(0, getWiser().getMessages().size());
        task = taskService.getTaskById(taskId);
        assertEquals(Status.Obsolete, task.getTaskData().getStatus());
        assertEquals(0, task.getDeadlines().getStartDeadlines().size());
        assertEquals(0, task.getDeadlines().getEndDeadlines().size());
    }
    @Test    
    public void testDelayedEmailNotificationOnDeadlineTaskExited() throws Exception {

        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("now", new Date());

        Reader reader = new InputStreamReader(getClass().getResourceAsStream(MvelFilePath.DeadlineWithNotification));
        Task task = (TaskImpl) TaskFactory.evalTask(reader, vars);
        
        task.getTaskData().setSkipable(true);
        PeopleAssignments assignments = new PeopleAssignmentsImpl();
        List<OrganizationalEntity> ba = new ArrayList<OrganizationalEntity>();
        ba.add(new UserImpl("Administrator"));
        assignments.setBusinessAdministrators(ba);
        
        List<OrganizationalEntity> po = new ArrayList<OrganizationalEntity>();
        po.add(new UserImpl("Administrator"));
        assignments.setPotentialOwners(po);
        
        task.setPeopleAssignments(assignments);
        taskService.addTask(task, new HashMap<String, Object>());
        long taskId = task.getId();

        Content content = new ContentImpl();
        
        Map<String, String> params = fillMarshalSubjectAndBodyParams();
        ContentDataImpl marshalledObject = ContentMarshallerHelper.marshal(params, null);
        content.setContent(marshalledObject.getContent());
        taskService.addContent(taskId, content);
        long contentId = content.getId();
        
        content = taskService.getContentById(contentId);
        Object unmarshallObject = ContentMarshallerHelper.unmarshall(content.getContent(), null);
        checkContentSubjectAndBody(unmarshallObject);
        
        taskService.exit(taskId, "Administrator");
        // emails should not be set yet
        assertEquals(0, getWiser().getMessages().size());
        Thread.sleep(100);

        // nor yet
        assertEquals(0, getWiser().getMessages().size());

        long time = 0;
        while (getWiser().getMessages().size() != 2 && time < 5000) {
            Thread.sleep(500);
            time += 500;
        }

        // no email should ne sent as task was completed before deadline was triggered
        assertEquals(0, getWiser().getMessages().size());
        task = taskService.getTaskById(taskId);
        assertEquals(Status.Exited, task.getTaskData().getStatus());
        assertEquals(0, task.getDeadlines().getStartDeadlines().size());
        assertEquals(0, task.getDeadlines().getEndDeadlines().size());
    }


      @Test
    public void testDelayedReassignmentOnDeadline() throws Exception {


        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("now", new Date());

        Reader reader = new InputStreamReader(getClass().getResourceAsStream(MvelFilePath.DeadlineWithReassignment));
        Task task = (TaskImpl) TaskFactory.evalTask(reader, vars);
        taskService.addTask(task, new HashMap<String, Object>());
        long taskId = task.getId();

        // Shouldn't have re-assigned yet
        Thread.sleep(1000);
        
        
        task = taskService.getTaskById(taskId);
        List<OrganizationalEntity> potentialOwners = (List<OrganizationalEntity>) task.getPeopleAssignments().getPotentialOwners();
        List<String> ids = new ArrayList<String>(potentialOwners.size());
        for (OrganizationalEntity entity : potentialOwners) {
            ids.add(entity.getId());
        }
        assertTrue(ids.contains("Tony Stark"));
        assertTrue(ids.contains("Luke Cage"));

        // should have re-assigned by now
        long time = 0;
        while (getWiser().getMessages().size() != 2 && time < 5000) {
            Thread.sleep(500);
            time += 500;
        }
        
        task = taskService.getTaskById(taskId);
        assertEquals(Status.Ready, task.getTaskData().getStatus());
        potentialOwners = (List<OrganizationalEntity>) task.getPeopleAssignments().getPotentialOwners();

        ids = new ArrayList<String>(potentialOwners.size());
        for (OrganizationalEntity entity : potentialOwners) {
            ids.add(entity.getId());
        }
        assertTrue(ids.contains("Bobba Fet"));
        assertTrue(ids.contains("Jabba Hutt"));
    }

      @Test
      public void testDelayedEmailNotificationStartDeadlineStatusDoesNotMatch() throws Exception {


           Map<String, Object> vars = new HashMap<String, Object>();
          vars.put("now", new Date());

          Reader reader = new InputStreamReader(getClass().getResourceAsStream(MvelFilePath.DeadlineWithNotification));
          Task task = (TaskImpl) TaskFactory.evalTask(reader, vars);
          
          task.getTaskData().setSkipable(true);
          PeopleAssignments assignments = new PeopleAssignmentsImpl();
          List<OrganizationalEntity> ba = new ArrayList<OrganizationalEntity>();
          ba.add(new UserImpl("Administrator"));
          assignments.setBusinessAdministrators(ba);
          
          List<OrganizationalEntity> po = new ArrayList<OrganizationalEntity>();
          po.add(new UserImpl("Administrator"));
          assignments.setPotentialOwners(po);
          
          task.setPeopleAssignments(assignments);
          
          taskService.addTask(task, new HashMap<String, Object>());
          long taskId = task.getId();

          Content content = new ContentImpl();
          
          Map<String, String> params = fillMarshalSubjectAndBodyParams();
          ContentDataImpl marshalledObject = ContentMarshallerHelper.marshal(params, null);
          content.setContent(marshalledObject.getContent());
          taskService.addContent(taskId, content);
          long contentId = content.getId();
          
          content = taskService.getContentById(contentId);
          Object unmarshallObject = ContentMarshallerHelper.unmarshall(content.getContent(), null);
          checkContentSubjectAndBody(unmarshallObject);
          
          taskService.start(taskId, "Administrator");
          
          // emails should not be set yet
          assertEquals(0, getWiser().getMessages().size());
          Thread.sleep(100);

          // nor yet
          assertEquals(0, getWiser().getMessages().size());

          long time = 0;
          while (getWiser().getMessages().size() != 2 && time < 5000) {
              Thread.sleep(500);
              time += 500;
          }

          // no email should ne sent as task was completed before deadline was triggered
          assertEquals(0, getWiser().getMessages().size());
          task = taskService.getTaskById(taskId);
          assertEquals(Status.InProgress, task.getTaskData().getStatus());
          assertEquals(0, task.getDeadlines().getStartDeadlines().size());
          assertEquals(0, task.getDeadlines().getEndDeadlines().size());
          
          
      }
}
