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
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleEngineDeviceRpcResponse;
import org.thingsboard.rule.engine.api.RuleEngineRpcService;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.rpc.RpcError;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TbSendRPCRequestNodeTest {
    
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
    public void givenRpcResponseWithoutError_whenOnMsg_thenSendsRpcRequest() {
        DeviceId deviceId = new DeviceId(UUID.fromString("dda00a40-9d9c-4464-a759-488b9617319c"));
        TenantId tenantId = new TenantId(UUID.fromString("81622599-afb3-4b52-9b47-f930f11ee963"));
        String data = """
                {
                  "method": "setGpio",
                  "params": {
                    "pin": "23",
                    "value": 1
                  }
                }
                """;
        TbMsg msg = TbMsg.newMsg(TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE, deviceId, TbMsgMetaData.EMPTY, data);
        TbMsg outMsg = TbMsg.newMsg(TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE, deviceId, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);

        when(ctxMock.getRpcService()).thenReturn(rpcServiceMock);
        when(ctxMock.getTenantId()).thenReturn(tenantId);
        when(ctxMock.newMsg(any(), any(String.class), any(), any(), any(), any())).thenReturn(outMsg);

        doAnswer(invocation -> {
            Consumer<RuleEngineDeviceRpcResponse> callback = invocation.getArgument(1);
            RuleEngineDeviceRpcResponse rpcResponseMock = mock(RuleEngineDeviceRpcResponse.class);
            when(rpcResponseMock.getError()).thenReturn(Optional.empty());
            when(rpcResponseMock.getResponse()).thenReturn(Optional.of(TbMsg.EMPTY_JSON_OBJECT));
            callback.accept(rpcResponseMock);
            return null;
        }).when(rpcServiceMock).sendRpcRequestToDevice(any(), any());

        node.onMsg(ctxMock, msg);

        verify(ctxMock).enqueueForTellNext(eq(outMsg), eq(TbNodeConnectionType.SUCCESS));
        verify(ctxMock).ack(eq(msg));
    }

    @Test
    public void givenRpcResponseWithError_whenOnMsg_thenTellFailure() {
        DeviceId deviceId = new DeviceId(UUID.fromString("dda00a40-9d9c-4464-a759-488b9617319c"));
        TenantId tenantId = new TenantId(UUID.fromString("81622599-afb3-4b52-9b47-f930f11ee963"));
        String data = """
                {
                  "method": "setGpio",
                  "params": {
                    "pin": "23",
                    "value": 1
                  }
                }
                """;
        TbMsg msg = TbMsg.newMsg(TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE, deviceId, TbMsgMetaData.EMPTY, data);
        TbMsg outMsg = TbMsg.newMsg(TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE, deviceId, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);

        when(ctxMock.getRpcService()).thenReturn(rpcServiceMock);
        when(ctxMock.getTenantId()).thenReturn(tenantId);
        when(ctxMock.newMsg(any(), any(String.class), any(), any(), any(), any())).thenReturn(outMsg);

        doAnswer(invocation -> {
            Consumer<RuleEngineDeviceRpcResponse> callback = invocation.getArgument(1);
            RuleEngineDeviceRpcResponse rpcResponseMock = mock(RuleEngineDeviceRpcResponse.class);
            when(rpcResponseMock.getError()).thenReturn(Optional.of(RpcError.NO_ACTIVE_CONNECTION));
            callback.accept(rpcResponseMock);
            return null;
        }).when(rpcServiceMock).sendRpcRequestToDevice(any(), any());

        node.onMsg(ctxMock, msg);

        verify(ctxMock).enqueueForTellFailure(eq(outMsg), eq("NO_ACTIVE_CONNECTION"));
        verify(ctxMock).ack(eq(msg));
    }

    @ParameterizedTest
    @EnumSource(EntityType.class)
    public void givenOriginatorIsNotDevice_whenOnMsg_thenThrowsException(EntityType entityType) {
        if (entityType == EntityType.DEVICE) return;
        EntityId entityId = new EntityId() {
            @Override
            public UUID getId() {
                return UUID.randomUUID();
            }

            @Override
            public EntityType getEntityType() {
                return entityType;
            }
        };
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, entityId, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);
        
        node.onMsg(ctxMock, msg);

        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctxMock).tellFailure(eq(msg), captor.capture());
        Throwable value = captor.getValue();
        assertThat(value.getClass()).isEqualTo(RuntimeException.class);
        assertThat(value.getMessage()).isEqualTo("Message originator is not a device entity!");
    }

    @ParameterizedTest
    @ValueSource(strings = {"method", "params"})
    public void givenMethodOrParamsAreNotPresent_whenOnMsg_thenThrowsException(String key) {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, deviceId, TbMsgMetaData.EMPTY, "{\"" + key + "\": \"value\"}");

        node.onMsg(ctxMock, msg);

        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctxMock).tellFailure(eq(msg), captor.capture());
        Throwable value = captor.getValue();
        assertThat(value.getClass()).isEqualTo(RuntimeException.class);
        assertThat(value.getMessage()).isEqualTo(
                key.equals("method") ? "Params are not present in the message!" : "Method is not present in the message!");
    }
}
