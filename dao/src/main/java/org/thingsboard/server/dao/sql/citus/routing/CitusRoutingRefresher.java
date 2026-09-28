// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus.routing;

import lombok.extern.slf4j.Slf4j;

/**
 * The single smart-routing refresh routine, shared by its two entry points: the fixed-delay
 * {@link CitusRoutingRefreshScheduler} tick and the failover-driven
 * {@link CitusFailoverRefreshTrigger}. Reconciling the worker pools and re-reading the placement
 * snapshot must always run as one unit and in one order, so the body lives here rather than in the
 * scheduler.
 *
 * <p>{@link CitusWorkerRegistry#refresh()} reconciles per-worker connection pools (workers can be
 * added, retired, re-addressed, or demoted behind an unchanged endpoint) and
 * {@link CitusShardPlacement#refresh()} re-reads bucket -> worker ownership (placements move on a
 * rebalance). Each call is individually try/caught: a transient coordinator hiccup logs a warning and
 * the other refresh still runs, so a single bad step never blocks the routine.
 *
 * <p><b>Order matters:</b> the worker registry is refreshed FIRST and the placement LAST. When a worker
 * is added and shards rebalance onto it, refreshing the registry first guarantees the new worker's pool
 * exists before any bucket is remapped to its {@code groupId}; until placement is refreshed, each bucket
 * keeps pointing at its old, still-pooled owner. A momentarily <i>stale</i> placement is correctness-safe
 * because Citus MX forwards to the true shard owner (one extra hop), whereas a placement pointing at a
 * not-yet-pooled worker would make {@code CitusShardRouter.forBucket(...)} -> {@code templateForGroup(...)}
 * throw a hard, no-fallback {@code IllegalStateException}. Hence placement must be refreshed LAST.
 *
 * <p><b>Concurrency.</b> A triggered refresh can race a scheduled one: {@code CitusWorkerRegistry.refresh()}
 * is {@code synchronized} and the placement refresh is a snapshot-swap, so the two serialize safely in
 * any order.
 */
@Slf4j
public class CitusRoutingRefresher {

    private final CitusShardPlacement placement;
    private final CitusWorkerRegistry workerRegistry;

    public CitusRoutingRefresher(CitusShardPlacement placement, CitusWorkerRegistry workerRegistry) {
        this.placement = placement;
        this.workerRegistry = workerRegistry;
    }

    public void refresh() {
        try {
            workerRegistry.refresh();
        } catch (Exception e) {
            log.warn("Citus worker registry refresh failed; keeping the existing worker pools until the " +
                    "next cycle.", e);
        }
        try {
            placement.refresh();
        } catch (Exception e) {
            log.warn("Citus shard placement refresh failed; keeping the previous placement snapshot until " +
                    "the next cycle.", e);
        }
    }
}
