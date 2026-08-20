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

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.thingsboard.monitoring.client.WsClient;
import org.thingsboard.monitoring.config.MonitoringConfig;
import org.thingsboard.monitoring.config.MonitoringTarget;
import org.thingsboard.monitoring.data.Latencies;
import org.thingsboard.monitoring.data.MonitoredServiceKey;
import org.thingsboard.monitoring.data.ServiceFailureException;
import org.thingsboard.monitoring.metrics.ProbeMetricsRecorder;
import org.thingsboard.monitoring.util.TbStopWatch;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
public abstract class BaseHealthChecker<C extends MonitoringConfig, T extends MonitoringTarget> {

    @Getter
    protected final C config;
    @Getter
    protected final T target;

    private Object info;

    @Autowired
    protected MonitoringEntityService entityService;
    @Autowired
    private MonitoringReporter reporter;
    @Autowired
    private ProbeMetricsRecorder probeMetricsRecorder;
    @Autowired
    private TbStopWatch stopWatch;
    @Value("${monitoring.check_timeout_ms}")
    private int resultCheckTimeoutMs;

    @Getter
    private final Map<String, BaseHealthChecker<C, T>> associates = new HashMap<>();

    public static final String TEST_TELEMETRY_KEY = "testData";
    public static final String TEST_CF_TELEMETRY_KEY = "testDataCf";
    // separate key so a late checkAccepted() message can't be mistaken for check()'s expected value
    public static final String ACCEPTED_TEST_TELEMETRY_KEY = "acceptedTestData";

    @PostConstruct
    private void init() {
        info = getInfo();
    }

    // the value recordProbe(info, ...) was called with, unlike getInfo() which recomputes a fresh one
    Object getCachedInfo() {
        return info;
    }

    protected abstract void initialize();

    public final void check(WsClient wsClient) {
        log.debug("[{}] Checking", info);
        boolean success = false;
        try {
            String testValue;
            String testPayload;
            try {
                int expectedUpdatesCount = isCfMonitoringEnabled() ? 2 : 1;
                wsClient.registerWaitForUpdates(expectedUpdatesCount);
                testValue = UUID.randomUUID().toString();
                testPayload = createTestPayload(testValue, TEST_TELEMETRY_KEY);
            } catch (Throwable e) {
                clearOwnActionDurations(); // neither action recorded this cycle yet
                throw new ServiceFailureException(info, e);
            }
            try {
                initClient();
                stopWatch.start();
                sendTestPayload(testPayload);
                long requestLatencyNanos = stopWatch.getTime();
                reporter.reportLatency(Latencies.request(getKey()), requestLatencyNanos);
                probeMetricsRecorder.recordActionDuration(info, ProbeMetricsRecorder.ACTION_REQUEST, requestLatencyNanos / 1_000_000);
                log.trace("[{}] Sent test payload ({})", info, testPayload);
            } catch (Throwable e) {
                clearOwnActionDurations();
                throw new ServiceFailureException(info, e);
            }

            log.trace("[{}] Waiting for WS update", info);
            checkWsUpdates(wsClient, testValue);

            reporter.serviceIsOk(info);
            success = true;
        } catch (ServiceFailureException e) {
            reporter.serviceFailure(e.getServiceKey(), e);
        } catch (Exception e) {
            reporter.serviceFailure(info, e);
        } finally {
            probeMetricsRecorder.recordProbe(info, success);
        }

        associates.values().forEach(healthChecker -> {
            healthChecker.check(wsClient);
        });
    }

    private void clearOwnActionDurations() {
        probeMetricsRecorder.removeActionDuration(info, ProbeMetricsRecorder.ACTION_REQUEST);
        probeMetricsRecorder.removeActionDuration(info, ProbeMetricsRecorder.ACTION_WS_UPDATE);
    }

    // unlike check(), doesn't wait for WS/core confirmation - not final since LwM2M overrides it as
    // a no-op. Never calls reporter.serviceIsOk: sharing check()'s key could wrongly resolve a real incident.
    protected void checkAccepted() {
        boolean success;
        try {
            initClient();
            sendTestPayload(createTestPayload(UUID.randomUUID().toString(), ACCEPTED_TEST_TELEMETRY_KEY));
            log.info("[{}] (accepted) is OK", info); // plain log, not reporter.serviceIsOk - see above
            success = true;
        } catch (Throwable e) {
            reporter.serviceFailure(info, e);
            success = false;
        }
        probeMetricsRecorder.recordAcceptedProbe(info, success);
        associates.values().forEach(healthChecker -> healthChecker.checkAccepted());
    }

    private void checkWsUpdates(WsClient wsClient, String testValue) {
        try {
            stopWatch.start();
            wsClient.waitForUpdates(resultCheckTimeoutMs);
            log.trace("[{}] Waited for WS update. Last WS msgs: {}", info, wsClient.lastMsgs);
            Map<String, String> latest = wsClient.getLatest(target.getDeviceId());
            if (latest.isEmpty()) {
                throw new ServiceFailureException(info, "No WS update arrived within " + resultCheckTimeoutMs + " ms");
            }
            String actualValue = latest.get(TEST_TELEMETRY_KEY);
            if (!testValue.equals(actualValue)) {
                throw new ServiceFailureException(info, "Was expecting value " + testValue + " but got " + actualValue);
            }
            if (isCfMonitoringEnabled()) {
                String cfTestValue = testValue + "-cf";
                String actualCfValue = latest.get(TEST_CF_TELEMETRY_KEY);
                if (actualCfValue == null) {
                    throw new ServiceFailureException(info, "No calculated field value arrived");
                } else if (!cfTestValue.equals(actualCfValue)) {
                    throw new ServiceFailureException(info, "Was expecting calculated field value " + cfTestValue + " but got " + actualCfValue);
                }
            }
            long wsUpdateLatencyNanos = stopWatch.getTime();
            reporter.reportLatency(Latencies.wsUpdate(getKey()), wsUpdateLatencyNanos);
            probeMetricsRecorder.recordActionDuration(info, ProbeMetricsRecorder.ACTION_WS_UPDATE, wsUpdateLatencyNanos / 1_000_000);
        } catch (Throwable e) {
            probeMetricsRecorder.removeActionDuration(info, ProbeMetricsRecorder.ACTION_WS_UPDATE);
            throw e;
        }
    }

    protected abstract void initClient() throws Exception;

    protected abstract String createTestPayload(String testValue, String telemetryKey);

    protected abstract void sendTestPayload(String payload) throws Exception;

    @PreDestroy
    protected abstract void destroyClient() throws Exception;

    protected abstract Object getInfo();

    protected abstract String getKey();

    protected abstract boolean isCfMonitoringEnabled();

}
