// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.alarm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class AlarmFilter {

    private List<String> typesList;
    private List<AlarmSeverity> severityList;
    private List<AlarmStatus> statusList;
    private Long startTime;

}
