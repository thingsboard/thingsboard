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
package org.thingsboard.rule.engine.metadata;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.thingsboard.rule.engine.util.TbMsgSource;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;
import org.thingsboard.server.dao.device.DeviceCredentialsService;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
    }

    @AfterEach
    void tearDown() {
        node.destroy();
    }

    @Test
    void givenDefaultConfig_whenInit_thenOK() {
        assertThat(node.config).isEqualTo(config);
        assertThat(node.fetchTo).isEqualTo(TbMsgSource.METADATA);
    }

    @Test
    void givenDefaultConfig_whenVerify_thenOK() {
        var defaultConfig = new TbFetchDeviceCredentialsNodeConfiguration().defaultConfiguration();
        assertThat(defaultConfig.getFetchTo()).isEqualTo(TbMsgSource.METADATA);
    }

    @Test
    void givenValidMsg_whenOnMsg_thenVerifyOutput() throws Exception {
        // GIVEN
        doReturn(deviceCredentialsServiceMock).when(ctxMock).getDeviceCredentialsService();
        doAnswer(invocation -> {
            DeviceCredentials deviceCredentials = new DeviceCredentials();
            deviceCredentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
            return deviceCredentials;
        }).when(deviceCredentialsServiceMock).findDeviceCredentialsByDeviceId(any(), any());
        doAnswer(invocation -> JacksonUtil.newObjectNode()).when(deviceCredentialsServiceMock).toCredentialsInfo(any());

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
        doReturn(deviceCredentialsServiceMock).when(ctxMock).getDeviceCredentialsService();
        willAnswer(invocation -> null).given(deviceCredentialsServiceMock).findDeviceCredentialsByDeviceId(any(), any());

        // WHEN
        node.onMsg(ctxMock, getTbMsg(deviceId));

        // THEN
        var newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        var exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(ctxMock, never()).tellSuccess(any());
        verify(ctxMock, times(1)).tellFailure(newMsgCaptor.capture(), exceptionCaptor.capture());

        assertThat(exceptionCaptor.getValue()).isInstanceOf(RuntimeException.class);
    }

    @Test
    void givenOldConfig_whenUpgrade_thenShouldReturnTrueResultWithNewConfig() throws Exception {
        String oldConfig = "{\"fetchToMetadata\":true}";
        JsonNode configJson = JacksonUtil.toJsonNode(oldConfig);
        TbPair<Boolean, JsonNode> upgrade = node.upgrade(0, configJson);
        assertTrue(upgrade.getFirst());
        assertEquals(config, JacksonUtil.treeToValue(upgrade.getSecond(), config.getClass()));
    }

    private TbMsg getTbMsg(EntityId entityId) {
        final Map<String, String> mdMap = Map.of(
                "country", "US",
                "city", "NY"
        );

        final var metaData = new TbMsgMetaData(mdMap);
        final String data = "{\"TestAttribute_1\": \"humidity\", \"TestAttribute_2\": \"voltage\"}";

        return TbMsg.newMsg()
                .type(TbMsgType.POST_ATTRIBUTES_REQUEST)
                .originator(entityId)
                .copyMetaData(metaData)
                .data(data)
                .callback(callbackMock)
                .build();
    }

}
