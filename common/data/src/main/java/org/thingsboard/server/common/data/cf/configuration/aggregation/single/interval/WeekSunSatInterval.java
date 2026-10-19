// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

@Schema
@Data
@NoArgsConstructor
public class WeekSunSatInterval extends BaseAggInterval {

    @Override
    public AggIntervalType getType() {
        return AggIntervalType.WEEK_SUN_SAT;
    }

    public WeekSunSatInterval(String tz, Long offsetSec) {
        super(tz, offsetSec);
    }

    @Override
    protected ZonedDateTime alignToIntervalStart(ZonedDateTime reference) {
        return reference.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)).truncatedTo(ChronoUnit.DAYS);
    }

    @Override
    public ZonedDateTime getNextIntervalStart(ZonedDateTime currentStart) {
        return currentStart.plusWeeks(1);
    }

}
