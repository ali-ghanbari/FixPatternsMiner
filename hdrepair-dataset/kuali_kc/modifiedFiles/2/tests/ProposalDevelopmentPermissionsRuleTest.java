/*
 * Copyright 2005-2014 The Kuali Foundation
 * 
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kuali.kra.proposaldevelopment.rules;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kuali.coeus.propdev.impl.core.ProposalDevelopmentDocument;
import org.kuali.coeus.propdev.impl.docperm.*;
import org.kuali.kra.infrastructure.Constants;
import org.kuali.kra.infrastructure.KeyConstants;
import org.kuali.kra.infrastructure.RoleConstants;
import org.kuali.rice.krad.util.ErrorMessage;
import org.kuali.rice.krad.util.GlobalVariables;
import org.kuali.rice.krad.util.MessageMap;

import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;
/**
 * Test the business rules for Proposal Permissions.
 * 
 * @author Kuali Research Administration Team (kualidev@oncourse.iu.edu)
 */
public class ProposalDevelopmentPermissionsRuleTest extends ProposalDevelopmentRuleTestBase {

    private ProposalDevelopmentPermissionsRule rule = null;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        rule = new ProposalDevelopmentPermissionsRule();
    }

    @After
    public void tearDown() throws Exception {
        rule = null;
        super.tearDown();
    }

    /**
     * Test a valid addition of a user.
     *  
     * @throws Exception
     */
    @Test
    public void testAddOK() throws Exception {
        ProposalDevelopmentDocument document = getNewProposalDevelopmentDocument();
        List<ProposalUserRoles> proposalUserRolesList = getProposalUserRoles();
        ProposalUserRoles proposalUser = createProposalUserRoles("majors");
        assertTrue(rule.processAddProposalUserBusinessRules(document, proposalUserRolesList, proposalUser));
    }
    
    /**
     * Test adding an unknown/invalid user.
     *  
     * @throws Exception
     */
    @Test
    public void testAddInvalidUser() throws Exception {
        ProposalDevelopmentDocument document = getNewProposalDevelopmentDocument();
        List<ProposalUserRoles> proposalUserRolesList = getProposalUserRoles();
        ProposalUserRoles proposalUser = createProposalUserRoles("xxx");
        assertFalse(rule.processAddProposalUserBusinessRules(document, proposalUserRolesList, proposalUser));
        assertError(Constants.PERMISSION_PROPOSAL_USERS_PROPERTY_KEY + ".username", KeyConstants.ERROR_UNKNOWN_USERNAME);
    }
    
    /**
     * Test adding a duplicate user.
     *  
     * @throws Exception
     */
    @Test
    public void testAddDuplicate() throws Exception {
        ProposalDevelopmentDocument document = getNewProposalDevelopmentDocument();
        List<ProposalUserRoles> proposalUserRolesList = getProposalUserRoles();
        ProposalUserRoles proposalUser = createProposalUserRoles("quickstart");
        assertFalse(rule.processAddProposalUserBusinessRules(document, proposalUserRolesList, proposalUser));
        assertError(Constants.PERMISSION_PROPOSAL_USERS_PROPERTY_KEY + ".username", KeyConstants.ERROR_DUPLICATE_PROPOSAL_USER);
    }
    
    /**
     * Test a valid deletion.
     *  
     * @throws Exception
     */
    @Test
    public void testDeleteOK() throws Exception {
        ProposalDevelopmentDocument document = getNewProposalDevelopmentDocument();
        List<ProposalUserRoles> proposalUserRolesList = getProposalUserRoles();
        assertTrue(rule.processDeleteProposalUserBusinessRules(document, proposalUserRolesList, 1));
    }
    
    /**
     * Test a deletion of a user who is the last aggregator.  
     *  
     * @throws Exception
     */
    @Test
    public void testDeleteLastAggregator() throws Exception {
        ProposalDevelopmentDocument document = getNewProposalDevelopmentDocument();
        List<ProposalUserRoles> proposalUserRolesList = getProposalUserRoles();
        assertFalse(rule.processDeleteProposalUserBusinessRules(document, proposalUserRolesList, 0));
        assertError(Constants.PERMISSION_PROPOSAL_USERS_COLLECTION_ID_KEY, KeyConstants.ERROR_LAST_AGGREGATOR);
    }
    
    /**
     * Test a valid edit.
     *  
     * @throws Exception
     */
    @Test
    public void testEditOK() throws Exception {
        ProposalDevelopmentDocument document = getNewProposalDevelopmentDocument();
        List<ProposalUserRoles> proposalUserRolesList = getProposalUserRoles();
        ProposalUserRoles editRoles = createProposalUserRoles("chew");
        editRoles.addRoleName(RoleConstants.AGGREGATOR);
        assertTrue(rule.processEditProposalUserRolesBusinessRules(document, proposalUserRolesList, editRoles));
    }

    /**
     * Test setting a user to have an Aggregator role with another role.
     *  
     * @throws Exception
     */
    @Test
    public void testEditAggregatorOnly() throws Exception {
        ProposalDevelopmentDocument document = getNewProposalDevelopmentDocument();
        List<ProposalUserRoles> proposalUserRolesList = getProposalUserRoles();
        ProposalUserRoles editRoles = createProposalUserRoles("chew");
        editRoles.addRoleName(RoleConstants.AGGREGATOR);
        editRoles.addRoleName(RoleConstants.NARRATIVE_WRITER);
        assertFalse(rule.processEditProposalUserRolesBusinessRules(document, proposalUserRolesList, editRoles));
        assertError(Constants.PERMISSION_PROPOSAL_USERS_COLLECTION_ID_KEY, KeyConstants.ERROR_AGGREGATOR_INCLUSIVE);
    }


    /**
     * Try removing the Aggregator role from the last user to have that role.
     *  
     * @throws Exception
     */
    @Test
    public void testEditLastAggregator() throws Exception {
        ProposalDevelopmentDocument document = getNewProposalDevelopmentDocument();
        List<ProposalUserRoles> proposalUserRolesList = getProposalUserRoles();
        ProposalUserRoles editRoles = createProposalUserRoles("quickstart");
        editRoles.addRoleName(RoleConstants.AGGREGATOR);
        assertFalse(rule.processEditProposalUserRolesBusinessRules(document, proposalUserRolesList, editRoles));
        assertError(Constants.PERMISSION_PROPOSAL_USERS_COLLECTION_ID_KEY, KeyConstants.ERROR_LAST_AGGREGATOR);
    }
    
    /**
     * Create a list of proposal users.
     * @return
     */
    private List<ProposalUserRoles> getProposalUserRoles() {
        List<ProposalUserRoles> proposalUserRolesList = new ArrayList<ProposalUserRoles>();
        
        ProposalUserRoles userRoles = new ProposalUserRoles();
        userRoles.setUsername("quickstart");
        userRoles.addRoleName("Aggregator");
        proposalUserRolesList.add(userRoles);
        
        userRoles = new ProposalUserRoles();
        userRoles.setUsername("chew");
        userRoles.addRoleName("Viewer");
        proposalUserRolesList.add(userRoles);
        
        return proposalUserRolesList;
    }
    
    /**
     * Create a proposal user to be added.
     * @param username
     * @return
     */
    private ProposalUserRoles createProposalUserRoles(String username) {
        ProposalUserRoles proposalUser = new ProposalUserRoles();
        proposalUser.setUsername(username);
        proposalUser.getRoleNames().add("Aggregator");
        return proposalUser;
    }
    
    /**
     * Assert an error.  The Error Map should have one error with the given
     * propery key and error key.
     * @param propertyKey
     * @param errorKey
     */
    private void assertError(String propertyKey, String errorKey) {
        MessageMap messages = GlobalVariables.getMessageMap();

        List errors = messages.getMessages(propertyKey);
        assertNotNull(errors);
        assertTrue(errors.size() == 1);
        
        ErrorMessage message = (ErrorMessage) errors.get(0);
        assertNotNull(message);
        assertEquals(message.getErrorKey(), errorKey);
    }
}
