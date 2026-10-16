// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.task;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.gen.transport.TransportProtos.TaskProto;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.memory.InMemoryStorage;
import org.thingsboard.server.queue.memory.InMemoryTbQueueProducer;

@Component
@ConditionalOnExpression("'${queue.type:null}' == 'in-memory'")
@RequiredArgsConstructor
public class InMemoryTaskProducerQueueFactory implements TaskProducerQueueFactory {

    private final InMemoryStorage storage;

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TaskProto>> createTaskProducer(JobType jobType) {
        return new InMemoryTbQueueProducer<>(storage, jobType.getTasksTopic());
    }

}
