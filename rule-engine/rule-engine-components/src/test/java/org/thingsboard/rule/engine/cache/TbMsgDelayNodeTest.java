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
package org.thingsboard.rule.engine.cache;

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
import org.thingsboard.rule.engine.api.RuleNodeCacheService;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.delay.TbMsgDelayNode;
import org.thingsboard.rule.engine.delay.TbMsgDelayNodeConfiguration;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

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
public class TbMsgDelayNodeTest {

    private final int delayPeriod = 1;

    private CountDownLatch awaitTellSelfLatch;
    private ScheduledExecutorService executorService;

    private TbContext ctx;
    private RuleNodeCacheService ruleNodeCacheService;

    private TbMsgDelayNode node;
    private TbMsgDelayNodeConfiguration config;

    @BeforeEach
    protected void init() throws TbNodeException {
        executorService = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("delay-node-test"));

        ctx = mock(TbContext.class);
        ruleNodeCacheService = mock(RuleNodeCacheService.class);

        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        RuleNodeId ruleNodeId = new RuleNodeId(UUID.randomUUID());

        when(ctx.getSelfId()).thenReturn(ruleNodeId);
        when(ctx.getTenantId()).thenReturn(tenantId);
        when(ctx.getRuleNodeCacheService()).thenReturn(Optional.of(ruleNodeCacheService));

        doAnswer((Answer<TbMsg>) invocationOnMock -> {
            TbMsgType type = (TbMsgType) (invocationOnMock.getArguments())[1];
            EntityId originator = (EntityId) (invocationOnMock.getArguments())[2];
            TbMsgMetaData metaData = (TbMsgMetaData) (invocationOnMock.getArguments())[3];
            String data = (String) (invocationOnMock.getArguments())[4];
            return TbMsg.newMsg(type, originator, metaData.copy(), data);
        }).when(ctx).newMsg(isNull(), eq(TbMsgType.DELAY_TIMEOUT_SELF_MSG), nullable(EntityId.class), any(TbMsgMetaData.class), any(String.class));
        node = spy(new TbMsgDelayNode());
        config = new TbMsgDelayNodeConfiguration().defaultConfiguration();
    }

    @AfterEach
    public void destroy() {
        executorService.shutdown();
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
        verify(ctx).tellFailure(eq(msgToReject), any());
        verify(ctx, times(successMsgCount)).enqueueForTellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.SUCCESS), successCaptor.capture(), failureCaptor.capture());
        verify(ruleNodeCacheService).getEntityIds(anyString());
        verify(ruleNodeCacheService).add(eq(getEntityIdsCacheKey()), eq(deviceId));
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
        verify(ruleNodeCacheService).evictTbMsgs(any(), any());
        verify(ruleNodeCacheService).evict(eq(getEntityIdsCacheKey()));

    }

    @Test
    public void given1MsgFromNonLocalEntity_whenOnMsg_thenVerifyEntityIgnored() throws TbNodeException, ExecutionException, InterruptedException {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());

        TopicPartitionInfo tpi = mock(TopicPartitionInfo.class);

        when(tpi.isMyPartition()).thenReturn(false);
        when(ctx.getTopicPartitionInfo(deviceId)).thenReturn(tpi);

        TbMsg msgToProcess = createMsg(deviceId, System.currentTimeMillis());
        node.onMsg(ctx, msgToProcess);

        verify(tpi).isMyPartition();
        verify(tpi, never()).getPartition();
        verify(ctx).getTopicPartitionInfo(eq(deviceId));

        node.destroy();
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

        verify(tpi).isMyPartition();
        verify(tpi).getPartition();
        verify(ctx).getTopicPartitionInfo(eq(deviceId));
        verify(ctx).enqueueForTellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.SUCCESS), successCaptor.capture(), failureCaptor.capture());
        verify(ruleNodeCacheService).getEntityIds(getEntityIdsCacheKey());
        verify(ruleNodeCacheService).getTbMsgs(eq(deviceId), eq(0));
        verify(ruleNodeCacheService).removeTbMsgList(eq(deviceId), eq(0), eq(Collections.singletonList(msgFromCache)));

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
        verify(node).onMsg(eq(ctx), any());
        verify(ctx).tellSelf(any(), anyLong());
        verify(ctx, times(2)).getTopicPartitionInfo(eq(deviceId));
        verify(ctx).enqueueForTellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.SUCCESS), successCaptor.capture(), failureCaptor.capture());
        verify(ruleNodeCacheService).getEntityIds(getEntityIdsCacheKey());
        verify(ruleNodeCacheService).getTbMsgs(eq(deviceId), eq(0));
        verify(ruleNodeCacheService).removeTbMsgList(eq(deviceId), eq(0), eq(Collections.singletonList(msgFromCache)));

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
        DeviceId deviceId = new DeviceId(UUID.randomUUID());

        TopicPartitionInfo tpi = mock(TopicPartitionInfo.class);

        when(tpi.isMyPartition()).thenReturn(false);
        when(ctx.getTopicPartitionInfo(deviceId)).thenReturn(tpi);
        when(ruleNodeCacheService.getEntityIds(getEntityIdsCacheKey())).thenReturn(Collections.singleton(deviceId));

        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        verify(tpi).isMyPartition();
        verify(tpi, never()).getPartition();
        verify(ctx).getTopicPartitionInfo(eq(deviceId));
        verify(ruleNodeCacheService).getEntityIds(getEntityIdsCacheKey());

        node.destroy();
    }

    @Test
    public void givenLocalEntityWithNoValuesInCache_whenInit_thenVerifyDoNothing() throws TbNodeException {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());

        TopicPartitionInfo tpi = mock(TopicPartitionInfo.class);

        when(tpi.isMyPartition()).thenReturn(true);
        when(tpi.getPartition()).thenReturn(Optional.of(0));
        when(ctx.getTopicPartitionInfo(deviceId)).thenReturn(tpi);
        when(ruleNodeCacheService.getEntityIds(getEntityIdsCacheKey())).thenReturn(Collections.singleton(deviceId));
        when(ruleNodeCacheService.getTbMsgs(eq(deviceId), eq(0))).thenReturn(Collections.emptySet());

        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        verify(tpi).isMyPartition();
        verify(tpi).getPartition();
        verify(ruleNodeCacheService).getEntityIds(getEntityIdsCacheKey());
        verify(ruleNodeCacheService).getTbMsgs(eq(deviceId), eq(0));
        verify(ctx).getTopicPartitionInfo(eq(deviceId));
        verify(ctx, never()).enqueueForTellNext(any(), anyString(), any(), any());
        verify(ctx, never()).tellSelf(any(), anyLong());

        node.destroy();
    }

    @Test
    public void givenPartitionThatAlreadyExists_whenOnPartitionChange_thenVerifyCheckCacheSkipped() throws TbNodeException {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());

        TopicPartitionInfo tpi = mock(TopicPartitionInfo.class);

        when(tpi.isMyPartition()).thenReturn(true);
        when(tpi.getPartition()).thenReturn(Optional.of(0));
        when(ctx.getTopicPartitionInfo(deviceId)).thenReturn(tpi);
        when(ruleNodeCacheService.getEntityIds(eq(getEntityIdsCacheKey()))).thenReturn(Collections.singleton(deviceId));
        when(ruleNodeCacheService.getTbMsgs(eq(deviceId), eq(0))).thenReturn(Collections.emptySet());

        Set<Integer> topicPartitionIds = new HashSet<>();
        tpi.getPartition().ifPresent(topicPartitionIds::add);
        PartitionChangeMsg partitionChangeMsg = new PartitionChangeMsg(ServiceType.TB_RULE_ENGINE, topicPartitionIds);

        // add partition to the partitions map ...
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // check that partition is present and cache check is skipped!
        node.onPartitionChangeMsg(ctx, partitionChangeMsg);

        verify(tpi).isMyPartition();
        verify(tpi, times(2)).getPartition();
        verify(ctx).getTopicPartitionInfo(eq(deviceId));
        verify(ctx, never()).enqueueForTellNext(any(), anyString(), any(), any());
        verify(ctx, never()).tellSelf(any(), anyLong());
        verify(ruleNodeCacheService).getEntityIds(getEntityIdsCacheKey());
        verify(ruleNodeCacheService).getTbMsgs(eq(deviceId), eq(0));

        node.destroy();
    }

    @Test
    public void givenNoEntitiesInCache_whenOnPartitionChange_thenVerifyDoNothing() {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());

        TopicPartitionInfo tpi = mock(TopicPartitionInfo.class);

        when(tpi.isMyPartition()).thenReturn(true);
        when(tpi.getPartition()).thenReturn(Optional.of(0));
        when(ctx.getTopicPartitionInfo(deviceId)).thenReturn(tpi);
        when(ruleNodeCacheService.getEntityIds(eq(getEntityIdsCacheKey()))).thenReturn(Collections.emptySet());

        Set<Integer> topicPartitionIds = new HashSet<>();
        tpi.getPartition().ifPresent(topicPartitionIds::add);
        PartitionChangeMsg partitionChangeMsg = new PartitionChangeMsg(ServiceType.TB_RULE_ENGINE, topicPartitionIds);

        node.onPartitionChangeMsg(ctx, partitionChangeMsg);

        verify(ruleNodeCacheService).getEntityIds(getEntityIdsCacheKey());
        verify(ctx, never()).getTopicPartitionInfo(eq(deviceId));

        node.destroy();
    }

    @Test
    public void givenLocalEntityWithNoValuesInCache_whenOnPartitionChange_thenVerifyDoNothing() {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());

        TopicPartitionInfo tpi = mock(TopicPartitionInfo.class);

        when(tpi.isMyPartition()).thenReturn(true);
        when(tpi.getPartition()).thenReturn(Optional.of(0));
        when(ctx.getTopicPartitionInfo(deviceId)).thenReturn(tpi);
        when(ruleNodeCacheService.getEntityIds(eq(getEntityIdsCacheKey()))).thenReturn(Collections.singleton(deviceId));
        when(ruleNodeCacheService.getTbMsgs(eq(deviceId), eq(0))).thenReturn(Collections.emptySet());

        Set<Integer> topicPartitionIds = new HashSet<>();
        tpi.getPartition().ifPresent(topicPartitionIds::add);
        PartitionChangeMsg partitionChangeMsg = new PartitionChangeMsg(ServiceType.TB_RULE_ENGINE, topicPartitionIds);

        node.onPartitionChangeMsg(ctx, partitionChangeMsg);

        verify(tpi).isMyPartition();
        verify(tpi, times(2)).getPartition();
        verify(ctx).getTopicPartitionInfo(eq(deviceId));
        verify(ctx, never()).enqueueForTellNext(any(), anyString(), any(), any());
        verify(ctx, never()).tellSelf(any(), anyLong());
        verify(ruleNodeCacheService).getEntityIds(getEntityIdsCacheKey());
        verify(ruleNodeCacheService).getTbMsgs(eq(deviceId), eq(0));

        node.destroy();
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

        Set<Integer> topicPartitionIds = new HashSet<>();
        tpi.getPartition().ifPresent(topicPartitionIds::add);
        PartitionChangeMsg partitionChangeMsg = new PartitionChangeMsg(ServiceType.TB_RULE_ENGINE, topicPartitionIds);

        node.onPartitionChangeMsg(ctx, partitionChangeMsg);

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Runnable> successCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Consumer<Throwable>> failureCaptor = ArgumentCaptor.forClass(Consumer.class);

        verify(tpi).isMyPartition();
        verify(tpi, times(2)).getPartition();
        verify(ctx).getTopicPartitionInfo(eq(deviceId));
        verify(ctx).enqueueForTellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.SUCCESS), successCaptor.capture(), failureCaptor.capture());
        verify(ruleNodeCacheService).getEntityIds(getEntityIdsCacheKey());
        verify(ruleNodeCacheService).getTbMsgs(eq(deviceId), eq(0));
        verify(ruleNodeCacheService).removeTbMsgList(eq(deviceId), eq(0), eq(Collections.singletonList(msgFromCache)));

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

        Set<Integer> topicPartitionIds = new HashSet<>();
        tpi.getPartition().ifPresent(topicPartitionIds::add);
        PartitionChangeMsg partitionChangeMsg = new PartitionChangeMsg(ServiceType.TB_RULE_ENGINE, topicPartitionIds);

        node.onPartitionChangeMsg(ctx, partitionChangeMsg);
        awaitTellSelfLatch.await();

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Runnable> successCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Consumer<Throwable>> failureCaptor = ArgumentCaptor.forClass(Consumer.class);

        verify(tpi, times(2)).isMyPartition();
        verify(tpi, times(3)).getPartition();
        verify(node).onMsg(eq(ctx), any());
        verify(ctx).tellSelf(any(), anyLong());
        verify(ctx, times(2)).getTopicPartitionInfo(eq(deviceId));
        verify(ctx).enqueueForTellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.SUCCESS), successCaptor.capture(), failureCaptor.capture());
        verify(ruleNodeCacheService).getEntityIds(getEntityIdsCacheKey());
        verify(ruleNodeCacheService).getTbMsgs(eq(deviceId), eq(0));
        verify(ruleNodeCacheService).removeTbMsgList(eq(deviceId), eq(0), eq(Collections.singletonList(msgFromCache)));

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

    private TbMsg createMsg(DeviceId deviceId, long ts) {
        ObjectNode dataNode = JacksonUtil.newObjectNode();
        dataNode.put("deviceId", deviceId.getId().toString());
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("ts", String.valueOf(ts));
        return TbMsg.newMsg(
                DataConstants.MAIN_QUEUE_NAME,
                TbMsgType.POST_TELEMETRY_REQUEST,
                deviceId,
                metaData,
                JacksonUtil.toString(dataNode));
    }

    private TbMsg createMsg(DeviceId deviceId) {
        ObjectNode dataNode = JacksonUtil.newObjectNode();
        dataNode.put("deviceId", deviceId.getId().toString());
        TbMsgMetaData metaData = new TbMsgMetaData();
        return TbMsg.newMsg(
                DataConstants.MAIN_QUEUE_NAME,
                TbMsgType.POST_TELEMETRY_REQUEST,
                deviceId,
                metaData,
                JacksonUtil.toString(dataNode));
    }

    private String getEntityIdsCacheKey() {
        return "delayed_originator_ids";
    }

}
