// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.timeseries;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Optional;

public enum SqlTsPartitionDate {

    DAYS("yyyy_MM_dd", ChronoUnit.DAYS), MONTHS("yyyy_MM", ChronoUnit.MONTHS), YEARS("yyyy", ChronoUnit.YEARS), INDEFINITE("indefinite", ChronoUnit.FOREVER);

    private final String pattern;
    private final transient TemporalUnit truncateUnit;
    public final static LocalDateTime EPOCH_START = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC);

    SqlTsPartitionDate(String pattern, TemporalUnit truncateUnit) {
        this.pattern = pattern;
        this.truncateUnit = truncateUnit;
    }

    public String getPattern() {
        return pattern;
    }

    public TemporalUnit getTruncateUnit() {
        return truncateUnit;
    }

    public LocalDateTime trancateTo(LocalDateTime time) {
        switch (this) {
            case DAYS:
                return time.truncatedTo(ChronoUnit.DAYS);
            case MONTHS:
                return time.truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1);
            case YEARS:
                return time.truncatedTo(ChronoUnit.DAYS).withDayOfYear(1);
            case INDEFINITE:
                return EPOCH_START;
            default:
                throw new RuntimeException("Failed to parse partitioning property!");
        }
    }

    public LocalDateTime plusTo(LocalDateTime time) {
        switch (this) {
            case DAYS:
                return time.plusDays(1);
            case MONTHS:
                return time.plusMonths(1);
            case YEARS:
                return time.plusYears(1);
            default:
                throw new RuntimeException("Failed to parse partitioning property!");
        }
    }

    public static Optional<SqlTsPartitionDate> parse(String name) {
        SqlTsPartitionDate partition = null;
        if (name != null) {
            for (SqlTsPartitionDate partitionDate : SqlTsPartitionDate.values()) {
                if (partitionDate.name().equalsIgnoreCase(name)) {
                    partition = partitionDate;
                    break;
                }
            }
        }
        return Optional.ofNullable(partition);
    }
}