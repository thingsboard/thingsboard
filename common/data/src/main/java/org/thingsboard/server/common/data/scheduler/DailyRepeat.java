// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.scheduler;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Calendar;

/**
 * Created by ashvayka on 28.11.17.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DailyRepeat extends SchedulerDate implements SchedulerRepeat {

    public static final long _1DAY = 1000 * 60 * 60 * 24;
    private long endsOn;

    @Override
    public SchedulerRepeatType getType() {
        return SchedulerRepeatType.DAILY;
    }


    @Override
    public long getNext(long startTime, long ts, String timezone) {
        return getNext(startTime, ts, timezone, endsOn, Calendar.DAY_OF_YEAR);
    }
}
