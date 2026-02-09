/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.edqs.data;

import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@NoArgsConstructor
public class RelationsRepo {

    private final ConcurrentMap<UUID, Set<RelationInfo>> fromRelations = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Set<RelationInfo>> toRelations = new ConcurrentHashMap<>();

    public boolean add(EntityData<?> from, EntityData<?> to, String type) {
        boolean addedFromRelation = fromRelations.computeIfAbsent(from.getId(), k -> ConcurrentHashMap.newKeySet()).add(new RelationInfo(type, to));
        boolean addedToRelation = toRelations.computeIfAbsent(to.getId(), k -> ConcurrentHashMap.newKeySet()).add(new RelationInfo(type, from));
        return addedFromRelation || addedToRelation;
    }

    public Set<RelationInfo> getFrom(UUID entityId) {
        var result = fromRelations.get(entityId);
        return result == null ? Collections.emptySet() : result;
    }

    public Set<RelationInfo> getTo(UUID entityId) {
        var result = toRelations.get(entityId);
        return result == null ? Collections.emptySet() : result;
    }

    public boolean remove(UUID from, UUID to, String type) {
        boolean removedFromRelation = false;
        boolean removedToRelation = false;
        Set<RelationInfo> fromRelations = this.fromRelations.get(from);
        if (fromRelations != null) {
            removedFromRelation = fromRelations.removeIf(relationInfo -> relationInfo.getTarget().getId().equals(to) && relationInfo.getType().equals(type));
        }
        Set<RelationInfo> toRelations = this.toRelations.get(to);
        if (toRelations != null) {
            removedToRelation = toRelations.removeIf(relationInfo -> relationInfo.getTarget().getId().equals(from) && relationInfo.getType().equals(type));
        }
        return removedFromRelation || removedToRelation;
    }

}
