// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.util.TbPair;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
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

    @Override
    public List<TbPair<Long, Long>> getIntervalsBetween(long startTs, long endTs) {
        List<TbPair<Long, Long>> intervals = new ArrayList<>();

        ZonedDateTime startDateTime = Instant.ofEpochMilli(startTs).atZone(getZoneId());
        long startInterval = getDateTimeIntervalStartTs(startDateTime);
        long endTsInterval = getDateTimeIntervalEndTs(startDateTime);

        ZonedDateTime lastIntervalDateTime = Instant.ofEpochMilli(endTs).atZone(getZoneId());
        long lastIntervalEndTs = getDateTimeIntervalEndTs(lastIntervalDateTime);

        while (endTsInterval < lastIntervalEndTs) {
            intervals.add(new TbPair<>(startInterval, endTsInterval));

            startInterval = endTsInterval;
            ZonedDateTime nextIntervalStart = Instant.ofEpochMilli(endTsInterval).atZone(getZoneId());
            endTsInterval = getNextIntervalStart(nextIntervalStart).toInstant().toEpochMilli();
        }

        return intervals;
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
