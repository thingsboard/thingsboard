// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.ie.exporting.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;

import java.util.Collections;
import java.util.Set;

import static org.thingsboard.server.service.sync.ie.importing.impl.DashboardImportService.WIDGET_CONFIG_PROCESSED_FIELDS_PATTERN;

@Service
@TbCoreComponent
public class DashboardExportService extends BaseEntityExportService<DashboardId, Dashboard, EntityExportData<Dashboard>> {

    @Override
    protected void setRelatedEntities(EntitiesExportCtx<?> ctx, Dashboard dashboard, EntityExportData<Dashboard> exportData) {
        if (CollectionUtils.isNotEmpty(dashboard.getAssignedCustomers())) {
            dashboard.getAssignedCustomers().forEach(customerInfo -> {
                customerInfo.setCustomerId(getExternalIdOrElseInternal(ctx, customerInfo.getCustomerId()));
            });
        }
        for (JsonNode entityAlias : dashboard.getEntityAliasesConfig()) {
            replaceUuidsRecursively(ctx, entityAlias, Set.of("id"), null);
        }
        for (JsonNode widgetConfig : dashboard.getWidgetsConfig()) {
            replaceUuidsRecursively(ctx, JacksonUtil.getSafely(widgetConfig, "config", "actions"), Collections.emptySet(), WIDGET_CONFIG_PROCESSED_FIELDS_PATTERN);
        }
    }

    @Override
    public Set<EntityType> getSupportedEntityTypes() {
        return Set.of(EntityType.DASHBOARD);
    }

}
