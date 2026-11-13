// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.scheduler;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Calendar;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MonthlyRepeat extends SchedulerDate implements SchedulerRepeat {

    private long endsOn;

    @Override
    public SchedulerRepeatType getType() {
        return SchedulerRepeatType.MONTHLY;
    }

    @Override
    public long getNext(long startTime, long ts, String timezone) {
        return getNext(startTime, ts, timezone, endsOn, Calendar.MONTH);
    }
}
