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
package org.thingsboard.server.service.queue;

import com.google.common.util.concurrent.FutureCallback;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.thingsboard.rule.engine.api.TbMsgQueueConsumer;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgPack;
import org.thingsboard.server.common.msg.TbMsgSubscriptionParams;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Slf4j
public class TbKafkaMsgQueueConsumer implements TbMsgQueueConsumer {

    private final KafkaConsumer<String, byte[]> consumer;

    private final Map<String, Set<UUID>> map = new ConcurrentHashMap<>();
    private final Set<UUID> set = new HashSet<>();

    private volatile boolean prevPackAcknowledged = true;

    @Builder
    private TbKafkaMsgQueueConsumer(TbConsumerSettings settings, String clientId, String groupId) {
        Properties props = settings.toProps();
        if (clientId != null) {
            props.put(ConsumerConfig.CLIENT_ID_CONFIG, clientId);
        }
        if (groupId != null) {
            props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        }
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        this.consumer = new KafkaConsumer<>(props);
    }

    public void subscribeTopics() {
        consumer.subscribe(Pattern.compile("tb.rule-engine.*"));
    }

    public void unsubscribeTopics() {
        consumer.unsubscribe();
    }

    @Override
    public void subscribe(TbMsgSubscriptionParams subscriptionParams, FutureCallback<TbMsgPack> callback) {

        long currTs = System.currentTimeMillis();

        if (prevPackAcknowledged) {
            prevPackAcknowledged = false;
            ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(subscriptionParams.getDuration()));
            if (records.count() > 0) {

                UUID tbMsgPackId = UUID.randomUUID();

                List<TbMsg> tbMsgs = new ArrayList<>();
                for (ConsumerRecord<String, byte[]> record : records) {

                    if (record.topic().equals(subscriptionParams.getTopic())) {

                        TbMsg msg = TbMsg.fromBytes(record.value());
                        tbMsgs.add(msg.copy(msg.getId(), tbMsgPackId, msg.getRuleChainId(), msg.getRuleNodeId(), msg.getClusterPartition()));

                        set.add(msg.getId());
                    }

                }

                TbMsgPack tbMsgPack = new TbMsgPack(tbMsgPackId, tbMsgs);
                if (callback != null) {
                    callback.onSuccess(tbMsgPack);
                }
            } else {
                log.debug("No messages fetched from topic {}", subscriptionParams.getTopic());
                if (callback != null) {
                    callback.onFailure(new RuntimeException());
                }
            }

            map.put(subscriptionParams.getTopic(), set);
        } else {


            sleep(1);
            //autoAcknowledgePack or add to queue?
        }
    }

    @Override
    public void ack(TbMsg tbMsg) {
        set.remove(tbMsg.getId());
        if (set.size() == 0) {
            consumer.commitSync(Collections.singletonMap(new TopicPartition("", 0), null));
            prevPackAcknowledged = true;
        }
    }

    private void autoAcknowledgePack() {

    }

    private void sleep(long sleepInterval) {
        try {
            Thread.sleep(sleepInterval);
        } catch (InterruptedException e) {
            log.warn("Failed to sleep a bit!", e);
        }
    }
}