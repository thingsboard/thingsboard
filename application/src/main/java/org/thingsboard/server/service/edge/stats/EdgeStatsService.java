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
package org.thingsboard.server.service.edge.stats;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TimeseriesSaveResult;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.edge.stats.EdgeStatsCounterService;
import org.thingsboard.server.dao.edge.stats.MsgCounters;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.kafka.KafkaAdmin;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.edge.stats.EdgeStatsKey.DOWNLINK_MSGS_ADDED;
import static org.thingsboard.server.dao.edge.stats.EdgeStatsKey.DOWNLINK_MSGS_LAG;
import static org.thingsboard.server.dao.edge.stats.EdgeStatsKey.DOWNLINK_MSGS_PERMANENTLY_FAILED;
import static org.thingsboard.server.dao.edge.stats.EdgeStatsKey.DOWNLINK_MSGS_PUSHED;
import static org.thingsboard.server.dao.edge.stats.EdgeStatsKey.DOWNLINK_MSGS_TMP_FAILED;

@TbCoreComponent
@ConditionalOnProperty(prefix = "edges.stats", name = "enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
@Service
@Slf4j
public class EdgeStatsService {

    private final TimeseriesService tsService;
    private final EdgeStatsCounterService statsCounterService;
    private final TopicService topicService;
    private final Optional<KafkaAdmin> kafkaAdmin;

    @Value("${edges.stats.ttl:30}")
    private int edgesStatsTtlDays;
    @Value("${edges.stats.report-interval-millis:600000}")
    private long reportIntervalMillis;


    @Scheduled(
            fixedDelayString = "${edges.stats.report-interval-millis:600000}",
            initialDelayString = "${edges.stats.report-interval-millis:600000}"
    )
    public void reportStats() {
        log.debug("Reporting Edge communication stats...");
        long now = System.currentTimeMillis();
        long ts = now - (now % reportIntervalMillis);

        Map<EdgeId, MsgCounters> countersByEdge = statsCounterService.getCounterByEdge();
        Map<EdgeId, Long> lagByEdgeId = kafkaAdmin.isPresent() ? getEdgeLagByEdgeId(countersByEdge) : Collections.emptyMap();
        Map<EdgeId, MsgCounters> countersByEdgeSnapshot = new HashMap<>(statsCounterService.getCounterByEdge());
        countersByEdgeSnapshot.forEach((edgeId, counters) -> {
            TenantId tenantId = counters.getTenantId();

            if (kafkaAdmin.isPresent()) {
                counters.getMsgsLag().set(lagByEdgeId.getOrDefault(edgeId, 0L));
            }
            List<TsKvEntry> statsEntries = List.of(
                    entry(ts, DOWNLINK_MSGS_ADDED.getKey(), counters.getMsgsAdded().get()),
                    entry(ts, DOWNLINK_MSGS_PUSHED.getKey(), counters.getMsgsPushed().get()),
                    entry(ts, DOWNLINK_MSGS_PERMANENTLY_FAILED.getKey(), counters.getMsgsPermanentlyFailed().get()),
                    entry(ts, DOWNLINK_MSGS_TMP_FAILED.getKey(), counters.getMsgsTmpFailed().get()),
                    entry(ts, DOWNLINK_MSGS_LAG.getKey(), counters.getMsgsLag().get())
            );

            log.trace("Reported Edge communication stats: {} tenantId - {}, edgeId - {}", statsEntries, tenantId, edgeId);
            saveTs(tenantId, edgeId, statsEntries);
        });
    }

    private Map<EdgeId, Long> getEdgeLagByEdgeId(Map<EdgeId, MsgCounters> countersByEdge) {
        Map<EdgeId, String> edgeToTopicMap = countersByEdge.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> topicService.buildEdgeEventNotificationsTopicPartitionInfo(e.getValue().getTenantId(), e.getKey()).getTopic()
                ));

        Map<String, Long> lagByTopic = kafkaAdmin.get().getTotalLagForGroupsBulk(new HashSet<>(edgeToTopicMap.values()));

        return edgeToTopicMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> lagByTopic.getOrDefault(e.getValue(), 0L)
                ));
    }

    private void saveTs(TenantId tenantId, EdgeId edgeId, List<TsKvEntry> statsEntries) {
        try {
            ListenableFuture<TimeseriesSaveResult> future = tsService.save(
                    tenantId,
                    edgeId,
                    statsEntries,
                    TimeUnit.DAYS.toSeconds(edgesStatsTtlDays)
            );

            Futures.addCallback(future, new FutureCallback<>() {
                @Override
                public void onSuccess(TimeseriesSaveResult result) {
                    log.debug("Successfully saved edge time-series stats: {} for edge: {}", statsEntries, edgeId);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.warn("Failed to save edge time-series stats for edge: {}", edgeId, t);
                }
            }, MoreExecutors.directExecutor());
        } finally {
            statsCounterService.clear(edgeId);
        }
    }

    private BasicTsKvEntry entry(long ts, String key, long value) {
        return new BasicTsKvEntry(ts, new LongDataEntry(key, value));
    }

}
