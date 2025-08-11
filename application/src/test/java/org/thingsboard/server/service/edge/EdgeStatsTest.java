/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.edge;

import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.TimeseriesSaveResult;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
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

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.dao.edge.stats.EdgeStatsKey.DOWNLINK_MSGS_ADDED;
import static org.thingsboard.server.dao.edge.stats.EdgeStatsKey.DOWNLINK_MSGS_LAG;
import static org.thingsboard.server.dao.edge.stats.EdgeStatsKey.DOWNLINK_MSGS_PERMANENTLY_FAILED;
import static org.thingsboard.server.dao.edge.stats.EdgeStatsKey.DOWNLINK_MSGS_PUSHED;
import static org.thingsboard.server.dao.edge.stats.EdgeStatsKey.DOWNLINK_MSGS_TMP_FAILED;

@ExtendWith(MockitoExtension.class)
public class EdgeStatsTest {

    @Mock
    private TimeseriesService tsService;
    @Mock
    private TopicService topicService;
    @Mock
    private EdgeStatsCounterService statsCounterService;
    private EdgeStatsService edgeStatsService;

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
    private final EdgeId edgeId = new EdgeId(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        edgeStatsService = new EdgeStatsService(
                tsService,
                statsCounterService,
                topicService,
                Optional.empty()
        );

        ReflectionTestUtils.setField(edgeStatsService, "edgesStatsTtlDays", 30);
        ReflectionTestUtils.setField(edgeStatsService, "reportIntervalMillis", 600_000L);
    }

    @Test
    public void testReportStatsSavesTelemetry() {
        // given
        MsgCounters counters = new MsgCounters(tenantId);
        counters.getMsgsAdded().set(5);
        counters.getMsgsPushed().set(3);
        counters.getMsgsPermanentlyFailed().set(1);
        counters.getMsgsTmpFailed().set(0);
        counters.getMsgsLag().set(10);

        ConcurrentHashMap<EdgeId, MsgCounters> countersByEdge = new ConcurrentHashMap<>();
        countersByEdge.put(edgeId, counters);

        when(statsCounterService.getCounterByEdge()).thenReturn(countersByEdge);

        ArgumentCaptor<List<TsKvEntry>> captor = ArgumentCaptor.forClass(List.class);
        when(tsService.save(eq(tenantId), eq(edgeId), captor.capture(), anyLong()))
                .thenReturn(Futures.immediateFuture(mock(TimeseriesSaveResult.class)));

        // when
        edgeStatsService.reportStats();

        // then
        List<TsKvEntry> entries = captor.getValue();
        Assertions.assertEquals(5, entries.size());

        Map<String, Long> valuesByKey = entries.stream()
                .collect(Collectors.toMap(TsKvEntry::getKey, e -> e.getLongValue().orElse(-1L)));

        Assertions.assertEquals(5L, valuesByKey.get(DOWNLINK_MSGS_ADDED.getKey()).longValue());
        Assertions.assertEquals(3L, valuesByKey.get(DOWNLINK_MSGS_PUSHED.getKey()).longValue());
        Assertions.assertEquals(1L, valuesByKey.get(DOWNLINK_MSGS_PERMANENTLY_FAILED.getKey()).longValue());
        Assertions.assertEquals(0L, valuesByKey.get(DOWNLINK_MSGS_TMP_FAILED.getKey()).longValue());
        Assertions.assertEquals(10L, valuesByKey.get(DOWNLINK_MSGS_LAG.getKey()).longValue());


        verify(statsCounterService).clear(edgeId);
    }

    @Test
    public void testReportStatsWithKafkaLag() {
        // given
        MsgCounters counters = new MsgCounters(tenantId);
        counters.getMsgsAdded().set(2);
        counters.getMsgsPushed().set(2);
        counters.getMsgsPermanentlyFailed().set(0);
        counters.getMsgsTmpFailed().set(1);
        counters.getMsgsLag().set(0);

        ConcurrentHashMap<EdgeId, MsgCounters> countersByEdge = new ConcurrentHashMap<>();
        countersByEdge.put(edgeId, counters);

        // mocks
        when(statsCounterService.getCounterByEdge()).thenReturn(countersByEdge);

        String topic = "edge-topic";
        TopicPartitionInfo partitionInfo = new TopicPartitionInfo(topic, tenantId, 0, false);
        when(topicService.buildEdgeEventNotificationsTopicPartitionInfo(tenantId, edgeId)).thenReturn(partitionInfo);

        KafkaAdmin kafkaAdmin = mock(KafkaAdmin.class);
        when(kafkaAdmin.getTotalLagForGroupsBulk(Set.of(topic)))
                .thenReturn(Map.of(topic, 15L));

        ArgumentCaptor<List<TsKvEntry>> captor = ArgumentCaptor.forClass(List.class);
        when(tsService.save(eq(tenantId), eq(edgeId), captor.capture(), anyLong()))
                .thenReturn(Futures.immediateFuture(mock(TimeseriesSaveResult.class)));

        edgeStatsService = new EdgeStatsService(
                tsService,
                statsCounterService,
                topicService,
                Optional.of(kafkaAdmin)
        );
        ReflectionTestUtils.setField(edgeStatsService, "edgesStatsTtlDays", 30);
        ReflectionTestUtils.setField(edgeStatsService, "reportIntervalMillis", 600_000L);

        // when
        edgeStatsService.reportStats();

        // then
        List<TsKvEntry> entries = captor.getValue();
        Map<String, Long> valuesByKey = entries.stream()
                .collect(Collectors.toMap(TsKvEntry::getKey, e -> e.getLongValue().orElse(-1L)));

        Assertions.assertEquals(15L, valuesByKey.get(DOWNLINK_MSGS_LAG.getKey()));
        verify(statsCounterService).clear(edgeId);
    }

}
