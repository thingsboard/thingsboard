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
package org.thingsboard.server.actors.service;

import org.junit.Test;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.discovery.QueueKey;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DefaultActorServiceTest {

    @Test
    public void resolveAffectedTenants_onlyTenantSpecificQueueKeysChanged_returnsThoseTenants() {
        TenantId tenant1 = new TenantId(UUID.randomUUID());
        TenantId tenant2 = new TenantId(UUID.randomUUID());
        QueueKey mainQueueTenant1 = new QueueKey(ServiceType.TB_RULE_ENGINE, "Main", tenant1);
        QueueKey highPriorityQueueTenant1 = new QueueKey(ServiceType.TB_RULE_ENGINE, "HighPriority", tenant1);
        QueueKey mainQueueTenant2 = new QueueKey(ServiceType.TB_RULE_ENGINE, "Main", tenant2);

        Map<QueueKey, Set<TopicPartitionInfo>> newPartitions = Map.of(
                mainQueueTenant1, Collections.emptySet(),
                highPriorityQueueTenant1, Collections.emptySet(),
                mainQueueTenant2, Collections.emptySet());
        PartitionChangeEvent event = new PartitionChangeEvent(this, ServiceType.TB_RULE_ENGINE, newPartitions, Map.of());

        Set<TenantId> affected = DefaultActorService.resolveAffectedTenants(event);

        assertEquals(Set.of(tenant1, tenant2), affected);
    }

    @Test
    public void resolveAffectedTenants_sysTenantQueueKeyChanged_returnsNull() {
        QueueKey sharedMainQueue = new QueueKey(ServiceType.TB_RULE_ENGINE, "Main", TenantId.SYS_TENANT_ID);
        Map<QueueKey, Set<TopicPartitionInfo>> newPartitions = Map.of(sharedMainQueue, Collections.emptySet());
        PartitionChangeEvent event = new PartitionChangeEvent(this, ServiceType.TB_RULE_ENGINE, newPartitions, Map.of());

        Set<TenantId> affected = DefaultActorService.resolveAffectedTenants(event);

        assertNull(affected);
    }
}
