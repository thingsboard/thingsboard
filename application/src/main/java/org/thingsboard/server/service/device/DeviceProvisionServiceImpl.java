/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.service.device;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.device.profile.X509CertificateChainProvisionConfiguration;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.AttributesSaveResult;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.transport.util.SslUtil;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceProvisionService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.device.provision.ProvisionFailedException;
import org.thingsboard.server.dao.device.provision.ProvisionRequest;
import org.thingsboard.server.dao.device.provision.ProvisionResponse;
import org.thingsboard.server.dao.device.provision.ProvisionResponseStatus;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
@Slf4j
@TbCoreComponent
public class DeviceProvisionServiceImpl implements DeviceProvisionService {

    protected TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> ruleEngineMsgProducer;

    private static final String DEVICE_PROVISION_STATE = "provisionState";
    private static final String PROVISIONED_STATE = "provisioned";

    private final DeviceProfileService deviceProfileService;
    private final DeviceService deviceService;
    private final DeviceCredentialsService deviceCredentialsService;
    private final AttributesService attributesService;
    private final AuditLogService auditLogService;
    private final PartitionService partitionService;

    public DeviceProvisionServiceImpl(TbQueueProducerProvider producerProvider, DeviceProfileService deviceProfileService, DeviceService deviceService, DeviceCredentialsService deviceCredentialsService, AttributesService attributesService, AuditLogService auditLogService, PartitionService partitionService) {
        ruleEngineMsgProducer = producerProvider.getRuleEngineMsgProducer();
        this.deviceProfileService = deviceProfileService;
        this.deviceService = deviceService;
        this.deviceCredentialsService = deviceCredentialsService;
        this.attributesService = attributesService;
        this.auditLogService = auditLogService;
        this.partitionService = partitionService;
    }

    @Override
    public ProvisionResponse provisionDeviceViaX509Chain(DeviceProfile targetProfile, ProvisionRequest provisionRequest) throws ProvisionFailedException {
        if (targetProfile == null) {
            throw new ProvisionFailedException("Device profile is not specified!");
        }
        if (!DeviceProfileProvisionType.X509_CERTIFICATE_CHAIN.equals(targetProfile.getProfileData().getProvisionConfiguration().getType())) {
            throw new ProvisionFailedException("Device profile provision strategy is not X509_CERTIFICATE_CHAIN!");
        }
        X509CertificateChainProvisionConfiguration configuration = (X509CertificateChainProvisionConfiguration) targetProfile.getProfileData().getProvisionConfiguration();
        String certificateValue = provisionRequest.getCredentialsData().getX509CertHash();
        String certificateRegEx = configuration.getCertificateRegExPattern();
        String commonName = getCNFromX509Certificate(targetProfile, certificateValue);
        String deviceName = extractDeviceNameFromCNByRegEx(targetProfile, commonName, certificateRegEx);
        provisionRequest.setDeviceName(deviceName);
        Device targetDevice = deviceService.findDeviceByTenantIdAndName(targetProfile.getTenantId(), provisionRequest.getDeviceName());
        X509CertificateChainProvisionConfiguration x509Configuration = (X509CertificateChainProvisionConfiguration) targetProfile.getProfileData().getProvisionConfiguration();
        if (targetDevice != null && targetDevice.getDeviceProfileId().equals(targetProfile.getId())) {
            DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(targetDevice.getTenantId(), targetDevice.getId());
            if (DeviceCredentialsType.X509_CERTIFICATE.equals(deviceCredentials.getCredentialsType())) {
                String updatedDeviceCertificateValue = provisionRequest.getCredentialsData().getX509CertHash();
                deviceCredentials = updateDeviceCredentials(targetDevice.getTenantId(), deviceCredentials,
                        updatedDeviceCertificateValue, DeviceCredentialsType.X509_CERTIFICATE);
            }
            return new ProvisionResponse(deviceCredentials, ProvisionResponseStatus.SUCCESS);
        } else if (x509Configuration.isAllowCreateNewDevicesByX509Certificate()) {
            return createDevice(provisionRequest, targetProfile);
        } else {
            log.warn("[{}][{}] Device with name {} doesn't exist and cannot be created due incorrect configuration for X509CertificateChainProvisionConfiguration",
                    targetProfile.getTenantId(), targetProfile.getId(), provisionRequest.getDeviceName());
            throw new ProvisionFailedException(ProvisionResponseStatus.FAILURE.name());
        }
    }

    @Override
    public ProvisionResponse provisionDevice(ProvisionRequest provisionRequest) {
        String provisionRequestKey = provisionRequest.getCredentials().getProvisionDeviceKey();
        String provisionRequestSecret = provisionRequest.getCredentials().getProvisionDeviceSecret();
        if (!StringUtils.isEmpty(provisionRequest.getDeviceName())) {
            provisionRequest.setDeviceName(provisionRequest.getDeviceName().trim());
            if (StringUtils.isEmpty(provisionRequest.getDeviceName())) {
                log.warn("Provision request contains empty device name!");
                throw new ProvisionFailedException(ProvisionResponseStatus.FAILURE.name());
            }
        }

        if (StringUtils.isEmpty(provisionRequestKey) || StringUtils.isEmpty(provisionRequestSecret)) {
            throw new ProvisionFailedException(ProvisionResponseStatus.NOT_FOUND.name());
        }

        DeviceProfile targetProfile = deviceProfileService.findDeviceProfileByProvisionDeviceKey(provisionRequestKey);

        if (targetProfile == null || targetProfile.getProfileData().getProvisionConfiguration() == null ||
                targetProfile.getProfileData().getProvisionConfiguration().getProvisionDeviceSecret() == null) {
            throw new ProvisionFailedException(ProvisionResponseStatus.NOT_FOUND.name());
        }

        Device targetDevice = deviceService.findDeviceByTenantIdAndName(targetProfile.getTenantId(), provisionRequest.getDeviceName());

        switch (targetProfile.getProvisionType()) {
            case ALLOW_CREATE_NEW_DEVICES:
                if (targetProfile.getProfileData().getProvisionConfiguration().getProvisionDeviceSecret().equals(provisionRequestSecret)) {
                    if (targetDevice != null) {
                        log.warn("[{}] The device is present and could not be provisioned once more!", targetDevice.getName());
                        notify(targetDevice, provisionRequest, TbMsgType.PROVISION_FAILURE, false);
                        throw new ProvisionFailedException(ProvisionResponseStatus.FAILURE.name());
                    } else {
                        return createDevice(provisionRequest, targetProfile);
                    }
                }
                break;
            case CHECK_PRE_PROVISIONED_DEVICES:
                if (targetProfile.getProfileData().getProvisionConfiguration().getProvisionDeviceSecret().equals(provisionRequestSecret)) {
                    if (targetDevice != null && targetDevice.getDeviceProfileId().equals(targetProfile.getId())) {
                        return processProvision(targetDevice, provisionRequest);
                    } else {
                        log.warn("[{}] Failed to find pre provisioned device!", provisionRequest.getDeviceName());
                        throw new ProvisionFailedException(ProvisionResponseStatus.FAILURE.name());
                    }
                }
                break;
            case X509_CERTIFICATE_CHAIN:
                throw new ProvisionFailedException("Invalid provision strategy type!");
        }
        throw new ProvisionFailedException(ProvisionResponseStatus.NOT_FOUND.name());
    }

    private ProvisionResponse processProvision(Device device, ProvisionRequest provisionRequest) {
        try {
            Optional<AttributeKvEntry> provisionState = attributesService.find(device.getTenantId(), device.getId(),
                    AttributeScope.SERVER_SCOPE, DEVICE_PROVISION_STATE).get();
            if (provisionState != null && provisionState.isPresent()) {
                if (provisionState.get().getValueAsString().equals(PROVISIONED_STATE)) {
                    notify(device, provisionRequest, TbMsgType.PROVISION_FAILURE, false);
                    throw new ProvisionFailedException(ProvisionResponseStatus.FAILURE.name());
                } else {
                    log.error("[{}][{}] Unknown provision state: {}!", device.getName(), DEVICE_PROVISION_STATE, provisionState.get().getValueAsString());
                    throw new ProvisionFailedException(ProvisionResponseStatus.FAILURE.name());
                }
            } else {
                saveProvisionStateAttribute(device).get();
                notify(device, provisionRequest, TbMsgType.PROVISION_SUCCESS, true);
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new ProvisionFailedException(ProvisionResponseStatus.FAILURE.name());
        }
        return new ProvisionResponse(deviceCredentialsService.findDeviceCredentialsByDeviceId(device.getTenantId(), device.getId()), ProvisionResponseStatus.SUCCESS);
    }

    private ProvisionResponse createDevice(ProvisionRequest provisionRequest, DeviceProfile profile) {
        return processCreateDevice(provisionRequest, profile);
    }

    private void notify(Device device, ProvisionRequest provisionRequest, TbMsgType type, boolean success) {
        pushProvisionEventToRuleEngine(provisionRequest, device, type);
        logAction(device.getTenantId(), device.getCustomerId(), device, success, provisionRequest);
    }

    private ProvisionResponse processCreateDevice(ProvisionRequest provisionRequest, DeviceProfile profile) {
        try {
            if (StringUtils.isEmpty(provisionRequest.getDeviceName())) {
                String newDeviceName = StringUtils.randomAlphanumeric(20);
                log.info("Device name not found in provision request. Generated name is: {}", newDeviceName);
                provisionRequest.setDeviceName(newDeviceName);
            }
            Device savedDevice = deviceService.saveDevice(provisionRequest, profile);
            saveProvisionStateAttribute(savedDevice).get();
            pushDeviceCreatedEventToRuleEngine(savedDevice);
            notify(savedDevice, provisionRequest, TbMsgType.PROVISION_SUCCESS, true);

            return new ProvisionResponse(getDeviceCredentials(savedDevice), ProvisionResponseStatus.SUCCESS);
        } catch (Exception e) {
            log.warn("[{}] Error during device creation from provision request: [{}]", provisionRequest.getDeviceName(), provisionRequest, e);
            Device device = deviceService.findDeviceByTenantIdAndName(profile.getTenantId(), provisionRequest.getDeviceName());
            if (device != null) {
                notify(device, provisionRequest, TbMsgType.PROVISION_FAILURE, false);
            }
            throw new ProvisionFailedException(ProvisionResponseStatus.FAILURE.name());
        }
    }

    private DeviceCredentials updateDeviceCredentials(TenantId tenantId, DeviceCredentials deviceCredentials, String certificateValue,
                                                      DeviceCredentialsType credentialsType) {
        log.trace("Updating device credentials [{}] with certificate value [{}]", deviceCredentials, certificateValue);
        deviceCredentials.setCredentialsValue(certificateValue);
        deviceCredentials.setCredentialsType(credentialsType);
        return deviceCredentialsService.updateDeviceCredentials(tenantId, deviceCredentials);
    }

    private ListenableFuture<AttributesSaveResult> saveProvisionStateAttribute(Device device) {
        return attributesService.save(
                device.getTenantId(), device.getId(), AttributeScope.SERVER_SCOPE,
                new BaseAttributeKvEntry(new StringDataEntry(DEVICE_PROVISION_STATE, PROVISIONED_STATE), System.currentTimeMillis())
        );
    }

    private DeviceCredentials getDeviceCredentials(Device device) {
        return deviceCredentialsService.findDeviceCredentialsByDeviceId(device.getTenantId(), device.getId());
    }

    private void pushProvisionEventToRuleEngine(ProvisionRequest request, Device device, TbMsgType type) {
        try {
            JsonNode entityNode = JacksonUtil.valueToTree(request);
            TbMsg msg = TbMsg.newMsg()
                    .type(type)
                    .originator(device.getId())
                    .customerId(device.getCustomerId())
                    .copyMetaData(createTbMsgMetaData(device))
                    .data(JacksonUtil.toString(entityNode))
                    .build();
            sendToRuleEngine(device.getTenantId(), msg, null);
        } catch (IllegalArgumentException e) {
            log.warn("[{}] Failed to push device action to rule engine: {}", device.getId(), type, e);
        }
    }

    private void pushDeviceCreatedEventToRuleEngine(Device device) {
        try {
            ObjectNode entityNode = JacksonUtil.OBJECT_MAPPER.valueToTree(device);
            TbMsg msg = TbMsg.newMsg()
                    .type(TbMsgType.ENTITY_CREATED)
                    .originator(device.getId())
                    .customerId(device.getCustomerId())
                    .copyMetaData(createTbMsgMetaData(device))
                    .data(JacksonUtil.OBJECT_MAPPER.writeValueAsString(entityNode))
                    .build();
            sendToRuleEngine(device.getTenantId(), msg, null);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.warn("[{}] Failed to push device action to rule engine: {}", device.getId(), TbMsgType.ENTITY_CREATED.name(), e);
        }
    }

    protected void sendToRuleEngine(TenantId tenantId, TbMsg tbMsg, TbQueueCallback callback) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_RULE_ENGINE, tenantId, tbMsg.getOriginator());
        TransportProtos.ToRuleEngineMsg msg = TransportProtos.ToRuleEngineMsg.newBuilder()
                .setTbMsgProto(TbMsg.toProto(tbMsg))
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits()).build();
        ruleEngineMsgProducer.send(tpi, new TbProtoQueueMsg<>(tbMsg.getId(), msg), callback);
    }

    private TbMsgMetaData createTbMsgMetaData(Device device) {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("tenantId", device.getTenantId().toString());
        return metaData;
    }

    private void logAction(TenantId tenantId, CustomerId customerId, Device device, boolean success, ProvisionRequest provisionRequest) {
        ActionType actionType = success ? ActionType.PROVISION_SUCCESS : ActionType.PROVISION_FAILURE;
        auditLogService.logEntityAction(tenantId, customerId, new UserId(UserId.NULL_UUID), device.getName(), device.getId(), device, actionType, null, provisionRequest);
    }

    private String getCNFromX509Certificate(DeviceProfile profile, String x509Value) {
        try {
            return SslUtil.parseCommonName(SslUtil.readCertFile(x509Value));
        } catch (Exception e) {
            log.trace("[{}][{}] Failed to parse CN from X509 certificate {}", profile.getTenantId(), profile.getId(), x509Value);
            return null;
        }
    }

    public String extractDeviceNameFromCNByRegEx(DeviceProfile profile, String commonName, String regex) throws ProvisionFailedException {
        try {
            log.trace("Extract device name from CN [{}] by regex pattern [{}]", commonName, regex);
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(commonName);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception ignored) {}
        log.trace("[{}][{}] Failed to match device name using [{}] from CN: [{}]", profile.getTenantId(), profile.getId(), regex, commonName);
        throw new ProvisionFailedException(ProvisionResponseStatus.FAILURE.name());
    }

}
