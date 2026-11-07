// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.profile;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.security.permission.OwnersCacheService;

@Component
@RequiredArgsConstructor
public class EdgeProfileEligibility {

    private final OwnersCacheService ownersCacheService;

    public boolean isEligible(TenantId tenantId, EntityId entityOwnerId, EntityId edgeOwnerId, boolean customerBound) {
        if (entityOwnerId == null || EntityType.TENANT.equals(entityOwnerId.getEntityType())) {
            return true;
        }
        // an edge holds only its owner's customer and the ones above it, so a customer-bound entity of any other customer is rejected there
        if (customerBound) {
            return entityOwnerId.equals(edgeOwnerId);
        }
        if (edgeOwnerId == null || EntityType.TENANT.equals(edgeOwnerId.getEntityType())) {
            return true;
        }
        if (entityOwnerId.equals(edgeOwnerId)) {
            return true;
        }
        return ownersCacheService.fetchOwnersHierarchy(tenantId, entityOwnerId).contains(edgeOwnerId);
    }

}
