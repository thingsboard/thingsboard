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
package org.thingsboard.server.dao.service.validator;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.squareup.wire.Syntax;
import com.squareup.wire.schema.Field;
import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.internal.parser.EnumElement;
import com.squareup.wire.schema.internal.parser.FieldElement;
import com.squareup.wire.schema.internal.parser.MessageElement;
import com.squareup.wire.schema.internal.parser.OneOfElement;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import com.squareup.wire.schema.internal.parser.ProtoParser;
import com.squareup.wire.schema.internal.parser.TypeElement;
import org.eclipse.leshan.core.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode;
import org.thingsboard.server.common.data.device.profile.CoapDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.CoapDeviceTypeConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultCoapDeviceTypeConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileAlarm;
import org.thingsboard.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.ProtoTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.TransportPayloadTypeConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.AbstractLwM2MBootstrapServerCredential;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.LwM2MBootstrapServerCredential;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.RPKLwM2MBootstrapServerCredential;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.X509LwM2MBootstrapServerCredential;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceDao;
import org.thingsboard.server.dao.device.DeviceProfileDao;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.DeviceCredentialsValidationException;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DeviceProfileDataValidator extends DataValidator<DeviceProfile> {

    private static final Location LOCATION = new Location("", "", -1, -1);
    private static final String ATTRIBUTES_PROTO_SCHEMA = "attributes proto schema";
    private static final String TELEMETRY_PROTO_SCHEMA = "telemetry proto schema";
    private static final String RPC_REQUEST_PROTO_SCHEMA = "rpc request proto schema";
    private static final String RPC_RESPONSE_PROTO_SCHEMA = "rpc response proto schema";
    @Autowired
    private DeviceProfileDao deviceProfileDao;
    @Autowired
    @Lazy
    private DeviceProfileService deviceProfileService;
    @Autowired
    private DeviceDao deviceDao;
    @Autowired
    private TenantService tenantService;
    @Lazy
    @Autowired
    private QueueService queueService;
    @Autowired
    private OtaPackageService otaPackageService;
    @Autowired
    private RuleChainService ruleChainService;
    @Autowired
    private DashboardService dashboardService;

    private static String invalidSchemaProvidedMessage(String schemaName) {
        return "[Transport Configuration] invalid " + schemaName + " provided!";
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, DeviceProfile deviceProfile) {
        if (StringUtils.isEmpty(deviceProfile.getName())) {
            throw new DataValidationException("Device profile name should be specified!");
        }
        if (deviceProfile.getType() == null) {
            throw new DataValidationException("Device profile type should be specified!");
        }
        if (deviceProfile.getTransportType() == null) {
            throw new DataValidationException("Device profile transport type should be specified!");
        }
        if (deviceProfile.getTenantId() == null) {
            throw new DataValidationException("Device profile should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(deviceProfile.getTenantId())) {
                throw new DataValidationException("Device profile is referencing to non-existent tenant!");
            }
        }
        if (deviceProfile.isDefault()) {
            DeviceProfile defaultDeviceProfile = deviceProfileService.findDefaultDeviceProfile(tenantId);
            if (defaultDeviceProfile != null && !defaultDeviceProfile.getId().equals(deviceProfile.getId())) {
                throw new DataValidationException("Another default device profile is present in scope of current tenant!");
            }
        }
        if (deviceProfile.getDefaultQueueId() != null) {
            Queue queue = queueService.findQueueById(tenantId, deviceProfile.getDefaultQueueId());
            if (queue == null) {
                throw new DataValidationException("Device profile is referencing to non-existent queue!");
            }
        }
        if (deviceProfile.getProvisionType() == null) {
            deviceProfile.setProvisionType(DeviceProfileProvisionType.DISABLED);
        }
        DeviceProfileTransportConfiguration transportConfiguration = deviceProfile.getProfileData().getTransportConfiguration();
        transportConfiguration.validate();
        if (transportConfiguration instanceof MqttDeviceProfileTransportConfiguration) {
            MqttDeviceProfileTransportConfiguration mqttTransportConfiguration = (MqttDeviceProfileTransportConfiguration) transportConfiguration;
            if (mqttTransportConfiguration.getTransportPayloadTypeConfiguration() instanceof ProtoTransportPayloadConfiguration) {
                ProtoTransportPayloadConfiguration protoTransportPayloadConfiguration =
                        (ProtoTransportPayloadConfiguration) mqttTransportConfiguration.getTransportPayloadTypeConfiguration();
                validateProtoSchemas(protoTransportPayloadConfiguration);
                validateTelemetryDynamicMessageFields(protoTransportPayloadConfiguration);
                validateRpcRequestDynamicMessageFields(protoTransportPayloadConfiguration);
            }
        } else if (transportConfiguration instanceof CoapDeviceProfileTransportConfiguration) {
            CoapDeviceProfileTransportConfiguration coapDeviceProfileTransportConfiguration = (CoapDeviceProfileTransportConfiguration) transportConfiguration;
            CoapDeviceTypeConfiguration coapDeviceTypeConfiguration = coapDeviceProfileTransportConfiguration.getCoapDeviceTypeConfiguration();
            if (coapDeviceTypeConfiguration instanceof DefaultCoapDeviceTypeConfiguration) {
                DefaultCoapDeviceTypeConfiguration defaultCoapDeviceTypeConfiguration = (DefaultCoapDeviceTypeConfiguration) coapDeviceTypeConfiguration;
                TransportPayloadTypeConfiguration transportPayloadTypeConfiguration = defaultCoapDeviceTypeConfiguration.getTransportPayloadTypeConfiguration();
                if (transportPayloadTypeConfiguration instanceof ProtoTransportPayloadConfiguration) {
                    ProtoTransportPayloadConfiguration protoTransportPayloadConfiguration = (ProtoTransportPayloadConfiguration) transportPayloadTypeConfiguration;
                    validateProtoSchemas(protoTransportPayloadConfiguration);
                    validateTelemetryDynamicMessageFields(protoTransportPayloadConfiguration);
                    validateRpcRequestDynamicMessageFields(protoTransportPayloadConfiguration);
                }
            }
        } else if (transportConfiguration instanceof Lwm2mDeviceProfileTransportConfiguration) {
            List<LwM2MBootstrapServerCredential> lwM2MBootstrapServersConfigurations = ((Lwm2mDeviceProfileTransportConfiguration) transportConfiguration).getBootstrap();
            if (lwM2MBootstrapServersConfigurations != null) {
                validateLwm2mServersConfigOfBootstrapForClient(lwM2MBootstrapServersConfigurations,
                        ((Lwm2mDeviceProfileTransportConfiguration) transportConfiguration).isBootstrapServerUpdateEnable());
                for (LwM2MBootstrapServerCredential bootstrapServerCredential : lwM2MBootstrapServersConfigurations) {
                    validateLwm2mServersCredentialOfBootstrapForClient(bootstrapServerCredential);
                }
            }
        }

        List<DeviceProfileAlarm> profileAlarms = deviceProfile.getProfileData().getAlarms();

        if (!CollectionUtils.isEmpty(profileAlarms)) {
            Set<String> alarmTypes = new HashSet<>();
            for (DeviceProfileAlarm alarm : profileAlarms) {
                String alarmType = alarm.getAlarmType();
                if (StringUtils.isEmpty(alarmType)) {
                    throw new DataValidationException("Alarm rule type should be specified!");
                }
                if (!alarmTypes.add(alarmType)) {
                    throw new DataValidationException(String.format("Can't create device profile with the same alarm rule types: \"%s\"!", alarmType));
                }
            }
        }

        if (deviceProfile.getDefaultRuleChainId() != null) {
            RuleChain ruleChain = ruleChainService.findRuleChainById(tenantId, deviceProfile.getDefaultRuleChainId());
            if (ruleChain == null) {
                throw new DataValidationException("Can't assign non-existent rule chain!");
            }
        }

        if (deviceProfile.getDefaultDashboardId() != null) {
            DashboardInfo dashboard = dashboardService.findDashboardInfoById(tenantId, deviceProfile.getDefaultDashboardId());
            if (dashboard == null) {
                throw new DataValidationException("Can't assign non-existent dashboard!");
            }
        }

        if (deviceProfile.getFirmwareId() != null) {
            OtaPackage firmware = otaPackageService.findOtaPackageById(tenantId, deviceProfile.getFirmwareId());
            if (firmware == null) {
                throw new DataValidationException("Can't assign non-existent firmware!");
            }
            if (!firmware.getType().equals(OtaPackageType.FIRMWARE)) {
                throw new DataValidationException("Can't assign firmware with type: " + firmware.getType());
            }
            if (firmware.getData() == null && !firmware.hasUrl()) {
                throw new DataValidationException("Can't assign firmware with empty data!");
            }
            if (!firmware.getDeviceProfileId().equals(deviceProfile.getId())) {
                throw new DataValidationException("Can't assign firmware with different deviceProfile!");
            }
        }

        if (deviceProfile.getSoftwareId() != null) {
            OtaPackage software = otaPackageService.findOtaPackageById(tenantId, deviceProfile.getSoftwareId());
            if (software == null) {
                throw new DataValidationException("Can't assign non-existent software!");
            }
            if (!software.getType().equals(OtaPackageType.SOFTWARE)) {
                throw new DataValidationException("Can't assign software with type: " + software.getType());
            }
            if (software.getData() == null && !software.hasUrl()) {
                throw new DataValidationException("Can't assign software with empty data!");
            }
            if (!software.getDeviceProfileId().equals(deviceProfile.getId())) {
                throw new DataValidationException("Can't assign firmware with different deviceProfile!");
            }
        }
    }

    @Override
    protected DeviceProfile validateUpdate(TenantId tenantId, DeviceProfile deviceProfile) {
        DeviceProfile old = deviceProfileDao.findById(deviceProfile.getTenantId(), deviceProfile.getId().getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing device profile!");
        }
        boolean profileTypeChanged = !old.getType().equals(deviceProfile.getType());
        boolean transportTypeChanged = !old.getTransportType().equals(deviceProfile.getTransportType());
        if (profileTypeChanged || transportTypeChanged) {
            Long profileDeviceCount = deviceDao.countDevicesByDeviceProfileId(deviceProfile.getTenantId(), deviceProfile.getId().getId());
            if (profileDeviceCount > 0) {
                String message = null;
                if (profileTypeChanged) {
                    message = "Can't change device profile type because devices referenced it!";
                } else if (transportTypeChanged) {
                    message = "Can't change device profile transport type because devices referenced it!";
                }
                throw new DataValidationException(message);
            }
        }
        return old;
    }

    private void validateProtoSchemas(ProtoTransportPayloadConfiguration protoTransportPayloadTypeConfiguration) {
        try {
            validateTransportProtoSchema(protoTransportPayloadTypeConfiguration.getDeviceAttributesProtoSchema(), ATTRIBUTES_PROTO_SCHEMA);
            validateTransportProtoSchema(protoTransportPayloadTypeConfiguration.getDeviceTelemetryProtoSchema(), TELEMETRY_PROTO_SCHEMA);
            validateTransportProtoSchema(protoTransportPayloadTypeConfiguration.getDeviceRpcRequestProtoSchema(), RPC_REQUEST_PROTO_SCHEMA);
            validateTransportProtoSchema(protoTransportPayloadTypeConfiguration.getDeviceRpcResponseProtoSchema(), RPC_RESPONSE_PROTO_SCHEMA);
        } catch (Exception exception) {
            throw new DataValidationException(exception.getMessage());
        }
    }

    private void validateTransportProtoSchema(String schema, String schemaName) throws IllegalArgumentException {
        ProtoParser schemaParser = new ProtoParser(LOCATION, schema.toCharArray());
        ProtoFileElement protoFileElement;
        try {
            protoFileElement = schemaParser.readProtoFile();
        } catch (Exception e) {
            throw new IllegalArgumentException("[Transport Configuration] failed to parse " + schemaName + " due to: " + e.getMessage());
        }
        checkProtoFileSyntax(schemaName, protoFileElement);
        checkProtoFileCommonSettings(schemaName, protoFileElement.getOptions().isEmpty(), " Schema options don't support!");
        checkProtoFileCommonSettings(schemaName, protoFileElement.getPublicImports().isEmpty(), " Schema public imports don't support!");
        checkProtoFileCommonSettings(schemaName, protoFileElement.getImports().isEmpty(), " Schema imports don't support!");
        checkProtoFileCommonSettings(schemaName, protoFileElement.getExtendDeclarations().isEmpty(), " Schema extend declarations don't support!");
        checkTypeElements(schemaName, protoFileElement);
    }

    private void checkProtoFileSyntax(String schemaName, ProtoFileElement protoFileElement) {
        if (protoFileElement.getSyntax() == null || !protoFileElement.getSyntax().equals(Syntax.PROTO_3)) {
            throw new IllegalArgumentException("[Transport Configuration] invalid schema syntax: " + protoFileElement.getSyntax() +
                    " for " + schemaName + " provided! Only " + Syntax.PROTO_3 + " allowed!");
        }
    }

    private void checkProtoFileCommonSettings(String schemaName, boolean isEmptySettings, String invalidSettingsMessage) {
        if (!isEmptySettings) {
            throw new IllegalArgumentException(invalidSchemaProvidedMessage(schemaName) + invalidSettingsMessage);
        }
    }

    private void checkTypeElements(String schemaName, ProtoFileElement protoFileElement) {
        List<TypeElement> types = protoFileElement.getTypes();
        if (!types.isEmpty()) {
            if (types.stream().noneMatch(typeElement -> typeElement instanceof MessageElement)) {
                throw new IllegalArgumentException(invalidSchemaProvidedMessage(schemaName) + " At least one Message definition should exists!");
            } else {
                checkEnumElements(schemaName, getEnumElements(types));
                checkMessageElements(schemaName, getMessageTypes(types));
            }
        } else {
            throw new IllegalArgumentException(invalidSchemaProvidedMessage(schemaName) + " Type elements is empty!");
        }
    }

    private void checkFieldElements(String schemaName, List<FieldElement> fieldElements) {
        if (!fieldElements.isEmpty()) {
            boolean hasRequiredLabel = fieldElements.stream().anyMatch(fieldElement -> {
                Field.Label label = fieldElement.getLabel();
                return label != null && label.equals(Field.Label.REQUIRED);
            });
            if (hasRequiredLabel) {
                throw new IllegalArgumentException(invalidSchemaProvidedMessage(schemaName) + " Required labels are not supported!");
            }
            boolean hasDefaultValue = fieldElements.stream().anyMatch(fieldElement -> fieldElement.getDefaultValue() != null);
            if (hasDefaultValue) {
                throw new IllegalArgumentException(invalidSchemaProvidedMessage(schemaName) + " Default values are not supported!");
            }
        }
    }

    private void checkEnumElements(String schemaName, List<EnumElement> enumTypes) {
        if (enumTypes.stream().anyMatch(enumElement -> !enumElement.getNestedTypes().isEmpty())) {
            throw new IllegalArgumentException(invalidSchemaProvidedMessage(schemaName) + " Nested types in Enum definitions are not supported!");
        }
        if (enumTypes.stream().anyMatch(enumElement -> !enumElement.getOptions().isEmpty())) {
            throw new IllegalArgumentException(invalidSchemaProvidedMessage(schemaName) + " Enum definitions options are not supported!");
        }
    }

    private void checkMessageElements(String schemaName, List<MessageElement> messageElementsList) {
        if (!messageElementsList.isEmpty()) {
            messageElementsList.forEach(messageElement -> {
                checkProtoFileCommonSettings(schemaName, messageElement.getGroups().isEmpty(),
                        " Message definition groups don't support!");
                checkProtoFileCommonSettings(schemaName, messageElement.getOptions().isEmpty(),
                        " Message definition options don't support!");
                checkProtoFileCommonSettings(schemaName, messageElement.getExtensions().isEmpty(),
                        " Message definition extensions don't support!");
                checkProtoFileCommonSettings(schemaName, messageElement.getReserveds().isEmpty(),
                        " Message definition reserved elements don't support!");
                checkFieldElements(schemaName, messageElement.getFields());
                List<OneOfElement> oneOfs = messageElement.getOneOfs();
                if (!oneOfs.isEmpty()) {
                    oneOfs.forEach(oneOfElement -> {
                        checkProtoFileCommonSettings(schemaName, oneOfElement.getGroups().isEmpty(),
                                " OneOf definition groups don't support!");
                        checkFieldElements(schemaName, oneOfElement.getFields());
                    });
                }
                List<TypeElement> nestedTypes = messageElement.getNestedTypes();
                if (!nestedTypes.isEmpty()) {
                    List<EnumElement> nestedEnumTypes = getEnumElements(nestedTypes);
                    if (!nestedEnumTypes.isEmpty()) {
                        checkEnumElements(schemaName, nestedEnumTypes);
                    }
                    List<MessageElement> nestedMessageTypes = getMessageTypes(nestedTypes);
                    checkMessageElements(schemaName, nestedMessageTypes);
                }
            });
        }
    }

    private List<MessageElement> getMessageTypes(List<TypeElement> types) {
        return types.stream()
                .filter(typeElement -> typeElement instanceof MessageElement)
                .map(typeElement -> (MessageElement) typeElement)
                .collect(Collectors.toList());
    }

    private List<EnumElement> getEnumElements(List<TypeElement> types) {
        return types.stream()
                .filter(typeElement -> typeElement instanceof EnumElement)
                .map(typeElement -> (EnumElement) typeElement)
                .collect(Collectors.toList());
    }

    private void validateTelemetryDynamicMessageFields(ProtoTransportPayloadConfiguration protoTransportPayloadTypeConfiguration) {
        String deviceTelemetryProtoSchema = protoTransportPayloadTypeConfiguration.getDeviceTelemetryProtoSchema();
        Descriptors.Descriptor telemetryDynamicMessageDescriptor = protoTransportPayloadTypeConfiguration.getTelemetryDynamicMessageDescriptor(deviceTelemetryProtoSchema);
        if (telemetryDynamicMessageDescriptor == null) {
            throw new DataValidationException(invalidSchemaProvidedMessage(TELEMETRY_PROTO_SCHEMA) + " Failed to get telemetryDynamicMessageDescriptor!");
        } else {
            List<Descriptors.FieldDescriptor> fields = telemetryDynamicMessageDescriptor.getFields();
            if (CollectionUtils.isEmpty(fields)) {
                throw new DataValidationException(invalidSchemaProvidedMessage(TELEMETRY_PROTO_SCHEMA) + " " + telemetryDynamicMessageDescriptor.getName() + " fields is empty!");
            } else if (fields.size() == 2) {
                Descriptors.FieldDescriptor tsFieldDescriptor = telemetryDynamicMessageDescriptor.findFieldByName("ts");
                Descriptors.FieldDescriptor valuesFieldDescriptor = telemetryDynamicMessageDescriptor.findFieldByName("values");
                if (tsFieldDescriptor != null && valuesFieldDescriptor != null) {
                    if (!Descriptors.FieldDescriptor.Type.MESSAGE.equals(valuesFieldDescriptor.getType())) {
                        throw new DataValidationException(invalidSchemaProvidedMessage(TELEMETRY_PROTO_SCHEMA) + " Field 'values' has invalid data type. Only message type is supported!");
                    }
                    if (!Descriptors.FieldDescriptor.Type.INT64.equals(tsFieldDescriptor.getType())) {
                        throw new DataValidationException(invalidSchemaProvidedMessage(TELEMETRY_PROTO_SCHEMA) + " Field 'ts' has invalid data type. Only int64 type is supported!");
                    }
                    if (!tsFieldDescriptor.hasOptionalKeyword()) {
                        throw new DataValidationException(invalidSchemaProvidedMessage(TELEMETRY_PROTO_SCHEMA) + " Field 'ts' has invalid label. Field 'ts' should have optional keyword!");
                    }
                }
            }
        }
    }

    private void validateRpcRequestDynamicMessageFields(ProtoTransportPayloadConfiguration protoTransportPayloadTypeConfiguration) {
        DynamicMessage.Builder rpcRequestDynamicMessageBuilder = protoTransportPayloadTypeConfiguration.getRpcRequestDynamicMessageBuilder(protoTransportPayloadTypeConfiguration.getDeviceRpcRequestProtoSchema());
        Descriptors.Descriptor rpcRequestDynamicMessageDescriptor = rpcRequestDynamicMessageBuilder.getDescriptorForType();
        if (rpcRequestDynamicMessageDescriptor == null) {
            throw new DataValidationException(invalidSchemaProvidedMessage(RPC_REQUEST_PROTO_SCHEMA) + " Failed to get rpcRequestDynamicMessageDescriptor!");
        } else {
            if (CollectionUtils.isEmpty(rpcRequestDynamicMessageDescriptor.getFields()) || rpcRequestDynamicMessageDescriptor.getFields().size() != 3) {
                throw new DataValidationException(invalidSchemaProvidedMessage(RPC_REQUEST_PROTO_SCHEMA) + " " + rpcRequestDynamicMessageDescriptor.getName() + " message should always contains 3 fields: method, requestId and params!");
            }
            Descriptors.FieldDescriptor methodFieldDescriptor = rpcRequestDynamicMessageDescriptor.findFieldByName("method");
            if (methodFieldDescriptor == null) {
                throw new DataValidationException(invalidSchemaProvidedMessage(RPC_REQUEST_PROTO_SCHEMA) + " Failed to get field descriptor for field: method!");
            } else {
                if (!Descriptors.FieldDescriptor.Type.STRING.equals(methodFieldDescriptor.getType())) {
                    throw new DataValidationException(invalidSchemaProvidedMessage(RPC_REQUEST_PROTO_SCHEMA) + " Field 'method' has invalid data type. Only string type is supported!");
                }
                if (methodFieldDescriptor.isRepeated()) {
                    throw new DataValidationException(invalidSchemaProvidedMessage(RPC_REQUEST_PROTO_SCHEMA) + " Field 'method' has invalid label!");
                }
            }
            Descriptors.FieldDescriptor requestIdFieldDescriptor = rpcRequestDynamicMessageDescriptor.findFieldByName("requestId");
            if (requestIdFieldDescriptor == null) {
                throw new DataValidationException(invalidSchemaProvidedMessage(RPC_REQUEST_PROTO_SCHEMA) + " Failed to get field descriptor for field: requestId!");
            } else {
                if (!Descriptors.FieldDescriptor.Type.INT32.equals(requestIdFieldDescriptor.getType())) {
                    throw new DataValidationException(invalidSchemaProvidedMessage(RPC_REQUEST_PROTO_SCHEMA) + " Field 'requestId' has invalid data type. Only int32 type is supported!");
                }
                if (requestIdFieldDescriptor.isRepeated()) {
                    throw new DataValidationException(invalidSchemaProvidedMessage(RPC_REQUEST_PROTO_SCHEMA) + " Field 'requestId' has invalid label!");
                }
            }
            Descriptors.FieldDescriptor paramsFieldDescriptor = rpcRequestDynamicMessageDescriptor.findFieldByName("params");
            if (paramsFieldDescriptor == null) {
                throw new DataValidationException(invalidSchemaProvidedMessage(RPC_REQUEST_PROTO_SCHEMA) + " Failed to get field descriptor for field: params!");
            } else {
                if (paramsFieldDescriptor.isRepeated()) {
                    throw new DataValidationException(invalidSchemaProvidedMessage(RPC_REQUEST_PROTO_SCHEMA) + " Field 'params' has invalid label!");
                }
            }
        }
    }

    private void validateLwm2mServersConfigOfBootstrapForClient(List<LwM2MBootstrapServerCredential> lwM2MBootstrapServersConfigurations, boolean isBootstrapServerUpdateEnable) {
        Set<String> uris = new HashSet<>();
        Set<Integer> shortServerIds = new HashSet<>();
        for (LwM2MBootstrapServerCredential bootstrapServerCredential : lwM2MBootstrapServersConfigurations) {
            AbstractLwM2MBootstrapServerCredential serverConfig = (AbstractLwM2MBootstrapServerCredential) bootstrapServerCredential;
            if (!isBootstrapServerUpdateEnable && serverConfig.isBootstrapServerIs()) {
                throw new DeviceCredentialsValidationException("Bootstrap config must not include \"Bootstrap Server\". \"Include Bootstrap Server updates\" is " + isBootstrapServerUpdateEnable + ".");
            }
            String server = serverConfig.isBootstrapServerIs() ? "Bootstrap Server" : "LwM2M Server" + " shortServerId: " + serverConfig.getShortServerId() + ":";
            if (serverConfig.getShortServerId() < 1 || serverConfig.getShortServerId() > 65534) {
                throw new DeviceCredentialsValidationException(server + " ShortServerId must not be less than 1 and more than 65534!");
            }
            if (!shortServerIds.add(serverConfig.getShortServerId())) {
                throw new DeviceCredentialsValidationException(server + " \"Short server Id\" value = " + serverConfig.getShortServerId() + ". This value must be a unique value for all servers!");
            }
            String uri = serverConfig.getHost() + ":" + serverConfig.getPort();
            if (!uris.add(uri)) {
                throw new DeviceCredentialsValidationException(server + " \"Host + port\" value = " + uri + ". This value must be a unique value for all servers!");
            }
            Integer port;
            if (LwM2MSecurityMode.NO_SEC.equals(serverConfig.getSecurityMode())) {
                port = serverConfig.isBootstrapServerIs() ? 5687 : 5685;
            } else {
                port = serverConfig.isBootstrapServerIs() ? 5688 : 5686;
            }
            if (serverConfig.getPort() == null || serverConfig.getPort().intValue() != port) {
                throw new DeviceCredentialsValidationException(server + " \"Port\" value = " + serverConfig.getPort() + ". This value for security " + serverConfig.getSecurityMode().name() + " must be " + port + "!");
            }
        }
    }

    private void validateLwm2mServersCredentialOfBootstrapForClient(LwM2MBootstrapServerCredential bootstrapServerConfig) {
        String server;
        switch (bootstrapServerConfig.getSecurityMode()) {
            case NO_SEC:
            case PSK:
                break;
            case RPK:
                RPKLwM2MBootstrapServerCredential rpkServerCredentials = (RPKLwM2MBootstrapServerCredential) bootstrapServerConfig;
                server = rpkServerCredentials.isBootstrapServerIs() ? "Bootstrap Server" : "LwM2M Server";
                if (StringUtils.isEmpty(rpkServerCredentials.getServerPublicKey())) {
                    throw new DeviceCredentialsValidationException(server + " RPK public key must be specified!");
                }
                try {
                    String pubkRpkSever = EncryptionUtil.pubkTrimNewLines(rpkServerCredentials.getServerPublicKey());
                    rpkServerCredentials.setServerPublicKey(pubkRpkSever);
                    SecurityUtil.publicKey.decode(rpkServerCredentials.getDecodedCServerPublicKey());
                } catch (Exception e) {
                    throw new DeviceCredentialsValidationException(server + " RPK public key must be in standard [RFC7250] and then encoded to Base64 format!");
                }
                break;
            case X509:
                X509LwM2MBootstrapServerCredential x509ServerCredentials = (X509LwM2MBootstrapServerCredential) bootstrapServerConfig;
                server = x509ServerCredentials.isBootstrapServerIs() ? "Bootstrap Server" : "LwM2M Server";
                if (StringUtils.isEmpty(x509ServerCredentials.getServerPublicKey())) {
                    throw new DeviceCredentialsValidationException(server + " X509 certificate must be specified!");
                }

                try {
                    String certServer = EncryptionUtil.certTrimNewLines(x509ServerCredentials.getServerPublicKey());
                    x509ServerCredentials.setServerPublicKey(certServer);
                    SecurityUtil.certificate.decode(x509ServerCredentials.getDecodedCServerPublicKey());
                } catch (Exception e) {
                    throw new DeviceCredentialsValidationException(server + " X509 certificate must be in DER-encoded X509v3 format and support only EC algorithm and then encoded to Base64 format!");
                }
                break;
        }
    }
}
