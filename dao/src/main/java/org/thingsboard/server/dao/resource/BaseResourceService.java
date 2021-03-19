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
import org.eclipse.leshan.core.model.DDFFileParser;
import org.eclipse.leshan.core.model.DefaultDDFFileValidator;
import org.eclipse.leshan.core.model.InvalidDDFFileException;
import org.eclipse.leshan.core.model.ObjectModel;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Resource;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.lwm2m.LwM2mInstance;
import org.thingsboard.server.common.data.lwm2m.LwM2mObject;
import org.thingsboard.server.common.data.lwm2m.LwM2mResource;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.exception.DataValidationException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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
    public List<Resource> findAllByTenantIdAndResourceType(TenantId tenantId, ResourceType resourceType) {
        log.trace("Executing findByTenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return resourceDao.findAllByTenantIdAndResourceType(tenantId, resourceType);
    }

    @Override
    public List<LwM2mObject> findLwM2mObjectPage(TenantId tenantId, String sortProperty, String sortOrder, PageLink pageLink) {
        log.trace("Executing findByTenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        PageData<Resource> resourcePageData = resourceDao.findResourcesByTenantIdAndResourceType(
                                                                        tenantId,
                                                                        ResourceType.LWM2M_MODEL, pageLink);
       return resourcePageData.getData().stream()
               .map(this::toLwM2mObject)
               .sorted(getComparator(sortProperty, sortOrder))
               .collect(Collectors.toList());
    }

    @Override
    public List<LwM2mObject> findLwM2mObject(TenantId tenantId, String sortOrder,
                                             String sortProperty,
                                             String[] objectIds) {
        log.trace("Executing findByTenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        List<Resource> resources = resourceDao.findResourcesByTenantIdAndResourceType(tenantId, ResourceType.LWM2M_MODEL,
                                                                                        objectIds,
                                                                                        null);
        return resources.stream()
                .map(this::toLwM2mObject)
                .sorted(getComparator(sortProperty, sortOrder))
                .collect(Collectors.toList());
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
        if (resource.getResourceType().equals(ResourceType.LWM2M_MODEL) && this.toLwM2mObject(resource) == null) {
            throw new DataValidationException(String.format("Could not parse the XML of objectModel with name %s", resource.getTextSearch()));
        }
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

    private LwM2mObject toLwM2mObject(Resource resource) {
        try {
            DDFFileParser ddfFileParser = new DDFFileParser(new DefaultDDFFileValidator());
            List<ObjectModel> objectModels =
                    ddfFileParser.parseEx(new ByteArrayInputStream(Base64.getDecoder().decode(resource.getValue())), resource.getTextSearch());
            if (objectModels.size() == 0) {
                return null;
            } else {
                ObjectModel obj = objectModels.get(0);
                LwM2mObject lwM2mObject = new LwM2mObject();
                lwM2mObject.setId(obj.id);
                lwM2mObject.setKeyId(resource.getResourceId());
                lwM2mObject.setName(obj.name);
                lwM2mObject.setMultiple(obj.multiple);
                lwM2mObject.setMandatory(obj.mandatory);
                LwM2mInstance instance = new LwM2mInstance();
                instance.setId(0);
                List<LwM2mResource> resources = new ArrayList<>();
                obj.resources.forEach((k, v) -> {
                    if (!v.operations.isExecutable()) {
                        LwM2mResource lwM2mResource = new LwM2mResource(k, v.name, false, false, false);
                        resources.add(lwM2mResource);
                    }
                });
                instance.setResources(resources.stream().toArray(LwM2mResource[]::new));
                lwM2mObject.setInstances(new LwM2mInstance[]{instance});
                return lwM2mObject;
            }
        } catch (IOException | InvalidDDFFileException e) {
            log.error("Could not parse the XML of objectModel with name [{}]", resource.getTextSearch(), e);
            return null;
        }
    }

    private Comparator<? super LwM2mObject> getComparator(String sortProperty, String sortOrder) {
        Comparator<LwM2mObject> comparator;
        if ("name".equals(sortProperty)) {
            comparator = Comparator.comparing(LwM2mObject::getName);
        } else {
            comparator = Comparator.comparingLong(LwM2mObject::getId);
        }
        return "DESC".equals(sortOrder) ? comparator.reversed() : comparator;
    }

}
