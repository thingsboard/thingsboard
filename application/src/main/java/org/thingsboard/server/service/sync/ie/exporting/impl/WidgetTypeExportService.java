// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.ie.exporting.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.sync.ie.WidgetTypeExportData;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;

import java.util.Set;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class WidgetTypeExportService extends BaseEntityExportService<WidgetTypeId, WidgetTypeDetails, WidgetTypeExportData> {

    @Override
    protected void setRelatedEntities(EntitiesExportCtx<?> ctx, WidgetTypeDetails widgetTypeDetails, WidgetTypeExportData exportData) {
        if (widgetTypeDetails.getTenantId() == null || widgetTypeDetails.getTenantId().isNullUid()) {
            throw new IllegalArgumentException("Export of system Widget Type is not allowed");
        }
    }

    @Override
    protected WidgetTypeExportData newExportData() {
        return new WidgetTypeExportData();
    }

    @Override
    public Set<EntityType> getSupportedEntityTypes() {
        return Set.of(EntityType.WIDGET_TYPE);
    }

}