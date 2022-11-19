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
package org.thingsboard.monitoring.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.monitoring.notification.channels.NotificationChannel;
import org.thingsboard.monitoring.data.notification.NotificationInfo;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final List<NotificationChannel> notificationChannels;

    public void notify(NotificationInfo notificationInfo) {
        notificationChannels.forEach(notificationChannel -> {
            try {
                notificationChannel.sendNotification(notificationInfo);
            } catch (Exception e) {
                log.error("Failed to send notification to {} ({})", notificationChannel.getClass().getSimpleName(), notificationInfo, e);
            }
        });
    }

}
