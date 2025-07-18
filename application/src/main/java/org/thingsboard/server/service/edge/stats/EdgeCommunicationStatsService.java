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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.kafka.TbKafkaAdmin;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EdgeCommunicationStatsService {

    private static final String DOWNLINK_MSGS_ADDED = "downlinkMsgsAdded";
    private static final String DOWNLINK_MSGS_PUSHED = "downlinkMsgsPushed";
    private static final String DOWNLINK_MSGS_PERMANENTLY_FAILED = "downlinkMsgsPermanentlyFailed";
    private static final String DOWNLINK_MSGS_TMP_FAILED = "downlinkMsgsTmpFailed";
    private static final String DOWNLINK_MSGS_LAG = "downlinkMsgsLag";

    @Autowired
    private TimeseriesService tsService;
    @Autowired
    private EdgeStatsCounterService statsCounterService;
    @Autowired
    private TopicService topicService;
    @Autowired(required = false)
    private TbKafkaAdmin tbKafkaAdmin;

    @Value("${edges.stats.enabled:true}")
    private boolean edgesStatsEnabled;
    @Value("${edges.stats.ttl:30}")
    private int edgesStatsTtlDays;
    @Value("${edges.stats.report-interval-millis:20000}")
    private long reportIntervalMillis;

    @Scheduled(fixedDelayString = "${edges.stats.report-interval-millis:20000}")
    public void reportStats() {
        if (!edgesStatsEnabled) {
            log.debug("Edge stats reporting is disabled by configuration.");
            return;
        }
        log.debug("Reporting Edge communication stats...");

        long ts = (System.currentTimeMillis() / reportIntervalMillis) * reportIntervalMillis;

        Map<EdgeId, MsgCounters> countersByEdge = statsCounterService.getCounterByEdge();
        Map<EdgeId, Long> lagByEdgeId = tbKafkaAdmin != null ? getEdgeLagByEdgeId(countersByEdge) : Collections.emptyMap();
        for (Map.Entry<EdgeId, MsgCounters> counterByEdge : countersByEdge.entrySet()) {
            EdgeId edgeId = counterByEdge.getKey();
            MsgCounters counters = counterByEdge.getValue();
            TenantId tenantId = counters.getTenantId();

            if (tbKafkaAdmin != null) {
                counters.getMsgsLag().set(lagByEdgeId.getOrDefault(edgeId, 0L));
            }
            List<TsKvEntry> statsEntries = List.of(
                    entry(ts, DOWNLINK_MSGS_ADDED, counters.getMsgsAdded().get()),
                    entry(ts, DOWNLINK_MSGS_PUSHED, counters.getMsgsPushed().get()),
                    entry(ts, DOWNLINK_MSGS_PERMANENTLY_FAILED, counters.getMsgsPermanentlyFailed().get()),
                    entry(ts, DOWNLINK_MSGS_TMP_FAILED, counters.getMsgsTmpFailed().get()),
                    entry(ts, DOWNLINK_MSGS_LAG, counters.getMsgsLag().get())
            );

            log.trace("Reported Edge communication stats: {}", statsEntries);
            saveTs(tenantId, edgeId, statsEntries);
        }
    }

    private Map<EdgeId, Long> getEdgeLagByEdgeId(Map<EdgeId, MsgCounters> countersByEdge) {
        Map<EdgeId, String> edgeToTopicMap = countersByEdge.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> topicService.buildEdgeEventNotificationsTopicPartitionInfo(e.getValue().getTenantId(), e.getKey()).getTopic()
                ));

        Map<String, Long> lagByTopic = tbKafkaAdmin.getTotalLagForGroupsBulk(new HashSet<>(edgeToTopicMap.values()));

        return edgeToTopicMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> lagByTopic.getOrDefault(e.getValue(), 0L)
                ));
    }

    private void saveTs(TenantId tenantId, EdgeId edgeId, List<TsKvEntry> statsEntries) {
        try {
            tsService.save(tenantId, edgeId, statsEntries, TimeUnit.DAYS.toSeconds(edgesStatsTtlDays));
            log.debug("Successfully saved edge time-series stats: {} for edge: {}", statsEntries, edgeId);
        } finally {
            statsCounterService.clear(edgeId);
        }
    }

    private BasicTsKvEntry entry(long ts, String key, long value) {
        return new BasicTsKvEntry(ts, new LongDataEntry(key, value));
    }

}
