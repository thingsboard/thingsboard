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
package org.thingsboard.monitoring.service.rpc.impl;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.monitoring.config.rpc.MqttRpcMonitoringConfig;
import org.thingsboard.monitoring.config.rpc.RpcMonitoringTarget;
import org.thingsboard.monitoring.service.MonitoringReporter;
import org.thingsboard.monitoring.util.TbStopWatch;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MqttRpcHealthCheckerTest {

    @Mock MqttClient mqttClient;
    @Mock IMqttToken connectToken;

    TestMqttRpcHealthChecker checker;

    @BeforeEach
    void setUp() throws Exception {
        MqttRpcMonitoringConfig config = new MqttRpcMonitoringConfig();
        config.setRequestTimeoutMs(5000);
        config.setQos(1);

        RpcMonitoringTarget target = new RpcMonitoringTarget();
        target.setBaseUrl("tcp://localhost:1883");
        target.setDeviceId(UUID.randomUUID().toString());
        target.setAccessToken("my-access-token");
        target.setLabel("test-shard");

        checker = new TestMqttRpcHealthChecker(config, target, mqttClient);
        // Inject no-op reporter and stopWatch so check() doesn't NPE if called
        ReflectionTestUtils.setField(checker, "reporter", mock(MonitoringReporter.class));
        ReflectionTestUtils.setField(checker, "stopWatch", mock(TbStopWatch.class));

        when(connectToken.getException()).thenReturn(null);
        when(mqttClient.connectWithResult(any())).thenReturn(connectToken);
        when(mqttClient.isConnected()).thenReturn(false);
    }

    @Test
    void initClient_connectsWithAccessTokenAsUsername() throws Exception {
        checker.initClient();

        ArgumentCaptor<MqttConnectOptions> optionsCaptor = ArgumentCaptor.forClass(MqttConnectOptions.class);
        verify(mqttClient).connectWithResult(optionsCaptor.capture());
        assertThat(optionsCaptor.getValue().getUserName()).isEqualTo("my-access-token");
    }

    @Test
    void initClient_setsConnectionTimeout() throws Exception {
        checker.initClient();

        ArgumentCaptor<MqttConnectOptions> optionsCaptor = ArgumentCaptor.forClass(MqttConnectOptions.class);
        verify(mqttClient).connectWithResult(optionsCaptor.capture());
        assertThat(optionsCaptor.getValue().getConnectionTimeout()).isEqualTo(5); // 5000ms / 1000
    }

    @Test
    void initClient_subscribesToRpcRequestTopic() throws Exception {
        checker.initClient();

        verify(mqttClient).subscribe(eq("v1/devices/me/rpc/request/+"), eq(1), any(IMqttMessageListener.class));
    }

    @Test
    void initClient_isIdempotentWhenAlreadyConnected() throws Exception {
        checker.initClient();
        when(mqttClient.isConnected()).thenReturn(true);
        checker.initClient();

        verify(mqttClient, times(1)).connectWithResult(any());
        verify(mqttClient, times(1)).subscribe(any(), anyInt(), any(IMqttMessageListener.class));
    }

    @Test
    void rpcCallback_echosParamsToResponseTopic() throws Exception {
        checker.initClient();

        ArgumentCaptor<IMqttMessageListener> listenerCaptor = ArgumentCaptor.forClass(IMqttMessageListener.class);
        verify(mqttClient).subscribe(any(), anyInt(), listenerCaptor.capture());
        IMqttMessageListener listener = listenerCaptor.getValue();

        MqttMessage incoming = new MqttMessage(
                "{\"method\":\"monitoringCheck\",\"params\":{\"value\":\"abc-123\"}}".getBytes());
        listener.messageArrived("v1/devices/me/rpc/request/42", incoming);

        ArgumentCaptor<MqttMessage> responseCaptor = ArgumentCaptor.forClass(MqttMessage.class);
        verify(mqttClient).publish(eq("v1/devices/me/rpc/response/42"), responseCaptor.capture());
        assertThat(new String(responseCaptor.getValue().getPayload())).isEqualTo("{\"value\":\"abc-123\"}");
    }

    @Test
    void rpcCallback_extractsRequestIdFromTopic() throws Exception {
        checker.initClient();

        ArgumentCaptor<IMqttMessageListener> listenerCaptor = ArgumentCaptor.forClass(IMqttMessageListener.class);
        verify(mqttClient).subscribe(any(), anyInt(), listenerCaptor.capture());

        MqttMessage incoming = new MqttMessage(
                "{\"method\":\"monitoringCheck\",\"params\":{\"value\":\"x\"}}".getBytes());
        listenerCaptor.getValue().messageArrived("v1/devices/me/rpc/request/99", incoming);

        verify(mqttClient).publish(eq("v1/devices/me/rpc/response/99"), any());
    }

    @Test
    void rpcCallback_usesConfiguredQos() throws Exception {
        checker.initClient();

        ArgumentCaptor<IMqttMessageListener> listenerCaptor = ArgumentCaptor.forClass(IMqttMessageListener.class);
        verify(mqttClient).subscribe(any(), anyInt(), listenerCaptor.capture());

        MqttMessage incoming = new MqttMessage(
                "{\"method\":\"monitoringCheck\",\"params\":{\"value\":\"v\"}}".getBytes());
        listenerCaptor.getValue().messageArrived("v1/devices/me/rpc/request/7", incoming);

        ArgumentCaptor<MqttMessage> responseCaptor = ArgumentCaptor.forClass(MqttMessage.class);
        verify(mqttClient).publish(any(), responseCaptor.capture());
        assertThat(responseCaptor.getValue().getQos()).isEqualTo(1);
    }

    @Test
    void destroyClient_disconnectsAndClearsClient() throws Exception {
        checker.initClient();
        checker.destroyClient();

        verify(mqttClient).disconnect();
        // A second destroyClient call should be a no-op (client is null)
        checker.destroyClient();
        verify(mqttClient, times(1)).disconnect();
    }

    @Test
    void destroyClient_beforeInit_isNoOp() throws Exception {
        checker.destroyClient();
        verify(mqttClient, never()).disconnect();
    }

    // Subclass that injects a mock MqttClient instead of creating a real one
    private static class TestMqttRpcHealthChecker extends MqttRpcHealthChecker {
        private final MqttClient mockClient;

        TestMqttRpcHealthChecker(MqttRpcMonitoringConfig config, RpcMonitoringTarget target, MqttClient mockClient) {
            super(config, target);
            this.mockClient = mockClient;
        }

        @Override
        protected MqttClient createMqttClient(String brokerUrl, String clientId) throws MqttException {
            return mockClient;
        }
    }

}
