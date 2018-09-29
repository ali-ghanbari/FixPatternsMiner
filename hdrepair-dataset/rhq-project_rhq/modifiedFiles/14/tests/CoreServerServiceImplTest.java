/*
 * RHQ Management Platform
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.rhq.enterprise.server.core;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.management.MBeanServer;
import javax.persistence.Query;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.server.core.AgentRegistrationException;
import org.rhq.core.clientapi.server.core.AgentRegistrationRequest;
import org.rhq.core.clientapi.server.core.AgentRegistrationResults;
import org.rhq.core.clientapi.server.core.AgentVersion;
import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.cloud.Server.OperationMode;
import org.rhq.core.domain.common.ProductInfo;
import org.rhq.core.domain.resource.Agent;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author John Mazzitelli
 */
@Test
public class CoreServerServiceImplTest extends AbstractEJB3Test {
    private static final String TEST_AGENT_NAME_PREFIX = "CoreServerServiceImplTest.Agent";
    private static final String RHQ_SERVER_NAME_PROPERTY = "rhq.server.high-availability.name";
    private AgentVersion agentVersion;
    private Server server;
    private String oldServerNamePropertyValue = null;
    private AgentRegistrationRequest aReq = null;
    private AgentRegistrationResults aResults = null;
    private AgentRegistrationRequest zReq = null;
    private AgentRegistrationResults zResults = null;

    private static final int A_PORT = 11111;
    private static final String A_HOST = "hostA";
    private static final int B_PORT = 22222;
    private static final String B_HOST = "hostB";

    public void testNewAgentRegistrationWithOldToken() throws Exception {
        // this tests the case where someone purged an agent from the DB, but then
        // changed their mind and want to re-run that agent and re-register it again.
        // In this case, the agent (if not using --cleanconfig) would still have the old token.
        // The agent should still be allowed to register again.
        CoreServerServiceImpl service = new CoreServerServiceImpl();
        AgentRegistrationRequest request = createRequest(prefixName("old"), "hostOld", 12345, "oldtoken");
        AgentRegistrationResults results = service.registerAgent(request);
        assert results != null : "cannot re-register an old agent";
        Agent agent = LookupUtil.getAgentManager().getAgentByAgentToken(results.getAgentToken());
        assert agent.getName().equals(request.getName());
        assert agent.getAddress().equals(request.getAddress());
        assert agent.getPort() == request.getPort();
        LookupUtil.getAgentManager().deleteAgent(agent);
    }

    public void testChangeAddressPort() throws Exception {
        CoreServerServiceImpl service = new CoreServerServiceImpl();
        AgentRegistrationRequest request;
        AgentRegistrationResults results;

        String zName = prefixName("Z");

        // create a new agent Z with host/port of hostZ/55550
        request = createRequest(zName, "hostZ", 55550, null);
        results = service.registerAgent(request);
        assert results != null : "got null results";

        // now change Z's host to hostZprime
        request = createRequest(zName, "hostZprime", 55550, results.getAgentToken());
        results = service.registerAgent(request);
        assert results != null;
        Agent agent = LookupUtil.getAgentManager().getAgentByAgentToken(results.getAgentToken());
        assert agent.getName().equals(zName);
        assert agent.getAddress().equals("hostZprime");
        assert agent.getPort() == 55550;

        // now change Z's port to 55551
        request = createRequest(zName, "hostZprime", 55551, results.getAgentToken());
        results = service.registerAgent(request);
        assert results != null;
        agent = LookupUtil.getAgentManager().getAgentByAgentToken(results.getAgentToken());
        assert agent.getName().equals(zName);
        assert agent.getAddress().equals("hostZprime");
        assert agent.getPort() == 55551;

        // now change Z's host/port to hostZdoubleprime/55552
        request = createRequest(zName, "hostZdoubleprime", 55552, results.getAgentToken());
        results = service.registerAgent(request);
        assert results != null;
        agent = LookupUtil.getAgentManager().getAgentByAgentToken(results.getAgentToken());
        assert agent.getName().equals(zName);
        assert agent.getAddress().equals("hostZdoubleprime");
        assert agent.getPort() == 55552;

        // now don't change Z's host/port but re-register everything the same with its token
        request = createRequest(zName, "hostZdoubleprime", 55552, results.getAgentToken());
        results = service.registerAgent(request);
        assert results != null;
        agent = LookupUtil.getAgentManager().getAgentByAgentToken(results.getAgentToken());
        assert agent.getName().equals(zName);
        assert agent.getAddress().equals("hostZdoubleprime");
        assert agent.getPort() == 55552;

        // now don't change Z's host/port but re-register everything the same, but with no token
        request = createRequest(zName, "hostZdoubleprime", 55552, null);
        results = service.registerAgent(request);
        assert results != null;
        agent = LookupUtil.getAgentManager().getAgentByAgentToken(results.getAgentToken());
        assert agent.getName().equals(zName);
        assert agent.getAddress().equals("hostZdoubleprime");
        assert agent.getPort() == 55552;

        // remember this agent so our later tests can use it
        zReq = request;
        zResults = results;
    }

    @Test(dependsOnMethods = "testChangeAddressPort")
    public void testNormalAgentRegistration() throws Exception {
        CoreServerServiceImpl service = new CoreServerServiceImpl();
        aReq = createRequest(prefixName("A"), A_HOST, A_PORT, null);
        aResults = service.registerAgent(aReq);
        assert aResults != null : "got null results";
    }

    @Test(dependsOnMethods = "testNormalAgentRegistration")
    public void testHijackExistingAgentAddressPort() throws Exception {
        CoreServerServiceImpl service = new CoreServerServiceImpl();
        AgentRegistrationRequest request;
        request = createRequest(prefixName("B"), aReq.getAddress(), aReq.getPort(), null);
        try {
            service.registerAgent(request);
            assert false : "Should not have been able to hijack a used host/port with new agent name";
        } catch (AgentRegistrationException ok) {
        }
    }

    @Test(dependsOnMethods = "testNormalAgentRegistration")
    public void testHijackExistingAgentName() throws Exception {
        CoreServerServiceImpl service = new CoreServerServiceImpl();
        AgentRegistrationRequest request;
        request = createRequest(aReq.getName(), aReq.getAddress(), B_PORT, null);
        try {
            service.registerAgent(request);
            assert false : "Should not have been able to hijack a used agent name without a token";
        } catch (AgentRegistrationException ok) {
        }
        request = createRequest(aReq.getName(), B_HOST, aReq.getPort(), null);
        try {
            service.registerAgent(request);
            assert false : "Should not have been able to hijack a used agent name without a token";
        } catch (AgentRegistrationException ok) {
        }
        request = createRequest(aReq.getName(), B_HOST, B_PORT, null);
        try {
            service.registerAgent(request);
            assert false : "Should not have been able to hijack a used agent name without a token";
        } catch (AgentRegistrationException ok) {
        }
    }

    @Test(dependsOnMethods = "testNormalAgentRegistration")
    public void testHijackExistingAgentAddressPortWithBogusToken() throws Exception {
        CoreServerServiceImpl service = new CoreServerServiceImpl();
        AgentRegistrationRequest request;
        request = createRequest(prefixName("B"), aReq.getAddress(), aReq.getPort(), "badtoken");
        try {
            service.registerAgent(request);
            assert false : "Should not have been able to hijack a used host/port with new agent name and invalid token";
        } catch (AgentRegistrationException ok) {
        }
    }

    @Test(dependsOnMethods = "testNormalAgentRegistration")
    public void testHijackExistingAgentNameWithBogusToken() throws Exception {
        CoreServerServiceImpl service = new CoreServerServiceImpl();
        AgentRegistrationRequest request;
        request = createRequest(aReq.getName(), aReq.getAddress(), aReq.getPort(), "badtoken");
        try {
            service.registerAgent(request);
            assert false : "Should not have been able to hijack a used agent name with an invalid token";
        } catch (AgentRegistrationException ok) {
        }
        request = createRequest(aReq.getName(), aReq.getAddress(), B_PORT, "badtoken");
        try {
            service.registerAgent(request);
            assert false : "Should not have been able to hijack a used agent name with an invalid token";
        } catch (AgentRegistrationException ok) {
        }
        request = createRequest(aReq.getName(), B_HOST, aReq.getPort(), "badtoken");
        try {
            service.registerAgent(request);
            assert false : "Should not have been able to hijack a used agent name with an invalid token";
        } catch (AgentRegistrationException ok) {
        }
        request = createRequest(aReq.getName(), B_HOST, B_PORT, "badtoken");
        try {
            service.registerAgent(request);
            assert false : "Should not have been able to hijack a used agent name with an invalid token";
        } catch (AgentRegistrationException ok) {
        }
    }

    @Test(dependsOnMethods = "testNormalAgentRegistration")
    public void testHijackExistingAgentNameWithAnotherAgentToken() throws Exception {
        CoreServerServiceImpl service = new CoreServerServiceImpl();
        AgentRegistrationRequest request;
        request = createRequest(aReq.getName(), aReq.getAddress(), aReq.getPort(), zResults.getAgentToken());
        try {
            service.registerAgent(request);
            assert false : "Should not have been able to hijack agent A using Z's token";
        } catch (AgentRegistrationException ok) {
        }
        request = createRequest(aReq.getName(), B_HOST, aReq.getPort(), zResults.getAgentToken());
        try {
            service.registerAgent(request);
            assert false : "Should not have been able to hijack agent A using Z's token";
        } catch (AgentRegistrationException ok) {
        }
        request = createRequest(aReq.getName(), aReq.getAddress(), B_PORT, zResults.getAgentToken());
        try {
            service.registerAgent(request);
            assert false : "Should not have been able to hijack agent A using Z's token";
        } catch (AgentRegistrationException ok) {
        }
        request = createRequest(aReq.getName(), B_HOST, B_PORT, zResults.getAgentToken());
        try {
            service.registerAgent(request);
            assert false : "Should not have been able to hijack agent A using Z's token";
        } catch (AgentRegistrationException ok) {
        }
    }

    @Test(dependsOnMethods = "testNormalAgentRegistration")
    public void testAgentHijackingAnotherAgentAddressPort() throws Exception {
        CoreServerServiceImpl service = new CoreServerServiceImpl();
        AgentRegistrationRequest request;
        request = createRequest(aReq.getName(), zReq.getAddress(), zReq.getPort(), aResults.getAgentToken());
        try {
            service.registerAgent(request);
            assert false : "An agent should not have been able to hijack another agent's host/port";
        } catch (AgentRegistrationException ok) {
        }
    }

    @Test(dependsOnMethods = "testNormalAgentRegistration")
    public void testAttemptToChangeAgentName() throws Exception {
        CoreServerServiceImpl service = new CoreServerServiceImpl();
        AgentRegistrationRequest request;
        request = createRequest(prefixName("newName"), zReq.getAddress(), zReq.getPort(), zResults.getAgentToken());
        try {
            service.registerAgent(request);
            assert false : "An agent should not be able to change its name";
        } catch (AgentRegistrationException ok) {
        }
    }

    private AgentRegistrationRequest createRequest(String name, String address, int port, String token) {
        return new AgentRegistrationRequest(name, address, port, "socket://" + address + ":" + port
            + "/?rhq.communications.connector.rhqtype=agent", true, token, agentVersion);
    }

    private String prefixName(String name) {
        return TEST_AGENT_NAME_PREFIX + name;
    }

    @BeforeClass
    public void prepare() throws Exception {
        // mock the name of our server via the sysprop (in production, this is normally set in rhq-server.properties)
        oldServerNamePropertyValue = System.getProperty(RHQ_SERVER_NAME_PROPERTY);
        String newServerNamePropertyValue = "CoreServerServiceImplTest.Server";
        System.setProperty(RHQ_SERVER_NAME_PROPERTY, newServerNamePropertyValue);

        // mock up our core server MBean that provides information about where the jboss home dir is
        MBeanServer mbs = getJBossMBeanServer();
        DummyCoreServer mbean = new DummyCoreServer();
        mbs.registerMBean(mbean, CoreServerMBean.OBJECT_NAME);

        // in order to register, we need to mock out the agent version file used by the server
        // to determine the agent version it supports.
        agentVersion = new AgentVersion("1.2.3", "12345");
        File agentVersionFile = new File(mbean.getJBossServerHomeDir(),
            "deploy/rhq.ear/rhq-downloads/rhq-agent/rhq-server-agent-versions.properties");
        agentVersionFile.getParentFile().mkdirs();
        agentVersionFile.delete();
        Properties agentVersionProps = new Properties();
        agentVersionProps.put("rhq-agent.latest.version", agentVersion.getVersion());
        agentVersionProps.put("rhq-agent.latest.build-number", agentVersion.getBuild());
        FileOutputStream fos = new FileOutputStream(agentVersionFile);
        try {
            agentVersionProps.store(fos, "This file was created by " + CoreServerServiceImplTest.class.getName());
        } finally {
            fos.close();
        }

        // this mocks out the endpoint ping - the server will think the agent that is registering is up and pingable
        prepareForTestAgents();

        // mock our server
        server = new Server();
        server.setName(newServerNamePropertyValue);
        server.setAddress("CoreServerServiceImplTest.localhost");
        server.setPort(12345);
        server.setSecurePort(12346);
        server.setOperationMode(OperationMode.NORMAL);
        int serverId = LookupUtil.getServerManager().create(server);
        server.setId(serverId);
    }

    @AfterClass
    public void unprepare() throws Exception {
        // clean up any agents we might have created
        Query q = getEntityManager().createQuery(
            "select a from Agent a where name like '" + TEST_AGENT_NAME_PREFIX + "%'");
        List<Agent> doomed = (List<Agent>) q.getResultList();
        for (Agent deleteMe : doomed) {
            LookupUtil.getAgentManager().deleteAgent(deleteMe);
        }

        // cleanup our test server
        LookupUtil.getCloudManager().updateServerMode(new Integer[] { server.getId() }, OperationMode.DOWN);
        LookupUtil.getCloudManager().deleteServer(server.getId());

        // shutdown our mock mbean server
        MBeanServer mbs = getJBossMBeanServer();
        mbs.unregisterMBean(CoreServerMBean.OBJECT_NAME);

        unprepareForTestAgents();

        // in case this was set before our tests, put it back the way it was
        if (oldServerNamePropertyValue != null) {
            System.setProperty(RHQ_SERVER_NAME_PROPERTY, oldServerNamePropertyValue);
        }
    }

    interface DummyCoreServerMBean extends CoreServerMBean {
    };

    class DummyCoreServer implements DummyCoreServerMBean {

        @Override
        public String getName() {
            return "CoreServer";
        }

        @Override
        public int getState() {
            return 0;
        }

        @Override
        public String getStateString() {
            return "";
        }

        @Override
        public void jbossInternalLifecycle(String arg0) throws Exception {
        }

        @Override
        public void create() throws Exception {
        }

        @Override
        public void destroy() {
        }

        @Override
        public void start() throws Exception {
        }

        @Override
        public void stop() {
        }

        @Override
        public String getVersion() {
            return null;
        }

        @Override
        public String getBuildNumber() {
            return null;
        }

        @Override
        public Date getBootTime() {
            return null;
        }

        @Override
        public File getInstallDir() {
            return null;
        }

        @Override
        public File getJBossServerHomeDir() {
            return new File(System.getProperty("java.io.tmpdir"), "CoreServerServiceImplTest");
        }

        @Override
        public File getJBossServerDataDir() {
            return null;
        }

        @Override
        public File getJBossServerTempDir() {
            return null;
        }

        @Override
        public ProductInfo getProductInfo() {
            return null;
        }
    }
}
