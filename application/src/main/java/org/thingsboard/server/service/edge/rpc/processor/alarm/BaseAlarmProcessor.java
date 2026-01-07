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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCreateOrUpdateActiveRequest;
import org.thingsboard.server.common.data.alarm.AlarmUpdateRequest;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterableByTenantIdEntityId;
import org.thingsboard.server.dao.alarm.AlarmCommentDao;
import org.thingsboard.server.gen.edge.v1.AlarmCommentUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AlarmUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.dao.edge.BaseRelatedEdgesService.RELATED_EDGES_CACHE_ITEMS;

@Slf4j
public abstract class BaseAlarmProcessor extends BaseEdgeProcessor {

    @Autowired
    protected AlarmCommentDao alarmCommentDao;

    public ListenableFuture<Void> processAlarmMsg(TenantId tenantId, AlarmUpdateMsg alarmUpdateMsg) {
        log.trace("[{}] processAlarmMsg [{}]", tenantId, alarmUpdateMsg);
        AlarmId alarmId = new AlarmId(new UUID(alarmUpdateMsg.getIdMSB(), alarmUpdateMsg.getIdLSB()));
        Alarm alarm = JacksonUtil.fromString(alarmUpdateMsg.getEntity(), Alarm.class, true);
        if (alarm == null) {
            throw new RuntimeException("[{" + tenantId + "}] alarmUpdateMsg {" + alarmUpdateMsg + "} cannot be converted to alarm");
        }
        if (alarm.getOriginator() == null) {
            log.warn("[{}] Originator not found for the alarm msg {}", tenantId, alarmUpdateMsg);
            return Futures.immediateFuture(null);
        }
        try {
            switch (alarmUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                    edgeCtx.getAlarmService().createAlarm(AlarmCreateOrUpdateActiveRequest.fromAlarm(alarm, null, alarmId));
                    break;
                case ENTITY_UPDATED_RPC_MESSAGE:
                    edgeCtx.getAlarmService().updateAlarm(AlarmUpdateRequest.fromAlarm(alarm));
                    break;
                case ALARM_ACK_RPC_MESSAGE:
                    Alarm alarmToAck = edgeCtx.getAlarmService().findAlarmById(tenantId, alarmId);
                    if (alarmToAck != null) {
                        edgeCtx.getAlarmService().acknowledgeAlarm(tenantId, alarmId, alarm.getAckTs());
                    }
                    break;
                case ALARM_CLEAR_RPC_MESSAGE:
                    Alarm alarmToClear = edgeCtx.getAlarmService().findAlarmById(tenantId, alarmId);
                    if (alarmToClear != null) {
                        edgeCtx.getAlarmService().clearAlarm(tenantId, alarmId, alarm.getClearTs(), alarm.getDetails());
                    }
                    break;
                case ENTITY_DELETED_RPC_MESSAGE:
                    Alarm alarmToDelete = edgeCtx.getAlarmService().findAlarmById(tenantId, alarmId);
                    if (alarmToDelete != null) {
                        edgeCtx.getAlarmService().delAlarm(tenantId, alarmId);
                    }
                    break;
                case UNRECOGNIZED:
                default:
                    return handleUnsupportedMsgType(alarmUpdateMsg.getMsgType());
            }
        } catch (Exception e) {
            log.error("[{}] Failed to process alarm update msg [{}]", tenantId, alarmUpdateMsg, e);
            return Futures.immediateFailedFuture(e);
        }
        return Futures.immediateFuture(null);
    }

    public ListenableFuture<Void> processAlarmCommentMsg(TenantId tenantId, AlarmCommentUpdateMsg alarmCommentUpdateMsg) {
        log.trace("[{}] processAlarmCommentMsg [{}]", tenantId, alarmCommentUpdateMsg);
        AlarmComment alarmComment = JacksonUtil.fromString(alarmCommentUpdateMsg.getEntity(), AlarmComment.class, true);
        if (alarmComment == null) {
            throw new RuntimeException("[{" + tenantId + "}] alarmCommentUpdateMsg {" + alarmCommentUpdateMsg + "} cannot be converted to alarm comment");
        }
        try {
            Alarm alarm = edgeCtx.getAlarmService().findAlarmById(tenantId, new AlarmId(alarmComment.getAlarmId().getId()));
            if (alarm == null) {
                return Futures.immediateFuture(null);
            }
            switch (alarmCommentUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                    alarmCommentDao.save(tenantId, alarmComment);
                    break;
                case ENTITY_UPDATED_RPC_MESSAGE:
                    edgeCtx.getAlarmCommentService().createOrUpdateAlarmComment(tenantId, alarmComment);
                    break;
                case ENTITY_DELETED_RPC_MESSAGE:
                    AlarmComment alarmCommentToDelete = edgeCtx.getAlarmCommentService().findAlarmCommentById(tenantId, alarmComment.getId());
                    if (alarmCommentToDelete != null) {
                        edgeCtx.getAlarmCommentService().saveAlarmComment(tenantId, alarmCommentToDelete);
                    }
                    break;
                case UNRECOGNIZED:
                default:
                    return handleUnsupportedMsgType(alarmCommentUpdateMsg.getMsgType());
            }
        } catch (Exception e) {
            log.error("[{}] Failed to process alarm comment update msg [{}]", tenantId, alarmCommentUpdateMsg, e);
            return Futures.immediateFailedFuture(e);
        }
        return Futures.immediateFuture(null);
    }

    protected List<ListenableFuture<Void>> pushEventToAllRelatedEdges(TenantId tenantId, EntityId originatorId, AlarmId alarmId,
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

}
