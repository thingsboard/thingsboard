/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.rule.engine.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.stubbing.Answer;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.rule.engine.api.RuleNodeCacheService;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.TbRelationTypes;
import org.thingsboard.rule.engine.delay.TbMsgDelayNode;
import org.thingsboard.rule.engine.delay.TbMsgDelayNodeConfiguration;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.msg.session.SessionMsgType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
public class TbMsgDelayNodeTest {

    private static final String TB_MSG_DELAY_NODE_MSG = "TbMsgDelayNodeMsg";
    private static final String DELAYED_ORIGINATOR_IDS_CACHE_KEY = "delayed_originator_ids";

    private TbContext ctx;
    private TopicPartitionInfo tpi;

    private final ThingsBoardThreadFactory factory = ThingsBoardThreadFactory.forName("deduplication-node-test");
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(factory);
    private final int delayPeriod = 1;

    private TenantId tenantId;

    private TbMsgDelayNode node;
    private TbMsgDelayNodeConfiguration config;
    private TbNodeConfiguration nodeConfiguration;
    private RuleNodeCacheService ruleNodeCacheService;

    private CountDownLatch awaitTellSelfLatch;

    @BeforeEach
    public void init() throws TbNodeException {
        ctx = mock(TbContext.class);
        tpi = mock(TopicPartitionInfo.class);
        ruleNodeCacheService = mock(RuleNodeCacheService.class);

        tenantId = TenantId.fromUUID(UUID.randomUUID());
        RuleNodeId ruleNodeId = new RuleNodeId(UUID.randomUUID());

        when(ctx.getSelfId()).thenReturn(ruleNodeId);
        when(ctx.getTenantId()).thenReturn(tenantId);
        when(ctx.getRuleNodeCacheService()).thenReturn(ruleNodeCacheService);
        when(ctx.getEntityTopicPartition(any())).thenReturn(tpi);
        when(tpi.getPartition()).thenReturn(Optional.of(0));
        when(tpi.isMyPartition()).thenReturn(true);
        when(ruleNodeCacheService.getEntityIds(DELAYED_ORIGINATOR_IDS_CACHE_KEY)).thenReturn(Collections.emptySet());
        when(ruleNodeCacheService.getTbMsgs(anyString(), any())).thenReturn(Collections.emptySet());

        doAnswer((Answer<TbMsg>) invocationOnMock -> {
            String type = (String) (invocationOnMock.getArguments())[1];
            EntityId originator = (EntityId) (invocationOnMock.getArguments())[2];
            TbMsgMetaData metaData = (TbMsgMetaData) (invocationOnMock.getArguments())[3];
            String data = (String) (invocationOnMock.getArguments())[4];
            return TbMsg.newMsg(type, originator, metaData.copy(), data);
        }).when(ctx).newMsg(isNull(), eq(TB_MSG_DELAY_NODE_MSG), nullable(EntityId.class), any(TbMsgMetaData.class), any(String.class));
        node = spy(new TbMsgDelayNode());
        config = new TbMsgDelayNodeConfiguration().defaultConfiguration();
    }

    private void invokeTellSelf(int maxNumberOfInvocation) {
        AtomicLong scheduleTimeout = new AtomicLong(delayPeriod);
        AtomicInteger scheduleCount = new AtomicInteger(0);
        doAnswer((Answer<Void>) invocationOnMock -> {
            scheduleCount.getAndIncrement();
            executorService.schedule(() -> {
                if (scheduleCount.get() <= maxNumberOfInvocation) {
                    TbMsg msg = (TbMsg) (invocationOnMock.getArguments())[0];
                    node.onMsg(ctx, msg);
                    awaitTellSelfLatch.countDown();
                }
            }, scheduleTimeout.get(), TimeUnit.SECONDS);
            return null;
        }).when(ctx).tellSelf(ArgumentMatchers.any(TbMsg.class), ArgumentMatchers.anyLong());
    }

    @AfterEach
    public void destroy() {
        executorService.shutdown();
        node.destroy();
    }

    @Test
    public void given_100_messages_then_verifyOutput() throws TbNodeException, ExecutionException, InterruptedException {
        int msgCount = 5;
        awaitTellSelfLatch = new CountDownLatch(msgCount);
        invokeTellSelf(msgCount);

        config.setPeriodInSeconds(delayPeriod);
        config.setMaxPendingMsgs(msgCount);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        DeviceId deviceId = new DeviceId(UUID.randomUUID());

        List<TbMsg> inputMsgs = getTbMsgs(deviceId, msgCount);
        for (TbMsg msg : inputMsgs) {
            node.onMsg(ctx, msg);
        }
        TbMsg msgToReject = createMsg(deviceId);
        node.onMsg(ctx, msgToReject);

        awaitTellSelfLatch.await();

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Runnable> successCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Consumer<Throwable>> failureCaptor = ArgumentCaptor.forClass(Consumer.class);

        verify(ruleNodeCacheService, times(1)).getEntityIds(anyString());
        verify(ruleNodeCacheService, times(1)).add(eq(DELAYED_ORIGINATOR_IDS_CACHE_KEY), eq(deviceId));
        verify(ruleNodeCacheService, times(msgCount)).add(eq(deviceId.toString()), any(), any(TbMsg.class));

        verify(node, times(msgCount * 2 + 1)).onMsg(eq(ctx), any());

        verify(ctx, times(msgCount)).ack(any());
        verify(ctx, times(1)).tellFailure(eq(msgToReject), any());
        verify(ctx, times(msgCount)).enqueueForTellNext(newMsgCaptor.capture(), eq(TbRelationTypes.SUCCESS), successCaptor.capture(), failureCaptor.capture());

        for (Runnable valueCaptor : successCaptor.getAllValues()) {
            valueCaptor.run();
        }
        verify(ruleNodeCacheService, times(msgCount)).removeTbMsgList(eq(deviceId.toString()), any(), anyList());

        List<ByteString> expectedMsgs = inputMsgs.stream().map(TbMsg::toByteString).collect(Collectors.toList());
        List<ByteString> actualMsgs = newMsgCaptor.getAllValues().stream().map(TbMsg::toByteString).collect(Collectors.toList());
        Assertions.assertEquals(expectedMsgs, actualMsgs);

        node.destroy(ctx, ComponentLifecycleEvent.DELETED);
        verify(ruleNodeCacheService, times(1)).evictTbMsgs(anyString(), any());
        verify(ruleNodeCacheService, times(1)).evict(eq(DELAYED_ORIGINATOR_IDS_CACHE_KEY));

    }

    private List<TbMsg> getTbMsgs(DeviceId deviceId, int msgCount) {
        List<TbMsg> inputMsgs = new ArrayList<>();
        for (int i = 0; i < msgCount; i++) {
            inputMsgs.add(createMsg(deviceId));
        }
        return inputMsgs;
    }

    private TbMsg createMsg(DeviceId deviceId) {
        ObjectNode dataNode = JacksonUtil.newObjectNode();
        dataNode.put("deviceId", deviceId.getId().toString());
        TbMsgMetaData metaData = new TbMsgMetaData();
        return TbMsg.newMsg(
                DataConstants.MAIN_QUEUE_NAME,
                SessionMsgType.POST_TELEMETRY_REQUEST.name(),
                deviceId,
                metaData,
                JacksonUtil.toString(dataNode));
    }

}
