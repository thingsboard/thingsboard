// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.sync.ie.exporting.impl;

import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.NotificationTargetType;
import org.thingsboard.server.common.data.notification.targets.platform.CustomerUsersFilter;
import org.thingsboard.server.common.data.notification.targets.platform.PlatformUsersNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.platform.UserGroupListFilter;
import org.thingsboard.server.common.data.notification.targets.platform.UserRoleFilter;
import org.thingsboard.server.common.data.notification.targets.platform.UsersFilter;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@TbCoreComponent
public class NotificationTargetExportService extends BaseEntityExportService<NotificationTargetId, NotificationTarget, EntityExportData<NotificationTarget>> {

    @Override
    protected void setRelatedEntities(EntitiesExportCtx<?> ctx, NotificationTarget notificationTarget, EntityExportData<NotificationTarget> exportData) {
        if (notificationTarget.getConfiguration().getType() == NotificationTargetType.PLATFORM_USERS) {
            UsersFilter usersFilter = ((PlatformUsersNotificationTargetConfig) notificationTarget.getConfiguration()).getUsersFilter();
            switch (usersFilter.getType()) {
                case CUSTOMER_USERS:
                    CustomerUsersFilter customerUsersFilter = (CustomerUsersFilter) usersFilter;
                    customerUsersFilter.setCustomerId(getExternalIdOrElseInternal(ctx, new CustomerId(customerUsersFilter.getCustomerId())).getId());
                    break;
                case USER_GROUP_LIST:
                    UserGroupListFilter userGroupListFilter = (UserGroupListFilter) usersFilter;
                    userGroupListFilter.setGroupsIds(toExternalIds(userGroupListFilter.getGroupsIds(), EntityGroupId::new, ctx).collect(Collectors.toList()));
                    break;
                case USER_ROLE:
                    UserRoleFilter userRoleFilter = (UserRoleFilter) usersFilter;
                    userRoleFilter.setRolesIds(toExternalIds(userRoleFilter.getRolesIds(), RoleId::new, ctx).collect(Collectors.toList()));
                case USER_LIST:
                    // users list stays as is and is replaced with current user id on import (due to user entities not being supported by VC)
                    break;
            }
        }
    }

    @Override
    public Set<EntityType> getSupportedEntityTypes() {
        return Set.of(EntityType.NOTIFICATION_TARGET);
    }

}
