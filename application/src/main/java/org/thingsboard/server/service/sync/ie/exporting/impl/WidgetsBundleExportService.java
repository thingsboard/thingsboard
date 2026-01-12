/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.sync.ie.exporting.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.sync.ie.WidgetsBundleExportData;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;

import java.util.List;
import java.util.Set;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class WidgetsBundleExportService extends BaseEntityExportService<WidgetsBundleId, WidgetsBundle, WidgetsBundleExportData> {

    private final WidgetTypeService widgetTypeService;

    @Override
    protected void setRelatedEntities(EntitiesExportCtx<?> ctx, WidgetsBundle widgetsBundle, WidgetsBundleExportData exportData) {
        if (widgetsBundle.getTenantId() == null || widgetsBundle.getTenantId().isNullUid()) {
            throw new IllegalArgumentException("Export of system Widget Bundles is not allowed");
        }

        List<String> fqns = widgetTypeService.findWidgetFqnsByWidgetsBundleId(ctx.getTenantId(), widgetsBundle.getId());
        exportData.setFqns(fqns);
    }

    @Override
    protected WidgetsBundleExportData newExportData() {
        return new WidgetsBundleExportData();
    }

    @Override
    public Set<EntityType> getSupportedEntityTypes() {
        return Set.of(EntityType.WIDGETS_BUNDLE);
    }

}
