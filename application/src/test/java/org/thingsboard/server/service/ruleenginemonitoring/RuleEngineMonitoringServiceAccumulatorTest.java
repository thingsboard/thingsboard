// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ruleenginemonitoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.QueueStatsRow;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.RuleEngineMonitoringDao;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.RuleNodeExecStatsRow;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.kafka.KafkaAdmin;
import org.thingsboard.server.service.queue.TbMsgPackProcessingContext;
import org.thingsboard.server.service.queue.processing.TbRuleEngineSubmitStrategy;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The accumulators are drained in place while rule nodes keep recording; no update may be lost or split across the
 * drain, and idle entries must be evicted without dropping a record that races with the eviction.
 */
class RuleEngineMonitoringServiceAccumulatorTest {

    private static final int THREADS = 8;
    private static final int RECORDS_PER_THREAD = 100_000;

    private final UUID tenantId = UUID.randomUUID();
    private final RuleEngineMonitoringDao dao = Mockito.mock(RuleEngineMonitoringDao.class);
    private final AtomicLong flushedExecs = new AtomicLong();
    private final AtomicLong flushedErrors = new AtomicLong();
    private final AtomicLong flushedDurationMs = new AtomicLong();
    private final AtomicLong flushedSuccesses = new AtomicLong();
    private RuleEngineMonitoringService service;

    @BeforeEach
    void setUp() {
        Mockito.doAnswer(invocation -> {
            List<RuleNodeExecStatsRow> rows = invocation.getArgument(0);
            rows.forEach(row -> {
                flushedExecs.addAndGet(row.execCount());
                flushedErrors.addAndGet(row.errorCount());
                flushedDurationMs.addAndGet(row.totalDurationMs());
            });
            return null;
        }).when(dao).saveRuleNodeStats(Mockito.anyList());
        Mockito.doAnswer(invocation -> {
            List<QueueStatsRow> rows = invocation.getArgument(0);
            rows.forEach(row -> flushedSuccesses.addAndGet(row.successCount()));
            return null;
        }).when(dao).saveQueueStats(Mockito.anyList());
        @SuppressWarnings("unchecked")
        ObjectProvider<KafkaAdmin> kafkaAdminProvider = Mockito.mock(ObjectProvider.class);
        service = new RuleEngineMonitoringService(dao, Mockito.mock(TbTenantProfileCache.class),
                Mockito.mock(TbServiceInfoProvider.class), Mockito.mock(TopicService.class), kafkaAdminProvider,
                Mockito.mock(StatsFactory.class, Mockito.RETURNS_DEEP_STUBS), Mockito.mock(QueueCacheService.class),
                30, 30, 1_000_000, 1_000_000, false, 120_000);
    }

    @Test
    void givenConcurrentRuleNodeRecordsAndFlushes_whenAllFlushed_thenEveryRecordIsCountedOnce() throws Exception {
        RuleNode ruleNode = ruleNode();

        raceWithFlushes(service::flushRuleNodeStats, i ->
                service.recordRuleNodeExec(tenantId, ruleNode, "Main", "svc", 2, i % 10 == 0));

        long total = (long) THREADS * RECORDS_PER_THREAD;
        assertThat(flushedExecs.get()).isEqualTo(total);
        assertThat(flushedErrors.get()).isEqualTo(total / 10);
        assertThat(flushedDurationMs.get()).isEqualTo(2 * total);
    }

    @Test
    void givenConcurrentQueueOutcomesAndFlushes_whenAllFlushed_thenEveryOutcomeIsCountedOnce() throws Exception {
        Queue queue = new Queue(new QueueId(UUID.randomUUID()));
        queue.setTenantId(TenantId.fromUUID(tenantId));
        queue.setName("Main");

        raceWithFlushes(service::flushQueueStats, i ->
                service.recordQueueOutcomes(succeededPack(), queue, "svc", Set.of()));

        assertThat(flushedSuccesses.get()).isEqualTo((long) THREADS * RECORDS_PER_THREAD);
    }

    @Test
    void givenIdleEntry_whenFlushedRepeatedly_thenItIsEvictedAndLaterRecordsStillCount() {
        RuleNode ruleNode = ruleNode();
        service.recordRuleNodeExec(tenantId, ruleNode, "Main", "svc", 5, false);
        service.flushRuleNodeStats();
        Map<?, ?> accumulator = (Map<?, ?>) ReflectionTestUtils.getField(service, "ruleNodeAccumulator");
        assertThat(accumulator).hasSize(1);

        for (int i = 0; i < 3; i++) {
            service.flushRuleNodeStats();
        }
        assertThat(accumulator).isEmpty();

        service.recordRuleNodeExec(tenantId, ruleNode, "Main", "svc", 5, false);
        service.flushRuleNodeStats();
        assertThat(flushedExecs.get()).isEqualTo(2);
    }

    private void raceWithFlushes(Runnable flush, java.util.function.IntConsumer record) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS + 1);
        try {
            AtomicBoolean recording = new AtomicBoolean(true);
            CountDownLatch start = new CountDownLatch(1);
            Future<?> flusher = executor.submit(() -> {
                while (recording.get()) {
                    flush.run();
                }
            });
            List<Future<?>> recorders = new java.util.ArrayList<>();
            for (int t = 0; t < THREADS; t++) {
                recorders.add(executor.submit(() -> {
                    start.await();
                    for (int i = 0; i < RECORDS_PER_THREAD; i++) {
                        record.accept(i);
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> recorder : recorders) {
                recorder.get(2, TimeUnit.MINUTES);
            }
            recording.set(false);
            flusher.get(1, TimeUnit.MINUTES);
            flush.run();
        } finally {
            executor.shutdownNow();
        }
    }

    private RuleNode ruleNode() {
        RuleNode ruleNode = new RuleNode(new RuleNodeId(UUID.randomUUID()));
        ruleNode.setRuleChainId(new RuleChainId(UUID.randomUUID()));
        return ruleNode;
    }

    private TbMsgPackProcessingContext succeededPack() {
        UUID id = UUID.randomUUID();
        ConcurrentHashMap<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> pending = new ConcurrentHashMap<>();
        pending.put(id, new TbProtoQueueMsg<>(id, ToRuleEngineMsg.getDefaultInstance()));
        TbRuleEngineSubmitStrategy submitStrategy = Mockito.mock(TbRuleEngineSubmitStrategy.class);
        Mockito.when(submitStrategy.getPendingMap()).thenReturn(pending);
        TbMsgPackProcessingContext packCtx = new TbMsgPackProcessingContext("Main", submitStrategy, false);
        packCtx.onSuccess(id);
        return packCtx;
    }

}
