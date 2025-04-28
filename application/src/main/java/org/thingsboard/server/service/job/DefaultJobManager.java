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
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobStats;
import org.thingsboard.server.common.data.job.JobStatus;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.job.Task;
import org.thingsboard.server.common.data.job.TaskResult;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.job.JobService;
import org.thingsboard.server.gen.transport.TransportProtos.JobStatsMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TaskProto;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.common.consumer.QueueConsumerManager;
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
    private final Map<JobType, JobProcessor> jobProcessors;
    private final Map<JobType, TbQueueProducer<TbProtoQueueMsg<TaskProto>>> taskProducers;
    private final QueueConsumerManager<TbProtoQueueMsg<JobStatsMsg>> jobStatsConsumer;
    private final ExecutorService executor;
    private final ExecutorService consumerExecutor;

    @Value("${queue.tasks.stats.processing_interval_ms:5000}")
    private int statsProcessingInterval;

    public DefaultJobManager(JobService jobService, JobStatsService jobStatsService, TbCoreQueueFactory queueFactory, List<JobProcessor> jobProcessors) {
        this.jobService = jobService;
        this.jobStatsService = jobStatsService;
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
        return jobService.createJob(job.getTenantId(), job);
    }

    @Override
    public void onJobUpdate(Job job) {
        if (job.getStatus() == JobStatus.PENDING) {
            executor.execute(() -> {
                TenantId tenantId = job.getTenantId();
                JobId jobId = job.getId();
                try {
                    int tasksCount = jobProcessors.get(job.getType()).process(job, this::submitTask); // todo: think about stopping tb - while tasks are being submitted
                    log.info("[{}][{}][{}] Submitted {} tasks", tenantId, jobId, job.getType(), tasksCount);
                    jobStatsService.reportAllTasksSubmitted(tenantId, jobId, tasksCount);
                } catch (Throwable e) {
                    log.error("[{}][{}][{}] Failed to submit tasks", tenantId, jobId, job.getType(), e);
                    try {
                        jobService.markAsFailed(tenantId, jobId, ExceptionUtils.getStackTrace(e));
                    } catch (Throwable e2) {
                        log.error("[{}][{}] Failed to mark job as failed", tenantId, jobId, e2);
                    }
                }
            });
        }
    }

    @Override
    public void cancelJob(TenantId tenantId, JobId jobId) {
        log.info("[{}][{}] Cancelling job", tenantId, jobId);
        jobService.cancelJob(tenantId, jobId);
    }

    private void submitTask(Task task) {
        log.info("[{}][{}] Submitting task: {}", task.getTenantId(), task.getJobId(), task);
        TaskProto taskProto = TaskProto.newBuilder()
                .setValue(JacksonUtil.toString(task))
                .build();

        TbQueueProducer<TbProtoQueueMsg<TaskProto>> producer = taskProducers.get(task.getJobType());
        TbProtoQueueMsg<TaskProto> msg = new TbProtoQueueMsg<>(task.getTenantId().getId(), taskProto); // one job at a time for a given tenant
        producer.send(TopicPartitionInfo.builder().topic(producer.getDefaultTopic()).build(), msg, new TbQueueCallback() {
            @Override
            public void onSuccess(TbQueueMsgMetadata metadata) {
                log.trace("Submitted task: {}", task);
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

        Thread.sleep(statsProcessingInterval); // todo: test with bigger interval
    }

    @PreDestroy
    private void destroy() {
        jobStatsConsumer.stop();
        executor.shutdownNow();
        consumerExecutor.shutdownNow();
    }

}
