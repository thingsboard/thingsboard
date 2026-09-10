// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.pat;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sql.ApiKeyEntity;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface ApiKeyRepository extends JpaRepository<ApiKeyEntity, UUID> {

    ApiKeyEntity findByValue(String value);

    Page<ApiKeyEntity> findByTenantId(UUID tenantId, Pageable pageable);

    List<ApiKeyEntity> findByTenantIdAndUserId(UUID tenantId, UUID userId);

    @Transactional
    @Modifying
    @Query(value = """
                DELETE FROM api_key
                WHERE tenant_id = :tenantId
                RETURNING value
            """, nativeQuery = true
    )
    Set<String> deleteByTenantId(@Param("tenantId") UUID tenantId);

    @Transactional
    @Modifying
    @Query(value = """
                DELETE FROM api_key
                WHERE tenant_id = :tenantId AND user_id = :userId
                RETURNING value
            """, nativeQuery = true
    )
    Set<String> deleteByUserId(@Param("tenantId") UUID tenantId,
                               @Param("userId") UUID userId);

    @Transactional
    @Modifying
    @Query("DELETE FROM ApiKeyEntity ak WHERE ak.expirationTime > 0 AND ak.expirationTime < :ts")
    int deleteAllByExpirationTimeBefore(@Param("ts") long ts);

}
