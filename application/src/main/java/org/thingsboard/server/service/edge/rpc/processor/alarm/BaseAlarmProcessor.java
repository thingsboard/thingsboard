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
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCreateOrUpdateActiveRequest;
import org.thingsboard.server.common.data.alarm.AlarmUpdateRequest;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.alarm.AlarmCommentDao;
import org.thingsboard.server.gen.edge.v1.AlarmCommentUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.service.edge.rpc.constructor.alarm.AlarmMsgConstructor;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.UUID;

@Slf4j
public abstract class BaseAlarmProcessor extends BaseEdgeProcessor {

    @Autowired
    protected AlarmCommentDao alarmCommentDao;

    public ListenableFuture<Void> processAlarmMsg(TenantId tenantId, AlarmUpdateMsg alarmUpdateMsg) {
        log.trace("[{}] processAlarmMsg [{}]", tenantId, alarmUpdateMsg);
        AlarmId alarmId = new AlarmId(new UUID(alarmUpdateMsg.getIdMSB(), alarmUpdateMsg.getIdLSB()));
        EntityId originatorId = getAlarmOriginatorFromMsg(tenantId, alarmUpdateMsg);
        Alarm alarm = constructAlarmFromUpdateMsg(tenantId, alarmId, originatorId, alarmUpdateMsg);
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
                    alarmService.createAlarm(AlarmCreateOrUpdateActiveRequest.fromAlarm(alarm, null, alarmId));
                    break;
                case ENTITY_UPDATED_RPC_MESSAGE:
                    alarmService.updateAlarm(AlarmUpdateRequest.fromAlarm(alarm));
                    break;
                case ALARM_ACK_RPC_MESSAGE:
                    Alarm alarmToAck = alarmService.findAlarmById(tenantId, alarmId);
                    if (alarmToAck != null) {
                        alarmService.acknowledgeAlarm(tenantId, alarmId, alarm.getAckTs());
                    }
                    break;
                case ALARM_CLEAR_RPC_MESSAGE:
                    Alarm alarmToClear = alarmService.findAlarmById(tenantId, alarmId);
                    if (alarmToClear != null) {
                        alarmService.clearAlarm(tenantId, alarmId, alarm.getClearTs(), alarm.getDetails());
                    }
                    break;
                case ENTITY_DELETED_RPC_MESSAGE:
                    Alarm alarmToDelete = alarmService.findAlarmById(tenantId, alarmId);
                    if (alarmToDelete != null) {
                        alarmService.delAlarm(tenantId, alarmId);
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
            Alarm alarm = alarmService.findAlarmById(tenantId, new AlarmId(alarmComment.getAlarmId().getId()));
            if (alarm == null) {
                return Futures.immediateFuture(null);
            }
            switch (alarmCommentUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                    alarmCommentDao.save(tenantId, alarmComment);
                    break;
                case ENTITY_UPDATED_RPC_MESSAGE:
                    alarmCommentService.createOrUpdateAlarmComment(tenantId, alarmComment);
                    break;
                case ENTITY_DELETED_RPC_MESSAGE:
                    AlarmComment alarmCommentToDelete = alarmCommentService.findAlarmCommentById(tenantId, alarmComment.getId());
                    if (alarmCommentToDelete != null) {
                        alarmCommentService.saveAlarmComment(tenantId, alarmCommentToDelete);
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


    protected abstract EntityId getAlarmOriginatorFromMsg(TenantId tenantId, AlarmUpdateMsg alarmUpdateMsg);

    protected abstract Alarm constructAlarmFromUpdateMsg(TenantId tenantId, AlarmId alarmId, EntityId originatorId, AlarmUpdateMsg alarmUpdateMsg);

    protected AlarmUpdateMsg convertAlarmEventToAlarmMsg(TenantId tenantId, UUID entityId, EdgeEventActionType actionType, JsonNode body, EdgeVersion edgeVersion) {
        AlarmId alarmId = new AlarmId(entityId);
        UpdateMsgType msgType = getUpdateMsgType(actionType);
        switch (actionType) {
            case ADDED, UPDATED, ALARM_ACK, ALARM_CLEAR -> {
                Alarm alarm = alarmService.findAlarmById(tenantId, alarmId);
                if (alarm != null) {
                    return ((AlarmMsgConstructor) alarmMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion))
                            .constructAlarmUpdatedMsg(msgType, alarm, findOriginatorEntityName(tenantId, alarm));
                }
            }
            case ALARM_DELETE, DELETED -> {
                Alarm deletedAlarm = JacksonUtil.convertValue(body, Alarm.class);
                if (deletedAlarm != null) {
                    return ((AlarmMsgConstructor) alarmMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion))
                            .constructAlarmUpdatedMsg(msgType, deletedAlarm, findOriginatorEntityName(tenantId, deletedAlarm));
                }
            }
        }
        return null;
    }

    private String findOriginatorEntityName(TenantId tenantId, Alarm alarm) {
        String entityName = null;
        switch (alarm.getOriginator().getEntityType()) {
            case DEVICE -> {
                Device deviceById = deviceService.findDeviceById(tenantId, new DeviceId(alarm.getOriginator().getId()));
                if (deviceById != null) {
                    entityName = deviceById.getName();
                }
            }
            case ASSET -> {
                Asset assetById = assetService.findAssetById(tenantId, new AssetId(alarm.getOriginator().getId()));
                if (assetById != null) {
                    entityName = assetById.getName();
                }
            }
            case ENTITY_VIEW -> {
                EntityView entityViewById = entityViewService.findEntityViewById(tenantId, new EntityViewId(alarm.getOriginator().getId()));
                if (entityViewById != null) {
                    entityName = entityViewById.getName();
                }
            }
        }
        return entityName;
    }

}
