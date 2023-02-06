/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.rule.engine.api.TbAlarmRuleStateService;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.alarm.rule.AlarmRuleEntityState;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleEntityFilter;
import org.thingsboard.server.common.data.id.AlarmRuleId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.alarm.rule.AlarmRuleEntityStateService;
import org.thingsboard.server.dao.alarm.rule.AlarmRuleService;
import org.thingsboard.server.queue.discovery.HashPartitionService;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final TbAlarmRuleEntityFilterWrapperFactory filterWrapperFactory;
    private final HashPartitionService partitionService;

    private ScheduledExecutorService scheduler;

    private final Map<EntityId, EntityState> entityStates = new ConcurrentHashMap<>();
    private final Map<TenantId, Map<AlarmRuleId, AlarmRule>> rules = new ConcurrentHashMap<>();
    private final Map<AlarmRuleId, Set<AlarmRuleEntityFilter>> alarmRuleFilters = new ConcurrentHashMap<>();
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
        Set<EntityState> entityStates = getOrCreateEntityStates(tbContext.getTenantId(), msg.getOriginator());
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
                addFilters(tenantId, alarmRule);
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
                addFilters(tenantId, alarmRule);

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
        alarmRuleFilters.remove(alarmRuleId);

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

    private Set<EntityState> getOrCreateEntityStates(TenantId tenantId, EntityId entityId) {
        List<AlarmRule> filteredRules =
                getOrFetchAlarmRules(tenantId)
                        .stream()
                        .filter(rule -> isEntityMatches(entityId, rule))
                        .collect(Collectors.toList());

        return filteredRules.stream().map(rule -> {
            EntityId targetEntity = rule.getConfiguration().getAlarmTargetEntity().getTargetEntity(entityId);
            return getOrCreateEntityState(tenantId, targetEntity, rule, null);
        }).collect(Collectors.toSet());
    }

    private void createEntityState(TenantId tenantId, EntityId entityId, AlarmRuleEntityState alarmRuleEntityState) {
        List<AlarmRule> filteredRules =
                getOrFetchAlarmRules(tenantId)
                        .stream()
                        .filter(rule -> rule.getConfiguration().getAlarmTargetEntity().getTargetEntity(entityId).equals(entityId))
                        .collect(Collectors.toList());

        filteredRules.forEach(rule -> getOrCreateEntityState(tenantId, entityId, rule, alarmRuleEntityState));
    }

    private Collection<AlarmRule> getOrFetchAlarmRules(TenantId tenantId) {
        return rules.computeIfAbsent(tenantId, key -> {
            Map<AlarmRuleId, AlarmRule> map = new HashMap<>();
            fetchAlarmRules(key).forEach(alarmRule -> {
                map.put(alarmRule.getId(), alarmRule);
                addFilters(tenantId, alarmRule);
            });
            return map;
        }).values();
    }

    private EntityState getOrCreateEntityState(TenantId tenantId, EntityId targetEntityId, AlarmRule alarmRule, AlarmRuleEntityState alarmRuleEntityState) {
        boolean isRuleAdded;
        EntityState entityState = entityStates.get(targetEntityId);
        if (entityState == null) {
            entityState = new EntityState(tenantId, targetEntityId, ctx, new EntityRulesState(alarmRule), alarmRuleEntityState);
            entityStates.put(targetEntityId, entityState);
            isRuleAdded = true;
        } else {
            isRuleAdded = entityState.addAlarmRule(alarmRule);
        }

        if (isRuleAdded) {
            myEntityStateIds.computeIfAbsent(alarmRule.getId(), key -> ConcurrentHashMap.newKeySet()).add(targetEntityId);
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

    private void addFilters(TenantId tenantId, AlarmRule alarmRule) {
        List<AlarmRuleEntityFilter> sourceEntityFilters = alarmRule.getConfiguration().getSourceEntityFilters();
        Set<AlarmRuleEntityFilter> wrappedFilters = sourceEntityFilters.stream().map(filter -> filterWrapperFactory.wrap(tenantId, filter)).collect(Collectors.toSet());
        alarmRuleFilters.put(alarmRule.getId(), wrappedFilters);
    }

    private boolean isEntityMatches(EntityId entityId, AlarmRule alarmRule) {
        for (AlarmRuleEntityFilter filter : alarmRuleFilters.get(alarmRule.getId())) {
            if (filter.isEntityMatches(entityId)) {
                return true;
            }
        }
        return false;
    }

    private boolean isLocalEntity(TenantId tenantId, EntityId entityId) {
        return partitionService.resolve(ServiceType.TB_RULE_ENGINE, tenantId, entityId).isMyPartition();
    }
}
