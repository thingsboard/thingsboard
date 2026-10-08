/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
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
