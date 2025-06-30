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
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TimeseriesSaveResult;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.kafka.TbKafkaSettings;
import org.thingsboard.server.queue.util.AfterStartUp;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "queue", value = "type", havingValue = "kafka")
public class EdgeKafkaLagReporter {
    private static final String LAG_KEY = "downlinkLag";

    private final TbKafkaSettings kafkaSettings;
    private final TimeseriesService tsService;

    @Value("${queue.kafka.lag-report.send-ts-interval-ms:60000}")
    private long reportIntervalMs;

    @Value("${queue.prefix:}")
    private String tbQueuePrefix;

    @Value("${queue.edge.event_notifications_topic:tb_edge_event.notifications}")
    private String tbQueueEdgeEventNotificationsTopic;

    private KafkaConsumer<String, byte[]> consumer;
    private ScheduledExecutorService scheduler;

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void init() {
        log.info("Initializing EdgeKafkaLagReporter with interval {}ms...", reportIntervalMs);
        this.consumer = new KafkaConsumer<>(kafkaSettings.toConsumerProps(null));
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.scheduler.scheduleAtFixedRate(this::reportLag, 0, reportIntervalMs, TimeUnit.MILLISECONDS);
    }

    private void reportLag() {
        try {
            String fullEdgeNotificationTopicName = tbQueuePrefix + tbQueueEdgeEventNotificationsTopic;
            AdminClient adminClient = kafkaSettings.getAdminClient();
            Set<String> groupIds = adminClient.listConsumerGroups()
                    .all()
                    .get(10, TimeUnit.SECONDS)
                    .stream()
                    .map(ConsumerGroupListing::groupId)
                    .filter(id -> id.startsWith(fullEdgeNotificationTopicName))
                    .collect(Collectors.toSet());

            log.debug("Found {} consumer groups with prefix '{}'", groupIds.size(), fullEdgeNotificationTopicName);

            for (String groupId : groupIds) {
                log.debug("Processing consumer group: {}", groupId);

                ListConsumerGroupOffsetsResult offsetsResult = adminClient.listConsumerGroupOffsets(groupId);
                Map<TopicPartition, OffsetAndMetadata> groupOffsets = offsetsResult.partitionsToOffsetAndMetadata().get(5, TimeUnit.SECONDS);
                Map<TopicPartition, Long> endOffsets = consumer.endOffsets(groupOffsets.keySet());

                for (Map.Entry<TopicPartition, OffsetAndMetadata> entry : groupOffsets.entrySet()) {
                    TopicPartition tp = entry.getKey();
                    String topic = tp.topic();
                    long committed = entry.getValue().offset();
                    Long endOffset = endOffsets.get(tp);
                    if (endOffset == null) {
                        log.warn("No end offset for topic-partition {}. Skipping.", tp);
                        continue;
                    }

                    long lag = endOffset - committed;

                    extractTenantEdge(topic).ifPresent(tenantIdEdgeId -> {
                        long now = System.currentTimeMillis();
                        long roundedTs = now - (now % reportIntervalMs);
                        TsKvEntry kvEntry = new BasicTsKvEntry(roundedTs, new LongDataEntry(LAG_KEY, lag));

                        ListenableFuture<TimeseriesSaveResult> future = tsService.save(
                                TenantId.fromUUID(tenantIdEdgeId.getFirst()),
                                EdgeId.fromUUID(tenantIdEdgeId.getSecond()),
                                kvEntry
                        );

                        Futures.addCallback(future, new com.google.common.util.concurrent.FutureCallback<>() {
                            @Override
                            public void onSuccess(TimeseriesSaveResult result) {
                                log.debug("Successfully saved downlinkLag [{}] for edge [{}]", lag, tenantIdEdgeId.getSecond());
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                log.warn("Failed to save downlinkLag for edge [{}]: {}", tenantIdEdgeId.getSecond(), t.getMessage(), t);
                            }
                        }, MoreExecutors.directExecutor());
                    });
                }
            }
        } catch (Exception e) {
            log.warn("Error reporting Edge Kafka lag: ", e);
        }
    }

    /**
     * Parse tenantId and edgeId from topic name.
     * Expected topic should always end with .<tenantId>.<edgeId>
     */
    private Optional<Pair<UUID, UUID>> extractTenantEdge(String topic) {
        String[] parts = topic.split("\\.");
        if (parts.length >= 2) {
            try {
                UUID tenantId = UUID.fromString(parts[parts.length - 2]);
                UUID edgeId = UUID.fromString(parts[parts.length - 1]);
                return Optional.of(Pair.of(tenantId, edgeId));
            } catch (IllegalArgumentException e) {
                log.debug("Failed to parse tenantId or edgeId from topic: {}", topic, e);
            }
        }
        return Optional.empty();
    }

    @PreDestroy
    public void shutdown() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        if (consumer != null) {
            consumer.close();
        }
    }

}
