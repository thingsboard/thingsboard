// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.entitiy.agent;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import java.util.List;

@AllArgsConstructor
@TbCoreComponent
@Service
@Slf4j
public class DefaultTbAgentService extends AbstractTbEntityService implements TbAgentService {

    private final AgentService agentService;

    @Override
    public Agent save(Agent agent, User user) throws Exception {
        return save(agent, null, user);
    }

    @Transactional
    @Override
    public Agent save(Agent agent, List<EntityGroup> entityGroups, User user) throws Exception {
        ActionType actionType = agent.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = agent.getTenantId();

        try {
            Agent savedAgent = checkNotNull(agentService.saveAgent(agent));
            createOrUpdateGroupEntity(tenantId, savedAgent, entityGroups, actionType, user);
            return savedAgent;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.AGENT), agent, actionType, user, e);
            throw e;
        }
    }

    @Transactional
    @Override
    public void delete(Agent agent, User user) {
        ActionType actionType = ActionType.DELETED;
        TenantId tenantId = agent.getTenantId();
        AgentId agentId = agent.getId();
        try {
            agentService.deleteAgent(tenantId, agentId);
            logEntityActionService.logEntityAction(tenantId, agentId, agent, agent.getCustomerId(), actionType, user, agentId.toString());
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.AGENT), actionType, user, e, agentId.toString());
            throw e;
        }
    }

}
