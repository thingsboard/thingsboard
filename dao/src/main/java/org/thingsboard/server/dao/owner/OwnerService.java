// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.owner;

import jakarta.annotation.Nullable;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.HasOwnerId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.List;
import java.util.Set;

public interface OwnerService {

    Set<EntityId> fetchOwnersHierarchy(TenantId tenantId, EntityId ownerId);

    @Nullable
    EntityId getOwner(TenantId tenantId, EntityId entityId);

    Set<EntityId> getOwners(TenantId tenantId, EntityId entityId);

    Set<EntityId> getOwners(TenantId tenantId, EntityId entityId, HasOwnerId hasOwnerId);

    Set<EntityId> getOwners(TenantId tenantId, EntityGroupId entityGroupId);

    void clearOwners(EntityId entityId);

    PageData<EntityInfo> findTenantOwnerByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<EntityInfo> findCustomerOwnersByTenantIdIncludingTenant(TenantId tenantId, PageLink pageLink);

    PageData<EntityInfo> findCustomerOwnersByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<EntityInfo> findCustomerOwnersByIdsAndTenantId(TenantId tenantId, List<CustomerId> ownerIds, PageLink pageLink);


}
