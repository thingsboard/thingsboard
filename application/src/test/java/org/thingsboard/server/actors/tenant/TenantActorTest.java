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
package org.thingsboard.server.actors.tenant;

import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.DefaultTbActorSystem;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.actors.TbActorMailbox;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.actors.TbActorSystem;
import org.thingsboard.server.actors.TbActorSystemSettings;
import org.thingsboard.server.actors.TbEntityActorId;
import org.thingsboard.server.actors.ruleChain.RuleChainActor;
import org.thingsboard.server.actors.ruleChain.RuleChainToRuleChainMsg;
import org.thingsboard.server.actors.service.DefaultActorService;
import org.thingsboard.server.actors.shared.RuleChainErrorActor;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.msg.rule.engine.DeviceDeleteMsg;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.actors.service.DefaultActorService.RULE_DISPATCHER_NAME;

public class TenantActorTest {

    TenantActor tenantActor;
    ActorSystemContext systemContext;
    RuleChainService ruleChainService;
    PartitionService partitionService;
    TenantId tenantId = TenantId.SYS_TENANT_ID;
    DeviceId deviceId = DeviceId.fromString("78bf9b26-74ef-4af2-9cfb-ad6cf24ad2ec");
    RuleChainId ruleChainId = new RuleChainId(UUID.fromString("48cfa2b0-3dca-11ef-8d1a-37c2894cc59c"));

    @Before
    public void setUp() throws Exception {
        systemContext = mock(ActorSystemContext.class);
        ruleChainService = mock(RuleChainService.class);
        partitionService = mock();

        TbServiceInfoProvider serviceInfoProvider = mock(TbServiceInfoProvider.class);
        TbApiUsageStateService apiUsageService = mock(TbApiUsageStateService.class);
        TenantService tenantService = mock(TenantService.class);

        when(systemContext.getRuleChainService()).thenReturn(ruleChainService);
        tenantActor = (TenantActor) new TenantActor.ActorCreator(systemContext, tenantId).createActor();

        when(tenantService.findTenantById(tenantId)).thenReturn(mock());
        when(systemContext.getTenantService()).thenReturn(tenantService);
        when(serviceInfoProvider.isService(ServiceType.TB_CORE)).thenReturn(true);
        when(serviceInfoProvider.isService(ServiceType.TB_RULE_ENGINE)).thenReturn(true);
        when(systemContext.getServiceInfoProvider()).thenReturn(serviceInfoProvider);
        when(partitionService.isManagedByCurrentService(tenantId)).thenReturn(true);
        when(systemContext.getPartitionService()).thenReturn(partitionService);
        when(systemContext.getApiUsageStateService()).thenReturn(apiUsageService);
        when(apiUsageService.getApiUsageState(tenantId)).thenReturn(new ApiUsageState());
    }

    @Test
    public void deleteDeviceTest() throws Exception {
        TbActorCtx ctx = mock(TbActorCtx.class);
        tenantActor.init(ctx);
        TbActorRef deviceActorRef = mock(TbActorRef.class);
        when(systemContext.resolve(ServiceType.TB_CORE, tenantId, deviceId)).thenReturn(new TopicPartitionInfo("Main", tenantId, 0, true));
        when(ctx.getOrCreateChildActor(any(), any(), any(), any())).thenReturn(deviceActorRef);

        ComponentLifecycleMsg componentLifecycleMsg = new ComponentLifecycleMsg(tenantId, deviceId, ComponentLifecycleEvent.DELETED);
        tenantActor.doProcess(componentLifecycleMsg);
        verify(deviceActorRef).tellWithHighPriority(eq(new DeviceDeleteMsg(tenantId, deviceId)));
        reset(ctx, deviceActorRef);

        when(systemContext.resolve(ServiceType.TB_CORE, tenantId, deviceId)).thenReturn(new TopicPartitionInfo("Main", tenantId, 1, false));
        tenantActor.doProcess(componentLifecycleMsg);
        verify(ctx, never()).getOrCreateChildActor(any(), any(), any(), any());
        verify(deviceActorRef, never()).tellWithHighPriority(any());
    }

    @Test
    public void ruleChainErrorActorTest() throws Exception {
        TbActorSystemSettings settings = new TbActorSystemSettings(0, 0, 0);
        TbActorSystem system = spy(new DefaultTbActorSystem(settings));
        system.createDispatcher(RULE_DISPATCHER_NAME, mock());
        system.createDispatcher(DefaultActorService.CF_MANAGER_DISPATCHER_NAME, mock());
        TbActorMailbox tenantCtx = new TbActorMailbox(system, settings, null, mock(), mock(), null);
        tenantActor.init(tenantCtx);

        TbMsg msg = mock(TbMsg.class);

        when(ruleChainService.findRuleChainById(tenantId, ruleChainId)).thenReturn(new RuleChain(ruleChainId));

        RuleChainToRuleChainMsg ruleChainMsg = new RuleChainToRuleChainMsg(ruleChainId, null, msg, null);
        tenantActor.doProcess(ruleChainMsg);
        verify(system).createChildActor(eq(RULE_DISPATCHER_NAME), any(RuleChainActor.ActorCreator.class), any());
        reset(system);
        tenantActor.doProcess(ruleChainMsg);
        verify(system, never()).createChildActor(any(), any(), any());

        //Delete rule-chain
        TbActorRef ruleChainActor = system.getActor(new TbEntityActorId(ruleChainId));
        assertNotNull(ruleChainActor);
        system.stop(ruleChainActor);
        when(ruleChainService.findRuleChainById(tenantId, ruleChainId)).thenReturn(null);

        tenantActor.doProcess(ruleChainMsg);
        verify(system).createChildActor(eq(RULE_DISPATCHER_NAME), any(RuleChainErrorActor.ActorCreator.class), any());
        reset(system);
        tenantActor.doProcess(ruleChainMsg);
        verify(system, never()).createChildActor(any(), any(), any());
        system.stop();
    }

}