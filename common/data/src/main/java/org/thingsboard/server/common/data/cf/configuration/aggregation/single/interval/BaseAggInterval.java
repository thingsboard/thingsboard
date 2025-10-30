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

import lombok.Data;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

@Data
public abstract class BaseAggInterval implements AggInterval {

    protected long offsetMillis; // delay millis since start of interval

    @Override
    public long getIntervalDurationMillis() {
        return getIntervalDurationMillis(getType(), 1);
    }

    public long getIntervalDurationMillis(AggIntervalType type, int multiplier) {
        return switch (type) {
            case MIN -> Duration.ofMinutes(multiplier).toMillis();
            case HOUR -> Duration.ofHours(multiplier).toMillis();
            case DAY -> Duration.ofDays(multiplier).toMillis();
            case WEEK, WEEK_SUN_SAT -> Duration.ofDays(7L * multiplier).toMillis();
            case MONTH -> Duration.ofDays(Math.round(30 * multiplier)).toMillis(); // average
            case YEAR -> Duration.ofDays(Math.round(365 * multiplier)).toMillis();
            default -> throw new IllegalArgumentException("Unsupported type: " + type);
        };
    }

    @Override
    public long getCurrentIntervalStartTs() {
        return getCurrentIntervalStartTs(getType(), 1);
    }

    protected long getCurrentIntervalStartTs(AggIntervalType type, int multiplier) {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime shiftedNow = now.minus(Duration.ofMillis(offsetMillis));
        ZonedDateTime alignedStart = getAlignedBoundary(type, multiplier, false, shiftedNow);
        ZonedDateTime actualStart = alignedStart.plus(Duration.ofMillis(offsetMillis));
        return actualStart.toInstant().toEpochMilli();
    }

    @Override
    public long getCurrentIntervalEndTs() {
        return getCurrentIntervalEndTs(getType(), 1);
    }

    protected long getCurrentIntervalEndTs(AggIntervalType type, int multiplier) {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime shiftedNow = now.minus(Duration.ofMillis(offsetMillis));
        ZonedDateTime alignedEnd = getAlignedBoundary(type, multiplier, true, shiftedNow);
        ZonedDateTime actualEnd = alignedEnd.plus(Duration.ofMillis(offsetMillis));
        return actualEnd.toInstant().toEpochMilli();
    }

    @Override
    public long getDelayUntilIntervalEnd() {
        return getDelayUntilIntervalEnd(getType(), 1);
    }

    protected long getDelayUntilIntervalEnd(AggIntervalType type, int multiplier) {
        long currentIntervalEndTs = getCurrentIntervalEndTs(type, multiplier);
        long now = System.currentTimeMillis();
        return currentIntervalEndTs - now;
    }

    protected ZonedDateTime getAlignedBoundary(AggIntervalType type, int multiplier, boolean next, ZonedDateTime reference) {
        return switch (type) {
            case MIN -> alignByMin(reference, multiplier, next);
            case HOUR -> alignByHour(reference, multiplier, next);
            case DAY -> alignByDay(reference, multiplier, next);
            case WEEK -> alignByWeek(reference, multiplier, DayOfWeek.MONDAY, next);
            case WEEK_SUN_SAT -> alignByWeek(reference, multiplier, DayOfWeek.SUNDAY, next);
            case MONTH -> alignByMonth(reference, multiplier, next);
            case YEAR -> alignByYear(reference, multiplier, next);
            default -> throw new IllegalArgumentException("Unsupported type: " + type);
        };
    }

    private ZonedDateTime alignByMin(ZonedDateTime now, int multiplier, boolean next) {
        ZonedDateTime startOfHour = now.truncatedTo(ChronoUnit.HOURS);
        long minsSinceHour = Duration.between(startOfHour, now).toHours();
        long aligned = (minsSinceHour / multiplier) * multiplier;
        if (next) aligned += multiplier;
        return startOfHour.plusMinutes(aligned);
    }

    private ZonedDateTime alignByHour(ZonedDateTime now, int multiplier, boolean next) {
        ZonedDateTime startOfDay = now.truncatedTo(ChronoUnit.DAYS);
        long hoursSinceMidnight = Duration.between(startOfDay, now).toHours();
        long aligned = (hoursSinceMidnight / multiplier) * multiplier;
        if (next) aligned += multiplier;
        return startOfDay.plusHours(aligned);
    }

    private ZonedDateTime alignByDay(ZonedDateTime now, int multiplier, boolean next) {
        long daysSinceEpoch = now.toLocalDate().toEpochDay();
        long aligned = (daysSinceEpoch / multiplier) * multiplier;
        if (next) aligned += multiplier;
        long diff = aligned - daysSinceEpoch;
        return now.truncatedTo(ChronoUnit.DAYS).plusDays(diff);
    }

    private ZonedDateTime alignByWeek(ZonedDateTime now, int multiplier, DayOfWeek startOfWeekDay, boolean next) {
        ZonedDateTime startOfWeek = now.with(TemporalAdjusters.previousOrSame(startOfWeekDay))
                .truncatedTo(ChronoUnit.DAYS);
        long weeksSinceEpoch = ChronoUnit.WEEKS.between(
                ZonedDateTime.ofInstant(Instant.EPOCH, now.getZone()), startOfWeek);
        long aligned = (weeksSinceEpoch / multiplier) * multiplier;
        if (next) aligned += multiplier;
        return startOfWeek.plusWeeks(aligned - weeksSinceEpoch);
    }

    private ZonedDateTime alignByMonth(ZonedDateTime now, int multiplier, boolean next) {
        ZonedDateTime startOfMonth = now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
        long monthsSinceEpoch = now.getYear() * 12L + now.getMonthValue() - 1;
        long aligned = (monthsSinceEpoch / multiplier) * multiplier;
        if (next) aligned += multiplier;
        return startOfMonth.plusMonths(aligned - monthsSinceEpoch);
    }

    private ZonedDateTime alignByYear(ZonedDateTime now, int multiplier, boolean next) {
        int year = now.getYear();
        int aligned = (year / multiplier) * multiplier;
        if (next) aligned += multiplier;
        return ZonedDateTime.of(LocalDate.of(aligned, 1, 1), LocalTime.MIDNIGHT, now.getZone());
    }

}
