// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ruleenginemonitoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.RuleEngineMonitoringDao;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.kafka.KafkaAdmin;
import org.thingsboard.server.service.queue.TbMsgPackProcessingContext;
import org.thingsboard.server.service.queue.processing.TbRuleEngineSubmitStrategy;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEngineMonitoringServiceQueueOutcomesTest {

    private final UUID tenantId = UUID.randomUUID();
    private final Queue queue = new Queue(new QueueId(UUID.randomUUID()));
    private RuleEngineMonitoringService service;

    @BeforeEach
    void setUp() {
        queue.setTenantId(TenantId.SYS_TENANT_ID);
        queue.setName("Main");
        @SuppressWarnings("unchecked")
        ObjectProvider<KafkaAdmin> kafkaAdminProvider = Mockito.mock(ObjectProvider.class);
        service = new RuleEngineMonitoringService(Mockito.mock(RuleEngineMonitoringDao.class), Mockito.mock(TbTenantProfileCache.class),
                Mockito.mock(TbServiceInfoProvider.class), Mockito.mock(TopicService.class), kafkaAdminProvider,
                Mockito.mock(StatsFactory.class, Mockito.RETURNS_DEEP_STUBS), Mockito.mock(QueueCacheService.class),
                30, 30, 1_000_000, 1_000_000, false, 120_000);
    }

    @Test
    void givenMessagesFinishingWhileOutcomesAreRecorded_whenRecorded_thenEachMessageCountsOnce() {
        List<UUID> ids = msgIds(5);
        TbMsgPackProcessingContext packCtx = Mockito.spy(pack(ids));
        // Two messages complete after pending has been read, before the success map is.
        Mockito.doAnswer(invocation -> {
            packCtx.onSuccess(ids.get(0));
            packCtx.onSuccess(ids.get(1));
            return invocation.callRealMethod();
        }).when(packCtx).getFailedMap();

        service.recordQueueOutcomes(packCtx, queue, "svc", Set.of());

        assertThat(outcomes()).containsExactlyInAnyOrderEntriesOf(Map.of(MetricType.TIMEOUT, 3L, MetricType.SUCCESS, 2L));
    }

    @Test
    void givenTimedOutMessagesToBeRetried_whenRecorded_thenOnlyFinalOutcomesCount() {
        List<UUID> ids = msgIds(4);
        TbMsgPackProcessingContext attempt = pack(ids);
        attempt.onSuccess(ids.get(0));
        attempt.onSuccess(ids.get(1));

        service.recordQueueOutcomes(attempt, queue, "svc", Set.of(ids.get(2), ids.get(3)));
        assertThat(outcomes()).containsExactlyInAnyOrderEntriesOf(Map.of(MetricType.SUCCESS, 2L));

        TbMsgPackProcessingContext retry = pack(ids.subList(2, 4));
        retry.onSuccess(ids.get(2));
        service.recordQueueOutcomes(retry, queue, "svc", Set.of());
        assertThat(outcomes()).containsExactlyInAnyOrderEntriesOf(Map.of(MetricType.SUCCESS, 1L, MetricType.TIMEOUT, 1L));
    }

    @SuppressWarnings("unchecked")
    private Map<MetricType, Long> outcomes() {
        var accumulator = ((AtomicReference<ConcurrentHashMap<QueueKey, QueueMetrics>>) ReflectionTestUtils.getField(service, "queueAccumulatorRef")).get();
        Map<MetricType, Long> byType = new EnumMap<>(MetricType.class);
        accumulator.forEach((key, metrics) -> {
            assertThat(key.tenantId()).isEqualTo(tenantId);
            byType.merge(key.metricType(), metrics.getAndReset(), Long::sum);
        });
        accumulator.clear();
        byType.values().removeIf(count -> count == 0);
        return byType;
    }

    private TbMsgPackProcessingContext pack(List<UUID> ids) {
        ConcurrentHashMap<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> pending = new ConcurrentHashMap<>();
        ToRuleEngineMsg proto = ToRuleEngineMsg.newBuilder()
                .setTenantIdMSB(tenantId.getMostSignificantBits())
                .setTenantIdLSB(tenantId.getLeastSignificantBits())
                .build();
        ids.forEach(id -> pending.put(id, new TbProtoQueueMsg<>(id, proto)));
        TbRuleEngineSubmitStrategy submitStrategy = Mockito.mock(TbRuleEngineSubmitStrategy.class);
        Mockito.when(submitStrategy.getPendingMap()).thenReturn(pending);
        return new TbMsgPackProcessingContext("Main", submitStrategy, false);
    }

    private static List<UUID> msgIds(int count) {
        return IntStream.range(0, count).mapToObj(i -> UUID.randomUUID()).toList();
    }

}
