// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.agent;

import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

public interface AgentAppRelationService {

    EntityId findRelatedEntityByConfig(TenantId tenantId, AgentApplication app);

    EntityId findRelatedEntity(TenantId tenantId, AgentApplication app);

    void createOrUpdateRelation(TenantId tenantId, AgentApplicationId appId, EntityId relatedEntityId);

    void deleteRelation(TenantId tenantId, AgentApplicationId appId);

    void validateRelatedEntityNotManaged(TenantId tenantId, AgentApplication app, EntityId relatedEntityId);

    /**
     * Rejects a related entity that does not belong to {@code tenantId}. Callers that reach the service without a
     * REST permission check (compose sync, auto-install) resolve the id from agent-reported content, so ownership
     * must be asserted here rather than relied upon from the controller.
     */
    void validateRelatedEntityInTenant(TenantId tenantId, EntityId relatedEntityId);
}
