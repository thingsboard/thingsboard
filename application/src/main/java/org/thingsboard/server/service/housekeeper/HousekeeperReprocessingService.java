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
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos.HousekeeperTaskProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToHousekeeperServiceMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.provider.TbCoreQueueFactory;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.util.TbCoreComponent;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@TbCoreComponent
@Service
@Slf4j
public class HousekeeperReprocessingService {

    private final DefaultHousekeeperService housekeeperService;
    private final PartitionService partitionService;
    private final TbCoreQueueFactory queueFactory;
    private final TbQueueProducerProvider producerProvider;

    @Value("${queue.core.housekeeper.reprocessing-start-delay-sec:15}") //  fixme: to 5 minutes
    private int startDelay;
    @Value("${queue.core.housekeeper.task-reprocessing-delay-sec:30}") // fixme: to 30 minutes or 1 hour
    private int reprocessingDelay;
    @Value("${queue.core.housekeeper.max-reprocessing-attempts:10}")
    private int maxReprocessingAttempts;
    @Value("${queue.core.housekeeper.poll-interval-ms:500}")
    private int pollInterval;

    private final ExecutorService consumerExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("housekeeper-reprocessing-consumer"));
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("housekeeper-reprocessing-scheduler"));

    private boolean stopped;

    public HousekeeperReprocessingService(@Lazy DefaultHousekeeperService housekeeperService,
                                          PartitionService partitionService, TbCoreQueueFactory queueFactory,
                                          TbQueueProducerProvider producerProvider) {
        this.housekeeperService = housekeeperService;
        this.partitionService = partitionService;
        this.queueFactory = queueFactory;
        this.producerProvider = producerProvider;
    }

    @PostConstruct
    private void init() {
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                startReprocessing();
            } catch (Throwable e) {
                log.error("Unexpected error during reprocessing", e);
            }
        }, startDelay, reprocessingDelay, TimeUnit.SECONDS);
    }

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
                        log.trace("Reprocessing task: {}", msg);
                        try {
                            housekeeperService.processTask(msg.getValue());
                        } catch (InterruptedException e) {
                            return;
                        } catch (Throwable e) {
                            log.error("Unexpected error during message reprocessing [{}]", msg, e);
                            submitForReprocessing(msg.getValue(), e);
                            // fixme: msgs are duplicated
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
    public void submitForReprocessing(ToHousekeeperServiceMsg msg, Throwable error) {
        HousekeeperTaskProto task = msg.getTask();

        int attempt = task.getAttempt() + 1;
        Set<String> errors = new LinkedHashSet<>(task.getErrorsList());
        errors.add(StringUtils.truncate(ExceptionUtils.getStackTrace(error), 1024));
        msg = msg.toBuilder()
                .setTask(task.toBuilder()
                        .setAttempt(attempt)
                        .clearErrors().addAllErrors(errors)
                        .setTs(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis((long) (reprocessingDelay * 0.8)))
                        .build())
                .build();

        log.trace("Submitting for reprocessing: {}", msg);
        var producer = producerProvider.getHousekeeperReprocessingMsgProducer();
        TopicPartitionInfo tpi = TopicPartitionInfo.builder().topic(producer.getDefaultTopic()).build();
        producer.send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), msg), null); // reprocessing topic has single partition, so we don't care about the msg key
    }

    @PreDestroy
    private void stop() {
        stopped = true;
        scheduler.shutdownNow();
        consumerExecutor.shutdownNow();
    }

}
