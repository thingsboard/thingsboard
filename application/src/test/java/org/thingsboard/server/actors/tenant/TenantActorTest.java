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
package org.thingsboard.server.actors.tenant;

import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.msg.rule.engine.DeviceDeleteMsg;
import org.thingsboard.server.dao.tenant.TenantService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TenantActorTest {

    TenantActor tenantActor;
    TbActorCtx ctx;
    ActorSystemContext systemContext;
    TenantId tenantId = TenantId.SYS_TENANT_ID;
    DeviceId deviceId = DeviceId.fromString("78bf9b26-74ef-4af2-9cfb-ad6cf24ad2ec");

    @Before
    public void setUp() throws Exception {
        systemContext = mock(ActorSystemContext.class);
        ctx = mock(TbActorCtx.class);
        tenantActor = (TenantActor) new TenantActor.ActorCreator(systemContext, tenantId).createActor();
        when(systemContext.getTenantService()).thenReturn(mock(TenantService.class));
        tenantActor.init(ctx);
        tenantActor.cantFindTenant = false;
    }

    @Test
    public void deleteDeviceTest() {
        TbActorRef deviceActorRef = mock(TbActorRef.class);
        when(systemContext.resolve(ServiceType.TB_CORE, tenantId, deviceId)).thenReturn(new TopicPartitionInfo("Main", tenantId, 0,true));
        when(ctx.getOrCreateChildActor(any(), any(), any(), any())).thenReturn(deviceActorRef);
        ComponentLifecycleMsg componentLifecycleMsg = new ComponentLifecycleMsg(tenantId, deviceId, ComponentLifecycleEvent.DELETED);
        tenantActor.doProcess(componentLifecycleMsg);
        verify(deviceActorRef).tellWithHighPriority(eq(new DeviceDeleteMsg(tenantId, deviceId)));

        reset(ctx, deviceActorRef);
        when(systemContext.resolve(ServiceType.TB_CORE, tenantId, deviceId)).thenReturn(new TopicPartitionInfo("Main", tenantId, 1,false));
        tenantActor.doProcess(componentLifecycleMsg);
        verify(ctx, never()).getOrCreateChildActor(any(), any(), any(), any());
        verify(deviceActorRef, never()).tellWithHighPriority(any());
    }

}