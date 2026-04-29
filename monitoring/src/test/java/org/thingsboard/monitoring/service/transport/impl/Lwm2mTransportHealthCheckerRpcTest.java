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
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.monitoring.client.TbClient;
import org.thingsboard.monitoring.config.transport.DeviceConfig;
import org.thingsboard.monitoring.config.transport.Lwm2mTransportMonitoringConfig;
import org.thingsboard.monitoring.config.transport.RpcCheckConfig;
import org.thingsboard.monitoring.config.transport.TransportMonitoringTarget;
import org.thingsboard.monitoring.data.ServiceFailureException;
import org.thingsboard.monitoring.service.MonitoringReporter;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.security.DeviceCredentials;

import java.net.SocketTimeoutException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Lwm2mTransportHealthCheckerRpcTest {

    private Lwm2mTransportMonitoringConfig config;
    private TransportMonitoringTarget target;
    private Lwm2mTransportHealthChecker checker;
    private TbClient tbClient;

    @BeforeEach
    void setUp() {
        config = new Lwm2mTransportMonitoringConfig();
        ReflectionTestUtils.invokeSetterMethod(config, "setRequestTimeoutMs", 6000, int.class);

        DeviceConfig device = new DeviceConfig();
        device.setId(UUID.randomUUID().toString());
        DeviceCredentials credentials = new DeviceCredentials();
        credentials.setCredentialsId("endpoint-1");
        device.setCredentials(credentials);

        target = new TransportMonitoringTarget();
        target.setBaseUrl("coap://localhost:5685");
        target.setDevice(device);

        tbClient = mock(TbClient.class);
        checker = new Lwm2mTransportHealthChecker(config, target);
        ReflectionTestUtils.setField(checker, "tbClient", tbClient);
        ReflectionTestUtils.setField(checker, "reporter", mock(MonitoringReporter.class));
    }

    @Test
    void doRpcCheckHappyPathReadsManufacturerResource() throws Exception {
        enableRpc();
        when(tbClient.handleTwoWayDeviceRPCRequest(any(DeviceId.class), any(JsonNode.class)))
                .thenReturn(new TextNode("Thingsboard"));
        ArgumentCaptor<JsonNode> bodyCaptor = ArgumentCaptor.forClass(JsonNode.class);

        checker.doRpcCheck();

        verify(tbClient).handleTwoWayDeviceRPCRequest(any(DeviceId.class), bodyCaptor.capture());
        JsonNode body = bodyCaptor.getValue();
        assertThat(body.get("method").asText()).isEqualTo("Read");
        assertThat(body.get("params").get("key").asText()).isEqualTo("/3/0/0");
        assertThat(body.get("timeout").asInt()).isEqualTo(6000);
    }

    @Test
    void doRpcCheckBlankResponseFailsWithRpcKey() {
        enableRpc();
        when(tbClient.handleTwoWayDeviceRPCRequest(any(DeviceId.class), any(JsonNode.class)))
                .thenReturn(new TextNode(""));

        assertThatThrownBy(() -> checker.doRpcCheck())
                .isInstanceOf(ServiceFailureException.class)
                .hasMessageContaining("blank result")
                .extracting(t -> ((ServiceFailureException) t).getServiceKey())
                .satisfies(key -> assertThat(key.toString()).endsWith("RPC"));
    }

    @Test
    void doRpcCheckNullResponseFailsWithRpcKey() {
        enableRpc();
        when(tbClient.handleTwoWayDeviceRPCRequest(any(DeviceId.class), any(JsonNode.class)))
                .thenReturn(null);

        assertThatThrownBy(() -> checker.doRpcCheck())
                .isInstanceOf(ServiceFailureException.class)
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
    void doRpcCheckUsesPerTargetTimeoutOverride() throws Exception {
        RpcCheckConfig rpc = new RpcCheckConfig();
        rpc.setEnabled(true);
        rpc.setRequestTimeoutMs(12000);
        target.setRpc(rpc);
        when(tbClient.handleTwoWayDeviceRPCRequest(any(DeviceId.class), any(JsonNode.class)))
                .thenReturn(new TextNode("ok"));
        ArgumentCaptor<JsonNode> bodyCaptor = ArgumentCaptor.forClass(JsonNode.class);

        checker.doRpcCheck();

        verify(tbClient).handleTwoWayDeviceRPCRequest(any(DeviceId.class), bodyCaptor.capture());
        assertThat(bodyCaptor.getValue().get("timeout").asInt()).isEqualTo(12000);
    }

    private void enableRpc() {
        RpcCheckConfig rpc = new RpcCheckConfig();
        rpc.setEnabled(true);
        target.setRpc(rpc);
    }

}
