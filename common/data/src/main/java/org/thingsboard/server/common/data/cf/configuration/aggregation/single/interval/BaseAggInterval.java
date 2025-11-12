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

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
public abstract class BaseAggInterval implements AggInterval {

    @NotBlank
    protected String tz;
    protected Long offsetSec; // delay seconds since start of interval

    @Override
    public ZoneId getZoneId() {
        return ZoneId.of(tz);
    }

    protected long getOffset() {
        return offsetSec != null ? offsetSec : 0L;
    }

    @Override
    public long getCurrentIntervalDurationMillis() {
        return getCurrentIntervalEndTs() - getCurrentIntervalStartTs();
    }

    @Override
    public long getCurrentIntervalStartTs() {
        ZoneId zoneId = getZoneId();
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        return getDateTimeIntervalStartTs(now);
    }

    @Override
    public long getDateTimeIntervalStartTs(ZonedDateTime dateTime) {
        long offset = getOffset();
        ZonedDateTime shiftedNow = dateTime.minusSeconds(offset);
        ZonedDateTime alignedStart = getAlignedBoundary(shiftedNow, false);
        ZonedDateTime actualStart = alignedStart.plusSeconds(offset);
        return actualStart.toInstant().toEpochMilli();
    }

    @Override
    public long getCurrentIntervalEndTs() {
        ZoneId zoneId = getZoneId();
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        return getDateTimeIntervalEndTs(now);
    }

    @Override
    public long getDateTimeIntervalEndTs(ZonedDateTime dateTime) {
        long offset = getOffset();
        ZonedDateTime shiftedNow = dateTime.minusSeconds(offset);
        ZonedDateTime alignedEnd = getAlignedBoundary(shiftedNow, true);
        ZonedDateTime actualEnd = alignedEnd.plusSeconds(offset);
        return actualEnd.toInstant().toEpochMilli();
    }

    protected abstract ZonedDateTime getAlignedBoundary(ZonedDateTime reference, boolean next);

}
