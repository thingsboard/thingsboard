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
package org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class BaseAggInterval implements AggInterval {

    @NotBlank
    protected String tz;
    protected Long offsetSec; // delay seconds since start of interval

    @JsonIgnore
    protected long getOffsetSec() {
        return offsetSec != null ? offsetSec : 0L;
    }

    @Override
    public long getIntervalDurationMillis() {
        return switch (getType()) {
            case HOUR -> Duration.ofHours(1).toMillis();
            case DAY -> Duration.ofDays(1).toMillis();
            case WEEK, WEEK_SUN_SAT -> Duration.ofDays(7L).toMillis();
            case MONTH -> Duration.ofDays(Math.round(30)).toMillis(); // average
            case QUARTER -> Duration.ofDays(Math.round(91)).toMillis();
            case YEAR -> Duration.ofDays(Math.round(365)).toMillis();
            default -> throw new IllegalArgumentException("Unsupported type: " + getType());
        };
    }

    @Override
    public long getCurrentIntervalStartTs() {
        ZoneId zoneId = ZoneId.of(tz);
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        long offset = getOffsetSec();
        ZonedDateTime shiftedNow = now.minusSeconds(offset);
        ZonedDateTime alignedStart = getAlignedBoundary(shiftedNow, false);
        ZonedDateTime actualStart = alignedStart.plusSeconds(offset);
        return actualStart.toInstant().toEpochMilli();
    }

    @Override
    public long getCurrentIntervalEndTs() {
        ZoneId zoneId = ZoneId.of(tz);
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        long offset = getOffsetSec();
        ZonedDateTime shiftedNow = now.minusSeconds(offset);
        ZonedDateTime alignedEnd = getAlignedBoundary(shiftedNow, true);
        ZonedDateTime actualEnd = alignedEnd.plusSeconds(offset);
        return actualEnd.toInstant().toEpochMilli();
    }

    @Override
    public long getDelayUntilIntervalEnd() {
        long currentIntervalEndTs = getCurrentIntervalEndTs();
        long now = System.currentTimeMillis();
        return currentIntervalEndTs - now;
    }

    protected ZonedDateTime getAlignedBoundary(ZonedDateTime reference, boolean next) {
        return switch (getType()) {
            case HOUR -> alignByHours(reference, next);
            case DAY -> alignByDays(reference, next);
            case WEEK -> alignByWeeks(reference, DayOfWeek.MONDAY, next);
            case WEEK_SUN_SAT -> alignByWeeks(reference, DayOfWeek.SUNDAY, next);
            case MONTH -> alignByMonths(reference, next);
            case QUARTER -> alignByQuarters(reference, next);
            case YEAR -> alignByYears(reference, next);
            default -> throw new IllegalArgumentException("Unsupported interval type: " + getType());
        };
    }

    private ZonedDateTime alignByHours(ZonedDateTime now, boolean next) {
        ZonedDateTime base = now.truncatedTo(ChronoUnit.HOURS);
        return next ? base.plusHours(1) : base;
    }

    private ZonedDateTime alignByDays(ZonedDateTime now, boolean next) {
        ZonedDateTime base = now.truncatedTo(ChronoUnit.DAYS);
        return next ? base.plusDays(1) : base;
    }

    private ZonedDateTime alignByWeeks(ZonedDateTime now, DayOfWeek startOfWeek, boolean next) {
        ZonedDateTime startOfWeekDate = now.with(TemporalAdjusters.previousOrSame(startOfWeek))
                .truncatedTo(ChronoUnit.DAYS);
        return next ? startOfWeekDate.plusWeeks(1) : startOfWeekDate;
    }

    private ZonedDateTime alignByMonths(ZonedDateTime now, boolean next) {
        ZonedDateTime base = now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
        return next ? base.plusMonths(1) : base;
    }

    private ZonedDateTime alignByQuarters(ZonedDateTime now, boolean next) {
        int month = now.getMonthValue();
        int quarterStartMonth = ((month - 1) / 3) * 3 + 1; // 1, 4, 7, 10
        ZonedDateTime base = ZonedDateTime.of(
                LocalDate.of(now.getYear(), quarterStartMonth, 1),
                LocalTime.MIDNIGHT,
                now.getZone());
        return next ? base.plusMonths(3) : base;
    }

    private ZonedDateTime alignByYears(ZonedDateTime now, boolean next) {
        ZonedDateTime base = ZonedDateTime.of(
                LocalDate.of(now.getYear(), 1, 1),
                LocalTime.MIDNIGHT,
                now.getZone());
        return next ? base.plusYears(1) : base;
    }

}
