// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.fetch;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.tenant.TenantService;

import java.util.List;

@AllArgsConstructor
@Slf4j
public class TenantEdgeEventFetcher extends BasePageableEdgeEventFetcher<Tenant> {

    private final TenantService tenantService;

    @Override
    PageData<Tenant> fetchEntities(TenantId tenantId, Edge edge, PageLink pageLink) {
        Tenant tenant = tenantService.findTenantById(tenantId);
        // returns PageData object to be in sync with other fetchers
        return new PageData<>(List.of(tenant), 1, 1, false);
    }

    @Override
    EdgeEvent constructEdgeEvent(TenantId tenantId, Edge edge, Tenant entity) {
        return EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.TENANT,
                EdgeEventActionType.UPDATED, entity.getId(), null);
    }

}
