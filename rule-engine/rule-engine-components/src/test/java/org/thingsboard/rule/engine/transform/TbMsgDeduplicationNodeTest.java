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
package org.thingsboard.rule.engine.transform;

import com.fasterxml.jackson.databind.node.ArrayNode;
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
import org.thingsboard.rule.engine.deduplication.DeduplicationStrategy;
import org.thingsboard.rule.engine.deduplication.TbMsgDeduplicationNode;
import org.thingsboard.rule.engine.deduplication.TbMsgDeduplicationNodeConfiguration;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.session.SessionMsgType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
public class TbMsgDeduplicationNodeTest {

    private static final String TB_MSG_DEDUPLICATION_TIMEOUT_MSG = "TbMsgDeduplicationNodeMsg";
    private static final String DEDUPLICATION_IDS_CACHE_KEY = "deduplication_ids";

    private TbContext ctx;

    private final ThingsBoardThreadFactory factory = ThingsBoardThreadFactory.forName("deduplication-node-test");
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(factory);
    private final int deduplicationInterval = 1;

    private TenantId tenantId;

    private TbMsgDeduplicationNode node;
    private TbMsgDeduplicationNodeConfiguration config;
    private TbNodeConfiguration nodeConfiguration;
    private RuleNodeCacheService ruleNodeCacheService;

    private CountDownLatch awaitTellSelfLatch;

    @BeforeEach
    public void init() throws TbNodeException {
        ctx = mock(TbContext.class);
        ruleNodeCacheService = mock(RuleNodeCacheService.class);

        tenantId = TenantId.fromUUID(UUID.randomUUID());
        RuleNodeId ruleNodeId = new RuleNodeId(UUID.randomUUID());

        when(ctx.getSelfId()).thenReturn(ruleNodeId);
        when(ctx.getTenantId()).thenReturn(tenantId);
        when(ctx.getRuleNodeCacheService()).thenReturn(ruleNodeCacheService);
        when(ruleNodeCacheService.getStrings(anyString())).thenReturn(Collections.emptySet());
        when(ruleNodeCacheService.getEntityIds(anyString())).thenReturn(Collections.emptySet());
        when(ruleNodeCacheService.getTbMsgs(anyString())).thenReturn(Collections.emptySet());

        doAnswer((Answer<TbMsg>) invocationOnMock -> {
            String type = (String) (invocationOnMock.getArguments())[1];
            EntityId originator = (EntityId) (invocationOnMock.getArguments())[2];
            TbMsgMetaData metaData = (TbMsgMetaData) (invocationOnMock.getArguments())[3];
            String data = (String) (invocationOnMock.getArguments())[4];
            return TbMsg.newMsg(type, originator, metaData.copy(), data);
        }).when(ctx).newMsg(isNull(), eq(TB_MSG_DEDUPLICATION_TIMEOUT_MSG), nullable(EntityId.class), any(TbMsgMetaData.class), any(String.class));
        node = spy(new TbMsgDeduplicationNode());
        config = new TbMsgDeduplicationNodeConfiguration().defaultConfiguration();
    }

    private void invokeTellSelf(int maxNumberOfInvocation) {
        invokeTellSelf(maxNumberOfInvocation, false, 0);
    }

    private void invokeTellSelf(int maxNumberOfInvocation, boolean delayScheduleTimeout, int delayMultiplier) {
        AtomicLong scheduleTimeout = new AtomicLong(deduplicationInterval);
        AtomicInteger scheduleCount = new AtomicInteger(0);
        doAnswer((Answer<Void>) invocationOnMock -> {
            scheduleCount.getAndIncrement();
            if (scheduleCount.get() <= maxNumberOfInvocation) {
                TbMsg msg = (TbMsg) (invocationOnMock.getArguments())[0];
                executorService.schedule(() -> {
                    try {
                        node.onMsg(ctx, msg);
                        awaitTellSelfLatch.countDown();
                    } catch (ExecutionException | InterruptedException | TbNodeException e) {
                        log.error("Failed to execute tellSelf method call due to: ", e);
                    }
                }, scheduleTimeout.get(), TimeUnit.SECONDS);
                if (delayScheduleTimeout) {
                    scheduleTimeout.set(scheduleTimeout.get() * delayMultiplier);
                }
            }

            return null;
        }).when(ctx).tellSelf(ArgumentMatchers.any(TbMsg.class), ArgumentMatchers.anyLong());
    }

    @AfterEach
    public void destroy() {
        executorService.shutdown();
        node.destroy();
    }

    @Test
    public void given_100_messages_strategy_first_then_verifyOutput() throws TbNodeException, ExecutionException, InterruptedException {
        int wantedNumberOfTellSelfInvocation = 2;
        int msgCount = 100;
        awaitTellSelfLatch = new CountDownLatch(wantedNumberOfTellSelfInvocation);
        invokeTellSelf(wantedNumberOfTellSelfInvocation);

        config.setInterval(deduplicationInterval);
        config.setMaxPendingMsgs(msgCount);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        long currentTimeMillis = System.currentTimeMillis();

        List<TbMsg> inputMsgs = getTbMsgs(deviceId, msgCount, currentTimeMillis, 500);
        for (TbMsg msg : inputMsgs) {
            node.onMsg(ctx, msg);
        }

        TbMsg msgToReject = createMsg(deviceId, inputMsgs.get(inputMsgs.size() - 1).getMetaDataTs() + 2);
        node.onMsg(ctx, msgToReject);

        awaitTellSelfLatch.await();

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Runnable> successCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Consumer<Throwable>> failureCaptor = ArgumentCaptor.forClass(Consumer.class);

        verify(ruleNodeCacheService, times(1)).getEntityIds(anyString());
        verify(ruleNodeCacheService, times(1)).add(eq(DEDUPLICATION_IDS_CACHE_KEY), eq(deviceId));
        verify(ruleNodeCacheService, times(msgCount)).add(eq(deviceId.toString()), any(TbMsg.class));

        verify(node, times(msgCount + wantedNumberOfTellSelfInvocation + 1)).onMsg(eq(ctx), any());

        verify(ctx, times(msgCount)).ack(any());
        verify(ctx, times(1)).tellFailure(eq(msgToReject), any());
        verify(ctx, times(1)).enqueueForTellNext(newMsgCaptor.capture(), eq(TbRelationTypes.SUCCESS), successCaptor.capture(), failureCaptor.capture());

        for (Runnable valueCaptor : successCaptor.getAllValues()) {
            valueCaptor.run();
        }
        verify(ruleNodeCacheService, times(1)).removeTbMsgList(eq(deviceId.toString()), anyList());
        verify(ctx, never()).schedule(any(), anyLong(), any());

        Assertions.assertEquals(TbMsg.toByteString(inputMsgs.get(0)), TbMsg.toByteString(newMsgCaptor.getValue()));

        node.destroy(ctx, ComponentLifecycleEvent.DELETED);
        verify(ruleNodeCacheService, times(2)).evict(anyString());

    }

    @Test
    public void given_100_messages_strategy_last_then_verifyOutput() throws TbNodeException, ExecutionException, InterruptedException {
        int wantedNumberOfTellSelfInvocation = 2;
        int msgCount = 100;
        awaitTellSelfLatch = new CountDownLatch(wantedNumberOfTellSelfInvocation);
        invokeTellSelf(wantedNumberOfTellSelfInvocation);

        config.setStrategy(DeduplicationStrategy.LAST);
        config.setInterval(deduplicationInterval);
        config.setMaxPendingMsgs(msgCount);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        long currentTimeMillis = System.currentTimeMillis();

        List<TbMsg> inputMsgs = getTbMsgs(deviceId, msgCount, currentTimeMillis, 500);
        TbMsg msgWithLatestTs = getMsgWithLatestTs(inputMsgs);

        for (TbMsg msg : inputMsgs) {
            node.onMsg(ctx, msg);
        }

        TbMsg msgToReject = createMsg(deviceId, inputMsgs.get(inputMsgs.size() - 1).getMetaDataTs() + 2);
        node.onMsg(ctx, msgToReject);

        awaitTellSelfLatch.await();

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Runnable> successCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Consumer<Throwable>> failureCaptor = ArgumentCaptor.forClass(Consumer.class);

        verify(ruleNodeCacheService, times(1)).getEntityIds(anyString());
        verify(ruleNodeCacheService, times(1)).add(eq(DEDUPLICATION_IDS_CACHE_KEY), eq(deviceId));
        verify(ruleNodeCacheService, times(msgCount)).add(eq(deviceId.toString()), any(TbMsg.class));

        verify(node, times(msgCount + wantedNumberOfTellSelfInvocation + 1)).onMsg(eq(ctx), any());

        verify(ctx, times(msgCount)).ack(any());
        verify(ctx, times(1)).tellFailure(eq(msgToReject), any());
        verify(ctx, times(1)).enqueueForTellNext(newMsgCaptor.capture(), eq(TbRelationTypes.SUCCESS), successCaptor.capture(), failureCaptor.capture());

        for (Runnable valueCaptor : successCaptor.getAllValues()) {
            valueCaptor.run();
        }
        verify(ruleNodeCacheService, times(1)).removeTbMsgList(eq(deviceId.toString()), anyList());
        verify(ctx, never()).schedule(any(), anyLong(), any());

        Assertions.assertEquals(TbMsg.toByteString(msgWithLatestTs), TbMsg.toByteString(newMsgCaptor.getValue()));

        node.destroy(ctx, ComponentLifecycleEvent.DELETED);
        verify(ruleNodeCacheService, times(2)).evict(anyString());
    }

    @Test
    public void given_100_messages_strategy_all_then_verifyOutput() throws TbNodeException, ExecutionException, InterruptedException {
        int wantedNumberOfTellSelfInvocation = 2;
        int msgCount = 100;
        awaitTellSelfLatch = new CountDownLatch(wantedNumberOfTellSelfInvocation);
        invokeTellSelf(wantedNumberOfTellSelfInvocation);

        config.setInterval(deduplicationInterval);
        config.setStrategy(DeduplicationStrategy.ALL);
        config.setOutMsgType(SessionMsgType.POST_ATTRIBUTES_REQUEST.name());
        config.setQueueName(DataConstants.HP_QUEUE_NAME);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        long currentTimeMillis = System.currentTimeMillis();

        List<TbMsg> inputMsgs = getTbMsgs(deviceId, msgCount, currentTimeMillis, 500);
        for (TbMsg msg : inputMsgs) {
            node.onMsg(ctx, msg);
        }

        awaitTellSelfLatch.await();

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Runnable> successCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Consumer<Throwable>> failureCaptor = ArgumentCaptor.forClass(Consumer.class);

        verify(ruleNodeCacheService, times(1)).getEntityIds(anyString());
        verify(ruleNodeCacheService, times(1)).add(eq(DEDUPLICATION_IDS_CACHE_KEY), eq(deviceId));
        verify(ruleNodeCacheService, times(msgCount)).add(eq(deviceId.toString()), any(TbMsg.class));

        verify(node, times(msgCount + wantedNumberOfTellSelfInvocation)).onMsg(eq(ctx), any());

        verify(ctx, times(msgCount)).ack(any());
        verify(ctx, times(1)).enqueueForTellNext(newMsgCaptor.capture(), eq(TbRelationTypes.SUCCESS), successCaptor.capture(), failureCaptor.capture());

        for (Runnable valueCaptor : successCaptor.getAllValues()) {
            valueCaptor.run();
        }
        verify(ruleNodeCacheService, times(1)).removeTbMsgList(eq(deviceId.toString()), anyList());
        verify(ctx, never()).schedule(any(), anyLong(), any());

        Assertions.assertEquals(1, newMsgCaptor.getAllValues().size());
        TbMsg outMessage = newMsgCaptor.getAllValues().get(0);
        Assertions.assertEquals(getMergedData(inputMsgs), outMessage.getData());
        Assertions.assertEquals(deviceId, outMessage.getOriginator());
        Assertions.assertEquals(config.getOutMsgType(), outMessage.getType());
        Assertions.assertEquals(config.getQueueName(), outMessage.getQueueName());

        node.destroy(ctx, ComponentLifecycleEvent.DELETED);
        verify(ruleNodeCacheService, times(2)).evict(anyString());
    }

    @Test
    public void given_100_messages_strategy_all_then_verifyOutput_2_packs() throws TbNodeException, ExecutionException, InterruptedException {
        int wantedNumberOfTellSelfInvocation = 2;
        int msgCount = 100;
        awaitTellSelfLatch = new CountDownLatch(wantedNumberOfTellSelfInvocation);
        invokeTellSelf(wantedNumberOfTellSelfInvocation, true, 3);

        config.setInterval(deduplicationInterval);
        config.setStrategy(DeduplicationStrategy.ALL);
        config.setOutMsgType(SessionMsgType.POST_ATTRIBUTES_REQUEST.name());
        config.setQueueName(DataConstants.HP_QUEUE_NAME);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        long currentTimeMillis = System.currentTimeMillis();

        List<TbMsg> firstMsgPack = getTbMsgs(deviceId, msgCount / 2, currentTimeMillis, 500);
        for (TbMsg msg : firstMsgPack) {
            node.onMsg(ctx, msg);
        }
        long firstPackDeduplicationPackEndTs = firstMsgPack.get(0).getMetaDataTs() + TimeUnit.SECONDS.toMillis(deduplicationInterval);

        List<TbMsg> secondMsgPack = getTbMsgs(deviceId, msgCount / 2, firstPackDeduplicationPackEndTs, 500);
        for (TbMsg msg : secondMsgPack) {
            node.onMsg(ctx, msg);
        }

        awaitTellSelfLatch.await();

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Runnable> successCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Consumer<Throwable>> failureCaptor = ArgumentCaptor.forClass(Consumer.class);

        verify(ruleNodeCacheService, times(1)).getEntityIds(anyString());
        verify(ruleNodeCacheService, times(1)).add(eq(DEDUPLICATION_IDS_CACHE_KEY), eq(deviceId));
        verify(ruleNodeCacheService, times(msgCount)).add(eq(deviceId.toString()), any(TbMsg.class));

        verify(node, times(msgCount + wantedNumberOfTellSelfInvocation)).onMsg(eq(ctx), any());

        verify(ctx, times(msgCount)).ack(any());
        verify(ctx, times(2)).enqueueForTellNext(newMsgCaptor.capture(), eq(TbRelationTypes.SUCCESS), successCaptor.capture(), failureCaptor.capture());

        for (Runnable valueCaptor : successCaptor.getAllValues()) {
            valueCaptor.run();
        }
        verify(ruleNodeCacheService, times(2)).removeTbMsgList(eq(deviceId.toString()), anyList());
        verify(ctx, never()).schedule(any(), anyLong(), any());

        List<TbMsg> resultMsgs = newMsgCaptor.getAllValues();
        Assertions.assertEquals(2, resultMsgs.size());

        TbMsg firstMsg = resultMsgs.get(0);
        Assertions.assertEquals(getMergedData(firstMsgPack), firstMsg.getData());
        Assertions.assertEquals(deviceId, firstMsg.getOriginator());
        Assertions.assertEquals(config.getOutMsgType(), firstMsg.getType());
        Assertions.assertEquals(config.getQueueName(), firstMsg.getQueueName());

        TbMsg secondMsg = resultMsgs.get(1);
        Assertions.assertEquals(getMergedData(secondMsgPack), secondMsg.getData());
        Assertions.assertEquals(deviceId, secondMsg.getOriginator());
        Assertions.assertEquals(config.getOutMsgType(), secondMsg.getType());
        Assertions.assertEquals(config.getQueueName(), secondMsg.getQueueName());

        node.destroy(ctx, ComponentLifecycleEvent.DELETED);
        verify(ruleNodeCacheService, times(2)).evict(anyString());
    }

    @Test
    public void given_100_messages_strategy_last_then_verifyOutput_2_packs() throws TbNodeException, ExecutionException, InterruptedException {
        int wantedNumberOfTellSelfInvocation = 2;
        int msgCount = 100;
        awaitTellSelfLatch = new CountDownLatch(wantedNumberOfTellSelfInvocation);
        invokeTellSelf(wantedNumberOfTellSelfInvocation, true, 3);

        config.setInterval(deduplicationInterval);
        config.setStrategy(DeduplicationStrategy.LAST);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        long currentTimeMillis = System.currentTimeMillis();

        List<TbMsg> firstMsgPack = getTbMsgs(deviceId, msgCount / 2, currentTimeMillis, 500);
        for (TbMsg msg : firstMsgPack) {
            node.onMsg(ctx, msg);
        }
        long firstPackDeduplicationPackEndTs = firstMsgPack.get(0).getMetaDataTs() + TimeUnit.SECONDS.toMillis(deduplicationInterval);
        TbMsg msgWithLatestTsInFirstPack = getMsgWithLatestTs(firstMsgPack);

        List<TbMsg> secondMsgPack = getTbMsgs(deviceId, msgCount / 2, firstPackDeduplicationPackEndTs, 500);
        for (TbMsg msg : secondMsgPack) {
            node.onMsg(ctx, msg);
        }
        TbMsg msgWithLatestTsInSecondPack = getMsgWithLatestTs(secondMsgPack);

        awaitTellSelfLatch.await();

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Runnable> successCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Consumer<Throwable>> failureCaptor = ArgumentCaptor.forClass(Consumer.class);

        verify(ruleNodeCacheService, times(1)).getEntityIds(anyString());
        verify(ruleNodeCacheService, times(1)).add(eq(DEDUPLICATION_IDS_CACHE_KEY), eq(deviceId));
        verify(ruleNodeCacheService, times(msgCount)).add(eq(deviceId.toString()), any(TbMsg.class));

        verify(node, times(msgCount + wantedNumberOfTellSelfInvocation)).onMsg(eq(ctx), any());

        verify(ctx, times(msgCount)).ack(any());
        verify(ctx, times(2)).enqueueForTellNext(newMsgCaptor.capture(), eq(TbRelationTypes.SUCCESS), successCaptor.capture(), failureCaptor.capture());

        for (Runnable valueCaptor : successCaptor.getAllValues()) {
            valueCaptor.run();
        }
        verify(ruleNodeCacheService, times(2)).removeTbMsgList(eq(deviceId.toString()), anyList());
        verify(ctx, never()).schedule(any(), anyLong(), any());

        List<TbMsg> resultMsgs = newMsgCaptor.getAllValues();
        Assertions.assertEquals(2, resultMsgs.size());
        List<ByteString> resultMsgsByteStrings = resultMsgs.stream().map(TbMsg::toByteString).collect(Collectors.toList());
        Assertions.assertTrue(resultMsgsByteStrings.contains(TbMsg.toByteString(msgWithLatestTsInFirstPack)));
        Assertions.assertTrue(resultMsgsByteStrings.contains(TbMsg.toByteString(msgWithLatestTsInSecondPack)));

        node.destroy(ctx, ComponentLifecycleEvent.DELETED);
        verify(ruleNodeCacheService, times(2)).evict(anyString());
    }

    private TbMsg getMsgWithLatestTs(List<TbMsg> firstMsgPack) {
        int indexOfLastMsgInArray = firstMsgPack.size() - 1;
        int indexToSetMaxTs = new Random().nextInt(indexOfLastMsgInArray) + 1;
        TbMsg currentMaxTsMsg = firstMsgPack.get(indexOfLastMsgInArray);
        TbMsg newLastMsgOfArray = firstMsgPack.get(indexToSetMaxTs);
        firstMsgPack.set(indexOfLastMsgInArray, newLastMsgOfArray);
        firstMsgPack.set(indexToSetMaxTs, currentMaxTsMsg);
        return currentMaxTsMsg;
    }

    private List<TbMsg> getTbMsgs(DeviceId deviceId, int msgCount, long currentTimeMillis, int initTsStep) {
        List<TbMsg> inputMsgs = new ArrayList<>();
        var ts = currentTimeMillis + initTsStep;
        for (int i = 0; i < msgCount; i++) {
            inputMsgs.add(createMsg(deviceId, ts));
            ts += 2;
        }
        return inputMsgs;
    }

    private TbMsg createMsg(DeviceId deviceId, long ts) {
        ObjectNode dataNode = JacksonUtil.newObjectNode();
        dataNode.put("deviceId", deviceId.getId().toString());
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("ts", String.valueOf(ts));
        return TbMsg.newMsg(
                DataConstants.MAIN_QUEUE_NAME,
                SessionMsgType.POST_TELEMETRY_REQUEST.name(),
                deviceId,
                metaData,
                JacksonUtil.toString(dataNode));
    }

    private String getMergedData(List<TbMsg> msgs) {
        ArrayNode mergedData = JacksonUtil.OBJECT_MAPPER.createArrayNode();
        msgs.forEach(msg -> {
            ObjectNode msgNode = JacksonUtil.newObjectNode();
            msgNode.set("msg", JacksonUtil.toJsonNode(msg.getData()));
            msgNode.set("metadata", JacksonUtil.valueToTree(msg.getMetaData().getData()));
            mergedData.add(msgNode);
        });
        return JacksonUtil.toString(mergedData);
    }

}
