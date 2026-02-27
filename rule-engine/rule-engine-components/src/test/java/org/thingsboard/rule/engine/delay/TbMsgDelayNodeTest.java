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
package org.thingsboard.rule.engine.delay;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.TbMsgProcessingCtx;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TbMsgDelayNodeTest {

    final DeviceId deviceId = new DeviceId(UUID.fromString("5770153d-6ca2-4447-8a54-5d8a4538e052"));
    final RuleNodeId ruleNodeId = new RuleNodeId(UUID.fromString("ee682a85-7f5a-4182-91bc-46e555138fe2"));

    TbMsgDelayNode node;

    @Mock
    TbContext ctxMock;

    @BeforeEach
    void setUp() throws TbNodeException {
        node = new TbMsgDelayNode();
        var config = new TbMsgDelayNodeConfiguration().defaultConfiguration();
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        lenient().when(ctxMock.getSelfId()).thenReturn(ruleNodeId);
    }

    @Test
    void shouldPreserveRuleNodeCounterAndResetCallbackWhenEnqueuingDelayedMsg() {
        // GIVEN
        int ruleNodeExecCounter = 5;
        var originalMsg = TbMsg.newMsg()
                .id(UUID.randomUUID())
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(deviceId)
                .metaData(TbMsgMetaData.EMPTY)
                .data("{\"temperature\":42}")
                .ctx(new TbMsgProcessingCtx(ruleNodeExecCounter))
                .build();

        String originalMsgId = originalMsg.getId().toString();
        var tickMsg = TbMsg.newMsg()
                .type(TbMsgType.DELAY_TIMEOUT_SELF_MSG)
                .originator(ruleNodeId)
                .metaData(TbMsgMetaData.EMPTY)
                .data(originalMsgId)
                .build();
        given(ctxMock.newMsg(null, TbMsgType.DELAY_TIMEOUT_SELF_MSG, ruleNodeId, null, TbMsgMetaData.EMPTY, originalMsgId)).willReturn(tickMsg);

        node.onMsg(ctxMock, originalMsg);

        // WHEN
        node.onMsg(ctxMock, tickMsg);

        // THEN
        var msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should().enqueueForTellNext(msgCaptor.capture(), eq(TbNodeConnectionType.SUCCESS));

        var enqueuedMsg = msgCaptor.getValue();
        assertThat(enqueuedMsg).usingRecursiveComparison()
                .ignoringFields("id", "ts", "callback")
                .isEqualTo(originalMsg);

        assertThat(enqueuedMsg.getId()).isNotNull().isNotEqualTo(originalMsg.getId());
        assertThat(enqueuedMsg.getAndIncrementRuleNodeCounter()).isEqualTo(ruleNodeExecCounter);
        assertThat(enqueuedMsg.getCallback()).isSameAs(TbMsgCallback.EMPTY);
    }

}
