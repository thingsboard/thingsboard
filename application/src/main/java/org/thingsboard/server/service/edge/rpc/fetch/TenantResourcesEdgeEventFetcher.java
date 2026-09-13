// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.fetch;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.resource.ResourceService;

@AllArgsConstructor
@Slf4j
public class TenantResourcesEdgeEventFetcher extends BasePageableEdgeEventFetcher<TbResource> {

    private final ResourceService resourceService;

    @Override
    PageData<TbResource> fetchEntities(TenantId tenantId, Edge edge, PageLink pageLink) {
        return resourceService.findAllTenantResources(tenantId, pageLink);
    }

    @Override
    EdgeEvent constructEdgeEvent(TenantId tenantId, Edge edge, TbResource tbResource) {
        return EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.TB_RESOURCE,
                EdgeEventActionType.ADDED, tbResource.getId(), null);
    }

}
