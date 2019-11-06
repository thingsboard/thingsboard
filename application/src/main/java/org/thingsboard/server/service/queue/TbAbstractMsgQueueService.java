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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.thingsboard.rule.engine.api.TbMsgQueueService;
import org.thingsboard.server.service.queue.strategy.TbMsgQueueHandlerStrategy;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public abstract class TbAbstractMsgQueueService implements TbMsgQueueService {

    @Value("${backpressure.timeout}")
    private long timeout;

    @Value("${backpressure.attempt}")
    private int attempt;

    @Autowired
    private TbMsgQueueHandlerStrategy handlerStrategy;

    private CountDownLatch countDownLatch;

    protected TbMsgQueuePack currentPack;

    protected final AtomicBoolean isAck = new AtomicBoolean(true);

    protected final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void ack(UUID msgId) {
        currentPack.ackMsg(msgId);
        if (currentPack.getAck().get()) {
            countDownLatch.countDown();
            isAck.set(true);
        }
    }

    public void send(TbMsgQueuePack msgPack) {
        //sending

        try {
            countDownLatch = new CountDownLatch(1);
            countDownLatch.await(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (currentPack.getAck().get()) {
            isAck.set(true);
        } else {
            currentPack = handlerStrategy.handleFailureMsgs(msgPack);
            retry(currentPack);
        }
    }

    private void retry(TbMsgQueuePack msgQueuePack) {
        if (msgQueuePack.getAck().get()) {
            isAck.set(true);
        } else if (msgQueuePack.getRetryAttempt().get() < attempt) {
            msgQueuePack.incrementRetryAttempt();
            msgQueuePack.getMsgs().forEach((id, msg) -> msg.incrementRetryAttempt());
            send(msgQueuePack);
        } else {
            msgQueuePack.getAck().set(true);
            isAck.set(true);
        }
    }
}
