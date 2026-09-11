// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.profile;

import lombok.Data;
import org.thingsboard.server.common.data.query.DynamicValue;

import java.util.Set;

@Data
public class SpecificTimeSchedule implements AlarmSchedule {

    private String timezone;
    private Set<Integer> daysOfWeek;
    private long startsOn;
    private long endsOn;

    private DynamicValue<String> dynamicValue;

    @Override
    public AlarmScheduleType getType() {
        return AlarmScheduleType.SPECIFIC_TIME;
    }

}
