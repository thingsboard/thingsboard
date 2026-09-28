// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.agent;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
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
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.service.DataValidator;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class BaseAgentAppEventService implements AgentAppEventService {

    private static final String INCORRECT_AGENT_APP_EVENT_ID = "Incorrect agentAppEventId ";

    @Autowired
    private AgentAppEventDao agentAppEventDao;

    @Autowired
    private DataValidator<AgentAppEvent> agentAppEventValidator;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    public AgentAppEvent save(TenantId tenantId, AgentAppEvent event) {
        return save(tenantId, event, true);
    }

    @Override
    public AgentAppEvent save(TenantId tenantId, AgentAppEvent event, boolean doValidate) {
        log.trace("Executing saveAgentAppEvent [{}]", event);
        if (doValidate) {
            agentAppEventValidator.validate(event, AgentAppEvent::getTenantId);
        }
        AgentAppEvent saved = agentAppEventDao.save(event.getTenantId(), event);
        eventPublisher.publishEvent(SaveEntityEvent.builder()
                .tenantId(saved.getTenantId())
                .entityId(saved.getId())
                .entity(saved)
                .created(event.getId() == null)
                .build());
        return saved;
    }

    @Override
    public AgentAppEvent findById(TenantId tenantId, AgentAppEventId id) {
        log.trace("Executing findAgentAppEventById [{}]", id);
        validateId(id, i -> INCORRECT_AGENT_APP_EVENT_ID + i);
        return agentAppEventDao.findById(tenantId, id.getId());
    }

    @Override
    public Optional<AgentAppEvent> findOldestPendingByApplicationId(AgentApplicationId applicationId) {
        log.trace("Executing findOldestPendingByApplicationId [{}]", applicationId);
        return agentAppEventDao.findOldestPendingByApplicationId(applicationId.getId());
    }

    @Override
    public boolean hasActiveEventForApplication(AgentApplicationId applicationId) {
        return agentAppEventDao.hasActiveEventForApplication(applicationId.getId());
    }

    @Override
    public boolean hasActiveOrPendingEventForApplication(AgentApplicationId applicationId) {
        return agentAppEventDao.hasActiveOrPendingEventForApplication(applicationId.getId());
    }

    @Override
    public boolean existsByApplicationIdAndBulkActionId(AgentApplicationId applicationId, UUID bulkActionId) {
        return agentAppEventDao.existsByApplicationIdAndBulkActionId(applicationId.getId(), bulkActionId);
    }

    @Override
    public Optional<AgentAppEvent> findActiveDeliveredByApplicationId(AgentApplicationId applicationId) {
        return agentAppEventDao.findActiveDeliveredByApplicationId(applicationId.getId());
    }

    @Override
    public boolean markDelivered(AgentAppEventId id) {
        log.trace("Executing markDelivered [{}]", id);
        return agentAppEventDao.markDelivered(id.getId());
    }

    @Override
    public boolean updateStatus(AgentAppEventId id, AgentAppEventStatusUpdate update) {
        log.trace("Executing updateStatus [{}] update [{}]", id, update);
        return agentAppEventDao.updateStatus(id.getId(), update);
    }

    @Override
    public boolean updateResolvedArguments(AgentAppEventId id, Map<String, String> resolvedArguments) {
        log.trace("Executing updateResolvedArguments [{}]", id);
        return agentAppEventDao.updateResolvedArguments(id.getId(), resolvedArguments);
    }

    @Override
    public void deleteAllPendingByApplicationId(AgentApplicationId applicationId) {
        log.trace("Executing deleteAllPendingByApplicationId [{}]", applicationId);
        agentAppEventDao.deleteAllPendingByApplicationId(applicationId.getId());
    }

    @Override
    public int cleanUpExpiredEvents(long expirationTs, int batchSize) {
        log.trace("Executing cleanUpExpiredEvents before [{}]", expirationTs);
        return agentAppEventDao.cleanUpExpiredEvents(expirationTs, batchSize);
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        log.trace("Executing deleteByTenantId [{}]", tenantId);
        validateId(tenantId, id -> "Incorrect tenantId " + id);
        int deleted = agentAppEventDao.deleteByTenantId(tenantId);
        log.debug("[{}] Deleted {} agent app events", tenantId, deleted);
    }

    @Override
    public PageData<AgentAppEventInfo> findInfosByBulkActionId(AgentBulkActionId bulkActionId, AgentAppEventActionType actionType,
                                                               AgentProcessingStatus processingStatus, PageLink pageLink) {
        log.trace("Executing findInfosByBulkActionId [{}] actionType [{}] processingStatus [{}]", bulkActionId, actionType, processingStatus);
        validateId(bulkActionId, id -> "Incorrect bulkActionId " + id);
        return agentAppEventDao.findInfosByBulkActionId(bulkActionId.getId(), actionType, processingStatus, pageLink);
    }

    @Override
    public AgentBulkActionEventStats findEventStatsByBulkActionId(AgentBulkActionId bulkActionId) {
        log.trace("Executing findEventStatsByBulkActionId [{}]", bulkActionId);
        validateId(bulkActionId, id -> "Incorrect bulkActionId " + id);
        Map<AgentProcessingStatus, Long> countsByStatus = agentAppEventDao.countByBulkActionIdGroupByProcessingStatus(bulkActionId.getId());
        long total = countsByStatus.values().stream().mapToLong(Long::longValue).sum();
        return new AgentBulkActionEventStats(countsByStatus, total);
    }

    @Override
    public PageData<AgentAppEvent> findByFilter(AgentAppEventFilter filter, PageLink pageLink) {
        log.trace("Executing findAgentAppEventsByFilter [{}]", filter);
        return agentAppEventDao.findByFilter(filter, pageLink);
    }

    @Override
    public PageData<AgentAppEvent> findByAgentId(TenantId tenantId, AgentId agentId, PageLink pageLink) {
        log.trace("Executing findAgentAppEventsByAgentId [{}]", agentId);
        return agentAppEventDao.findByTenantIdAndAgentId(tenantId, agentId, pageLink);
    }

    @Override
    public PageData<AgentAppEventInfo> findInfosByAgentId(TenantId tenantId, AgentId agentId,
                                                          AgentAppEventActionType actionType,
                                                          AgentProcessingStatus processingStatus,
                                                          PageLink pageLink) {
        log.trace("Executing findAgentAppEventInfosByAgentId [{}] actionType [{}] processingStatus [{}]",
                agentId, actionType, processingStatus);
        validateId(tenantId, id -> "Incorrect tenantId " + id);
        validateId(agentId, id -> "Incorrect agentId " + id);
        return agentAppEventDao.findInfosByTenantIdAndAgentId(tenantId, agentId, actionType, processingStatus, pageLink);
    }

    @Override
    public boolean hasActiveOrPendingAgentEvent(AgentId agentId) {
        return agentAppEventDao.hasActiveOrPendingAgentEvent(agentId.getId());
    }

    @Override
    public Set<AgentApplicationId> findApplicationIdsWithActiveOrPendingEvents(Collection<AgentApplicationId> appIds) {
        log.trace("Executing findApplicationIdsWithActiveOrPendingEvents for {} apps", appIds.size());
        return agentAppEventDao.findApplicationIdsWithActiveOrPendingEvents(
                appIds.stream().map(AgentApplicationId::getId).collect(Collectors.toSet()));
    }

    @Override
    public Set<AgentId> findAgentIdsWithActiveOrPendingAgentEvents(Collection<AgentId> agentIds) {
        log.trace("Executing findAgentIdsWithActiveOrPendingAgentEvents for {} agents", agentIds.size());
        return agentAppEventDao.findAgentIdsWithActiveOrPendingAgentEvents(
                agentIds.stream().map(AgentId::getId).collect(Collectors.toSet()));
    }

    @Override
    public List<AgentAppEvent> saveAll(TenantId tenantId, List<AgentAppEvent> events) {
        log.trace("Executing saveAll for {} agent app events", events.size());
        if (events.isEmpty()) {
            return List.of();
        }
        List<AgentAppEvent> saved = agentAppEventDao.saveAll(tenantId, events);
        for (AgentAppEvent event : saved) {
            eventPublisher.publishEvent(SaveEntityEvent.builder()
                    .tenantId(event.getTenantId())
                    .entityId(event.getId())
                    .entity(event)
                    .created(true)
                    .build());
        }
        return saved;
    }

    @Override
    public boolean hasActiveOrPendingAppEventForAgent(AgentId agentId) {
        return agentAppEventDao.hasActiveOrPendingAppEventForAgent(agentId.getId());
    }

    @Override
    public Optional<AgentAppEvent> findActiveDeliveredAgentEventByAgentId(AgentId agentId) {
        return agentAppEventDao.findActiveDeliveredAgentEventByAgentId(agentId.getId());
    }

    @Override
    public Optional<AgentAppEvent> findOldestPendingAgentEventByAgentId(AgentId agentId) {
        return agentAppEventDao.findOldestPendingAgentEventByAgentId(agentId.getId());
    }

    @Override
    public Optional<AgentAppEvent> findLatestAgentEventByAgentId(AgentId agentId) {
        return agentAppEventDao.findLatestAgentEventByAgentId(agentId.getId());
    }

    @Override
    public boolean claimFinalizeWinner(AgentAppEventId id, String containerId) {
        log.trace("Executing claimFinalizeWinner [{}] containerId [{}]", id, containerId);
        return agentAppEventDao.claimFinalizeWinner(id.getId(), containerId);
    }

    @Override
    public boolean updateFinalizeDeadlineIfAbsent(AgentAppEventId id, long deadlineTs) {
        return agentAppEventDao.updateFinalizeDeadlineIfAbsent(id.getId(), deadlineTs);
    }

    @Override
    public PageData<AgentAppEvent> findStuckAgentEvents(long deadline, long stale, PageLink pageLink) {
        return agentAppEventDao.findStuckAgentEvents(deadline, stale, pageLink);
    }

    @Override
    public List<AgentApplicationId> findApplicationIdsWithOutstandingEvents(AgentId agentId) {
        log.trace("Executing findApplicationIdsWithOutstandingEvents [{}]", agentId);
        validateId(agentId, id -> "Incorrect agentId " + id);
        return agentAppEventDao.findApplicationIdsWithOutstandingEvents(agentId.getId());
    }

    @Override
    public boolean updateActivityUnguarded(AgentAppEventId id, String activity) {
        return agentAppEventDao.updateActivityUnguarded(id.getId(), activity);
    }

    @Override
    public boolean mergeContextMetadata(AgentAppEvent event, Map<String, String> values) {
        if (!agentAppEventDao.mergeContextMetadata(event.getUuidId(), values)) {
            return false;
        }
        Map<String, String> merged = new HashMap<>();
        if (event.getContextMetadata() != null) {
            merged.putAll(event.getContextMetadata());
        }
        merged.putAll(values);
        event.setContextMetadata(merged);
        return true;
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findById(tenantId, new AgentAppEventId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        ListenableFuture<AgentAppEvent> future = agentAppEventDao.findByIdAsync(tenantId, entityId.getId());
        return FluentFuture.from(future).transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT_APP_EVENT;
    }
}
