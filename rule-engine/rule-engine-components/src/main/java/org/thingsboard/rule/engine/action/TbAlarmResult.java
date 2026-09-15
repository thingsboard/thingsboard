// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.action;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TbAlarmResult {

    boolean isCreated;
    boolean isUpdated;
    boolean isSeverityUpdated;
    boolean isCleared;
    Alarm alarm;

    Long conditionRepeats;
    Long conditionDuration;

    public TbAlarmResult(boolean isCreated, boolean isUpdated, boolean isCleared, Alarm alarm) {
        this.isCreated = isCreated;
        this.isUpdated = isUpdated;
        this.isCleared = isCleared;
        this.alarm = alarm;
    }

    public static TbAlarmResult fromAlarmResult(AlarmApiCallResult result) {
        boolean isSeverityChanged = result.isSeverityChanged();
        return TbAlarmResult.builder()
                .isCreated(result.isCreated())
                .isUpdated(result.isModified() && !isSeverityChanged)
                .isSeverityUpdated(isSeverityChanged)
                .isCleared(result.isCleared())
                .alarm(result.getAlarm())
                .build();
    }

}
