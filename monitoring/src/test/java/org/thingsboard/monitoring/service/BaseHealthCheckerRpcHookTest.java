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
package org.thingsboard.monitoring.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.monitoring.client.WsClient;
import org.thingsboard.monitoring.config.MonitoringConfig;
import org.thingsboard.monitoring.config.MonitoringTarget;
import org.thingsboard.monitoring.data.ServiceFailureException;
import org.thingsboard.monitoring.util.TbStopWatch;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BaseHealthCheckerRpcHookTest {

    private MonitoringReporter reporter;
    private WsClient wsClient;
    private TestTarget target;
    private TestChecker checker;

    @BeforeEach
    void setUp() {
        reporter = mock(MonitoringReporter.class);
        wsClient = mock(WsClient.class);
        target = new TestTarget(UUID.randomUUID());
        checker = new TestChecker(new TestConfig(), target);
        ReflectionTestUtils.setField(checker, "info", "test-info");
        ReflectionTestUtils.setField(checker, "reporter", reporter);
        ReflectionTestUtils.setField(checker, "stopWatch", new TbStopWatch());
        ReflectionTestUtils.setField(checker, "resultCheckTimeoutMs", 1000);
    }

    @Test
    void rpcCheckRunsAfterSuccessfulTelemetryAndWsUpdate() {
        stubWsUpdateEchoesPayload();

        checker.check(wsClient);

        assertThat(checker.rpcCheckCalled).isTrue();
        verify(reporter).serviceIsOk("test-info");
    }

    @Test
    void rpcCheckIsNotRunWhenTelemetryPublishFails() {
        checker.failTelemetry = true;

        checker.check(wsClient);

        assertThat(checker.rpcCheckCalled).isFalse();
        verify(reporter).serviceFailure(eq("test-info"), any(Throwable.class));
        verify(reporter, never()).serviceIsOk(any());
    }

    @Test
    void rpcCheckIsNotRunWhenWsUpdateMissing() {
        when(wsClient.getLatest(any())).thenReturn(Collections.emptyMap());

        checker.check(wsClient);

        assertThat(checker.rpcCheckCalled).isFalse();
        verify(reporter).serviceFailure(eq("test-info"), any(Throwable.class));
        verify(reporter, never()).serviceIsOk(any());
    }

    @Test
    void rpcFailureRoutesThroughDedicatedServiceKey() {
        stubWsUpdateEchoesPayload();
        Object rpcKey = new Object() {
            @Override public String toString() { return "test-info RPC"; }
        };
        checker.rpcServiceKey = rpcKey;
        checker.rpcFailure = new IOException("rpc failed");

        checker.check(wsClient);

        assertThat(checker.rpcCheckCalled).isTrue();
        verify(reporter).serviceFailure(eq(rpcKey), any(Throwable.class));
        verify(reporter, never()).serviceIsOk(any());
    }

    @Test
    void defaultDoRpcCheckIsNoOpAndDoesNotPreventOk() {
        TestChecker noOpRpc = new TestChecker(new TestConfig(), target);
        ReflectionTestUtils.setField(noOpRpc, "info", "noop-info");
        ReflectionTestUtils.setField(noOpRpc, "reporter", reporter);
        ReflectionTestUtils.setField(noOpRpc, "stopWatch", new TbStopWatch());
        ReflectionTestUtils.setField(noOpRpc, "resultCheckTimeoutMs", 1000);
        noOpRpc.skipRpcOverride = true;
        when(wsClient.getLatest(any())).thenAnswer(latestEcho(noOpRpc));

        noOpRpc.check(wsClient);

        verify(reporter).serviceIsOk("noop-info");
    }

    private void stubWsUpdateEchoesPayload() {
        when(wsClient.getLatest(any())).thenAnswer(latestEcho(checker));
    }

    private static org.mockito.stubbing.Answer<Map<String, String>> latestEcho(TestChecker checker) {
        return (InvocationOnMock invocation) -> {
            String value = checker.capturedTestValue;
            return value == null ? Collections.emptyMap() : Map.of("testData", value);
        };
    }

    private static final class TestConfig implements MonitoringConfig<TestTarget> {
        @Override
        public List<TestTarget> getTargets() {
            return Collections.emptyList();
        }
    }

    private static final class TestTarget implements MonitoringTarget {
        private final UUID deviceId;

        TestTarget(UUID deviceId) {
            this.deviceId = deviceId;
        }

        @Override public UUID getDeviceId() { return deviceId; }
        @Override public String getBaseUrl() { return "test://target"; }
        @Override public boolean isCheckDomainIps() { return false; }
    }

    private static class TestChecker extends BaseHealthChecker<TestConfig, TestTarget> {

        String capturedTestValue;
        boolean failTelemetry;
        boolean rpcCheckCalled;
        boolean skipRpcOverride;
        Object rpcServiceKey;
        Throwable rpcFailure;

        TestChecker(TestConfig config, TestTarget target) {
            super(config, target);
        }

        @Override protected void initialize() { }
        @Override protected void initClient() { }
        @Override protected void destroyClient() { }
        @Override protected Object getInfo() { return "test-info"; }
        @Override protected String getKey() { return "test"; }
        @Override protected boolean isCfMonitoringEnabled() { return false; }

        @Override
        protected String createTestPayload(String testValue) {
            capturedTestValue = testValue;
            return testValue;
        }

        @Override
        protected void sendTestPayload(String payload) throws Exception {
            if (failTelemetry) {
                throw new IOException("telemetry publish failed");
            }
        }

        @Override
        protected void doRpcCheck() throws Exception {
            if (skipRpcOverride) {
                super.doRpcCheck();
                return;
            }
            rpcCheckCalled = true;
            if (rpcFailure != null) {
                throw new ServiceFailureException(rpcServiceKey, rpcFailure);
            }
        }
    }

}
