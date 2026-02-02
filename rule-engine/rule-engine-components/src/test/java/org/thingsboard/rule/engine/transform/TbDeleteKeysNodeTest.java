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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.util.TbMsgSource;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class TbDeleteKeysNodeTest {
    DeviceId deviceId;
    TbDeleteKeysNode node;
    TbDeleteKeysNodeConfiguration config;
    TbNodeConfiguration nodeConfiguration;
    TbContext ctx;
    TbMsgCallback callback;

    @BeforeEach
    void setUp() throws TbNodeException {
        deviceId = new DeviceId(UUID.randomUUID());
        callback = mock(TbMsgCallback.class);
        ctx = mock(TbContext.class);
        config = new TbDeleteKeysNodeConfiguration().defaultConfiguration();
        config.setKeys(Set.of("TestKey_1", "TestKey_2", "TestKey_3", "(\\w*)Data(\\w*)"));
        config.setDeleteFrom(TbMsgSource.METADATA);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node = spy(new TbDeleteKeysNode());
        node.init(ctx, nodeConfiguration);
    }

    @AfterEach
    void tearDown() {
        node.destroy();
    }

    @Test
    void givenDefaultConfig_whenVerify_thenOK() {
        TbDeleteKeysNodeConfiguration defaultConfig = new TbDeleteKeysNodeConfiguration().defaultConfiguration();
        assertThat(defaultConfig.getKeys()).isEqualTo(Collections.emptySet());
        assertThat(defaultConfig.getDeleteFrom()).isEqualTo(TbMsgSource.DATA);
    }

    @Test
    void givenDeleteFromMetadata_whenOnMsg_thenVerifyOutput() throws Exception {
        node.onMsg(ctx, getTbMsg(deviceId, TbMsg.EMPTY_JSON_OBJECT));

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx).tellSuccess(newMsgCaptor.capture());
        verify(ctx, never()).tellFailure(any(), any());

        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();

        Map<String, String> metaDataMap = newMsg.getMetaData().getData();
        assertThat(metaDataMap.containsKey("TestKey_1")).isEqualTo(false);
        assertThat(metaDataMap.containsKey("voltageDataValue")).isEqualTo(false);
    }

    @Test
    void givenDeleteFromMsgConfig_whenOnMsg_thenVerifyOutput() throws Exception {
        config.setDeleteFrom(TbMsgSource.DATA);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        String data = "{\"Voltage\":22.5,\"TempDataValue\":10.5}";
        node.onMsg(ctx, getTbMsg(deviceId, data));

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx).tellSuccess(newMsgCaptor.capture());
        verify(ctx, never()).tellFailure(any(), any());

        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();

        JsonNode dataNode = JacksonUtil.toJsonNode(newMsg.getData());
        assertThat(dataNode.has("TempDataValue")).isEqualTo(false);
        assertThat(dataNode.has("Voltage")).isEqualTo(true);
    }

    @Test
    void givenEmptyKeys_whenOnMsg_thenVerifyOutput() throws Exception {
        TbDeleteKeysNodeConfiguration defaultConfig = new TbDeleteKeysNodeConfiguration().defaultConfiguration();
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(defaultConfig));
        node.init(ctx, nodeConfiguration);

        String data = "{\"Voltage\":220,\"Humidity\":56}";
        node.onMsg(ctx, getTbMsg(deviceId, data));

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx).tellSuccess(newMsgCaptor.capture());
        verify(ctx, never()).tellFailure(any(), any());

        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();

        assertThat(newMsg.getData()).isEqualTo(data);
    }

    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyUpgradeResultAndConfig() {
        return Stream.of(
                Arguments.of(0, "{\"fromMetadata\":false,\"keys\":[\"temperature\"]}", true, "{\"deleteFrom\":\"DATA\",\"keys\":[\"temperature\"]}"),
                Arguments.of(0, "{\"fromMetadata\":true,\"keys\":[\"temperature\"]}", true, "{\"deleteFrom\":\"METADATA\",\"keys\":[\"temperature\"]}"),
                Arguments.of(1, "{\"dataToFetch\":\"METADATA\",\"keys\":[\"temperature\"]}", true, "{\"deleteFrom\":\"METADATA\",\"keys\":[\"temperature\"]}"),
                Arguments.of(1, "{\"dataToFetch\":\"DATA\",\"keys\":[\"temperature\"]}", true, "{\"deleteFrom\":\"DATA\",\"keys\":[\"temperature\"]}"),
                Arguments.of(1, "{\"deleteFrom\":\"METADATA\",\"keys\":[\"temperature\"]}", false, "{\"deleteFrom\":\"METADATA\",\"keys\":[\"temperature\"]}"),
                Arguments.of(1, "{\"deleteFrom\":\"DATA\",\"keys\":[\"temperature\"]}", false, "{\"deleteFrom\":\"DATA\",\"keys\":[\"temperature\"]}")
        );
    }

    @ParameterizedTest
    @MethodSource
    void givenFromVersionAndConfig_whenUpgrade_thenVerifyUpgradeResultAndConfig(int givenVersion, String givenConfigStr,
                                                                                boolean hasChanges, String expectedConfigStr) throws Exception {
        // GIVEN
        JsonNode givenConfig = JacksonUtil.toJsonNode(givenConfigStr);
        JsonNode expectedConfig = JacksonUtil.toJsonNode(expectedConfigStr);

        // WHEN
        var upgradeResult = node.upgrade(givenVersion, givenConfig);

        // THEN
        assertThat(upgradeResult.getFirst()).isEqualTo(hasChanges);
        ObjectNode upgradedConfig = (ObjectNode) upgradeResult.getSecond();
        assertThat(upgradedConfig).isEqualTo(expectedConfig);
    }

    private TbMsg getTbMsg(EntityId entityId, String data) {
        final Map<String, String> mdMap = Map.of(
                "TestKey_1", "Test",
                "country", "US",
                "voltageDataValue", "220",
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
