/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.queue.common;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueMsgHeaders;
import org.thingsboard.server.queue.TbQueueProducer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
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
    long maxRequestTimeout = 20;
    long maxPendingRequests = 32;
    long pollInterval = 25;

    DefaultTbQueueRequestTemplate inst;

    @Before
    public void setUp() throws Exception {
        willReturn(topic).given(responseTemplate).getTopic();
        inst = spy(new DefaultTbQueueRequestTemplate(
                queueAdmin, requestTemplate, responseTemplate,
                maxRequestTimeout, maxPendingRequests, pollInterval, executorMock));

    }

    @After
    public void tearDown() throws Exception {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    public void givenInstance_whenVerifyInitialParameters_thenOK() {
        assertEquals(maxPendingRequests, inst.maxPendingRequests);
        assertEquals(maxRequestTimeout, inst.maxRequestTimeout);
        assertEquals(pollInterval, inst.pollInterval);
        assertEquals(executorMock, inst.executor);
        assertFalse(inst.stopped);
        assertFalse(inst.internalExecutor);
    }

    @Test
    public void givenExternalExecutor_whenInitStop_thenOK() {
        willDoNothing().given(inst).mainLoop();

        inst.init();
        assertNotEquals(0, inst.tickTs);
        assertEquals(0, inst.nextCleanupMs);
        verify(queueAdmin, times(1)).createTopicIfNotExists(topic);
        verify(requestTemplate, times(1)).init();
        verify(responseTemplate, times(1)).subscribe();
        verify(executorMock, times(1)).submit(any(Runnable.class));

        inst.stop();
        assertTrue(inst.stopped);
        verify(responseTemplate, times(1)).unsubscribe();
        verify(requestTemplate, times(1)).stop();
        verify(executorMock, never()).shutdownNow();
    }

    @Test
    public void givenMainLoop_whenLoopFewTimes_thenVerifyInvocationCount() throws InterruptedException {
        executor = inst.createExecutor();
        CountDownLatch latch = new CountDownLatch(5);
        willDoNothing().given(inst).sleep();
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
        verify(inst, times(2)).sleep();
    }

    @Test
    public void givenMessages_whenSend_thenOK() {
        willDoNothing().given(inst).sendToRequestTemplate(any(), any(), any(), any());
        inst.init();
        int msgCount = 10;
        for (int i = 0; i < msgCount; i++) {
            inst.send(getRequestMsgMock());
        }
        assertEquals(msgCount, inst.pendingRequests.size());
        verify(inst, times(msgCount)).sendToRequestTemplate(any(), any(), any(), any());
    }

    @Test
    public void givenMessagesOverMaxPendingRequests_whenSend_thenImmediateFailedFutureForTheOfRequests() {
        willDoNothing().given(inst).sendToRequestTemplate(any(), any(), any(), any());
        inst.init();
        assertEquals(0, inst.tickSize);
        int msgOverflowCount = 10;
        for (int i = 0; i < inst.maxPendingRequests; i++) {
            assertFalse(inst.send(getRequestMsgMock()).isDone()); //SettableFuture future - pending only
        }
        for (int i = 0; i < msgOverflowCount; i++) {
            assertTrue("max pending requests overflow", inst.send(getRequestMsgMock()).isDone()); //overflow, immediate failed future
        }
        assertEquals(inst.maxPendingRequests, inst.pendingRequests.size());
        verify(inst, times((int) inst.maxPendingRequests)).sendToRequestTemplate(any(), any(), any(), any());
    }

    @Test
    public void givenNothing_whenFetchAndProcessResponsesWithTimeout_thenFail() {

    }

    TbQueueMsg getRequestMsgMock() {
        TbQueueMsg requestMsg = mock(TbQueueMsg.class);
        willReturn(mock(TbQueueMsgHeaders.class)).given(requestMsg).getHeaders();
        return requestMsg;
    }
}