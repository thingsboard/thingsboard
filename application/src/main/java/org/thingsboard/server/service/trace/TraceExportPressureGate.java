/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.service.trace;

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
        this.capacity = Math.max(1, maxQueueSize);
        this.traceIdThreshold = threshold(this.capacity, traceIdMaxUsagePct);
        this.spanThreshold = threshold(this.capacity, spanMaxUsagePct);
        this.deniedTraceIds = statsFactory.createDefaultCounter("trace.pressure.denied", "type", "traceId");
        this.deniedSpans = statsFactory.createDefaultCounter("trace.pressure.denied", "type", "span");
        this.shedSpans = statsFactory.createDefaultCounter("trace.pressure.denied", "type", "shed");
        log.info("Trace export pressure gate: capacity = [{}], trace id threshold = [{}], span threshold = [{}]",
                this.capacity, this.traceIdThreshold, this.spanThreshold);
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
