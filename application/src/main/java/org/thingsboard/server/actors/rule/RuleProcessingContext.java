/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.actors.rule;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmId;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.common.msg.device.ToDeviceActorMsg;
import org.thingsboard.server.extensions.api.device.DeviceMetaData;
import org.thingsboard.server.extensions.api.rules.RuleContext;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class RuleProcessingContext implements RuleContext {

    private final TimeseriesService tsService;
    private final EventService eventService;
    private final AlarmService alarmService;
    private final RuleId ruleId;
    private TenantId tenantId;
    private CustomerId customerId;
    private DeviceId deviceId;
    private DeviceMetaData deviceMetaData;

    RuleProcessingContext(ActorSystemContext systemContext, RuleId ruleId) {
        this.tsService = systemContext.getTsService();
        this.eventService = systemContext.getEventService();
        this.alarmService = systemContext.getAlarmService();
        this.ruleId = ruleId;
    }

    void update(ToDeviceActorMsg toDeviceActorMsg, DeviceMetaData deviceMetaData) {
        this.tenantId = toDeviceActorMsg.getTenantId();
        this.customerId = toDeviceActorMsg.getCustomerId();
        this.deviceId = toDeviceActorMsg.getDeviceId();
        this.deviceMetaData = deviceMetaData;
    }

    @Override
    public RuleId getRuleId() {
        return ruleId;
    }

    @Override
    public DeviceMetaData getDeviceMetaData() {
        return deviceMetaData;
    }

    @Override
    public Event save(Event event) {
        checkEvent(event);
        return eventService.save(event);
    }

    @Override
    public Optional<Event> saveIfNotExists(Event event) {
        checkEvent(event);
        return eventService.saveIfNotExists(event);
    }

    @Override
    public Optional<Event> findEvent(String eventType, String eventUid) {
        return eventService.findEvent(tenantId, deviceId, eventType, eventUid);
    }

    @Override
    public Alarm createOrUpdateAlarm(Alarm alarm) {
        alarm.setTenantId(tenantId);
        return alarmService.createOrUpdateAlarm(alarm);
    }

    public Optional<Alarm> findLatestAlarm(EntityId originator, String alarmType) {
        try {
            return Optional.ofNullable(alarmService.findLatestByOriginatorAndType(tenantId, originator, alarmType).get());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to lookup alarm!", e);
        }
    }

    @Override
    public ListenableFuture<Boolean> clearAlarm(AlarmId alarmId, long clearTs) {
        return alarmService.clearAlarm(alarmId, clearTs);
    }

    private void checkEvent(Event event) {
        if (event.getTenantId() == null) {
            event.setTenantId(tenantId);
        } else if (!tenantId.equals(event.getTenantId())) {
            throw new IllegalArgumentException("Invalid Tenant id!");
        }
        if (event.getEntityId() == null) {
            event.setEntityId(deviceId);
        }
    }
}
