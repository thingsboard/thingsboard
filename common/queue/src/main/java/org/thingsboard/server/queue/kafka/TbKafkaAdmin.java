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

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.TopicExistsException;
import org.thingsboard.server.queue.TbEdgeQueueAdmin;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.util.PropertyUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Created by ashvayka on 24.09.18.
 */
@Slf4j
public class TbKafkaAdmin implements TbQueueAdmin, TbEdgeQueueAdmin {

    private final TbKafkaSettings settings;
    private final Map<String, String> topicConfigs;
    @Getter
    private final int numPartitions;
    private volatile Set<String> topics;

    private final short replicationFactor;

    public TbKafkaAdmin(TbKafkaSettings settings, Map<String, String> topicConfigs) {
        this.settings = settings;
        this.topicConfigs = topicConfigs;

        String numPartitionsStr = topicConfigs.get(TbKafkaTopicConfigs.NUM_PARTITIONS_SETTING);
        if (numPartitionsStr != null) {
            numPartitions = Integer.parseInt(numPartitionsStr);
        } else {
            numPartitions = 1;
        }
        replicationFactor = settings.getReplicationFactor();
    }

    @Override
    public void createTopicIfNotExists(String topic, String properties) {
        Set<String> topics = getTopics();
        if (topics.contains(topic)) {
            return;
        }
        try {
            Map<String, String> configs = PropertyUtils.getProps(topicConfigs, properties);
            configs.remove(TbKafkaTopicConfigs.NUM_PARTITIONS_SETTING);
            NewTopic newTopic = new NewTopic(topic, numPartitions, replicationFactor).configs(configs);
            createTopic(newTopic).values().get(topic).get();
            topics.add(topic);
        } catch (ExecutionException ee) {
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

    @Override
    public void deleteTopic(String topic) {
        Set<String> topics = getTopics();
        if (topics.remove(topic)) {
            settings.getAdminClient().deleteTopics(Collections.singletonList(topic));
        } else {
            try {
                if (settings.getAdminClient().listTopics().names().get().contains(topic)) {
                    settings.getAdminClient().deleteTopics(Collections.singletonList(topic));
                } else {
                    log.warn("Kafka topic [{}] does not exist.", topic);
                }
            } catch (InterruptedException | ExecutionException e) {
                log.error("Failed to delete kafka topic [{}].", topic, e);
            }
        }
    }

    private Set<String> getTopics() {
        if (topics == null) {
            synchronized (this) {
                if (topics == null) {
                    topics = ConcurrentHashMap.newKeySet();
                    try {
                        topics.addAll(settings.getAdminClient().listTopics().names().get());
                    } catch (InterruptedException | ExecutionException e) {
                        log.error("Failed to get all topics.", e);
                    }
                }
            }
        }
        return topics;
    }

    public Set<String> getAllTopics() {
        try {
            return settings.getAdminClient().listTopics().names().get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to get all topics.", e);
        }
        return null;
    }

    public CreateTopicsResult createTopic(NewTopic topic) {
        return settings.getAdminClient().createTopics(Collections.singletonList(topic));
    }

    @Override
    public void destroy() {
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

    /**
     * Sync edge notifications offsets from a fat group to a single group per edge
     * */
    public void syncEdgeNotificationsOffsets(String fatGroupId, String newGroupId) {
        try {
            log.info("syncEdgeNotificationsOffsets [{}][{}]", fatGroupId, newGroupId);
            syncOffsetsUnsafe(fatGroupId, newGroupId, newGroupId);
        } catch (Exception e) {
            log.warn("Failed to syncEdgeNotificationsOffsets from {} to {}", fatGroupId, newGroupId, e);
        }
    }

    @Override
    public void deleteConsumerGroup(String consumerGroupId) {
        try {
            settings.getAdminClient().deleteConsumerGroups(Collections.singletonList(consumerGroupId));
        } catch (Exception e) {
            log.warn("Failed to delete consumer group {}", consumerGroupId, e);
        }
    }

    void syncOffsetsUnsafe(String fatGroupId, String newGroupId, String topicSuffix) throws ExecutionException, InterruptedException, TimeoutException {
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
            settings.getAdminClient().alterConsumerGroupOffsets(newGroupId, Map.of(tp, om)).all().get(10, TimeUnit.SECONDS);
            log.info("[{}] altered new consumer groupId {}", tp, newGroupId);
            break;
        }
    }

    @SneakyThrows
    public Map<TopicPartition, OffsetAndMetadata> getConsumerGroupOffsets(String groupId) {
        return settings.getAdminClient().listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata().get(10, TimeUnit.SECONDS);
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

            List<TopicPartition> allPartitions = settings.getAdminClient().describeTopics(existingTopics).topicNameValues().entrySet().stream()
                    .flatMap(entry -> {
                        String topic = entry.getKey();
                        TopicDescription topicDescription;
                        try {
                            topicDescription = entry.getValue().get();
                        } catch (InterruptedException | ExecutionException e) {
                            throw new RuntimeException(e);
                        }
                        return topicDescription.partitions().stream().map(partitionInfo -> new TopicPartition(topic, partitionInfo.partition()));
                    })
                    .toList();

            Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> beginningOffsets = settings.getAdminClient().listOffsets(allPartitions.stream()
                    .collect(Collectors.toMap(partition -> partition, partition -> OffsetSpec.earliest()))).all().get();
            Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> endOffsets = settings.getAdminClient().listOffsets(allPartitions.stream()
                    .collect(Collectors.toMap(partition -> partition, partition -> OffsetSpec.latest()))).all().get();

            for (TopicPartition partition : allPartitions) {
                long beginningOffset = beginningOffsets.get(partition).offset();
                long endOffset = endOffsets.get(partition).offset();

                if (beginningOffset != endOffset) {
                    log.debug("Partition [{}] of topic [{}] is not empty. Returning false.", partition.partition(), partition.topic());
                    return false;
                }
            }
            return true;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to check if topics [{}] empty.", topics, e);
            return false;
        }
    }

}
