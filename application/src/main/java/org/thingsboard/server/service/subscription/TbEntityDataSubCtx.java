/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.TsValue;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.service.ws.WebSocketService;
import org.thingsboard.server.service.ws.WebSocketSessionRef;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityDataCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityDataUpdate;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.LatestValueCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.TimeSeriesCmd;
import org.thingsboard.server.service.ws.telemetry.sub.TelemetrySubscriptionUpdate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class TbEntityDataSubCtx extends TbAbstractDataSubCtx<EntityDataQuery> {

    @Getter
    @Setter
    private volatile boolean initialDataSent;
    private TimeSeriesCmd curTsCmd;
    private LatestValueCmd latestValueCmd;
    @Getter
    private final int maxEntitiesPerDataSubscription;
    private Map<EntityId, Map<String, TsValue>> latestTsEntityData;

    public TbEntityDataSubCtx(String serviceId, WebSocketService wsService, EntityService entityService,
                              TbLocalSubscriptionService localSubscriptionService, AttributesService attributesService,
                              SubscriptionServiceStatistics stats, WebSocketSessionRef sessionRef, int cmdId, int maxEntitiesPerDataSubscription) {
        super(serviceId, wsService, entityService, localSubscriptionService, attributesService, stats, sessionRef, cmdId);
        this.maxEntitiesPerDataSubscription = maxEntitiesPerDataSubscription;
    }

    @Override
    public void fetchData() {
        super.fetchData();
        this.updateLatestTsData(this.data);
    }

    @Override
    protected void sendWsMsg(String sessionId, TelemetrySubscriptionUpdate subscriptionUpdate, EntityKeyType keyType, boolean resultToLatestValues) {
        EntityId entityId = subToEntityIdMap.get(subscriptionUpdate.getSubscriptionId());
        if (entityId != null) {
            log.trace("[{}][{}][{}][{}] Received subscription update: {}", sessionId, cmdId, subscriptionUpdate.getSubscriptionId(), keyType, subscriptionUpdate);
            if (resultToLatestValues) {
                sendLatestWsMsg(entityId, sessionId, subscriptionUpdate, keyType);
            } else {
                sendTsWsMsg(entityId, sessionId, subscriptionUpdate, keyType);
            }
        } else {
            log.trace("[{}][{}][{}][{}] Received stale subscription update: {}", sessionId, cmdId, subscriptionUpdate.getSubscriptionId(), keyType, subscriptionUpdate);
        }
    }

    @Override
    protected Aggregation getCurrentAggregation() {
        return (this.curTsCmd == null || this.curTsCmd.getAgg() == null) ? Aggregation.NONE : this.curTsCmd.getAgg();
    }

    private void sendLatestWsMsg(EntityId entityId, String sessionId, TelemetrySubscriptionUpdate subscriptionUpdate, EntityKeyType keyType) {
        Map<String, TsValue> latestUpdate = new HashMap<>();
        subscriptionUpdate.getData().forEach((k, v) -> {
            Object[] data = (Object[]) v.get(0);
            latestUpdate.put(k, new TsValue((Long) data[0], (String) data[1]));
        });
        EntityData entityData = getDataForEntity(entityId);
        if (entityData != null && entityData.getLatest() != null) {
            Map<String, TsValue> latestCtxValues = entityData.getLatest().get(keyType);
            log.trace("[{}][{}][{}] Going to compare update with {}", sessionId, cmdId, subscriptionUpdate.getSubscriptionId(), latestCtxValues);
            if (latestCtxValues != null) {
                latestCtxValues.forEach((k, v) -> {
                    TsValue update = latestUpdate.get(k);
                    if (update != null) {
                        //Ignore notifications about deleted keys
                        if (!(update.getTs() == 0 && (update.getValue() == null || update.getValue().isEmpty()))) {
                            if (update.getTs() < v.getTs()) {
                                log.trace("[{}][{}][{}] Removed stale update for key: {} and ts: {}", sessionId, cmdId, subscriptionUpdate.getSubscriptionId(), k, update.getTs());
                                latestUpdate.remove(k);
                            } else if ((update.getTs() == v.getTs() && update.getValue().equals(v.getValue()))) {
                                log.trace("[{}][{}][{}] Removed duplicate update for key: {} and ts: {}", sessionId, cmdId, subscriptionUpdate.getSubscriptionId(), k, update.getTs());
                                latestUpdate.remove(k);
                            }
                        } else {
                            log.trace("[{}][{}][{}] Received deleted notification for: {}", sessionId, cmdId, subscriptionUpdate.getSubscriptionId(), k);
                        }
                    }
                });
                //Setting new values
                latestUpdate.forEach(latestCtxValues::put);
            }
        }
        if (!latestUpdate.isEmpty()) {
            Map<EntityKeyType, Map<String, TsValue>> latestMap = Collections.singletonMap(keyType, latestUpdate);
            entityData = new EntityData(entityId, latestMap, null);
            sendWsMsg(new EntityDataUpdate(cmdId, null, Collections.singletonList(entityData), maxEntitiesPerDataSubscription));
        }
    }

    private void sendTsWsMsg(EntityId entityId, String sessionId, TelemetrySubscriptionUpdate subscriptionUpdate, EntityKeyType keyType) {
        Map<String, List<TsValue>> tsUpdate = new HashMap<>();
        subscriptionUpdate.getData().forEach((k, v) -> {
            Object[] data = (Object[]) v.get(0);
            tsUpdate.computeIfAbsent(k, key -> new ArrayList<>()).add(new TsValue((Long) data[0], (String) data[1]));
        });
        Map<String, TsValue> latestCtxValues = getLatestTsValuesForEntity(entityId);
        log.trace("[{}][{}][{}] Going to compare update with {}", sessionId, cmdId, subscriptionUpdate.getSubscriptionId(), latestCtxValues);
        if (latestCtxValues != null) {
            latestCtxValues.forEach((k, v) -> {
                List<TsValue> updateList = tsUpdate.get(k);
                if (updateList != null) {
                    for (TsValue update : new ArrayList<>(updateList)) {
                        if (update.getTs() < v.getTs()) {
                            log.trace("[{}][{}][{}] Removed stale update for key: {} and ts: {}", sessionId, cmdId, subscriptionUpdate.getSubscriptionId(), k, update.getTs());
                            // Looks like this is redundant feature and our UI is ready to merge the updates.
                            //updateList.remove(update);
                        } else if ((update.getTs() == v.getTs() && update.getValue().equals(v.getValue()))) {
                            log.trace("[{}][{}][{}] Removed duplicate update for key: {} and ts: {}", sessionId, cmdId, subscriptionUpdate.getSubscriptionId(), k, update.getTs());
                            updateList.remove(update);
                        }
                        if (updateList.isEmpty()) {
                            tsUpdate.remove(k);
                        }
                    }
                }
            });
            //Setting new values
            tsUpdate.forEach((k, v) -> {
                Optional<TsValue> maxValue = v.stream().max(Comparator.comparingLong(TsValue::getTs));
                maxValue.ifPresent(max -> latestCtxValues.put(k, max));
            });
        }
        if (!tsUpdate.isEmpty()) {
            Map<String, TsValue[]> tsMap = new HashMap<>();
            tsUpdate.forEach((key, tsValue) -> tsMap.put(key, tsValue.toArray(new TsValue[tsValue.size()])));
            EntityData entityData = new EntityData(entityId, null, tsMap);
            sendWsMsg(new EntityDataUpdate(cmdId, null, Collections.singletonList(entityData), maxEntitiesPerDataSubscription));
        }
    }

    private EntityData getDataForEntity(EntityId entityId) {
        return data.getData().stream().filter(item -> item.getEntityId().equals(entityId)).findFirst().orElse(null);
    }

    private Map<String, TsValue> getLatestTsValuesForEntity(EntityId entityId) {
        return latestTsEntityData.get(entityId);
    }

    private void updateLatestTsData(PageData<EntityData> data) {
        latestTsEntityData = new HashMap<>();
        data.getData().stream().forEach(entityData -> {
            Map<String, TsValue> latestTsMap = new HashMap<>();
            latestTsEntityData.put(entityData.getEntityId(), latestTsMap);
            if (entityData.getLatest() != null) {
                Map<String, TsValue> latestTsValues = entityData.getLatest().get(EntityKeyType.TIME_SERIES);
                if (latestTsValues != null) {
                    latestTsValues.forEach(latestTsMap::put);
                }
            }
        });
    }

    @Override
    public synchronized void doUpdate(Map<EntityId, EntityData> newDataMap) {
        this.updateLatestTsData(this.data);
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
            // NOTE: We ignore the TS subscriptions for new entities here, because widgets will re-init it's content and will create new subscriptions.
            if (curTsCmd == null && latestValueCmd != null) {
                List<EntityKey> keys = latestValueCmd.getKeys();
                if (keys != null && !keys.isEmpty()) {
                    Map<EntityKeyType, List<EntityKey>> keysByType = getEntityKeyByTypeMap(keys);
                    newSubsList.forEach(
                            entity -> {
                                log.trace("[{}][{}] Found new subscription for entity: {}", sessionRef.getSessionId(), cmdId, entity.getEntityId());
                                subsToAdd.addAll(addSubscriptions(entity, keysByType, true, 0, 0));
                            }
                    );
                }
            }
        }
        subIdsToCancel.forEach(subId -> localSubscriptionService.cancelSubscription(getTenantId(), getSessionId(), subId));
        subsToAdd.forEach(subscription -> localSubscriptionService.addSubscription(subscription, sessionRef));
        sendWsMsg(new EntityDataUpdate(cmdId, data, null, maxEntitiesPerDataSubscription));
    }

    public void setCurrentCmd(EntityDataCmd cmd) {
        curTsCmd = cmd.getTsCmd();
        latestValueCmd = cmd.getLatestCmd();
    }

    @Override
    protected EntityDataQuery buildEntityDataQuery() {
        return query;
    }

}
