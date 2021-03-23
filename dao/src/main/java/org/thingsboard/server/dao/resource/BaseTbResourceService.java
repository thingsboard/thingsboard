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
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.lwm2m.LwM2mInstance;
import org.thingsboard.server.common.data.lwm2m.LwM2mObject;
import org.thingsboard.server.common.data.lwm2m.LwM2mResource;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.lwm2m.LwM2mConstants.LWM2M_SEPARATOR_KEY;
import static org.thingsboard.server.common.data.lwm2m.LwM2mConstants.LWM2M_SEPARATOR_SEARCH_TEXT;
import static org.thingsboard.server.dao.device.DeviceServiceImpl.INCORRECT_TENANT_ID;
import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class BaseTbResourceService implements TbResourceService {

    public static final String INCORRECT_RESOURCE_ID = "Incorrect resourceId ";
    private final TbResourceDao resourceDao;
    private final TbResourceInfoDao resourceInfoDao;
    private final DDFFileParser ddfFileParser;

    public BaseTbResourceService(TbResourceDao resourceDao, TbResourceInfoDao resourceInfoDao) {
        this.resourceDao = resourceDao;
        this.resourceInfoDao = resourceInfoDao;
        this.ddfFileParser = new DDFFileParser(new DefaultDDFFileValidator());
    }

    @Override
    public TbResource saveResource(TbResource tbResource) throws InvalidDDFFileException, IOException {
        log.trace("Executing saveResource [{}]", tbResource);
        if (ResourceType.LWM2M_MODEL.equals(tbResource.getResourceType())) {
            List<ObjectModel> objectModels =
                    ddfFileParser.parseEx(new ByteArrayInputStream(Base64.getDecoder().decode(tbResource.getData())), tbResource.getSearchText());
            if (!objectModels.isEmpty()) {
                ObjectModel objectModel = objectModels.get(0);
                String resourceKey = objectModel.id + LWM2M_SEPARATOR_KEY + objectModel.getVersion();
                String name = objectModel.name;
                tbResource.setResourceKey(resourceKey);
                tbResource.setTitle(name);
                tbResource.setSearchText(resourceKey + LWM2M_SEPARATOR_SEARCH_TEXT + name);
            } else {
                throw new DataValidationException(String.format("Could not parse the XML of objectModel with name %s", tbResource.getSearchText()));
            }
        }
        validate(tbResource);
        return resourceDao.save(tbResource.getTenantId(), tbResource);
    }

    @Override
    public TbResource getResource(TenantId tenantId, ResourceType resourceType, String resourceKey) {
        log.trace("Executing getResource [{}] [{}] [{}]", tenantId, resourceType, resourceKey);
        validate(tenantId, resourceType, resourceKey);
        return resourceDao.getResource(tenantId, resourceType, resourceKey);
    }

    @Override
    public TbResource findResourceById(TenantId tenantId, TbResourceId resourceId) {
        log.trace("Executing findResourceById [{}] [{}]", tenantId, resourceId);
        Validator.validateId(resourceId, INCORRECT_RESOURCE_ID + resourceId);
        return resourceDao.findById(tenantId, resourceId.getId());
    }

    @Override
    public TbResourceInfo findResourceInfoById(TenantId tenantId, TbResourceId resourceId) {
        log.trace("Executing findResourceInfoById [{}] [{}]", tenantId, resourceId);
        Validator.validateId(resourceId, INCORRECT_RESOURCE_ID + resourceId);
        return resourceInfoDao.findById(tenantId, resourceId.getId());
    }

    @Override
    public void deleteResource(TenantId tenantId, TbResourceId resourceId) {
        log.trace("Executing deleteResource [{}] [{}]", tenantId, resourceId);
        Validator.validateId(resourceId, INCORRECT_RESOURCE_ID + resourceId);
        resourceDao.removeById(tenantId, resourceId.getId());
    }

    @Override
    public PageData<TbResourceInfo> findResourcesByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findByTenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return resourceInfoDao.findTbResourcesByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public List<LwM2mObject> findLwM2mObjectPage(TenantId tenantId, String sortProperty, String sortOrder, PageLink pageLink) {
        log.trace("Executing findByTenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        PageData<TbResource> resourcePageData = resourceDao.findResourcesByTenantIdAndResourceType(
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
        List<TbResource> resources = resourceDao.findResourcesByTenantIdAndResourceType(tenantId, ResourceType.LWM2M_MODEL,
                objectIds,
                null);
        return resources.stream()
                .map(this::toLwM2mObject)
                .sorted(getComparator(sortProperty, sortOrder))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteResourcesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteResourcesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantResourcesRemover.removeEntities(tenantId, tenantId);
    }

    protected void validate(TbResource resource) {
        if (resource == null) {
            throw new DataValidationException("Resource should be specified!");
        }
        if (resource.getData() == null) {
            throw new DataValidationException("Resource value should be specified!");
        }
        validate(resource.getTenantId(), resource.getResourceType(), resource.getResourceKey());
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

    private LwM2mObject toLwM2mObject(TbResource resource) {
        try {
            DDFFileParser ddfFileParser = new DDFFileParser(new DefaultDDFFileValidator());
            List<ObjectModel> objectModels =
                    ddfFileParser.parseEx(new ByteArrayInputStream(Base64.getDecoder().decode(resource.getData())), resource.getSearchText());
            if (objectModels.size() == 0) {
                return null;
            } else {
                ObjectModel obj = objectModels.get(0);
                LwM2mObject lwM2mObject = new LwM2mObject();
                lwM2mObject.setId(obj.id);
                lwM2mObject.setKeyId(resource.getResourceKey());
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
                instance.setResources(resources.toArray(LwM2mResource[]::new));
                lwM2mObject.setInstances(new LwM2mInstance[]{instance});
                return lwM2mObject;
            }
        } catch (IOException | InvalidDDFFileException e) {
            log.error("Could not parse the XML of objectModel with name [{}]", resource.getSearchText(), e);
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

    private PaginatedRemover<TenantId, TbResource> tenantResourcesRemover =
            new PaginatedRemover<>() {

                @Override
                protected PageData<TbResource> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
                    return resourceDao.findAllByTenantId(id, pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, TbResource entity) {
                    deleteResource(tenantId, new TbResourceId(entity.getUuidId()));
                }
            };
}
