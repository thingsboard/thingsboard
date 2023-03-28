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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.thingsboard.rule.engine.deduplication.DeduplicationStrategy;
import org.thingsboard.rule.engine.deduplication.TbMsgDeduplicationNode;
import org.thingsboard.rule.engine.deduplication.TbMsgDeduplicationNodeConfiguration;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.msg.session.SessionMsgType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
public class TbMsgDeduplicationNodeTest extends TbAbstractCacheBasedRuleNodeTest<TbMsgDeduplicationNode, TbMsgDeduplicationNodeConfiguration> {

    private static final String TB_MSG_DEDUPLICATION_TIMEOUT_MSG = "TbMsgDeduplicationNodeMsg";
    private static final String DEDUPLICATION_IDS_CACHE_KEY = "deduplication_ids";
    private static final String TEST_THREAD_FACTORY = "deduplication-node-test";
    private static final int deduplicationInterval = 1;

    @Override
    protected ThingsBoardThreadFactory getThreadFactory() {
        return ThingsBoardThreadFactory.forName(TEST_THREAD_FACTORY);
    }

    @Override
    protected String getTickMsgType() {
        return TB_MSG_DEDUPLICATION_TIMEOUT_MSG;
    }

    @Override
    protected String getEntityIdsCacheKey() {
        return DEDUPLICATION_IDS_CACHE_KEY;
    }

    @BeforeEach
    protected void init() throws TbNodeException {
        doInit();
        node = spy(new TbMsgDeduplicationNode());
        config = new TbMsgDeduplicationNodeConfiguration().defaultConfiguration();
    }

    @Test
    public void given1MsgFromNonLocalEntity_whenOnMsg_thenVerifyMsgIgnored() throws TbNodeException, ExecutionException, InterruptedException {
        test_given1MsgFromNonLocalEntity_whenOnMsg_thenVerifyMsgIgnored();
    }

    @Test
    public void givenNonLocalEntity_whenInit_thenVerifyEntityIgnored() throws TbNodeException {
        test_givenNonLocalEntity_whenInit_thenVerifyEntityIgnored();
    }

    @Test
    public void givenLocalEntity_whenInit_thenVerify1PackToOutput() throws TbNodeException, InterruptedException, ExecutionException {
        awaitTellSelfLatch = new CountDownLatch(1);
        invokeTellSelf(1);

        DeviceId deviceId = new DeviceId(UUID.randomUUID());

        TopicPartitionInfo tpi = mock(TopicPartitionInfo.class);

        when(tpi.isMyPartition()).thenReturn(true);
        when(tpi.getPartition()).thenReturn(Optional.of(0));
        when(ctx.getTopicPartitionInfo(deviceId)).thenReturn(tpi);

        TbMsg msgFromCache = createMsg(deviceId, System.currentTimeMillis() - 500);

        when(ruleNodeCacheService.getEntityIds(getEntityIdsCacheKey())).thenReturn(Collections.singleton(deviceId));
        when(ruleNodeCacheService.getTbMsgs(eq(deviceId), eq(0))).thenReturn(Collections.singleton(msgFromCache));

        config.setInterval(deduplicationInterval);
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

        successCaptor.getValue().run();

        verify(ruleNodeCacheService, times(1)).removeTbMsgList(eq(deviceId), eq(0), eq(Collections.singletonList(msgFromCache)));

        TbMsg actualMsg = newMsgCaptor.getValue();
        Assertions.assertNotEquals(msgFromCache.getId(), actualMsg.getId());
        Assertions.assertEquals(msgFromCache.getOriginator(), actualMsg.getOriginator());
        Assertions.assertEquals(msgFromCache.getCustomerId(), actualMsg.getCustomerId());
        Assertions.assertEquals(msgFromCache.getData(), actualMsg.getData());
        Assertions.assertEquals(msgFromCache.getMetaData(), actualMsg.getMetaData());
        Assertions.assertEquals(msgFromCache.getType(), actualMsg.getType());

        node.destroy();
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
    public void givenLocalEntity_whenOnPartitionChange_thenVerify1PackToOutput() throws TbNodeException, InterruptedException, ExecutionException {
        awaitTellSelfLatch = new CountDownLatch(1);
        invokeTellSelf(1);

        DeviceId deviceId = new DeviceId(UUID.randomUUID());

        TopicPartitionInfo tpi = mock(TopicPartitionInfo.class);

        when(ctx.getTopicPartitionInfo(deviceId)).thenReturn(tpi);

        TbMsg msgFromCache = createMsg(deviceId, System.currentTimeMillis() - 500);

        when(ruleNodeCacheService.getEntityIds(eq(getEntityIdsCacheKey()))).thenReturn(Collections.singleton(deviceId));
        when(ruleNodeCacheService.getTbMsgs(eq(deviceId), eq(0))).thenReturn(Collections.singleton(msgFromCache));
        // before init set partition to non-local
        when(tpi.isMyPartition()).thenReturn(false);

        // init node with deduplicationInterval set to 1 sec.
        config.setInterval(deduplicationInterval);
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // before onPartitionChangeMsg set partition to local
        when(tpi.isMyPartition()).thenReturn(true);
        when(tpi.getPartition()).thenReturn(Optional.of(0));

        Set<TopicPartitionInfo> topicPartitionInfoSet = new HashSet<>();
        topicPartitionInfoSet.add(tpi);
        PartitionChangeMsg partitionChangeMsg = new PartitionChangeMsg(ServiceType.TB_RULE_ENGINE, topicPartitionInfoSet);

        node.onPartitionChangeMsg(ctx, partitionChangeMsg);
        awaitTellSelfLatch.await();

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Runnable> successCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Consumer<Throwable>> failureCaptor = ArgumentCaptor.forClass(Consumer.class);

        verify(tpi, times(3)).isMyPartition();
        verify(tpi, times(3)).getPartition();
        verify(node, times(1)).onMsg(eq(ctx), any());
        verify(ctx, times(1)).tellSelf(any(), anyLong());
        verify(ctx, times(3)).getTopicPartitionInfo(eq(deviceId));
        verify(ctx, times(1)).enqueueForTellNext(newMsgCaptor.capture(), eq(TbRelationTypes.SUCCESS), successCaptor.capture(), failureCaptor.capture());
        verify(ruleNodeCacheService, times(2)).getEntityIds(getEntityIdsCacheKey());
        verify(ruleNodeCacheService, times(1)).getTbMsgs(eq(deviceId), eq(0));

        successCaptor.getValue().run();

        verify(ruleNodeCacheService, times(1)).removeTbMsgList(eq(deviceId), eq(0), eq(Collections.singletonList(msgFromCache)));

        TbMsg actualMsg = newMsgCaptor.getValue();
        Assertions.assertNotEquals(msgFromCache.getId(), actualMsg.getId());
        Assertions.assertEquals(msgFromCache.getOriginator(), actualMsg.getOriginator());
        Assertions.assertEquals(msgFromCache.getCustomerId(), actualMsg.getCustomerId());
        Assertions.assertEquals(msgFromCache.getData(), actualMsg.getData());
        Assertions.assertEquals(msgFromCache.getMetaData(), actualMsg.getMetaData());
        Assertions.assertEquals(msgFromCache.getType(), actualMsg.getType());

        node.destroy();
    }

    @Test
    public void given101MessagesStrategyFirst_thenVerify1PackToOutputAnd1MsgRejected() throws TbNodeException, ExecutionException, InterruptedException {
        TopicPartitionInfo tpi = mock(TopicPartitionInfo.class);

        when(tpi.getPartition()).thenReturn(Optional.of(0));
        when(tpi.isMyPartition()).thenReturn(true);
        when(ctx.getTopicPartitionInfo(any())).thenReturn(tpi);
        when(ruleNodeCacheService.getEntityIds(getEntityIdsCacheKey())).thenReturn(Collections.emptySet());
        when(ruleNodeCacheService.getTbMsgs(any(), any())).thenReturn(Collections.emptySet());

        int wantedNumberOfTellSelfInvocation = 2;
        int msgCount = 100;
        awaitTellSelfLatch = new CountDownLatch(wantedNumberOfTellSelfInvocation);
        invokeTellSelf(wantedNumberOfTellSelfInvocation);

        config.setInterval(deduplicationInterval);
        config.setMaxPendingMsgs(msgCount);
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

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

        verify(node, times(msgCount + wantedNumberOfTellSelfInvocation + 1)).onMsg(eq(ctx), any());
        verify(ctx, times(msgCount)).ack(any());
        verify(ctx, times(1)).tellFailure(eq(msgToReject), any());
        verify(ctx, times(1)).enqueueForTellNext(newMsgCaptor.capture(), eq(TbRelationTypes.SUCCESS), successCaptor.capture(), failureCaptor.capture());
        verify(ruleNodeCacheService, times(1)).getEntityIds(anyString());
        verify(ruleNodeCacheService, times(1)).add(eq(getEntityIdsCacheKey()), eq(deviceId));
        verify(ruleNodeCacheService, times(msgCount)).add(eq(deviceId), any(), any(TbMsg.class));

        for (Runnable valueCaptor : successCaptor.getAllValues()) {
            valueCaptor.run();
        }
        verify(ruleNodeCacheService, times(1)).removeTbMsgList(eq(deviceId), any(), anyList());
        verify(ctx, never()).schedule(any(), anyLong(), any());

        // verify that newMsg is called but content of messages is the same as in the first message.
        TbMsg msgWithExpectedContent = inputMsgs.get(0);
        TbMsg actualMsg = newMsgCaptor.getValue();
        Assertions.assertNotEquals(msgWithExpectedContent.getId(), actualMsg.getId());
        Assertions.assertEquals(msgWithExpectedContent.getOriginator(), actualMsg.getOriginator());
        Assertions.assertEquals(msgWithExpectedContent.getCustomerId(), actualMsg.getCustomerId());
        Assertions.assertEquals(msgWithExpectedContent.getData(), actualMsg.getData());
        Assertions.assertEquals(msgWithExpectedContent.getMetaData(), actualMsg.getMetaData());
        Assertions.assertEquals(msgWithExpectedContent.getType(), actualMsg.getType());

        node.destroy(ctx, ComponentLifecycleEvent.DELETED);
        verify(ruleNodeCacheService, times(1)).evictTbMsgs(any(), any());
        verify(ruleNodeCacheService, times(1)).evict(eq(getEntityIdsCacheKey()));
    }

    @Test
    public void given101MessagesStrategyLast_thenVerify1PackToOutputAnd1MsgRejected() throws TbNodeException, ExecutionException, InterruptedException {
        TopicPartitionInfo tpi = mock(TopicPartitionInfo.class);

        when(tpi.getPartition()).thenReturn(Optional.of(0));
        when(tpi.isMyPartition()).thenReturn(true);
        when(ctx.getTopicPartitionInfo(any())).thenReturn(tpi);
        when(ruleNodeCacheService.getEntityIds(getEntityIdsCacheKey())).thenReturn(Collections.emptySet());
        when(ruleNodeCacheService.getTbMsgs(any(), any())).thenReturn(Collections.emptySet());

        int wantedNumberOfTellSelfInvocation = 2;
        int msgCount = 100;
        awaitTellSelfLatch = new CountDownLatch(wantedNumberOfTellSelfInvocation);
        invokeTellSelf(wantedNumberOfTellSelfInvocation);

        config.setStrategy(DeduplicationStrategy.LAST);
        config.setInterval(deduplicationInterval);
        config.setMaxPendingMsgs(msgCount);
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

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

        verify(node, times(msgCount + wantedNumberOfTellSelfInvocation + 1)).onMsg(eq(ctx), any());
        verify(ctx, times(msgCount)).ack(any());
        verify(ctx, times(1)).tellFailure(eq(msgToReject), any());
        verify(ctx, times(1)).enqueueForTellNext(newMsgCaptor.capture(), eq(TbRelationTypes.SUCCESS), successCaptor.capture(), failureCaptor.capture());
        verify(ruleNodeCacheService, times(1)).getEntityIds(anyString());
        verify(ruleNodeCacheService, times(1)).add(eq(getEntityIdsCacheKey()), eq(deviceId));
        verify(ruleNodeCacheService, times(msgCount)).add(eq(deviceId), any(), any(TbMsg.class));

        for (Runnable valueCaptor : successCaptor.getAllValues()) {
            valueCaptor.run();
        }
        verify(ruleNodeCacheService, times(1)).removeTbMsgList(eq(deviceId), any(), anyList());
        verify(ctx, never()).schedule(any(), anyLong(), any());

        // verify that newMsg is called but content of messages is the same as in the last msg.
        TbMsg actualMsg = newMsgCaptor.getValue();
        Assertions.assertNotEquals(msgWithLatestTs.getId(), actualMsg.getId());
        Assertions.assertEquals(msgWithLatestTs.getOriginator(), actualMsg.getOriginator());
        Assertions.assertEquals(msgWithLatestTs.getCustomerId(), actualMsg.getCustomerId());
        Assertions.assertEquals(msgWithLatestTs.getData(), actualMsg.getData());
        Assertions.assertEquals(msgWithLatestTs.getMetaData(), actualMsg.getMetaData());
        Assertions.assertEquals(msgWithLatestTs.getType(), actualMsg.getType());

        node.destroy(ctx, ComponentLifecycleEvent.DELETED);
        verify(ruleNodeCacheService, times(1)).evictTbMsgs(any(), any());
        verify(ruleNodeCacheService, times(1)).evict(eq(getEntityIdsCacheKey()));
    }

    @Test
    public void given100MessagesStrategyAll_thenVerify1PackToOutput() throws TbNodeException, ExecutionException, InterruptedException {
        TopicPartitionInfo tpi = mock(TopicPartitionInfo.class);

        when(tpi.getPartition()).thenReturn(Optional.of(0));
        when(tpi.isMyPartition()).thenReturn(true);
        when(ctx.getTopicPartitionInfo(any())).thenReturn(tpi);
        when(ruleNodeCacheService.getEntityIds(getEntityIdsCacheKey())).thenReturn(Collections.emptySet());
        when(ruleNodeCacheService.getTbMsgs(any(), any())).thenReturn(Collections.emptySet());

        int wantedNumberOfTellSelfInvocation = 2;
        int msgCount = 100;
        awaitTellSelfLatch = new CountDownLatch(wantedNumberOfTellSelfInvocation);
        invokeTellSelf(wantedNumberOfTellSelfInvocation);

        config.setInterval(deduplicationInterval);
        config.setStrategy(DeduplicationStrategy.ALL);
        config.setOutMsgType(SessionMsgType.POST_ATTRIBUTES_REQUEST.name());
        config.setQueueName(DataConstants.HP_QUEUE_NAME);
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

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

        verify(node, times(msgCount + wantedNumberOfTellSelfInvocation)).onMsg(eq(ctx), any());
        verify(ctx, times(msgCount)).ack(any());
        verify(ctx, times(1)).enqueueForTellNext(newMsgCaptor.capture(), eq(TbRelationTypes.SUCCESS), successCaptor.capture(), failureCaptor.capture());
        verify(ruleNodeCacheService, times(1)).getEntityIds(anyString());
        verify(ruleNodeCacheService, times(1)).add(eq(getEntityIdsCacheKey()), eq(deviceId));
        verify(ruleNodeCacheService, times(msgCount)).add(eq(deviceId), any(), any(TbMsg.class));

        for (Runnable valueCaptor : successCaptor.getAllValues()) {
            valueCaptor.run();
        }
        verify(ruleNodeCacheService, times(1)).removeTbMsgList(eq(deviceId), any(), anyList());
        verify(ctx, never()).schedule(any(), anyLong(), any());

        Assertions.assertEquals(1, newMsgCaptor.getAllValues().size());
        TbMsg outMessage = newMsgCaptor.getAllValues().get(0);
        Assertions.assertEquals(getMergedData(inputMsgs), outMessage.getData());
        Assertions.assertEquals(deviceId, outMessage.getOriginator());
        Assertions.assertEquals(config.getOutMsgType(), outMessage.getType());
        Assertions.assertEquals(config.getQueueName(), outMessage.getQueueName());

        node.destroy(ctx, ComponentLifecycleEvent.DELETED);
        verify(ruleNodeCacheService, times(1)).evictTbMsgs(any(), any());
        verify(ruleNodeCacheService, times(1)).evict(eq(getEntityIdsCacheKey()));
    }

    @Test
    public void given100MessagesStrategyAll_thenVerify2PacksToOutput() throws TbNodeException, ExecutionException, InterruptedException {
        TopicPartitionInfo tpi = mock(TopicPartitionInfo.class);

        when(tpi.getPartition()).thenReturn(Optional.of(0));
        when(tpi.isMyPartition()).thenReturn(true);
        when(ctx.getTopicPartitionInfo(any())).thenReturn(tpi);
        when(ruleNodeCacheService.getEntityIds(getEntityIdsCacheKey())).thenReturn(Collections.emptySet());
        when(ruleNodeCacheService.getTbMsgs(any(), any())).thenReturn(Collections.emptySet());

        int wantedNumberOfTellSelfInvocation = 2;
        int msgCount = 100;
        awaitTellSelfLatch = new CountDownLatch(wantedNumberOfTellSelfInvocation);
        invokeTellSelf(wantedNumberOfTellSelfInvocation, true, 3);

        config.setInterval(deduplicationInterval);
        config.setStrategy(DeduplicationStrategy.ALL);
        config.setOutMsgType(SessionMsgType.POST_ATTRIBUTES_REQUEST.name());
        config.setQueueName(DataConstants.HP_QUEUE_NAME);
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

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

        verify(node, times(msgCount + wantedNumberOfTellSelfInvocation)).onMsg(eq(ctx), any());
        verify(ctx, times(msgCount)).ack(any());
        verify(ctx, times(2)).enqueueForTellNext(newMsgCaptor.capture(), eq(TbRelationTypes.SUCCESS), successCaptor.capture(), failureCaptor.capture());
        verify(ruleNodeCacheService, times(1)).getEntityIds(anyString());
        verify(ruleNodeCacheService, times(1)).add(eq(getEntityIdsCacheKey()), eq(deviceId));
        verify(ruleNodeCacheService, times(msgCount)).add(eq(deviceId), any(), any(TbMsg.class));

        for (Runnable valueCaptor : successCaptor.getAllValues()) {
            valueCaptor.run();
        }

        verify(ruleNodeCacheService, times(2)).removeTbMsgList(eq(deviceId), any(), anyList());
        verify(ctx, never()).schedule(any(), anyLong(), any());

        List<TbMsg> resultMsgs = newMsgCaptor.getAllValues();
        Assertions.assertEquals(2, resultMsgs.size());

        TbMsg firstOutMsg = resultMsgs.get(0);
        Assertions.assertEquals(getMergedData(firstMsgPack), firstOutMsg.getData());
        Assertions.assertEquals(deviceId, firstOutMsg.getOriginator());
        Assertions.assertEquals(config.getOutMsgType(), firstOutMsg.getType());
        Assertions.assertEquals(config.getQueueName(), firstOutMsg.getQueueName());

        TbMsg secondOutMsg = resultMsgs.get(1);
        Assertions.assertEquals(getMergedData(secondMsgPack), secondOutMsg.getData());
        Assertions.assertEquals(deviceId, secondOutMsg.getOriginator());
        Assertions.assertEquals(config.getOutMsgType(), secondOutMsg.getType());
        Assertions.assertEquals(config.getQueueName(), secondOutMsg.getQueueName());

        node.destroy(ctx, ComponentLifecycleEvent.DELETED);
        verify(ruleNodeCacheService, times(1)).evictTbMsgs(any(), any());
        verify(ruleNodeCacheService, times(1)).evict(eq(getEntityIdsCacheKey()));
    }

    @Test
    public void given100MessagesStrategyLast_thenVerify2PacksToOutput() throws TbNodeException, ExecutionException, InterruptedException {
        TopicPartitionInfo tpi = mock(TopicPartitionInfo.class);

        when(tpi.getPartition()).thenReturn(Optional.of(0));
        when(tpi.isMyPartition()).thenReturn(true);
        when(ctx.getTopicPartitionInfo(any())).thenReturn(tpi);
        when(ruleNodeCacheService.getEntityIds(getEntityIdsCacheKey())).thenReturn(Collections.emptySet());
        when(ruleNodeCacheService.getTbMsgs(any(), any())).thenReturn(Collections.emptySet());

        int wantedNumberOfTellSelfInvocation = 2;
        int msgCount = 100;
        awaitTellSelfLatch = new CountDownLatch(wantedNumberOfTellSelfInvocation);
        invokeTellSelf(wantedNumberOfTellSelfInvocation, true, 3);

        config.setInterval(deduplicationInterval);
        config.setStrategy(DeduplicationStrategy.LAST);
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

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

        verify(node, times(msgCount + wantedNumberOfTellSelfInvocation)).onMsg(eq(ctx), any());
        verify(ctx, times(msgCount)).ack(any());
        verify(ctx, times(2)).enqueueForTellNext(newMsgCaptor.capture(), eq(TbRelationTypes.SUCCESS), successCaptor.capture(), failureCaptor.capture());
        verify(ruleNodeCacheService, times(1)).getEntityIds(anyString());
        verify(ruleNodeCacheService, times(1)).add(eq(getEntityIdsCacheKey()), eq(deviceId));
        verify(ruleNodeCacheService, times(msgCount)).add(eq(deviceId), any(), any(TbMsg.class));

        for (Runnable valueCaptor : successCaptor.getAllValues()) {
            valueCaptor.run();
        }

        verify(ruleNodeCacheService, times(2)).removeTbMsgList(eq(deviceId), any(), anyList());
        verify(ctx, never()).schedule(any(), anyLong(), any());

        List<TbMsg> resultMsgs = newMsgCaptor.getAllValues();
        Assertions.assertEquals(2, resultMsgs.size());

        // verify that newMsg is called but content of messages is the same as in the last msg for the first pack.
        TbMsg actualMsg = resultMsgs.get(0);
        Assertions.assertNotEquals(msgWithLatestTsInFirstPack.getId(), actualMsg.getId());
        Assertions.assertEquals(msgWithLatestTsInFirstPack.getOriginator(), actualMsg.getOriginator());
        Assertions.assertEquals(msgWithLatestTsInFirstPack.getCustomerId(), actualMsg.getCustomerId());
        Assertions.assertEquals(msgWithLatestTsInFirstPack.getData(), actualMsg.getData());
        Assertions.assertEquals(msgWithLatestTsInFirstPack.getMetaData(), actualMsg.getMetaData());
        Assertions.assertEquals(msgWithLatestTsInFirstPack.getType(), actualMsg.getType());

        // verify that newMsg is called but content of messages is the same as in the last msg for the second pack.
        actualMsg = resultMsgs.get(1);
        Assertions.assertNotEquals(msgWithLatestTsInSecondPack.getId(), actualMsg.getId());
        Assertions.assertEquals(msgWithLatestTsInSecondPack.getOriginator(), actualMsg.getOriginator());
        Assertions.assertEquals(msgWithLatestTsInSecondPack.getCustomerId(), actualMsg.getCustomerId());
        Assertions.assertEquals(msgWithLatestTsInSecondPack.getData(), actualMsg.getData());
        Assertions.assertEquals(msgWithLatestTsInSecondPack.getMetaData(), actualMsg.getMetaData());
        Assertions.assertEquals(msgWithLatestTsInSecondPack.getType(), actualMsg.getType());

        node.destroy(ctx, ComponentLifecycleEvent.DELETED);
        verify(ruleNodeCacheService, times(1)).evictTbMsgs(any(), any());
        verify(ruleNodeCacheService, times(1)).evict(eq(getEntityIdsCacheKey()));
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

    @AfterEach
    public void destroy() {
        executorService.shutdown();
    }

}
