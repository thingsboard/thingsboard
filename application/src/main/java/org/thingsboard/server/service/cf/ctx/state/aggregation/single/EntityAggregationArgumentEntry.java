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
package org.thingsboard.server.service.cf.ctx.state.aggregation.single;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.script.api.tbel.TbelCfArg;
import org.thingsboard.server.common.data.cf.configuration.aggregation.single.EntityAggregationCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval.AggInterval;
import org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval.Watermark;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntryType;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;
import org.thingsboard.server.service.cf.ctx.state.SingleValueArgumentEntry;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Data
public class EntityAggregationArgumentEntry implements ArgumentEntry {

    private Map<AggIntervalEntry, AggIntervalEntryStatus> aggIntervals;

    private boolean forceResetPrevious;

    private AggInterval interval;
    private long watermarkDuration;

    public EntityAggregationArgumentEntry(Map<AggIntervalEntry, AggIntervalEntryStatus> aggIntervals) {
        this.aggIntervals = aggIntervals;
    }

    public EntityAggregationArgumentEntry(Map<AggIntervalEntry, AggIntervalEntryStatus> aggIntervals, CalculatedFieldCtx ctx) {
        this(aggIntervals);
        setCtx(ctx);
    }

    @Override
    public ArgumentEntryType getType() {
        return ArgumentEntryType.ENTITY_AGGREGATION;
    }

    @Override
    public Object getValue() {
        return aggIntervals;
    }

    public void setCtx(CalculatedFieldCtx ctx) {
        var configuration = (EntityAggregationCalculatedFieldConfiguration) ctx.getCalculatedField().getConfiguration();
        interval = configuration.getInterval();
        Watermark watermark = configuration.getWatermark();
        watermarkDuration = watermark == null ? 0 : TimeUnit.SECONDS.toMillis(watermark.getDuration());
    }

    @Override
    public boolean updateEntry(ArgumentEntry entry) {
        if (entry instanceof EntityAggregationArgumentEntry entityAggEntry) {
            aggIntervals.putAll(entityAggEntry.getAggIntervals());
            return true;
        } else if (entry instanceof SingleValueArgumentEntry singleValueArgEntry) {
            long entryTs = singleValueArgEntry.getTs();
            long now = System.currentTimeMillis();
            if (updateExistingIntervals(singleValueArgEntry, entryTs, now)) {
                return true;
            }
            return createNewInterval(entryTs, now);
        }
        return false;
    }

    private boolean updateExistingIntervals(SingleValueArgumentEntry entry, long entryTs, long now) {
        boolean updated = false;

        for (Map.Entry<AggIntervalEntry, AggIntervalEntryStatus> aggIntervalEntry : aggIntervals.entrySet()) {
            AggIntervalEntry interval = aggIntervalEntry.getKey();
            AggIntervalEntryStatus status = aggIntervalEntry.getValue();
            if (entry.isForceResetPrevious()) {
                status.setLastArgsRefreshTs(now);
                updated = true;
                continue;
            }
            if (interval.belongsToInterval(entryTs)) {
                status.setLastArgsRefreshTs(now);
                return true;
            }
        }

        return updated;
    }

    private boolean createNewInterval(long entryTs, long now) {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(entryTs), interval.getZoneId());

        long startTs = interval.getDateTimeIntervalStartTs(zdt);
        long endTs = interval.getDateTimeIntervalEndTs(zdt);

        if (now - endTs > watermarkDuration) {
            return false;
        }

        AggIntervalEntry newInterval = new AggIntervalEntry(startTs, endTs);
        aggIntervals.computeIfAbsent(newInterval, i -> new AggIntervalEntryStatus(now));
        return true;
    }

    @Override
    public boolean isEmpty() {
        return aggIntervals.isEmpty();
    }

    @Override
    public JsonNode jsonValue() {
        return JacksonUtil.valueToTree(aggIntervals);
    }

    @Override
    public TbelCfArg toTbelCfArg() {
        return null;
    }

}
