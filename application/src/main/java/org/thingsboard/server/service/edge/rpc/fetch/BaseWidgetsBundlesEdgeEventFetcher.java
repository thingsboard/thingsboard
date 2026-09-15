// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.fetch;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.widget.WidgetsBundleService;

@Slf4j
@AllArgsConstructor
public abstract class BaseWidgetsBundlesEdgeEventFetcher extends BasePageableEdgeEventFetcher<WidgetsBundle> {

    protected final WidgetsBundleService widgetsBundleService;

    @Override
    PageData<WidgetsBundle> fetchEntities(TenantId tenantId, Edge edge, PageLink pageLink) {
        return findWidgetsBundles(tenantId, pageLink);
    }

    @Override
    EdgeEvent constructEdgeEvent(TenantId tenantId, Edge edge, WidgetsBundle widgetsBundle) {
        return EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.WIDGETS_BUNDLE,
                EdgeEventActionType.ADDED, widgetsBundle.getId(), null);
    }

    protected abstract PageData<WidgetsBundle> findWidgetsBundles(TenantId tenantId, PageLink pageLink);

}
