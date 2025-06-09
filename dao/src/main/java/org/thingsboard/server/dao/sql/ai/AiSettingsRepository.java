/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.ai;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sql.AiSettingsEntity;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface AiSettingsRepository extends JpaRepository<AiSettingsEntity, UUID> {

    @Query("SELECT ai " +
            "FROM AiSettingsEntity ai " +
            "WHERE ai.tenantId = :tenantId " +
            "AND (:textSearch IS NULL " +
            "OR ilike(ai.name, CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(ai.provider, CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(ai.model, CONCAT('%', :textSearch, '%')) = true)")
    Page<AiSettingsEntity> findByTenantId(@Param("tenantId") UUID tenantId, @Param("textSearch") String textSearch, Pageable pageable);

    Optional<AiSettingsEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    long countByTenantId(UUID tenantId);

    @Transactional
    @Modifying
    @Query("DELETE FROM AiSettingsEntity ai WHERE ai.id IN (:ids)")
    int deleteByIdIn(@Param("ids") Set<UUID> ids);

    @Transactional
    int deleteByTenantId(UUID tenantId);

    @Transactional
    @Modifying
    @Query("DELETE FROM AiSettingsEntity ai WHERE ai.tenantId = :tenantId AND ai.id IN (:ids)")
    int deleteByTenantIdAndIdIn(@Param("tenantId") UUID tenantId, @Param("ids") Set<UUID> ids);

}
