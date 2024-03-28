/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.rule.engine.telemetry;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.ArgumentCaptor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.AbstractRuleNodeUpgradeTest;
import org.thingsboard.rule.engine.api.RuleEngineTelemetryService;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Slf4j
public class TbMsgDeleteAttributesNodeTest extends AbstractRuleNodeUpgradeTest {
    DeviceId deviceId;
    TbMsgDeleteAttributesNode node;
    TbMsgDeleteAttributesNodeConfiguration config;
    TbNodeConfiguration nodeConfiguration;
    TbContext ctx;
    TbMsgCallback callback;

    RuleEngineTelemetryService telemetryService;

    @BeforeEach
    void setUp() throws TbNodeException {
        deviceId = new DeviceId(UUID.randomUUID());
        callback = mock(TbMsgCallback.class);
        ctx = mock(TbContext.class);
        config = new TbMsgDeleteAttributesNodeConfiguration().defaultConfiguration();
        config.setKeys(List.of("${TestAttribute_1}", "$[TestAttribute_2]", "$[TestAttribute_3]", "TestAttribute_4"));
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node = spy(new TbMsgDeleteAttributesNode());
        node.init(ctx, nodeConfiguration);
        telemetryService = mock(RuleEngineTelemetryService.class);

        willReturn(telemetryService).given(ctx).getTelemetryService();
        willAnswer(invocation -> {
            TelemetryNodeCallback callBack = invocation.getArgument(5);
            callBack.onSuccess(null);
            return null;
        }).given(telemetryService).deleteAndNotify(
                any(), any(), any(AttributeScope.class), anyList(), anyBoolean(), any());
    }

    @AfterEach
    void tearDown() {
        node.destroy();
    }

    @Test
    void givenDefaultConfig_whenVerify_thenOK() {
        TbMsgDeleteAttributesNodeConfiguration defaultConfig = new TbMsgDeleteAttributesNodeConfiguration().defaultConfiguration();
        assertThat(defaultConfig.getScope()).isEqualTo(DataConstants.SERVER_SCOPE);
        assertThat(defaultConfig.getKeys()).isEqualTo(Collections.emptyList());
    }

    @Test
    void givenMsg_whenOnMsg_thenVerifyOutput_NoSendAttributesDeletedNotification_NoNotifyDevice() throws Exception {
        onMsg_thenVerifyOutput(false, false, false);
    }

    @Test
    void givenMsg_whenOnMsg_thenVerifyOutput_SendAttributesDeletedNotification_NoNotifyDevice() throws Exception {
        config.setSendAttributesDeletedNotification(true);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);
        onMsg_thenVerifyOutput(true, false, false);
    }

    @Test
    void givenMsg_whenOnMsg_thenVerifyOutput_SendAttributesDeletedNotification_NotifyDevice() throws Exception {
        config.setSendAttributesDeletedNotification(true);
        config.setNotifyDevice(true);
        config.setScope(DataConstants.SHARED_SCOPE);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);
        onMsg_thenVerifyOutput(true, true, false);
    }

    @Test
    void givenMsg_whenOnMsg_thenVerifyOutput_NoSendAttributesDeletedNotification_NotifyDeviceMetadata() throws Exception {
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);
        onMsg_thenVerifyOutput(false, false, true);
    }

    @Test
    void givenUseTemplateTrueAndMetadataKey_whenOnMsg_thenDeleteAttributes() throws Exception {
        config.setUseAttributesScopeTemplate(true);
        config.setScope("${scopeInMetadata}");
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        final Map<String, String> mdMap = Map.of("scopeInMetadata", "SERVER_SCOPE");
        final String data = "{\"TestAttribute_2\": \"humidity\", \"TestAttribute_3\": \"voltage\"}";
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_ATTRIBUTES_REQUEST, deviceId, new TbMsgMetaData(mdMap), data);

        node.onMsg(ctx, msg);
        List<String> keysToDelete = config.getKeys().stream()
                .map(keyPattern -> TbNodeUtils.processPattern(keyPattern, msg))
                .distinct()
                .filter(StringUtils::isNotBlank)
                .toList();

        verify(telemetryService).deleteAndNotify(eq(ctx.getTenantId()), eq(msg.getOriginator()), eq(AttributeScope.SERVER_SCOPE),
                eq(keysToDelete), eq(false), eq(new TelemetryNodeCallback(ctx, msg)));
        verify(ctx).tellSuccess(eq(msg));
    }

    @Test
    void givenUseTemplateTrueAndDataKey_whenOnMsg_thenDeleteAttributes() throws Exception {
        config.setUseAttributesScopeTemplate(true);
        config.setScope("$[scopeInData]");
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        final String data = "{\"TestAttribute_2\": \"humidity\", \"TestAttribute_3\": \"voltage\", \"scopeInData\": \"SERVER_SCOPE\"}";
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_ATTRIBUTES_REQUEST, deviceId, TbMsgMetaData.EMPTY, data);

        node.onMsg(ctx, msg);
        List<String> keysToDelete = config.getKeys().stream()
                .map(keyPattern -> TbNodeUtils.processPattern(keyPattern, msg))
                .distinct()
                .filter(StringUtils::isNotBlank)
                .toList();

        verify(telemetryService).deleteAndNotify(eq(ctx.getTenantId()), eq(msg.getOriginator()), eq(AttributeScope.SERVER_SCOPE),
                eq(keysToDelete), eq(false), eq(new TelemetryNodeCallback(ctx, msg)));
        verify(ctx).tellSuccess(eq(msg));
    }

    @Test
    void givenUseTemplateTrueAndNoMetadataKeyInMsg_whenOnMsg_thenDeleteAttributes() throws Exception {
        config.setUseAttributesScopeTemplate(true);
        config.setScope("${scopeInMetadata}");
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        final Map<String, String> mdMap = Map.of("scope", "SERVER_SCOPE");
        final String data = "{\"TestAttribute_2\": \"humidity\", \"TestAttribute_3\": \"voltage\"}";
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_ATTRIBUTES_REQUEST, deviceId, new TbMsgMetaData(mdMap), data);

        node.onMsg(ctx, msg);
        List<String> keysToDelete = config.getKeys().stream()
                .map(keyPattern -> TbNodeUtils.processPattern(keyPattern, msg))
                .distinct()
                .filter(StringUtils::isNotBlank)
                .toList();

        verify(telemetryService).deleteAndNotify(eq(ctx.getTenantId()), eq(msg.getOriginator()), eq(AttributeScope.SERVER_SCOPE),
                eq(keysToDelete), eq(false), eq(new TelemetryNodeCallback(ctx, msg)));
        verify(ctx).tellSuccess(eq(msg));
    }

    @Test
    void givenUseTemplateTrueAndStrangeDataKey_whenOnMsg_thenDeleteAttributes() throws Exception {
        config.setUseAttributesScopeTemplate(true);
        config.setScope("$[scopeInData]");
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        final String data = "{\"TestAttribute_2\": \"humidity\", \"TestAttribute_3\": \"voltage\", \"scopeInData\": \" SeRvEr_ScOpE \"}";
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_ATTRIBUTES_REQUEST, deviceId, TbMsgMetaData.EMPTY, data);

        node.onMsg(ctx, msg);
        List<String> keysToDelete = config.getKeys().stream()
                .map(keyPattern -> TbNodeUtils.processPattern(keyPattern, msg))
                .distinct()
                .filter(StringUtils::isNotBlank)
                .toList();

        verify(telemetryService).deleteAndNotify(eq(ctx.getTenantId()), eq(msg.getOriginator()), eq(AttributeScope.SERVER_SCOPE),
                eq(keysToDelete), eq(false), eq(new TelemetryNodeCallback(ctx, msg)));
        verify(ctx).tellSuccess(eq(msg));
    }

    @Test
    void givenUseTemplateTrueAndInvalidMetadataKey_whenOnMsg_thenThrowException() throws TbNodeException {
        config.setUseAttributesScopeTemplate(true);
        config.setScope("${scopeInMetadata}");
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        final Map<String, String> mdMap = Map.of("scopeMetadata", "SERVER_SCOPE");
        final String data = "{\"TestAttribute_2\": \"humidity\", \"TestAttribute_3\": \"voltage\"}";
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_ATTRIBUTES_REQUEST, deviceId, new TbMsgMetaData(mdMap), data);

        Assertions.assertThatThrownBy(() -> node.onMsg(ctx, msg))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Failed to parse scope! No enum constant for name: " + config.getScope());
    }

    @Test
    void givenUseTemplateTrueAndMetadataKeyNonExistingScope_whenOnMsg_thenThrowException() throws TbNodeException {
        config.setUseAttributesScopeTemplate(true);
        config.setScope("${scopeInMetadata}");
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        final Map<String, String> mdMap = Map.of("scopeInMetadata", "ANOTHER_SCOPE");
        final String data = "{\"TestAttribute_2\": \"humidity\", \"TestAttribute_3\": \"voltage\"}";
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_ATTRIBUTES_REQUEST, deviceId, new TbMsgMetaData(mdMap), data);

        assertThatThrownBy(() -> node.onMsg(ctx, msg))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Failed to parse scope! No enum constant for name: " + msg.getMetaData().getValue("scopeInMetadata"));

    }
    @Test
    void givenScopeIsNull_whenOnMsg_thenThrowException() throws TbNodeException {
        config.setScope(null);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        final Map<String, String> mdMap = Map.of("scopeInMetadata", "ANOTHER_SCOPE");
        final String data = "{\"TestAttribute_2\": \"humidity\", \"TestAttribute_3\": \"voltage\"}";
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_ATTRIBUTES_REQUEST, deviceId, new TbMsgMetaData(mdMap), data);

        assertThatThrownBy(() -> node.onMsg(ctx, msg))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Failed to parse scope! Provided value is null!");
    }

    @Test
    void givenDefaultConfig_whenInit_thenOk() {
        config = new TbMsgDeleteAttributesNodeConfiguration().defaultConfiguration();
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        assertThatCode(() -> node.init(ctx, nodeConfiguration))
                .doesNotThrowAnyException();
    }

    void onMsg_thenVerifyOutput(boolean sendAttributesDeletedNotification, boolean notifyDevice, boolean notifyDeviceMetadata) throws Exception {
        final Map<String, String> mdMap = Map.of(
                "TestAttribute_1", "temperature",
                "city", "NY"
        );
        TbMsgMetaData metaData = new TbMsgMetaData(mdMap);
        if (notifyDeviceMetadata) {
            metaData.putValue(DataConstants.NOTIFY_DEVICE_METADATA_KEY, "true");
            metaData.putValue(DataConstants.SCOPE, DataConstants.SHARED_SCOPE);
        }
        final String data = "{\"TestAttribute_2\": \"humidity\", \"TestAttribute_3\": \"voltage\"}";

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_ATTRIBUTES_REQUEST, deviceId, metaData, data, callback);
        node.onMsg(ctx, msg);

        ArgumentCaptor<Runnable> successCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Consumer<Throwable>> failureCaptor = ArgumentCaptor.forClass(Consumer.class);
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);

        if (sendAttributesDeletedNotification) {
            verify(ctx, times(1)).enqueue(any(), successCaptor.capture(), failureCaptor.capture());
            successCaptor.getValue().run();
            verify(ctx, times(1)).attributesDeletedActionMsg(any(), any(), anyString(), anyList());
        }
        verify(ctx, times(1)).tellSuccess(newMsgCaptor.capture());
        verify(ctx, never()).tellFailure(any(), any());
        verify(telemetryService, times(1)).deleteAndNotify(any(), any(), any(AttributeScope.class), anyList(), eq(notifyDevice || notifyDeviceMetadata), any());
    }

    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                //config for version 0
                Arguments.of(0,
                        "{\"scope\": \"SERVER_SCOPE\", \"keys\": [], \"sendAttributesDeletedNotification\": false, \"notifyDevice\": false}",
                        true,
                        "{\"scope\": \"SERVER_SCOPE\", \"keys\": [], \"sendAttributesDeletedNotification\": false, \"notifyDevice\": false, \"useAttributesScopeTemplate\": false}"
                ),
                //config for version 1 with upgrade from version 0
                Arguments.of(1,
                        "{\"scope\": \"SERVER_SCOPE\", \"keys\": [], \"sendAttributesDeletedNotification\": false, \"notifyDevice\": false, \"useAttributesScopeTemplate\": false}",
                        false,
                        "{\"scope\": \"SERVER_SCOPE\", \"keys\": [], \"sendAttributesDeletedNotification\": false, \"notifyDevice\": false, \"useAttributesScopeTemplate\": false}"
                )
        );
    }

    @Override
    protected TbNode getTestNode() {
        return node;
    }
}
