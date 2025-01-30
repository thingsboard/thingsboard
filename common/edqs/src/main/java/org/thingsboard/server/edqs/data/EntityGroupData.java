/**
 * Copyright © 2016-2024 ThingsBoard, Inc.
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

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edqs.fields.EntityGroupFields;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class EntityGroupData extends BaseEntityData<EntityGroupFields> {

    private final ConcurrentMap<UUID, EntityData<?>> entitiesById = new ConcurrentHashMap<>();

    public EntityGroupData(UUID entityId) {
        super(entityId);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ENTITY_GROUP;
    }

    public Collection<EntityData<?>> getEntities() {
        return entitiesById.values();
    }

    public boolean addOrUpdate(EntityData<?> ed) {
        return entitiesById.put(ed.getId(), ed) == null;
    }

    public boolean remove(EntityData<?> ed) {
        return entitiesById.remove(ed.getId()) != null;
    }

    public EntityData<?> getEntity(UUID entityId) {
        return entitiesById.get(entityId);
    }

    public boolean remove(UUID toId) {
        return entitiesById.remove(toId) != null;
    }
}
