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
package org.thingsboard.server.dao.sql;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;
import org.thingsboard.server.dao.util.SqlDao;

@SqlDao
@Slf4j
public abstract class JpaPartitionedAbstractDao<E extends BaseEntity<D>, D> extends JpaAbstractDao<E, D> {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private SqlPartitioningRepository partitioningRepository;

    @Override
    protected E doSave(E entity, boolean isNew) {
        createPartition(entity);
        if (getEntityType() == EntityType.NOTIFICATION) {
            log.trace("Created partition for entity {}. available partitions: {}", entity, partitioningRepository.fetchPartitions("notification"));
        }
        try {
            if (isNew) {
                entityManager.persist(entity);
            } else {
                entity = entityManager.merge(entity);
            }
        } catch (Throwable t) {
            if (getEntityType() == EntityType.NOTIFICATION) {
                log.trace("Failed to save {} (isNew {})", entity, isNew, t);
            }
            throw t;
        }
        return entity;
    }

    public abstract void createPartition(E entity);

}
