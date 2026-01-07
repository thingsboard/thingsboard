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
import org.thingsboard.server.dao.model.sql.OtaPackageInfoEntity;

import java.util.UUID;

public interface OtaPackageInfoRepository extends JpaRepository<OtaPackageInfoEntity, UUID> {

    @Query("SELECT new OtaPackageInfoEntity(f.id, f.createdTime, f.tenantId, f.deviceProfileId, f.type, f.title, f.version, f.tag, f.url, f.fileName, f.contentType, f.checksumAlgorithm, f.checksum, f.dataSize, f.additionalInfo, f.externalId, CASE WHEN (f.data IS NOT NULL OR f.url IS NOT NULL) THEN true ELSE false END) FROM OtaPackageEntity f WHERE " +
            "f.tenantId = :tenantId " +
            "AND (:searchText IS NULL OR ilike(f.title, CONCAT('%', :searchText, '%')) = true)")
    Page<OtaPackageInfoEntity> findAllByTenantId(@Param("tenantId") UUID tenantId,
                                                 @Param("searchText") String searchText,
                                                 Pageable pageable);

    @Query("SELECT new OtaPackageInfoEntity(f.id, f.createdTime, f.tenantId, f.deviceProfileId, f.type, f.title, f.version, f.tag, f.url, f.fileName, f.contentType, f.checksumAlgorithm, f.checksum, f.dataSize, f.additionalInfo, f.externalId, true) FROM OtaPackageEntity f WHERE " +
            "f.tenantId = :tenantId " +
            "AND f.deviceProfileId = :deviceProfileId " +
            "AND f.type = :type " +
            "AND (f.data IS NOT NULL OR f.url IS NOT NULL) " +
            "AND (:searchText IS NULL OR ilike(f.title, CONCAT('%', :searchText, '%')) = true)")
    Page<OtaPackageInfoEntity> findAllByTenantIdAndTypeAndDeviceProfileIdAndHasData(@Param("tenantId") UUID tenantId,
                                                                                    @Param("deviceProfileId") UUID deviceProfileId,
                                                                                    @Param("type") OtaPackageType type,
                                                                                    @Param("searchText") String searchText,
                                                                                    Pageable pageable);

    @Query("SELECT new OtaPackageInfoEntity(f.id, f.createdTime, f.tenantId, f.deviceProfileId, f.type, f.title, f.version, f.tag, f.url, f.fileName, f.contentType, f.checksumAlgorithm, f.checksum, f.dataSize, f.additionalInfo, f.externalId, CASE WHEN (f.data IS NOT NULL OR f.url IS NOT NULL)  THEN true ELSE false END) FROM OtaPackageEntity f WHERE f.id = :id")
    OtaPackageInfoEntity findOtaPackageInfoById(@Param("id") UUID id);

    @Query(value = "SELECT exists(SELECT * " +
            "FROM device_profile AS dp " +
            "LEFT JOIN device AS d ON dp.id = d.device_profile_id " +
            "WHERE dp.id = :deviceProfileId AND " +
            "(('FIRMWARE' = :type AND (dp.firmware_id = :otaPackageId OR d.firmware_id = :otaPackageId)) " +
            "OR ('SOFTWARE' = :type AND (dp.software_id = :otaPackageId or d.software_id = :otaPackageId))))", nativeQuery = true)
    boolean isOtaPackageUsed(@Param("otaPackageId") UUID otaPackageId, @Param("deviceProfileId") UUID deviceProfileId, @Param("type") String type);

}
