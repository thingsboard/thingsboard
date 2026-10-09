// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.agent;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.agent.imitator.AgentImitator;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentProfile;
import org.thingsboard.server.common.data.agent.AgentProvisionType;
import org.thingsboard.server.common.data.id.AgentProfileId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.agent.v1.ProvisionResponse;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
@TestPropertySource(properties = {
        "edges.enabled=true",
        "agents.enabled=true",
        "queue.rule-engine.stats.enabled=false"
})
public class AgentProvisioningTest extends AbstractControllerTest {

    @DynamicPropertySource
    static void agentTestProps(DynamicPropertyRegistry registry) {
        registry.add("edges.rpc.port", () -> AbstractAgentTest.AGENT_PORT);
    }

    @Autowired
    private AgentService agentService;

    private final List<AgentProfileId> createdProfiles = new ArrayList<>();
    private AgentImitator imitator;

    @Before
    public void setUp() throws Exception {
        loginTenantAdmin();
    }

    @After
    public void tearDown() throws Exception {
        if (imitator != null) {
            try {
                imitator.disconnect();
            } catch (Exception ignored) {
            }
        }
        loginTenantAdmin();
        for (Agent agent : agentService.findAgentsByTenantId(tenantId, new PageLink(1000)).getData()) {
            try {
                agentService.deleteAgent(tenantId, agent.getId());
            } catch (Exception ignored) {
            }
        }
        for (AgentProfileId id : createdProfiles) {
            try {
                doDelete("/api/agent/profile/" + id.getId()).andExpect(status().isOk());
            } catch (Exception ignored) {
            }
        }
    }

    @Test
    public void testHappyPath() throws Exception {
        AgentProfile agentProfile = createAgentProfile(AgentProvisionType.AUTO_INSTALL_PER_APP_PROFILE, null, null);
        Assert.assertNotNull(agentProfile.getProvisionKey());
        Assert.assertNotNull(agentProfile.getProvisionSecret());
        Assert.assertEquals(20, agentProfile.getProvisionKey().length());
        Assert.assertEquals(20, agentProfile.getProvisionSecret().length());

        imitator = new AgentImitator(AbstractAgentTest.AGENT_HOST, AbstractAgentTest.AGENT_PORT, "", "");
        imitator.provision(agentProfile.getProvisionKey(), agentProfile.getProvisionSecret());

        ProvisionResponse resp = imitator.getProvisionResponse();
        Assert.assertNotNull(resp);
        Assert.assertTrue("Expected success, got: " + resp.getErrorMessage(), resp.getSuccess());
        Assert.assertFalse(resp.getRoutingKey().isEmpty());
        Assert.assertFalse(resp.getRoutingSecret().isEmpty());

        // Verify Agent persisted with the returned routing key, tied to the agentProfile
        Agent provisioned = agentService.findAgentByRoutingKey(tenantId, resp.getRoutingKey());
        Assert.assertNotNull("Provisioned agent should exist in DB", provisioned);
        Assert.assertEquals(agentProfile.getId(), provisioned.getAgentProfileId());
        Assert.assertEquals(resp.getRoutingSecret(), provisioned.getSecret());

        // Reconnect with the routing creds the imitator captured from ProvisionResponse
        imitator.connect();
        Assert.assertNotNull(imitator.getHelloAck());
        Assert.assertTrue(imitator.getHelloAck().getSuccess());
    }

    @Test
    public void testProvisioningWithPerAppTypeStrategy() throws Exception {
        AgentProfile agentProfile = createAgentProfile(AgentProvisionType.AUTO_INSTALL_PER_APP_TYPE, null, null);
        Assert.assertNotNull(agentProfile.getProvisionKey());
        Assert.assertNotNull(agentProfile.getProvisionSecret());

        imitator = new AgentImitator(AbstractAgentTest.AGENT_HOST, AbstractAgentTest.AGENT_PORT, "", "");
        imitator.provision(agentProfile.getProvisionKey(), agentProfile.getProvisionSecret());

        ProvisionResponse resp = imitator.getProvisionResponse();
        Assert.assertNotNull(resp);
        Assert.assertTrue("Expected success, got: " + resp.getErrorMessage(), resp.getSuccess());

        Agent provisioned = agentService.findAgentByRoutingKey(tenantId, resp.getRoutingKey());
        Assert.assertNotNull("Provisioned agent should exist in DB", provisioned);
        Assert.assertEquals(agentProfile.getId(), provisioned.getAgentProfileId());
    }

    @Test
    public void testProvisioningWithNoAutoInstallStrategy() throws Exception {
        AgentProfile agentProfile = createAgentProfile(AgentProvisionType.NO_AUTO_INSTALL, null, null);
        Assert.assertNotNull(agentProfile.getProvisionKey());
        Assert.assertNotNull(agentProfile.getProvisionSecret());

        imitator = new AgentImitator(AbstractAgentTest.AGENT_HOST, AbstractAgentTest.AGENT_PORT, "", "");
        imitator.provision(agentProfile.getProvisionKey(), agentProfile.getProvisionSecret());

        ProvisionResponse resp = imitator.getProvisionResponse();
        Assert.assertNotNull(resp);
        Assert.assertTrue("Expected success, got: " + resp.getErrorMessage(), resp.getSuccess());

        Agent provisioned = agentService.findAgentByRoutingKey(tenantId, resp.getRoutingKey());
        Assert.assertNotNull("Provisioned agent should exist in DB", provisioned);
        Assert.assertEquals(agentProfile.getId(), provisioned.getAgentProfileId());
    }

    @Test
    public void testInvalidProvisionKey() throws Exception {
        AgentProfile agentProfile = createAgentProfile(AgentProvisionType.AUTO_INSTALL_PER_APP_PROFILE, null, null);

        imitator = new AgentImitator(AbstractAgentTest.AGENT_HOST, AbstractAgentTest.AGENT_PORT, "", "");
        imitator.provision("not-a-real-key", agentProfile.getProvisionSecret());

        ProvisionResponse resp = imitator.getProvisionResponse();
        Assert.assertNotNull(resp);
        Assert.assertFalse(resp.getSuccess());
        Assert.assertEquals("Invalid provisioning credentials", resp.getErrorMessage());
    }

    @Test
    public void testInvalidProvisionSecret() throws Exception {
        AgentProfile agentProfile = createAgentProfile(AgentProvisionType.AUTO_INSTALL_PER_APP_PROFILE, null, null);

        imitator = new AgentImitator(AbstractAgentTest.AGENT_HOST, AbstractAgentTest.AGENT_PORT, "", "");
        imitator.provision(agentProfile.getProvisionKey(), "wrong-secret");

        ProvisionResponse resp = imitator.getProvisionResponse();
        Assert.assertNotNull(resp);
        Assert.assertFalse(resp.getSuccess());
        Assert.assertEquals("Invalid provisioning credentials", resp.getErrorMessage());
    }

    @Test
    public void testProvisioningDisabled() throws Exception {
        // Auto-generation only kicks in for non-DISABLED profiles; set keys explicitly here
        String key = StringUtils.randomAlphanumeric(20);
        String secret = StringUtils.randomAlphanumeric(20);
        AgentProfile agentProfile = createAgentProfile(AgentProvisionType.DISABLED, key, secret);

        imitator = new AgentImitator(AbstractAgentTest.AGENT_HOST, AbstractAgentTest.AGENT_PORT, "", "");
        imitator.provision(key, secret);

        ProvisionResponse resp = imitator.getProvisionResponse();
        Assert.assertNotNull(resp);
        Assert.assertFalse(resp.getSuccess());
        Assert.assertEquals("Auto-provisioning is disabled", resp.getErrorMessage());
    }

    @Test
    public void testAutoGeneratedKeysAreUnique() {
        AgentProfile g1 = createAgentProfile(AgentProvisionType.AUTO_INSTALL_PER_APP_PROFILE, null, null);
        AgentProfile g2 = createAgentProfile(AgentProvisionType.AUTO_INSTALL_PER_APP_PROFILE, null, null);
        Assert.assertNotEquals(g1.getProvisionKey(), g2.getProvisionKey());
        Assert.assertNotEquals(g1.getProvisionSecret(), g2.getProvisionSecret());
    }

    private AgentProfile createAgentProfile(AgentProvisionType type, String provisionKey, String provisionSecret) {
        AgentProfile g = new AgentProfile();
        g.setName("provision-test-" + System.nanoTime());
        g.setProvisionType(type);
        if (provisionKey != null) {
            g.setProvisionKey(provisionKey);
        }
        if (provisionSecret != null) {
            g.setProvisionSecret(provisionSecret);
        }
        try {
            AgentProfile saved = doPost("/api/agent/profile", g, AgentProfile.class);
            createdProfiles.add(saved.getId());
            return saved;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
