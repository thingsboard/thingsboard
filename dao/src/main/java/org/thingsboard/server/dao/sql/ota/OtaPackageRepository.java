// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.ota;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.OtaPackageEntity;

import java.util.UUID;

public interface OtaPackageRepository extends JpaRepository<OtaPackageEntity, UUID>, ExportableEntityRepository<OtaPackageEntity> {

    @Query(value = "SELECT COALESCE(SUM(ota.data_size), 0) FROM ota_package ota WHERE ota.tenant_id = :tenantId AND ota.data IS NOT NULL", nativeQuery = true)
    Long sumDataSizeByTenantId(@Param("tenantId") UUID tenantId);

    Page<OtaPackageEntity> findByTenantId(UUID tenantId, Pageable pageable);

    OtaPackageEntity findByTenantIdAndTitleAndVersion(UUID tenantId, String title, String version);

    @Query("SELECT externalId FROM OtaPackageEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

    @Query("SELECT r.id FROM OtaPackageEntity r WHERE r.tenantId = :tenantId")
    Page<UUID> findIdsByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    // The 'data' column is of type OID (PostgreSQL large object reference), so it returns the OID as Long
    @Query(value = "SELECT data FROM ota_package WHERE id = :id AND data IS NOT NULL", nativeQuery = true)
    Long getDataOidById(@Param("id") UUID id);

    @Transactional
    @Query(value = "SELECT lo_unlink(:oid)", nativeQuery = true)
    Integer unlinkLargeObject(@Param("oid") Long oid);

}
