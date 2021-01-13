/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.entityconfig;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.EntityConfig;
import org.thingsboard.server.dao.model.sql.EntityConfigEntity;
import org.thingsboard.server.dao.model.sql.EntityViewEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EntityConfigRepository extends PagingAndSortingRepository<EntityConfigEntity, UUID> {
/*
    @Query("SELECT e FROM EntityConfigEntity WHERE e.tenantId = :tenantId and e.entityId = entityId ORDER BY e.createdTime DESC LIMIT 1")
    EntityConfigEntity findLatestEntityConfigByEntityId(@Param("tenantId") UUID tenantId, @Param("entityId") UUID entityId);

 */
    Optional<EntityConfigEntity> findFirstByTenantIdAndEntityIdOrderByCreatedTimeDesc(UUID tenantId, UUID entityId);

    @Query("SELECT c FROM EntityConfigEntity c " +
            "WHERE c.tenantId = :tenantId " +
            "AND c.entityId = :entityId")
    Page<EntityConfigEntity> findEntityConfigs(@Param("tenantId") UUID tenantId,
                                               @Param("entityId") UUID entityId,
                                               Pageable pageable);

    void deleteByTenantIdAndEntityId(@Param("tenantId") UUID tenantId,
                                     @Param("entityId") UUID entityId);
}
