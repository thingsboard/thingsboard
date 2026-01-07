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
package org.thingsboard.server.service.cf.cache;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TenantEntityProfileCache {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<EntityId, Set<EntityId>> allEntities = new HashMap<>();

    public void removeProfileId(EntityId profileId) {
        lock.writeLock().lock();
        try {
            // Remove from allEntities
            allEntities.remove(profileId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void removeEntityId(EntityId entityId) {
        lock.writeLock().lock();
        try {
            // Remove from allEntities
            allEntities.values().forEach(set -> set.remove(entityId));
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void remove(EntityId profileId, EntityId entityId) {
        lock.writeLock().lock();
        try {
            // Remove from allEntities
            removeSafely(allEntities, profileId, entityId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void add(EntityId profileId, EntityId entityId) {
        lock.writeLock().lock();
        try {
            if (EntityType.DEVICE.equals(profileId.getEntityType()) || EntityType.ASSET.equals(profileId.getEntityType())) {
                throw new RuntimeException("Entity type '" + profileId.getEntityType() + "' is not a profileId.");
            }
            allEntities.computeIfAbsent(profileId, k -> new HashSet<>()).add(entityId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void update(EntityId oldProfileId, EntityId newProfileId, EntityId entityId) {
        remove(oldProfileId, entityId);
        add(newProfileId, entityId);
    }

    public Collection<EntityId> getEntityIdsByProfileId(EntityId profileId) {
        lock.readLock().lock();
        try {
            var entities = allEntities.getOrDefault(profileId, Collections.emptySet());
            List<EntityId> result = new ArrayList<>(entities.size());
            result.addAll(entities);
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    private void removeSafely(Map<EntityId, Set<EntityId>> map, EntityId profileId, EntityId entityId) {
        var set = map.get(profileId);
        if (set != null) {
            set.remove(entityId);
        }
    }
}
