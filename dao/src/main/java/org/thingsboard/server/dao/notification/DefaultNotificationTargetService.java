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
import org.thingsboard.server.common.data.notification.targets.CustomerUsersNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.NotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.SingleUserNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.UserListNotificationTargetConfig;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.user.UserService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultNotificationTargetService implements NotificationTargetService {

    private final NotificationTargetDao notificationTargetDao;
    private final UserService userService;
    private final NotificationTargetValidator validator = new NotificationTargetValidator();

    @Override
    public NotificationTarget saveNotificationTarget(TenantId tenantId, NotificationTarget notificationTarget) {
        validator.validate(notificationTarget, NotificationTarget::getTenantId);
        return notificationTargetDao.save(tenantId, notificationTarget);
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
    public PageData<User> findRecipientsForNotificationTarget(TenantId tenantId, NotificationTargetId notificationTargetId, PageLink pageLink) {
        NotificationTarget notificationTarget = findNotificationTargetById(tenantId, notificationTargetId);
        NotificationTargetConfig configuration = notificationTarget.getConfiguration();
        return findRecipientsForNotificationTargetConfig(tenantId, configuration, pageLink);
    }

    @Override
    public PageData<User> findRecipientsForNotificationTargetConfig(TenantId tenantId, NotificationTargetConfig targetConfig, PageLink pageLink) {
        switch (targetConfig.getType()) {
            case SINGLE_USER: {
                UserId userId = new UserId(((SingleUserNotificationTargetConfig) targetConfig).getUserId());
                User user = userService.findUserById(tenantId, userId);
                return new PageData<>(List.of(user), 1, 1, false);
            }
            case USER_LIST: {
                List<User> users = ((UserListNotificationTargetConfig) targetConfig).getUsersIds().stream()
                        .map(UserId::new).map(userId -> userService.findUserById(tenantId, userId))
                        .collect(Collectors.toList());
                return new PageData<>(users, 1, users.size(), false);
            }
            case CUSTOMER_USERS: {
                if (tenantId.equals(TenantId.SYS_TENANT_ID)) {
                    throw new IllegalArgumentException("Customer users target is not supported for system administrator");
                }
                CustomerId customerId = new CustomerId(((CustomerUsersNotificationTargetConfig) targetConfig).getCustomerId());
                return userService.findCustomerUsers(tenantId, customerId, pageLink);
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
    public void deleteNotificationTarget(TenantId tenantId, NotificationTargetId notificationTargetId) {
        notificationTargetDao.removeById(tenantId, notificationTargetId.getId());
        // todo: delete related notification requests (?)
    }

    private static class NotificationTargetValidator extends DataValidator<NotificationTarget> {

        @Override
        protected void validateDataImpl(TenantId tenantId, NotificationTarget notificationTarget) {
            super.validateDataImpl(tenantId, notificationTarget);
        }
    }

}
