// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.subscription;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins that the enforcement check runs unconditionally - no partition ownership, no node-type restriction -
 * unlike the accrual it reads the counter from, covered separately by
 * {@code org.thingsboard.server.service.license.NonProductionUptimeServiceTest} in the application module.
 */
public class NonProductionEntitlementEnforcerTest {

    private final SubscriptionService subscriptionService = mock(SubscriptionService.class);
    private final TbClusterStore tbClusterStore = mock(TbClusterStore.class);
    private final ApplicationContext applicationContext = mock(ApplicationContext.class);
    private final AtomicInteger shutdowns = new AtomicInteger();
    /** The shutdown is stubbed out: the real one calls System.exit and would take the test JVM with it. */
    private final NonProductionEntitlementEnforcer enforcer =
            new NonProductionEntitlementEnforcer(subscriptionService, tbClusterStore, applicationContext) {
                @Override
                void shutdown() {
                    shutdowns.incrementAndGet();
                }
            };

    @Test
    public void aLicensedInstanceNeverConsultsTheCounter() {
        when(subscriptionService.isNonProductionMode()).thenReturn(false);

        enforcer.enforce();

        verify(tbClusterStore, never()).getNonProductionUptimeMs();
        verify(subscriptionService, never()).revokeNonProductionEntitlement();
        assertThat(shutdowns).hasValue(0);
    }

    @Test
    public void underTheLimitDoesNotRevoke() {
        when(subscriptionService.isNonProductionMode()).thenReturn(true);
        when(tbClusterStore.getNonProductionUptimeMs()).thenReturn(TbClusterStore.NON_PRODUCTION_UPTIME_LIMIT_MS - 1);

        enforcer.enforce();

        verify(subscriptionService, never()).revokeNonProductionEntitlement();
        assertThat(shutdowns).hasValue(0);
    }

    @Test
    public void atTheLimitRevokesAndShutsDown() {
        // Locking alone would leave the data plane serving, so the cap would have almost no effect.
        when(subscriptionService.isNonProductionMode()).thenReturn(true);
        when(tbClusterStore.getNonProductionUptimeMs()).thenReturn(TbClusterStore.NON_PRODUCTION_UPTIME_LIMIT_MS);
        when(subscriptionService.revokeNonProductionEntitlement()).thenReturn(true);

        enforcer.enforce();

        verify(subscriptionService).revokeNonProductionEntitlement();
        assertThat(shutdowns).hasValue(1);
    }

    @Test
    public void aNodeThatAcquiredALicenseInTheWindowIsNotShutDown() {
        // The verdict is decided in three steps, so a key applied in between makes the revocation decline -
        // and shutting down on a declined revocation would kill a node that is now licensed.
        when(subscriptionService.isNonProductionMode()).thenReturn(true);
        when(tbClusterStore.getNonProductionUptimeMs()).thenReturn(TbClusterStore.NON_PRODUCTION_UPTIME_LIMIT_MS);
        when(subscriptionService.revokeNonProductionEntitlement()).thenReturn(false);

        enforcer.enforce();

        verify(subscriptionService).revokeNonProductionEntitlement();
        assertThat(shutdowns).hasValue(0);
    }

    @Test
    public void doesNotShutDownWhenAPeerStoredASecretBeforeThisNodeTicked() {
        // isNonProductionMode() is a plain field read that converges nothing, so without the pick-up this node
        // exits minutes after the cluster was licensed on another one.
        when(subscriptionService.isNonProductionMode()).thenReturn(true, false);
        when(tbClusterStore.getNonProductionUptimeMs()).thenReturn(TbClusterStore.NON_PRODUCTION_UPTIME_LIMIT_MS);

        enforcer.enforce();

        verify(subscriptionService).pickUpStoredLicenseSecret();
        verify(subscriptionService, never()).revokeNonProductionEntitlement();
        assertThat(shutdowns).hasValue(0);
    }

    @Test
    public void pastTheLimitRevokes() {
        // No partition dependency at all - this is the case a rule-engine or transport node hits, since
        // those node types never own the tb-core system partition and would never even be asked to.
        when(subscriptionService.isNonProductionMode()).thenReturn(true);
        when(tbClusterStore.getNonProductionUptimeMs()).thenReturn(TbClusterStore.NON_PRODUCTION_UPTIME_LIMIT_MS + 1);
        when(subscriptionService.revokeNonProductionEntitlement()).thenReturn(true);

        enforcer.enforce();

        verify(subscriptionService).revokeNonProductionEntitlement();
        assertThat(shutdowns).hasValue(1);
    }

}
