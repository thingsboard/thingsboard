// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

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

    protected long getOffsetSafe() {
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
        long offset = getOffsetSafe();
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
        long offset = getOffsetSafe();
        ZonedDateTime shiftedNow = dateTime.minusSeconds(offset);
        ZonedDateTime alignedEnd = getAlignedBoundary(shiftedNow, true);
        ZonedDateTime actualEnd = alignedEnd.plusSeconds(offset);
        return actualEnd.toInstant().toEpochMilli();
    }

    protected abstract ZonedDateTime alignToIntervalStart(ZonedDateTime reference);

    protected ZonedDateTime getAlignedBoundary(ZonedDateTime reference, boolean next) {
        ZonedDateTime base = alignToIntervalStart(reference);
        return next ? getNextIntervalStart(base) : base;
    }

    @Override
    public void validate() {
        try {
            getZoneId();
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid timezone in interval: " + ex.getMessage());
        }
        if (offsetSec != null) {
            if (offsetSec < 0) {
                throw new IllegalArgumentException("Offset cannot be negative.");
            }
            if (TimeUnit.SECONDS.toMillis(offsetSec) >= getCurrentIntervalDurationMillis()) {
                throw new IllegalArgumentException("Offset must be greater than interval duration.");
            }
        }
    }

}
