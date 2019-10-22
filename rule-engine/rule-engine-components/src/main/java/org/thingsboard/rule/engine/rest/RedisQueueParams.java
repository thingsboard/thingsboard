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
package org.thingsboard.rule.engine.rest;

import lombok.Data;
import org.springframework.data.redis.core.ListOperations;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Data
public class RedisQueueParams {

    private final ExecutorService executor;
    private final ListOperations<String, Object> listOperations;

    private final String redisKey;
    private final boolean trimQueue;
    private final int maxQueueSize;

    private Future future;

    RedisQueueParams(ListOperations<String, Object> listOperations, String redisKey, boolean trimQueue, int maxQueueSize) {
        this.executor = Executors.newSingleThreadExecutor();
        this.listOperations = listOperations;
        this.redisKey = redisKey;
        this.trimQueue = trimQueue;
        this.maxQueueSize = maxQueueSize;
    }

    public void destroy() {
        if (future != null) {
            future.cancel(true);
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}
