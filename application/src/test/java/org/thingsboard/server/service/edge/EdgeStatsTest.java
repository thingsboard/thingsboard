// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge;

import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TimeseriesSaveResult;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.edge.stats.EdgeStatsCounterService;
import org.thingsboard.server.dao.edge.stats.MsgCounters;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.kafka.KafkaAdmin;
import org.thingsboard.server.service.edge.stats.EdgeStatsService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.dao.edge.stats.EdgeStatsKey.DOWNLINK_MSGS_ADDED;
import static org.thingsboard.server.dao.edge.stats.EdgeStatsKey.DOWNLINK_MSGS_LAG;
import static org.thingsboard.server.dao.edge.stats.EdgeStatsKey.DOWNLINK_MSGS_PERMANENTLY_FAILED;
import static org.thingsboard.server.dao.edge.stats.EdgeStatsKey.DOWNLINK_MSGS_PUSHED;
import static org.thingsboard.server.dao.edge.stats.EdgeStatsKey.DOWNLINK_MSGS_TMP_FAILED;

@ExtendWith(MockitoExtension.class)
public class EdgeStatsTest {

    private static final int TTL_DAYS = 30;
    private static final long REPORT_INTERVAL_MILLIS = 600_000L;

    private static final long EXPECTED_MSGS_ADDED = 5L;
    private static final long EXPECTED_MSGS_PUSHED = 3L;
    private static final long EXPECTED_MSGS_PERMANENTLY_FAILED = 1L;
    private static final long EXPECTED_MSGS_TMP_FAILED = 0L;
    private static final long EXPECTED_MSGS_LAG = 10L;
    private static final long EXPECTED_MSGS_KAFKA_LAG = 15L;
    private static final long EXPECTED_MSGS_PENDING_LAG = 42L;
    private static final int MAX_LAG_COUNT = 100_000;
    private static final long MISORDERING_COMPENSATION_MILLIS = 60_000L;
    private static final long QUEUE_START_TS = 1_700_000_000_000L;
    private static final long QUEUE_START_SEQ_ID = 777L;

    @Mock
    private TimeseriesService tsService;
    @Mock
    private TopicService topicService;
    @Mock
    private EdgeStatsCounterService statsCounterService;
    @Mock
    private AttributesService attributesService;
    @Mock
    private EdgeEventService edgeEventService;
    private EdgeStatsService edgeStatsService;

    @Captor
    private ArgumentCaptor<List<TsKvEntry>> captor;

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
    private final EdgeId edgeId = new EdgeId(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        edgeStatsService = createEdgeStatsService(Optional.empty());
    }

    private EdgeStatsService createEdgeStatsService(Optional<KafkaAdmin> kafkaAdmin) {
        return createEdgeStatsService(statsCounterService, kafkaAdmin);
    }

    private EdgeStatsService createEdgeStatsService(EdgeStatsCounterService counterService, Optional<KafkaAdmin> kafkaAdmin) {
        EdgeStatsService service = new EdgeStatsService(
                tsService,
                counterService,
                topicService,
                kafkaAdmin,
                attributesService,
                edgeEventService
        );
        ReflectionTestUtils.setField(service, "edgesStatsTtlDays", TTL_DAYS);
        ReflectionTestUtils.setField(service, "reportIntervalMillis", REPORT_INTERVAL_MILLIS);
        ReflectionTestUtils.setField(service, "maxLagCount", MAX_LAG_COUNT);
        ReflectionTestUtils.setField(service, "misorderingCompensationMillis", MISORDERING_COMPENSATION_MILLIS);
        return service;
    }

    @Test
    public void testReportStatsSavesTelemetry() {
        // GIVEN
        setupCounters();

        // WHEN
        edgeStatsService.reportStats();

        // THEN
        Map<String, Long> counters = verifyCounters();
        Assertions.assertEquals(EXPECTED_MSGS_LAG, counters.get(DOWNLINK_MSGS_LAG.getKey()).longValue());
    }

    @Test
    public void testReportStatsWithKafkaLag() {
        // GIVEN
        setupCounters();
        setupKafkaLag();

        // WHEN
        edgeStatsService.reportStats();

        // THEN
        Map<String, Long> valuesByKey = verifyCounters();
        Assertions.assertEquals(EXPECTED_MSGS_KAFKA_LAG, valuesByKey.get(DOWNLINK_MSGS_LAG.getKey()));
        verifyNoInteractions(attributesService, edgeEventService);
    }

    @Test
    public void testReportStatsCountsPendingEventsWhenLagNotMeasured() throws Exception {
        // GIVEN
        setupCounters(false);
        when(attributesService.find(eq(tenantId), eq(edgeId), eq(AttributeScope.SERVER_SCOPE), anyList()))
                .thenReturn(Futures.immediateFuture(List.<AttributeKvEntry>of(
                        new BaseAttributeKvEntry(new LongDataEntry(DataConstants.EDGE_QUEUE_START_TS_ATTR_KEY, QUEUE_START_TS), 0L),
                        new BaseAttributeKvEntry(new LongDataEntry(DataConstants.EDGE_QUEUE_START_SEQ_ID_ATTR_KEY, QUEUE_START_SEQ_ID), 0L))));
        when(edgeEventService.countEdgeEvents(tenantId, edgeId, QUEUE_START_SEQ_ID, QUEUE_START_TS - MISORDERING_COMPENSATION_MILLIS, MAX_LAG_COUNT))
                .thenReturn(EXPECTED_MSGS_PENDING_LAG);

        // WHEN
        edgeStatsService.reportStats();

        // THEN
        Map<String, Long> valuesByKey = verifyCounters();
        Assertions.assertEquals(EXPECTED_MSGS_PENDING_LAG, valuesByKey.get(DOWNLINK_MSGS_LAG.getKey()));
    }

    @Test
    public void testReportStatsCountsAllEventsWhenQueueOffsetMissing() throws Exception {
        // GIVEN
        setupCounters(false);
        when(attributesService.find(eq(tenantId), eq(edgeId), eq(AttributeScope.SERVER_SCOPE), anyList()))
                .thenReturn(Futures.immediateFuture(List.of()));
        when(edgeEventService.countEdgeEvents(tenantId, edgeId, 0L, 0L, MAX_LAG_COUNT)).thenReturn(EXPECTED_MSGS_PENDING_LAG);

        // WHEN
        edgeStatsService.reportStats();

        // THEN
        Map<String, Long> valuesByKey = verifyCounters();
        Assertions.assertEquals(EXPECTED_MSGS_PENDING_LAG, valuesByKey.get(DOWNLINK_MSGS_LAG.getKey()));
    }

    @Test
    public void testReportStatsDoesNotCountPendingEventsWhenLagMeasured() {
        // GIVEN
        setupCounters(true);

        // WHEN
        edgeStatsService.reportStats();

        // THEN
        Map<String, Long> valuesByKey = verifyCounters();
        Assertions.assertEquals(EXPECTED_MSGS_LAG, valuesByKey.get(DOWNLINK_MSGS_LAG.getKey()));
        verifyNoInteractions(attributesService, edgeEventService);
    }

    @Test
    public void testReportStatsCountsPendingEventsAfterSessionDisconnect() throws Exception {
        // GIVEN: a session measured lag 0 earlier in the interval, then the edge disconnected and new events were added
        EdgeStatsCounterService counterService = new EdgeStatsCounterService();
        counterService.recordEvent(DOWNLINK_MSGS_LAG, tenantId, edgeId, 0);
        counterService.resetLagMeasured(edgeId);
        counterService.recordEvent(DOWNLINK_MSGS_ADDED, tenantId, edgeId, EXPECTED_MSGS_ADDED);
        when(attributesService.find(eq(tenantId), eq(edgeId), eq(AttributeScope.SERVER_SCOPE), anyList()))
                .thenReturn(Futures.immediateFuture(List.of()));
        when(edgeEventService.countEdgeEvents(tenantId, edgeId, 0L, 0L, MAX_LAG_COUNT)).thenReturn(EXPECTED_MSGS_PENDING_LAG);
        when(tsService.save(eq(tenantId), eq(edgeId), captor.capture(), anyLong()))
                .thenReturn(Futures.immediateFuture(mock(TimeseriesSaveResult.class)));
        edgeStatsService = createEdgeStatsService(counterService, Optional.empty());

        // WHEN
        edgeStatsService.reportStats();

        // THEN
        Map<String, Long> valuesByKey = captor.getValue().stream()
                .collect(Collectors.toMap(TsKvEntry::getKey, e -> e.getLongValue().orElse(-1L)));
        Assertions.assertEquals(EXPECTED_MSGS_PENDING_LAG, valuesByKey.get(DOWNLINK_MSGS_LAG.getKey()));
        Assertions.assertEquals(EXPECTED_MSGS_ADDED, valuesByKey.get(DOWNLINK_MSGS_ADDED.getKey()));
    }

    @Test
    public void testResetLagMeasuredDoesNotCreateCounters() {
        EdgeStatsCounterService counterService = new EdgeStatsCounterService();

        counterService.resetLagMeasured(edgeId);

        Assertions.assertFalse(counterService.getMsgCountersByEdge().containsKey(edgeId));
    }

    private void setupCounters() {
        setupCounters(true);
    }

    private void setupCounters(boolean lagMeasured) {
        MsgCounters counters = new MsgCounters(tenantId);
        counters.getMsgsAdded().set(EXPECTED_MSGS_ADDED);
        counters.getMsgsPushed().set(EXPECTED_MSGS_PUSHED);
        counters.getMsgsPermanentlyFailed().set(EXPECTED_MSGS_PERMANENTLY_FAILED);
        counters.getMsgsTmpFailed().set(EXPECTED_MSGS_TMP_FAILED);
        if (lagMeasured) {
            counters.getMsgsLag().set(EXPECTED_MSGS_LAG);
            counters.getMsgsLagMeasured().set(true);
        }

        ConcurrentHashMap<EdgeId, MsgCounters> countersByEdge = new ConcurrentHashMap<>();
        countersByEdge.put(edgeId, counters);

        when(statsCounterService.getMsgCountersByEdge()).thenReturn(countersByEdge);

        when(tsService.save(eq(tenantId), eq(edgeId), captor.capture(), anyLong()))
                .thenReturn(Futures.immediateFuture(mock(TimeseriesSaveResult.class)));
    }

    private Map<String, Long> verifyCounters() {
        verify(tsService, times(1)).save(eq(tenantId), eq(edgeId), anyList(), anyLong());
        verify(statsCounterService, times(1)).clear(edgeId);

        List<TsKvEntry> entries = captor.getValue();
        Assertions.assertEquals(5, entries.size());

        Map<String, Long> valuesByKey = entries.stream()
                .collect(Collectors.toMap(TsKvEntry::getKey, e -> e.getLongValue().orElse(-1L)));

        Assertions.assertEquals(EXPECTED_MSGS_ADDED, valuesByKey.get(DOWNLINK_MSGS_ADDED.getKey()).longValue());
        Assertions.assertEquals(EXPECTED_MSGS_PUSHED, valuesByKey.get(DOWNLINK_MSGS_PUSHED.getKey()).longValue());
        Assertions.assertEquals(EXPECTED_MSGS_PERMANENTLY_FAILED, valuesByKey.get(DOWNLINK_MSGS_PERMANENTLY_FAILED.getKey()).longValue());
        Assertions.assertEquals(EXPECTED_MSGS_TMP_FAILED, valuesByKey.get(DOWNLINK_MSGS_TMP_FAILED.getKey()).longValue());
        return valuesByKey;
    }

    private void setupKafkaLag() {
        String topic = "edge-topic";
        TopicPartitionInfo partitionInfo = new TopicPartitionInfo(topic, tenantId, 0, false);
        when(topicService.buildEdgeEventNotificationsTopicPartitionInfo(tenantId, edgeId)).thenReturn(partitionInfo);

        KafkaAdmin kafkaAdmin = mock(KafkaAdmin.class);
        when(kafkaAdmin.getTotalLagForGroupsBulk(Set.of(topic)))
                .thenReturn(Map.of(topic, EXPECTED_MSGS_KAFKA_LAG));

        edgeStatsService = createEdgeStatsService(Optional.of(kafkaAdmin));
    }

}
