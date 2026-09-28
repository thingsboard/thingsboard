// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.group;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableEntityDao;
import org.thingsboard.server.dao.TenantEntityDao;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface EntityGroupDao extends Dao<EntityGroup>, TenantEntityDao<EntityGroup>, ExportableEntityDao<EntityGroupId, EntityGroup> {

    PageData<EntityGroup> findEntityGroupsByType
            (UUID tenantId, UUID parentEntityId, EntityType parentEntityType, EntityType groupType, PageLink pageLink);

    PageData<EntityGroup> findEntityGroupsByType(UUID tenantId, EntityType groupType, PageLink pageLink);

    PageData<EntityGroup> findAllEntityGroupsByParentRelation(UUID tenantId, UUID parentEntityId, EntityType parentEntityType, PageLink pageLink);

    PageData<EntityGroup> findAllEntityGroups(UUID tenantId, UUID parentEntityId, EntityType parentEntityType, PageLink pageLink);

    Optional<EntityGroup> findEntityGroupByTypeAndName(UUID tenantId, UUID parentEntityId,
                                                       EntityType parentEntityType, EntityType groupType, String name);

    @Deprecated(since = "3.7.1", forRemoval = true)
    ListenableFuture<Optional<EntityGroup>> findEntityGroupByTypeAndNameAsync(UUID tenantId, UUID parentEntityId,
                                                                              EntityType parentEntityType, EntityType groupType, String name);

    ListenableFuture<PageData<EntityId>> findGroupEntityIds(EntityType entityType, UUID groupId, PageLink pageLink);

    PageData<EntityId> findGroupEntityIdsSync(EntityType entityType, UUID groupId, PageLink pageLink);

    PageData<EntityGroup> findEdgeEntityGroupsByType(UUID tenantId, UUID edgeId, String relationType, PageLink pageLink);

    PageData<EntityGroup> findEdgeEntityGroupsByOwnerIdAndType(UUID tenantId, UUID edgeId, UUID ownerId, String relationType, PageLink pageLink);

    Set<String> findUserGroupNamesByUserId(TenantId tenantId, UserId userId);

}
