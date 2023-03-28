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
package org.thingsboard.rule.engine.cache;

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
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.TbRelationTypes;
import org.thingsboard.rule.engine.delay.TbMsgDelayNode;
import org.thingsboard.rule.engine.delay.TbMsgDelayNodeConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
public class TbMsgDelayNodeTest extends TbAbstractCacheBasedRuleNodeTest<TbMsgDelayNode, TbMsgDelayNodeConfiguration> {

    private static final String TB_MSG_DELAY_NODE_MSG = "TbMsgDelayNodeMsg";
    private static final String DELAYED_ORIGINATOR_IDS_CACHE_KEY = "delayed_originator_ids";
    private static final String TEST_THREAD_FACTORY = "delay-node-test";
    private static final int delayPeriod = 1;

    @Override
    protected ThingsBoardThreadFactory getThreadFactory() {
        return ThingsBoardThreadFactory.forName(TEST_THREAD_FACTORY);
    }

    @Override
    protected String getTickMsgType() {
        return TB_MSG_DELAY_NODE_MSG;
    }

    @Override
    protected String getEntityIdsCacheKey() {
        return DELAYED_ORIGINATOR_IDS_CACHE_KEY;
    }

    @BeforeEach
    protected void init() throws TbNodeException {
        doInit();
        node = spy(new TbMsgDelayNode());
        config = new TbMsgDelayNodeConfiguration().defaultConfiguration();
    }

    @Test
    public void given101Msgs_whenOnMsg_thenVerify100SuccessAnd1Failure() throws TbNodeException, InterruptedException, ExecutionException {
        TopicPartitionInfo tpi = mock(TopicPartitionInfo.class);

        when(tpi.getPartition()).thenReturn(Optional.of(0));
        when(tpi.isMyPartition()).thenReturn(true);
        when(ctx.getTopicPartitionInfo(any())).thenReturn(tpi);
        when(ruleNodeCacheService.getEntityIds(getEntityIdsCacheKey())).thenReturn(Collections.emptySet());
        when(ruleNodeCacheService.getTbMsgs(any(), any())).thenReturn(Collections.emptySet());

        int successMsgCount = 100;
        awaitTellSelfLatch = new CountDownLatch(successMsgCount);
        invokeTellSelf(successMsgCount);

        config.setPeriodInSeconds(delayPeriod);
        config.setMaxPendingMsgs(successMsgCount);
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        DeviceId deviceId = new DeviceId(UUID.randomUUID());

        List<TbMsg> inputMsgs = getTbMsgs(deviceId, successMsgCount);
        for (TbMsg msg : inputMsgs) {
            node.onMsg(ctx, msg);
        }
        TbMsg msgToReject = createMsg(deviceId);
        node.onMsg(ctx, msgToReject);

        awaitTellSelfLatch.await();

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Runnable> successCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Consumer<Throwable>> failureCaptor = ArgumentCaptor.forClass(Consumer.class);

        verify(tpi, times(successMsgCount * 2 + 1)).isMyPartition();
        verify(tpi, times(successMsgCount * 2 + 1)).getPartition();
        verify(node, times(successMsgCount * 2 + 1)).onMsg(eq(ctx), any());
        verify(ctx, times(successMsgCount * 2 + 1)).getTopicPartitionInfo(any());
        verify(ctx, times(successMsgCount)).ack(any());
        verify(ctx, times(1)).tellFailure(eq(msgToReject), any());
        verify(ctx, times(successMsgCount)).enqueueForTellNext(newMsgCaptor.capture(), eq(TbRelationTypes.SUCCESS), successCaptor.capture(), failureCaptor.capture());
        verify(ruleNodeCacheService, times(1)).getEntityIds(anyString());
        verify(ruleNodeCacheService, times(1)).add(eq(getEntityIdsCacheKey()), eq(deviceId));
        verify(ruleNodeCacheService, times(successMsgCount)).add(eq(deviceId), any(), any(TbMsg.class));

        for (Runnable valueCaptor : successCaptor.getAllValues()) {
            valueCaptor.run();
        }
        verify(ruleNodeCacheService, times(successMsgCount)).removeTbMsgList(eq(deviceId), any(), anyList());

        List<TbMsg> actualMsgs = newMsgCaptor.getAllValues();
        for (int i = 0; i < inputMsgs.size(); i++) {
            TbMsg inputMsg = inputMsgs.get(i);
            TbMsg actualMsg = actualMsgs.get(i);
            Assertions.assertNotEquals(inputMsg.getId(), actualMsg.getId());
            Assertions.assertEquals(inputMsg.getOriginator(), actualMsg.getOriginator());
            Assertions.assertEquals(inputMsg.getCustomerId(), actualMsg.getCustomerId());
            Assertions.assertEquals(inputMsg.getData(), actualMsg.getData());
            Assertions.assertEquals(inputMsg.getMetaData(), actualMsg.getMetaData());
            Assertions.assertEquals(inputMsg.getType(), actualMsg.getType());
        }

        node.destroy(ctx, ComponentLifecycleEvent.DELETED);
        verify(ruleNodeCacheService, times(1)).evictTbMsgs(any(), any());
        verify(ruleNodeCacheService, times(1)).evict(eq(getEntityIdsCacheKey()));

    }

    @Test
    public void given1MsgFromNonLocalEntity_whenOnMsg_thenVerifyEntityIgnored() throws TbNodeException, ExecutionException, InterruptedException {
        test_given1MsgFromNonLocalEntity_whenOnMsg_thenVerifyMsgIgnored();
    }

    @Test
    public void givenLocalEntity_whenInit_thenVerifyEnqueueForTellNext() throws TbNodeException {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());

        TopicPartitionInfo tpi = mock(TopicPartitionInfo.class);

        when(tpi.isMyPartition()).thenReturn(true);
        when(tpi.getPartition()).thenReturn(Optional.of(0));
        when(ctx.getTopicPartitionInfo(deviceId)).thenReturn(tpi);

        TbMsg msgFromCache = createMsg(deviceId);
        msgFromCache.getMetaData().putValue(ctx.getSelfId().getId().toString(), String.valueOf(System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(config.getPeriodInSeconds())));

        when(ruleNodeCacheService.getEntityIds(getEntityIdsCacheKey())).thenReturn(Collections.singleton(deviceId));
        when(ruleNodeCacheService.getTbMsgs(eq(deviceId), eq(0))).thenReturn(Collections.singleton(msgFromCache));

        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Runnable> successCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Consumer<Throwable>> failureCaptor = ArgumentCaptor.forClass(Consumer.class);

        verify(tpi, times(1)).isMyPartition();
        verify(tpi, times(1)).getPartition();
        verify(ctx, times(1)).getTopicPartitionInfo(eq(deviceId));
        verify(ctx, times(1)).enqueueForTellNext(newMsgCaptor.capture(), eq(TbRelationTypes.SUCCESS), successCaptor.capture(), failureCaptor.capture());
        verify(ruleNodeCacheService, times(1)).getEntityIds(getEntityIdsCacheKey());
        verify(ruleNodeCacheService, times(1)).getTbMsgs(eq(deviceId), eq(0));
        verify(ruleNodeCacheService, times(1)).removeTbMsgList(eq(deviceId), eq(0), eq(Collections.singletonList(msgFromCache)));

        successCaptor.getValue().run();

        TbMsg actualMsg = newMsgCaptor.getValue();
        Assertions.assertNotEquals(msgFromCache.getId(), actualMsg.getId());
        Assertions.assertEquals(msgFromCache.getOriginator(), actualMsg.getOriginator());
        Assertions.assertEquals(msgFromCache.getCustomerId(), actualMsg.getCustomerId());
        Assertions.assertEquals(msgFromCache.getData(), actualMsg.getData());
        Assertions.assertNotEquals(msgFromCache.getMetaData(), actualMsg.getMetaData());
        Assertions.assertEquals(TbMsgMetaData.EMPTY, actualMsg.getMetaData());
        Assertions.assertEquals(msgFromCache.getType(), actualMsg.getType());

        node.destroy();
    }

    @Test
    public void givenLocalEntity_whenInit_thenVerifyScheduleEnqueueForTellNext() throws TbNodeException, InterruptedException, ExecutionException {
        awaitTellSelfLatch = new CountDownLatch(1);
        invokeTellSelf(1);

        DeviceId deviceId = new DeviceId(UUID.randomUUID());

        TopicPartitionInfo tpi = mock(TopicPartitionInfo.class);

        when(tpi.isMyPartition()).thenReturn(true);
        when(tpi.getPartition()).thenReturn(Optional.of(0));
        when(ctx.getTopicPartitionInfo(deviceId)).thenReturn(tpi);

        TbMsg msgFromCache = createMsg(deviceId);
        msgFromCache.getMetaData().putValue(ctx.getSelfId().getId().toString(), String.valueOf(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(config.getPeriodInSeconds())));

        when(ruleNodeCacheService.getEntityIds(getEntityIdsCacheKey())).thenReturn(Collections.singleton(deviceId));
        when(ruleNodeCacheService.getTbMsgs(eq(deviceId), eq(0))).thenReturn(Collections.singleton(msgFromCache));

        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
        awaitTellSelfLatch.await();

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Runnable> successCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Consumer<Throwable>> failureCaptor = ArgumentCaptor.forClass(Consumer.class);

        verify(tpi, times(2)).isMyPartition();
        verify(tpi, times(2)).getPartition();
        verify(node, times(1)).onMsg(eq(ctx), any());
        verify(ctx, times(1)).tellSelf(any(), anyLong());
        verify(ctx, times(2)).getTopicPartitionInfo(eq(deviceId));
        verify(ctx, times(1)).enqueueForTellNext(newMsgCaptor.capture(), eq(TbRelationTypes.SUCCESS), successCaptor.capture(), failureCaptor.capture());
        verify(ruleNodeCacheService, times(1)).getEntityIds(getEntityIdsCacheKey());
        verify(ruleNodeCacheService, times(1)).getTbMsgs(eq(deviceId), eq(0));
        verify(ruleNodeCacheService, times(1)).removeTbMsgList(eq(deviceId), eq(0), eq(Collections.singletonList(msgFromCache)));

        successCaptor.getValue().run();

        TbMsg actualMsg = newMsgCaptor.getValue();
        Assertions.assertNotEquals(msgFromCache.getId(), actualMsg.getId());
        Assertions.assertEquals(msgFromCache.getOriginator(), actualMsg.getOriginator());
        Assertions.assertEquals(msgFromCache.getCustomerId(), actualMsg.getCustomerId());
        Assertions.assertEquals(msgFromCache.getData(), actualMsg.getData());
        Assertions.assertNotEquals(msgFromCache.getMetaData(), actualMsg.getMetaData());
        Assertions.assertEquals(TbMsgMetaData.EMPTY, actualMsg.getMetaData());
        Assertions.assertEquals(msgFromCache.getType(), actualMsg.getType());

        node.destroy();
    }

    @Test
    public void givenNonLocalEntity_whenInit_thenVerifyEntityIgnored() throws TbNodeException {
        test_givenNonLocalEntity_whenInit_thenVerifyEntityIgnored();
    }

    @Test
    public void givenLocalEntityWithNoValuesInCache_whenInit_thenVerifyDoNothing() throws TbNodeException {
        test_givenLocalEntityWithNoValuesInCache_whenInit_thenVerifyDoNothing();
    }

    @Test
    public void givenPartitionThatAlreadyExists_whenOnPartitionChange_thenVerifyCheckCacheSkipped() throws TbNodeException {
        test_givenPartitionThatAlreadyExists_whenOnPartitionChange_thenVerifyCheckCacheSkipped();
    }

    @Test
    public void givenNoEntitiesInCache_whenOnPartitionChange_thenVerifyDoNothing() {
        test_givenNoEntitiesInCache_whenOnPartitionChange_thenVerifyDoNothing();
    }

    @Test
    public void givenLocalEntityWithNoValuesInCache_whenOnPartitionChange_thenVerifyDoNothing() {
        test_givenLocalEntityWithNoValuesInCache_whenOnPartitionChange_thenVerifyDoNothing();
    }

    @Test
    public void givenLocalEntity_whenOnPartitionChange_thenVerifyEnqueueForTellNext() {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());

        TopicPartitionInfo tpi = mock(TopicPartitionInfo.class);

        when(tpi.isMyPartition()).thenReturn(true);
        when(tpi.getPartition()).thenReturn(Optional.of(0));
        when(ctx.getTopicPartitionInfo(deviceId)).thenReturn(tpi);

        TbMsg msgFromCache = createMsg(deviceId);
        msgFromCache.getMetaData().putValue(ctx.getSelfId().getId().toString(), String.valueOf(System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(config.getPeriodInSeconds())));

        when(ruleNodeCacheService.getEntityIds(eq(getEntityIdsCacheKey()))).thenReturn(Collections.singleton(deviceId));
        when(ruleNodeCacheService.getTbMsgs(eq(deviceId), eq(0))).thenReturn(Collections.singleton(msgFromCache));

        Set<TopicPartitionInfo> topicPartitionInfoSet = new HashSet<>();
        topicPartitionInfoSet.add(tpi);
        PartitionChangeMsg partitionChangeMsg = new PartitionChangeMsg(ServiceType.TB_RULE_ENGINE, topicPartitionInfoSet);

        node.onPartitionChangeMsg(ctx, partitionChangeMsg);

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Runnable> successCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Consumer<Throwable>> failureCaptor = ArgumentCaptor.forClass(Consumer.class);

        verify(tpi, times(1)).isMyPartition();
        verify(tpi, times(2)).getPartition();
        verify(ctx, times(1)).getTopicPartitionInfo(eq(deviceId));
        verify(ctx, times(1)).enqueueForTellNext(newMsgCaptor.capture(), eq(TbRelationTypes.SUCCESS), successCaptor.capture(), failureCaptor.capture());
        verify(ruleNodeCacheService, times(1)).getEntityIds(getEntityIdsCacheKey());
        verify(ruleNodeCacheService, times(1)).getTbMsgs(eq(deviceId), eq(0));
        verify(ruleNodeCacheService, times(1)).removeTbMsgList(eq(deviceId), eq(0), eq(Collections.singletonList(msgFromCache)));

        successCaptor.getValue().run();

        TbMsg actualMsg = newMsgCaptor.getValue();
        Assertions.assertNotEquals(msgFromCache.getId(), actualMsg.getId());
        Assertions.assertEquals(msgFromCache.getOriginator(), actualMsg.getOriginator());
        Assertions.assertEquals(msgFromCache.getCustomerId(), actualMsg.getCustomerId());
        Assertions.assertEquals(msgFromCache.getData(), actualMsg.getData());
        Assertions.assertNotEquals(msgFromCache.getMetaData(), actualMsg.getMetaData());
        Assertions.assertEquals(TbMsgMetaData.EMPTY, actualMsg.getMetaData());
        Assertions.assertEquals(msgFromCache.getType(), actualMsg.getType());

        node.destroy();
    }

    @Test
    public void givenLocalEntity_whenOnPartitionChange_thenVerifyScheduleEnqueueForTellNext() throws InterruptedException, TbNodeException, ExecutionException {
        awaitTellSelfLatch = new CountDownLatch(1);
        invokeTellSelf(1);

        DeviceId deviceId = new DeviceId(UUID.randomUUID());

        TopicPartitionInfo tpi = mock(TopicPartitionInfo.class);

        when(tpi.isMyPartition()).thenReturn(true);
        when(tpi.getPartition()).thenReturn(Optional.of(0));
        when(ctx.getTopicPartitionInfo(deviceId)).thenReturn(tpi);

        TbMsg msgFromCache = createMsg(deviceId);
        msgFromCache.getMetaData().putValue(ctx.getSelfId().getId().toString(), String.valueOf(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(config.getPeriodInSeconds())));

        when(ruleNodeCacheService.getEntityIds(eq(getEntityIdsCacheKey()))).thenReturn(Collections.singleton(deviceId));
        when(ruleNodeCacheService.getTbMsgs(eq(deviceId), eq(0))).thenReturn(Collections.singleton(msgFromCache));

        Set<TopicPartitionInfo> topicPartitionInfoSet = new HashSet<>();
        topicPartitionInfoSet.add(tpi);
        PartitionChangeMsg partitionChangeMsg = new PartitionChangeMsg(ServiceType.TB_RULE_ENGINE, topicPartitionInfoSet);

        node.onPartitionChangeMsg(ctx, partitionChangeMsg);
        awaitTellSelfLatch.await();

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Runnable> successCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Consumer<Throwable>> failureCaptor = ArgumentCaptor.forClass(Consumer.class);

        verify(tpi, times(2)).isMyPartition();
        verify(tpi, times(3)).getPartition();
        verify(node, times(1)).onMsg(eq(ctx), any());
        verify(ctx, times(1)).tellSelf(any(), anyLong());
        verify(ctx, times(2)).getTopicPartitionInfo(eq(deviceId));
        verify(ctx, times(1)).enqueueForTellNext(newMsgCaptor.capture(), eq(TbRelationTypes.SUCCESS), successCaptor.capture(), failureCaptor.capture());
        verify(ruleNodeCacheService, times(1)).getEntityIds(getEntityIdsCacheKey());
        verify(ruleNodeCacheService, times(1)).getTbMsgs(eq(deviceId), eq(0));
        verify(ruleNodeCacheService, times(1)).removeTbMsgList(eq(deviceId), eq(0), eq(Collections.singletonList(msgFromCache)));

        successCaptor.getValue().run();

        TbMsg actualMsg = newMsgCaptor.getValue();
        Assertions.assertNotEquals(msgFromCache.getId(), actualMsg.getId());
        Assertions.assertEquals(msgFromCache.getOriginator(), actualMsg.getOriginator());
        Assertions.assertEquals(msgFromCache.getCustomerId(), actualMsg.getCustomerId());
        Assertions.assertEquals(msgFromCache.getData(), actualMsg.getData());
        Assertions.assertNotEquals(msgFromCache.getMetaData(), actualMsg.getMetaData());
        Assertions.assertEquals(TbMsgMetaData.EMPTY, actualMsg.getMetaData());
        Assertions.assertEquals(msgFromCache.getType(), actualMsg.getType());

        node.destroy();
    }

    @AfterEach
    public void destroy() {
        executorService.shutdown();
    }

    private void invokeTellSelf(int maxNumberOfInvocation) {
        AtomicLong scheduleTimeout = new AtomicLong(delayPeriod);
        AtomicInteger scheduleCount = new AtomicInteger(0);
        doAnswer((Answer<Void>) invocationOnMock -> {
            scheduleCount.getAndIncrement();
            executorService.schedule(() -> {
                if (scheduleCount.get() <= maxNumberOfInvocation) {
                    TbMsg msg = (TbMsg) (invocationOnMock.getArguments())[0];
                    try {
                        node.onMsg(ctx, msg);
                    } catch (ExecutionException | InterruptedException | TbNodeException e) {
                        log.error("Failed to process onMsg method on tellSelf invocation due to : ", e);
                    }
                    awaitTellSelfLatch.countDown();
                }
            }, scheduleTimeout.get(), TimeUnit.SECONDS);
            return null;
        }).when(ctx).tellSelf(ArgumentMatchers.any(TbMsg.class), ArgumentMatchers.anyLong());
    }

    private List<TbMsg> getTbMsgs(DeviceId deviceId, int msgCount) {
        List<TbMsg> inputMsgs = new ArrayList<>();
        for (int i = 0; i < msgCount; i++) {
            inputMsgs.add(createMsg(deviceId));
        }
        return inputMsgs;
    }

}
