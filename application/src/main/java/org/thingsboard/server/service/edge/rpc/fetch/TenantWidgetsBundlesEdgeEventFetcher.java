// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.fetch;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.widget.WidgetsBundleService;

@Slf4j
public class TenantWidgetsBundlesEdgeEventFetcher extends BaseWidgetsBundlesEdgeEventFetcher {

    public TenantWidgetsBundlesEdgeEventFetcher(WidgetsBundleService widgetsBundleService) {
        super(widgetsBundleService);
    }

    @Override
    protected PageData<WidgetsBundle> findWidgetsBundles(TenantId tenantId, PageLink pageLink) {
        return widgetsBundleService.findTenantWidgetsBundlesByTenantId(tenantId, pageLink);
    }
}
