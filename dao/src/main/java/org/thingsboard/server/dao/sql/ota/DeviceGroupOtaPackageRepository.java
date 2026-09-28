// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.ota;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.dao.model.sql.DeviceGroupOtaPackageEntity;

import java.util.List;
import java.util.UUID;

public interface DeviceGroupOtaPackageRepository extends JpaRepository<DeviceGroupOtaPackageEntity, UUID> {

    DeviceGroupOtaPackageEntity findByGroupIdAndOtaPackageType(UUID groupId, OtaPackageType otaPackageType);

    List<DeviceGroupOtaPackageEntity> findByGroupId(UUID groupId);

    @Query("SELECT o FROM DeviceGroupOtaPackageEntity o WHERE o.otaPackageId IN (SELECT p.id FROM OtaPackageEntity p WHERE p.tenantId = :tenantId)")
    Page<DeviceGroupOtaPackageEntity> findByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

}
