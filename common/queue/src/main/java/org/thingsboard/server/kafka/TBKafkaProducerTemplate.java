/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.header.Header;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;

/**
 * Created by ashvayka on 24.09.18.
 */
@Slf4j
public class TBKafkaProducerTemplate<T> {

    private final KafkaProducer<String, byte[]> producer;
    private final TbKafkaEncoder<T> encoder;

    @Builder.Default
    private TbKafkaEnricher<T> enricher = ((value, responseTopic, requestId) -> value);

    private final TbKafkaPartitioner<T> partitioner;
    private List<PartitionInfo> partitionInfoList;
    @Getter
    private final String defaultTopic;

    @Getter
    private final TbKafkaSettings settings;

    @Builder
    private TBKafkaProducerTemplate(TbKafkaSettings settings, TbKafkaEncoder<T> encoder, TbKafkaEnricher<T> enricher,
                                    TbKafkaPartitioner<T> partitioner, String defaultTopic) {
        Properties props = settings.toProps();
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");
        this.settings = settings;
        this.producer = new KafkaProducer<>(props);
        this.encoder = encoder;
        this.enricher = enricher;
        this.partitioner = partitioner;
        this.defaultTopic = defaultTopic;
    }

    public void init() {
        try {
            TBKafkaAdmin admin = new TBKafkaAdmin(this.settings);
            CreateTopicsResult result = admin.createTopic(new NewTopic(defaultTopic, 100, (short) 1));
            result.all().get();
        } catch (Exception e) {
            log.trace("Failed to create topic: {}", e.getMessage(), e);
        }
        //Maybe this should not be cached, but we don't plan to change size of partitions
        this.partitionInfoList = producer.partitionsFor(defaultTopic);
    }

    public T enrich(T value, String responseTopic, UUID requestId) {
        return enricher.enrich(value, responseTopic, requestId);
    }

    public Future<RecordMetadata> send(String key, T value) {
        return send(key, value, null, null);
    }

    public Future<RecordMetadata> send(String key, T value, Iterable<Header> headers) {
        return send(key, value, null, headers);
    }

    public Future<RecordMetadata> send(String key, T value, Long timestamp, Iterable<Header> headers) {
        return send(this.defaultTopic, key, value, timestamp, headers);
    }

    public Future<RecordMetadata> send(String topic, String key, T value, Long timestamp, Iterable<Header> headers) {
        byte[] data = encoder.encode(value);
        ProducerRecord<String, byte[]> record;
        Integer partition = getPartition(topic, key, value, data);
        record = new ProducerRecord<>(this.defaultTopic, partition, timestamp, key, data, headers);
        return producer.send(record);
    }

    private Integer getPartition(String topic, String key, T value, byte[] data) {
        if (partitioner == null) {
            return null;
        } else {
            return partitioner.partition(this.defaultTopic, key, value, data, partitionInfoList);
        }
    }
}
