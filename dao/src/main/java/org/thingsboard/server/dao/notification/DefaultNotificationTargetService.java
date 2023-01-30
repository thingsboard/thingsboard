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
package org.thingsboard.server.dao.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.NotificationRequestStatus;
import org.thingsboard.server.common.data.notification.targets.platform.CustomerUsersFilter;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.NotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.platform.PlatformUsersNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.platform.UserListFilter;
import org.thingsboard.server.common.data.notification.targets.platform.UsersFilter;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.user.UserService;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultNotificationTargetService extends AbstractEntityService implements NotificationTargetService {

    private final NotificationTargetDao notificationTargetDao;
    private final NotificationRequestDao notificationRequestDao;
    private final NotificationRuleDao notificationRuleDao;
    private final UserService userService;

    @Override
    public NotificationTarget saveNotificationTarget(TenantId tenantId, NotificationTarget notificationTarget) {
        try {
            return notificationTargetDao.saveAndFlush(tenantId, notificationTarget);
        } catch (Exception e) {
            checkConstraintViolation(e, Map.of(
                    "uq_notification_target_name", "Notification target with such name already exists"
            ));
            throw e;
        }
    }

    @Override
    public NotificationTarget findNotificationTargetById(TenantId tenantId, NotificationTargetId id) {
        return notificationTargetDao.findById(tenantId, id.getId());
    }

    @Override
    public PageData<NotificationTarget> findNotificationTargetsByTenantId(TenantId tenantId, PageLink pageLink) {
        return notificationTargetDao.findByTenantIdAndPageLink(tenantId, pageLink);
    }

    @Override
    public PageData<User> findRecipientsForNotificationTarget(TenantId tenantId, CustomerId customerId, NotificationTargetId targetId, PageLink pageLink) {
        NotificationTarget notificationTarget = findNotificationTargetById(tenantId, targetId);
        Objects.requireNonNull(notificationTarget, "Notification target [" + targetId + "] not found");
        NotificationTargetConfig configuration = notificationTarget.getConfiguration();
        return findRecipientsForNotificationTargetConfig(tenantId, customerId, configuration, pageLink);
    }

    @Override
    public int countRecipientsForNotificationTargetConfig(TenantId tenantId, NotificationTargetConfig targetConfig) {
        return (int) findRecipientsForNotificationTargetConfig(tenantId, null, targetConfig, new PageLink(1)).getTotalElements();
    }

    @Override
    public PageData<User> findRecipientsForNotificationTargetConfig(TenantId tenantId, CustomerId customerId, NotificationTargetConfig targetConfig, PageLink pageLink) {
        if (!(targetConfig instanceof PlatformUsersNotificationTargetConfig)) {
            throw new IllegalArgumentException("Unsupported target type " + targetConfig.getType());
        }
        UsersFilter usersFilter = ((PlatformUsersNotificationTargetConfig) targetConfig).getUsersFilter();
        switch (usersFilter.getType()) {
            case USER_LIST: {
                List<User> users = ((UserListFilter) usersFilter).getUsersIds().stream()
                        .map(UserId::new).map(userId -> userService.findUserById(tenantId, userId))
                        .collect(Collectors.toList());
                return new PageData<>(users, 1, users.size(), false);
            }
            case CUSTOMER_USERS: {
                if (tenantId.equals(TenantId.SYS_TENANT_ID)) {
                    throw new IllegalArgumentException("Customer users target is not supported for system administrator");
                }
                CustomerUsersFilter customerUsersConfig = (CustomerUsersFilter) usersFilter;
                if (!customerUsersConfig.isGetCustomerIdFromOriginatorEntity()) {
                    customerId = new CustomerId(customerUsersConfig.getCustomerId());
                }
                if (customerId != null && !customerId.isNullUid()) {
                    return userService.findCustomerUsers(tenantId, customerId, pageLink);
                }
                break;
            }
            case ALL_USERS: {
                if (!tenantId.equals(TenantId.SYS_TENANT_ID)) {
                    return userService.findUsersByTenantId(tenantId, pageLink);
                } else {
                    return userService.findUsers(TenantId.SYS_TENANT_ID, pageLink);
                }
            }
        }
        return new PageData<>();
    }

    @Override
    public void deleteNotificationTargetById(TenantId tenantId, NotificationTargetId id) {
        if (notificationRequestDao.existsByStatusAndTargetId(tenantId, NotificationRequestStatus.SCHEDULED, id)) {
            throw new IllegalArgumentException("Notification target is referenced by scheduled notification request");
        }
        if (notificationRuleDao.existsByTargetId(tenantId, id)) {
            throw new IllegalArgumentException("Notification target is being used in notification rule");
        }
        notificationTargetDao.removeById(tenantId, id.getId());
    }

}
