/**
 * Copyright © 2016-2019 The Thingsboard Authors
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
import org.springframework.beans.factory.annotation.Value;
import org.thingsboard.rule.engine.api.TbMsgQueueService;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgPack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public abstract class TbAbstractMsgQueueService implements TbMsgQueueService {

    @Value("${backpressure.timeout}")
    private long timeout;

    @Value("${backpressure.strategy}")
    private Strategy strategy;

    @Value("${backpressure.attempt}")
    private int attempt;

    private CountDownLatch countDownLatch;

    protected final Map<UUID, TbMsg> map = new ConcurrentHashMap<>();

    protected final ExecutorService executor = Executors.newSingleThreadExecutor();

    protected final AtomicInteger currentAttempt = new AtomicInteger(0);

    @Override
    public void ack(UUID msgId) {
        map.remove(msgId);
        if (map.isEmpty()) {
            countDownLatch.countDown();
        }
    }

    protected void send(TbMsgPack msgPack) {
        //sending

        try {
            countDownLatch = new CountDownLatch(1);
            countDownLatch.await(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        processingAfterSend();
    }

    private void processingAfterSend() {
        if (!map.isEmpty()) {
            switch (strategy) {
                case RETRY:
                    retry();
                    break;
                case IGNORE:
                    autoAcknowledgePack();
                    break;
            }
        }
    }

    private void retry() {
        if (currentAttempt.get() < attempt) {
            currentAttempt.incrementAndGet();
            List<TbMsg> msgs = new ArrayList<>(map.size());
            map.forEach((k, v) -> msgs.add(v));
            TbMsgPack pack = new TbMsgPack(UUID.randomUUID(), msgs);
            send(pack);
        }
    }

    private void autoAcknowledgePack() {
        map.clear();
    }
}
