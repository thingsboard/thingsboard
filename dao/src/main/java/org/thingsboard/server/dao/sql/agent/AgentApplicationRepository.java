// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.agent;

import jakarta.persistence.Tuple;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sql.AgentApplicationEntity;
import org.thingsboard.server.dao.model.sql.AgentApplicationInfoEntity;

import java.util.List;
import java.util.UUID;

public interface AgentApplicationRepository extends JpaRepository<AgentApplicationEntity, UUID> {

    String MANAGED_RELATED_ENTITY_IDS = "SELECT re.from_id FROM relation re JOIN agent_application a ON a.id = re.to_id " +
            "WHERE re.to_type = 'AGENT_APPLICATION' AND re.relation_type_group = 'AGENT' AND re.relation_type = 'ManagedByAgentApp' " +
            "AND a.tenant_id = :tenantId";

    String EDGE_CANDIDATES_WHERE = "FROM edge e WHERE e.tenant_id = :tenantId " +
            "AND (:textSearch IS NULL OR e.name ILIKE CONCAT('%', :textSearch, '%')) " +
            "AND (e.id = :currentEntityId OR e.id NOT IN (" + MANAGED_RELATED_ENTITY_IDS + " AND re.from_type = 'EDGE'))";

    String GATEWAY_DEVICE_CANDIDATES_WHERE = "FROM device d WHERE d.tenant_id = :tenantId " +
            "AND cast(d.additional_info AS json) ->> 'gateway' = 'true' " +
            "AND (:textSearch IS NULL OR d.name ILIKE CONCAT('%', :textSearch, '%')) " +
            "AND (d.id = :currentEntityId OR d.id NOT IN (" + MANAGED_RELATED_ENTITY_IDS + " AND re.from_type = 'DEVICE'))";

    @Query(value = "SELECT e.id AS id, e.name AS name " + EDGE_CANDIDATES_WHERE,
            countQuery = "SELECT count(*) " + EDGE_CANDIDATES_WHERE,
            nativeQuery = true)
    Page<Tuple> findEdgeCandidates(@Param("tenantId") UUID tenantId,
                                   @Param("textSearch") String textSearch,
                                   @Param("currentEntityId") UUID currentEntityId,
                                   Pageable pageable);

    @Query(value = "SELECT d.id AS id, d.name AS name " + GATEWAY_DEVICE_CANDIDATES_WHERE,
            countQuery = "SELECT count(*) " + GATEWAY_DEVICE_CANDIDATES_WHERE,
            nativeQuery = true)
    Page<Tuple> findGatewayDeviceCandidates(@Param("tenantId") UUID tenantId,
                                            @Param("textSearch") String textSearch,
                                            @Param("currentEntityId") UUID currentEntityId,
                                            Pageable pageable);

    @Query(value = "SELECT * FROM agent_application a WHERE a.id = :id FOR UPDATE NOWAIT", nativeQuery = true)
    AgentApplicationEntity findByIdForUpdate(@Param("id") UUID id);

    @Query("SELECT count(*) FROM AgentApplicationEntity a WHERE a.tenantId = :tenantId")
    Long countByTenantId(@Param("tenantId") UUID tenantId);

    List<AgentApplicationEntity> findByTenantIdAndAgentId(UUID tenantId, UUID agentId);

    @Query("SELECT e FROM AgentApplicationEntity e WHERE e.tenantId = :tenantId AND e.agentId = :agentId " +
            "AND (:textSearch IS NULL OR ilike(e.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<AgentApplicationEntity> findByAgentId(@Param("tenantId") UUID tenantId,
                                               @Param("agentId") UUID agentId,
                                               @Param("textSearch") String textSearch,
                                               Pageable pageable);

    @Transactional
    @Modifying
    @Query("DELETE FROM AgentApplicationEntity e WHERE e.tenantId = :tenantId AND e.agentId = :agentId")
    void deleteByAgentId(@Param("tenantId") UUID tenantId, @Param("agentId") UUID agentId);

    @Transactional
    @Modifying
    @Query("UPDATE AgentApplicationEntity e SET e.templateVersion = :templateVersion, e.desiredTemplateVersion = null, " +
            "e.version = e.version + 1 WHERE e.id = :id AND e.tenantId = :tenantId")
    int promoteDesiredTemplate(@Param("tenantId") UUID tenantId, @Param("id") UUID id, @Param("templateVersion") String templateVersion);

    AgentApplicationEntity findByTenantIdAndAgentIdAndProjectName(UUID tenantId, UUID agentId, String projectName);

    @Query("SELECT app FROM AgentApplicationEntity app JOIN AgentAppEventEntity evt ON app.id = evt.applicationId WHERE evt.id = :eventId AND app.tenantId = :tenantId")
    AgentApplicationEntity findByEventId(@Param("tenantId") UUID tenantId, @Param("eventId") UUID eventId);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.AgentApplicationInfoEntity(a, p.version, p.name, p.templateVersion, re.fromId, re.fromType) " +
            "FROM AgentApplicationEntity a " +
            "LEFT JOIN AgentAppProfileEntity p ON a.applicationProfileId = p.id " +
            "LEFT JOIN RelationEntity re ON re.toId = a.id AND re.toType = 'AGENT_APPLICATION' " +
            "    AND re.relationTypeGroup = 'AGENT' AND re.relationType = 'ManagedByAgentApp' " +
            "WHERE a.id = :id AND a.tenantId = :tenantId")
    AgentApplicationInfoEntity findInfoById(@Param("tenantId") UUID tenantId, @Param("id") UUID id);

    @Query(value = "SELECT new org.thingsboard.server.dao.model.sql.AgentApplicationInfoEntity(a, p.version, p.name, p.templateVersion, re.fromId, re.fromType) " +
            "FROM AgentApplicationEntity a " +
            "LEFT JOIN AgentAppProfileEntity p ON a.applicationProfileId = p.id " +
            "LEFT JOIN RelationEntity re ON re.toId = a.id AND re.toType = 'AGENT_APPLICATION' " +
            "    AND re.relationTypeGroup = 'AGENT' AND re.relationType = 'ManagedByAgentApp' " +
            "WHERE a.tenantId = :tenantId AND a.agentId = :agentId " +
            "AND (:textSearch IS NULL OR ilike(a.name, CONCAT('%', :textSearch, '%')) = true)",
            countQuery = "SELECT COUNT(a.id) FROM AgentApplicationEntity a " +
                    "WHERE a.tenantId = :tenantId AND a.agentId = :agentId " +
                    "AND (:textSearch IS NULL OR ilike(a.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<AgentApplicationInfoEntity> findInfosByAgentId(@Param("tenantId") UUID tenantId,
                                                        @Param("agentId") UUID agentId,
                                                        @Param("textSearch") String textSearch,
                                                        Pageable pageable);

    @Query(value = "SELECT new org.thingsboard.server.dao.model.sql.AgentApplicationInfoEntity(a, p.version, p.name, p.templateVersion, ag.name, re.fromId, re.fromType) " +
            "FROM AgentApplicationEntity a " +
            "JOIN AgentEntity ag ON a.agentId = ag.id " +
            "LEFT JOIN AgentAppProfileEntity p ON a.applicationProfileId = p.id " +
            "LEFT JOIN RelationEntity re ON re.toId = a.id AND re.toType = 'AGENT_APPLICATION' " +
            "    AND re.relationTypeGroup = 'AGENT' AND re.relationType = 'ManagedByAgentApp' " +
            "WHERE a.tenantId = :tenantId AND a.applicationProfileId = :applicationProfileId AND ag.agentProfileId = :agentProfileId",
            countQuery = "SELECT COUNT(a.id) FROM AgentApplicationEntity a " +
                    "JOIN AgentEntity ag ON a.agentId = ag.id " +
                    "WHERE a.tenantId = :tenantId AND a.applicationProfileId = :applicationProfileId AND ag.agentProfileId = :agentProfileId")
    Page<AgentApplicationInfoEntity> findByApplicationProfileIdAndAgentProfileId(@Param("tenantId") UUID tenantId,
                                                                                @Param("applicationProfileId") UUID applicationProfileId,
                                                                                @Param("agentProfileId") UUID agentProfileId,
                                                                                Pageable pageable);


    @Query("SELECT a FROM AgentApplicationEntity a, RelationEntity re " +
            "WHERE a.id = re.toId AND re.toType = 'AGENT_APPLICATION' " +
            "AND re.relationTypeGroup = 'AGENT' " +
            "AND re.relationType = 'ManagedByAgentApp' " +
            "AND re.fromId = :relatedEntityId AND a.tenantId = :tenantId")
    AgentApplicationEntity findByRelatedEntityId(@Param("tenantId") UUID tenantId, @Param("relatedEntityId") UUID relatedEntityId);

    @Query("SELECT re.fromId FROM RelationEntity re, AgentApplicationEntity a " +
            "WHERE a.id = re.toId AND re.toType = 'AGENT_APPLICATION' " +
            "AND re.relationTypeGroup = 'AGENT' AND re.relationType = 'ManagedByAgentApp' " +
            "AND re.fromType = :relatedEntityType AND a.tenantId = :tenantId")
    List<UUID> findManagedRelatedEntityIds(@Param("tenantId") UUID tenantId,
                                           @Param("relatedEntityType") String relatedEntityType);

    @Query("SELECT a FROM AgentApplicationEntity a, RelationEntity re " +
            "WHERE a.id = re.toId AND re.toType = 'AGENT_APPLICATION' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId = :groupId AND re.fromType = 'ENTITY_GROUP' " +
            "AND (:textSearch IS NULL OR ilike(a.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<AgentApplicationEntity> findByEntityGroupId(@Param("groupId") UUID groupId,
                                                     @Param("textSearch") String textSearch,
                                                     Pageable pageable);

    @Query("SELECT a FROM AgentApplicationEntity a, RelationEntity re " +
            "WHERE a.id = re.toId AND re.toType = 'AGENT_APPLICATION' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId IN :groupIds AND re.fromType = 'ENTITY_GROUP' " +
            "AND (:textSearch IS NULL OR ilike(a.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<AgentApplicationEntity> findByEntityGroupIds(@Param("groupIds") List<UUID> groupIds,
                                                      @Param("textSearch") String textSearch,
                                                      Pageable pageable);

}
