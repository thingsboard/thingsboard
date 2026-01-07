/**
 * Copyright © 2016-2026 The Thingsboard Authors
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

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.springframework.util.StopWatch;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.common.AbstractTbQueueConsumerTemplate;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by ashvayka on 24.09.18.
 */
@Slf4j
public class TbKafkaConsumerTemplate<T extends TbQueueMsg> extends AbstractTbQueueConsumerTemplate<ConsumerRecord<String, byte[]>, T> {

    private final TbKafkaAdmin admin;
    private final KafkaConsumer<String, byte[]> consumer;
    private final TbKafkaDecoder<T> decoder;

    private final TbKafkaConsumerStatsService statsService;
    @Getter
    private final String groupId;

    @Setter
    private Function<String, Long> startOffsetProvider;
    private final boolean readFromBeginning; // reset offset to beginning
    private final boolean stopWhenRead; // stop consuming when reached end offset remembered on start
    private int readCount;
    private Map<Integer, Long> endOffsets; // needed if stopWhenRead is true

    @Builder
    private TbKafkaConsumerTemplate(TbKafkaSettings settings, TbKafkaDecoder<T> decoder,
                                    String clientId, String groupId, String topic,
                                    TbQueueAdmin admin, TbKafkaConsumerStatsService statsService,
                                    boolean readFromBeginning, boolean stopWhenRead) {
        super(topic);
        Properties props = settings.toConsumerProps(topic);
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, clientId);
        if (groupId != null) {
            props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        }

        this.statsService = statsService;
        this.groupId = groupId;

        if (statsService != null) {
            statsService.registerClientGroup(groupId);
        }

        this.admin = (TbKafkaAdmin) admin;
        this.consumer = new KafkaConsumer<>(props);
        this.decoder = decoder;
        this.readFromBeginning = readFromBeginning;
        this.stopWhenRead = stopWhenRead;
    }

    @Override
    protected void doSubscribe(Set<TopicPartitionInfo> partitions) {
        Map<String, List<Integer>> topics;
        if (partitions == null) {
            topics = Collections.emptyMap();
        } else {
            topics = new HashMap<>();
            partitions.forEach(tpi -> {
                if (tpi.isUseInternalPartition()) {
                    topics.computeIfAbsent(tpi.getFullTopicName(), t -> new ArrayList<>()).add(tpi.getPartition().get());
                } else {
                    topics.put(tpi.getFullTopicName(), null);
                }
            });
        }
        if (!topics.isEmpty()) {
            topics.keySet().forEach(admin::createTopicIfNotExists);
            List<String> toSubscribe = new ArrayList<>();
            topics.forEach((topic, kafkaPartitions) -> {
                if (kafkaPartitions == null) {
                    if (groupId != null) {
                        toSubscribe.add(topic);
                        return;
                    } else { // if no consumer group management - manually assigning all topic partitions
                        kafkaPartitions = IntStream.range(0, admin.getNumPartitions()).boxed().toList();
                    }
                }

                List<TopicPartition> topicPartitions = kafkaPartitions.stream()
                        .map(partition -> new TopicPartition(topic, partition))
                        .toList();
                consumer.assign(topicPartitions);
                onPartitionsAssigned(topicPartitions);
            });
            if (!toSubscribe.isEmpty()) {
                if (readFromBeginning || stopWhenRead) {
                    consumer.subscribe(toSubscribe, new ConsumerRebalanceListener() {
                        @Override
                        public void onPartitionsRevoked(Collection<TopicPartition> partitions) {}

                        @Override
                        public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                            log.debug("Handling onPartitionsAssigned {}", partitions);
                            TbKafkaConsumerTemplate.this.onPartitionsAssigned(partitions);
                        }
                    });
                } else {
                    consumer.subscribe(toSubscribe);
                }
            }
        } else {
            log.info("unsubscribe due to empty topic list");
            consumer.unsubscribe();
        }
    }

    @Override
    protected List<ConsumerRecord<String, byte[]>> doPoll(long durationInMillis) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        log.trace("poll topic {} maxDuration {}", getTopic(), durationInMillis);

        ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(durationInMillis));

        stopWatch.stop();
        log.trace("poll topic {} took {}ms", getTopic(), stopWatch.getTotalTimeMillis());

        List<ConsumerRecord<String, byte[]>> recordList;
        if (records.isEmpty()) {
            recordList = Collections.emptyList();
        } else {
            recordList = new ArrayList<>(256);
            records.forEach(record -> {
                recordList.add(record);
                if (stopWhenRead && endOffsets != null) {
                    readCount++;
                    int partition = record.partition();
                    Long endOffset = endOffsets.get(partition);
                    if (endOffset == null) {
                        log.debug("End offset not found for {} [{}]", record.topic(), partition);
                        return;
                    }
                    log.trace("[{}-{}] Got record offset {}, expected end offset: {}", record.topic(), partition, record.offset(), endOffset - 1);
                    if (record.offset() >= endOffset - 1) {
                        endOffsets.remove(partition);
                    }
                }
            });
        }
        if (stopWhenRead && endOffsets != null && endOffsets.isEmpty()) {
            log.info("Finished reading {}, processed {} messages", partitions, readCount);
            stop();
        }
        return recordList;
    }

    private void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        if (readFromBeginning) {
            log.debug("Seeking to beginning for {}", partitions);
            consumer.seekToBeginning(partitions);
        } else if (startOffsetProvider != null) {
            partitions.forEach(topicPartition -> {
                Long offset = startOffsetProvider.apply(topicPartition.topic());
                if (offset != null) {
                    log.debug("Seeking to offset {} for {}", offset, topicPartition);
                    consumer.seek(topicPartition, offset);
                } else {
                    log.info("No start offset provided for {}", topicPartition);
                }
            });
        }
        if (stopWhenRead) {
            log.debug("Getting end offsets for {}", partitions);
            endOffsets = consumer.endOffsets(partitions).entrySet().stream()
                    .filter(entry -> entry.getValue() > 0)
                    .collect(Collectors.toMap(entry -> entry.getKey().partition(), Map.Entry::getValue));
        }
    }

    @Override
    public T decode(ConsumerRecord<String, byte[]> record) throws IOException {
        return decoder.decode(new KafkaTbQueueMsg(record));
    }

    @Override
    protected void doCommit() {
        if (groupId != null) {
            consumer.commitSync();
        }
    }

    @Override
    protected void doUnsubscribe() {
        if (consumer != null) {
            consumer.unsubscribe();
            consumer.close();
        }
        if (statsService != null) {
            statsService.unregisterClientGroup(groupId);
        }
    }

    @Override
    public boolean isLongPollingSupported() {
        return true;
    }

}
