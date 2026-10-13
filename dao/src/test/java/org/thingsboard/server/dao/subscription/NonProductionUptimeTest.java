// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.subscription;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Round-trips {@link TbClusterStore#tickNonProductionUptime} and {@link TbClusterStore#getNonProductionUptimeMs}
 * against the real {@code tb_cluster} columns created by {@code dao/src/main/resources/sql/schema-entities.sql} -
 * the only schema source {@code PostgreSqlInitializer} builds the test database from. It does not exercise
 * either upgrade script ({@code application/src/main/data/upgrade/lts/4.4.0.0/schema_update.sql} or
 * {@code application/src/main/data/upgrade/pe/schema_update.sql}): a column present here but missing, or
 * differently defined, in one of those would not be caught by this test.
 */
@DaoSqlTest
public class NonProductionUptimeTest extends AbstractServiceTest {

    private static final long MAX_DELTA_MS = TbClusterStore.MAX_DELTA_MS;
    /** A normally spaced tick, which must be credited in full rather than clamped. */
    private static final long TICK_MS = TimeUnit.MINUTES.toMillis(TbClusterStore.TICK_INTERVAL_MINUTES);

    @Autowired
    private TbClusterStore tbClusterStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @After
    public void tearDown() {
        // tb_cluster is the single shared row: reset the ticker columns so the next test starts from the
        // same null baseline the installer leaves behind.
        jdbcTemplate.update("UPDATE tb_cluster SET non_production_uptime_ms = 0, non_production_last_tick = NULL");
    }

    @Test
    public void aTickAccumulatesTheElapsedInterval() {
        long start = 1_000_000L;
        tbClusterStore.tickNonProductionUptime(start, MAX_DELTA_MS);
        tbClusterStore.tickNonProductionUptime(start + TICK_MS, MAX_DELTA_MS);

        assertThat(tbClusterStore.getNonProductionUptimeMs())
                .isEqualTo(TICK_MS);
    }

    @Test
    public void downtimeIsNotCounted() {
        // The whole point of cumulative uptime: a server switched off for six months accrues nothing at all,
        // not six months and not one capped interval either. A wall-clock timer could not tell these apart.
        // Crediting the cap would hand a deployment MAX_DELTA_MS of licensed uptime it never spent running,
        // once per restart, which is exactly what this counter exists to refuse.
        long start = 1_000_000L;
        long restart = start + TimeUnit.DAYS.toMillis(180);
        tbClusterStore.tickNonProductionUptime(start, MAX_DELTA_MS);
        tbClusterStore.tickNonProductionUptime(restart, MAX_DELTA_MS);

        assertThat(tbClusterStore.getNonProductionUptimeMs()).isZero();

        // The baseline still moved to the restart, so accrual resumes from there rather than stalling: a
        // last_tick left behind at the pre-downtime value would keep every following tick over the cap and
        // accruing nothing, freezing the counter permanently.
        tbClusterStore.tickNonProductionUptime(restart + TICK_MS, MAX_DELTA_MS);

        assertThat(tbClusterStore.getNonProductionUptimeMs()).isEqualTo(TICK_MS);
    }

    @Test
    public void theFirstTickAccruesNothing() {
        tbClusterStore.tickNonProductionUptime(1_000_000L, MAX_DELTA_MS);

        assertThat(tbClusterStore.getNonProductionUptimeMs()).isZero();
    }

    @Test
    public void aBackwardClockDoesNotReduceTheAccruedUptime() {
        // A counter named cumulative uptime must never move backwards. Inter-node clock skew makes this
        // reachable in practice: a failover away from a node whose clock runs fast hands ticking duty to a
        // node that observes an earlier "now", and every such failover would otherwise subtract.
        long start = 1_000_000L;
        tbClusterStore.tickNonProductionUptime(start, MAX_DELTA_MS);
        tbClusterStore.tickNonProductionUptime(start + TICK_MS, MAX_DELTA_MS);
        tbClusterStore.tickNonProductionUptime(start, MAX_DELTA_MS);

        assertThat(tbClusterStore.getNonProductionUptimeMs()).isEqualTo(TICK_MS);
    }

}
