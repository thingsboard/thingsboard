// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.secret;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.SecretInfoEntity;

import java.util.List;
import java.util.UUID;

public interface SecretInfoRepository extends JpaRepository<SecretInfoEntity, UUID> {

    @Query("SELECT d FROM SecretInfoEntity d WHERE d.tenantId = :tenantId AND " +
            "(:searchText is NULL OR " +
            "ilike(d.name, concat('%', :searchText, '%')) = true OR " +
            "ilike(d.description, concat('%', :searchText, '%')) = true)")
    Page<SecretInfoEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                          @Param("searchText") String searchText,
                                          Pageable pageable);

    SecretInfoEntity findByTenantIdAndName(UUID id, String name);

    @Query("SELECT d.name FROM SecretInfoEntity d WHERE d.tenantId = :tenantId")
    List<String> findAllNamesByTenantId(@Param("tenantId") UUID tenantId);

}
