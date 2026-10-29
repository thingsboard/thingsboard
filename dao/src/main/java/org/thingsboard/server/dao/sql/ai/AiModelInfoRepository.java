// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.ai;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.AiModelInfoEntity;

import java.util.UUID;

interface AiModelInfoRepository extends JpaRepository<AiModelInfoEntity, UUID> {

    @Query("SELECT m FROM AiModelInfoEntity m WHERE m.tenantId = :tenantId " +
            "AND (:textSearch IS NULL " +
            "OR ilike(m.name, CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(REPLACE(m.provider, '_', ' '), CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(m.modelId, CONCAT('%', :textSearch, '%')) = true)")
    Page<AiModelInfoEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                           @Param("textSearch") String textSearch,
                                           Pageable pageable);

}
