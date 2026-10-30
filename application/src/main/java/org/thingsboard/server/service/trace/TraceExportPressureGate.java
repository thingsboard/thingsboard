// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.trace;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.stats.DefaultCounter;
import org.thingsboard.server.common.stats.StatsFactory;

import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@ConditionalOnExpression(TraceConditions.TRACE_ENABLED)
public class TraceExportPressureGate {
    private final AtomicLong inFlight;
    // The only reader of batch.max-queue-size: OpenTelemetryConfig sizes the BatchSpanProcessor from this value, so
    // the thresholds below are always percentages of the real queue.
    @Getter
    private final int maxQueueSize;
    private final long capacity;
    private final long traceIdThreshold;
    private final long spanThreshold;

    private final DefaultCounter deniedTraceIds;
    private final DefaultCounter deniedSpans;
    private final DefaultCounter shedSpans;

    public TraceExportPressureGate(StatsFactory statsFactory,
                                   @Value("${otel.processor.trace.batch.max-queue-size:131072}") int maxQueueSize,
                                   @Value("${otel.processor.trace.pressure.trace-id-max-queue-usage-pct:50}") int traceIdMaxUsagePct,
                                   @Value("${otel.processor.trace.pressure.span-max-queue-usage-pct:100}") int spanMaxUsagePct) {
        this.inFlight = statsFactory.createGauge("trace.pressure.inFlight", new AtomicLong(0));
        // OpenTelemetry rounds the queue up to a power of two; round the same way so the thresholds below are
        // percentages of the real queue size.
        this.maxQueueSize = roundUpToPowerOfTwo(maxQueueSize);
        if (this.maxQueueSize != maxQueueSize) {
            log.info("Trace export queue size [{}] rounded up to a power of two: [{}]", maxQueueSize, this.maxQueueSize);
        }
        this.capacity = this.maxQueueSize;
        this.traceIdThreshold = threshold(this.capacity, traceIdMaxUsagePct);
        this.spanThreshold = threshold(this.capacity, spanMaxUsagePct);
        this.deniedTraceIds = statsFactory.createDefaultCounter("trace.pressure.denied", "type", "traceId");
        this.deniedSpans = statsFactory.createDefaultCounter("trace.pressure.denied", "type", "span");
        this.shedSpans = statsFactory.createDefaultCounter("trace.pressure.denied", "type", "shed");
        log.info("Trace export pressure gate: capacity = [{}], trace id threshold = [{}], span threshold = [{}]",
                this.capacity, this.traceIdThreshold, this.spanThreshold);
    }

    static int roundUpToPowerOfTwo(int value) {
        if (value <= 1) {
            return 1;
        }
        return value > (1 << 30) ? 1 << 30 : Integer.highestOneBit(value - 1) << 1;
    }

    private static long threshold(long capacity, int pct) {
        return capacity * Math.max(0, Math.min(100, pct)) / 100;
    }

    public boolean canIssueTraceId() {
        if (inFlight.get() < traceIdThreshold) {
            return true;
        }
        deniedTraceIds.increment();
        return false;
    }

    public boolean canCreateSpan() {
        if (inFlight.get() < spanThreshold) {
            return true;
        }
        deniedSpans.increment();
        return false;
    }

    // Counts ended spans queued for export, not started ones: a span that never ends (timed-out pack,
    // lost callback) then holds no slot.
    boolean admitEndedSpan() {
        if (inFlight.incrementAndGet() > capacity) {
            inFlight.decrementAndGet();
            shedSpans.increment();
            return false;
        }
        return true;
    }

    void onExported(int spans) {
        if (spans > 0) {
            inFlight.addAndGet(-spans);
        }
    }

    long getInFlight() {
        return inFlight.get();
    }
}
