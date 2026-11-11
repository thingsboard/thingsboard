// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.entityview;

import org.thingsboard.server.common.data.EntityViewInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

import java.util.UUID;

public interface EntityViewInfoDao extends Dao<EntityViewInfo> {

    PageData<EntityViewInfo> findEntityViewsByTenantId(UUID tenantId, PageLink pageLink);

    PageData<EntityViewInfo> findEntityViewsByTenantIdAndType(UUID tenantId, String type, PageLink pageLink);

    PageData<EntityViewInfo> findTenantEntityViewsByTenantId(UUID tenantId, PageLink pageLink);

    PageData<EntityViewInfo> findTenantEntityViewsByTenantIdAndType(UUID tenantId, String type, PageLink pageLink);

    PageData<EntityViewInfo> findEntityViewsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink);

    PageData<EntityViewInfo> findEntityViewsByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, PageLink pageLink);

    PageData<EntityViewInfo> findEntityViewsByTenantIdAndCustomerIdIncludingSubCustomers(UUID tenantId, UUID customerId, PageLink pageLink);

    PageData<EntityViewInfo> findEntityViewsByTenantIdAndCustomerIdAndTypeIncludingSubCustomers(UUID tenantId, UUID customerId, String type, PageLink pageLink);
}
