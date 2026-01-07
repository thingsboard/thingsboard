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

import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.monitoring.client.TbClient;
import org.thingsboard.monitoring.data.Latency;
import org.thingsboard.monitoring.data.MonitoredServiceKey;
import org.thingsboard.monitoring.data.notification.HighLatencyNotification;
import org.thingsboard.monitoring.data.notification.ServiceFailureNotification;
import org.thingsboard.monitoring.data.notification.ServiceRecoveryNotification;
import org.thingsboard.monitoring.notification.NotificationService;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class MonitoringReporter {

    private final NotificationService notificationService;
    private final TbClient tbClient;
    private final MonitoringEntityService entityService;

    private final Map<String, Latency> latencies = new ConcurrentHashMap<>();
    private final Map<Object, AtomicInteger> failuresCounters = new ConcurrentHashMap<>();

    @Value("${monitoring.failures_threshold}")
    private int failuresThreshold;
    @Value("${monitoring.repeated_failure_notification}")
    private int repeatedFailureNotification;

    @Value("${monitoring.latency.enabled}")
    private boolean latencyReportingEnabled;
    @Value("${monitoring.latency.threshold_ms}")
    private int latencyThresholdMs;
    @Value("${monitoring.latency.reporting_asset_id}")
    private String reportingAssetId;

    public void reportLatencies() {
        if (latencies.isEmpty()) {
            return;
        }
        log.debug("Latencies:\n{}", latencies.values().stream().map(latency -> latency.getKey() + ": " + latency.getFormattedValue())
                .collect(Collectors.joining("\n")) + "\n");
        if (!latencyReportingEnabled) return;

        List<Latency> highLatencies = latencies.values().stream()
                .filter(latency -> latency.getValue() >= (double) latencyThresholdMs)
                .collect(Collectors.toList());
        if (!highLatencies.isEmpty()) {
            HighLatencyNotification highLatencyNotification = new HighLatencyNotification(highLatencies, latencyThresholdMs);
            notificationService.sendNotification(highLatencyNotification);
            log.warn("{}", highLatencyNotification.getText());
        }

        try {
            if (StringUtils.isBlank(reportingAssetId)) {
                Asset monitoringAsset = entityService.getOrCreateMonitoringAsset();
                reportingAssetId = monitoringAsset.getId().toString();
            }

            ObjectNode msg = JacksonUtil.newObjectNode();
            latencies.values().forEach(latency -> {
                msg.set(latency.getKey(), new DoubleNode(latency.getValue()));
            });
            tbClient.saveEntityTelemetry(new AssetId(UUID.fromString(reportingAssetId)), "time", msg);
            latencies.clear();
        } catch (Exception e) {
            log.error("Failed to report latencies: {}", e.getMessage());
        }
    }

    public void reportLatency(String key, long latencyInNanos) {
        String latencyKey = key + "Latency";
        double latencyInMs = (double) latencyInNanos / 1000_000;
        log.trace("Reporting latency [{}]: {} ms", key, latencyInMs);
        latencies.put(latencyKey, Latency.of(latencyKey, latencyInMs));
    }

    public void serviceFailure(Object serviceKey, Throwable error) {
        if (log.isDebugEnabled()) {
            log.error("[{}] Error occurred", serviceKey, error);
        }
        int failuresCount = failuresCounters.computeIfAbsent(serviceKey, k -> new AtomicInteger()).incrementAndGet();
        ServiceFailureNotification notification = new ServiceFailureNotification(serviceKey, error, failuresCount);
        log.error(notification.getText());
        if (failuresCount == failuresThreshold || (repeatedFailureNotification != 0 && failuresCount % repeatedFailureNotification == 0)) {
            notificationService.sendNotification(notification);
        }
    }

    public void serviceIsOk(Object serviceKey) {
        ServiceRecoveryNotification notification = new ServiceRecoveryNotification(serviceKey);
        if (!serviceKey.equals(MonitoredServiceKey.GENERAL)) {
            log.info(notification.getText());
        }
        AtomicInteger failuresCounter = failuresCounters.get(serviceKey);
        if (failuresCounter != null) {
            if (failuresCounter.get() >= failuresThreshold) {
                notificationService.sendNotification(notification);
            }
            failuresCounter.set(0);
        }
    }

}
