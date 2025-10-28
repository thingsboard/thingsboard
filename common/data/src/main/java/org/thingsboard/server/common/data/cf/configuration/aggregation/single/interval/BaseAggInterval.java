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

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public abstract class BaseAggInterval implements AggInterval {

    protected long offsetMillis; // delay millis since start of interval

    @Override
    public long getDelayUntilIntervalEnd() {
        return getDelayUntilIntervalEnd(getType(), 1);
    }

    protected long getDelayUntilIntervalEnd(AggIntervalType type, long multiplier) {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime next;

        switch (getType()) {
            case HOUR -> next = now.plusHours(multiplier).truncatedTo(ChronoUnit.HOURS);
            case DAY -> next = now.plusDays(multiplier).truncatedTo(ChronoUnit.DAYS);
            case WEEK -> next = now.plusWeeks(multiplier).with(DayOfWeek.MONDAY).truncatedTo(ChronoUnit.DAYS);
            case WEEK_SUN_SAT -> next = now.plusWeeks(multiplier).with(DayOfWeek.SUNDAY).truncatedTo(ChronoUnit.DAYS);
            case MONTH -> next = now.plusMonths(multiplier).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
            case YEAR -> next = now.plusYears(multiplier).withDayOfYear(1).truncatedTo(ChronoUnit.DAYS);
            default -> throw new IllegalArgumentException("Unsupported type: " + getType());
        }

        long delayMillis = Duration.between(now, next).toMillis();
        if (offsetMillis > 0) {
            delayMillis += offsetMillis;
        }

        return Math.max(delayMillis, 0);
    }

}
