// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
    private final Object key;
    private volatile TbQueueConsumer<M> consumer;
    private volatile Supplier<TbQueueConsumer<M>> consumerSupplier;
    @Getter
    private final Runnable callback;

    @Setter
    private Future<?> task;

    public TbQueueConsumerTask(Object key, Supplier<TbQueueConsumer<M>> consumerSupplier, Runnable callback) {
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

}
