// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus.routing;

/**
 * Thrown by {@link CitusWorkerRegistry#templateForGroup(int)} when no connection pool exists for the
 * requested worker group. Reachable in steady operation: a scheduled reconcile can log-and-skip a newly
 * discovered worker whose pool build fails (reachable from the coordinator, so present in
 * {@code pg_dist_node} and in the placement snapshot, but not yet reachable from the app), leaving
 * buckets mapped to an unpooled group.
 *
 * <p>Extends {@link IllegalStateException} so existing caller handling is unchanged; the dedicated type
 * exists so {@link CitusShardRouter#isFailoverFailure} can classify exactly this state as refresh-worthy
 * (healing it via the debounced failover trigger in seconds instead of waiting out the scheduled tick)
 * without treating every generic {@code IllegalStateException} as a failover signature.
 */
public class CitusNoWorkerPoolException extends IllegalStateException {

    public CitusNoWorkerPoolException(String message) {
        super(message);
    }
}
