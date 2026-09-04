// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
