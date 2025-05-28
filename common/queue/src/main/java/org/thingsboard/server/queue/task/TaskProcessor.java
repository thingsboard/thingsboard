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
package org.thingsboard.server.queue.task;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.SetCache;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.job.task.Task;
import org.thingsboard.server.common.data.job.task.TaskResult;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.queue.QueueConfig;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos.TaskProto;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.common.consumer.MainQueueConsumerManager;
import org.thingsboard.server.queue.discovery.QueueKey;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.settings.TasksQueueConfig;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class TaskProcessor<T extends Task<R>, R extends TaskResult> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private TaskProcessorQueueFactory queueFactory;
    @Autowired
    private JobStatsService statsService;
    @Autowired
    private TaskProcessorExecutors executors;
    @Autowired
    private TasksQueueConfig config;

    private QueueKey queueKey;
    private MainQueueConsumerManager<TbProtoQueueMsg<TaskProto>, QueueConfig> taskConsumer;
    private final ExecutorService taskExecutor = Executors.newCachedThreadPool(ThingsBoardThreadFactory.forName(getJobType().name().toLowerCase() + "-task-processor"));

    private final SetCache<String> discarded = new SetCache<>(TimeUnit.MINUTES.toMillis(60));
    private final SetCache<String> failed = new SetCache<>(TimeUnit.MINUTES.toMillis(60));

    private final SetCache<UUID> deletedTenants = new SetCache<>(TimeUnit.MINUTES.toMillis(60));

    @PostConstruct
    public void init() {
        queueKey = new QueueKey(ServiceType.TASK_PROCESSOR, getJobType().name());
        taskConsumer = MainQueueConsumerManager.<TbProtoQueueMsg<TaskProto>, QueueConfig>builder()
                .queueKey(queueKey)
                .config(QueueConfig.of(true, config.getPollInterval()))
                .msgPackProcessor(this::processMsgs)
                .consumerCreator((queueConfig, tpi) -> queueFactory.createTaskConsumer(getJobType()))
                .consumerExecutor(executors.getConsumersExecutor())
                .scheduler(executors.getScheduler())
                .taskExecutor(executors.getMgmtExecutor())
                .build();
    }

    @EventListener
    public void onPartitionChangeEvent(PartitionChangeEvent event) {
        if (event.getServiceType() == ServiceType.TASK_PROCESSOR) {
            Set<TopicPartitionInfo> partitions = event.getNewPartitions().get(queueKey);
            taskConsumer.update(partitions);
        }
    }

    @EventListener
    public void onComponentLifecycle(ComponentLifecycleMsg event) {
        EntityId entityId = event.getEntityId();
        switch (entityId.getEntityType()) {
            case JOB -> {
                String tasksKey = event.getInfo().get("tasksKey").asText();
                if (event.getEvent() == ComponentLifecycleEvent.STOPPED) {
                    log.info("Adding job {} ({}) to discarded", entityId, tasksKey);
                    addToDiscarded(tasksKey);
                } else if (event.getEvent() == ComponentLifecycleEvent.FAILED) {
                    log.info("Adding job {} ({}) to failed", entityId, tasksKey);
                    failed.add(tasksKey);
                }
            }
            case TENANT -> {
                if (event.getEvent() == ComponentLifecycleEvent.DELETED) {
                    deletedTenants.add(entityId.getId());
                    log.info("Adding tenant {} to deleted", entityId);
                }
            }
        }
    }

    private void processMsgs(List<TbProtoQueueMsg<TaskProto>> msgs, TbQueueConsumer<TbProtoQueueMsg<TaskProto>> consumer, QueueConfig queueConfig) throws Exception {
        for (TbProtoQueueMsg<TaskProto> msg : msgs) {
            try {
                @SuppressWarnings("unchecked")
                T task = (T) JacksonUtil.fromString(msg.getValue().getValue(), Task.class);
                if (discarded.contains(task.getKey())) {
                    log.debug("Skipping task for discarded job {}: {}", task.getJobId(), task);
                    reportTaskDiscarded(task);
                    continue;
                } else if (failed.contains(task.getKey())) {
                    log.debug("Skipping task for failed job {}: {}", task.getJobId(), task);
                    continue;
                } else if (deletedTenants.contains(task.getTenantId().getId())) {
                    log.debug("Skipping task for deleted tenant {}: {}", task.getTenantId(), task);
                    continue;
                }

                processTask(task);
            } catch (InterruptedException e) {
                throw e;
            } catch (Exception e) {
                log.error("Failed to process msg: {}", msg, e);
            }
        }
        consumer.commit();
    }

    private void processTask(T task) throws InterruptedException {
        task.setAttempt(task.getAttempt() + 1);
        log.debug("Processing task: {}", task);
        Future<R> future = null;
        try {
            long startNs = System.nanoTime();
            future = taskExecutor.submit(() -> process(task));
            R result;
            try {
                result = future.get(getTaskProcessingTimeout(), TimeUnit.MILLISECONDS);
            } catch (ExecutionException e) {
                throw e.getCause();
            } catch (TimeoutException e) {
                throw new TimeoutException("Timeout after " + getTaskProcessingTimeout() + " ms");
            }
            long timingNs = System.nanoTime() - startNs;
            log.info("Processed task in {} ms: {}", timingNs / 1000000.0, task);
            reportTaskResult(task, result);
        } catch (InterruptedException e) {
            throw e;
        } catch (Throwable e) {
            log.error("Failed to process task (attempt {}): {}", task.getAttempt(), task, e);
            if (task.getAttempt() <= task.getRetries()) {
                processTask(task);
            } else {
                reportTaskFailure(task, e);
            }
        } finally {
            if (future != null && !future.isDone()) {
                future.cancel(true);
            }
        }
    }

    public abstract R process(T task) throws Exception;

    private void reportTaskFailure(T task, Throwable error) {
        R taskResult = task.toFailed(error);
        reportTaskResult(task, taskResult);
    }

    private void reportTaskDiscarded(T task) {
        R taskResult = task.toDiscarded();
        reportTaskResult(task, taskResult);
    }

    private void reportTaskResult(T task, R result) {
        statsService.reportTaskResult(task.getTenantId(), task.getJobId(), result);
    }

    public void addToDiscarded(String tasksKey) {
        discarded.add(tasksKey);
    }

    protected <V> V wait(Future<V> future) throws Exception {
        try {
            return future.get(); // will be interrupted after task processing timeout
        } catch (InterruptedException e) {
            future.cancel(true); // interrupting the underlying task
            throw e;
        }
    }

    @PreDestroy
    public void destroy() {
        taskConsumer.stop();
        taskConsumer.awaitStop();
        taskExecutor.shutdownNow();
    }

    public abstract long getTaskProcessingTimeout();

    public abstract JobType getJobType();

}
