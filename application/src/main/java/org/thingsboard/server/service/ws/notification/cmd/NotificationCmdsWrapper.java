// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.ws.notification.cmd;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.thingsboard.server.service.ws.WsCommandsWrapper;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @deprecated Use {@link WsCommandsWrapper}. This class is left for backward compatibility
 * */
@Data
@Deprecated
public class NotificationCmdsWrapper {

    private NotificationsCountSubCmd unreadCountSubCmd;

    private NotificationsSubCmd unreadSubCmd;

    private MarkNotificationsAsReadCmd markAsReadCmd;

    private MarkAllNotificationsAsReadCmd markAllAsReadCmd;

    private NotificationsUnsubCmd unsubCmd;

    @JsonIgnore
    public WsCommandsWrapper toCommonCmdsWrapper() {
        return new WsCommandsWrapper(null, Stream.of(
                        unreadCountSubCmd, unreadSubCmd, markAsReadCmd, markAllAsReadCmd, unsubCmd
                )
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
    }

}
