/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.stats;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.dao.model.sql.EntityStatisticsEntity;

import java.util.UUID;

@Repository
public interface EntityStatisticsRepository extends JpaRepository<EntityStatisticsEntity, EntityStatisticsEntity.CompositeKey> {

    EntityStatisticsEntity findByEntityIdAndEntityType(UUID entityId, EntityType entityType);

    @Query(value = "SELECT count(*) FROM entity_statistics WHERE tenant_id = :tenantId " +
            "AND ts >= :startTs AND ts <= :endTs " +
            "AND latest_value ->> :property = :value", nativeQuery = true)
    int countByTenantIdAndTsBetweenAndLatestValueProperty(@Param("tenantId") UUID tenantId,
                                                          @Param("startTs") long startTs,
                                                          @Param("endTs") long endTs,
                                                          @Param("property") String property,
                                                          @Param("value") String value);

    @Query(value = "SELECT count(*) FROM entity_statistics WHERE " +
            "ts >= :startTs AND ts <= :endTs " +
            "AND latest_value ->> :property = :value", nativeQuery = true)
    int countByTsBetweenAndLatestValueProperty(@Param("startTs") long startTs,
                                               @Param("endTs") long endTs,
                                               @Param("property") String property,
                                               @Param("value") String value);

    Page<EntityStatisticsEntity> findByTenantIdAndEntityType(UUID tenantId, EntityType entityType, Pageable pageable);

    Page<EntityStatisticsEntity> findByEntityType(EntityType entityType, Pageable pageable);

    @Transactional
    void deleteByTsBefore(long ts);

}
