// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.agent;

import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentInfo;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;

public interface AgentService extends EntityDaoService {
    Agent saveAgent(Agent agent);
    Agent findAgentById(TenantId tenantId, AgentId agentId);
    Agent findAgentByRoutingKey(TenantId tenantId, String routingKey);
    AgentInfo findAgentInfoById(TenantId tenantId, AgentId agentId);
    List<AgentInfo> findAgentInfosByTenantIdAndIds(TenantId tenantId, List<AgentId> agentIds);
    PageData<Agent> findAgentsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);
    PageData<AgentId> findAgentIdsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);
    PageData<AgentInfo> findAgentInfosByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);
    PageData<Agent> findAgentsByTenantId(TenantId tenantId, PageLink pageLink);
    PageData<AgentInfo> findAgentInfosByTenantId(TenantId tenantId, PageLink pageLink);
    PageData<Agent> findAgentsByEntityGroupId(EntityGroupId groupId, PageLink pageLink);
    PageData<Agent> findAgentsByEntityGroupIds(List<EntityGroupId> groupIds, PageLink pageLink);
    void deleteAgent(TenantId tenantId, AgentId agentId);
    void deleteAgentsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId);
    Long countAgents();
}
