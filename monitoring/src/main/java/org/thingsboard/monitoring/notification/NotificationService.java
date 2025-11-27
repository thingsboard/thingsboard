/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.monitoring.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.monitoring.data.notification.Notification;
import org.thingsboard.monitoring.notification.channels.NotificationChannel;
import jakarta.annotation.PreDestroy;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final List<NotificationChannel> notificationChannels;
    private final ExecutorService notificationExecutor = Executors.newSingleThreadExecutor();

    @Value("${monitoring.notifications.message_prefix}")
    private String messagePrefix;

    public List<? extends Future<?>> sendNotification(Notification notification) {
        String message;
        if (StringUtils.isEmpty(messagePrefix)) {
            message = notification.getText();
        } else {
            message = messagePrefix + " " + notification.getText();
        }
        return notificationChannels.stream().map(notificationChannel ->
            notificationExecutor.submit(() -> {
                try {
                    notificationChannel.sendNotification(message);
                } catch (Exception e) {
                    log.error("Failed to send notification to {}", notificationChannel.getClass().getSimpleName(), e);
                }
            })
        ).toList();
    }

    @PreDestroy
    public void shutdownExecutor() {
        try {
            notificationExecutor.shutdown();
            if (!notificationExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                java.util.List<Runnable> dropped = notificationExecutor.shutdownNow();
                log.warn("Notification executor did not terminate in time. Forced shutdown; {} task(s) will not be executed.", dropped.size());
            }
        } catch (InterruptedException e) {
            java.util.List<Runnable> dropped = notificationExecutor.shutdownNow();
            Thread.currentThread().interrupt();
            log.warn("Interrupted during notification executor shutdown. Forced shutdown; {} task(s) will not be executed.", dropped.size());
        } catch (Exception e) {
            log.warn("Unexpected error while shutting down notification executor", e);
        }
    }
}
