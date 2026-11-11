// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.scheduler;

import org.thingsboard.server.common.data.StringUtils;

import java.util.Calendar;
import java.util.TimeZone;

public class SchedulerUtils {

    public static Calendar getCalendarWithTimeZone(String timezone) {
        TimeZone tz;
        if (StringUtils.isEmpty(timezone)) {
            tz = TimeZone.getTimeZone("UTC");
        } else {
            tz = TimeZone.getTimeZone(timezone);
        }
        return Calendar.getInstance(tz);
    }

}
