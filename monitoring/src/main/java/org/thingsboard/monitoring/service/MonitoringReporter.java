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

import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.monitoring.client.TbClient;
import org.thingsboard.monitoring.data.Latency;
import org.thingsboard.monitoring.data.notification.HighLatencyNotification;
import org.thingsboard.monitoring.data.notification.ServiceFailureNotification;
import org.thingsboard.monitoring.data.notification.ServiceRecoveryNotification;
import org.thingsboard.monitoring.notification.NotificationService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class MonitoringReporter {

    private final TbClient tbClient;
    private final NotificationService notificationService;
    private final ScheduledExecutorService monitoringExecutor;

    private final Map<String, Latency> latencies = new ConcurrentHashMap<>();
    private final Map<Object, AtomicInteger> failuresCounters = new ConcurrentHashMap<>();

    @Value("${monitoring.failures_threshold}")
    private int failuresThreshold;
    @Value("${monitoring.latency.threshold_ms}")
    private int latencyThresholdMs;
    @Value("${monitoring.send_repeated_failure_notification}")
    private boolean sendRepeatedFailureNotification;
    @Value("${monitoring.latency.reporting_entity_type}")
    private EntityType reportingEntityType;
    @Value("${monitoring.latency.reporting_entity_id}")
    private String reportingEntityId;
    @Value("${monitoring.latency.monitoring_rate_ms}")
    private int latenciesMonitoringRateMs;

    @EventListener(ApplicationReadyEvent.class)
    public void startLatenciesMonitoring() {
        monitoringExecutor.scheduleWithFixedDelay(() -> {
            if (latencies.isEmpty()) {
                return;
            }
            log.info("Latencies:\n{}", latencies.values());
            if (latencies.values().stream().anyMatch(latency -> latency.getAvg() >= (double) latencyThresholdMs)) {
                HighLatencyNotification highLatencyNotification = new HighLatencyNotification(latencies.values(), latencyThresholdMs);
                notificationService.sendNotification(highLatencyNotification);
            }

            if (reportingEntityType != null && StringUtils.isNotBlank(reportingEntityId)) {
                try {
                    EntityId entityId;
                    try {
                        entityId = EntityIdFactory.getByTypeAndUuid(reportingEntityType, reportingEntityId);
                    } catch (Exception e) {
                        return;
                    }
                    tbClient.logIn();
                    ObjectNode msg = JacksonUtil.newObjectNode();
                    latencies.forEach((key, latency) -> {
                        msg.set(key, new DoubleNode(latency.getAvg()));
                        latency.reset();
                    });
                    tbClient.saveEntityTelemetry(entityId, "time", msg);
                } catch (Exception e) {
                    log.error("Failed to report latencies: {}", e.getMessage());
                }
            }
        }, latenciesMonitoringRateMs, latenciesMonitoringRateMs, TimeUnit.MILLISECONDS);
    }

    public void reportLatency(String key, long latencyInNanos) {
        String latencyKey = key + "Latency";
        double latencyInMs = (double) latencyInNanos / 1000_000;
        latencies.computeIfAbsent(key, k -> new Latency(latencyKey)).report(latencyInMs);
    }

    public void serviceFailure(Object serviceKey, Exception error) {
        int failuresCount = failuresCounters.computeIfAbsent(serviceKey, k -> new AtomicInteger()).incrementAndGet();
        ServiceFailureNotification notification = new ServiceFailureNotification(serviceKey, error, failuresCount);
        log.error(notification.getText());
        if (failuresCount == failuresThreshold || (sendRepeatedFailureNotification && failuresCount % failuresThreshold == 0)) {
            notificationService.sendNotification(notification);
        }
    }

    public void serviceIsOk(Object serviceKey) {
        ServiceRecoveryNotification notification = new ServiceRecoveryNotification(serviceKey);
        log.info(notification.getText());

        AtomicInteger failuresCounter = failuresCounters.get(serviceKey);
        if (failuresCounter != null) {
            if (failuresCounter.get() >= failuresThreshold) {
                notificationService.sendNotification(notification);
            }
            failuresCounter.set(0);
        }
    }

}
