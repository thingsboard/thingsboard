// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.integration.api.IntegrationStatisticsKey;
import org.thingsboard.integration.api.IntegrationStatisticsMetricName;
import org.thingsboard.integration.api.IntegrationStatisticsService;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.stats.DefaultCounter;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.StatsType;
import org.thingsboard.server.queue.util.TbCoreOrIntegrationExecutorComponent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
@TbCoreOrIntegrationExecutorComponent
public class DefaultIntegrationStatisticsService implements IntegrationStatisticsService {

    private final Map<IntegrationStatisticsKey, DefaultCounter> counters = new ConcurrentHashMap<>();
    private final Map<IntegrationStatisticsKey, AtomicLong> gauges = new ConcurrentHashMap<>();

    private static final String STATS_KEY_COUNTER = StatsType.INTEGRATION.getName() + "_stats_counter";
    private static final String STATS_KEY_GAUGE = StatsType.INTEGRATION.getName() + "_stats_gauge";

    private final StatsFactory statsFactory;

    @Override
    public void onIntegrationStateUpdate(IntegrationType integrationType, ComponentLifecycleEvent state, boolean success) {
        try {
            if (ComponentLifecycleEvent.STARTED.equals(state)) {
                incrementCounter(new IntegrationStatisticsKey(IntegrationStatisticsMetricName.START, success, integrationType));
            } else if (!success || ComponentLifecycleEvent.FAILED.equals(state)) {
                incrementCounter(new IntegrationStatisticsKey(IntegrationStatisticsMetricName.START, false, integrationType));
            }
        } catch (Exception e) {
            log.error("[{}][{}][{}] Failed to process onIntegrationStateUpdate. ", integrationType, state, success, e);
        }
    }

    @Override
    public void onIntegrationsCountUpdate(IntegrationType integrationType, int started, int failed) {
        try {
            setGaugeValue(new IntegrationStatisticsKey(IntegrationStatisticsMetricName.START, true, integrationType), started);
            setGaugeValue(new IntegrationStatisticsKey(IntegrationStatisticsMetricName.START, false, integrationType), failed);
        } catch (Exception e) {
            log.error("onIntegrationsCountUpdate type: [{}], started: [{}], failed: [{}]", integrationType, started, failed, e);
        }
    }

    @Override
    public void onUplinkMsg(IntegrationType integrationType, boolean success) {
        onMsg(IntegrationStatisticsMetricName.MSGS_UPLINK, integrationType, true);
    }

    @Override
    public void onDownlinkMsg(IntegrationType integrationType, boolean success) {
        onMsg(IntegrationStatisticsMetricName.MSGS_DOWNLINK, integrationType, true);
    }

    private void onMsg(IntegrationStatisticsMetricName metric, IntegrationType integrationType, boolean success) {
        try {
            incrementCounter(new IntegrationStatisticsKey(metric, success, integrationType));
        } catch (Exception e) {
            log.error("[{}][{}] onMsg: [{}]", metric, integrationType, success, e);
        }
    }

    @Override
    public void printStats() {
        if (!counters.isEmpty()) {
            StringBuilder stats = new StringBuilder();
            counters.forEach((key, value) -> stats.append(key).append(" = [").append(value.get()).append("] "));
            log.info("Integration Stats: {}", stats);
        }
    }

    @Override
    public void reset() {
        counters.values().forEach(DefaultCounter::clear);
    }

    private void incrementCounter(IntegrationStatisticsKey tags) {
        getOrCreateStatsCounter(tags).increment();
    }

    private void setGaugeValue(IntegrationStatisticsKey tags, int value) {
        getOrCreateStatsGauge(tags).set(value);
    }

    private DefaultCounter getOrCreateStatsCounter(IntegrationStatisticsKey tags) {
        return counters.computeIfAbsent(tags, s ->
                statsFactory.createDefaultCounter(STATS_KEY_COUNTER, tags.getTags()));
    }

    private AtomicLong getOrCreateStatsGauge(IntegrationStatisticsKey tags) {
        return gauges.computeIfAbsent(tags, s ->
                statsFactory.createGauge(STATS_KEY_GAUGE, new AtomicLong(0), tags.getTags()));
    }

}
