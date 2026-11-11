// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.agent;

import org.thingsboard.server.common.data.agent.AgentProfile;
import org.thingsboard.server.common.data.agent.AgentProfileInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

import java.util.UUID;

public interface AgentProfileDao extends Dao<AgentProfile> {

    AgentProfileInfo findAgentProfileInfoById(UUID agentProfileId);

    PageData<AgentProfile> findByTenantId(UUID tenantId, PageLink pageLink);

    PageData<AgentProfileInfo> findAgentProfileInfosByTenantId(UUID tenantId, PageLink pageLink);

    Long countByTenantId(TenantId tenantId);

    AgentProfile findByProvisionKey(String provisionKey);

    AgentProfile findByTenantIdAndName(UUID tenantId, String name);

    AgentProfile findDefaultAgentProfile(TenantId tenantId);
}
