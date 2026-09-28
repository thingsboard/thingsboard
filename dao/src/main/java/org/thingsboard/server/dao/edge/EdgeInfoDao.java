// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.edge;

import org.thingsboard.server.common.data.edge.EdgeInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

import java.util.UUID;

public interface EdgeInfoDao extends Dao<EdgeInfo> {

    PageData<EdgeInfo> findEdgesByTenantId(UUID tenantId, PageLink pageLink);

    PageData<EdgeInfo> findEdgesByTenantIdAndType(UUID tenantId, String type, PageLink pageLink);

    PageData<EdgeInfo> findTenantEdgesByTenantId(UUID tenantId, PageLink pageLink);

    PageData<EdgeInfo> findTenantEdgesByTenantIdAndType(UUID tenantId, String type, PageLink pageLink);

    PageData<EdgeInfo> findEdgesByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink);

    PageData<EdgeInfo> findEdgesByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, PageLink pageLink);

    PageData<EdgeInfo> findEdgesByTenantIdAndCustomerIdIncludingSubCustomers(UUID tenantId, UUID customerId, PageLink pageLink);

    PageData<EdgeInfo> findEdgesByTenantIdAndCustomerIdAndTypeIncludingSubCustomers(UUID tenantId, UUID customerId, String type, PageLink pageLink);
}
