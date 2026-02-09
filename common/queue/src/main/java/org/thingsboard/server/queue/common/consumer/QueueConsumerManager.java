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
package org.thingsboard.server.queue.common.consumer;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueMsg;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

@Slf4j
public class QueueConsumerManager<M extends TbQueueMsg> {

    private final String name;
    private final MsgPackProcessor<M> msgPackProcessor;
    private final long pollInterval;
    private final ExecutorService consumerExecutor;
    private final String threadPrefix;

    @Getter
    private final TbQueueConsumer<M> consumer;
    private Future<?> consumerTask;
    private volatile boolean stopped;

    @Builder
    public QueueConsumerManager(String name, MsgPackProcessor<M> msgPackProcessor,
                                long pollInterval, Supplier<TbQueueConsumer<M>> consumerCreator,
                                ExecutorService consumerExecutor, String threadPrefix) {
        this.name = name;
        this.pollInterval = pollInterval;
        this.msgPackProcessor = msgPackProcessor;
        this.consumerExecutor = consumerExecutor;
        this.threadPrefix = threadPrefix;
        this.consumer = consumerCreator.get();
    }

    public void subscribe() {
        consumer.subscribe();
    }

    public void subscribe(Set<TopicPartitionInfo> partitions) {
        consumer.subscribe(partitions);
    }

    public void launch() {
        log.info("[{}] Launching consumer", name);
        consumerTask = consumerExecutor.submit(() -> {
            if (threadPrefix != null) {
                ThingsBoardThreadFactory.addThreadNamePrefix(threadPrefix);
            }
            try {
                consumerLoop(consumer);
            } catch (Throwable e) {
                log.error("Failure in consumer loop", e);
            }
            log.info("[{}] Consumer stopped", name);
        });
    }

    private void consumerLoop(TbQueueConsumer<M> consumer) {
        while (!stopped && !consumer.isStopped()) {
            try {
                List<M> msgs = consumer.poll(pollInterval);
                if (msgs.isEmpty()) {
                    continue;
                }
                msgPackProcessor.process(msgs, consumer);
            } catch (Exception e) {
                if (!consumer.isStopped()) {
                    log.warn("Failed to process messages from queue", e);
                    try {
                        Thread.sleep(pollInterval);
                    } catch (InterruptedException interruptedException) {
                        log.trace("Failed to wait until the server has capacity to handle new requests", interruptedException);
                    }
                }
            }
        }
    }

    public void stop() {
        log.debug("[{}] Stopping consumer", name);
        stopped = true;
        consumer.unsubscribe();
        try {
            if (consumerTask != null) {
                consumerTask.get(10, TimeUnit.SECONDS);
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("[{}] Failed to await consumer loop stop", name, e);
        }
    }

    public interface MsgPackProcessor<M extends TbQueueMsg> {
        void process(List<M> msgs, TbQueueConsumer<M> consumer) throws Exception;
    }

}
