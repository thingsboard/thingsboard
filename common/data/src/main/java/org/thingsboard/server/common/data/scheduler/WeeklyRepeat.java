// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.scheduler;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Calendar;
import java.util.List;

/**
 * Created by ashvayka on 28.11.17.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeeklyRepeat implements SchedulerRepeat {

    private long endsOn;
    private List<Integer> repeatOn;

    @Override
    public SchedulerRepeatType getType() {
        return SchedulerRepeatType.WEEKLY;
    }

    @Override
    public long getNext(long startTime, long ts, String timezone) {
        Calendar calendar = SchedulerUtils.getCalendarWithTimeZone(timezone);
        long tmp = startTime;

        calendar.setTimeInMillis(tmp);

        while (tmp < endsOn) {
            if (tmp > ts) {
                int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
                dayOfWeek = dayOfWeek - 1; // The UI calendar starts from 0;
                if (repeatOn.contains(dayOfWeek)) {
                    return tmp;
                }
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            tmp = calendar.getTimeInMillis();
        }
        return 0L;
    }
}
