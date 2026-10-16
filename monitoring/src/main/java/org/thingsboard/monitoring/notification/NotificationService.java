// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.monitoring.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.monitoring.data.notification.Notification;
import org.thingsboard.monitoring.notification.channels.NotificationChannel;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final List<NotificationChannel> notificationChannels;
    private final ExecutorService notificationExecutor = Executors.newSingleThreadExecutor();

    @Value("${monitoring.notifications.message_prefix}")
    private String messagePrefix;

    public void sendNotification(Notification notification) {
        String message;
        if (StringUtils.isEmpty(messagePrefix)) {
            message = notification.getText();
        } else {
            message = messagePrefix + System.lineSeparator() + notification.getText();
        }
        notificationChannels.forEach(notificationChannel -> {
            notificationExecutor.submit(() -> {
                try {
                    notificationChannel.sendNotification(message);
                } catch (Exception e) {
                    log.error("Failed to send notification to {}", notificationChannel.getClass().getSimpleName(), e);
                }
            });
        });
    }

}
