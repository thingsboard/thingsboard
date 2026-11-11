// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
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
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.NotificationTargetType;
import org.thingsboard.server.common.data.notification.targets.platform.CustomerUsersFilter;
import org.thingsboard.server.common.data.notification.targets.platform.PlatformUsersNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.platform.TenantAdministratorsFilter;
import org.thingsboard.server.common.data.notification.targets.platform.UserGroupListFilter;
import org.thingsboard.server.common.data.notification.targets.platform.UserListFilter;
import org.thingsboard.server.common.data.notification.targets.platform.UserRoleFilter;
import org.thingsboard.server.common.data.notification.targets.platform.UsersFilter;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.dao.notification.NotificationTargetService;
import org.thingsboard.server.dao.service.ConstraintValidator;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

import java.util.List;
import java.util.stream.Collectors;

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
                case USER_GROUP_LIST:
                    UserGroupListFilter userGroupListFilter = (UserGroupListFilter) usersFilter;
                    userGroupListFilter.setGroupsIds(userGroupListFilter.getGroupsIds().stream()
                            .map(EntityGroupId::new).map(idProvider::getInternalId)
                            .map(UUIDBased::getId).collect(Collectors.toList()));
                    break;
                case USER_ROLE:
                    UserRoleFilter userRoleFilter = (UserRoleFilter) usersFilter;
                    userRoleFilter.setRolesIds(userRoleFilter.getRolesIds().stream()
                            .map(RoleId::new).map(idProvider::getInternalId)
                            .map(UUIDBased::getId).collect(Collectors.toList()));
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
