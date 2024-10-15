/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.service.entitiy.widgets.bundle;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
@TbCoreComponent
@AllArgsConstructor
@Slf4j
public class DefaultWidgetsBundleService extends AbstractTbEntityService implements TbWidgetsBundleService {

    private final WidgetsBundleService widgetsBundleService;
    private final WidgetTypeService widgetTypeService;

    @Override
    public WidgetsBundle save(WidgetsBundle widgetsBundle, User user) throws Exception {
        ActionType actionType = widgetsBundle.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = widgetsBundle.getTenantId();
        try {
            WidgetsBundle savedWidgetsBundle = checkNotNull(widgetsBundleService.saveWidgetsBundle(widgetsBundle));
            autoCommit(user, savedWidgetsBundle.getId());
            logEntityActionService.logEntityAction(tenantId, savedWidgetsBundle.getId(), savedWidgetsBundle,
                    null, actionType, user);
            return savedWidgetsBundle;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.WIDGETS_BUNDLE), widgetsBundle, actionType, user, e);
            throw e;
        }
    }

    @Override
    public void delete(WidgetsBundle widgetsBundle, User user) {
        ActionType actionType = ActionType.DELETED;
        TenantId tenantId = widgetsBundle.getTenantId();
        try {
            widgetsBundleService.deleteWidgetsBundle(widgetsBundle.getTenantId(), widgetsBundle.getId());
            logEntityActionService.logEntityAction(tenantId, widgetsBundle.getId(), widgetsBundle, null, actionType, user);
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.WIDGETS_BUNDLE), actionType, user, e, widgetsBundle.getId());
            throw e;
        }
    }

    @Override
    public void updateWidgetsBundleWidgetTypes(WidgetsBundleId widgetsBundleId, List<WidgetTypeId> widgetTypeIds, User user) throws Exception {
        widgetTypeService.updateWidgetsBundleWidgetTypes(user.getTenantId(), widgetsBundleId, widgetTypeIds);
        autoCommit(user, widgetsBundleId);
    }

    @Override
    public void updateWidgetsBundleWidgetFqns(WidgetsBundleId widgetsBundleId, List<String> widgetFqns, User user) throws Exception {
        widgetTypeService.updateWidgetsBundleWidgetFqns(user.getTenantId(), widgetsBundleId, widgetFqns);
        autoCommit(user, widgetsBundleId);
    }

    @Transactional
    @Override
    public void updateWidgets(TenantId tenantId, Stream<JsonNode> bundles, Stream<JsonNode> widgets) {
        widgets.forEach(widgetTypeJson -> {
            try {
                WidgetTypeDetails widgetTypeDetails = JacksonUtil.treeToValue(widgetTypeJson, WidgetTypeDetails.class);
                WidgetType existingWidget = widgetTypeService.findWidgetTypeByTenantIdAndFqn(tenantId, widgetTypeDetails.getFqn());
                if (existingWidget != null) {
                    widgetTypeDetails.setId(existingWidget.getId());
                    widgetTypeDetails.setCreatedTime(existingWidget.getCreatedTime());
                }
                widgetTypeDetails.setTenantId(tenantId);
                widgetTypeService.saveWidgetType(widgetTypeDetails);
                log.debug("{} widget type {}", existingWidget == null ? "Created" : "Updated", widgetTypeDetails.getFqn());
            } catch (Exception e) {
                throw new RuntimeException("Unable to load widget type from json: " + widgetTypeJson, e);
            }
        });

        bundles.forEach(widgetsBundleDescriptorJson -> {
            if (widgetsBundleDescriptorJson == null || !widgetsBundleDescriptorJson.has("widgetsBundle")) {
                throw new RuntimeException("Invalid widgets bundle json: [" + widgetsBundleDescriptorJson + "]");
            }

            JsonNode widgetsBundleJson = widgetsBundleDescriptorJson.get("widgetsBundle");
            WidgetsBundle widgetsBundle = JacksonUtil.treeToValue(widgetsBundleJson, WidgetsBundle.class);
            WidgetsBundle existingWidgetsBundle = widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(tenantId, widgetsBundle.getAlias());
            if (existingWidgetsBundle != null) {
                widgetsBundle.setId(existingWidgetsBundle.getId());
                widgetsBundle.setCreatedTime(existingWidgetsBundle.getCreatedTime());
            }
            widgetsBundle.setTenantId(tenantId);
            widgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);
            log.debug("{} widgets bundle {}", existingWidgetsBundle == null ? "Created" : "Updated", widgetsBundle.getAlias());

            List<String> widgetTypeFqns = new ArrayList<>();
            if (widgetsBundleDescriptorJson.has("widgetTypes")) {
                JsonNode widgetTypesArrayJson = widgetsBundleDescriptorJson.get("widgetTypes");
                widgetTypesArrayJson.forEach(widgetTypeJson -> {
                    try {
                        WidgetTypeDetails widgetTypeDetails = JacksonUtil.treeToValue(widgetTypeJson, WidgetTypeDetails.class);
                        WidgetType existingWidget = widgetTypeService.findWidgetTypeByTenantIdAndFqn(tenantId, widgetTypeDetails.getFqn());
                        if (existingWidget != null) {
                            widgetTypeDetails.setId(existingWidget.getId());
                            widgetTypeDetails.setCreatedTime(existingWidget.getCreatedTime());
                        }
                        widgetTypeDetails.setTenantId(tenantId);
                        widgetTypeDetails = widgetTypeService.saveWidgetType(widgetTypeDetails);
                        widgetTypeFqns.add(widgetTypeDetails.getFqn());
                    } catch (Exception e) {
                        throw new RuntimeException("Unable to load widget type from json: " + widgetsBundleDescriptorJson, e);
                    }
                });
            }
            if (widgetsBundleDescriptorJson.has("widgetTypeFqns")) {
                JsonNode widgetFqnsArrayJson = widgetsBundleDescriptorJson.get("widgetTypeFqns");
                widgetFqnsArrayJson.forEach(fqnJson -> {
                    widgetTypeFqns.add(fqnJson.asText());
                });
            }
            widgetTypeService.updateWidgetsBundleWidgetFqns(tenantId, widgetsBundle.getId(), widgetTypeFqns);
        });
    }

}
