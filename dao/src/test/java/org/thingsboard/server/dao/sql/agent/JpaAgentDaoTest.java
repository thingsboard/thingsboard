// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.agent;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentProfile;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentInfo;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.agent.AgentInfoDao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class JpaAgentDaoTest extends AbstractAgentJpaDaoTest {

    UUID tenantId2;
    UUID customerId1;
    UUID customerId2;
    AgentProfile agentProfile2;
    List<Agent> agents = new ArrayList<>();

    @Autowired
    private AgentInfoDao agentInfoDao;

    @Before
    public void setUp() {
        tenantId1 = Uuids.timeBased();
        tenantId2 = Uuids.timeBased();
        customerId1 = Uuids.timeBased();
        customerId2 = Uuids.timeBased();
        agentProfile1 = saveAgentProfile(tenantId1, "AGENT_TEST_PROFILE_1");
        agentProfile2 = saveAgentProfile(tenantId2, "AGENT_TEST_PROFILE_2");
        for (int i = 0; i < 60; i++) {
            UUID agentId = Uuids.timeBased();
            UUID tenantId = i % 2 == 0 ? tenantId1 : tenantId2;
            UUID customerId = i % 2 == 0 ? customerId1 : customerId2;
            agents.add(saveAgent(agentId, tenantId, customerId, "AGENT_" + i));
        }
        assertEquals(30, agentDao.countByTenantId(TenantId.fromUUID(tenantId1)).longValue());
        assertEquals(30, agentDao.countByTenantId(TenantId.fromUUID(tenantId2)).longValue());
    }

    @After
    public void tearDown() {
        for (Agent agent : agents) {
            agentDao.removeById(agent.getTenantId(), agent.getUuidId());
        }
        agents.clear();
        if (agentProfile1 != null) {
            agentProfileDao.removeById(TenantId.fromUUID(tenantId1), agentProfile1.getId().getId());
        }
        if (agentProfile2 != null) {
            agentProfileDao.removeById(TenantId.fromUUID(tenantId2), agentProfile2.getId().getId());
        }
    }

    @Test
    public void testSaveAgentName0x00_thenSomeDatabaseException() {
        assertThatThrownBy(() -> agents.add(
                saveAgent(UUID.randomUUID(), tenantId2, customerId2, "F0929906\000\000\000\000\000\000\000\000\000")));
    }

    @Test
    public void testFindAgentsByTenantId() {
        PageLink pageLink = new PageLink(20, 0, "AGENT_");
        PageData<Agent> agents1 = agentDao.findAgentsByTenantId(tenantId1, pageLink);
        assertEquals(20, agents1.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<Agent> agents2 = agentDao.findAgentsByTenantId(tenantId1, pageLink);
        assertEquals(10, agents2.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<Agent> agents3 = agentDao.findAgentsByTenantId(tenantId1, pageLink);
        assertEquals(0, agents3.getData().size());
    }

    @Test
    public void testFindAgentInfosByTenantId() {
        PageLink pageLink = new PageLink(20, 0, "AGENT_");
        PageData<AgentInfo> agents1 = agentInfoDao.findAgentInfosByTenantId(tenantId1, pageLink);
        assertEquals(20, agents1.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<AgentInfo> agents2 = agentInfoDao.findAgentInfosByTenantId(tenantId1, pageLink);
        assertEquals(10, agents2.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<AgentInfo> agents3 = agentInfoDao.findAgentInfosByTenantId(tenantId1, pageLink);
        assertEquals(0, agents3.getData().size());
    }

    @Test
    public void testFindAgentsByTenantIdAndCustomerId() {
        PageLink pageLink = new PageLink(20, 0, "AGENT_");
        PageData<Agent> agents1 = agentDao.findAgentsByTenantIdAndCustomerId(tenantId1, customerId1, pageLink);
        assertEquals(20, agents1.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<Agent> agents2 = agentDao.findAgentsByTenantIdAndCustomerId(tenantId1, customerId1, pageLink);
        assertEquals(10, agents2.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<Agent> agents3 = agentDao.findAgentsByTenantIdAndCustomerId(tenantId1, customerId1, pageLink);
        assertEquals(0, agents3.getData().size());
    }

    @Test
    public void testFindAgentInfosByTenantIdAndCustomerId() {
        PageLink pageLink = new PageLink(20, 0, "AGENT_");
        PageData<AgentInfo> agents1 = agentInfoDao.findAgentInfosByTenantIdAndCustomerId(tenantId1, customerId1, pageLink);
        assertEquals(20, agents1.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<AgentInfo> agents2 = agentInfoDao.findAgentInfosByTenantIdAndCustomerId(tenantId1, customerId1, pageLink);
        assertEquals(10, agents2.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<AgentInfo> agents3 = agentInfoDao.findAgentInfosByTenantIdAndCustomerId(tenantId1, customerId1, pageLink);
        assertEquals(0, agents3.getData().size());
    }

    @Test
    public void testFindAgentInfoById() {
        UUID agentId = Uuids.timeBased();
        String name = "TEST_AGENT";
        agents.add(saveAgent(agentId, tenantId1, customerId1, name));

        AgentInfo agentInfo = agentInfoDao.findAgentInfoById(TenantId.fromUUID(tenantId1), agentId);
        assertNotNull(agentInfo);
        assertEquals(agentId, agentInfo.getId().getId());
        assertEquals(name, agentInfo.getName());
    }

    @Test
    public void testCountByTenantId() {
        Long count = agentDao.countByTenantId(TenantId.fromUUID(tenantId1));
        assertEquals(30, count.longValue());

        Long count2 = agentDao.countByTenantId(TenantId.fromUUID(tenantId2));
        assertEquals(30, count2.longValue());
    }

    @Test
    public void testFindAgentsByTenantIdWithEmptyTextSearch() {
        PageLink pageLink = new PageLink(20, 0, null);
        PageData<Agent> agents1 = agentDao.findAgentsByTenantId(tenantId1, pageLink);
        assertEquals(20, agents1.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<Agent> agents2 = agentDao.findAgentsByTenantId(tenantId1, pageLink);
        assertEquals(10, agents2.getData().size());
    }

    @Test
    public void testFindAgentsByTenantIdAndCustomerIdWithEmptyTextSearch() {
        PageLink pageLink = new PageLink(20, 0, null);
        PageData<Agent> agents1 = agentDao.findAgentsByTenantIdAndCustomerId(tenantId1, customerId1, pageLink);
        assertEquals(20, agents1.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<Agent> agents2 = agentDao.findAgentsByTenantIdAndCustomerId(tenantId1, customerId1, pageLink);
        assertEquals(10, agents2.getData().size());
    }

    @Test
    public void testFindAgentsByTenantIdWithPartialTextSearch() {
        PageLink pageLink = new PageLink(10, 0, "AGENT_1");
        PageData<Agent> agents1 = agentDao.findAgentsByTenantId(tenantId1, pageLink);
        // Should match AGENT_10, AGENT_12, AGENT_14, AGENT_16, AGENT_18 (5 agents with even indices)
        assertEquals(5, agents1.getData().size());
    }

    @Test
    public void testFindAgentInfosByTenantIdWithPartialTextSearch() {
        PageLink pageLink = new PageLink(10, 0, "AGENT_2");
        PageData<AgentInfo> agents1 = agentInfoDao.findAgentInfosByTenantId(tenantId1, pageLink);
        // Should match AGENT_2, AGENT_20, AGENT_22, AGENT_24, AGENT_26, AGENT_28 (6 agents with even indices)
        assertEquals(6, agents1.getData().size());
    }

    @Test
    public void testDeleteAgentRemovesAgentApplications() {
        UUID agentId = Uuids.timeBased();
        Agent agent = saveAgent(agentId, tenantId1, customerId1, "AGENT_FOR_APPS");
        agents.add(agent);

        AgentApplication app1 = new AgentApplication();
        app1.setTenantId(TenantId.fromUUID(tenantId1));
        app1.setAgentId(new AgentId(agentId));
        app1.setAppType(AgentApplicationType.EDGE);
        app1.setTemplateVersion("4.3.1.2EDGEPE");
        agentApplicationDao.save(TenantId.fromUUID(tenantId1), app1);

        AgentApplication app2 = new AgentApplication();
        app2.setTenantId(TenantId.fromUUID(tenantId1));
        app2.setAgentId(new AgentId(agentId));
        app2.setAppType(AgentApplicationType.EDGE);
        app2.setTemplateVersion("4.3.1.2EDGEPE");
        agentApplicationDao.save(TenantId.fromUUID(tenantId1), app2);

        List<AgentApplication> before = agentApplicationDao.findByAgentId(TenantId.fromUUID(tenantId1), agentId);
        assertEquals(2, before.size());

        agentDao.removeById(TenantId.fromUUID(tenantId1), agentId);
        agents.remove(agent);

        List<AgentApplication> after = agentApplicationDao.findByAgentId(TenantId.fromUUID(tenantId1), agentId);
        assertEquals(0, after.size());
    }

    @Test
    public void testFindByRoutingKey() {
        UUID agentId = Uuids.timeBased();
        String routingKey = "routing-key-" + agentId;
        Agent agent = saveAgent(agentId, tenantId1, customerId1, "ROUTING_KEY_AGENT", routingKey, "test-secret");
        agents.add(agent);

        Agent found = agentDao.findByRoutingKey(tenantId1, routingKey);
        assertNotNull(found);
        assertEquals(agent.getId(), found.getId());
        assertEquals(routingKey, found.getRoutingKey());
        assertEquals("test-secret", found.getSecret());

        Agent notFound = agentDao.findByRoutingKey(tenantId1, "non-existent-key");
        assertNull(notFound);
    }

    @Override
    protected Agent saveAgent(UUID id, UUID tenantId, UUID customerId, String name) {
        return saveAgent(id, tenantId, customerId, name, UUID.randomUUID().toString(), UUID.randomUUID().toString());
    }

    private Agent saveAgent(UUID id, UUID tenantId, UUID customerId, String name, String routingKey, String secret) {
        Agent agent = new Agent();
        agent.setId(new AgentId(id));
        agent.setTenantId(TenantId.fromUUID(tenantId));
        agent.setCustomerId(new CustomerId(customerId));
        agent.setName(name);
        agent.setRoutingKey(routingKey);
        agent.setSecret(secret);
        agent.setAgentProfileId(tenantId.equals(tenantId1) ? agentProfile1.getId() : agentProfile2.getId());
        return agentDao.save(TenantId.fromUUID(tenantId), agent);
    }

}
