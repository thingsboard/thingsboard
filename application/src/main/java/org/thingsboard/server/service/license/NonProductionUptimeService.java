// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.license;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.subscription.NonProductionEntitlementEnforcer;
import org.thingsboard.server.dao.subscription.SubscriptionService;
import org.thingsboard.server.dao.subscription.TbClusterStore;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.concurrent.TimeUnit;

/**
 * Tracks how long a keyless, non-production deployment has cumulatively been running. A tick accrues only the
 * interval elapsed since the previous tick, and nothing at all when that interval exceeds
 * {@link TbClusterStore#MAX_DELTA_MS}, so downtime is not counted as uptime.
 * <p>
 * There is deliberately no disable switch: the counter accrued here withdraws non-production entitlements once
 * {@link TbClusterStore#NON_PRODUCTION_UPTIME_LIMIT_MS} is reached. The tick interval and the derived gap limit
 * live on {@link TbClusterStore} next to the counter they describe, so the two cannot drift apart. Accrual only:
 * enforcement is a separate {@link NonProductionEntitlementEnforcer}, which is deliberately not a
 * {@link TbCoreComponent} the way this class is - that annotation is here because accrual is a write serialized
 * to one node via {@link PartitionService}, and restricting the enforcer too would leave tb-rule-engine
 * ungoverned.
 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class NonProductionUptimeService {

    private final SubscriptionService subscriptionService;
    private final PartitionService partitionService;
    private final TbClusterStore tbClusterStore;

    /**
     * The initial delay matters as much as the rate: without it the first tick after a restart would measure its
     * delta against the {@code non_production_last_tick} left by the previous run, counting the downtime.
     */
    @Scheduled(fixedRate = TbClusterStore.TICK_INTERVAL_MINUTES, initialDelay = TbClusterStore.TICK_INTERVAL_MINUTES,
            timeUnit = TimeUnit.MINUTES)
    public void tick() {
        // No try/catch: Spring's default LOG_AND_SUPPRESS_ERROR_HANDLER already logs the stack trace at ERROR
        // and keeps the schedule, so catching here would only downgrade the severity.
        if (!subscriptionService.isNonProductionMode()) {
            return;
        }
        // Reduces write contention to one UPDATE per interval; what keeps an N-node cluster from accruing N
        // times the elapsed time is the delta-based SQL, which a fixed "+ TICK_INTERVAL" increment would lose.
        if (!partitionService.isSystemPartitionMine(ServiceType.TB_CORE)) {
            return;
        }
        tbClusterStore.tickNonProductionUptime(System.currentTimeMillis(), TbClusterStore.MAX_DELTA_MS);
    }

}
