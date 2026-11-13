// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.session;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.gen.agent.v1.ServerToAgent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class BaseAgentSessionTest {

    private static final int MAX_PENDING = 3;

    private final ListeningExecutorService writer = MoreExecutors.newDirectExecutorService();
    private TestObserver observer;
    private BaseAgentSession session;

    @BeforeEach
    void setUp() {
        observer = new TestObserver();
        session = new BaseAgentSession(writer, observer, MAX_PENDING);
    }

    @Test
    void readyObserverDrainsEveryPushedMessage() {
        assertThat(session.push(msg("a"))).isTrue();
        assertThat(session.push(msg("b"))).isTrue();

        assertThat(observer.sent).hasSize(2);
    }

    @Test
    void pushIsDeclinedAtCapacityAndAcceptedAgainAfterTheDrain() {
        observer.ready = false;

        for (int i = 0; i < MAX_PENDING; i++) {
            assertThat(session.push(msg("m" + i))).isTrue();
        }
        assertThat(session.push(msg("overflow"))).isFalse();
        assertThat(observer.sent).isEmpty();

        observer.ready = true;
        session.drainIfPossible();

        assertThat(observer.sent).hasSize(MAX_PENDING);
        assertThat(session.push(msg("after-drain"))).isTrue();
    }

    @Test
    void drainStopsWhenTheObserverIsNotReadyAndResumesOnTheReadyHandler() {
        observer.ready = false;
        session.push(msg("a"));
        session.push(msg("b"));
        assertThat(observer.sent).isEmpty();

        observer.ready = true;
        // this is what AgentGrpcService wires into setOnReadyHandler
        session.drainIfPossible();

        assertThat(observer.sent).hasSize(2);
    }

    @Test
    void pushIsDeclinedOnceTheSessionIsClosed() {
        session.closeSilently();

        assertThat(session.push(msg("a"))).isFalse();
        assertThat(observer.sent).isEmpty();
    }

    @Test
    void closeSilentlyReleasesThePermitsOfTheMessagesItDrops() {
        observer.ready = false;
        for (int i = 0; i < MAX_PENDING; i++) {
            session.push(msg("m" + i));
        }
        assertThat(availablePermits()).isZero();

        session.closeSilently();

        assertThat(availablePermits()).isEqualTo(MAX_PENDING);
    }

    private int availablePermits() {
        Semaphore permits = (Semaphore) ReflectionTestUtils.getField(session, "pendingPermits");
        return permits.availablePermits();
    }

    @Test
    void completeDrainsWhatIsQueuedAndThenCompletesTheStreamExactlyOnce() {
        observer.ready = false;
        session.push(msg("a"));

        session.complete();
        assertThat(observer.completed).isZero();

        observer.ready = true;
        session.drainIfPossible();

        assertThat(observer.sent).hasSize(1);
        assertThat(observer.completed).isOne();

        session.complete();
        assertThat(observer.completed).isOne();
    }

    @Test
    void errorClosesTheStreamWithTheReportedStatus() {
        session.onError(Status.INTERNAL.withDescription("boom"));

        assertThat(observer.errors).hasSize(1);
        assertThat(Status.fromThrowable(observer.errors.get(0)).getCode()).isEqualTo(Status.Code.INTERNAL);
        assertThat(session.push(msg("a"))).isFalse();
    }

    @Test
    void schedulingAWatchdogTwiceForOneEventLeavesASingleLiveFuture() throws Exception {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        try {
            AgentAppEventId eventId = new AgentAppEventId(UUID.randomUUID());
            AtomicInteger fired = new AtomicInteger();
            CountDownLatch start = new CountDownLatch(1);
            int threads = 8;
            CountDownLatch done = new CountDownLatch(threads);

            for (int i = 0; i < threads; i++) {
                new Thread(() -> {
                    try {
                        start.await();
                        session.scheduleEventWatchdog(eventId, scheduler, fired::incrementAndGet, 300, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                }).start();
            }
            start.countDown();
            assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();

            session.cancelEventWatchdog(eventId);
            Thread.sleep(600);

            assertThat(fired.get()).as("a watchdog orphaned by a concurrent schedule fires after the cancel").isZero();
        } finally {
            scheduler.shutdownNow();
        }
    }

    @Test
    void closingTheSessionCancelsWatchdogsScheduledConcurrentlyWithTheClose() throws Exception {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        try {
            AgentAppEventId eventId = new AgentAppEventId(UUID.randomUUID());
            AtomicInteger fired = new AtomicInteger();

            session.closeSilently();
            session.scheduleEventWatchdog(eventId, scheduler, fired::incrementAndGet, 200, TimeUnit.MILLISECONDS);
            Thread.sleep(500);

            assertThat(fired.get()).isZero();
        } finally {
            scheduler.shutdownNow();
        }
    }

    private static ServerToAgent msg(String unitId) {
        return ServerToAgent.newBuilder()
                .setStopLogStream(org.thingsboard.server.gen.agent.v1.StopLogStream.newBuilder().setUnitId(unitId))
                .build();
    }

    private static class TestObserver extends ServerCallStreamObserver<ServerToAgent> {

        private final List<ServerToAgent> sent = new ArrayList<>();
        private final List<Throwable> errors = new ArrayList<>();
        private volatile boolean ready = true;
        private volatile boolean cancelled;
        private int completed;

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void setOnCancelHandler(Runnable onCancelHandler) {
        }

        @Override
        public void setCompression(String compression) {
        }

        @Override
        public boolean isReady() {
            return ready;
        }

        @Override
        public void setOnReadyHandler(Runnable onReadyHandler) {
        }

        @Override
        public void disableAutoInboundFlowControl() {
        }

        @Override
        public void request(int count) {
        }

        @Override
        public void setMessageCompression(boolean enable) {
        }

        @Override
        public void onNext(ServerToAgent value) {
            sent.add(value);
        }

        @Override
        public void onError(Throwable t) {
            errors.add(t);
        }

        @Override
        public void onCompleted() {
            completed++;
        }
    }
}
