// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.agent;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.cache.agent.AgentAppUnitCacheEvictEvent;
import org.thingsboard.server.cache.agent.AgentAppUnitCacheKey;
import org.thingsboard.server.cache.logexternal.LogChunkBuffer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.agent.AgentAppUnitFilter;
import org.thingsboard.server.common.data.agent.AgentAppUnitInfo;
import org.thingsboard.server.common.data.agent.AgentAppUnitType;
import org.thingsboard.server.common.data.id.AgentAppUnitId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.service.DataValidator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.dao.service.Validator.validateId;

@Service("AgentAppUnitDaoService")
@Slf4j
public class BaseAgentAppUnitService extends AbstractCachedEntityService<AgentAppUnitCacheKey, AgentAppUnit, AgentAppUnitCacheEvictEvent> implements AgentAppUnitService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_AGENT_APP_UNIT_ID = "Incorrect agentAppUnitId ";
    public static final String INCORRECT_AGENT_APPLICATION_ID = "Incorrect agentApplicationId ";

    @Autowired
    private AgentAppUnitDao agentAppUnitDao;

    @Autowired
    private DataValidator<AgentAppUnit> agentAppUnitValidator;

    @Autowired
    private AgentApplicationService agentApplicationService;

    @Autowired
    private LogChunkBuffer logChunkBuffer;

    @Override
    @TransactionalEventListener
    public void handleEvictEvent(AgentAppUnitCacheEvictEvent event) {
        List<AgentAppUnitCacheKey> keys = new ArrayList<>(2);
        keys.add(AgentAppUnitCacheKey.from(event.getAgentAppUnitId()));
        if (event.getTenantId() != null
                && event.getAgentId() != null
                && event.getProjectName() != null
                && event.getIdentifier() != null
                && event.getType() != null) {
            keys.add(AgentAppUnitCacheKey.from(event));
        }
        cache.evict(keys);
        if (event.isDeleted()) {
            logChunkBuffer.deleteUnit(event.getTenantId(), event.getAgentAppUnitId());
        }
    }

    @Override
    @Transactional
    public AgentAppUnit saveAgentAppUnit(TenantId tenantId, AgentAppUnit agentAppUnit) {
        return saveAgentAppUnit(tenantId, agentAppUnit, resolveApplication(tenantId, agentAppUnit));
    }

    @Override
    @Transactional
    public AgentAppUnit saveAgentAppUnit(TenantId tenantId, AgentAppUnit agentAppUnit, AgentApplication app) {
        log.trace("Executing saveAgentAppUnit [{}]", agentAppUnit);
        agentAppUnit.setTenantId(tenantId);
        AgentAppUnit old = agentAppUnitValidator.validate(agentAppUnit, unit -> tenantId);
        AgentAppUnit saved = agentAppUnitDao.save(tenantId, agentAppUnit);
        publishEvictEvent(buildEvictEvent(tenantId, saved, app, false));
        if (old != null && (!Objects.equals(old.getIdentifier(), saved.getIdentifier()) || old.getType() != saved.getType())) {
            publishEvictEvent(buildEvictEvent(tenantId, old, app, false));
        }
        eventPublisher.publishEvent(SaveEntityEvent.builder()
                .tenantId(tenantId)
                .entityId(saved.getId())
                .entity(saved)
                .oldEntity(old)
                .created(agentAppUnit.getId() == null)
                .build());
        return saved;
    }

    @Override
    public AgentAppUnit findAgentAppUnitById(TenantId tenantId, AgentAppUnitId agentAppUnitId) {
        log.trace("Executing findAgentAppUnitById [{}]", agentAppUnitId);
        validateId(agentAppUnitId, id -> INCORRECT_AGENT_APP_UNIT_ID + id);
        return cache.getAndPutInTransaction(AgentAppUnitCacheKey.from(agentAppUnitId),
                () -> agentAppUnitDao.findById(tenantId, agentAppUnitId.getId()), true);
    }

    @Override
    public AgentAppUnitInfo findAgentAppUnitInfoById(TenantId tenantId, AgentAppUnitId agentAppUnitId) {
        log.trace("Executing findAgentAppUnitInfoById [{}]", agentAppUnitId);
        validateId(agentAppUnitId, id -> INCORRECT_AGENT_APP_UNIT_ID + id);
        return agentAppUnitDao.findAgentAppUnitInfoById(tenantId, agentAppUnitId);
    }

    @Override
    public AgentAppUnit findByAgentAndProjectAndIdentifier(TenantId tenantId, AgentId agentId, String projectName,
                                                           String identifier, AgentAppUnitType type) {
        log.trace("Executing findByAgentAndProjectAndIdentifier [{}] [{}] [{}] [{}] [{}]", tenantId, agentId, projectName, identifier, type);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(agentId, id -> "Incorrect agentId " + id);
        if (projectName == null || projectName.isEmpty() || identifier == null || identifier.isEmpty() || type == null) {
            return null;
        }
        return cache.getAndPutInTransaction(
                AgentAppUnitCacheKey.from(tenantId, agentId, projectName, identifier, type),
                () -> agentAppUnitDao.findByAgentAndProjectAndIdentifier(tenantId, agentId, projectName, identifier, type),
                true);
    }

    @Override
    public PageData<AgentAppUnit> findByFilter(AgentAppUnitFilter filter, PageLink pageLink) {
        log.trace("Executing findByFilter [{}]", filter);
        validateId(filter.getTenantId(), id -> INCORRECT_TENANT_ID + id);
        validateId(filter.getApplicationId(), id -> INCORRECT_AGENT_APPLICATION_ID + id);
        return agentAppUnitDao.findByFilter(filter, pageLink);
    }

    @Override
    public List<AgentAppUnit> findAgentAppUnitsByAgentAppId(TenantId tenantId, AgentApplicationId agentAppId) {
        log.trace("Executing findAgentAppUnitsByAgentApplicationId, tenantId [{}], agentApplicationId [{}]", tenantId, agentAppId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(agentAppId, id -> INCORRECT_AGENT_APPLICATION_ID + id);
        return agentAppUnitDao.findByAgentApplicationId(tenantId, agentAppId.getId());
    }

    @Override
    @Transactional
    public void deleteAgentAppUnit(TenantId tenantId, AgentAppUnitId agentAppUnitId) {
        deleteAgentAppUnit(tenantId, agentAppUnitId, null);
    }

    @Override
    @Transactional
    public void deleteAgentAppUnit(TenantId tenantId, AgentAppUnitId agentAppUnitId, AgentApplication app) {
        log.trace("Executing deleteAgentAppUnit [{}]", agentAppUnitId);
        validateId(agentAppUnitId, id -> INCORRECT_AGENT_APP_UNIT_ID + id);
        AgentAppUnit unit = agentAppUnitDao.findById(tenantId, agentAppUnitId.getId());
        if (unit == null) {
            return;
        }
        agentAppUnitDao.removeById(tenantId, agentAppUnitId.getId());
        publishCacheEvictAndDeleteEvent(tenantId, unit, app != null ? app : resolveApplication(tenantId, unit));
    }

    @Override
    @Transactional
    public void deleteByAgentApplicationId(TenantId tenantId, AgentApplicationId agentAppId) {
        log.trace("Executing deleteByAgentApplicationId, tenantId [{}], agentApplicationId [{}]", tenantId, agentAppId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(agentAppId, id -> INCORRECT_AGENT_APPLICATION_ID + id);
        AgentApplication app = agentApplicationService.findById(tenantId, agentAppId);
        agentAppUnitDao.findByAgentApplicationId(tenantId, agentAppId.getId())
                .forEach(u -> publishCacheEvictAndDeleteEvent(tenantId, u, app));
        agentAppUnitDao.removeByAgentApplicationId(tenantId, agentAppId.getId());
    }

    private AgentApplication resolveApplication(TenantId tenantId, AgentAppUnit unit) {
        return unit.getAgentApplicationId() != null
                ? agentApplicationService.findById(tenantId, unit.getAgentApplicationId())
                : null;
    }

    private void publishCacheEvictAndDeleteEvent(TenantId tenantId, AgentAppUnit unit, AgentApplication app) {
        AgentAppUnitId agentAppUnitId = unit.getId();
        publishEvictEvent(buildEvictEvent(tenantId, unit, app, true));
        eventPublisher.publishEvent(DeleteEntityEvent.builder()
                .tenantId(tenantId)
                .entityId(agentAppUnitId)
                .entity(unit)
                .build());
    }

    private AgentAppUnitCacheEvictEvent buildEvictEvent(TenantId tenantId, AgentAppUnit unit, AgentApplication app, boolean deleted) {
        return new AgentAppUnitCacheEvictEvent(unit.getId(), tenantId,
                app != null ? app.getAgentId() : null,
                app != null ? app.getProjectName() : null,
                unit.getIdentifier(),
                unit.getType(),
                deleted);
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findAgentAppUnitById(tenantId, new AgentAppUnitId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        ListenableFuture<AgentAppUnit> future = agentAppUnitDao.findByIdAsync(tenantId, entityId.getId());
        return FluentFuture.from(future).transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT_APP_UNIT;
    }
}
