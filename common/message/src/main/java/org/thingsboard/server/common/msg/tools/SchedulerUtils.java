/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.common.msg.tools;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.TemporalUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoUnit.MONTHS;

public class SchedulerUtils {

    private final static ZoneId UTC = ZoneId.of("UTC");
    private static final ConcurrentMap<String, ZoneId> tzMap = new ConcurrentHashMap<>();

    public static ZoneId getZoneId(String tz) {
        return tzMap.computeIfAbsent(tz == null || tz.isEmpty() ? "UTC" : tz, ZoneId::of);
    }

    public static long getStartOfCurrentHour() {
        return getStartOfCurrentHour(UTC);
    }

    public static long getStartOfCurrentHour(ZoneId zoneId) {
        return LocalDateTime.now(ZoneOffset.UTC).atZone(zoneId).truncatedTo(ChronoUnit.HOURS).toInstant().toEpochMilli();
    }

    public static long getStartOfCurrentMonth() {
        return getStartOfCurrentMonth(UTC);
    }

    public static long getStartOfCurrentMonth(ZoneId zoneId) {
        return LocalDate.now().withDayOfMonth(1).atStartOfDay(zoneId).toInstant().toEpochMilli();
    }

    public static long getStartOfNextMonth() {
        return getStartOfNextMonth(UTC);
    }

    public static long getStartOfNextMonth(ZoneId zoneId) {
        return LocalDate.now().with(TemporalAdjusters.firstDayOfNextMonth()).atStartOfDay(zoneId).toInstant().toEpochMilli();
    }

    public static long getStartOfNextNextMonth() {
        return getStartOfNextNextMonth(UTC);
    }

    public static long getStartOfNextNextMonth(ZoneId zoneId) {
        return LocalDate.now().with(firstDayOfNextNextMonth()).atStartOfDay(zoneId).toInstant().toEpochMilli();
    }

    public static TemporalAdjuster firstDayOfNextNextMonth() {
        return (temporal) -> temporal.with(DAY_OF_MONTH, 1).plus(2, MONTHS);
    }

}
