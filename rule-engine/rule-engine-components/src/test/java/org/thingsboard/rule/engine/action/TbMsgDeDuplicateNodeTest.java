/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.stubbing.Answer;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.TbRelationTypes;
import org.thingsboard.rule.engine.deduplicate.DeDuplicateStrategy;
import org.thingsboard.rule.engine.deduplicate.TbMsgDeDuplicateNode;
import org.thingsboard.rule.engine.deduplicate.TbMsgDeDuplicateNodeConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.session.SessionMsgType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

import static org.mockito.ArgumentMatchers.any;
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
public class TbMsgDeDuplicateNodeTest {

    private static final String MAIN_QUEUE_NAME = "Main";
    private static final String HIGH_PRIORITY_QUEUE_NAME = "HighPriority";
    private static final String TB_MSG_DEDUPLICATION_TIMEOUT_MSG = "TbMsgDeDuplicateNodeMsg";

    private TbContext ctx;

    private final ThingsBoardThreadFactory factory = ThingsBoardThreadFactory.forName("de-duplication-node-test");
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(factory);
    private final int deDuplicationInterval = 1;

    private TenantId tenantId;

    private TbMsgDeDuplicateNode node;
    private TbMsgDeDuplicateNodeConfiguration config;
    private TbNodeConfiguration nodeConfiguration;

    private CountDownLatch awaitTellSelfLatch;

    @BeforeEach
    public void init() throws TbNodeException {
        ctx = mock(TbContext.class);

        tenantId = TenantId.fromUUID(UUID.randomUUID());
        RuleNodeId ruleNodeId = new RuleNodeId(UUID.randomUUID());

        when(ctx.getSelfId()).thenReturn(ruleNodeId);
        when(ctx.getTenantId()).thenReturn(tenantId);

        doAnswer((Answer<TbMsg>) invocationOnMock -> {
            String type = (String) (invocationOnMock.getArguments())[1];
            EntityId originator = (EntityId) (invocationOnMock.getArguments())[2];
            TbMsgMetaData metaData = (TbMsgMetaData) (invocationOnMock.getArguments())[3];
            String data = (String) (invocationOnMock.getArguments())[4];
            return TbMsg.newMsg(type, originator, metaData.copy(), data);
        }).when(ctx).newMsg(isNull(), eq(TB_MSG_DEDUPLICATION_TIMEOUT_MSG), nullable(EntityId.class), any(TbMsgMetaData.class), any(String.class));
        node = spy(new TbMsgDeDuplicateNode());
        config = new TbMsgDeDuplicateNodeConfiguration().defaultConfiguration();
    }

    private void invokeTellSelf(int maxNumberOfInvocation, boolean incrementScheduleTimeout) {
        AtomicLong scheduleTimeout = new AtomicLong(deDuplicationInterval);
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
                if (incrementScheduleTimeout) {
                    scheduleTimeout.set(scheduleTimeout.get() * 3);
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
    public void given_100_messages_then_verifyOutputFirst() throws TbNodeException, ExecutionException, InterruptedException {
        int wantedNumberOfTellSelfInvocation = 2;
        int msgCount = 100;
        awaitTellSelfLatch = new CountDownLatch(wantedNumberOfTellSelfInvocation);
        invokeTellSelf(wantedNumberOfTellSelfInvocation, false);

        config.setDelay(deDuplicationInterval);
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

        verify(ctx, times(msgCount)).ack(any());
        verify(ctx, times(1)).tellFailure(eq(msgToReject), any());
        verify(node, times(msgCount + wantedNumberOfTellSelfInvocation + 1)).onMsg(eq(ctx), any());
        verify(ctx, times(1)).enqueueForTellNext(newMsgCaptor.capture(), eq(TbRelationTypes.SUCCESS), successCaptor.capture(), failureCaptor.capture());
        Assertions.assertEquals(inputMsgs.get(0), newMsgCaptor.getValue());
    }

    @Test
    public void given_100_messages_then_verifyOutputLast() throws TbNodeException, ExecutionException, InterruptedException {
        int wantedNumberOfTellSelfInvocation = 2;
        int msgCount = 100;
        awaitTellSelfLatch = new CountDownLatch(wantedNumberOfTellSelfInvocation);
        invokeTellSelf(wantedNumberOfTellSelfInvocation, false);

        config.setStrategy(DeDuplicateStrategy.LAST);
        config.setDelay(deDuplicationInterval);
        config.setMaxPendingMsgs(msgCount);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        long currentTimeMillis = System.currentTimeMillis();

        List<TbMsg> inputMsgs = getTbMsgs(deviceId, msgCount, currentTimeMillis, 500);

        int indexOfLastMsgInArray = inputMsgs.size() - 1;
        int indexToSetMaxTs = new Random().nextInt(indexOfLastMsgInArray) + 1;
        TbMsg currentMaxTsMsg = inputMsgs.get(indexOfLastMsgInArray);
        TbMsg newLastMsgOfArray = inputMsgs.get(indexToSetMaxTs);
        inputMsgs.set(indexOfLastMsgInArray, newLastMsgOfArray);
        inputMsgs.set(indexToSetMaxTs, currentMaxTsMsg);

        for (TbMsg msg : inputMsgs) {
            node.onMsg(ctx, msg);
        }

        TbMsg msgToReject = createMsg(deviceId, inputMsgs.get(indexOfLastMsgInArray).getMetaDataTs() + 2);
        node.onMsg(ctx, msgToReject);

        awaitTellSelfLatch.await();

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Runnable> successCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Consumer<Throwable>> failureCaptor = ArgumentCaptor.forClass(Consumer.class);

        verify(ctx, times(msgCount)).ack(any());
        verify(ctx, times(1)).tellFailure(eq(msgToReject), any());
        verify(node, times(msgCount + wantedNumberOfTellSelfInvocation + 1)).onMsg(eq(ctx), any());
        verify(ctx, times(1)).enqueueForTellNext(newMsgCaptor.capture(), eq(TbRelationTypes.SUCCESS), successCaptor.capture(), failureCaptor.capture());
        Assertions.assertEquals(currentMaxTsMsg, newMsgCaptor.getValue());
    }

    @Test
    public void given_100_messages_then_verifyOutputAll() throws TbNodeException, ExecutionException, InterruptedException {
        int wantedNumberOfTellSelfInvocation = 2;
        int msgCount = 100;
        awaitTellSelfLatch = new CountDownLatch(wantedNumberOfTellSelfInvocation);
        invokeTellSelf(wantedNumberOfTellSelfInvocation, false);

        config.setDelay(deDuplicationInterval);
        config.setMaxPendingMsgs(msgCount);
        config.setStrategy(DeDuplicateStrategy.ALL);
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

        verify(ctx, times(msgCount)).ack(any());
        verify(node, times(msgCount + wantedNumberOfTellSelfInvocation)).onMsg(eq(ctx), any());
        verify(ctx, times(1)).enqueueForTellNext(newMsgCaptor.capture(), eq(TbRelationTypes.SUCCESS), successCaptor.capture(), failureCaptor.capture());

        Assertions.assertEquals(1, newMsgCaptor.getAllValues().size());
        TbMsg outMessage = newMsgCaptor.getAllValues().get(0);
        Assertions.assertEquals(getMergedData(inputMsgs), outMessage.getData());
        Assertions.assertEquals(deviceId, outMessage.getOriginator());
    }

    @Test
    public void given_100_messages_then_verifyOutput_2_packs() throws TbNodeException, ExecutionException, InterruptedException {
        int wantedNumberOfTellSelfInvocation = 2;
        int msgCount = 100;
        awaitTellSelfLatch = new CountDownLatch(wantedNumberOfTellSelfInvocation);
        invokeTellSelf(wantedNumberOfTellSelfInvocation, true);

        config.setDelay(deDuplicationInterval);
        config.setMaxPendingMsgs(msgCount);
        config.setStrategy(DeDuplicateStrategy.ALL);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        long currentTimeMillis = System.currentTimeMillis();

        List<TbMsg> firstMsgPack = getTbMsgs(deviceId, msgCount / 2, currentTimeMillis, 500);
        for (TbMsg msg : firstMsgPack) {
            node.onMsg(ctx, msg);
        }
        TbMsg firstMsgFromFirstPack = firstMsgPack.get(0);
        long firstPackDeDuplicationPackEndTs = firstMsgFromFirstPack.getMetaDataTs() + TimeUnit.SECONDS.toMillis(deDuplicationInterval);

        List<TbMsg> secondMsgPack = getTbMsgs(deviceId, msgCount / 2, firstPackDeDuplicationPackEndTs, 500);
        for (TbMsg msg : secondMsgPack) {
            node.onMsg(ctx, msg);
        }

        awaitTellSelfLatch.await();

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Runnable> successCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Consumer<Throwable>> failureCaptor = ArgumentCaptor.forClass(Consumer.class);

        verify(ctx, times(msgCount)).ack(any());
        verify(node, times(msgCount + wantedNumberOfTellSelfInvocation)).onMsg(eq(ctx), any());
        verify(ctx, times(2)).enqueueForTellNext(newMsgCaptor.capture(), eq(TbRelationTypes.SUCCESS), successCaptor.capture(), failureCaptor.capture());

        Assertions.assertEquals(2, newMsgCaptor.getAllValues().size());
        Assertions.assertEquals(getMergedData(firstMsgPack), newMsgCaptor.getAllValues().get(0).getData());
        Assertions.assertEquals(getMergedData(secondMsgPack), newMsgCaptor.getAllValues().get(1).getData());
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
                MAIN_QUEUE_NAME,
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
