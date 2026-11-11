// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.scheduler;

import java.util.Calendar;

abstract class SchedulerDate {

    protected long getNext(long startTime, long ts, String timezone, long endsOn, int calendarField) {
        Calendar calendar = SchedulerUtils.getCalendarWithTimeZone(timezone);

        long tmp = startTime;
        int repeatIteration = 0;
        while (tmp < endsOn) {
            calendar.setTimeInMillis(startTime);
            calendar.add(calendarField, repeatIteration);
            tmp = calendar.getTimeInMillis();
            if (tmp > ts && tmp < endsOn) {
                return tmp;
            }
            repeatIteration++;
        }
        return 0L;
    }
}
