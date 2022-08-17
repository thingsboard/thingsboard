/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.rule.engine.transform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.EmptyNodeConfiguration;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TbSplitArrayMsgNodeTest {
    final ObjectMapper mapper = new ObjectMapper();

    DeviceId deviceId;
    TbSplitArrayMsgNode node;
    EmptyNodeConfiguration config;
    TbNodeConfiguration nodeConfiguration;
    TbContext ctx;
    TbMsgCallback callback;

    @BeforeEach
    void setUp() throws TbNodeException {
        deviceId = new DeviceId(UUID.randomUUID());
        callback = mock(TbMsgCallback.class);
        ctx = mock(TbContext.class);
        config = new EmptyNodeConfiguration();
        nodeConfiguration = new TbNodeConfiguration(mapper.valueToTree(config));
        node = spy(new TbSplitArrayMsgNode());
        node.init(ctx, nodeConfiguration);
    }

    @AfterEach
    void tearDown() {
        node.destroy();
    }

    @Test
    void givenDefaultConfig_whenInit_thenOK() {
        assertThat(node.config).isEqualTo(config);
    }

    @Test
    void givenFewMsg_whenOnMsg_thenVerifyOutput() throws Exception {
        String data = "[{\"Attribute_1\":22.5,\"Attribute_2\":10.3}, {\"Attribute_1\":1,\"Attribute_2\":2}]";
        VerifyOutputMsg(data, 2);
    }

    @Test
    void givenOneMsg_whenOnMsg_thenVerifyOutput() throws Exception {
        String data = "[{\"Attribute_1\":22.5,\"Attribute_2\":10.3}]";
        VerifyOutputMsg(data, 1);
    }

    @Test
    void givenZeroMsg_whenOnMsg_thenVerifyOutput() throws Exception {
        String data = "[]";
        VerifyOutputMsg(data, 0);
    }

    @Test
    void givenNoArrayMsg_whenOnMsg_thenVerifyOutput() throws Exception {
        String data = "{\"Attribute_1\":22.5,\"Attribute_2\":10.3}";
        JsonNode dataNode = JacksonUtil.toJsonNode(data);
        TbMsg msg = getTbMsg(deviceId, dataNode.toString());
        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellSuccess(newMsgCaptor.capture());
        verify(ctx, never()).tellFailure(any(), any());

        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();

        assertThat(newMsg).isSameAs(msg);
    }

    private void VerifyOutputMsg(String data, int sizeArray) throws Exception {
        JsonNode dataNode = JacksonUtil.toJsonNode(data);
        node.onMsg(ctx, getTbMsg(deviceId, dataNode.toString()));

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(dataNode.size())).tellSuccess(newMsgCaptor.capture());
        verify(ctx, times(sizeArray)).tellSuccess(newMsgCaptor.capture());
        verify(ctx, never()).tellFailure(any(), any());
    }

    private TbMsg getTbMsg(EntityId entityId, String data) {
        Map<String, String> mdMap = Map.of(
                "country", "US",
                "city", "NY"
        );
        return TbMsg.newMsg("POST_ATTRIBUTES_REQUEST", entityId, new TbMsgMetaData(mdMap), data, callback);
    }
}