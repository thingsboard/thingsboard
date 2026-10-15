// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

@Schema
@Data
@NoArgsConstructor
public class MonthInterval extends BaseAggInterval {

    @Override
    public AggIntervalType getType() {
        return AggIntervalType.MONTH;
    }

    public MonthInterval(String tz, Long offsetSec) {
        super(tz, offsetSec);
    }

    @Override
    protected ZonedDateTime alignToIntervalStart(ZonedDateTime reference) {
        return reference.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
    }

    @Override
    public ZonedDateTime getNextIntervalStart(ZonedDateTime currentStart) {
        return currentStart.plusMonths(1);
    }

}
