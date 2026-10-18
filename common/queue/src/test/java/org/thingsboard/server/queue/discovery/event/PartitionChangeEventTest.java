// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.discovery.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.discovery.QueueKey;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class PartitionChangeEventTest {

    private final TenantId tenant1 = TenantId.fromUUID(UUID.randomUUID());
    private final TenantId tenant2 = TenantId.fromUUID(UUID.randomUUID());
    private final TenantId tenant3 = TenantId.fromUUID(UUID.randomUUID());

    @Test
    void isolatedTenantQueuesChanged_returnsTheirTenants() {
        PartitionChangeEvent event = ruleEngineEvent(Set.of(
                queueKey(DataConstants.MAIN_QUEUE_NAME, tenant1),
                queueKey(DataConstants.HP_QUEUE_NAME, tenant1),
                queueKey(DataConstants.MAIN_QUEUE_NAME, tenant2)), Collections.emptySet());

        assertThat(event.getAffectedTenants()).containsExactlyInAnyOrder(tenant1, tenant2);
    }

    @Test
    void managedStatusChangedTenantsAreIncluded() {
        PartitionChangeEvent event = ruleEngineEvent(Set.of(queueKey(DataConstants.MAIN_QUEUE_NAME, tenant1)), Set.of(tenant3));

        assertThat(event.getAffectedTenants()).containsExactlyInAnyOrder(tenant1, tenant3);
    }

    @Test
    void sharedQueueChanged_returnsNull() {
        PartitionChangeEvent event = ruleEngineEvent(Set.of(queueKey(DataConstants.MAIN_QUEUE_NAME, TenantId.SYS_TENANT_ID)), Collections.emptySet());

        assertThat(event.getAffectedTenants()).isNull();
    }

    @Test
    void sharedAndIsolatedQueuesChanged_returnsNull() {
        PartitionChangeEvent event = ruleEngineEvent(Set.of(
                queueKey(DataConstants.MAIN_QUEUE_NAME, TenantId.SYS_TENANT_ID),
                queueKey(DataConstants.MAIN_QUEUE_NAME, tenant1)), Set.of(tenant3));

        assertThat(event.getAffectedTenants()).isNull();
    }

    @Test
    void noQueuesChanged_returnsNull() {
        PartitionChangeEvent event = ruleEngineEvent(Collections.emptySet(), Set.of(tenant3));

        assertThat(event.getAffectedTenants()).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = ServiceType.class, names = "TB_RULE_ENGINE", mode = EnumSource.Mode.EXCLUDE)
    void otherServiceType_returnsNull(ServiceType serviceType) {
        QueueKey queueKey = new QueueKey(serviceType, DataConstants.MAIN_QUEUE_NAME, tenant1);
        PartitionChangeEvent event = new PartitionChangeEvent(this, serviceType,
                Map.of(queueKey, Collections.emptySet()), Collections.emptyMap(), Set.of(tenant3));

        assertThat(event.getAffectedTenants()).isNull();
    }

    private PartitionChangeEvent ruleEngineEvent(Set<QueueKey> changedQueueKeys, Set<TenantId> managedStatusChangedTenants) {
        Map<QueueKey, Set<TopicPartitionInfo>> newPartitions = changedQueueKeys.stream()
                .collect(Collectors.toMap(queueKey -> queueKey, queueKey -> Collections.emptySet()));
        return new PartitionChangeEvent(this, ServiceType.TB_RULE_ENGINE, newPartitions, Collections.emptyMap(), managedStatusChangedTenants);
    }

    private QueueKey queueKey(String queueName, TenantId tenantId) {
        return new QueueKey(ServiceType.TB_RULE_ENGINE, queueName, tenantId);
    }

}
