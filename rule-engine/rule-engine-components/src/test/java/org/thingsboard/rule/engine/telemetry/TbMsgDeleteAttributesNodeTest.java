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
package org.thingsboard.rule.engine.telemetry;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.AttributesDeleteRequest;
import org.thingsboard.rule.engine.api.RuleEngineTelemetryService;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.DataConstants;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Slf4j
public class TbMsgDeleteAttributesNodeTest {
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
            AttributesDeleteRequest request = invocation.getArgument(0);
            request.getCallback().onSuccess(null);
            return null;
        }).given(telemetryService).deleteAttributes(any());
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

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_ATTRIBUTES_REQUEST)
                .originator(deviceId)
                .copyMetaData(metaData)
                .data(data)
                .callback(callback)
                .build();
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
        verify(telemetryService, times(1)).deleteAttributes(assertArg(request -> {
            assertThat(request.isNotifyDevice()).isEqualTo(notifyDevice || notifyDeviceMetadata);
        }));
    }
}
