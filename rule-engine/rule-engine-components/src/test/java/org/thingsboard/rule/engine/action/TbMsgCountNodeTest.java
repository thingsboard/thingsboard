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
package org.thingsboard.rule.engine.action;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class TbMsgCountNodeTest {

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private TbMsgCountNode node;
    private TbMsgCountNodeConfiguration config;
    private CountDownLatch awaitTellSelfLatch;

    @Mock
    private TbContext ctxMock;

    @BeforeEach
    public void setUp() {
        node = new TbMsgCountNode();
        config = new TbMsgCountNodeConfiguration();
    }

    @Test
    public void givenIncomingMsgs_whenOnMsg_thenSendsMsgWithMsgCount() throws TbNodeException, InterruptedException {
        config.setInterval(10);
        config.setTelemetryPrefix("count");
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        int numberOfMsgs = 7;
        int wantedNumberOfTellSelfInvocation = 2;

        awaitTellSelfLatch = new CountDownLatch(wantedNumberOfTellSelfInvocation);
        invokeTellSelf(wantedNumberOfTellSelfInvocation, config.getInterval());

        TbMsg tickMsg = TbMsg.newMsg(null, TbMsgType.MSG_COUNT_SELF_MSG, null, null, TbMsgMetaData.EMPTY, TbMsg.EMPTY_STRING);
        when(ctxMock.newMsg(any(), any(TbMsgType.class), any(), any(), any(), any())).thenReturn(tickMsg);

        node.init(ctxMock, configuration);

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, null, TbMsgMetaData.EMPTY, TbMsg.EMPTY_STRING);
        for (int i = 0; i < numberOfMsgs; i++) {
            node.onMsg(ctxMock, msg);
            Thread.sleep(2000);
        }

        awaitTellSelfLatch.await();

        verify(ctxMock, times(numberOfMsgs)).ack(any());
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctxMock, times(wantedNumberOfTellSelfInvocation)).enqueueForTellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.SUCCESS));

        List<TbMsg> resultMsgs = newMsgCaptor.getAllValues();
        Assertions.assertEquals(wantedNumberOfTellSelfInvocation, resultMsgs.size());

        TbMsg firstMsg = resultMsgs.get(0);
        assertThat(firstMsg.getData()).isEqualTo("{\"count_null\":5}");

        TbMsg lastMsg = resultMsgs.get(1);
        assertThat(lastMsg.getData()).isEqualTo("{\"count_null\":2}");
    }

    @Test
    public void givenSingleIncomingMsg_thenOnMsg_thenContinueSendingMsgsWithMsgCountIsZero() throws TbNodeException, InterruptedException {
        config.setInterval(2);
        config.setTelemetryPrefix("count");
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        int numberOfMsgs = 1;
        int wantedNumberOfTellSelfInvocation = 3;

        awaitTellSelfLatch = new CountDownLatch(wantedNumberOfTellSelfInvocation);
        invokeTellSelf(wantedNumberOfTellSelfInvocation, config.getInterval());

        TbMsg tickMsg = TbMsg.newMsg(null, TbMsgType.MSG_COUNT_SELF_MSG, null, null, TbMsgMetaData.EMPTY, TbMsg.EMPTY_STRING);
        when(ctxMock.newMsg(any(), any(TbMsgType.class), any(), any(), any(), any())).thenReturn(tickMsg);

        node.init(ctxMock, configuration);

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, null, TbMsgMetaData.EMPTY, TbMsg.EMPTY_STRING);
        node.onMsg(ctxMock, msg);

        awaitTellSelfLatch.await();

        verify(ctxMock, times(numberOfMsgs)).ack(any());
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctxMock, times(wantedNumberOfTellSelfInvocation)).enqueueForTellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.SUCCESS));

        List<TbMsg> resultMsgs = newMsgCaptor.getAllValues();
        Assertions.assertEquals(wantedNumberOfTellSelfInvocation, resultMsgs.size());

        TbMsg firstMsg = resultMsgs.get(0);
        assertThat(firstMsg.getData()).isEqualTo("{\"count_null\":1}");

        TbMsg secondMsg = resultMsgs.get(1);
        assertThat(secondMsg.getData()).isEqualTo("{\"count_null\":0}");

        TbMsg thirdMsg = resultMsgs.get(2);
        assertThat(thirdMsg.getData()).isEqualTo("{\"count_null\":0}");
    }

    @Test
    public void givenNoIncomingMsgs_whenOnMsg_thenSendsMsgsWithMsgCountIsZero() throws TbNodeException, InterruptedException {
        config.setInterval(3);
        config.setTelemetryPrefix("count");
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        int wantedNumberOfTellSelfInvocation = 3;

        awaitTellSelfLatch = new CountDownLatch(wantedNumberOfTellSelfInvocation);
        invokeTellSelf(wantedNumberOfTellSelfInvocation, config.getInterval());

        TbMsg tickMsg = TbMsg.newMsg(null, TbMsgType.MSG_COUNT_SELF_MSG, null, null, TbMsgMetaData.EMPTY, TbMsg.EMPTY_STRING);
        when(ctxMock.newMsg(any(), any(TbMsgType.class), any(), any(), any(), any())).thenReturn(tickMsg);

        node.init(ctxMock, configuration);

        awaitTellSelfLatch.await();

        verify(ctxMock, never()).ack(any());
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctxMock, times(wantedNumberOfTellSelfInvocation)).enqueueForTellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.SUCCESS));

        List<TbMsg> resultMsgs = newMsgCaptor.getAllValues();
        Assertions.assertEquals(wantedNumberOfTellSelfInvocation, resultMsgs.size());

        TbMsg firstMsg = resultMsgs.get(0);
        assertThat(firstMsg.getData()).isEqualTo("{\"count_null\":0}");

        TbMsg secondMsg = resultMsgs.get(1);
        assertThat(secondMsg.getData()).isEqualTo("{\"count_null\":0}");

        TbMsg thirdMsg = resultMsgs.get(2);
        assertThat(thirdMsg.getData()).isEqualTo("{\"count_null\":0}");
    }

    private void invokeTellSelf(int maxNumberOfInvocation, int interval) {
        AtomicInteger scheduleCount = new AtomicInteger(0);
        doAnswer((Answer<Void>) invocationOnMock -> {
            scheduleCount.getAndIncrement();
            if (scheduleCount.get() <= maxNumberOfInvocation) {
                TbMsg msg = (TbMsg) (invocationOnMock.getArguments())[0];
                executorService.schedule(() -> {
                    node.onMsg(ctxMock, msg);
                    awaitTellSelfLatch.countDown();
                }, interval, TimeUnit.SECONDS);
            }
            return null;
        }).when(ctxMock).tellSelf(ArgumentMatchers.any(TbMsg.class), ArgumentMatchers.anyLong());
    }
}
