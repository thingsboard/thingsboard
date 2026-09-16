// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.ws.notification.sub;

import lombok.Data;

@Data
public class NotificationsSubscriptionUpdate {

    private final NotificationUpdate notificationUpdate;
    private final NotificationRequestUpdate notificationRequestUpdate;

    public NotificationsSubscriptionUpdate(NotificationUpdate notificationUpdate) {
        this.notificationUpdate = notificationUpdate;
        this.notificationRequestUpdate = null;
    }

    public NotificationsSubscriptionUpdate(NotificationRequestUpdate notificationRequestUpdate) {
        this.notificationUpdate = null;
        this.notificationRequestUpdate = notificationRequestUpdate;
    }

}
