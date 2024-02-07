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
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.housekeeper.HousekeeperService;
import org.thingsboard.server.dao.housekeeper.data.HousekeeperTask;
import org.thingsboard.server.dao.housekeeper.data.HousekeeperTaskType;
import org.thingsboard.server.gen.transport.TransportProtos.HousekeeperTaskProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToHousekeeperServiceMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.provider.TbCoreQueueFactory;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.housekeeper.processor.HousekeeperTaskProcessor;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@TbCoreComponent
@Service
@Slf4j
public class DefaultHousekeeperService implements HousekeeperService {

    private final Map<HousekeeperTaskType, HousekeeperTaskProcessor> taskProcessors;

    private final TbQueueConsumer<TbProtoQueueMsg<ToHousekeeperServiceMsg>> consumer;
    private final TbQueueProducer<TbProtoQueueMsg<ToHousekeeperServiceMsg>> producer;
    private final HousekeeperReprocessingService reprocessingService;
    private final DataDecodingEncodingService dataDecodingEncodingService;
    private final ExecutorService consumerExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("housekeeper-consumer"));

    @Value("${queue.core.housekeeper.poll-interval-ms:10000}")
    private int pollInterval;

    private boolean stopped;

    public DefaultHousekeeperService(HousekeeperReprocessingService reprocessingService,
                                     TbCoreQueueFactory queueFactory,
                                     TbQueueProducerProvider producerProvider,
                                     DataDecodingEncodingService dataDecodingEncodingService, List<HousekeeperTaskProcessor> taskProcessors) {
        this.consumer = queueFactory.createHousekeeperMsgConsumer();
        this.producer = producerProvider.getHousekeeperMsgProducer();
        this.reprocessingService = reprocessingService;
        this.taskProcessors = taskProcessors.stream().collect(Collectors.toMap(HousekeeperTaskProcessor::getTaskType, p -> p));
        this.dataDecodingEncodingService = dataDecodingEncodingService;
    }

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void afterStartUp() {
        consumer.subscribe();
        consumerExecutor.submit(() -> {
            while (!stopped && !consumer.isStopped()) {
                try {
                    List<TbProtoQueueMsg<ToHousekeeperServiceMsg>> msgs = consumer.poll(pollInterval);
                    if (msgs.isEmpty()) {
                        continue;
                    }

                    for (TbProtoQueueMsg<ToHousekeeperServiceMsg> msg : msgs) {
                        try {
                            processTask(msg);
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
        log.info("Started Housekeeper service");
    }

    protected void processTask(TbProtoQueueMsg<ToHousekeeperServiceMsg> msg) {
        HousekeeperTask task = dataDecodingEncodingService.<HousekeeperTask>decode(msg.getValue().getTask().getValue().toByteArray()).get();
        HousekeeperTaskProcessor taskProcessor = taskProcessors.get(task.getTaskType());
        if (taskProcessor == null) {
            log.error("Unsupported task type {}: {}", task.getTaskType(), task);
            return;
        }

        log.info("[{}] Processing task: {}", task.getTenantId(), task);
        try {
            taskProcessor.process(task);
        } catch (Exception e) {
            log.error("[{}] Task processing failed: {}", task.getTenantId(), task, e);
            reprocessingService.submitForReprocessing(msg);
        }
    }

    @Override
    public void submitTask(HousekeeperTask task) {
        TopicPartitionInfo tpi = TopicPartitionInfo.builder().topic(producer.getDefaultTopic()).build();
        producer.send(tpi, new TbProtoQueueMsg<>(Uuids.timeBased(), ToHousekeeperServiceMsg.newBuilder()
                .setTask(HousekeeperTaskProto.newBuilder()
                        .setValue(ByteString.copyFrom(dataDecodingEncodingService.encode(task)))
                        .build())
                .build()), null);
    }

    @PreDestroy
    private void stop() {
        log.info("Stopped Housekeeper service");
        stopped = true;
        consumer.unsubscribe();
        consumerExecutor.shutdownNow();
    }
}
