/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.dao.resource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Resource;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.exception.DataValidationException;

import java.util.List;

import static org.thingsboard.server.dao.device.DeviceServiceImpl.INCORRECT_TENANT_ID;
import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class BaseResourceService implements ResourceService {

    private final ResourceDao resourceDao;

    public BaseResourceService(ResourceDao resourceDao) {
        this.resourceDao = resourceDao;
    }

    @Override
    public Resource saveResource(Resource resource) {
        log.trace("Executing saveResource [{}]", resource);
        validate(resource);
        return resourceDao.saveResource(resource);
    }

    @Override
    public Resource getResource(TenantId tenantId, ResourceType resourceType, String resourceId) {
        log.trace("Executing getResource [{}] [{}] [{}]", tenantId, resourceType, resourceId);
        validate(tenantId, resourceType, resourceId);
        return resourceDao.getResource(tenantId, resourceType, resourceId);
    }

    @Override
    public void deleteResource(TenantId tenantId, ResourceType resourceType, String resourceId) {
        log.trace("Executing deleteResource [{}] [{}] [{}]", tenantId, resourceType, resourceId);
        validate(tenantId, resourceType, resourceId);
        resourceDao.deleteResource(tenantId, resourceType, resourceId);
    }

    @Override
    public PageData<Resource> findResourcesByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findByTenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return resourceDao.findAllByTenantId(tenantId, pageLink);
    }


    @Override
    public List<Resource> findResourcesByTenantIdResourceType(TenantId tenantId, ResourceType resourceType) {
        log.trace("Executing findByTenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return resourceDao.findAllByTenantIdResourceType(tenantId, resourceType);
    }

    @Override
    public void deleteResourcesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteDevicesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        resourceDao.removeAllByTenantId(tenantId);
    }

    protected void validate(Resource resource) {
        if (resource == null) {
            throw new DataValidationException("Resource should be specified!");
        }

        if (resource.getValue() == null) {
            throw new DataValidationException("Resource value should be specified!");
        }
        validate(resource.getTenantId(), resource.getResourceType(), resource.getResourceId());
    }

    protected void validate(TenantId tenantId, ResourceType resourceType, String resourceId) {
        if (resourceType == null) {
            throw new DataValidationException("Resource type should be specified!");
        }
        if (resourceId == null) {
            throw new DataValidationException("Resource id should be specified!");
        }
        validateId(tenantId, "Incorrect tenantId ");
    }

}
