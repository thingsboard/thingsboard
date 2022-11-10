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

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.rest.client.RestClient;
import org.thingsboard.server.common.data.notification.AlarmOriginatedNotificationInfo;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationInfo;
import org.thingsboard.server.common.data.notification.NotificationOriginatorType;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class NotificationsClient extends NotificationApiWsClient {

    private NotificationsClient(String wsUrl, String token) throws Exception {
        super(wsUrl, token);
    }

    public static NotificationsClient newInstance(String username, String password) throws Exception {
        RestClient restClient = new RestClient("http://localhost:8080");
        restClient.login(username, password);
        NotificationsClient client = new NotificationsClient("ws://localhost:8080", restClient.getToken());
        client.connectBlocking();
        return client;
    }

    @Override
    public void onMessage(String s) {
        super.onMessage(s);
//        printNotificationsCount();
        printNotifications();
    }

    public void printNotifications() {
        System.out.println(StringUtils.repeat(System.lineSeparator(), 20));
        List<Notification> notifications = getNotifications();
        System.out.printf("   %s NEW MESSAGE%s\n\n", getUnreadCount(), notifications.size() > 1 ? "S" : "");
        notifications.forEach(notification -> {
            String notificationInfoStr = "";
            if (notification.getOriginatorType() == NotificationOriginatorType.ALARM) {
                AlarmOriginatedNotificationInfo info = (AlarmOriginatedNotificationInfo) notification.getInfo();
                notificationInfoStr = String.format("Alarm of type %s - %s severity - status: %s",
                        info.getAlarmType(), info.getAlarmSeverity(), info.getAlarmStatus());
            } else if (notification.getInfo() != null) {
                notificationInfoStr = Strings.nullToEmpty(notification.getInfo().getDescription());
            }
            SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            String time = format.format(new Date(notification.getCreatedTime()));
            System.out.printf("[%s] %-19s | %-30s | (%s)\n", time, notification.getReason(), notification.getText(), notificationInfoStr);
        });
        System.out.println(StringUtils.repeat(System.lineSeparator(), 5));
    }

    public void printNotificationsCount() {
        System.out.println();
        System.out.println();
        System.out.println();
        int unreadCount = getUnreadCount();
        System.out.printf("\r\r%s NEW MESSAGE%s", unreadCount, unreadCount > 1 ? "S" : "");
    }

    public static void main(String[] args) throws Exception {
        NotificationsClient client = NotificationsClient.newInstance("tenant@thingsboard.org", "tenant");
        client.subscribeForUnreadNotifications(5);
//        client.subscribeForUnreadNotificationsCount();
        new Scanner(System.in).nextLine();
    }
}
