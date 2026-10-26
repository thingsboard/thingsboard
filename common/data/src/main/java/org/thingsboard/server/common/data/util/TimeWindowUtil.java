// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.util;

import lombok.experimental.UtilityClass;
import org.thingsboard.server.common.data.StringUtils;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public final class TimeWindowUtil {
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter HH_MM_SS = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter ISO_LOCAL_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    public static ZoneId parseZoneId(String timezone, String fieldName) {
        if (StringUtils.isBlank(timezone)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        try {
            return ZoneId.of(timezone.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException(fieldName + " must be a valid timezone ID: " + timezone, e);
        }
    }

    public static LocalDate parseLocalDate(String date, String fieldName) {
        if (StringUtils.isBlank(date)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        try {
            return LocalDate.parse(date.trim(), ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(fieldName + " must be in yyyy-MM-dd format", e);
        }
    }

    public static LocalTime parseLocalTime(String time, String fieldName) {
        if (StringUtils.isBlank(time)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }

        String trimmed = time.trim();
        if (trimmed.length() != 5 && trimmed.length() != 8) {
            throw new IllegalArgumentException(fieldName + " must be in HH:mm or HH:mm:ss format");
        }

        try {
            if (trimmed.length() == 5) {
                return LocalTime.parse(trimmed, HH_MM);
            }
            return LocalTime.parse(trimmed, HH_MM_SS);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(fieldName + " must be in HH:mm or HH:mm:ss format with valid values", e);
        }
    }

    public static LocalDateTime toLocalDateTime(Instant nowUtc, ZoneId zoneId) {
        return LocalDateTime.ofInstant(nowUtc, zoneId);
    }

    public static boolean isDateInRange(LocalDate date, LocalDate startInclusive, LocalDate endInclusive) {
        return !date.isBefore(startInclusive) && !date.isAfter(endInclusive);
    }

    public static boolean isTimeInHalfOpenWindow(LocalTime time, LocalTime startInclusive, LocalTime endExclusive) {
        return !time.isBefore(startInclusive) && time.isBefore(endExclusive);
    }

    public static List<DayOfWeek> parseDaysOfWeek(String daysStr, String fieldName) {
        if (StringUtils.isBlank(daysStr)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        try {
            return Arrays.stream(daysStr.split(","))
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .map(DayOfWeek::valueOf)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(fieldName + " must contain valid day names (MONDAY-SUNDAY): " + daysStr, e);
        }
    }
}
