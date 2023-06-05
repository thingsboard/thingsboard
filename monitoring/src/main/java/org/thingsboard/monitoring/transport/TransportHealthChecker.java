/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.monitoring.transport;

import com.fasterxml.jackson.databind.node.TextNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.monitoring.client.WsClient;
import org.thingsboard.monitoring.config.MonitoringTargetConfig;
import org.thingsboard.monitoring.config.TransportType;
import org.thingsboard.monitoring.config.service.TransportMonitoringConfig;
import org.thingsboard.monitoring.data.Latencies;
import org.thingsboard.monitoring.data.MonitoredServiceKey;
import org.thingsboard.monitoring.data.TransportFailureException;
import org.thingsboard.monitoring.data.TransportInfo;
import org.thingsboard.monitoring.service.MonitoringReporter;
import org.thingsboard.monitoring.util.TbStopWatch;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.UUID;

@Slf4j
public abstract class TransportHealthChecker<C extends TransportMonitoringConfig> {

    protected final C config;
    protected final MonitoringTargetConfig target;
    private TransportInfo transportInfo;

    @Autowired
    private MonitoringReporter reporter;
    @Autowired
    private TbStopWatch stopWatch;
    @Value("${monitoring.check_timeout_ms}")
    private int resultCheckTimeoutMs;

    public static final String TEST_TELEMETRY_KEY = "testData";

    protected TransportHealthChecker(C config, MonitoringTargetConfig target) {
        this.config = config;
        this.target = target;
    }

    @PostConstruct
    private void init() {
        transportInfo = new TransportInfo(getTransportType(), target.getBaseUrl());
    }

    public final void check(WsClient wsClient) {
        log.debug("[{}] Checking", transportInfo);
        try {
            wsClient.registerWaitForUpdate();

            String testValue = UUID.randomUUID().toString();
            String testPayload = createTestPayload(testValue);
            try {
                initClientAndSendPayload(testPayload);
                log.trace("[{}] Sent test payload ({})", transportInfo, testPayload);
            } catch (Throwable e) {
                throw new TransportFailureException(e);
            }

            log.trace("[{}] Waiting for WS update", transportInfo);
            checkWsUpdate(wsClient, testValue);

            reporter.serviceIsOk(transportInfo);
            reporter.serviceIsOk(MonitoredServiceKey.GENERAL);
        } catch (TransportFailureException transportFailureException) {
            reporter.serviceFailure(transportInfo, transportFailureException);
        } catch (Exception e) {
            reporter.serviceFailure(MonitoredServiceKey.GENERAL, e);
        }
    }

    private void initClientAndSendPayload(String payload) throws Throwable {
        initClient();
        stopWatch.start();
        sendTestPayload(payload);
        reporter.reportLatency(Latencies.transportRequest(getTransportType()), stopWatch.getTime());
    }

    private void checkWsUpdate(WsClient wsClient, String testValue) {
        stopWatch.start();
        wsClient.waitForUpdate(resultCheckTimeoutMs);
        log.trace("[{}] Waited for WS update. Last WS msg: {}", transportInfo, wsClient.lastMsg);
        Object update = wsClient.getTelemetryUpdate(target.getDevice().getId(), TEST_TELEMETRY_KEY);
        if (update == null) {
            throw new TransportFailureException("No WS update arrived within " + resultCheckTimeoutMs + " ms");
        } else if (!update.toString().equals(testValue)) {
            throw new TransportFailureException("Was expecting value " + testValue + " but got " + update);
        }
        reporter.reportLatency(Latencies.WS_UPDATE, stopWatch.getTime());
    }

    protected String createTestPayload(String testValue) {
        return JacksonUtil.newObjectNode().set(TEST_TELEMETRY_KEY, new TextNode(testValue)).toString();
    }

    protected abstract void initClient() throws Exception;

    protected abstract void sendTestPayload(String payload) throws Exception;

    @PreDestroy
    protected abstract void destroyClient() throws Exception;

    protected abstract TransportType getTransportType();

}
