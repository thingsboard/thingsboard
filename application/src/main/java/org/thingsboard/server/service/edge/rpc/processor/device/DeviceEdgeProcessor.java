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
package org.thingsboard.server.service.edge.rpc.processor.device;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.rpc.RpcError;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponse;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceRpcCallMsg;
import org.thingsboard.server.gen.edge.v1.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.rpc.FromDeviceRpcResponseActorMsg;

import java.util.UUID;

@Component
@Slf4j
@TbCoreComponent
public class DeviceEdgeProcessor extends BaseDeviceProcessor {

    public ListenableFuture<Void> processDeviceMsgFromEdge(TenantId tenantId, Edge edge, DeviceUpdateMsg deviceUpdateMsg) {
        log.trace("[{}] executing processDeviceMsgFromEdge [{}] from edge [{}]", tenantId, deviceUpdateMsg, edge.getName());
        DeviceId deviceId = new DeviceId(new UUID(deviceUpdateMsg.getIdMSB(), deviceUpdateMsg.getIdLSB()));
        try {
            edgeSynchronizationManager.getSync().set(true);

            switch (deviceUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    saveOrUpdateDevice(tenantId, deviceId, deviceUpdateMsg, edge);
                    return saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.DEVICE, EdgeEventActionType.CREDENTIALS_REQUEST, deviceId, null);
                case ENTITY_DELETED_RPC_MESSAGE:
                    Device deviceToDelete = deviceService.findDeviceById(tenantId, deviceId);
                    if (deviceToDelete != null) {
                        deviceService.unassignDeviceFromEdge(tenantId, deviceId, edge.getId());
                    }
                    return Futures.immediateFuture(null);
                case UNRECOGNIZED:
                default:
                    return handleUnsupportedMsgType(deviceUpdateMsg.getMsgType());
            }
        } catch (DataValidationException e) {
            if (e.getMessage().contains("limit reached")) {
                log.warn("[{}] Number of allowed devices violated {}", tenantId, deviceUpdateMsg, e);
                return Futures.immediateFuture(null);
            } else {
                return Futures.immediateFailedFuture(e);
            }
        } finally {
            edgeSynchronizationManager.getSync().remove();
        }
    }

    private void saveOrUpdateDevice(TenantId tenantId, DeviceId deviceId, DeviceUpdateMsg deviceUpdateMsg, Edge edge) {
        CustomerId customerId = safeGetCustomerId(deviceUpdateMsg.getCustomerIdMSB(), deviceUpdateMsg.getCustomerIdLSB());
        Pair<Boolean, Boolean> resultPair = super.saveOrUpdateDevice(tenantId, deviceId, deviceUpdateMsg, customerId);
        Boolean created = resultPair.getFirst();
        if (created) {
            createRelationFromEdge(tenantId, edge.getId(), deviceId);
            pushDeviceCreatedEventToRuleEngine(tenantId, edge, deviceId);
            deviceService.assignDeviceToEdge(tenantId, deviceId, edge.getId());
        }
        Boolean deviceNameUpdated = resultPair.getSecond();
        if (deviceNameUpdated) {
            saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.DEVICE, EdgeEventActionType.UPDATED, deviceId, null);
        }
    }

    private void pushDeviceCreatedEventToRuleEngine(TenantId tenantId, Edge edge, DeviceId deviceId) {
        try {
            Device device = deviceService.findDeviceById(tenantId, deviceId);
            String deviceAsString = JacksonUtil.toString(device);
            TbMsgMetaData msgMetaData = getEdgeActionTbMsgMetaData(edge, device.getCustomerId());
            pushEntityEventToRuleEngine(tenantId, deviceId, device.getCustomerId(), TbMsgType.ENTITY_CREATED, deviceAsString, msgMetaData);
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to push device action to rule engine: {}", tenantId, deviceId, TbMsgType.ENTITY_CREATED.name(), e);
        }
    }

    public ListenableFuture<Void> processDeviceRpcCallFromEdge(TenantId tenantId, Edge edge, DeviceRpcCallMsg deviceRpcCallMsg) {
        log.trace("[{}] processDeviceRpcCallFromEdge [{}]", tenantId, deviceRpcCallMsg);
        if (deviceRpcCallMsg.hasResponseMsg()) {
            return processDeviceRpcResponseFromEdge(tenantId, deviceRpcCallMsg);
        } else if (deviceRpcCallMsg.hasRequestMsg()) {
            return processDeviceRpcRequestFromEdge(tenantId, edge, deviceRpcCallMsg);
        }
        return Futures.immediateFuture(null);
    }

    private ListenableFuture<Void> processDeviceRpcResponseFromEdge(TenantId tenantId, DeviceRpcCallMsg deviceRpcCallMsg) {
        SettableFuture<Void> futureToSet = SettableFuture.create();
        UUID requestUuid = new UUID(deviceRpcCallMsg.getRequestUuidMSB(), deviceRpcCallMsg.getRequestUuidLSB());
        DeviceId deviceId = new DeviceId(new UUID(deviceRpcCallMsg.getDeviceIdMSB(), deviceRpcCallMsg.getDeviceIdLSB()));

        FromDeviceRpcResponse response;
        if (!StringUtils.isEmpty(deviceRpcCallMsg.getResponseMsg().getError())) {
            response = new FromDeviceRpcResponse(requestUuid, null, RpcError.valueOf(deviceRpcCallMsg.getResponseMsg().getError()));
        } else {
            response = new FromDeviceRpcResponse(requestUuid, deviceRpcCallMsg.getResponseMsg().getResponse(), null);
        }
        TbQueueCallback callback = new TbQueueCallback() {
            @Override
            public void onSuccess(TbQueueMsgMetadata metadata) {
                futureToSet.set(null);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("[{}] Can't process push notification to core [{}]", tenantId, deviceRpcCallMsg, t);
                futureToSet.setException(t);
            }
        };
        FromDeviceRpcResponseActorMsg msg =
                new FromDeviceRpcResponseActorMsg(deviceRpcCallMsg.getRequestId(),
                        tenantId,
                        deviceId, response);
        tbClusterService.pushMsgToCore(msg, callback);
        return futureToSet;
    }

    private ListenableFuture<Void> processDeviceRpcRequestFromEdge(TenantId tenantId, Edge edge, DeviceRpcCallMsg deviceRpcCallMsg) {
        DeviceId deviceId = new DeviceId(new UUID(deviceRpcCallMsg.getDeviceIdMSB(), deviceRpcCallMsg.getDeviceIdLSB()));
        try {
            TbMsgMetaData metaData = new TbMsgMetaData();
            String requestId = Integer.toString(deviceRpcCallMsg.getRequestId());
            metaData.putValue("requestId", requestId);
            metaData.putValue("serviceId", deviceRpcCallMsg.getServiceId());
            metaData.putValue("sessionId", deviceRpcCallMsg.getSessionId());
            metaData.putValue(DataConstants.EDGE_ID, edge.getId().toString());
            Device device = deviceService.findDeviceById(tenantId, deviceId);
            if (device != null) {
                metaData.putValue("deviceName", device.getName());
                metaData.putValue("deviceType", device.getType());
                metaData.putValue(DataConstants.DEVICE_ID, deviceId.getId().toString());
            }
            ObjectNode data = JacksonUtil.newObjectNode();
            data.put("method", deviceRpcCallMsg.getRequestMsg().getMethod());
            data.put("params", deviceRpcCallMsg.getRequestMsg().getParams());
            TbMsg tbMsg = TbMsg.newMsg(TbMsgType.TO_SERVER_RPC_REQUEST, deviceId, null, metaData,
                    TbMsgDataType.JSON, JacksonUtil.OBJECT_MAPPER.writeValueAsString(data));
            tbClusterService.pushMsgToRuleEngine(tenantId, deviceId, tbMsg, new TbQueueCallback() {
                @Override
                public void onSuccess(TbQueueMsgMetadata metadata) {
                    log.debug("[{}] Successfully send TO_SERVER_RPC_REQUEST to rule engine [{}], deviceRpcCallMsg {}",
                            tenantId, device, deviceRpcCallMsg);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.debug("[{}] Failed to send TO_SERVER_RPC_REQUEST to rule engine [{}], deviceRpcCallMsg {}",
                            tenantId, device, deviceRpcCallMsg, t);
                }
            });
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.warn("[{}][{}] Failed to push TO_SERVER_RPC_REQUEST to rule engine. deviceRpcCallMsg {}", tenantId, deviceId, deviceRpcCallMsg, e);
        }

        return Futures.immediateFuture(null);
    }

    public DownlinkMsg convertDeviceEventToDownlink(EdgeEvent edgeEvent, EdgeId edgeId, EdgeVersion edgeVersion) {
        DeviceId deviceId = new DeviceId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEvent.getAction()) {
            case ADDED:
            case UPDATED:
            case ASSIGNED_TO_EDGE:
            case ASSIGNED_TO_CUSTOMER:
            case UNASSIGNED_FROM_CUSTOMER:
                Device device = deviceService.findDeviceById(edgeEvent.getTenantId(), deviceId);
                if (device != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    DeviceUpdateMsg deviceUpdateMsg =
                            deviceMsgConstructor.constructDeviceUpdatedMsg(msgType, device);
                    DownlinkMsg.Builder builder = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addDeviceUpdateMsg(deviceUpdateMsg);
                    if (UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE.equals(msgType)) {
                        DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileById(edgeEvent.getTenantId(), device.getDeviceProfileId());
                        deviceProfile = checkIfDeviceProfileDefaultFieldsAssignedToEdge(edgeEvent.getTenantId(), edgeId, deviceProfile, edgeVersion);
                        builder.addDeviceProfileUpdateMsg(deviceProfileMsgConstructor.constructDeviceProfileUpdatedMsg(msgType, deviceProfile));
                    }
                    downlinkMsg = builder.build();
                }
                break;
            case DELETED:
            case UNASSIGNED_FROM_EDGE:
                DeviceUpdateMsg deviceUpdateMsg =
                        deviceMsgConstructor.constructDeviceDeleteMsg(deviceId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addDeviceUpdateMsg(deviceUpdateMsg)
                        .build();
                break;
            case CREDENTIALS_UPDATED:
                DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(edgeEvent.getTenantId(), deviceId);
                if (deviceCredentials != null) {
                    DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg =
                            deviceMsgConstructor.constructDeviceCredentialsUpdatedMsg(deviceCredentials);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addDeviceCredentialsUpdateMsg(deviceCredentialsUpdateMsg)
                            .build();
                }
                break;
            case RPC_CALL:
                return convertRpcCallEventToDownlink(edgeEvent);
            case CREDENTIALS_REQUEST:
                return convertCredentialsRequestEventToDownlink(edgeEvent);
        }
        return downlinkMsg;
    }

    private DownlinkMsg convertRpcCallEventToDownlink(EdgeEvent edgeEvent) {
        return DownlinkMsg.newBuilder()
                .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                .addDeviceRpcCallMsg(deviceMsgConstructor.constructDeviceRpcCallMsg(edgeEvent.getEntityId(), edgeEvent.getBody()))
                .build();
    }

    private DownlinkMsg convertCredentialsRequestEventToDownlink(EdgeEvent edgeEvent) {
        DeviceId deviceId = new DeviceId(edgeEvent.getEntityId());
        DeviceCredentialsRequestMsg deviceCredentialsRequestMsg = DeviceCredentialsRequestMsg.newBuilder()
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .build();
        DownlinkMsg.Builder builder = DownlinkMsg.newBuilder()
                .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                .addDeviceCredentialsRequestMsg(deviceCredentialsRequestMsg);
        return builder.build();
    }
}
