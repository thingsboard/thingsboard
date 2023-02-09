/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.rule.engine.api.TbAlarmRuleStateService;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.alarm.rule.AlarmRuleEntityState;
import org.thingsboard.server.common.data.alarm.rule.AlarmRuleRelationTargetEntity;
import org.thingsboard.server.common.data.alarm.rule.AlarmRuleSpecifiedTargetEntity;
import org.thingsboard.server.common.data.alarm.rule.AlarmRuleTargetEntity;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleEntityFilter;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.AlarmRuleId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.alarm.rule.AlarmRuleEntityStateService;
import org.thingsboard.server.dao.alarm.rule.AlarmRuleService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.queue.discovery.HashPartitionService;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;
import org.thingsboard.server.service.alarm.rule.state.PersistedEntityState;
import org.thingsboard.server.service.profile.TbAssetProfileCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@TbRuleEngineComponent
@RequiredArgsConstructor
public class DefaultTbAlarmRuleStateService implements TbAlarmRuleStateService {

    private final TbAlarmRuleContext ctx;
    private final AlarmRuleService alarmRuleService;
    private final AlarmRuleEntityStateService alarmRuleEntityStateService;
    private final HashPartitionService partitionService;
    private final RelationService relationService;
    private final TbDeviceProfileCache deviceProfileCache;
    private final TbAssetProfileCache assetProfileCache;

    private ScheduledExecutorService scheduler;

    private final Map<EntityId, EntityState> entityStates = new ConcurrentHashMap<>();
    private final Map<TenantId, Map<AlarmRuleId, AlarmRule>> rules = new ConcurrentHashMap<>();
    private final Map<AlarmRuleId, Set<EntityId>> myEntityStateIds = new ConcurrentHashMap<>();
    private final Map<TenantId, Set<EntityId>> otherEntityStateIds = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        scheduler = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("alarm-rule-service"));

        log.info("Fetching alarm rule state.");
        int fetchCount = 0;
        PageLink pageLink = new PageLink(1024);
        while (true) {
            PageData<AlarmRuleEntityState> entityStates = alarmRuleEntityStateService.findAll(pageLink);
            List<AlarmRuleEntityState> myStates = new ArrayList<>();

            entityStates.getData().forEach(state -> {
                TenantId tenantId = state.getTenantId();
                EntityId entityId = state.getEntityId();
                if (isLocalEntity(tenantId, entityId)) {
                    myStates.add(state);
                } else {
                    otherEntityStateIds.computeIfAbsent(tenantId, key -> ConcurrentHashMap.newKeySet()).add(entityId);
                }
            });

            for (AlarmRuleEntityState ares : myStates) {
                TenantId tenantId = ares.getTenantId();
                EntityId entityId = ares.getEntityId();
                fetchCount++;
                createEntityState(tenantId, entityId, ares);
            }
            if (!entityStates.hasNext()) {
                break;
            } else {
                pageLink = pageLink.nextPageLink();
            }
        }
        log.info("Fetched alarm rule state for {} entities.", fetchCount);

        scheduler.scheduleAtFixedRate(this::harvestAlarms, 1, 1, TimeUnit.MINUTES);
    }

    @PreDestroy
    private void destroy() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    @Override
    public void process(TbContext tbContext, TbMsg msg) throws Exception {
        List<EntityState> entityStates = getOrCreateEntityStates(tbContext.getTenantId(), msg.getOriginator());
        for (EntityState entityState : entityStates) {
            entityState.process(tbContext, msg);
        }
    }

    @Override
    public void processEntityDeleted(TbMsg msg) {
        EntityId entityId = msg.getOriginator();
        EntityState state = entityStates.remove(entityId);
        if (state != null) {
            myEntityStateIds.values().forEach(ids -> ids.remove(entityId));
            alarmRuleEntityStateService.deleteByEntityId(state.getTenantId(), entityId);
        }
    }

    @EventListener(PartitionChangeEvent.class)
    public void onPartitionChangeEvent(PartitionChangeEvent event) {
        List<EntityId> toRemove = new ArrayList<>();
        List<AlarmRuleEntityState> toAdd = new ArrayList<>();

        otherEntityStateIds.forEach(((tenantId, entityIds) -> {
            List<EntityId> ids = new ArrayList<>();
            entityIds.forEach(entityId -> {
                if (isLocalEntity(tenantId, entityId)) {
                    ids.add(entityId);
                }
            });
            ids.forEach(entityIds::remove);
            toAdd.addAll(alarmRuleEntityStateService.findAllByIds(ids));
        }));

        entityStates.values().forEach(entityState -> {
            TenantId tenantId = entityState.getTenantId();
            EntityId entityId = entityState.getEntityId();
            if (!isLocalEntity(tenantId, entityId)) {
                toRemove.add(entityId);
                otherEntityStateIds.computeIfAbsent(tenantId, key -> ConcurrentHashMap.newKeySet()).add(entityId);
            }
        });

        toRemove.forEach(entityStates::remove);
        myEntityStateIds.values().forEach(ids -> toRemove.forEach(ids::remove));

        toAdd.forEach(ares -> createEntityState(ares.getTenantId(), ares.getEntityId(), ares));
    }

    @Override
    public void createAlarmRule(TenantId tenantId, AlarmRuleId alarmRuleId) {
        AlarmRule alarmRule = alarmRuleService.findAlarmRuleById(tenantId, alarmRuleId);
        if (alarmRule.isEnabled()) {
            Map<AlarmRuleId, AlarmRule> tenantRules = rules.get(tenantId);
            if (tenantRules != null) {
                tenantRules.put(alarmRule.getId(), alarmRule);
            }
        }
    }

    @Override
    public void updateAlarmRule(TenantId tenantId, AlarmRuleId alarmRuleId) {
        AlarmRule alarmRule = alarmRuleService.findAlarmRuleById(tenantId, alarmRuleId);
        if (alarmRule.isEnabled()) {
            Map<AlarmRuleId, AlarmRule> tenantRules = rules.get(tenantId);
            if (tenantRules != null) {
                tenantRules.put(alarmRule.getId(), alarmRule);

                Set<EntityId> stateIds = myEntityStateIds.get(alarmRule.getId());

                stateIds.forEach(stateId -> {
                    EntityState entityState = entityStates.get(stateId);
                    try {
                        entityState.updateAlarmRule(alarmRule);
                    } catch (Exception e) {
                        log.error("[{}] [{}] Failed to update alarm rule!", tenantId, alarmRule.getName(), e);
                    }
                });
            }
        } else {
            deleteAlarmRule(tenantId, alarmRuleId);
        }
    }

    @Override
    public void deleteAlarmRule(TenantId tenantId, AlarmRuleId alarmRuleId) {
        Map<AlarmRuleId, AlarmRule> tenantRules = rules.get(tenantId);

        Set<EntityId> otherIds = otherEntityStateIds.get(tenantId);

        if (otherIds != null) {
            otherIds.remove(alarmRuleId);
        }

        if (tenantRules == null) {
            return;
        }

        tenantRules.remove(alarmRuleId);

        Set<EntityId> stateIds = myEntityStateIds.remove(alarmRuleId);

        if (stateIds != null) {
            stateIds.forEach(id -> {
                EntityState entityState = entityStates.get(id);
                entityState.removeAlarmRule(alarmRuleId);
                if (entityState.isEmpty()) {
                    entityStates.remove(entityState.getEntityId());
                    alarmRuleEntityStateService.deleteByEntityId(tenantId, id);
                }
            });
        }
    }

    @Override
    public void deleteTenant(TenantId tenantId) {
        Map<AlarmRuleId, AlarmRule> tenantRules = rules.get(tenantId);
        if (tenantRules != null) {
            tenantRules.keySet().forEach(alarmRuleId -> deleteAlarmRule(tenantId, alarmRuleId));
            rules.remove(tenantId);
            otherEntityStateIds.remove(tenantId);
        }
    }

    private List<EntityState> getOrCreateEntityStates(TenantId tenantId, EntityId msgOriginator) {
        List<AlarmRule> filteredRules =
                getOrFetchAlarmRules(tenantId)
                        .stream()
                        .filter(rule -> isEntityMatches(tenantId, msgOriginator, rule))
                        .collect(Collectors.toList());

        Map<EntityId, List<AlarmRule>> entityAlarmRules = new HashMap<>();

        filteredRules.forEach(alarmRule -> {
            List<EntityId> targetEntities = getTargetEntities(tenantId, msgOriginator, alarmRule.getConfiguration().getAlarmTargetEntity());
            targetEntities.forEach(targetEntityId -> entityAlarmRules.computeIfAbsent(targetEntityId, key -> new ArrayList<>()).add(alarmRule));
        });

        return entityAlarmRules
                .entrySet()
                .stream()
                .map(entry -> getOrCreateEntityState(tenantId, entry.getKey(), entry.getValue(), null))
                .collect(Collectors.toList());
    }

    private void createEntityState(TenantId tenantId, EntityId entityId, AlarmRuleEntityState alarmRuleEntityState) {
        PersistedEntityState persistedEntityState = JacksonUtil.fromString(alarmRuleEntityState.getData(), PersistedEntityState.class);

        Set<AlarmRuleId> alarmRuleIds =
                persistedEntityState.getAlarmStates().keySet().stream().map(id -> new AlarmRuleId(UUID.fromString(id))).collect(Collectors.toSet());

        List<AlarmRule> filteredRules =
                getOrFetchAlarmRules(tenantId)
                        .stream()
                        .filter(rule -> alarmRuleIds.contains(rule.getId()))
                        .collect(Collectors.toList());


        getOrCreateEntityState(tenantId, entityId, filteredRules, alarmRuleEntityState);
    }

    private Collection<AlarmRule> getOrFetchAlarmRules(TenantId tenantId) {
        return rules.computeIfAbsent(tenantId, key -> {
            Map<AlarmRuleId, AlarmRule> map = new HashMap<>();
            fetchAlarmRules(key).forEach(alarmRule -> {
                map.put(alarmRule.getId(), alarmRule);
            });
            return map;
        }).values();
    }

    private EntityState getOrCreateEntityState(TenantId tenantId, EntityId targetEntityId, List<AlarmRule> alarmRules, AlarmRuleEntityState alarmRuleEntityState) {
        EntityState entityState = entityStates.get(targetEntityId);
        if (entityState == null) {
            entityState = new EntityState(tenantId, targetEntityId, ctx, new EntityRulesState(alarmRules), alarmRuleEntityState);
            entityStates.put(targetEntityId, entityState);
            alarmRules.forEach(alarmRule -> myEntityStateIds.computeIfAbsent(alarmRule.getId(), key -> ConcurrentHashMap.newKeySet()).add(targetEntityId));
        } else {
            for (AlarmRule alarmRule : alarmRules) {
                Set<EntityId> entityIds = myEntityStateIds.get(alarmRule.getId());
                if (entityIds == null || !entityIds.contains(targetEntityId)) {
                    myEntityStateIds.computeIfAbsent(alarmRule.getId(), key -> ConcurrentHashMap.newKeySet()).add(targetEntityId);
                    entityState.addAlarmRule(alarmRule);
                }
            }
        }

        return entityState;
    }

    protected void harvestAlarms() {
        long ts = System.currentTimeMillis();
        for (EntityState state : entityStates.values()) {
            try {
                state.harvestAlarms(ts);
            } catch (Exception e) {
                log.warn("[{}] [{}] Failed to harvest alarms.", state.getTenantId(), state.getEntityId());
            }
        }
    }

    private List<AlarmRule> fetchAlarmRules(TenantId tenantId) {
        List<AlarmRule> alarmRules = new ArrayList<>();

        PageLink pageLink = new PageLink(1024);
        PageData<AlarmRule> pageData;

        do {
            pageData = alarmRuleService.findEnabledAlarmRules(tenantId, pageLink);

            alarmRules.addAll(pageData.getData());

            pageLink = pageLink.nextPageLink();
        } while (pageData.hasNext());

        return alarmRules;
    }

    private List<EntityId> getTargetEntities(TenantId tenantId, EntityId originator, AlarmRuleTargetEntity targetEntity) {
        switch (targetEntity.getType()) {
            case ORIGINATOR:
                return Collections.singletonList(originator);
//            case SINGLE_ENTITY:
//                return Collections.singletonList(((AlarmRuleSpecifiedTargetEntity) targetEntity).getEntityId());
//            case RELATION:
//                AlarmRuleRelationTargetEntity relationTargetEntity = (AlarmRuleRelationTargetEntity) targetEntity;
//                if (EntitySearchDirection.FROM == relationTargetEntity.getDirection()) {
//                    List<EntityRelation> relations = relationService.findByToAndType(tenantId, originator, relationTargetEntity.getRelationType(), RelationTypeGroup.COMMON);
//                    return relations.stream().map(EntityRelation::getFrom).collect(Collectors.toList());
//                } else {
//                    List<EntityRelation> relations = relationService.findByFromAndType(tenantId, originator, relationTargetEntity.getRelationType(), RelationTypeGroup.COMMON);
//                    return relations.stream().map(EntityRelation::getTo).collect(Collectors.toList());
//                }
        }
        return Collections.emptyList();
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
            case SINGLE_ENTITY:
            case ENTITY_LIST:
                return filter.isEntityMatches(entityId);
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
                log.warn("AlarmRuleEntityFilter {} not implemented!", filter.getType());
                break;
        }
        return false;
    }

    private boolean isLocalEntity(TenantId tenantId, EntityId entityId) {
        return partitionService.resolve(ServiceType.TB_RULE_ENGINE, tenantId, entityId).isMyPartition();
    }
}
