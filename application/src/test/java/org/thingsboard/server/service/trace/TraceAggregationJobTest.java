// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.trace;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.sql.trace.TraceSummaryInfo;
import org.thingsboard.server.queue.discovery.PartitionService;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TraceAggregationJobTest {
    private static final int DRAIN_BATCH_SPANS = 5_000;

    private TraceGroupAggregator aggregator;
    private TraceGroupPersistenceService persistenceService;
    private TraceSummaryBuilder traceSummaryBuilder;
    private PartitionService partitionService;

    @BeforeEach
    void setUp() {
        aggregator = mock(TraceGroupAggregator.class);
        persistenceService = mock(TraceGroupPersistenceService.class);
        traceSummaryBuilder = mock(TraceSummaryBuilder.class);
        partitionService = mock(PartitionService.class);
    }

    @Test
    void localBufferIsDrainedWithoutCheckingThePartitionOwner() {
        TraceBuffer buffer = bufferWithBatches(false, 1);
        stubAggregation();

        newJob(buffer).run();

        verifyNoInteractions(partitionService);
        verify(persistenceService).persist(anyLong(), anyLong(), any(), any(), any());
    }

    @Test
    void sharedBufferIsDrainedByTheSystemPartitionOwner() {
        TraceBuffer buffer = bufferWithBatches(true, 1);
        stubAggregation();
        when(partitionService.isSystemTenantPartitionMine(ServiceType.TB_RULE_ENGINE)).thenReturn(true);

        newJob(buffer).run();

        verify(persistenceService).persist(anyLong(), anyLong(), any(), any(), any());
    }

    @Test
    void sharedBufferIsLeftAloneWhenAnotherNodeOwnsTheSystemPartition() {
        TraceBuffer buffer = bufferWithBatches(true, 1);
        when(partitionService.isSystemTenantPartitionMine(ServiceType.TB_RULE_ENGINE)).thenReturn(false);

        newJob(buffer).run();

        verify(buffer, never()).drainExpired(anyLong(), anyInt());
        verifyNoInteractions(aggregator, persistenceService);
    }

    @Test
    void runKeepsDrainingUntilTheBufferIsEmpty() {
        TraceBuffer buffer = bufferWithBatches(false, 3);
        stubAggregation();

        newJob(buffer).run();

        verify(buffer, times(4)).drainExpired(anyLong(), eq(DRAIN_BATCH_SPANS));
        verify(persistenceService, times(3)).persist(anyLong(), anyLong(), any(), any(), any());
    }

    // Under steady load new traces keep expiring while a run drains; a run that re-read the clock per batch would
    // never see an empty batch. Every batch must use the time the run started.
    @Test
    void everyBatchOfARunDrainsUpToTheRunStartTime() {
        TraceBuffer buffer = bufferWithBatches(false, 3);
        stubAggregation();
        TraceAggregationJob job = newJob(buffer);
        ReflectionTestUtils.setField(job, "clock", steppingClock(40L));

        job.run();

        verify(buffer, times(4)).drainExpired(eq(0L), eq(DRAIN_BATCH_SPANS));
    }

    @Test
    void aFailingBatchIsDroppedAndTheRunContinues() {
        TraceBuffer buffer = bufferWithBatches(false, 2);
        stubAggregation();
        doThrow(new IllegalStateException("boom")).doNothing()
                .when(persistenceService).persist(anyLong(), anyLong(), any(), any(), any());

        newJob(buffer).run();

        verify(persistenceService, times(2)).persist(anyLong(), anyLong(), any(), any(), any());
    }

    @Test
    void eachDrainedTraceHasItsPathBuiltOnce() {
        TraceBuffer buffer = bufferWithBatches(false, 1);
        stubAggregation();
        TracePathBuilder pathBuilder = spy(new TracePathBuilder());

        newJob(buffer, pathBuilder).run();

        verify(pathBuilder, times(1)).build(any());
    }

    private TraceAggregationJob newJob(TraceBuffer buffer) {
        return newJob(buffer, new TracePathBuilder());
    }

    private TraceAggregationJob newJob(TraceBuffer buffer, TracePathBuilder pathBuilder) {
        return new TraceAggregationJob(buffer, aggregator, persistenceService, traceSummaryBuilder, pathBuilder,
                partitionService, DRAIN_BATCH_SPANS, AbstractTraceBufferTest.statsFactory(new SimpleMeterRegistry()));
    }

    private TraceBuffer bufferWithBatches(boolean shared, int batches) {
        TraceBuffer buffer = mock(TraceBuffer.class);
        when(buffer.isShared()).thenReturn(shared);
        var stubbing = when(buffer.drainExpired(anyLong(), anyInt()));
        for (int i = 0; i < batches; i++) {
            stubbing = stubbing.thenReturn(oneTrace("trace-" + i));
        }
        stubbing.thenReturn(List.of());
        return buffer;
    }

    private static List<BufferedTrace> oneTrace(String traceId) {
        return List.of(new BufferedTrace(UUID.randomUUID(), traceId, false, Set.of()));
    }

    private static Clock steppingClock(long stepMillis) {
        AtomicLong millis = new AtomicLong();
        return new Clock() {
            @Override
            public ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return Instant.ofEpochMilli(millis.getAndAdd(stepMillis));
            }
        };
    }

    private void stubAggregation() {
        when(aggregator.aggregate(any(), anyLong(), anyLong())).thenReturn(Map.of());
        when(traceSummaryBuilder.buildByTraceId(any())).thenReturn(Map.<String, TraceSummaryInfo>of());
    }
}
