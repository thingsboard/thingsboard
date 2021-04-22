/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.firmware;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.FirmwareInfoEntity;

import java.util.UUID;

public interface FirmwareInfoRepository extends CrudRepository<FirmwareInfoEntity, UUID> {
    @Query("SELECT new FirmwareInfoEntity(f.id, f.createdTime, f.tenantId, f.title, f.version, f.additionalInfo, f.data IS NOT NULL) FROM FirmwareEntity f WHERE " +
            "f.tenantId = :tenantId " +
            "AND LOWER(f.searchText) LIKE LOWER(CONCAT(:searchText, '%'))")
    Page<FirmwareInfoEntity> findAllByTenantId(@Param("tenantId") UUID tenantId,
                                               @Param("searchText") String searchText,
                                               Pageable pageable);

    @Query("SELECT new FirmwareInfoEntity(f.id, f.createdTime, f.tenantId, f.title, f.version, f.additionalInfo, f.data IS NOT NULL) FROM FirmwareEntity f WHERE " +
            "f.tenantId = :tenantId " +
            "AND ((f.data IS NOT NULL AND :hasData = true) OR (f.data IS NULL AND :hasData = false ))" +
            "AND LOWER(f.searchText) LIKE LOWER(CONCAT(:searchText, '%'))")
    Page<FirmwareInfoEntity> findAllByTenantIdAndHasData(@Param("tenantId") UUID tenantId,
                                                         @Param("hasData") boolean hasData,
                                                         @Param("searchText") String searchText,
                                                         Pageable pageable);

    @Query("SELECT new FirmwareInfoEntity(f.id, f.createdTime, f.tenantId, f.title, f.version, f.additionalInfo, f.data IS NOT NULL) FROM FirmwareEntity f WHERE f.id = :id")
    FirmwareInfoEntity findFirmwareInfoById(@Param("id") UUID id);
}
