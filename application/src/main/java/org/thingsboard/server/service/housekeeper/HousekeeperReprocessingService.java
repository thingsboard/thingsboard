/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.service.housekeeper;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos.ToHousekeeperServiceMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueMsgHeaders;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.provider.TbCoreQueueFactory;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbCoreComponent;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.thingsboard.server.queue.common.AbstractTbQueueTemplate.bytesToLong;
import static org.thingsboard.server.queue.common.AbstractTbQueueTemplate.longToBytes;

@TbCoreComponent
@Service
@Slf4j
public class HousekeeperReprocessingService {

    private final DefaultHousekeeperService housekeeperService;
    private final TbQueueProducerProvider producerProvider;
    private final TbQueueConsumer<TbProtoQueueMsg<ToHousekeeperServiceMsg>> consumer;
    private final ExecutorService consumerExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("housekeeper-reprocessing-consumer"));

    @Value("${queue.core.housekeeper.poll-interval-ms:10000}")
    private int pollInterval;

    private final long startTs = System.currentTimeMillis();
    private boolean stopped;
    // todo: stats

    public HousekeeperReprocessingService(@Lazy DefaultHousekeeperService housekeeperService,
                                          TbCoreQueueFactory queueFactory,
                                          TbQueueProducerProvider producerProvider) {
        this.housekeeperService = housekeeperService;
        this.consumer = queueFactory.createHousekeeperDelayedMsgConsumer();
        this.producerProvider = producerProvider;
    }

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void afterStartUp() {
        consumer.subscribe();
        consumerExecutor.submit(() -> {
            while (!stopped && !consumer.isStopped()) {
                try {
                    List<TbProtoQueueMsg<ToHousekeeperServiceMsg>> msgs = consumer.poll(pollInterval);
                    if (msgs.isEmpty()) {
                        stop();
                        return;
                    }

                    for (TbProtoQueueMsg<ToHousekeeperServiceMsg> msg : msgs) {
                        long msgTs = Uuids.unixTimestamp(msg.getKey());
                        if (msgTs >= startTs) {
                            stop();
                            return; // fixme: we should commit already reprocessed messages
                        }

                        try {
                            reprocessTask(msg);
                        } catch (Exception e) {
                            log.error("Message processing failed", e);
                        }
                    }
                    consumer.commit();
                } catch (Throwable t) {
                    if (!consumer.isStopped()) {
                        log.warn("Failed to process messages from queue", t);
                        try {
                            Thread.sleep(pollInterval);
                        } catch (InterruptedException interruptedException) {
                            log.trace("Failed to wait until the server has capacity to handle new requests", interruptedException);
                        }
                    }
                }
            }
        });
        log.info("Started Housekeeper tasks reprocessing");
    }

    private void reprocessTask(TbProtoQueueMsg<ToHousekeeperServiceMsg> msg) {
        housekeeperService.processTask(msg);// fixme: or should we submit to queue?
    }

    public void submitForReprocessing(TbProtoQueueMsg<ToHousekeeperServiceMsg> msg) {
        TbQueueMsgHeaders msgHeaders = msg.getHeaders();
        long reprocessingAttempts = Optional.ofNullable(msgHeaders.get("reprocessingAttempts"))
                .map(header -> bytesToLong(header))
                .orElse(0L);
        reprocessingAttempts++;
        msgHeaders.put("reprocessingAttempts", longToBytes(reprocessingAttempts));

        var producer = producerProvider.getHousekeeperDelayedMsgProducer();
        TopicPartitionInfo tpi = TopicPartitionInfo.builder().topic(producer.getDefaultTopic()).build();
        producer.send(tpi, new TbProtoQueueMsg<>(Uuids.timeBased(), msg.getValue(), msgHeaders), null);
    }

    @PreDestroy
    private void stop() {
        log.info("Stopped Housekeeper tasks reprocessing");
        stopped = true;
        consumer.unsubscribe();
        consumerExecutor.shutdownNow();
    }

}
