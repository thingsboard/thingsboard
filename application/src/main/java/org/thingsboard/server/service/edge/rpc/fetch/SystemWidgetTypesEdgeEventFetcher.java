// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.fetch;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.widget.DeprecatedFilter;
import org.thingsboard.server.common.data.widget.WidgetTypeFilter;
import org.thingsboard.server.common.data.widget.WidgetTypeInfo;
import org.thingsboard.server.dao.widget.WidgetTypeService;

@Slf4j
public class SystemWidgetTypesEdgeEventFetcher extends BaseWidgetTypesEdgeEventFetcher {

    public SystemWidgetTypesEdgeEventFetcher(WidgetTypeService widgetTypeService) {
        super(widgetTypeService);
    }

    @Override
    protected PageData<WidgetTypeInfo> findWidgetTypes(TenantId tenantId, PageLink pageLink) {
        return widgetTypeService.findSystemWidgetTypesByPageLink(
                WidgetTypeFilter.builder()
                        .tenantId(tenantId)
                        .fullSearch(false)
                        .deprecatedFilter(DeprecatedFilter.ALL)
                        .widgetTypes(null).build(),
                pageLink);
    }
}
