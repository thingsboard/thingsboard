/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmCreateOrUpdateActiveRequest;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.alarm.AlarmUpdateRequest;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.utils.EdgeVersionUtils;

import java.util.UUID;

@Slf4j
public abstract class BaseAlarmProcessor extends BaseEdgeProcessor {

    public ListenableFuture<Void> processAlarmMsg(TenantId tenantId, AlarmUpdateMsg alarmUpdateMsg, EdgeVersion edgeVersion) {
        log.trace("[{}] processAlarmMsg [{}]", tenantId, alarmUpdateMsg);
        AlarmId alarmId = new AlarmId(new UUID(alarmUpdateMsg.getIdMSB(), alarmUpdateMsg.getIdLSB()));
        boolean isEdgeProtoDeprecated = EdgeVersionUtils.isEdgeProtoDeprecated(edgeVersion);
        Alarm alarm = isEdgeProtoDeprecated ? createDeprecatedAlarm(tenantId, alarmUpdateMsg)
                : JacksonUtil.fromStringIgnoreUnknownProperties(alarmUpdateMsg.getEntity(), Alarm.class);
        if (alarm == null) {
            throw new RuntimeException("[{" + tenantId + "}] alarmUpdateMsg {" + alarmUpdateMsg + "} cannot be converted to alarm");
        }
        EntityId originatorId = isEdgeProtoDeprecated
                ? getAlarmOriginator(tenantId, alarmUpdateMsg.getOriginatorName(), EntityType.valueOf(alarmUpdateMsg.getOriginatorType()))
                : alarm.getOriginator();
        if (originatorId == null) {
            log.warn("[{}] Originator not found for the alarm msg {}", tenantId, alarmUpdateMsg);
            return Futures.immediateFuture(null);
        }
        try {
            edgeSynchronizationManager.getSync().set(true);
            switch (alarmUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    alarm.setId(alarmId);
                    alarm.setOriginator(originatorId);
                    if (UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE.equals(alarmUpdateMsg.getMsgType())) {
                        alarmService.createAlarm(AlarmCreateOrUpdateActiveRequest.fromAlarm(alarm, null, alarmId));
                    } else {
                        alarmService.updateAlarm(AlarmUpdateRequest.fromAlarm(alarm));
                    }
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
        } finally {
            edgeSynchronizationManager.getSync().remove();
        }
        return Futures.immediateFuture(null);
    }

    private Alarm createDeprecatedAlarm(TenantId tenantId, AlarmUpdateMsg alarmUpdateMsg) {
        Alarm alarm = new Alarm();
        alarm.setTenantId(tenantId);
        alarm.setType(alarmUpdateMsg.getName());
        alarm.setSeverity(AlarmSeverity.valueOf(alarmUpdateMsg.getSeverity()));
        alarm.setStartTs(alarmUpdateMsg.getStartTs());
        AlarmStatus alarmStatus = AlarmStatus.valueOf(alarmUpdateMsg.getStatus());
        alarm.setClearTs(alarmUpdateMsg.getClearTs());
        alarm.setPropagate(alarmUpdateMsg.getPropagate());
        alarm.setCleared(alarmStatus.isCleared());
        alarm.setAcknowledged(alarmStatus.isAck());
        alarm.setAckTs(alarmUpdateMsg.getAckTs());
        alarm.setEndTs(alarmUpdateMsg.getEndTs());
        alarm.setDetails(JacksonUtil.toJsonNode(alarmUpdateMsg.getDetails()));
        return alarm;
    }

    private EntityId getAlarmOriginator(TenantId tenantId, String entityName, EntityType entityType) {
        switch (entityType) {
            case DEVICE:
                return deviceService.findDeviceByTenantIdAndName(tenantId, entityName).getId();
            case ASSET:
                return assetService.findAssetByTenantIdAndName(tenantId, entityName).getId();
            case ENTITY_VIEW:
                return entityViewService.findEntityViewByTenantIdAndName(tenantId, entityName).getId();
            default:
                return null;
        }
    }

    public AlarmUpdateMsg convertAlarmEventToAlarmMsg(TenantId tenantId, UUID entityId, EdgeEventActionType actionType, JsonNode body, EdgeVersion edgeVersion) {
        AlarmId alarmId = new AlarmId(entityId);
        UpdateMsgType msgType = getUpdateMsgType(actionType);
        switch (actionType) {
            case ADDED:
            case UPDATED:
            case ALARM_ACK:
            case ALARM_CLEAR:
                Alarm alarm = alarmService.findAlarmById(tenantId, alarmId);
                if (alarm != null) {
                    return alarmMsgConstructor.constructAlarmUpdatedMsg(msgType, alarm, findOriginatorEntityName(tenantId, alarm), edgeVersion);
                }
                break;
            case DELETED:
                Alarm deletedAlarm = JacksonUtil.convertValue(body, Alarm.class);
                return alarmMsgConstructor.constructAlarmUpdatedMsg(msgType, deletedAlarm, findOriginatorEntityName(tenantId, deletedAlarm), edgeVersion);
        }
        return null;
    }

    private String findOriginatorEntityName(TenantId tenantId, Alarm alarm) {
        String entityName = null;
        switch (alarm.getOriginator().getEntityType()) {
            case DEVICE:
                Device deviceById = deviceService.findDeviceById(tenantId, new DeviceId(alarm.getOriginator().getId()));
                if (deviceById != null) {
                    entityName = deviceById.getName();
                }
                break;
            case ASSET:
                Asset assetById = assetService.findAssetById(tenantId, new AssetId(alarm.getOriginator().getId()));
                if (assetById != null) {
                    entityName = assetById.getName();
                }
                break;
            case ENTITY_VIEW:
                EntityView entityViewById = entityViewService.findEntityViewById(tenantId, new EntityViewId(alarm.getOriginator().getId()));
                if (entityViewById != null) {
                    entityName = entityViewById.getName();
                }
                break;
        }
        return entityName;
    }
}
