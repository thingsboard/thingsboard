// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.agent;

import org.thingsboard.server.common.data.agent.AgentInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.UUID;

public interface AgentInfoDao extends Dao<AgentInfo> {

    AgentInfo findAgentInfoById(TenantId tenantId, UUID agentId);

    List<AgentInfo> findAgentInfosByTenantIdAndIds(UUID tenantId, List<UUID> agentIds);

    PageData<AgentInfo> findAgentInfosByTenantId(UUID tenantId, PageLink pageLink);

    PageData<AgentInfo> findAgentInfosByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink);
}
