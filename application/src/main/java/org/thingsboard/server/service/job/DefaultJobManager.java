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
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobStats;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.job.Task;
import org.thingsboard.server.common.data.job.TaskResult;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.task.JobService;
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
    private final QueueConsumerManager<TbProtoQueueMsg<JobStatsMsg>> taskResultConsumer;
    private final ExecutorService consumerExecutor;

    @Value("${queue.tasks.stats.processing_interval_ms:5000}")
    private int statsProcessingInterval;

    public DefaultJobManager(JobService jobService, JobStatsService jobStatsService, TbCoreQueueFactory queueFactory, List<JobProcessor> jobProcessors) {
        this.jobService = jobService;
        this.jobStatsService = jobStatsService;
        this.jobProcessors = jobProcessors.stream().collect(Collectors.toMap(JobProcessor::getType, Function.identity()));
        this.taskProducers = Arrays.stream(JobType.values()).collect(Collectors.toMap(Function.identity(), queueFactory::createTaskProducer));
        this.consumerExecutor = Executors.newCachedThreadPool(ThingsBoardThreadFactory.forName("task-result-consumer"));
        this.taskResultConsumer = QueueConsumerManager.<TbProtoQueueMsg<JobStatsMsg>>builder()
                .name("job-stats")
                .msgPackProcessor(this::processStats)
                .pollInterval(125)
                .consumerCreator(queueFactory::createJobStatsConsumer)
                .consumerExecutor(consumerExecutor)
                .build();
    }

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void afterStartUp() {
        taskResultConsumer.subscribe();
        taskResultConsumer.launch();
    }

    @Override
    public Job submitJob(Job job) {
        job = jobService.createJob(job.getTenantId(), job);
        log.info("Submitting job: {}", job);

        int tasksCount = jobProcessors.get(job.getType()).process(job, this::submitTask);
        jobStatsService.reportAllTasksSubmitted(job.getId(), tasksCount);
        return job;
    }

    private void submitTask(Task task) {
        log.info("Submitting task: {}", task);
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
            JobId jobId = new JobId(new UUID(statsMsg.getJobIdMSB(), statsMsg.getJobIdLSB()));
            JobStats jobStats = stats.computeIfAbsent(jobId, JobStats::new);

            if (statsMsg.hasTaskResult()) {
                TaskResult taskResult = JacksonUtil.fromString(statsMsg.getTaskResult().getValue(), TaskResult.class);
                jobStats.getTaskResults().add(taskResult);
            }
            if (statsMsg.hasTotalTasksCount()) {
                jobStats.setTotalTasksCount(statsMsg.getTotalTasksCount());
            }
        }

        stats.forEach((jobId, jobStats) -> {
            try {
                log.info("[{}] Processing job stats: {}", jobId, stats);
                jobService.processStats(jobId, jobStats);
            } catch (Exception e) {
                log.warn("Failed to process job stats for {}: {}", jobId, jobStats, e);
            }
        });
        consumer.commit();

        Thread.sleep(statsProcessingInterval);
    }

    @PreDestroy
    private void destroy() {
        taskResultConsumer.stop();
        consumerExecutor.shutdownNow();
    }

}
