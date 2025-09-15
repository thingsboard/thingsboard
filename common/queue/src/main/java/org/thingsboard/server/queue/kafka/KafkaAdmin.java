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
package org.thingsboard.server.queue.kafka;

import jakarta.annotation.PreDestroy;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.TopicExistsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.CachedValue;
import org.thingsboard.server.queue.util.TbKafkaComponent;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@TbKafkaComponent
@Component
@Slf4j
public class KafkaAdmin {

    /*
     * TODO: Get rid of per consumer/producer TbKafkaAdmin,
     *  use single KafkaAdmin instance that accepts topicConfigs.
     * */

    private final TbKafkaSettings settings;

    @Value("${queue.kafka.request.timeout.ms:30000}")
    private int requestTimeoutMs;
    @Value("${queue.kafka.topics_cache_ttl_ms:300000}") // 5 minutes by default
    private int topicsCacheTtlMs;

    private final LazyInitializer<AdminClient> adminClient;
    private final CachedValue<Set<String>> topics;

    public KafkaAdmin(@Lazy TbKafkaSettings settings) {
        this.settings = settings;
        this.adminClient = LazyInitializer.<AdminClient>builder()
                .setInitializer(() -> AdminClient.create(settings.toAdminProps()))
                .get();
        this.topics = new CachedValue<>(() -> {
            Set<String> topics = ConcurrentHashMap.newKeySet();
            topics.addAll(listTopics());
            return topics;
        }, topicsCacheTtlMs);
    }

    public void createTopicIfNotExists(String topic, Map<String, String> properties, boolean force) {
        Set<String> topics = getTopics();
        if (!force && topics.contains(topic)) {
            log.trace("Topic {} already present in cache", topic);
            return;
        }

        log.debug("Creating topic {} with properties {}", topic, properties);
        String numPartitionsStr = properties.remove(TbKafkaTopicConfigs.NUM_PARTITIONS_SETTING);
        int partitions = numPartitionsStr != null ? Integer.parseInt(numPartitionsStr) : 1;
        NewTopic newTopic = new NewTopic(topic, partitions, settings.getReplicationFactor()).configs(properties);

        try {
            getClient().createTopics(List.of(newTopic)).all().get(requestTimeoutMs, TimeUnit.MILLISECONDS);
            topics.add(topic);
        } catch (ExecutionException ee) {
            log.trace("Failed to create topic {} with properties {}", topic, properties, ee);
            if (ee.getCause() instanceof TopicExistsException) {
                //do nothing
            } else {
                log.warn("[{}] Failed to create topic", topic, ee);
                throw new RuntimeException(ee);
            }
        } catch (Exception e) {
            log.warn("[{}] Failed to create topic", topic, e);
            throw new RuntimeException(e);
        }
    }

    public void deleteTopic(String topic) {
        log.debug("Deleting topic {}", topic);
        try {
            getClient().deleteTopics(List.of(topic)).all().get(requestTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("Failed to delete kafka topic [{}].", topic, e);
        }
    }

    private Set<String> getTopics() {
        return topics.get();
    }

    public Set<String> listTopics() {
        try {
            Set<String> topics = getClient().listTopics().names().get(requestTimeoutMs, TimeUnit.MILLISECONDS);
            log.trace("Listed topics: {}", topics);
            return topics;
        } catch (Exception e) {
            log.error("Failed to get all topics.", e);
            return Collections.emptySet();
        }
    }

    public Map<String, Long> getTotalLagForGroupsBulk(Set<String> groupIds) {
        Map<String, Long> result = new HashMap<>();
        for (String groupId : groupIds) {
            result.put(groupId, getTotalConsumerGroupLag(groupId));
        }
        return result;
    }

    public long getTotalConsumerGroupLag(String groupId) {
        try {
            Map<TopicPartition, OffsetAndMetadata> committedOffsets = getConsumerGroupOffsets(groupId);
            if (committedOffsets.isEmpty()) {
                return 0L;
            }

            Map<TopicPartition, OffsetSpec> latestOffsetsSpec = committedOffsets.keySet().stream()
                    .collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.latest()));

            Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> endOffsets =
                    getClient().listOffsets(latestOffsetsSpec).all().get(requestTimeoutMs, TimeUnit.MILLISECONDS);

            return committedOffsets.entrySet().stream()
                    .mapToLong(entry -> {
                        TopicPartition tp = entry.getKey();
                        long committed = entry.getValue().offset();
                        long end = endOffsets.getOrDefault(tp,
                                new ListOffsetsResult.ListOffsetsResultInfo(0L, 0L, Optional.empty())).offset();
                        return end - committed;
                    }).sum();

        } catch (Exception e) {
            log.error("Failed to get total lag for consumer group: {}", groupId, e);
            return 0L;
        }
    }

    @SneakyThrows
    public Map<TopicPartition, OffsetAndMetadata> getConsumerGroupOffsets(String groupId) {
        return getClient().listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata().get(requestTimeoutMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Sync offsets from a fat group to a single-partition group
     * Migration back from single-partition consumer to a fat group is not supported
     * TODO: The best possible approach to synchronize the offsets is to do the synchronization as a part of the save Queue parameters with stop all consumers
     * */
    public void syncOffsets(String fatGroupId, String newGroupId, Integer partitionId) {
        try {
            log.info("syncOffsets [{}][{}][{}]", fatGroupId, newGroupId, partitionId);
            if (partitionId == null) {
                return;
            }
            syncOffsetsUnsafe(fatGroupId, newGroupId, "." + partitionId);
        } catch (Exception e) {
            log.warn("Failed to syncOffsets from {} to {} partitionId {}", fatGroupId, newGroupId, partitionId, e);
        }
    }

    public void syncOffsetsUnsafe(String fatGroupId, String newGroupId, String topicSuffix) throws ExecutionException, InterruptedException, TimeoutException {
        Map<TopicPartition, OffsetAndMetadata> oldOffsets = getConsumerGroupOffsets(fatGroupId);
        if (oldOffsets.isEmpty()) {
            return;
        }

        for (var consumerOffset : oldOffsets.entrySet()) {
            var tp = consumerOffset.getKey();
            if (!tp.topic().endsWith(topicSuffix)) {
                continue;
            }
            var om = consumerOffset.getValue();
            Map<TopicPartition, OffsetAndMetadata> newOffsets = getConsumerGroupOffsets(newGroupId);

            var existingOffset = newOffsets.get(tp);
            if (existingOffset == null) {
                log.info("[{}] topic offset does not exists in the new node group {}, all found offsets {}", tp, newGroupId, newOffsets);
            } else if (existingOffset.offset() >= om.offset()) {
                log.info("[{}] topic offset {} >= than old node group offset {}", tp, existingOffset.offset(), om.offset());
                break;
            } else {
                log.info("[{}] SHOULD alter topic offset [{}] less than old node group offset [{}]", tp, existingOffset.offset(), om.offset());
            }
            getClient().alterConsumerGroupOffsets(newGroupId, Map.of(tp, om)).all().get(requestTimeoutMs, TimeUnit.MILLISECONDS);
            log.info("[{}] altered new consumer groupId {}", tp, newGroupId);
            break;
        }
    }

    public boolean isTopicEmpty(String topic) {
        return areAllTopicsEmpty(Set.of(topic));
    }

    public boolean areAllTopicsEmpty(Set<String> topics) {
        try {
            List<String> existingTopics = getTopics().stream().filter(topics::contains).toList();
            if (existingTopics.isEmpty()) {
                return true;
            }

            List<TopicPartition> allPartitions = getClient().describeTopics(existingTopics).allTopicNames().get(requestTimeoutMs, TimeUnit.MILLISECONDS)
                    .entrySet().stream()
                    .flatMap(entry -> {
                        String topic = entry.getKey();
                        TopicDescription topicDescription = entry.getValue();
                        return topicDescription.partitions().stream().map(partitionInfo -> new TopicPartition(topic, partitionInfo.partition()));
                    })
                    .toList();

            Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> beginningOffsets = getClient().listOffsets(allPartitions.stream()
                    .collect(Collectors.toMap(partition -> partition, partition -> OffsetSpec.earliest()))).all().get(requestTimeoutMs, TimeUnit.MILLISECONDS);
            Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> endOffsets = getClient().listOffsets(allPartitions.stream()
                    .collect(Collectors.toMap(partition -> partition, partition -> OffsetSpec.latest()))).all().get(requestTimeoutMs, TimeUnit.MILLISECONDS);

            for (TopicPartition partition : allPartitions) {
                long beginningOffset = beginningOffsets.get(partition).offset();
                long endOffset = endOffsets.get(partition).offset();

                if (beginningOffset != endOffset) {
                    log.debug("Partition [{}] of topic [{}] is not empty. Returning false.", partition.partition(), partition.topic());
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.error("Failed to check if topics [{}] empty.", topics, e);
            return false;
        }
    }

    public void deleteConsumerGroup(String consumerGroupId) {
        try {
            getClient().deleteConsumerGroups(List.of(consumerGroupId)).all().get(requestTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.warn("Failed to delete consumer group {}", consumerGroupId, e);
        }
    }

    public AdminClient getClient() {
        try {
            return adminClient.get();
        } catch (ConcurrentException e) {
            throw new RuntimeException("Failed to initialize Kafka admin client", e);
        }
    }

    @PreDestroy
    private void destroy() throws Exception {
        if (adminClient.isInitialized()) {
            adminClient.get().close();
        }
    }

}
