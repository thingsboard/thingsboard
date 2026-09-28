// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.owner;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.OwnerInfoEntity;

import java.util.List;
import java.util.UUID;

public interface OwnerInfoRepository extends JpaRepository<OwnerInfoEntity, UUID> {

    @Query("SELECT oi FROM OwnerInfoEntity oi " +
            "WHERE oi.id = :tenantId AND oi.entityType = 'TENANT' " +
            "AND (:searchText IS NULL OR ilike(oi.name, CONCAT('%', :searchText, '%')) = true)")
    Page<OwnerInfoEntity> findTenantOwnerByTenantId(@Param("tenantId") UUID tenantId,
                                                    @Param("searchText") String searchText,
                                                    Pageable pageable);

    @Query("SELECT oi FROM OwnerInfoEntity oi " +
            "WHERE ((oi.tenantId = :tenantId AND oi.entityType = 'CUSTOMER' AND oi.isPublic = false) " +
            "OR (oi.id = :tenantId AND oi.entityType = 'TENANT')) " +
            "AND (:searchText IS NULL OR ilike(oi.name, CONCAT('%', :searchText, '%')) = true)")
    Page<OwnerInfoEntity> findCustomerOwnersByTenantIdIncludingTenant(@Param("tenantId") UUID tenantId,
                                                                      @Param("searchText") String searchText,
                                                                      Pageable pageable);

    @Query("SELECT oi FROM OwnerInfoEntity oi " +
            "WHERE oi.tenantId = :tenantId AND oi.entityType = 'CUSTOMER' AND oi.isPublic = false " +
            "AND (:searchText IS NULL OR ilike(oi.name, CONCAT('%', :searchText, '%')) = true)")
    Page<OwnerInfoEntity> findCustomerOwnersByTenantId(@Param("tenantId") UUID tenantId,
                                                       @Param("searchText") String searchText,
                                                       Pageable pageable);

    @Query("SELECT oi FROM OwnerInfoEntity oi " +
           "WHERE oi.id IN :ownerIds AND oi.tenantId = :tenantId AND oi.entityType = 'CUSTOMER' AND oi.isPublic = false " +
            "AND (:searchText IS NULL OR ilike(oi.name, CONCAT('%', :searchText, '%')) = true)")
    Page<OwnerInfoEntity> findCustomerOwnersByIdsAndTenantId(@Param("tenantId") UUID tenantId,
                                                             @Param("ownerIds") List<UUID> ownerIds,
                                                             @Param("searchText") String searchText,
                                                             Pageable pageable);

}
