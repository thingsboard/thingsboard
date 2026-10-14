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
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentProcessingStatus;
import org.thingsboard.server.dao.model.sql.AgentAppEventEntity;
import org.thingsboard.server.dao.model.sql.AgentAppEventInfoEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgentAppEventRepository extends JpaRepository<AgentAppEventEntity, UUID> {

    @Query("""
           SELECT e FROM AgentAppEventEntity e
           WHERE e.applicationId = :appId AND e.startStatus = 'PENDING'
           ORDER BY e.createdTime ASC LIMIT 1
           """)
    Optional<AgentAppEventEntity> findOldestPendingByApplicationId(@Param("appId") UUID applicationId);

    @Query("""
           SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM AgentAppEventEntity e
           WHERE e.applicationId = :appId AND e.startStatus = 'DELIVERED'
           AND (e.processingStatus IS NULL OR e.processingStatus NOT IN ('FINISHED', 'ERROR'))
           """)
    boolean hasActiveEventForApplication(@Param("appId") UUID applicationId);

    @Query("""
           SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM AgentAppEventEntity e
           WHERE e.applicationId = :appId
                      AND (
                            (e.startStatus = 'DELIVERED' AND (e.processingStatus IS NULL OR e.processingStatus NOT IN ('FINISHED', 'ERROR')))
                            OR (e.startStatus = 'PENDING')
                      )
           """)
    boolean hasActiveOrPendingEventForApplication(@Param("appId") UUID applicationId);

    @Query("""
           SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM AgentAppEventEntity e
           WHERE e.applicationId = :appId AND e.bulkActionId = :bulkActionId
           """)
    boolean existsByApplicationIdAndBulkActionId(@Param("appId") UUID applicationId, @Param("bulkActionId") UUID bulkActionId);

    @Query("""
           SELECT e FROM AgentAppEventEntity e
           WHERE e.applicationId = :appId AND e.startStatus = 'DELIVERED'
           AND (e.processingStatus IS NULL OR e.processingStatus NOT IN ('FINISHED', 'ERROR'))
           ORDER BY e.createdTime ASC LIMIT 1
           """)
    Optional<AgentAppEventEntity> findActiveDeliveredByApplicationId(@Param("appId") UUID applicationId);

    @Transactional
    @Modifying
    @Query("""
           UPDATE AgentAppEventEntity e SET e.startStatus = 'DELIVERED', e.updatedTime = :now
           WHERE e.id = :eventId AND e.startStatus = 'PENDING'
           """)
    int markDelivered(@Param("eventId") UUID eventId, @Param("now") long now);

    @Transactional
    @Modifying
    @Query("""
           UPDATE AgentAppEventEntity e SET
                       e.processingStatus = COALESCE(:processingStatus, e.processingStatus),
                       e.currentStepId = COALESCE(:stepId, e.currentStepId),
                       e.currentActivity = COALESCE(:activity, e.currentActivity),
                       e.errorMessage = COALESCE(:errorMessage, e.errorMessage),
                       e.updatedTime = :now
           WHERE e.id = :eventId
           AND (e.processingStatus IS NULL OR e.processingStatus NOT IN ('FINISHED', 'ERROR'))
           """)
    int updateStatus(@Param("eventId") UUID eventId, @Param("processingStatus") AgentProcessingStatus processingStatus,
                     @Param("stepId") UUID currentStepId, @Param("activity") String currentActivity,
                     @Param("errorMessage") String errorMessage, @Param("now") long now);

    @Transactional
    @Modifying
    @Query(value = "UPDATE agent_app_event SET resolved_arguments = cast(:json as jsonb) WHERE id = :eventId", nativeQuery = true)
    int updateResolvedArguments(@Param("eventId") UUID eventId, @Param("json") String json);

    @Transactional
    @Modifying
    @Query(value = "UPDATE agent_app_event SET context_metadata = COALESCE(context_metadata, '{}'::jsonb) || cast(:json as jsonb) WHERE id = :eventId", nativeQuery = true)
    int mergeContextMetadata(@Param("eventId") UUID eventId, @Param("json") String json);

    @Query("""
           SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM AgentAppEventEntity e
           WHERE e.agentId = :agentId AND e.agentScoped = true
           AND e.startStatus IN ('PENDING', 'DELIVERED')
           AND (e.processingStatus IS NULL OR e.processingStatus NOT IN ('FINISHED', 'ERROR'))
           """)
    boolean hasActiveOrPendingAgentEvent(@Param("agentId") UUID agentId);

    @Query("""
           SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM AgentAppEventEntity e
           WHERE e.agentId = :agentId AND e.agentScoped = false
           AND e.startStatus IN ('PENDING', 'DELIVERED')
           AND (e.processingStatus IS NULL OR e.processingStatus NOT IN ('FINISHED', 'ERROR'))
           """)
    boolean hasActiveOrPendingAppEventForAgent(@Param("agentId") UUID agentId);

    @Query("""
           SELECT DISTINCT e.applicationId FROM AgentAppEventEntity e
           WHERE e.applicationId IN :appIds
           AND (
                 (e.startStatus = 'DELIVERED' AND (e.processingStatus IS NULL OR e.processingStatus NOT IN ('FINISHED', 'ERROR')))
                 OR (e.startStatus = 'PENDING')
           )
           """)
    List<UUID> findApplicationIdsWithActiveOrPendingEvents(@Param("appIds") Collection<UUID> appIds);

    @Query("""
           SELECT DISTINCT e.agentId FROM AgentAppEventEntity e
           WHERE e.agentId IN :agentIds AND e.agentScoped = true
           AND e.startStatus IN ('PENDING', 'DELIVERED')
           AND (e.processingStatus IS NULL OR e.processingStatus NOT IN ('FINISHED', 'ERROR'))
           """)
    List<UUID> findAgentIdsWithActiveOrPendingAgentEvents(@Param("agentIds") Collection<UUID> agentIds);

    @Query("""
           SELECT e FROM AgentAppEventEntity e
           WHERE e.agentId = :agentId AND e.agentScoped = true
           AND e.startStatus = 'DELIVERED'
           AND (e.processingStatus IS NULL OR e.processingStatus NOT IN ('FINISHED', 'ERROR'))
           ORDER BY e.createdTime ASC LIMIT 1
           """)
    Optional<AgentAppEventEntity> findActiveDeliveredAgentEventByAgentId(@Param("agentId") UUID agentId);

    @Query("""
           SELECT DISTINCT e.applicationId FROM AgentAppEventEntity e
           WHERE e.agentId = :agentId AND e.agentScoped = false AND e.applicationId IS NOT NULL
           AND (e.startStatus = 'PENDING'
                OR (e.startStatus = 'DELIVERED' AND (e.processingStatus IS NULL OR e.processingStatus NOT IN ('FINISHED', 'ERROR'))))
           """)
    List<UUID> findApplicationIdsWithOutstandingEvents(@Param("agentId") UUID agentId);

    @Query("""
           SELECT e FROM AgentAppEventEntity e
           WHERE e.agentId = :agentId AND e.agentScoped = true
           AND e.startStatus = 'PENDING'
           AND (e.processingStatus IS NULL OR e.processingStatus NOT IN ('FINISHED', 'ERROR'))
           ORDER BY e.createdTime ASC LIMIT 1
           """)
    Optional<AgentAppEventEntity> findOldestPendingAgentEventByAgentId(@Param("agentId") UUID agentId);

    @Query("""
           SELECT e FROM AgentAppEventEntity e
           WHERE e.agentId = :agentId AND e.agentScoped = true
           ORDER BY e.createdTime DESC LIMIT 1
           """)
    Optional<AgentAppEventEntity> findLatestAgentEventByAgentId(@Param("agentId") UUID agentId);

    @Transactional
    @Modifying
    @Query("""
           UPDATE AgentAppEventEntity e SET e.winnerContainerId = :containerId, e.updatedTime = :now
           WHERE e.id = :eventId AND (e.winnerContainerId IS NULL OR e.winnerContainerId = :containerId)
           """)
    int claimFinalizeWinner(@Param("eventId") UUID eventId, @Param("containerId") String containerId, @Param("now") long now);

    @Transactional
    @Modifying
    @Query("""
           UPDATE AgentAppEventEntity e SET e.finalizeDeadlineTs = :deadlineTs, e.updatedTime = :now
           WHERE e.id = :eventId AND e.finalizeDeadlineTs IS NULL
           """)
    int updateFinalizeDeadlineIfAbsent(@Param("eventId") UUID eventId, @Param("deadlineTs") long deadlineTs, @Param("now") long now);

    @Query("""
           SELECT e FROM AgentAppEventEntity e
           WHERE e.agentScoped = true AND e.startStatus = 'DELIVERED'
           AND (e.processingStatus IS NULL OR e.processingStatus NOT IN ('FINISHED', 'ERROR'))
           AND ((e.finalizeDeadlineTs IS NOT NULL AND e.finalizeDeadlineTs < :deadline)
                OR (e.finalizeDeadlineTs IS NULL AND e.updatedTime < :stale))
           """)
    Page<AgentAppEventEntity> findStuckAgentEvents(@Param("deadline") long deadline,
                                                   @Param("stale") long stale,
                                                   Pageable pageable);

    @Transactional
    @Modifying
    @Query("""
           UPDATE AgentAppEventEntity e SET e.currentActivity = :activity, e.updatedTime = :now
           WHERE e.id = :eventId
           """)
    int updateActivityUnguarded(@Param("eventId") UUID eventId, @Param("activity") String activity, @Param("now") long now);

    @Transactional
    @Modifying
    @Query("DELETE FROM AgentAppEventEntity e WHERE e.applicationId = :appId AND e.startStatus = 'PENDING'")
    void deleteAllPendingByApplicationId(@Param("appId") UUID applicationId);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM agent_app_event WHERE id IN " +
            "(SELECT id FROM agent_app_event WHERE updated_time < :expirationTs LIMIT :batchSize)",
            nativeQuery = true)
    int deleteEventsUpdatedBeforeBatch(@Param("expirationTs") long expirationTs, @Param("batchSize") int batchSize);

    @Transactional
    @Modifying
    @Query("DELETE FROM AgentAppEventEntity e WHERE e.tenantId = :tenantId")
    int deleteByTenantId(@Param("tenantId") UUID tenantId);

    @Query(value = """
           SELECT * FROM agent_app_event e
           WHERE e.tenant_id = :tenantId AND e.application_id = :applicationId
           AND (CAST(:actionType AS varchar) IS NULL OR e.action_type = CAST(:actionType AS varchar))
           AND (CAST(:processingStatus AS varchar) IS NULL OR e.processing_status = CAST(:processingStatus AS varchar))
           AND (CAST(:textSearch AS varchar) IS NULL
                OR LOWER(e.action_type) LIKE LOWER(CONCAT('%', CAST(:textSearch AS varchar), '%'))
                OR LOWER(e.processing_status) LIKE LOWER(CONCAT('%', CAST(:textSearch AS varchar), '%')))
           """,
           countQuery = """
           SELECT count(*) FROM agent_app_event e
           WHERE e.tenant_id = :tenantId AND e.application_id = :applicationId
           AND (CAST(:actionType AS varchar) IS NULL OR e.action_type = CAST(:actionType AS varchar))
           AND (CAST(:processingStatus AS varchar) IS NULL OR e.processing_status = CAST(:processingStatus AS varchar))
           AND (CAST(:textSearch AS varchar) IS NULL
                OR LOWER(e.action_type) LIKE LOWER(CONCAT('%', CAST(:textSearch AS varchar), '%'))
                OR LOWER(e.processing_status) LIKE LOWER(CONCAT('%', CAST(:textSearch AS varchar), '%')))
           """,
           nativeQuery = true)
    Page<AgentAppEventEntity> findByFilter(@Param("tenantId") UUID tenantId,
                                           @Param("applicationId") UUID applicationId,
                                           @Param("actionType") String actionType,
                                           @Param("processingStatus") String processingStatus,
                                           @Param("textSearch") String textSearch,
                                           Pageable pageable);

    @Query("""
           SELECT e FROM AgentAppEventEntity e
           WHERE e.tenantId = :tenantId AND e.agentId = :agentId
           """)
    Page<AgentAppEventEntity> findByTenantIdAndAgentId(@Param("tenantId") UUID tenantId,
                                                       @Param("agentId") UUID agentId,
                                                       Pageable pageable);

    @Query(value = """
           SELECT new org.thingsboard.server.dao.model.sql.AgentAppEventInfoEntity(e, COALESCE(a.name, e.applicationName), ag.name)
           FROM AgentAppEventEntity e
           LEFT JOIN AgentApplicationEntity a ON e.applicationId = a.id
           LEFT JOIN AgentEntity ag ON e.agentId = ag.id
           WHERE e.tenantId = :tenantId AND e.agentId = :agentId
           AND (:actionType IS NULL OR e.actionType = :actionType)
           AND (:processingStatus IS NULL OR e.processingStatus = :processingStatus)
           AND (:textSearch IS NULL OR ilike(COALESCE(a.name, e.applicationName), CONCAT('%', :textSearch, '%')) = true)
           """,
           countQuery = """
           SELECT COUNT(e)
           FROM AgentAppEventEntity e
           LEFT JOIN AgentApplicationEntity a ON e.applicationId = a.id
           WHERE e.tenantId = :tenantId AND e.agentId = :agentId
           AND (:actionType IS NULL OR e.actionType = :actionType)
           AND (:processingStatus IS NULL OR e.processingStatus = :processingStatus)
           AND (:textSearch IS NULL OR ilike(COALESCE(a.name, e.applicationName), CONCAT('%', :textSearch, '%')) = true)
           """)
    Page<AgentAppEventInfoEntity> findInfosByTenantIdAndAgentId(@Param("tenantId") UUID tenantId,
                                                                @Param("agentId") UUID agentId,
                                                                @Param("actionType") AgentAppEventActionType actionType,
                                                                @Param("processingStatus") AgentProcessingStatus processingStatus,
                                                                @Param("textSearch") String textSearch,
                                                                Pageable pageable);

    @Query(value = """
           SELECT new org.thingsboard.server.dao.model.sql.AgentAppEventInfoEntity(e, COALESCE(a.name, e.applicationName), ag.name)
           FROM AgentAppEventEntity e
           LEFT JOIN AgentApplicationEntity a ON e.applicationId = a.id
           LEFT JOIN AgentEntity ag ON e.agentId = ag.id
           WHERE e.bulkActionId = :bulkActionId
           AND (:actionType IS NULL OR e.actionType = :actionType)
           AND (:processingStatus IS NULL OR e.processingStatus = :processingStatus)
           AND (:textSearch IS NULL
                OR ilike(COALESCE(a.name, e.applicationName), CONCAT('%', :textSearch, '%')) = true
                OR ilike(ag.name, CONCAT('%', :textSearch, '%')) = true)
           """,
           countQuery = """
           SELECT COUNT(e)
           FROM AgentAppEventEntity e
           LEFT JOIN AgentApplicationEntity a ON e.applicationId = a.id
           LEFT JOIN AgentEntity ag ON e.agentId = ag.id
           WHERE e.bulkActionId = :bulkActionId
           AND (:actionType IS NULL OR e.actionType = :actionType)
           AND (:processingStatus IS NULL OR e.processingStatus = :processingStatus)
           AND (:textSearch IS NULL
                OR ilike(COALESCE(a.name, e.applicationName), CONCAT('%', :textSearch, '%')) = true
                OR ilike(ag.name, CONCAT('%', :textSearch, '%')) = true)
           """)
    Page<AgentAppEventInfoEntity> findInfosByBulkActionId(@Param("bulkActionId") UUID bulkActionId,
                                                          @Param("actionType") AgentAppEventActionType actionType,
                                                          @Param("processingStatus") AgentProcessingStatus processingStatus,
                                                          @Param("textSearch") String textSearch,
                                                          Pageable pageable);

    @Query("""
           SELECT e.processingStatus, COUNT(e) FROM AgentAppEventEntity e
           WHERE e.bulkActionId = :bulkActionId AND e.processingStatus IS NOT NULL
           GROUP BY e.processingStatus
           """)
    List<Object[]> countByBulkActionIdGroupByProcessingStatus(@Param("bulkActionId") UUID bulkActionId);
}
