/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.service.transport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.device.credentials.BasicMqttCredentials;
import org.thingsboard.server.common.data.device.credentials.ProvisionDeviceCredentialsData;
import org.thingsboard.server.common.data.device.profile.ProvisionDeviceProfileCredentials;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.data.Resource;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.transport.util.DataDecodingEncodingService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProvisionService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.device.provision.ProvisionFailedException;
import org.thingsboard.server.dao.device.provision.ProvisionRequest;
import org.thingsboard.server.dao.device.provision.ProvisionResponse;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.DeviceInfoProto;
import org.thingsboard.server.gen.transport.TransportProtos.GetEntityProfileRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetEntityProfileResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetOrCreateDeviceFromGatewayRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetOrCreateDeviceFromGatewayResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetResourceRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ProvisionDeviceRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ProvisionResponseStatus;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceCredentialsResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceLwM2MCredentialsRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceTokenRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceX509CertRequestMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;
import org.thingsboard.server.service.queue.TbClusterService;
import org.thingsboard.server.service.state.DeviceStateService;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by ashvayka on 05.10.18.
 */
@Slf4j
@Service
@TbCoreComponent
public class DefaultTransportApiService implements TransportApiService {

    private static final ObjectMapper mapper = new ObjectMapper();

    private final TbDeviceProfileCache deviceProfileCache;
    private final TbTenantProfileCache tenantProfileCache;
    private final TbApiUsageStateService apiUsageStateService;
    private final DeviceService deviceService;
    private final RelationService relationService;
    private final DeviceCredentialsService deviceCredentialsService;
    private final DeviceStateService deviceStateService;
    private final DbCallbackExecutorService dbCallbackExecutorService;
    private final TbClusterService tbClusterService;
    private final DataDecodingEncodingService dataDecodingEncodingService;
    private final DeviceProvisionService deviceProvisionService;
    private final ResourceService resourceService;

    private final ConcurrentMap<String, ReentrantLock> deviceCreationLocks = new ConcurrentHashMap<>();

    public DefaultTransportApiService(TbDeviceProfileCache deviceProfileCache,
                                      TbTenantProfileCache tenantProfileCache, TbApiUsageStateService apiUsageStateService, DeviceService deviceService,
                                      RelationService relationService, DeviceCredentialsService deviceCredentialsService,
                                      DeviceStateService deviceStateService, DbCallbackExecutorService dbCallbackExecutorService,
                                      TbClusterService tbClusterService, DataDecodingEncodingService dataDecodingEncodingService,
                                      DeviceProvisionService deviceProvisionService, ResourceService resourceService) {
        this.deviceProfileCache = deviceProfileCache;
        this.tenantProfileCache = tenantProfileCache;
        this.apiUsageStateService = apiUsageStateService;
        this.deviceService = deviceService;
        this.relationService = relationService;
        this.deviceCredentialsService = deviceCredentialsService;
        this.deviceStateService = deviceStateService;
        this.dbCallbackExecutorService = dbCallbackExecutorService;
        this.tbClusterService = tbClusterService;
        this.dataDecodingEncodingService = dataDecodingEncodingService;
        this.deviceProvisionService = deviceProvisionService;
        this.resourceService = resourceService;
    }

    @Override
    public ListenableFuture<TbProtoQueueMsg<TransportApiResponseMsg>> handle(TbProtoQueueMsg<TransportApiRequestMsg> tbProtoQueueMsg) {
        TransportApiRequestMsg transportApiRequestMsg = tbProtoQueueMsg.getValue();
        if (transportApiRequestMsg.hasValidateTokenRequestMsg()) {
            ValidateDeviceTokenRequestMsg msg = transportApiRequestMsg.getValidateTokenRequestMsg();
            return Futures.transform(validateCredentials(msg.getToken(), DeviceCredentialsType.ACCESS_TOKEN),
                    value -> new TbProtoQueueMsg<>(tbProtoQueueMsg.getKey(), value, tbProtoQueueMsg.getHeaders()), MoreExecutors.directExecutor());
        } else if (transportApiRequestMsg.hasValidateBasicMqttCredRequestMsg()) {
            TransportProtos.ValidateBasicMqttCredRequestMsg msg = transportApiRequestMsg.getValidateBasicMqttCredRequestMsg();
            return Futures.transform(validateCredentials(msg),
                    value -> new TbProtoQueueMsg<>(tbProtoQueueMsg.getKey(), value, tbProtoQueueMsg.getHeaders()), MoreExecutors.directExecutor());
        } else if (transportApiRequestMsg.hasValidateX509CertRequestMsg()) {
            ValidateDeviceX509CertRequestMsg msg = transportApiRequestMsg.getValidateX509CertRequestMsg();
            return Futures.transform(validateCredentials(msg.getHash(), DeviceCredentialsType.X509_CERTIFICATE),
                    value -> new TbProtoQueueMsg<>(tbProtoQueueMsg.getKey(), value, tbProtoQueueMsg.getHeaders()), MoreExecutors.directExecutor());
        } else if (transportApiRequestMsg.hasGetOrCreateDeviceRequestMsg()) {
            return Futures.transform(handle(transportApiRequestMsg.getGetOrCreateDeviceRequestMsg()),
                    value -> new TbProtoQueueMsg<>(tbProtoQueueMsg.getKey(), value, tbProtoQueueMsg.getHeaders()), MoreExecutors.directExecutor());
        } else if (transportApiRequestMsg.hasEntityProfileRequestMsg()) {
            return Futures.transform(handle(transportApiRequestMsg.getEntityProfileRequestMsg()),
                    value -> new TbProtoQueueMsg<>(tbProtoQueueMsg.getKey(), value, tbProtoQueueMsg.getHeaders()), MoreExecutors.directExecutor());
        } else if (transportApiRequestMsg.hasLwM2MRequestMsg()) {
            return Futures.transform(handle(transportApiRequestMsg.getLwM2MRequestMsg()),
                    value -> new TbProtoQueueMsg<>(tbProtoQueueMsg.getKey(), value, tbProtoQueueMsg.getHeaders()), MoreExecutors.directExecutor());
        } else if (transportApiRequestMsg.hasValidateDeviceLwM2MCredentialsRequestMsg()) {
            ValidateDeviceLwM2MCredentialsRequestMsg msg = transportApiRequestMsg.getValidateDeviceLwM2MCredentialsRequestMsg();
            return Futures.transform(validateCredentials(msg.getCredentialsId(), DeviceCredentialsType.LWM2M_CREDENTIALS),
                    value -> new TbProtoQueueMsg<>(tbProtoQueueMsg.getKey(), value, tbProtoQueueMsg.getHeaders()), MoreExecutors.directExecutor());
        } else if (transportApiRequestMsg.hasProvisionDeviceRequestMsg()) {
            return Futures.transform(handle(transportApiRequestMsg.getProvisionDeviceRequestMsg()),
                    value -> new TbProtoQueueMsg<>(tbProtoQueueMsg.getKey(), value, tbProtoQueueMsg.getHeaders()), MoreExecutors.directExecutor());
        } else if (transportApiRequestMsg.hasResourceRequestMsg()) {
            return Futures.transform(handle(transportApiRequestMsg.getResourceRequestMsg()),
                    value -> new TbProtoQueueMsg<>(tbProtoQueueMsg.getKey(), value, tbProtoQueueMsg.getHeaders()), MoreExecutors.directExecutor());
        }
        return Futures.transform(getEmptyTransportApiResponseFuture(),
                value -> new TbProtoQueueMsg<>(tbProtoQueueMsg.getKey(), value, tbProtoQueueMsg.getHeaders()), MoreExecutors.directExecutor());
    }

    private ListenableFuture<TransportApiResponseMsg> validateCredentials(String credentialsId, DeviceCredentialsType credentialsType) {
        //TODO: Make async and enable caching
        DeviceCredentials credentials = deviceCredentialsService.findDeviceCredentialsByCredentialsId(credentialsId);
        if (credentials != null && credentials.getCredentialsType() == credentialsType) {
            return getDeviceInfo(credentials.getDeviceId(), credentials);
        } else {
            return getEmptyTransportApiResponseFuture();
        }
    }

    private ListenableFuture<TransportApiResponseMsg> validateCredentials(TransportProtos.ValidateBasicMqttCredRequestMsg mqtt) {
        DeviceCredentials credentials = null;
        if (!StringUtils.isEmpty(mqtt.getUserName())) {
            credentials = deviceCredentialsService.findDeviceCredentialsByCredentialsId(mqtt.getUserName());
            if (credentials != null) {
                if (credentials.getCredentialsType() == DeviceCredentialsType.ACCESS_TOKEN) {
                    return getDeviceInfo(credentials.getDeviceId(), credentials);
                } else if (credentials.getCredentialsType() == DeviceCredentialsType.MQTT_BASIC) {
                    if (!checkMqttCredentials(mqtt, credentials)) {
                        credentials = null;
                    }
                } else {
                    return getEmptyTransportApiResponseFuture();
                }
            }
            if (credentials == null) {
                credentials = checkMqttCredentials(mqtt, EncryptionUtil.getSha3Hash("|", mqtt.getClientId(), mqtt.getUserName()));
            }
        }
        if (credentials == null) {
            credentials = checkMqttCredentials(mqtt, EncryptionUtil.getSha3Hash(mqtt.getClientId()));
        }
        if (credentials != null) {
            return getDeviceInfo(credentials.getDeviceId(), credentials);
        } else {
            return getEmptyTransportApiResponseFuture();
        }
    }

    private DeviceCredentials checkMqttCredentials(TransportProtos.ValidateBasicMqttCredRequestMsg clientCred, String credId) {
        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByCredentialsId(credId);
        if (deviceCredentials != null && deviceCredentials.getCredentialsType() == DeviceCredentialsType.MQTT_BASIC) {
            if (!checkMqttCredentials(clientCred, deviceCredentials)) {
                return null;
            } else {
                return deviceCredentials;
            }
        }
        return null;
    }

    private boolean checkMqttCredentials(TransportProtos.ValidateBasicMqttCredRequestMsg clientCred, DeviceCredentials deviceCredentials) {
        BasicMqttCredentials dbCred = JacksonUtil.fromString(deviceCredentials.getCredentialsValue(), BasicMqttCredentials.class);
        if (!StringUtils.isEmpty(dbCred.getClientId()) && !dbCred.getClientId().equals(clientCred.getClientId())) {
            return false;
        }
        if (!StringUtils.isEmpty(dbCred.getUserName()) && !dbCred.getUserName().equals(clientCred.getUserName())) {
            return false;
        }
        if (!StringUtils.isEmpty(dbCred.getPassword())) {
            if (StringUtils.isEmpty(clientCred.getPassword())) {
                return false;
            } else {
                if (!dbCred.getPassword().equals(clientCred.getPassword())) {
                    return false;
                }
            }
        }
        return true;
    }

    private ListenableFuture<TransportApiResponseMsg> handle(GetOrCreateDeviceFromGatewayRequestMsg requestMsg) {
        DeviceId gatewayId = new DeviceId(new UUID(requestMsg.getGatewayIdMSB(), requestMsg.getGatewayIdLSB()));
        ListenableFuture<Device> gatewayFuture = deviceService.findDeviceByIdAsync(TenantId.SYS_TENANT_ID, gatewayId);
        return Futures.transform(gatewayFuture, gateway -> {
            Lock deviceCreationLock = deviceCreationLocks.computeIfAbsent(requestMsg.getDeviceName(), id -> new ReentrantLock());
            deviceCreationLock.lock();
            try {
                Device device = deviceService.findDeviceByTenantIdAndName(gateway.getTenantId(), requestMsg.getDeviceName());
                if (device == null) {
                    TenantId tenantId = gateway.getTenantId();
                    device = new Device();
                    device.setTenantId(tenantId);
                    device.setName(requestMsg.getDeviceName());
                    device.setType(requestMsg.getDeviceType());
                    device.setCustomerId(gateway.getCustomerId());
                    DeviceProfile deviceProfile = deviceProfileCache.findOrCreateDeviceProfile(gateway.getTenantId(), requestMsg.getDeviceType());
                    device.setDeviceProfileId(deviceProfile.getId());
                    device = deviceService.saveDevice(device);
                    relationService.saveRelationAsync(TenantId.SYS_TENANT_ID, new EntityRelation(gateway.getId(), device.getId(), "Created"));
                    deviceStateService.onDeviceAdded(device);

                    TbMsgMetaData metaData = new TbMsgMetaData();
                    CustomerId customerId = gateway.getCustomerId();
                    if (customerId != null && !customerId.isNullUid()) {
                        metaData.putValue("customerId", customerId.toString());
                    }
                    metaData.putValue("gatewayId", gatewayId.toString());

                    DeviceId deviceId = device.getId();
                    ObjectNode entityNode = mapper.valueToTree(device);
                    TbMsg tbMsg = TbMsg.newMsg(DataConstants.ENTITY_CREATED, deviceId, metaData, TbMsgDataType.JSON, mapper.writeValueAsString(entityNode));
                    tbClusterService.pushMsgToRuleEngine(tenantId, deviceId, tbMsg, null);
                }
                GetOrCreateDeviceFromGatewayResponseMsg.Builder builder = GetOrCreateDeviceFromGatewayResponseMsg.newBuilder()
                        .setDeviceInfo(getDeviceInfoProto(device));
                DeviceProfile deviceProfile = deviceProfileCache.get(device.getTenantId(), device.getDeviceProfileId());
                if (deviceProfile != null) {
                    builder.setProfileBody(ByteString.copyFrom(dataDecodingEncodingService.encode(deviceProfile)));
                } else {
                    log.warn("[{}] Failed to find device profile [{}] for device. ", device.getId(), device.getDeviceProfileId());
                }
                return TransportApiResponseMsg.newBuilder()
                        .setGetOrCreateDeviceResponseMsg(builder.build())
                        .build();
            } catch (JsonProcessingException e) {
                log.warn("[{}] Failed to lookup device by gateway id and name: [{}]", gatewayId, requestMsg.getDeviceName(), e);
                throw new RuntimeException(e);
            } finally {
                deviceCreationLock.unlock();
            }
        }, dbCallbackExecutorService);
    }

    private ListenableFuture<TransportApiResponseMsg> handle(ProvisionDeviceRequestMsg requestMsg) {
        ListenableFuture<ProvisionResponse> provisionResponseFuture = null;
        try {
            provisionResponseFuture = Futures.immediateFuture(deviceProvisionService.provisionDevice(
                    new ProvisionRequest(
                            requestMsg.getDeviceName(),
                            requestMsg.getCredentialsType() != null ? DeviceCredentialsType.valueOf(requestMsg.getCredentialsType().name()) : null,
                            new ProvisionDeviceCredentialsData(requestMsg.getCredentialsDataProto().getValidateDeviceTokenRequestMsg().getToken(),
                                    requestMsg.getCredentialsDataProto().getValidateBasicMqttCredRequestMsg().getClientId(),
                                    requestMsg.getCredentialsDataProto().getValidateBasicMqttCredRequestMsg().getUserName(),
                                    requestMsg.getCredentialsDataProto().getValidateBasicMqttCredRequestMsg().getPassword(),
                                    requestMsg.getCredentialsDataProto().getValidateDeviceX509CertRequestMsg().getHash()),
                            new ProvisionDeviceProfileCredentials(
                                    requestMsg.getProvisionDeviceCredentialsMsg().getProvisionDeviceKey(),
                                    requestMsg.getProvisionDeviceCredentialsMsg().getProvisionDeviceSecret()))));
        } catch (ProvisionFailedException e) {
            return Futures.immediateFuture(getTransportApiResponseMsg(
                    new DeviceCredentials(),
                    TransportProtos.ProvisionResponseStatus.valueOf(e.getMessage())));
        }
        return Futures.transform(provisionResponseFuture, provisionResponse -> getTransportApiResponseMsg(provisionResponse.getDeviceCredentials(), TransportProtos.ProvisionResponseStatus.SUCCESS),
                dbCallbackExecutorService);
    }

    private TransportApiResponseMsg getTransportApiResponseMsg(DeviceCredentials deviceCredentials, TransportProtos.ProvisionResponseStatus status) {
        if (!status.equals(ProvisionResponseStatus.SUCCESS)) {
            return TransportApiResponseMsg.newBuilder().setProvisionDeviceResponseMsg(TransportProtos.ProvisionDeviceResponseMsg.newBuilder().setStatus(status).build()).build();
        }
        TransportProtos.ProvisionDeviceResponseMsg.Builder provisionResponse = TransportProtos.ProvisionDeviceResponseMsg.newBuilder()
                .setCredentialsType(TransportProtos.CredentialsType.valueOf(deviceCredentials.getCredentialsType().name()))
                .setStatus(status);
        switch (deviceCredentials.getCredentialsType()) {
            case ACCESS_TOKEN:
                provisionResponse.setCredentialsValue(deviceCredentials.getCredentialsId());
                break;
            case MQTT_BASIC:
            case X509_CERTIFICATE:
            case LWM2M_CREDENTIALS:
                provisionResponse.setCredentialsValue(deviceCredentials.getCredentialsValue());
                break;
        }

        return TransportApiResponseMsg.newBuilder()
                .setProvisionDeviceResponseMsg(provisionResponse.build())
                .build();
    }

    private ListenableFuture<TransportApiResponseMsg> handle(GetEntityProfileRequestMsg requestMsg) {
        EntityType entityType = EntityType.valueOf(requestMsg.getEntityType());
        UUID entityUuid = new UUID(requestMsg.getEntityIdMSB(), requestMsg.getEntityIdLSB());
        GetEntityProfileResponseMsg.Builder builder = GetEntityProfileResponseMsg.newBuilder();
        if (entityType.equals(EntityType.DEVICE_PROFILE)) {
            DeviceProfileId deviceProfileId = new DeviceProfileId(entityUuid);
            DeviceProfile deviceProfile = deviceProfileCache.find(deviceProfileId);
            builder.setData(ByteString.copyFrom(dataDecodingEncodingService.encode(deviceProfile)));
        } else if (entityType.equals(EntityType.TENANT)) {
            TenantId tenantId = new TenantId(entityUuid);
            TenantProfile tenantProfile = tenantProfileCache.get(tenantId);
            ApiUsageState state = apiUsageStateService.getApiUsageState(tenantId);
            builder.setData(ByteString.copyFrom(dataDecodingEncodingService.encode(tenantProfile)));
            builder.setApiState(ByteString.copyFrom(dataDecodingEncodingService.encode(state)));
        } else {
            throw new RuntimeException("Invalid entity profile request: " + entityType);
        }
        return Futures.immediateFuture(TransportApiResponseMsg.newBuilder().setEntityProfileResponseMsg(builder).build());
    }

    private ListenableFuture<TransportApiResponseMsg> handle(GetResourceRequestMsg requestMsg) {
        TenantId tenantId = new TenantId(new UUID(requestMsg.getTenantIdMSB(), requestMsg.getTenantIdLSB()));
        ResourceType resourceType = ResourceType.valueOf(requestMsg.getResourceType());
        String resourceId = requestMsg.getResourceId();
        TransportProtos.GetResourceResponseMsg.Builder builder = TransportProtos.GetResourceResponseMsg.newBuilder();
        Resource resource = resourceService.getResource(tenantId, resourceType, resourceId);
        if (resource != null) {
            builder.setResource(ByteString.copyFrom(dataDecodingEncodingService.encode(resource)));
        }

        return Futures.immediateFuture(TransportApiResponseMsg.newBuilder().setResourceResponseMsg(builder).build());
    }

    private ListenableFuture<TransportApiResponseMsg> getDeviceInfo(DeviceId deviceId, DeviceCredentials credentials) {
        return Futures.transform(deviceService.findDeviceByIdAsync(TenantId.SYS_TENANT_ID, deviceId), device -> {
            if (device == null) {
                log.trace("[{}] Failed to lookup device by id", deviceId);
                return getEmptyTransportApiResponse();
            }
            try {
                ValidateDeviceCredentialsResponseMsg.Builder builder = ValidateDeviceCredentialsResponseMsg.newBuilder();
                builder.setDeviceInfo(getDeviceInfoProto(device));
                DeviceProfile deviceProfile = deviceProfileCache.get(device.getTenantId(), device.getDeviceProfileId());
                if (deviceProfile != null) {
                    builder.setProfileBody(ByteString.copyFrom(dataDecodingEncodingService.encode(deviceProfile)));
                } else {
                    log.warn("[{}] Failed to find device profile [{}] for device. ", device.getId(), device.getDeviceProfileId());
                }
                if (!StringUtils.isEmpty(credentials.getCredentialsValue())) {
                    builder.setCredentialsBody(credentials.getCredentialsValue());
                }
                return TransportApiResponseMsg.newBuilder()
                        .setValidateCredResponseMsg(builder.build()).build();
            } catch (JsonProcessingException e) {
                log.warn("[{}] Failed to lookup device by id", deviceId, e);
                return getEmptyTransportApiResponse();
            }
        }, MoreExecutors.directExecutor());
    }

    private DeviceInfoProto getDeviceInfoProto(Device device) throws JsonProcessingException {
        return DeviceInfoProto.newBuilder()
                .setTenantIdMSB(device.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(device.getTenantId().getId().getLeastSignificantBits())
                .setDeviceIdMSB(device.getId().getId().getMostSignificantBits())
                .setDeviceIdLSB(device.getId().getId().getLeastSignificantBits())
                .setDeviceName(device.getName())
                .setDeviceType(device.getType())
                .setDeviceProfileIdMSB(device.getDeviceProfileId().getId().getMostSignificantBits())
                .setDeviceProfileIdLSB(device.getDeviceProfileId().getId().getLeastSignificantBits())
                .setAdditionalInfo(mapper.writeValueAsString(device.getAdditionalInfo()))
                .build();
    }

    private ListenableFuture<TransportApiResponseMsg> getEmptyTransportApiResponseFuture() {
        return Futures.immediateFuture(getEmptyTransportApiResponse());
    }

    private TransportApiResponseMsg getEmptyTransportApiResponse() {
        return TransportApiResponseMsg.newBuilder()
                .setValidateCredResponseMsg(ValidateDeviceCredentialsResponseMsg.getDefaultInstance()).build();
    }

    private ListenableFuture<TransportApiResponseMsg> handle(TransportProtos.LwM2MRequestMsg requestMsg) {
        if (requestMsg.hasRegistrationMsg()) {
            return handleRegistration(requestMsg.getRegistrationMsg());
        } else {
            return Futures.immediateFailedFuture(new RuntimeException("Not supported!"));
        }
    }

    private ListenableFuture<TransportApiResponseMsg> handleRegistration(TransportProtos.LwM2MRegistrationRequestMsg msg) {
        TenantId tenantId = new TenantId(UUID.fromString(msg.getTenantId()));
        String deviceName = msg.getEndpoint();
        Lock deviceCreationLock = deviceCreationLocks.computeIfAbsent(deviceName, id -> new ReentrantLock());
        deviceCreationLock.lock();
        try {
            Device device = deviceService.findDeviceByTenantIdAndName(tenantId, deviceName);
            if (device == null) {
                device = new Device();
                device.setTenantId(tenantId);
                device.setName(deviceName);
                device.setType("LwM2M");
                device = deviceService.saveDevice(device);
                deviceStateService.onDeviceAdded(device);
            }
            TransportProtos.LwM2MRegistrationResponseMsg registrationResponseMsg =
                    TransportProtos.LwM2MRegistrationResponseMsg.newBuilder()
                            .setDeviceInfo(getDeviceInfoProto(device)).build();
            TransportProtos.LwM2MResponseMsg responseMsg = TransportProtos.LwM2MResponseMsg.newBuilder().setRegistrationMsg(registrationResponseMsg).build();
            return Futures.immediateFuture(TransportApiResponseMsg.newBuilder().setLwM2MResponseMsg(responseMsg).build());
        } catch (JsonProcessingException e) {
            log.warn("[{}][{}] Failed to lookup device by gateway id and name", tenantId, deviceName, e);
            throw new RuntimeException(e);
        } finally {
            deviceCreationLock.unlock();
        }
    }
}
