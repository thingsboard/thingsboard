/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.service.sync.ie.importing.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.sync.ie.WidgetsBundleExportData;
import org.thingsboard.server.common.data.widget.BaseWidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetTypeInfo;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class WidgetsBundleImportService extends BaseEntityImportService<WidgetsBundleId, WidgetsBundle, WidgetsBundleExportData> {

    private final WidgetsBundleService widgetsBundleService;
    private final WidgetTypeService widgetTypeService;

    @Override
    protected void setOwner(TenantId tenantId, WidgetsBundle widgetsBundle, IdProvider idProvider) {
        widgetsBundle.setTenantId(tenantId);
    }

    @Override
    protected WidgetsBundle prepare(EntitiesImportCtx ctx, WidgetsBundle widgetsBundle, WidgetsBundle old, WidgetsBundleExportData exportData, IdProvider idProvider) {
        return widgetsBundle;
    }

    @Override
    protected WidgetsBundle saveOrUpdate(EntitiesImportCtx ctx, WidgetsBundle widgetsBundle, WidgetsBundleExportData exportData, IdProvider idProvider) {
        WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);
        if (widgetsBundle.getId() == null) {
            for (WidgetTypeDetails widget : exportData.getWidgets()) {
                widget.setId(null);
                widget.setTenantId(ctx.getTenantId());
                widget.setBundleAlias(savedWidgetsBundle.getAlias());
                widgetTypeService.saveWidgetType(widget);
            }
        } else {
            Map<String, WidgetTypeInfo> existingWidgets = widgetTypeService.findWidgetTypesInfosByTenantIdAndBundleAlias(ctx.getTenantId(), savedWidgetsBundle.getAlias()).stream()
                    .collect(Collectors.toMap(BaseWidgetType::getAlias, w -> w));
            for (WidgetTypeDetails widget : exportData.getWidgets()) {
                WidgetTypeInfo existingWidget;
                if ((existingWidget = existingWidgets.remove(widget.getAlias())) != null) {
                    widget.setId(existingWidget.getId());
                    widget.setCreatedTime(existingWidget.getCreatedTime());
                } else {
                    widget.setId(null);
                }
                widget.setTenantId(ctx.getTenantId());
                widget.setBundleAlias(savedWidgetsBundle.getAlias());
                widgetTypeService.saveWidgetType(widget);
            }
            existingWidgets.values().stream()
                    .map(BaseWidgetType::getId)
                    .forEach(widgetTypeId -> widgetTypeService.deleteWidgetType(ctx.getTenantId(), widgetTypeId));
        }
        return savedWidgetsBundle;
    }

    @Override
    protected boolean compare(EntitiesImportCtx ctx, WidgetsBundleExportData exportData, WidgetsBundle prepared, WidgetsBundle existing) {
        return true;
    }

    @Override
    protected void onEntitySaved(User user, WidgetsBundle savedWidgetsBundle, WidgetsBundle oldWidgetsBundle) throws ThingsboardException {
        entityNotificationService.notifySendMsgToEdgeService(user.getTenantId(), savedWidgetsBundle.getId(),
                oldWidgetsBundle == null ? EdgeEventActionType.ADDED : EdgeEventActionType.UPDATED);
    }

    @Override
    protected WidgetsBundle deepCopy(WidgetsBundle widgetsBundle) {
        return new WidgetsBundle(widgetsBundle);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.WIDGETS_BUNDLE;
    }

}
