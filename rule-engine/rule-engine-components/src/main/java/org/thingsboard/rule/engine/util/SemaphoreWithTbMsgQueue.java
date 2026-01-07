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
package org.thingsboard.rule.engine.util;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.function.BiFunction;

/**
 * A utility class designed to manage a queue of messages for a specific entity, ensuring that
 * message processing is synchronized on a per-entity basis. This is achieved through the use of a semaphore,
 * allowing only one message at a time to be processed for each entity ID, thus preventing race conditions
 * and ensuring thread-safe operations.
 * <p>
 * This class is especially useful in scenarios where the order of message processing and
 * resource access synchronization are crucial, such as updating caches or databases in a concurrent environment.
 */
@Data
@Slf4j
public class SemaphoreWithTbMsgQueue {

    private final EntityId entityId;
    private final Semaphore semaphore = new Semaphore(1);
    private final Queue<TbMsgTbContextBiFunction> queue = new ConcurrentLinkedQueue<>();

    /**
     * Adds a message to the queue for asynchronous processing and attempts to process the queue if possible.
     * This method is thread-safe and ensures that messages are processed in the order they were added,
     * with each message for a specific entity being processed one at a time due to the semaphore control.
     *
     * @param msg                   The message to be processed.
     * @param ctx                   The context in which the message should be processed.
     * @param msgProcessingFunction The function that defines how the message will be processed.
     */
    public void addToQueueAndTryProcess(TbMsg msg, TbContext ctx, BiFunction<TbContext, TbMsg, ListenableFuture<TbMsg>> msgProcessingFunction) {
        queue.add(new TbMsgTbContextBiFunction(msg, ctx, msgProcessingFunction));
        tryProcessQueue();
    }

    /**
     * Attempts to process the next message in the queue. If the semaphore is available (indicating
     * that no other message for the same entity is currently being processed), this method will
     * acquire the semaphore and start processing the message. If the semaphore is not available,
     * this method will return immediately, ensuring that messages are processed sequentially
     * for each entity.
     * <p>
     * This method is automatically called after adding a message to the queue to ensure
     * that the queue is processed promptly.
     */
    private void tryProcessQueue() {
        while (!queue.isEmpty()) {
            // The semaphore have to be acquired before EACH poll and released before NEXT poll.
            // Otherwise, some message will remain unprocessed in queue
            if (!semaphore.tryAcquire()) {
                return;
            }
            TbMsgTbContextBiFunction tbMsgTbContext = null;
            try {
                tbMsgTbContext = queue.poll();
                if (tbMsgTbContext == null) {
                    semaphore.release();
                    continue;
                }
                final TbMsg msg = tbMsgTbContext.msg();
                if (!msg.getCallback().isMsgValid()) {
                    log.trace("[{}] Skipping non-valid message [{}]", entityId, msg);
                    semaphore.release();
                    continue;
                }
                //DO PROCESSING
                final TbContext ctx = tbMsgTbContext.ctx();
                final ListenableFuture<TbMsg> resultMsgFuture = tbMsgTbContext.biFunction().apply(ctx, msg);
                DonAsynchron.withCallback(resultMsgFuture, resultMsg -> {
                    try {
                        ctx.tellSuccess(resultMsg);
                    } finally {
                        semaphore.release();
                        tryProcessQueue();
                    }
                }, t -> {
                    try {
                        ctx.tellFailure(msg, t);
                    } finally {
                        semaphore.release();
                        tryProcessQueue();
                    }
                }, ctx.getDbCallbackExecutor());
            } catch (Throwable t) {
                semaphore.release();
                if (tbMsgTbContext == null) { // if no message polled, the loop become infinite, will throw exception
                    log.error("[{}] Failed to process TbMsgTbContext queue", entityId, t);
                    throw t;
                }
                TbMsg msg = tbMsgTbContext.msg();
                TbContext ctx = tbMsgTbContext.ctx();
                log.debug("[{}] Failed to process message: {}", entityId, msg, t);
                ctx.tellFailure(msg, t); // you are not allowed to throw here, because queue will remain unprocessed
                continue; // We are probably the last who process the queue. We have to continue poll until get successful callback or queue is empty
            }
            break; //submitted async exact one task. next poll will try on callback
        }
    }

    /**
     * A utility record to hold the tuple of a {@link TbMsg}, {@link TbContext}, and the message processing function.
     * This facilitates passing these three elements as a single object within the queue.
     */
    private record TbMsgTbContextBiFunction(TbMsg msg, TbContext ctx,
                                            BiFunction<TbContext, TbMsg, ListenableFuture<TbMsg>> biFunction) {
    }

}
