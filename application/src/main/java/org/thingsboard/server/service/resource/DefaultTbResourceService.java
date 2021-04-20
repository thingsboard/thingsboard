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
package org.thingsboard.server.service.resource;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.leshan.core.model.DDFFileParser;
import org.eclipse.leshan.core.model.DefaultDDFFileValidator;
import org.eclipse.leshan.core.model.InvalidDDFFileException;
import org.eclipse.leshan.core.model.ObjectModel;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.lwm2m.LwM2mInstance;
import org.thingsboard.server.common.data.lwm2m.LwM2mObject;
import org.thingsboard.server.common.data.lwm2m.LwM2mResourceObserve;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.resource.ResourceService;

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

@Slf4j
@Service
public class DefaultTbResourceService implements TbResourceService {

    private final ResourceService resourceService;
    private final DDFFileParser ddfFileParser;

    public DefaultTbResourceService(ResourceService resourceService) {
        this.resourceService = resourceService;
        this.ddfFileParser = new DDFFileParser(new DefaultDDFFileValidator());
    }

    @Override
    public TbResource saveResource(TbResource resource) throws ThingsboardException {
        log.trace("Executing saveResource [{}]", resource);
        if (StringUtils.isEmpty(resource.getData())) {
            throw new DataValidationException("Resource data should be specified!");
        }
        if (ResourceType.LWM2M_MODEL.equals(resource.getResourceType())) {
            try {
                List<ObjectModel> objectModels =
                        ddfFileParser.parseEx(new ByteArrayInputStream(Base64.getDecoder().decode(resource.getData())), resource.getSearchText());
                if (!objectModels.isEmpty()) {
                    ObjectModel objectModel = objectModels.get(0);

                    String resourceKey = objectModel.id + LWM2M_SEPARATOR_KEY + objectModel.getVersion();
                    String name = objectModel.name;
                    resource.setResourceKey(resourceKey);
                    if (resource.getId() == null) {
                        resource.setTitle(name + " id=" + objectModel.id + " v" + objectModel.getVersion());
                    }
                    resource.setSearchText(resourceKey + LWM2M_SEPARATOR_SEARCH_TEXT + name);
                } else {
                    throw new DataValidationException(String.format("Could not parse the XML of objectModel with name %s", resource.getSearchText()));
                }
            } catch (InvalidDDFFileException | IOException e) {
                throw new ThingsboardException(e, ThingsboardErrorCode.GENERAL);
            }
            if (resource.getResourceType().equals(ResourceType.LWM2M_MODEL) && toLwM2mObject(resource) == null) {
                throw new DataValidationException(String.format("Could not parse the XML of objectModel with name %s", resource.getSearchText()));
            }
        } else {
            resource.setResourceKey(resource.getFileName());
        }

        return resourceService.saveResource(resource);
    }

    @Override
    public TbResource getResource(TenantId tenantId, ResourceType resourceType, String resourceId) {
        return resourceService.getResource(tenantId, resourceType, resourceId);
    }

    @Override
    public TbResource findResourceById(TenantId tenantId, TbResourceId resourceId) {
        return resourceService.findResourceById(tenantId, resourceId);
    }

    @Override
    public TbResourceInfo findResourceInfoById(TenantId tenantId, TbResourceId resourceId) {
        return resourceService.findResourceInfoById(tenantId, resourceId);
    }

    @Override
    public PageData<TbResourceInfo> findAllTenantResourcesByTenantId(TenantId tenantId, PageLink pageLink) {
        return resourceService.findAllTenantResourcesByTenantId(tenantId, pageLink);
    }

    @Override
    public PageData<TbResourceInfo> findTenantResourcesByTenantId(TenantId tenantId, PageLink pageLink) {
        return resourceService.findTenantResourcesByTenantId(tenantId, pageLink);
    }

    @Override
    public List<LwM2mObject> findLwM2mObject(TenantId tenantId, String sortOrder, String sortProperty, String[] objectIds) {
        log.trace("Executing findByTenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        List<TbResource> resources = resourceService.findTenantResourcesByResourceTypeAndObjectIds(tenantId, ResourceType.LWM2M_MODEL,
                objectIds);
        return resources.stream()
                .map(this::toLwM2mObject)
                .sorted(getComparator(sortProperty, sortOrder))
                .collect(Collectors.toList());
    }

    @Override
    public List<LwM2mObject> findLwM2mObjectPage(TenantId tenantId, String sortProperty, String sortOrder, PageLink pageLink) {
        log.trace("Executing findByTenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        PageData<TbResource> resourcePageData = resourceService.findTenantResourcesByResourceTypeAndPageLink(tenantId, ResourceType.LWM2M_MODEL, pageLink);
        return resourcePageData.getData().stream()
                .map(this::toLwM2mObject)
                .sorted(getComparator(sortProperty, sortOrder))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteResource(TenantId tenantId, TbResourceId resourceId) {
        resourceService.deleteResource(tenantId, resourceId);
    }

    @Override
    public void deleteResourcesByTenantId(TenantId tenantId) {
        resourceService.deleteResourcesByTenantId(tenantId);
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
                List<LwM2mResourceObserve> resources = new ArrayList<>();
                obj.resources.forEach((k, v) -> {
                    if (!v.operations.isExecutable()) {
                        LwM2mResourceObserve lwM2MResourceObserve = new LwM2mResourceObserve(k, v.name, false, false, false);
                        resources.add(lwM2MResourceObserve);
                    }
                });
                instance.setResources(resources.toArray(LwM2mResourceObserve[]::new));
                lwM2mObject.setInstances(new LwM2mInstance[]{instance});
                return lwM2mObject;
            }
        } catch (IOException | InvalidDDFFileException e) {
            log.error("Could not parse the XML of objectModel with name [{}]", resource.getSearchText(), e);
            return null;
        }
    }
}
