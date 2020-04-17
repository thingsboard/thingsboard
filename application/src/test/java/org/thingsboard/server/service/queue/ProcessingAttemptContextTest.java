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
public class ProcessingAttemptContextTest {

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
            ProcessingAttemptContext context = new ProcessingAttemptContext(strategyMock);
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
