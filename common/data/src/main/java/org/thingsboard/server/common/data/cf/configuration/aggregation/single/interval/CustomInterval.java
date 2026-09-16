// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.ZonedDateTime;

@Schema
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class CustomInterval extends BaseAggInterval {

    @NotNull
    @Min(1)
    private Long durationSec;

    public CustomInterval(String tz, Long offsetSec, Long durationSec) {
        super(tz, offsetSec);
        this.durationSec = durationSec;
    }

    @Override
    public AggIntervalType getType() {
        return AggIntervalType.CUSTOM;
    }

    @Override
    public long getCurrentIntervalDurationMillis() {
        return getDurationMillis();
    }

    private long getDurationMillis() {
        return Duration.ofSeconds(durationSec).toMillis();
    }

    @Override
    protected ZonedDateTime alignToIntervalStart(ZonedDateTime reference) {
        ZonedDateTime localMidnight = reference.toLocalDate().atStartOfDay(reference.getZone());
        long secondsFromMidnight = Duration.between(localMidnight, reference).getSeconds();
        long alignedSecondsFromMidnight = (secondsFromMidnight / durationSec) * durationSec;
        return localMidnight.plusSeconds(alignedSecondsFromMidnight);
    }

    @Override
    public ZonedDateTime getNextIntervalStart(ZonedDateTime currentStart) {
        return currentStart.plusSeconds(durationSec);
    }

}
