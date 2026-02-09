/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.ota;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.ota.OtaPackageType;
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

}
