// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.entity;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.dao.Dao;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@SuppressWarnings({"unchecked"})
public class EntityDaoRegistry {

    private final Map<EntityType, Dao<?>> daos = new EnumMap<>(EntityType.class);

    private EntityDaoRegistry(List<Dao<?>> daos) {
        daos.forEach(dao -> {
            EntityType entityType = dao.getEntityType();
            if (entityType != null) {
                this.daos.put(entityType, dao);
            }
        });
    }

    public <T> Dao<T> getDao(EntityType entityType) {
        Dao<T> dao = (Dao<T>) daos.get(entityType);
        if (dao == null) {
            throw new IllegalArgumentException("Missing dao for entity type " + entityType);
        }
        return dao;
    }

}
