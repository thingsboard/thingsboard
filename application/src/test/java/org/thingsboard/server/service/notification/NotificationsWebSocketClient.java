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
package org.thingsboard.server.service.notification;

import lombok.Getter;
import org.apache.commons.lang3.RandomUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.controller.TbTestWebSocketClient;
import org.thingsboard.server.service.ws.notification.cmd.MarkNotificationAsReadCmd;
import org.thingsboard.server.service.ws.notification.cmd.NotificationCmdsWrapper;
import org.thingsboard.server.service.ws.notification.cmd.NotificationsSubCmd;
import org.thingsboard.server.service.ws.notification.cmd.UnreadNotificationsUpdate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class NotificationsWebSocketClient extends TbTestWebSocketClient {

    private final Map<UUID, Notification> currentNotifications = new LinkedHashMap<>();
    @Getter
    private int totalUnreadCount;
    @Getter
    private UnreadNotificationsUpdate lastUpdate;

    public NotificationsWebSocketClient(String wsUrl, String token) throws URISyntaxException {
        super(new URI(wsUrl + "/api/ws/plugins/notifications?token=" + token));
    }

    public void subscribeForUnreadNotifications(int limit) {
        NotificationCmdsWrapper cmdsWrapper = new NotificationCmdsWrapper();
        cmdsWrapper.setUnreadSubCmd(new NotificationsSubCmd(newCmdId(), limit));
        sendCmd(cmdsWrapper);
    }

    public void markNotificationAsRead(UUID notificationId) {
        NotificationCmdsWrapper cmdsWrapper = new NotificationCmdsWrapper();
        cmdsWrapper.setMarkAsReadCmd(new MarkNotificationAsReadCmd(newCmdId(), notificationId));
        sendCmd(cmdsWrapper);
    }


    private void handleUpdate(UnreadNotificationsUpdate update) {
        totalUnreadCount = update.getTotalUnreadCount();
        if (update.getNotifications() != null) {
            currentNotifications.clear();
            currentNotifications.putAll(update.getNotifications().stream().collect(Collectors.toMap(IdBased::getUuidId, n -> n)));
        } else if (update.getUpdate() != null) {
            Notification notification = update.getUpdate();
            currentNotifications.put(notification.getUuidId(), notification);
        }
    }


    public void sendCmd(NotificationCmdsWrapper cmdsWrapper) {
        send(JacksonUtil.toString(cmdsWrapper));
    }

    @Override
    public void onMessage(String s) {
        UnreadNotificationsUpdate update = JacksonUtil.fromString(s, UnreadNotificationsUpdate.class);
        lastUpdate = update;
        handleUpdate(update);
        super.onMessage(s);
    }

    private static int newCmdId() {
        return RandomUtils.nextInt(1, 1000);
    }

}
