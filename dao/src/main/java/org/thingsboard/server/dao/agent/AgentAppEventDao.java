// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.agent;

import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventFilter;
import org.thingsboard.server.common.data.agent.AgentProcessingStatus;
import org.thingsboard.server.common.data.agent.AgentAppEventInfo;
import org.thingsboard.server.common.data.agent.AgentAppEventStatusUpdate;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface AgentAppEventDao extends Dao<AgentAppEvent> {

    Optional<AgentAppEvent> findOldestPendingByApplicationId(UUID applicationId);

    boolean hasActiveEventForApplication(UUID applicationId);

    boolean hasActiveOrPendingEventForApplication(UUID applicationId);

    boolean existsByApplicationIdAndBulkActionId(UUID applicationId, UUID bulkActionId);

    Optional<AgentAppEvent> findActiveDeliveredByApplicationId(UUID applicationId);

    boolean markDelivered(UUID eventId);

    boolean updateStatus(UUID eventId, AgentAppEventStatusUpdate update);

    boolean updateActivityUnguarded(UUID eventId, String activity);

    boolean updateResolvedArguments(UUID eventId, Map<String, String> resolvedArguments);

    void deleteAllPendingByApplicationId(UUID applicationId);

    int cleanUpExpiredEvents(long expirationTs, int batchSize);

    int deleteByTenantId(TenantId tenantId);

    PageData<AgentAppEventInfo> findInfosByBulkActionId(UUID bulkActionId, AgentAppEventActionType actionType,
                                                        AgentProcessingStatus processingStatus, PageLink pageLink);

    Map<AgentProcessingStatus, Long> countByBulkActionIdGroupByProcessingStatus(UUID bulkActionId);

    PageData<AgentAppEvent> findByFilter(AgentAppEventFilter filter, PageLink pageLink);

    PageData<AgentAppEvent> findByTenantIdAndAgentId(TenantId tenantId, AgentId agentId, PageLink pageLink);

    PageData<AgentAppEventInfo> findInfosByTenantIdAndAgentId(TenantId tenantId, AgentId agentId,
                                                              AgentAppEventActionType actionType,
                                                              AgentProcessingStatus processingStatus,
                                                              PageLink pageLink);

    boolean hasActiveOrPendingAgentEvent(UUID agentId);

    boolean hasActiveOrPendingAppEventForAgent(UUID agentId);

    Set<AgentApplicationId> findApplicationIdsWithActiveOrPendingEvents(Collection<UUID> appIds);

    Set<AgentId> findAgentIdsWithActiveOrPendingAgentEvents(Collection<UUID> agentIds);

    List<AgentAppEvent> saveAll(TenantId tenantId, List<AgentAppEvent> events);

    boolean claimFinalizeWinner(UUID eventId, String containerId);

    boolean updateFinalizeDeadlineIfAbsent(UUID eventId, long deadlineTs);

    /**
     * Merges {@code contextMetadata} into the event's existing context_metadata in a single statement, so two writers
     * cannot lose each other's keys the way a read-modify-write would.
     */
    boolean mergeContextMetadata(UUID eventId, Map<String, String> contextMetadata);

    Optional<AgentAppEvent> findActiveDeliveredAgentEventByAgentId(UUID agentId);

    List<AgentApplicationId> findApplicationIdsWithOutstandingEvents(UUID agentId);

    Optional<AgentAppEvent> findOldestPendingAgentEventByAgentId(UUID agentId);

    Optional<AgentAppEvent> findLatestAgentEventByAgentId(UUID agentId);

    PageData<AgentAppEvent> findStuckAgentEvents(long deadline, long stale, PageLink pageLink);
}
