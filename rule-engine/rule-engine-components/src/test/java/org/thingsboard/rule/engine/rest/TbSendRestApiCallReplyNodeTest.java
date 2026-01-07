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
package org.thingsboard.rule.engine.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleEngineRpcService;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TbSendRestApiCallReplyNodeTest {

    private final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("212445ad-9852-4bfd-819d-6b01ab6ee6b6"));

    private TbSendRestApiCallReplyNode node;
    private TbSendRestApiCallReplyNodeConfiguration config;
    
    @Mock
    private TbContext ctxMock;
    @Mock
    private RuleEngineRpcService rpcServiceMock;

    @BeforeEach
    public void setUp() throws TbNodeException {
        node = new TbSendRestApiCallReplyNode();
        config = new TbSendRestApiCallReplyNodeConfiguration().defaultConfiguration();
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, configuration);
    }

    @Test
    public void givenDefaultConfig_whenInit_thenDoesNotThrowException() {
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        assertThatNoException().isThrownBy(() -> node.init(ctxMock, configuration));
    }

    @ParameterizedTest
    @MethodSource
    public void givenValidRestApiRequest_whenOnMsg_thenTellSuccess(String requestIdAttribute, String serviceIdAttribute) throws TbNodeException {
        config.setRequestIdMetaDataAttribute(requestIdAttribute);
        config.setServiceIdMetaDataAttribute(serviceIdAttribute);
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, configuration);
        when(ctxMock.getRpcService()).thenReturn(rpcServiceMock);
        String requestUUIDStr = "80b7883b-7ec6-4872-9dd3-b2afd5660fa6";
        String serviceIdStr = "tb-core-0";
        String data = """
                {
                "temperature": 23,
                }
                """;
        Map<String, String> metadata = Map.of(
                requestIdAttribute, requestUUIDStr,
                serviceIdAttribute, serviceIdStr);
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.REST_API_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(new TbMsgMetaData(metadata))
                .data(data)
                .build();

        node.onMsg(ctxMock, msg);

        UUID requestUUID = UUID.fromString(requestUUIDStr);
        verify(rpcServiceMock).sendRestApiCallReply(serviceIdStr, requestUUID, msg);
        verify(ctxMock).tellSuccess(msg);
    }

    private static Stream<Arguments> givenValidRestApiRequest_whenOnMsg_thenTellSuccess() {
        return Stream.of(
                Arguments.of("requestId", "service"),
                Arguments.of("requestUUID", "serviceId"),
                Arguments.of("some_custom_request_id_field", "some_custom_service_id_field")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void givenInvalidRequest_whenOnMsg_thenTellFailure(TbMsgMetaData metaData, String data, String errorMsg) {
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.REST_API_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(metaData)
                .data(data)
                .build();

        node.onMsg(ctxMock, msg);

        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctxMock).tellFailure(eq(msg), captor.capture());
        Throwable throwable = captor.getValue();
        assertThat(throwable).isInstanceOf(RuntimeException.class).hasMessage(errorMsg);
    }

    private static Stream<Arguments> givenInvalidRequest_whenOnMsg_thenTellFailure() {
        return Stream.of(
                Arguments.of(TbMsgMetaData.EMPTY, TbMsg.EMPTY_STRING, "Request id is not present in the metadata!"),
                Arguments.of(new TbMsgMetaData(Map.of("requestUUID", "e1dd3985-efad-45a0-b0d2-0ff5dff2ccac")),
                        TbMsg.EMPTY_STRING, "Service id is not present in the metadata!"),
                Arguments.of(new TbMsgMetaData(Map.of("serviceId", "tb-core-0")),
                        TbMsg.EMPTY_STRING, "Request id is not present in the metadata!"),
                Arguments.of(new TbMsgMetaData(Map.of("requestUUID", "e1dd3985-efad-45a0-b0d2-0ff5dff2ccac", "serviceId", "tb-core-0")),
                        TbMsg.EMPTY_STRING, "Request body is empty!")
        );
    }
}
