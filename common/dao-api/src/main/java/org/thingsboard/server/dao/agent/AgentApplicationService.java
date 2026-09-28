// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.agent;

import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationInfo;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;

public interface AgentApplicationService extends EntityDaoService {

    AgentApplication save(TenantId tenantId, AgentApplication agentApplication);
    AgentApplication saveWithRelatedEntity(TenantId tenantId, AgentApplication agentApplication, EntityId relatedEntityId);
    AgentApplication findById(TenantId tenantId, AgentApplicationId agentApplicationId);
    AgentApplication findByIdForUpdate(TenantId tenantId, AgentApplicationId agentApplicationId);
    AgentApplicationInfo findInfoById(TenantId tenantId, AgentApplicationId agentApplicationId);
    AgentApplication findByProjectName(TenantId tenantId, AgentId agentId, String projectName);
    AgentApplication findByEventId(TenantId tenantId, AgentAppEventId agentAppEventId);
    PageData<AgentApplication> findByAgentId(TenantId tenantId, AgentId agentId, PageLink pageLink);
    PageData<AgentApplicationInfo> findInfosByAgentId(TenantId tenantId, AgentId agentId, PageLink pageLink);
    PageData<AgentApplication> findByEntityGroupId(EntityGroupId groupId, PageLink pageLink);
    PageData<AgentApplication> findByEntityGroupIds(List<EntityGroupId> groupIds, PageLink pageLink);
    AgentApplication findByRelatedEntity(TenantId tenantId, EntityId entityId);
    List<EntityId> findManagedRelatedEntityIds(TenantId tenantId, EntityType relatedEntityType);

    PageData<EntityInfo> findRelatedEntityCandidates(TenantId tenantId, EntityType relatedEntityType, EntityId currentEntityId, PageLink pageLink);
    void delete(TenantId tenantId, AgentApplicationId agentApplicationId);
    void deleteByAgentId(TenantId tenantId, AgentId agentId);
    void promoteDesiredTemplate(TenantId tenantId, AgentApplicationId agentApplicationId);
    AgentApplication assignRelatedEntity(TenantId tenantId, AgentApplicationId agentApplicationId, EntityId relatedEntityId);
    AgentApplication unassignRelatedEntity(TenantId tenantId, AgentApplicationId agentApplicationId);

}
