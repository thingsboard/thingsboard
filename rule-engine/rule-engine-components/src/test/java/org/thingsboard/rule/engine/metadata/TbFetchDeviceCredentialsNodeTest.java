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
package org.thingsboard.rule.engine.metadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;
import org.thingsboard.server.dao.device.DeviceCredentialsService;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TbFetchDeviceCredentialsNodeTest {
    final ObjectMapper mapper = new ObjectMapper();

    DeviceId deviceId;
    TbFetchDeviceCredentialsNode node;
    TbFetchDeviceCredentialsNodeConfiguration config;
    TbNodeConfiguration nodeConfiguration;
    TbContext ctx;
    TbMsgCallback callback;
    DeviceCredentialsService deviceCredentialsService;

    @BeforeEach
    void setUp() throws TbNodeException {
        deviceId = new DeviceId(UUID.randomUUID());
        callback = mock(TbMsgCallback.class);
        ctx = mock(TbContext.class);
        config = new TbFetchDeviceCredentialsNodeConfiguration().defaultConfiguration();
        config.setFetchToMetadata(true);
        nodeConfiguration = new TbNodeConfiguration(mapper.valueToTree(config));
        node = spy(new TbFetchDeviceCredentialsNode());
        node.init(ctx, nodeConfiguration);
        deviceCredentialsService = mock(DeviceCredentialsService.class);

        willReturn(deviceCredentialsService).given(ctx).getDeviceCredentialsService();
        willAnswer(invocation -> {
            return new DeviceCredentials();
        }).given(deviceCredentialsService).findDeviceCredentialsByDeviceId(any(), any());
    }

    @AfterEach
    void tearDown() {
        node.destroy();
    }

    @Test
    void givenDefaultConfig_whenInit_thenOK() {
        assertThat(node.config).isEqualTo(config);
        assertThat(node.fetchToMetadata).isEqualTo(true);
    }

    @Test
    void givenDefaultConfig_whenVerify_thenOK() {
        TbFetchDeviceCredentialsNodeConfiguration defaultConfig = new TbFetchDeviceCredentialsNodeConfiguration().defaultConfiguration();
        assertThat(defaultConfig.isFetchToMetadata()).isEqualTo(true);
    }

    @Test
    void givenMsg_whenOnMsg_thenVerifyOutput() throws Exception {
        node.onMsg(ctx, getTbMsg(deviceId));

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellSuccess(newMsgCaptor.capture());
        verify(ctx, never()).tellFailure(any(), any());
        verify(deviceCredentialsService, times(1)).findDeviceCredentialsByDeviceId(any(), any());

        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();

        assertThat(newMsg.getMetaData().getData().containsKey("deviceCredentials")).isEqualTo(true);
    }

    @Test
    void givenUnsupportedOriginatorType_whenOnMsg_thenTellFailure() throws Exception {
        node.onMsg(ctx, getTbMsg(new CustomerId(UUID.randomUUID())));

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(ctx, never()).tellSuccess(any());
        verify(ctx, times(1)).tellFailure(newMsgCaptor.capture(), exceptionCaptor.capture());

        assertThat(exceptionCaptor.getValue()).isInstanceOf(RuntimeException.class);
    }

    @Test
    void givenGetDeviceCredentials_whenOnMsg_thenTellFailure() throws Exception {
        willAnswer(invocation -> {
            return null;
        }).given(deviceCredentialsService).findDeviceCredentialsByDeviceId(any(), any());

        node.onMsg(ctx, getTbMsg(deviceId));

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(ctx, never()).tellSuccess(any());
        verify(ctx, times(1)).tellFailure(newMsgCaptor.capture(), exceptionCaptor.capture());

        assertThat(exceptionCaptor.getValue()).isInstanceOf(RuntimeException.class);
    }

    private TbMsg getTbMsg(EntityId entityId) {
        final Map<String, String> mdMap = Map.of(
                "country", "US",
                "city", "NY"
        );

        final TbMsgMetaData metaData = new TbMsgMetaData(mdMap);
        final String data = "{\"TestAttribute_1\": \"humidity\", \"TestAttribute_2\": \"voltage\"}";

        return TbMsg.newMsg("POST_ATTRIBUTES_REQUEST", entityId, metaData, data, callback);
    }
}

