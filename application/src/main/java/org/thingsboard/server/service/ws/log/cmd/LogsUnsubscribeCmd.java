// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ws.log.cmd;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.service.ws.WsCmd;
import org.thingsboard.server.service.ws.WsCmdType;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.UnsubscribeCmd;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogsUnsubscribeCmd implements UnsubscribeCmd, WsCmd {

    private int cmdId;

    @Override
    public WsCmdType getType() {
        return WsCmdType.LOGS_UNSUBSCRIBE;
    }
}
