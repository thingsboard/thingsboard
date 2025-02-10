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

import java.util.List;

@Slf4j
@ToString(callSuper = true)
public class TbAlarmCountSubCtx extends TbAbstractEntityQuerySubCtx<AlarmCountQuery> {

    private final AlarmService alarmService;

    private final int maxEntitiesPerAlarmSubscription;

    @Getter
    @Setter
    private volatile int result;

    public TbAlarmCountSubCtx(String serviceId, WebSocketService wsService,
                              EntityService entityService, TbLocalSubscriptionService localSubscriptionService,
                              AttributesService attributesService, SubscriptionServiceStatistics stats, AlarmService alarmService,
                              WebSocketSessionRef sessionRef, int cmdId, int maxEntitiesPerAlarmSubscription) {
        super(serviceId, wsService, entityService, localSubscriptionService, attributesService, stats, sessionRef, cmdId);
        this.alarmService = alarmService;
        this.maxEntitiesPerAlarmSubscription = maxEntitiesPerAlarmSubscription;
    }

    @Override
    public void fetchData() {
        result = countAlarms();
        sendWsMsg(new AlarmCountUpdate(cmdId, result));
    }

    @Override
    protected void update() {
        int newCount = countAlarms();
        if (newCount != result) {
            result = newCount;
            sendWsMsg(new AlarmCountUpdate(cmdId, result));
        }
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    private int countAlarms() {
        List<EntityId> entityIds = null;
        if (query.getEntityFilter() != null) {
            PageData<EntityData> data = entityService.findEntityDataByQuery(getTenantId(), getCustomerId(), buildEntityDataQuery());
            if (data.getData().isEmpty()) {
                return 0;
            }
            entityIds = data.getData().stream().map(EntityData::getEntityId).toList();
        }
        return (int) alarmService.countAlarmsByQuery(getTenantId(), getCustomerId(), query, entityIds);
    }

    private EntityDataQuery buildEntityDataQuery() {
        EntityDataPageLink edpl = new EntityDataPageLink(maxEntitiesPerAlarmSubscription, 0, null,
                new EntityDataSortOrder(new EntityKey(EntityKeyType.ENTITY_FIELD, ModelConstants.CREATED_TIME_PROPERTY)));
        return new EntityDataQuery(query.getEntityFilter(), edpl, null, null, null);
    }
}
