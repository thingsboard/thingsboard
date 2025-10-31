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

import lombok.Data;
import org.thingsboard.script.api.tbel.TbelCfArg;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntryType;
import org.thingsboard.server.service.cf.ctx.state.SingleValueArgumentEntry;

import java.util.Map;

@Data
public class EntityAggregationArgumentEntry implements ArgumentEntry {

    private Map<AggIntervalEntry, AggIntervalEntryStatus> aggIntervals;

    private boolean forceResetPrevious;

    public EntityAggregationArgumentEntry(Map<AggIntervalEntry, AggIntervalEntryStatus> aggIntervals) {
        this.aggIntervals = aggIntervals;
    }

    @Override
    public ArgumentEntryType getType() {
        return ArgumentEntryType.ENTITY_AGGREGATION;
    }

    @Override
    public Object getValue() {
        return aggIntervals;
    }

    @Override
    public boolean updateEntry(ArgumentEntry entry) {
        if (entry instanceof EntityAggregationArgumentEntry entityAggEntry) {
            aggIntervals.putAll(entityAggEntry.getAggIntervals());
        } else if (entry instanceof SingleValueArgumentEntry singleValueArgEntry) {
            long entryTs = singleValueArgEntry.getTs();
            for (Map.Entry<AggIntervalEntry, AggIntervalEntryStatus> aggIntervalEntry : aggIntervals.entrySet()) {
                if (aggIntervalEntry.getKey().belongsToInterval(entryTs)) {
                    aggIntervalEntry.getValue().setLastArgsRefreshTs(System.currentTimeMillis());
                    aggIntervals.put(aggIntervalEntry.getKey(), aggIntervalEntry.getValue());
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public TbelCfArg toTbelCfArg() {
        return null;
    }

}
