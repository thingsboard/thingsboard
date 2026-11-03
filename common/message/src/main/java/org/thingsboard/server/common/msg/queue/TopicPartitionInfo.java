// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.msg.queue;

import lombok.Builder;
import lombok.Getter;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class TopicPartitionInfo {

    private final String topic;
    private final TenantId tenantId;
    private final Integer partition;
    @Getter
    private final boolean useInternalPartition;
    @Getter
    private final String fullTopicName;
    @Getter
    private final boolean myPartition;

    @Builder
    public TopicPartitionInfo(String topic, TenantId tenantId, Integer partition, boolean useInternalPartition, boolean myPartition) {
        this.topic = topic;
        this.tenantId = tenantId;
        this.partition = partition;
        this.useInternalPartition = useInternalPartition;
        this.myPartition = myPartition;
        String tmp = topic;
        if (tenantId != null && !tenantId.isNullUid()) {
            tmp += ".isolated." + tenantId.getId().toString();
        }
        if (partition != null && !useInternalPartition) {
            tmp += "." + partition;
        }
        this.fullTopicName = tmp;
    }

    public TopicPartitionInfo(String topic, TenantId tenantId, Integer partition, boolean myPartition) {
        this(topic, tenantId, partition, false, myPartition);
    }

    public String getTopic() {
        return topic;
    }

    public Optional<TenantId> getTenantId() {
        return Optional.ofNullable(tenantId);
    }

    public Optional<Integer> getPartition() {
        return Optional.ofNullable(partition);
    }

    public TopicPartitionInfo withTopic(String topic) {
        return new TopicPartitionInfo(topic, this.tenantId, this.partition, this.useInternalPartition, this.myPartition);
    }

    public static Set<TopicPartitionInfo> withTopic(Set<TopicPartitionInfo> partitions, String topic) {
        return partitions.stream().map(tpi -> tpi.withTopic(topic)).collect(Collectors.toSet());
    }

    public TopicPartitionInfo withUseInternalPartition(boolean useInternalPartition) {
        return new TopicPartitionInfo(this.topic, this.tenantId, this.partition, useInternalPartition, this.myPartition);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TopicPartitionInfo that = (TopicPartitionInfo) o;
        return Objects.equals(partition, that.partition) &&
                fullTopicName.equals(that.fullTopicName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullTopicName, partition);
    }

    @Override
    public String toString() {
        String str = fullTopicName;
        if (useInternalPartition) {
            str += "[" + partition + "]";
        }
        return str;
    }

}
