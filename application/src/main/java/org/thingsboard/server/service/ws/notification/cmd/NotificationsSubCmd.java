// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.ws.notification.cmd;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.service.ws.WsCmd;
import org.thingsboard.server.service.ws.WsCmdType;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationsSubCmd implements WsCmd {
    private int cmdId;
    private int limit;
    private Set<NotificationType> types;

    @Override
    public WsCmdType getType() {
        return WsCmdType.NOTIFICATIONS;
    }
}
