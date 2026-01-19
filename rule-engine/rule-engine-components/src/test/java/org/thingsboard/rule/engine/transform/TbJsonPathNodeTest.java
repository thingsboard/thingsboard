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
package org.thingsboard.rule.engine.transform;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.PathNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TbJsonPathNodeTest {
    DeviceId deviceId;
    TbJsonPathNode node;
    TbJsonPathNodeConfiguration config;
    TbNodeConfiguration nodeConfiguration;
    TbContext ctx;
    TbMsgCallback callback;

    @BeforeEach
    void setUp() throws TbNodeException {
        deviceId = new DeviceId(UUID.randomUUID());
        callback = mock(TbMsgCallback.class);
        ctx = mock(TbContext.class);
        config = new TbJsonPathNodeConfiguration();
        config.setJsonPath("$.Attribute_2");
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node = spy(new TbJsonPathNode());
        node.init(ctx, nodeConfiguration);
    }

    @AfterEach
    void tearDown() {
        node.destroy();
    }

    @Test
    void givenDefaultConfig_whenInit_thenFail() {
        config.setJsonPath("");
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        assertThatThrownBy(() -> node.init(ctx, nodeConfiguration)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenDefaultConfig_whenVerify_thenOK() {
        TbJsonPathNodeConfiguration defaultConfig = new TbJsonPathNodeConfiguration().defaultConfiguration();
        assertThat(defaultConfig.getJsonPath()).isEqualTo(TbJsonPathNodeConfiguration.DEFAULT_JSON_PATH);
    }

    @Test
    void givenJsonMsg_whenOnMsg_thenVerifyOutputJsonPrimitiveNode() throws Exception {
        String data = "{\"Attribute_1\":22.5,\"Attribute_2\":100}";
        VerifyOutputMsg(data, 1, 100);

        data = "{\"Attribute_1\":22.5,\"Attribute_2\":\"StringValue\"}";
        VerifyOutputMsg(data, 2, "StringValue");
    }

    @Test
    void givenJsonMsg_whenOnMsg_thenVerifyJavaPrimitiveOutput() throws Exception {
        config.setJsonPath("$.attributes.length()");
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        String data = "{\"attributes\":[{\"attribute_1\":10},{\"attribute_2\":20},{\"attribute_3\":30},{\"attribute_4\":40}]}";
        VerifyOutputMsg(data, 1, 4);

    }

    @Test
    void givenJsonArray_whenOnMsg_thenVerifyOutput() throws Exception {
        String data = "{\"Attribute_1\":22.5,\"Attribute_2\":[{\"Attribute_3\":22.5,\"Attribute_4\":10.3}, {\"Attribute_5\":22.5,\"Attribute_6\":10.3}]}";
        VerifyOutputMsg(data, 1, JacksonUtil.toJsonNode(data).get("Attribute_2"));
    }

    @Test
    void givenJsonNode_whenOnMsg_thenVerifyOutput() throws Exception {
        String data = "{\"Attribute_1\":22.5,\"Attribute_2\":{\"Attribute_3\":22.5,\"Attribute_4\":10.3}}";
        VerifyOutputMsg(data, 1, JacksonUtil.toJsonNode(data).get("Attribute_2"));
    }

    @Test
    void givenJsonArrayWithFilter_whenOnMsg_thenVerifyOutput() throws Exception {
        config.setJsonPath("$.Attribute_2[?(@.voltage > 200)]");
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        String data = "{\"Attribute_1\":22.5,\"Attribute_2\":[{\"voltage\":220}, {\"voltage\":250}, {\"voltage\":110}]}";
        VerifyOutputMsg(data, 1, JacksonUtil.toJsonNode("[{\"voltage\":220}, {\"voltage\":250}]"));
    }

    @Test
    void givenNoArrayMsg_whenOnMsg_thenTellFailure() throws Exception {
        String data = "{\"Attribute_1\":22.5,\"Attribute_5\":10.3}";
        JsonNode dataNode = JacksonUtil.toJsonNode(data);
        TbMsg msg = getTbMsg(deviceId, dataNode.toString());
        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(ctx, never()).tellSuccess(any());
        verify(ctx, times(1)).tellFailure(newMsgCaptor.capture(), exceptionCaptor.capture());

        assertThat(newMsgCaptor.getValue()).isSameAs(msg);
        assertThat(exceptionCaptor.getValue()).isInstanceOf(RuntimeException.class);
    }

    @Test
    void givenNoResultsForPath_whenOnMsg_thenTellFailure() throws Exception {
        String data = "{\"Attribute_1\":22.5,\"Attribute_5\":10.3}";
        JsonNode dataNode = JacksonUtil.toJsonNode(data);
        TbMsg msg = getTbMsg(deviceId, dataNode.toString());
        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(ctx, never()).tellSuccess(any());
        verify(ctx, times(1)).tellFailure(newMsgCaptor.capture(), exceptionCaptor.capture());

        assertThat(newMsgCaptor.getValue()).isSameAs(msg);
        assertThat(exceptionCaptor.getValue()).isInstanceOf(PathNotFoundException.class);
    }

    private void VerifyOutputMsg(String data, int countTellSuccess, Object value) throws Exception {
        JsonNode dataNode = JacksonUtil.toJsonNode(data);
        node.onMsg(ctx, getTbMsg(deviceId, dataNode.toString()));

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(countTellSuccess)).tellSuccess(newMsgCaptor.capture());
        verify(ctx, never()).tellFailure(any(), any());

        assertThat(newMsgCaptor.getValue().getData()).isEqualTo(JacksonUtil.toString(value));
    }

    private TbMsg getTbMsg(EntityId entityId, String data) {
        Map<String, String> mdMap = Map.of("country", "US",
                "city", "NY"
        );
        return TbMsg.newMsg()
                .type(TbMsgType.POST_ATTRIBUTES_REQUEST)
                .originator(entityId)
                .copyMetaData(new TbMsgMetaData(mdMap))
                .data(data)
                .callback(callback)
                .build();
    }
}
