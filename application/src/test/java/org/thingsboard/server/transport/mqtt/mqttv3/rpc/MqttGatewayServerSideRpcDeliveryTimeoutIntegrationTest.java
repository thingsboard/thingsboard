// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.mqtt.mqttv3.rpc;

import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.mqtt.MqttTestConfigProperties;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        // manualAcks = true → the gateway client never PUBACKs the QoS1 downlink.
        GatewayRpcSession session = connectGatewayAndSubscribeForRpc("Gateway Device Persistent RPC Timeout Json", true);

        long expirationTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1);
        String rpcRequest = "{\"method\":\"toggle_gpio\",\"params\":{\"pin\":1},\"persistent\":true,\"retries\":0,\"expirationTime\":" + expirationTime + "}";
        String response = doPostAsync("/api/rpc/twoway/" + session.device().getId().getId().toString(), rpcRequest, String.class, status().isOk());
        String rpcId = JacksonUtil.toJsonNode(response).get("rpcId").asText();

        session.callback().getSubscribeLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        // Confirm the downlink arrived at QoS1 — a QoS0 downlink would produce no PUBACK path at all,
        // so the timeout would fire for the wrong reason and mask a regression.
        assertEquals(MqttQoS.AT_LEAST_ONCE.value(), session.callback().getMessageArrivedQoS());

        // No PUBACK is ever sent. The gateway awaiting-ack scheduler emits TIMEOUT after
        // transport.mqtt.timeout (100 ms); the RPC status reverts to QUEUED (re-queued via the
        // close_session_on_rpc_delivery_timeout path).
        awaitRpcStatus(rpcId, RpcStatus.QUEUED, 50, 100);

        session.client().disconnect();
    }

}
