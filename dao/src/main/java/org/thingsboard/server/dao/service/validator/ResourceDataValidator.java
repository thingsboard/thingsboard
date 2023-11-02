/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.dao.service.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.widget.BaseWidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.resource.TbResourceDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.widget.WidgetTypeDao;

import java.util.List;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.EntityType.TB_RESOURCE;

@Component
public class ResourceDataValidator extends DataValidator<TbResource> {

    @Autowired
    private TbResourceDao resourceDao;

    @Autowired
    private WidgetTypeDao widgetTypeDao;

    @Autowired
    private TenantService tenantService;

    @Autowired
    @Lazy
    private TbTenantProfileCache tenantProfileCache;

    @Override
    protected void validateCreate(TenantId tenantId, TbResource resource) {
        if (resource.getData() == null || resource.getData().length == 0) {
            throw new DataValidationException("Resource data should be specified");
        }
    }

    @Override
    protected TbResource validateUpdate(TenantId tenantId, TbResource resource) {
        if (resource.getData() != null && !resource.getResourceType().isUpdatable()) {
            throw new DataValidationException("This type of resource can't be updated");
        }
        return resource;
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, TbResource resource) {
        validateString("Resource title", resource.getTitle());
        if (resource.getTenantId() == null) {
            resource.setTenantId(TenantId.SYS_TENANT_ID);
        }
        if (!resource.getTenantId().isSysTenantId()) {
            if (!tenantService.tenantExists(resource.getTenantId())) {
                throw new DataValidationException("Resource is referencing to non-existent tenant!");
            }
        }
        if (resource.getResourceType() == null) {
            throw new DataValidationException("Resource type should be specified!");
        }
        if (!resource.getTenantId().isSysTenantId() && resource.getData() != null) {
            DefaultTenantProfileConfiguration profileConfiguration = tenantProfileCache.get(tenantId).getDefaultProfileConfiguration();
            long maxResourceSize = profileConfiguration.getMaxResourceSize();
            if (maxResourceSize > 0 && resource.getData().length > maxResourceSize) {
                throw new IllegalArgumentException("Resource exceeds the maximum size of " + maxResourceSize + " bytes");
            }
            long maxSumResourcesDataInBytes = profileConfiguration.getMaxResourcesInBytes();
            validateMaxSumDataSizePerTenant(tenantId, resourceDao, maxSumResourcesDataInBytes, resource.getData().length, TB_RESOURCE);
        }
        if (resource.getResourceType().getDefaultMediaType() != null) {
            resource.setMediaType(resource.getResourceType().getDefaultMediaType());
        } else {
            if (resource.getMediaType() == null) {
                throw new DataValidationException("Media type is required");
            } else {
                try {
                    MediaType.parseMediaType(resource.getMediaType());
                } catch (InvalidMediaTypeException e) {
                    throw new DataValidationException("Invalid media type", e);
                }
            }
        }
        if (StringUtils.isEmpty(resource.getFileName())) {
            throw new DataValidationException("Resource file name should be specified!");
        }
        if (StringUtils.isEmpty(resource.getResourceKey())) {
            throw new DataValidationException("Resource key should be specified!");
        }
    }

    @Override
    public void validateDelete(TenantId tenantId, EntityId resourceId) {
        List<WidgetTypeDetails> widgets = widgetTypeDao.findWidgetTypesInfosByTenantIdAndResourceId(tenantId.getId(),
                resourceId.getId());
        if (!widgets.isEmpty()) {
            List<String> widgetNames = widgets.stream().map(BaseWidgetType::getName).collect(Collectors.toList());
            throw new DataValidationException(String.format("Following widget types uses current resource: %s", widgetNames));
        }
    }

}
