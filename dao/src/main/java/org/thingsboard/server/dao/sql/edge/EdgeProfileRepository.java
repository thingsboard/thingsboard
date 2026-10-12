// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.edge;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.EdgeProfileEntity;

import java.util.List;
import java.util.UUID;

public interface EdgeProfileRepository extends JpaRepository<EdgeProfileEntity, UUID> {

    @Query("SELECT p FROM EdgeProfileEntity p WHERE p.tenantId = :tenantId " +
            "AND (:textSearch IS NULL OR ilike(p.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<EdgeProfileEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                           @Param("textSearch") String textSearch,
                                           Pageable pageable);

    EdgeProfileEntity findByTenantIdAndName(UUID tenantId, String name);

    @Query("SELECT p FROM EdgeProfileEntity p WHERE p.tenantId = :tenantId AND p.isDefault = true")
    EdgeProfileEntity findByTenantIdAndDefaultTrue(@Param("tenantId") UUID tenantId);

    @Query(value = "SELECT * FROM edge_profile WHERE tenant_id = :tenantId " +
            "AND strpos(cast(profile_data as text), :entityId) > 0", nativeQuery = true)
    List<EdgeProfileEntity> findByTenantIdAndReferencedEntityId(@Param("tenantId") UUID tenantId,
                                                                @Param("entityId") String entityId);

    boolean existsByTenantIdAndRootRuleChainId(UUID tenantId, UUID rootRuleChainId);

    Long countByTenantId(UUID tenantId);

}
