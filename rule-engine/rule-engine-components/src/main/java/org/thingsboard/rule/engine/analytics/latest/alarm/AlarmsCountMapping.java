// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.latest.alarm;

import lombok.Data;
import org.thingsboard.server.common.data.alarm.AlarmFilter;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;

import java.util.List;

@Data
public class AlarmsCountMapping {

    private String target;
    private List<String> typesList;
    private List<AlarmSeverity> severityList;
    private List<AlarmStatus> statusList;
    private long latestInterval;

    public AlarmFilter createAlarmFilter() {
        Long startTime = null;
        if (latestInterval > 0) {
            startTime = System.currentTimeMillis() - latestInterval;
        }
        return new AlarmFilter(nullIfEmpty(this.typesList), nullIfEmpty(this.severityList), nullIfEmpty(this.statusList), startTime);
    }

    private <T> List<T> nullIfEmpty(List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list;
    }
}
