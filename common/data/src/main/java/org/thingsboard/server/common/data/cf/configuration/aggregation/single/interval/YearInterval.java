// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;

@Schema
@Data
@NoArgsConstructor
public class YearInterval extends BaseAggInterval {

    @Override
    public AggIntervalType getType() {
        return AggIntervalType.YEAR;
    }

    public YearInterval(String tz, Long offsetSec) {
        super(tz, offsetSec);
    }

    @Override
    protected ZonedDateTime alignToIntervalStart(ZonedDateTime reference) {
        return ZonedDateTime.of(
                LocalDate.of(reference.getYear(), 1, 1),
                LocalTime.MIDNIGHT,
                reference.getZone());
    }

    @Override
    public ZonedDateTime getNextIntervalStart(ZonedDateTime currentStart) {
        return currentStart.plusYears(1);
    }

}
