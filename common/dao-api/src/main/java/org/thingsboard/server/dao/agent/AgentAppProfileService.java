// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.agent;

import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentAppProfileInfo;
import org.thingsboard.server.common.data.agent.AgentAppProfileRelationInfo;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentProfileId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;

public interface AgentAppProfileService extends EntityDaoService {

    AgentAppProfile saveProfile(AgentAppProfile profile);

    AgentAppProfile findProfileById(TenantId tenantId, AgentAppProfileId profileId);

    AgentAppProfileInfo findProfileInfoById(TenantId tenantId, AgentAppProfileId profileId);

    PageData<AgentAppProfile> findProfilesByTenantId(TenantId tenantId, PageLink pageLink);

    List<AgentAppProfileInfo> findProfileInfosByTenantIdAndAppType(TenantId tenantId, AgentApplicationType appType);

    List<AgentAppProfile> findProfilesByTenantIdAndAppTypeAndTemplateVersion(TenantId tenantId, AgentApplicationType appType, String templateVersion);

    List<AgentAppProfile> findProfilesByTenantIdAndIds(TenantId tenantId, List<AgentAppProfileId> profileIds);

    List<AgentAppProfileRelationInfo> findProfileRelationInfosByAgentProfileId(TenantId tenantId, AgentProfileId agentProfileId);

    List<AgentAppProfileRelationInfo> findProfileRelationInfosByAgentProfileIdAndAppTypeAndTemplateVersion(
            TenantId tenantId, AgentProfileId agentProfileId, AgentApplicationType appType, String templateVersion);

    void deleteProfile(TenantId tenantId, AgentAppProfileId profileId);

    List<AgentAppProfile> findUninstalledAppProfilesForAgentProfile(TenantId tenantId, AgentProfileId agentProfileId, AgentId agentId);

    List<AgentAppProfile> findUninstalledAppProfilesByAppTypeForAgentProfile(TenantId tenantId, AgentProfileId agentProfileId, AgentId agentId);
}
