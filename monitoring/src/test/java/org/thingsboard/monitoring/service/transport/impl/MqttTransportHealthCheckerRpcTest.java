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
package org.thingsboard.monitoring.service.transport.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.monitoring.client.TbClient;
import org.thingsboard.monitoring.config.transport.DeviceConfig;
import org.thingsboard.monitoring.config.transport.MqttTransportMonitoringConfig;
import org.thingsboard.monitoring.config.transport.RpcCheckConfig;
import org.thingsboard.monitoring.config.transport.TransportMonitoringTarget;
import org.thingsboard.monitoring.data.ServiceFailureException;
import org.thingsboard.monitoring.service.MonitoringReporter;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.security.DeviceCredentials;

import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MqttTransportHealthCheckerRpcTest {

    private MqttTransportMonitoringConfig config;
    private TransportMonitoringTarget target;
    private MqttTransportHealthChecker checker;
    private TbClient tbClient;
    private MqttClient mqttClient;

    @BeforeEach
    void setUp() throws Exception {
        config = new MqttTransportMonitoringConfig();
        ReflectionTestUtils.setField(config, "qos", 1);
        ReflectionTestUtils.invokeSetterMethod(config, "setRequestTimeoutMs", 4000, int.class);

        DeviceConfig device = new DeviceConfig();
        device.setId(UUID.randomUUID().toString());
        device.setName("test-device");
        DeviceCredentials credentials = new DeviceCredentials();
        credentials.setCredentialsId("token-123");
        device.setCredentials(credentials);

        target = new TransportMonitoringTarget();
        target.setBaseUrl("tcp://localhost:1883");
        target.setDevice(device);

        tbClient = mock(TbClient.class);
        mqttClient = mock(MqttClient.class);
        lenient().when(mqttClient.isConnected()).thenReturn(true);

        checker = new MqttTransportHealthChecker(config, target);
        checker.mqttClient = mqttClient;
        ReflectionTestUtils.setField(checker, "tbClient", tbClient);
        ReflectionTestUtils.setField(checker, "reporter", mock(MonitoringReporter.class));
    }

    @Test
    void doRpcCheckHappyPath() throws Exception {
        enableRpc();
        ArgumentCaptor<JsonNode> bodyCaptor = ArgumentCaptor.forClass(JsonNode.class);
        when(tbClient.handleTwoWayDeviceRPCRequest(any(DeviceId.class), bodyCaptor.capture()))
                .thenAnswer(invocation -> {
                    JsonNode params = bodyCaptor.getValue().get("params");
                    return params;
                });

        checker.doRpcCheck();

        JsonNode sent = bodyCaptor.getValue();
        assertThat(sent.get("method").asText()).isEqualTo("monitoringCheck");
        assertThat(sent.get("timeout").asInt()).isEqualTo(4000);
        assertThat(sent.get("params").get("value").asText()).isNotBlank();
    }

    @Test
    void doRpcCheckValueMismatchThrowsServiceFailureWithRpcKey() {
        enableRpc();
        when(tbClient.handleTwoWayDeviceRPCRequest(any(DeviceId.class), any(JsonNode.class)))
                .thenReturn(JacksonUtil.newObjectNode().put("value", "wrong"));

        assertThatThrownBy(() -> checker.doRpcCheck())
                .isInstanceOf(ServiceFailureException.class)
                .hasMessageContaining("RPC echo mismatch")
                .extracting(t -> ((ServiceFailureException) t).getServiceKey())
                .satisfies(key -> assertThat(key.toString()).endsWith("RPC"));
    }

    @Test
    void doRpcCheckRestExceptionWraps() {
        enableRpc();
        when(tbClient.handleTwoWayDeviceRPCRequest(any(DeviceId.class), any(JsonNode.class)))
                .thenThrow(new RuntimeException("400 Bad Request"));

        assertThatThrownBy(() -> checker.doRpcCheck())
                .isInstanceOf(ServiceFailureException.class)
                .hasCauseInstanceOf(RuntimeException.class)
                .extracting(t -> ((ServiceFailureException) t).getServiceKey())
                .satisfies(key -> assertThat(key.toString()).endsWith("RPC"));
    }

    @Test
    void doRpcCheckTimeoutWraps() {
        enableRpc();
        when(tbClient.handleTwoWayDeviceRPCRequest(any(DeviceId.class), any(JsonNode.class)))
                .thenThrow(new ResourceAccessException("read timed out", new SocketTimeoutException()));

        assertThatThrownBy(() -> checker.doRpcCheck())
                .isInstanceOf(ServiceFailureException.class)
                .hasMessageContaining("read timed out");
    }

    @Test
    void doRpcCheckIsNoOpWhenRpcDisabled() throws Exception {
        // target.rpc not set → isRpcEnabled() == false

        checker.doRpcCheck();

        verify(tbClient, never()).handleTwoWayDeviceRPCRequest(any(), any());
    }

    @Test
    void initClientSubscribesToRpcRequestTopicWhenRpcEnabled() throws Exception {
        enableRpc();

        checker.initClient();

        verify(mqttClient).subscribe(eq("v1/devices/me/rpc/request/+"), eq(1), any(IMqttMessageListener.class));
    }

    @Test
    void initClientSkipsSubscriptionWhenRpcDisabled() throws Exception {
        checker.initClient();

        verify(mqttClient, never()).subscribe(anyString(), anyInt(), any(IMqttMessageListener.class));
    }

    @Test
    void initClientSubscribesOnlyOnce() throws Exception {
        enableRpc();

        checker.initClient();
        checker.initClient();

        verify(mqttClient, org.mockito.Mockito.times(1))
                .subscribe(eq("v1/devices/me/rpc/request/+"), eq(1), any(IMqttMessageListener.class));
    }

    @Test
    void echoRpcRequestPublishesParamsBackToResponseTopic() throws Exception {
        enableRpc();
        ObjectNode body = JacksonUtil.newObjectNode();
        body.put("method", "monitoringCheck");
        ObjectNode params = JacksonUtil.newObjectNode().put("value", "uuid-42");
        body.set("params", params);
        MqttMessage incoming = new MqttMessage(JacksonUtil.toString(body).getBytes(StandardCharsets.UTF_8));

        checker.echoRpcRequest("v1/devices/me/rpc/request/77", incoming);

        ArgumentCaptor<MqttMessage> publishedCaptor = ArgumentCaptor.forClass(MqttMessage.class);
        verify(mqttClient).publish(eq("v1/devices/me/rpc/response/77"), publishedCaptor.capture());
        JsonNode echoed = JacksonUtil.toJsonNode(new String(publishedCaptor.getValue().getPayload(), StandardCharsets.UTF_8));
        assertThat(echoed.get("value").asText()).isEqualTo("uuid-42");
    }

    private void enableRpc() {
        RpcCheckConfig rpc = new RpcCheckConfig();
        rpc.setEnabled(true);
        target.setRpc(rpc);
    }

}
