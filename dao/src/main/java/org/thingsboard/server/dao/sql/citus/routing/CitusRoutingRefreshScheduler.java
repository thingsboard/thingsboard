// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus.routing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Periodically runs the {@link CitusRoutingRefresher} routine (worker pools first, placement last —
 * see the refresher's Javadoc for the ordering contract) so a Citus rebalance or a worker failover is
 * picked up without a restart.
 *
 * <p>The cadence is {@code database.citus.smart_routing.placement_refresh_interval_ms} (default 5min).
 * The refresher isolates each step's failures internally, so a single bad cycle never stops future
 * cycles. This scheduled tick is the BACKSTOP: after a worker failover the fast path is
 * {@link CitusFailoverRefreshTrigger}, which runs the same refresher within about a second of the
 * first failed routed operation.
 *
 * <p>The startup bucket-alignment assertion ({@code CitusShardRouter.assertBucketAlignment}) is the
 * hard gate and is deliberately NOT re-run on this scheduled path: a transient mid-rebalance count
 * skew must not crash an already-running application.
 */
@Slf4j
public class CitusRoutingRefreshScheduler {

    private final CitusRoutingRefresher refresher;

    public CitusRoutingRefreshScheduler(CitusRoutingRefresher refresher) {
        this.refresher = refresher;
    }

    @Scheduled(fixedDelayString = "${database.citus.smart_routing.placement_refresh_interval_ms:300000}")
    public void refresh() {
        refresher.refresh();
    }
}
