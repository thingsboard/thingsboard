// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

@Schema
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class HourInterval extends BaseAggInterval {

    public HourInterval(String tz, Long offsetSec) {
        super(tz, offsetSec);
    }

    @Override
    public AggIntervalType getType() {
        return AggIntervalType.HOUR;
    }

    @Override
    protected ZonedDateTime alignToIntervalStart(ZonedDateTime reference) {
        return reference.truncatedTo(ChronoUnit.HOURS);
    }

    @Override
    public ZonedDateTime getNextIntervalStart(ZonedDateTime currentStart) {
        return currentStart.plusHours(1);
    }

}
