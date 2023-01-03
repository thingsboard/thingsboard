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
import org.thingsboard.rule.engine.delay.DeDuplicateStrategy;
import org.thingsboard.rule.engine.delay.TbMsgDeDuplicateNode;
import org.thingsboard.rule.engine.delay.TbMsgDeDuplicateNodeConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.session.SessionMsgType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
public class TbMsgDeDuplicateNodeTest {

    private static final String MAIN_QUEUE_NAME = "Main";
    private static final String HIGH_PRIORITY_QUEUE_NAME = "HighPriority";
    private static final String TB_MSG_DEDUPLICATION_TIMEOUT_MSG = "TbMsgDeDuplicateNodeMsg";

    private TbContext ctx;

    private ExecutorService executorService = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("de-duplication-node-test"));

    private TenantId tenantId;
    private RuleNodeId ruleNodeId;

    private TbMsgDeDuplicateNode node;
    private TbMsgDeDuplicateNodeConfiguration config;
    private TbNodeConfiguration nodeConfiguration;

    private CountDownLatch awaitTellSelfLatch;
    private CountDownLatch awaitAllMsgsProcessedLatch;
    private int scheduleCount;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void init() throws TbNodeException {
        ctx = mock(TbContext.class);
        awaitTellSelfLatch = new CountDownLatch(1);
        awaitAllMsgsProcessedLatch = new CountDownLatch(1);

        tenantId = TenantId.fromUUID(UUID.randomUUID());
        ruleNodeId = new RuleNodeId(UUID.randomUUID());

        when(ctx.getSelfId()).thenReturn(ruleNodeId);
        when(ctx.getTenantId()).thenReturn(tenantId);

        doAnswer((Answer<TbMsg>) invocationOnMock -> {
            String type = (String) (invocationOnMock.getArguments())[1];
            EntityId originator = (EntityId) (invocationOnMock.getArguments())[2];
            TbMsgMetaData metaData = (TbMsgMetaData) (invocationOnMock.getArguments())[3];
            String data = (String) (invocationOnMock.getArguments())[4];
            return TbMsg.newMsg(type, originator, metaData.copy(), data);
        }).when(ctx).newMsg(isNull(), eq(TB_MSG_DEDUPLICATION_TIMEOUT_MSG), nullable(EntityId.class), any(TbMsgMetaData.class), any(String.class));

        scheduleCount = 0;

        doAnswer((Answer<Void>) invocationOnMock -> {
            scheduleCount++;
            if (scheduleCount == 1) {
                TbMsg msg = (TbMsg) (invocationOnMock.getArguments())[0];
                executorService.submit(() -> {
                    try {
                        awaitAllMsgsProcessedLatch.await();
                        node.onMsg(ctx, msg);
                        awaitTellSelfLatch.countDown();
                    } catch (ExecutionException | InterruptedException | TbNodeException e) {
                        log.error("Failed to execute tellSelf method call due to: ", e);
                    }
                });
            }
            return null;
        }).when(ctx).tellSelf(ArgumentMatchers.any(TbMsg.class), ArgumentMatchers.anyLong());

        node = new TbMsgDeDuplicateNode();
        config = new TbMsgDeDuplicateNodeConfiguration().defaultConfiguration();
    }

    @AfterEach
    public void destroy() {
        if (executorService != null) {
            executorService.shutdown();
        }
        node.destroy();
    }

    @Test
    public void given_multipleMessages_thenVerifyOutputFirst() throws TbNodeException, ExecutionException, InterruptedException {
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        List<TbMsg> inputMsgs = createTbMsgs(deviceId);
        for (TbMsg msg : inputMsgs) {
            node.onMsg(ctx, msg);
        }
        awaitAllMsgsProcessedLatch.countDown();
        awaitTellSelfLatch.await();

        verify(ctx, times(inputMsgs.size())).ack(any());
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Runnable> successCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Consumer<Throwable>> failureCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(ctx, times(1)).enqueueForTellNext(newMsgCaptor.capture(), eq(TbRelationTypes.SUCCESS), successCaptor.capture(), failureCaptor.capture());
        Assertions.assertEquals(inputMsgs.get(0), newMsgCaptor.getValue());
    }

    @Test
    public void given_moreMessagesThenAllowedInConfiguration_thenVerifyTellFailure() throws TbNodeException, ExecutionException, InterruptedException {
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        List<TbMsg> inputMsgs = createTbMsgs(deviceId, 101);
        for (TbMsg msg : inputMsgs) {
            node.onMsg(ctx, msg);
        }
        awaitAllMsgsProcessedLatch.countDown();
        awaitTellSelfLatch.await();

        verify(ctx, times(inputMsgs.size() -1)).ack(any());
        verify(ctx, times(1)).tellFailure(eq(inputMsgs.get(inputMsgs.size() -1)), any());
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Runnable> successCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Consumer<Throwable>> failureCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(ctx, times(1)).enqueueForTellNext(newMsgCaptor.capture(), eq(TbRelationTypes.SUCCESS), successCaptor.capture(), failureCaptor.capture());
        Assertions.assertEquals(inputMsgs.get(0), newMsgCaptor.getValue());
    }

    @Test
    public void given_multipleMessages_thenVerifyOutputLast() throws TbNodeException, ExecutionException, InterruptedException {
        config.setStrategy(DeDuplicateStrategy.LAST);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        List<TbMsg> inputMsgs = createTbMsgs(deviceId);
        for (TbMsg msg : inputMsgs) {
            node.onMsg(ctx, msg);
        }
        awaitAllMsgsProcessedLatch.countDown();
        awaitTellSelfLatch.await();

        verify(ctx, times(inputMsgs.size())).ack(any());
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Runnable> successCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Consumer<Throwable>> failureCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(ctx, times(1)).enqueueForTellNext(newMsgCaptor.capture(), eq(TbRelationTypes.SUCCESS), successCaptor.capture(), failureCaptor.capture());
        Assertions.assertEquals(inputMsgs.get(inputMsgs.size() - 1), newMsgCaptor.getValue());
    }

    @Test
    public void given_multipleMessagesFromTwoOriginators_thenVerifyOutputAllForEachOriginator() throws TbNodeException, ExecutionException, InterruptedException {
        config.setStrategy(DeDuplicateStrategy.ALL);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        DeviceId firstDeviceId = new DeviceId(UUID.randomUUID());
        List<TbMsg> firstDeviceInputMsgs = createTbMsgs(firstDeviceId);

        DeviceId secondDeviceId = new DeviceId(UUID.randomUUID());
        List<TbMsg> secondDeviceInputMsgs = createTbMsgs(secondDeviceId);

        List<TbMsg> inputMsgs = new ArrayList<>();
        inputMsgs.addAll(firstDeviceInputMsgs);
        inputMsgs.addAll(secondDeviceInputMsgs);

        for (TbMsg msg : inputMsgs) {
            node.onMsg(ctx, msg);
        }
        awaitAllMsgsProcessedLatch.countDown();
        awaitTellSelfLatch.await();

        verify(ctx, times(inputMsgs.size())).ack(any());
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Runnable> successCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Consumer<Throwable>> failureCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(ctx, times(2)).enqueueForTellNext(newMsgCaptor.capture(), eq(TbRelationTypes.SUCCESS), successCaptor.capture(), failureCaptor.capture());

        List<TbMsg> outMessages = newMsgCaptor.getAllValues();
        Assertions.assertEquals(2, outMessages.size());
        for (TbMsg tbMsg : outMessages) {
            if (tbMsg.getOriginator().equals(firstDeviceId)) {
                Assertions.assertEquals(getMergedData(firstDeviceInputMsgs), tbMsg.getData());
            } else {
                Assertions.assertEquals(getMergedData(secondDeviceInputMsgs), tbMsg.getData());
                Assertions.assertEquals(secondDeviceId, tbMsg.getOriginator());
            }
        }
    }

    @Test
    public void given_multipleMessagesFromTwoOriginators_thenVerifyOutputAll() throws TbNodeException, ExecutionException, InterruptedException {
        config.setStrategy(DeDuplicateStrategy.ALL);
        config.setDeDuplicateByOriginator(false);
        config.setQueueName(HIGH_PRIORITY_QUEUE_NAME);
        config.setOutMsgType(SessionMsgType.POST_ATTRIBUTES_REQUEST.name());
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        DeviceId firstDeviceId = new DeviceId(UUID.randomUUID());
        List<TbMsg> firstDeviceInputMsgs = createTbMsgs(firstDeviceId, 50);

        DeviceId secondDeviceId = new DeviceId(UUID.randomUUID());
        List<TbMsg> secondDeviceInputMsgs = createTbMsgs(secondDeviceId, 50);

        List<TbMsg> inputMsgs = new ArrayList<>();
        inputMsgs.addAll(firstDeviceInputMsgs);
        inputMsgs.addAll(secondDeviceInputMsgs);

        for (TbMsg msg : inputMsgs) {
            node.onMsg(ctx, msg);
        }
        awaitAllMsgsProcessedLatch.countDown();
        awaitTellSelfLatch.await();

        verify(ctx, times(inputMsgs.size())).ack(any());
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Runnable> successCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Consumer<Throwable>> failureCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(ctx, times(1)).enqueueForTellNext(newMsgCaptor.capture(), eq(TbRelationTypes.SUCCESS), successCaptor.capture(), failureCaptor.capture());

        Assertions.assertEquals(1, newMsgCaptor.getAllValues().size());
        TbMsg outMessage = newMsgCaptor.getAllValues().get(0);
        Assertions.assertEquals(getMergedData(inputMsgs), outMessage.getData());
        Assertions.assertEquals(tenantId, outMessage.getOriginator());
    }

    private List<TbMsg> createTbMsgs(DeviceId deviceId) {
        return createTbMsgs(deviceId, 100);
    }

    private List<TbMsg> createTbMsgs(DeviceId deviceId, int msgCount) {
        List<TbMsg> inputMsgs = new ArrayList<>();
        for (int i = 0; i < msgCount; i++) {
            ObjectNode dataNode = JacksonUtil.newObjectNode();
            dataNode.put("msgId", i);
            dataNode.put("deviceId", deviceId.getId().toString());
            TbMsg tbMsg = TbMsg.newMsg(
                    MAIN_QUEUE_NAME,
                    SessionMsgType.POST_TELEMETRY_REQUEST.name(),
                    deviceId,
                    new TbMsgMetaData(),
                    JacksonUtil.toString(dataNode));
            inputMsgs.add(tbMsg);
        }
        return inputMsgs;
    }

    private String getMergedData(List<TbMsg> msgs) {
        ArrayNode mergedData = JacksonUtil.OBJECT_MAPPER.createArrayNode();
        msgs.forEach(msg -> {
            ObjectNode msgNode = JacksonUtil.newObjectNode();
            msgNode.set("msg", JacksonUtil.toJsonNode(msg.getData()));
            msgNode.set("metadata", JacksonUtil.valueToTree(msg.getMetaData()));
            mergedData.add(msgNode);
        });
        return JacksonUtil.toString(mergedData);
    }

}
