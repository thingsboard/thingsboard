/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.entitiy.tenant.profile;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantProfileService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.entitiy.queue.TbQueueService;

import java.util.List;

@Slf4j
@Service
@TbCoreComponent
@AllArgsConstructor
public class DefaultTbTenantProfileService extends AbstractTbEntityService implements TbTenantProfileService {
    private final TbQueueService tbQueueService;
    private final TenantProfileService tenantProfileService;
    private final TenantService tenantService;
    private final TbTenantProfileCache tenantProfileCache;

    @Override
    public TenantProfile
    save(TenantId tenantId, TenantProfile tenantProfile, TenantProfile oldTenantProfile, User user) throws ThingsboardException {
        ActionType actionType = tenantProfile.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        try {
            TenantProfile savedTenantProfile = checkNotNull(tenantProfileService.saveTenantProfile(tenantId, tenantProfile));
            tenantProfileCache.put(savedTenantProfile);
            logEntityActionService.logEntityAction(tenantId, savedTenantProfile.getId(), savedTenantProfile, null,
                    actionType, user);

            List<TenantId> tenantIds = tenantService.findTenantIdsByTenantProfileId(savedTenantProfile.getId());
            tbQueueService.updateQueuesByTenants(tenantIds, savedTenantProfile, oldTenantProfile);

            return savedTenantProfile;
        } catch (ThingsboardException e) {
            log.error("Failed to save tenant profile because ThingsboardException [{}]", tenantProfile, e);
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.TENANT_PROFILE), tenantProfile, actionType, user, e);
            throw e;
        } catch (DataValidationException e) {
            log.error("Failed to save tenant profile because data validation [{}]", tenantProfile, e);
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.TENANT_PROFILE), tenantProfile, actionType, user, e);
            throw new ThingsboardException(e.getMessage(), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        } catch (Exception e) {
            log.error("Failed to save tenant profile because Exception [{}]", tenantProfile, e);
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.TENANT_PROFILE), tenantProfile, actionType, user, e);
            throw new ThingsboardException(e, ThingsboardErrorCode.GENERAL);
        }

    }

    @Override
    public void delete(TenantId tenantId, TenantProfile tenantProfile, User user) throws ThingsboardException {
        ActionType actionType = ActionType.DELETED;
        try {
            tenantProfileService.deleteTenantProfile(tenantId, tenantProfile.getId());
            logEntityActionService.logEntityAction(tenantId, tenantProfile.getId(), tenantProfile, null, actionType, user);
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, tenantProfile.getId(), tenantProfile, null, actionType, user);
            throw e;
        }
    }

    @Override
    public TenantProfile setDefaultTenantProfile(TenantId tenantId, TenantProfile tenantProfile, User user) throws ThingsboardException {
        ActionType actionType = ActionType.UPDATED;
        try {
            TenantProfile savedTenantProfile = tenantProfileService.setDefaultTenantProfile(tenantId, tenantProfile.getId());
            logEntityActionService.logEntityAction(tenantId, tenantProfile.getId(), savedTenantProfile, null, actionType, user);
            return savedTenantProfile;
        } catch (DataValidationException e) {
            log.error("Failed to set default tenant profile due to data validation [{}]", tenantProfile, e);
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.TENANT_PROFILE), tenantProfile, actionType, user, e);
            throw new ThingsboardException(e.getMessage(), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        } catch (Exception e) {
            log.error("Failed to set default tenant profile [{}]", tenantProfile, e);
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.TENANT_PROFILE), tenantProfile, actionType, user, e);
            throw new ThingsboardException(e, ThingsboardErrorCode.GENERAL);
        }
    }
}
