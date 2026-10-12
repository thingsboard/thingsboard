// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.device;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sql.DeviceCertificateRevocationEntity;

import java.util.UUID;

public interface DeviceCertificateRevocationRepository extends JpaRepository<DeviceCertificateRevocationEntity, UUID> {

    DeviceCertificateRevocationEntity findByTenantIdAndCertificateHash(UUID tenantId, String certificateHash);

    @Query("SELECT r FROM DeviceCertificateRevocationEntity r WHERE r.tenantId = :tenantId AND " +
            "(:searchText is NULL OR ilike(r.subject, concat('%', :searchText, '%')) = true " +
            "OR ilike(r.issuer, concat('%', :searchText, '%')) = true " +
            "OR ilike(r.serialNumber, concat('%', :searchText, '%')) = true " +
            "OR ilike(r.reason, concat('%', :searchText, '%')) = true)")
    Page<DeviceCertificateRevocationEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                                           @Param("searchText") String searchText,
                                                           Pageable pageable);

    @Transactional
    @Modifying
    @Query("DELETE FROM DeviceCertificateRevocationEntity r WHERE r.tenantId = :tenantId")
    void deleteByTenantId(@Param("tenantId") UUID tenantId);

    @Transactional
    @Modifying
    @Query("DELETE FROM DeviceCertificateRevocationEntity r WHERE r.notAfter < :ts")
    int deleteAllByNotAfterBefore(@Param("ts") long ts);

}
