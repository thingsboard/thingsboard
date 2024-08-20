/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.transport.mqtt.mqttv3.rpc;

import com.fasterxml.jackson.core.type.TypeReference;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.mqtt.AbstractMqttIntegrationTest;
import org.thingsboard.server.transport.mqtt.MqttTestConfigProperties;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestCallback;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestClient;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestSubscribeOnTopicCallback;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_RPC_REQUESTS_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_RPC_RESPONSE_SUB_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_RPC_RESPONSE_TOPIC;

@DaoSqlTest
public class MqttClientSideRpcIntegrationTest extends AbstractMqttIntegrationTest {

    @Value("${transport.mqtt.netty.max_payload_size}")
    private Integer maxPayloadSize;

    @Value("${transport.mqtt.msg_queue_size_per_device_limit:100}")
    private int maxQueueSize;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        TenantProfile tenantProfile = doGet("/api/tenantProfile/" + tenantProfileId, TenantProfile.class);
        Assert.assertNotNull(tenantProfile);

        DefaultTenantProfileConfiguration profileConfiguration = (DefaultTenantProfileConfiguration) tenantProfile.getProfileData().getConfiguration();

        profileConfiguration.setTransportGatewayMsgRateLimit(null);
        profileConfiguration.setTransportGatewayTelemetryMsgRateLimit(null);
        profileConfiguration.setTransportGatewayTelemetryDataPointsRateLimit(null);

        profileConfiguration.setTransportGatewayDeviceMsgRateLimit(null);
        profileConfiguration.setTransportGatewayDeviceTelemetryMsgRateLimit(null);
        profileConfiguration.setTransportGatewayDeviceTelemetryDataPointsRateLimit(null);

        doPost("/api/tenantProfile", tenantProfile);
    }

    @Test
    public void getServiceConfigurationRpcForDeviceTest() throws Exception {
        TenantProfile tenantProfile = doGet("/api/tenantProfile/" + tenantProfileId, TenantProfile.class);
        DefaultTenantProfileConfiguration profileConfiguration = (DefaultTenantProfileConfiguration) tenantProfile.getProfileData().getConfiguration();

        profileConfiguration.setTransportDeviceMsgRateLimit("20:600");
        profileConfiguration.setTransportDeviceTelemetryMsgRateLimit("10:600");
        profileConfiguration.setTransportDeviceTelemetryDataPointsRateLimit("40:600");

        doPost("/api/tenantProfile", tenantProfile);

        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Get Service Configuration")
                .transportPayloadType(TransportPayloadType.JSON)
                .build();
        processBeforeTest(configProperties);

        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(accessToken);

        MqttTestCallback callback = new MqttTestSubscribeOnTopicCallback(DEVICE_RPC_RESPONSE_TOPIC + "1");
        client.setCallback(callback);
        client.subscribeAndWait(DEVICE_RPC_RESPONSE_SUB_TOPIC, MqttQoS.AT_MOST_ONCE);

        client.publishAndWait(DEVICE_RPC_REQUESTS_TOPIC + "1", "{\"method\":\"getServiceConfiguration\",\"params\":{}}".getBytes());

        assertThat(callback.getSubscribeLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .as("await callback").isTrue();

        var payload = callback.getPayloadBytes();
        Map<String, Object> response = JacksonUtil.fromBytes(payload, new TypeReference<>() {});

        assertNotNull(response);
        assertEquals(response.size(), 6);
        assertEquals(response.get("deviceMsgRateLimit"), profileConfiguration.getTransportDeviceMsgRateLimit());
        assertEquals(response.get("deviceTelemetryMsgRateLimit"), profileConfiguration.getTransportDeviceTelemetryMsgRateLimit());
        assertEquals(response.get("deviceTelemetryDataPointsRateLimit"), profileConfiguration.getTransportDeviceTelemetryDataPointsRateLimit());
        assertEquals(response.get("maxPayloadSize"), maxPayloadSize);
        assertEquals(response.get("maxQueueSize"), maxQueueSize);
        assertEquals(response.get("payloadType"), TransportPayloadType.JSON.name());

        client.disconnect();
    }

    @Test
    public void getServiceConfigurationRpcForGatewayTest() throws Exception {
        TenantProfile tenantProfile = doGet("/api/tenantProfile/" + tenantProfileId, TenantProfile.class);
        DefaultTenantProfileConfiguration profileConfiguration = (DefaultTenantProfileConfiguration) tenantProfile.getProfileData().getConfiguration();

        profileConfiguration.setTransportGatewayMsgRateLimit("100:600");
        profileConfiguration.setTransportGatewayTelemetryMsgRateLimit("50:600");
        profileConfiguration.setTransportGatewayTelemetryDataPointsRateLimit("200:600");

        profileConfiguration.setTransportGatewayDeviceMsgRateLimit("20:600");
        profileConfiguration.setTransportGatewayDeviceTelemetryMsgRateLimit("10:600");
        profileConfiguration.setTransportGatewayDeviceTelemetryDataPointsRateLimit("40:600");

        doPost("/api/tenantProfile", tenantProfile);

        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .gatewayName("Test Get Service Configuration Gateway")
                .transportPayloadType(TransportPayloadType.JSON)
                .build();
        processBeforeTest(configProperties);

        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(gatewayAccessToken);

        MqttTestCallback callback = new MqttTestSubscribeOnTopicCallback(DEVICE_RPC_RESPONSE_TOPIC + "1");
        client.setCallback(callback);
        client.subscribeAndWait(DEVICE_RPC_RESPONSE_SUB_TOPIC, MqttQoS.AT_MOST_ONCE);

        client.publishAndWait(DEVICE_RPC_REQUESTS_TOPIC + "1", "{\"method\":\"getServiceConfiguration\",\"params\":{}}".getBytes());

        assertTrue(callback.getSubscribeLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS));

        var payload = callback.getPayloadBytes();
        Map<String, Object> response = JacksonUtil.fromBytes(payload, new TypeReference<>() {});

        assertNotNull(response);
        assertEquals(response.size(), 9);
        assertEquals(response.get("gatewayMsgRateLimit"), profileConfiguration.getTransportGatewayMsgRateLimit());
        assertEquals(response.get("gatewayTelemetryMsgRateLimit"), profileConfiguration.getTransportGatewayTelemetryMsgRateLimit());
        assertEquals(response.get("gatewayTelemetryDataPointsRateLimit"), profileConfiguration.getTransportGatewayTelemetryDataPointsRateLimit());
        assertEquals(response.get("gatewayDeviceMsgRateLimit"), profileConfiguration.getTransportGatewayDeviceMsgRateLimit());
        assertEquals(response.get("gatewayDeviceTelemetryMsgRateLimit"), profileConfiguration.getTransportGatewayDeviceTelemetryMsgRateLimit());
        assertEquals(response.get("gatewayDeviceTelemetryDataPointsRateLimit"), profileConfiguration.getTransportGatewayDeviceTelemetryDataPointsRateLimit());
        assertEquals(response.get("maxPayloadSize"), maxPayloadSize);
        assertEquals(response.get("maxQueueSize"), maxQueueSize);
        assertEquals(response.get("payloadType"), TransportPayloadType.JSON.name());

        client.disconnect();
    }
}
