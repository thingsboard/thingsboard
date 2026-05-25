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
    void givenEmptyStack_whenCountOccurrences_thenReturnZero() {
        TbMsgProcessingCtx ctx = new TbMsgProcessingCtx();

        assertThat(ctx.countOccurrences(RULE_CHAIN_ID, RULE_NODE_ID)).isZero();
    }

    @Test
    void givenStackWithoutMatch_whenCountOccurrences_thenReturnZero() {
        TbMsgProcessingCtx ctx = new TbMsgProcessingCtx();
        ctx.push(new RuleChainId(UUID.randomUUID()), new RuleNodeId(UUID.randomUUID()));
        ctx.push(new RuleChainId(UUID.randomUUID()), new RuleNodeId(UUID.randomUUID()));

        assertThat(ctx.countOccurrences(RULE_CHAIN_ID, RULE_NODE_ID)).isZero();
    }

    @Test
    void givenStackWithSingleMatch_whenCountOccurrences_thenReturnOne() {
        TbMsgProcessingCtx ctx = new TbMsgProcessingCtx();
        ctx.push(new RuleChainId(UUID.randomUUID()), new RuleNodeId(UUID.randomUUID()));
        ctx.push(RULE_CHAIN_ID, RULE_NODE_ID);
        ctx.push(new RuleChainId(UUID.randomUUID()), new RuleNodeId(UUID.randomUUID()));

        assertThat(ctx.countOccurrences(RULE_CHAIN_ID, RULE_NODE_ID)).isEqualTo(1);
    }

    @Test
    void givenStackWithThreeMatches_whenCountOccurrences_thenReturnThree() {
        TbMsgProcessingCtx ctx = new TbMsgProcessingCtx();
        ctx.push(RULE_CHAIN_ID, RULE_NODE_ID);
        ctx.push(new RuleChainId(UUID.randomUUID()), new RuleNodeId(UUID.randomUUID()));
        ctx.push(RULE_CHAIN_ID, RULE_NODE_ID);
        ctx.push(RULE_CHAIN_ID, RULE_NODE_ID);

        assertThat(ctx.countOccurrences(RULE_CHAIN_ID, RULE_NODE_ID)).isEqualTo(3);
    }

    @Test
    void givenStackWithMatchThenPopped_whenCountOccurrences_thenReturnZero() {
        TbMsgProcessingCtx ctx = new TbMsgProcessingCtx();
        ctx.push(RULE_CHAIN_ID, RULE_NODE_ID);
        ctx.pop();

        assertThat(ctx.countOccurrences(RULE_CHAIN_ID, RULE_NODE_ID)).isZero();
    }

    @Test
    void givenStackWithSameChainButDifferentNode_whenCountOccurrences_thenReturnZero() {
        TbMsgProcessingCtx ctx = new TbMsgProcessingCtx();
        ctx.push(RULE_CHAIN_ID, new RuleNodeId(UUID.randomUUID()));

        assertThat(ctx.countOccurrences(RULE_CHAIN_ID, RULE_NODE_ID)).isZero();
    }

    @Test
    void givenStackWithSameNodeButDifferentChain_whenCountOccurrences_thenReturnZero() {
        TbMsgProcessingCtx ctx = new TbMsgProcessingCtx();
        ctx.push(new RuleChainId(UUID.randomUUID()), RULE_NODE_ID);

        assertThat(ctx.countOccurrences(RULE_CHAIN_ID, RULE_NODE_ID)).isZero();
    }

}
