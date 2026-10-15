// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.ws.notification.cmd;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.service.ws.WsCmd;
import org.thingsboard.server.service.ws.WsCmdType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationsCountSubCmd implements WsCmd {
    private int cmdId;

    @Override
    public WsCmdType getType() {
        return WsCmdType.NOTIFICATIONS_COUNT;
    }
}
