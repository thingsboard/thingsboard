// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
