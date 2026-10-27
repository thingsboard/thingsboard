// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.task;

import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.gen.transport.TransportProtos.JobStatsMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TaskProto;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

public interface TaskProcessorQueueFactory {

    TbQueueConsumer<TbProtoQueueMsg<TaskProto>> createTaskConsumer(JobType jobType);

    TbQueueProducer<TbProtoQueueMsg<JobStatsMsg>> createJobStatsProducer();

}
