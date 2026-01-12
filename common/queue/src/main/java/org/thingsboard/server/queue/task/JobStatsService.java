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
package org.thingsboard.server.queue.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.task.TaskResult;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos.JobStatsMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TaskResultProto;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

@Lazy
@Service
@Slf4j
public class JobStatsService {

    private final TbQueueProducer<TbProtoQueueMsg<JobStatsMsg>> producer;

    public JobStatsService(TaskProcessorQueueFactory queueFactory) {
        this.producer = queueFactory.createJobStatsProducer();
    }

    public void reportTaskResult(TenantId tenantId, JobId jobId, TaskResult result) {
        report(tenantId, jobId, JobStatsMsg.newBuilder()
                .setTaskResult(TaskResultProto.newBuilder()
                        .setValue(JacksonUtil.toString(result))
                        .build()));
    }

    public void reportAllTasksSubmitted(TenantId tenantId, JobId jobId, int tasksCount) {
        report(tenantId, jobId, JobStatsMsg.newBuilder()
                .setTotalTasksCount(tasksCount));
    }

    private void report(TenantId tenantId, JobId jobId, JobStatsMsg.Builder statsMsg) {
        log.debug("[{}][{}] Reporting: {}", tenantId, jobId, statsMsg);
        statsMsg.setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setJobIdMSB(jobId.getId().getMostSignificantBits())
                .setJobIdLSB(jobId.getId().getLeastSignificantBits());

        // using job id as msg key so that all stats for a certain job are submitted to the same partition
        TbProtoQueueMsg<JobStatsMsg> msg = new TbProtoQueueMsg<>(jobId.getId(), statsMsg.build());
        producer.send(TopicPartitionInfo.builder().topic(producer.getDefaultTopic()).build(), msg, TbQueueCallback.EMPTY);
    }

}
