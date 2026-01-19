/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.job;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.rule.engine.api.JobManager;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobResult;
import org.thingsboard.server.common.data.job.JobStatus;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.job.task.Task;
import org.thingsboard.server.common.data.job.task.TaskResult;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.job.JobService;
import org.thingsboard.server.gen.transport.TransportProtos.TaskProto;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.settings.TasksQueueConfig;
import org.thingsboard.server.queue.task.JobStatsService;
import org.thingsboard.server.queue.task.TaskProducerQueueFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class DefaultJobManager implements JobManager {

    private final JobService jobService;
    private final JobStatsService jobStatsService;
    private final PartitionService partitionService;
    private final TasksQueueConfig queueConfig;
    private final Map<JobType, JobProcessor> jobProcessors;
    private final Map<JobType, TbQueueProducer<TbProtoQueueMsg<TaskProto>>> taskProducers;
    private final ExecutorService executor;

    public DefaultJobManager(JobService jobService, JobStatsService jobStatsService, PartitionService partitionService,
                             TaskProducerQueueFactory queueFactory, TasksQueueConfig queueConfig,
                             List<JobProcessor> jobProcessors) {
        this.jobService = jobService;
        this.jobStatsService = jobStatsService;
        this.partitionService = partitionService;
        this.queueConfig = queueConfig;
        this.jobProcessors = jobProcessors.stream().collect(Collectors.toMap(JobProcessor::getType, Function.identity()));
        this.taskProducers = Arrays.stream(JobType.values()).collect(Collectors.toMap(Function.identity(), queueFactory::createTaskProducer));
        this.executor = ThingsBoardExecutors.newWorkStealingPool(Math.max(4, Runtime.getRuntime().availableProcessors()), getClass());
    }

    @Override
    public ListenableFuture<Job> submitJob(Job job) {
        log.debug("Submitting job: {}", job);
        return Futures.submit(() -> jobService.saveJob(job.getTenantId(), job), executor);
    }

    @Override
    public void onJobUpdate(Job job) {
        JobStatus status = job.getStatus();
        switch (status) {
            case PENDING -> {
                executor.execute(() -> {
                    try {
                        processJob(job);
                    } catch (Throwable e) {
                        log.error("Failed to process job update: {}", job, e);
                    }
                });
            }
            case COMPLETED, FAILED -> {
                executor.execute(() -> {
                    try {
                        getJobProcessor(job.getType()).onJobFinished(job);
                    } catch (Throwable e) {
                        log.error("Failed to process job update: {}", job, e);
                    }
                });
            }
        }
    }

    private void processJob(Job job) {
        TenantId tenantId = job.getTenantId();
        JobId jobId = job.getId();
        try {
            JobProcessor processor = getJobProcessor(job.getType());
            List<TaskResult> toReprocess = job.getConfiguration().getToReprocess();
            if (toReprocess == null) {
                int tasksCount = processor.process(job, this::submitTask);
                log.info("[{}][{}][{}] Submitted {} tasks", tenantId, jobId, job.getType(), tasksCount);
                jobStatsService.reportAllTasksSubmitted(tenantId, jobId, tasksCount);
            } else {
                processor.reprocess(job, toReprocess, this::submitTask);
                log.info("[{}][{}][{}] Submitted {} tasks for reprocessing", tenantId, jobId, job.getType(), toReprocess.size());
            }
        } catch (Throwable e) {
            log.error("[{}][{}][{}] Failed to submit tasks", tenantId, jobId, job.getType(), e);
            jobService.markAsFailed(tenantId, jobId, e.getMessage());
        }
    }

    @Override
    public void cancelJob(TenantId tenantId, JobId jobId) {
        log.info("[{}][{}] Cancelling job", tenantId, jobId);
        jobService.cancelJob(tenantId, jobId);
    }

    @Override
    public void reprocessJob(TenantId tenantId, JobId jobId) {
        log.info("[{}][{}] Reprocessing job", tenantId, jobId);
        Job job = jobService.findJobById(tenantId, jobId);
        if (job.getStatus() != JobStatus.FAILED) {
            throw new IllegalArgumentException("Job is not failed");
        }

        JobResult result = job.getResult();
        if (result.getGeneralError() != null) {
            job.presetResult();
        } else {
            List<TaskResult> taskFailures = result.getResults().stream()
                    .filter(taskResult -> !taskResult.isSuccess() && !taskResult.isDiscarded())
                    .toList();
            if (result.getFailedCount() > taskFailures.size()) {
                throw new IllegalArgumentException("Reprocessing not allowed since there are too many failures (more than " + taskFailures.size() + ")");
            }
            result.setFailedCount(0);
            result.setResults(result.getResults().stream()
                    .filter(TaskResult::isSuccess)
                    .toList());
            job.getConfiguration().setToReprocess(taskFailures);
        }
        job.getConfiguration().setTasksKey(UUID.randomUUID().toString());
        jobService.saveJob(tenantId, job);
    }

    private void submitTask(Task<?> task) {
        if (ObjectUtils.anyNull(task.getTenantId(), task.getJobId(), task.getKey())) {
            throw new IllegalArgumentException("Task " + task + " missing required fields");
        }

        log.debug("[{}][{}] Submitting task: {}", task.getTenantId(), task.getJobId(), task);
        TaskProto taskProto = TaskProto.newBuilder()
                .setValue(JacksonUtil.toString(task))
                .build();

        TbQueueProducer<TbProtoQueueMsg<TaskProto>> producer = taskProducers.get(task.getJobType());
        EntityId entityId = null;
        if (queueConfig.getPartitioningStrategy().equals("entity")) {
            entityId = task.getEntityId();
        }
        if (entityId == null) {
            entityId = task.getTenantId();
        }
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TASK_PROCESSOR, task.getJobType().name(), task.getTenantId(), entityId);
        producer.send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), taskProto), new TbQueueCallback() {
            @Override
            public void onSuccess(TbQueueMsgMetadata metadata) {
                log.trace("Submitted task to {}: {}", tpi, taskProto);
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("Failed to submit task: {}", task, t);
            }
        });
    }

    private JobProcessor getJobProcessor(JobType jobType) {
        return jobProcessors.get(jobType);
    }

    @PreDestroy
    private void destroy() {
        executor.shutdownNow();
    }

}
