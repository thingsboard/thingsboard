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
package org.thingsboard.server.service.cf.ctx.state.aggregation;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.script.api.tbel.TbelCfArg;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntryType;

import java.util.Map;

@Data
@AllArgsConstructor
public class AggArgumentEntry implements ArgumentEntry {

    private final Map<EntityId, ArgumentEntry> aggInputs;

    private boolean forceResetPrevious;

    @Override
    public ArgumentEntryType getType() {
        return ArgumentEntryType.AGGREGATE_LATEST;
    }

    @Override
    public Object getValue() {
        return aggInputs;
    }

    @Override
    public boolean updateEntry(ArgumentEntry entry) {
        if (entry instanceof AggArgumentEntry aggArgumentEntry) {
            aggInputs.putAll(aggArgumentEntry.aggInputs);
            return true;
        } else if (entry instanceof AggSingleArgumentEntry aggSingleArgumentEntry) {
            if (aggSingleArgumentEntry.isDeleted()) {
                aggInputs.remove(aggSingleArgumentEntry.getEntityId());
            } else {
                aggInputs.put(aggSingleArgumentEntry.getEntityId(), aggSingleArgumentEntry);
            }
            return true;
        } else {
            throw new IllegalArgumentException("Unsupported argument entry type for aggregation argument entry: " + entry.getType());
        }
    }

    @Override
    public boolean isEmpty() {
        return aggInputs.isEmpty();
    }

    @Override
    public TbelCfArg toTbelCfArg() {
        return null;
    }

}
