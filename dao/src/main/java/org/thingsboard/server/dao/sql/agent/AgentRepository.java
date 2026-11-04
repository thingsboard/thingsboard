// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.agent;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.AgentEntity;

import java.util.List;
import java.util.UUID;

public interface AgentRepository extends JpaRepository<AgentEntity, UUID> {

    @Query("SELECT a FROM AgentEntity a WHERE a.tenantId = :tenantId " +
            "AND a.customerId = :customerId " +
            "AND (:textSearch IS NULL OR ilike(a.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<AgentEntity> findByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                   @Param("customerId") UUID customerId,
                                                   @Param("textSearch") String textSearch,
                                                   Pageable pageable);

    @Query("SELECT a.id FROM AgentEntity a WHERE a.tenantId = :tenantId AND (a.customerId is null OR a.customerId = org.thingsboard.server.common.data.id.EntityId.NULL_UUID)")
    Page<UUID> findIdsByTenantIdAndNullCustomerId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT a.id FROM AgentEntity a WHERE a.tenantId = :tenantId AND a.customerId = :customerId")
    Page<UUID> findIdsByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                              @Param("customerId") UUID customerId,
                                              Pageable pageable);

    AgentEntity findByRoutingKey(String routingKey);

    @Query("SELECT count(*) FROM AgentEntity a WHERE a.tenantId = :tenantId")
    Long countByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT a FROM AgentEntity a WHERE a.tenantId = :tenantId " +
            "AND (:textSearch IS NULL OR ilike(a.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<AgentEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                     @Param("textSearch") String textSearch,
                                     Pageable pageable);

    @Query("SELECT a FROM AgentEntity a, RelationEntity re " +
            "WHERE a.id = re.toId AND re.toType = 'AGENT' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId = :groupId AND re.fromType = 'ENTITY_GROUP' " +
            "AND (:textSearch IS NULL OR ilike(a.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<AgentEntity> findByEntityGroupId(@Param("groupId") UUID groupId,
                                          @Param("textSearch") String textSearch,
                                          Pageable pageable);

    @Query("SELECT a FROM AgentEntity a, RelationEntity re " +
            "WHERE a.id = re.toId AND re.toType = 'AGENT' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId IN :groupIds AND re.fromType = 'ENTITY_GROUP' " +
            "AND (:textSearch IS NULL OR ilike(a.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<AgentEntity> findByEntityGroupIds(@Param("groupIds") List<UUID> groupIds,
                                           @Param("textSearch") String textSearch,
                                           Pageable pageable);
}
