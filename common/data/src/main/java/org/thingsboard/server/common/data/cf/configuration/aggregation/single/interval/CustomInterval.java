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

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;

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
    protected ZonedDateTime getAlignedBoundary(ZonedDateTime reference, boolean next) {
        ZonedDateTime localMidnight = reference.toLocalDate().atStartOfDay(reference.getZone());
        long secondsFromMidnight = Duration.between(localMidnight, reference).getSeconds();
        long alignedSecondsFromMidnight = (secondsFromMidnight / durationSec) * durationSec;
        ZonedDateTime aligned = localMidnight.plusSeconds(alignedSecondsFromMidnight);
        return next ? aligned.plusSeconds(durationSec) : aligned;
    }

    @Override
    public ZonedDateTime getNextIntervalStart(ZonedDateTime currentStart) {
        return currentStart.plusSeconds(durationSec);
    }

}
