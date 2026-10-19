// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.id.UserId;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlarmFilterConfig {

    private List<String> typeList;
    private List<AlarmSearchStatus> statusList;
    private List<AlarmSeverity> severityList;
    private UserId assigneeId;
    private boolean searchPropagatedAlarms;
}
