// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.alarm.rule.condition.schedule;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema
@Data
public class CustomTimeSchedule implements AlarmSchedule {

    private String timezone;
    private List<CustomTimeScheduleItem> items;

    @Override
    public AlarmScheduleType getType() {
        return AlarmScheduleType.CUSTOM;
    }

}
