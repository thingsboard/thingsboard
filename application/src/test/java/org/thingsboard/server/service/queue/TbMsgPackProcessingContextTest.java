/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.service.queue.processing.TbRuleEngineSubmitStrategy;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class TbMsgPackProcessingContextTest {

    @Test
    public void testHighConcurrencyCase() throws InterruptedException {
        TbRuleEngineSubmitStrategy strategyMock = mock(TbRuleEngineSubmitStrategy.class);
        int msgCount = 1000;
        int parallelCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(parallelCount);
        try {
            ConcurrentMap<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> messages = new ConcurrentHashMap<>();
            for (int i = 0; i < msgCount; i++) {
                messages.put(UUID.randomUUID(), new TbProtoQueueMsg<>(UUID.randomUUID(), null));
            }
            when(strategyMock.getPendingMap()).thenReturn(messages);
            TbMsgPackProcessingContext context = new TbMsgPackProcessingContext("Main", strategyMock);
            for (UUID uuid : messages.keySet()) {
                for (int i = 0; i < parallelCount; i++) {
                    executorService.submit(() -> context.onSuccess(uuid));
                }
            }
            Assert.assertTrue(context.await(10, TimeUnit.SECONDS));
            Mockito.verify(strategyMock, Mockito.times(msgCount)).onSuccess(Mockito.any(UUID.class));
        } finally {
            executorService.shutdownNow();
        }
    }
}
