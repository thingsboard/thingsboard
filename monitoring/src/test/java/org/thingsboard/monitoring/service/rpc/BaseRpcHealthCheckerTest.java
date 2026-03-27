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
package org.thingsboard.monitoring.service.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.monitoring.client.TbClient;
import org.thingsboard.monitoring.config.rpc.MqttRpcMonitoringConfig;
import org.thingsboard.monitoring.config.rpc.RpcMonitoringTarget;
import org.thingsboard.monitoring.config.rpc.RpcTransportType;
import org.thingsboard.monitoring.data.MonitoredServiceKey;
import org.thingsboard.monitoring.service.MonitoringReporter;
import org.thingsboard.monitoring.util.TbStopWatch;
import org.thingsboard.server.common.data.id.DeviceId;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BaseRpcHealthCheckerTest {

    @Mock TbClient tbClient;
    @Mock MonitoringReporter reporter;
    @Mock TbStopWatch stopWatch;

    TestRpcChecker checker;
    RpcMonitoringTarget target;

    @BeforeEach
    void setUp() {
        MqttRpcMonitoringConfig config = new MqttRpcMonitoringConfig();
        config.setRequestTimeoutMs(5000);
        config.setQos(1);

        target = new RpcMonitoringTarget();
        target.setDeviceId("00000000-0000-0000-0000-000000000001");
        target.setAccessToken("test-token");
        target.setLabel("shard0");
        target.setBaseUrl("tcp://localhost:1883");

        checker = new TestRpcChecker(config, target);
        ReflectionTestUtils.setField(checker, "tbClient", tbClient);
        ReflectionTestUtils.setField(checker, "reporter", reporter);
        ReflectionTestUtils.setField(checker, "stopWatch", stopWatch);
    }

    @Test
    void check_happyPath_reportsOkAndLatency() {
        when(tbClient.handleTwoWayDeviceRPCRequest(any(DeviceId.class), any())).thenAnswer(inv -> {
            JsonNode body = inv.getArgument(1, JsonNode.class);
            String value = body.path("params").path("value").asText();
            return JacksonUtil.newObjectNode().put("value", value);
        });

        checker.check(null);

        verify(reporter).serviceIsOk(checker.getInfo());
        verify(reporter).serviceIsOk(MonitoredServiceKey.GENERAL);
        verify(reporter).reportLatency(contains("RpcRoundTrip"), anyLong());
        verify(reporter, never()).serviceFailure(any(), any(Throwable.class));
    }

    @Test
    void check_requestBodyHasCorrectStructure() {
        when(tbClient.handleTwoWayDeviceRPCRequest(any(DeviceId.class), any())).thenAnswer(inv -> {
            JsonNode body = inv.getArgument(1, JsonNode.class);
            String value = body.path("params").path("value").asText();
            return JacksonUtil.newObjectNode().put("value", value);
        });

        checker.check(null);

        ArgumentCaptor<JsonNode> bodyCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(tbClient).handleTwoWayDeviceRPCRequest(any(), bodyCaptor.capture());
        JsonNode body = bodyCaptor.getValue();

        assertThat(body.path("method").asText()).isEqualTo("monitoringCheck");
        assertThat(body.path("params").path("value").asText()).isNotBlank();
        assertThat(body.path("timeout").asInt()).isEqualTo(5000);
    }

    @Test
    void check_requestTargetsCorrectDevice() {
        when(tbClient.handleTwoWayDeviceRPCRequest(any(), any())).thenAnswer(inv -> {
            JsonNode body = inv.getArgument(1, JsonNode.class);
            return JacksonUtil.newObjectNode().put("value", body.path("params").path("value").asText());
        });

        checker.check(null);

        ArgumentCaptor<DeviceId> deviceCaptor = ArgumentCaptor.forClass(DeviceId.class);
        verify(tbClient).handleTwoWayDeviceRPCRequest(deviceCaptor.capture(), any());
        assertThat(deviceCaptor.getValue().getId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    }

    @Test
    void check_wrongEchoedValue_reportsFailure() {
        when(tbClient.handleTwoWayDeviceRPCRequest(any(), any()))
                .thenReturn(JacksonUtil.newObjectNode().put("value", "wrong-value"));

        checker.check(null);

        verify(reporter).serviceFailure(eq(checker.getInfo()), any(Throwable.class));
        verify(reporter, never()).serviceIsOk(checker.getInfo());
    }

    @Test
    void check_nullResponse_reportsFailure() {
        when(tbClient.handleTwoWayDeviceRPCRequest(any(), any())).thenReturn(null);

        checker.check(null);

        verify(reporter).serviceFailure(eq(checker.getInfo()), any(Throwable.class));
        verify(reporter, never()).serviceIsOk(checker.getInfo());
    }

    @Test
    void check_restCallThrows_reportsFailure() {
        when(tbClient.handleTwoWayDeviceRPCRequest(any(), any()))
                .thenThrow(new RuntimeException("connection refused"));

        checker.check(null);

        verify(reporter).serviceFailure(eq(checker.getInfo()), any(Throwable.class));
        verify(reporter, never()).serviceIsOk(checker.getInfo());
    }

    @Test
    void check_restCallThrows_doesNotReportLatency() {
        when(tbClient.handleTwoWayDeviceRPCRequest(any(), any()))
                .thenThrow(new RuntimeException("timeout"));

        checker.check(null);

        verify(reporter, never()).reportLatency(any(), anyLong());
    }

    @Test
    void getKey_includesTransportTypeAndLabel() {
        assertThat(checker.getKey()).isEqualTo("mqttRpc_shard0");
    }

    // Minimal concrete subclass — initClient/destroyClient are no-ops
    private static class TestRpcChecker extends BaseRpcHealthChecker<MqttRpcMonitoringConfig> {
        TestRpcChecker(MqttRpcMonitoringConfig config, RpcMonitoringTarget target) {
            super(config, target);
        }

        @Override protected void initClient() {}
        @Override protected void destroyClient() {}
        @Override protected RpcTransportType getTransportType() { return RpcTransportType.MQTT; }

        // expose getInfo() for assertions
        @Override public Object getInfo() { return super.getInfo(); }
    }

}
