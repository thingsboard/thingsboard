/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.usagerecord;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.edqs.fields.ApiUsageStateFields;
import org.thingsboard.server.dao.model.sql.ApiUsageStateEntity;

import java.util.List;
import java.util.UUID;

public interface ApiUsageStateRepository extends JpaRepository<ApiUsageStateEntity, UUID> {

    @Query("SELECT ur FROM ApiUsageStateEntity ur WHERE ur.tenantId = :tenantId " +
            "AND ur.entityId = :tenantId AND ur.entityType = 'TENANT' ")
    ApiUsageStateEntity findByTenantId(@Param("tenantId") UUID tenantId);

    ApiUsageStateEntity findByEntityIdAndEntityType(UUID entityId, String entityType);

    Page<ApiUsageStateEntity> findAllByTenantId(UUID tenantId, Pageable pageable);

    @Transactional
    @Modifying
    @Query("DELETE FROM ApiUsageStateEntity ur WHERE ur.tenantId = :tenantId")
    void deleteApiUsageStateByTenantId(@Param("tenantId") UUID tenantId);

    @Transactional
    @Modifying
    @Query("DELETE FROM ApiUsageStateEntity e WHERE e.entityId = :entityId and e.entityType = :entityType")
    void deleteByEntityIdAndEntityType(@Param("entityId") UUID entityId, @Param("entityType") String entityType);

    @Query("SELECT new org.thingsboard.server.common.data.edqs.fields.ApiUsageStateFields(a.id, a.createdTime, a.tenantId," +
            "a.entityId, a.entityType, a.transportState, a.dbStorageState, a.reExecState, a.jsExecState, a.tbelExecState, " +
            "a.emailExecState, a.smsExecState, a.alarmExecState, a.version) FROM ApiUsageStateEntity a WHERE a.id > :id ORDER BY a.id")
    List<ApiUsageStateFields> findNextBatch(@Param("id") UUID id, Limit limit);

}
