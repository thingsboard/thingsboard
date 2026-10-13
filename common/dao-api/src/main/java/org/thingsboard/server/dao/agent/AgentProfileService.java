// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.agent;

import org.thingsboard.server.common.data.agent.AgentProfile;
import org.thingsboard.server.common.data.agent.AgentProfileInfo;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

public interface AgentProfileService extends EntityDaoService {

    String RELATES_ON_AUTO_DISCOVERY = "relates-on-auto-discovery";

    AgentProfile saveProfile(AgentProfile profile);

    AgentProfile createDefaultAgentProfile(TenantId tenantId);

    AgentProfile findOrCreateAgentProfile(TenantId tenantId, String name);

    AgentProfile findDefaultAgentProfile(TenantId tenantId);

    AgentProfileInfo findDefaultAgentProfileInfo(TenantId tenantId);

    boolean setDefaultAgentProfile(TenantId tenantId, AgentProfileId profileId);

    AgentProfile findProfileById(TenantId tenantId, AgentProfileId profileId);

    AgentProfile findProfileByName(TenantId tenantId, String name);

    AgentProfile findProfileByProvisionKey(String provisionKey);

    AgentProfileInfo findAgentProfileInfoById(TenantId tenantId, AgentProfileId profileId);

    PageData<AgentProfile> findAgentProfilesByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<AgentProfileInfo> findAgentProfileInfosByTenantId(TenantId tenantId, PageLink pageLink);

    void deleteProfile(TenantId tenantId, AgentProfileId profileId);

    void assignAppProfileToAgentProfile(TenantId tenantId, AgentProfileId agentProfileId, AgentAppProfileId profileId);

    void unassignAppProfileFromAgentProfile(TenantId tenantId, AgentProfileId agentProfileId, AgentAppProfileId profileId);

    void setAppProfileRelatesOnAutoDiscovery(TenantId tenantId, AgentProfileId agentProfileId, AgentAppProfileId appProfileId, boolean enabled);
}

