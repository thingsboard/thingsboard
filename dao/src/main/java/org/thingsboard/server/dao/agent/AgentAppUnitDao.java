// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.agent;

import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.agent.AgentAppUnitFilter;
import org.thingsboard.server.common.data.agent.AgentAppUnitInfo;
import org.thingsboard.server.common.data.agent.AgentAppUnitType;
import org.thingsboard.server.common.data.id.AgentAppUnitId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.UUID;

public interface AgentAppUnitDao extends Dao<AgentAppUnit> {

    List<AgentAppUnit> findByAgentApplicationId(TenantId tenantId, UUID agentApplicationId);

    AgentAppUnit findByAgentAndProjectAndIdentifier(TenantId tenantId, AgentId agentId, String projectName,
                                                   String identifier, AgentAppUnitType type);

    AgentAppUnitInfo findAgentAppUnitInfoById(TenantId tenantId, AgentAppUnitId agentAppUnitId);

    PageData<AgentAppUnit> findByFilter(AgentAppUnitFilter filter, PageLink pageLink);

    void removeByAgentApplicationId(TenantId tenantId, UUID agentApplicationId);

}
