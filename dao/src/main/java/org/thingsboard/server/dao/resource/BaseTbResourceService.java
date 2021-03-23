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
import org.apache.commons.lang3.StringUtils;
import org.eclipse.leshan.core.model.DDFFileParser;
import org.eclipse.leshan.core.model.DefaultDDFFileValidator;
import org.eclipse.leshan.core.model.InvalidDDFFileException;
import org.eclipse.leshan.core.model.ObjectModel;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.lwm2m.LwM2mInstance;
import org.thingsboard.server.common.data.lwm2m.LwM2mObject;
import org.thingsboard.server.common.data.lwm2m.LwM2mResource;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.tenant.TenantDao;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.device.DeviceServiceImpl.INCORRECT_TENANT_ID;
import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class BaseTbResourceService implements TbResourceService {

    public static final String INCORRECT_RESOURCE_ID = "Incorrect resourceId ";
    private final TbResourceDao resourceDao;
    private final TbResourceInfoDao resourceInfoDao;
    private final TenantDao tenantDao;
    private final DDFFileParser ddfFileParser;

    public BaseTbResourceService(TbResourceDao resourceDao, TbResourceInfoDao resourceInfoDao, TenantDao tenantDao) {
        this.resourceDao = resourceDao;
        this.resourceInfoDao = resourceInfoDao;
        this.tenantDao = tenantDao;
        this.ddfFileParser = new DDFFileParser(new DefaultDDFFileValidator());
    }

    @Override
    public TbResource saveResource(TbResource resource) throws InvalidDDFFileException, IOException {
        log.trace("Executing saveResource [{}]", resource);
        if (StringUtils.isEmpty(resource.getData())) {
            throw new DataValidationException("Resource data should be specified!");
        }
        if (ResourceType.LWM2M_MODEL.equals(resource.getResourceType())) {
            List<ObjectModel> objectModels =
                    ddfFileParser.parseEx(new ByteArrayInputStream(Base64.getDecoder().decode(resource.getData())), resource.getSearchText());
            if (!objectModels.isEmpty()) {
                ObjectModel objectModel = objectModels.get(0);

                String resourceKey = objectModel.id + "_" + objectModel.getVersion();
                String name = objectModel.name;
                resource.setResourceKey(resourceKey);
                resource.setTitle(name);
                resource.setSearchText(resourceKey + ":" + name);
            }
        } else {
            resource.setResourceKey(resource.getFileName());
        }

        resourceValidator.validate(resource, TbResourceInfo::getTenantId);

        try {
            return resourceDao.save(resource.getTenantId(), resource);
        } catch (Exception t) {
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("resource_unq_key")) {
                String field = ResourceType.LWM2M_MODEL.equals(resource.getResourceType()) ? "resourceKey" : "fileName";
                throw new DataValidationException("Resource with such " + field + " already exists!");
            } else {
                throw t;
            }
        }

    }

    @Override
    public TbResource getResource(TenantId tenantId, ResourceType resourceType, String resourceKey) {
        log.trace("Executing getResource [{}] [{}] [{}]", tenantId, resourceType, resourceKey);
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
    public PageData<TbResourceInfo> findAllTenantResourcesByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findAllTenantResourcesByTenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return resourceInfoDao.findAllTenantResourcesByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<TbResourceInfo> findTenantResourcesByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findTenantResourcesByTenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return resourceInfoDao.findTenantResourcesByTenantId(tenantId.getId(), pageLink);
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

    private DataValidator<TbResource> resourceValidator = new DataValidator<>() {

        @Override
        protected void validateDataImpl(TenantId tenantId, TbResource resource) {
            if (StringUtils.isEmpty(resource.getTitle())) {
                throw new DataValidationException("Resource title should be specified!");
            }
            if (resource.getResourceType() == null) {
                throw new DataValidationException("Resource type should be specified!");
            }
            if (StringUtils.isEmpty(resource.getFileName())) {
                throw new DataValidationException("Resource file name should be specified!");
            }
            if (StringUtils.isEmpty(resource.getResourceKey())) {
                throw new DataValidationException("Resource key should be specified!");
            }
            if (resource.getTenantId() == null) {
                resource.setTenantId(new TenantId(ModelConstants.NULL_UUID));
            }
            if (!resource.getTenantId().getId().equals(ModelConstants.NULL_UUID)) {
                Tenant tenant = tenantDao.findById(tenantId, resource.getTenantId().getId());
                if (tenant == null) {
                    throw new DataValidationException("Resource is referencing to non-existent tenant!");
                }
            }
            if (resource.getResourceType().equals(ResourceType.LWM2M_MODEL) && toLwM2mObject(resource) == null) {
                throw new DataValidationException(String.format("Could not parse the XML of objectModel with name %s", resource.getSearchText()));
            }
        }
    };

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

    protected Optional<ConstraintViolationException> extractConstraintViolationException(Exception t) {
        if (t instanceof ConstraintViolationException) {
            return Optional.of((ConstraintViolationException) t);
        } else if (t.getCause() instanceof ConstraintViolationException) {
            return Optional.of((ConstraintViolationException) (t.getCause()));
        } else {
            return Optional.empty();
        }
    }
}
