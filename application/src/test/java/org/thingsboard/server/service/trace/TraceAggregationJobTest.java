package org.thingsboard.server.service.trace;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.dao.sql.trace.TraceDao;
import org.thingsboard.server.dao.sql.trace.TraceSummaryInfo;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;

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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TraceAggregationJobTest {
    private static final String SERVICE_ID = "tb-rule-engine-0";
    private static final long LEADER_LEASE_MS = 300_000L;
    private static final int DRAIN_BATCH_SPANS = 5_000;

    private TraceDao traceDao;
    private TraceGroupAggregator aggregator;
    private TraceGroupPersistenceService persistenceService;
    private TraceSummaryBuilder traceSummaryBuilder;
    private TbServiceInfoProvider serviceInfoProvider;

    @BeforeEach
    void setUp() {
        traceDao = mock(TraceDao.class);
        aggregator = mock(TraceGroupAggregator.class);
        persistenceService = mock(TraceGroupPersistenceService.class);
        traceSummaryBuilder = mock(TraceSummaryBuilder.class);
        serviceInfoProvider = mock(TbServiceInfoProvider.class);
        when(serviceInfoProvider.getServiceId()).thenReturn(SERVICE_ID);
    }

    @Test
    void localBufferIsDrainedWithoutConsultingTheLease() {
        TraceBuffer buffer = bufferWithBatches(false, 1);
        stubAggregation();

        newJob(buffer).run();

        verify(traceDao, never()).tryAcquireAggregationLeader(anyString(), anyLong(), anyLong());
        verify(persistenceService).persist(anyLong(), anyLong(), any(), any(), any());
    }

    @Test
    void localBufferIsDrainedEvenWhileAnotherServiceHoldsTheLease() {
        TraceBuffer buffer = bufferWithBatches(false, 1);
        stubAggregation();
        when(traceDao.tryAcquireAggregationLeader(anyString(), anyLong(), anyLong())).thenReturn(false);

        newJob(buffer).run();

        verify(persistenceService).persist(anyLong(), anyLong(), any(), any(), any());
    }

    @Test
    void sharedBufferIsDrainedOnlyByTheLeaseHolder() {
        TraceBuffer buffer = bufferWithBatches(true, 1);
        stubAggregation();
        when(traceDao.tryAcquireAggregationLeader(eq(SERVICE_ID), anyLong(), anyLong())).thenReturn(true);

        newJob(buffer).run();

        verify(traceDao).tryAcquireAggregationLeader(anyString(), anyLong(), anyLong());
        verify(persistenceService).persist(anyLong(), anyLong(), any(), any(), any());
    }

    @Test
    void sharedBufferIsLeftAloneWhenAnotherServiceHoldsTheLease() {
        TraceBuffer buffer = bufferWithBatches(true, 1);
        when(traceDao.tryAcquireAggregationLeader(anyString(), anyLong(), anyLong())).thenReturn(false);

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

    @Test
    void aSharedBufferRunStopsOnceItsLeaderLeaseHasExpired() {
        TraceBuffer buffer = bufferWithEndlessBatches(true);
        stubAggregation();
        when(traceDao.tryAcquireAggregationLeader(anyString(), anyLong(), anyLong())).thenReturn(true);

        TraceAggregationJob job = newJob(buffer, 100L);

        ReflectionTestUtils.setField(job, "clock", steppingClock(40L));

        job.run();

        verify(buffer, times(2)).drainExpired(anyLong(), anyInt());
        verify(persistenceService, times(2)).persist(anyLong(), anyLong(), any(), any(), any());
    }

    @Test
    void aLocalBufferRunIsNotBoundedByTheLeaseDuration() {
        TraceBuffer buffer = bufferWithBatches(false, 3);
        stubAggregation();

        TraceAggregationJob job = newJob(buffer, 1L);
        ReflectionTestUtils.setField(job, "clock", steppingClock(40L));

        job.run();

        verify(buffer, times(4)).drainExpired(anyLong(), anyInt());
        verify(persistenceService, times(3)).persist(anyLong(), anyLong(), any(), any(), any());
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

    private TraceAggregationJob newJob(TraceBuffer buffer) {
        return newJob(buffer, LEADER_LEASE_MS);
    }

    private TraceAggregationJob newJob(TraceBuffer buffer, long leaderLeaseMs) {
        return new TraceAggregationJob(buffer, aggregator, persistenceService, traceDao, traceSummaryBuilder,
                serviceInfoProvider, leaderLeaseMs, DRAIN_BATCH_SPANS,
                AbstractTraceBufferTest.statsFactory(new SimpleMeterRegistry()));
    }

    private TraceBuffer bufferWithEndlessBatches(boolean shared) {
        TraceBuffer buffer = mock(TraceBuffer.class);
        when(buffer.isShared()).thenReturn(shared);
        when(buffer.drainExpired(anyLong(), anyInt())).thenReturn(oneTrace("trace-key"));
        return buffer;
    }

    private TraceBuffer bufferWithBatches(boolean shared, int batches) {
        TraceBuffer buffer = mock(TraceBuffer.class);
        when(buffer.isShared()).thenReturn(shared);
        var stubbing = when(buffer.drainExpired(anyLong(), anyInt()));
        for (int i = 0; i < batches; i++) {
            stubbing = stubbing.thenReturn(oneTrace("trace-key-" + i));
        }
        stubbing.thenReturn(List.of());
        return buffer;
    }

    private static List<LoadedTrace> oneTrace(String bufferKey) {
        return List.of(new LoadedTrace(bufferKey, new BufferedTrace(UUID.randomUUID(), "trace-1", false, Set.of())));
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
