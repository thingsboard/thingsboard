// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityType;
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
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.agent.AgentAppEventDao;
import org.thingsboard.server.dao.model.sql.AgentAppEventEntity;
import org.thingsboard.server.dao.model.sql.AgentAppEventInfoEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@SqlDao
@Slf4j
public class JpaAgentAppEventDao extends JpaAbstractDao<AgentAppEventEntity, AgentAppEvent> implements AgentAppEventDao {

    @Autowired
    private AgentAppEventRepository repository;

    private static final Map<String, String> EVENT_COLUMN_MAP = Map.of(
            "createdTime", "created_time",
            "updatedTime", "updated_time",
            "actionType", "action_type",
            "startStatus", "start_status",
            "processingStatus", "processing_status"
    );

    @Override
    protected Class<AgentAppEventEntity> getEntityClass() {
        return AgentAppEventEntity.class;
    }

    @Override
    protected JpaRepository<AgentAppEventEntity, UUID> getRepository() {
        return repository;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT_APP_EVENT;
    }

    @Override
    public Optional<AgentAppEvent> findOldestPendingByApplicationId(UUID applicationId) {
        return repository.findOldestPendingByApplicationId(applicationId).map(AgentAppEventEntity::toData);
    }

    @Override
    public boolean hasActiveEventForApplication(UUID applicationId) {
        return repository.hasActiveEventForApplication(applicationId);
    }

    @Override
    public boolean hasActiveOrPendingEventForApplication(UUID applicationId) {
        return repository.hasActiveOrPendingEventForApplication(applicationId);
    }

    @Override
    public boolean existsByApplicationIdAndBulkActionId(UUID applicationId, UUID bulkActionId) {
        return repository.existsByApplicationIdAndBulkActionId(applicationId, bulkActionId);
    }

    @Override
    public Optional<AgentAppEvent> findActiveDeliveredByApplicationId(UUID applicationId) {
        return repository.findActiveDeliveredByApplicationId(applicationId).map(AgentAppEventEntity::toData);
    }

    @Override
    public boolean markDelivered(UUID eventId) {
        return repository.markDelivered(eventId, System.currentTimeMillis()) == 1;
    }

    @Override
    public boolean updateStatus(UUID eventId, AgentAppEventStatusUpdate update) {
        return repository.updateStatus(eventId, update.getProcessingStatus(), update.getCurrentStepId(),
                update.getCurrentActivity(), update.getErrorMessage(), System.currentTimeMillis()) == 1;
    }

    @Override
    public boolean updateResolvedArguments(UUID eventId, Map<String, String> resolvedArguments) {
        return repository.updateResolvedArguments(eventId, JacksonUtil.toString(resolvedArguments)) == 1;
    }

    @Override
    public void deleteAllPendingByApplicationId(UUID applicationId) {
        repository.deleteAllPendingByApplicationId(applicationId);
    }

    @Override
    public int cleanUpExpiredEvents(long expirationTs, int batchSize) {
        int removed = 0;
        int batchRemoved;
        do {
            batchRemoved = repository.deleteEventsUpdatedBeforeBatch(expirationTs, batchSize);
            removed += batchRemoved;
        } while (batchRemoved >= batchSize);
        return removed;
    }

    @Override
    public int deleteByTenantId(TenantId tenantId) {
        return repository.deleteByTenantId(tenantId.getId());
    }

    @Override
    public PageData<AgentAppEventInfo> findInfosByBulkActionId(UUID bulkActionId, AgentAppEventActionType actionType,
                                                               AgentProcessingStatus processingStatus, PageLink pageLink) {
        return DaoUtil.pageToPageData(
                repository.findInfosByBulkActionId(
                                bulkActionId,
                                actionType,
                                processingStatus,
                                normalizeTextSearch(pageLink),
                                DaoUtil.toPageable(pageLink))
                        .map(AgentAppEventInfoEntity::toData)
        );
    }

    @Override
    public Map<AgentProcessingStatus, Long> countByBulkActionIdGroupByProcessingStatus(UUID bulkActionId) {
        Map<AgentProcessingStatus, Long> counts = new EnumMap<>(AgentProcessingStatus.class);
        for (Object[] row : repository.countByBulkActionIdGroupByProcessingStatus(bulkActionId)) {
            counts.put((AgentProcessingStatus) row[0], (Long) row[1]);
        }
        return counts;
    }

    @Override
    public PageData<AgentAppEvent> findByFilter(AgentAppEventFilter filter, PageLink pageLink) {
        return DaoUtil.pageToPageData(
                repository.findByFilter(
                                filter.getTenantId().getId(),
                                filter.getApplicationId().getId(),
                                nameOrNull(filter.getActionType()),
                                nameOrNull(filter.getProcessingStatus()),
                                normalizeTextSearch(pageLink),
                                DaoUtil.toPageable(pageLink, EVENT_COLUMN_MAP))
                        .map(AgentAppEventEntity::toData)
        );
    }

    private static String normalizeTextSearch(PageLink pageLink) {
        String ts = pageLink.getTextSearch();
        return (ts == null || ts.isBlank()) ? null : ts.trim();
    }

    private static String nameOrNull(Enum<?> value) {
        return value != null ? value.name() : null;
    }

    @Override
    public PageData<AgentAppEvent> findByTenantIdAndAgentId(TenantId tenantId, AgentId agentId, PageLink pageLink) {
        return DaoUtil.pageToPageData(
                repository.findByTenantIdAndAgentId(
                                tenantId.getId(),
                                agentId.getId(),
                                DaoUtil.toPageable(pageLink))
                        .map(AgentAppEventEntity::toData)
        );
    }

    @Override
    public PageData<AgentAppEventInfo> findInfosByTenantIdAndAgentId(TenantId tenantId, AgentId agentId,
                                                                     AgentAppEventActionType actionType,
                                                                     AgentProcessingStatus processingStatus,
                                                                     PageLink pageLink) {
        return DaoUtil.pageToPageData(
                repository.findInfosByTenantIdAndAgentId(
                                tenantId.getId(),
                                agentId.getId(),
                                actionType,
                                processingStatus,
                                normalizeTextSearch(pageLink),
                                DaoUtil.toPageable(pageLink))
                        .map(AgentAppEventInfoEntity::toData)
        );
    }

    @Override
    public boolean hasActiveOrPendingAgentEvent(UUID agentId) {
        return repository.hasActiveOrPendingAgentEvent(agentId);
    }

    @Override
    public boolean hasActiveOrPendingAppEventForAgent(UUID agentId) {
        return repository.hasActiveOrPendingAppEventForAgent(agentId);
    }

    @Override
    public Set<AgentApplicationId> findApplicationIdsWithActiveOrPendingEvents(Collection<UUID> appIds) {
        if (appIds.isEmpty()) {
            return Set.of();
        }
        return repository.findApplicationIdsWithActiveOrPendingEvents(appIds)
                .stream().map(AgentApplicationId::new).collect(Collectors.toSet());
    }

    @Override
    public Set<AgentId> findAgentIdsWithActiveOrPendingAgentEvents(Collection<UUID> agentIds) {
        if (agentIds.isEmpty()) {
            return Set.of();
        }
        return repository.findAgentIdsWithActiveOrPendingAgentEvents(agentIds)
                .stream().map(AgentId::new).collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public List<AgentAppEvent> saveAll(TenantId tenantId, List<AgentAppEvent> events) {
        long now = System.currentTimeMillis();
        List<AgentAppEventEntity> entities = events.stream()
                .map(event -> {
                    AgentAppEventEntity entity = new AgentAppEventEntity(event);
                    if (entity.getCreatedTime() == 0) {
                        entity.setCreatedTime(now);
                    }
                    return entity;
                })
                .toList();
        return DaoUtil.convertDataList(repository.saveAll(entities));
    }

    @Override
    public Optional<AgentAppEvent> findActiveDeliveredAgentEventByAgentId(UUID agentId) {
        return repository.findActiveDeliveredAgentEventByAgentId(agentId).map(AgentAppEventEntity::toData);
    }

    @Override
    public Optional<AgentAppEvent> findOldestPendingAgentEventByAgentId(UUID agentId) {
        return repository.findOldestPendingAgentEventByAgentId(agentId).map(AgentAppEventEntity::toData);
    }

    @Override
    public Optional<AgentAppEvent> findLatestAgentEventByAgentId(UUID agentId) {
        return repository.findLatestAgentEventByAgentId(agentId).map(AgentAppEventEntity::toData);
    }

    @Override
    public boolean claimFinalizeWinner(UUID eventId, String containerId) {
        return repository.claimFinalizeWinner(eventId, containerId, System.currentTimeMillis()) == 1;
    }

    @Override
    public boolean updateFinalizeDeadlineIfAbsent(UUID eventId, long deadlineTs) {
        return repository.updateFinalizeDeadlineIfAbsent(eventId, deadlineTs, System.currentTimeMillis()) == 1;
    }

    @Override
    public List<AgentApplicationId> findApplicationIdsWithOutstandingEvents(UUID agentId) {
        return repository.findApplicationIdsWithOutstandingEvents(agentId)
                .stream().map(AgentApplicationId::new).toList();
    }

    @Override
    public PageData<AgentAppEvent> findStuckAgentEvents(long deadline, long stale, PageLink pageLink) {
        return DaoUtil.toPageData(repository.findStuckAgentEvents(deadline, stale, DaoUtil.toPageable(pageLink)));
    }

    @Override
    public boolean updateActivityUnguarded(UUID eventId, String activity) {
        return repository.updateActivityUnguarded(eventId, activity, System.currentTimeMillis()) == 1;
    }

    @Override
    public boolean mergeContextMetadata(UUID eventId, Map<String, String> contextMetadata) {
        return repository.mergeContextMetadata(eventId, JacksonUtil.toString(contextMetadata)) == 1;
    }
}
