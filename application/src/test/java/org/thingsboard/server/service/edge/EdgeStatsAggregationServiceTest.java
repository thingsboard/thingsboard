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
import org.thingsboard.server.cache.SimpleTbCacheValueWrapper;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TimeseriesSaveResult;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.gen.transport.TransportProtos.EdgeStatsMsgProto;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.kafka.KafkaAdmin;
import org.thingsboard.server.service.edge.stats.EdgeStatsAggregationService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.anyCollection;
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
public class EdgeStatsAggregationServiceTest {

    private static final long REPORT_INTERVAL_MILLIS = 600_000L;
    private static final int MAX_LAG_COUNT = 100_000;

    @Mock
    private TimeseriesService tsService;
    @Mock
    private TopicService topicService;
    @Mock
    private AttributesService attributesService;
    @Mock
    private EdgeEventService edgeEventService;
    @Mock
    private TbTransactionalCache<EdgeId, String> edgeIdServiceIdCache;
    @Captor
    private ArgumentCaptor<List<TsKvEntry>> captor;

    private EdgeStatsAggregationService aggregationService;

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
    private final EdgeId edgeId = new EdgeId(UUID.randomUUID());
    private long ts;

    @BeforeEach
    void setUp() {
        aggregationService = createService(Optional.empty());
        long now = System.currentTimeMillis();
        ts = now - now % REPORT_INTERVAL_MILLIS;
    }

    private EdgeStatsAggregationService createService(Optional<KafkaAdmin> kafkaAdmin) {
        EdgeStatsAggregationService service = new EdgeStatsAggregationService(tsService, topicService, kafkaAdmin, attributesService, edgeEventService, edgeIdServiceIdCache);
        ReflectionTestUtils.setField(service, "edgesStatsTtlDays", 30);
        ReflectionTestUtils.setField(service, "reportIntervalMillis", REPORT_INTERVAL_MILLIS);
        ReflectionTestUtils.setField(service, "maxLagCount", MAX_LAG_COUNT);
        ReflectionTestUtils.setField(service, "misorderingCompensationMillis", 60_000L);
        return service;
    }

    @Test
    public void testSumsPartsOfAllNodes() throws Exception {
        sessionConnected();
        // GIVEN: node A and rule engine node only added events, node B holds the session and measured the lag
        noSavedStats();
        mockSave();
        aggregationService.onEdgeStatsMsg(msg(ts, 100, 0, 0, false), TbCallback.EMPTY);
        aggregationService.onEdgeStatsMsg(msg(ts, 50, 180, 0, true), TbCallback.EMPTY);
        aggregationService.onEdgeStatsMsg(msg(ts, 30, 0, 0, false), TbCallback.EMPTY);

        // WHEN
        aggregationService.flush();

        // THEN
        Map<String, Long> saved = savedValues(1);
        Assertions.assertEquals(180, saved.get(DOWNLINK_MSGS_ADDED.getKey()));
        Assertions.assertEquals(180, saved.get(DOWNLINK_MSGS_PUSHED.getKey()));
        Assertions.assertEquals(0, saved.get(DOWNLINK_MSGS_LAG.getKey()));
        verifyNoInteractions(edgeEventService, attributesService);
    }

    @Test
    public void testCountsPendingEventsWhenLagNotMeasured() throws Exception {
        // GIVEN
        noSavedStats();
        mockSave();
        when(attributesService.find(eq(tenantId), eq(edgeId), eq(AttributeScope.SERVER_SCOPE), anyList()))
                .thenReturn(Futures.immediateFuture(List.of()));
        when(edgeEventService.countEdgeEvents(tenantId, edgeId, 0L, 0L, MAX_LAG_COUNT)).thenReturn(42L);
        aggregationService.onEdgeStatsMsg(msg(ts, 5, 0, 0, false), TbCallback.EMPTY);

        // WHEN
        aggregationService.flush();

        // THEN
        Map<String, Long> saved = savedValues(1);
        Assertions.assertEquals(5, saved.get(DOWNLINK_MSGS_ADDED.getKey()));
        Assertions.assertEquals(42, saved.get(DOWNLINK_MSGS_LAG.getKey()));
    }

    @Test
    public void testUsesKafkaLag() throws Exception {
        // GIVEN
        KafkaAdmin kafkaAdmin = mock(KafkaAdmin.class);
        aggregationService = createService(Optional.of(kafkaAdmin));
        noSavedStats();
        mockSave();
        when(topicService.buildEdgeEventNotificationsTopicPartitionInfo(tenantId, edgeId))
                .thenReturn(new TopicPartitionInfo("edge-topic", tenantId, 0, false));
        when(kafkaAdmin.getTotalLagForGroupsBulk(Set.of("edge-topic"))).thenReturn(Map.of("edge-topic", 15L));
        aggregationService.onEdgeStatsMsg(msg(ts, 5, 5, 0, false), TbCallback.EMPTY);

        // WHEN
        aggregationService.flush();

        // THEN
        Assertions.assertEquals(15, savedValues(1).get(DOWNLINK_MSGS_LAG.getKey()));
        verifyNoInteractions(edgeEventService);
    }

    @Test
    public void testAddsToValuesAlreadySavedForInterval() throws Exception {
        sessionConnected();
        // GIVEN: a previous owner of the edge partition already saved a part of this interval
        when(tsService.findLatest(eq(tenantId), eq(edgeId), anyCollection())).thenReturn(Futures.immediateFuture(List.of(
                new BasicTsKvEntry(ts, new LongDataEntry(DOWNLINK_MSGS_ADDED.getKey(), 7L)),
                new BasicTsKvEntry(ts, new LongDataEntry(DOWNLINK_MSGS_PUSHED.getKey(), 6L)),
                new BasicTsKvEntry(ts, new LongDataEntry(DOWNLINK_MSGS_PERMANENTLY_FAILED.getKey(), 0L)),
                new BasicTsKvEntry(ts, new LongDataEntry(DOWNLINK_MSGS_TMP_FAILED.getKey(), 1L)))));
        mockSave();
        aggregationService.onEdgeStatsMsg(msg(ts, 5, 5, 0, true), TbCallback.EMPTY);

        // WHEN
        aggregationService.flush();

        // THEN
        Map<String, Long> saved = savedValues(1);
        Assertions.assertEquals(12, saved.get(DOWNLINK_MSGS_ADDED.getKey()));
        Assertions.assertEquals(11, saved.get(DOWNLINK_MSGS_PUSHED.getKey()));
        Assertions.assertEquals(1, saved.get(DOWNLINK_MSGS_TMP_FAILED.getKey()));
    }

    @Test
    public void testIgnoresValuesSavedForPreviousInterval() throws Exception {
        sessionConnected();
        // GIVEN
        when(tsService.findLatest(eq(tenantId), eq(edgeId), anyCollection())).thenReturn(Futures.immediateFuture(List.of(
                new BasicTsKvEntry(ts - REPORT_INTERVAL_MILLIS, new LongDataEntry(DOWNLINK_MSGS_ADDED.getKey(), 7L)))));
        mockSave();
        aggregationService.onEdgeStatsMsg(msg(ts, 5, 5, 0, true), TbCallback.EMPTY);

        // WHEN
        aggregationService.flush();

        // THEN
        Assertions.assertEquals(5, savedValues(1).get(DOWNLINK_MSGS_ADDED.getKey()));
    }

    @Test
    public void testSavesOnlyChangedIntervals() throws Exception {
        sessionConnected();
        // GIVEN
        noSavedStats();
        mockSave();
        aggregationService.onEdgeStatsMsg(msg(ts, 5, 5, 0, true), TbCallback.EMPTY);

        // WHEN
        aggregationService.flush();
        aggregationService.flush();
        aggregationService.onEdgeStatsMsg(msg(ts, 2, 2, 0, true), TbCallback.EMPTY);
        aggregationService.flush();

        // THEN: the second flush has nothing to save, the third one saves the growing total of the same interval
        Assertions.assertEquals(7, savedValues(2).get(DOWNLINK_MSGS_ADDED.getKey()));
    }

    @Test
    public void testDropsPreviousIntervalAfterFlush() throws Exception {
        sessionConnected();
        // GIVEN
        noSavedStats();
        mockSave();
        long previousTs = ts - REPORT_INTERVAL_MILLIS;
        aggregationService.onEdgeStatsMsg(msg(previousTs, 5, 5, 0, true), TbCallback.EMPTY);
        aggregationService.onEdgeStatsMsg(msg(ts, 1, 1, 0, true), TbCallback.EMPTY);

        // WHEN
        aggregationService.flush();

        // THEN
        verify(tsService, times(2)).save(eq(tenantId), eq(edgeId), anyList(), anyLong());
        Map<?, ?> intervals = (Map<?, ?>) ReflectionTestUtils.getField(aggregationService, "intervals");
        Assertions.assertEquals(1, intervals.size());
    }

    @Test
    public void testCountsPendingEventsWhenSessionThatMeasuredLagIsGone() throws Exception {
        // GIVEN: the session node reported lag 0, then the edge went offline before the flush
        noSavedStats();
        mockSave();
        when(edgeIdServiceIdCache.get(edgeId)).thenReturn(null);
        when(attributesService.find(eq(tenantId), eq(edgeId), eq(AttributeScope.SERVER_SCOPE), anyList()))
                .thenReturn(Futures.immediateFuture(List.of()));
        when(edgeEventService.countEdgeEvents(tenantId, edgeId, 0L, 0L, MAX_LAG_COUNT)).thenReturn(300L);
        aggregationService.onEdgeStatsMsg(msg(ts, 0, 10, 0, true), TbCallback.EMPTY);
        aggregationService.onEdgeStatsMsg(msg(ts, 300, 0, 0, false), TbCallback.EMPTY);

        // WHEN
        aggregationService.flush();

        // THEN
        Assertions.assertEquals(300, savedValues(1).get(DOWNLINK_MSGS_LAG.getKey()));
    }

    @Test
    public void testKeepsNewestMeasuredLagWhateverArrivalOrder() throws Exception {
        // GIVEN: the edge reconnected to another node; the newer measurement arrives first
        sessionConnected();
        noSavedStats();
        mockSave();
        aggregationService.onEdgeStatsMsg(msg(ts, 0, 0, 5, true, 2_000L), TbCallback.EMPTY);
        aggregationService.onEdgeStatsMsg(msg(ts, 0, 0, 9, true, 1_000L), TbCallback.EMPTY);

        // WHEN
        aggregationService.flush();

        // THEN
        Assertions.assertEquals(5, savedValues(1).get(DOWNLINK_MSGS_LAG.getKey()));
    }

    @Test
    public void testSavesOnlyCountersForPreviousInterval() throws Exception {
        // GIVEN: a late part of the previous interval, its lag was saved while it was current
        noSavedStats();
        mockSave();
        long previousTs = ts - REPORT_INTERVAL_MILLIS;
        aggregationService.onEdgeStatsMsg(msg(previousTs, 5, 5, 0, false), TbCallback.EMPTY);

        // WHEN
        aggregationService.flush();

        // THEN
        verify(tsService, times(1)).save(eq(tenantId), eq(edgeId), anyList(), anyLong());
        List<TsKvEntry> entries = captor.getValue();
        Assertions.assertEquals(4, entries.size());
        Assertions.assertTrue(entries.stream().allMatch(e -> e.getTs() == previousTs));
        Assertions.assertTrue(entries.stream().noneMatch(e -> e.getKey().equals(DOWNLINK_MSGS_LAG.getKey())));
        verifyNoInteractions(edgeEventService, attributesService, edgeIdServiceIdCache);
    }

    @Test
    public void testCountsPendingEventsOnceForCurrentIntervalOnly() throws Exception {
        // GIVEN: in one flush the edge has a late part of the previous interval and a part of the current one
        noSavedStats();
        mockSave();
        when(attributesService.find(eq(tenantId), eq(edgeId), eq(AttributeScope.SERVER_SCOPE), anyList()))
                .thenReturn(Futures.immediateFuture(List.of()));
        when(edgeEventService.countEdgeEvents(tenantId, edgeId, 0L, 0L, MAX_LAG_COUNT)).thenReturn(42L);
        long previousTs = ts - REPORT_INTERVAL_MILLIS;
        aggregationService.onEdgeStatsMsg(msg(previousTs, 5, 5, 0, false), TbCallback.EMPTY);
        aggregationService.onEdgeStatsMsg(msg(ts, 3, 0, 0, false), TbCallback.EMPTY);

        // WHEN
        aggregationService.flush();

        // THEN
        verify(edgeEventService, times(1)).countEdgeEvents(tenantId, edgeId, 0L, 0L, MAX_LAG_COUNT);
        verify(tsService, times(2)).save(eq(tenantId), eq(edgeId), anyList(), anyLong());
        Map<Long, Map<String, Long>> savedByTs = captor.getAllValues().stream()
                .collect(Collectors.toMap(entries -> entries.get(0).getTs(),
                        entries -> entries.stream().collect(Collectors.toMap(TsKvEntry::getKey, e -> e.getLongValue().orElse(-1L)))));
        Assertions.assertFalse(savedByTs.get(previousTs).containsKey(DOWNLINK_MSGS_LAG.getKey()));
        Assertions.assertEquals(42, savedByTs.get(ts).get(DOWNLINK_MSGS_LAG.getKey()));
        Assertions.assertEquals(3, savedByTs.get(ts).get(DOWNLINK_MSGS_ADDED.getKey()));
    }

    private void sessionConnected() {
        when(edgeIdServiceIdCache.get(edgeId)).thenReturn(SimpleTbCacheValueWrapper.wrap("tb-core-1"));
    }

    private void noSavedStats() {
        when(tsService.findLatest(eq(tenantId), eq(edgeId), anyCollection())).thenReturn(Futures.immediateFuture(List.of()));
    }

    private void mockSave() {
        when(tsService.save(eq(tenantId), eq(edgeId), captor.capture(), anyLong()))
                .thenReturn(Futures.immediateFuture(mock(TimeseriesSaveResult.class)));
    }

    private Map<String, Long> savedValues(int expectedSaves) {
        verify(tsService, times(expectedSaves)).save(eq(tenantId), eq(edgeId), anyList(), anyLong());
        List<TsKvEntry> entries = captor.getValue();
        Assertions.assertEquals(5, entries.size());
        Assertions.assertTrue(entries.stream().allMatch(e -> e.getTs() == ts));
        return entries.stream().collect(Collectors.toMap(TsKvEntry::getKey, e -> e.getLongValue().orElse(-1L)));
    }

    private EdgeStatsMsgProto msg(long msgTs, long added, long pushed, long lag, boolean lagMeasured) {
        return msg(msgTs, added, pushed, lag, lagMeasured, lagMeasured ? System.currentTimeMillis() : 0L);
    }

    private EdgeStatsMsgProto msg(long msgTs, long added, long pushed, long lag, boolean lagMeasured, long lagTs) {
        return EdgeStatsMsgProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setEdgeIdMSB(edgeId.getId().getMostSignificantBits())
                .setEdgeIdLSB(edgeId.getId().getLeastSignificantBits())
                .setTs(msgTs)
                .setMsgsAdded(added)
                .setMsgsPushed(pushed)
                .setMsgsLag(lag)
                .setMsgsLagMeasured(lagMeasured)
                .setMsgsLagTs(lagTs)
                .build();
    }

}
