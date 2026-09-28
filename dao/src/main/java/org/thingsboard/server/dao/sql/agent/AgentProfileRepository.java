// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.agent;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.AgentProfileEntity;
import org.thingsboard.server.dao.model.sql.AgentProfileInfoEntity;

import java.util.Optional;
import java.util.UUID;

public interface AgentProfileRepository extends JpaRepository<AgentProfileEntity, UUID> {

    @Query("SELECT g FROM AgentProfileEntity g WHERE g.tenantId = :tenantId " +
            "AND (:textSearch IS NULL OR ilike(g.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<AgentProfileEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                           @Param("textSearch") String textSearch,
                                           Pageable pageable);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.AgentProfileInfoEntity(g) " +
            "FROM AgentProfileEntity g " +
            "WHERE g.id = :agentProfileId")
    AgentProfileInfoEntity findAgentProfileInfoById(@Param("agentProfileId") UUID agentProfileId);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.AgentProfileInfoEntity(g) " +
            "FROM AgentProfileEntity g " +
            "WHERE g.tenantId = :tenantId " +
            "AND (:textSearch IS NULL OR ilike(g.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<AgentProfileInfoEntity> findAgentProfileInfosByTenantId(@Param("tenantId") UUID tenantId,
                                                              @Param("textSearch") String textSearch,
                                                              Pageable pageable);

    @Query("SELECT count(*) FROM AgentProfileEntity g WHERE g.tenantId = :tenantId")
    Long countByTenantId(@Param("tenantId") UUID tenantId);

    Optional<AgentProfileEntity> findByProvisionKey(String provisionKey);

    Optional<AgentProfileEntity> findByTenantIdAndName(UUID tenantId, String name);

    @Query("SELECT a FROM AgentProfileEntity a WHERE a.tenantId = :tenantId AND a.isDefault = true")
    AgentProfileEntity findByTenantIdAndDefaultTrue(@Param("tenantId") UUID tenantId);

}
