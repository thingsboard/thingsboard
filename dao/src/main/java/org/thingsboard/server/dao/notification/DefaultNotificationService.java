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
import org.thingsboard.server.common.data.id.NotificationId;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationStatus;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.dao.sql.query.EntityKeyMapping;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultNotificationService implements NotificationService {

    private final NotificationDao notificationDao;

    @Override
    public Notification saveNotification(TenantId tenantId, Notification notification) {
        return notificationDao.save(tenantId, notification);
    }

    @Override
    public Notification findNotificationById(TenantId tenantId, NotificationId notificationId) {
        return notificationDao.findById(tenantId, notificationId.getId());
    }

    @Override
    public boolean markNotificationAsRead(TenantId tenantId, UserId userId, NotificationId notificationId) {
        return notificationDao.updateStatusByIdAndUserId(tenantId, userId, notificationId, NotificationStatus.READ);
    }

    @Override
    public PageData<Notification> findNotificationsByUserIdAndReadStatus(TenantId tenantId, UserId userId, boolean unreadOnly, PageLink pageLink) {
        if (unreadOnly) {
            return notificationDao.findUnreadByUserIdAndPageLink(tenantId, userId, pageLink);
        } else {
            return notificationDao.findByUserIdAndPageLink(tenantId, userId, pageLink);
        }
    }

    @Override
    public PageData<Notification> findLatestUnreadNotificationsByUserId(TenantId tenantId, UserId userId, int limit) {
        SortOrder sortOrder = new SortOrder(EntityKeyMapping.CREATED_TIME, SortOrder.Direction.DESC);
        PageLink pageLink = new PageLink(limit, 0, null, sortOrder);
        return findNotificationsByUserIdAndReadStatus(tenantId, userId, true, pageLink);
    }

    @Override
    public int countUnreadNotificationsByUserId(TenantId tenantId, UserId userId) {
        return notificationDao.countUnreadByUserId(tenantId, userId);
    }

    @Override
    public void updateNotificationsStatusByRequestId(TenantId tenantId, NotificationRequestId requestId, NotificationStatus status) {
        notificationDao.updateStatusesByRequestId(tenantId, requestId, status);
    }

    @Override
    public boolean deleteNotification(TenantId tenantId, UserId userId, NotificationId notificationId) {
        return notificationDao.deleteByIdAndUserId(tenantId, userId, notificationId);
    }

}
