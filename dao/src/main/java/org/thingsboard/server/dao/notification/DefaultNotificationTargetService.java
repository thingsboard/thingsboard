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
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.NotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.SingleUserNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.UserListNotificationTargetConfig;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.service.DataValidator;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultNotificationTargetService implements NotificationTargetService {

    private final NotificationTargetDao notificationTargetDao;
    private final NotificationTargetValidator validator = new NotificationTargetValidator();

    @Override
    public NotificationTarget saveNotificationTarget(TenantId tenantId, NotificationTarget notificationTarget) {
        notificationTarget.setTenantId(tenantId);
        validator.validate(notificationTarget, NotificationTarget::getTenantId);
        return notificationTargetDao.save(tenantId, notificationTarget);
    }

    @Override
    public NotificationTarget findNotificationTargetById(TenantId tenantId, NotificationTargetId id) {
        return notificationTargetDao.findById(tenantId, id.getId());
    }

    @Override
    public PageData<NotificationTarget> findNotificationTargetsByTenantIdAndPageLink(TenantId tenantId, PageLink pageLink) {
        return notificationTargetDao.findByTenantIdAndPageLink(tenantId, pageLink);
    }

    @Override
    public List<UserId> findRecipientsForNotificationTarget(TenantId tenantId, NotificationTargetId notificationTargetId) {
        NotificationTarget notificationTarget = findNotificationTargetById(tenantId, notificationTargetId);
        NotificationTargetConfig configuration = notificationTarget.getConfiguration();
        List<UserId> recipients = new ArrayList<>();
        switch (configuration.getType()) {
            case SINGLE_USER:
                SingleUserNotificationTargetConfig singleUserNotificationTargetConfig = (SingleUserNotificationTargetConfig) configuration;
                recipients.add(singleUserNotificationTargetConfig.getUserId());
                break;
            case USER_LIST:
                UserListNotificationTargetConfig userListNotificationTargetConfig = (UserListNotificationTargetConfig) configuration;
                recipients.addAll(userListNotificationTargetConfig.getUsersIds());
                break;
        }
        return recipients;
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
