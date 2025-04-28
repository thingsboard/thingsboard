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
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.job.Task;
import org.thingsboard.server.common.data.job.TaskResult;
import org.thingsboard.server.common.data.job.TaskResult.TaskFailure;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TaskProto;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.common.consumer.QueueConsumerManager;
import org.thingsboard.server.queue.provider.TaskProcessorQueueFactory;
import org.thingsboard.server.queue.util.AfterStartUp;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class TaskProcessor<T extends Task> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private TaskProcessorQueueFactory queueFactory;
    @Autowired
    private JobStatsService statsService;

    private QueueConsumerManager<TbProtoQueueMsg<TaskProto>> taskConsumer;
    private ExecutorService consumerExecutor;

    private final Set<UUID> deletedTenants = ConcurrentHashMap.newKeySet();
    private final Set<UUID> discardedJobs = ConcurrentHashMap.newKeySet(); // fixme use caffeine

    @PostConstruct
    public void init() {
        consumerExecutor = Executors.newCachedThreadPool(ThingsBoardThreadFactory.forName(getJobType().name().toLowerCase() + "-task-consumer"));
        taskConsumer = QueueConsumerManager.<TbProtoQueueMsg<TaskProto>>builder() // fixme: should be consumer per partition
                .name(getJobType().name().toLowerCase() + "-tasks")
                .msgPackProcessor(this::processMsgs) // todo: max.poll.records = 1
                .pollInterval(125)
                .consumerCreator(() -> queueFactory.createTaskConsumer(getJobType()))
                .consumerExecutor(consumerExecutor)
                .build();
    }

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void afterStartUp() {
        taskConsumer.subscribe();
        taskConsumer.launch();
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

    private void processMsgs(List<TbProtoQueueMsg<TaskProto>> msgs, TbQueueConsumer<TbProtoQueueMsg<TaskProto>> consumer) throws Exception {
        for (TbProtoQueueMsg<TaskProto> msg : msgs) {
            try {
                Task task = JacksonUtil.fromString(msg.getValue().getValue(), Task.class);
                if (discardedJobs.contains(task.getJobId().getId())) {
                    log.info("Skipping task '{}' for cancelled job {}", task.getKey(), task.getJobId());
                    reportCancelled(task);
                    continue;
                } else if (deletedTenants.contains(task.getTenantId().getId())) {
                    log.info("Skipping task '{}' for deleted tenant {}", task.getKey(), task.getTenantId());
                    continue;
                }
                processTask((T) task);
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
            process(task);
            reportSuccess(task);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to process task (attempt {}): {}", task.getAttempt(), task, e);
            if (task.getAttempt() <= task.getRetries()) {
                processTask(task);
            } else {
                reportFailure(task, e);
            }
        }
    }

    public abstract void process(T task) throws Exception;

    private void reportSuccess(Task task) {
        TaskResult result = TaskResult.builder()
                .success(true)
                .build();
        statsService.reportTaskResult(task.getTenantId(), task.getJobId(), result);
    }

    private void reportFailure(Task task, Throwable error) {
        TaskResult result = TaskResult.builder()
                .failure(TaskFailure.builder()
                        .error(error.getMessage())
                        .task(task)
                        .build())
                .build();
        statsService.reportTaskResult(task.getTenantId(), task.getJobId(), result);
    }

    private void reportCancelled(Task task) {
        TaskResult result = TaskResult.builder()
                .discarded(true)
                .build();
        statsService.reportTaskResult(task.getTenantId(), task.getJobId(), result);
    }

    public void addToDiscardedJobs(UUID jobId) {
        discardedJobs.add(jobId);
    }

    @PreDestroy
    public void destroy() {
        taskConsumer.stop();
        consumerExecutor.shutdownNow();
    }


    public abstract JobType getJobType();

}
