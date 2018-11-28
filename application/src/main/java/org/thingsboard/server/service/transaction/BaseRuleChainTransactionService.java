/**
 * Copyright © 2016-2018 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.transaction;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.RuleChainTransactionService;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

@Service
@Slf4j
public class BaseRuleChainTransactionService implements RuleChainTransactionService {

    @Value("${actors.rule.transaction.queue_size}")
    private int finalQueueSize;

    private final Lock transactionLock = new ReentrantLock();
    private final ConcurrentMap<EntityId, BlockingQueue<TbTransactionTask>> transactionMap = new ConcurrentHashMap<>();

    //TODO: add delete on timeout from queue -> onFailure accept

    @Override
    public void beginTransaction(TbContext ctx, TbMsg msg, Consumer<TbMsg> onStart, Consumer<TbMsg> onEnd, Consumer<Throwable> onFailure) {
        BlockingQueue<TbTransactionTask> queue = transactionMap.computeIfAbsent(msg.getTransactionData().getOriginatorId(), id ->
                new LinkedBlockingQueue<>(finalQueueSize));
        transactionLock.lock();
        try {
            TbTransactionTask task = new TbTransactionTask(msg, onStart, onEnd, onFailure);
            int queueSize = queue.size();
            if (queueSize >= finalQueueSize) {
                onFailure.accept(new RuntimeException("Queue has no space!"));
            } else {
                addMsgToQueue(queue, task, onFailure);
                if (queueSize == 0) {
                    onStart.accept(msg);
                } else {
                    log.info("Msg [{}] [{}] is waiting to start transaction!", msg.getId(), msg.getType());
                }
            }
        } finally {
            transactionLock.unlock();
        }
    }

    private void addMsgToQueue(BlockingQueue<TbTransactionTask> queue, TbTransactionTask task, Consumer<Throwable> onFailure) {
        try {
            queue.add(task);
            log.info("Added msg to queue, size: [{}]", queue.size());
        } catch (Exception e) {
            log.error("Error when trying to add msg [{}] to the queue", task.getMsg(), e);
            onFailure.accept(e);
        }
    }

    @Override
    public boolean endTransaction(TbContext ctx, TbMsg msg, Consumer<Throwable> onFailure) {
        transactionLock.lock();
        try {
            BlockingQueue<TbTransactionTask> queue = transactionMap.get(msg.getTransactionData().getOriginatorId());
            try {
                TbTransactionTask currentTask = queue.element();
                if (currentTask.getMsg().getTransactionData().getTransactionId().equals(msg.getTransactionData().getTransactionId())) {
                    queue.remove();
                    log.info("Removed msg from queue, size [{}]", queue.size());
                    currentTask.getOnEnd().accept(currentTask.getMsg());

                    TbTransactionTask nextTask = queue.peek();
                    if (nextTask != null) {
                        nextTask.getOnStart().accept(nextTask.getMsg());
                    }
                }
            } catch (Exception e) {
                log.error("Queue is empty!", queue);
                onFailure.accept(e);
                return true;
            }
        } finally {
            transactionLock.unlock();
        }
        return false;
    }
}
