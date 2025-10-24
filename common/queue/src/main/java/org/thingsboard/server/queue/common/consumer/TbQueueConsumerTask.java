/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.queue.common.consumer;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueMsg;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
public class TbQueueConsumerTask<M extends TbQueueMsg> {

    @Getter
    private final ConsumerKey key;
    private volatile TbQueueConsumer<M> consumer;
    private volatile Supplier<TbQueueConsumer<M>> consumerSupplier;
    @Getter
    private final Runnable callback;

    @Setter
    private Future<?> task;

    public TbQueueConsumerTask(ConsumerKey key, Supplier<TbQueueConsumer<M>> consumerSupplier, Runnable callback) {
        this.key = key;
        this.consumer = null;
        this.consumerSupplier = consumerSupplier;
        this.callback = callback;
    }

    public TbQueueConsumer<M> getConsumer() {
        if (consumer == null) {
            synchronized (this) {
                if (consumer == null) {
                    Objects.requireNonNull(consumerSupplier, "consumerSupplier for key [" + key + "] is null");
                    consumer = consumerSupplier.get();
                    Objects.requireNonNull(consumer, "consumer for key [" + key + "] is null");
                    consumerSupplier = null;
                }
            }
        }
        return consumer;
    }

    public void subscribe(Set<TopicPartitionInfo> partitions) {
        log.trace("[{}] Subscribing to partitions: {}", key, partitions);
        getConsumer().subscribe(partitions);
    }

    public void initiateStop() {
        log.debug("[{}] Initiating stop", key);
        getConsumer().stop();
    }

    public void awaitCompletion() {
        awaitCompletion(30);
    }

    public void awaitCompletion(int timeoutSec) {
        log.trace("[{}] Awaiting finish", key);
        if (isRunning()) {
            try {
                if (timeoutSec > 0) {
                    task.get(timeoutSec, TimeUnit.SECONDS);
                } else {
                    task.get();
                }
                log.trace("[{}] Awaited finish", key);
            } catch (Exception e) {
                log.warn("[{}] Failed to await for consumer to stop (timeout {} sec)", key, timeoutSec, e);
            }
            task = null;
        }
    }

    public boolean isRunning() {
        return task != null;
    }

    public record ConsumerKey(Object queueKey, TopicPartitionInfo partition) {

        @Override
        public String toString() {
            if (partition != null) {
                Integer partitionId = partition.getPartition().orElse(-1);
                return queueKey + "-" + partitionId;
            } else {
                return queueKey.toString();
            }
        }

    }

}
