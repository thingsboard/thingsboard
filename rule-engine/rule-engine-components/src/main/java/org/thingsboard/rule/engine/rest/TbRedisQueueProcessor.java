/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.rule.engine.rest;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ListOperations;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Data
@Slf4j
class TbRedisQueueProcessor {

    private static final int MAX_QUEUE_SIZE = Integer.MAX_VALUE;

    private final TbContext ctx;
    private final TbHttpClient httpClient;
    private final ExecutorService executor;
    private final ListOperations<String, Object> listOperations;
    private final String redisKey;
    private final boolean trimQueue;
    private final int maxQueueSize;

    private AtomicInteger failuresCounter;
    private Future future;

    TbRedisQueueProcessor(TbContext ctx, TbHttpClient httpClient, boolean trimQueue, int maxQueueSize) {
        this.ctx = ctx;
        this.httpClient = httpClient;
        this.executor = Executors.newSingleThreadExecutor();
        this.listOperations = ctx.getRedisTemplate().opsForList();
        this.redisKey = constructRedisKey();
        this.trimQueue = trimQueue;
        this.maxQueueSize = maxQueueSize;
        init();
    }

    private void init() {
        failuresCounter = new AtomicInteger(0);
        future = executor.submit(() -> {
            while (true) {
                if (failuresCounter.get() != 0 && failuresCounter.get() % 50 == 0) {
                    sleep("Target HTTP server is down...", 3);
                }
                if (listOperations.size(redisKey) > 0) {
                    List<Object> list = listOperations.range(redisKey, -10, -1);
                    list.forEach(obj -> {
                        TbMsg msg = TbMsg.fromBytes((byte[]) obj);
                        log.debug("Trying to send the message: {}", msg);
                        listOperations.remove(redisKey, -1, obj);
                        httpClient.processMessage(ctx, msg, this);
                    });
                } else {
                    sleep("Queue is empty, waiting for tasks!", 1);
                }
            }
        });
    }

    void destroy() {
        if (future != null) {
            future.cancel(true);
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    void push(TbMsg msg) {
        listOperations.leftPush(redisKey, TbMsg.toByteArray(msg));
        if (trimQueue) {
            listOperations.trim(redisKey, 0, validateMaxQueueSize());
        }
    }

    void pushOnFailure(TbMsg msg) {
        listOperations.rightPush(redisKey, TbMsg.toByteArray(msg));
        failuresCounter.incrementAndGet();
    }

    void resetCounter() {
        failuresCounter.set(0);
    }

    private String constructRedisKey() {
        return ctx.getServerAddress() + ctx.getSelfId();
    }

    private int validateMaxQueueSize() {
        if (maxQueueSize != 0) {
            return maxQueueSize;
        }
        return MAX_QUEUE_SIZE;
    }

    private void sleep(String logMessage, int sleepSeconds) {
        try {
            log.debug(logMessage);
            TimeUnit.SECONDS.sleep(sleepSeconds);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Thread interrupted!", e);
        }
    }
}
