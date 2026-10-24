// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus.routing;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Debounced, asynchronous entry point for failover-driven smart-routing refreshes.
 * {@link CitusShardRouter} calls {@link #requestRefresh} when a routed operation fails with a
 * failover signature (SQLSTATE {@code 25006} from a demoted worker, or a worker
 * connection-acquisition failure), collapsing the post-failover outage from "up to one scheduled
 * tick" (default 5min) to roughly one failed operation plus a pool rebuild: Patroni has typically
 * already rewritten {@code pg_dist_node} by the time the first pinned connection fails, so the
 * triggered {@link CitusRoutingRefresher} run picks the new leader up within about a second.
 *
 * <p><b>Debounce + at-most-one-pending.</b> At most one refresh runs per debounce window
 * ({@code database.citus.smart_routing.failover_refresh_debounce_ms}, default 10s), and at most one
 * is pending/running at any moment: a burst of failing routed operations (every KV op fails until the
 * pools heal) collapses into a single refresh, and while the catalog has not flipped yet, retries
 * happen at most once per window — no tight loop, no log storm, with the scheduled tick as the
 * backstop. Dropped requests are logged at debug.
 *
 * <p>The refresh runs on a dedicated single-thread daemon executor, NOT on the caller's thread: the
 * caller is a failing KV operation (write-queue flush or a routed read) that must rethrow immediately,
 * and {@code CitusWorkerRegistry.refresh()} can block on pool builds. {@link #close()}
 * ({@code @PreDestroy}) shuts the executor down.
 */
@Slf4j
public class CitusFailoverRefreshTrigger {

    private final CitusRoutingRefresher refresher;
    private final long debounceMs;
    private final Clock clock;
    private final Executor executor;

    /** True while a triggered refresh is pending or running; enforces at-most-one-pending. */
    private final AtomicBoolean inFlight = new AtomicBoolean();
    /** End of the current debounce window. Written only by the winner of the {@link #inFlight} CAS. */
    private volatile long windowEndMillis;

    public CitusFailoverRefreshTrigger(CitusRoutingRefresher refresher, long debounceMs) {
        this(refresher, debounceMs, Clock.systemUTC(), Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "citus-failover-refresh");
            thread.setDaemon(true);
            return thread;
        }));
    }

    // Package-private: lets tests inject a mutable clock and a synchronous executor for determinism.
    CitusFailoverRefreshTrigger(CitusRoutingRefresher refresher, long debounceMs, Clock clock, Executor executor) {
        this.refresher = refresher;
        this.debounceMs = debounceMs;
        this.clock = clock;
        this.executor = executor;
    }

    /**
     * Requests an asynchronous routing refresh, at most once per debounce window. Requests landing
     * inside the window, or while a refresh is already pending/running, are dropped (debug-logged).
     * Never throws and never blocks beyond the executor handoff — callers are failing routed
     * operations that must rethrow their original exception unchanged.
     */
    public void requestRefresh(String reason) {
        if (!inFlight.compareAndSet(false, true)) {
            log.debug("Dropping Citus failover refresh request (one already pending/running): {}", reason);
            return;
        }
        long now = clock.millis();
        if (now < windowEndMillis) {
            inFlight.set(false);
            log.debug("Dropping Citus failover refresh request (inside the {}ms debounce window): {}", debounceMs, reason);
            return;
        }
        windowEndMillis = now + debounceMs;
        log.info("Citus routing refresh triggered: {}", reason);
        try {
            executor.execute(() -> {
                try {
                    refresher.refresh();
                } finally {
                    inFlight.set(false);
                }
            });
        } catch (RejectedExecutionException e) {
            // Shutting down; the scheduled tick (or the next app start) covers it.
            inFlight.set(false);
            log.debug("Citus failover refresh request rejected (executor shut down): {}", reason);
        }
    }

    @PreDestroy
    public void close() {
        if (executor instanceof ExecutorService executorService) {
            executorService.shutdownNow();
        }
    }
}
