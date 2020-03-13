/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.kafka;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.util.StringUtils;
import org.thingsboard.server.TbQueueCallback;
import org.thingsboard.server.TbQueueMsg;
import org.thingsboard.server.TbQueueProducer;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by ashvayka on 24.09.18.
 */
@Slf4j
public class TBKafkaProducerTemplate<T extends TbQueueMsg> implements TbQueueProducer<T> {

    private final KafkaProducer<String, byte[]> producer;

    private final TbKafkaPartitioner<T> partitioner;

    private ConcurrentMap<String, List<PartitionInfo>> partitionInfoMap;

    @Getter
    private final String defaultTopic;

    @Getter
    private final TbKafkaSettings settings;

    @Builder
    private TBKafkaProducerTemplate(TbKafkaSettings settings, TbKafkaPartitioner<T> partitioner, String defaultTopic, String clientId) {
        Properties props = settings.toProps();
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");
        if (!StringUtils.isEmpty(clientId)) {
            props.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
        }
        this.settings = settings;
        this.producer = new KafkaProducer<>(props);
        this.partitioner = partitioner;
        this.defaultTopic = defaultTopic;
    }

    public void init() {
        this.partitionInfoMap = new ConcurrentHashMap<>();
        if (!StringUtils.isEmpty(defaultTopic)) {
            try {
                TBKafkaAdmin admin = new TBKafkaAdmin(this.settings);
                admin.waitForTopic(defaultTopic, 30, TimeUnit.SECONDS);
                log.info("[{}] Topic exists.", defaultTopic);
            } catch (Exception e) {
                log.info("[{}] Failed to wait for topic: {}", defaultTopic, e.getMessage(), e);
                throw new RuntimeException(e);
            }
            //Maybe this should not be cached, but we don't plan to change size of partitions
            this.partitionInfoMap.putIfAbsent(defaultTopic, producer.partitionsFor(defaultTopic));
        }
    }

    @Override
    public void send(T msg, TbQueueCallback callback) {
        send(defaultTopic, msg, callback);
    }

    @Override
    public void send(String topic, T msg, TbQueueCallback callback) {
        String key = msg.getKey().toString();
        byte[] data = msg.getData();
        ProducerRecord<String, byte[]> record;
        Iterable<Header> headers = msg.getHeaders().getData().entrySet().stream().map(e -> new RecordHeader(e.getKey(), e.getValue())).collect(Collectors.toList());

        Integer partition = getPartition(topic, msg);
        record = new ProducerRecord<>(topic, partition, key, data, headers);
        Future<RecordMetadata> result = producer.send(record, (metadata, exception) -> {
            if (exception == null) {
                callback.onSuccess(new KafkaTbQueueMsgMetadata(metadata));
            } else {
                callback.onFailure(exception);
            }
        });
    }

    private Integer getPartition(String topic, T value) {
        if (partitioner == null) {
            return null;
        } else {
            return partitioner.partition(topic, value.getKey().toString(), value, value.getData(), partitionInfoMap.computeIfAbsent(topic, producer::partitionsFor));
        }
    }
}
