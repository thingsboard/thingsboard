// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.mqtt.mqttv3.rpc;

import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.mqtt.MqttTestConfigProperties;

@Slf4j
@DaoSqlTest
@TestPropertySource(properties = {
        "actors.rpc.submit_strategy=SEQUENTIAL_ON_ACK_FROM_DEVICE",
})
public class MqttServerSideRpcSequenceOnAckIntegrationTest extends AbstractMqttServerSideRpcIntegrationTest {

    @Before
    public void beforeTest() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("RPC test device")
                .gatewayName("RPC test gateway")
                .build();
        processBeforeTest(configProperties);
    }

    @Test
    public void testSequenceServerMqttOneWayRpcQoSAtMostOnce() throws Exception {
        processSequenceOneWayRpcTest(MqttQoS.AT_MOST_ONCE);
    }

    @Test
    public void testSequenceServerMqttOneWayRpcQoSAtLeastOnce() throws Exception {
        processSequenceOneWayRpcTest(MqttQoS.AT_LEAST_ONCE);
    }

    @Test
    public void testSequenceServerMqttTwoWayRpcQoSAtMostOnce() throws Exception {
        processSequenceTwoWayRpcTest(MqttQoS.AT_MOST_ONCE);
    }

    @Test
    public void testSequenceServerMqttTwoWayRpcQoSAtLeastOnce() throws Exception {
        processSequenceTwoWayRpcTest(MqttQoS.AT_LEAST_ONCE);
    }

    @Test
    public void testSequenceServerMqttTwoWayRpcQoSAtMostOnceWithManualAcksEnabled() throws Exception {
        processSequenceTwoWayRpcTest(MqttQoS.AT_MOST_ONCE, true);
    }

    @Test
    public void testSequenceServerMqttTwoWayRpcQoSAtLeastOnceWithoutManualAcksEnabled() throws Exception {
        processSequenceTwoWayRpcTest(MqttQoS.AT_LEAST_ONCE, true);
    }

}
