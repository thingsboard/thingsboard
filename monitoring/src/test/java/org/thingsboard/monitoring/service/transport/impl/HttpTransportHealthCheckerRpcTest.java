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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.monitoring.client.TbClient;
import org.thingsboard.monitoring.config.transport.DeviceConfig;
import org.thingsboard.monitoring.config.transport.HttpTransportMonitoringConfig;
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
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HttpTransportHealthCheckerRpcTest {

    private HttpTransportMonitoringConfig config;
    private TransportMonitoringTarget target;
    private HttpTransportHealthChecker checker;
    private TbClient tbClient;
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        config = new HttpTransportMonitoringConfig();
        ReflectionTestUtils.invokeSetterMethod(config, "setRequestTimeoutMs", 4000, int.class);

        DeviceConfig device = new DeviceConfig();
        device.setId(UUID.randomUUID().toString());
        DeviceCredentials credentials = new DeviceCredentials();
        credentials.setCredentialsId("token-abc");
        device.setCredentials(credentials);

        target = new TransportMonitoringTarget();
        target.setBaseUrl("http://localhost:8080");
        target.setDevice(device);

        tbClient = mock(TbClient.class);
        restTemplate = mock(RestTemplate.class);

        checker = new HttpTransportHealthChecker(config, target);
        checker.restTemplate = restTemplate;
        ReflectionTestUtils.setField(checker, "tbClient", tbClient);
        ReflectionTestUtils.setField(checker, "reporter", mock(MonitoringReporter.class));
    }

    @Test
    void doRpcCheckHappyPath() throws Exception {
        enableRpc();
        ArgumentCaptor<JsonNode> bodyCaptor = ArgumentCaptor.forClass(JsonNode.class);
        when(tbClient.handleTwoWayDeviceRPCRequest(any(DeviceId.class), bodyCaptor.capture()))
                .thenAnswer(inv -> bodyCaptor.getValue().get("params"));

        checker.doRpcCheck();

        assertThat(bodyCaptor.getValue().get("method").asText()).isEqualTo("monitoringCheck");
    }

    @Test
    void doRpcCheckValueMismatchUsesRpcKey() {
        enableRpc();
        when(tbClient.handleTwoWayDeviceRPCRequest(any(DeviceId.class), any(JsonNode.class)))
                .thenReturn(JacksonUtil.newObjectNode().put("value", "wrong"));

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
    void initClientStartsPollingThreadWhenRpcEnabled() throws Exception {
        enableRpc();

        checker.initClient();

        Thread thread = (Thread) ReflectionTestUtils.getField(checker, "rpcPollThread");
        assertThat(thread).isNotNull();
        assertThat(thread.isDaemon()).isTrue();
        // give the thread a moment, then tear down
        checker.destroyClient();
    }

    @Test
    void initClientDoesNotStartPollingThreadWhenRpcDisabled() throws Exception {
        checker.initClient();

        Thread thread = (Thread) ReflectionTestUtils.getField(checker, "rpcPollThread");
        assertThat(thread).isNull();
    }

    @Test
    void initClientIsIdempotentForPollingThread() throws Exception {
        enableRpc();

        checker.initClient();
        Thread first = (Thread) ReflectionTestUtils.getField(checker, "rpcPollThread");
        checker.initClient();
        Thread second = (Thread) ReflectionTestUtils.getField(checker, "rpcPollThread");

        assertThat(second).isSameAs(first);
        checker.destroyClient();
    }

    @Test
    void destroyClientStopsPollingThread() throws Exception {
        enableRpc();
        checker.initClient();

        checker.destroyClient();

        Thread thread = (Thread) ReflectionTestUtils.getField(checker, "rpcPollThread");
        assertThat(thread).isNull();
    }

    @Test
    void pollOnceEchoesParamsWhenRpcArrives() throws Exception {
        ObjectNode rpc = JacksonUtil.newObjectNode();
        rpc.put("id", 99L);
        rpc.put("method", "monitoringCheck");
        rpc.set("params", JacksonUtil.newObjectNode().put("value", "uuid-99"));
        when(restTemplate.getForEntity(contains("/rpc?timeout="), eq(JsonNode.class)))
                .thenReturn(ResponseEntity.ok(rpc));

        checker.pollOnce();

        ArgumentCaptor<JsonNode> bodyCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(restTemplate).postForLocation(eq("http://localhost:8080/api/v1/token-abc/rpc/99"), bodyCaptor.capture());
        assertThat(bodyCaptor.getValue().get("value").asText()).isEqualTo("uuid-99");
    }

    @Test
    void pollOnceIsSilentOn408() throws Exception {
        when(restTemplate.getForEntity(contains("/rpc?timeout="), eq(JsonNode.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.REQUEST_TIMEOUT, "timeout", null, null, null));

        checker.pollOnce();

        verify(restTemplate, never()).postForLocation(any(String.class), any());
    }

    @Test
    void pollOnceSwallowsBodylessOk() throws Exception {
        when(restTemplate.getForEntity(contains("/rpc?timeout="), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        checker.pollOnce();

        verify(restTemplate, never()).postForLocation(any(String.class), any());
    }

    private void enableRpc() {
        RpcCheckConfig rpc = new RpcCheckConfig();
        rpc.setEnabled(true);
        target.setRpc(rpc);
    }

}
