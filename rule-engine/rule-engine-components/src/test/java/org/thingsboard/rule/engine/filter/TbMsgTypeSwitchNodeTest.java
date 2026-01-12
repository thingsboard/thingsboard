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
package org.thingsboard.rule.engine.filter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class TbMsgTypeSwitchNodeTest {

    private static final DeviceId DEVICE_ID = new DeviceId(UUID.randomUUID());

    private TbMsgTypeSwitchNode node;

    private TbContext ctx;

    @BeforeEach
    void setUp() {
        ctx = mock(TbContext.class);
        node = new TbMsgTypeSwitchNode();
    }

    @AfterEach
    void tearDown() {
        node.destroy();
    }

    @Test
    void givenAllTypes_whenOnMsg_then_allTypesSupported() throws TbNodeException {
        // GIVEN
        List<TbMsg> tbMsgList = new ArrayList<>();
        var tbMsgTypes = TbMsgType.values();
        for (var msgType : tbMsgTypes) {
            tbMsgList.add(getTbMsg(msgType));
        }

        // WHEN
        for (TbMsg tbMsg : tbMsgList) {
            node.onMsg(ctx, tbMsg);
        }

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<String> nodeConnectionCapture = ArgumentCaptor.forClass(String.class);
        verify(ctx, times(tbMsgList.size())).tellNext(newMsgCaptor.capture(), nodeConnectionCapture.capture());
        verify(ctx, never()).tellFailure(any(), any());
        var resultMsgs = newMsgCaptor.getAllValues();
        var resultNodeConnections = nodeConnectionCapture.getAllValues();
        for (int i = 0; i < resultMsgs.size(); i++) {
            var msg = resultMsgs.get(i);
            assertThat(msg).isNotNull();
            assertThat(msg.getType()).isNotNull();
            assertThat(msg.getType()).isEqualTo(msg.getInternalType().name());
            assertThat(msg).isSameAs(tbMsgList.get(i));
            assertThat(resultNodeConnections.get(i))
                    .isEqualTo(msg.getInternalType().getRuleNodeConnection());
        }
    }

    private TbMsg getTbMsg(TbMsgType msgType) {
        return TbMsg.newMsg()
                .type(msgType)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
    }

}
