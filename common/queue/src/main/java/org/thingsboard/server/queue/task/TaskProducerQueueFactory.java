// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.task;

import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.gen.transport.TransportProtos.TaskProto;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

public interface TaskProducerQueueFactory {

    TbQueueProducer<TbProtoQueueMsg<TaskProto>> createTaskProducer(JobType jobType);

}
