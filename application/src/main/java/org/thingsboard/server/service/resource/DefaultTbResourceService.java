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
package org.thingsboard.server.service.resource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.lwm2m.LwM2mObject;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.thingsboard.server.dao.device.DeviceServiceImpl.INCORRECT_TENANT_ID;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.utils.LwM2mObjectModelUtils.toLwM2mObject;
import static org.thingsboard.server.utils.LwM2mObjectModelUtils.toLwm2mResource;

@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultTbResourceService extends AbstractTbEntityService implements TbResourceService {

    private final ResourceService resourceService;

    @Override
    public TbResource save(TbResource resource, User user) throws ThingsboardException {
        if (resource.getResourceType() == ResourceType.IMAGE) {
            throw new IllegalArgumentException("Image resource type is not supported");
        }
        ActionType actionType = resource.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = resource.getTenantId();
        try {
            if (ResourceType.LWM2M_MODEL.equals(resource.getResourceType())) {
                toLwm2mResource(resource);
            } else if (resource.getResourceKey() == null) {
                resource.setResourceKey(resource.getFileName());
            }
            TbResource savedResource = resourceService.saveResource(resource);
            logEntityActionService.logEntityAction(tenantId, savedResource.getId(), savedResource, actionType, user);
            return savedResource;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.TB_RESOURCE),
                    resource, actionType, user, e);
            throw e;
        }
    }

    @Override
    public void delete(TbResource tbResource, User user) {
        if (tbResource.getResourceType() == ResourceType.IMAGE) {
            throw new IllegalArgumentException("Image resource type is not supported");
        }
        ActionType actionType = ActionType.DELETED;
        TbResourceId resourceId = tbResource.getId();
        TenantId tenantId = tbResource.getTenantId();
        try {
            resourceService.deleteResource(tenantId, resourceId);
            logEntityActionService.logEntityAction(tenantId, resourceId, tbResource, actionType, user, resourceId.toString());
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.TB_RESOURCE),
                    actionType, user, e, resourceId.toString());
            throw e;
        }
    }

    @Override
    public List<LwM2mObject> findLwM2mObject(TenantId tenantId, String sortOrder, String sortProperty, String[] objectIds) {
        log.trace("Executing findByTenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        List<TbResource> resources = resourceService.findTenantResourcesByResourceTypeAndObjectIds(tenantId, ResourceType.LWM2M_MODEL,
                objectIds);
        return resources.stream()
                .flatMap(s -> Stream.ofNullable(toLwM2mObject(s, false)))
                .sorted(getComparator(sortProperty, sortOrder))
                .collect(Collectors.toList());
    }

    @Override
    public List<LwM2mObject> findLwM2mObjectPage(TenantId tenantId, String sortProperty, String sortOrder, PageLink pageLink) {
        log.trace("Executing findByTenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        PageData<TbResource> resourcePageData = resourceService.findTenantResourcesByResourceTypeAndPageLink(tenantId, ResourceType.LWM2M_MODEL, pageLink);
        return resourcePageData.getData().stream()
                .flatMap(s -> Stream.ofNullable(toLwM2mObject(s, false)))
                .sorted(getComparator(sortProperty, sortOrder))
                .collect(Collectors.toList());
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
