// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.agent;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.AgentInfoEntity;

import java.util.List;
import java.util.UUID;

public interface AgentInfoRepository extends JpaRepository<AgentInfoEntity, UUID> {

    List<AgentInfoEntity> findAgentInfosByTenantIdAndIdIn(UUID tenantId, List<UUID> agentIds);

    @Query("SELECT a FROM AgentInfoEntity a " +
            "WHERE a.tenantId = :tenantId " +
            "AND (:textSearch IS NULL OR ilike(a.name, CONCAT('%', :textSearch, '%')) = true " +
            "     OR ilike(a.customerTitle, CONCAT('%', :textSearch, '%')) = true)")
    Page<AgentInfoEntity> findAgentInfosByTenantId(@Param("tenantId") UUID tenantId,
                                                   @Param("textSearch") String textSearch,
                                                   Pageable pageable);

    @Query("SELECT a FROM AgentInfoEntity a " +
            "WHERE a.tenantId = :tenantId " +
            "AND a.customerId = :customerId " +
            "AND (:textSearch IS NULL OR ilike(a.name, CONCAT('%', :textSearch, '%')) = true " +
            "     OR ilike(a.customerTitle, CONCAT('%', :textSearch, '%')) = true)")
    Page<AgentInfoEntity> findAgentInfosByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                                @Param("customerId") UUID customerId,
                                                                @Param("textSearch") String textSearch,
                                                                Pageable pageable);
}
