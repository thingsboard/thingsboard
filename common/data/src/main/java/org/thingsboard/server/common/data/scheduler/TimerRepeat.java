// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.scheduler;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.concurrent.TimeUnit;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TimerRepeat implements SchedulerRepeat {

    private long repeatInterval;
    private TimeUnit timeUnit;
    private long endsOn;


    @Override
    public long getEndsOn() {
        return endsOn;
    }

    @Override
    public SchedulerRepeatType getType() {
        return SchedulerRepeatType.TIMER;
    }

    @Override
    public long getNext(long startTime, long ts, String timezone) {
        long interval = timeUnit.toMillis(repeatInterval);
        for (long tmp = startTime; tmp < endsOn; tmp += interval) {
            if (tmp > ts) {
                return tmp;
            }
        }
        return 0L;
    }
}
