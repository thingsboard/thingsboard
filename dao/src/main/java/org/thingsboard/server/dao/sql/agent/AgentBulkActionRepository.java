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
import org.thingsboard.server.common.data.agent.AgentBulkActionStatus;
import org.thingsboard.server.dao.model.sql.AgentBulkActionEntity;

import java.util.UUID;

public interface AgentBulkActionRepository extends JpaRepository<AgentBulkActionEntity, UUID> {

    @Query("""
           SELECT a FROM AgentBulkActionEntity a
           WHERE (a.status = 'IN_PROGRESS' AND a.processingStartedTime < :threshold)
              OR (a.status = 'QUEUED' AND a.createdTime < :threshold)
           """)
    Page<AgentBulkActionEntity> findStuckBulkActions(@Param("threshold") long threshold, Pageable pageable);

    @Query("""
           SELECT a FROM AgentBulkActionEntity a
           WHERE a.tenantId = :tenantId AND a.agentProfileId = :agentProfileId
           """)
    Page<AgentBulkActionEntity> findByAgentProfileId(@Param("tenantId") UUID tenantId,
                                                     @Param("agentProfileId") UUID agentProfileId,
                                                     Pageable pageable);

    @Query("""
           SELECT a FROM AgentBulkActionEntity a
           WHERE a.tenantId = :tenantId
             AND a.agentProfileId = :agentProfileId
             AND a.applicationProfileId = :applicationProfileId
           """)
    Page<AgentBulkActionEntity> findByAgentProfileIdAndApplicationProfileId(
            @Param("tenantId") UUID tenantId,
            @Param("agentProfileId") UUID agentProfileId,
            @Param("applicationProfileId") UUID applicationProfileId,
            Pageable pageable);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM agent_app_event WHERE id IN " +
            "(SELECT id FROM agent_app_event WHERE bulk_action_id IS NOT NULL AND created_time < :expirationTs LIMIT :batchSize)",
            nativeQuery = true)
    int deleteEventsByBulkActionCreatedTimeBeforeBatch(@Param("expirationTs") long expirationTs, @Param("batchSize") int batchSize);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM agent_bulk_action WHERE id IN " +
            "(SELECT id FROM agent_bulk_action WHERE created_time < :expirationTs LIMIT :batchSize)",
            nativeQuery = true)
    int deleteBulkActionsCreatedTimeBeforeBatch(@Param("expirationTs") long expirationTs, @Param("batchSize") int batchSize);

    @Transactional
    @Modifying
    @Query("UPDATE AgentBulkActionEntity a SET a.status = :status, a.errorMsg = :errorMsg " +
            "WHERE a.id = :id AND a.status IN ('QUEUED', 'IN_PROGRESS')")
    int failIfStillStuck(@Param("id") UUID id, @Param("status") AgentBulkActionStatus status, @Param("errorMsg") String errorMsg);
}
