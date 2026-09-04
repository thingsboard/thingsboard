// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.task;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.gen.transport.TransportProtos.JobStatsMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TaskProto;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.memory.InMemoryStorage;
import org.thingsboard.server.queue.memory.InMemoryTbQueueConsumer;
import org.thingsboard.server.queue.memory.InMemoryTbQueueProducer;
import org.thingsboard.server.queue.settings.TasksQueueConfig;

@Component
@ConditionalOnExpression("'${queue.type:null}'=='in-memory'")
@RequiredArgsConstructor
public class InMemoryTaskProcessorQueueFactory implements TaskProcessorQueueFactory {

    private final InMemoryStorage storage;
    private final TasksQueueConfig tasksQueueConfig;

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TaskProto>> createTaskConsumer(JobType jobType) {
        return new InMemoryTbQueueConsumer<>(storage, jobType.getTasksTopic());
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<JobStatsMsg>> createJobStatsProducer() {
        return new InMemoryTbQueueProducer<>(storage, tasksQueueConfig.getStatsTopic());
    }

}
