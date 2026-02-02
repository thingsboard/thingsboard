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
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.mqtt.MqttTestConfigProperties;

@Slf4j
@DaoSqlTest
@TestPropertySource(properties = {
        "actors.rpc.submit_strategy=SEQUENTIAL_ON_RESPONSE_FROM_DEVICE",
})
public class MqttServerSideRpcSequenceOnResponseIntegrationTest extends AbstractMqttServerSideRpcIntegrationTest {

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
