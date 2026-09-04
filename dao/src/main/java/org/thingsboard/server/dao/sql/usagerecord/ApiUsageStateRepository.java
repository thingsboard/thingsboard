// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
