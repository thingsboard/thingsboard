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
package org.thingsboard.server.transport.mqtt;

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
import org.thingsboard.server.common.data.device.profile.JsonTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.ProtoTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.TransportPayloadTypeConfiguration;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.AbstractTransportIntegrationTest;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public abstract class AbstractMqttIntegrationTest extends AbstractTransportIntegrationTest {

    protected Device savedGateway;
    protected String gatewayAccessToken;

    protected DeviceProfile deviceProfile;

    protected void processBeforeTest (String deviceName, String gatewayName, TransportPayloadType payloadType, String telemetryTopic, String attributesTopic) throws Exception {
        this.processBeforeTest(deviceName, gatewayName, payloadType, telemetryTopic, attributesTopic, null, null, null, null, null, null, DeviceProfileProvisionType.DISABLED);
    }

    protected void processBeforeTest(String deviceName,
                                     String gatewayName,
                                     TransportPayloadType payloadType,
                                     String telemetryTopic,
                                     String attributesTopic,
                                     String telemetryProtoSchema,
                                     String attributesProtoSchema,
                                     String rpcResponseProtoSchema,
                                     String rpcRequestProtoSchema,
                                     String provisionKey,
                                     String provisionSecret,
                                     DeviceProfileProvisionType provisionType
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
            DeviceProfile mqttDeviceProfile = createMqttDeviceProfile(payloadType, telemetryTopic, attributesTopic, telemetryProtoSchema, attributesProtoSchema, rpcResponseProtoSchema, rpcRequestProtoSchema, provisionKey, provisionSecret, provisionType);
            deviceProfile = doPost("/api/deviceProfile", mqttDeviceProfile, DeviceProfile.class);
            device.setType(deviceProfile.getName());
            device.setDeviceProfileId(deviceProfile.getId());
            gateway.setType(deviceProfile.getName());
            gateway.setDeviceProfileId(deviceProfile.getId());
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

    protected DeviceProfile createMqttDeviceProfile(TransportPayloadType transportPayloadType,
                                                    String telemetryTopic, String attributesTopic,
                                                    String telemetryProtoSchema, String attributesProtoSchema,
                                                    String rpcResponseProtoSchema, String rpcRequestProtoSchema,
                                                    String provisionKey, String provisionSecret,
                                                    DeviceProfileProvisionType provisionType) {
        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setName(transportPayloadType.name());
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        deviceProfile.setTransportType(DeviceTransportType.MQTT);
        deviceProfile.setProvisionType(provisionType);
        deviceProfile.setProvisionDeviceKey(provisionKey);
        deviceProfile.setDescription(transportPayloadType.name() + " Test");
        DeviceProfileData deviceProfileData = new DeviceProfileData();
        DefaultDeviceProfileConfiguration configuration = new DefaultDeviceProfileConfiguration();
        MqttDeviceProfileTransportConfiguration mqttDeviceProfileTransportConfiguration = new MqttDeviceProfileTransportConfiguration();
        if (!StringUtils.isEmpty(telemetryTopic)) {
            mqttDeviceProfileTransportConfiguration.setDeviceTelemetryTopic(telemetryTopic);
        }
        if (!StringUtils.isEmpty(attributesTopic)) {
            mqttDeviceProfileTransportConfiguration.setDeviceAttributesTopic(attributesTopic);
        }
        TransportPayloadTypeConfiguration transportPayloadTypeConfiguration;
        if (TransportPayloadType.JSON.equals(transportPayloadType)) {
            transportPayloadTypeConfiguration = new JsonTransportPayloadConfiguration();
        } else {
            ProtoTransportPayloadConfiguration protoTransportPayloadConfiguration = new ProtoTransportPayloadConfiguration();
            if (StringUtils.isEmpty(telemetryProtoSchema)) {
                telemetryProtoSchema = DEVICE_TELEMETRY_PROTO_SCHEMA;
            }
            if (StringUtils.isEmpty(attributesProtoSchema)) {
                attributesProtoSchema = DEVICE_ATTRIBUTES_PROTO_SCHEMA;
            }
            if (StringUtils.isEmpty(rpcResponseProtoSchema)) {
                rpcResponseProtoSchema = DEVICE_RPC_RESPONSE_PROTO_SCHEMA;
            }
            if (StringUtils.isEmpty(rpcRequestProtoSchema)) {
                rpcRequestProtoSchema = DEVICE_RPC_REQUEST_PROTO_SCHEMA;
            }
            protoTransportPayloadConfiguration.setDeviceTelemetryProtoSchema(telemetryProtoSchema);
            protoTransportPayloadConfiguration.setDeviceAttributesProtoSchema(attributesProtoSchema);
            protoTransportPayloadConfiguration.setDeviceRpcResponseProtoSchema(rpcResponseProtoSchema);
            protoTransportPayloadConfiguration.setDeviceRpcRequestProtoSchema(rpcRequestProtoSchema);
            transportPayloadTypeConfiguration = protoTransportPayloadConfiguration;
        }
        mqttDeviceProfileTransportConfiguration.setTransportPayloadTypeConfiguration(transportPayloadTypeConfiguration);
        deviceProfileData.setTransportConfiguration(mqttDeviceProfileTransportConfiguration);
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
                break;
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
}
