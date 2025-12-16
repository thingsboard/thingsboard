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
package org.thingsboard.server.service.cf.ctx.state.propagation;

import lombok.Data;
import org.thingsboard.script.api.tbel.TbelCfArg;
import org.thingsboard.script.api.tbel.TbelCfPropagationArg;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntryType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class PropagationArgumentEntry implements ArgumentEntry {

    private Set<EntityId> entityIds;
    private transient List<EntityId> added;
    private transient EntityId removed;

    private transient boolean forceResetPrevious;
    private transient boolean ignoreRemovedEntities;

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
    public boolean updateEntry(ArgumentEntry entry) {
        if (!(entry instanceof PropagationArgumentEntry updated)) {
            throw new IllegalArgumentException("Unsupported argument entry type for propagation argument entry: " + entry.getType());
        }
        if (updated.getAdded() != null) {
            return checkAdded(updated.getAdded());
        }
        if (updated.getRemoved() != null) {
            return entityIds.remove(updated.getRemoved());
        }
        if (updated.isIgnoreRemovedEntities()) {
            Set<EntityId> updatedIds = updated.getEntityIds();
            if (updatedIds.isEmpty()) {
                entityIds.clear();
                return false;
            }
            entityIds.retainAll(updatedIds);
            return checkAdded(updatedIds);
        }
        if (updated.isEmpty()) {
            entityIds.clear();
            return true;
        }
        entityIds = updated.getEntityIds();
        return true;
    }

    private boolean checkAdded(Collection<EntityId> updatedIds) {
        for (EntityId id : updatedIds) {
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
