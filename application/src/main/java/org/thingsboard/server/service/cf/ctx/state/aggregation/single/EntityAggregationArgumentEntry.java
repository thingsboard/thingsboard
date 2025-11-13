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
        boolean updated = false;
        if (entry instanceof EntityAggregationArgumentEntry entityAggEntry) {
            aggIntervals.putAll(entityAggEntry.getAggIntervals());
        } else if (entry instanceof SingleValueArgumentEntry singleValueArgEntry) {
            long entryTs = singleValueArgEntry.getTs();
            long argUpdateTs = System.currentTimeMillis();
            for (Map.Entry<AggIntervalEntry, AggIntervalEntryStatus> aggIntervalEntry : aggIntervals.entrySet()) {
                if (singleValueArgEntry.isForceResetPrevious()) {
                    aggIntervalEntry.getValue().setLastArgsRefreshTs(argUpdateTs);
                    updated = true;
                    continue;
                }
                if (aggIntervalEntry.getKey().belongsToInterval(entryTs)) {
                    aggIntervalEntry.getValue().setLastArgsRefreshTs(argUpdateTs);
                    return true;
                }
            }
        }
        return updated;
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
