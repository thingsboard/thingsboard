/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.queue.common;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueProducer;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.RETURNS_DEEP_STUBS;
import static org.mockito.BDDMockito.atLeastOnce;
import static org.mockito.BDDMockito.lenient;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.spy;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.hamcrest.MockitoHamcrest.longThat;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class DefaultTbQueueRequestTemplateTest {

    @Mock
    TbQueueAdmin queueAdmin;
    @Mock
    TbQueueProducer<TbQueueMsg> requestTemplate;
    @Mock
    TbQueueConsumer<TbQueueMsg> responseTemplate;
    @Mock
    ExecutorService executorMock;

    ExecutorService executor;
    String topic = "js-responses-tb-node-0";
    long maxRequestTimeout = 10;
    long maxPendingRequests = 32;
    long pollInterval = 5;

    DefaultTbQueueRequestTemplate<TbQueueMsg, TbQueueMsg> inst;

    @BeforeEach
    public void setUp() throws Exception {
        lenient().doReturn(topic).when(responseTemplate).getTopic();
        inst = spy(new DefaultTbQueueRequestTemplate<>(
                queueAdmin, requestTemplate, responseTemplate,
                maxRequestTimeout, maxPendingRequests, pollInterval, executorMock));

    }

    @AfterEach
    public void tearDown() throws Exception {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    public void givenInstance_whenVerifyInitialParameters_thenOK() {
        assertThat(inst.maxPendingRequests, equalTo(maxPendingRequests));
        assertThat(inst.maxRequestTimeoutNs, equalTo(TimeUnit.MILLISECONDS.toNanos(maxRequestTimeout)));
        assertThat(inst.pollInterval, equalTo(pollInterval));
        assertThat(inst.executor, is(executorMock));
        assertThat(inst.stopped, is(false));
        assertThat(inst.internalExecutor, is(false));
    }

    @Test
    public void givenExternalExecutor_whenInitStop_thenOK() {
        inst.init();
        assertThat(inst.nextCleanupNs, equalTo(0L));
        verify(queueAdmin, times(1)).createTopicIfNotExists(topic);
        verify(responseTemplate, times(1)).subscribe();
        verify(executorMock, times(1)).submit(any(Runnable.class));

        inst.stop();
        assertThat(inst.stopped, is(true));
        verify(responseTemplate, times(1)).unsubscribe();
        verify(requestTemplate, times(1)).stop();
        verify(executorMock, never()).shutdownNow();
    }

    @Test
    public void givenMainLoop_whenLoopFewTimes_thenVerifyInvocationCount() throws InterruptedException {
        executor = inst.createExecutor();
        CountDownLatch latch = new CountDownLatch(5);
        willDoNothing().given(inst).sleep(anyLong());
        willAnswer(invocation -> {
            if (latch.getCount() == 1) {
                inst.stop(); //stop the loop in natural way
            }
            if (latch.getCount() == 3 || latch.getCount() == 4) {
                latch.countDown();
                throw new RuntimeException("test catch block");
            }
            latch.countDown();
            return null;
        }).given(inst).fetchAndProcessResponses();

        executor.submit(inst::mainLoop);
        latch.await(10, TimeUnit.SECONDS);

        verify(inst, times(5)).fetchAndProcessResponses();
        verify(inst, times(2)).sleep(longThat(lessThan(TimeUnit.MILLISECONDS.toNanos(inst.pollInterval))));
    }

    @Test
    public void givenMessages_whenSend_thenOK() {
        willDoNothing().given(inst).sendToRequestTemplate(any(), any(), any(), any(), any());
        inst.init();
        final int msgCount = 10;
        for (int i = 0; i < msgCount; i++) {
            inst.send(getRequestMsgMock());
        }
        assertThat(inst.pendingRequests.mappingCount(), equalTo((long) msgCount));
        verify(inst, times(msgCount)).sendToRequestTemplate(any(), any(), any(), any(), any());
    }

    @Test
    public void givenMessagesOverMaxPendingRequests_whenSend_thenImmediateFailedFutureForTheOfRequests() {
        willDoNothing().given(inst).sendToRequestTemplate(any(), any(), any(), any(), any());
        inst.init();
        int msgOverflowCount = 10;
        for (int i = 0; i < inst.maxPendingRequests; i++) {
            assertThat(inst.send(getRequestMsgMock()).isDone(), is(false)); //SettableFuture future - pending only
        }
        for (int i = 0; i < msgOverflowCount; i++) {
            assertThat("max pending requests overflow", inst.send(getRequestMsgMock()).isDone(), is(true)); //overflow, immediate failed future
        }
        assertThat(inst.pendingRequests.mappingCount(), equalTo(inst.maxPendingRequests));
        verify(inst, times((int) inst.maxPendingRequests)).sendToRequestTemplate(any(), any(), any(), any(), any());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void givenNothing_whenSendAndFetchAndProcessResponsesWithTimeout_thenFail() {
        //given
        AtomicLong currentTime = new AtomicLong();
        willAnswer(x -> {
            log.info("currentTime={}", currentTime.get());
            return currentTime.get();
        }).given(inst).getCurrentClockNs();
        inst.init();
        inst.setupNextCleanup();
        willReturn(Collections.emptyList()).given(inst).doPoll();

        //when
        long stepNs = TimeUnit.MILLISECONDS.toNanos(1);
        for (long i = 0; i <= inst.maxRequestTimeoutNs * 2; i = i + stepNs) {
            currentTime.addAndGet(stepNs);
            assertThat(inst.send(getRequestMsgMock()).isDone(), is(false)); //SettableFuture future - pending only
            if (i % (inst.maxRequestTimeoutNs * 3 / 2) == 0) {
                inst.fetchAndProcessResponses();
            }
        }

        //then
        ArgumentCaptor<DefaultTbQueueRequestTemplate.ResponseMetaData> argumentCaptorResp = ArgumentCaptor.forClass(DefaultTbQueueRequestTemplate.ResponseMetaData.class);
        ArgumentCaptor<UUID> argumentCaptorUUID = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<Long> argumentCaptorLong = ArgumentCaptor.forClass(Long.class);
        verify(inst, atLeastOnce()).setTimeoutException(argumentCaptorUUID.capture(), argumentCaptorResp.capture(), argumentCaptorLong.capture());

        List<DefaultTbQueueRequestTemplate.ResponseMetaData> responseMetaDataList = argumentCaptorResp.getAllValues();
        List<Long> tickTsList = argumentCaptorLong.getAllValues();
        for (int i = 0; i < responseMetaDataList.size(); i++) {
            assertThat("tickTs >= calculatedExpTime", tickTsList.get(i), greaterThanOrEqualTo(responseMetaDataList.get(i).getSubmitTime() + responseMetaDataList.get(i).getTimeout()));
        }
    }

    TbQueueMsg getRequestMsgMock() {
        return mock(TbQueueMsg.class, RETURNS_DEEP_STUBS);
    }
}
