// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.agent;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.cache.agent.AgentCacheEvictEvent;
import org.thingsboard.server.cache.agent.AgentCacheKey;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentInfo;
import org.thingsboard.server.common.data.agent.AgentProfile;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.AgentProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.entity.EntityCountService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateIds;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;
import static org.thingsboard.server.dao.service.Validator.validateString;

@Service("AgentDaoService")
@Slf4j
public class BaseAgentService extends AbstractCachedEntityService<AgentCacheKey, Agent, AgentCacheEvictEvent> implements AgentService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_AGENT_ID = "Incorrect agentId ";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";

    @Autowired
    private AgentDao agentDao;

    @Autowired
    private AgentInfoDao agentInfoDao;

    @Lazy
    @Autowired
    private AgentApplicationService agentApplicationService;

    @Autowired
    private AgentProfileService agentProfileService;

    @Autowired
    private EntityCountService entityCountService;

    @Autowired
    private DataValidator<Agent> agentValidator;

    @Override
    @TransactionalEventListener
    public void handleEvictEvent(AgentCacheEvictEvent event) {
        List<AgentCacheKey> keys = new ArrayList<>(3);
        if (event.getAgentId() != null) {
            keys.add(AgentCacheKey.forId(event.getAgentId()));
        }
        if (StringUtils.isNotEmpty(event.getRoutingKey())) {
            keys.add(AgentCacheKey.forRoutingKey(event.getRoutingKey()));
        }
        if (StringUtils.isNotEmpty(event.getOldRoutingKey()) && !event.getOldRoutingKey().equals(event.getRoutingKey())) {
            keys.add(AgentCacheKey.forRoutingKey(event.getOldRoutingKey()));
        }
        if (!keys.isEmpty()) {
            cache.evict(keys);
        }
    }

    @Override
    @Transactional
    public Agent saveAgent(Agent agent) {
        return saveEntity(agent, () -> doSaveAgent(agent));
    }

    private Agent doSaveAgent(Agent agent) {
        log.trace("Executing saveAgent [{}]", agent);
        Agent oldAgent = agentValidator.validate(agent, Agent::getTenantId);
        String oldRoutingKey = oldAgent != null ? oldAgent.getRoutingKey() : null;
        AgentCacheEvictEvent evictEvent = new AgentCacheEvictEvent(agent.getId(), agent.getRoutingKey(), oldRoutingKey);
        try {
            if (agent.getAgentProfileId() == null) {
                AgentProfile defaultProfile = agentProfileService.findDefaultAgentProfile(agent.getTenantId());
                if (defaultProfile == null) {
                    throw new DataValidationException("Tenant does not have a default agent profile!");
                }
                agent.setAgentProfileId(new AgentProfileId(defaultProfile.getId().getId()));
            } else {
                AgentProfile agentProfile = agentProfileService.findProfileById(agent.getTenantId(), agent.getAgentProfileId());
                if (agentProfile == null) {
                    throw new DataValidationException("Agent is referencing non existing agent profile!");
                }
                if (!agentProfile.getTenantId().equals(agent.getTenantId())) {
                    throw new DataValidationException("Agent can`t be referencing to agent profile from different tenant!");
                }
            }
            Agent savedAgent = agentDao.save(agent.getTenantId(), agent);
            publishEvictEvent(new AgentCacheEvictEvent(savedAgent.getId(), savedAgent.getRoutingKey(), oldRoutingKey));
            if (agent.getId() == null) {
                entityGroupService.addEntityToEntityGroupAll(savedAgent.getTenantId(), savedAgent.getOwnerId(), savedAgent.getId());
                entityCountService.publishCountEntityEvictEvent(savedAgent.getTenantId(), EntityType.AGENT);
            }
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(savedAgent.getTenantId())
                    .entityId(savedAgent.getId()).entity(savedAgent).oldEntity(oldAgent).created(agent.getId() == null).build());
            return savedAgent;
        } catch (Exception t) {
            handleEvictEvent(evictEvent);
            checkConstraintViolation(t, "agent_name_unq_key", "Agent with such name already exists!");
            checkConstraintViolation(t, "agent_routing_key_unq_key", "Agent with such routing key already exists!");
            throw t;
        }
    }

    @Override
    public Agent findAgentById(TenantId tenantId, AgentId agentId) {
        log.trace("Executing findAgentById [{}]", agentId);
        validateId(agentId, id -> INCORRECT_AGENT_ID + id);
        return cache.getAndPutInTransaction(AgentCacheKey.forId(agentId),
                () -> agentDao.findById(tenantId, agentId.getId()), true);
    }

    @Override
    public Agent findAgentByRoutingKey(TenantId tenantId, String routingKey) {
        log.trace("Executing findAgentByRoutingKey [{}]", routingKey);
        validateString(routingKey, "Incorrect agent routingKey for search request.");
        return cache.getAndPutInTransaction(AgentCacheKey.forRoutingKey(routingKey),
                () -> agentDao.findByRoutingKey(tenantId.getId(), routingKey), true);
    }

    @Override
    public AgentInfo findAgentInfoById(TenantId tenantId, AgentId agentId) {
        log.trace("Executing findAgentInfoById [{}]", agentId);
        validateId(agentId, id -> INCORRECT_AGENT_ID + id);
        return agentInfoDao.findAgentInfoById(tenantId, agentId.getId());
    }

    @Override
    public List<AgentInfo> findAgentInfosByTenantIdAndIds(TenantId tenantId, List<AgentId> agentIds) {
        log.trace("Executing findAgentInfosByTenantIdAndIds, tenantId [{}], agentIds [{}]", tenantId, agentIds);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateIds(agentIds, ids -> "Incorrect agentIds " + ids);
        return agentInfoDao.findAgentInfosByTenantIdAndIds(tenantId.getId(), DaoUtil.toUUIDs(agentIds));
    }

    @Override
    public PageData<Agent> findAgentsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findAgentsByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        validatePageLink(pageLink);
        return agentDao.findAgentsByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
    }

    @Override
    public PageData<AgentId> findAgentIdsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findAgentIdsByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        validatePageLink(pageLink);
        return agentDao.findAgentIdsByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
    }

    @Override
    public PageData<AgentInfo> findAgentInfosByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findAgentInfosByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        validatePageLink(pageLink);
        return agentInfoDao.findAgentInfosByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
    }

    @Override
    public PageData<Agent> findAgentsByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findAgentsByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        return agentDao.findAgentsByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<AgentInfo> findAgentInfosByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findAgentInfosByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        return agentInfoDao.findAgentInfosByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findAgentById(tenantId, new AgentId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        ListenableFuture<Agent> future = agentDao.findByIdAsync(tenantId, entityId.getId());
        return FluentFuture.from(future)
                .transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public long countByTenantId(TenantId tenantId) {
        return agentDao.countByTenantId(tenantId);
    }

    @Override
    @Transactional
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        deleteAgent(tenantId, new AgentId(id.getId()));
    }

    @Override
    @Transactional
    public void deleteByTenantId(TenantId tenantId) {
        log.trace("Executing deleteByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        tenantAgentsRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    @Transactional
    public void deleteAgent(TenantId tenantId, AgentId agentId) {
        log.trace("Executing deleteAgent [{}]", agentId);
        validateId(agentId, id -> INCORRECT_AGENT_ID + id);

        Agent agent = agentDao.findById(tenantId, agentId.getId());
        if (agent == null) {
            return;
        }
        agentApplicationService.deleteByAgentId(tenantId, agentId);
        agentDao.removeById(tenantId, agentId.getId());
        entityCountService.publishCountEntityEvictEvent(tenantId, EntityType.AGENT);

        publishEvictEvent(new AgentCacheEvictEvent(agent.getId(), agent.getRoutingKey(), null));
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(agentId).entity(agent).build());
    }

    @Override
    public PageData<Agent> findAgentsByEntityGroupId(EntityGroupId groupId, PageLink pageLink) {
        log.trace("Executing findAgentsByEntityGroupId, groupId [{}], pageLink [{}]", groupId, pageLink);
        validateId(groupId, id -> "Incorrect entityGroupId " + id);
        validatePageLink(pageLink);
        return agentDao.findAgentsByEntityGroupId(groupId.getId(), pageLink);
    }

    @Override
    public PageData<Agent> findAgentsByEntityGroupIds(List<EntityGroupId> groupIds, PageLink pageLink) {
        log.trace("Executing findAgentsByEntityGroupIds, groupIds [{}], pageLink [{}]", groupIds, pageLink);
        validateIds(groupIds, ids -> "Incorrect groupIds " + ids);
        validatePageLink(pageLink);
        return agentDao.findAgentsByEntityGroupIds(DaoUtil.toUUIDs(groupIds), pageLink);
    }

    @Override
    @Transactional
    public void deleteAgentsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing deleteAgentsByTenantIdAndCustomerId, tenantId [{}], customerId [{}]", tenantId, customerId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        customerAgentsRemover.removeEntities(tenantId, customerId);
    }

    @Override
    public Long countAgents() {
        log.trace("Executing countAgents");
        return agentDao.countAgents();
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT;
    }

    private final PaginatedRemover<TenantId, Agent> tenantAgentsRemover = new PaginatedRemover<>() {
        @Override
        protected PageData<Agent> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
            return agentDao.findAgentsByTenantId(id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, Agent entity) {
            deleteAgent(tenantId, new AgentId(entity.getUuidId()));
        }
    };

    private final PaginatedRemover<CustomerId, Agent> customerAgentsRemover = new PaginatedRemover<>() {

        @Override
        protected PageData<Agent> findEntities(TenantId tenantId, CustomerId id, PageLink pageLink) {
            return agentDao.findAgentsByTenantIdAndCustomerId(tenantId.getId(), id.getId(), pageLink);
        }

        @Override
        @Transactional
        protected void removeEntity(TenantId tenantId, Agent entity) {
            deleteAgent(tenantId, new AgentId(entity.getUuidId()));
        }
    };

}
