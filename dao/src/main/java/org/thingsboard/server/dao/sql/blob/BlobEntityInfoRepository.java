// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.blob;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.BlobEntityInfoEntity;
import org.thingsboard.server.dao.model.sql.BlobEntityWithCustomerInfoEntity;

import java.util.List;
import java.util.UUID;

public interface BlobEntityInfoRepository extends JpaRepository<BlobEntityInfoEntity, UUID> {

    @Query("SELECT new org.thingsboard.server.dao.model.sql.BlobEntityWithCustomerInfoEntity(b, c.title, c.additionalInfo) " +
            "FROM BlobEntityInfoEntity b " +
            "LEFT JOIN CustomerEntity c on c.id = b.customerId " +
            "WHERE b.id = :blobEntityId")
    BlobEntityWithCustomerInfoEntity findBlobEntityWithCustomerInfoById(@Param("blobEntityId") UUID blobEntityId);

    List<BlobEntityInfoEntity> findBlobEntitiesByTenantIdAndIdIn(UUID tenantId, List<UUID> blobEntityIds);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.BlobEntityWithCustomerInfoEntity(b, c.title, c.additionalInfo) " +
            "FROM BlobEntityInfoEntity b " +
            "LEFT JOIN CustomerEntity c on c.id = b.customerId " +
            "WHERE b.tenantId = :tenantId " +
            "AND (:startTime IS NULL OR b.createdTime >= :startTime) " +
            "AND (:endTime IS NULL OR b.createdTime <= :endTime) " +
            "AND (:textSearch IS NULL OR ilike(b.name, CONCAT('%', :textSearch, '%')) = true)"
    )
    Page<BlobEntityWithCustomerInfoEntity> findByTenantId(
            @Param("tenantId") UUID tenantId,
            @Param("textSearch") String textSearch,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime,
            Pageable pageable);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.BlobEntityWithCustomerInfoEntity(b, c.title, c.additionalInfo) " +
            "FROM BlobEntityInfoEntity b " +
            "LEFT JOIN CustomerEntity c on c.id = b.customerId " +
            "WHERE b.tenantId = :tenantId " +
            "AND b.type = :type " +
            "AND (:startTime IS NULL OR b.createdTime >= :startTime) " +
            "AND (:endTime IS NULL OR b.createdTime <= :endTime) " +
            "AND (:textSearch IS NULL OR ilike(b.name, CONCAT('%', :textSearch, '%')) = true)"
    )
    Page<BlobEntityWithCustomerInfoEntity> findByTenantIdAndType(
            @Param("tenantId") UUID tenantId,
            @Param("type") String type,
            @Param("textSearch") String textSearch,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime,
            Pageable pageable);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.BlobEntityWithCustomerInfoEntity(b, c.title, c.additionalInfo) " +
            "FROM BlobEntityInfoEntity b " +
            "LEFT JOIN CustomerEntity c on c.id = b.customerId " +
            "WHERE b.tenantId = :tenantId " +
            "AND b.customerId = :customerId " +
            "AND (:startTime IS NULL OR b.createdTime >= :startTime) " +
            "AND (:endTime IS NULL OR b.createdTime <= :endTime) " +
            "AND (:textSearch IS NULL OR ilike(b.name, CONCAT('%', :textSearch, '%')) = true)"
    )
    Page<BlobEntityWithCustomerInfoEntity> findByTenantIdAndCustomerId(
            @Param("tenantId") UUID tenantId,
            @Param("customerId") UUID customerId,
            @Param("textSearch") String textSearch,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime,
            Pageable pageable);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.BlobEntityWithCustomerInfoEntity(b, c.title, c.additionalInfo) " +
            "FROM BlobEntityInfoEntity b " +
            "LEFT JOIN CustomerEntity c on c.id = b.customerId " +
            "WHERE b.tenantId = :tenantId " +
            "AND b.customerId = :customerId " +
            "AND b.type = :type " +
            "AND (:startTime IS NULL OR b.createdTime >= :startTime) " +
            "AND (:endTime IS NULL OR b.createdTime <= :endTime) " +
            "AND (:textSearch IS NULL OR ilike(b.name, CONCAT('%', :textSearch, '%')) = true)"
    )
    Page<BlobEntityWithCustomerInfoEntity> findByTenantIdAndCustomerIdAndType(
            @Param("tenantId") UUID tenantId,
            @Param("customerId") UUID customerId,
            @Param("type") String type,
            @Param("textSearch") String textSearch,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime,
            Pageable pageable);

}
