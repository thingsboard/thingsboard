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
package org.thingsboard.server.service.entitiy.widgets.type;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ResourceExportData;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.widget.WidgetExportData;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetsExportData;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.resource.TbImageService;
import org.thingsboard.server.service.resource.TbResourceService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@TbCoreComponent
@AllArgsConstructor
public class DefaultWidgetTypeService extends AbstractTbEntityService implements TbWidgetTypeService {


    private final WidgetTypeService widgetTypeService;
    private final ImageService imageService;
    private final TbImageService tbImageService;
    private final ResourceService resourceService;
    private final TbResourceService tbResourceService;

    @Override
    public WidgetTypeDetails save(WidgetTypeDetails entity, User user) throws Exception {
        return this.save(entity, false, user);
    }

    @Override
    public WidgetTypeDetails save(WidgetTypeDetails widgetTypeDetails, boolean updateExistingByFqn, User user) throws Exception {
        TenantId tenantId = widgetTypeDetails.getTenantId();
        if (widgetTypeDetails.getId() == null && StringUtils.isNotEmpty(widgetTypeDetails.getFqn()) && updateExistingByFqn) {
            WidgetType widgetType = widgetTypeService.findWidgetTypeByTenantIdAndFqn(tenantId, widgetTypeDetails.getFqn());
            if (widgetType != null) {
                widgetTypeDetails.setId(widgetType.getId());
            }
        }
        ActionType actionType = widgetTypeDetails.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        try {
            WidgetTypeDetails savedWidgetTypeDetails = checkNotNull(widgetTypeService.saveWidgetType(widgetTypeDetails));
            autoCommit(user, savedWidgetTypeDetails.getId());
            logEntityActionService.logEntityAction(tenantId, savedWidgetTypeDetails.getId(), savedWidgetTypeDetails,
                    null, actionType, user);
            return savedWidgetTypeDetails;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.WIDGET_TYPE), widgetTypeDetails, actionType, user, e);
            throw e;
        }
    }

    @Override
    public void delete(WidgetTypeDetails widgetTypeDetails, User user) {
        ActionType actionType = ActionType.DELETED;
        TenantId tenantId = widgetTypeDetails.getTenantId();
        try {
            widgetTypeService.deleteWidgetType(widgetTypeDetails.getTenantId(), widgetTypeDetails.getId());
            logEntityActionService.logEntityAction(tenantId, widgetTypeDetails.getId(), widgetTypeDetails, null, actionType, user);
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.WIDGET_TYPE), actionType, user, e, widgetTypeDetails.getId());
            throw e;
        }
    }

    @Override
    public WidgetExportData exportWidgetType(TenantId tenantId, WidgetTypeDetails widgetTypeDetails, SecurityUser user) throws ThingsboardException {
        List<ResourceExportData> resources = processWidgetTypesForExport(List.of(widgetTypeDetails), user);

        WidgetExportData exportData = new WidgetExportData();
        exportData.setWidgetTypeDetails(widgetTypeDetails);
        exportData.setResources(resources);
        return exportData;
    }

    @Override
    public WidgetsExportData exportBundleWidgetTypes(TenantId tenantId, WidgetsBundleId bundleId, SecurityUser user) throws ThingsboardException {
        List<WidgetTypeDetails> widgetTypes = widgetTypeService.findWidgetTypesDetailsByWidgetsBundleId(tenantId, bundleId);
        List<ResourceExportData> resources = processWidgetTypesForExport(widgetTypes, user);

        WidgetsExportData exportData = new WidgetsExportData();
        exportData.setWidgetTypesDetails(widgetTypes);
        exportData.setResources(resources);
        return exportData;
    }

    private List<ResourceExportData> processWidgetTypesForExport(List<WidgetTypeDetails> widgetTypes, SecurityUser user) throws ThingsboardException {
        Map<TbResourceId, TbResourceInfo> resources = new HashMap<>();
        for (WidgetTypeDetails widgetTypeDetails : widgetTypes) {
            for (TbResourceInfo imageInfo : imageService.inlineImages(widgetTypeDetails)) {
                resources.putIfAbsent(imageInfo.getId(), imageInfo);
            }
            for (TbResourceInfo resourceInfo : resourceService.processResourcesForExport(widgetTypeDetails)) {
                resources.putIfAbsent(resourceInfo.getId(), resourceInfo);
            }
        }
        for (TbResourceInfo resourceInfo : resources.values()) {
            accessControlService.checkPermission(user, Resource.TB_RESOURCE, Operation.READ, resourceInfo.getId(), resourceInfo);
        }

        return resources.values().stream()
                .map(resourceInfo -> {
                    if (resourceInfo.getResourceType() == ResourceType.IMAGE) {
                        ResourceExportData imageExportData = tbImageService.exportImage(resourceInfo);
                        imageExportData.setResourceKey(null); // so that the image is not updated by resource key on import
                        return imageExportData;
                    } else {
                        return tbResourceService.exportResource(resourceInfo);
                    }
                })
                .toList();
    }

    @Override
    public WidgetTypeDetails importWidgetType(WidgetExportData exportData, SecurityUser user) throws Exception {
        return importWidgetTypes(List.of(exportData.getWidgetTypeDetails()), exportData.getResources(), user).get(0);
    }

    @Override
    public List<WidgetTypeDetails> importWidgetTypes(WidgetsExportData exportData, SecurityUser user) throws Exception {
        return importWidgetTypes(exportData.getWidgetTypesDetails(), exportData.getResources(), user);
    }

    private List<WidgetTypeDetails> importWidgetTypes(List<WidgetTypeDetails> widgetTypesDetails, List<ResourceExportData> resources, SecurityUser user) throws Exception {
        Map<TbResourceId, TbResourceId> importedResources = new HashMap<>();
        for (ResourceExportData resourceExportData : resources) {
            if (resourceExportData.getType() == ResourceType.IMAGE) {
                tbImageService.importImage(resourceExportData, true, user);
            } else {
                TbResourceInfo importedResource = tbResourceService.importResource(resourceExportData, true, user);
                importedResources.put(resourceExportData.getId(), importedResource.getId());
            }
        }

        List<WidgetTypeDetails> result = new ArrayList<>();
        for (WidgetTypeDetails widgetTypeDetails : widgetTypesDetails) {
            resourceService.processResourcesForImport(widgetTypeDetails, importedResources);
            // FIXME: shouldn't just save it, have to be processed somehow
            widgetTypeDetails = save(widgetTypeDetails, user);
            result.add(widgetTypeDetails);
        }
        return result;
    }

}
