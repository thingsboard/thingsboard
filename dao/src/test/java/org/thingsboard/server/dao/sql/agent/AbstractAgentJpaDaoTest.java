// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.agent;

import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.AgentProfile;
import org.thingsboard.server.common.data.agent.AgentProvisionType;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.agent.AgentApplicationDao;
import org.thingsboard.server.dao.agent.AgentDao;
import org.thingsboard.server.dao.agent.AgentProfileDao;

import java.util.UUID;

/**
 * Shared fixture for the agent JPA dao tests: an agent profile, an agent under it, and applications of that
 * agent, saved straight through the daos.
 */
public abstract class AbstractAgentJpaDaoTest extends AbstractJpaDaoTest {

    protected static final String TEMPLATE_VERSION = "4.3.1.2EDGEPE";

    protected UUID tenantId1;
    protected UUID agentId1;
    protected Agent agent1;
    protected AgentProfile agentProfile1;

    @Autowired
    protected AgentDao agentDao;
    @Autowired
    protected AgentProfileDao agentProfileDao;
    @Autowired
    protected AgentApplicationDao agentApplicationDao;

    protected AgentProfile saveAgentProfile(UUID tenantId, String name) {
        AgentProfile profile = new AgentProfile();
        profile.setTenantId(TenantId.fromUUID(tenantId));
        profile.setName(name);
        profile.setProvisionType(AgentProvisionType.DISABLED);
        return agentProfileDao.save(TenantId.fromUUID(tenantId), profile);
    }

    protected Agent saveAgent(UUID id, UUID tenantId, UUID customerId, String name) {
        Agent agent = new Agent();
        agent.setId(new AgentId(id));
        agent.setTenantId(TenantId.fromUUID(tenantId));
        agent.setCustomerId(new CustomerId(customerId));
        agent.setName(name);
        agent.setAgentProfileId(agentProfile1.getId());
        return agentDao.save(TenantId.fromUUID(tenantId), agent);
    }

    protected AgentApplication saveApplication(String name) {
        AgentApplication app = new AgentApplication();
        app.setTenantId(TenantId.fromUUID(tenantId1));
        app.setAgentId(new AgentId(agentId1));
        app.setAppType(AgentApplicationType.EDGE);
        app.setName(name);
        app.setTemplateVersion(TEMPLATE_VERSION);
        return agentApplicationDao.save(TenantId.fromUUID(tenantId1), app);
    }
}
