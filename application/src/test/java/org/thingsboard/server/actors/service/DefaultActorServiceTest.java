// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.queue.discovery.QueueKey;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DefaultActorServiceTest {

    private final TenantId tenant1 = TenantId.fromUUID(UUID.randomUUID());
    private final TenantId tenant2 = TenantId.fromUUID(UUID.randomUUID());

    private DefaultActorService actorService;
    private TbActorRef appActor;

    @BeforeEach
    void setUp() {
        actorService = new DefaultActorService();
        appActor = mock(TbActorRef.class);
        ReflectionTestUtils.setField(actorService, "appActor", appActor);
    }

    @Test
    void isolatedTenantQueueChanged_sendsMsgForAffectedTenantsOnly() {
        actorService.onTbApplicationEvent(event(ServiceType.TB_RULE_ENGINE, tenant1));

        PartitionChangeMsg msg = captureMsg();
        assertThat(msg.getServiceType()).isEqualTo(ServiceType.TB_RULE_ENGINE);
        assertThat(msg.isForAllTenants()).isFalse();
        assertThat(msg.isAffected(tenant1)).isTrue();
        assertThat(msg.isAffected(tenant2)).isFalse();
    }

    @Test
    void sharedQueueChanged_sendsMsgForAllTenants() {
        actorService.onTbApplicationEvent(event(ServiceType.TB_RULE_ENGINE, TenantId.SYS_TENANT_ID));

        assertThat(captureMsg().isForAllTenants()).isTrue();
    }

    @Test
    void corePartitionsChanged_sendsMsgForAllTenants() {
        actorService.onTbApplicationEvent(event(ServiceType.TB_CORE, TenantId.SYS_TENANT_ID));

        PartitionChangeMsg msg = captureMsg();
        assertThat(msg.getServiceType()).isEqualTo(ServiceType.TB_CORE);
        assertThat(msg.isForAllTenants()).isTrue();
    }

    private PartitionChangeEvent event(ServiceType serviceType, TenantId tenantId) {
        QueueKey queueKey = new QueueKey(serviceType, DataConstants.MAIN_QUEUE_NAME, tenantId);
        return new PartitionChangeEvent(this, serviceType, Map.of(queueKey, Collections.emptySet()), Collections.emptyMap());
    }

    private PartitionChangeMsg captureMsg() {
        ArgumentCaptor<PartitionChangeMsg> captor = ArgumentCaptor.forClass(PartitionChangeMsg.class);
        verify(appActor).tellWithHighPriority(captor.capture());
        return captor.getValue();
    }

}
