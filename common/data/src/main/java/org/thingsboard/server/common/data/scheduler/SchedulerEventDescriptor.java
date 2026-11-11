// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.scheduler;

public record SchedulerEventDescriptor(long startTime,
                                       String timezone,
                                       SchedulerRepeat repeat) {

    public boolean passedAway(long ts) {
        return repeat == null ? startTime < ts : repeat.getEndsOn() < ts;
    }

    public long getNextEventTime(long ts) {
        if (repeat != null && repeat.getEndsOn() > ts) {
            return repeat.getNext(startTime, ts, timezone);
        } else if (ts < startTime) {
            return startTime;
        } else {
            return 0L;
        }
    }

}
