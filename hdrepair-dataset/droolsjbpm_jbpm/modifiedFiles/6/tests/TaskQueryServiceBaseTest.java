/**
 * Copyright 2013 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.jbpm.task;



import static org.junit.Assert.assertEquals;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.jbpm.task.impl.factories.TaskFactory;
import org.jbpm.task.query.TaskSummary;
import org.junit.Ignore;
import org.junit.Test;

public abstract class TaskQueryServiceBaseTest extends BaseTest {
    
       
    // getTasksAssignedAsBusinessAdministrator(String userId, String language);
    
    @Test
    public void testGetTasksAssignedAsBusinessAdministratorWithUserLangNoTask() {
        List<TaskSummary> tasks = taskService.getTasksAssignedAsBusinessAdministrator("Bobba Fet", "en-UK");
        assertEquals(0, tasks.size());
    }

    @Test
    public void testGetTasksAssignedAsBusinessAdministratorWithUserLangOneTask() {
        // One potential owner, should go straight to state Reserved
        String str = "(with (new Task()) { priority = 55, taskData = (with( new TaskData()) { } ), ";
        str += "peopleAssignments = (with ( new PeopleAssignments() ) { businessAdministrators = [new User('Bobba Fet')], }),";
        str += "names = [ new I18NText( 'en-UK', 'This is my task name')] })";
        Task task = TaskFactory.evalTask(new StringReader(str));
        taskService.addTask(task, new HashMap<String, Object>());
        List<TaskSummary> tasks = taskService.getTasksAssignedAsBusinessAdministrator("Bobba Fet", "en-UK");
        assertEquals(1, tasks.size());
    }
    
    
    // getTasksAssignedAsExcludedOwner(String userId, String language);
    
    @Test
    public void testGetTasksAssignedAsExcludedOwnerWithUserLangNoTask() {
        List<TaskSummary> tasks = taskService.getTasksAssignedAsExcludedOwner("Bobba Fet", "en-UK");
        assertEquals(0, tasks.size());
    }
    
    @Test
    public void testGetTasksAssignedAsExcludedOwnerWithUserLangOneTask() {
        // One potential owner, should go straight to state Reserved
        String str = "(with (new Task()) { priority = 55, taskData = (with( new TaskData()) { } ), ";
        str += "peopleAssignments = (with ( new PeopleAssignments() ) { excludedOwners = [new User('Bobba Fet')], }),";
        str += "names = [ new I18NText( 'en-UK', 'This is my task name')] })";
        Task task = TaskFactory.evalTask(new StringReader(str));
        taskService.addTask(task, new HashMap<String, Object>());
        List<TaskSummary> tasks = taskService.getTasksAssignedAsExcludedOwner("Bobba Fet", "en-UK");
        assertEquals(1, tasks.size());
    }
    
    
    // getTasksAssignedAsPotentialOwner(String userId, String language)
    
    @Test
    public void testGetTasksAssignedAsPotentialOwnerWithUserLangNoTask() {
        List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwner("Bobba Fet", "en-UK");
        assertEquals(0, tasks.size());
    }
    
    @Test
    public void testGetTasksAssignedAsPotentialOwnerWithUserLangOneTask() {
        // One potential owner, should go straight to state Reserved
        String str = "(with (new Task()) { priority = 55, taskData = (with( new TaskData()) { } ), ";
        str += "peopleAssignments = (with ( new PeopleAssignments() ) { potentialOwners = [new User('Bobba Fet')  ], }),";
        str += "names = [ new I18NText( 'en-UK', 'This is my task name')] })";
        Task task = TaskFactory.evalTask(new StringReader(str));
        taskService.addTask(task, new HashMap<String, Object>());
        List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwner("Bobba Fet", "en-UK");
        assertEquals(1, tasks.size());
        assertEquals("Bobba Fet", tasks.get(0).getActualOwner().getId());
    }

    
    // getTasksAssignedAsPotentialOwner(String userId, List<String> groupIds, String language)
    
    @Test
    public void testGetTasksAssignedAsPotentialOwnerWithUserGroupsLangNoTaskNoGroupIds() {
        List<String> groupIds = new ArrayList<String>();
        groupIds.add("Crusaders");
        List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwner("Bobba Fet", groupIds, "en-UK");
        assertEquals(0, tasks.size());
    }
    
    @Test
    public void testGetTasksAssignedAsPotentialOwnerWithUserGroupsLangOneTaskOneUser() {
        // One potential owner, should go straight to state Reserved
        String str = "(with (new Task()) { priority = 55, taskData = (with( new TaskData()) { } ), ";
        str += "peopleAssignments = (with ( new PeopleAssignments() ) { potentialOwners = [new User('Bobba Fet')  ], }),";
        str += "names = [ new I18NText( 'en-UK', 'This is my task name')] })";
        Task task = TaskFactory.evalTask(new StringReader(str));
        taskService.addTask(task, new HashMap<String, Object>());
        List<String> groupIds = new ArrayList<String>();
        groupIds.add("Crusaders");
        List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwner("Bobba Fet", groupIds, "en-UK");
        assertEquals(1, tasks.size());
        assertEquals("Bobba Fet", tasks.get(0).getActualOwner().getId());
    }
    
    @Ignore("requires fix - returns two tasks, only one should be returned")
    @Test
    public void testGetTasksAssignedAsPotentialOwnerWithUserGroupsLangOneTaskOneGroup() {
        // One potential owner, should go straight to state Reserved
        String str = "(with (new Task()) { priority = 55, taskData = (with( new TaskData()) { } ), ";
        str += "peopleAssignments = (with ( new PeopleAssignments() ) { potentialOwners = [new User('Bobba Fet'), new Group('Crusaders'), ], }),";
        str += "names = [ new I18NText( 'en-UK', 'This is my task name')] })";
        Task task = TaskFactory.evalTask(new StringReader(str));
        taskService.addTask(task, new HashMap<String, Object>());
        List<String> groupIds = new ArrayList<String>();
        groupIds.add("Crusaders");
        List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwner("Bobba Fet", groupIds, "en-UK");
        assertEquals(1, tasks.size());
        assertEquals("Darth Vader", tasks.get(0).getActualOwner().getId());
    }
    
    
    // getTasksAssignedAsPotentialOwner(String userId, List<String> groupIds, String language, int firstResult, int maxResults);

    @Test
    public void testGetTasksAssignedAsPotentialOwnerWithUserGroupsLangOffsetCountNoTask() {
        List<String> groupIds = new ArrayList<String>();
        groupIds.add("Crusaders");
        List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwner("Bobba Fet", groupIds, "en-UK", 0, 1);
        assertEquals(0, tasks.size());
    }
        
    @Test
    public void testGetTasksAssignedAsPotentialOwnerWithUserGroupsLangOffsetCountTwoTasksOneMaxResult() {
        // One potential owner, should go straight to state Reserved
        String str1 = "(with (new Task()) { priority = 55, taskData = (with( new TaskData()) { } ), ";
        str1 += "peopleAssignments = (with ( new PeopleAssignments() ) { potentialOwners = [new User('Bobba Fet') ], }),";
        str1 += "names = [ new I18NText( 'en-UK', 'First task')] })";
        String str2 = "(with (new Task()) { priority = 55, taskData = (with( new TaskData()) { } ), ";
        str2 += "peopleAssignments = (with ( new PeopleAssignments() ) { potentialOwners = [new User('Bobba Fet') ], }),";
        str2 += "names = [ new I18NText( 'en-UK', 'Second task')] })";
        Task task1 = TaskFactory.evalTask(new StringReader(str1));
        taskService.addTask(task1, new HashMap<String, Object>());
        Task task2 = TaskFactory.evalTask(new StringReader(str2));
        taskService.addTask(task2, new HashMap<String, Object>());       
        List<String> groupIds = new ArrayList<String>();
        groupIds.add("Crusaders");
        List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwner("Bobba Fet", groupIds, "en-UK", 0, 1);
        assertEquals(1, tasks.size());
        // FIXME tasks are returned in random order
        // assertEquals("First task", tasks.get(0).getName());
    }
    
    @Test
    public void testGetTasksAssignedAsPotentialOwnerWithUserGroupsLangOffsetCountTwoTasksTwoMaxResults() {
        // One potential owner, should go straight to state Reserved
        String str1 = "(with (new Task()) { priority = 55, taskData = (with( new TaskData()) { } ), ";
        str1 += "peopleAssignments = (with ( new PeopleAssignments() ) { potentialOwners = [new User('Bobba Fet') ], }),";
        str1 += "names = [ new I18NText( 'en-UK', 'First task')] })";
        String str2 = "(with (new Task()) { priority = 55, taskData = (with( new TaskData()) { } ), ";
        str2 += "peopleAssignments = (with ( new PeopleAssignments() ) { potentialOwners = [new User('Bobba Fet') ], }),";
        str2 += "names = [ new I18NText( 'en-UK', 'Second task')] })";
        Task task1 = TaskFactory.evalTask(new StringReader(str1));
        taskService.addTask(task1, new HashMap<String, Object>());
        Task task2 = TaskFactory.evalTask(new StringReader(str2));
        taskService.addTask(task2, new HashMap<String, Object>());       
        List<String> groupIds = new ArrayList<String>();
        groupIds.add("Crusaders");
        List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwner("Bobba Fet", groupIds, "en-UK", 0, 2);
        assertEquals(2, tasks.size());
        // FIXME tasks are returned in random order
        // assertEquals("First task", tasks.get(0).getName());
        // assertEquals("Second task", tasks.get(1).getName());
    }
    
    @Test
    public void testGetTasksAssignedAsPotentialOwnerWithUserGroupsLangOffsetCountTwoTasksOneOffsetOneMaxResult() {
        // One potential owner, should go straight to state Reserved
        String str1 = "(with (new Task()) { priority = 55, taskData = (with( new TaskData()) { } ), ";
        str1 += "peopleAssignments = (with ( new PeopleAssignments() ) { potentialOwners = [new User('Bobba Fet') ], }),";
        str1 += "names = [ new I18NText( 'en-UK', 'First task')] })";
        String str2 = "(with (new Task()) { priority = 55, taskData = (with( new TaskData()) { } ), ";
        str2 += "peopleAssignments = (with ( new PeopleAssignments() ) { potentialOwners = [new User('Bobba Fet') ], }),";
        str2 += "names = [ new I18NText( 'en-UK', 'Second task')] })";
        Task task1 = TaskFactory.evalTask(new StringReader(str1));
        taskService.addTask(task1, new HashMap<String, Object>());
        Task task2 = TaskFactory.evalTask(new StringReader(str2));
        taskService.addTask(task2, new HashMap<String, Object>());       
        List<String> groupIds = new ArrayList<String>();
        groupIds.add("Crusaders");
        List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwner("Bobba Fet", groupIds, "en-UK", 1, 1);
        // FIXME tasks are returned in random order
        // assertEquals(1, tasks.size());
        // assertEquals("Second task", tasks.get(0).getName());
    }
    
    
    // getTasksAssignedAsPotentialOwnerByStatus(String userId, List<Status> status, String language);
    
    @Test
    public void testGetTasksAssignedAsPotentialOwnerByStatusWithUserStatusLangNoTask() {
        
        List<Status> statuses = new ArrayList<Status>();
        statuses.add(Status.Created);
        statuses.add(Status.Ready);
        List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwnerByStatus("Bobba Fet", statuses, "en-UK");
        assertEquals(0, tasks.size());
    }
    
    @Test
    public void testGetTasksAssignedAsPotentialOwnerByStatusWithUserStatusLangOneTask() {
        // One potential owner, should go straight to state Reserved
        String str = "(with (new Task()) { priority = 55, taskData = (with( new TaskData()) { } ), ";
        str += "peopleAssignments = (with ( new PeopleAssignments() ) { potentialOwners = [new User('Bobba Fet') ], }),";
        str += "names = [ new I18NText( 'en-UK', 'This is my task name')] })";
        Task task = TaskFactory.evalTask(new StringReader(str));
        taskService.addTask(task, new HashMap<String, Object>());
        List<Status> statuses = new ArrayList<Status>();
        statuses.add(Status.Created);
        statuses.add(Status.Ready);
        List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwnerByStatus("Bobba Fet", statuses, "en-UK");
        assertEquals(0, tasks.size());
    }
    
    @Test
    public void testGetTasksAssignedAsPotentialOwnerByStatusWithUserStatusLangOneTaskReserved() {
        // One potential owner, should go straight to state Reserved
        String str = "(with (new Task()) { priority = 55, taskData = (with( new TaskData()) { } ), ";
        str += "peopleAssignments = (with ( new PeopleAssignments() ) { potentialOwners = [new User('Bobba Fet') ], }),";
        str += "names = [ new I18NText( 'en-UK', 'This is my task name')] })";
        Task task = TaskFactory.evalTask(new StringReader(str));
        taskService.addTask(task, new HashMap<String, Object>());
        List<Status> statuses = new ArrayList<Status>();
        statuses.add(Status.Reserved);
        List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwnerByStatus("Bobba Fet", statuses, "en-UK");
        assertEquals(1, tasks.size());
        assertEquals("Bobba Fet", tasks.get(0).getActualOwner().getId());
    }
    
    
    // getTasksAssignedAsPotentialOwnerByStatusByGroup(String userId, List<String> groupIds, List<Status> status, String language);
    
    @Test
    public void testGetTasksAssignedAsPotentialOwnerByStatusByGroupWithUserStatusLangNoTask() {
        List<String> groupIds = new ArrayList<String>();
        groupIds.add("Crusaders");
        List<Status> statuses = new ArrayList<Status>();
        statuses.add(Status.Created);
        statuses.add(Status.Ready);
        List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwnerByStatusByGroup("Bobba Fet", groupIds, statuses, "en-UK");
        assertEquals(0, tasks.size());
    }
    
    @Test
    public void testGetTasksAssignedAsPotentialOwnerByStatusByGroupWithUserStatusLangOneTask() {
        // One potential owner, should go straight to state Reserved
        String str = "(with (new Task()) { priority = 55, taskData = (with( new TaskData()) { } ), ";
        str += "peopleAssignments = (with ( new PeopleAssignments() ) { potentialOwners = [new User('Bobba Fet') ], }),";
        str += "names = [ new I18NText( 'en-UK', 'This is my task name')] })";
        Task task = TaskFactory.evalTask(new StringReader(str));
        taskService.addTask(task, new HashMap<String, Object>());
        List<String> groupIds = new ArrayList<String>();
        groupIds.add("Crusaders");
        List<Status> statuses = new ArrayList<Status>();
        statuses.add(Status.Created);
        statuses.add(Status.Ready);
        List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwnerByStatusByGroup("Bobba Fet", groupIds, statuses, "en-UK");
        assertEquals(0, tasks.size());
    }
    
    @Test
    public void testGetTasksAssignedAsPotentialOwnerByStatusByGroupWithUserStatusLangOneTaskReserved() {
        // One potential owner, should go straight to state Reserved
        String str = "(with (new Task()) { priority = 55, taskData = (with( new TaskData()) { } ), ";
        str += "peopleAssignments = (with ( new PeopleAssignments() ) { potentialOwners = [new User('Bobba Fet') ], }),";
        str += "names = [ new I18NText( 'en-UK', 'This is my task name')] })";
        Task task = TaskFactory.evalTask(new StringReader(str));
        taskService.addTask(task, new HashMap<String, Object>());
        List<String> groupIds = new ArrayList<String>();
        groupIds.add("Crusaders");
        List<Status> statuses = new ArrayList<Status>();
        statuses.add(Status.Reserved);
        List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwnerByStatusByGroup("Bobba Fet", groupIds, statuses, "en-UK");
        assertEquals(1, tasks.size());
        assertEquals("Bobba Fet", tasks.get(0).getActualOwner().getId());
    }
    
    
    // getTasksAssignedAsRecipient(String userId, String language);
    
    @Test
    public void testGetTasksAssignedAsRecipientWithUserLangNoTask() {
        List<TaskSummary> tasks = taskService.getTasksAssignedAsRecipient("Bobba Fet", "en-UK");
        assertEquals(0, tasks.size());
    }
    
    @Test
    public void testGetTasksAssignedAsRecipientWithUserLangOneTask() {
        // One potential owner, should go straight to state Reserved
        String str = "(with (new Task()) { priority = 55, taskData = (with( new TaskData()) { } ), ";
        str += "peopleAssignments = (with ( new PeopleAssignments() ) { recipients = [new User('Bobba Fet')], }),";
        str += "names = [ new I18NText( 'en-UK', 'This is my task name')] })";
        Task task = TaskFactory.evalTask(new StringReader(str));
        taskService.addTask(task, new HashMap<String, Object>());
        List<TaskSummary> tasks = taskService.getTasksAssignedAsRecipient("Bobba Fet", "en-UK");
        assertEquals(1, tasks.size());
    }
    
    
    // getTasksAssignedAsTaskInitiator(String userId, String language);
    
    @Test
    public void testGetTasksAssignedAsTaskInitiatorWithUserLangNoTask() {
        List<TaskSummary> tasks = taskService.getTasksAssignedAsTaskInitiator("Bobba Fet", "en-UK");
        assertEquals(0, tasks.size());
    }
    
    @Test
    public void testGetTasksAssignedAsTaskInitiatorWithUserLangOneTask() {
        // One potential owner, should go straight to state Reserved
        String str = "(with (new Task()) { priority = 55, taskData = (with( new TaskData()) { } ), ";
        str += "peopleAssignments = (with ( new PeopleAssignments() ) { taskInitiator = new User('Bobba Fet'), }),";
        str += "names = [ new I18NText( 'en-UK', 'This is my task name')] })";
        Task task = TaskFactory.evalTask(new StringReader(str));
        taskService.addTask(task, new HashMap<String, Object>());
        List<TaskSummary> tasks = taskService.getTasksAssignedAsTaskInitiator("Bobba Fet", "en-UK");
        assertEquals(1, tasks.size());
    }
    
    
    // getTasksAssignedAsTaskStakeholder(String userId, String language);
    
    @Test
    public void testGetTasksAssignedAsTaskStakeholderWithUserLangNoTask() {
        List<TaskSummary> tasks = taskService.getTasksAssignedAsTaskStakeholder("Bobba Fet", "en-UK");
        assertEquals(0, tasks.size());
    }
    
    @Test
    public void testGetTasksAssignedAsTaskStakeholderWithUserLangOneTask() {
        // One potential owner, should go straight to state Reserved
        String str = "(with (new Task()) { priority = 55, taskData = (with( new TaskData()) { } ), ";
        str += "peopleAssignments = (with ( new PeopleAssignments() ) { taskStakeholders = [new User('Bobba Fet')], }),";
        str += "names = [ new I18NText( 'en-UK', 'This is my task name')] })";
        Task task = TaskFactory.evalTask(new StringReader(str));
        taskService.addTask(task, new HashMap<String, Object>());
        List<TaskSummary> tasks = taskService.getTasksAssignedAsTaskStakeholder("Bobba Fet", "en-UK");
        assertEquals(1, tasks.size());
    }
    
    
    // getTasksAssignedByGroup(String groupId, String language)
    
    @Test
    public void testGetTasksAssignedByGroupWithGroupLangNoTask() {
        List<TaskSummary> tasks = taskService.getTasksAssignedByGroup("Crusaders", "en-UK");
        assertEquals(0, tasks.size());
    }
    
    @Test
    public void testGetTasksAssignedByGroupWithGroupLangOneTask() {
        // One potential owner, should go straight to state Reserved
        String str = "(with (new Task()) { priority = 55, taskData = (with( new TaskData()) { } ), ";
        str += "peopleAssignments = (with ( new PeopleAssignments() ) { potentialOwners = [new Group('Crusaders')  ], }),";
        str += "names = [ new I18NText( 'en-UK', 'This is my task name')] })";
        Task task = TaskFactory.evalTask(new StringReader(str));
        taskService.addTask(task, new HashMap<String, Object>());
        List<TaskSummary> tasks = taskService.getTasksAssignedByGroup("Crusaders", "en-UK");
        assertEquals(1, tasks.size());
    }
    
    
    // getTasksAssignedByGroups(List<String> groupsId, String language);
    
    @Test
    public void testGetTasksAssignedByGroupsWithGroupsLangNoTask() {
        List<String> groupIds = new ArrayList<String>();
        groupIds.add("Crusaders");
        List<TaskSummary> tasks = taskService.getTasksAssignedByGroups(groupIds, "en-UK");
        assertEquals(0, tasks.size());
    }
    
    @Test
    public void testGetTasksAssignedByGroupsWithGroupsLangOneTask() {
        // One potential owner, should go straight to state Reserved
        String str = "(with (new Task()) { priority = 55, taskData = (with( new TaskData()) { } ), ";
        str += "peopleAssignments = (with ( new PeopleAssignments() ) { potentialOwners = [new Group('Crusaders')  ], }),";
        str += "names = [ new I18NText( 'en-UK', 'This is my task name')] })";
        Task task = TaskFactory.evalTask(new StringReader(str));
        taskService.addTask(task, new HashMap<String, Object>());
        List<String> groupIds = new ArrayList<String>();
        groupIds.add("Crusaders");
        List<TaskSummary> tasks = taskService.getTasksAssignedByGroups(groupIds, "en-UK");
        assertEquals(1, tasks.size());
    }
    
    
    // getTasksAssignedByGroupsByExpirationDate(List<String> groupIds, String language, Date expirationDate);
    
    @Test
    public void testGetTasksAssignedByGroupsByExpirationDateWithGroupsLangDateNoTask() {
        List<String> groupIds = new ArrayList<String>();
        groupIds.add("Crusaders");
        Date date = new Date();
        List<TaskSummary> tasks = taskService.getTasksAssignedByGroupsByExpirationDate(groupIds, "en-UK", date);
        assertEquals(0, tasks.size());
    }
    
    @Test
    public void testGetTasksAssignedByGroupsByExpirationDateWithUserStatusDateOneTaskReserved() {
        // One potential owner, should go straight to state Reserved
        String str = "(with (new Task()) { priority = 55, taskData = (with( new TaskData()) { expirationTime = new Date( 10000000 ), } ), ";
        str += "peopleAssignments = (with ( new PeopleAssignments() ) { potentialOwners = [new Group('Crusaders')  ], }),";
        str += "names = [ new I18NText( 'en-UK', 'This is my task name')] })";
        Task task = TaskFactory.evalTask(new StringReader(str));
        taskService.addTask(task, new HashMap<String, Object>());
        List<String> groupIds = new ArrayList<String>();
        groupIds.add("Crusaders");
        Date date = new Date(10000000);
        List<TaskSummary> tasks = taskService.getTasksAssignedByGroupsByExpirationDate(groupIds, "en-UK", date);
        assertEquals(1, tasks.size());
        //assertEquals("Bobba Fet", tasks.get(0).getActualOwner().getId());
    }
    
    
    // getTasksOwned(String userId);
    
    @Test
    public void testGetTasksOwnedWithUserNoTask() {
        List<TaskSummary> tasks = taskService.getTasksOwned("Bobba Fet");
        assertEquals(0, tasks.size());
    }
    
    @Test
    public void testGetTasksOwnedWithUserOneTask() {
        // One potential owner, should go straight to state Reserved
        String str = "(with (new Task()) { priority = 55, taskData = (with( new TaskData()) { } ), ";
        str += "peopleAssignments = (with ( new PeopleAssignments() ) { potentialOwners = [new User('Bobba Fet')  ], }),";
        str += "names = [ new I18NText( 'en-UK', 'This is my task name')] })";
        Task task = TaskFactory.evalTask(new StringReader(str));
        taskService.addTask(task, new HashMap<String, Object>());
        List<TaskSummary> tasks = taskService.getTasksOwned("Bobba Fet");
        assertEquals(1, tasks.size());
        assertEquals("Bobba Fet", tasks.get(0).getActualOwner().getId());
    }
    
    
    // getTasksOwned(String userId, List<Status> status, String language);
    
    @Test
    public void testGetTasksOwnedWithUserStatusLangNoTask() {
        List<Status> statuses = new ArrayList<Status>();
        statuses.add(Status.Created);
        statuses.add(Status.Ready);
        List<TaskSummary> tasks = taskService.getTasksOwned("Darth Vader", statuses, "en-UK");
        assertEquals(0, tasks.size());
    }
    
    @Test
    public void testGetTasksOwnedWithUserStatusLangOneTask() {
        // One potential owner, should go straight to state Reserved
        String str = "(with (new Task()) { priority = 55, taskData = (with( new TaskData()) { } ), ";
        str += "peopleAssignments = (with ( new PeopleAssignments() ) { potentialOwners = [new User('Bobba Fet')  ], }),";
        str += "names = [ new I18NText( 'en-UK', 'This is my task name')] })";
        Task task = TaskFactory.evalTask(new StringReader(str));
        taskService.addTask(task, new HashMap<String, Object>());
        List<Status> statuses = new ArrayList<Status>();
        statuses.add(Status.Reserved);
        List<TaskSummary> tasks = taskService.getTasksOwned("Bobba Fet", statuses, "en-UK");
        assertEquals(1, tasks.size());
        assertEquals("Bobba Fet", tasks.get(0).getActualOwner().getId());
    }
    
    @Test
    public void testGetTasksOwnedWithUserStatusLangOneTaskCompleted() {
        // One potential owner, should go straight to state Reserved
        String str = "(with (new Task()) { priority = 55, taskData = (with( new TaskData()) { } ), ";
        str += "peopleAssignments = (with ( new PeopleAssignments() ) { potentialOwners = [new User('Bobba Fet')  ], }),";
        str += "names = [ new I18NText( 'en-UK', 'This is my task name')] })";
        Task task = TaskFactory.evalTask(new StringReader(str));
        taskService.addTask(task, new HashMap<String, Object>());
        List<Status> statuses = new ArrayList<Status>();
        statuses.add(Status.Completed);
        List<TaskSummary> tasks = taskService.getTasksOwned("Bobba Fet", statuses, "en-UK");
        assertEquals(0, tasks.size());
    }
    
    
    // getTasksOwnedByExpirationDate(String userId, List<Status> status, Date expirationDate);
    
    @Test
    public void testGetTasksOwnedByExpirationDateWithUserStatusDateNoTask() {
        List<Status> statuses = new ArrayList<Status>();
        statuses.add(Status.Created);
        statuses.add(Status.Ready);
        Date date = new Date();
        List<TaskSummary> tasks = taskService.getTasksOwnedByExpirationDate("Darth Vader", statuses, date);
        assertEquals(0, tasks.size());
    }
    
    @Test
    public void testGetTasksOwnedByExpirationDateWithUserStatusDateOneTaskReserved() {
        // One potential owner, should go straight to state Reserved
        String str = "(with (new Task()) { priority = 55, taskData = (with( new TaskData()) { expirationTime = new Date( 10000000 ), } ), ";
        str += "peopleAssignments = (with ( new PeopleAssignments() ) { potentialOwners = [new User('Bobba Fet')  ], }),";
        str += "names = [ new I18NText( 'en-UK', 'This is my task name')] })";
        Task task = TaskFactory.evalTask(new StringReader(str));
        taskService.addTask(task, new HashMap<String, Object>());
        List<Status> statuses = new ArrayList<Status>();
        statuses.add(Status.Reserved);
        Date date = new Date(10000000);
        List<TaskSummary> tasks = taskService.getTasksOwnedByExpirationDate("Bobba Fet", statuses, date);
        assertEquals(1, tasks.size());
        assertEquals("Bobba Fet", tasks.get(0).getActualOwner().getId());
    }
    
    @Test
    public void testGetTasksOwnedByExpirationDateWithUserStatusDateOneTaskCompleted() {
        // One potential owner, should go straight to state Reserved
        String str = "(with (new Task()) { priority = 55, taskData = (with( new TaskData()) { expirationTime = new Date( 10000000 ), } ), ";
        str += "peopleAssignments = (with ( new PeopleAssignments() ) { potentialOwners = [new User('Bobba Fet')  ], }),";
        str += "names = [ new I18NText( 'en-UK', 'This is my task name')] })";
        Task task = TaskFactory.evalTask(new StringReader(str));
        taskService.addTask(task, new HashMap<String, Object>());
        List<Status> statuses = new ArrayList<Status>();
        statuses.add(Status.Completed);
        Date date = new Date(10000000);
        List<TaskSummary> tasks = taskService.getTasksOwnedByExpirationDate("Bobba Fet", statuses, date);
        assertEquals(0, tasks.size());
    }
    
    
    // getSubTasksAssignedAsPotentialOwner(long parentId, String userId, String language);

    @Ignore("not familiar with sub task concept; groupIds is not supplied to corresponding query")
    @Test
    public void testGetSubTasksAssignedAsPotentialOwnerWithParentUserLangNoTask() {
        List<TaskSummary> tasks = taskService.getSubTasksAssignedAsPotentialOwner(0, "Bobba Fet", "en-UK");
        assertEquals(0, tasks.size());
    }
    
    @Ignore("not familiar with sub task concept")
    @Test
    public void testGetSubTasksAssignedAsPotentialOwnerWithParentUserLangOneTask() {
        // One potential owner, should go straight to state Reserved
        String str = "(with (new Task()) { priority = 55, taskData = (with( new TaskData()) { } ), ";
        str += "peopleAssignments = (with ( new PeopleAssignments() ) { taskStakeholders = [new User('Bobba Fet')], }),";
        str += "names = [ new I18NText( 'en-UK', 'This is my task name')] })";
        Task task = TaskFactory.evalTask(new StringReader(str));
        taskService.addTask(task, new HashMap<String, Object>());
        List<TaskSummary> tasks = taskService.getSubTasksAssignedAsPotentialOwner(0, "Bobba Fet", "en-UK");
        assertEquals(1, tasks.size());
    }
    
    
    // getSubTasksByParent(long parentId);
    
    @Test
    public void testGetSubTasksByParentWithParentNoTask() {
        List<TaskSummary> tasks = taskService.getSubTasksByParent(0);
        assertEquals(0, tasks.size());
    }
    
    
    // getPendingSubTasksByParent(long parentId);
    
    @Test
    public void testGetPendingSubTasksByParentWithParentNoTask() {
        int count = taskService.getPendingSubTasksByParent(0);
        assertEquals(0, count);
    }
    
    
    // Task getTaskByWorkItemId(long workItemId);
    
    @Test
    public void testGetTaskByWorkItemIdWithWorkItemNoTask() {
        Task task = taskService.getTaskByWorkItemId(0);
        assertEquals(null, task);
    }
    
    
    // Task getTaskInstanceById(long taskId);
    
    @Test
    public void testGetTaskInstanceByIdWithWorkItemNoTask() {
        Task task = taskService.getTaskByWorkItemId(0);
        assertEquals(null, task);
    }
    
    @Test
    public void testGetTasksAssignedByExpirationDateOptional() {
        // One potential owner, should go straight to state Reserved
        String str = "(with (new Task()) { priority = 55, taskData = (with( new TaskData()) { } ), ";
        str += "peopleAssignments = (with ( new PeopleAssignments() ) { potentialOwners = [new User('Bobba Fet')], }),";
        str += "names = [ new I18NText( 'en-UK', 'This is my task name')] })";
        Task task = TaskFactory.evalTask(new StringReader(str));
        taskService.addTask(task, new HashMap<String, Object>());
        
        List<Status> statuses = new ArrayList<Status>();      
        statuses.add(Status.InProgress);
        statuses.add(Status.Reserved);
        statuses.add(Status.Created);
        List<TaskSummary> tasks = taskService.getTasksOwnedByExpirationDateOptional("Bobba Fet", statuses, new Date());
        assertEquals(1, tasks.size());
    }
    
    @Test
    public void testGetTasksAssignedByGroupsByExpirationDateOptional() {
        // One potential owner, should go straight to state Reserved
        String str = "(with (new Task()) { priority = 55, taskData = (with( new TaskData()) { } ), ";
        str += "peopleAssignments = (with ( new PeopleAssignments() ) { potentialOwners = [new Group('Crusaders')  ], }),";
        str += "names = [ new I18NText( 'en-UK', 'This is my task name')] })";
        Task task = TaskFactory.evalTask(new StringReader(str));
        taskService.addTask(task, new HashMap<String, Object>());
        List<String> groupIds = new ArrayList<String>();
        groupIds.add("Crusaders");
        
        List<TaskSummary> tasks = taskService.getTasksAssignedByGroupsByExpirationDateOptional(groupIds, "en-UK", new Date());
        assertEquals(1, tasks.size());
    }
}
