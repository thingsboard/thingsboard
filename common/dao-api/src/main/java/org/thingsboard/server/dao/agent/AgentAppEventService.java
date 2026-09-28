// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.agent;

import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventFilter;
import org.thingsboard.server.common.data.agent.AgentProcessingStatus;
import org.thingsboard.server.common.data.agent.AgentAppEventInfo;
import org.thingsboard.server.common.data.agent.AgentAppEventStatusUpdate;
import org.thingsboard.server.common.data.agent.AgentBulkActionEventStats;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentBulkActionId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface AgentAppEventService extends EntityDaoService {

    AgentAppEvent save(TenantId tenantId, AgentAppEvent event);

    AgentAppEvent save(TenantId tenantId, AgentAppEvent event, boolean doValidate);

    AgentAppEvent findById(TenantId tenantId, AgentAppEventId id);

    Optional<AgentAppEvent> findOldestPendingByApplicationId(AgentApplicationId applicationId);

    boolean hasActiveEventForApplication(AgentApplicationId applicationId);

    boolean hasActiveOrPendingEventForApplication(AgentApplicationId applicationId);

    boolean existsByApplicationIdAndBulkActionId(AgentApplicationId applicationId, UUID bulkActionId);

    Optional<AgentAppEvent> findActiveDeliveredByApplicationId(AgentApplicationId applicationId);

    boolean markDelivered(AgentAppEventId id);

    boolean updateStatus(AgentAppEventId id, AgentAppEventStatusUpdate update);

    boolean updateResolvedArguments(AgentAppEventId id, Map<String, String> resolvedArguments);

    void deleteAllPendingByApplicationId(AgentApplicationId applicationId);

    int cleanUpExpiredEvents(long expirationTs, int batchSize);

    PageData<AgentAppEventInfo> findInfosByBulkActionId(AgentBulkActionId bulkActionId, AgentAppEventActionType actionType,
                                                        AgentProcessingStatus processingStatus, PageLink pageLink);

    AgentBulkActionEventStats findEventStatsByBulkActionId(AgentBulkActionId bulkActionId);

    PageData<AgentAppEvent> findByFilter(AgentAppEventFilter filter, PageLink pageLink);

    PageData<AgentAppEvent> findByAgentId(TenantId tenantId, AgentId agentId, PageLink pageLink);

    PageData<AgentAppEventInfo> findInfosByAgentId(TenantId tenantId, AgentId agentId,
                                                   AgentAppEventActionType actionType,
                                                   AgentProcessingStatus processingStatus,
                                                   PageLink pageLink);

    boolean hasActiveOrPendingAgentEvent(AgentId agentId);

    boolean hasActiveOrPendingAppEventForAgent(AgentId agentId);

    /**
     * Set-based variant of {@link #hasActiveOrPendingEventForApplication}: returns the subset of {@code appIds} that
     * currently has an active or pending event, so a caller filtering a page of applications pays one query, not one
     * per application.
     */
    Set<AgentApplicationId> findApplicationIdsWithActiveOrPendingEvents(Collection<AgentApplicationId> appIds);

    /**
     * Set-based variant of {@link #hasActiveOrPendingAgentEvent}: returns the subset of {@code agentIds} that currently
     * has an active or pending agent-scoped event.
     */
    Set<AgentId> findAgentIdsWithActiveOrPendingAgentEvents(Collection<AgentId> agentIds);

    /**
     * Persists all {@code events} in one batch. Used by the bulk-operation path, which inserts one synthetic event per
     * skipped application and would otherwise issue one statement per row.
     */
    List<AgentAppEvent> saveAll(TenantId tenantId, List<AgentAppEvent> events);

    Optional<AgentAppEvent> findActiveDeliveredAgentEventByAgentId(AgentId agentId);

    List<AgentApplicationId> findApplicationIdsWithOutstandingEvents(AgentId agentId);

    Optional<AgentAppEvent> findOldestPendingAgentEventByAgentId(AgentId agentId);

    Optional<AgentAppEvent> findLatestAgentEventByAgentId(AgentId agentId);

    boolean claimFinalizeWinner(AgentAppEventId id, String containerId);

    boolean updateFinalizeDeadlineIfAbsent(AgentAppEventId id, long deadlineTs);

    PageData<AgentAppEvent> findStuckAgentEvents(long deadline, long stale, PageLink pageLink);

    boolean updateActivityUnguarded(AgentAppEventId id, String activity);

    /**
     * Merges {@code values} into the stored event's context metadata with a single atomic statement and mirrors the
     * result onto {@code event}. Returns false when no row was updated (the event is gone).
     */
    boolean mergeContextMetadata(AgentAppEvent event, Map<String, String> values);
}
