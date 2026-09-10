// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.timeseries;

import lombok.Getter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Getter
public enum NoSqlTsPartitionDate {

    MINUTES("yyyy-MM-dd-HH-mm", ChronoUnit.MINUTES), HOURS("yyyy-MM-dd-HH", ChronoUnit.HOURS), DAYS("yyyy-MM-dd", ChronoUnit.DAYS), MONTHS("yyyy-MM", ChronoUnit.MONTHS), YEARS("yyyy", ChronoUnit.YEARS),INDEFINITE("",ChronoUnit.FOREVER);

    private final String pattern;
    private final transient TemporalUnit truncateUnit;
    private final transient long durationMs;
    public final static LocalDateTime EPOCH_START = LocalDateTime.ofEpochSecond(0,0, ZoneOffset.UTC);

    NoSqlTsPartitionDate(String pattern, TemporalUnit truncateUnit) {
        this.pattern = pattern;
        this.truncateUnit = truncateUnit;
        this.durationMs = TimeUnit.SECONDS.toMillis(this.truncateUnit.getDuration().getSeconds());
    }

    public LocalDateTime truncatedTo(LocalDateTime time) {
        switch (this){
            case MONTHS:
                return time.truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1);
            case YEARS:
                return time.truncatedTo(ChronoUnit.DAYS).withDayOfYear(1);
            case INDEFINITE:
                 return EPOCH_START;
            default:
                return time.truncatedTo(truncateUnit);
        }
    }

    public static Optional<NoSqlTsPartitionDate> parse(String name) {
        NoSqlTsPartitionDate partition = null;
        if (name != null) {
            for (NoSqlTsPartitionDate partitionDate : NoSqlTsPartitionDate.values()) {
                if (partitionDate.name().equalsIgnoreCase(name)) {
                    partition = partitionDate;
                    break;
                }
            }
        }
        return Optional.ofNullable(partition);
    }
}
