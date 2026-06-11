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
package org.thingsboard.server.service.edge.rpc;

import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.edge.v1.ResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdgeEventNotificationMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.common.consumer.QueueConsumerManager;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.kafka.KafkaAdmin;
import org.thingsboard.server.queue.provider.TbCoreQueueFactory;
import org.thingsboard.server.service.edge.EdgeContextComponent;

import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KafkaEdgeGrpcSessionTest {

    private static final long POLL_INTERVAL_MS = 20L;

    private EdgeContextComponent ctx;
    private TbCoreQueueFactory tbCoreQueueFactory;
    private KafkaEdgeGrpcSession session;

    @BeforeEach
    void setUp() {
        ctx = mock(EdgeContextComponent.class);
        TopicService topicService = mock(TopicService.class);
        tbCoreQueueFactory = mock(TbCoreQueueFactory.class);
        KafkaAdmin kafkaAdmin = mock(KafkaAdmin.class);
        @SuppressWarnings("unchecked")
        StreamObserver<ResponseMsg> outputStream = mock(StreamObserver.class);

        session = new KafkaEdgeGrpcSession(ctx, topicService, tbCoreQueueFactory, kafkaAdmin, outputStream,
                (edgeId, s) -> {}, (edge, uuid) -> {}, null, 0, 0);

        ReflectionTestUtils.setField(session, "edge", new Edge(new EdgeId(UUID.randomUUID())));
        ReflectionTestUtils.setField(session, "tenantId", TenantId.fromUUID(UUID.randomUUID()));
    }

    @AfterEach
    void tearDown() {
        if (session != null) {
            session.destroy();
        }
    }

    @Test
    void readyOnlyWhenConnectedNotSyncingNotHighPriority() {
        setReadinessFlags(true, false, false);
        assertThat(isReadyToProcessGeneralEvents())
                .as("connected, not syncing, no high-priority work -> ready")
                .isTrue();
    }

    @Test
    void notReadyWhenDisconnected() {
        setReadinessFlags(false, false, false);
        assertThat(isReadyToProcessGeneralEvents())
                .as("disconnected -> not ready")
                .isFalse();
    }

    @Test
    void notReadyWhileSyncInProgress() {
        setReadinessFlags(true, true, false);
        assertThat(isReadyToProcessGeneralEvents())
                .as("sync in progress -> not ready (this is the window where events were being dropped)")
                .isFalse();
    }

    @Test
    void notReadyWhileHighPriorityProcessing() {
        setReadinessFlags(true, false, true);
        assertThat(isReadyToProcessGeneralEvents())
                .as("high-priority processing -> not ready")
                .isFalse();
    }

    @Test
    void processEdgeEventsWiresReadinessPredicateIntoConsumerGate() {
        // processEdgeEvents() builds the consumer lazily; stub just enough of that path.
        EdgeEventStorageSettings storageSettings = new EdgeEventStorageSettings();
        storageSettings.setNoRecordsSleepInterval(1000L);
        when(ctx.getEdgeEventStorageSettings()).thenReturn(storageSettings);

        @SuppressWarnings("unchecked")
        TbQueueConsumer<TbProtoQueueMsg<ToEdgeEventNotificationMsg>> queueConsumer = mock(TbQueueConsumer.class);
        // Report stopped so the launched consumer loop exits immediately - this test asserts on wiring, not polling.
        when(queueConsumer.isStopped()).thenReturn(true);
        when(tbCoreQueueFactory.createEdgeEventMsgConsumer(any(), any())).thenReturn(queueConsumer);

        // The consumer is only started when the session is ready.
        setReadinessFlags(true, false, false);
        session.processEdgeEvents();

        QueueConsumerManager<TbProtoQueueMsg<ToEdgeEventNotificationMsg>> manager = session.getConsumer();
        assertThat(manager).as("processEdgeEvents must build the consumer when ready").isNotNull();

        BooleanSupplier readinessCheck = (BooleanSupplier) ReflectionTestUtils.getField(manager, "readinessCheck");
        assertThat(readinessCheck)
                .as("the edge consumer must be wired with a readinessCheck (the .readinessCheck(...) builder line)")
                .isNotNull();

        // It must be the live predicate, not a snapshot: flipping the session's state must flip the gate.
        assertThat(readinessCheck.getAsBoolean()).as("ready session -> gate open").isTrue();
        setReadinessFlags(true, true, false);
        assertThat(readinessCheck.getAsBoolean()).as("sync starts -> gate closes, consumer pauses polling").isFalse();
    }

    @Test
    void eventArrivingDuringSyncIsHeldByTheEdgeConsumerUntilSyncCompletes() {
        EdgeEventStorageSettings storageSettings = new EdgeEventStorageSettings();
        storageSettings.setNoRecordsSleepInterval(POLL_INTERVAL_MS);
        when(ctx.getEdgeEventStorageSettings()).thenReturn(storageSettings);

        RecordingEdgeEventConsumer queueConsumer = new RecordingEdgeEventConsumer();
        when(tbCoreQueueFactory.createEdgeEventMsgConsumer(any(), any())).thenReturn(queueConsumer);

        // The consumer is launched only while the session is ready - that is how it starts in production.
        setReadinessFlags(true, false, false);
        session.processEdgeEvents();

        // Sync starts: the gate closes. Wait until the loop has actually parked on it (poll count stops advancing)
        // before enqueuing - otherwise we would race an in-flight poll() and the test would be non-deterministic.
        setReadinessFlags(true, true, false);
        awaitParkedOnClosedGate(queueConsumer);

        // An event lands in the edge-event topic during the sync window - exactly the case that used to be dropped.
        @SuppressWarnings("unchecked")
        TbProtoQueueMsg<ToEdgeEventNotificationMsg> event = mock(TbProtoQueueMsg.class);
        int pollsBeforeEvent = queueConsumer.getPollCount();
        queueConsumer.enqueue(List.of(event));

        // While sync is in progress the consumer stays parked: it neither polls nor consumes the event,
        // so the event survives in the queue instead of being read-and-skipped.
        sleepQuietly(POLL_INTERVAL_MS * 5);
        assertThat(queueConsumer.getPolledEvents())
                .as("event must not be polled while sync is in progress (it must stay queued, not be dropped)")
                .isEmpty();
        assertThat(queueConsumer.getPollCount())
                .as("consumer must not poll at all while the gate is closed")
                .isEqualTo(pollsBeforeEvent);

        // Sync completes: the gate opens and the held event is finally picked up by the consumer.
        // (We assert at the poll boundary - the actual drop point - since the downlink-send path that
        // processMsgs drives afterwards is not reachable from a unit test.)
        setReadinessFlags(true, false, false);
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(queueConsumer.getPolledEvents())
                        .as("event held during sync must be picked up once sync completes, not lost")
                        .hasSize(1));
    }

    private boolean isReadyToProcessGeneralEvents() {
        return Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(session, "isReadyToProcessGeneralEvents"));
    }

    private void setReadinessFlags(boolean connected, boolean syncInProgress, boolean highPriorityProcessing) {
        ReflectionTestUtils.setField(session, "connected", connected);
        ReflectionTestUtils.setField(session, "syncInProgress", syncInProgress);
        ReflectionTestUtils.setField(session, "isHighPriorityProcessing", highPriorityProcessing);
    }

    private static void awaitParkedOnClosedGate(RecordingEdgeEventConsumer consumer) {
        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            int before = consumer.getPollCount();
            // Several poll intervals with no new poll means the loop is parked on the readiness gate rather than
            // blocked inside poll(), so it is safe to enqueue without racing an in-flight read.
            sleepQuietly(POLL_INTERVAL_MS * 5);
            return consumer.getPollCount() == before;
        });
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Fake edge-event consumer that records what it was actually polled, so a test can prove an event stayed in the
     * queue while the readiness gate was closed and was only read once it opened.
     */
    private static class RecordingEdgeEventConsumer implements TbQueueConsumer<TbProtoQueueMsg<ToEdgeEventNotificationMsg>> {

        private final Queue<List<TbProtoQueueMsg<ToEdgeEventNotificationMsg>>> pending = new ConcurrentLinkedQueue<>();
        private final List<TbProtoQueueMsg<ToEdgeEventNotificationMsg>> polledEvents = new CopyOnWriteArrayList<>();
        private final AtomicInteger pollCount = new AtomicInteger();
        private volatile boolean stopped;

        void enqueue(List<TbProtoQueueMsg<ToEdgeEventNotificationMsg>> batch) {
            pending.add(batch);
        }

        int getPollCount() {
            return pollCount.get();
        }

        List<TbProtoQueueMsg<ToEdgeEventNotificationMsg>> getPolledEvents() {
            return polledEvents;
        }

        @Override
        public List<TbProtoQueueMsg<ToEdgeEventNotificationMsg>> poll(long durationInMillis) {
            pollCount.incrementAndGet();
            List<TbProtoQueueMsg<ToEdgeEventNotificationMsg>> batch = pending.poll();
            if (batch != null) {
                polledEvents.addAll(batch);
                return batch;
            }
            sleepQuietly(durationInMillis);
            return Collections.emptyList();
        }

        @Override
        public String getTopic() {
            return "test-edge-event-topic";
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
        public List<String> getFullTopicNames() {
            return Collections.emptyList();
        }

        @Override
        public Set<TopicPartitionInfo> getPartitions() {
            return Collections.emptySet();
        }

    }

}
