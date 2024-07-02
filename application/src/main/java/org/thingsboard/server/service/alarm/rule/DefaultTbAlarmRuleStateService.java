/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.alarm.rule;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.TbAlarmRuleStateService;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleEntityFilter;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.AlarmRuleId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.alarm.rule.AlarmRuleService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;
import org.thingsboard.server.service.alarm.rule.state.PersistedEntityState;
import org.thingsboard.server.service.alarm.rule.store.AlarmRuleEntityStateStore;
import org.thingsboard.server.service.partition.AbstractPartitionBasedService;
import org.thingsboard.server.service.profile.TbAssetProfileCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.DataConstants.INTERNAL_QUEUE_NAME;
import static org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent.DELETED;

@Slf4j
@Service
@TbRuleEngineComponent
@RequiredArgsConstructor
public class DefaultTbAlarmRuleStateService extends AbstractPartitionBasedService<EntityId> implements TbAlarmRuleStateService {

    private final TbAlarmRuleContext ctx;
    private final AlarmRuleService alarmRuleService;
    private final AlarmRuleEntityStateStore stateStore;
    private final TbDeviceProfileCache deviceProfileCache;
    private final TbAssetProfileCache assetProfileCache;

    private final Map<EntityId, EntityState> entityStates = new ConcurrentHashMap<>();
    private final Map<AlarmRuleId, Set<EntityId>> myEntityStateIdsPerAlarmRule = new ConcurrentHashMap<>();
    private final Map<TenantId, Set<EntityId>> myEntityStateIds = new ConcurrentHashMap<>();
    private Cache<TenantId, ConcurrentHashMap<AlarmRuleId, AlarmRule>> rulesCache;

    @PostConstruct
    protected void init() {
        super.init();
        rulesCache = Caffeine.newBuilder()
                .expireAfterAccess(24, TimeUnit.HOURS)
                .maximumSize(1024).build();

        scheduledExecutor.scheduleAtFixedRate(this::harvestAlarms, 1, 1, TimeUnit.MINUTES);
    }

    protected ServiceType getServiceType() {
        return ServiceType.TB_RULE_ENGINE;
    }

    @Override
    protected String getServiceName() {
        return "Alarm Rule State";
    }

    @Override
    protected String getSchedulerExecutorName() {
        return "alarm-rule-state-scheduler";
    }

    @Override
    protected Map<TopicPartitionInfo, List<ListenableFuture<?>>> onAddedPartitions(Set<TopicPartitionInfo> addedPartitions) {
        AtomicInteger addedEntityStates = new AtomicInteger(0);
        addedPartitions.stream().map(tpi -> tpi.getTenantId().orElse(TenantId.SYS_TENANT_ID)).distinct().forEach(tenantId -> {
            List<PersistedEntityState> states = stateStore.getAll(tenantId);
            addedEntityStates.addAndGet(states.size());
            states.forEach(state -> {
                try {
                    EntityId entityId = state.getEntityId();
                    if (!entityStates.containsKey(entityId)) {
                        createEntityState(state.getTenantId(), entityId, state);
                    }
                } catch (Exception e) {
                    log.error("Failed to create entity state during repartitioning!", e);
                }
            });
        });
        log.info("Added new entity states: {}", addedEntityStates.get());
        return Collections.emptyMap();
    }

    @Override
    protected void cleanupEntitiesOnPartitionRemoval(Set<EntityId> ids) {
        ids.forEach(entityStates::remove);
        myEntityStateIdsPerAlarmRule.values().forEach(set -> set.removeAll(ids));
    }

    @Override
    protected void cleanupEntityOnPartitionRemoval(EntityId entityId) {
    }

    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent event) {
        if (getServiceType().equals(event.getServiceType())) {
            var internalPartitions = event.getPartitionsMap().entrySet().stream().filter(entry -> entry.getKey().isInternal()).flatMap(entry -> entry.getValue().stream()).collect(Collectors.toSet());
            log.debug("onTbApplicationEvent, processing event: {}", internalPartitions);

            subscribeQueue.add(internalPartitions);
            scheduledExecutor.submit(this::pollInitStateFromDB);
        }
    }

    @EventListener(ComponentLifecycleMsg.class)
    public void onComponentLifecycleEvent(ComponentLifecycleMsg eventMsg) {
        TenantId tenantId = eventMsg.getTenantId();
        EntityId entityId = eventMsg.getEntityId();
        ComponentLifecycleEvent event = eventMsg.getEvent();
        switch (entityId.getEntityType()) {
            case TENANT -> {
                if (event == DELETED) {
                    deleteTenant((TenantId) entityId);
                }
            }
            case ALARM_RULE -> {
                AlarmRuleId alarmRuleId = (AlarmRuleId) entityId;
                switch (event) {
                    case CREATED, UPDATED -> createOrUpdateAlarmRule(tenantId, alarmRuleId);
                    case DELETED -> deleteAlarmRule(tenantId, alarmRuleId);
                }
            }
            case DEVICE, ASSET -> {
                switch (event) {
                    case UPDATED -> processEntityUpdated(tenantId, entityId);
                    case DELETED -> cleanupEntityState(tenantId, entityId);
                }
            }
        }
    }

    @Override
    public void process(TbContext tbContext, TbMsg msg) throws ExecutionException, InterruptedException {
        TenantId tenantId = tbContext.getTenantId();
        EntityId entityId = msg.getOriginator();
        RuleNode self = tbContext.getSelf();
        RuleChainId ruleChainId = self.getRuleChainId();
        RuleNodeId ruleNodeId = self.getId();
        if (isLocalEntity(tenantId, entityId)) {
            TbAlarmRuleRequestCtx requestCtx = new TbAlarmRuleRequestCtx();
            requestCtx.setRuleChainId(ruleChainId);
            requestCtx.setRuleNodeId(ruleNodeId);
            requestCtx.setDebugMode(self.isDebugMode());
            doProcess(requestCtx, tenantId, msg);
            tbContext.tellSuccess(msg);
        } else {
            TopicPartitionInfo tpi = getTpi(tenantId, entityId);
            tbContext.ack(msg);

            var tbMsg = TbMsg.newMsg(msg, msg.getQueueName(), ruleChainId, ruleNodeId);

            TransportProtos.ToRuleEngineMsg toQueueMsg = TransportProtos.ToRuleEngineMsg.newBuilder().setTenantIdMSB(tenantId.getId().getMostSignificantBits()).setTenantIdLSB(tenantId.getId().getLeastSignificantBits()).setTbMsg(TbMsg.toByteString(tbMsg)).setQueueName(tbMsg.getQueueName()).build();

            tbContext.getClusterService().pushMsgToRuleEngine(tpi, tbMsg.getId(), toQueueMsg, null);
        }
    }

    private void doProcess(TbAlarmRuleRequestCtx requestCtx, TenantId tenantId, TbMsg msg) throws ExecutionException, InterruptedException {
        EntityState entityState = getOrCreateEntityState(tenantId, msg.getOriginator());
        if (entityState != null) {
            entityState.process(requestCtx, msg);
        }
    }

    private void processEntityUpdated(TenantId tenantId, EntityId entityId) {
        EntityState entityState = entityStates.get(entityId);
        if (entityState != null) {
            entityState.getLock().lock();
            entityState = entityStates.get(entityId);
            try {
                EntityId oldProfileId = entityState.getProfileId();
                EntityId newProfileId = getProfileId(tenantId, entityId);

                if (!oldProfileId.equals(newProfileId)) {
                    List<AlarmRule> oldAlarmRules = entityState.getAlarmRules();
                    List<AlarmRule> newAlarmRules = getAlarmRulesForEntity(tenantId, entityId);

                    List<AlarmRule> toAdd = CollectionsUtil.diffLists(oldAlarmRules, newAlarmRules);
                    List<AlarmRuleId> toRemoveIds = CollectionsUtil.diffLists(newAlarmRules, oldAlarmRules).stream().map(AlarmRule::getId).collect(Collectors.toList());

                    toAdd.forEach(entityState::addAlarmRule);
                    entityState.removeAlarmRules(toRemoveIds);
                    toRemoveIds.forEach(alarmRuleId -> myEntityStateIdsPerAlarmRule.get(alarmRuleId).remove(entityId));

                    if (entityState.isEmpty()) {
                        cleanupEntityState(tenantId, entityId);
                    } else {
                        entityState.setProfileId(newProfileId);
                    }
                }
            } finally {
                entityState.getLock().unlock();
            }
        }
    }

    private void cleanupEntityState(TenantId tenantId, EntityId entityId) {
        EntityState state = entityStates.remove(entityId);
        if (state != null) {
            myEntityStateIdsPerAlarmRule.values().forEach(ids -> ids.remove(entityId));
            Set<EntityId> myEntityIds = myEntityStateIds.get(tenantId);
            if (myEntityIds != null) {
                myEntityIds.remove(entityId);
            }
            stateStore.remove(tenantId, entityId);
            partitionedEntities.remove(getTpi(tenantId, entityId));
        }
    }

    private void createOrUpdateAlarmRule(TenantId tenantId, AlarmRuleId alarmRuleId) {
        if (myEntityStateIds.containsKey(tenantId)) {
            AlarmRule alarmRule = alarmRuleService.findAlarmRuleById(tenantId, alarmRuleId);

            if (!alarmRule.isEnabled()) {
                deleteAlarmRule(tenantId, alarmRuleId);
                return;
            }

            ConcurrentHashMap<AlarmRuleId, AlarmRule> tenantRules = rulesCache.getIfPresent(tenantId);
            if (tenantRules != null) {
                tenantRules.put(alarmRuleId, alarmRule);
            }

            Set<EntityId> newIds = myEntityStateIds.get(tenantId).stream().filter(entityId -> isEntityMatches(tenantId, entityId, alarmRule)).collect(Collectors.toSet());
            Set<EntityId> oldIds = myEntityStateIdsPerAlarmRule.get(alarmRuleId);

            if (CollectionsUtil.isEmpty(oldIds)) {
                newIds.forEach(entityId -> {
                    EntityState entityState = entityStates.get(entityId);
                    myEntityStateIdsPerAlarmRule.computeIfAbsent(alarmRuleId, key -> ConcurrentHashMap.newKeySet()).add(entityId);
                    entityState.addAlarmRule(alarmRule);
                });
            } else {
                Set<EntityId> toAdd = CollectionsUtil.diffSets(oldIds, newIds);
                Set<EntityId> toUpdate = CollectionsUtil.diffSets(toAdd, newIds);
                Set<EntityId> toRemove = CollectionsUtil.diffSets(newIds, oldIds);

                toAdd.forEach(entityId -> {
                    EntityState entityState = entityStates.get(entityId);
                    myEntityStateIdsPerAlarmRule.computeIfAbsent(alarmRuleId, key -> ConcurrentHashMap.newKeySet()).add(entityId);
                    entityState.addAlarmRule(alarmRule);
                });

                toUpdate.forEach(entityId -> {
                    EntityState entityState = entityStates.get(entityId);
                    try {
                        entityState.updateAlarmRule(alarmRule);
                    } catch (Exception e) {
                        log.error("[{}] [{}] Failed to update alarm rule!", tenantId, alarmRule.getName(), e);
                    }
                });

                toRemove.forEach(entityId -> {
                    EntityState entityState = entityStates.get(entityId);
                    entityState.removeAlarmRule(alarmRuleId);
                    myEntityStateIdsPerAlarmRule.get(alarmRuleId).remove(entityId);
                    if (entityState.isEmpty()) {
                        cleanupEntityState(tenantId, entityId);
                    }
                });
            }
        } else {
            rulesCache.invalidate(tenantId);
        }
    }

    private void deleteAlarmRule(TenantId tenantId, AlarmRuleId alarmRuleId) {
        Map<AlarmRuleId, AlarmRule> tenantRules = rulesCache.getIfPresent(tenantId);

        if (tenantRules != null) {
            tenantRules.remove(alarmRuleId);
        }

        Set<EntityId> stateIds = myEntityStateIdsPerAlarmRule.remove(alarmRuleId);

        if (stateIds != null) {
            stateIds.forEach(id -> {
                EntityState entityState = entityStates.get(id);
                Lock lock = entityState.getLock();
                try {
                    lock.lock();
                    entityState.removeAlarmRule(alarmRuleId);
                    if (entityState.isEmpty()) {
                        cleanupEntityState(tenantId, id);
                    }
                } finally {
                    lock.unlock();
                }
            });
        }
    }

    private void deleteTenant(TenantId tenantId) {
        Map<AlarmRuleId, AlarmRule> tenantRules = rulesCache.getIfPresent(tenantId);
        if (tenantRules != null) {
            tenantRules.keySet().forEach(alarmRuleId -> deleteAlarmRule(tenantId, alarmRuleId));
            rulesCache.invalidate(tenantId);
        } else {
            Set<EntityId> ids = myEntityStateIds.remove(tenantId);
            if (CollectionsUtil.isNotEmpty(ids)) {
                ids.forEach(entityId -> cleanupEntityState(tenantId, entityId));
            }
        }
    }

    private void harvestAlarms() {
        long ts = System.currentTimeMillis();
        for (EntityState state : entityStates.values()) {
            try {
                state.harvestAlarms(ts);
            } catch (Exception e) {
                log.warn("[{}] [{}] Failed to harvest alarms.", state.getTenantId(), state.getEntityId());
            }
        }
    }

    private EntityState getOrCreateEntityState(TenantId tenantId, EntityId msgOriginator) {
        EntityState entityState = entityStates.get(msgOriginator);

        if (entityState != null) {
            return entityState;
        }

        List<AlarmRule> filteredRules = getAlarmRulesForEntity(tenantId, msgOriginator);

        if (filteredRules.isEmpty()) {
            return null;
        }
        return getOrCreateEntityState(tenantId, msgOriginator, filteredRules, null);
    }

    private List<AlarmRule> getAlarmRulesForEntity(TenantId tenantId, EntityId entityId) {
        return getOrFetchAlarmRules(tenantId).stream().filter(rule -> isEntityMatches(tenantId, entityId, rule)).collect(Collectors.toList());
    }

    private EntityState getOrCreateEntityState(TenantId tenantId, EntityId targetEntityId, List<AlarmRule> alarmRules, PersistedEntityState persistedEntityState) {
        EntityState entityState = entityStates.get(targetEntityId);
        if (entityState == null) {
            entityState = new EntityState(tenantId, targetEntityId, getProfileId(tenantId, targetEntityId), ctx, new EntityRulesState(alarmRules), persistedEntityState);
            myEntityStateIds.computeIfAbsent(tenantId, key -> ConcurrentHashMap.newKeySet()).add(targetEntityId);
            entityStates.put(targetEntityId, entityState);
            alarmRules.forEach(alarmRule -> myEntityStateIdsPerAlarmRule.computeIfAbsent(alarmRule.getId(), key -> ConcurrentHashMap.newKeySet()).add(targetEntityId));
            partitionedEntities.computeIfAbsent(getTpi(tenantId, targetEntityId), key -> ConcurrentHashMap.newKeySet()).add(targetEntityId);
        } else {
            for (AlarmRule alarmRule : alarmRules) {
                Set<EntityId> entityIds = myEntityStateIdsPerAlarmRule.get(alarmRule.getId());
                if (entityIds == null || !entityIds.contains(targetEntityId)) {
                    myEntityStateIdsPerAlarmRule.computeIfAbsent(alarmRule.getId(), key -> ConcurrentHashMap.newKeySet()).add(targetEntityId);
                    entityState.addAlarmRule(alarmRule);
                }
            }
        }

        return entityState;
    }

    private EntityId getProfileId(TenantId tenantId, EntityId entityId) {
        HasId<? extends EntityId> profile = switch (entityId.getEntityType()) {
            case ASSET -> assetProfileCache.get(tenantId, (AssetId) entityId);
            case DEVICE -> deviceProfileCache.get(tenantId, (DeviceId) entityId);
            default -> throw new IllegalArgumentException("Wrong entity type: " + entityId.getEntityType());
        };

        if (profile == null) {
            throw new RuntimeException(String.format("[%s] The entity %s [%s] has already been deleted!", tenantId, entityId.getEntityType(), entityId));
        }

        return profile.getId();
    }

    private void createEntityState(TenantId tenantId, EntityId entityId, PersistedEntityState persistedEntityState) {
        Set<AlarmRuleId> alarmRuleIds = persistedEntityState.getAlarmStates().keySet().stream().map(id -> new AlarmRuleId(UUID.fromString(id))).collect(Collectors.toSet());
        List<AlarmRule> filteredRules = getOrFetchAlarmRules(tenantId).stream().filter(rule -> alarmRuleIds.contains(rule.getId())).collect(Collectors.toList());
        getOrCreateEntityState(tenantId, entityId, filteredRules, persistedEntityState);
    }

    private Collection<AlarmRule> getOrFetchAlarmRules(TenantId tenantId) {
        return rulesCache.get(tenantId, key -> {
            ConcurrentHashMap<AlarmRuleId, AlarmRule> map = new ConcurrentHashMap<>();
            fetchAlarmRules(key).forEach(alarmRule -> map.put(alarmRule.getId(), alarmRule));
            return map;
        }).values();
    }

    private List<AlarmRule> fetchAlarmRules(TenantId tenantId) {
        List<AlarmRule> alarmRules = new ArrayList<>();
        PageDataIterable<AlarmRule> pageIterable = new PageDataIterable<>(pageLink -> alarmRuleService.findEnabledAlarmRules(tenantId, pageLink), 1024);
        pageIterable.forEach(alarmRules::add);
        return alarmRules;
    }

    private boolean isEntityMatches(TenantId tenantId, EntityId entityId, AlarmRule alarmRule) {
        List<AlarmRuleEntityFilter> sourceEntityFilters = alarmRule.getConfiguration().getSourceEntityFilters();

        for (AlarmRuleEntityFilter filter : sourceEntityFilters) {
            if (isEntityMatches(tenantId, entityId, filter)) {
                return true;
            }
        }
        return false;
    }

    private boolean isEntityMatches(TenantId tenantId, EntityId entityId, AlarmRuleEntityFilter filter) {
        switch (filter.getType()) {
            case DEVICE_TYPE:
                if (entityId.getEntityType() == EntityType.DEVICE) {
                    DeviceProfile deviceProfile = deviceProfileCache.get(tenantId, (DeviceId) entityId);
                    return deviceProfile != null && filter.isEntityMatches(deviceProfile.getId());
                }
                break;
            case ASSET_TYPE:
                if (entityId.getEntityType() == EntityType.ASSET) {
                    AssetProfile assetProfile = assetProfileCache.get(tenantId, (AssetId) entityId);
                    return assetProfile != null && filter.isEntityMatches(assetProfile.getId());
                }
                break;
            default:
                return filter.isEntityMatches(entityId);
        }
        return false;
    }

    private boolean isLocalEntity(TenantId tenantId, EntityId entityId) {
        return getTpi(tenantId, entityId).isMyPartition();
    }

    private TopicPartitionInfo getTpi(TenantId tenantId, EntityId entityId) {
        return partitionService.resolve(ServiceType.TB_RULE_ENGINE, INTERNAL_QUEUE_NAME, tenantId, entityId);
    }
}
