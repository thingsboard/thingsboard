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
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.monitoring.client.TbClient;
import org.thingsboard.monitoring.client.WsClient;
import org.thingsboard.monitoring.client.WsClientFactory;
import org.thingsboard.monitoring.config.transport.TransportMonitoringConfig;
import org.thingsboard.monitoring.config.transport.TransportMonitoringTarget;
import org.thingsboard.monitoring.data.MonitoredServiceKey;
import org.thingsboard.monitoring.data.ServiceFailureException;
import org.thingsboard.monitoring.metrics.ProbeMetricsRecorder;
import org.thingsboard.monitoring.util.TbStopWatch;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BaseMonitoringServiceProbeMetricsTest {

    private TbClient tbClient;
    private WsClientFactory wsClientFactory;
    private MonitoringReporter reporter;
    private ProbeMetricsRecorder probeMetricsRecorder;
    private TestMonitoringService service;
    private WsClient wsClient;
    private BaseHealthChecker<TransportMonitoringConfig, TransportMonitoringTarget> healthChecker;

    @BeforeEach
    public void setUp() throws Exception {
        tbClient = mock(TbClient.class);
        wsClientFactory = mock(WsClientFactory.class);
        reporter = mock(MonitoringReporter.class);
        probeMetricsRecorder = mock(ProbeMetricsRecorder.class);
        wsClient = mock(WsClient.class);
        when(wsClient.subscribeForTelemetry(any(), any())).thenReturn(wsClient);

        service = new TestMonitoringService();
        ReflectionTestUtils.setField(service, "tbClient", tbClient);
        ReflectionTestUtils.setField(service, "wsClientFactory", wsClientFactory);
        ReflectionTestUtils.setField(service, "reporter", reporter);
        ReflectionTestUtils.setField(service, "probeMetricsRecorder", probeMetricsRecorder);
        ReflectionTestUtils.setField(service, "stopWatch", new TbStopWatch());

        // one stub health checker so runChecks() doesn't short-circuit on the "healthCheckers.isEmpty()" guard;
        // its own check() outcome is irrelevant to this test (it's exercised in BaseHealthCheckerProbeMetricsTest).
        // getTarget() must be stubbed: BaseMonitoringService.check() reads target.isCheckDomainIps() right
        // after invoking it, and an unstubbed null there would NPE out of a "successful" run.
        healthChecker = mock(BaseHealthChecker.class);
        TransportMonitoringTarget target = new TransportMonitoringTarget();
        target.setCheckDomainIps(false);
        when(healthChecker.getTarget()).thenReturn(target);
        List<BaseHealthChecker<TransportMonitoringConfig, TransportMonitoringTarget>> healthCheckers =
                (List) ReflectionTestUtils.getField(service, "healthCheckers");
        healthCheckers.add(healthChecker);
    }

    @Test
    public void successfulLoginAndWs_recordsBothAsSuccessful() throws Exception {
        when(tbClient.logIn()).thenReturn("token");
        when(wsClientFactory.createClient("token")).thenReturn(wsClient);
        when(wsClient.waitForReply()).thenReturn(null);

        service.runChecks();

        verify(probeMetricsRecorder).recordProbe(eq(MonitoredServiceKey.LOGIN), eq(true));
        verify(probeMetricsRecorder).recordProbe(eq(MonitoredServiceKey.WS), eq(true));
    }

    @Test
    public void successfulLoginAndWs_recordsRequestConnectAndSubscribeStageDurations() throws Exception {
        when(tbClient.logIn()).thenReturn("token");
        when(wsClientFactory.createClient("token")).thenReturn(wsClient);
        when(wsClient.waitForReply()).thenReturn(null);

        service.runChecks();

        verify(probeMetricsRecorder).recordActionDuration(eq(MonitoredServiceKey.LOGIN), eq("request"), anyLong());
        verify(probeMetricsRecorder).recordActionDuration(eq(MonitoredServiceKey.WS), eq("connect"), anyLong());
        verify(probeMetricsRecorder).recordActionDuration(eq(MonitoredServiceKey.WS), eq("subscribe"), anyLong());
    }

    @Test
    public void loginFailure_recordsLoginFailureAndNeverRecordsWs() throws Exception {
        when(tbClient.logIn()).thenThrow(new RuntimeException("login failed"));

        service.runChecks();

        verify(probeMetricsRecorder).recordProbe(eq(MonitoredServiceKey.LOGIN), eq(false));
        verify(probeMetricsRecorder, never()).recordProbe(eq(MonitoredServiceKey.WS), any(Boolean.class));
    }

    @Test
    public void loginFailure_removesWsProbeMetric() throws Exception {
        when(tbClient.logIn()).thenThrow(new RuntimeException("login failed"));

        service.runChecks();

        verify(probeMetricsRecorder).removeProbe(eq(MonitoredServiceKey.WS));
    }

    @Test
    public void loginFailure_neverRecordsLoginRequestStageDuration() throws Exception {
        when(tbClient.logIn()).thenThrow(new RuntimeException("login failed"));

        service.runChecks();

        verify(probeMetricsRecorder, never()).recordActionDuration(eq(MonitoredServiceKey.LOGIN), eq("request"), anyLong());
    }

    @Test
    public void loginFailure_clearsTransportProbeMetrics() throws Exception {
        // transport checks never ran this cycle - their gauges must not keep reporting last cycle's value
        Object transportInfo = new Object();
        when(healthChecker.getCachedInfo()).thenReturn(transportInfo);
        when(tbClient.logIn()).thenThrow(new RuntimeException("login failed"));

        service.runChecks();

        // scoped to the transport healthChecker's own info - distinct from the WS-key removal from Fix A,
        // which is asserted separately by loginFailure_removesWsProbeMetric
        verify(probeMetricsRecorder, times(1)).removeProbe(transportInfo);
    }

    @Test
    public void loginFailure_checksTransportAcceptance() throws Exception {
        when(tbClient.logIn()).thenThrow(new RuntimeException("login failed"));

        service.runChecks();

        verify(healthChecker).checkAccepted();
    }

    @Test
    public void loginFailure_neverRemovesAcceptedProbeBeforeFallbackRuns() throws Exception {
        // removeAcceptedProbe must never precede the fallback check on the failure path
        when(tbClient.logIn()).thenThrow(new RuntimeException("login failed"));

        service.runChecks();

        verify(probeMetricsRecorder, never()).removeAcceptedProbe(any());
    }

    @Test
    public void wsConnectFailure_recordsWsFailure() throws Exception {
        when(tbClient.logIn()).thenReturn("token");
        when(wsClientFactory.createClient("token")).thenThrow(new RuntimeException("connect failed"));

        service.runChecks();

        verify(probeMetricsRecorder).recordProbe(eq(MonitoredServiceKey.LOGIN), eq(true));
        verify(probeMetricsRecorder).recordProbe(eq(MonitoredServiceKey.WS), eq(false));
    }

    @Test
    public void wsConnectFailure_neverRecordsConnectStageDuration() throws Exception {
        when(tbClient.logIn()).thenReturn("token");
        when(wsClientFactory.createClient("token")).thenThrow(new RuntimeException("connect failed"));

        service.runChecks();

        verify(probeMetricsRecorder, never()).recordActionDuration(eq(MonitoredServiceKey.WS), eq("connect"), anyLong());
    }

    @Test
    public void wsConnectFailure_clearsTransportProbeMetrics() throws Exception {
        when(tbClient.logIn()).thenReturn("token");
        when(wsClientFactory.createClient("token")).thenThrow(new RuntimeException("connect failed"));

        service.runChecks();

        verify(probeMetricsRecorder, times(1)).removeProbe(any());
    }

    @Test
    public void wsConnectFailure_checksTransportAcceptance() throws Exception {
        when(tbClient.logIn()).thenReturn("token");
        when(wsClientFactory.createClient("token")).thenThrow(new RuntimeException("connect failed"));

        service.runChecks();

        verify(healthChecker).checkAccepted();
    }

    @Test
    public void wsConnectFailure_neverRemovesAcceptedProbeBeforeFallbackRuns() throws Exception {
        when(tbClient.logIn()).thenReturn("token");
        when(wsClientFactory.createClient("token")).thenThrow(new RuntimeException("connect failed"));

        service.runChecks();

        verify(probeMetricsRecorder, never()).removeAcceptedProbe(any());
    }

    @Test
    public void wsSubscribeFailure_recordsWsFailure() throws Exception {
        when(tbClient.logIn()).thenReturn("token");
        when(wsClientFactory.createClient("token")).thenReturn(wsClient);
        when(wsClient.waitForReply()).thenThrow(new IllegalStateException("no reply"));

        service.runChecks();

        verify(probeMetricsRecorder).recordProbe(eq(MonitoredServiceKey.WS), eq(false));
    }

    @Test
    public void wsSubscribeFailure_clearsTransportProbeMetrics() throws Exception {
        when(tbClient.logIn()).thenReturn("token");
        when(wsClientFactory.createClient("token")).thenReturn(wsClient);
        when(wsClient.waitForReply()).thenThrow(new IllegalStateException("no reply"));

        service.runChecks();

        verify(probeMetricsRecorder, times(1)).removeProbe(any());
    }

    @Test
    public void wsSubscribeFailure_checksTransportAcceptance() throws Exception {
        when(tbClient.logIn()).thenReturn("token");
        when(wsClientFactory.createClient("token")).thenReturn(wsClient);
        when(wsClient.waitForReply()).thenThrow(new IllegalStateException("no reply"));

        service.runChecks();

        verify(healthChecker).checkAccepted();
    }

    @Test
    public void wsSubscribeFailure_neverRemovesAcceptedProbeBeforeFallbackRuns() throws Exception {
        when(tbClient.logIn()).thenReturn("token");
        when(wsClientFactory.createClient("token")).thenReturn(wsClient);
        when(wsClient.waitForReply()).thenThrow(new IllegalStateException("no reply"));

        service.runChecks();

        verify(probeMetricsRecorder, never()).removeAcceptedProbe(any());
    }

    @Test
    public void successfulRun_neverClearsTransportProbeMetrics() throws Exception {
        when(tbClient.logIn()).thenReturn("token");
        when(wsClientFactory.createClient("token")).thenReturn(wsClient);
        when(wsClient.waitForReply()).thenReturn(null);

        service.runChecks();

        verify(probeMetricsRecorder, never()).removeProbe(any());
    }

    @Test
    public void successfulRun_neverChecksTransportAcceptance() throws Exception {
        // WS is healthy, so E2E already covers this target - checkAccepted() firing too would double-send
        when(tbClient.logIn()).thenReturn("token");
        when(wsClientFactory.createClient("token")).thenReturn(wsClient);
        when(wsClient.waitForReply()).thenReturn(null);

        service.runChecks();

        verify(healthChecker, never()).checkAccepted();
    }

    @Test
    public void successfulRun_removesAcceptedProbeForEachHealthChecker() throws Exception {
        // fresh E2E data means any stale accepted-fallback value must be cleared, not frozen forever
        when(tbClient.logIn()).thenReturn("token");
        when(wsClientFactory.createClient("token")).thenReturn(wsClient);
        when(wsClient.waitForReply()).thenReturn(null);

        service.runChecks();

        verify(probeMetricsRecorder, times(1)).removeAcceptedProbe(any());
    }

    @Test
    public void successfulRun_removesAcceptedProbeForAssociatesToo() throws Exception {
        // associates get their own kind="accepted" gauge during an outage too - recovery must clear it
        BaseHealthChecker<TransportMonitoringConfig, TransportMonitoringTarget> associate =
                mock(BaseHealthChecker.class);
        Object associateInfo = new Object();
        when(associate.getCachedInfo()).thenReturn(associateInfo);
        when(healthChecker.getAssociates()).thenReturn(java.util.Map.of("associate-url", associate));

        when(tbClient.logIn()).thenReturn("token");
        when(wsClientFactory.createClient("token")).thenReturn(wsClient);
        when(wsClient.waitForReply()).thenReturn(null);

        service.runChecks();

        verify(probeMetricsRecorder, times(1)).removeAcceptedProbe(associateInfo);
    }

    @Test
    public void unexpectedServiceFailureMidLoop_alsoClearsAcceptedProbeMetrics() throws Exception {
        // caught by the outer handler, not the 3 known branches - must still clear kind="accepted"
        when(tbClient.logIn()).thenReturn("token");
        when(wsClientFactory.createClient("token")).thenReturn(wsClient);
        when(wsClient.waitForReply()).thenReturn(null);
        doThrow(new ServiceFailureException(MonitoredServiceKey.GENERAL, new RuntimeException("boom")))
                .when(healthChecker).check(any());

        service.runChecks();

        verify(probeMetricsRecorder, times(1)).removeAcceptedProbe(any());
    }

    @Test
    public void unexpectedThrowableMidLoop_alsoClearsAcceptedProbeMetrics() throws Exception {
        when(tbClient.logIn()).thenReturn("token");
        when(wsClientFactory.createClient("token")).thenReturn(wsClient);
        when(wsClient.waitForReply()).thenReturn(null);
        doThrow(new RuntimeException("boom")).when(healthChecker).check(any());

        service.runChecks();

        verify(probeMetricsRecorder, times(1)).removeAcceptedProbe(any());
    }

    @Test
    public void reconciliationFailureAfterSuccessfulCheck_doesNotClearThatTargetsMetrics() throws Exception {
        // healthChecker.check() completes normally, so its finally block already recorded fresh
        // probe_success/probe_duration_ms for this cycle. The domain-IP-associate reconciliation
        // that runs afterward (only when isCheckDomainIps() is true) then fails - here because the
        // configured host can never resolve (RFC 2606 reserves the ".invalid" TLD for exactly this).
        // That late, unrelated bookkeeping failure must not wipe the metrics check() already recorded.
        Object firstInfo = new Object();
        when(healthChecker.getCachedInfo()).thenReturn(firstInfo);
        TransportMonitoringTarget target = new TransportMonitoringTarget();
        target.setCheckDomainIps(true);
        target.setBaseUrl("tcp://this-host-does-not-resolve.invalid:1883");
        when(healthChecker.getTarget()).thenReturn(target);

        when(tbClient.logIn()).thenReturn("token");
        when(wsClientFactory.createClient("token")).thenReturn(wsClient);
        when(wsClient.waitForReply()).thenReturn(null);

        assertDoesNotThrow(() -> service.runChecks());

        verify(probeMetricsRecorder, never()).removeProbe(firstInfo);
    }

    @Test
    public void unexpectedThrowableMidLoop_doesNotClearAlreadyCheckedTargets() throws Exception {
        // a SECOND checker's check() throws directly - the first checker already completed and
        // recorded fresh data this cycle (checkedCount was incremented past it) and must survive
        Object firstInfo = new Object();
        when(healthChecker.getCachedInfo()).thenReturn(firstInfo);

        BaseHealthChecker<TransportMonitoringConfig, TransportMonitoringTarget> secondChecker =
                mock(BaseHealthChecker.class);
        TransportMonitoringTarget secondTarget = new TransportMonitoringTarget();
        secondTarget.setCheckDomainIps(false);
        when(secondChecker.getTarget()).thenReturn(secondTarget);
        Object secondInfo = new Object();
        when(secondChecker.getCachedInfo()).thenReturn(secondInfo);
        doThrow(new RuntimeException("boom")).when(secondChecker).check(any());
        List<BaseHealthChecker<TransportMonitoringConfig, TransportMonitoringTarget>> healthCheckers =
                (List) ReflectionTestUtils.getField(service, "healthCheckers");
        healthCheckers.add(secondChecker);

        when(tbClient.logIn()).thenReturn("token");
        when(wsClientFactory.createClient("token")).thenReturn(wsClient);
        when(wsClient.waitForReply()).thenReturn(null);

        assertDoesNotThrow(() -> service.runChecks());

        verify(probeMetricsRecorder, never()).removeProbe(firstInfo);
        verify(probeMetricsRecorder, times(1)).removeProbe(secondInfo);
    }

    @Test
    public void runChecks_alwaysRecordsHeartbeatOnce_regardlessOfOutcome() throws Exception {
        when(tbClient.logIn()).thenReturn("token");
        when(wsClientFactory.createClient("token")).thenReturn(wsClient);
        when(wsClient.waitForReply()).thenReturn(null);

        service.runChecks();

        verify(probeMetricsRecorder, times(1)).recordHeartbeat();
    }

    @Test
    public void runChecks_loginFailure_stillRecordsHeartbeatOnce() throws Exception {
        when(tbClient.logIn()).thenThrow(new RuntimeException("login failed"));

        service.runChecks();

        verify(probeMetricsRecorder, times(1)).recordHeartbeat();
    }


    private static class TestMonitoringService extends BaseMonitoringService<TransportMonitoringConfig, TransportMonitoringTarget> {
        @Override
        protected BaseHealthChecker<?, ?> createHealthChecker(TransportMonitoringConfig config, TransportMonitoringTarget target) {
            throw new UnsupportedOperationException("not exercised in this test");
        }

        @Override
        protected TransportMonitoringTarget createTarget(String baseUrl) {
            TransportMonitoringTarget target = new TransportMonitoringTarget();
            target.setBaseUrl(baseUrl);
            return target;
        }

        @Override
        protected String getName() {
            return "test";
        }
    }

}
