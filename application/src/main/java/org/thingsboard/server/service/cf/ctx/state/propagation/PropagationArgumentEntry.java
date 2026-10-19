// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cf.ctx.state.propagation;

import lombok.Data;
import org.thingsboard.script.api.tbel.TbelCfArg;
import org.thingsboard.script.api.tbel.TbelCfPropagationArg;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntryType;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;
import org.thingsboard.server.service.cf.ctx.state.HasEntityLimit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class PropagationArgumentEntry implements ArgumentEntry, HasEntityLimit {

    private Set<EntityId> entityIds;
    private transient List<EntityId> added;
    private transient EntityId removed;

    private transient boolean forceResetPrevious;
    private transient boolean syncWithDb;

    public PropagationArgumentEntry() {
        this.entityIds = new HashSet<>();
        this.added = null;
        this.removed = null;
    }

    public PropagationArgumentEntry(List<EntityId> entityIds) {
        this.entityIds = new HashSet<>(entityIds);
    }

    @Override
    public ArgumentEntryType getType() {
        return ArgumentEntryType.PROPAGATION;
    }

    @Override
    public Object getValue() {
        return entityIds;
    }

    @Override
    public boolean updateEntry(ArgumentEntry entry, CalculatedFieldCtx ctx) {
        if (!(entry instanceof PropagationArgumentEntry updated)) {
            throw new IllegalArgumentException("Unsupported argument entry type for propagation argument entry: " + entry.getType());
        }
        if (updated.getAdded() != null) {
            return checkAdded(updated.getAdded(), ctx);
        }
        if (updated.getRemoved() != null) {
            return entityIds.remove(updated.getRemoved());
        }
        if (updated.isSyncWithDb()) {
            Set<EntityId> dbEntityIds = updated.getEntityIds();
            if (dbEntityIds.isEmpty()) {
                if (entityIds.isEmpty()) {
                    return false;
                }
                entityIds.clear();
                return true;
            }
            boolean retained = entityIds.retainAll(dbEntityIds);
            boolean added = checkAdded(dbEntityIds, ctx);
            return retained || added;
        }
        if (updated.isEmpty()) {
            entityIds.clear();
            return true;
        }
        entityIds = updated.getEntityIds();
        return true;
    }

    private boolean checkAdded(Collection<EntityId> updatedIds, CalculatedFieldCtx ctx) {
        for (EntityId id : updatedIds) {
            checkEntityLimit(entityIds.size(), ctx);
            if (entityIds.add(id)) {
                if (added == null) {
                    added = new ArrayList<>();
                }
                added.add(id);
            }
        }
        return added != null && !added.isEmpty();
    }

    @Override
    public boolean isEmpty() {
        return entityIds.isEmpty();
    }

    @Override
    public TbelCfArg toTbelCfArg() {
        return new TbelCfPropagationArg(entityIds);
    }

}
