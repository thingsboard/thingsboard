// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.scheduler;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Calendar;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EveryNDaysRepeat implements SchedulerRepeat {

    private long endsOn;

    private int days;

    @Override
    public SchedulerRepeatType getType() {
        return SchedulerRepeatType.EVERY_N_DAYS;
    }

    @Override
    public long getNext(long startTime, long ts, String timezone) {
        Calendar calendar = SchedulerUtils.getCalendarWithTimeZone(timezone);
        long tmp = startTime;
        int repeatIteration = 0;
        while (tmp < endsOn) {
            calendar.setTimeInMillis(startTime);
            calendar.add(Calendar.DAY_OF_YEAR, repeatIteration * this.days);
            tmp = calendar.getTimeInMillis();
            if (tmp > ts && tmp < endsOn) {
                return tmp;
            }
            repeatIteration++;
        }
        return 0L;
    }
}
