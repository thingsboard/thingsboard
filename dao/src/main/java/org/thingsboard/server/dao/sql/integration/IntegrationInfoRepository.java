// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.integration;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.IntegrationInfoEntity;

import java.util.UUID;

public interface IntegrationInfoRepository extends JpaRepository<IntegrationInfoEntity, UUID> {

    @Query("SELECT ii FROM IntegrationInfoEntity ii WHERE ii.tenantId = :tenantId " +
            "AND ii.edgeTemplate = :isEdgeTemplate " +
            "AND (:searchText IS NULL OR ilike(ii.name, CONCAT('%', :searchText, '%')) = true)")
    Page<IntegrationInfoEntity> findByTenantIdAndIsEdgeTemplate(@Param("tenantId") UUID tenantId,
                                                                @Param("searchText") String searchText,
                                                                @Param("isEdgeTemplate") boolean isEdgeTemplate,
                                                                Pageable pageable);

    @Query("SELECT ii FROM IntegrationInfoEntity ii, RelationEntity re WHERE ii.tenantId = :tenantId " +
            "AND ii.id = re.toId AND re.toType = 'INTEGRATION' AND re.relationTypeGroup = 'EDGE' " +
            "AND re.relationType = 'Contains' AND re.fromId = :edgeId AND re.fromType = 'EDGE' " +
            "AND (:searchText IS NULL OR ilike(ii.name, CONCAT('%', :searchText, '%')) = true)")
    Page<IntegrationInfoEntity> findByTenantIdAndEdgeId(@Param("tenantId") UUID tenantId,
                                                        @Param("edgeId") UUID edgeId,
                                                        @Param("searchText") String searchText,
                                                        Pageable pageable);

    @Query("SELECT ii FROM IntegrationInfoEntity ii WHERE ii.tenantId = :tenantId " +
            "AND ii.edgeTemplate = :isEdgeTemplate " +
            "AND (:searchText IS NULL OR ilike(ii.name, CONCAT('%', :searchText, '%')) = true)")
    Page<IntegrationInfoEntity> findAllIntegrationInfos(@Param("tenantId") UUID tenantId,
                                                        @Param("searchText") String searchText,
                                                        @Param("isEdgeTemplate") boolean isEdgeTemplate,
                                                        Pageable pageable);

}
