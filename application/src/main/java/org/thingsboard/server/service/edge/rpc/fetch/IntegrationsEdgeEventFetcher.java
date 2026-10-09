// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.rpc.fetch;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.integration.IntegrationService;

@AllArgsConstructor
@Slf4j
public class IntegrationsEdgeEventFetcher extends BasePageableEdgeEventFetcher<Integration> {

    private final IntegrationService integrationService;

    @Override
    PageData<Integration> fetchEntities(TenantId tenantId, Edge edge, PageLink pageLink) {
        return integrationService.findIntegrationsByTenantIdAndEdgeId(tenantId, edge.getId(), pageLink);
    }

    @Override
    EdgeEvent constructEdgeEvent(TenantId tenantId, Edge edge, Integration integration) {
        return EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.INTEGRATION,
                EdgeEventActionType.ADDED, integration.getId(), null);
    }

}
