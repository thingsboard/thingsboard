// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.ws.telemetry.cmd.v2;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.service.ws.WsCmd;
import org.thingsboard.server.service.ws.WsCmdType;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AlarmStatusCmd implements WsCmd {

    private int cmdId;
    private EntityId originatorId;
    private List<String> typeList;
    private List<AlarmSeverity> severityList;

    @Override
    public WsCmdType getType() {
        return WsCmdType.ALARM_STATUS;
    }
}
