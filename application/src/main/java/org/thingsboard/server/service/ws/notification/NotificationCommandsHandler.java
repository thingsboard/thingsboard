// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.ws.notification;

import org.thingsboard.server.service.ws.WebSocketSessionRef;
import org.thingsboard.server.service.ws.notification.cmd.MarkAllNotificationsAsReadCmd;
import org.thingsboard.server.service.ws.notification.cmd.MarkNotificationsAsReadCmd;
import org.thingsboard.server.service.ws.notification.cmd.NotificationsCountSubCmd;
import org.thingsboard.server.service.ws.notification.cmd.NotificationsSubCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.UnsubscribeCmd;

public interface NotificationCommandsHandler {

    void handleUnreadNotificationsSubCmd(WebSocketSessionRef sessionRef, NotificationsSubCmd cmd);

    void handleUnreadNotificationsCountSubCmd(WebSocketSessionRef sessionRef, NotificationsCountSubCmd cmd);

    void handleMarkAsReadCmd(WebSocketSessionRef sessionRef, MarkNotificationsAsReadCmd cmd);

    void handleMarkAllAsReadCmd(WebSocketSessionRef sessionRef, MarkAllNotificationsAsReadCmd cmd);

    void handleUnsubCmd(WebSocketSessionRef sessionRef, UnsubscribeCmd cmd);

}
