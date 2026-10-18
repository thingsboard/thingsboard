// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors.app;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.actors.AbstractTbActor;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.DefaultTbActorSystem;
import org.thingsboard.server.actors.TbActor;
import org.thingsboard.server.actors.TbActorCreator;
import org.thingsboard.server.actors.TbActorId;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.actors.TbActorSystem;
import org.thingsboard.server.actors.TbActorSystemSettings;
import org.thingsboard.server.actors.TbEntityActorId;
import org.thingsboard.server.actors.TbStringActorId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppActorTest {

    private static final String DISPATCHER = "app-actor-test-dispatcher";

    private final TenantId tenant1 = TenantId.fromUUID(UUID.randomUUID());
    private final TenantId tenant2 = TenantId.fromUUID(UUID.randomUUID());

    private ExecutorService executor;
    private TbActorSystem system;
    private TbActorRef appActor;
    private List<TbActorMsg> tenant1Msgs;
    private List<TbActorMsg> tenant2Msgs;
    private List<TbActorMsg> otherChildMsgs;

    @BeforeEach
    void setUp() {
        ActorSystemContext systemContext = mock(ActorSystemContext.class);
        when(systemContext.getTenantService()).thenReturn(mock(TenantService.class));
        when(systemContext.getServiceInfoProvider()).thenReturn(mock(TbServiceInfoProvider.class));
        when(systemContext.isTenantComponentsInitEnabled()).thenReturn(false);

        executor = Executors.newSingleThreadExecutor();
        system = new DefaultTbActorSystem(new TbActorSystemSettings(5, 1, 42));
        system.createDispatcher(DISPATCHER, executor);
        appActor = system.createRootActor(DISPATCHER, new AppActor.ActorCreator(systemContext));
        appActor.tellWithHighPriority(new AppInitMsg());

        tenant1Msgs = createRecordingChild(new TbEntityActorId(tenant1));
        tenant2Msgs = createRecordingChild(new TbEntityActorId(tenant2));
        otherChildMsgs = createRecordingChild(new TbStringActorId("other"));
    }

    @AfterEach
    void tearDown() {
        system.stop();
        executor.shutdownNow();
    }

    @Test
    void msgForTenants_isDeliveredToAffectedTenantActorsOnly() {
        PartitionChangeMsg msg = PartitionChangeMsg.forTenants(ServiceType.TB_RULE_ENGINE, Set.of(tenant1));
        appActor.tellWithHighPriority(msg);
        // processed by the app actor after msg, so it reaches every child after msg would have
        PartitionChangeMsg marker = PartitionChangeMsg.forAllTenants(ServiceType.TB_RULE_ENGINE);
        appActor.tellWithHighPriority(marker);

        await().atMost(10, TimeUnit.SECONDS).until(() -> tenant2Msgs.contains(marker) && otherChildMsgs.contains(marker));
        assertThat(tenant1Msgs).containsExactly(msg, marker);
        assertThat(tenant2Msgs).containsExactly(marker);
        assertThat(otherChildMsgs).containsExactly(marker);
    }

    @Test
    void msgForAllTenants_isDeliveredToEveryChild() {
        PartitionChangeMsg msg = PartitionChangeMsg.forAllTenants(ServiceType.TB_RULE_ENGINE);
        appActor.tellWithHighPriority(msg);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(tenant1Msgs).containsExactly(msg);
            assertThat(tenant2Msgs).containsExactly(msg);
            assertThat(otherChildMsgs).containsExactly(msg);
        });
    }

    private List<TbActorMsg> createRecordingChild(TbActorId actorId) {
        List<TbActorMsg> received = new CopyOnWriteArrayList<>();
        system.createChildActor(DISPATCHER, new TbActorCreator() {
            @Override
            public TbActorId createActorId() {
                return actorId;
            }

            @Override
            public TbActor createActor() {
                return new AbstractTbActor() {
                    @Override
                    public boolean process(TbActorMsg msg) {
                        received.add(msg);
                        return true;
                    }
                };
            }
        }, appActor.getActorId());
        return received;
    }

}
