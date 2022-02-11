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
package org.thingsboard.server.transport.mqtt.rpc;

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.device.profile.MqttTopics;

@Slf4j
public abstract class AbstractMqttServerSideRpcBackwardCompatibilityIntegrationTest extends AbstractMqttServerSideRpcIntegrationTest {

    @After
    public void afterTest() throws Exception {
        super.processAfterTest();
    }

    @Test
    public void testServerMqttOneWayRpcWithEnabledJsonCompatibilityAndJsonDownlinks() throws Exception {
        super.processBeforeTest("RPC test device", "RPC test gateway", TransportPayloadType.PROTOBUF, null, null, null, null, null, RPC_REQUEST_PROTO_SCHEMA, null, null, DeviceProfileProvisionType.DISABLED, true, true);
        processOneWayRpcTest(MqttTopics.DEVICE_RPC_REQUESTS_SUB_TOPIC);
    }

    @Test
    public void testServerMqttOneWayRpcOnShortTopicWithEnabledJsonCompatibilityAndJsonDownlinks() throws Exception {
        super.processBeforeTest("RPC test device", "RPC test gateway", TransportPayloadType.PROTOBUF, null, null, null, null, null, RPC_REQUEST_PROTO_SCHEMA, null, null, DeviceProfileProvisionType.DISABLED, true, true);
        processOneWayRpcTest(MqttTopics.DEVICE_RPC_REQUESTS_SUB_SHORT_TOPIC);
    }

    @Test
    public void testServerMqttOneWayRpcOnShortProtoTopicWithEnabledJsonCompatibilityAndJsonDownlinks() throws Exception {
        super.processBeforeTest("RPC test device", "RPC test gateway", TransportPayloadType.PROTOBUF, null, null, null, null, null, RPC_REQUEST_PROTO_SCHEMA, null, null, DeviceProfileProvisionType.DISABLED, true, true);
        processOneWayRpcTest(MqttTopics.DEVICE_RPC_REQUESTS_SUB_SHORT_PROTO_TOPIC);
    }

    @Test
    public void testServerMqttTwoWayRpcWithEnabledJsonCompatibility() throws Exception {
        super.processBeforeTest("RPC test device", "RPC test gateway", TransportPayloadType.PROTOBUF, null, null, null, null, null, RPC_REQUEST_PROTO_SCHEMA, null, null, DeviceProfileProvisionType.DISABLED, true, false);
        processProtoTwoWayRpcTest(MqttTopics.DEVICE_RPC_REQUESTS_SUB_TOPIC);
    }

    @Test
    public void testServerMqttTwoWayRpcWithEnabledJsonCompatibilityAndJsonDownlinks() throws Exception {
        super.processBeforeTest("RPC test device", "RPC test gateway", TransportPayloadType.PROTOBUF, null, null, null, null, null, RPC_REQUEST_PROTO_SCHEMA, null, null, DeviceProfileProvisionType.DISABLED, true, true);
        processJsonTwoWayRpcTest(MqttTopics.DEVICE_RPC_REQUESTS_SUB_TOPIC);
    }

    @Test
    public void testServerMqttTwoWayRpcOnShortTopic() throws Exception {
        super.processBeforeTest("RPC test device", "RPC test gateway", TransportPayloadType.PROTOBUF, null, null, null, null, null, RPC_REQUEST_PROTO_SCHEMA, null, null, DeviceProfileProvisionType.DISABLED, true, true);
        processProtoTwoWayRpcTest(MqttTopics.DEVICE_RPC_REQUESTS_SUB_SHORT_TOPIC);
    }

    @Test
    public void testServerMqttTwoWayRpcOnShortProtoTopicWithEnabledJsonCompatibilityAndJsonDownlinks() throws Exception {
        super.processBeforeTest("RPC test device", "RPC test gateway", TransportPayloadType.PROTOBUF, null, null, null, null, null, RPC_REQUEST_PROTO_SCHEMA, null, null, DeviceProfileProvisionType.DISABLED, true, true);
        processProtoTwoWayRpcTest(MqttTopics.DEVICE_RPC_REQUESTS_SUB_SHORT_PROTO_TOPIC);
    }

    @Test
    public void testServerMqttTwoWayRpcOnShortJsonTopicWithEnabledJsonCompatibilityAndJsonDownlinks() throws Exception {
        super.processBeforeTest("RPC test device", "RPC test gateway", TransportPayloadType.PROTOBUF, null, null, null, null, null, RPC_REQUEST_PROTO_SCHEMA, null, null, DeviceProfileProvisionType.DISABLED, true, true);
        processJsonTwoWayRpcTest(MqttTopics.DEVICE_RPC_REQUESTS_SUB_SHORT_JSON_TOPIC);
    }

    @Test
    public void testGatewayServerMqttOneWayRpcWithEnabledJsonCompatibilityAndJsonDownlinks() throws Exception {
        super.processBeforeTest("RPC test device", "RPC test gateway", TransportPayloadType.PROTOBUF, null, null, null, null, null, RPC_REQUEST_PROTO_SCHEMA, null, null, DeviceProfileProvisionType.DISABLED, true, true);
        processProtoOneWayRpcTestGateway("Gateway Device OneWay RPC Proto");
    }

    @Test
    public void testGatewayServerMqttTwoWayRpcWithEnabledJsonCompatibilityAndJsonDownlinks() throws Exception {
        super.processBeforeTest("RPC test device", "RPC test gateway", TransportPayloadType.PROTOBUF, null, null, null, null, null, RPC_REQUEST_PROTO_SCHEMA, null, null, DeviceProfileProvisionType.DISABLED, true, true);
        processProtoTwoWayRpcTestGateway("Gateway Device TwoWay RPC Proto");
    }

}
