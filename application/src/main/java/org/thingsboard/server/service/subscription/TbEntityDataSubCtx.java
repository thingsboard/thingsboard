/**
 * Copyright © 2016-2020 The Thingsboard Authors
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

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.TsValue;
import org.thingsboard.server.service.telemetry.TelemetryWebSocketService;
import org.thingsboard.server.service.telemetry.TelemetryWebSocketSessionRef;
import org.thingsboard.server.service.telemetry.cmd.v2.EntityDataUpdate;
import org.thingsboard.server.service.telemetry.cmd.v2.LatestValueCmd;
import org.thingsboard.server.service.telemetry.cmd.v2.TimeSeriesCmd;
import org.thingsboard.server.service.telemetry.sub.SubscriptionUpdate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Data
public class TbEntityDataSubCtx {

    public static final int MAX_SUBS_PER_CMD = 1024 * 8;
    private final String serviceId;
    private final TelemetryWebSocketService wsService;
    private final TelemetryWebSocketSessionRef sessionRef;
    private final int cmdId;
    private EntityDataQuery query;
    private LatestValueCmd latestCmd;
    private TimeSeriesCmd tsCmd;
    private PageData<EntityData> data;
    private boolean initialDataSent;
    private List<TbSubscription> tbSubs;
    private Map<Integer, EntityId> subToEntityIdMap;

    public TbEntityDataSubCtx(String serviceId, TelemetryWebSocketService wsService, TelemetryWebSocketSessionRef sessionRef, int cmdId) {
        this.serviceId = serviceId;
        this.wsService = wsService;
        this.sessionRef = sessionRef;
        this.cmdId = cmdId;
    }

    public String getSessionId() {
        return sessionRef.getSessionId();
    }

    public TenantId getTenantId() {
        return sessionRef.getSecurityCtx().getTenantId();
    }

    public CustomerId getCustomerId() {
        return sessionRef.getSecurityCtx().getCustomerId();
    }

    public void setData(PageData<EntityData> data) {
        this.data = data;
    }

    public List<TbSubscription> createSubscriptions(List<EntityKey> keys) {
        this.subToEntityIdMap = new HashMap<>();
        tbSubs = new ArrayList<>();
        List<EntityKey> attrSubKeys = new ArrayList<>();
        List<EntityKey> tsSubKeys = new ArrayList<>();
        for (EntityKey key : keys) {
            switch (key.getType()) {
                case TIME_SERIES:
                    tsSubKeys.add(key);
                    break;
                case ATTRIBUTE:
                case CLIENT_ATTRIBUTE:
                case SHARED_ATTRIBUTE:
                case SERVER_ATTRIBUTE:
                    attrSubKeys.add(key);
            }
        }
        for (EntityData entityData : data.getData()) {
            if (!tsSubKeys.isEmpty()) {
                tbSubs.add(createTsSub(entityData, tsSubKeys));
            }
        }
        return tbSubs;
    }

    private TbSubscription createTsSub(EntityData entityData, List<EntityKey> tsSubKeys) {
        int subIdx = sessionRef.getSessionSubIdSeq().incrementAndGet();
        subToEntityIdMap.put(subIdx, entityData.getEntityId());
        Map<String, Long> keyStates = new HashMap<>();
        tsSubKeys.forEach(key -> keyStates.put(key.getKey(), 0L));
        if (entityData.getLatest() != null) {
            Map<String, TsValue> currentValues = entityData.getLatest().get(EntityKeyType.TIME_SERIES);
            if (currentValues != null) {
                currentValues.forEach((k, v) -> {
                    log.trace("[{}][{}] Updating key: {} with ts: {}", serviceId, cmdId, k, v.getTs());
                    keyStates.put(k, v.getTs());
                });
            }
        }
        if (entityData.getTimeseries() != null) {
            entityData.getTimeseries().forEach((k, v) -> {
                long ts = Arrays.stream(v).map(TsValue::getTs).max(Long::compareTo).orElse(0L);
                log.trace("[{}][{}] Updating key: {} with ts: {}", serviceId, cmdId, k, ts);
                keyStates.put(k, ts);
            });
        }

        log.trace("[{}][{}][{}] Creating subscription with keys: {}", serviceId, cmdId, subIdx, keyStates);
        return TbTimeseriesSubscription.builder()
                .serviceId(serviceId)
                .sessionId(sessionRef.getSessionId())
                .subscriptionId(subIdx)
                .tenantId(sessionRef.getSecurityCtx().getTenantId())
                .entityId(entityData.getEntityId())
                .updateConsumer(this::sendTsWsMsg)
                .allKeys(false)
                .keyStates(keyStates).build();
    }


    private void sendTsWsMsg(String sessionId, SubscriptionUpdate subscriptionUpdate) {
        EntityId entityId = subToEntityIdMap.get(subscriptionUpdate.getSubscriptionId());
        if (entityId != null) {
            log.trace("[{}][{}][{}] Received subscription update: {}", sessionId, cmdId, subscriptionUpdate.getSubscriptionId(), subscriptionUpdate);
            Map<String, TsValue> latestUpdate = new HashMap<>();
            subscriptionUpdate.getData().forEach((k, v) -> {
                Object[] data = (Object[]) v.get(0);
                latestUpdate.put(k, new TsValue((Long) data[0], (String) data[1]));
            });
            EntityData entityData = getDataForEntity(entityId);
            if (entityData != null && entityData.getLatest() != null) {
                Map<String, TsValue> latestCtxValues = entityData.getLatest().get(EntityKeyType.TIME_SERIES);
                log.trace("[{}][{}][{}] Going to compare update with {}", sessionId, cmdId, subscriptionUpdate.getSubscriptionId(), latestCtxValues);
                if (latestCtxValues != null) {
                    latestCtxValues.forEach((k, v) -> {
                        TsValue update = latestUpdate.get(k);
                        if (update.getTs() < v.getTs()) {
                            log.trace("[{}][{}][{}] Removed stale update for key: {} and ts: {}", sessionId, cmdId, subscriptionUpdate.getSubscriptionId(), k, update.getTs());
                            latestUpdate.remove(k);
                        } else if ((update.getTs() == v.getTs() && update.getValue().equals(v.getValue()))) {
                            log.trace("[{}][{}][{}] Removed duplicate update for key: {} and ts: {}", sessionId, cmdId, subscriptionUpdate.getSubscriptionId(), k, update.getTs());
                            latestUpdate.remove(k);
                        }
                    });
                    //Setting new values
                    latestUpdate.forEach(latestCtxValues::put);
                }
            }
            if (!latestUpdate.isEmpty()) {
                Map<EntityKeyType, Map<String, TsValue>> latestMap = Collections.singletonMap(EntityKeyType.TIME_SERIES, latestUpdate);
                entityData = new EntityData(entityId, latestMap, null);
                wsService.sendWsMsg(sessionId, new EntityDataUpdate(cmdId, null, Collections.singletonList(entityData)));
            }
        } else {
            log.trace("[{}][{}][{}] Received stale subscription update: {}", sessionId, cmdId, subscriptionUpdate.getSubscriptionId(), subscriptionUpdate);
        }
    }

    private EntityData getDataForEntity(EntityId entityId) {
        return data.getData().stream().filter(item -> item.getEntityId().equals(entityId)).findFirst().orElse(null);
    }

    public Collection<Integer> clearSubscriptions() {
        List<Integer> oldSubIds = new ArrayList<>(subToEntityIdMap.keySet());
        subToEntityIdMap.clear();
        return oldSubIds;
    }
}
