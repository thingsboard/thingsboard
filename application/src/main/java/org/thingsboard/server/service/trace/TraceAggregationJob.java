// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.trace;

import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.stats.DefaultCounter;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.TraceGroup;
import org.thingsboard.server.dao.sql.trace.TraceSummaryInfo;
import org.thingsboard.server.queue.discovery.PartitionService;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@ConditionalOnExpression(TraceConditions.TRACE_ENABLED)
public class TraceAggregationJob {
    private final TraceBuffer buffer;
    private final TraceGroupAggregator aggregator;
    private final TraceGroupPersistenceService persistenceService;
    private final TraceSummaryBuilder traceSummaryBuilder;
    private final PartitionService partitionService;

    private Clock clock = Clock.systemUTC();
    private final int drainBatchSpans;
    private final Timer runTimer;
    private final DefaultCounter drainedTracesCounter;

    public TraceAggregationJob(TraceBuffer buffer, TraceGroupAggregator aggregator,
                               TraceGroupPersistenceService persistenceService,
                               TraceSummaryBuilder traceSummaryBuilder, PartitionService partitionService,
                               @Value("${otel.processor.trace.aggregation.drain-batch-spans:5000}") int drainBatchSpans,
                               StatsFactory statsFactory) {
        this.buffer = buffer;
        this.aggregator = aggregator;
        this.persistenceService = persistenceService;
        this.traceSummaryBuilder = traceSummaryBuilder;
        this.partitionService = partitionService;
        this.drainBatchSpans = drainBatchSpans;
        this.runTimer = statsFactory.createTimer("trace.aggregation.duration");
        this.drainedTracesCounter = statsFactory.createDefaultCounter("trace.aggregation.drained");
    }

    @Scheduled(initialDelayString = "${otel.processor.trace.aggregation.interval-ms:10000}",
            fixedRateString = "${otel.processor.trace.aggregation.interval-ms:10000}")
    public void run() {
        long startedAt = clock.millis();
        try {
            // A local buffer holds only this node's spans, so every node drains its own. The shared buffer is drained
            // by the node that owns the system rule engine partition, which the cluster assigns to exactly one node.
            if (buffer.isShared() && !partitionService.isSystemTenantPartitionMine(ServiceType.TB_RULE_ENGINE)) {
                log.debug("Skip run - the system rule engine partition belongs to another node");
                return;
            }
            drain(startedAt);
        } finally {
            runTimer.record(clock.millis() - startedAt, TimeUnit.MILLISECONDS);
        }
    }

    // Drains only the traces that had expired when the run started, so the run ends even under steady load; traces
    // that expire meanwhile are left for the next run.
    private void drain(long cutoff) {
        int batches = 0;
        int traces = 0;
        while (true) {
            List<BufferedTrace> batch = buffer.drainExpired(cutoff, drainBatchSpans);
            if (batch.isEmpty()) {
                if (batches > 0) {
                    log.debug("Buffer drained: {} batches / {} traces", batches, traces);
                }
                return;
            }
            long now = clock.millis();
            batches++;
            traces += batch.size();
            drainedTracesCounter.add(batch.size());
            try {
                aggregateAndPersist(batch, now);
            } catch (Exception e) {
                log.warn("Failed to aggregate/persist {} drained traces; they are dropped (no retry)",
                        batch.size(), e);
            }
        }
    }

    private void aggregateAndPersist(List<BufferedTrace> loaded, long now) {
        long bucketTime = TraceGroup.floorToDay(now);
        Map<TraceGroupKey, TraceGroupContribution> contributions = aggregator.aggregate(loaded, bucketTime, now);
        Map<String, BufferedTrace> tracesByTraceId = loaded.stream()
                .collect(Collectors.toMap(BufferedTrace::traceId, Function.identity(), (a, b) -> a));
        Map<String, TraceSummaryInfo> summariesByTraceId = traceSummaryBuilder.buildByTraceId(loaded);
        persistenceService.persist(bucketTime, now,
                contributions.values(), tracesByTraceId, summariesByTraceId);
    }
}
