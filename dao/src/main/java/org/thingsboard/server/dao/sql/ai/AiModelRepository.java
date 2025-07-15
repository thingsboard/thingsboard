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
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.AiModelEntity;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

interface AiModelRepository extends JpaRepository<AiModelEntity, UUID>, ExportableEntityRepository<AiModelEntity> {

    Optional<AiModelEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<AiModelEntity> findByTenantIdAndName(UUID tenantId, String name);

    @Query(
            value = """
                    SELECT *
                    FROM ai_model model
                    WHERE model.tenant_id = :tenantId
                      AND (:textSearch IS NULL
                        OR model.name ILIKE '%' || :textSearch || '%'
                        OR REPLACE(model.configuration ->> 'provider', '_', ' ') ILIKE '%' || :textSearch || '%'
                        OR model.configuration ->> 'modelId' ILIKE '%' || :textSearch || '%')
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM ai_model model
                    WHERE model.tenant_id = :tenantId
                      AND (:textSearch IS NULL
                        OR model.name ILIKE '%' || :textSearch || '%'
                        OR REPLACE(model.configuration ->> 'provider', '_', ' ') ILIKE '%' || :textSearch || '%'
                        OR (model.configuration ->> 'modelId') ILIKE '%' || :textSearch || '%')
                    """,
            nativeQuery = true
    )
    Page<AiModelEntity> findByTenantId(@Param("tenantId") UUID tenantId, @Param("textSearch") String textSearch, Pageable pageable);

    @Query("SELECT ai_model.id FROM AiModelEntity ai_model WHERE ai_model.tenantId = :tenantId")
    Page<UUID> findIdsByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT externalId FROM AiModelEntity WHERE id = :id")
    Optional<UUID> getExternalIdById(@Param("id") UUID id);

    long countByTenantId(UUID tenantId);

    @Transactional
    @Modifying
    @Query("DELETE FROM AiModelEntity ai_model WHERE ai_model.id IN (:ids)")
    int deleteByIdIn(@Param("ids") Set<UUID> ids);

    @Transactional
    @Modifying
    @Query(value = """
                DELETE FROM ai_model
                WHERE tenant_id = :tenantId
                RETURNING id
            """, nativeQuery = true
    )
    Set<UUID> deleteByTenantId(@Param("tenantId") UUID tenantId);

    @Transactional
    @Modifying
    @Query("DELETE FROM AiModelEntity ai_model WHERE ai_model.tenantId = :tenantId AND ai_model.id IN (:ids)")
    int deleteByTenantIdAndIdIn(@Param("tenantId") UUID tenantId, @Param("ids") Set<UUID> ids);

}
