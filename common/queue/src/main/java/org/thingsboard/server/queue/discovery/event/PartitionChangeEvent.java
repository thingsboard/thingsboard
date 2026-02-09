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
package org.thingsboard.server.queue.discovery.event;

import lombok.Getter;
import lombok.ToString;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.discovery.QueueKey;

import java.io.Serial;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ToString(callSuper = true)
public class PartitionChangeEvent extends TbApplicationEvent {

    @Serial
    private static final long serialVersionUID = -8731788167026510559L;

    @Getter
    private final ServiceType serviceType;
    @Getter
    private final Map<QueueKey, Set<TopicPartitionInfo>> newPartitions;
    @Getter
    private final Map<QueueKey, Set<TopicPartitionInfo>> oldPartitions;

    public PartitionChangeEvent(Object source, ServiceType serviceType,
                                Map<QueueKey, Set<TopicPartitionInfo>> newPartitions,
                                Map<QueueKey, Set<TopicPartitionInfo>> oldPartitions) {
        super(source);
        this.serviceType = serviceType;
        this.newPartitions = newPartitions;
        this.oldPartitions = oldPartitions;
    }

    public Set<TopicPartitionInfo> getCorePartitions() {
        return getPartitionsByServiceTypeAndQueueName(ServiceType.TB_CORE, DataConstants.MAIN_QUEUE_NAME);
    }

    public Set<TopicPartitionInfo> getEdgePartitions() {
        return getPartitionsByServiceTypeAndQueueName(ServiceType.TB_CORE, DataConstants.EDGE_QUEUE_NAME);
    }

    public Set<TopicPartitionInfo> getPartitions() {
        return newPartitions.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
    }

    public Set<TopicPartitionInfo> getCfPartitions() {
        return getPartitionsByServiceTypeAndQueueName(ServiceType.TB_RULE_ENGINE, DataConstants.CF_QUEUE_NAME);
    }

    public Set<TopicPartitionInfo> getPartitionsByServiceTypeAndQueueName(ServiceType serviceType, String queueName) {
        return newPartitions.entrySet()
                .stream()
                .filter(entry -> serviceType.equals(entry.getKey().getType()) && queueName.equals(entry.getKey().getQueueName()))
                .flatMap(entry -> entry.getValue().stream())
                .collect(Collectors.toSet());
    }

}
