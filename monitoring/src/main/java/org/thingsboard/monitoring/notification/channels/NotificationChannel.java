// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.monitoring.notification.channels;

import org.thingsboard.monitoring.data.notification.Notification;

public interface NotificationChannel {

    void sendNotification(String message, Notification notification);

}
