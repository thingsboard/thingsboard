// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.secret;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.SecretType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.dao.model.sql.SecretEntity;

import java.util.List;
import java.util.UUID;

public interface SecretRepository extends JpaRepository<SecretEntity, UUID> {

    @Query("SELECT d FROM SecretEntity d WHERE d.tenantId = :tenantId AND " +
            "(:searchText is NULL OR " +
            "ilike(d.name, concat('%', :searchText, '%')) = true OR " +
            "ilike(d.description, concat('%', :searchText, '%')) = true)")
    Page<SecretEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                      @Param("searchText") String searchText,
                                      Pageable pageable);

    SecretEntity findByTenantIdAndName(UUID id, String name);

    @Transactional
    @Modifying
    @Query("DELETE FROM SecretEntity r WHERE r.tenantId = :tenantId")
    void deleteByTenantId(@Param("tenantId") UUID tenantId);
    
    @Query("SELECT NEW org.thingsboard.server.common.data.util.TbPair(se.type, COUNT(*))" +
            "FROM SecretEntity se " +
            "GROUP BY se.type")
    List<TbPair<SecretType, Long>> countSecretsPerType();

}
