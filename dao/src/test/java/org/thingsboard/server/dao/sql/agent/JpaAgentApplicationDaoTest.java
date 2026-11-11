// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.agent;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.ProcessingStartStatus;
import org.thingsboard.server.common.data.agent.AgentProcessingStatus;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppEventDao;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class JpaAgentApplicationDaoTest extends AbstractAgentJpaDaoTest {

    @Autowired
    private AgentAppEventDao agentAppEventDao;

    @Before
    public void setUp() {
        tenantId1 = Uuids.timeBased();
        agentId1 = Uuids.timeBased();
        agentProfile1 = saveAgentProfile(tenantId1, "AGENT_APP_TEST_PROFILE");
        agent1 = saveAgent(agentId1, tenantId1, Uuids.timeBased(), "AGENT_APP_TEST");
    }

    @After
    public void tearDown() {
        if (agent1 != null) {
            List<AgentApplication> apps = agentApplicationDao.findByAgentId(TenantId.fromUUID(tenantId1), agentId1);
            for (AgentApplication app : apps) {
                agentApplicationDao.removeById(TenantId.fromUUID(tenantId1), app.getId().getId());
            }
            agentDao.removeById(TenantId.fromUUID(tenantId1), agentId1);
        }
        if (agentProfile1 != null) {
            agentProfileDao.removeById(TenantId.fromUUID(tenantId1), agentProfile1.getId().getId());
        }
    }

    @Test
    public void testSaveFindByIdFindByAgentId() {
        String templateVersion = TEMPLATE_VERSION;
        AgentApplication app = new AgentApplication();
        app.setTenantId(TenantId.fromUUID(tenantId1));
        app.setAgentId(new AgentId(agentId1));
        app.setAppType(AgentApplicationType.EDGE);
        app.setTemplateVersion(templateVersion);

        AgentApplication saved = agentApplicationDao.save(TenantId.fromUUID(tenantId1), app);
        assertNotNull(saved.getId());

        AgentApplication found = agentApplicationDao.findById(TenantId.fromUUID(tenantId1), saved.getId().getId());
        assertNotNull(found);
        assertEquals(saved.getId(), found.getId());
        assertEquals(agentId1, found.getAgentId().getId());
        assertEquals(templateVersion, found.getTemplateVersion());

        List<AgentApplication> byAgent = agentApplicationDao.findByAgentId(TenantId.fromUUID(tenantId1), agentId1);
        assertEquals(1, byAgent.size());
        assertEquals(saved.getId(), byAgent.get(0).getId());
        assertEquals(templateVersion, byAgent.get(0).getTemplateVersion());

        agentApplicationDao.removeById(TenantId.fromUUID(tenantId1), saved.getId().getId());
    }

    @Test
    public void testRemoveById() {
        AgentApplication app = saveApplication("v1");
        agentApplicationDao.removeById(TenantId.fromUUID(tenantId1), app.getId().getId());
        AgentApplication found = agentApplicationDao.findById(TenantId.fromUUID(tenantId1), app.getId().getId());
        assertNull(found);
    }

    @Test
    public void testRemoveByAgentId() {
        saveApplication("a1");
        saveApplication("a2");
        List<AgentApplication> before = agentApplicationDao.findByAgentId(TenantId.fromUUID(tenantId1), agentId1);
        assertEquals(2, before.size());

        agentApplicationDao.removeByAgentId(TenantId.fromUUID(tenantId1), agentId1);
        List<AgentApplication> after = agentApplicationDao.findByAgentId(TenantId.fromUUID(tenantId1), agentId1);
        assertTrue(after.isEmpty());
    }

    @Test
    public void testDeleteAgentRemovesAgentApplications() {
        saveApplication("cascade1");
        saveApplication("cascade2");
        List<AgentApplication> before = agentApplicationDao.findByAgentId(TenantId.fromUUID(tenantId1), agentId1);
        assertEquals(2, before.size());

        agentDao.removeById(TenantId.fromUUID(tenantId1), agentId1);
        agent1 = null;

        List<AgentApplication> after = agentApplicationDao.findByAgentId(TenantId.fromUUID(tenantId1), agentId1);
        assertTrue(after.isEmpty());
    }

    @Test
    public void testFindByEventId() {
        AgentApplication app = saveApplication("eventApp");
        TenantId tid = TenantId.fromUUID(tenantId1);

        AgentAppEvent event = new AgentAppEvent();
        event.setTenantId(tid);
        event.setApplicationId(app.getId());
        event.setActionType(AgentAppEventActionType.INSTALL);
        event.setStartStatus(ProcessingStartStatus.PENDING);
        event.setProcessingStatus(AgentProcessingStatus.PENDING);
        event.setUpdatedTime(System.currentTimeMillis());
        AgentAppEvent savedEvent = agentAppEventDao.save(tid, event);

        AgentApplication found = agentApplicationDao.findByEventId(tid, savedEvent.getId().getId());
        assertNotNull(found);
        assertEquals(app.getId(), found.getId());

        // non-existent event id
        AgentApplication notFound = agentApplicationDao.findByEventId(tid, Uuids.timeBased());
        assertNull(notFound);
    }

}
