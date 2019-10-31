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

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.TbMsgQueueConsumer;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgPack;
import org.thingsboard.server.common.msg.TbMsgSubscriptionParams;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@Slf4j
@Service
public class TbKafkaMsgQueueConsumer implements TbMsgQueueConsumer {

    private CountDownLatch countDownLatch;

    private KafkaConsumer<String, byte[]> consumer;

    private final Map<UUID, TbMsg> map = new ConcurrentHashMap<>();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Autowired
    private TbConsumerSettings settings;

    @Value("${back.pressure.timeout}")
    private long timeout;

    @Value("${back.pressure.strategy}")
    private Strategy strategy;

    @Value("${back.pressure.attempt}")
    private int attempt;

    private final AtomicInteger currentAttempt = new AtomicInteger(0);

    @PostConstruct
    private void init() {
        Properties props = settings.toProps();

        props.put(ConsumerConfig.CLIENT_ID_CONFIG, "tb_rule-engine_client");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "tb_rule-engine_group");

        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArrayDeserializer");

        this.consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Pattern.compile("tb.rule-engine.*"));
    }

    public void init(TbMsgSubscriptionParams subscriptionParams) {

        executor.execute(() -> {
            while (true) {
                if (map.isEmpty()) {
                    currentAttempt.set(0);
                    ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(subscriptionParams.getDuration()));
                    if (records.count() > 0) {

                        UUID tbMsgPackId = UUID.randomUUID();

                        List<TbMsg> tbMsgs = new ArrayList<>();
                        for (ConsumerRecord<String, byte[]> record : records) {
                            TbMsg msg = TbMsg.fromBytes(record.value());
                            tbMsgs.add(msg.copy(msg.getId(), tbMsgPackId, msg.getRuleChainId(), msg.getRuleNodeId(), msg.getClusterPartition()));
                            map.put(msg.getId(), msg);
                        }

                        TbMsgPack tbMsgPack = new TbMsgPack(tbMsgPackId, tbMsgs);
                        send(tbMsgPack);
                    }
                }
            }
        });
    }

    @Override
    public void ack(UUID msgId) {
        map.remove(msgId);
        if (map.isEmpty()) {
            countDownLatch.countDown();
        }
    }

    private void processingAfterSend() {
        if (!map.isEmpty()) {
            switch (strategy) {
                case RETRY:
                    retry();
                    break;
                case IGNORE:
                    autoAcknowledgePack();
                    break;
            }
        }
    }

    private void retry() {
        if (currentAttempt.get() < attempt) {
            currentAttempt.incrementAndGet();
            List<TbMsg> msgs = new ArrayList<>(map.size());
            map.forEach((k, v) -> msgs.add(v));
            TbMsgPack pack = new TbMsgPack(UUID.randomUUID(), msgs);
            send(pack);
        }
    }

    private void send(TbMsgPack msgPack) {
        //sending

        try {
            countDownLatch = new CountDownLatch(1);
            countDownLatch.await(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        processingAfterSend();
    }


    private void autoAcknowledgePack() {
        map.clear();
    }

    @PreDestroy
    private void destroy() {
        executor.shutdown();
        consumer.close();
    }
}