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
package org.thingsboard.server.service.transport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.device.credentials.BasicMqttCredentials;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.transport.util.DataDecodingEncodingService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.tenant.TenantProfileService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.util.mapping.JacksonUtil;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.DeviceInfoProto;
import org.thingsboard.server.gen.transport.TransportProtos.GetOrCreateDeviceFromGatewayRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetOrCreateDeviceFromGatewayResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetTenantRoutingInfoRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetTenantRoutingInfoResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceCredentialsResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceTokenRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceX509CertRequestMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
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

    //TODO: Constructor dependencies;
    private final DeviceProfileService deviceProfileService;
    private final TenantService tenantService;
    private final TenantProfileService tenantProfileService;
    private final DeviceService deviceService;
    private final RelationService relationService;
    private final DeviceCredentialsService deviceCredentialsService;
    private final DeviceStateService deviceStateService;
    private final DbCallbackExecutorService dbCallbackExecutorService;
    private final TbClusterService tbClusterService;
    private final DataDecodingEncodingService dataDecodingEncodingService;


    private final ConcurrentMap<String, ReentrantLock> deviceCreationLocks = new ConcurrentHashMap<>();

    public DefaultTransportApiService(DeviceProfileService deviceProfileService, TenantService tenantService,
                                      TenantProfileService tenantProfileService, DeviceService deviceService,
                                      RelationService relationService, DeviceCredentialsService deviceCredentialsService,
                                      DeviceStateService deviceStateService, DbCallbackExecutorService dbCallbackExecutorService,
                                      TbClusterService tbClusterService, DataDecodingEncodingService dataDecodingEncodingService) {
        this.deviceProfileService = deviceProfileService;
        this.tenantService = tenantService;
        this.tenantProfileService = tenantProfileService;
        this.deviceService = deviceService;
        this.relationService = relationService;
        this.deviceCredentialsService = deviceCredentialsService;
        this.deviceStateService = deviceStateService;
        this.dbCallbackExecutorService = dbCallbackExecutorService;
        this.tbClusterService = tbClusterService;
        this.dataDecodingEncodingService = dataDecodingEncodingService;
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
        } else if (transportApiRequestMsg.hasGetTenantRoutingInfoRequestMsg()) {
            return Futures.transform(handle(transportApiRequestMsg.getGetTenantRoutingInfoRequestMsg()),
                    value -> new TbProtoQueueMsg<>(tbProtoQueueMsg.getKey(), value, tbProtoQueueMsg.getHeaders()), MoreExecutors.directExecutor());
        } else if (transportApiRequestMsg.hasGetDeviceProfileRequestMsg()) {
            return Futures.transform(handle(transportApiRequestMsg.getGetDeviceProfileRequestMsg()),
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
        DeviceCredentials credentials = deviceCredentialsService.findDeviceCredentialsByCredentialsId(mqtt.getUserName());
        if (credentials != null) {
            if (credentials.getCredentialsType() == DeviceCredentialsType.ACCESS_TOKEN) {
                return getDeviceInfo(credentials.getDeviceId(), credentials);
            } else if (credentials.getCredentialsType() == DeviceCredentialsType.MQTT_BASIC) {
                if (!checkMqttCredentials(mqtt, credentials)) {
                    credentials = null;
                }
            }
        }
        if (credentials == null) {
            credentials = checkMqttCredentials(mqtt, EncryptionUtil.getSha3Hash("|", mqtt.getClientId(), mqtt.getUserName()));
            if (credentials == null) {
                credentials = checkMqttCredentials(mqtt, EncryptionUtil.getSha3Hash(mqtt.getClientId()));
            }
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
                    DeviceProfile deviceProfile = deviceProfileService.findOrCreateDeviceProfile(gateway.getTenantId(), requestMsg.getDeviceType());
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
                DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileById(device.getTenantId(), device.getDeviceProfileId());
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

    private ListenableFuture<TransportApiResponseMsg> handle(GetTenantRoutingInfoRequestMsg requestMsg) {
        TenantId tenantId = new TenantId(new UUID(requestMsg.getTenantIdMSB(), requestMsg.getTenantIdLSB()));
        // TODO: Tenant Profile from cache
        ListenableFuture<TenantProfile> tenantProfileFuture =
                Futures.transform(tenantService.findTenantByIdAsync(TenantId.SYS_TENANT_ID, tenantId), tenant ->
                        tenantProfileService.findTenantProfileById(TenantId.SYS_TENANT_ID, tenant.getTenantProfileId()), dbCallbackExecutorService);
        return Futures.transform(tenantProfileFuture, tenantProfile -> TransportApiResponseMsg.newBuilder()
                .setGetTenantRoutingInfoResponseMsg(GetTenantRoutingInfoResponseMsg.newBuilder().setIsolatedTbCore(tenantProfile.isIsolatedTbCore())
                        .setIsolatedTbRuleEngine(tenantProfile.isIsolatedTbRuleEngine()).build()).build(), dbCallbackExecutorService);
    }

    private ListenableFuture<TransportApiResponseMsg> handle(TransportProtos.GetDeviceProfileRequestMsg requestMsg) {
        DeviceProfileId profileId = new DeviceProfileId(new UUID(requestMsg.getProfileIdMSB(), requestMsg.getProfileIdLSB()));
        DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileById(TenantId.SYS_TENANT_ID, profileId);
        return Futures.immediateFuture(TransportApiResponseMsg.newBuilder()
                .setGetDeviceProfileResponseMsg(
                        TransportProtos.GetDeviceProfileResponseMsg.newBuilder()
                                .setData(ByteString.copyFrom(dataDecodingEncodingService.encode(deviceProfile)))
                                .build()).build());
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
                DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileById(device.getTenantId(), device.getDeviceProfileId());
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
}
