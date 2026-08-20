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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.monitoring.client.WsClient;
import org.thingsboard.monitoring.config.MonitoringConfig;
import org.thingsboard.monitoring.config.MonitoringTarget;
import org.thingsboard.monitoring.metrics.ProbeMetricsRecorder;
import org.thingsboard.monitoring.util.TbStopWatch;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class BaseHealthCheckerProbeMetricsTest {

    private static final String INFO = "test-transport-info";

    private MonitoringReporter reporter;
    private ProbeMetricsRecorder probeMetricsRecorder;
    private StubHealthChecker checker;
    private WsClient wsClient;

    @BeforeEach
    public void setUp() {
        reporter = mock(MonitoringReporter.class);
        probeMetricsRecorder = mock(ProbeMetricsRecorder.class);
        wsClient = mock(WsClient.class);
        when(wsClient.subscribeForTelemetry(any(), any())).thenReturn(wsClient);

        checker = new StubHealthChecker(new StubConfig(), new StubTarget());
        ReflectionTestUtils.setField(checker, "reporter", reporter);
        ReflectionTestUtils.setField(checker, "probeMetricsRecorder", probeMetricsRecorder);
        ReflectionTestUtils.setField(checker, "stopWatch", new TbStopWatch());
        ReflectionTestUtils.setField(checker, "resultCheckTimeoutMs", 100);
        ReflectionTestUtils.invokeMethod(checker, "init"); // triggers @PostConstruct, sets the "info" field
    }

    @Test
    public void successfulCheck_recordsSuccess() {
        when(wsClient.waitForUpdates(100L)).thenReturn(null);
        when(wsClient.getLatest(any())).thenAnswer(invocation ->
                Map.of(BaseHealthChecker.TEST_TELEMETRY_KEY, StubHealthChecker.LAST_TEST_VALUE.get()));

        checker.check(wsClient);

        verify(probeMetricsRecorder).recordProbe(eq(INFO), eq(true));
    }

    @Test
    public void successfulCheck_recordsRequestAndWsUpdateStageDurations() {
        when(wsClient.waitForUpdates(100L)).thenReturn(null);
        when(wsClient.getLatest(any())).thenAnswer(invocation ->
                Map.of(BaseHealthChecker.TEST_TELEMETRY_KEY, StubHealthChecker.LAST_TEST_VALUE.get()));

        checker.check(wsClient);

        verify(probeMetricsRecorder).recordActionDuration(eq(INFO), eq("request"), anyLong());
        verify(probeMetricsRecorder).recordActionDuration(eq(INFO), eq("ws_update"), anyLong());
    }

    @Test
    public void failedSendTestPayload_recordsFailure() {
        checker.failOnSend = true;

        checker.check(wsClient);

        verify(probeMetricsRecorder).recordProbe(eq(INFO), eq(false));
    }

    @Test
    public void failedSendTestPayload_neverRecordsRequestStageDuration() {
        checker.failOnSend = true;

        checker.check(wsClient);

        verify(probeMetricsRecorder, never()).recordActionDuration(eq(INFO), eq("request"), anyLong());
    }

    @Test
    public void failedSendTestPayload_removesRequestAndWsUpdateStageDurations() {
        // the specific probe that didn't respond must lose its probe_duration_ms gauge this cycle,
        // and ws_update never even ran this cycle - its last value is stale too
        checker.failOnSend = true;

        checker.check(wsClient);

        verify(probeMetricsRecorder).removeActionDuration(eq(INFO), eq("request"));
        verify(probeMetricsRecorder).removeActionDuration(eq(INFO), eq("ws_update"));
    }

    @Test
    public void failedWsUpdate_recordsFailure() {
        when(wsClient.waitForUpdates(100L)).thenReturn(null);
        when(wsClient.getLatest(any())).thenReturn(Map.of()); // no matching telemetry -> ServiceFailureException

        checker.check(wsClient);

        verify(probeMetricsRecorder).recordProbe(eq(INFO), eq(false));
    }

    @Test
    public void failedWsUpdate_recordsRequestStageButNeverWsUpdateStageDuration() {
        when(wsClient.waitForUpdates(100L)).thenReturn(null);
        when(wsClient.getLatest(any())).thenReturn(Map.of()); // no matching telemetry -> ServiceFailureException

        checker.check(wsClient);

        verify(probeMetricsRecorder).recordActionDuration(eq(INFO), eq("request"), anyLong());
        verify(probeMetricsRecorder, never()).recordActionDuration(eq(INFO), eq("ws_update"), anyLong());
    }

    @Test
    public void failedWsUpdate_removesWsUpdateStageDurationButNeverTheRequestOne() {
        // the request already succeeded this cycle - only the ws_update stage's duration must go away
        when(wsClient.waitForUpdates(100L)).thenReturn(null);
        when(wsClient.getLatest(any())).thenReturn(Map.of()); // no matching telemetry -> ServiceFailureException

        checker.check(wsClient);

        verify(probeMetricsRecorder).removeActionDuration(eq(INFO), eq("ws_update"));
        verify(probeMetricsRecorder, never()).removeActionDuration(eq(INFO), eq("request"));
    }

    @Test
    public void failedWsUpdate_withPlainRuntimeException_stillRemovesWsUpdateStageDuration() {
        // e.g. WsClient.getLastMsgs()'s "WS error from server: ..." or a JSON-parsing failure - not a
        // ServiceFailureException, but the ws_update duration gauge must still not go stale
        when(wsClient.waitForUpdates(100L)).thenReturn(null);
        when(wsClient.getLatest(any())).thenThrow(new RuntimeException("WS error from server: boom"));

        checker.check(wsClient);

        verify(probeMetricsRecorder).removeActionDuration(eq(INFO), eq("ws_update"));
    }

    @Test
    public void checkAccepted_recordsSuccessWhenSendSucceeds() {
        checker.checkAccepted();

        verify(probeMetricsRecorder).recordAcceptedProbe(eq(INFO), eq(true));
    }

    @Test
    public void checkAccepted_recordsFailureWhenSendThrows() {
        checker.failOnSend = true;

        checker.checkAccepted();

        verify(probeMetricsRecorder).recordAcceptedProbe(eq(INFO), eq(false));
    }

    @Test
    public void checkAccepted_neverReportsAnythingWhenSendSucceeds() {
        // must never call serviceIsOk here - it shares check()'s service key, so an "ok" from this
        // fallback alone could resolve/reset a real, still-open incident that only check() should clear
        checker.checkAccepted();

        verify(reporter, never()).serviceIsOk(any());
        verify(reporter, never()).serviceFailure(any(), any());
    }

    @Test
    public void checkAccepted_reportsServiceFailureWhenSendThrows() {
        checker.failOnSend = true;

        checker.checkAccepted();

        verify(reporter).serviceFailure(eq(INFO), any());
        verify(reporter, never()).serviceIsOk(any());
    }

    @Test
    public void checkAccepted_neverWaitsForWsUpdate() {
        checker.checkAccepted();

        verifyNoInteractions(wsClient);
    }

    @Test
    public void checkAccepted_usesDistinctTelemetryKey_notTheE2eTestKey() {
        checker.checkAccepted();

        JsonNode payload = JacksonUtil.toJsonNode(checker.lastSentPayload);
        assertThat(payload.has(BaseHealthChecker.ACCEPTED_TEST_TELEMETRY_KEY)).isTrue();
        assertThat(payload.has(BaseHealthChecker.TEST_TELEMETRY_KEY)).isFalse();
    }

    @Test
    public void check_stillUsesTheE2eTestKey_notTheAcceptedKey() {
        when(wsClient.waitForUpdates(100L)).thenReturn(null);
        when(wsClient.getLatest(any())).thenAnswer(invocation ->
                Map.of(BaseHealthChecker.TEST_TELEMETRY_KEY, StubHealthChecker.LAST_TEST_VALUE.get()));

        checker.check(wsClient);

        JsonNode payload = JacksonUtil.toJsonNode(checker.lastSentPayload);
        assertThat(payload.has(BaseHealthChecker.TEST_TELEMETRY_KEY)).isTrue();
        assertThat(payload.has(BaseHealthChecker.ACCEPTED_TEST_TELEMETRY_KEY)).isFalse();
    }

    @Test
    public void checkAccepted_alsoChecksAssociates() {
        StubHealthChecker associate = new StubHealthChecker(new StubConfig(), new StubTarget());
        ReflectionTestUtils.setField(associate, "reporter", reporter);
        ReflectionTestUtils.setField(associate, "probeMetricsRecorder", probeMetricsRecorder);
        ReflectionTestUtils.invokeMethod(associate, "init");
        checker.getAssociates().put("associate-url", associate);

        checker.checkAccepted();

        verify(probeMetricsRecorder, times(2)).recordAcceptedProbe(eq(INFO), eq(true));
        verify(reporter, never()).serviceIsOk(any());
    }

    private static class StubConfig implements MonitoringConfig<StubTarget> {
        @Override
        public java.util.List<StubTarget> getTargets() {
            return java.util.List.of();
        }
    }

    private static class StubTarget implements MonitoringTarget {
        @Override
        public UUID getDeviceId() {
            return UUID.randomUUID();
        }

        @Override
        public String getBaseUrl() {
            return "stub://localhost";
        }

        @Override
        public boolean isCheckDomainIps() {
            return false;
        }
    }

    private static class StubHealthChecker extends BaseHealthChecker<StubConfig, StubTarget> {

        static final ThreadLocal<String> LAST_TEST_VALUE = new ThreadLocal<>();
        boolean failOnSend = false;
        String lastSentPayload;

        protected StubHealthChecker(StubConfig config, StubTarget target) {
            super(config, target);
        }

        @Override
        protected void initialize() {
        }

        @Override
        protected void initClient() {
        }

        @Override
        protected String createTestPayload(String testValue, String telemetryKey) {
            LAST_TEST_VALUE.set(testValue);
            return JacksonUtil.newObjectNode().set(telemetryKey, new TextNode(testValue)).toString();
        }

        @Override
        protected void sendTestPayload(String payload) throws Exception {
            if (failOnSend) {
                throw new RuntimeException("send failed");
            }
            lastSentPayload = payload;
        }

        @Override
        protected void destroyClient() {
        }

        @Override
        protected Object getInfo() {
            return INFO;
        }

        @Override
        protected String getKey() {
            return "stub";
        }

        @Override
        protected boolean isCfMonitoringEnabled() {
            return false;
        }

    }

}
