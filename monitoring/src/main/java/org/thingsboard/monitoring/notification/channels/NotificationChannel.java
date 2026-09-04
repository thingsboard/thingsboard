// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.monitoring.notification.channels;

public interface NotificationChannel {

    void sendNotification(String message);

}
