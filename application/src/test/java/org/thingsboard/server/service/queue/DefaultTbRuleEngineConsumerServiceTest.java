// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.queue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.rpc.RpcError;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponse;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineNotificationMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.service.queue.ruleengine.TbRuleEngineConsumerContext;
import org.thingsboard.server.service.rpc.TbRuleEngineDeviceRpcService;
import org.thingsboard.server.service.ruleenginemonitoring.QueueCacheService;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
    private QueueCacheService queueCacheServiceMock;
    @Mock
    private PartitionService partitionServiceMock;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private TbRuleEngineConsumerContext ctxMock;

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

    @Test
    public void givenQueueUpdateMsg_whenHandleNotification_thenQueueCacheIsInvalidated() {
        givenQueueNotificationDependencies();
        var queueUpdateMsg = TransportProtos.QueueUpdateMsg.newBuilder()
                .setTenantIdMSB(1).setTenantIdLSB(1).setQueueIdMSB(2).setQueueIdLSB(2)
                .setQueueName("Main").setQueueTopic("tb_rule_engine.main").setPartitions(1)
                .build();
        var queueMsg = new TbProtoQueueMsg<>(UUID.randomUUID(), ToRuleEngineNotificationMsg.newBuilder().addQueueUpdateMsgs(queueUpdateMsg).build());
        doCallRealMethod().when(defaultTbRuleEngineConsumerServiceMock).handleNotification(queueMsg.getKey(), queueMsg, tbCallbackMock);

        defaultTbRuleEngineConsumerServiceMock.handleNotification(queueMsg.getKey(), queueMsg, tbCallbackMock);

        then(queueCacheServiceMock).should().invalidateQueues();
        then(tbCallbackMock).should().onSuccess();
    }

    @Test
    public void givenQueueDeleteMsg_whenHandleNotification_thenQueueCacheIsInvalidated() {
        givenQueueNotificationDependencies();
        ReflectionTestUtils.setField(defaultTbRuleEngineConsumerServiceMock, "consumers", new ConcurrentHashMap<>());
        var queueDeleteMsg = TransportProtos.QueueDeleteMsg.newBuilder()
                .setTenantIdMSB(1).setTenantIdLSB(1).setQueueIdMSB(2).setQueueIdLSB(2).setQueueName("Main")
                .build();
        var queueMsg = new TbProtoQueueMsg<>(UUID.randomUUID(), ToRuleEngineNotificationMsg.newBuilder().addQueueDeleteMsgs(queueDeleteMsg).build());
        doCallRealMethod().when(defaultTbRuleEngineConsumerServiceMock).handleNotification(queueMsg.getKey(), queueMsg, tbCallbackMock);

        defaultTbRuleEngineConsumerServiceMock.handleNotification(queueMsg.getKey(), queueMsg, tbCallbackMock);

        then(queueCacheServiceMock).should().invalidateQueues();
        then(tbCallbackMock).should().onSuccess();
    }

    private void givenQueueNotificationDependencies() {
        // The mock skips field initializers, including the instance logger the queue handlers use.
        ReflectionTestUtils.setField(defaultTbRuleEngineConsumerServiceMock, "log", LoggerFactory.getLogger(DefaultTbRuleEngineConsumerService.class));
        ReflectionTestUtils.setField(defaultTbRuleEngineConsumerServiceMock, "queueCacheService", queueCacheServiceMock);
        ReflectionTestUtils.setField(defaultTbRuleEngineConsumerServiceMock, "partitionService", partitionServiceMock);
        ReflectionTestUtils.setField(defaultTbRuleEngineConsumerServiceMock, "ctx", ctxMock);
    }

}
