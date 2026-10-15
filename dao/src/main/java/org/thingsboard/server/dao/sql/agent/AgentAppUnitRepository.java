// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.agent;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.agent.AgentAppUnitType;
import org.thingsboard.server.dao.model.sql.AgentAppUnitEntity;
import org.thingsboard.server.dao.model.sql.AgentAppUnitInfoEntity;

import java.util.List;
import java.util.UUID;

public interface AgentAppUnitRepository extends JpaRepository<AgentAppUnitEntity, UUID> {

    List<AgentAppUnitEntity> findByAgentApplicationId(UUID agentApplicationId);

    @Query("SELECT u FROM AgentAppUnitEntity u INNER JOIN AgentApplicationEntity app ON u.agentApplicationId = app.id " +
            "WHERE app.tenantId = :tenantId AND app.agentId = :agentId " +
            "AND app.projectName = :projectName AND u.identifier = :identifier AND u.type = :type")
    AgentAppUnitEntity findByAgentAndProjectAndIdentifier(@Param("tenantId") UUID tenantId,
                                                          @Param("agentId") UUID agentId,
                                                          @Param("projectName") String projectName,
                                                          @Param("identifier") String identifier,
                                                          @Param("type") AgentAppUnitType type);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.AgentAppUnitInfoEntity(u, app.agentId, app.projectName) " +
            "FROM AgentAppUnitEntity u INNER JOIN AgentApplicationEntity app ON u.agentApplicationId = app.id " +
            "WHERE app.tenantId = :tenantId AND u.id = :id")
    AgentAppUnitInfoEntity findInfoById(@Param("tenantId") UUID tenantId, @Param("id") UUID id);

    @Query(value = """
           SELECT * FROM agent_app_unit u
           WHERE u.agent_application_id = :applicationId
           AND (CAST(:type AS varchar) IS NULL OR u.type = CAST(:type AS varchar))
           AND (CAST(:textSearch AS varchar) IS NULL
                OR LOWER(u.identifier) LIKE LOWER(CONCAT('%', CAST(:textSearch AS varchar), '%')))
           """,
           countQuery = """
           SELECT count(*) FROM agent_app_unit u
           WHERE u.agent_application_id = :applicationId
           AND (CAST(:type AS varchar) IS NULL OR u.type = CAST(:type AS varchar))
           AND (CAST(:textSearch AS varchar) IS NULL
                OR LOWER(u.identifier) LIKE LOWER(CONCAT('%', CAST(:textSearch AS varchar), '%')))
           """,
           nativeQuery = true)
    Page<AgentAppUnitEntity> findByFilter(@Param("applicationId") UUID applicationId,
                                          @Param("type") String type,
                                          @Param("textSearch") String textSearch,
                                          Pageable pageable);

    @Transactional
    @Modifying
    @Query("DELETE FROM AgentAppUnitEntity e WHERE e.agentApplicationId = :agentApplicationId")
    void deleteByAgentApplicationId(@Param("agentApplicationId") UUID agentApplicationId);

}
