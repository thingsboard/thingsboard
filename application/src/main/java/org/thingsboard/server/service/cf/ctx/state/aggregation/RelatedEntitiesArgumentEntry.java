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
import org.thingsboard.script.api.tbel.TbelCfRelatedEntitiesArgumentValue;
import org.thingsboard.script.api.tbel.TbelCfSingleValueArg;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntryType;
import org.thingsboard.server.service.cf.ctx.state.SingleValueArgumentEntry;

import java.util.Map;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class RelatedEntitiesArgumentEntry implements ArgumentEntry {

    private final Map<EntityId, ArgumentEntry> entityInputs;

    private boolean forceResetPrevious;

    @Override
    public ArgumentEntryType getType() {
        return ArgumentEntryType.RELATED_ENTITIES;
    }

    @Override
    public Object getValue() {
        return entityInputs;
    }

    @Override
    public boolean updateEntry(ArgumentEntry entry) {
        if (entry instanceof RelatedEntitiesArgumentEntry relatedEntitiesArgumentEntry) {
            entityInputs.putAll(relatedEntitiesArgumentEntry.entityInputs);
            return true;
        } else if (entry instanceof SingleValueArgumentEntry singleValueArgumentEntry) {
            if (entry.isForceResetPrevious()) {
                entityInputs.put(singleValueArgumentEntry.getEntityId(), singleValueArgumentEntry);
                return true;
            }
            ArgumentEntry argumentEntry = entityInputs.get(singleValueArgumentEntry.getEntityId());
            if (argumentEntry != null) {
                argumentEntry.updateEntry(singleValueArgumentEntry);
            } else {
                entityInputs.put(singleValueArgumentEntry.getEntityId(), singleValueArgumentEntry);
            }
            return true;
        } else {
            throw new IllegalArgumentException("Unsupported argument entry type for aggregation argument entry: " + entry.getType());
        }
    }

    @Override
    public boolean isEmpty() {
        return entityInputs.isEmpty();
    }

    @Override
    public TbelCfArg toTbelCfArg() {
        var inputs = entityInputs.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().getId(),
                        e -> (TbelCfSingleValueArg) e.getValue().toTbelCfArg()
                ));
        return new TbelCfRelatedEntitiesArgumentValue(inputs);
    }

}
