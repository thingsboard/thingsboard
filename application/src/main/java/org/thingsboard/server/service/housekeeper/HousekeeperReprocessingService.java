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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos.HousekeeperTaskProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToHousekeeperServiceMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.provider.TbCoreQueueFactory;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.util.TbCoreComponent;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@TbCoreComponent
@Service
@Slf4j
public class HousekeeperReprocessingService {

    private final DefaultHousekeeperService housekeeperService;
    private final PartitionService partitionService;
    private final TbCoreQueueFactory queueFactory;
    private final TbQueueProducerProvider producerProvider;

    private final ExecutorService consumerExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("housekeeper-reprocessing-consumer"));

    private static final int startDelay = 15; // fixme - to 5 minutes
    private static final int reprocessingDelay = 30; // seconds
    private static final int maxReprocessingAttempts = 5;
    @Value("${queue.core.housekeeper.poll-interval-ms:500}")
    private int pollInterval;

    private boolean stopped;
    // todo: stats

    public HousekeeperReprocessingService(@Lazy DefaultHousekeeperService housekeeperService,
                                          PartitionService partitionService, TbCoreQueueFactory queueFactory,
                                          TbQueueProducerProvider producerProvider) {
        this.housekeeperService = housekeeperService;
        this.partitionService = partitionService;
        this.queueFactory = queueFactory;
        this.producerProvider = producerProvider;
    }

    @Scheduled(initialDelay = startDelay, fixedDelay = reprocessingDelay, timeUnit = TimeUnit.SECONDS)
    public void startReprocessing() {
        if (!partitionService.isMyPartition(ServiceType.TB_CORE, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID)) {
            return;
        }

        var consumer = queueFactory.createHousekeeperReprocessingMsgConsumer();
        consumer.subscribe();
        consumerExecutor.submit(() -> {
            log.info("Starting Housekeeper tasks reprocessing");
            long startTs = System.currentTimeMillis();
            while (!stopped) {
                try {
                    List<TbProtoQueueMsg<ToHousekeeperServiceMsg>> msgs = consumer.poll(pollInterval);
                    if (msgs.isEmpty() || msgs.stream().anyMatch(msg -> msg.getValue().getTask().getTs() >= startTs)) { // msg batch size should be 1. otherwise some tasks won't be reprocessed immediately
                        break;
                    }

                    for (TbProtoQueueMsg<ToHousekeeperServiceMsg> msg : msgs) {
                        try {
                            housekeeperService.processTask(msg);
                        } catch (Exception e) {
                            log.error("Unexpected error during message reprocessing [{}]", msg, e);
                            submitForReprocessing(msg, e);
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
            consumer.unsubscribe();
            log.info("Stopped Housekeeper tasks reprocessing");
        });
    }

    // todo: dead letter queue if attempts count exceeds the configured maximum
    public void submitForReprocessing(TbProtoQueueMsg<ToHousekeeperServiceMsg> queueMsg, Throwable error) {
        ToHousekeeperServiceMsg msg = queueMsg.getValue();
        HousekeeperTaskProto task = msg.getTask();
        int attempt = task.getAttempt() + 1;
        msg = msg.toBuilder()
                .setTask(task.toBuilder()
                        .setAttempt(attempt)
                        .addErrors(StringUtils.truncate(ExceptionUtils.getStackTrace(error), 1024))
                        .setTs(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(reprocessingDelay))
                        .build())
                .build();

        log.trace("Submitting for reprocessing: {}", msg);
        var producer = producerProvider.getHousekeeperReprocessingMsgProducer();
        TopicPartitionInfo tpi = TopicPartitionInfo.builder().topic(producer.getDefaultTopic()).build();
        producer.send(tpi, new TbProtoQueueMsg<>(queueMsg.getKey(), msg), null);
    }

    @PreDestroy
    private void stop() {
        stopped = true;
        consumerExecutor.shutdownNow();
    }

}
