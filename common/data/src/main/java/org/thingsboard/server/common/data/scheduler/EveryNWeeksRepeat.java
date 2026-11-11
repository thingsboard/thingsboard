// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.scheduler;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Calendar;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EveryNWeeksRepeat implements SchedulerRepeat {

    private long endsOn;

    private int weeks;

    @Override
    public SchedulerRepeatType getType() {
        return SchedulerRepeatType.EVERY_N_WEEKS;
    }

    @Override
    public long getNext(long startTime, long ts, String timezone) {
        Calendar calendar = SchedulerUtils.getCalendarWithTimeZone(timezone);
        long tmp = startTime;
        int repeatIteration = 0;
        while (tmp < endsOn) {
            calendar.setTimeInMillis(startTime);
            calendar.add(Calendar.DAY_OF_YEAR, repeatIteration * this.weeks * 7);
            tmp = calendar.getTimeInMillis();
            if (tmp > ts && tmp < endsOn) {
                return tmp;
            }
            repeatIteration++;
        }
        return 0L;
    }
}
