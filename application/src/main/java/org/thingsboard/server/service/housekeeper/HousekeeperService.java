/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTask;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTaskType;
import org.thingsboard.server.common.data.notification.rule.trigger.TaskProcessingFailureTrigger;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.gen.transport.TransportProtos.ToHousekeeperServiceMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.common.consumer.QueueConsumerManager;
import org.thingsboard.server.queue.housekeeper.HousekeeperConfig;
import org.thingsboard.server.queue.provider.TbCoreQueueFactory;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.housekeeper.processor.HousekeeperTaskProcessor;
import org.thingsboard.server.service.housekeeper.stats.HousekeeperStatsService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@TbCoreComponent
@Service
@Slf4j
public class HousekeeperService {

    private final Map<HousekeeperTaskType, HousekeeperTaskProcessor<?>> taskProcessors;

    private final HousekeeperConfig config;
    private final HousekeeperReprocessingService reprocessingService;
    private final Optional<HousekeeperStatsService> statsService;
    private final NotificationRuleProcessor notificationRuleProcessor;
    private final QueueConsumerManager<TbProtoQueueMsg<ToHousekeeperServiceMsg>> consumer;

    private final ExecutorService consumerExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("housekeeper-consumer"));
    private final ExecutorService taskExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("housekeeper-task-processor"));
    private final ExecutorService asyncTaskExecutor;
    private final ScheduledExecutorService timeoutScheduler;

    public HousekeeperService(HousekeeperConfig config,
                              HousekeeperReprocessingService reprocessingService,
                              TbCoreQueueFactory queueFactory,
                              Optional<HousekeeperStatsService> statsService,
                              NotificationRuleProcessor notificationRuleProcessor,
                              @Lazy List<HousekeeperTaskProcessor<?>> taskProcessors) {
        this.config = config;
        this.reprocessingService = reprocessingService;
        this.statsService = statsService;
        this.notificationRuleProcessor = notificationRuleProcessor;
        this.consumer = QueueConsumerManager.<TbProtoQueueMsg<ToHousekeeperServiceMsg>>builder()
                .name("Housekeeper")
                .msgPackProcessor(this::processMsgs)
                .pollInterval(config.getPollInterval())
                .consumerCreator(queueFactory::createHousekeeperMsgConsumer)
                .consumerExecutor(consumerExecutor)
                .build();
        this.taskProcessors = taskProcessors.stream().collect(Collectors.toMap(HousekeeperTaskProcessor::getTaskType, p -> p));
        if (config.isAsyncProcessingEnabled()) {
            int threads = config.getAsyncProcessingThreads() == 0 ? Math.max(4, Runtime.getRuntime().availableProcessors()) : config.getAsyncProcessingThreads();
            this.asyncTaskExecutor = Executors.newFixedThreadPool(threads, ThingsBoardThreadFactory.forName("housekeeper-async-task-processor"));
            this.timeoutScheduler = Executors.newScheduledThreadPool(Math.max(2, threads / 4), ThingsBoardThreadFactory.forName("housekeeper-timeout-scheduler"));
            log.trace("Async processing enabled with {} threads and timeout scheduler", threads);
        } else {
            this.asyncTaskExecutor = null;
            this.timeoutScheduler = null;
            log.trace("Async processing disabled, all tasks will be processed sequentially");
        }
    }

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void afterStartUp() {
        consumer.subscribe();
        consumer.launch();
    }

    private void processMsgs(List<TbProtoQueueMsg<ToHousekeeperServiceMsg>> msgs, TbQueueConsumer<TbProtoQueueMsg<ToHousekeeperServiceMsg>> consumer) {
        List<SettableFuture<Void>> asyncFutures = new ArrayList<>();

        for (TbProtoQueueMsg<ToHousekeeperServiceMsg> msg : msgs) {
            log.trace("Processing task: {}", msg);
            try {
                SettableFuture<Void> asyncFuture = processTask(msg.getValue(), false);
                if (asyncFuture != null) {
                    asyncFutures.add(asyncFuture);
                }
            } catch (InterruptedException e) {
                return;
            } catch (Throwable e) {
                log.error("Unexpected error during message processing [{}]", msg, e);
                reprocessingService.submitForReprocessing(msg.getValue(), e);
            }
        }

        if (!asyncFutures.isEmpty()) {
            ListenableFuture<List<Void>> allFutures = Futures.allAsList(asyncFutures);
            try {
                allFutures.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (ExecutionException e) {
                log.trace("Async tasks completed with errors (already handled)", e);
            }
        }

        consumer.commit();
    }

    @SuppressWarnings("unchecked")
    protected <T extends HousekeeperTask> SettableFuture<Void> processTask(ToHousekeeperServiceMsg msg, boolean isReprocessing) throws Exception {
        HousekeeperTask task = JacksonUtil.fromString(msg.getTask().getValue(), HousekeeperTask.class);
        HousekeeperTaskType taskType = task.getTaskType();

        if (config.getDisabledTaskTypes().contains(taskType)) {
            log.debug("Task type {} is disabled, ignoring {}", taskType, task);
            return null;
        }
        HousekeeperTaskProcessor<T> taskProcessor = (HousekeeperTaskProcessor<T>) taskProcessors.get(taskType);
        if (taskProcessor == null) {
            throw new IllegalArgumentException("Unsupported task type " + taskType);
        }

        if (!isReprocessing && config.isAsyncProcessingEnabled() && taskProcessor.supportsAsyncProcessing()) {
            return processTaskAsync(msg, task, taskType, taskProcessor);
        } else {
            processTaskSync(msg, task, taskType, taskProcessor);
            return null;
        }
    }

    private <T extends HousekeeperTask> SettableFuture<Void> processTaskAsync(ToHousekeeperServiceMsg msg, HousekeeperTask task, HousekeeperTaskType taskType, HousekeeperTaskProcessor<T> taskProcessor) {
        long startTs = System.currentTimeMillis();
        SettableFuture<Void> completionFuture = SettableFuture.create();

        asyncTaskExecutor.submit(() -> {
            try {
                ListenableFuture<Void> processFuture = taskProcessor.processAsync((T) task);

                ScheduledFuture<?> timeoutFuture = timeoutScheduler.schedule(() -> {
                    if (!completionFuture.isDone()) {
                        TimeoutException timeoutException = new TimeoutException("Timeout after " + config.getTaskProcessingTimeout() + " ms");
                        completionFuture.setException(timeoutException);
                        handleFailure(taskType, msg, task, timeoutException);
                    }
                }, config.getTaskProcessingTimeout(), TimeUnit.MILLISECONDS);

                processFuture.addListener(() -> {
                    timeoutFuture.cancel(false);
                    try {
                        processFuture.get();
                        long timing = System.currentTimeMillis() - startTs;
                        if (log.isDebugEnabled()) {
                            log.debug("[{}] Processed {} in {} ms (attempt {})", task.getTenantId(), task.getDescription(), timing, msg.getTask().getAttempt());
                        }
                        statsService.ifPresent(s -> s.reportProcessed(taskType, msg, timing));
                        completionFuture.set(null);
                    } catch (Throwable e) {
                        if (!completionFuture.isDone()) {
                            handleFailure(taskType, msg, task, e);
                            completionFuture.setException(e);
                        }
                    }
                }, asyncTaskExecutor);

            } catch (Throwable e) {
                if (!completionFuture.isDone()) {
                    handleFailure(taskType, msg, task, e);
                    completionFuture.setException(e);
                }
            }
        });

        return completionFuture;
    }

    private <T extends HousekeeperTask> void processTaskSync(ToHousekeeperServiceMsg msg, HousekeeperTask task, HousekeeperTaskType taskType, HousekeeperTaskProcessor<T> taskProcessor) throws Exception {
        Future<Object> future = null;
        try {
            long startTs = System.currentTimeMillis();
            future = taskExecutor.submit(() -> {
                taskProcessor.process((T) task);
                return null;
            });
            future.get(config.getTaskProcessingTimeout(), TimeUnit.MILLISECONDS);

            long timing = System.currentTimeMillis() - startTs;
            if (log.isDebugEnabled()) {
                log.debug("[{}] Processed {} in {} ms (attempt {})", task.getTenantId(), task.getDescription(), timing, msg.getTask().getAttempt());
            }
            statsService.ifPresent(statsService -> statsService.reportProcessed(taskType, msg, timing));
        } catch (InterruptedException e) {
            throw e;
        } catch (Throwable e) {
            handleFailure(taskType, msg, task, e);
            throw e;
        } finally {
            if (future != null && !future.isDone()) {
                future.cancel(true);
            }
        }
    }

    private void handleFailure(HousekeeperTaskType taskType, ToHousekeeperServiceMsg msg, HousekeeperTask task, Throwable error) {
        if (error instanceof ExecutionException && error.getCause() != null) {
            error = error.getCause();
        }
        if (error instanceof TimeoutException) {
            error = new TimeoutException("Timeout after " + config.getTaskProcessingTimeout() + " ms");
        }

        if (msg.getTask().getAttempt() < config.getMaxReprocessingAttempts()) {
            log.warn("[{}] Failed to process {} (attempt {}), submitting for reprocessing",
                    task.getTenantId(), task.getDescription(), msg.getTask().getAttempt(), error);
            reprocessingService.submitForReprocessing(msg, error);
        } else {
            log.error("[{}] Failed to process task in {} attempts: {}", task.getTenantId(), msg.getTask().getAttempt(), msg, error);
            notificationRuleProcessor.process(TaskProcessingFailureTrigger.builder()
                    .task(task)
                    .error(error)
                    .attempt(msg.getTask().getAttempt())
                    .build());
        }
        statsService.ifPresent(statsService -> statsService.reportFailure(taskType, msg));
    }

    private void initAsyncTaskExecutor() {

    }

    @PreDestroy
    private void stop() {
        consumer.stop();
        consumerExecutor.shutdownNow();
        taskExecutor.shutdownNow();
        if (asyncTaskExecutor != null) {
            asyncTaskExecutor.shutdownNow();
        }
        if (timeoutScheduler != null) {
            timeoutScheduler.shutdownNow();
        }
        log.info("Stopped Housekeeper service");
    }

}
