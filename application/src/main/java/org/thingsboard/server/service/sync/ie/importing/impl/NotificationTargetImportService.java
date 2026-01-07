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
package org.thingsboard.server.service.sync.ie.importing.impl;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.NotificationTargetType;
import org.thingsboard.server.common.data.notification.targets.platform.CustomerUsersFilter;
import org.thingsboard.server.common.data.notification.targets.platform.PlatformUsersNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.platform.TenantAdministratorsFilter;
import org.thingsboard.server.common.data.notification.targets.platform.UserListFilter;
import org.thingsboard.server.common.data.notification.targets.platform.UsersFilter;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.dao.notification.NotificationTargetService;
import org.thingsboard.server.dao.service.ConstraintValidator;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

import java.util.List;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class NotificationTargetImportService extends BaseEntityImportService<NotificationTargetId, NotificationTarget, EntityExportData<NotificationTarget>> {

    private final NotificationTargetService notificationTargetService;

    @Override
    protected void setOwner(TenantId tenantId, NotificationTarget notificationTarget, IdProvider idProvider) {
        notificationTarget.setTenantId(tenantId);
    }

    @Override
    protected NotificationTarget prepare(EntitiesImportCtx ctx, NotificationTarget notificationTarget, NotificationTarget oldNotificationTarget, EntityExportData<NotificationTarget> exportData, IdProvider idProvider) {
        if (notificationTarget.getConfiguration().getType() == NotificationTargetType.PLATFORM_USERS) {
            UsersFilter usersFilter = ((PlatformUsersNotificationTargetConfig) notificationTarget.getConfiguration()).getUsersFilter();
            switch (usersFilter.getType()) {
                case CUSTOMER_USERS:
                    CustomerUsersFilter customerUsersFilter = (CustomerUsersFilter) usersFilter;
                    customerUsersFilter.setCustomerId(idProvider.getInternalId(new CustomerId(customerUsersFilter.getCustomerId())).getId());
                    break;
                case USER_LIST:
                    UserListFilter userListFilter = (UserListFilter) usersFilter;
                    userListFilter.setUsersIds(List.of(ctx.getUser().getUuidId())); // user entities are not supported by VC; replacing with current user id
                    break;
                case TENANT_ADMINISTRATORS:
                    if (CollectionUtils.isNotEmpty(((TenantAdministratorsFilter) usersFilter).getTenantsIds()) ||
                            CollectionUtils.isNotEmpty(((TenantAdministratorsFilter) usersFilter).getTenantProfilesIds())) {
                        throw new IllegalArgumentException("Permission denied");
                    }
                    break;
                case SYSTEM_ADMINISTRATORS:
                    throw new AccessDeniedException("Permission denied");
            }
        }
        return notificationTarget;
    }

    @Override
    protected NotificationTarget saveOrUpdate(EntitiesImportCtx ctx, NotificationTarget notificationTarget, EntityExportData<NotificationTarget> exportData, IdProvider idProvider, CompareResult compareResult) {
        ConstraintValidator.validateFields(notificationTarget);
        return notificationTargetService.saveNotificationTarget(ctx.getTenantId(), notificationTarget);
    }

    @Override
    protected void onEntitySaved(User user, NotificationTarget savedEntity, NotificationTarget oldEntity) throws ThingsboardException {
        entityActionService.logEntityAction(user, savedEntity.getId(), savedEntity, null,
                oldEntity == null ? ActionType.ADDED : ActionType.UPDATED, null);
    }

    @Override
    protected NotificationTarget deepCopy(NotificationTarget notificationTarget) {
        return new NotificationTarget(notificationTarget);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.NOTIFICATION_TARGET;
    }

}
