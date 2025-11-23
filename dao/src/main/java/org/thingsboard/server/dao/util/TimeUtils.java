/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.kv.IntervalType;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.time.temporal.WeekFields;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TimeUtils {

    public static long calculateIntervalEnd(long startTs, IntervalType intervalType, ZoneId tzId) {
        var startTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(startTs), tzId);
        switch (intervalType) {
            case WEEK:
                return startTime.truncatedTo(ChronoUnit.DAYS).with(WeekFields.SUNDAY_START.dayOfWeek(), 1).plusDays(7).toInstant().toEpochMilli();
            case WEEK_ISO:
                return startTime.truncatedTo(ChronoUnit.DAYS).with(WeekFields.ISO.dayOfWeek(), 1).plusDays(7).toInstant().toEpochMilli();
            case MONTH:
                return startTime.truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1).plusMonths(1).toInstant().toEpochMilli();
            case QUARTER:
                return startTime.truncatedTo(ChronoUnit.DAYS).with(IsoFields.DAY_OF_QUARTER, 1).plusMonths(3).toInstant().toEpochMilli();
            default:
                throw new RuntimeException("Not supported!");
        }
    }

    public static ZonedDateTime toZonedDateTime(long ts, ZoneId zoneId) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(ts), zoneId);
    }

}
