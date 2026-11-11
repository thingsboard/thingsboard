// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.sql.settings;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.dao.model.sql.AdminSettingsEntity;

import java.util.List;
import java.util.UUID;

public interface AdminSettingsRepository extends JpaRepository<AdminSettingsEntity, UUID> {

    AdminSettingsEntity findByTenantIdAndKey(UUID tenantId, String key);

    void deleteByTenantIdAndKey(UUID tenantId, String key);

    void deleteByTenantId(UUID tenantId);

    boolean existsByTenantIdAndKey(UUID tenantId, String key);

    Page<AdminSettingsEntity> findByTenantId(UUID tenantId, Pageable pageable);

    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(settings.id, 'ADMIN_SETTINGS', settings.key) " +
            "FROM AdminSettingsEntity settings WHERE settings.tenantId = :tenantId AND ilike(settings.jsonValue, CONCAT('%', :placeholder, '%'))")
    List<EntityInfo> findByTenantIdAndSecretPlaceholder(@Param("tenantId") UUID tenantId,
                                                        @Param("placeholder") String placeholder);

}
