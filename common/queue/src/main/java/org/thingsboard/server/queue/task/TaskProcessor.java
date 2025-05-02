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
import org.thingsboard.server.queue.provider.TaskProcessorQueueFactory;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class TaskProcessor<T extends Task<R>, R extends TaskResult> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private TaskProcessorQueueFactory queueFactory;
    @Autowired
    private JobStatsService statsService;
    @Autowired
    private TaskProcessorExecutors executors;

    private QueueKey queueKey;
    private MainQueueConsumerManager<TbProtoQueueMsg<TaskProto>, QueueConfig> taskConsumer;

    private final Set<UUID> deletedTenants = ConcurrentHashMap.newKeySet();
    private final Set<UUID> discardedJobs = ConcurrentHashMap.newKeySet(); // fixme use caffeine

    @PostConstruct
    public void init() {
        queueKey = new QueueKey(ServiceType.TASK_PROCESSOR, getJobType().name());
        taskConsumer = MainQueueConsumerManager.<TbProtoQueueMsg<TaskProto>, QueueConfig>builder()
                .queueKey(queueKey)
                .config(QueueConfig.of(true, 500))
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
                if (event.getEvent() == ComponentLifecycleEvent.STOPPED) {
                    log.debug("Adding job {} to discarded", entityId);
                    addToDiscardedJobs(entityId.getId());
                }
            }
            case TENANT -> {
                if (event.getEvent() == ComponentLifecycleEvent.DELETED) {
                    deletedTenants.add(entityId.getId());
                    log.debug("Adding tenant {} to deleted", entityId);
                }
            }
        }
    }

    private void processMsgs(List<TbProtoQueueMsg<TaskProto>> msgs, TbQueueConsumer<TbProtoQueueMsg<TaskProto>> consumer, QueueConfig queueConfig) throws Exception {
        for (TbProtoQueueMsg<TaskProto> msg : msgs) {
            try {
                @SuppressWarnings("unchecked")
                T task = (T) JacksonUtil.fromString(msg.getValue().getValue(), Task.class);
                if (discardedJobs.contains(task.getJobId().getId())) {
                    log.info("Skipping task '{}' for cancelled job {}", task.getKey(), task.getJobId());
                    reportTaskDiscarded(task);
                    continue;
                } else if (deletedTenants.contains(task.getTenantId().getId())) {
                    log.info("Skipping task '{}' for deleted tenant {}", task.getKey(), task.getTenantId());
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

    private void processTask(T task) throws Exception { // todo: timeout and task interruption
        task.setAttempt(task.getAttempt() + 1);
        log.info("Processing task: {}", task);
        try {
            R result = process(task);
            reportTaskResult(task, result);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to process task (attempt {}): {}", task.getAttempt(), task, e);
            if (task.getAttempt() <= task.getRetries()) {
                processTask(task);
            } else {
                reportTaskFailure(task, e);
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

    public void addToDiscardedJobs(UUID jobId) {
        discardedJobs.add(jobId);
    }

    @PreDestroy
    public void destroy() {
        taskConsumer.stop();
        taskConsumer.awaitStop();
    }


    public abstract JobType getJobType();

}
