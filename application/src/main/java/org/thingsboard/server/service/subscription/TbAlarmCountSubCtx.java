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
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AlarmCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.service.ws.WebSocketService;
import org.thingsboard.server.service.ws.WebSocketSessionRef;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmCountUpdate;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@ToString(callSuper = true)
public class TbAlarmCountSubCtx extends TbAbstractEntityQuerySubCtx<AlarmCountQuery> {

    private final AlarmService alarmService;

    protected final Map<Integer, EntityId> subToEntityIdMap;

    @Getter
    private LinkedHashSet<EntityId> entitiesIds;

    private final int maxEntitiesPerAlarmSubscription;

    private final int maxAlarmQueriesPerRefreshInterval;

    @Getter
    @Setter
    private volatile int result;

    @Getter
    @Setter
    private boolean tooManyEntities;

    private int alarmCountInvocationAttempts;

    public TbAlarmCountSubCtx(String serviceId, WebSocketService wsService,
                              EntityService entityService, TbLocalSubscriptionService localSubscriptionService,
                              AttributesService attributesService, SubscriptionServiceStatistics stats, AlarmService alarmService,
                              WebSocketSessionRef sessionRef, int cmdId, int maxEntitiesPerAlarmSubscription, int maxAlarmQueriesPerRefreshInterval) {
        super(serviceId, wsService, entityService, localSubscriptionService, attributesService, stats, sessionRef, cmdId);
        this.alarmService = alarmService;
        this.subToEntityIdMap = new ConcurrentHashMap<>();
        this.maxEntitiesPerAlarmSubscription = maxEntitiesPerAlarmSubscription;
        this.maxAlarmQueriesPerRefreshInterval = maxAlarmQueriesPerRefreshInterval;
        this.entitiesIds = null;
    }

    @Override
    public void clearSubscriptions() {
        clearAlarmSubscriptions();
    }

    @Override
    public void fetchData() {
        resetInvocationCounter();
        if (query.getEntityFilter() != null) {
            entitiesIds = new LinkedHashSet<>();
            log.trace("[{}] Fetching data: {}", cmdId, alarmCountInvocationAttempts);
            PageData<EntityData> data = entityService.findEntityDataByQuery(getTenantId(), getCustomerId(), buildEntityDataQuery());
            entitiesIds.clear();
            tooManyEntities = data.hasNext();
            for (EntityData entityData : data.getData()) {
                entitiesIds.add(entityData.getEntityId());
            }
        }
    }

    @Override
    protected void update() {
        resetInvocationCounter();
        fetchAlarmCount();
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    public void fetchAlarmCount() {
        alarmCountInvocationAttempts++;
        log.trace("[{}] Fetching alarms: {}", cmdId, alarmCountInvocationAttempts);
        if (alarmCountInvocationAttempts <= maxAlarmQueriesPerRefreshInterval) {
            int newCount = (int) alarmService.countAlarmsByQuery(getTenantId(), getCustomerId(), query, entitiesIds);
            if (newCount != result) {
                result = newCount;
                sendWsMsg(new AlarmCountUpdate(cmdId, result));
            }
        } else {
            log.trace("[{}] Ignore alarm count fetch due to rate limit: [{}] of maximum [{}]", cmdId, alarmCountInvocationAttempts, maxAlarmQueriesPerRefreshInterval);
        }
    }

    public void doFetchAlarmCount() {
        result = (int) alarmService.countAlarmsByQuery(getTenantId(), getCustomerId(), query, entitiesIds);
        sendWsMsg(new AlarmCountUpdate(cmdId, result));
    }

    private EntityDataQuery buildEntityDataQuery() {
        EntityDataPageLink edpl = new EntityDataPageLink(maxEntitiesPerAlarmSubscription, 0, null,
                new EntityDataSortOrder(new EntityKey(EntityKeyType.ENTITY_FIELD, ModelConstants.CREATED_TIME_PROPERTY)));
        return new EntityDataQuery(query.getEntityFilter(), edpl, null, null, query.getKeyFilters());
    }

    private void resetInvocationCounter() {
        alarmCountInvocationAttempts = 0;
    }

    public void createAlarmSubscriptions() {
        for (EntityId entityId : entitiesIds) {
            createAlarmSubscriptionForEntity(entityId);
        }
    }

    private void createAlarmSubscriptionForEntity(EntityId entityId) {
        int subIdx = sessionRef.getSessionSubIdSeq().incrementAndGet();
        subToEntityIdMap.put(subIdx, entityId);
        log.trace("[{}][{}][{}] Creating alarms subscription for [{}] ", serviceId, cmdId, subIdx, entityId);
        TbAlarmsSubscription subscription = TbAlarmsSubscription.builder()
                .serviceId(serviceId)
                .sessionId(sessionRef.getSessionId())
                .subscriptionId(subIdx)
                .tenantId(sessionRef.getSecurityCtx().getTenantId())
                .entityId(entityId)
                .updateProcessor((sub, update) -> fetchAlarmCount())
                .build();
        localSubscriptionService.addSubscription(subscription, sessionRef);
    }

    public void clearAlarmSubscriptions() {
        if (subToEntityIdMap != null) {
            for (Integer subId : subToEntityIdMap.keySet()) {
                localSubscriptionService.cancelSubscription(getTenantId(), getSessionId(), subId);
            }
            subToEntityIdMap.clear();
        }
    }

}
