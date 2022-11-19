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
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.monitoring.client.TbClientFactory;
import org.thingsboard.monitoring.client.WsClient;
import org.thingsboard.monitoring.config.MonitoringTargetConfig;
import org.thingsboard.monitoring.config.TransportType;
import org.thingsboard.monitoring.config.WsConfig;
import org.thingsboard.monitoring.config.service.TransportMonitoringServiceConfig;
import org.thingsboard.monitoring.data.TransportInfo;
import org.thingsboard.monitoring.data.notification.MonitoringFailureNotificationInfo;
import org.thingsboard.monitoring.data.notification.MonitoringRecoveryNotificationInfo;
import org.thingsboard.monitoring.data.notification.TransportFailureNotificationInfo;
import org.thingsboard.monitoring.data.notification.TransportRecoveryNotificationInfo;
import org.thingsboard.monitoring.notification.NotificationService;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public abstract class TransportMonitoringService<C extends TransportMonitoringServiceConfig> {

    protected final C config;
    protected final MonitoringTargetConfig target;
    private TransportInfo transportInfo;
    @Autowired
    private WsConfig wsConfig;

    @Autowired
    private NotificationService notificationService;
    @Autowired
    private MonitoringReporter monitoringReporter;
    @Autowired
    @Qualifier("monitoringExecutor")
    private ScheduledExecutorService monitoringExecutor;
    @Autowired
    @Qualifier("requestExecutor")
    private ExecutorService requestExecutor;
    @Autowired
    private TbClientFactory tbClientFactory;

    private final AtomicInteger transportFailuresCounter = new AtomicInteger();
    private final AtomicInteger monitoringFailuresCounter = new AtomicInteger();

    private final StopWatch stopWatch = new StopWatch();

    protected static final String TEST_TELEMETRY_KEY = "testData";

    protected TransportMonitoringService(C config, MonitoringTargetConfig target) {
        this.config = config;
        this.target = target;
    }

    @PostConstruct
    public void startMonitoring() {
        transportInfo = new TransportInfo(getTransportType(), target.getBaseUrl());
        // todo: create devices
        monitoringExecutor.scheduleWithFixedDelay(() -> {
            try {
                startStopWatch();
                initClient();
                monitoringReporter.reportTransportConnectLatency(transportInfo, getElapsedTime());

                WsClient wsClient = establishWsClient();
                wsClient.registerWaitForUpdate();
                String testPayload = createTestPayload(UUID.randomUUID().toString());

                startStopWatch();
                Future<?> resultFuture = requestExecutor.submit(() -> {
                    try {
                        sendTestPayload(testPayload);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                resultFuture.get(config.getRequestTimeoutMs(), TimeUnit.MILLISECONDS);
                monitoringReporter.reportTransportRequestLatency(transportInfo, getElapsedTime());

                startStopWatch();
                wsClient.waitForUpdate(wsConfig.getResultCheckTimeoutMs());
                Object update = wsClient.getTelemetryKeyUpdate(TEST_TELEMETRY_KEY);
                boolean success = update != null && update.toString().equals(testPayload);
                if (!success) {
                    throw new RuntimeException("No WS update arrived");
                }
                monitoringReporter.reportWsUpdateLatency(getElapsedTime());
                wsClient.closeBlocking();
                destroyClient();
            } catch (Exception e) {
                monitoringReporter.reportFailure(transportInfo, e);
            }
        }, config.getInitialDelayMs(), config.getMonitoringRateMs(), TimeUnit.MILLISECONDS);
        log.info("Started monitoring for transport type {} for target {}", getTransportType(), target);
    }

    private WsClient establishWsClient() throws Exception {
        startStopWatch();
        String accessToken = tbClientFactory.createClient().logIn();
        monitoringReporter.reportLogInLatency(getElapsedTime());

        URI uri = new URI(wsConfig.getBaseUrl() + "/api/ws/plugins/telemetry?token=" + accessToken);
        startStopWatch();
        WsClient wsClient = new WsClient(uri);
        boolean connected = wsClient.connectBlocking(wsConfig.getRequestTimeoutMs(), TimeUnit.MILLISECONDS);
        if (!connected) {
            throw new IllegalStateException("Failed to establish WS session");
        }
        wsClient.subscribeForTelemetry(target.getDevice().getId(), TEST_TELEMETRY_KEY);
        Optional.ofNullable(wsClient.waitForReply(wsConfig.getRequestTimeoutMs()))
                .orElseThrow(() -> new IllegalStateException("Failed to subscribe for telemetry"));
        monitoringReporter.reportWsConnectLatency(getElapsedTime());
        return wsClient;
    }

    protected abstract void initClient() throws Exception;

    protected String createTestPayload(String testValue) {
        return JacksonUtil.newObjectNode().set(TEST_TELEMETRY_KEY, new TextNode(testValue)).toString();
    }

    protected abstract void sendTestPayload(String payload) throws Exception;

    protected abstract void destroyClient() throws Exception;


    private void startStopWatch() {
        stopWatch.start();
    }

    private long getElapsedTime() {
        stopWatch.stop();
        long nanoTime = stopWatch.getNanoTime();
        stopWatch.reset();
        return nanoTime;
    }

    private void onTransportFailure(Exception e) {
        log.debug("[{}] Transport failure", transportInfo, e);

        int failuresCount = transportFailuresCounter.incrementAndGet();
        if (failuresCount == config.getFailureThreshold()) {
            notificationService.notify(new TransportFailureNotificationInfo(transportInfo, e));
        }
    }

    private void onMonitoringFailure(Exception e) {
        log.debug("[{}] Monitoring failure", transportInfo, e);

        int failuresCount = monitoringFailuresCounter.incrementAndGet();
        if (failuresCount == config.getFailureThreshold()) {
            notificationService.notify(new MonitoringFailureNotificationInfo(transportInfo, e));
        }
    }

    private void onTransportIsOk() {
        log.debug("[{}] Transport is OK", transportInfo);

        if (transportFailuresCounter.get() >= config.getFailureThreshold()) {
            notificationService.notify(new TransportRecoveryNotificationInfo(transportInfo));
        }
        if (monitoringFailuresCounter.get() >= config.getFailureThreshold()) {
            notificationService.notify(new MonitoringRecoveryNotificationInfo(transportInfo));
        }

        transportFailuresCounter.set(0);
        monitoringFailuresCounter.set(0);
    }


    protected abstract TransportType getTransportType();

}
