// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.notification;

import org.thingsboard.server.common.data.id.NotificationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.Set;

public interface NotificationService {

    Notification saveNotification(TenantId tenantId, Notification notification);

    Notification findNotificationById(TenantId tenantId, NotificationId notificationId);

    boolean markNotificationAsRead(TenantId tenantId, UserId recipientId, NotificationId notificationId);

    int markAllNotificationsAsRead(TenantId tenantId, NotificationDeliveryMethod deliveryMethod, UserId recipientId);

    PageData<Notification> findNotificationsByRecipientIdAndReadStatus(TenantId tenantId, NotificationDeliveryMethod deliveryMethod, UserId recipientId, boolean unreadOnly, PageLink pageLink);

    PageData<Notification> findLatestUnreadNotificationsByRecipientIdAndNotificationTypes(TenantId tenantId, NotificationDeliveryMethod deliveryMethod, UserId recipientId, Set<NotificationType> types, int limit);

    int countUnreadNotificationsByRecipientId(TenantId tenantId, NotificationDeliveryMethod deliveryMethod, UserId recipientId);

    boolean deleteNotification(TenantId tenantId, UserId recipientId, NotificationId notificationId);

}
