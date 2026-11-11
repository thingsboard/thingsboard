// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.agent;

import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentAppProfileInfo;
import org.thingsboard.server.common.data.agent.AgentAppProfileRelationInfo;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.UUID;

public interface AgentAppProfileDao extends Dao<AgentAppProfile> {

    PageData<AgentAppProfile> findByTenantId(UUID tenantId, PageLink pageLink);

    AgentAppProfileInfo findInfoById(UUID profileId);

    List<AgentAppProfileInfo> findInfosByTenantIdAndAppType(UUID tenantId, AgentApplicationType appType);

    List<AgentAppProfile> findByTenantIdAndAppTypeAndTemplateVersion(UUID tenantId, AgentApplicationType appType, String templateVersion);

    List<AgentAppProfile> findByTenantIdAndIds(UUID tenantId, List<UUID> profileIds);

    List<AgentAppProfileRelationInfo> findRelationInfosByAgentProfileId(UUID agentProfileId, String relationType);

    List<AgentAppProfileRelationInfo> findRelationInfosByAgentProfileIdAndAppTypeAndTemplateVersion(UUID agentProfileId, AgentApplicationType appType, String templateVersion, String relationType);

    Long countByTenantId(TenantId tenantId);

    List<AgentAppProfile> findUninstalledAppProfilesForAgentProfile(UUID agentProfileId, UUID agentId, String relationType);

    List<AgentAppProfile> findUninstalledAppProfilesByAppTypeForAgentProfile(UUID agentProfileId, UUID agentId, String relationType);
}
