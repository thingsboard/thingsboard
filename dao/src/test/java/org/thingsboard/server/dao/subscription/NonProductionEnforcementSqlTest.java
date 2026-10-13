// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.subscription;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The accrual and the enforcement joined up over the real {@code tb_cluster} counter: one ticks it, the
 * other reads it back and decides. Both sides are covered separately with the other side stubbed out -
 * {@link NonProductionUptimeTest} and {@link NonProductionEntitlementEnforcerTest} - so nothing else would
 * notice the two drifting onto different columns or different units.
 */
@DaoSqlTest
public class NonProductionEnforcementSqlTest extends AbstractServiceTest {

    private static final long LAST_INTERVAL_MS = TimeUnit.MINUTES.toMillis(10);

    @Autowired
    private TbClusterStore tbClusterStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final SubscriptionService subscriptionService = mock(SubscriptionService.class);
    private final AtomicInteger shutdowns = new AtomicInteger();

    private NonProductionEntitlementEnforcer enforcer;

    @Before
    public void setUp() {
        // Built here rather than in a field initializer, which would run before the store is injected.
        enforcer = new NonProductionEntitlementEnforcer(
                subscriptionService, tbClusterStore, mock(ApplicationContext.class)) {
            @Override
            void shutdown() {
                // The real shutdown calls System.exit and would take the test JVM with it.
                shutdowns.incrementAndGet();
            }
        };
    }

    @After
    public void tearDown() {
        // tb_cluster is the single shared row: put the ticker columns back to the installer's baseline, or
        // the next class to run starts on an exhausted allowance.
        jdbcTemplate.update("UPDATE tb_cluster SET non_production_uptime_ms = 0, non_production_last_tick = NULL");
    }

    @Test
    public void anAllowanceTickedUpToTheLimitShutsTheInstanceDown() {
        when(subscriptionService.isNonProductionMode()).thenReturn(true);
        when(subscriptionService.revokeNonProductionEntitlement()).thenReturn(true);
        givenAccruedUptime(TbClusterStore.NON_PRODUCTION_UPTIME_LIMIT_MS - LAST_INTERVAL_MS);

        long now = 1_000_000L;
        tbClusterStore.tickNonProductionUptime(now, TbClusterStore.MAX_DELTA_MS);

        // The baseline tick accrues nothing, so the allowance is still short of the limit here. Asserted
        // rather than assumed: if it were already over, the tick below would prove nothing.
        enforcer.enforce();
        assertThat(shutdowns).hasValue(0);

        tbClusterStore.tickNonProductionUptime(now + LAST_INTERVAL_MS, TbClusterStore.MAX_DELTA_MS);
        enforcer.enforce();

        assertThat(tbClusterStore.getNonProductionUptimeMs())
                .isEqualTo(TbClusterStore.NON_PRODUCTION_UPTIME_LIMIT_MS);
        verify(subscriptionService).revokeNonProductionEntitlement();
        assertThat(shutdowns).hasValue(1);
    }

    @Test
    public void downtimeAcrossTheLimitDoesNotExhaustTheAllowance() {
        // The counter is cumulative uptime, not wall-clock age: an instance switched off for six months and
        // started again is still owed the rest of its allowance.
        when(subscriptionService.isNonProductionMode()).thenReturn(true);
        givenAccruedUptime(TbClusterStore.NON_PRODUCTION_UPTIME_LIMIT_MS - LAST_INTERVAL_MS);

        long now = 1_000_000L;
        tbClusterStore.tickNonProductionUptime(now, TbClusterStore.MAX_DELTA_MS);
        tbClusterStore.tickNonProductionUptime(now + TimeUnit.DAYS.toMillis(180), TbClusterStore.MAX_DELTA_MS);
        enforcer.enforce();

        verify(subscriptionService, never()).revokeNonProductionEntitlement();
        assertThat(shutdowns).hasValue(0);
    }

    private void givenAccruedUptime(long uptimeMs) {
        jdbcTemplate.update("UPDATE tb_cluster SET non_production_uptime_ms = ?, non_production_last_tick = NULL",
                uptimeMs);
    }

}
