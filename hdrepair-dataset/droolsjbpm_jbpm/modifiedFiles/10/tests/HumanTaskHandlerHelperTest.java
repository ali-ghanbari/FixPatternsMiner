package org.jbpm.process.workitem.wsht;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Collections;

import org.drools.process.instance.WorkItem;
import org.drools.process.instance.impl.WorkItemImpl;
import org.jbpm.task.Deadlines;
import org.jbpm.task.EmailNotification;
import org.jbpm.task.EmailNotificationHeader;
import org.jbpm.task.Language;
import org.jbpm.task.Notification;
import org.jbpm.task.Reassignment;
import org.junit.Test;

public class HumanTaskHandlerHelperTest {

	@Test
	public void testSetDeadlinesNotStartedReassign() {
		
		WorkItem workItem = new WorkItemImpl();
		workItem.setParameter("NotStartedReassign", "[users:john]@[4h]");
		
		@SuppressWarnings("unchecked")
		Deadlines deadlines = HumanTaskHandlerHelper.setDeadlines(workItem, Collections.EMPTY_LIST);
		assertNotNull(deadlines);
		assertEquals(1, deadlines.getStartDeadlines().size());
		assertEquals(0, deadlines.getEndDeadlines().size());
		assertEquals(1, deadlines.getStartDeadlines().get(0).getEscalations().size());
		assertEquals(1, deadlines.getStartDeadlines().get(0).getEscalations().get(0).getReassignments().size());
		assertEquals(0, deadlines.getStartDeadlines().get(0).getEscalations().get(0).getNotifications().size());
		
		// verify reassignment
		Reassignment reassignment = deadlines.getStartDeadlines().get(0).getEscalations().get(0).getReassignments().get(0);
		assertEquals(1, reassignment.getPotentialOwners().size());
		assertEquals("john", reassignment.getPotentialOwners().get(0).getId());
		
		// check deadline expiration time
		assertNotNull(deadlines.getStartDeadlines().get(0).getDate());
		long expirationTime = deadlines.getStartDeadlines().get(0).getDate().getTime() - System.currentTimeMillis();
		
		assertEquals(4, roundExpirationTime(expirationTime));
		
	}
	
	@Test
	public void testSetDeadlinesNotStartedReassignWithGroups() {
		
		WorkItem workItem = new WorkItemImpl();
		workItem.setParameter("NotStartedReassign", "[users:john|groups:sales]@[4h]");
		
		@SuppressWarnings("unchecked")
		Deadlines deadlines = HumanTaskHandlerHelper.setDeadlines(workItem, Collections.EMPTY_LIST);
		assertNotNull(deadlines);
		assertEquals(1, deadlines.getStartDeadlines().size());
		assertEquals(0, deadlines.getEndDeadlines().size());
		assertEquals(1, deadlines.getStartDeadlines().get(0).getEscalations().size());
		assertEquals(1, deadlines.getStartDeadlines().get(0).getEscalations().get(0).getReassignments().size());
		assertEquals(0, deadlines.getStartDeadlines().get(0).getEscalations().get(0).getNotifications().size());
		
		// verify reassignment
		Reassignment reassignment = deadlines.getStartDeadlines().get(0).getEscalations().get(0).getReassignments().get(0);
		assertEquals(2, reassignment.getPotentialOwners().size());
		assertEquals("john", reassignment.getPotentialOwners().get(0).getId());
		assertEquals("sales", reassignment.getPotentialOwners().get(1).getId());
		
		// check deadline expiration time
		assertNotNull(deadlines.getStartDeadlines().get(0).getDate());
		long expirationTime = deadlines.getStartDeadlines().get(0).getDate().getTime() - System.currentTimeMillis();
		
		assertEquals(4, roundExpirationTime(expirationTime));
		
	}
	
	@Test
	public void testSetDeadlinesNotStartedReassignTwoTimes() {
		
		WorkItem workItem = new WorkItemImpl();
		workItem.setParameter("NotStartedReassign", "[users:john]@[4h,6h]");
		
		@SuppressWarnings("unchecked")
		Deadlines deadlines = HumanTaskHandlerHelper.setDeadlines(workItem, Collections.EMPTY_LIST);
		assertNotNull(deadlines);
		assertEquals(2, deadlines.getStartDeadlines().size());
		assertEquals(0, deadlines.getEndDeadlines().size());
		assertEquals(1, deadlines.getStartDeadlines().get(0).getEscalations().size());
		assertEquals(1, deadlines.getStartDeadlines().get(0).getEscalations().get(0).getReassignments().size());
		assertEquals(0, deadlines.getStartDeadlines().get(0).getEscalations().get(0).getNotifications().size());
		
		assertEquals(1, deadlines.getStartDeadlines().get(1).getEscalations().size());
		assertEquals(1, deadlines.getStartDeadlines().get(1).getEscalations().get(0).getReassignments().size());
		assertEquals(0, deadlines.getStartDeadlines().get(1).getEscalations().get(0).getNotifications().size());
		
		// verify reassignment
		Reassignment reassignment = deadlines.getStartDeadlines().get(0).getEscalations().get(0).getReassignments().get(0);
		assertEquals(1, reassignment.getPotentialOwners().size());
		assertEquals("john", reassignment.getPotentialOwners().get(0).getId());
		
		// check deadline expiration time
		assertNotNull(deadlines.getStartDeadlines().get(0).getDate());
		long expirationTime = deadlines.getStartDeadlines().get(0).getDate().getTime() - System.currentTimeMillis();
		
		assertEquals(4, roundExpirationTime(expirationTime));
		
		// verify reassignment
		reassignment = deadlines.getStartDeadlines().get(1).getEscalations().get(0).getReassignments().get(0);
		assertEquals(1, reassignment.getPotentialOwners().size());
		assertEquals("john", reassignment.getPotentialOwners().get(0).getId());
		
		// check deadline expiration time
		assertNotNull(deadlines.getStartDeadlines().get(1).getDate());
		expirationTime = deadlines.getStartDeadlines().get(1).getDate().getTime() - System.currentTimeMillis();
		
		assertEquals(6, roundExpirationTime(expirationTime));
		
	}
	
	@Test
	public void testSetDeadlinesNotCompletedReassign() {
		
		WorkItem workItem = new WorkItemImpl();
		workItem.setParameter("NotCompletedReassign", "[users:john]@[4h]");
		
		@SuppressWarnings("unchecked")
		Deadlines deadlines = HumanTaskHandlerHelper.setDeadlines(workItem, Collections.EMPTY_LIST);
		assertNotNull(deadlines);
		assertEquals(1, deadlines.getEndDeadlines().size());
		assertEquals(0, deadlines.getStartDeadlines().size());
		assertEquals(1, deadlines.getEndDeadlines().get(0).getEscalations().size());
		assertEquals(1, deadlines.getEndDeadlines().get(0).getEscalations().get(0).getReassignments().size());
		assertEquals(0, deadlines.getEndDeadlines().get(0).getEscalations().get(0).getNotifications().size());
		
		// verify reassignment
		Reassignment reassignment = deadlines.getEndDeadlines().get(0).getEscalations().get(0).getReassignments().get(0);
		assertEquals(1, reassignment.getPotentialOwners().size());
		assertEquals("john", reassignment.getPotentialOwners().get(0).getId());
		
		// check deadline expiration time
		assertNotNull(deadlines.getEndDeadlines().get(0).getDate());
		long expirationTime = deadlines.getEndDeadlines().get(0).getDate().getTime() - System.currentTimeMillis();
		
		assertEquals(4, roundExpirationTime(expirationTime));
		
	}
	
	@Test
	public void testSetDeadlinesNotCompletedReassignWithGroups() {
		
		WorkItem workItem = new WorkItemImpl();
		workItem.setParameter("NotCompletedReassign", "[users:john|groups:sales]@[4h]");
		
		@SuppressWarnings("unchecked")
		Deadlines deadlines = HumanTaskHandlerHelper.setDeadlines(workItem, Collections.EMPTY_LIST);
		assertNotNull(deadlines);
		assertEquals(1, deadlines.getEndDeadlines().size());
		assertEquals(0, deadlines.getStartDeadlines().size());
		assertEquals(1, deadlines.getEndDeadlines().get(0).getEscalations().size());
		assertEquals(1, deadlines.getEndDeadlines().get(0).getEscalations().get(0).getReassignments().size());
		assertEquals(0, deadlines.getEndDeadlines().get(0).getEscalations().get(0).getNotifications().size());
		
		// verify reassignment
		Reassignment reassignment = deadlines.getEndDeadlines().get(0).getEscalations().get(0).getReassignments().get(0);
		assertEquals(2, reassignment.getPotentialOwners().size());
		assertEquals("john", reassignment.getPotentialOwners().get(0).getId());
		assertEquals("sales", reassignment.getPotentialOwners().get(1).getId());
		
		// check deadline expiration time
		assertNotNull(deadlines.getEndDeadlines().get(0).getDate());
		long expirationTime = deadlines.getEndDeadlines().get(0).getDate().getTime() - System.currentTimeMillis();
		
		assertEquals(4, roundExpirationTime(expirationTime));
		
	}
	
	@Test
	public void testSetDeadlinesNotCompletedReassignTwoTimes() {
		
		WorkItem workItem = new WorkItemImpl();
		workItem.setParameter("NotCompletedReassign", "[users:john]@[4h,6h]");
		
		@SuppressWarnings("unchecked")
		Deadlines deadlines = HumanTaskHandlerHelper.setDeadlines(workItem, Collections.EMPTY_LIST);
		assertNotNull(deadlines);
		assertEquals(2, deadlines.getEndDeadlines().size());
		assertEquals(0, deadlines.getStartDeadlines().size());
		assertEquals(1, deadlines.getEndDeadlines().get(0).getEscalations().size());
		assertEquals(1, deadlines.getEndDeadlines().get(0).getEscalations().get(0).getReassignments().size());
		assertEquals(0, deadlines.getEndDeadlines().get(0).getEscalations().get(0).getNotifications().size());
		
		assertEquals(1, deadlines.getEndDeadlines().get(1).getEscalations().size());
		assertEquals(1, deadlines.getEndDeadlines().get(1).getEscalations().get(0).getReassignments().size());
		assertEquals(0, deadlines.getEndDeadlines().get(1).getEscalations().get(0).getNotifications().size());
		
		// verify reassignment
		Reassignment reassignment = deadlines.getEndDeadlines().get(0).getEscalations().get(0).getReassignments().get(0);
		assertEquals(1, reassignment.getPotentialOwners().size());
		assertEquals("john", reassignment.getPotentialOwners().get(0).getId());
		
		// check deadline expiration time
		assertNotNull(deadlines.getEndDeadlines().get(0).getDate());
		long expirationTime = deadlines.getEndDeadlines().get(0).getDate().getTime() - System.currentTimeMillis();
		
		assertEquals(4, roundExpirationTime(expirationTime));
		
		// verify reassignment
		reassignment = deadlines.getEndDeadlines().get(1).getEscalations().get(0).getReassignments().get(0);
		assertEquals(1, reassignment.getPotentialOwners().size());
		assertEquals("john", reassignment.getPotentialOwners().get(0).getId());
		
		// check deadline expiration time
		assertNotNull(deadlines.getEndDeadlines().get(1).getDate());
		expirationTime = deadlines.getEndDeadlines().get(1).getDate().getTime() - System.currentTimeMillis();
		
		assertEquals(6, roundExpirationTime(expirationTime));
		
	}
	
	@Test
	public void testNotStartedNotifyMinimal() {
		WorkItem workItem = new WorkItemImpl();
		workItem.setParameter("NotStartedNotify", "[tousers:john|subject:Test of notification|body:And here is the body]@[4h]");
		
		@SuppressWarnings("unchecked")
		Deadlines deadlines = HumanTaskHandlerHelper.setDeadlines(workItem, Collections.EMPTY_LIST);
		assertNotNull(deadlines);
		assertEquals(1, deadlines.getStartDeadlines().size());
		assertEquals(0, deadlines.getEndDeadlines().size());
		assertEquals(1, deadlines.getStartDeadlines().get(0).getEscalations().size());
		assertEquals(1, deadlines.getStartDeadlines().get(0).getEscalations().get(0).getNotifications().size());
		assertEquals(0, deadlines.getStartDeadlines().get(0).getEscalations().get(0).getReassignments().size());
		
		// verify notification
		Notification notification = deadlines.getStartDeadlines().get(0).getEscalations().get(0).getNotifications().get(0);
		assertNotNull(notification);
		assertEquals(1, notification.getRecipients().size());
		assertEquals("john", notification.getRecipients().get(0).getId());
		
		assertEquals(1, notification.getSubjects().size());
		assertEquals("Test of notification", notification.getSubjects().get(0).getText());
		
		EmailNotification emailNotification = (EmailNotification) notification;
		assertEquals(1, emailNotification.getEmailHeaders().size());
		EmailNotificationHeader header = emailNotification.getEmailHeaders().get(new Language("en-UK"));
		assertNotNull(header);
		assertEquals("Test of notification", header.getSubject());
		assertEquals("And here is the body", header.getBody());
		
		// check deadline expiration time
		assertNotNull(deadlines.getStartDeadlines().get(0).getDate());
		long expirationTime = deadlines.getStartDeadlines().get(0).getDate().getTime() - System.currentTimeMillis();
		
		assertEquals(4, roundExpirationTime(expirationTime));
	}
	
	@Test
	public void testNotStartedNotifyAllElements() {
		WorkItem workItem = new WorkItemImpl();
		workItem.setParameter("NotStartedNotify", "[from:mike|tousers:john,mary|togroups:sales,hr|replyto:mike|subject:Test of notification|body:And here is the body]@[4h]");
		
		@SuppressWarnings("unchecked")
		Deadlines deadlines = HumanTaskHandlerHelper.setDeadlines(workItem, Collections.EMPTY_LIST);
		assertNotNull(deadlines);
		assertEquals(1, deadlines.getStartDeadlines().size());
		assertEquals(0, deadlines.getEndDeadlines().size());
		assertEquals(1, deadlines.getStartDeadlines().get(0).getEscalations().size());
		assertEquals(1, deadlines.getStartDeadlines().get(0).getEscalations().get(0).getNotifications().size());
		assertEquals(0, deadlines.getStartDeadlines().get(0).getEscalations().get(0).getReassignments().size());
		
		// verify notification
		Notification notification = deadlines.getStartDeadlines().get(0).getEscalations().get(0).getNotifications().get(0);
		assertNotNull(notification);
		assertEquals(4, notification.getRecipients().size());
		assertEquals("john", notification.getRecipients().get(0).getId());
		assertEquals("mary", notification.getRecipients().get(1).getId());
		assertEquals("sales", notification.getRecipients().get(2).getId());
		assertEquals("hr", notification.getRecipients().get(3).getId());
		
		assertEquals(1, notification.getSubjects().size());
		assertEquals("Test of notification", notification.getSubjects().get(0).getText());
		
		EmailNotification emailNotification = (EmailNotification) notification;
		assertEquals(1, emailNotification.getEmailHeaders().size());
		EmailNotificationHeader header = emailNotification.getEmailHeaders().get(new Language("en-UK"));
		assertNotNull(header);
		assertEquals("Test of notification", header.getSubject());
		assertEquals("And here is the body", header.getBody());
		assertEquals("mike", header.getFrom());
		assertEquals("mike", header.getReplyTo());
		
		// check deadline expiration time
		assertNotNull(deadlines.getStartDeadlines().get(0).getDate());
		long expirationTime = deadlines.getStartDeadlines().get(0).getDate().getTime() - System.currentTimeMillis();
		
		assertEquals(4, roundExpirationTime(expirationTime));
	}
	
	@Test
	public void testNotStartedNotifyMinimalMultipleExpirations() {
		WorkItem workItem = new WorkItemImpl();
		workItem.setParameter("NotStartedNotify", "[tousers:john|subject:Test of notification|body:And here is the body]@[4h,6h]");
		
		@SuppressWarnings("unchecked")
		Deadlines deadlines = HumanTaskHandlerHelper.setDeadlines(workItem, Collections.EMPTY_LIST);
		assertNotNull(deadlines);
		assertEquals(2, deadlines.getStartDeadlines().size());
		assertEquals(0, deadlines.getEndDeadlines().size());
		assertEquals(1, deadlines.getStartDeadlines().get(0).getEscalations().size());
		assertEquals(1, deadlines.getStartDeadlines().get(0).getEscalations().get(0).getNotifications().size());
		assertEquals(0, deadlines.getStartDeadlines().get(0).getEscalations().get(0).getReassignments().size());
		
		// verify notification
		Notification notification = deadlines.getStartDeadlines().get(0).getEscalations().get(0).getNotifications().get(0);
		assertNotNull(notification);
		assertEquals(1, notification.getRecipients().size());
		assertEquals("john", notification.getRecipients().get(0).getId());
		
		assertEquals(1, notification.getSubjects().size());
		assertEquals("Test of notification", notification.getSubjects().get(0).getText());
		
		EmailNotification emailNotification = (EmailNotification) notification;
		assertEquals(1, emailNotification.getEmailHeaders().size());
		EmailNotificationHeader header = emailNotification.getEmailHeaders().get(new Language("en-UK"));
		assertNotNull(header);
		assertEquals("Test of notification", header.getSubject());
		assertEquals("And here is the body", header.getBody());
		
		// check deadline expiration time
		assertNotNull(deadlines.getStartDeadlines().get(0).getDate());
		long expirationTime = deadlines.getStartDeadlines().get(0).getDate().getTime() - System.currentTimeMillis();
		
		assertEquals(4, roundExpirationTime(expirationTime));
		
		// verify notification
		notification = deadlines.getStartDeadlines().get(1).getEscalations().get(0).getNotifications().get(0);
		assertNotNull(notification);
		assertEquals(1, notification.getRecipients().size());
		assertEquals("john", notification.getRecipients().get(0).getId());
		
		assertEquals(1, notification.getSubjects().size());
		assertEquals("Test of notification", notification.getSubjects().get(0).getText());
		
		emailNotification = (EmailNotification) notification;
		assertEquals(1, emailNotification.getEmailHeaders().size());
		header = emailNotification.getEmailHeaders().get(new Language("en-UK"));
		assertNotNull(header);
		assertEquals("Test of notification", header.getSubject());
		assertEquals("And here is the body", header.getBody());
		
		// check deadline expiration time
		assertNotNull(deadlines.getStartDeadlines().get(1).getDate());
		expirationTime = deadlines.getStartDeadlines().get(1).getDate().getTime() - System.currentTimeMillis();
		
		assertEquals(6, roundExpirationTime(expirationTime));
	}
	
	
	@Test
	public void testNotStartedNotifyMinimalWithHtml() {
		WorkItem workItem = new WorkItemImpl();
		workItem.setParameter("NotStartedNotify", "[tousers:john|subject:Test of notification|body:&lt;html&gt;"+
			    "&lt;body&gt;"+
			    "Reason {s}&lt;br/&gt;"+
			    "body of notification:&lt;br/&gt;"+
			    "work item id - ${workItemId}&lt;br/&gt;"+
			    "process instance id - ${processInstanceId}&lt;br/&gt;"+
			    "task id - ${taskId}&lt;br/&gt;" +
			    "http://localhost:8080/taskserver-url"+
			    "expiration time - ${doc['Deadlines'][0].expires}&lt;br/&gt;"+
			    "&lt;/body&gt;"+
			  "&lt;/html&gt;]@[4h]");
		
		@SuppressWarnings("unchecked")
		Deadlines deadlines = HumanTaskHandlerHelper.setDeadlines(workItem, Collections.EMPTY_LIST);
		assertNotNull(deadlines);
		assertEquals(1, deadlines.getStartDeadlines().size());
		assertEquals(0, deadlines.getEndDeadlines().size());
		assertEquals(1, deadlines.getStartDeadlines().get(0).getEscalations().size());
		assertEquals(1, deadlines.getStartDeadlines().get(0).getEscalations().get(0).getNotifications().size());
		assertEquals(0, deadlines.getStartDeadlines().get(0).getEscalations().get(0).getReassignments().size());
		
		// verify notification
		Notification notification = deadlines.getStartDeadlines().get(0).getEscalations().get(0).getNotifications().get(0);
		assertNotNull(notification);
		assertEquals(1, notification.getRecipients().size());
		assertEquals("john", notification.getRecipients().get(0).getId());
		
		assertEquals(1, notification.getSubjects().size());
		assertEquals("Test of notification", notification.getSubjects().get(0).getText());
		
		EmailNotification emailNotification = (EmailNotification) notification;
		assertEquals(1, emailNotification.getEmailHeaders().size());
		EmailNotificationHeader header = emailNotification.getEmailHeaders().get(new Language("en-UK"));
		assertNotNull(header);
		assertEquals("Test of notification", header.getSubject());
		assertTrue((header.getBody().indexOf("http://localhost:8080/taskserver-url") != -1));
		
		// check deadline expiration time
		assertNotNull(deadlines.getStartDeadlines().get(0).getDate());
		long expirationTime = deadlines.getStartDeadlines().get(0).getDate().getTime() - System.currentTimeMillis();
		
		assertEquals(4, roundExpirationTime(expirationTime));
	}
	
	
	private long roundExpirationTime(long expirationTime) {
		BigDecimal a = new BigDecimal(expirationTime);
		a.setScale(1, 1);
		BigDecimal b = new BigDecimal(60*60*1000);
		b.setScale(1, 1);
		double devided = a.doubleValue()/b.doubleValue();

		long roundedValue = Math.round(devided);
		
		return roundedValue;
	}
}
