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
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationStatus;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.service.DataValidator;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultNotificationService implements NotificationService {

    private final NotificationRequestDao notificationRequestDao;
    private final NotificationDao notificationDao;
    private final NotificationTargetService notificationTargetService;
    private final NotificationRequestValidator notificationRequestValidator = new NotificationRequestValidator();

    @Override
    public NotificationRequest createNotificationRequest(TenantId tenantId, NotificationRequest notificationRequest) {
        if (notificationRequest.getId() != null) {
            throw new IllegalArgumentException();
        }
        notificationRequestValidator.validate(notificationRequest, NotificationRequest::getTenantId);
        return notificationRequestDao.save(tenantId, notificationRequest);
    }

    @Override
    public PageData<NotificationRequest> findNotificationRequestsByTenantIdAndPageLink(TenantId tenantId, PageLink pageLink) {
        return null;
    }

    @Override
    public Notification createNotification(TenantId tenantId, Notification notification) {
        if (notification.getId() != null) {
            throw new IllegalArgumentException();
        }
        return notificationDao.save(tenantId, notification);
    }

    @Override
    public void updateNotificationStatus(TenantId tenantId, NotificationId notificationId, NotificationStatus status) {

    }

    @Override
    public PageData<Notification> findNotificationsByUserIdAndPageLink(TenantId tenantId, UserId userId, PageLink pageLink) {
        return null;
    }

    private static class NotificationRequestValidator extends DataValidator<NotificationRequest> {
    }

}
