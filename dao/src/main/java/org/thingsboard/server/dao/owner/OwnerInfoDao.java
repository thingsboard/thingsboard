// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.owner;

import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.UUID;

public interface OwnerInfoDao extends Dao<EntityInfo> {

    PageData<EntityInfo> findTenantOwnerByTenantId(UUID tenantId, PageLink pageLink);

    PageData<EntityInfo> findCustomerOwnersByTenantIdIncludingTenant(UUID tenantId, PageLink pageLink);

    PageData<EntityInfo> findCustomerOwnersByTenantId(UUID tenantId, PageLink pageLink);

    PageData<EntityInfo> findCustomerOwnersByIdsAndTenantId(UUID tenantId, List<UUID> ownerIds, PageLink pageLink);

}
