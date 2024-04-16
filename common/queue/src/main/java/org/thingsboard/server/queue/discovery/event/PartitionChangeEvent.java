/**
 * Copyright © 2016-2024 The Thingsboard Authors
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

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@ToString(callSuper = true)
public class PartitionChangeEvent extends TbApplicationEvent {

    private static final long serialVersionUID = -8731788167026510559L;

    @Getter
    private final ServiceType serviceType;
    @Getter
    private final Map<QueueKey, Set<TopicPartitionInfo>> partitionsMap;

    public PartitionChangeEvent(Object source, ServiceType serviceType, Map<QueueKey, Set<TopicPartitionInfo>> partitionsMap) {
        super(source);
        this.serviceType = serviceType;
        this.partitionsMap = partitionsMap;
    }

    public Set<TopicPartitionInfo> getCorePartitions() {
        return getPartitions(entry -> !entry.getKey().getQueueName().equals(DataConstants.EDGE_QUEUE_NAME));
    }

    public Set<TopicPartitionInfo> getEdgePartitions() {
        return getPartitions(entry -> entry.getKey().getQueueName().equals(DataConstants.EDGE_QUEUE_NAME));
    }

    private Set<TopicPartitionInfo> getPartitions(Predicate<Map.Entry<QueueKey, Set<TopicPartitionInfo>>> predicate) {
        return partitionsMap.entrySet()
                .stream()
                .filter(predicate)
                .flatMap(entry -> entry.getValue().stream())
                .collect(Collectors.toSet());
    }

}
