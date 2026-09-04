// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.kafka;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

@Slf4j
class TbKafkaAdminTest {

    Properties props;
    AdminClient admin;

    @BeforeEach
    void setUp() {
        props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        admin = AdminClient.create(props);
    }

    @AfterEach
    void tearDown() {
        admin.close();
    }

    @Disabled
    @Test
    void testListOffsets() throws ExecutionException, InterruptedException {
        log.info("Getting consumer groups list...");
        Collection<ConsumerGroupListing> consumerGroupListings = admin.listConsumerGroups().all().get();
        consumerGroupListings = consumerGroupListings.stream().sorted(Comparator.comparing(ConsumerGroupListing::groupId)).toList();
        for (ConsumerGroupListing consumerGroup : consumerGroupListings) {
            String groupId = consumerGroup.groupId();
            log.info("=== consumer group: {}", groupId);
            Map<TopicPartition, OffsetAndMetadata> consumerOffsets = admin.listConsumerGroupOffsets(groupId)
                    .partitionsToOffsetAndMetadata().get();

            // Printing the fetched offsets
            consumerOffsets.forEach((tp, om) -> log.info(tp.topic() + " partition " + tp.partition() + " offset " + om.offset()));
            if (groupId.startsWith("re-") && groupId.endsWith("consumer")) {
                log.info("****** Migrating groupId [{}] ...", groupId);

                for (var consumerOffset : consumerOffsets.entrySet()) {
                    var tp = consumerOffset.getKey();
                    var om = consumerOffset.getValue();
                    final Integer tbPartitionId = parsePartitionIdFromTopicName(tp.topic());
                    if (tbPartitionId == null || tbPartitionId < 0) {
                        continue;
                    }

                    String newGroupId = groupId + "-" + tbPartitionId;
                    log.info("Getting offsets for consumer groupId [{}]", newGroupId);
                    Map<TopicPartition, OffsetAndMetadata> newConsumerOffsets = admin.listConsumerGroupOffsets(newGroupId)
                            .partitionsToOffsetAndMetadata().get();

                    if (!newConsumerOffsets.isEmpty()) {
                        log.info("Found existing new group ConsumerOffsets {}", newConsumerOffsets);
                    }

                    var existingOffset = newConsumerOffsets.get(tp);
                    if (existingOffset == null) {
                        log.info("topic offset does not exists in the new node group, all found offsets");
                    } else if (existingOffset.offset() >= om.offset()) {
                        log.info("topic offset {} >= than old node group offset {}", existingOffset.offset(), om.offset());
                        continue;
                    } else {
                        log.info("SHOULD alter topic offset [{}] less than old node group offset [{}]", existingOffset.offset(), om.offset());
                    }

                    Map<TopicPartition, OffsetAndMetadata> newOffsets = Map.of(tp, om);

                    log.warn("@@@@@ alterConsumerGroupOffsets [{}] with new offsets [{}]", newGroupId, newOffsets);
                    admin.alterConsumerGroupOffsets(newGroupId, newOffsets).all().whenComplete((res, err) -> {
                        if (err != null) {
                            log.error("Failed to alterConsumerGroupOffsets for groupId [{}], new offsets [{}]", newGroupId, newOffsets, err);
                        } else {
                            log.info("Updated new consumer group [{}], offsets [{}]", newGroupId, newOffsets);
                        }
                    }).get();  // Handle asynchronously as appropriate

                    //Verify

                    Map<TopicPartition, OffsetAndMetadata> resultedConsumerOffsets = admin.listConsumerGroupOffsets(newGroupId)
                            .partitionsToOffsetAndMetadata().get();

                    MapDifference<TopicPartition, OffsetAndMetadata> diff = Maps.difference(newOffsets, resultedConsumerOffsets);

                    if (!diff.areEqual()) {
                        log.error("Verify failed for groupId [{}], current offset {} is not the same as expected {}", newGroupId, resultedConsumerOffsets, newOffsets);
                    } else {
                        log.info("Verify passed for groupId [{}]", newGroupId);
                    }

                }

            }
        }

    }

    Integer parsePartitionIdFromTopicName(String topic) {
        if (topic == null) {
            return null;
        }
        int dotIndex = topic.lastIndexOf('.');
        if (dotIndex <= 0) {
            return null;
        }

        String indexStr = topic.substring(dotIndex + 1);
        try {
            return Integer.parseInt(indexStr);
        } catch (Throwable t) {
            log.warn("Can't parse partition Id from topic name [{}]", topic, t);
        }
        return null;
    }

}
