// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
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

    private ActorSystemContext systemContext;
    private RuleNodeActorMessageProcessor processor;

    @BeforeEach
    void setUp() {
        systemContext = Mockito.mock(ActorSystemContext.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(systemContext.getComponentService().getRuleNodeInfo(Mockito.anyString()))
                .thenReturn(Optional.empty());

        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        RuleNodeId ruleNodeId = new RuleNodeId(UUID.randomUUID());

        RuleNode ruleNode = new RuleNode(new RuleNodeId(UUID.randomUUID()));
        ruleNode.setType("org.thingsboard.rule.engine.filter.TbMsgTypeSwitchNode");
        ruleNode.setName("Test Node");

        Mockito.when(systemContext.getRuleChainService().findRuleNodeById(tenantId, ruleNodeId))
                .thenReturn(ruleNode, (RuleNode) null);

        TbActorCtx selfActor = Mockito.mock(TbActorCtx.class);
        processor = new RuleNodeActorMessageProcessor(tenantId, "Test Rule Chain", ruleNodeId, systemContext, selfActor);
    }

    @Test
    void onUpdateShouldNotFailWhenRuleNodeIsDeleted() throws Exception {
        // The rule node was deleted between the CREATED and the UPDATED lifecycle messages,
        // so findRuleNodeById returns null and onUpdate must silently skip instead of failing.
        TbActorCtx actorCtx = Mockito.mock(TbActorCtx.class);
        Assertions.assertDoesNotThrow(() -> processor.onUpdate(actorCtx));
    }
}
