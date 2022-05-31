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
package org.thingsboard.server.service.entitiy.resource;


import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.security.model.SecurityUser;

@Service
@TbCoreComponent
@AllArgsConstructor
@Slf4j
public class DefaultTbResourceNotifyService extends AbstractTbEntityService implements TbResourceNotifyService {

    @Override
    public TbResource save(TbResource tbResource, SecurityUser user) throws ThingsboardException {
        ActionType actionType = tbResource.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = tbResource.getTenantId();
        try {

            TbResource savedResource = checkNotNull(resourceService.saveResource(tbResource));
            tbClusterService.onResourceChange(savedResource, null);
            notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, savedResource.getId(),
                    savedResource, user, actionType, false, null);
            return savedResource;
        } catch (Exception e) {
            notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, emptyId(EntityType.TB_RESOURCE),
                    tbResource, user, actionType, false, e);
            throw handleException(e);
        }
    }

    @Override
    public void delete(TbResource tbResource, SecurityUser user) throws ThingsboardException {
        TbResourceId resourceId = tbResource.getId();
        TenantId tenantId = tbResource.getTenantId();
        try {
            resourceService.deleteResource(tenantId, resourceId);
            tbClusterService.onResourceDeleted(tbResource, null);
            notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, resourceId, tbResource, user, ActionType.DELETED,
                    false, null, resourceId.toString());
        } catch (Exception e) {
            notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, emptyId(EntityType.TB_RESOURCE), null, user, ActionType.DELETED,
                    false, e, resourceId.toString());
            throw handleException(e);
        }
    }
}
