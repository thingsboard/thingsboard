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
package org.thingsboard.server.service.edge.rpc.processor.alarm;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterableByTenantIdEntityId;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.gen.edge.v1.AlarmCommentUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.service.edge.rpc.constructor.alarm.AlarmMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.alarm.AlarmMsgConstructorFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.dao.edge.BaseRelatedEdgesService.RELATED_EDGES_CACHE_ITEMS;

@Slf4j
public abstract class AlarmEdgeProcessor extends BaseAlarmProcessor implements AlarmProcessor {

    @Autowired
    private AlarmMsgConstructorFactory alarmMsgConstructorFactory;

    @Autowired
    private EntityService entityService;

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
    public DownlinkMsg convertAlarmEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        AlarmUpdateMsg alarmUpdateMsg = convertAlarmEventToAlarmMsg(edgeEvent.getTenantId(), edgeEvent.getEntityId(),
                edgeEvent.getAction(), edgeEvent.getBody(), edgeVersion);
        if (alarmUpdateMsg != null) {
            return DownlinkMsg.newBuilder()
                    .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                    .addAlarmUpdateMsg(alarmUpdateMsg)
                    .build();
        }
        return null;
    }

    @Override
    public ListenableFuture<Void> processAlarmCommentMsgFromEdge(TenantId tenantId, EdgeId edgeId, AlarmCommentUpdateMsg alarmCommentUpdateMsg) {
        log.trace("[{}] processAlarmCommentMsgFromEdge [{}]", tenantId, alarmCommentUpdateMsg);
        try {
            edgeSynchronizationManager.getEdgeId().set(edgeId);
            return processAlarmCommentMsg(tenantId, alarmCommentUpdateMsg);
        } finally {
            edgeSynchronizationManager.getEdgeId().remove();
        }
    }

    @Override
    public DownlinkMsg convertAlarmCommentEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
        var msgConstructor = (AlarmMsgConstructor) alarmMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion);
        switch (edgeEvent.getAction()) {
            case ADDED_COMMENT:
            case UPDATED_COMMENT:
            case DELETED_COMMENT:
                AlarmComment alarmComment = JacksonUtil.convertValue(edgeEvent.getBody(), AlarmComment.class);
                if (alarmComment != null) {
                    return DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addAlarmCommentUpdateMsg(msgConstructor.constructAlarmCommentUpdatedMsg(msgType, alarmComment))
                            .build();
                }
            default:
                return null;
        }
    }

    public ListenableFuture<Void> processAlarmNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
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

    public ListenableFuture<Void> processAlarmCommentNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        EdgeEventActionType actionType = EdgeEventActionType.valueOf(edgeNotificationMsg.getAction());
        AlarmId alarmId = new AlarmId(new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
        EdgeId originatorEdgeId = safeGetEdgeId(edgeNotificationMsg.getOriginatorEdgeIdMSB(), edgeNotificationMsg.getOriginatorEdgeIdLSB());
        AlarmComment alarmComment = JacksonUtil.fromString(edgeNotificationMsg.getBody(), AlarmComment.class);
        if (alarmComment == null) {
            return Futures.immediateFuture(null);
        }
        Alarm alarmById = edgeCtx.getAlarmService().findAlarmById(tenantId, new AlarmId(alarmComment.getAlarmId().getId()));
        List<ListenableFuture<Void>> delFutures = pushEventToAllRelatedEdges(tenantId, alarmById.getOriginator(),
                alarmId, actionType, JacksonUtil.valueToTree(alarmComment), originatorEdgeId, EdgeEventType.ALARM_COMMENT);
        return Futures.transform(Futures.allAsList(delFutures), voids -> null, dbCallbackExecutorService);
    }

    private List<ListenableFuture<Void>> pushEventToAllRelatedEdges(TenantId tenantId, EntityId originatorId, AlarmId alarmId,
                                                                    EdgeEventActionType actionType, JsonNode body, EdgeId sourceEdgeId,
                                                                    EdgeEventType edgeEventType) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        PageDataIterableByTenantIdEntityId<EdgeId> edgeIds =
                new PageDataIterableByTenantIdEntityId<>(edgeCtx.getEdgeService()::findRelatedEdgeIdsByEntityId, tenantId, originatorId, RELATED_EDGES_CACHE_ITEMS);
        for (EdgeId relatedEdgeId : edgeIds) {
            if (!relatedEdgeId.equals(sourceEdgeId)) {
                futures.add(saveEdgeEvent(tenantId, relatedEdgeId, edgeEventType, actionType, alarmId, body));
            }
        }
        return futures;
    }

    private AlarmUpdateMsg convertAlarmEventToAlarmMsg(TenantId tenantId, UUID entityId, EdgeEventActionType actionType, JsonNode body, EdgeVersion edgeVersion) {
        AlarmId alarmId = new AlarmId(entityId);
        UpdateMsgType msgType = getUpdateMsgType(actionType);
        var msgConstructor = (AlarmMsgConstructor) alarmMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion);
        switch (actionType) {
            case ADDED, UPDATED, ALARM_ACK, ALARM_CLEAR -> {
                Alarm alarm = edgeCtx.getAlarmService().findAlarmById(tenantId, alarmId);
                if (alarm != null) {
                    return msgConstructor.constructAlarmUpdatedMsg(msgType, alarm,
                            entityService.fetchEntityName(tenantId, alarm.getOriginator()).orElse(null));
                }
            }
            case ALARM_DELETE, DELETED -> {
                Alarm deletedAlarm = JacksonUtil.convertValue(body, Alarm.class);
                if (deletedAlarm != null) {
                    return msgConstructor.constructAlarmUpdatedMsg(msgType, deletedAlarm,
                            entityService.fetchEntityName(tenantId, deletedAlarm.getOriginator()).orElse(null));
                }
            }
        }
        return null;
    }

}
