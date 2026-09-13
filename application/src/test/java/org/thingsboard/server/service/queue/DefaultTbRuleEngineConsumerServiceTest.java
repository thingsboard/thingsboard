// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.queue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.rpc.RpcError;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponse;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineNotificationMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.service.rpc.TbRuleEngineDeviceRpcService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doCallRealMethod;

@ExtendWith(MockitoExtension.class)
public class DefaultTbRuleEngineConsumerServiceTest {

    @Mock
    private TbRuleEngineDeviceRpcService tbDeviceRpcServiceMock;
    @Mock
    private TbCallback tbCallbackMock;

    @Mock
    private DefaultTbRuleEngineConsumerService defaultTbRuleEngineConsumerServiceMock;

    @Test
    public void givenNotFoundErrorAndNoResponse_whenHandleFromDeviceRpcResponse_thenNotFoundAndNullResponseAreRecovered() {
        // GIVEN
        ReflectionTestUtils.setField(defaultTbRuleEngineConsumerServiceMock, "tbDeviceRpcService", tbDeviceRpcServiceMock);
        var requestId = UUID.randomUUID();
        // error = NOT_FOUND.ordinal() (0) and response left unset: the previously broken combination
        // ('error > 0' dropped NOT_FOUND, proto3 default collapsed a null response to "").
        var proto = TransportProtos.FromDeviceRPCResponseProto.newBuilder()
                .setRequestIdMSB(requestId.getMostSignificantBits())
                .setRequestIdLSB(requestId.getLeastSignificantBits())
                .setError(RpcError.NOT_FOUND.ordinal())
                .build();
        var nfMsg = ToRuleEngineNotificationMsg.newBuilder().setFromDeviceRpcResponse(proto).build();
        var queueMsg = new TbProtoQueueMsg<>(requestId, nfMsg);
        doCallRealMethod().when(defaultTbRuleEngineConsumerServiceMock).handleNotification(requestId, queueMsg, tbCallbackMock);

        // WHEN
        defaultTbRuleEngineConsumerServiceMock.handleNotification(requestId, queueMsg, tbCallbackMock);

        // THEN
        var responseCaptor = ArgumentCaptor.forClass(FromDeviceRpcResponse.class);
        then(tbDeviceRpcServiceMock).should().processRpcResponseFromDevice(responseCaptor.capture());
        var response = responseCaptor.getValue();
        assertThat(response.getId()).isEqualTo(requestId);
        assertThat(response.getError()).contains(RpcError.NOT_FOUND);
        assertThat(response.getResponse()).isEmpty();
        then(tbCallbackMock).should().onSuccess();
    }

}
