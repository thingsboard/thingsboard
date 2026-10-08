package org.thingsboard.server.service.trace;

import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.stats.DefaultCounter;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.sql.trace.TraceDao;
import org.thingsboard.server.dao.sql.trace.TraceSummaryInfo;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;

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
    private static final long DAY_BUCKET_MS = 86_400_000L;

    private final TraceBuffer buffer;
    private final TraceGroupAggregator aggregator;
    private final TraceGroupPersistenceService persistenceService;
    private final TraceDao traceDao;
    private final TraceSummaryBuilder traceSummaryBuilder;

    private Clock clock = Clock.systemUTC();
    private final String serviceId;
    private final long leaderLeaseMs;
    private final int drainBatchSpans;
    private final Timer runTimer;
    private final DefaultCounter drainedTracesCounter;

    public TraceAggregationJob(TraceBuffer buffer, TraceGroupAggregator aggregator,
                               TraceGroupPersistenceService persistenceService, TraceDao traceDao,
                               TraceSummaryBuilder traceSummaryBuilder, TbServiceInfoProvider serviceInfoProvider,
                               @Value("${otel.processor.trace.aggregation.leader-lease-ms:300000}") long leaderLeaseMs,
                               @Value("${otel.processor.trace.aggregation.drain-batch-spans:5000}") int drainBatchSpans,
                               StatsFactory statsFactory) {
        this.buffer = buffer;
        this.aggregator = aggregator;
        this.persistenceService = persistenceService;
        this.traceDao = traceDao;
        this.traceSummaryBuilder = traceSummaryBuilder;
        this.serviceId = serviceInfoProvider.getServiceId();
        this.leaderLeaseMs = leaderLeaseMs;
        this.drainBatchSpans = drainBatchSpans;
        this.runTimer = statsFactory.createTimer("trace.aggregation.duration");
        this.drainedTracesCounter = statsFactory.createDefaultCounter("trace.aggregation.drained");
    }

    @Scheduled(initialDelayString = "${otel.processor.trace.aggregation.interval-ms:60000}",
            fixedRateString = "${otel.processor.trace.aggregation.interval-ms:60000}")
    public void run() {
        long startedAt = clock.millis();
        try {
            if (!buffer.isShared()) {
                drain(Long.MAX_VALUE);
                return;
            }
            if (!traceDao.tryAcquireAggregationLeader(serviceId, startedAt, leaderLeaseMs)) {
                log.debug("[{}] skip run - another pod holds the aggregation leader lease", serviceId);
                return;
            }
            drain(startedAt + leaderLeaseMs);
        } finally {
            runTimer.record(clock.millis() - startedAt, TimeUnit.MILLISECONDS);
        }
    }

    private void drain(long leaseExpiresAt) {
        int batches = 0;
        int traces = 0;
        while (true) {
            long now = clock.millis();
            if (now >= leaseExpiresAt) {
                log.warn("[{}] aggregation leader lease expired mid-run after {} batches / {} traces - stopping " +
                         "so this run does not drain alongside a new leader; the rest is left for the next run",
                        serviceId, batches, traces);
                return;
            }
            List<LoadedTrace> batch = buffer.drainExpired(now, drainBatchSpans);
            if (batch.isEmpty()) {
                if (batches > 0) {
                    log.debug("[{}] buffer drained: {} batches / {} traces", serviceId, batches, traces);
                }
                return;
            }
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

    private void aggregateAndPersist(List<LoadedTrace> loaded, long now) {
        long bucketTime = floorToDay(now);
        Map<TraceGroupKey, TraceGroupContribution> contributions = aggregator.aggregate(loaded, bucketTime, now);
        Map<String, LoadedTrace> tracesByTraceId = loaded.stream()
                .collect(Collectors.toMap(t -> t.trace().traceId(), Function.identity(), (a, b) -> a));
        Map<String, TraceSummaryInfo> summariesByTraceId = traceSummaryBuilder.buildByTraceId(loaded);
        persistenceService.persist(bucketTime, now,
                contributions.values(), tracesByTraceId, summariesByTraceId);
    }

    private static long floorToDay(long timestampMs) {
        return (timestampMs / DAY_BUCKET_MS) * DAY_BUCKET_MS;
    }
}
