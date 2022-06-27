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
package org.thingsboard.server.service.entitiy.alarm;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;

@Service
@TbCoreComponent
@AllArgsConstructor
public class DefaultTbAlarmService extends AbstractTbEntityService implements TbAlarmService {

    @Override
    public Alarm save(Alarm alarm, SecurityUser user) throws ThingsboardException {
        ActionType actionType = alarm.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = alarm.getTenantId();
        try {
            Alarm savedAlarm = checkNotNull(alarmService.createOrUpdateAlarm(alarm).getAlarm());
            notificationEntityService.notifyCreateOrUpdateAlarm(savedAlarm, actionType, user);
            return savedAlarm;
        } catch (Exception e) {
            notificationEntityService.notifyEntity(tenantId, emptyId(EntityType.ALARM), alarm, null, actionType, user, e);
            throw handleException(e);
        }
    }

    @Override
    public void ack(Alarm alarm, SecurityUser user) throws ThingsboardException {
        try {
            long ackTs = System.currentTimeMillis();
            alarmService.ackAlarm(alarm.getTenantId(), alarm.getId(), ackTs).get();
            alarm.setAckTs(ackTs);
            alarm.setStatus(alarm.getStatus().isCleared() ? AlarmStatus.CLEARED_ACK : AlarmStatus.ACTIVE_ACK);
            notificationEntityService.notifyCreateOrUpdateAlarm(alarm, ActionType.ALARM_ACK, user);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @Override
    public void clear(Alarm alarm, SecurityUser user) throws ThingsboardException {
        try {
            long clearTs = System.currentTimeMillis();
            alarmService.clearAlarm(alarm.getTenantId(), alarm.getId(), null, clearTs).get();
            alarm.setClearTs(clearTs);
            alarm.setStatus(alarm.getStatus().isAck() ? AlarmStatus.CLEARED_ACK : AlarmStatus.CLEARED_UNACK);
            notificationEntityService.notifyCreateOrUpdateAlarm(alarm, ActionType.ALARM_CLEAR, user);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @Override
    public Boolean delete(Alarm alarm, SecurityUser user) throws ThingsboardException {
        return delete(alarm, user.getCustomerId(), user);
    }

    @Override
    public Boolean delete(Alarm alarm, CustomerId customerId) throws ThingsboardException {
        return delete(alarm, customerId, null);
    }

    private Boolean delete(Alarm alarm, CustomerId customerId, SecurityUser user) throws ThingsboardException {
        try {
            List<EdgeId> relatedEdgeIds = findRelatedEdgeIds(alarm.getTenantId(), alarm.getOriginator());
            notificationEntityService.notifyDeleteAlarm(alarm.getTenantId(), alarm, alarm.getOriginator(), customerId,
                    relatedEdgeIds, user, JacksonUtil.OBJECT_MAPPER.writeValueAsString(alarm));
            return alarmService.deleteAlarm(alarm.getTenantId(), alarm.getId()).isSuccessful();
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}