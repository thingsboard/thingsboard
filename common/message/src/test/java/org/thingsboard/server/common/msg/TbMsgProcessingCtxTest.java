// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.msg;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TbMsgProcessingCtxTest {

    private final RuleChainId RULE_CHAIN_ID = new RuleChainId(UUID.fromString("b87c4123-f9f2-41a6-9a09-e3a5b6580b11"));
    private final RuleNodeId RULE_NODE_ID = new RuleNodeId(UUID.fromString("1ca5e2ef-1309-41d9-bafa-709e9df0e2a6"));

    @Test
    void givenEmptyStack_whenIsAlreadyInStack_thenReturnFalse() {
        TbMsgProcessingCtx ctx = new TbMsgProcessingCtx();

        assertThat(ctx.isAlreadyInStack(RULE_CHAIN_ID, RULE_NODE_ID)).isFalse();
    }

    @Test
    void givenStackWithDifferentEntry_whenIsAlreadyInStack_thenReturnFalse() {
        TbMsgProcessingCtx ctx = new TbMsgProcessingCtx();
        ctx.push(new RuleChainId(UUID.randomUUID()), new RuleNodeId(UUID.randomUUID()));

        assertThat(ctx.isAlreadyInStack(RULE_CHAIN_ID, RULE_NODE_ID)).isFalse();
    }

    @Test
    void givenStackWithMatchingEntry_whenIsAlreadyInStack_thenReturnTrue() {
        TbMsgProcessingCtx ctx = new TbMsgProcessingCtx();
        ctx.push(RULE_CHAIN_ID, RULE_NODE_ID);

        assertThat(ctx.isAlreadyInStack(RULE_CHAIN_ID, RULE_NODE_ID)).isTrue();
    }

    @Test
    void givenStackWithMatchingEntryAmongOthers_whenIsAlreadyInStack_thenReturnTrue() {
        TbMsgProcessingCtx ctx = new TbMsgProcessingCtx();
        ctx.push(new RuleChainId(UUID.randomUUID()), new RuleNodeId(UUID.randomUUID()));
        ctx.push(RULE_CHAIN_ID, RULE_NODE_ID);
        ctx.push(new RuleChainId(UUID.randomUUID()), new RuleNodeId(UUID.randomUUID()));

        assertThat(ctx.isAlreadyInStack(RULE_CHAIN_ID, RULE_NODE_ID)).isTrue();
    }

    @Test
    void givenStackWithSameChainButDifferentNode_whenIsAlreadyInStack_thenReturnFalse() {
        TbMsgProcessingCtx ctx = new TbMsgProcessingCtx();
        ctx.push(RULE_CHAIN_ID, new RuleNodeId(UUID.randomUUID()));

        assertThat(ctx.isAlreadyInStack(RULE_CHAIN_ID, RULE_NODE_ID)).isFalse();
    }

    @Test
    void givenStackWithSameNodeButDifferentChain_whenIsAlreadyInStack_thenReturnFalse() {
        TbMsgProcessingCtx ctx = new TbMsgProcessingCtx();
        ctx.push(new RuleChainId(UUID.randomUUID()), RULE_NODE_ID);

        assertThat(ctx.isAlreadyInStack(RULE_CHAIN_ID, RULE_NODE_ID)).isFalse();
    }

    @Test
    void givenStackWithEntryThenPopped_whenIsAlreadyInStack_thenReturnFalse() {
        TbMsgProcessingCtx ctx = new TbMsgProcessingCtx();
        ctx.push(RULE_CHAIN_ID, RULE_NODE_ID);
        ctx.pop();

        assertThat(ctx.isAlreadyInStack(RULE_CHAIN_ID, RULE_NODE_ID)).isFalse();
    }

}
