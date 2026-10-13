// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.agent;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.config.AgentAppArgumentSource;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.owner.OwnerService;

@Service
@RequiredArgsConstructor
public class AgentAppArgumentSourceResolver {

    private final OwnerService ownerService;
    private final AgentAppRelationService agentAppRelationService;

    public EntityId resolveContextSource(TenantId tenantId, AgentApplication application, AgentAppArgumentSource sourceType) {
        return switch (sourceType) {
            case AGENT -> application.getAgentId();
            case OWNER -> ownerService.getOwner(tenantId, application.getAgentId());
            case RELATED_ENTITY -> resolveRelatedEntity(tenantId, application);
            case TENANT -> tenantId;
            default -> null;
        };
    }

    private EntityId resolveRelatedEntity(TenantId tenantId, AgentApplication application) {
        EntityId related = agentAppRelationService.findRelatedEntity(tenantId, application);
        return related != null ? related : agentAppRelationService.findRelatedEntityByConfig(tenantId, application);
    }

}
