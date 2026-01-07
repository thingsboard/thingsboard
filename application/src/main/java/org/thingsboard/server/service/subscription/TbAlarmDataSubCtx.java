/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.subscription;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataPageLink;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.TsValue;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.sql.query.EntityKeyMapping;
import org.thingsboard.server.service.ws.WebSocketService;
import org.thingsboard.server.service.ws.WebSocketSessionRef;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmDataUpdate;
import org.thingsboard.server.service.ws.telemetry.sub.AlarmSubscriptionUpdate;
import org.thingsboard.server.service.ws.telemetry.sub.TelemetrySubscriptionUpdate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@ToString(callSuper = true)
public class TbAlarmDataSubCtx extends TbAbstractDataSubCtx<AlarmDataQuery> {

    private final AlarmService alarmService;
    @Getter
    private final LinkedHashMap<EntityId, EntityData> entitiesMap;
    @Getter
    private final HashMap<AlarmId, AlarmData> alarmsMap;

    private final int maxEntitiesPerAlarmSubscription;

    private final int maxAlarmQueriesPerRefreshInterval;

    @Getter
    @Setter
    private PageData<AlarmData> alarms;
    @Getter
    @Setter
    private boolean tooManyEntities;

    private int alarmInvocationAttempts;

    public TbAlarmDataSubCtx(String serviceId, WebSocketService wsService,
                             EntityService entityService, TbLocalSubscriptionService localSubscriptionService,
                             AttributesService attributesService, SubscriptionServiceStatistics stats, AlarmService alarmService,
                             WebSocketSessionRef sessionRef, int cmdId,
                             int maxEntitiesPerAlarmSubscription, int maxAlarmQueriesPerRefreshInterval) {
        super(serviceId, wsService, entityService, localSubscriptionService, attributesService, stats, sessionRef, cmdId);
        this.maxEntitiesPerAlarmSubscription = maxEntitiesPerAlarmSubscription;
        this.maxAlarmQueriesPerRefreshInterval = maxAlarmQueriesPerRefreshInterval;
        this.alarmService = alarmService;
        this.entitiesMap = new LinkedHashMap<>();
        this.alarmsMap = new HashMap<>();
    }

    @Override
    public void clearSubscriptions() {
        super.clearSubscriptions();
    }

    public void fetchAlarms() {
        alarmInvocationAttempts++;
        log.trace("[{}] Fetching alarms: {}", cmdId, alarmInvocationAttempts);
        if (alarmInvocationAttempts <= maxAlarmQueriesPerRefreshInterval) {
            doFetchAlarms();
        } else {
            log.trace("[{}] Ignore alarm fetch due to rate limit: [{}] of maximum [{}]", cmdId, alarmInvocationAttempts, maxAlarmQueriesPerRefreshInterval);
        }
    }

    private void doFetchAlarms() {
        AlarmDataUpdate update;
        if (!entitiesMap.isEmpty()) {
            long start = System.currentTimeMillis();
            PageData<AlarmData> alarms = alarmService.findAlarmDataByQueryForEntities(getTenantId(), query, getOrderedEntityIds());
            long end = System.currentTimeMillis();
            stats.getAlarmQueryInvocationCnt().incrementAndGet();
            stats.getAlarmQueryTimeSpent().addAndGet(end - start);
            alarms = setAndMergeAlarmsData(alarms);
            update = new AlarmDataUpdate(cmdId, alarms, null, maxEntitiesPerAlarmSubscription, data.getTotalElements());
        } else {
            update = new AlarmDataUpdate(cmdId, new PageData<>(), null, maxEntitiesPerAlarmSubscription, data.getTotalElements());
        }
        sendWsMsg(update);
    }

    public void fetchData() {
        resetInvocationCounter();
        log.trace("[{}] Fetching data: {}", cmdId, alarmInvocationAttempts);
        super.fetchData();
        entitiesMap.clear();
        tooManyEntities = data.hasNext();
        for (EntityData entityData : data.getData()) {
            entitiesMap.put(entityData.getEntityId(), entityData);
        }
    }

    public Collection<EntityId> getOrderedEntityIds() {
        return entitiesMap.keySet();
    }

    public PageData<AlarmData> setAndMergeAlarmsData(PageData<AlarmData> alarms) {
        this.alarms = alarms;
        for (AlarmData alarmData : alarms.getData()) {
            EntityId entityId = alarmData.getEntityId();
            if (entityId != null) {
                EntityData entityData = entitiesMap.get(entityId);
                if (entityData != null) {
                    alarmData.getLatest().putAll(entityData.getLatest());
                }
            }
        }
        alarmsMap.clear();
        alarmsMap.putAll(alarms.getData().stream().collect(Collectors.toMap(AlarmData::getId, Function.identity(), (a, b) -> a)));
        return this.alarms;
    }

    @Override
    public void createLatestValuesSubscriptions(List<EntityKey> keys) {
        super.createLatestValuesSubscriptions(keys);
        createAlarmSubscriptions();
    }

    public void createAlarmSubscriptions() {
        AlarmDataPageLink pageLink = query.getPageLink();
        long startTs = System.currentTimeMillis() - pageLink.getTimeWindow();
        for (EntityData entityData : entitiesMap.values()) {
            createAlarmSubscriptionForEntity(pageLink, startTs, entityData);
        }
    }

    private void createAlarmSubscriptionForEntity(AlarmDataPageLink pageLink, long startTs, EntityData entityData) {
        int subIdx = sessionRef.getSessionSubIdSeq().incrementAndGet();
        subToEntityIdMap.put(subIdx, entityData.getEntityId());
        log.trace("[{}][{}][{}] Creating alarms subscription for [{}] with query: {}", serviceId, cmdId, subIdx, entityData.getEntityId(), pageLink);
        TbAlarmsSubscription subscription = TbAlarmsSubscription.builder()
                .serviceId(serviceId)
                .sessionId(sessionRef.getSessionId())
                .subscriptionId(subIdx)
                .tenantId(sessionRef.getSecurityCtx().getTenantId())
                .entityId(entityData.getEntityId())
                .updateProcessor((sub, update) -> sendWsMsg(sub.getSessionId(), update))
                .ts(startTs)
                .build();
        localSubscriptionService.addSubscription(subscription, sessionRef);
    }

    @Override
    void sendWsMsg(String sessionId, TelemetrySubscriptionUpdate subscriptionUpdate, EntityKeyType keyType, boolean resultToLatestValues) {
        EntityId entityId = subToEntityIdMap.get(subscriptionUpdate.getSubscriptionId());
        if (entityId != null) {
            Map<String, TsValue> latestUpdate = new HashMap<>();
            subscriptionUpdate.getValues().forEach((key, values) -> {
                latestUpdate.put(key, getLatest(values));
            });
            EntityData entityData = entitiesMap.get(entityId);
            entityData.getLatest().computeIfAbsent(keyType, tmp -> new HashMap<>()).putAll(latestUpdate);
            log.trace("[{}][{}][{}][{}] Received subscription update: {}", sessionId, cmdId, subscriptionUpdate.getSubscriptionId(), keyType, subscriptionUpdate);
            List<AlarmData> update = alarmsMap.values().stream().filter(alarm -> entityId.equals(alarm.getEntityId())).map(alarm -> {
                alarm.getLatest().computeIfAbsent(keyType, tmp -> new HashMap<>()).putAll(latestUpdate);
                return alarm;
            }).collect(Collectors.toList());
            if (!update.isEmpty()) {
                sendWsMsg(new AlarmDataUpdate(cmdId, null, update, maxEntitiesPerAlarmSubscription, data.getTotalElements()));
            }
        } else {
            log.trace("[{}][{}][{}][{}] Received stale subscription update: {}", sessionId, cmdId, subscriptionUpdate.getSubscriptionId(), keyType, subscriptionUpdate);
        }
    }

    @Override
    protected Aggregation getCurrentAggregation() {
        return Aggregation.NONE;
    }

    private void sendWsMsg(String sessionId, AlarmSubscriptionUpdate subscriptionUpdate) {
        Alarm alarm = subscriptionUpdate.getAlarm();
        AlarmId alarmId = alarm.getId();
        if (subscriptionUpdate.isAlarmDeleted()) {
            Alarm deleted = alarmsMap.remove(alarmId);
            if (deleted != null) {
                fetchAlarms();
            }
        } else {
            AlarmData current = alarmsMap.get(alarmId);
            boolean onCurrentPage = current != null;
            boolean matchesFilter = filter(alarm);
            if (onCurrentPage) {
                if (matchesFilter) {
                    AlarmData updated = new AlarmData(subscriptionUpdate.getAlarm(), current);
                    alarmsMap.put(alarmId, updated);
                    sendWsMsg(new AlarmDataUpdate(cmdId, null, Collections.singletonList(updated), maxEntitiesPerAlarmSubscription, data.getTotalElements()));
                } else {
                    fetchAlarms();
                }
            } else if (matchesFilter && query.getPageLink().getPage() == 0) {
                fetchAlarms();
            }
        }
    }

    public void cleanupOldAlarms() {
        long expTime = System.currentTimeMillis() - query.getPageLink().getTimeWindow();
        boolean shouldRefresh = false;
        for (AlarmData alarmData : alarms.getData()) {
            if (alarmData.getCreatedTime() < expTime) {
                shouldRefresh = true;
                break;
            }
        }
        if (shouldRefresh) {
            doFetchAlarms();
        }
    }

    private boolean filter(Alarm alarm) {
        AlarmDataPageLink filter = query.getPageLink();
        long startTs = System.currentTimeMillis() - filter.getTimeWindow();
        if (alarm.getCreatedTime() < startTs) {
            //Skip update that does not match time window.
            return false;
        }
        if (filter.getTypeList() != null && !filter.getTypeList().isEmpty() && !filter.getTypeList().contains(alarm.getType())) {
            return false;
        }
        if (filter.getSeverityList() != null && !filter.getSeverityList().isEmpty()) {
            if (!filter.getSeverityList().contains(alarm.getSeverity())) {
                return false;
            }
        }
        if (filter.getStatusList() != null && !filter.getStatusList().isEmpty()) {
            boolean matches = false;
            for (AlarmSearchStatus status : filter.getStatusList()) {
                switch (status) {
                    case ANY:
                        matches = true;
                        break;
                    case ACK:
                        matches = alarm.isAcknowledged();
                        break;
                    case UNACK:
                        matches = !alarm.isAcknowledged();
                        break;
                    case CLEARED:
                        matches = alarm.isCleared();
                        break;
                    case ACTIVE:
                        matches = !alarm.isCleared();
                        break;
                }
                if (matches) {
                    break;
                }
            }
            if (!matches) {
                return false;
            }
        }
        return true;
    }

    public synchronized void checkAndResetInvocationCounter() {
        boolean fetchNeeded = this.alarmInvocationAttempts > maxAlarmQueriesPerRefreshInterval;
        resetInvocationCounter();
        if (fetchNeeded) {
            fetchAlarms();
        } else {
            cleanupOldAlarms();
        }
    }

    @Override
    protected synchronized void doUpdate(Map<EntityId, EntityData> newDataMap) {
        resetInvocationCounter();
        entitiesMap.clear();
        tooManyEntities = data.hasNext();
        for (EntityData entityData : data.getData()) {
            entitiesMap.put(entityData.getEntityId(), entityData);
        }
        fetchAlarms();
        List<Integer> subIdsToCancel = new ArrayList<>();
        List<TbSubscription> subsToAdd = new ArrayList<>();
        Set<EntityId> currentSubs = new HashSet<>();
        subToEntityIdMap.forEach((subId, entityId) -> {
            if (!newDataMap.containsKey(entityId)) {
                subIdsToCancel.add(subId);
            } else {
                currentSubs.add(entityId);
            }
        });
        log.trace("[{}][{}] Subscriptions that are invalid: {}", sessionRef.getSessionId(), cmdId, subIdsToCancel);
        subIdsToCancel.forEach(subToEntityIdMap::remove);
        List<EntityData> newSubsList = newDataMap.entrySet().stream().filter(entry -> !currentSubs.contains(entry.getKey())).map(Map.Entry::getValue).collect(Collectors.toList());
        if (!newSubsList.isEmpty()) {
            List<EntityKey> keys = query.getLatestValues();
            if (keys != null && !keys.isEmpty()) {
                Map<EntityKeyType, List<EntityKey>> keysByType = getEntityKeyByTypeMap(keys);
                newSubsList.forEach(
                        entity -> {
                            log.trace("[{}][{}] Found new subscription for entity: {}", sessionRef.getSessionId(), cmdId, entity.getEntityId());
                            subsToAdd.addAll(addSubscriptions(entity, keysByType, true, 0, 0));
                        }
                );
            }
            long startTs = System.currentTimeMillis() - query.getPageLink().getTimeWindow();
            newSubsList.forEach(entity -> createAlarmSubscriptionForEntity(query.getPageLink(), startTs, entity));
        }
        subIdsToCancel.forEach(subId -> localSubscriptionService.cancelSubscription(getTenantId(), getSessionId(), subId));
        subsToAdd.forEach(subscription -> localSubscriptionService.addSubscription(subscription, sessionRef));
    }

    private void resetInvocationCounter() {
        alarmInvocationAttempts = 0;
    }

    @Override
    protected EntityDataQuery buildEntityDataQuery() {
        EntityDataSortOrder sortOrder = query.getPageLink().getSortOrder();
        EntityDataSortOrder entitiesSortOrder;
        if (sortOrder == null || sortOrder.getKey().getType().equals(EntityKeyType.ALARM_FIELD)) {
            entitiesSortOrder = new EntityDataSortOrder(new EntityKey(EntityKeyType.ENTITY_FIELD, EntityKeyMapping.CREATED_TIME));
        } else {
            entitiesSortOrder = sortOrder;
        }
        EntityDataPageLink edpl = new EntityDataPageLink(maxEntitiesPerAlarmSubscription, 0, null, entitiesSortOrder);
        return new EntityDataQuery(query.getEntityFilter(), edpl, query.getEntityFields(), query.getLatestValues(), query.getKeyFilters());
    }

}
