/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.dao.entity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EntityDaoRegistry {

    private final Map<EntityType, Dao<?>> daos;

    private EntityDaoRegistry(List<Dao<?>> daos) {
        this.daos = daos.stream().filter(dao -> dao.getEntityType() != null)
                .collect(Collectors.toMap(Dao::getEntityType, dao -> dao));
    }

    @SuppressWarnings("unchecked")
    public <T> Dao<T> getDao(EntityType entityType) {
        Dao<T> dao = (Dao<T>) daos.get(entityType);
        if (dao == null) {
            throw new IllegalArgumentException("Missing dao for entity type " + entityType);
        }
        return dao;
    }

}
