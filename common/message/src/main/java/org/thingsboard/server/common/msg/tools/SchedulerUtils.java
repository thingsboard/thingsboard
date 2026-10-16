// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.msg.tools;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoUnit.MONTHS;

public class SchedulerUtils {

    private static final ConcurrentMap<String, ZoneId> tzMap = new ConcurrentHashMap<>();

    public static ZoneId getZoneId(String tz) {
        return tzMap.computeIfAbsent(tz == null || tz.isEmpty() ? "UTC" : tz, ZoneId::of);
    }

    public static long getStartOfCurrentHour() {
        return getStartOfCurrentHour(UTC);
    }

    public static long getStartOfCurrentHour(ZoneId zoneId) {
        return LocalDateTime.now(UTC).atZone(zoneId).truncatedTo(ChronoUnit.HOURS).toInstant().toEpochMilli();
    }

    public static long getStartOfCurrentMonth() {
        return getStartOfCurrentMonth(UTC);
    }

    public static long getStartOfCurrentMonth(ZoneId zoneId) {
        return LocalDate.now(UTC).withDayOfMonth(1).atStartOfDay(zoneId).toInstant().toEpochMilli();
    }

    public static long getStartOfNextMonth() {
        return getStartOfNextMonth(UTC);
    }

    public static long getStartOfNextMonth(ZoneId zoneId) {
        return LocalDate.now(UTC).with(TemporalAdjusters.firstDayOfNextMonth()).atStartOfDay(zoneId).toInstant().toEpochMilli();
    }

}
