// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.agent;

import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationInfo;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.TenantEntityDao;

import java.util.List;
import java.util.UUID;

public interface AgentApplicationDao extends Dao<AgentApplication>, TenantEntityDao<AgentApplication> {

    void removeByAgentId(TenantId tenantId, UUID agentId);

    AgentApplication findByIdForUpdate(TenantId tenantId, UUID id);

    List<AgentApplication> findByAgentId(TenantId tenantId, UUID agentId);

    PageData<AgentApplication> findByAgentId(TenantId tenantId, UUID agentId, PageLink pageLink);

    AgentApplication findByProjectName(TenantId tenantId, AgentId agentId, String projectName);

    AgentApplication findByEventId(TenantId tenantId, UUID eventId);

    AgentApplicationInfo findInfoById(TenantId tenantId, UUID id);

    PageData<AgentApplicationInfo> findInfosByAgentId(TenantId tenantId, UUID agentId, PageLink pageLink);

    PageData<AgentApplicationInfo> findByApplicationProfileIdAndAgentProfileId(TenantId tenantId, UUID profileId, UUID agentProfileId, PageLink pageLink);

    PageData<AgentApplication> findByEntityGroupId(UUID groupId, PageLink pageLink);

    PageData<AgentApplication> findByEntityGroupIds(List<UUID> groupIds, PageLink pageLink);

    AgentApplication findByRelatedEntity(TenantId tenantId, UUID relatedEntityId);

    List<UUID> findManagedRelatedEntityIds(TenantId tenantId, String relatedEntityType);

    PageData<EntityInfo> findRelatedEntityCandidates(TenantId tenantId, EntityType relatedEntityType, UUID currentEntityId, PageLink pageLink);

    int promoteDesiredTemplate(TenantId tenantId, AgentApplicationId applicationId, String templateVersion);

}
