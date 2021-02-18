/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.common.AbstractTbQueueConsumerTemplate;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by ashvayka on 24.09.18.
 */
@Slf4j
public class TbKafkaConsumerTemplate<T extends TbQueueMsg> extends AbstractTbQueueConsumerTemplate<ConsumerRecord<String, byte[]>, T> {

    private final TbQueueAdmin admin;
    private final KafkaConsumer<String, byte[]> consumer;
    private final TbKafkaDecoder<T> decoder;

    private final String clientId;
    private final String groupId;
    private final TbKafkaConsumerStatisticConfig statsConfig;

    @Builder
    private TbKafkaConsumerTemplate(TbKafkaSettings settings, TbKafkaDecoder<T> decoder,
                                    String clientId, String groupId, String topic,
                                    TbQueueAdmin admin, TbKafkaConsumerStatisticConfig statsConfig) {
        super(topic);
        Properties props = settings.toConsumerProps();
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, clientId);
        if (groupId != null) {
            props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        }

        this.clientId = clientId;
        this.groupId = groupId;
        this.statsConfig = statsConfig;

        this.admin = admin;
        this.consumer = new KafkaConsumer<>(props);
        this.decoder = decoder;
    }

    @Override
    protected void doSubscribe(List<String> topicNames) {
        if (!topicNames.isEmpty()) {
            topicNames.forEach(admin::createTopicIfNotExists);
            consumer.subscribe(topicNames);
        } else {
            consumer.unsubscribe();
        }
        processConsumerStats();
    }

    @Override
    protected List<ConsumerRecord<String, byte[]>> doPoll(long durationInMillis) {
        ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(durationInMillis));
        processConsumerStats();
        if (records.isEmpty()) {
            return Collections.emptyList();
        } else {
            List<ConsumerRecord<String, byte[]>> recordList = new ArrayList<>(256);
            records.forEach(recordList::add);
            return recordList;
        }
    }

    @Override
    public T decode(ConsumerRecord<String, byte[]> record) throws IOException {
        return decoder.decode(new KafkaTbQueueMsg(record));
    }

    @Override
    protected void doCommit() {
        consumer.commitAsync();
    }

    @Override
    protected void doUnsubscribe() {
        if (consumer != null) {
            consumer.unsubscribe();
            consumer.close();
        }
    }

    private final AtomicLong previousLogTime = new AtomicLong();

    private void processConsumerStats() {
        if (statsConfig == null || !statsConfig.getStatsEnabled()) {
            return;
        }
        try {
            long now = System.currentTimeMillis();
            long msSinceLastPrint = now - previousLogTime.get();
            if (msSinceLastPrint > statsConfig.getMinimumPrintIntervalMs()) {
                previousLogTime.getAndSet(now);
                Duration timeoutDuration = Duration.ofMillis(statsConfig.getKafkaResponseTimeoutMs());
                Set<TopicPartition> consumerTopicPartitions = consumer.assignment();
                Map<TopicPartition, Long> endOffsets = consumer.endOffsets(consumerTopicPartitions, timeoutDuration);

                StringBuilder statsStringBuilder = new StringBuilder();
                for (TopicPartition topicPartition : consumerTopicPartitions) {
                    Long endOffset = endOffsets.get(topicPartition);
                    long currentOffset = consumer.position(topicPartition, timeoutDuration);
                    statsStringBuilder.append("[topic:[").append(topicPartition.topic()).append("],")
                            .append("partition:[").append(topicPartition.partition()).append("],")
                            .append("currentOffset:[").append(currentOffset).append("],")
                            .append("endOffset:[").append(endOffset).append("],")
                            .append("lag:[").append(endOffset - currentOffset).append("]],");
                }
                log.debug("[{}][{}] Stats: {}. Seconds between prints: {}", groupId, clientId, statsStringBuilder.toString(), msSinceLastPrint / 1000);
            }
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to load consumer stats. Reason: {}", groupId, clientId, e.getMessage());
            log.trace("Detailed error: ", e);
        }
    }
}
