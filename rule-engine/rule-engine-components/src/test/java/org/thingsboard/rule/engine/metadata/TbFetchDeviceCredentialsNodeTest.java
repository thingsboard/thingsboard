/**
 * Copyright © 2016-2023 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.rule.engine.metadata;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.thingsboard.server.common.data.security.DeviceCredentialsType.ACCESS_TOKEN;

@ExtendWith(MockitoExtension.class)
public class TbFetchDeviceCredentialsNodeTest {

    @Mock
    private TbContext ctxMock;
    @Mock
    private TbMsgCallback callbackMock;
    @Mock
    private DeviceCredentialsService deviceCredentialsServiceMock;
    @Spy
    private TbFetchDeviceCredentialsNode node;
    private DeviceId deviceId;
    private TbFetchDeviceCredentialsNodeConfiguration config;

    @BeforeEach
    void setUp() throws TbNodeException {
        deviceId = new DeviceId(UUID.randomUUID());
        config = new TbFetchDeviceCredentialsNodeConfiguration().defaultConfiguration();
        config.setFetchTo(FetchTo.METADATA);
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        lenient().doReturn(deviceCredentialsServiceMock).when(ctxMock).getDeviceCredentialsService();
        lenient().doAnswer(invocation -> {
            DeviceCredentials deviceCredentials = new DeviceCredentials();
            deviceCredentials.setCredentialsType(ACCESS_TOKEN);
            return deviceCredentials;
        }).when(deviceCredentialsServiceMock).findDeviceCredentialsByDeviceId(any(), any());
        lenient().doAnswer(invocation -> JacksonUtil.newObjectNode()).when(deviceCredentialsServiceMock).toCredentialsInfo(any());
    }

    @AfterEach
    void tearDown() {
        node.destroy();
    }

    @Test
    void givenDefaultConfig_whenInit_thenOK() {
        assertThat(node.config).isEqualTo(config);
        assertThat(node.fetchTo).isEqualTo(FetchTo.METADATA);
    }

    @Test
    void givenDefaultConfig_whenVerify_thenOK() {
        var defaultConfig = new TbFetchDeviceCredentialsNodeConfiguration().defaultConfiguration();
        assertThat(defaultConfig.getFetchTo()).isEqualTo(FetchTo.METADATA);
    }

    @Test
    void givenValidMsg_whenOnMsg_thenVerifyOutput() throws Exception {
        // GIVEN

        // WHEN
        node.onMsg(ctxMock, getTbMsg(deviceId));

        // THEN
        var newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctxMock, times(1)).tellSuccess(newMsgCaptor.capture());
        verify(ctxMock, never()).tellFailure(any(), any());
        verify(deviceCredentialsServiceMock, times(1)).findDeviceCredentialsByDeviceId(any(), any());

        var newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();

        assertThat(newMsg.getMetaData().getData().containsKey("credentials")).isEqualTo(true);
        assertThat(newMsg.getMetaData().getData().containsKey("credentialsType")).isEqualTo(true);
    }

    @Test
    void givenUnsupportedOriginatorType_whenOnMsg_thenShouldTellFailure() throws Exception {
        // GIVEN
        var randomCustomerId = new CustomerId(UUID.randomUUID());

        // WHEN
        node.onMsg(ctxMock, getTbMsg(randomCustomerId));

        // THEN
        var newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        var exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(ctxMock, never()).tellSuccess(any());
        verify(ctxMock, times(1)).tellFailure(newMsgCaptor.capture(), exceptionCaptor.capture());

        assertThat(exceptionCaptor.getValue()).isInstanceOf(RuntimeException.class);
    }

    @Test
    void givenGetDeviceCredentials_whenOnMsg_thenShouldTellFailure() throws Exception {
        // GIVEN
        willAnswer(invocation -> null)
                .given(deviceCredentialsServiceMock).findDeviceCredentialsByDeviceId(any(), any());

        // WHEN
        node.onMsg(ctxMock, getTbMsg(deviceId));

        // THEN
        var newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        var exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(ctxMock, never()).tellSuccess(any());
        verify(ctxMock, times(1)).tellFailure(newMsgCaptor.capture(), exceptionCaptor.capture());

        assertThat(exceptionCaptor.getValue()).isInstanceOf(RuntimeException.class);
    }

    private TbMsg getTbMsg(EntityId entityId) {
        final Map<String, String> mdMap = Map.of(
                "country", "US",
                "city", "NY"
        );

        final var metaData = new TbMsgMetaData(mdMap);
        final String data = "{\"TestAttribute_1\": \"humidity\", \"TestAttribute_2\": \"voltage\"}";

        return TbMsg.newMsg("POST_ATTRIBUTES_REQUEST", entityId, metaData, data, callbackMock);
    }

}
