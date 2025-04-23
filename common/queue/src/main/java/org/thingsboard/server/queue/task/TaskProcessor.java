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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.job.Task;
import org.thingsboard.server.common.data.job.TaskResult;
import org.thingsboard.server.common.data.job.TaskResult.TaskFailure;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos.TaskProto;
import org.thingsboard.server.gen.transport.TransportProtos.TaskResultProto;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.common.consumer.QueueConsumerManager;
import org.thingsboard.server.queue.provider.TaskProcessorQueueFactory;
import org.thingsboard.server.queue.util.AfterStartUp;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public abstract class TaskProcessor<T extends Task> {

    @Autowired
    private TaskProcessorQueueFactory queueFactory;

    private QueueConsumerManager<TbProtoQueueMsg<TaskProto>> taskConsumer;
    private TbQueueProducer<TbProtoQueueMsg<TaskResultProto>> taskResultProducer;
    private ExecutorService consumerExecutor;

    @PostConstruct
    public void init() {
        consumerExecutor = Executors.newCachedThreadPool(ThingsBoardThreadFactory.forName(getJobType().name().toLowerCase() + "-task-consumer"));
        taskConsumer = QueueConsumerManager.<TbProtoQueueMsg<TaskProto>>builder() // fixme: should be consumer per partition
                .name(getJobType().name() + "-tasks")
                .msgPackProcessor(this::processMsgs)
                .pollInterval(125)
                .consumerCreator(() -> queueFactory.createTaskConsumer(getJobType()))
                .consumerExecutor(consumerExecutor)
                .build();
        taskResultProducer = queueFactory.createTaskResultProducer();
    }

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void afterStartUp() {
        taskConsumer.subscribe();
        taskConsumer.launch();
    }

    @PreDestroy
    public void destroy() {
        taskConsumer.stop();
        consumerExecutor.shutdownNow();
    }

    private void processMsgs(List<TbProtoQueueMsg<TaskProto>> msgs, TbQueueConsumer<TbProtoQueueMsg<TaskProto>> consumer) {
        for (TbProtoQueueMsg<TaskProto> msg : msgs) {
            TaskProto taskProto = msg.getValue();
            Task task = JacksonUtil.fromString(taskProto.getValue(), Task.class);
            processTask((T) task);
        }
        consumer.commit();
    }

    private void processTask(T task) {
        task.setAttempt(task.getAttempt() + 1);
        log.info("Processing task: {}", task);
        try {
            process(task);
            reportSuccess(task);
        } catch (Exception e) {
            log.error("Failed to process task (attempt {}): {}", task.getAttempt(), task, e);
            if (task.getAttempt() < 3) {
                processTask(task);
            } else {
                reportFailure(task, e);
            }
        }
    }

    private void reportSuccess(Task task) {
        TaskResult result = TaskResult.builder()
                .tenantId(task.getTenantId())
                .jobId(task.getJobId())
                .success(true)
                .build();
        reportResult(result);
    }

    private void reportFailure(Task task, Throwable error) {
        TaskResult result = TaskResult.builder()
                .tenantId(task.getTenantId())
                .jobId(task.getJobId())
                .failure(TaskFailure.builder()
                        .error(error.getMessage())
                        .task(task)
                        .build())
                .build();
        reportResult(result);
    }

    private void reportResult(TaskResult result) {
        log.info("Reporting result: {}", result);
        TaskResultProto resultProto = TaskResultProto.newBuilder()
                .setValue(JacksonUtil.toString(result))
                .build();
        TbProtoQueueMsg<TaskResultProto> msg = new TbProtoQueueMsg<>(result.getJobId().getId(), resultProto);
        taskResultProducer.send(TopicPartitionInfo.builder()
                .topic(taskResultProducer.getDefaultTopic())
                .build(), msg, TbQueueCallback.EMPTY);
    }

    protected abstract void process(T task) throws Exception;

    public abstract JobType getJobType();

}
