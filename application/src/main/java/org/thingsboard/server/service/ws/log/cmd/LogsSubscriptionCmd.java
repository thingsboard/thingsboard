// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ws.log.cmd;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.service.ws.WsCmd;
import org.thingsboard.server.service.ws.WsCmdType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogsSubscriptionCmd implements WsCmd {

    private int cmdId;
    private String entityType;
    private String entityId;
    private long lastSeenSeq;

    @Override
    public WsCmdType getType() {
        return WsCmdType.LOGS;
    }
}
