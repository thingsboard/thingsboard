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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import java.util.List;

@Service
@AllArgsConstructor
public class DefaultTbAlarmService extends AbstractTbEntityService implements TbAlarmService {

    @Override
    public Alarm save(Alarm alarm, User user) throws ThingsboardException {
        ActionType actionType = alarm.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = alarm.getTenantId();
        try {
            Alarm savedAlarm = checkNotNull(alarmSubscriptionService.createOrUpdateAlarm(alarm));
            notificationEntityService.notifyCreateOrUpdateAlarm(savedAlarm, actionType, user);
            return savedAlarm;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ALARM), alarm, actionType, user, e);
            throw e;
        }
    }

    @Override
    public ListenableFuture<Void> ack(Alarm alarm, User user) {
        long ackTs = System.currentTimeMillis();
        ListenableFuture<Boolean> future = alarmSubscriptionService.ackAlarm(alarm.getTenantId(), alarm.getId(), ackTs);
        return Futures.transform(future, result -> {
            alarm.setAckTs(ackTs);
            alarm.setStatus(alarm.getStatus().isCleared() ? AlarmStatus.CLEARED_ACK : AlarmStatus.ACTIVE_ACK);
            notificationEntityService.notifyCreateOrUpdateAlarm(alarm, ActionType.ALARM_ACK, user);
            return null;
        }, MoreExecutors.directExecutor());
    }

    @Override
    public ListenableFuture<Void> clear(Alarm alarm, User user) {
        long clearTs = System.currentTimeMillis();
        ListenableFuture<Boolean> future = alarmSubscriptionService.clearAlarm(alarm.getTenantId(), alarm.getId(), null, clearTs);
        return Futures.transform(future, result -> {
            alarm.setClearTs(clearTs);
            alarm.setStatus(alarm.getStatus().isAck() ? AlarmStatus.CLEARED_ACK : AlarmStatus.CLEARED_UNACK);
            notificationEntityService.notifyCreateOrUpdateAlarm(alarm, ActionType.ALARM_CLEAR, user);
            return null;
        }, MoreExecutors.directExecutor());
    }

    @Override
    public Boolean delete(Alarm alarm, User user) {
        TenantId tenantId = alarm.getTenantId();
        List<EdgeId> relatedEdgeIds = edgeService.findAllRelatedEdgeIds(tenantId, alarm.getOriginator());
        notificationEntityService.notifyDeleteAlarm(tenantId, alarm, alarm.getOriginator(), alarm.getCustomerId(),
                relatedEdgeIds, user, JacksonUtil.toString(alarm));
        return alarmSubscriptionService.deleteAlarm(tenantId, alarm.getId());
    }
}