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

import org.thingsboard.server.common.data.id.NotificationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

public interface NotificationService {

    Notification saveNotification(TenantId tenantId, Notification notification);

    Notification findNotificationById(TenantId tenantId, NotificationId notificationId);

    boolean markNotificationAsRead(TenantId tenantId, UserId userId, NotificationId notificationId);

    PageData<Notification> findNotificationsByUserIdAndReadStatus(TenantId tenantId, UserId userId, boolean unreadOnly, PageLink pageLink);

    PageData<Notification> findLatestUnreadNotificationsByUserId(TenantId tenantId, UserId userId, int limit);

    int countUnreadNotificationsByUserId(TenantId tenantId, UserId userId);

    boolean deleteNotification(TenantId tenantId, UserId userId, NotificationId notificationId);

}
