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
package org.thingsboard.server.transport.mqtt.sparkplug.rpc;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.transport.mqtt.sparkplug.AbstractMqttV5ClientSparkplugTest;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.exception.ThingsboardErrorCode.INVALID_ARGUMENTS;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType.DCMD;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType.NCMD;

@Slf4j
public abstract class AbstractMqttV5RpcSparkplugTest  extends AbstractMqttV5ClientSparkplugTest {

    private static final int metricBirthValue_Int32 = 123456;
    private static final String sparkplugRpcRequest = "{\"metricName\":\"" + metricBirthName_Int32 + "\",\"value\":" + metricBirthValue_Int32 + "}";

    @Test
    public void processClientNodeWithCorrectAccessTokenPublish_TwoWayRpc_Success() throws Exception {
        clientWithCorrectNodeAccessTokenWithNDEATH();
        connectionWithNBirth(metricBirthDataType_Int32, metricBirthName_Int32, nextInt32());
        Assert.assertTrue("Connection node is failed", client.isConnected());
        awaitForDeviceActorToReceiveSubscription(savedGateway.getId(), FeatureType.RPC, 1);
        String expected = "{\"result\":\"Success: " + SparkplugMessageType.NCMD.name() + "\"}";
        String actual = sendRPCSparkplug(NCMD.name(), sparkplugRpcRequest, savedGateway);
        await(alias + SparkplugMessageType.NCMD.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(expected, actual);
        Assert.assertEquals(metricBirthName_Int32, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertTrue(metricBirthValue_Int32 == mqttCallback.getMessageArrivedMetrics().get(0).getIntValue());
    }

    @Test
    public void processClientDeviceWithCorrectAccessTokenPublish_TwoWayRpc_Success() throws Exception {
        long ts = calendar.getTimeInMillis();
        List<Device> devices = connectClientWithCorrectAccessTokenWithNDEATHCreatedDevices(1, ts);
        awaitForDeviceActorToReceiveSubscription(devices.get(0).getId(), FeatureType.RPC, 1);
        String expected = "{\"result\":\"Success: " + DCMD.name() + "\"}";
        String actual = sendRPCSparkplug(DCMD.name() , sparkplugRpcRequest, devices.get(0));
        await(alias + DCMD.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(expected, actual);
        Assert.assertEquals(metricBirthName_Int32, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertTrue(metricBirthValue_Int32 == mqttCallback.getMessageArrivedMetrics().get(0).getIntValue());
    }

    @Test
    public void processClientDeviceWithCorrectAccessTokenPublishWithAlias_TwoWayRpc_Success() throws Exception {
        long ts = calendar.getTimeInMillis();
        List<Device> devices = connectClientWithCorrectAccessTokenWithNDEATHWithAliasCreatedDevices(ts);
        awaitForDeviceActorToReceiveSubscription(devices.get(0).getId(), FeatureType.RPC, 1);
        String expected = "{\"result\":\"Success: " + DCMD.name() + "\"}";
        String actual = sendRPCSparkplug(DCMD.name() , sparkplugRpcRequest, devices.get(0));
        await(alias + DCMD.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(expected, actual);
        Assert.assertFalse(mqttCallback.getMessageArrivedMetrics().get(0).hasName());
        Assert.assertTrue(mqttCallback.getMessageArrivedMetrics().get(0).hasAlias());
        Assert.assertTrue(2L == mqttCallback.getMessageArrivedMetrics().get(0).getAlias());
        Assert.assertTrue(metricBirthValue_Int32 == mqttCallback.getMessageArrivedMetrics().get(0).getIntValue());
    }

    @Test
    public void processClientNodeWithCorrectAccessTokenPublishWithAliasWithoutMetricName_TwoWayRpc_BAD_REQUEST_PARAMS() throws Exception {
        long ts = calendar.getTimeInMillis() - PUBLISH_TS_DELTA_MS;
        long value = bdSeq = 0;
        MqttException actualException = Assert.assertThrows(MqttException.class, () -> clientMqttV5ConnectWithNDEATH(ts, value, "",4L));
        String expectedMessage = "Server unavailable.";
        int expectedReasonCode = 136;
        Assert.assertEquals(expectedMessage, actualException.getMessage());
        Assert.assertEquals(expectedReasonCode, actualException.getReasonCode());
    }

    @Test
    public void processClientNodeWithCorrectAccessTokenPublish_TwoWayRpc_InvalidTypeMessage_INVALID_ARGUMENTS() throws Exception {
        clientWithCorrectNodeAccessTokenWithNDEATH();
        connectionWithNBirth(metricBirthDataType_Int32, metricBirthName_Int32, nextInt32());
        Assert.assertTrue("Connection node is failed", client.isConnected());
        awaitForDeviceActorToReceiveSubscription(savedGateway.getId(), FeatureType.RPC, 1);
        String invalidateTypeMessageName = "RCMD";
        String expected = "{\"result\":\"" + INVALID_ARGUMENTS + "\",\"error\":\"Failed to convert device RPC command to MQTT msg: " +
                invalidateTypeMessageName + "{\\\"metricName\\\":\\\"" + metricBirthName_Int32 + "\\\",\\\"value\\\":" + metricBirthValue_Int32 + "}\"}";
        String actual = sendRPCSparkplug(invalidateTypeMessageName, sparkplugRpcRequest, savedGateway);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void processClientNodeWithCorrectAccessTokenPublish_TwoWayRpc_InBirthNotHaveMetric_BAD_REQUEST_PARAMS() throws Exception {
        clientWithCorrectNodeAccessTokenWithNDEATH();
        connectionWithNBirth(metricBirthDataType_Int32, metricBirthName_Int32, nextInt32());
        Assert.assertTrue("Connection node is failed", client.isConnected());
        awaitForDeviceActorToReceiveSubscription(savedGateway.getId(), FeatureType.RPC, 1);
        String metricNameBad = metricBirthName_Int32 + "_Bad";
        String sparkplugRpcRequestBad = "{\"metricName\":\"" + metricNameBad + "\",\"value\":" + metricBirthValue_Int32 + "}";
        String expected = "{\"result\":\"BAD_REQUEST_PARAMS\",\"error\":\"Failed send To Node Rpc Request: " +
                DCMD.name() + ". This node does not have a metricName: [" + metricNameBad + "]\"}";
        String actual = sendRPCSparkplug(DCMD.name(), sparkplugRpcRequestBad, savedGateway);
        Assert.assertEquals(expected, actual);
     }

    private String sendRPCSparkplug(String nameTypeMessage, String keyValue, Device device) throws Exception {
        String setRpcRequest = "{\"method\": \"" + nameTypeMessage + "\", \"params\": " + keyValue + "}";
        return doPostAsync("/api/plugins/rpc/twoway/" + device.getId().getId(), setRpcRequest, String.class, status().isOk());
    }

}
