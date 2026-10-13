// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventInfo;
import org.thingsboard.server.common.data.agent.AgentInfo;
import org.thingsboard.server.common.data.agent.AgentInstructions;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class AgentControllerTest extends AbstractControllerTest {

    @Before
    public void beforeTest() throws Exception {
        loginTenantAdmin();
    }

    @Test
    public void testSaveGetAndDeleteAgent() throws Exception {
        Agent saved = createAgent("Controller Agent");
        Assert.assertNotNull(saved.getId());
        Assert.assertEquals(tenantId, saved.getTenantId());

        Agent found = doGet("/api/agent/" + saved.getId().getId(), Agent.class);
        Assert.assertEquals(saved.getId(), found.getId());

        doDelete("/api/agent/" + saved.getId().getId()).andExpect(status().isOk());
        doGet("/api/agent/" + saved.getId().getId()).andExpect(status().isNotFound());
    }

    @Test
    public void testGetTenantAgents_paging() throws Exception {
        createAgent("Paging Agent A");
        createAgent("Paging Agent B");

        PageData<Agent> page = doGetTypedWithPageLink("/api/tenant/agents?",
                new TypeReference<>() {}, new PageLink(100));
        Assert.assertTrue(page.getTotalElements() >= 2);
    }

    @Test
    public void testDeleteAgent_customerForbidden() throws Exception {
        Agent saved = createAgent("Customer Forbidden Agent");

        loginCustomerUser();
        doDelete("/api/agent/" + saved.getId().getId()).andExpect(status().isForbidden());

        loginTenantAdmin();
    }

    @Test
    public void testGetAgent_crossTenantDenied() throws Exception {
        Agent saved = createAgent("Cross Tenant Agent");

        loginDifferentTenant();
        doGet("/api/agent/" + saved.getId().getId()).andExpect(status().isForbidden());

        loginTenantAdmin();
    }

    @Test
    public void testGetNonExistentAgent_notFound() throws Exception {
        doGet("/api/agent/" + UUID.randomUUID()).andExpect(status().isNotFound());
    }

    @Test
    public void testGetTenantAgentInfos_paging() throws Exception {
        Agent saved = createAgent("Info Paging Agent");

        PageData<AgentInfo> page = doGetTypedWithPageLink("/api/tenant/agentInfos?",
                new TypeReference<>() {}, new PageLink(100));
        Assert.assertTrue(page.getData().stream().anyMatch(info -> info.getId().equals(saved.getId())));
    }

    @Test
    public void testGetAgentInfoById() throws Exception {
        Agent saved = createAgent("Info Agent");

        AgentInfo info = doGet("/api/agent/info/" + saved.getId().getId(), AgentInfo.class);
        Assert.assertEquals(saved.getId(), info.getId());
        Assert.assertEquals(saved.getName(), info.getName());
    }

    @Test
    public void testGetAgentInfosByIds() throws Exception {
        Agent first = createAgent("By Ids Agent A");
        Agent second = createAgent("By Ids Agent B");
        createAgent("By Ids Agent C");

        List<AgentInfo> infos = doGetTyped("/api/agentInfos?agentIds="
                + first.getId().getId() + "," + second.getId().getId() + "," + UUID.randomUUID(), new TypeReference<>() {});
        Assert.assertEquals(2, infos.size());
        Assert.assertEquals(first.getId(), infos.get(0).getId());
        Assert.assertEquals(first.getName(), infos.get(0).getName());
        Assert.assertEquals(second.getId(), infos.get(1).getId());
    }

    @Test
    public void testGetAgentInfosByIds_crossTenantReturnsNothing() throws Exception {
        Agent saved = createAgent("By Ids Cross Tenant Agent");

        loginDifferentTenant();
        List<AgentInfo> infos = doGetTyped("/api/agentInfos?agentIds=" + saved.getId().getId(), new TypeReference<>() {});
        Assert.assertTrue(infos.isEmpty());

        loginTenantAdmin();
    }

    @Test
    public void testGetAgentInfo_crossTenantDenied() throws Exception {
        Agent saved = createAgent("Info Cross Tenant Agent");

        loginDifferentTenant();
        doGet("/api/agent/info/" + saved.getId().getId()).andExpect(status().isForbidden());

        loginTenantAdmin();
    }

    @Test
    public void testGetCustomerAgentsAndInfos() throws Exception {
        Agent saved = createAgent("Customer Agent");

        PageData<Agent> agents = doGetTypedWithPageLink("/api/customer/" + customerId.getId() + "/agents?",
                new TypeReference<>() {}, new PageLink(100));
        Assert.assertTrue(agents.getData().stream().noneMatch(agent -> agent.getId().equals(saved.getId())));

        PageData<AgentInfo> infos = doGetTypedWithPageLink("/api/customer/" + customerId.getId() + "/agentInfos?",
                new TypeReference<>() {}, new PageLink(100));
        Assert.assertTrue(infos.getData().stream().noneMatch(info -> info.getId().equals(saved.getId())));
    }

    @Test
    public void testGetAgentAppEventsAndEventInfos_emptyForFreshAgent() throws Exception {
        Agent saved = createAgent("Event Listing Agent");

        PageData<AgentAppEvent> events = doGetTypedWithPageLink("/api/agent/" + saved.getId().getId() + "/events?",
                new TypeReference<>() {}, new PageLink(100));
        Assert.assertEquals(0, events.getTotalElements());

        PageData<AgentAppEventInfo> eventInfos = doGetTypedWithPageLink("/api/agent/" + saved.getId().getId() + "/eventInfos?",
                new TypeReference<>() {}, new PageLink(100));
        Assert.assertEquals(0, eventInfos.getTotalElements());
    }

    @Test
    public void testGetAgentAppEvents_crossTenantDenied() throws Exception {
        Agent saved = createAgent("Event Cross Tenant Agent");

        loginDifferentTenant();
        doGet("/api/agent/" + saved.getId().getId() + "/events?pageSize=100&page=0").andExpect(status().isForbidden());
        doGet("/api/agent/" + saved.getId().getId() + "/eventInfos?pageSize=100&page=0").andExpect(status().isForbidden());

        loginTenantAdmin();
    }

    @Test
    public void testUpgradeEndpointsForAnAgentThatNeverReportedAnImage() throws Exception {
        Agent saved = createAgent("Upgrade Agent");

        String available = doGet("/api/agent/" + saved.getId().getId() + "/upgrade/available")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Assert.assertTrue(available, available.contains("false"));

        String target = doGet("/api/agent/" + saved.getId().getId() + "/upgrade/target")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Assert.assertFalse(target, target.contains("thingsboard/tb-remote-agent"));

        String latest = doGet("/api/agent/upgrade/latest")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Assert.assertTrue(latest, latest.contains("thingsboard/tb-remote-agent:"));
    }

    @Test
    public void testUpgradeAvailable_crossTenantDenied() throws Exception {
        Agent saved = createAgent("Upgrade Cross Tenant Agent");

        loginDifferentTenant();
        doGet("/api/agent/" + saved.getId().getId() + "/upgrade/available").andExpect(status().isForbidden());
        doGet("/api/agent/" + saved.getId().getId() + "/upgrade/target").andExpect(status().isForbidden());

        loginTenantAdmin();
    }

    @Test
    public void testGetAgentInstallInstructions() throws Exception {
        Agent saved = createAgent("Install Instructions Agent");

        AgentInstructions instructions = doGet("/api/agent/instructions/install/" + saved.getId().getId() + "/docker",
                AgentInstructions.class);
        Assert.assertNotNull(instructions.getInstructions());
        Assert.assertTrue(instructions.getInstructions().contains("TB_AGENT_ROUTING_KEY="));
        Assert.assertTrue(instructions.getInstructions().contains("thingsboard/tb-remote-agent:"));
    }

    @Test
    public void testGetAgentInstallInstructions_crossTenantDenied() throws Exception {
        Agent saved = createAgent("Install Instructions Cross Tenant Agent");

        loginDifferentTenant();
        doGet("/api/agent/instructions/install/" + saved.getId().getId() + "/docker").andExpect(status().isForbidden());

        loginTenantAdmin();
    }

    @Test
    public void testSaveAgent_tenantProfileLimitReached_thenForbiddenUntilAgentDeleted() throws Exception {
        long limit = 3;
        updateDefaultTenantProfileConfig(config -> config.setMaxAgents(limit));
        try {
            List<Agent> agents = new ArrayList<>();
            for (int i = 0; i < limit; i++) {
                agents.add(createAgent("Limit Agent " + i));
            }

            doPost("/api/agent", newAgent("Limit Agent Out Of Limit")).andExpect(status().isForbidden());

            doDelete("/api/agent/" + agents.get(0).getId().getId()).andExpect(status().isOk());
            Assert.assertNotNull(createAgent("Limit Agent After Delete").getId());
        } finally {
            updateDefaultTenantProfileConfig(config -> config.setMaxAgents(0));
        }
    }

    private Agent createAgent(String name) throws Exception {
        return doPost("/api/agent", newAgent(name), Agent.class);
    }

    private Agent newAgent(String name) {
        Agent agent = new Agent();
        agent.setName(name);
        agent.setRoutingKey(StringUtils.randomAlphanumeric(20));
        agent.setSecret(StringUtils.randomAlphanumeric(20));
        return agent;
    }
}
