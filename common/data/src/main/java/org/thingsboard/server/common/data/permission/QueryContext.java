// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.permission;

import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.query.EntitiesByGroupNameFilter;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.common.data.query.EntityFilterType;
import org.thingsboard.server.common.data.query.EntityGroupFilter;
import org.thingsboard.server.common.data.query.EntityGroupListFilter;
import org.thingsboard.server.common.data.query.EntityGroupNameFilter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class QueryContext {

    @Getter
    private final TenantId tenantId;
    @Getter
    private final CustomerId customerId;

    private final EntityType entityType;

    @Getter
    private final boolean ignorePermissionCheck;

    private final MergedUserPermissions userPermissions;

    private final EntityFilter entityFilter;

    @Getter
    @Setter
    private final EntityId ownerId;

    @Getter
    @Setter
    private final EntityType entityGroupType;

    @Getter
    private final Map<UUID, UUID> relatedParentIdMap = new HashMap<>();

    public QueryContext(TenantId tenantId, CustomerId customerId, EntityType entityType, MergedUserPermissions userPermissions, EntityFilter entityFilter) {
        this(tenantId, customerId, entityType, userPermissions, entityFilter, null, null, false);
    }

    public QueryContext(TenantId tenantId, CustomerId customerId, EntityType entityType, MergedUserPermissions userPermissions, EntityFilter entityFilter, boolean ignorePermissionCheck) {
        this(tenantId, customerId, entityType, userPermissions, entityFilter, null, null, ignorePermissionCheck);
    }

    public QueryContext(TenantId tenantId, CustomerId customerId, EntityType entityType, MergedUserPermissions userPermissions, EntityFilter entityFilter, EntityId ownerId, boolean ignorePermissionCheck) {
        this(tenantId, customerId, entityType, userPermissions, entityFilter, ownerId, null, ignorePermissionCheck);
    }

    public QueryContext(TenantId tenantId, CustomerId customerId, EntityType entityType, MergedUserPermissions userPermissions, EntityFilter entityFilter, EntityType entityGroupType, boolean ignorePermissionCheck) {
        this(tenantId, customerId, entityType, userPermissions, entityFilter, null, entityGroupType, ignorePermissionCheck);
    }

    private QueryContext(TenantId tenantId, CustomerId customerId, EntityType entityType, MergedUserPermissions userPermissions, EntityFilter entityFilter, EntityId ownerId, EntityType entityGroupType, boolean ignorePermissionCheck) {
        this.tenantId = tenantId;
        this.customerId = customerId;
        this.entityType = entityType;
        this.userPermissions = ignorePermissionCheck ? new MergedUserPermissions(Collections.singletonMap(Resource.ALL, Collections.singleton(Operation.ALL)), Collections.emptyMap()) : userPermissions;
        this.entityFilter = entityFilter;
        this.ownerId = ownerId;
        this.entityGroupType = entityGroupType;
        this.ignorePermissionCheck = ignorePermissionCheck;
    }

    public boolean isTenantUser() {
        return customerId == null || customerId.isNullUid();
    }

    public boolean hasGeneric(Operation operation) {
        return userPermissions.hasGenericPermission(Resource.resourceFromEntityType(entityType), operation);
    }

    public MergedGroupTypePermissionInfo getMergedReadPermissionsByEntityType() {
        return userPermissions.getReadEntityPermissions().get(getResource());
    }

    public MergedGroupTypePermissionInfo getMergedReadGroupPermissionsByEntityType() {
        return userPermissions.getReadGroupPermissions().get(entityType);
    }

    public EntityType getEntityType() {
        EntityType entityType;
        if (entityFilter != null) {
            entityType = switch (entityFilter.getType()) {
                case ENTITY_GROUP_NAME -> ((EntityGroupNameFilter) entityFilter).getGroupType();
                case ENTITIES_BY_GROUP_NAME -> ((EntitiesByGroupNameFilter) entityFilter).getGroupType();
                case ENTITY_GROUP -> ((EntityGroupFilter) entityFilter).getGroupType();
                default -> this.entityType;
            };
        } else {
            entityType = this.entityType;
        }
        return entityType;
    }

    private Resource getResource() {
        if (entityFilter != null) {
            switch (entityFilter.getType()) {
                case ENTITY_GROUP_NAME:
                    return Resource.groupResourceFromGroupType(((EntityGroupNameFilter) entityFilter).getGroupType());
                case ENTITY_GROUP_LIST:
                    return Resource.groupResourceFromGroupType(((EntityGroupListFilter) entityFilter).getGroupType());
            }
        }
        if (entityGroupType != null) {
            return Resource.groupResourceFromGroupType(entityGroupType);
        } else {
            return Resource.resourceFromEntityType(entityType);
        }
    }

    public MergedGroupTypePermissionInfo getMergedReadAttrPermissionsByEntityType() {
        return userPermissions.getReadAttrPermissions().get(getResource());
    }

    public MergedGroupTypePermissionInfo getMergedReadTsPermissionsByEntityType() {
        return userPermissions.getReadTsPermissions().get(getResource());
    }

    public Map<Resource, MergedGroupTypePermissionInfo> getMergedReadEntityPermissionsMap() {
        return userPermissions.getReadEntityPermissions();
    }

    public Map<Resource, MergedGroupTypePermissionInfo> getMergedReadAttrPermissionsMap() {
        return userPermissions.getReadAttrPermissions();
    }

    public Map<Resource, MergedGroupTypePermissionInfo> getMergedReadTsPermissionsMap() {
        return userPermissions.getReadTsPermissions();
    }

    public boolean isEntityGroup() {
        return EntityType.ENTITY_GROUP.equals(entityType)
                || EntityFilterType.ENTITY_GROUP_NAME.equals(entityFilter.getType())
                || EntityFilterType.ENTITY_GROUP_LIST.equals(entityFilter.getType());
    }

}
