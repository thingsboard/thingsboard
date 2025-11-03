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
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class CustomInterval extends BaseAggInterval {

    private Long durationSec;

    public CustomInterval(Long durationSec, Long offsetMillis, String tz) {
        this.tz = tz;
        this.offsetSec = offsetMillis;
        this.durationSec = durationSec;
    }

    @Override
    public AggIntervalType getType() {
        return AggIntervalType.CUSTOM;
    }

    @Override
    public long getIntervalDurationMillis() {
        return Duration.ofSeconds(durationSec).toMillis();
    }

    @Override
    public long getCurrentIntervalStartTs() {
        ZoneId zoneId = ZoneId.of(tz);
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        ZonedDateTime shiftedNow = now.minusSeconds(offsetSec);

        long durationMillis = getIntervalDurationMillis();
        long shiftedNowMillis = shiftedNow.toInstant().toEpochMilli();
        long alignedStartMillis = (shiftedNowMillis / durationMillis) * durationMillis;

        long offsetMillis = TimeUnit.SECONDS.toMillis(offsetSec);
        return alignedStartMillis + offsetMillis;
    }

    @Override
    public long getCurrentIntervalEndTs() {
        return getCurrentIntervalStartTs() + getIntervalDurationMillis();
    }

    @Override
    public long getDelayUntilIntervalEnd() {
        return getCurrentIntervalEndTs() - System.currentTimeMillis();
    }

}
