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
package org.thingsboard.server.transport.mqtt.mqttv3.rpc;

import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.mqtt.MqttTestConfigProperties;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestCallback;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestClient;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestSubscribeOnTopicCallback;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.GATEWAY_CONNECT_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.GATEWAY_RPC_TOPIC;

@Slf4j
@DaoSqlTest
@TestPropertySource(properties = {
        "actors.rpc.close_session_on_rpc_delivery_timeout=true",
        "transport.mqtt.timeout=100",
})
public class MqttGatewayServerSideRpcDeliveryTimeoutIntegrationTest extends AbstractMqttServerSideRpcIntegrationTest {

    @Before
    public void beforeTest() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("RPC timeout test device")
                .gatewayName("RPC timeout test gateway")
                .transportPayloadType(TransportPayloadType.JSON)
                .build();
        processBeforeTest(configProperties);
    }

    @Test
    public void testGatewayPersistentRpcTimeoutWhenNoPuback() throws Exception {
        MqttTestClient client = new MqttTestClient();
        client.enableManualAcks();
        client.connectAndWait(gatewayAccessToken);

        String deviceName = "Gateway Device Persistent RPC Timeout Json";
        String connectPayload = "{\"device\": \"" + deviceName + "\", \"type\": \"" + TransportPayloadType.JSON.name() + "\"}";
        client.publish(GATEWAY_CONNECT_TOPIC, connectPayload.getBytes());

        Device device = doExecuteWithRetriesAndInterval(() -> getDeviceByName(deviceName), 20, 100);
        assertNotNull(device);

        MqttTestCallback callback = new MqttTestSubscribeOnTopicCallback(GATEWAY_RPC_TOPIC);
        client.setCallback(callback);
        // QoS1 downlink so the gateway would need to PUBACK — but manual acks withhold it.
        // Use the two-step form (client.subscribeAndWait + awaitForDeviceActorToReceiveSubscription)
        // rather than subscribeAndWait(client, topic, deviceId, featureType, qos) because the gateway
        // device actor already has one RPC subscription entry created when the CONNECT message was
        // processed above, so subscribeAndWait (which waits for count+1) would wait for 2 forever.
        client.subscribeAndWait(GATEWAY_RPC_TOPIC, MqttQoS.AT_LEAST_ONCE);
        awaitForDeviceActorToReceiveSubscription(device.getId(), FeatureType.RPC, 1);

        long expirationTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1);
        String rpcRequest = "{\"method\":\"toggle_gpio\",\"params\":{\"pin\":1},\"persistent\":true,\"retries\":0,\"expirationTime\":" + expirationTime + "}";
        String response = doPostAsync("/api/rpc/twoway/" + device.getId().getId().toString(), rpcRequest, String.class, status().isOk());
        String rpcId = JacksonUtil.toJsonNode(response).get("rpcId").asText();

        callback.getSubscribeLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // No PUBACK is ever sent. The gateway awaiting-ack scheduler emits TIMEOUT after
        // transport.mqtt.timeout (100 ms); the server re-queues the RPC to QUEUED.
        Rpc rpc = doExecuteWithRetriesAndInterval(() -> {
            Rpc current = doGet("/api/rpc/persistent/" + rpcId, Rpc.class);
            return RpcStatus.QUEUED.equals(current.getStatus()) ? current : null;
        }, 50, 100);
        assertNotNull(rpc);
        assertEquals(RpcStatus.QUEUED, rpc.getStatus());

        client.disconnect();
    }

    private Device getDeviceByName(String deviceName) throws Exception {
        return doGet("/api/tenant/devices?deviceName=" + deviceName, Device.class);
    }

}
