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
package org.thingsboard.rule.engine.rpc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleEngineDeviceRpcRequest;
import org.thingsboard.rule.engine.api.RuleEngineDeviceRpcResponse;
import org.thingsboard.rule.engine.api.RuleEngineRpcService;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.rpc.RpcError;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class TbSendRPCRequestNodeTest {

    private final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("d3a47f8b-d863-4c1f-b6f0-2c946b43f21c"));
    private final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("b052ae59-b9b4-47e8-ac71-39e7124bbd66"));

    private final String MSG_DATA = """
            {
              "method": "setGpio",
              "params": {
                "pin": "23",
                "value": 1
              },
              "additionalInfo": "information"
            }
            """;

    private TbSendRPCRequestNode node;
    private TbSendRpcRequestNodeConfiguration config;

    @Mock
    private TbContext ctxMock;
    @Mock
    private RuleEngineRpcService rpcServiceMock;

    @BeforeEach
    void setUp() throws TbNodeException {
        node = new TbSendRPCRequestNode();
        config = new TbSendRpcRequestNodeConfiguration().defaultConfiguration();
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, configuration);
    }

    @Test
    void verifyDefaultConfig() {
        assertThat(config.getTimeoutInSeconds()).isEqualTo(60);
    }

    @ParameterizedTest
    @MethodSource
    void givenOneway_whenOnMsg_thenVerifyRequest(Map<String, String> metadata, Consumer<RuleEngineDeviceRpcRequest> requestConsumer) {
        given(ctxMock.getRpcService()).willReturn(rpcServiceMock);
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);

        TbMsgMetaData msgMetadata = metadata == null ? TbMsgMetaData.EMPTY : new TbMsgMetaData(metadata);
        TbMsg msg = TbMsg.newMsg(TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE, DEVICE_ID, msgMetadata, MSG_DATA);
        node.onMsg(ctxMock, msg);

        verifyRequest(requestConsumer);
    }

    private static Stream<Arguments> givenOneway_whenOnMsg_thenVerifyRequest() {
        var metadata = new HashMap<>();
        metadata.put("oneway", null);
        return Stream.of(
                Arguments.of(Map.of("oneway", "true"), (Consumer<RuleEngineDeviceRpcRequest>) req ->
                        assertThat(req.isOneway()).isTrue()),
                Arguments.of(null, (Consumer<RuleEngineDeviceRpcRequest>) req ->
                        assertThat(req.isOneway()).isFalse()),
                Arguments.of(Map.of("oneway", ""), (Consumer<RuleEngineDeviceRpcRequest>) req ->
                        assertThat(req.isOneway()).isFalse()),
                Arguments.of(metadata, (Consumer<RuleEngineDeviceRpcRequest>) req ->
                        assertThat(req.isOneway()).isFalse())
        );
    }

    @Test
    void givenMsgBody_whenOnMsg_thenVerifyRequest() {
        given(ctxMock.getRpcService()).willReturn(rpcServiceMock);
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);

        TbMsg msg = TbMsg.newMsg(TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE, DEVICE_ID, TbMsgMetaData.EMPTY, MSG_DATA);
        node.onMsg(ctxMock, msg);

        ArgumentCaptor<RuleEngineDeviceRpcRequest> requestCaptor = ArgumentCaptor.forClass(RuleEngineDeviceRpcRequest.class);
        then(rpcServiceMock).should().sendRpcRequestToDevice(requestCaptor.capture(), any(Consumer.class));
        assertThat(requestCaptor.getValue())
                .hasFieldOrPropertyWithValue("method", "setGpio")
                .hasFieldOrPropertyWithValue("body", "{\"pin\":\"23\",\"value\":1}")
                .hasFieldOrPropertyWithValue("deviceId", DEVICE_ID)
                .hasFieldOrPropertyWithValue("tenantId", TENANT_ID)
                .hasFieldOrPropertyWithValue("additionalInfo", "information");
    }

    @ParameterizedTest
    @MethodSource
    void givenRequestId_whenOnMsg_thenVerifyRequest(String requestId, Consumer<RuleEngineDeviceRpcRequest> requestConsumer) {
        given(ctxMock.getRpcService()).willReturn(rpcServiceMock);
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);

        String data = String.format("""
                {
                  "method": "setGpio",
                  "params": {
                    "pin": "23",
                    "value": 1
                  }%s%s
                }
                """, requestId != null ? ",\"requestId\":" : "", requestId != null ? requestId : "");
        TbMsg msg = TbMsg.newMsg(TbMsgType.TO_SERVER_RPC_REQUEST, DEVICE_ID, TbMsgMetaData.EMPTY, data);
        node.onMsg(ctxMock, msg);

        verifyRequest(requestConsumer);
    }

    private static Stream<Arguments> givenRequestId_whenOnMsg_thenVerifyRequest() {
        return Stream.of(
                Arguments.of("12345", (Consumer<RuleEngineDeviceRpcRequest>) req ->
                        assertThat(req.getRequestId()).isEqualTo(12345)),
                Arguments.of(null, (Consumer<RuleEngineDeviceRpcRequest>) req ->
                        assertThat(req.getRequestId()).isNotNull())
        );
    }

    @ParameterizedTest
    @MethodSource
    void givenRequestUUID_whenOnMsg_thenVerifyRequest(Map<String, String> metadata, Consumer<RuleEngineDeviceRpcRequest> requestConsumer) {
        given(ctxMock.getRpcService()).willReturn(rpcServiceMock);
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);

        TbMsgMetaData msgMetadata = metadata == null ? TbMsgMetaData.EMPTY : new TbMsgMetaData(metadata);
        TbMsg msg = TbMsg.newMsg(TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE, DEVICE_ID, msgMetadata, MSG_DATA);
        node.onMsg(ctxMock, msg);

        verifyRequest(requestConsumer);
    }

    private static Stream<Arguments> givenRequestUUID_whenOnMsg_thenVerifyRequest() {
        var metadata= new HashMap<>();
        metadata.put("requestUUID", null);
        return Stream.of(
                Arguments.of(Map.of("requestUUID", "1c4ef338-ea1b-495f-8e2b-67981f27cf35"), (Consumer<RuleEngineDeviceRpcRequest>) req ->
                        assertThat(req.getRequestUUID()).isEqualTo(UUID.fromString("1c4ef338-ea1b-495f-8e2b-67981f27cf35"))),
                Arguments.of(null, (Consumer<RuleEngineDeviceRpcRequest>) req ->
                        assertThat(req.getRequestUUID()).isNotNull()),
                Arguments.of(Map.of("requestUUID", ""), (Consumer<RuleEngineDeviceRpcRequest>) req ->
                        assertThat(req.getRequestUUID()).isNotNull()),
                Arguments.of(metadata, (Consumer<RuleEngineDeviceRpcRequest>) req ->
                        assertThat(req.getRequestUUID()).isNotNull())
        );
    }

    @ParameterizedTest
    @MethodSource
    void givenOriginServiceId_whenOnMsg_thenVerifyRequest(Map<String, String> metadata, Consumer<RuleEngineDeviceRpcRequest> requestConsumer) {
        given(ctxMock.getRpcService()).willReturn(rpcServiceMock);
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);

        TbMsgMetaData msgMetaData = metadata == null ? TbMsgMetaData.EMPTY : new TbMsgMetaData(metadata);
        TbMsg msg = TbMsg.newMsg(TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE, DEVICE_ID, msgMetaData, MSG_DATA);
        node.onMsg(ctxMock, msg);

        verifyRequest(requestConsumer);
    }

    private static Stream<Arguments> givenOriginServiceId_whenOnMsg_thenVerifyRequest() {
        var metadata= new HashMap<>();
        metadata.put("originServiceId", null);
        return Stream.of(
                Arguments.of(Map.of("originServiceId", "service-id-123"), (Consumer<RuleEngineDeviceRpcRequest>) req ->
                        assertThat(req.getOriginServiceId()).isEqualTo("service-id-123")),
                Arguments.of(null, (Consumer<RuleEngineDeviceRpcRequest>) req ->
                        assertThat(req.getOriginServiceId()).isNull()),
                Arguments.of(Map.of("originServiceId", ""), (Consumer<RuleEngineDeviceRpcRequest>) req ->
                        assertThat(req.getOriginServiceId()).isNull()),
                Arguments.of(metadata, (Consumer<RuleEngineDeviceRpcRequest>) req ->
                        assertThat(req.getOriginServiceId()).isNull())
        );
    }

    @ParameterizedTest
    @MethodSource
    void givenExpirationTime_whenOnMsg_thenVerifyRequest(Map<String, String> metadata, Consumer<RuleEngineDeviceRpcRequest> requestConsumer) {
        given(ctxMock.getRpcService()).willReturn(rpcServiceMock);
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);

        TbMsgMetaData msgMetaData = metadata == null ? TbMsgMetaData.EMPTY : new TbMsgMetaData(metadata);
        TbMsg msg = TbMsg.newMsg(TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE, DEVICE_ID, msgMetaData, MSG_DATA);
        node.onMsg(ctxMock, msg);

        verifyRequest(requestConsumer);
    }

    private static Stream<Arguments> givenExpirationTime_whenOnMsg_thenVerifyRequest() {
        var metadata= new HashMap<>();
        metadata.put(DataConstants.EXPIRATION_TIME, null);
        return Stream.of(
                Arguments.of(Map.of(DataConstants.EXPIRATION_TIME, "2000000000000"), (Consumer<RuleEngineDeviceRpcRequest>) req ->
                        assertThat(req.getExpirationTime()).isEqualTo(2000000000000L)),
                Arguments.of(null, (Consumer<RuleEngineDeviceRpcRequest>) req ->
                        assertThat(req.getExpirationTime()).isGreaterThan(System.currentTimeMillis())),
                Arguments.of(Map.of(DataConstants.EXPIRATION_TIME, ""), (Consumer<RuleEngineDeviceRpcRequest>) req ->
                        assertThat(req.getExpirationTime()).isGreaterThan(System.currentTimeMillis())),
                Arguments.of(metadata, (Consumer<RuleEngineDeviceRpcRequest>) req ->
                        assertThat(req.getExpirationTime()).isGreaterThan(System.currentTimeMillis()))
        );
    }

    @ParameterizedTest
    @MethodSource
    void givenRetries_whenOnMsg_thenVerifyRequest(Map<String, String> metadata, Consumer<RuleEngineDeviceRpcRequest> requestConsumer) {
        given(ctxMock.getRpcService()).willReturn(rpcServiceMock);
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);

        TbMsgMetaData msgMetaData = metadata == null ? TbMsgMetaData.EMPTY : new TbMsgMetaData(metadata);
        TbMsg msg = TbMsg.newMsg(TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE, DEVICE_ID, msgMetaData, MSG_DATA);
        node.onMsg(ctxMock, msg);

        verifyRequest(requestConsumer);
    }

    private static Stream<Arguments> givenRetries_whenOnMsg_thenVerifyRequest() {
        var metadata= new HashMap<>();
        metadata.put(DataConstants.RETRIES, null);
        return Stream.of(
                Arguments.of(Map.of(DataConstants.RETRIES, "3"), (Consumer<RuleEngineDeviceRpcRequest>) req ->
                        assertThat(req.getRetries()).isEqualTo(3)),
                Arguments.of(null, (Consumer<RuleEngineDeviceRpcRequest>) req ->
                        assertThat(req.getRetries()).isNull()),
                Arguments.of(Map.of(DataConstants.RETRIES,""), (Consumer<RuleEngineDeviceRpcRequest>) req ->
                        assertThat(req.getRetries()).isNull()),
                Arguments.of(metadata, (Consumer<RuleEngineDeviceRpcRequest>) req ->
                        assertThat(req.getRetries()).isNull())
        );
    }

    @ParameterizedTest
    @MethodSource
    void givenTbMsgType_whenOnMsg_thenVerifyRequest(TbMsgType msgType, Consumer<RuleEngineDeviceRpcRequest> requestConsumer) {
        given(ctxMock.getRpcService()).willReturn(rpcServiceMock);
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);

        TbMsg msg = TbMsg.newMsg(msgType, DEVICE_ID, TbMsgMetaData.EMPTY, MSG_DATA);
        node.onMsg(ctxMock, msg);

        verifyRequest(requestConsumer);
    }

    private static Stream<Arguments> givenTbMsgType_whenOnMsg_thenVerifyRequest() {
        return Stream.of(
                Arguments.of(TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE, (Consumer<RuleEngineDeviceRpcRequest>) req ->
                        assertThat(req.isRestApiCall()).isTrue()),
                Arguments.of(TbMsgType.TO_SERVER_RPC_REQUEST, (Consumer<RuleEngineDeviceRpcRequest>) req ->
                        assertThat(req.isRestApiCall()).isFalse())
        );
    }

    @ParameterizedTest
    @MethodSource
    void givenPersistent_whenOnMsg_thenVerifyRequest(Map<String, String> metadata, Consumer<RuleEngineDeviceRpcRequest> requestConsumer) {
        given(ctxMock.getRpcService()).willReturn(rpcServiceMock);
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);

        TbMsgMetaData msgMetaData = metadata == null ? TbMsgMetaData.EMPTY : new TbMsgMetaData(metadata);
        TbMsg msg = TbMsg.newMsg(TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE, DEVICE_ID, msgMetaData, MSG_DATA);
        node.onMsg(ctxMock, msg);

        verifyRequest(requestConsumer);
    }

    private static Stream<Arguments> givenPersistent_whenOnMsg_thenVerifyRequest() {
        var metadata= new HashMap<>();
        metadata.put(DataConstants.PERSISTENT, null);
        return Stream.of(
                Arguments.of(Map.of(DataConstants.PERSISTENT, "true"), (Consumer<RuleEngineDeviceRpcRequest>) req ->
                        assertThat(req.isPersisted()).isTrue()),
                Arguments.of(null, (Consumer<RuleEngineDeviceRpcRequest>) req ->
                        assertThat(req.isPersisted()).isFalse()),
                Arguments.of(Map.of(DataConstants.PERSISTENT, ""), (Consumer<RuleEngineDeviceRpcRequest>) req ->
                        assertThat(req.isPersisted()).isFalse()),
                Arguments.of(metadata, (Consumer<RuleEngineDeviceRpcRequest>) req ->
                        assertThat(req.isPersisted()).isFalse())
        );
    }

    private void verifyRequest(Consumer<RuleEngineDeviceRpcRequest> requestConsumer) {
        ArgumentCaptor<RuleEngineDeviceRpcRequest> requestCaptor = ArgumentCaptor.forClass(RuleEngineDeviceRpcRequest.class);
        then(rpcServiceMock).should().sendRpcRequestToDevice(requestCaptor.capture(), any(Consumer.class));
        requestConsumer.accept(requestCaptor.getValue());
    }

    @Test
    void givenRpcResponseWithoutError_whenOnMsg_thenSendsRpcRequest() {
        TbMsg outMsg = TbMsg.newMsg(TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE, DEVICE_ID, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);

        given(ctxMock.getRpcService()).willReturn(rpcServiceMock);
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);
        // TODO: replace deprecated method newMsg()
        given(ctxMock.newMsg(any(), any(String.class), any(), any(), any(), any())).willReturn(outMsg);
        willAnswer(invocation -> {
            Consumer<RuleEngineDeviceRpcResponse> consumer = invocation.getArgument(1);
            RuleEngineDeviceRpcResponse rpcResponseMock = mock(RuleEngineDeviceRpcResponse.class);
            given(rpcResponseMock.getError()).willReturn(Optional.empty());
            given(rpcResponseMock.getResponse()).willReturn(Optional.of(TbMsg.EMPTY_JSON_OBJECT));
            consumer.accept(rpcResponseMock);
            return null;
        }).given(rpcServiceMock).sendRpcRequestToDevice(any(RuleEngineDeviceRpcRequest.class), any(Consumer.class));

        TbMsg msg = TbMsg.newMsg(TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE, DEVICE_ID, TbMsgMetaData.EMPTY, MSG_DATA);
        node.onMsg(ctxMock, msg);

        then(ctxMock).should().enqueueForTellNext(outMsg, TbNodeConnectionType.SUCCESS);
        then(ctxMock).should().ack(msg);
    }

    @Test
    void givenRpcResponseWithError_whenOnMsg_thenTellFailure() {
        TbMsg outMsg = TbMsg.newMsg(TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE, DEVICE_ID, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);

        given(ctxMock.getRpcService()).willReturn(rpcServiceMock);
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);
        // TODO: replace deprecated method newMsg()
        given(ctxMock.newMsg(any(), any(String.class), any(), any(), any(), any())).willReturn(outMsg);
        willAnswer(invocation -> {
            Consumer<RuleEngineDeviceRpcResponse> consumer = invocation.getArgument(1);
            RuleEngineDeviceRpcResponse rpcResponseMock = mock(RuleEngineDeviceRpcResponse.class);
            given(rpcResponseMock.getError()).willReturn(Optional.of(RpcError.NO_ACTIVE_CONNECTION));
            consumer.accept(rpcResponseMock);
            return null;
        }).given(rpcServiceMock).sendRpcRequestToDevice(any(RuleEngineDeviceRpcRequest.class), any(Consumer.class));

        TbMsg msg = TbMsg.newMsg(TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE, DEVICE_ID, TbMsgMetaData.EMPTY, MSG_DATA);
        node.onMsg(ctxMock, msg);

        then(ctxMock).should().enqueueForTellFailure(outMsg, RpcError.NO_ACTIVE_CONNECTION.name());
        then(ctxMock).should().ack(msg);
    }

    @ParameterizedTest
    @EnumSource(EntityType.class)
    void givenOriginatorIsNotDevice_whenOnMsg_thenThrowsException(EntityType entityType) {
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(entityType, "ac21a1bb-eabf-4463-8313-24bea1f498d9");

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, entityId, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);
        node.onMsg(ctxMock, msg);

        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        then(ctxMock).should().tellFailure(eq(msg), throwableCaptor.capture());
        assertThat(throwableCaptor.getValue()).isInstanceOf(RuntimeException.class)
                .hasMessage(EntityType.DEVICE != entityType ? "Message originator is not a device entity!"
                        : "Method is not present in the message!");
    }

    @ParameterizedTest
    @ValueSource(strings = {"method", "params"})
    void givenMethodOrParamsAreNotPresent_whenOnMsg_thenThrowsException(String key) {
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, TbMsgMetaData.EMPTY, "{\"" + key + "\": \"value\"}");

        node.onMsg(ctxMock, msg);

        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        then(ctxMock).should().tellFailure(eq(msg), throwableCaptor.capture());
        assertThat(throwableCaptor.getValue()).isInstanceOf(RuntimeException.class)
                .hasMessage(key.equals("method") ? "Params are not present in the message!" : "Method is not present in the message!");
    }
}
