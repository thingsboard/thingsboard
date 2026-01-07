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
package org.thingsboard.server.service.edge.rpc.processor.alarm;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class AlarmEdgeProcessor extends BaseAlarmProcessor implements AlarmProcessor {

    @Override
    public ListenableFuture<Void> processAlarmMsgFromEdge(TenantId tenantId, EdgeId edgeId, AlarmUpdateMsg alarmUpdateMsg) {
        log.trace("[{}] processAlarmMsgFromEdge [{}]", tenantId, alarmUpdateMsg);
        try {
            edgeSynchronizationManager.getEdgeId().set(edgeId);
            return processAlarmMsg(tenantId, alarmUpdateMsg);
        } finally {
            edgeSynchronizationManager.getEdgeId().remove();
        }
    }

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        AlarmUpdateMsg alarmUpdateMsg = convertAlarmEventToAlarmMsg(edgeEvent);
        if (alarmUpdateMsg != null) {
            return DownlinkMsg.newBuilder()
                    .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                    .addAlarmUpdateMsg(alarmUpdateMsg)
                    .build();
        }
        return null;
    }

    @Override
    public ListenableFuture<Void> processEntityNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        EdgeEventActionType actionType = EdgeEventActionType.valueOf(edgeNotificationMsg.getAction());
        AlarmId alarmId = new AlarmId(new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
        EdgeId originatorEdgeId = safeGetEdgeId(edgeNotificationMsg.getOriginatorEdgeIdMSB(), edgeNotificationMsg.getOriginatorEdgeIdLSB());
        if (EdgeEventActionType.DELETED.equals(actionType) || EdgeEventActionType.ALARM_DELETE.equals(actionType)) {
            Alarm deletedAlarm = JacksonUtil.fromString(edgeNotificationMsg.getBody(), Alarm.class);
            if (deletedAlarm == null) {
                return Futures.immediateFuture(null);
            }
            List<ListenableFuture<Void>> delFutures = pushEventToAllRelatedEdges(tenantId, deletedAlarm.getOriginator(),
                    alarmId, actionType, JacksonUtil.valueToTree(deletedAlarm), originatorEdgeId, EdgeEventType.ALARM);
            return Futures.transform(Futures.allAsList(delFutures), voids -> null, dbCallbackExecutorService);
        }
        ListenableFuture<Alarm> alarmFuture = edgeCtx.getAlarmService().findAlarmByIdAsync(tenantId, alarmId);
        return Futures.transformAsync(alarmFuture, alarm -> {
            if (alarm == null) {
                return Futures.immediateFuture(null);
            }
            EdgeEventType type = EdgeUtils.getEdgeEventTypeByEntityType(alarm.getOriginator().getEntityType());
            if (type == null) {
                return Futures.immediateFuture(null);
            }
            List<ListenableFuture<Void>> futures = pushEventToAllRelatedEdges(tenantId, alarm.getOriginator(),
                    alarmId, actionType, null, originatorEdgeId, EdgeEventType.ALARM);
            return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
        }, dbCallbackExecutorService);
    }

    private AlarmUpdateMsg convertAlarmEventToAlarmMsg(EdgeEvent edgeEvent) {
        AlarmId alarmId = new AlarmId(edgeEvent.getEntityId());
        UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
        switch (edgeEvent.getAction()) {
            case ADDED, UPDATED, ALARM_ACK, ALARM_CLEAR -> {
                Alarm alarm = edgeCtx.getAlarmService().findAlarmById(edgeEvent.getTenantId(), alarmId);
                if (alarm != null) {
                    return EdgeMsgConstructorUtils.constructAlarmUpdatedMsg(msgType, alarm);
                }
            }
            case ALARM_DELETE, DELETED -> {
                Alarm deletedAlarm = JacksonUtil.convertValue(edgeEvent.getBody(), Alarm.class);
                if (deletedAlarm != null) {
                    return EdgeMsgConstructorUtils.constructAlarmUpdatedMsg(msgType, deletedAlarm);
                }
            }
        }
        return null;
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.ALARM;
    }

}
