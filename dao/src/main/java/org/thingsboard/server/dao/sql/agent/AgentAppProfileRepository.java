// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.agent;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.dao.model.sql.AgentAppProfileEntity;
import org.thingsboard.server.dao.model.sql.AgentAppProfileInfoEntity;
import org.thingsboard.server.dao.model.sql.AgentAppProfileRelationInfoEntity;

import java.util.List;
import java.util.UUID;

public interface AgentAppProfileRepository extends JpaRepository<AgentAppProfileEntity, UUID> {

    @Query("SELECT new org.thingsboard.server.dao.model.sql.AgentAppProfileInfoEntity(p, p.templateVersion) " +
            "FROM AgentAppProfileEntity p " +
            "WHERE p.id = :profileId")
    AgentAppProfileInfoEntity findInfoById(@Param("profileId") UUID profileId);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.AgentAppProfileInfoEntity(p, p.templateVersion) " +
            "FROM AgentAppProfileEntity p " +
            "WHERE p.tenantId = :tenantId AND p.appType = :appType " +
            "ORDER BY p.name ASC")
    List<AgentAppProfileInfoEntity> findInfosByTenantIdAndAppType(@Param("tenantId") UUID tenantId,
                                                                  @Param("appType") AgentApplicationType appType);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.AgentAppProfileRelationInfoEntity(p, p.templateVersion, r.fromId, " +
            "(SELECT COUNT(a) FROM AgentApplicationEntity a " +
            " JOIN AgentEntity ag ON ag.id = a.agentId " +
            " WHERE a.applicationProfileId = p.id AND ag.agentProfileId = :agentProfileId), r.additionalInfo) " +
            "FROM AgentAppProfileEntity p " +
            "JOIN RelationEntity r ON r.toId = p.id " +
            "WHERE r.fromId = :agentProfileId " +
            "AND r.fromType = 'AGENT_PROFILE' " +
            "AND r.relationTypeGroup = 'AGENT' " +
            "AND r.relationType = :relationType " +
            "ORDER BY p.name ASC")
    List<AgentAppProfileRelationInfoEntity> findRelationInfosByAgentProfileId(@Param("agentProfileId") UUID agentProfileId,
                                                                              @Param("relationType") String relationType);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.AgentAppProfileRelationInfoEntity(p, p.templateVersion, r.fromId, r.additionalInfo) " +
            "FROM AgentAppProfileEntity p " +
            "JOIN RelationEntity r ON r.toId = p.id " +
            "WHERE r.fromId = :agentProfileId " +
            "AND r.fromType = 'AGENT_PROFILE' " +
            "AND r.relationTypeGroup = 'AGENT' " +
            "AND r.relationType = :relationType " +
            "AND p.appType = :appType " +
            "AND p.templateVersion = :templateVersion " +
            "ORDER BY p.name ASC")
    List<AgentAppProfileRelationInfoEntity> findRelationInfosByAgentProfileIdAndAppTypeAndTemplateVersion(@Param("agentProfileId") UUID agentProfileId,
                                                                                                     @Param("appType") AgentApplicationType appType,
                                                                                                     @Param("templateVersion") String templateVersion,
                                                                                                     @Param("relationType") String relationType);

    @Query("SELECT p FROM AgentAppProfileEntity p WHERE p.tenantId = :tenantId " +
            "AND p.appType = :appType AND p.templateVersion = :templateVersion " +
            "ORDER BY p.createdTime ASC, p.id ASC")
    List<AgentAppProfileEntity> findByTenantIdAndAppTypeAndTemplateVersion(@Param("tenantId") UUID tenantId,
                                                                           @Param("appType") AgentApplicationType appType,
                                                                           @Param("templateVersion") String templateVersion);

    @Query("SELECT p FROM AgentAppProfileEntity p WHERE p.tenantId = :tenantId " +
            "AND (:textSearch IS NULL OR ilike(p.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<AgentAppProfileEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                               @Param("textSearch") String textSearch,
                                               Pageable pageable);

    @Query("SELECT count(*) FROM AgentAppProfileEntity p WHERE p.tenantId = :tenantId")
    Long countByTenantId(@Param("tenantId") UUID tenantId);

    List<AgentAppProfileEntity> findByTenantIdAndIdIn(UUID tenantId, List<UUID> profileIds);

    @Query("SELECT p FROM AgentAppProfileEntity p " +
            "JOIN RelationEntity r ON r.toId = p.id " +
            "WHERE r.fromId = :agentProfileId " +
            "AND r.fromType = 'AGENT_PROFILE' " +
            "AND r.relationTypeGroup = 'AGENT' " +
            "AND r.relationType = :relationType " +
            "AND NOT EXISTS (SELECT 1 FROM AgentApplicationEntity a " +
            "                WHERE a.agentId = :agentId " +
            "                AND ((p.appType = org.thingsboard.server.common.data.agent.AgentApplicationType.GENERIC " +
            "                      AND a.applicationProfileId = p.id) " +
            "                  OR (p.appType <> org.thingsboard.server.common.data.agent.AgentApplicationType.GENERIC " +
            "                      AND (a.applicationProfileId = p.id " +
            "                        OR (a.appType = p.appType AND a.templateVersion = p.templateVersion)))))")
    List<AgentAppProfileEntity> findUninstalledAppProfilesForAgentProfile(@Param("agentProfileId") UUID agentProfileId,
                                                                          @Param("agentId") UUID agentId,
                                                                          @Param("relationType") String relationType);

    @Query("SELECT p FROM AgentAppProfileEntity p " +
            "JOIN RelationEntity r ON r.toId = p.id " +
            "WHERE r.fromId = :agentProfileId " +
            "AND r.fromType = 'AGENT_PROFILE' " +
            "AND r.relationTypeGroup = 'AGENT' " +
            "AND r.relationType = :relationType " +
            "AND NOT EXISTS (SELECT 1 FROM AgentApplicationEntity a " +
            "                WHERE a.agentId = :agentId " +
            "                AND ((p.appType = org.thingsboard.server.common.data.agent.AgentApplicationType.GENERIC " +
            "                      AND a.applicationProfileId = p.id) " +
            "                  OR (p.appType <> org.thingsboard.server.common.data.agent.AgentApplicationType.GENERIC " +
            "                      AND a.appType = p.appType))) " +
            "ORDER BY p.createdTime ASC, p.id ASC")
    List<AgentAppProfileEntity> findUninstalledAppProfilesByAppTypeForAgentProfile(@Param("agentProfileId") UUID agentProfileId,
                                                                                   @Param("agentId") UUID agentId,
                                                                                   @Param("relationType") String relationType);
}
