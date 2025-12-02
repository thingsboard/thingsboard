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
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

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
