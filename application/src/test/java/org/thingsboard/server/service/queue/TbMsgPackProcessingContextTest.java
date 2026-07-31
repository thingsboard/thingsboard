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
package org.thingsboard.server.service.queue;

import com.google.common.util.concurrent.MoreExecutors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.service.queue.processing.TbRuleEngineSubmitStrategy;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TbMsgPackProcessingContextTest {

    TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());

    @Mock
    TbRuleEngineSubmitStrategy submitStrategy;
    @Mock
    TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg> mockMsg;

    ConcurrentMap<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> pendingMap;

    ExecutorService executorService;

    @BeforeEach
    void setup() {
        pendingMap = new ConcurrentHashMap<>();
        lenient().when(submitStrategy.getPendingMap()).thenReturn(pendingMap);
    }

    @AfterEach
    void tearDown() {
        if (executorService != null) {
            MoreExecutors.shutdownAndAwaitTermination(executorService, 5, TimeUnit.SECONDS);
        }
    }

    @Test
    void testAwait_shouldReturnTrue_whenOnSuccessIsCalledBeforeTimeout() throws InterruptedException {
        // GIVEN - a context with one pending message
        executorService = Executors.newSingleThreadExecutor();

        UUID msgId = UUID.randomUUID();
        pendingMap.put(msgId, mockMsg);
        var context = new TbMsgPackProcessingContext("test-queue", submitStrategy, false);

        // WHEN - onSuccess() is called in another thread before timeout
        executorService.submit(() -> {
            try {
                Thread.sleep(100);
                context.onSuccess(msgId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // THEN - await() should return true (successful completion)
        boolean result = context.await(5000, TimeUnit.MILLISECONDS);
        assertThat(result).as("await() should return true when latch is counted down before timeout").isTrue();

        // Verify the message was moved to success map
        assertThat(context.getSuccessMap()).containsKey(msgId);
        assertThat(context.getPendingMap()).isEmpty();
        assertThat(context.getExceptionsMap()).isEmpty();


        // Verify submit strategy was notified about successful message processing
        then(submitStrategy).should().onSuccess(msgId);
    }

    @Test
    void testAwait_shouldReturnTrue_whenOnFailureIsCalledBeforeTimeout() throws InterruptedException {
        // GIVEN - a context with one pending message
        executorService = Executors.newSingleThreadExecutor();

        UUID msgId = UUID.randomUUID();
        pendingMap.put(msgId, mockMsg);
        var context = new TbMsgPackProcessingContext("test-queue", submitStrategy, false);

        var exception = new RuleEngineException("Test exception");

        // WHEN - onFailure() is called in another thread before timeout
        executorService.submit(() -> {
            try {
                Thread.sleep(100);
                context.onFailure(tenantId, msgId, exception);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // THEN - await() should return true (successful completion, even if message processing failed)
        boolean result = context.await(5000, TimeUnit.MILLISECONDS);
        assertThat(result).as("await() should return true when latch is counted down before timeout").isTrue();

        // Verify the exception was added to exceptions map
        assertThat(context.getSuccessMap()).isEmpty();
        assertThat(context.getPendingMap()).isEmpty();
        assertThat(context.getExceptionsMap()).containsEntry(tenantId, exception);
    }

    @Test
    void testAwait_shouldReturnFalse_whenTimeoutOccurs() throws InterruptedException {
        // GIVEN - a context with one pending message and no processing
        UUID msgId = UUID.randomUUID();
        pendingMap.put(msgId, mockMsg);
        var context = new TbMsgPackProcessingContext("test-queue", submitStrategy, false);

        // WHEN - await() is called with short timeout and no message processing happens
        long startTime = System.nanoTime();
        boolean result = context.await(100, TimeUnit.MILLISECONDS);
        long elapsedTime = System.nanoTime() - startTime;

        // THEN - await() should return false (timeout occurred)
        assertThat(result).as("await() should return false when timeout occurs").isFalse();
        assertThat(elapsedTime).as("await() should wait for at least the timeout duration").isGreaterThanOrEqualTo(100L);

        // Message should still be in pending map
        assertThat(context.getSuccessMap()).isEmpty();
        assertThat(context.getPendingMap()).containsKey(msgId);
        assertThat(context.getExceptionsMap()).isEmpty();
    }

    @Test
    void testAwait_shouldHandleMultiplePendingMessages() throws InterruptedException {
        // GIVEN - a context with multiple pending messages
        executorService = Executors.newSingleThreadExecutor();

        UUID msgId1 = UUID.randomUUID();
        UUID msgId2 = UUID.randomUUID();
        UUID msgId3 = UUID.randomUUID();

        pendingMap.put(msgId1, mockMsg);
        pendingMap.put(msgId2, mockMsg);
        pendingMap.put(msgId3, mockMsg);

        var context = new TbMsgPackProcessingContext("test-queue", submitStrategy, false);

        // WHEN - messages are processed one by one
        executorService.submit(() -> {
            try {
                Thread.sleep(50);
                context.onSuccess(msgId1);
                Thread.sleep(50);
                context.onSuccess(msgId2);
                Thread.sleep(50);
                context.onSuccess(msgId3);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // THEN - await() should return true only after all messages are processed
        boolean result = context.await(5000, TimeUnit.MILLISECONDS);
        assertThat(result).as("await() should return true after all messages are processed").isTrue();

        // All messages should be in success map
        assertThat(context.getSuccessMap()).containsKeys(msgId1, msgId2, msgId3);
        assertThat(context.getPendingMap()).isEmpty();
        assertThat(context.getExceptionsMap()).isEmpty();
    }

    @Test
    void testAwait_shouldNotCountDownPrematurely_withMultipleMessages() throws InterruptedException {
        // GIVEN - a context with multiple pending messages
        executorService = Executors.newSingleThreadExecutor();

        UUID msgId1 = UUID.randomUUID();
        UUID msgId2 = UUID.randomUUID();

        pendingMap.put(msgId1, mockMsg);
        pendingMap.put(msgId2, mockMsg);

        var context = new TbMsgPackProcessingContext("test-queue", submitStrategy, false);

        // WHEN - only one message is processed
        executorService.submit(() -> {
            try {
                Thread.sleep(100);
                context.onSuccess(msgId1);
                // msgId2 still in processing
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // THEN: await should timeout because not all messages were processed
        boolean result = context.await(2000, TimeUnit.MILLISECONDS);
        assertThat(result).as("await() should timeout when not all messages are processed").isFalse();

        // One message in success, one still pending
        assertThat(context.getSuccessMap()).containsOnlyKeys(msgId1);
        assertThat(context.getPendingMap()).containsOnlyKeys(msgId2);
        assertThat(context.getExceptionsMap()).isEmpty();
    }

    @Test
    void testAwait_shouldHandleMixedSuccessAndFailure() throws InterruptedException {
        // GIVEN - multiple messages
        executorService = Executors.newSingleThreadExecutor();

        UUID msgId1 = UUID.randomUUID();
        UUID msgId2 = UUID.randomUUID();

        pendingMap.put(msgId1, mockMsg);
        pendingMap.put(msgId2, mockMsg);

        var context = new TbMsgPackProcessingContext("test-queue", submitStrategy, false);

        var exception = new RuleEngineException("Test exception");

        // WHEN - one succeeds, one fails
        executorService.submit(() -> {
            try {
                Thread.sleep(50);
                context.onSuccess(msgId1);
                Thread.sleep(50);
                context.onFailure(tenantId, msgId2, exception);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // THEN - await() should complete successfully
        boolean result = context.await(5000, TimeUnit.MILLISECONDS);
        assertThat(result).as("await() should return true when all messages are processed").isTrue();

        assertThat(context.getSuccessMap()).containsOnlyKeys(msgId1);
        assertThat(context.getPendingMap()).isEmpty();
        assertThat(context.getExceptionsMap()).containsEntry(tenantId, exception);
    }

    @Test
    void testHighConcurrencyCase() throws InterruptedException {
        int msgCount = 1000;
        int parallelCount = 5;
        executorService = Executors.newFixedThreadPool(parallelCount, ThingsBoardThreadFactory.forName(getClass().getSimpleName() + "-test-scope"));

        ConcurrentMap<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> messages = new ConcurrentHashMap<>(msgCount);
        for (int i = 0; i < msgCount; i++) {
            messages.put(UUID.randomUUID(), new TbProtoQueueMsg<>(UUID.randomUUID(), null));
        }
        TbRuleEngineSubmitStrategy strategyMock = mock(TbRuleEngineSubmitStrategy.class);
        when(strategyMock.getPendingMap()).thenReturn(messages);

        TbMsgPackProcessingContext context = new TbMsgPackProcessingContext(DataConstants.MAIN_QUEUE_NAME, strategyMock, false);
        for (UUID uuid : messages.keySet()) {
            final CountDownLatch readyLatch = new CountDownLatch(parallelCount);
            final CountDownLatch startLatch = new CountDownLatch(1);
            final CountDownLatch finishLatch = new CountDownLatch(parallelCount);
            for (int i = 0; i < parallelCount; i++) {
                executorService.submit(() -> {
                    readyLatch.countDown();
                    try {
                        startLatch.await();
                    } catch (InterruptedException e) {
                        fail("failed to await");
                    }
                    context.onSuccess(uuid);
                    finishLatch.countDown();
                });
            }
            assertTrue(readyLatch.await(10, TimeUnit.SECONDS));
            Thread.yield();
            startLatch.countDown(); //run all-at-once submitted tasks
            assertTrue(finishLatch.await(10, TimeUnit.SECONDS));
        }
        assertTrue(context.await(10, TimeUnit.SECONDS));
        verify(strategyMock, times(msgCount)).onSuccess(any(UUID.class));
    }

}
