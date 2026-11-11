// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.entity.EntityDaoRegistry;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.queue.util.TbCoreComponent;

/**
 * Checks that entities referenced by edge messages belong to the edge's tenant.
 */
@Slf4j
@Component
@TbCoreComponent
@RequiredArgsConstructor
public class EdgeEntityTenantChecker {

    private final EntityService entityService;
    private final EntityDaoRegistry entityDaoRegistry;

    /**
     * Returns true if the entity exists and belongs to the tenant.
     * Uses the entity services, so cached and tenant-scoped lookups are used where available.
     */
    public boolean isTenantEntity(TenantId tenantId, EntityId entityId) {
        if (entityId == null) {
            return false;
        }
        try {
            return entityService.fetchEntity(tenantId, entityId)
                    .map(entity -> entity instanceof HasTenantId hasTenantId && tenantId.equals(hasTenantId.getTenantId()))
                    .orElse(false);
        } catch (IllegalArgumentException e) {
            log.warn("[{}] Unsupported entity type of {}", tenantId, entityId);
            return false;
        }
    }

    /**
     * Loads the entity regardless of its tenant, so that an id of another tenant's entity is detected.
     * Returns null if the entity doesn't exist, throws if it belongs to a different tenant.
     */
    @SuppressWarnings("unchecked")
    public <T> T findTenantEntity(TenantId tenantId, EntityId entityId) {
        Object entity = findEntity(tenantId, entityId);
        if (entity instanceof HasTenantId hasTenantId && !tenantId.equals(hasTenantId.getTenantId())) {
            throw new DataValidationException(entityId.getEntityType() + " " + entityId.getId() + " belongs to a different tenant!");
        }
        return (T) entity;
    }

    /**
     * Returns the tenant of the entity regardless of the given tenant, or null if the entity doesn't exist.
     */
    public TenantId findEntityTenantId(TenantId tenantId, EntityId entityId) {
        return findEntity(tenantId, entityId) instanceof HasTenantId hasTenantId ? hasTenantId.getTenantId() : null;
    }

    private Object findEntity(TenantId tenantId, EntityId entityId) {
        if (entityId == null) {
            return null;
        }
        try {
            return entityDaoRegistry.getDao(entityId.getEntityType()).findById(tenantId, entityId.getId());
        } catch (IllegalArgumentException e) {
            log.warn("[{}] Unsupported entity type of {}", tenantId, entityId);
            return null;
        }
    }

}
