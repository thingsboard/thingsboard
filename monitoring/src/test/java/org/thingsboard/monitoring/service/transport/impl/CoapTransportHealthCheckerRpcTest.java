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
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.monitoring.client.TbClient;
import org.thingsboard.monitoring.config.transport.CoapTransportMonitoringConfig;
import org.thingsboard.monitoring.config.transport.DeviceConfig;
import org.thingsboard.monitoring.config.transport.RpcCheckConfig;
import org.thingsboard.monitoring.config.transport.RpcInfo;
import org.thingsboard.monitoring.config.transport.TransportMonitoringTarget;
import org.thingsboard.monitoring.data.ServiceFailureException;
import org.thingsboard.monitoring.service.MonitoringReporter;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.security.DeviceCredentials;

import java.net.SocketTimeoutException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CoapTransportHealthCheckerRpcTest {

    private CoapTransportMonitoringConfig config;
    private TransportMonitoringTarget target;
    private TbClient tbClient;
    private CoapClient mockRpcCoapClient;
    private CoapObserveRelation mockObserveRelation;
    private TestableCoapChecker checker;

    @BeforeEach
    void setUp() {
        config = new CoapTransportMonitoringConfig();
        ReflectionTestUtils.invokeSetterMethod(config, "setRequestTimeoutMs", 4000, int.class);

        DeviceConfig device = new DeviceConfig();
        device.setId(UUID.randomUUID().toString());
        DeviceCredentials credentials = new DeviceCredentials();
        credentials.setCredentialsId("token-coap");
        device.setCredentials(credentials);

        target = new TransportMonitoringTarget();
        target.setBaseUrl("coap://localhost");
        target.setDevice(device);

        tbClient = mock(TbClient.class);
        mockRpcCoapClient = mock(CoapClient.class);
        mockObserveRelation = mock(CoapObserveRelation.class);
        lenient().when(mockRpcCoapClient.observe(any(CoapHandler.class))).thenReturn(mockObserveRelation);

        checker = new TestableCoapChecker(config, target);
        // pre-set both clients so initClient() does not call new CoapClient(...)
        ReflectionTestUtils.setField(checker, "coapClient", mock(CoapClient.class));
        checker.rpcCoapClient = mockRpcCoapClient;
        ReflectionTestUtils.setField(checker, "tbClient", tbClient);
        ReflectionTestUtils.setField(checker, "reporter", mock(MonitoringReporter.class));
    }

    @Test
    void doRpcCheckHappyPath() throws Exception {
        enableRpc();
        AtomicReference<JsonNode> capturedBody = new AtomicReference<>();
        when(tbClient.handleTwoWayDeviceRPCRequest(any(DeviceId.class), any(JsonNode.class)))
                .thenAnswer(inv -> {
                    JsonNode b = inv.getArgument(1);
                    capturedBody.set(b);
                    return b.get("params");
                });

        checker.doRpcCheck();

        assertThat(capturedBody.get().get("method").asText()).isEqualTo("monitoringCheck");
    }

    @Test
    void doRpcCheckValueMismatchUsesRpcKey() {
        enableRpc();
        when(tbClient.handleTwoWayDeviceRPCRequest(any(DeviceId.class), any(JsonNode.class)))
                .thenReturn(JacksonUtil.newObjectNode().put("value", "wrong"));

        assertThatThrownBy(() -> checker.doRpcCheck())
                .isInstanceOf(ServiceFailureException.class)
                .extracting(t -> ((ServiceFailureException) t).getServiceKey())
                .satisfies(key -> assertThat(key.toString()).endsWith(RpcInfo.RPC_SUFFIX));
    }

    @Test
    void doRpcCheckRestExceptionWraps() {
        enableRpc();
        when(tbClient.handleTwoWayDeviceRPCRequest(any(DeviceId.class), any(JsonNode.class)))
                .thenThrow(new RuntimeException("400 Bad Request"));

        assertThatThrownBy(() -> checker.doRpcCheck())
                .isInstanceOf(ServiceFailureException.class)
                .hasCauseInstanceOf(RuntimeException.class);
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
        checker.doRpcCheck();

        verify(tbClient, never()).handleTwoWayDeviceRPCRequest(any(), any());
    }

    @Test
    void initClientStartsObserveWhenRpcEnabled() throws Exception {
        enableRpc();

        checker.initClient();

        verify(mockRpcCoapClient).observe(any(CoapHandler.class));
        assertThat(checker.rpcObserveRelation).isSameAs(mockObserveRelation);
    }

    @Test
    void initClientSkipsObserveWhenRpcDisabled() throws Exception {
        checker.initClient();

        verify(mockRpcCoapClient, never()).observe(any(CoapHandler.class));
    }

    @Test
    void initClientObserveIsIdempotent() throws Exception {
        enableRpc();

        checker.initClient();
        checker.initClient();

        verify(mockRpcCoapClient, org.mockito.Mockito.times(1)).observe(any(CoapHandler.class));
    }

    @Test
    void destroyClientCancelsObserveAndShutsDownRpcClient() throws Exception {
        enableRpc();
        checker.initClient();

        checker.destroyClient();

        verify(mockObserveRelation).proactiveCancel();
        verify(mockRpcCoapClient).shutdown();
        assertThat(checker.rpcObserveRelation).isNull();
        assertThat(checker.rpcCoapClient).isNull();
    }

    @Test
    void handleRpcNotificationEchoesParamsBackToResponseEndpoint() {
        enableRpc();
        CoapResponse response = mock(CoapResponse.class);
        when(response.getResponseText()).thenReturn(
                "{\"id\":42,\"method\":\"monitoringCheck\",\"params\":{\"value\":\"uuid-42\"}}");

        checker.handleRpcNotification(response);

        assertThat(checker.capturedResponseUri).isEqualTo("coap://localhost/api/v1/token-coap/rpc/42");
        JsonNode echoed = JacksonUtil.toJsonNode(checker.capturedResponsePayload);
        assertThat(echoed.get("value").asText()).isEqualTo("uuid-42");
    }

    @Test
    void handleRpcNotificationIgnoresEmptyResponse() {
        enableRpc();
        CoapResponse response = mock(CoapResponse.class);
        when(response.getResponseText()).thenReturn("");

        checker.handleRpcNotification(response);

        assertThat(checker.capturedResponseUri).isNull();
    }

    @Test
    void handleRpcNotificationIgnoresMalformedRpcWithoutId() {
        enableRpc();
        CoapResponse response = mock(CoapResponse.class);
        when(response.getResponseText()).thenReturn("{\"method\":\"x\"}");

        checker.handleRpcNotification(response);

        assertThat(checker.capturedResponseUri).isNull();
    }

    private void enableRpc() {
        RpcCheckConfig rpc = new RpcCheckConfig();
        rpc.setEnabled(true);
        target.setRpc(rpc);
    }

    private static final class TestableCoapChecker extends CoapTransportHealthChecker {
        String capturedResponseUri;
        String capturedResponsePayload;

        TestableCoapChecker(CoapTransportMonitoringConfig config, TransportMonitoringTarget target) {
            super(config, target);
        }

        @Override
        void postRpcResponse(String uri, String payload) {
            capturedResponseUri = uri;
            capturedResponsePayload = payload;
        }
    }

}
