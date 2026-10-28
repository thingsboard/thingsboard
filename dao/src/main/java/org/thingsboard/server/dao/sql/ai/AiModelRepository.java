// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.sql.ai;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.AiModelEntity;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.AI_MODEL_MODEL_ID_EXPRESSION;
import static org.thingsboard.server.dao.model.ModelConstants.AI_MODEL_PROVIDER_EXPRESSION;

interface AiModelRepository extends JpaRepository<AiModelEntity, UUID>, ExportableEntityRepository<AiModelEntity> {

    String FIND_BY_TENANT_ID_WHERE = " WHERE model.tenant_id = :tenantId" +
            " AND (CAST(:customerAccessEnabled AS BOOLEAN) IS NULL OR model.customer_access_enabled = CAST(:customerAccessEnabled AS BOOLEAN))" +
            " AND (:textSearch IS NULL" +
            " OR model.name ILIKE '%' || :textSearch || '%'" +
            " OR REPLACE(" + AI_MODEL_PROVIDER_EXPRESSION + ", '_', ' ') ILIKE '%' || :textSearch || '%'" +
            " OR " + AI_MODEL_MODEL_ID_EXPRESSION + " ILIKE '%' || :textSearch || '%')";

    Optional<AiModelEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<AiModelEntity> findByTenantIdAndName(UUID tenantId, String name);

    @Query(
            value = "SELECT * FROM ai_model model" + FIND_BY_TENANT_ID_WHERE,
            countQuery = "SELECT COUNT(*) FROM ai_model model" + FIND_BY_TENANT_ID_WHERE,
            nativeQuery = true
    )
    Page<AiModelEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                       @Param("customerAccessEnabled") Boolean customerAccessEnabled,
                                       @Param("textSearch") String textSearch,
                                       Pageable pageable);

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

    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(m.id, 'AI_MODEL', m.name) " +
           "FROM AiModelEntity m WHERE m.tenantId = :tenantId AND ilike(m.configuration, CONCAT('%', :placeholder, '%'))")
    List<EntityInfo> findByTenantIdAndSecretPlaceholder(@Param("tenantId") UUID tenantId,
                                                        @Param("placeholder") String placeholder);

}
