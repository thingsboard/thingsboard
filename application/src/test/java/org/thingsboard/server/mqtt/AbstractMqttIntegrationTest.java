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
package org.thingsboard.server.mqtt;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.Assert;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.device.profile.AllowCreateNewDevicesDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.CheckPreProvisionedDevicesDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.DeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.DisabledDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.MqttJsonDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.MqttProtoDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public abstract class AbstractMqttIntegrationTest extends AbstractControllerTest {

    protected static final String MQTT_URL = "tcp://localhost:1883";

    private static final AtomicInteger atomicInteger = new AtomicInteger(2);

    protected static final String DEVICE_TELEMETRY_PROTO_SCHEMA = "syntax = \"proto2\";\n" +
            "package test;\n" +
            "\n" +
            "option java_package = \"org.thingsboard.server.gen.test\";\n" +
            "option java_outer_classname = \"TestProtos\";\n" +
            "\n" +
            "message nestedPostTelemetry {\n" +
            "  required bool bool_v = 1;\n" +
            "  required int64 long_v = 2;\n" +
            "  required double double_v = 3;\n" +
            "  required string string_v = 4;\n" +
            "  required string json_v = 5;\n" +
            "}\n" +
            "\n" +
            "message PostTelemetry {\n" +
            "  repeated nestedPostTelemetry postTelemetry = 1;\n" +
            "}";
    protected static final String DEVICE_ATTRIBUTES_PROTO_SCHEMA = "syntax = \"proto2\";\n" +
            "package test;\n" +
            "\n" +
            "option java_package = \"org.thingsboard.server.gen.test\";\n" +
            "option java_outer_classname = \"TestProtos\";\n" +
            "\n" +
            "message nestedPostAttributes {\n" +
            "  required bool bool_v = 1;\n" +
            "  required int64 long_v = 2;\n" +
            "  required double double_v = 3;\n" +
            "  required string string_v = 4;\n" +
            "  required string json_v = 5;\n" +
            "}\n" +
            "\n" +
            "message PostAttributes {\n" +
            "  repeated nestedPostAttributes postAttributes = 1;\n" +
            "}";

    protected Tenant savedTenant;
    protected User tenantAdmin;

    protected Device savedDevice;
    protected String accessToken;

    protected Device savedGateway;
    protected String gatewayAccessToken;

    protected void processBeforeTest (String deviceName, String gatewayName, TransportPayloadType payloadType, String telemetryTopic, String attributesTopic) throws Exception {
        this.processBeforeTest(deviceName, gatewayName, payloadType, telemetryTopic, attributesTopic, DeviceProfileProvisionType.DISABLED, null, null);
    }

    protected void processBeforeTest(String deviceName,
                                     String gatewayName,
                                     TransportPayloadType payloadType,
                                     String telemetryTopic,
                                     String attributesTopic,
                                     DeviceProfileProvisionType provisionType,
                                     String provisionKey, String provisionSecret
                                     ) throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant" + atomicInteger.getAndIncrement() + "@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");

        Device device = new Device();
        device.setName(deviceName);
        device.setType("default");

        Device gateway = new Device();
        gateway.setName(gatewayName);
        gateway.setType("default");
        ObjectNode additionalInfo = mapper.createObjectNode();
        additionalInfo.put("gateway", true);
        gateway.setAdditionalInfo(additionalInfo);

        if (payloadType != null) {
            DeviceProfile mqttDeviceProfile = createMqttDeviceProfile(payloadType, telemetryTopic, attributesTopic, provisionType, provisionKey, provisionSecret);
            DeviceProfile savedDeviceProfile = doPost("/api/deviceProfile", mqttDeviceProfile, DeviceProfile.class);
            device.setType(savedDeviceProfile.getName());
            device.setDeviceProfileId(savedDeviceProfile.getId());
            gateway.setType(savedDeviceProfile.getName());
            gateway.setDeviceProfileId(savedDeviceProfile.getId());
        }

        savedDevice = doPost("/api/device", device, Device.class);

        DeviceCredentials deviceCredentials =
                doGet("/api/device/" + savedDevice.getId().getId().toString() + "/credentials", DeviceCredentials.class);

        savedGateway = doPost("/api/device", gateway, Device.class);

        DeviceCredentials gatewayCredentials =
                doGet("/api/device/" + savedGateway.getId().getId().toString() + "/credentials", DeviceCredentials.class);

        assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        accessToken = deviceCredentials.getCredentialsId();
        assertNotNull(accessToken);

        assertEquals(savedGateway.getId(), gatewayCredentials.getDeviceId());
        gatewayAccessToken = gatewayCredentials.getCredentialsId();
        assertNotNull(gatewayAccessToken);

    }

    protected void processAfterTest() throws Exception {
        loginSysAdmin();
        if (savedTenant != null) {
            doDelete("/api/tenant/" + savedTenant.getId().getId().toString()).andExpect(status().isOk());
        }
    }

    protected MqttAsyncClient getMqttAsyncClient(String accessToken) throws MqttException {
        String clientId = MqttAsyncClient.generateClientId();
        MqttAsyncClient client = new MqttAsyncClient(MQTT_URL, clientId, new MemoryPersistence());

        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(accessToken);
        client.connect(options).waitForCompletion();
        return client;
    }

    protected void publishMqttMsg(MqttAsyncClient client, byte[] payload, String topic) throws MqttException {
        MqttMessage message = new MqttMessage();
        message.setPayload(payload);
        client.publish(topic, message);
    }

    protected List<TransportProtos.KeyValueProto> getKvProtos(List<String> expectedKeys) {
        List<TransportProtos.KeyValueProto> keyValueProtos = new ArrayList<>();
        TransportProtos.KeyValueProto strKeyValueProto = getKeyValueProto(expectedKeys.get(0), "value1", TransportProtos.KeyValueType.STRING_V);
        TransportProtos.KeyValueProto boolKeyValueProto = getKeyValueProto(expectedKeys.get(1), "true", TransportProtos.KeyValueType.BOOLEAN_V);
        TransportProtos.KeyValueProto dblKeyValueProto = getKeyValueProto(expectedKeys.get(2), "3.0", TransportProtos.KeyValueType.DOUBLE_V);
        TransportProtos.KeyValueProto longKeyValueProto = getKeyValueProto(expectedKeys.get(3), "4", TransportProtos.KeyValueType.LONG_V);
        TransportProtos.KeyValueProto jsonKeyValueProto = getKeyValueProto(expectedKeys.get(4), "{\"someNumber\": 42, \"someArray\": [1,2,3], \"someNestedObject\": {\"key\": \"value\"}}", TransportProtos.KeyValueType.JSON_V);
        keyValueProtos.add(strKeyValueProto);
        keyValueProtos.add(boolKeyValueProto);
        keyValueProtos.add(dblKeyValueProto);
        keyValueProtos.add(longKeyValueProto);
        keyValueProtos.add(jsonKeyValueProto);
        return keyValueProtos;
    }

    protected TransportProtos.KeyValueProto getKeyValueProto(String key, String strValue, TransportProtos.KeyValueType type) {
        TransportProtos.KeyValueProto.Builder keyValueProtoBuilder = TransportProtos.KeyValueProto.newBuilder();
        keyValueProtoBuilder.setKey(key);
        keyValueProtoBuilder.setType(type);
        switch (type) {
            case BOOLEAN_V:
                keyValueProtoBuilder.setBoolV(Boolean.parseBoolean(strValue));
                break;
            case LONG_V:
                keyValueProtoBuilder.setLongV(Long.parseLong(strValue));
                break;
            case DOUBLE_V:
                keyValueProtoBuilder.setDoubleV(Double.parseDouble(strValue));
                break;
            case STRING_V:
                keyValueProtoBuilder.setStringV(strValue);
                break;
            case JSON_V:
                keyValueProtoBuilder.setJsonV(strValue);
                break;
        }
        return keyValueProtoBuilder.build();
    }

    protected DeviceProfile createMqttDeviceProfile(TransportPayloadType transportPayloadType,
                                                    String telemetryTopic, String attributesTopic,
                                                    DeviceProfileProvisionType provisionType,
                                                    String provisionKey, String provisionSecret
    ) {
        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setName(transportPayloadType.name());
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        deviceProfile.setTransportType(DeviceTransportType.MQTT);
        deviceProfile.setProvisionType(provisionType);
        deviceProfile.setProvisionDeviceKey(provisionKey);
        deviceProfile.setDescription(transportPayloadType.name() + " Test");
        DeviceProfileData deviceProfileData = new DeviceProfileData();
        DefaultDeviceProfileConfiguration configuration = new DefaultDeviceProfileConfiguration();
        MqttDeviceProfileTransportConfiguration transportConfiguration;
        if (TransportPayloadType.JSON.equals(transportPayloadType)) {
            transportConfiguration = new MqttJsonDeviceProfileTransportConfiguration();
        } else {
            MqttProtoDeviceProfileTransportConfiguration protoTransportConfiguration = new MqttProtoDeviceProfileTransportConfiguration();
            protoTransportConfiguration.setDeviceAttributesProtoSchema(DEVICE_TELEMETRY_PROTO_SCHEMA);
            protoTransportConfiguration.setDeviceTelemetryProtoSchema(DEVICE_ATTRIBUTES_PROTO_SCHEMA);
            transportConfiguration = protoTransportConfiguration;
        }
        if (!StringUtils.isEmpty(telemetryTopic)) {
            transportConfiguration.setDeviceTelemetryTopic(telemetryTopic);
        }
        if (!StringUtils.isEmpty(attributesTopic)) {
            transportConfiguration.setDeviceAttributesTopic(attributesTopic);
        }
        deviceProfileData.setTransportConfiguration(transportConfiguration);
        DeviceProfileProvisionConfiguration provisionConfiguration;
        switch (provisionType) {
            case ALLOW_CREATE_NEW_DEVICES:
                provisionConfiguration = new AllowCreateNewDevicesDeviceProfileProvisionConfiguration(provisionSecret);
                break;
            case CHECK_PRE_PROVISIONED_DEVICES:
                provisionConfiguration = new CheckPreProvisionedDevicesDeviceProfileProvisionConfiguration(provisionSecret);
                break;
            case DISABLED:
            default:
                provisionConfiguration = new DisabledDeviceProfileProvisionConfiguration(provisionSecret);
        }
        deviceProfileData.setProvisionConfiguration(provisionConfiguration);
        deviceProfileData.setConfiguration(configuration);
        deviceProfile.setProfileData(deviceProfileData);
        deviceProfile.setDefault(false);
        deviceProfile.setDefaultRuleChainId(null);
        return deviceProfile;
    }

    protected TransportProtos.PostAttributeMsg getPostAttributeMsg(List<String> expectedKeys) {
        List<TransportProtos.KeyValueProto> kvProtos = getKvProtos(expectedKeys);
        TransportProtos.PostAttributeMsg.Builder builder = TransportProtos.PostAttributeMsg.newBuilder();
        builder.addAllKv(kvProtos);
        return builder.build();
    }

    protected <T> T doExecuteWithRetriesAndInterval(SupplierWithThrowable<T> supplier, int retries, int intervalMs) throws Exception {
        int count = 0;
        T result = null;
        Throwable lastException = null;
        while (count < retries) {
            try {
                result = supplier.get();
                if (result != null) {
                    return result;
                }
            } catch (Throwable e) {
                lastException = e;
            }
            count++;
            if (count < retries) {
                Thread.sleep(intervalMs);
            }
        }
        if (lastException != null) {
            throw new RuntimeException(lastException);
        } else {
            return result;
        }
    }

    @FunctionalInterface
    public interface SupplierWithThrowable<T> {
        T get() throws Throwable;
    }
}
