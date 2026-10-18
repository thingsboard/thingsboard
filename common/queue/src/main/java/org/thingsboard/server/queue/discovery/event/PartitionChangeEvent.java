// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.discovery.event;

import lombok.Getter;
import lombok.ToString;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.TenantId;
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

    /**
     * Returns the tenants affected by this rule engine partition change, or {@code null} if every tenant may be affected.
     * <p>
     * {@code newPartitions} holds only the changed queue keys. A system tenant key stands for the shared queues,
     * whose tenants can't be enumerated, so the result is {@code null}. It is also {@code null} for other service types.
     */
    public Set<TenantId> getAffectedTenants() {
        if (serviceType != ServiceType.TB_RULE_ENGINE || newPartitions.isEmpty() ||
                newPartitions.keySet().stream().anyMatch(queueKey -> queueKey.getTenantId().isSysTenantId())) {
            return null;
        }
        return newPartitions.keySet().stream().map(QueueKey::getTenantId).collect(Collectors.toSet());
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
