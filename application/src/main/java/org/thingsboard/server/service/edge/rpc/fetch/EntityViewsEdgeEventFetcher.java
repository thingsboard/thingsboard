// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.fetch;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entityview.EntityViewService;

@AllArgsConstructor
@Slf4j
public class EntityViewsEdgeEventFetcher extends BasePageableEdgeEventFetcher<EntityView> {

    private final EntityViewService entityViewService;

    @Override
    PageData<EntityView> fetchEntities(TenantId tenantId, Edge edge, PageLink pageLink) {
        return entityViewService.findEntityViewsByTenantIdAndEdgeId(tenantId, edge.getId(), pageLink);
    }

    @Override
    EdgeEvent constructEdgeEvent(TenantId tenantId, Edge edge, EntityView entityView) {
        return EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.ENTITY_VIEW,
                EdgeEventActionType.ADDED, entityView.getId(), null);
    }

}
