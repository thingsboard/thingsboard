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
import org.thingsboard.server.common.data.agent.AgentBulkAction;
import org.thingsboard.server.common.data.agent.AgentBulkActionStatus;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentBulkActionId;
import org.thingsboard.server.common.data.id.AgentProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;

import java.util.Optional;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class BaseAgentBulkActionService implements AgentBulkActionService {

    private static final String INCORRECT_AGENT_BULK_ACTION_ID = "Incorrect agentBulkActionId ";

    @Autowired
    private AgentBulkActionDao agentBulkActionDao;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    public AgentBulkAction save(TenantId tenantId, AgentBulkAction bulkAction) {
        log.trace("Executing saveAgentBulkAction [{}]", bulkAction);
        AgentBulkAction saved = agentBulkActionDao.save(tenantId, bulkAction);
        eventPublisher.publishEvent(SaveEntityEvent.builder()
                .tenantId(saved.getTenantId())
                .entityId(saved.getId())
                .entity(saved)
                .created(bulkAction.getId() == null)
                .build());
        return saved;
    }

    @Override
    public AgentBulkAction findById(TenantId tenantId, AgentBulkActionId id) {
        log.trace("Executing findAgentBulkActionById [{}]", id);
        validateId(id, i -> INCORRECT_AGENT_BULK_ACTION_ID + i);
        return agentBulkActionDao.findById(tenantId, id.getId());
    }

    @Override
    public PageData<AgentBulkAction> findStuckBulkActions(long threshold, PageLink pageLink) {
        return agentBulkActionDao.findStuckBulkActions(threshold, pageLink);
    }

    @Override
    public PageData<AgentBulkAction> findByAgentProfileId(TenantId tenantId, AgentProfileId agentProfileId, PageLink pageLink) {
        log.trace("Executing findBulkActionsByProfileId [{}]", agentProfileId);
        validateId(tenantId, id -> "Incorrect tenantId " + id);
        validateId(agentProfileId, id -> "Incorrect agentProfileId " + id);
        return agentBulkActionDao.findByAgentProfileId(tenantId, agentProfileId, pageLink);
    }

    @Override
    public PageData<AgentBulkAction> findByAgentProfileIdAndApplicationProfileId(TenantId tenantId,
                                                                                 AgentProfileId agentProfileId,
                                                                                 AgentAppProfileId applicationProfileId,
                                                                                 PageLink pageLink) {
        log.trace("Executing findBulkActionsByProfileIdAndAppProfileId [{}, {}]", agentProfileId, applicationProfileId);
        validateId(tenantId, id -> "Incorrect tenantId " + id);
        validateId(agentProfileId, id -> "Incorrect agentProfileId " + id);
        validateId(applicationProfileId, id -> "Incorrect applicationProfileId " + id);
        return agentBulkActionDao.findByAgentProfileIdAndApplicationProfileId(tenantId, agentProfileId, applicationProfileId, pageLink);
    }

    @Override
    public int cleanUpExpiredBulkActions(long expirationTs, int batchSize) {
        log.trace("Executing cleanUpExpiredBulkActions before [{}]", expirationTs);
        return agentBulkActionDao.cleanUpExpiredBulkActions(expirationTs, batchSize);
    }

    @Override
    public int failIfStillStuck(AgentBulkActionId id, AgentBulkActionStatus status, String errorMsg) {
        log.trace("Executing failIfStillStuck [{}] -> [{}]", id, status);
        validateId(id, i -> "Incorrect agentBulkActionId " + i);
        return agentBulkActionDao.failIfStillStuck(id, status, errorMsg);
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findById(tenantId, new AgentBulkActionId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        ListenableFuture<AgentBulkAction> future = agentBulkActionDao.findByIdAsync(tenantId, entityId.getId());
        return FluentFuture.from(future).transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT_BULK_ACTION;
    }
}
