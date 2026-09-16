// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edqs.data;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edqs.fields.CustomerFields;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CustomerData extends BaseEntityData<CustomerFields> {

    private final ConcurrentMap<EntityType, ConcurrentMap<UUID, EntityData<?>>> entitiesById = new ConcurrentHashMap<>();

    public CustomerData(UUID entityId) {
        super(entityId);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.CUSTOMER;
    }

    public Collection<EntityData<?>> getEntities(EntityType entityType) {
        var map = entitiesById.get(entityType);
        if (map == null) {
            return Collections.emptyList();
        } else {
            return map.values();
        }
    }

    public void addOrUpdate(EntityData<?> ed) {
        entitiesById.computeIfAbsent(ed.getEntityType(), et -> new ConcurrentHashMap<>()).put(ed.getId(), ed);
    }

    public boolean remove(EntityType entityType, UUID entityId) {
        var map = entitiesById.get(entityType);
        if (map != null) {
            return map.remove(entityId) != null;
        } else {
            return false;
        }
    }

}
