/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.TestSocketUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileInfo;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TransportPayloadType;
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
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.AbstractTransportIntegrationTest;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestClient;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@TestPropertySource(properties = {
        "service.integrations.supported=ALL",
        "transport.mqtt.enabled=true",
})
@Slf4j
public abstract class AbstractMqttIntegrationTest extends AbstractTransportIntegrationTest {

    public static final String MQTT_HOST = "localhost";
    public static final int MQTT_PORT = TestSocketUtils.findAvailableTcpPort();
    public static final String MQTT_URL = "tcp://" + MQTT_HOST + ":" + MQTT_PORT;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        log.warn("transport.mqtt.bind_port = {}", MQTT_PORT);
        registry.add("transport.mqtt.bind_port", () -> MQTT_PORT);
    }

    protected Device savedGateway;
    protected String gatewayAccessToken;

    protected void processBeforeTest(MqttTestConfigProperties config) throws Exception {
        loginTenantAdmin();
        deviceProfile = createMqttDeviceProfile(config);
        assertNotNull(deviceProfile);
        if (config.getDeviceName() != null) {
            savedDevice = createDevice(config.getDeviceName(), deviceProfile.getName(), false);
            DeviceCredentials deviceCredentials =
                    doGet("/api/device/" + savedDevice.getId().getId().toString() + "/credentials", DeviceCredentials.class);
            assertNotNull(deviceCredentials);
            assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
            accessToken = deviceCredentials.getCredentialsId();
            assertNotNull(accessToken);
        }
        if (config.getGatewayName() != null) {
            savedGateway = createDevice(config.getGatewayName(), deviceProfile.getName(), true);
            DeviceCredentials gatewayCredentials =
                    doGet("/api/device/" + savedGateway.getId().getId().toString() + "/credentials", DeviceCredentials.class);
            assertNotNull(gatewayCredentials);
            assertEquals(savedGateway.getId(), gatewayCredentials.getDeviceId());
            gatewayAccessToken = gatewayCredentials.getCredentialsId();
            assertNotNull(gatewayAccessToken);
        }
    }

    protected DeviceProfile createMqttDeviceProfile(MqttTestConfigProperties config) throws Exception {
        TransportPayloadType transportPayloadType = config.getTransportPayloadType();
        if (transportPayloadType == null) {
            DeviceProfileInfo defaultDeviceProfileInfo = doGet("/api/deviceProfileInfo/default", DeviceProfileInfo.class);
            return doGet("/api/deviceProfile/" + defaultDeviceProfileInfo.getId().getId(), DeviceProfile.class);
        } else {
            DeviceProfile deviceProfile = new DeviceProfile();
            deviceProfile.setName(transportPayloadType.name());
            deviceProfile.setType(DeviceProfileType.DEFAULT);
            deviceProfile.setTransportType(DeviceTransportType.MQTT);
            DeviceProfileProvisionType provisionType = config.getProvisionType() != null ?
                    config.getProvisionType() : DeviceProfileProvisionType.DISABLED;
            deviceProfile.setProvisionType(provisionType);
            deviceProfile.setProvisionDeviceKey(config.getProvisionKey());
            deviceProfile.setDescription(transportPayloadType.name() + " Test");
            DeviceProfileData deviceProfileData = new DeviceProfileData();
            DefaultDeviceProfileConfiguration configuration = new DefaultDeviceProfileConfiguration();
            MqttDeviceProfileTransportConfiguration mqttDeviceProfileTransportConfiguration = new MqttDeviceProfileTransportConfiguration();
            if (StringUtils.hasLength(config.getTelemetryTopicFilter())) {
                mqttDeviceProfileTransportConfiguration.setDeviceTelemetryTopic(config.getTelemetryTopicFilter());
            }
            if (StringUtils.hasLength(config.getAttributesTopicFilter())) {
                mqttDeviceProfileTransportConfiguration.setDeviceAttributesTopic(config.getAttributesTopicFilter());
            }
            mqttDeviceProfileTransportConfiguration.setSparkplug(config.isSparkplug());
            mqttDeviceProfileTransportConfiguration.setSparkplugAttributesMetricNames(config.sparkplugAttributesMetricNames);
            mqttDeviceProfileTransportConfiguration.setSendAckOnValidationException(config.isSendAckOnValidationException());
            TransportPayloadTypeConfiguration transportPayloadTypeConfiguration;
            if (TransportPayloadType.JSON.equals(transportPayloadType)) {
                transportPayloadTypeConfiguration = new JsonTransportPayloadConfiguration();
            } else {
                ProtoTransportPayloadConfiguration protoTransportPayloadConfiguration = new ProtoTransportPayloadConfiguration();
                String telemetryProtoSchema = config.getTelemetryProtoSchema();
                String attributesProtoSchema = config.getAttributesProtoSchema();
                String rpcResponseProtoSchema = config.getRpcResponseProtoSchema();
                String rpcRequestProtoSchema = config.getRpcRequestProtoSchema();
                protoTransportPayloadConfiguration.setDeviceTelemetryProtoSchema(
                        telemetryProtoSchema != null ? telemetryProtoSchema : DEVICE_TELEMETRY_PROTO_SCHEMA
                );
                protoTransportPayloadConfiguration.setDeviceAttributesProtoSchema(
                        attributesProtoSchema != null ? attributesProtoSchema : DEVICE_ATTRIBUTES_PROTO_SCHEMA
                );
                protoTransportPayloadConfiguration.setDeviceRpcResponseProtoSchema(
                        rpcResponseProtoSchema != null ? rpcResponseProtoSchema : DEVICE_RPC_RESPONSE_PROTO_SCHEMA
                );
                protoTransportPayloadConfiguration.setDeviceRpcRequestProtoSchema(
                        rpcRequestProtoSchema != null ? rpcRequestProtoSchema : DEVICE_RPC_REQUEST_PROTO_SCHEMA
                );
                protoTransportPayloadConfiguration.setEnableCompatibilityWithJsonPayloadFormat(
                        config.isEnableCompatibilityWithJsonPayloadFormat()
                );
                protoTransportPayloadConfiguration.setUseJsonPayloadFormatForDefaultDownlinkTopics(
                        config.isEnableCompatibilityWithJsonPayloadFormat() &&
                                config.isUseJsonPayloadFormatForDefaultDownlinkTopics()
                );
                transportPayloadTypeConfiguration = protoTransportPayloadConfiguration;
            }
            mqttDeviceProfileTransportConfiguration.setTransportPayloadTypeConfiguration(transportPayloadTypeConfiguration);
            deviceProfileData.setTransportConfiguration(mqttDeviceProfileTransportConfiguration);
            DeviceProfileProvisionConfiguration provisionConfiguration;
            switch (provisionType) {
                case ALLOW_CREATE_NEW_DEVICES:
                    provisionConfiguration = new AllowCreateNewDevicesDeviceProfileProvisionConfiguration(config.getProvisionSecret());
                    break;
                case CHECK_PRE_PROVISIONED_DEVICES:
                    provisionConfiguration = new CheckPreProvisionedDevicesDeviceProfileProvisionConfiguration(config.getProvisionSecret());
                    break;
                case DISABLED:
                default:
                    provisionConfiguration = new DisabledDeviceProfileProvisionConfiguration(config.getProvisionSecret());
                    break;
            }
            deviceProfileData.setProvisionConfiguration(provisionConfiguration);
            deviceProfileData.setConfiguration(configuration);
            deviceProfile.setProfileData(deviceProfileData);
            deviceProfile.setDefault(false);
            deviceProfile.setDefaultRuleChainId(null);
            return doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);
        }
    }

    protected Device createDevice(String name, String type, boolean gateway) throws Exception {
        Device device = new Device();
        device.setName(name);
        device.setType(type);
        if (gateway) {
            ObjectNode additionalInfo = JacksonUtil.newObjectNode();
            additionalInfo.put("gateway", true);
            device.setAdditionalInfo(additionalInfo);
        }
        return doPost("/api/device", device, Device.class);
    }

    protected TransportProtos.PostAttributeMsg getPostAttributeMsg(List<String> expectedKeys) {
        List<TransportProtos.KeyValueProto> kvProtos = getKvProtos(expectedKeys);
        TransportProtos.PostAttributeMsg.Builder builder = TransportProtos.PostAttributeMsg.newBuilder();
        builder.addAllKv(kvProtos);
        return builder.build();
    }

    protected void subscribeAndWait(MqttTestClient client, String attrSubTopic, DeviceId deviceId, FeatureType featureType) throws MqttException {
        subscribeAndWait(client, attrSubTopic, deviceId, featureType, MqttQoS.AT_MOST_ONCE);
    }

    protected void subscribeAndWait(MqttTestClient client, String subTopic, DeviceId deviceId, FeatureType featureType, MqttQoS mqttQoS) throws MqttException {
        int subscriptionCount = getDeviceActorSubscriptionCount(deviceId, featureType);
        client.subscribeAndWait(subTopic, mqttQoS);
        // TODO: This test awaits for the device actor to receive the subscription. Ideally it should not happen. See details below:
        // The transport layer acknowledge subscription request once the message about subscription is in the queue.
        // Test sends data immediately after acknowledgement.
        // But there is a time lag between push to the queue and read from the queue in the tb-core component.
        // Ideally, we should reply to device with SUBACK only when the device actor on the tb-core receives the message.
        awaitForDeviceActorToReceiveSubscription(deviceId, featureType, subscriptionCount + 1);
    }

    protected void subscribeAndCheckSubscription(MqttTestClient client, String subTopic, DeviceId deviceId, FeatureType featureType) throws MqttException {
        client.subscribeAndWait(subTopic, MqttQoS.AT_MOST_ONCE);
        // TODO: This test awaits for the device actor to receive the subscription. Ideally it should not happen. See details below:
        // The transport layer acknowledge subscription request once the message about subscription is in the queue.
        // Test sends data immediately after acknowledgement.
        // But there is a time lag between push to the queue and read from the queue in the tb-core component.
        // Ideally, we should reply to device with SUBACK only when the device actor on the tb-core receives the message.
        awaitForDeviceActorToReceiveSubscription(deviceId, featureType, 1);
    }
}
