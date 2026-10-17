// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.actors.ruleChain;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleNode;

import java.util.Optional;
import java.util.UUID;

class RuleNodeActorMessageProcessorTest {

    private TenantId tenantId;
    private RuleNodeId ruleNodeId;
    private TbActorCtx selfActor;
    private ActorSystemContext systemContext;
    private RuleNode ruleNode;

    @BeforeEach
    void setUp() {
        systemContext = Mockito.mock(ActorSystemContext.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(systemContext.getComponentService().getRuleNodeInfo(Mockito.anyString()))
                .thenReturn(Optional.empty());
        tenantId = TenantId.fromUUID(UUID.randomUUID());
        ruleNodeId = new RuleNodeId(UUID.randomUUID());
        ruleNode = new RuleNode(new RuleNodeId(UUID.randomUUID()));
        ruleNode.setType("org.thingsboard.rule.engine.filter.TbMsgTypeSwitchNode");
        ruleNode.setName("Test Node");
        selfActor = Mockito.mock(TbActorCtx.class);
    }

    @Test
    void onUpdateShouldNotFailWhenRuleNodeIsDeleted() throws Exception {
        // The rule node was deleted between the CREATED and the UPDATED lifecycle messages,
        // so findRuleNodeById returns null and onUpdate must silently skip instead of failing.
        Mockito.when(systemContext.getRuleChainService().findRuleNodeById(tenantId, ruleNodeId))
                .thenReturn(ruleNode, (RuleNode) null);
        RuleNodeActorMessageProcessor processor = new RuleNodeActorMessageProcessor(tenantId, "Test Rule Chain", ruleNodeId, systemContext, selfActor);
        TbActorCtx actorCtx = Mockito.mock(TbActorCtx.class);
        Assertions.assertDoesNotThrow(() -> processor.onUpdate(actorCtx));
        // The node is looked up a second time by onUpdate, and the update itself is skipped:
        // the secret usage replacement runs only for the node resolved by the constructor.
        Mockito.verify(systemContext.getRuleChainService(), Mockito.times(2)).findRuleNodeById(tenantId, ruleNodeId);
        Mockito.verify(systemContext.getComponentService(), Mockito.times(1)).getRuleNodeInfo(Mockito.anyString());
    }

    @Test
    void constructorShouldNotFailWhenRuleNodeIsDeleted() {
        // The processor is created while the node is already gone: RuleChainActorMessageProcessor reads the node list
        // and creates the child actor, but ComponentActor.init -> createProcessor runs later on the dispatcher.
        Mockito.when(systemContext.getRuleChainService().findRuleNodeById(tenantId, ruleNodeId)).thenReturn(null);
        Assertions.assertDoesNotThrow(() -> new RuleNodeActorMessageProcessor(tenantId, "Test Rule Chain", ruleNodeId, systemContext, selfActor));
    }

}
