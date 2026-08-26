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
package org.thingsboard.rule.engine.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
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
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.throwable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

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
    public void setUp() throws TbNodeException {
        node = new TbSendRPCRequestNode();
        config = new TbSendRpcRequestNodeConfiguration().defaultConfiguration();
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, configuration);
    }

    @Test
    public void verifyDefaultConfig() {
        assertThat(config.getTimeoutInSeconds()).isEqualTo(60);
        assertThat(config.isForceAck()).isTrue();
        assertThat(config.isOverrideResponseTimeout()).isFalse();
    }

    @Test
    public void givenVersionZeroConfig_whenUpgrade_thenBothFlagsSetToLegacyValues() throws TbNodeException {
        // GIVEN
        JsonNode oldConfiguration = JacksonUtil.newObjectNode().put("timeoutInSeconds", 60);

        // WHEN
        TbPair<Boolean, JsonNode> upgradeResult = node.upgrade(0, oldConfiguration);

        // THEN
        assertThat(upgradeResult.getFirst()).isTrue();
        assertThat(upgradeResult.getSecond().get("forceAck").asBoolean()).isTrue();
        assertThat(upgradeResult.getSecond().get("overrideResponseTimeout").asBoolean()).isFalse();
        assertThat(upgradeResult.getSecond().get("timeoutInSeconds").asInt()).isEqualTo(60);
    }

    @Test
    public void givenAlreadyUpgradedConfig_whenUpgrade_thenNoChanges() throws TbNodeException {
        // GIVEN
        JsonNode oldConfiguration = JacksonUtil.newObjectNode()
                .put("timeoutInSeconds", 60)
                .put("forceAck", false)
                .put("overrideResponseTimeout", true);

        // WHEN
        TbPair<Boolean, JsonNode> upgradeResult = node.upgrade(0, oldConfiguration);

        // THEN
        assertThat(upgradeResult.getFirst()).isFalse();
        assertThat(upgradeResult.getSecond().get("forceAck").asBoolean()).isFalse();
        assertThat(upgradeResult.getSecond().get("overrideResponseTimeout").asBoolean()).isTrue();
    }

    @ParameterizedTest
    @MethodSource
    public void givenOneway_whenOnMsg_thenVerifyRequest(String mdKeyValue, boolean expectedResult) {
        given(ctxMock.getRpcService()).willReturn(rpcServiceMock);
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);

        TbMsgMetaData msgMetadata = new TbMsgMetaData();
        msgMetadata.putValue("oneway", mdKeyValue);
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE)
                .originator(DEVICE_ID)
                .copyMetaData(msgMetadata)
                .data(MSG_DATA)
                .build();
        node.onMsg(ctxMock, msg);

        var ruleEngineDeviceRpcRequestCaptor = captureRequest();
        assertThat(ruleEngineDeviceRpcRequestCaptor.getValue().isOneway()).isEqualTo(expectedResult);
    }

    private static Stream<Arguments> givenOneway_whenOnMsg_thenVerifyRequest() {
        return Stream.of(
                Arguments.of("true", true),
                Arguments.of("false", false),
                Arguments.of(null, false),
                Arguments.of("", false)
        );
    }

    @Test
    public void givenMsgBody_whenOnMsg_thenVerifyRequest() {
        given(ctxMock.getRpcService()).willReturn(rpcServiceMock);
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(MSG_DATA)
                .build();
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

    @Test
    public void givenRequestIdIsNotSet_whenOnMsg_thenVerifyRequest() {
        Random randomMock = mock(Random.class);
        given(randomMock.nextInt()).willReturn(123);
        ReflectionTestUtils.setField(node, "random", randomMock);
        given(ctxMock.getRpcService()).willReturn(rpcServiceMock);
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.TO_SERVER_RPC_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(MSG_DATA)
                .build();
        node.onMsg(ctxMock, msg);

        ArgumentCaptor<RuleEngineDeviceRpcRequest> requestCaptor = captureRequest();
        assertThat(requestCaptor.getValue().getRequestId()).isEqualTo(123);
    }

    @Test
    public void givenRequestId_whenOnMsg_thenVerifyRequest() {
        given(ctxMock.getRpcService()).willReturn(rpcServiceMock);
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);
        String data = """
                {
                  "method": "setGpio",
                  "params": {
                    "pin": "23",
                    "value": 1
                  },
                  "requestId": 12345
                }
                """;
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.TO_SERVER_RPC_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(data)
                .build();
        node.onMsg(ctxMock, msg);

        ArgumentCaptor<RuleEngineDeviceRpcRequest> requestCaptor = captureRequest();
        assertThat(requestCaptor.getValue().getRequestId()).isEqualTo(12345);
    }

    @Test
    public void givenRequestUUID_whenOnMsg_thenVerifyRequest() {
        given(ctxMock.getRpcService()).willReturn(rpcServiceMock);
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);

        String requestUUID = "b795a241-5a30-48fb-92d5-46b864d47130";
        TbMsgMetaData metadata = new TbMsgMetaData();
        metadata.putValue("requestUUID", requestUUID);
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE)
                .originator(DEVICE_ID)
                .copyMetaData(metadata)
                .data(MSG_DATA)
                .build();
        node.onMsg(ctxMock, msg);

        ArgumentCaptor<RuleEngineDeviceRpcRequest> requestCaptor = captureRequest();
        assertThat(requestCaptor.getValue().getRequestUUID()).isEqualTo(UUID.fromString(requestUUID));
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void givenInvalidRequestUUID_whenOnMsg_thenVerifyRequest(String requestUUID) {
        given(ctxMock.getRpcService()).willReturn(rpcServiceMock);
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);

        TbMsgMetaData metadata = new TbMsgMetaData();
        metadata.putValue("requestUUID", requestUUID);
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE)
                .originator(DEVICE_ID)
                .copyMetaData(metadata)
                .data(MSG_DATA)
                .build();
        node.onMsg(ctxMock, msg);

        ArgumentCaptor<RuleEngineDeviceRpcRequest> requestCaptor = captureRequest();
        assertThat(requestCaptor.getValue().getRequestUUID()).isNotNull();
    }

    @Test
    public void givenOriginServiceId_whenOnMsg_thenVerifyRequest() {
        given(ctxMock.getRpcService()).willReturn(rpcServiceMock);
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);

        String originServiceId = "service-id-123";
        TbMsgMetaData metadata = new TbMsgMetaData();
        metadata.putValue("originServiceId", originServiceId);
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE)
                .originator(DEVICE_ID)
                .copyMetaData(metadata)
                .data(MSG_DATA)
                .build();
        node.onMsg(ctxMock, msg);

        ArgumentCaptor<RuleEngineDeviceRpcRequest> requestCaptor = captureRequest();
        assertThat(requestCaptor.getValue().getOriginServiceId()).isEqualTo(originServiceId);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void givenInvalidOriginServiceId_whenOnMsg_thenVerifyRequest(String originServiceId) {
        given(ctxMock.getRpcService()).willReturn(rpcServiceMock);
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);

        TbMsgMetaData metadata = new TbMsgMetaData();
        metadata.putValue("originServiceId", originServiceId);
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE)
                .originator(DEVICE_ID)
                .copyMetaData(metadata)
                .data(MSG_DATA)
                .build();
        node.onMsg(ctxMock, msg);

        ArgumentCaptor<RuleEngineDeviceRpcRequest> requestCaptor = captureRequest();
        assertThat(requestCaptor.getValue().getOriginServiceId()).isNull();
    }

    @Test
    public void givenExpirationTime_whenOnMsg_thenVerifyRequest() {
        given(ctxMock.getRpcService()).willReturn(rpcServiceMock);
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);

        String expirationTime = "2000000000000";
        TbMsgMetaData metadata = new TbMsgMetaData();
        metadata.putValue(DataConstants.EXPIRATION_TIME, expirationTime);
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE)
                .originator(DEVICE_ID)
                .copyMetaData(metadata)
                .data(MSG_DATA)
                .build();
        node.onMsg(ctxMock, msg);

        ArgumentCaptor<RuleEngineDeviceRpcRequest> requestCaptor = captureRequest();
        assertThat(requestCaptor.getValue().getExpirationTime()).isEqualTo(Long.parseLong(expirationTime));
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void givenInvalidExpirationTime_whenOnMsg_thenVerifyRequest(String expirationTime) {
        given(ctxMock.getRpcService()).willReturn(rpcServiceMock);
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);

        TbMsgMetaData metadata = new TbMsgMetaData();
        metadata.putValue(DataConstants.EXPIRATION_TIME, expirationTime);
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE)
                .originator(DEVICE_ID)
                .copyMetaData(metadata)
                .data(MSG_DATA)
                .build();
        node.onMsg(ctxMock, msg);

        ArgumentCaptor<RuleEngineDeviceRpcRequest> requestCaptor = captureRequest();
        assertThat(requestCaptor.getValue().getExpirationTime()).isGreaterThan(System.currentTimeMillis());
    }

    @Test
    public void givenRetries_whenOnMsg_thenVerifyRequest() {
        given(ctxMock.getRpcService()).willReturn(rpcServiceMock);
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);

        Integer retries = 3;
        TbMsgMetaData metadata = new TbMsgMetaData();
        metadata.putValue(DataConstants.RETRIES, String.valueOf(retries));
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE)
                .originator(DEVICE_ID)
                .copyMetaData(metadata)
                .data(MSG_DATA)
                .build();
        node.onMsg(ctxMock, msg);

        ArgumentCaptor<RuleEngineDeviceRpcRequest> requestCaptor = captureRequest();
        assertThat(requestCaptor.getValue().getRetries()).isEqualTo(retries);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void givenInvalidRetriesValue_whenOnMsg_thenVerifyRequest(String retries) {
        given(ctxMock.getRpcService()).willReturn(rpcServiceMock);
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);

        TbMsgMetaData metadata = new TbMsgMetaData();
        metadata.putValue(DataConstants.RETRIES, retries);
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE)
                .originator(DEVICE_ID)
                .copyMetaData(metadata)
                .data(MSG_DATA)
                .build();
        node.onMsg(ctxMock, msg);

        ArgumentCaptor<RuleEngineDeviceRpcRequest> requestCaptor = captureRequest();
        assertThat(requestCaptor.getValue().getRetries()).isNull();
    }

    @ParameterizedTest
    @EnumSource(TbMsgType.class)
    public void givenTbMsgType_whenOnMsg_thenVerifyRequest(TbMsgType msgType) {
        given(ctxMock.getRpcService()).willReturn(rpcServiceMock);
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);

        TbMsg msg = TbMsg.newMsg()
                .type(msgType)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(MSG_DATA)
                .build();
        node.onMsg(ctxMock, msg);

        ArgumentCaptor<RuleEngineDeviceRpcRequest> requestCaptor = captureRequest();
        if (msgType == TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE) {
            assertThat(requestCaptor.getValue().isRestApiCall()).isTrue();
            return;
        }
        assertThat(requestCaptor.getValue().isRestApiCall()).isFalse();
    }

    @ParameterizedTest
    @MethodSource
    public void givenPersistent_whenOnMsg_thenVerifyRequest(String isPersisted, boolean expectedPersistence) {
        given(ctxMock.getRpcService()).willReturn(rpcServiceMock);
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);

        TbMsgMetaData metadata = new TbMsgMetaData();
        metadata.putValue(DataConstants.PERSISTENT, isPersisted);
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE)
                .originator(DEVICE_ID)
                .copyMetaData(metadata)
                .data(MSG_DATA)
                .build();
        node.onMsg(ctxMock, msg);

        ArgumentCaptor<RuleEngineDeviceRpcRequest> requestCaptor = captureRequest();
        assertThat(requestCaptor.getValue().isPersisted()).isEqualTo(expectedPersistence);
    }

    private static Stream<Arguments> givenPersistent_whenOnMsg_thenVerifyRequest() {
        return Stream.of(
                Arguments.of("true", true),
                Arguments.of("false", false),
                Arguments.of(null, false),
                Arguments.of("", false)
        );
    }

    private ArgumentCaptor<RuleEngineDeviceRpcRequest> captureRequest() {
        ArgumentCaptor<RuleEngineDeviceRpcRequest> requestCaptor = ArgumentCaptor.forClass(RuleEngineDeviceRpcRequest.class);
        then(rpcServiceMock).should().sendRpcRequestToDevice(requestCaptor.capture(), any(Consumer.class));
        return requestCaptor;
    }

    private void stubRpcResponse(RpcError error, String response) {
        willAnswer(invocation -> {
            Consumer<RuleEngineDeviceRpcResponse> consumer = invocation.getArgument(1);
            RuleEngineDeviceRpcResponse rpcResponse = mock(RuleEngineDeviceRpcResponse.class);
            given(rpcResponse.getError()).willReturn(Optional.ofNullable(error));
            if (error == null) {
                given(rpcResponse.getResponse()).willReturn(Optional.ofNullable(response));
            }
            consumer.accept(rpcResponse);
            return null;
        }).given(rpcServiceMock).sendRpcRequestToDevice(any(RuleEngineDeviceRpcRequest.class), any(Consumer.class));
    }

    @Test
    public void givenRpcResponseWithoutError_whenOnMsg_thenSendsRpcRequest() {
        TbMsg outMsg = TbMsg.newMsg()
                .type(TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();

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

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(MSG_DATA)
                .build();
        node.onMsg(ctxMock, msg);

        then(ctxMock).should().enqueueForTellNext(outMsg, TbNodeConnectionType.SUCCESS);
        then(ctxMock).should().ack(msg);
    }

    @Test
    public void givenRpcResponseWithError_whenOnMsg_thenTellFailure() {
        TbMsg outMsg = TbMsg.newMsg()
                .type(TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();

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

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(MSG_DATA)
                .build();
        node.onMsg(ctxMock, msg);

        then(ctxMock).should().enqueueForTellFailure(outMsg, RpcError.NO_ACTIVE_CONNECTION.name());
        then(ctxMock).should().ack(msg);
    }

    @Test
    public void givenForceAckDisabledAndSuccessfulResponse_whenOnMsg_thenTellSuccessOnIncomingMsg() throws TbNodeException {
        // GIVEN
        config.setForceAck(false);
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
        given(ctxMock.getRpcService()).willReturn(rpcServiceMock);
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);
        stubRpcResponse(null, "{\"rpcId\":\"6b04b5b2-1d94-4a4e-9b47-4f1e0e40e4a1\"}");

        TbMsgMetaData metadata = new TbMsgMetaData();
        metadata.putValue(DataConstants.PERSISTENT, "true");
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE)
                .originator(DEVICE_ID)
                .copyMetaData(metadata)
                .data(MSG_DATA)
                .build();

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        then(ctxMock).should(never()).ack(any());
        then(ctxMock).should(never()).enqueueForTellNext(any(), any(String.class));
        ArgumentCaptor<TbMsg> outMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should().tellSuccess(outMsgCaptor.capture());
        TbMsg outMsg = outMsgCaptor.getValue();
        assertThat(outMsg.getId()).isEqualTo(msg.getId());
        assertThat(outMsg.getMetaData()).isEqualTo(msg.getMetaData());
        assertThat(outMsg.getData()).isEqualTo("{\"rpcId\":\"6b04b5b2-1d94-4a4e-9b47-4f1e0e40e4a1\"}");
    }

    @Test
    public void givenForceAckDisabledAndEmptyResponse_whenOnMsg_thenTellSuccessWithEmptyJson() throws TbNodeException {
        // GIVEN
        config.setForceAck(false);
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
        given(ctxMock.getRpcService()).willReturn(rpcServiceMock);
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);
        stubRpcResponse(null, null);

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(MSG_DATA)
                .build();

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        ArgumentCaptor<TbMsg> outMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should().tellSuccess(outMsgCaptor.capture());
        assertThat(outMsgCaptor.getValue().getData()).isEqualTo(TbMsg.EMPTY_JSON_OBJECT);
    }

    @Test
    public void givenForceAckDisabledAndErrorResponse_whenOnMsg_thenTellFailureOnIncomingMsg() throws TbNodeException {
        // GIVEN
        config.setForceAck(false);
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
        given(ctxMock.getRpcService()).willReturn(rpcServiceMock);
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);
        stubRpcResponse(RpcError.TIMEOUT, null);

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(MSG_DATA)
                .build();

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        then(ctxMock).should(never()).ack(any());
        then(ctxMock).should(never()).enqueueForTellFailure(any(), any(String.class));
        ArgumentCaptor<TbMsg> outMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Throwable> errorCaptor = ArgumentCaptor.forClass(Throwable.class);
        then(ctxMock).should().tellFailure(outMsgCaptor.capture(), errorCaptor.capture());
        assertThat(outMsgCaptor.getValue().getId()).isEqualTo(msg.getId());
        assertThat(outMsgCaptor.getValue().getData()).isEqualTo("{\"error\":\"TIMEOUT\"}");
        assertThat(errorCaptor.getValue()).hasMessage("TIMEOUT");
    }

    @ParameterizedTest
    @EnumSource(EntityType.class)
    public void givenOriginatorIsNotDevice_whenOnMsg_thenThrowsException(EntityType entityType) {
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(entityType, "ac21a1bb-eabf-4463-8313-24bea1f498d9");

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(entityId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctxMock, msg);

        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        then(ctxMock).should().tellFailure(eq(msg), throwableCaptor.capture());
        assertThat(throwableCaptor.getValue()).isInstanceOf(RuntimeException.class)
                .hasMessage(EntityType.DEVICE != entityType ? "Message originator is not a device entity!"
                        : "Method is not present in the message!");
    }

    @Test
    public void givenOverrideResponseTimeoutDisabled_whenOnMsg_thenDeadlineIsExpirationTime() {
        given(ctxMock.getRpcService()).willReturn(rpcServiceMock);
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);
        long expirationTime = System.currentTimeMillis() + 120_000L;

        TbMsgMetaData metadata = new TbMsgMetaData();
        metadata.putValue(DataConstants.EXPIRATION_TIME, Long.toString(expirationTime));
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE)
                .originator(DEVICE_ID)
                .copyMetaData(metadata)
                .data(MSG_DATA)
                .build();
        node.onMsg(ctxMock, msg);

        assertThat(captureRequest().getValue().getRuleEngineResponseDeadline()).isEqualTo(expirationTime);
    }

    @Test
    public void givenOverrideResponseTimeoutEnabled_whenOnMsg_thenDeadlineIsNowPlusTimeout() throws TbNodeException {
        config.setOverrideResponseTimeout(true);
        config.setTimeoutInSeconds(15);
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
        given(ctxMock.getRpcService()).willReturn(rpcServiceMock);
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);

        TbMsgMetaData metadata = new TbMsgMetaData();
        metadata.putValue(DataConstants.EXPIRATION_TIME, Long.toString(System.currentTimeMillis() + 600_000L));
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE)
                .originator(DEVICE_ID)
                .copyMetaData(metadata)
                .data(MSG_DATA)
                .build();

        long before = System.currentTimeMillis();
        node.onMsg(ctxMock, msg);
        long after = System.currentTimeMillis();

        assertThat(captureRequest().getValue().getRuleEngineResponseDeadline())
                .isBetween(before + 15_000L, after + 15_000L);
    }

    @Test
    public void givenOverrideResponseTimeoutEnabledAndZeroTimeout_whenOnMsg_thenDeadlineIsNow() throws TbNodeException {
        config.setOverrideResponseTimeout(true);
        config.setTimeoutInSeconds(0);
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
        given(ctxMock.getRpcService()).willReturn(rpcServiceMock);
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(MSG_DATA)
                .build();

        long before = System.currentTimeMillis();
        node.onMsg(ctxMock, msg);
        long after = System.currentTimeMillis();

        assertThat(captureRequest().getValue().getRuleEngineResponseDeadline()).isBetween(before, after);
    }

    @Test
    public void givenNegativeTimeout_whenInit_thenThrowsUnrecoverableException() {
        config.setTimeoutInSeconds(-1);
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        var nodeUnderTest = new TbSendRPCRequestNode();

        assertThatThrownBy(() -> nodeUnderTest.init(ctxMock, configuration))
                .isInstanceOf(TbNodeException.class)
                .hasMessage("Timeout in seconds must be non-negative!")
                .asInstanceOf(throwable(TbNodeException.class))
                .extracting(TbNodeException::isUnrecoverable)
                .isEqualTo(true);
    }

    @Test
    public void givenZeroTimeout_whenInit_thenNoException() {
        config.setTimeoutInSeconds(0);
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        var nodeUnderTest = new TbSendRPCRequestNode();

        assertThatCode(() -> nodeUnderTest.init(ctxMock, configuration)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"method", "params"})
    public void givenMethodOrParamsAreNotPresent_whenOnMsg_thenThrowsException(String key) {
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data("{\"" + key + "\": \"value\"}")
                .build();

        node.onMsg(ctxMock, msg);

        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        then(ctxMock).should().tellFailure(eq(msg), throwableCaptor.capture());
        assertThat(throwableCaptor.getValue()).isInstanceOf(RuntimeException.class)
                .hasMessage(key.equals("method") ? "Params are not present in the message!" : "Method is not present in the message!");
    }
}
