// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sql.DomainEntity;

import java.util.UUID;

public interface DomainRepository extends JpaRepository<DomainEntity, UUID> {

    @Query("SELECT d FROM DomainEntity d WHERE d.tenantId = :tenantId AND " +
            "(:searchText is NULL OR ilike(d.name, concat('%', :searchText, '%')) = true)")
    Page<DomainEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                      @Param("searchText") String searchText,
                                      Pageable pageable);

    @Transactional
    @Modifying
    @Query("DELETE FROM DomainEntity r WHERE r.tenantId = :tenantId")
    void deleteByTenantId(@Param("tenantId") UUID tenantId);

    int countByTenantIdAndOauth2Enabled(UUID tenantId, boolean oauth2Enabled);

}
