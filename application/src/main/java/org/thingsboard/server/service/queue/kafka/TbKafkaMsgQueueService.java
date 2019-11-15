/**
 * Copyright © 2016-2019 The Thingsboard Authors
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
package org.thingsboard.server.service.queue.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.service.queue.TbAbstractMsgQueueService;
import org.thingsboard.server.service.queue.TbMsgQueuePack;
import org.thingsboard.server.service.queue.TbMsgQueueState;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@Service
@Slf4j
@ConditionalOnProperty(prefix = "backpressure", value = "type", havingValue = "kafka")
public class TbKafkaMsgQueueService extends TbAbstractMsgQueueService {

    @Autowired
    private TbConsumerSettings consumerSettings;

    @Autowired
    private TbProducerSettings producerSettings;

    @Value("${backpressure.timeout}")
    private long timeout;

    @Value("${kafka.queue.producer.topic}")
    private String topic;

    @Value("${kafka.queue.consumer.topic_pattern}")
    private String topicPattern;

    private KafkaProducer<String, byte[]> producer;
    private KafkaConsumer<String, byte[]> consumer;

    @PostConstruct
    private void init() {
        ackMap.put(collectiveTenantId, new AtomicBoolean(true));
        specialTenants.forEach(tenantId -> ackMap.put(tenantId, new AtomicBoolean(true)));

        initProducer();
        initConsumer();
    }

    @Override
    public void add(TbMsg msg, TenantId tenantId) {
        log.info("Add new message: [{}] for tenant: [{}]", msg, tenantId.getId());
        byte[] data = TbMsg.toByteArray(msg);
        producer.send(new ProducerRecord<>(topic, tenantId.getId().toString(), data));
    }

    private void initProducer() {
        Properties props = producerSettings.toProps();
        this.producer = new KafkaProducer<>(props);
    }

    private void initConsumer() {
        Properties props = consumerSettings.toProps();
        this.consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Pattern.compile(topicPattern));

        executor.execute(() -> {
            while (!STOPPED) {
                if (ackMap.get(collectiveTenantId).get()) {
                    ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(timeout));
                    if (records.count() > 0) {
                        ackMap.get(collectiveTenantId).set(false);
                        createAndSendTbMsgQueuePack(records);
                    }
                }
            }
        });
    }

    private void createAndSendTbMsgQueuePack(ConsumerRecords<String, byte[]> records) {
        UUID packId = UUID.randomUUID();
        TbMsgQueuePack pack = new TbMsgQueuePack(packId, new AtomicInteger(0), new AtomicInteger(0), new AtomicInteger(0), new AtomicBoolean(false), collectiveTenantId);
        for (ConsumerRecord<String, byte[]> record : records) {
            TenantId tenantId = new TenantId(UUID.fromString(record.key()));
            TbMsg msg = TbMsg.fromBytes(record.value());
            TbMsgQueueState msgQueueState = new TbMsgQueueState(
                    msg.copy(msg.getId(), packId, msg.getRuleChainId(), msg.getRuleNodeId(), msg.getClusterPartition()),
                    tenantId,
                    new AtomicInteger(0),
                    new AtomicBoolean(false));
            pack.addMsg(msgQueueState);
        }
        packMap.put(pack.getTenantId(), pack);
        send(pack);
    }

    @PreDestroy
    @Override
    protected void destroy() {
        super.destroy();
        if (producer != null) {
            producer.close();
        }
        if (consumer != null) {
            consumer.close();
        }
    }
}
