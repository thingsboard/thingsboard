/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.queue.discovery;

import lombok.Data;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.gen.transport.TransportProtos.GetQueueRoutingInfoResponseMsg;

import java.util.UUID;


@Data
public class QueueRoutingInfo {

    private final TenantId tenantId;
    private final String queueName;
    private final String queueTopic;
    private final int partitions;

    public QueueRoutingInfo(TenantId tenantId, String queueName, String queueTopic, int partitions) {
        this.tenantId = tenantId;
        this.queueName = queueName;
        this.queueTopic = queueTopic;
        this.partitions = partitions;
    }

    public QueueRoutingInfo(Queue queue) {
        this.tenantId = queue.getTenantId();
        this.queueName = queue.getName();
        this.queueTopic = queue.getTopic();
        this.partitions = queue.getPartitions();
    }

    public QueueRoutingInfo(GetQueueRoutingInfoResponseMsg routingInfo) {
        this.tenantId = new TenantId(new UUID(routingInfo.getTenantIdMSB(), routingInfo.getTenantIdLSB()));
        this.queueName = routingInfo.getQueueName();
        this.queueTopic = routingInfo.getQueueTopic();
        this.partitions = routingInfo.getPartitions();
    }
}
