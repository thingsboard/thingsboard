// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.group;

import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EntityGroupInfoDao extends Dao<EntityGroupInfo> {

    EntityInfo findEntityGroupEntityInfoById(TenantId tenantId, UUID entityGroupId);

    PageData<EntityGroupInfo> findEntityGroupsByType(UUID tenantId, UUID parentEntityId, EntityType parentEntityType, EntityType groupType, PageLink pageLink);

    PageData<EntityInfo> findEntityGroupEntityInfosByType(UUID tenantId, UUID parentEntityId, EntityType parentEntityType, EntityType groupType, PageLink pageLink);

    PageData<EntityGroupInfo> findEntityGroupsByOwnerIdsAndType(UUID tenantId, List<UUID> ownerIds, EntityType groupType, PageLink pageLink);

    PageData<EntityInfo> findEntityGroupEntityInfosByOwnerIdsAndType(UUID tenantId, List<UUID> ownerIds, EntityType groupType, PageLink pageLink);

    PageData<EntityGroupInfo> findEntityGroupsByIds(UUID tenantId, List<UUID> entityGroupIds, PageLink pageLink);

    PageData<EntityInfo> findEntityGroupEntityInfosByIds(UUID tenantId, List<UUID> entityGroupIds, PageLink pageLink);

    PageData<EntityGroupInfo> findEntityGroupsByTypeOrIds(UUID tenantId, UUID parentEntityId, EntityType parentEntityType, EntityType groupType,
                                                          List<UUID> entityGroupIds, PageLink pageLink);

    PageData<EntityInfo> findEntityGroupEntityInfosByTypeOrIds(UUID tenantId, UUID parentEntityId, EntityType parentEntityType, EntityType groupType,
                                                               List<UUID> entityGroupIds, PageLink pageLink);

    PageData<EntityGroupInfo> findEdgeEntityGroupsByOwnerIdAndType(UUID tenantId, UUID edgeId, UUID ownerId, EntityType ownerType, String relationType, PageLink pageLink);

    Optional<EntityGroupInfo> findEntityGroupByTypeAndName(UUID tenantId, UUID parentEntityId,
                                                           EntityType parentEntityType, EntityType groupType, String name);

}
