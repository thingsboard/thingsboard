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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTask;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTaskType;
import org.thingsboard.server.common.data.notification.rule.trigger.TaskProcessingFailureTrigger;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.gen.transport.TransportProtos.HousekeeperTaskProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToHousekeeperServiceMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.provider.TbCoreQueueFactory;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.housekeeper.processor.HousekeeperTaskProcessor;
import org.thingsboard.server.service.housekeeper.stats.HousekeeperStatsService;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@TbCoreComponent
@Service
@Slf4j
@ConditionalOnProperty(name = "queue.core.housekeeper.enabled", havingValue = "true", matchIfMissing = true)
public class HousekeeperService {

    private final Map<HousekeeperTaskType, HousekeeperTaskProcessor<?>> taskProcessors;

    private final HousekeeperReprocessingService reprocessingService;
    private final Optional<HousekeeperStatsService> statsService;
    private final NotificationRuleProcessor notificationRuleProcessor;
    private final TbQueueConsumer<TbProtoQueueMsg<ToHousekeeperServiceMsg>> consumer;

    private final ExecutorService consumerExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("housekeeper-consumer"));
    private final ExecutorService executor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("housekeeper-task-processor"));

    @Value("${queue.core.housekeeper.task-processing-timeout-ms:120000}")
    private int taskProcessingTimeout;
    @Value("${queue.core.housekeeper.poll-interval-ms:500}")
    private int pollInterval;

    private boolean stopped;

    public HousekeeperService(HousekeeperReprocessingService reprocessingService,
                              TbCoreQueueFactory queueFactory,
                              Optional<HousekeeperStatsService> statsService,
                              NotificationRuleProcessor notificationRuleProcessor,
                              @Lazy List<HousekeeperTaskProcessor<?>> taskProcessors) {
        this.reprocessingService = reprocessingService;
        this.statsService = statsService;
        this.notificationRuleProcessor = notificationRuleProcessor;
        this.consumer = queueFactory.createHousekeeperMsgConsumer();
        this.taskProcessors = taskProcessors.stream().collect(Collectors.toMap(HousekeeperTaskProcessor::getTaskType, p -> p));
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
                        log.trace("Processing task: {}", msg);
                        try {
                            processTask(msg.getValue());
                        } catch (InterruptedException e) {
                            return;
                        } catch (Throwable e) {
                            log.error("Unexpected error during message processing [{}]", msg, e);
                            reprocessingService.submitForReprocessing(msg.getValue(), e);
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

    @SuppressWarnings("unchecked")
    protected <T extends HousekeeperTask> void processTask(ToHousekeeperServiceMsg msg) throws Exception {
        HousekeeperTask task = JacksonUtil.fromString(msg.getTask().getValue(), HousekeeperTask.class);
        HousekeeperTaskProcessor<T> taskProcessor = (HousekeeperTaskProcessor<T>) taskProcessors.get(task.getTaskType());
        if (taskProcessor == null) {
            throw new IllegalArgumentException("Unsupported task type " + task.getTaskType());
        }

        if (log.isDebugEnabled()) {
            log.debug("[{}] {} task {}", task.getTenantId(), isNew(msg.getTask()) ? "Processing" : "Reprocessing", msg.getTask().getValue());
        }
        try {
            Future<Object> future = executor.submit(() -> {
                taskProcessor.process((T) task);
                return null;
            });
            future.get(taskProcessingTimeout, TimeUnit.MILLISECONDS);

            statsService.ifPresent(statsService -> statsService.reportProcessed(task.getTaskType(), msg));
            log.debug("[{}] Successfully {} task {}", task.getTenantId(), isNew(msg.getTask()) ? "processed" : "reprocessed", msg.getTask().getValue());
        } catch (InterruptedException e) {
            throw e;
        } catch (Throwable e) {
            Throwable error = e;
            if (e instanceof ExecutionException) {
                error = e.getCause();
            } else if (e instanceof TimeoutException) {
                error = new TimeoutException("Timeout after " + taskProcessingTimeout + " seconds");
            }
            log.error("[{}][{}][{}] {} task processing failed, submitting for reprocessing (attempt {}): {}",
                    task.getTenantId(), task.getEntityId().getEntityType(), task.getEntityId(),
                    task.getTaskType(), msg.getTask().getAttempt(), task, error);
            reprocessingService.submitForReprocessing(msg, error);

            statsService.ifPresent(statsService -> statsService.reportFailure(task.getTaskType(), msg));
            notificationRuleProcessor.process(TaskProcessingFailureTrigger.builder()
                    .task(task)
                    .error(error)
                    .attempt(msg.getTask().getAttempt())
                    .build());
        }
    }

    private boolean isNew(HousekeeperTaskProto task) {
        return task.getErrorsCount() == 0;
    }

    @PreDestroy
    private void stop() throws Exception {
        stopped = true;
        consumer.unsubscribe();
        consumerExecutor.shutdown();
        if (!consumerExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
            consumerExecutor.shutdownNow();
        }
        log.info("Stopped Housekeeper service");
    }

}
