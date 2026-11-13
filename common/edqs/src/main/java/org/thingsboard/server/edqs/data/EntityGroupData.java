// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
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

    public EntityData<?> getEntity(UUID entityId) {
        return entitiesById.get(entityId);
    }

    public boolean remove(UUID toId) {
        return entitiesById.remove(toId) != null;
    }

}
