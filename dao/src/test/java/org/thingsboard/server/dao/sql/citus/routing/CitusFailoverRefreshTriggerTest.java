// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus.routing;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class CitusFailoverRefreshTriggerTest {

    private static final long DEBOUNCE_MS = 10_000L;

    /** Mutable test clock: {@code millis()} returns whatever the test last set. */
    private static class MutableClock extends Clock {
        private long millis;

        void set(long millis) {
            this.millis = millis;
        }

        @Override
        public long millis() {
            return millis;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(millis);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    private final CitusRoutingRefresher refresher = mock(CitusRoutingRefresher.class);
    private final MutableClock clock = new MutableClock();

    /** Synchronous executor: the refresh runs inline, so each requestRefresh's effect is immediately observable. */
    private final Executor synchronousExecutor = Runnable::run;

    private CitusFailoverRefreshTrigger trigger() {
        return new CitusFailoverRefreshTrigger(refresher, DEBOUNCE_MS, clock, synchronousExecutor);
    }

    @Test
    void twoRequestsInsideTheWindowRunOneRefresh() {
        CitusFailoverRefreshTrigger trigger = trigger();
        clock.set(1_000L);

        trigger.requestRefresh("worker group 1 returned 25006");
        clock.set(1_000L + DEBOUNCE_MS - 1);
        trigger.requestRefresh("worker group 1 returned 25006 again");

        verify(refresher, times(1)).refresh();
    }

    @Test
    void requestPastTheWindowRunsASecondRefresh() {
        CitusFailoverRefreshTrigger trigger = trigger();
        clock.set(1_000L);

        trigger.requestRefresh("first failover");
        clock.set(1_000L + DEBOUNCE_MS);
        trigger.requestRefresh("catalog still not flipped");

        verify(refresher, times(2)).refresh();
    }

    @Test
    void requestWhileARefreshIsRunningIsDropped() {
        // A refresh that itself issues a nested request models a request arriving mid-refresh: the
        // in-flight guard must drop it (at-most-one-pending), not re-enter refresh(). The counter lives
        // OUTSIDE the refresher so the final assertion also fails if the outer request never ran at all
        // (a vacuous pass an in-refresh-only assertion could not catch).
        AtomicInteger refreshes = new AtomicInteger();
        CitusFailoverRefreshTrigger[] triggerHolder = new CitusFailoverRefreshTrigger[1];
        CitusRoutingRefresher reentrant = new CitusRoutingRefresher(mock(CitusShardPlacement.class), mock(CitusWorkerRegistry.class)) {
            @Override
            public void refresh() {
                refreshes.incrementAndGet();
                triggerHolder[0].requestRefresh("request landing while a refresh is running");
            }
        };
        triggerHolder[0] = new CitusFailoverRefreshTrigger(reentrant, DEBOUNCE_MS, clock, synchronousExecutor);
        clock.set(1_000L);

        triggerHolder[0].requestRefresh("first failover");

        assertThat(refreshes.get())
                .as("the outer request must have run exactly once and the nested request must not re-enter")
                .isEqualTo(1);
    }

    @Test
    void pendingGuardIsReleasedEvenWhenTheRefreshThrows() {
        // The refresher's public contract is to isolate failures internally, but the guard release must
        // not depend on that: a throwing refresh (synchronous executor propagates it here; the real
        // executor would swallow it on its own thread) must still allow the next-window request to run.
        CitusFailoverRefreshTrigger trigger = trigger();
        doThrow(new RuntimeException("refresh blew up")).doNothing().when(refresher).refresh();
        clock.set(1_000L);

        assertThatThrownBy(() -> trigger.requestRefresh("first failover")).hasMessage("refresh blew up");

        clock.set(1_000L + DEBOUNCE_MS);
        trigger.requestRefresh("after the window");

        verify(refresher, times(2)).refresh();
    }

    @Test
    void rejectedExecutionIsDroppedNotThrown() {
        Executor rejecting = runnable -> {
            throw new RejectedExecutionException("shut down");
        };
        CitusFailoverRefreshTrigger trigger = new CitusFailoverRefreshTrigger(refresher, DEBOUNCE_MS, clock, rejecting);
        clock.set(1_000L);

        assertThatCode(() -> trigger.requestRefresh("failover during shutdown"))
                .as("a rejected handoff must never surface to the failing routed operation")
                .doesNotThrowAnyException();
        verify(refresher, never()).refresh();
    }

    @Test
    void realExecutorIsShutDownOnCloseAndLaterRequestsAreDropped() throws InterruptedException {
        // Uses the production constructor (real single-thread daemon executor) to pin @PreDestroy behavior.
        CitusFailoverRefreshTrigger trigger = new CitusFailoverRefreshTrigger(refresher, 0L);

        trigger.close();

        assertThatCode(() -> trigger.requestRefresh("after shutdown")).doesNotThrowAnyException();
        Thread.sleep(50); // nothing should have been scheduled; give a hypothetical stray task time to surface
        verify(refresher, never()).refresh();
    }
}
