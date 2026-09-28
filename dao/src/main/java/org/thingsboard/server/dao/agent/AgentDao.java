// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.agent;

import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.TenantEntityDao;

import java.util.List;
import java.util.UUID;

public interface AgentDao extends Dao<Agent>, TenantEntityDao<Agent> {

    Agent findByRoutingKey(UUID tenantId, String routingKey);
    PageData<Agent> findAgentsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink);
    PageData<AgentId> findAgentIdsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink);
    PageData<Agent> findAgentsByTenantId(UUID id, PageLink pageLink);
    PageData<Agent> findAgentsByEntityGroupId(UUID groupId, PageLink pageLink);
    PageData<Agent> findAgentsByEntityGroupIds(List<UUID> groupIds, PageLink pageLink);
    Long countByTenantId(TenantId tenantId);
    Long countAgents();
}
