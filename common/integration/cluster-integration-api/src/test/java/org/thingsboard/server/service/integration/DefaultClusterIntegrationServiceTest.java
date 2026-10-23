// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.gen.integration.ToIntegrationExecutorNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.QueueDeleteMsg;
import org.thingsboard.server.gen.transport.TransportProtos.QueueUpdateMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.provider.TbCoreIntegrationExecutorQueueFactory;
import org.thingsboard.server.queue.settings.TbQueueIntegrationExecutorSettings;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DefaultClusterIntegrationServiceTest {

    @Mock
    private TbServiceInfoProvider serviceInfoProvider;
    @Mock
    private TbQueueIntegrationExecutorSettings integrationNotificationSettings;
    @Mock
    private TbCoreIntegrationExecutorQueueFactory queueFactory;
    @Mock
    private IntegrationManagerService integrationManagerService;
    @Mock
    private PartitionService partitionService;
    @Mock
    private TbCallback callback;

    private DefaultClusterIntegrationService service;

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        service = new DefaultClusterIntegrationService(serviceInfoProvider, integrationNotificationSettings, queueFactory,
                integrationManagerService, partitionService);
    }

    @Test
    void givenQueueUpdateNotification_whenHandled_thenQueuesAreUpdatedInPartitionService() {
        QueueUpdateMsg queueUpdateMsg = QueueUpdateMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setQueueName("Main")
                .setQueueTopic("tb_rule_engine.main")
                .setPartitions(3)
                .build();

        handle(ToIntegrationExecutorNotificationMsg.newBuilder().addQueueUpdateMsgs(queueUpdateMsg).build());

        verify(partitionService).updateQueues(List.of(queueUpdateMsg));
        verify(integrationManagerService, never()).handleComponentLifecycleMsg(any());
        verify(callback).onSuccess();
    }

    @Test
    void givenQueueDeleteNotification_whenHandled_thenQueuesAreRemovedFromPartitionService() {
        QueueDeleteMsg queueDeleteMsg = QueueDeleteMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setQueueName("Main")
                .build();

        handle(ToIntegrationExecutorNotificationMsg.newBuilder().addQueueDeleteMsgs(queueDeleteMsg).build());

        verify(partitionService).removeQueues(List.of(queueDeleteMsg));
        verify(integrationManagerService, never()).handleComponentLifecycleMsg(any());
        verify(callback).onSuccess();
    }

    @Test
    void givenTenantUpdated_whenHandled_thenTenantRoutingInfoIsEvictedAfterTenantProfileCaches() {
        handle(lifecycleNotification(tenantId, tenantId));

        // the routing info is re-read from the tenant profile cache, so it must be evicted after that cache
        InOrder inOrder = inOrder(integrationManagerService, partitionService);
        inOrder.verify(integrationManagerService).handleComponentLifecycleMsg(any());
        inOrder.verify(partitionService).evictTenantInfo(tenantId);
        verify(callback).onSuccess();
    }

    @Test
    void givenTenantProfileUpdated_whenHandled_thenRoutingInfoOfItsTenantsIsEvictedAfterTenantProfileCaches() {
        TenantProfileId tenantProfileId = new TenantProfileId(UUID.randomUUID());

        handle(lifecycleNotification(TenantId.SYS_TENANT_ID, tenantProfileId));

        InOrder inOrder = inOrder(integrationManagerService, partitionService);
        inOrder.verify(integrationManagerService).handleComponentLifecycleMsg(any());
        inOrder.verify(partitionService).evictTenantProfileInfo(tenantProfileId);
        verify(callback).onSuccess();
    }

    @Test
    void givenIntegrationUpdated_whenHandled_thenRoutingInfoIsKept() {
        handle(lifecycleNotification(tenantId, new IntegrationId(UUID.randomUUID())));

        verify(integrationManagerService).handleComponentLifecycleMsg(any());
        verifyNoInteractions(partitionService);
        verify(callback).onSuccess();
    }

    private ToIntegrationExecutorNotificationMsg lifecycleNotification(TenantId tenantId, EntityId entityId) {
        ComponentLifecycleMsg msg = new ComponentLifecycleMsg(tenantId, entityId, ComponentLifecycleEvent.UPDATED);
        return ToIntegrationExecutorNotificationMsg.newBuilder().setComponentLifecycle(ProtoUtils.toProto(msg)).build();
    }

    private void handle(ToIntegrationExecutorNotificationMsg nf) {
        UUID id = UUID.randomUUID();
        service.handleNotification(id, new TbProtoQueueMsg<>(id, nf), callback);
    }

}
