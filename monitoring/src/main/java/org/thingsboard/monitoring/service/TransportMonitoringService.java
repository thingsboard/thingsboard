/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.node.TextNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.monitoring.client.TbClient;
import org.thingsboard.monitoring.client.WsClient;
import org.thingsboard.monitoring.client.WsClientFactory;
import org.thingsboard.monitoring.config.MonitoringTargetConfig;
import org.thingsboard.monitoring.config.TransportType;
import org.thingsboard.monitoring.config.WsConfig;
import org.thingsboard.monitoring.config.service.TransportMonitoringServiceConfig;
import org.thingsboard.monitoring.data.Latencies;
import org.thingsboard.monitoring.data.MonitoredServiceKey;
import org.thingsboard.monitoring.data.TransportFailureException;
import org.thingsboard.monitoring.data.TransportInfo;
import org.thingsboard.monitoring.util.TbStopWatch;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public abstract class TransportMonitoringService<C extends TransportMonitoringServiceConfig> {

    protected final C config;
    protected final MonitoringTargetConfig target;
    private TransportInfo transportInfo;
    @Autowired
    private WsConfig wsConfig;

    @Autowired
    private MonitoringReporter monitoringReporter;
    @Autowired
    private WsClientFactory wsClientFactory;
    @Autowired
    private ScheduledExecutorService monitoringExecutor;
    @Autowired
    private ExecutorService requestExecutor;
    @Autowired
    private TbClient tbClient;
    @Autowired
    private TbStopWatch stopWatch;

    protected static final String TEST_TELEMETRY_KEY = "testData";

    protected TransportMonitoringService(C config, MonitoringTargetConfig target) {
        this.config = config;
        this.target = target;
    }

    @PostConstruct
    private void init() {
        transportInfo = new TransportInfo(getTransportType(), target.getBaseUrl());
    }

    public final void startMonitoring() {
        monitoringExecutor.scheduleWithFixedDelay(() -> {
            WsClient wsClient = null;
            try {
                log.trace("[{}] Checking", transportInfo);
                wsClient = establishWsClient();
                wsClient.registerWaitForUpdate();

                String testValue = UUID.randomUUID().toString();
                String testPayload = JacksonUtil.newObjectNode().set(TEST_TELEMETRY_KEY, new TextNode(testValue)).toString();
                try {
                    initClientAndSendPayload(testPayload);
                    log.trace("[{}] Sent test payload ({})", transportInfo, testPayload);
                } catch (Throwable e) {
                    throw new TransportFailureException(e);
                }

                log.trace("[{}] Waiting for WS update", transportInfo);
                checkWsUpdate(wsClient, testValue);

                monitoringReporter.serviceIsOk(transportInfo);
                monitoringReporter.serviceIsOk(MonitoredServiceKey.GENERAL);
            } catch (TransportFailureException transportFailureException) {
                monitoringReporter.serviceFailure(transportInfo, transportFailureException);
            } catch (Exception e) {
                monitoringReporter.serviceFailure(MonitoredServiceKey.GENERAL, e);
            } finally {
                if (wsClient != null) wsClient.close();
            }
        }, config.getInitialDelayMs(), config.getMonitoringRateMs(), TimeUnit.MILLISECONDS);
        log.info("Started monitoring for transport type {} for target {}", getTransportType(), target);
    }

    private void initClientAndSendPayload(String payload) throws Throwable {
        initClient();
        stopWatch.start();
        Future<?> resultFuture = requestExecutor.submit(() -> {
            try {
                sendTestPayload(payload);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        });
        try {
            resultFuture.get(config.getRequestTimeoutMs(), TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            throw e.getCause();
        } catch (TimeoutException e) {
            throw new TimeoutException("Transport request timeout");
        }
        monitoringReporter.reportLatency(Latencies.transportRequest(getTransportType()), stopWatch.getTime());
    }

    private WsClient establishWsClient() throws Exception {
        stopWatch.start();
        String accessToken = tbClient.logIn();
        monitoringReporter.reportLatency(Latencies.LOG_IN, stopWatch.getTime());

        WsClient wsClient = wsClientFactory.createClient(accessToken);
        wsClient.subscribeForTelemetry(target.getDevice().getId(), TEST_TELEMETRY_KEY);
        Optional.ofNullable(wsClient.waitForReply(wsConfig.getRequestTimeoutMs()))
                .orElseThrow(() -> new IllegalStateException("Failed to subscribe for telemetry"));
        return wsClient;
    }

    private void checkWsUpdate(WsClient wsClient, String testValue) {
        stopWatch.start();
        wsClient.waitForUpdate(wsConfig.getResultCheckTimeoutMs());
        log.trace("[{}] Waited for WS update. Last WS msg: {}", transportInfo, wsClient.lastMsg);
        Object update = wsClient.getTelemetryKeyUpdate(TEST_TELEMETRY_KEY);
        if (update == null) {
            throw new TransportFailureException("No WS update arrived within " + wsConfig.getResultCheckTimeoutMs() + " ms");
        } else if (!update.toString().equals(testValue)) {
            throw new TransportFailureException("Was expecting value " + testValue + " but got " + update);
        }
        monitoringReporter.reportLatency(Latencies.WS_UPDATE, stopWatch.getTime());
    }


    protected abstract void initClient() throws Exception;

    protected abstract void sendTestPayload(String payload) throws Exception;

    @PreDestroy
    protected abstract void destroyClient() throws Exception;

    protected abstract TransportType getTransportType();

}
