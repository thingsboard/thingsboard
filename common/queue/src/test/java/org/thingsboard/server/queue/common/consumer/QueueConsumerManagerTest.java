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

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueMsg;

import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;

@Slf4j
class QueueConsumerManagerTest {

    private static final long POLL_INTERVAL_MS = 20L;
    // Before asserting the consumer never polled we wait until the loop has evaluated the readiness gate at least
    // this many times. That proves the consumer thread is actually running and deciding not to poll, rather than the
    // assertion passing vacuously because the thread simply has not started yet.
    private static final int MIN_READINESS_CHECKS = 3;

    private final AtomicBoolean readyToProcess = new AtomicBoolean(false);
    private final AtomicInteger readinessChecks = new AtomicInteger();
    private final TestQueueConsumer consumer = new TestQueueConsumer();
    private ExecutorService consumerExecutor;
    private QueueConsumerManager<TbQueueMsg> manager;

    @AfterEach
    void tearDown() {
        if (manager != null) {
            manager.stop();
        }
        if (consumerExecutor != null) {
            consumerExecutor.shutdownNow();
        }
    }

    @Test
    void eventQueuedWhileNotReadyIsDeliveredAfterReadinessGateOpensInsteadOfBeingDropped() {
        List<TbQueueMsg> delivered = new CopyOnWriteArrayList<>();

        consumer.enqueue(List.of(mock(TbQueueMsg.class)));

        // The processor is unconditional: only the readiness gate may hold the event back, so delivery proves the
        // gate (not the processor) is what kept the event queued while not ready.
        manager = launchManager(consumer, countingReadiness(readyToProcess, readinessChecks), (msgs, c) -> {
            delivered.addAll(msgs);
            c.commit();
        });

        // The loop is running and repeatedly evaluating the gate during the not-ready (sync) window...
        awaitReadinessGateEvaluated(readinessChecks);
        // ...yet the queued event is neither polled nor delivered - it stays in the queue rather than being dropped.
        assertThat(consumer.getPollCount())
                .as("consumer must not poll while not ready")
                .isZero();
        assertThat(delivered)
                .as("event must not be delivered while not ready")
                .isEmpty();

        // Sync completes - the processor becomes ready.
        readyToProcess.set(true);

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(delivered)
                        .as("event queued during the not-ready window must be delivered, not dropped")
                        .hasSize(1));
    }

    @Test
    void consumerIsNotPolledWhileNotReadyToProcess() {
        manager = launchManager(consumer, countingReadiness(readyToProcess, readinessChecks), (msgs, c) -> c.commit());

        awaitReadinessGateEvaluated(readinessChecks);
        assertThat(consumer.getPollCount())
                .as("consumer must not be polled while not ready to process")
                .isZero();

        readyToProcess.set(true);
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(consumer.getPollCount())
                        .as("consumer resumes polling once ready")
                        .isPositive());
    }

    @Test
    void consumerWithoutReadinessCheckPollsAndDeliversImmediately() {
        List<TbQueueMsg> delivered = new CopyOnWriteArrayList<>();

        consumer.enqueue(List.of(mock(TbQueueMsg.class)));

        // No readiness gate configured - the consumer must default to "always ready", preserving the behaviour every
        // consumer that does not opt in relies on.
        manager = launchManager(consumer, null, (msgs, c) -> {
            delivered.addAll(msgs);
            c.commit();
        });

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(delivered)
                        .as("consumer without a readiness gate must poll and deliver immediately")
                        .hasSize(1));
    }

    @Test
    void consumerLoopExitsWhenInterruptedWhileNotReady() throws Exception {
        manager = launchManager(consumer, countingReadiness(readyToProcess, readinessChecks), (msgs, c) -> c.commit());

        // The loop is parked in the not-ready wait...
        awaitReadinessGateEvaluated(readinessChecks);

        // ...interrupting the worker (as shutdownNow does on stop) must end the loop, not spin or hang.
        consumerExecutor.shutdownNow();
        assertThat(consumerExecutor.awaitTermination(5, TimeUnit.SECONDS))
                .as("consumer loop must exit when interrupted while waiting to become ready")
                .isTrue();
    }

    private static void awaitReadinessGateEvaluated(AtomicInteger readinessChecks) {
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(readinessChecks.get())
                        .as("consumer loop must be running and repeatedly evaluating the readiness gate")
                        .isGreaterThanOrEqualTo(MIN_READINESS_CHECKS));
    }

    private static BooleanSupplier countingReadiness(AtomicBoolean ready, AtomicInteger readinessChecks) {
        return () -> {
            readinessChecks.incrementAndGet();
            return ready.get();
        };
    }

    private QueueConsumerManager<TbQueueMsg> launchManager(TestQueueConsumer consumer, BooleanSupplier readinessCheck,
                                                           QueueConsumerManager.MsgPackProcessor<TbQueueMsg> processor) {
        consumerExecutor = Executors.newSingleThreadExecutor();
        QueueConsumerManager<TbQueueMsg> queueConsumerManager = QueueConsumerManager.<TbQueueMsg>builder()
                .name("test-consumer")
                .pollInterval(POLL_INTERVAL_MS)
                .consumerCreator(() -> consumer)
                .consumerExecutor(consumerExecutor)
                .readinessCheck(readinessCheck)
                .msgPackProcessor(processor)
                .build();
        queueConsumerManager.subscribe();
        queueConsumerManager.launch();
        return queueConsumerManager;
    }

    private static class TestQueueConsumer implements TbQueueConsumer<TbQueueMsg> {

        private final Queue<List<TbQueueMsg>> batches = new ConcurrentLinkedQueue<>();
        private final AtomicInteger pollCount = new AtomicInteger();
        private volatile boolean stopped;

        void enqueue(List<TbQueueMsg> batch) {
            batches.add(batch);
        }

        int getPollCount() {
            return pollCount.get();
        }

        @Override
        public List<TbQueueMsg> poll(long durationInMillis) {
            pollCount.incrementAndGet();
            List<TbQueueMsg> batch = batches.poll();
            if (batch != null) {
                return batch;
            }
            try {
                Thread.sleep(durationInMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return Collections.emptyList();
        }

        @Override
        public String getTopic() {
            return "test-topic";
        }

        @Override
        public void subscribe() {
        }

        @Override
        public void subscribe(Set<TopicPartitionInfo> partitions) {
        }

        @Override
        public void stop() {
            stopped = true;
        }

        @Override
        public void unsubscribe() {
            stopped = true;
        }

        @Override
        public void commit() {
        }

        @Override
        public boolean isStopped() {
            return stopped;
        }

        @Override
        public Set<TopicPartitionInfo> getPartitions() {
            return Collections.emptySet();
        }

        @Override
        public List<String> getFullTopicNames() {
            return Collections.emptyList();
        }

    }

}
