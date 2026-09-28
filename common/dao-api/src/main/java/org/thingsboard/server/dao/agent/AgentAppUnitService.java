// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.agent;

import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.agent.AgentAppUnitFilter;
import org.thingsboard.server.common.data.agent.AgentAppUnitInfo;
import org.thingsboard.server.common.data.agent.AgentAppUnitType;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.id.AgentAppUnitId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;

public interface AgentAppUnitService extends EntityDaoService {

    AgentAppUnit saveAgentAppUnit(TenantId tenantId, AgentAppUnit agentAppUnit);
    AgentAppUnit saveAgentAppUnit(TenantId tenantId, AgentAppUnit agentAppUnit, AgentApplication agentApplication);
    AgentAppUnit findAgentAppUnitById(TenantId tenantId, AgentAppUnitId agentAppUnitId);
    AgentAppUnitInfo findAgentAppUnitInfoById(TenantId tenantId, AgentAppUnitId agentAppUnitId);
    AgentAppUnit findByAgentAndProjectAndIdentifier(TenantId tenantId, AgentId agentId, String projectName,
                                                   String identifier, AgentAppUnitType type);
    List<AgentAppUnit> findAgentAppUnitsByAgentAppId(TenantId tenantId, AgentApplicationId agentAppId);
    PageData<AgentAppUnit> findByFilter(AgentAppUnitFilter filter, PageLink pageLink);
    void deleteAgentAppUnit(TenantId tenantId, AgentAppUnitId agentAppUnitId);
    void deleteAgentAppUnit(TenantId tenantId, AgentAppUnitId agentAppUnitId, AgentApplication agentApplication);
    void deleteByAgentApplicationId(TenantId tenantId, AgentApplicationId agentAppId);

}
