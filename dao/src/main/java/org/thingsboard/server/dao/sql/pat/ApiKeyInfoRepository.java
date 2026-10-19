// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.pat;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.ApiKeyInfoEntity;

import java.util.UUID;

public interface ApiKeyInfoRepository extends JpaRepository<ApiKeyInfoEntity, UUID> {

    @Query("SELECT ak FROM ApiKeyInfoEntity ak WHERE ak.tenantId = :tenantId AND ak.userId = :userId AND " +
            "(:searchText is NULL OR ilike(ak.description, concat('%', :searchText, '%')) = true)")
    Page<ApiKeyInfoEntity> findByUserId(@Param("tenantId") UUID tenantId,
                                        @Param("userId") UUID userId,
                                        @Param("searchText") String searchText,
                                        Pageable pageable);

}
