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
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntryType;

import java.util.ArrayList;
import java.util.List;

@Data
public class PropagationArgumentEntry implements ArgumentEntry {

    private List<EntityId> propagationEntityIds;

    private boolean forceResetPrevious;

    public PropagationArgumentEntry(List<EntityId> propagationEntityIds) {
        this.propagationEntityIds = new ArrayList<>(propagationEntityIds);
    }

    @Override
    public ArgumentEntryType getType() {
        return ArgumentEntryType.PROPAGATION;
    }

    @Override
    public Object getValue() {
        return propagationEntityIds;
    }

    @Override
    public boolean updateEntry(ArgumentEntry entry) {
        if (!(entry instanceof PropagationArgumentEntry propagationArgumentEntry)) {
            throw new IllegalArgumentException("Unsupported argument entry type for propagation argument entry: " + entry.getType());
        }
        if (propagationArgumentEntry.isEmpty()) {
            propagationEntityIds.clear();
        } else {
            propagationEntityIds = propagationArgumentEntry.getPropagationEntityIds();
        }
        return true;
    }

    @Override
    public boolean isEmpty() {
        return CollectionsUtil.isEmpty(propagationEntityIds);
    }

    @Override
    public TbelCfArg toTbelCfArg() {
        return new TbelCfPropagationArg(propagationEntityIds);
    }

    public boolean addPropagationEntityId(EntityId propagationEntityId) {
        if (propagationEntityIds.contains(propagationEntityId)) {
            return false;
        }
        return propagationEntityIds.add(propagationEntityId);
    }

    public void removePropagationEntityId(EntityId relatedEntityId) {
        propagationEntityIds.remove(relatedEntityId);
    }

}
