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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.security.AccessValidator;
import org.thingsboard.server.transport.mqtt.MqttTestConfigProperties;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_RPC_REQUESTS_SUB_SHORT_JSON_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_RPC_REQUESTS_SUB_SHORT_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_RPC_REQUESTS_SUB_TOPIC;

@Slf4j
@DaoSqlTest
public class MqttServerSideRpcDefaultIntegrationTest extends AbstractMqttServerSideRpcIntegrationTest {

    @Before
    public void beforeTest() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("RPC test device")
                .gatewayName("RPC test gateway")
                .build();
        processBeforeTest(configProperties);
    }

    @Test
    public void testServerMqttOneWayRpcDeviceOffline() throws Exception {
        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"24\",\"value\": 1},\"timeout\": 6000}";
        String deviceId = savedDevice.getId().getId().toString();

        doPostAsync("/api/rpc/oneway/" + deviceId, setGpioRequest, String.class, status().is(504),
                asyncContextTimeoutToUseRpcPlugin);
    }

    @Test
    public void testServerMqttOneWayRpcDeviceDoesNotExist() throws Exception {
        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"25\",\"value\": 1}}";
        String nonExistentDeviceId = Uuids.timeBased().toString();

        String result = doPostAsync("/api/rpc/oneway/" + nonExistentDeviceId, setGpioRequest, String.class,
                status().isNotFound());
        Assert.assertEquals(AccessValidator.DEVICE_WITH_REQUESTED_ID_NOT_FOUND, result);
    }

    @Test
    public void testServerMqttTwoWayRpcDeviceOffline() throws Exception {
        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"27\",\"value\": 1},\"timeout\": 6000}";
        String deviceId = savedDevice.getId().getId().toString();

        doPostAsync("/api/rpc/twoway/" + deviceId, setGpioRequest, String.class, status().is(504),
                asyncContextTimeoutToUseRpcPlugin);
    }

    @Test
    public void testServerMqttTwoWayRpcDeviceDoesNotExist() throws Exception {
        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"28\",\"value\": 1}}";
        String nonExistentDeviceId = Uuids.timeBased().toString();

        String result = doPostAsync("/api/rpc/twoway/" + nonExistentDeviceId, setGpioRequest, String.class,
                status().isNotFound());
        Assert.assertEquals(AccessValidator.DEVICE_WITH_REQUESTED_ID_NOT_FOUND, result);
    }

    @Test
    public void testServerMqttOneWayRpc() throws Exception {
        processOneWayRpcTest(DEVICE_RPC_REQUESTS_SUB_TOPIC);
    }

    @Test
    public void testServerMqttOneWayRpcOnShortTopic() throws Exception {
        processOneWayRpcTest(DEVICE_RPC_REQUESTS_SUB_SHORT_TOPIC);
    }

    @Test
    public void testServerMqttOneWayRpcOnShortJsonTopic() throws Exception {
        processOneWayRpcTest(DEVICE_RPC_REQUESTS_SUB_SHORT_JSON_TOPIC);
    }

    @Test
    public void testServerMqttTwoWayRpc() throws Exception {
        processJsonTwoWayRpcTest(DEVICE_RPC_REQUESTS_SUB_TOPIC);
    }

    @Test
    public void testServerMqttTwoWayRpcOnShortTopic() throws Exception {
        processJsonTwoWayRpcTest(DEVICE_RPC_REQUESTS_SUB_SHORT_TOPIC);
    }

    @Test
    public void testServerMqttTwoWayRpcOnShortJsonTopic() throws Exception {
        processJsonTwoWayRpcTest(DEVICE_RPC_REQUESTS_SUB_SHORT_JSON_TOPIC);
    }

    @Test
    public void testGatewayServerMqttOneWayRpc() throws Exception {
        processJsonOneWayRpcTestGateway("Gateway Device OneWay RPC");
    }

    @Test
    public void testGatewayServerMqttTwoWayRpc() throws Exception {
        processJsonTwoWayRpcTestGateway("Gateway Device TwoWay RPC");
    }

}
