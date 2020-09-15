/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.rpc.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import org.thingsboard.rule.engine.api.RpcError;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.gen.edge.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.DeviceRpcCallMsg;
import org.thingsboard.server.gen.edge.DeviceUpdateMsg;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.service.rpc.FromDeviceRpcResponse;

import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
public class DeviceProcessor extends BaseProcessor {

    private static final ReentrantLock deviceCreationLock = new ReentrantLock();

    public ListenableFuture<Void> onDeviceUpdate(TenantId tenantId, Edge edge, DeviceUpdateMsg deviceUpdateMsg) {
        DeviceId edgeDeviceId = new DeviceId(new UUID(deviceUpdateMsg.getIdMSB(), deviceUpdateMsg.getIdLSB()));
        switch (deviceUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
                String deviceName = deviceUpdateMsg.getName();
                Device device = deviceService.findDeviceByTenantIdAndName(tenantId, deviceName);
                if (device != null) {
                    // device with this name already exists on the cloud - update ID on the edge
                    if (!device.getId().equals(edgeDeviceId)) {
                        saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.DEVICE, ActionType.ENTITY_EXISTS_REQUEST, device.getId(), null);
                    }
                } else {
                    Device deviceById = deviceService.findDeviceById(edge.getTenantId(), edgeDeviceId);
                    if (deviceById != null) {
                        // this ID already used by other device - create new device and update ID on the edge
                        device = createDevice(tenantId, edge, deviceUpdateMsg);
                        saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.DEVICE, ActionType.ENTITY_EXISTS_REQUEST, device.getId(), null);
                    } else {
                        device = createDevice(tenantId, edge, deviceUpdateMsg);
                    }
                }
                // TODO: voba - assign device only in case device is not assigned yet. Missing functionality to check this relation prior assignment
                deviceService.assignDeviceToEdge(edge.getTenantId(), device.getId(), edge.getId());
                break;
            case ENTITY_UPDATED_RPC_MESSAGE:
                updateDevice(tenantId, edge, deviceUpdateMsg);
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                Device deviceToDelete = deviceService.findDeviceById(tenantId, edgeDeviceId);
                if (deviceToDelete != null) {
                    deviceService.unassignDeviceFromEdge(tenantId, edgeDeviceId, edge.getId());
                }
                break;
            case UNRECOGNIZED:
                log.error("Unsupported msg type {}", deviceUpdateMsg.getMsgType());
                return Futures.immediateFailedFuture(new RuntimeException("Unsupported msg type " + deviceUpdateMsg.getMsgType()));
        }
        return Futures.immediateFuture(null);
    }


    public ListenableFuture<Void> onDeviceCredentialsUpdate(TenantId tenantId, DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg) {
        log.debug("Executing onDeviceCredentialsUpdate, deviceCredentialsUpdateMsg [{}]", deviceCredentialsUpdateMsg);
        DeviceId deviceId = new DeviceId(new UUID(deviceCredentialsUpdateMsg.getDeviceIdMSB(), deviceCredentialsUpdateMsg.getDeviceIdLSB()));
        ListenableFuture<Device> deviceFuture = deviceService.findDeviceByIdAsync(tenantId, deviceId);
        return Futures.transform(deviceFuture, device -> {
            if (device != null) {
                log.debug("Updating device credentials for device [{}]. New device credentials Id [{}], value [{}]",
                        device.getName(), deviceCredentialsUpdateMsg.getCredentialsId(), deviceCredentialsUpdateMsg.getCredentialsValue());
                try {
                    DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
                    deviceCredentials.setCredentialsType(DeviceCredentialsType.valueOf(deviceCredentialsUpdateMsg.getCredentialsType()));
                    deviceCredentials.setCredentialsId(deviceCredentialsUpdateMsg.getCredentialsId());
                    deviceCredentials.setCredentialsValue(deviceCredentialsUpdateMsg.getCredentialsValue());
                    deviceCredentialsService.updateDeviceCredentials(tenantId, deviceCredentials);
                } catch (Exception e) {
                    log.error("Can't update device credentials for device [{}], deviceCredentialsUpdateMsg [{}]", device.getName(), deviceCredentialsUpdateMsg, e);
                    throw new RuntimeException(e);
                }
            }
            return null;
        }, dbCallbackExecutorService);
    }


    private void updateDevice(TenantId tenantId, Edge edge, DeviceUpdateMsg deviceUpdateMsg) {
        DeviceId deviceId = new DeviceId(new UUID(deviceUpdateMsg.getIdMSB(), deviceUpdateMsg.getIdLSB()));
        Device device = deviceService.findDeviceById(tenantId, deviceId);
        device.setName(deviceUpdateMsg.getName());
        device.setType(deviceUpdateMsg.getType());
        device.setLabel(deviceUpdateMsg.getLabel());
        deviceService.saveDevice(device);

        saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.DEVICE, ActionType.CREDENTIALS_REQUEST, deviceId, null);
    }

    private Device createDevice(TenantId tenantId, Edge edge, DeviceUpdateMsg deviceUpdateMsg) {
        Device device;
        try {
            deviceCreationLock.lock();
            DeviceId deviceId = new DeviceId(new UUID(deviceUpdateMsg.getIdMSB(), deviceUpdateMsg.getIdLSB()));
            device = new Device();
            device.setTenantId(edge.getTenantId());
            device.setCustomerId(edge.getCustomerId());
            device.setId(deviceId);
            device.setName(deviceUpdateMsg.getName());
            device.setType(deviceUpdateMsg.getType());
            device.setLabel(deviceUpdateMsg.getLabel());
            device = deviceService.saveDevice(device);
            createDeviceCredentials(device);
            createRelationFromEdge(tenantId, edge.getId(), device.getId());
            deviceStateService.onDeviceAdded(device);
            pushDeviceCreatedEventToRuleEngine(tenantId, edge, device);

            saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.DEVICE, ActionType.CREDENTIALS_REQUEST, deviceId, null);
        } finally {
            deviceCreationLock.unlock();
        }
        return device;
    }

    private void createRelationFromEdge(TenantId tenantId, EdgeId edgeId, EntityId entityId) {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(edgeId);
        relation.setTo(entityId);
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        relation.setType(EntityRelation.EDGE_TYPE);
        relationService.saveRelation(tenantId, relation);
    }

    private void createDeviceCredentials(Device device) {
        DeviceCredentials deviceCredentials = new DeviceCredentials();
        deviceCredentials.setDeviceId(device.getId());
        deviceCredentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        deviceCredentials.setCredentialsId(RandomStringUtils.randomAlphanumeric(20));
        deviceCredentialsService.createDeviceCredentials(device.getTenantId(), deviceCredentials);
    }

    private void pushDeviceCreatedEventToRuleEngine(TenantId tenantId, Edge edge, Device device) {
        try {
            DeviceId deviceId = device.getId();
            ObjectNode entityNode = mapper.valueToTree(device);
            TbMsg tbMsg = TbMsg.newMsg(DataConstants.ENTITY_CREATED, deviceId,
                    getActionTbMsgMetaData(edge, device.getCustomerId()), TbMsgDataType.JSON, mapper.writeValueAsString(entityNode));
            tbClusterService.pushMsgToRuleEngine(tenantId, deviceId, tbMsg, new TbQueueCallback() {
                @Override
                public void onSuccess(TbQueueMsgMetadata metadata) {
                    // TODO: voba - handle success
                    log.debug("Successfully send ENTITY_CREATED EVENT to rule engine [{}]", device);
                }

                @Override
                public void onFailure(Throwable t) {
                    // TODO: voba - handle failure
                    log.debug("Failed to send ENTITY_CREATED EVENT to rule engine [{}]", device, t);
                }

                ;
            });
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.warn("[{}] Failed to push device action to rule engine: {}", device.getId(), DataConstants.ENTITY_CREATED, e);
        }
    }


    private TbMsgMetaData getActionTbMsgMetaData(Edge edge, CustomerId customerId) {
        TbMsgMetaData metaData = getTbMsgMetaData(edge);
        if (customerId != null && !customerId.isNullUid()) {
            metaData.putValue("customerId", customerId.toString());
        }
        return metaData;
    }


    private TbMsgMetaData getTbMsgMetaData(Edge edge) {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("edgeId", edge.getId().toString());
        metaData.putValue("edgeName", edge.getName());
        return metaData;
    }

    public ListenableFuture<Void> processDeviceRpcCallResponseMsg(TenantId tenantId, DeviceRpcCallMsg deviceRpcCallMsg) {
        UUID uuid = new UUID(deviceRpcCallMsg.getRequestIdMSB(), deviceRpcCallMsg.getRequestIdLSB());
        FromDeviceRpcResponse response;
        if (!StringUtils.isEmpty(deviceRpcCallMsg.getResponseMsg().getError())) {
            response = new FromDeviceRpcResponse(uuid, null, RpcError.valueOf(deviceRpcCallMsg.getResponseMsg().getError()));
        } else {
            response = new FromDeviceRpcResponse(uuid, deviceRpcCallMsg.getResponseMsg().getResponse(), null);
        }
        tbDeviceRpcService.sendRpcResponseToTbCore(deviceRpcCallMsg.getOriginServiceId(), response);
        return Futures.immediateFuture(null);
    }

}
