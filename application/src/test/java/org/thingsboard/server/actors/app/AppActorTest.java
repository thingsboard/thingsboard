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
package org.thingsboard.server.actors.app;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.actors.TbActorId;
import org.thingsboard.server.actors.TbEntityActorId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;

import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AppActorTest {

    AppActor appActor;
    TbActorCtx ctx;
    TenantId tenant1 = new TenantId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    TenantId tenant2 = new TenantId(UUID.fromString("22222222-2222-2222-2222-222222222222"));

    @Before
    public void setUp() throws Exception {
        ActorSystemContext systemContext = mock(ActorSystemContext.class);
        TenantService tenantService = mock(TenantService.class);
        when(systemContext.getTenantService()).thenReturn(tenantService);
        when(systemContext.isTenantComponentsInitEnabled()).thenReturn(false);
        when(systemContext.getServiceInfoProvider()).thenReturn(mock(TbServiceInfoProvider.class));

        appActor = (AppActor) new AppActor.ActorCreator(systemContext).createActor();
        ctx = mock(TbActorCtx.class);
        appActor.init(ctx);
        appActor.doProcess(new AppInitMsg());
    }

    @Test
    public void onPartitionChangeMsgWithAffectedTenants_onlyForwardsToThoseTenants() {
        PartitionChangeMsg msg = new PartitionChangeMsg(ServiceType.TB_RULE_ENGINE, Set.of(tenant1));

        appActor.doProcess(msg);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Predicate<TbActorId>> filterCaptor = ArgumentCaptor.forClass(Predicate.class);
        verify(ctx).broadcastToChildren(eq(msg), filterCaptor.capture(), eq(true));
        Predicate<TbActorId> filter = filterCaptor.getValue();

        assertTrue(filter.test(new TbEntityActorId(tenant1)));
        assertFalse(filter.test(new TbEntityActorId(tenant2)));
        assertFalse(filter.test(new TbEntityActorId(new DeviceId(UUID.randomUUID()))));
    }

    @Test
    public void onPartitionChangeMsgWithNullAffectedTenants_broadcastsToEveryone() {
        PartitionChangeMsg msg = new PartitionChangeMsg(ServiceType.TB_RULE_ENGINE);

        appActor.doProcess(msg);

        verify(ctx).broadcastToChildren(msg, true);
        verify(ctx, never()).broadcastToChildren(any(), any(), anyBoolean());
    }
}
