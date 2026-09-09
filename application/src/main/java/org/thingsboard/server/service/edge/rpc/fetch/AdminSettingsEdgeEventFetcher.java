// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.fetch;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.settings.AdminSettingsService;

@AllArgsConstructor
@Slf4j
public class AdminSettingsEdgeEventFetcher extends BasePageableEdgeEventFetcher<AdminSettings> {

    private final AdminSettingsService adminSettingsService;
    private final TenantId fetcherTenantId;

    @Override
    PageData<AdminSettings> fetchEntities(TenantId tenantId, Edge edge, PageLink pageLink) {
        return adminSettingsService.findAllByTenantId(fetcherTenantId, pageLink);
    }

    @Override
    EdgeEvent constructEdgeEvent(TenantId tenantId, Edge edge, AdminSettings adminSettings) {
        return EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.ADMIN_SETTINGS,
                EdgeEventActionType.UPDATED, adminSettings.getId(), null);
    }
}
