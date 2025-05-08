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
package org.thingsboard.server.service.job;

import jakarta.annotation.PreDestroy;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.rule.engine.api.NotificationCenter;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobResult;
import org.thingsboard.server.common.data.job.JobStats;
import org.thingsboard.server.common.data.job.JobStatus;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.job.task.Task;
import org.thingsboard.server.common.data.job.task.TaskResult;
import org.thingsboard.server.common.data.notification.info.GeneralNotificationInfo;
import org.thingsboard.server.common.data.notification.targets.platform.TenantAdministratorsFilter;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.job.JobService;
import org.thingsboard.server.dao.notification.DefaultNotifications;
import org.thingsboard.server.gen.transport.TransportProtos.JobStatsMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TaskProto;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.common.consumer.QueueConsumerManager;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.provider.TbCoreQueueFactory;
import org.thingsboard.server.queue.task.JobStatsService;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

@TbCoreComponent
@Component
@Slf4j
public class DefaultJobManager implements JobManager {

    private final JobService jobService;
    private final JobStatsService jobStatsService;
    private final NotificationCenter notificationCenter;
    private final PartitionService partitionService;
    private final Map<JobType, JobProcessor> jobProcessors;
    private final Map<JobType, TbQueueProducer<TbProtoQueueMsg<TaskProto>>> taskProducers;
    private final QueueConsumerManager<TbProtoQueueMsg<JobStatsMsg>> jobStatsConsumer;
    private final ExecutorService executor;
    private final ExecutorService consumerExecutor;

    @Value("${queue.tasks.partitioning_strategy:tenant}")
    private String tasksPartitioningStrategy;
    @Value("${queue.tasks.stats.processing_interval_ms:1000}")
    private int statsProcessingInterval;

    public DefaultJobManager(JobService jobService, JobStatsService jobStatsService, NotificationCenter notificationCenter,
                             PartitionService partitionService, TbCoreQueueFactory queueFactory, List<JobProcessor> jobProcessors) {
        this.jobService = jobService;
        this.jobStatsService = jobStatsService;
        this.notificationCenter = notificationCenter;
        this.partitionService = partitionService;
        this.jobProcessors = jobProcessors.stream().collect(Collectors.toMap(JobProcessor::getType, Function.identity()));
        this.taskProducers = Arrays.stream(JobType.values()).collect(Collectors.toMap(Function.identity(), queueFactory::createTaskProducer));
        this.executor = ThingsBoardExecutors.newWorkStealingPool(Math.max(4, Runtime.getRuntime().availableProcessors()), getClass());
        this.consumerExecutor = Executors.newCachedThreadPool(ThingsBoardThreadFactory.forName("job-stats-consumer"));
        this.jobStatsConsumer = QueueConsumerManager.<TbProtoQueueMsg<JobStatsMsg>>builder()
                .name("job-stats")
                .msgPackProcessor(this::processStats)
                .pollInterval(125)
                .consumerCreator(queueFactory::createJobStatsConsumer)
                .consumerExecutor(consumerExecutor)
                .build();
    }

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void afterStartUp() {
        jobStatsConsumer.subscribe();
        jobStatsConsumer.launch();
    }

    @Override
    public Job submitJob(Job job) {
        log.debug("Submitting job: {}", job);
        return jobService.submitJob(job.getTenantId(), job);
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
                        if (status == JobStatus.COMPLETED) {
                            getJobProcessor(job.getType()).onJobCompleted(job);
                        }
                        sendJobFinishedNotification(job);
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
            throw new IllegalArgumentException("Reprocessing not allowed since job has general error");
        }
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

        jobService.submitJob(tenantId, job);
    }

    private void submitTask(Task<?> task) {
        log.debug("[{}][{}] Submitting task: {}", task.getTenantId(), task.getJobId(), task);
        TaskProto taskProto = TaskProto.newBuilder()
                .setValue(JacksonUtil.toString(task))
                .build();

        TbQueueProducer<TbProtoQueueMsg<TaskProto>> producer = taskProducers.get(task.getJobType());
        EntityId entityId = null;
        if (tasksPartitioningStrategy.equals("entity")) {
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

    @SneakyThrows
    private void processStats(List<TbProtoQueueMsg<JobStatsMsg>> msgs, TbQueueConsumer<TbProtoQueueMsg<JobStatsMsg>> consumer) {
        Map<JobId, JobStats> stats = new HashMap<>();

        for (TbProtoQueueMsg<JobStatsMsg> msg : msgs) {
            JobStatsMsg statsMsg = msg.getValue();
            TenantId tenantId = TenantId.fromUUID(new UUID(statsMsg.getTenantIdMSB(), statsMsg.getTenantIdLSB()));
            JobId jobId = new JobId(new UUID(statsMsg.getJobIdMSB(), statsMsg.getJobIdLSB()));
            JobStats jobStats = stats.computeIfAbsent(jobId, __ -> new JobStats(tenantId, jobId));

            if (statsMsg.hasTaskResult()) {
                TaskResult taskResult = JacksonUtil.fromString(statsMsg.getTaskResult().getValue(), TaskResult.class);
                jobStats.getTaskResults().add(taskResult);
            }
            if (statsMsg.hasTotalTasksCount()) {
                jobStats.setTotalTasksCount(statsMsg.getTotalTasksCount());
            }
        }

        stats.forEach((jobId, jobStats) -> {
            TenantId tenantId = jobStats.getTenantId();
            try {
                log.debug("[{}][{}] Processing job stats: {}", tenantId, jobId, stats);
                jobService.processStats(tenantId, jobId, jobStats);
            } catch (Exception e) {
                log.error("[{}][{}] Failed to process job stats: {}", tenantId, jobId, jobStats, e);
            }
        });
        consumer.commit();

        Thread.sleep(statsProcessingInterval);
    }

    private void sendJobFinishedNotification(Job job) {
        NotificationTemplate template = DefaultNotifications.DefaultNotification.builder()
                .name("Job finished")
                .subject("${type} task ${status}")
                .text("${description} ${status}: ${result}")
                .build().toTemplate();
        GeneralNotificationInfo info = new GeneralNotificationInfo(Map.of(
                "type", job.getType().getTitle(),
                "description", job.getDescription(),
                "status", job.getStatus().name().toLowerCase(),
                "result", job.getResult().getDescription()
        ));
        // todo: button to see details (forward to jobs page)
        notificationCenter.sendGeneralWebNotification(job.getTenantId(), new TenantAdministratorsFilter(), template, info);
    }

    private JobProcessor getJobProcessor(JobType jobType) {
        return jobProcessors.get(jobType);
    }

    @PreDestroy
    private void destroy() {
        jobStatsConsumer.stop();
        executor.shutdownNow();
        consumerExecutor.shutdownNow();
    }

}
