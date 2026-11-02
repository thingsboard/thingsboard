// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.profile;

import com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.service.security.permission.OwnersCacheService;

import java.util.HashSet;
import java.util.Set;

/**
 * Owner changes are the only writes that change which rows an edge may see without going through a profile.
 * A moved edge is re-applied right away; a moved customer re-applies the edges of the owners whose view changed as a job.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EdgeProfileOwnerChangeListener {

    private final EdgeProfileJobService edgeProfileJobService;
    private final OwnersCacheService ownersCacheService;

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void onOwnerChanged(ActionEntityEvent<?> event) {
        if (event.getActionType() != ActionType.CHANGE_OWNER || event.getEntityId() == null) {
            return;
        }
        TenantId tenantId = event.getTenantId();
        switch (event.getEntityId().getEntityType()) {
            case EDGE -> applyToMovedEdge(tenantId, new EdgeId(event.getEntityId().getId()));
            case CUSTOMER -> {
                CustomerId movedCustomerId = new CustomerId(event.getEntityId().getId());
                Set<CustomerId> owners = ownersWhoseViewChanged(tenantId, movedCustomerId, JacksonUtil.fromString(event.getBody(), EntityId.class));
                if (!owners.isEmpty()) {
                    edgeProfileJobService.submitOwnerChange(tenantId, movedCustomerId, owners);
                }
            }
            default -> {
            }
        }
    }

    private void applyToMovedEdge(TenantId tenantId, EdgeId edgeId) {
        try {
            edgeProfileJobService.submitEdgeOwnerChange(tenantId, edgeId);
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to submit edge profile re-apply after an owner change", tenantId, edgeId, e);
        }
    }

    // edges under the moved customer keep their view; the owners it left lose its rows and the ones it joined gain them
    private Set<CustomerId> ownersWhoseViewChanged(TenantId tenantId, EntityId movedId, EntityId formerOwnerId) {
        Set<EntityId> former = ownerChain(tenantId, formerOwnerId);
        Set<EntityId> current = ownerChain(tenantId, ownersCacheService.getOwner(tenantId, movedId));
        Set<CustomerId> changed = new HashSet<>();
        for (EntityId ownerId : Sets.symmetricDifference(former, current)) {
            if (EntityType.CUSTOMER.equals(ownerId.getEntityType())) {
                changed.add(new CustomerId(ownerId.getId()));
            }
        }
        return changed;
    }

    private Set<EntityId> ownerChain(TenantId tenantId, EntityId ownerId) {
        return ownerId == null ? Set.of() : ownersCacheService.fetchOwnersHierarchy(tenantId, ownerId);
    }

}
