/**
 * Copyright © 2016-2020 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataPageLink;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.service.telemetry.TelemetryWebSocketService;
import org.thingsboard.server.service.telemetry.TelemetryWebSocketSessionRef;
import org.thingsboard.server.service.telemetry.sub.AlarmSubscriptionUpdate;
import org.thingsboard.server.service.telemetry.sub.TsSubscriptionUpdate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class TbAlarmDataSubCtx extends TbAbstractDataSubCtx<AlarmDataQuery> {

    @Getter
    @Setter
    private final LinkedHashMap<EntityId, EntityData> entitiesMap;
    @Getter
    @Setter
    private PageData<AlarmData> alarms;
    @Getter
    @Setter
    private boolean tooManyEntities;

    private Map<Integer, EntityId> subToEntityIdMap;
    @Setter
    private long lastFetchTs;

    public TbAlarmDataSubCtx(String serviceId, TelemetryWebSocketService wsService, TelemetryWebSocketSessionRef sessionRef, int cmdId) {
        super(serviceId, wsService, sessionRef, cmdId);
        this.entitiesMap = new LinkedHashMap<>();
    }

    public void setEntitiesData(PageData<EntityData> entitiesData) {
        entitiesMap.clear();
        tooManyEntities = entitiesData.hasNext();
        for (EntityData entityData : entitiesData.getData()) {
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
        return this.alarms;
    }

    public List<TbSubscription> createSubscriptions() {
        this.subToEntityIdMap = new HashMap<>();
        AlarmDataPageLink pageLink = query.getPageLink();
        List<TbSubscription> result = new ArrayList<>();
        for (EntityData entityData : entitiesMap.values()) {
            int subIdx = sessionRef.getSessionSubIdSeq().incrementAndGet();
            subToEntityIdMap.put(subIdx, entityData.getEntityId());
            log.trace("[{}][{}][{}] Creating alarms subscription for [{}] with query: {}", serviceId, cmdId, subIdx, entityData.getEntityId(), pageLink);
            result.add(TbAlarmsSubscription.builder()
                    .serviceId(serviceId)
                    .sessionId(sessionRef.getSessionId())
                    .subscriptionId(subIdx)
                    .tenantId(sessionRef.getSecurityCtx().getTenantId())
                    .entityId(entityData.getEntityId())
                    .updateConsumer(this::sendWsMsg)
                    .ts(lastFetchTs)
                    .typeList(pageLink.getTypeList())
                    .severityList(pageLink.getSeverityList())
                    .statusList(pageLink.getStatusList())
                    .searchPropagatedAlarms(pageLink.isSearchPropagatedAlarms())
                    .build());
        }
        return result;
    }

    private void sendWsMsg(String sessionId, AlarmSubscriptionUpdate subscriptionUpdate) {

    }
}
