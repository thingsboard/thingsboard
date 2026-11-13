// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.action;

import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.dao.group.EntityGroupService;

import java.util.Optional;

/**
 * Walks up the ownership hierarchy to the tenant, the same order entityGroupInfosHierarchy returns.
 * <p>
 * Holds no cache of its own on purpose: {@link EntityGroupService#findEntityGroupByTypeAndName} and
 * {@code getOwner} are already cache-backed and evict on save, rename and delete. A node-local cache
 * would be the one layer nothing invalidates.
 */
public class EntityGroupResolver {

    // Every owner chain is meant to terminate at TENANT; a cyclic one would spin forever on the message thread.
    private static final int MAX_OWNER_HIERARCHY_DEPTH = 64;

    private final TbContext ctx;

    public EntityGroupResolver(TbContext ctx) {
        this.ctx = ctx;
    }

    public EntityGroupId resolve(EntityId startingOwnerId, EntityType groupType, String groupName) {
        return findInOwnerHierarchy(startingOwnerId, groupType, groupName)
                .orElseThrow(() -> new RuntimeException("No entity group found with type '" + groupType
                        + "' and name '" + groupName + "' for owner " + startingOwnerId + "!"));
    }

    public EntityGroupId resolveOrCreate(EntityId startingOwnerId, EntityType groupType, String groupName) {
        return findInOwnerHierarchy(startingOwnerId, groupType, groupName)
                .orElseGet(() -> createUnder(startingOwnerId, groupType, groupName));
    }

    private Optional<EntityGroupId> findInOwnerHierarchy(EntityId startingOwnerId, EntityType groupType, String groupName) {
        EntityGroupService entityGroupService = ctx.getPeContext().getEntityGroupService();
        EntityId ownerId = startingOwnerId;
        int depth = 0;
        while (ownerId != null) {
            if (depth++ >= MAX_OWNER_HIERARCHY_DEPTH) {
                throw new RuntimeException("Owner hierarchy for " + startingOwnerId
                        + " exceeded the maximum depth of " + MAX_OWNER_HIERARCHY_DEPTH + "!");
            }
            Optional<EntityGroup> entityGroup = entityGroupService
                    .findEntityGroupByTypeAndName(ctx.getTenantId(), ownerId, groupType, groupName);
            if (entityGroup.isPresent()) {
                return Optional.of(entityGroup.get().getId());
            }
            if (EntityType.TENANT.equals(ownerId.getEntityType())) {
                break;
            }
            ownerId = ctx.getPeContext().getOwner(ctx.getTenantId(), ownerId);
        }
        return Optional.empty();
    }

    private EntityGroupId createUnder(EntityId ownerId, EntityType groupType, String groupName) {
        EntityGroup newGroup = new EntityGroup();
        newGroup.setName(groupName);
        newGroup.setType(groupType);
        return ctx.getPeContext().getEntityGroupService()
                .saveEntityGroup(ctx.getTenantId(), ownerId, newGroup).getId();
    }

}
