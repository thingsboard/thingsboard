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
package org.thingsboard.server.service.edge.rpc.processor;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rpc.RpcError;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponse;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceRpcCallMsg;
import org.thingsboard.server.gen.edge.v1.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.rpc.FromDeviceRpcResponseActorMsg;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
@TbCoreComponent
public class DeviceEdgeProcessor extends BaseEdgeProcessor {

    private final Map<String, EdgeRpcRequestMetadata> toServerRpcPendingMap = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler;

    @Autowired
    private DataDecodingEncodingService dataDecodingEncodingService;

    private static final ReentrantLock deviceCreationLock = new ReentrantLock();

    @PostConstruct
    public void init(){
        this.scheduler = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("device-edge-processor-scheduler"));
    }

    public ListenableFuture<Void> processDeviceFromEdge(TenantId tenantId, Edge edge, DeviceUpdateMsg deviceUpdateMsg) {
        log.trace("[{}] onDeviceUpdate [{}] from edge [{}]", tenantId, deviceUpdateMsg, edge.getName());
        switch (deviceUpdateMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
                String deviceName = deviceUpdateMsg.getName();
                Device device = deviceService.findDeviceByTenantIdAndName(tenantId, deviceName);
                if (device != null) {
                    boolean deviceAlreadyExistsForThisEdge = isDeviceAlreadyExistsOnCloudForThisEdge(tenantId, edge, device);
                    if (deviceAlreadyExistsForThisEdge) {
                        log.info("[{}] Device with name '{}' already exists on the cloud, and related to this edge [{}]. " +
                                "deviceUpdateMsg [{}], Updating device", tenantId, deviceName, edge.getId(), deviceUpdateMsg);
                        return updateDevice(tenantId, edge, deviceUpdateMsg);
                    } else {
                        log.info("[{}] Device with name '{}' already exists on the cloud, but not related to this edge [{}]. deviceUpdateMsg [{}]." +
                                "Creating a new device with random prefix and relate to this edge", tenantId, deviceName, edge.getId(), deviceUpdateMsg);
                        String newDeviceName = deviceUpdateMsg.getName() + "_" + StringUtils.randomAlphabetic(15);
                        Device newDevice;
                        try {
                            newDevice = createDevice(tenantId, edge, deviceUpdateMsg, newDeviceName);
                        } catch (DataValidationException e) {
                            log.error("[{}] Device update msg can't be processed due to data validation [{}]", tenantId, deviceUpdateMsg, e);
                            return Futures.immediateFuture(null);
                        }
                        ObjectNode body = JacksonUtil.OBJECT_MAPPER.createObjectNode();
                        body.put("conflictName", deviceName);
                        ListenableFuture<Void> input = saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.DEVICE, EdgeEventActionType.ENTITY_MERGE_REQUEST, newDevice.getId(), body);
                        return Futures.transformAsync(input, unused ->
                                saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.DEVICE, EdgeEventActionType.CREDENTIALS_REQUEST, newDevice.getId(), null),
                                dbCallbackExecutorService);
                    }
                } else {
                    log.info("[{}] Creating new device on the cloud [{}]", tenantId, deviceUpdateMsg);
                    try {
                        device = createDevice(tenantId, edge, deviceUpdateMsg, deviceUpdateMsg.getName());
                    } catch (DataValidationException e) {
                        log.error("[{}] Device update msg can't be processed due to data validation [{}]", tenantId, deviceUpdateMsg, e);
                        return Futures.immediateFuture(null);
                    }
                    return saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.DEVICE, EdgeEventActionType.CREDENTIALS_REQUEST, device.getId(), null);
                }
            case ENTITY_UPDATED_RPC_MESSAGE:
                return updateDevice(tenantId, edge, deviceUpdateMsg);
            case ENTITY_DELETED_RPC_MESSAGE:
                DeviceId deviceId = new DeviceId(new UUID(deviceUpdateMsg.getIdMSB(), deviceUpdateMsg.getIdLSB()));
                Device deviceToDelete = deviceService.findDeviceById(tenantId, deviceId);
                if (deviceToDelete != null) {
                    deviceService.unassignDeviceFromEdge(tenantId, deviceId, edge.getId());
                }
                return Futures.immediateFuture(null);
            case UNRECOGNIZED:
            default:
                log.error("Unsupported msg type {}", deviceUpdateMsg.getMsgType());
                return Futures.immediateFailedFuture(new RuntimeException("Unsupported msg type " + deviceUpdateMsg.getMsgType()));
        }
    }

    private boolean isDeviceAlreadyExistsOnCloudForThisEdge(TenantId tenantId, Edge edge, Device device) {
        PageLink pageLink = new PageLink(DEFAULT_PAGE_SIZE);
        PageData<EdgeId> pageData;
        do {
            pageData = edgeService.findRelatedEdgeIdsByEntityId(tenantId, device.getId(), pageLink);
            if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                if (pageData.getData().contains(edge.getId())) {
                    return true;
                }
                if (pageData.hasNext()) {
                    pageLink = pageLink.nextPageLink();
                }
            }
        } while (pageData != null && pageData.hasNext());
        return false;
    }

    public ListenableFuture<Void> processDeviceCredentialsFromEdge(TenantId tenantId, DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg) {
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
                    if (deviceCredentialsUpdateMsg.hasCredentialsValue()) {
                        deviceCredentials.setCredentialsValue(deviceCredentialsUpdateMsg.getCredentialsValue());
                    }
                    deviceCredentialsService.updateDeviceCredentials(tenantId, deviceCredentials);
                } catch (Exception e) {
                    log.error("Can't update device credentials for device [{}], deviceCredentialsUpdateMsg [{}]", device.getName(), deviceCredentialsUpdateMsg, e);
                    throw new RuntimeException(e);
                }
            }
            return null;
        }, dbCallbackExecutorService);
    }


    private ListenableFuture<Void> updateDevice(TenantId tenantId, Edge edge, DeviceUpdateMsg deviceUpdateMsg) {
        DeviceId deviceId = new DeviceId(new UUID(deviceUpdateMsg.getIdMSB(), deviceUpdateMsg.getIdLSB()));
        Device device = deviceService.findDeviceById(tenantId, deviceId);
        if (device != null) {
            device.setName(deviceUpdateMsg.getName());
            device.setType(deviceUpdateMsg.getType());
            if (deviceUpdateMsg.hasLabel()) {
                device.setLabel(deviceUpdateMsg.getLabel());
            }
            if (deviceUpdateMsg.hasAdditionalInfo()) {
                device.setAdditionalInfo(JacksonUtil.toJsonNode(deviceUpdateMsg.getAdditionalInfo()));
            }
            if (deviceUpdateMsg.hasDeviceProfileIdMSB() && deviceUpdateMsg.hasDeviceProfileIdLSB()) {
                DeviceProfileId deviceProfileId = new DeviceProfileId(
                        new UUID(deviceUpdateMsg.getDeviceProfileIdMSB(),
                                deviceUpdateMsg.getDeviceProfileIdLSB()));
                device.setDeviceProfileId(deviceProfileId);
            }
            device.setCustomerId(getCustomerId(deviceUpdateMsg));
            Optional<DeviceData> deviceDataOpt =
                    dataDecodingEncodingService.decode(deviceUpdateMsg.getDeviceDataBytes().toByteArray());
            if (deviceDataOpt.isPresent()) {
                device.setDeviceData(deviceDataOpt.get());
            }
            Device savedDevice = deviceService.saveDevice(device);
            tbClusterService.onDeviceUpdated(savedDevice, device, false);
            return saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.DEVICE, EdgeEventActionType.CREDENTIALS_REQUEST, deviceId, null);
        } else {
            String errMsg = String.format("[%s] can't find device [%s], edge [%s]", tenantId, deviceUpdateMsg, edge.getId());
            log.warn(errMsg);
            return Futures.immediateFailedFuture(new RuntimeException(errMsg));
        }
    }

    private Device createDevice(TenantId tenantId, Edge edge, DeviceUpdateMsg deviceUpdateMsg, String deviceName) {
        Device device;
        deviceCreationLock.lock();
        try {
            log.debug("[{}] Creating device entity [{}] from edge [{}]", tenantId, deviceUpdateMsg, edge.getName());
            DeviceId deviceId = new DeviceId(new UUID(deviceUpdateMsg.getIdMSB(), deviceUpdateMsg.getIdLSB()));
            device = deviceService.findDeviceById(tenantId, deviceId);
            boolean created = false;
            if (device == null) {
                device = new Device();
                device.setTenantId(tenantId);
                device.setCreatedTime(Uuids.unixTimestamp(deviceId.getId()));
                created = true;
            }
            device.setName(deviceName);
            device.setType(deviceUpdateMsg.getType());
            if (deviceUpdateMsg.hasLabel()) {
                device.setLabel(deviceUpdateMsg.getLabel());
            }
            if (deviceUpdateMsg.hasAdditionalInfo()) {
                device.setAdditionalInfo(JacksonUtil.toJsonNode(deviceUpdateMsg.getAdditionalInfo()));
            }
            if (deviceUpdateMsg.hasDeviceProfileIdMSB() && deviceUpdateMsg.hasDeviceProfileIdLSB()) {
                DeviceProfileId deviceProfileId = new DeviceProfileId(
                        new UUID(deviceUpdateMsg.getDeviceProfileIdMSB(),
                                deviceUpdateMsg.getDeviceProfileIdLSB()));
                device.setDeviceProfileId(deviceProfileId);
            }
            device.setCustomerId(getCustomerId(deviceUpdateMsg));
            Optional<DeviceData> deviceDataOpt =
                    dataDecodingEncodingService.decode(deviceUpdateMsg.getDeviceDataBytes().toByteArray());
            if (deviceDataOpt.isPresent()) {
                device.setDeviceData(deviceDataOpt.get());
            }
            if (created) {
                deviceValidator.validate(device, Device::getTenantId);
                device.setId(deviceId);
            } else {
                deviceValidator.validate(device, Device::getTenantId);
            }
            Device savedDevice = deviceService.saveDevice(device, false);
            tbClusterService.onDeviceUpdated(savedDevice, created ? null : device, false);
            if (created) {
                DeviceCredentials deviceCredentials = new DeviceCredentials();
                deviceCredentials.setDeviceId(new DeviceId(savedDevice.getUuidId()));
                deviceCredentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
                deviceCredentials.setCredentialsId(StringUtils.randomAlphanumeric(20));
                deviceCredentialsService.createDeviceCredentials(device.getTenantId(), deviceCredentials);
            }
            createRelationFromEdge(tenantId, edge.getId(), device.getId());
            pushDeviceCreatedEventToRuleEngine(tenantId, edge, device);
            deviceService.assignDeviceToEdge(edge.getTenantId(), device.getId(), edge.getId());
        } finally {
            deviceCreationLock.unlock();
        }
        return device;
    }

    private CustomerId getCustomerId(DeviceUpdateMsg deviceUpdateMsg) {
        if (deviceUpdateMsg.hasCustomerIdMSB() && deviceUpdateMsg.hasCustomerIdLSB()) {
            return new CustomerId(new UUID(deviceUpdateMsg.getCustomerIdMSB(), deviceUpdateMsg.getCustomerIdLSB()));
        } else {
            return null;
        }
    }

    private void createRelationFromEdge(TenantId tenantId, EdgeId edgeId, EntityId entityId) {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(edgeId);
        relation.setTo(entityId);
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        relation.setType(EntityRelation.EDGE_TYPE);
        relationService.saveRelation(tenantId, relation);
    }

    private void pushDeviceCreatedEventToRuleEngine(TenantId tenantId, Edge edge, Device device) {
        try {
            DeviceId deviceId = device.getId();
            ObjectNode entityNode = JacksonUtil.OBJECT_MAPPER.valueToTree(device);
            TbMsg tbMsg = TbMsg.newMsg(DataConstants.ENTITY_CREATED, deviceId, device.getCustomerId(),
                    getActionTbMsgMetaData(edge, device.getCustomerId()), TbMsgDataType.JSON, JacksonUtil.OBJECT_MAPPER.writeValueAsString(entityNode));
            tbClusterService.pushMsgToRuleEngine(tenantId, deviceId, tbMsg, new TbQueueCallback() {
                @Override
                public void onSuccess(TbQueueMsgMetadata metadata) {
                    log.debug("Successfully send ENTITY_CREATED EVENT to rule engine [{}]", device);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.debug("Failed to send ENTITY_CREATED EVENT to rule engine [{}]", device, t);
                }
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
                log.error("Can't process push notification to core [{}]", deviceRpcCallMsg, t);
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
        UUID requestUUID = new UUID(deviceRpcCallMsg.getRequestUuidMSB(), deviceRpcCallMsg.getRequestUuidLSB());
        try {
            ObjectNode entityNode = JacksonUtil.OBJECT_MAPPER.createObjectNode();
            TbMsgMetaData metaData = new TbMsgMetaData();
            String requestId = Integer.toString(deviceRpcCallMsg.getRequestId());
            metaData.putValue("requestId", requestId);
            metaData.putValue("requestUUID", requestUUID.toString());
            // ?? metaData.putValue("originServiceId", deviceRpcRequestMsg.get);
            metaData.putValue("expirationTime", Long.toString(deviceRpcCallMsg.getExpirationTime()));
            metaData.putValue("oneway", Boolean.toString(deviceRpcCallMsg.getOneway()));
            metaData.putValue(DataConstants.PERSISTENT, Boolean.toString(deviceRpcCallMsg.getPersisted()));

            if (deviceRpcCallMsg.getRetries() > 0) {
                metaData.putValue(DataConstants.RETRIES, Integer.toString(deviceRpcCallMsg.getRetries()));
            }

            metaData.putValue(DataConstants.EDGE_ID, edge.getId().toString());

            Device device = deviceService.findDeviceById(tenantId, deviceId);
            if (device != null) {
                metaData.putValue("deviceName", device.getName());
                metaData.putValue("deviceType", device.getType());
                metaData.putValue(DataConstants.DEVICE_ID, deviceId.getId().toString());
            }

            entityNode.put("method", deviceRpcCallMsg.getRequestMsg().getMethod());
            entityNode.put("params", deviceRpcCallMsg.getRequestMsg().getParams());

            entityNode.put(DataConstants.ADDITIONAL_INFO, deviceRpcCallMsg.getAdditionalInfo());
            TbMsg tbMsg = TbMsg.newMsg(SessionMsgType.TO_SERVER_RPC_REQUEST.name(), deviceId, null, metaData,
                    TbMsgDataType.JSON, JacksonUtil.OBJECT_MAPPER.writeValueAsString(entityNode));
            tbClusterService.pushMsgToRuleEngine(tenantId, deviceId, tbMsg, new TbQueueCallback() {
                @Override
                public void onSuccess(TbQueueMsgMetadata metadata) {
                    log.debug("Successfully send ENTITY_CREATED EVENT to rule engine [{}]", device);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.debug("Failed to send ENTITY_CREATED EVENT to rule engine [{}]", device, t);
                }
            });
            toServerRpcPendingMap.put(requestId, new EdgeRpcRequestMetadata(tenantId, edge.getId(), deviceId));
            scheduler.schedule(() -> processTimeout(requestId), 60000, TimeUnit.MILLISECONDS);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.warn("[{}] Failed to push device action to rule engine: {}", deviceId, DataConstants.ENTITY_CREATED, e);
        }

        return Futures.immediateFuture(null);
    }

    private void processTimeout(String requestId) {
        EdgeRpcRequestMetadata data = toServerRpcPendingMap.remove(requestId);
        if (data != null) {
            // TODO: add failure body
            saveEdgeEvent(data.getTenantId(), data.getEdgeId(), EdgeEventType.DEVICE, EdgeEventActionType.RPC_CALL_RESPONSE,
                    data.getDeviceId(), JacksonUtil.OBJECT_MAPPER.valueToTree("{}"));
        }
    }

    public DownlinkMsg convertDeviceEventToDownlink(EdgeEvent edgeEvent) {
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
                            deviceMsgConstructor.constructDeviceUpdatedMsg(msgType, device, null);
                    DownlinkMsg.Builder builder = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addDeviceUpdateMsg(deviceUpdateMsg);
                    if (UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE.equals(msgType)) {
                        DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileById(edgeEvent.getTenantId(), device.getDeviceProfileId());
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
            case RPC_CALL_REQUEST:
                return convertRpcCallRequestEventToDownlink(edgeEvent);
            case RPC_CALL_RESPONSE:
                return convertRpcCallResponseEventToDownlink(edgeEvent);
            case CREDENTIALS_REQUEST:
                return convertCredentialsRequestEventToDownlink(edgeEvent);
            case ENTITY_MERGE_REQUEST:
                return convertEntityMergeRequestEventToDownlink(edgeEvent);
        }
        return downlinkMsg;
    }

    private DownlinkMsg convertRpcCallRequestEventToDownlink(EdgeEvent edgeEvent) {
        log.trace("Executing convertRpcCallRequestEventToDownlink, edgeEvent [{}]", edgeEvent);
        return DownlinkMsg.newBuilder()
                .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                .addDeviceRpcCallMsg(deviceMsgConstructor.constructDeviceRpcRequestMsg(edgeEvent.getEntityId(), edgeEvent.getBody()))
                .build();
    }

    private DownlinkMsg convertRpcCallResponseEventToDownlink(EdgeEvent edgeEvent) {
        log.trace("Executing convertRpcCallResponseEventToDownlink, edgeEvent [{}]", edgeEvent);
        DeviceRpcCallMsg deviceRpcCallMsg = deviceMsgConstructor.constructDeviceRpcResponseMsg(edgeEvent.getEntityId(), edgeEvent.getBody());
        toServerRpcPendingMap.remove(Integer.toString(deviceRpcCallMsg.getRequestId()));
        return DownlinkMsg.newBuilder()
                .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                .addDeviceRpcCallMsg(deviceRpcCallMsg)
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

    public DownlinkMsg convertEntityMergeRequestEventToDownlink(EdgeEvent edgeEvent) {
        DeviceId deviceId = new DeviceId(edgeEvent.getEntityId());
        Device device = deviceService.findDeviceById(edgeEvent.getTenantId(), deviceId);
        String conflictName = null;
        if(edgeEvent.getBody() != null) {
            conflictName = edgeEvent.getBody().get("conflictName").asText();
        }
        DeviceUpdateMsg deviceUpdateMsg = deviceMsgConstructor
                .constructDeviceUpdatedMsg(UpdateMsgType.ENTITY_MERGE_RPC_MESSAGE, device, conflictName);
        return DownlinkMsg.newBuilder()
                .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                .addDeviceUpdateMsg(deviceUpdateMsg)
                .build();
    }

    public ListenableFuture<Void> processDeviceNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        return processEntityNotification(tenantId, edgeNotificationMsg);
    }
}
