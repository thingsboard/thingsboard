// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cf.ctx.state.aggregation;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.script.api.tbel.TbelCfArg;
import org.thingsboard.script.api.tbel.TbelCfRelatedEntitiesArgumentValue;
import org.thingsboard.script.api.tbel.TbelCfSingleValueArg;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntryType;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;
import org.thingsboard.server.service.cf.ctx.state.HasEntityLimit;
import org.thingsboard.server.service.cf.ctx.state.HasLatestTs;
import org.thingsboard.server.service.cf.ctx.state.SingleValueArgumentEntry;

import java.util.Map;
import java.util.stream.Collectors;

import static org.thingsboard.server.service.cf.ctx.state.BaseCalculatedFieldState.DEFAULT_LAST_UPDATE_TS;

@Data
@AllArgsConstructor
public class RelatedEntitiesArgumentEntry implements ArgumentEntry, HasLatestTs, HasEntityLimit {

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
    public long getLatestTs() {
        long latestTs = DEFAULT_LAST_UPDATE_TS;
        for (ArgumentEntry entry : entityInputs.values()) {
            if (entry instanceof SingleValueArgumentEntry single) {
                if (!single.isDefaultValue()) {
                    latestTs = Math.max(latestTs, single.getTs());
                }
            }
        }
        return latestTs;
    }

    @Override
    public boolean updateEntry(ArgumentEntry entry, CalculatedFieldCtx ctx) {
        if (entry instanceof RelatedEntitiesArgumentEntry relatedEntitiesArgumentEntry) {
            checkEntityLimit(entityInputs.size(), ctx);
            entityInputs.putAll(relatedEntitiesArgumentEntry.entityInputs);
        } else if (entry instanceof SingleValueArgumentEntry singleValueArgumentEntry) {
            if (entry.isForceResetPrevious()) {
                checkEntityLimit(entityInputs.size(), ctx);
                entityInputs.put(singleValueArgumentEntry.getEntityId(), singleValueArgumentEntry);
            } else {
                ArgumentEntry argumentEntry = entityInputs.get(singleValueArgumentEntry.getEntityId());
                if (argumentEntry != null) {
                    argumentEntry.updateEntry(singleValueArgumentEntry, ctx);
                } else {
                    checkEntityLimit(entityInputs.size(), ctx);
                    entityInputs.put(singleValueArgumentEntry.getEntityId(), singleValueArgumentEntry);
                }
            }
        } else {
            throw new IllegalArgumentException("Unsupported argument entry type for aggregation argument entry: " + entry.getType());
        }
        return true;
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
