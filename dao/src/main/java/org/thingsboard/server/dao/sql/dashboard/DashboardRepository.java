// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.dashboard;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.edqs.fields.DashboardFields;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.DashboardEntity;

import java.util.List;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
public interface DashboardRepository extends JpaRepository<DashboardEntity, UUID>, ExportableEntityRepository<DashboardEntity> {

    Long countByTenantId(UUID tenantId);

    List<DashboardEntity> findByTenantIdAndTitle(UUID tenantId, String title);

    Page<DashboardEntity> findByTenantId(UUID tenantId, Pageable pageable);

    @Query("SELECT externalId FROM DashboardEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

    @Query("SELECT d.id FROM DashboardEntity d WHERE d.tenantId = :tenantId")
    Page<UUID> findIdsByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT d.id FROM DashboardEntity d")
    Page<UUID> findAllIds(Pageable pageable);

    @Query("SELECT new org.thingsboard.server.common.data.edqs.fields.DashboardFields(d.id, d.createdTime, d.tenantId, " +
            "d.assignedCustomers, d.title, d.version) FROM DashboardEntity d WHERE d.id > :id ORDER BY d.id")
    List<DashboardFields> findNextBatch(@Param("id") UUID id, Limit limit);
}
