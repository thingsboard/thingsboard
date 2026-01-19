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

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.DataConstants;
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

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class TbMsgPackProcessingContextTest {

    public static final int TIMEOUT = 10;
    ExecutorService executorService;

    @After
    public void tearDown() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    @Test
    public void testHighConcurrencyCase() throws InterruptedException {
        //log.warn("preparing the test...");
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
                //final String taskName = "" + uuid + " " + i;
                executorService.submit(() -> {
                    //log.warn("ready {}", taskName);
                    readyLatch.countDown();
                    try {
                        startLatch.await();
                    } catch (InterruptedException e) {
                        Assert.fail("failed to await");
                    }
                    //log.warn("go    {}", taskName);

                    context.onSuccess(uuid);

                    finishLatch.countDown();
                });
            }
            assertTrue(readyLatch.await(TIMEOUT, TimeUnit.SECONDS));
            Thread.yield();
            startLatch.countDown(); //run all-at-once submitted tasks
            assertTrue(finishLatch.await(TIMEOUT, TimeUnit.SECONDS));
        }
        assertTrue(context.await(TIMEOUT, TimeUnit.SECONDS));
        verify(strategyMock, times(msgCount)).onSuccess(any(UUID.class));
    }
}
