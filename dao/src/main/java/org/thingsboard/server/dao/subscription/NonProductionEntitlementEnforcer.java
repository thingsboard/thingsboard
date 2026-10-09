// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.subscription;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Revokes a keyless deployment's non-production entitlement once the cumulative uptime reaches
 * {@link TbClusterStore#NON_PRODUCTION_UPTIME_LIMIT_MS}, then shuts the node down, since locking alone would
 * leave the data plane serving. Runs on every node that can grant the entitlement, with no node-type
 * restriction: unlike the accrual, this check never advances the shared {@code tb_cluster} row, so every node
 * reaches the same verdict independently. Not a pure read, though: a node that has reached the limit first
 * attempts an activation from whatever secret the cluster now holds, so a node that missed the licence
 * broadcast is not shut down over an allowance that no longer governs it.
 */
@Service
@Profile("!install & !test")
@RequiredArgsConstructor
@Slf4j
public class NonProductionEntitlementEnforcer {

    /**
     * How often the cumulative uptime is re-checked, and therefore how late the lock can land. Declared apart
     * from {@link TbClusterStore#TICK_INTERVAL_MINUTES} so retuning one does not retune the other: the accrual
     * interval is short to bound what a restart discards, while this one only decides how late the lock lands.
     */
    private static final long ENFORCEMENT_INTERVAL_MINUTES = 15;

    /** Non-zero so a supervisor can tell an exhausted allowance from a clean stop. */
    private static final int ALLOWANCE_EXHAUSTED_EXIT_CODE = 1;

    private final SubscriptionService subscriptionService;
    private final TbClusterStore tbClusterStore;
    private final ApplicationContext applicationContext;

    @Scheduled(fixedRate = ENFORCEMENT_INTERVAL_MINUTES, timeUnit = TimeUnit.MINUTES)
    public void enforce() {
        // No try/catch: Spring's scheduler already logs and suppresses task failures, and the schedule survives.
        if (!subscriptionService.isNonProductionMode()) {
            return;
        }
        if (tbClusterStore.getNonProductionUptimeMs() < TbClusterStore.NON_PRODUCTION_UPTIME_LIMIT_MS) {
            return;
        }
        // isNonProductionMode() above converges nothing, so a node that missed the license broadcast would
        // shut itself down minutes after an operator licensed the cluster on a peer. Unthrottled, and
        // needsStoredSecret() is true for a keyless node, so this actually reads the row. On a normal keyless
        // deployment nothing is stored and it costs one SELECT.
        subscriptionService.pickUpStoredLicenseSecret();
        if (!subscriptionService.isNonProductionMode()) {
            return;
        }
        // The revocation declines when the node acquired a real licence in the meantime; shutting down on a
        // declined revocation would kill a licensed node.
        if (!subscriptionService.revokeNonProductionEntitlement()) {
            return;
        }
        // Locking alone would leave the data plane serving, hence the shutdown.
        log.error("Shutting down: the non-production allowance is exhausted.");
        shutdown();
    }

    /** Package-visible and overridable so the test can assert the shutdown without killing the JVM. */
    void shutdown() {
        int exitCode = SpringApplication.exit(applicationContext, () -> ALLOWANCE_EXHAUSTED_EXIT_CODE);
        System.exit(exitCode);
    }

}
