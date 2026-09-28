// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus.routing;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.dao.sql.citus.AbstractCitusContainerTest;
import org.thingsboard.server.dao.sql.citus.CitusShardLocator;

import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchRuntimeException;

/**
 * Failover self-heal integration test for the smart-routing hardening (design
 * {@code 2026-07-09-citus-failover-hardening}): simulates a worker demotion and verifies the full
 * detect - refuse - heal cycle against a real Citus cluster.
 *
 * <p><b>Demotion simulation.</b> {@code ALTER SYSTEM SET default_transaction_read_only = on} +
 * {@code pg_reload_conf()} on the (single, shared) worker. The JDBC driver's
 * {@code targetServerType=primary} gate, Hikari's borrow-time validation and the reconcile probe all
 * key off {@code transaction_read_only} — the very signal a real standby reports — so this exercises
 * every hardening layer; a true {@code pg_is_in_recovery()} standby is not reproducible in the
 * single-container-per-worker harness. The setting is force-reverted in {@code @AfterEach}: the worker
 * container is shared by every Citus test class in the JVM, and leaving it read-only would poison all
 * of them.
 *
 * <p>The failure signature the routed write surfaces depends on borrow timing (both qualify and both
 * are asserted through the shared classifier): a connection reused within Hikari's 500ms
 * alive-bypass window skips validation and fails in-query with SQLSTATE {@code 25006}, while a
 * validated borrow evicts the demoted connection, the replacement connect is refused by the driver,
 * and the borrow times out as a connection-acquisition failure.
 */
@Slf4j
class CitusFailoverSelfHealIntegrationTest extends AbstractCitusContainerTest {

    private static final int SHARD_COUNT = 4;
    private static final long DEBOUNCE_MS = 60_000L; // one triggered refresh per test; asserted exactly

    private static final String UPSERT_SQL =
            "INSERT INTO ts_kv_latest (entity_id, key, ts, long_v, version) VALUES (?::uuid, ?, ?, ?, 1) " +
                    "ON CONFLICT (entity_id, key) DO UPDATE SET ts = EXCLUDED.ts, long_v = EXCLUDED.long_v, " +
                    "version = ts_kv_latest.version + 1";

    /** Counts refresher runs so "the trigger fired" is asserted on registry-healing behavior, not logs. */
    private static class CountingRefresher extends CitusRoutingRefresher {
        final AtomicInteger refreshes = new AtomicInteger();

        CountingRefresher(CitusShardPlacement placement, CitusWorkerRegistry workerRegistry) {
            super(placement, workerRegistry);
        }

        @Override
        public void refresh() {
            refreshes.incrementAndGet();
            super.refresh();
        }
    }

    private JdbcTemplate workerTemplate;
    private CitusShardLocator locator;
    private CitusShardPlacement placement;
    private CitusWorkerRegistry registry;
    private CountingRefresher refresher;
    private CitusShardRouter router;

    @BeforeEach
    void setUp() {
        workerTemplate = newJdbcTemplate(WORKER);

        jdbcTemplate.execute("DROP TABLE IF EXISTS ts_kv_latest CASCADE");
        jdbcTemplate.execute("CREATE TABLE ts_kv_latest (entity_id uuid, key int, ts bigint, long_v bigint, " +
                "version bigint, CONSTRAINT ts_kv_latest_pkey PRIMARY KEY (entity_id, key))");
        jdbcTemplate.execute("SELECT create_distributed_table('ts_kv_latest','entity_id', shard_count => " + SHARD_COUNT + ")");

        locator = new CitusShardLocator(jdbcTemplate, newCitusSettings(SHARD_COUNT), "ts_kv_latest");
        locator.refresh();

        placement = new CitusShardPlacement(jdbcTemplate, "ts_kv_latest");
        placement.refresh();

        CitusSmartRoutingSettings routingSettings = new CitusSmartRoutingSettings();
        ReflectionTestUtils.setField(routingSettings, "enabled", true);
        ReflectionTestUtils.setField(routingSettings, "workerPoolSize", 4);
        // Short connect budget so a refused rebuild / borrow-timeout failure surfaces in ~2s (fail fast, no hangs).
        ReflectionTestUtils.setField(routingSettings, "workerConnectionTimeoutMs", 2000L);
        ReflectionTestUtils.setField(routingSettings, "workerHostOverridesRaw",
                WORKER_ALIAS + "=" + WORKER.getHost() + ":" + WORKER.getMappedPort(PG_PORT));

        registry = new CitusWorkerRegistry(jdbcTemplate, routingSettings, CITUS.getJdbcUrl(), "postgres", "postgres");
        registry.start();

        refresher = new CountingRefresher(placement, registry);
        // Synchronous executor: the triggered refresh completes inside the failing routed call, so each
        // step's effect on the registry is deterministic and immediately assertable.
        CitusFailoverRefreshTrigger trigger =
                new CitusFailoverRefreshTrigger(refresher, DEBOUNCE_MS, Clock.systemUTC(), Runnable::run);
        router = new CitusShardRouter(locator, placement, registry, routingSettings, trigger);
    }

    @AfterEach
    void tearDown() {
        // Force-revert the demotion FIRST: the worker container is shared by every Citus test class in
        // the JVM, so this must run even when the test failed mid-read-only.
        try {
            revertDemotion();
        } finally {
            if (registry != null) {
                registry.close();
            }
            if (workerTemplate != null && workerTemplate.getDataSource() instanceof SingleConnectionDataSource singleConnectionDataSource) {
                singleConnectionDataSource.destroy();
            }
        }
    }

    @Test
    void demotedWorkerIsRefusedAndHealsAfterRepromotion() {
        UUID entityId = UUID.randomUUID();
        int bucket = locator.bucket(entityId);
        JdbcTemplate poolTemplateBefore = router.forBucket(bucket);

        // 1. Baseline: the routed write path works.
        assertThat(upsert(bucket, entityId, 100L)).as("baseline routed write inserts version 1").isEqualTo(1);
        assertThat(refresher.refreshes).hasValue(0);

        // 2. Demote the worker; the first failed routed write carries a failover signature and fires the trigger.
        demoteWorker();
        RuntimeException failure = catchRuntimeException(() -> upsert(bucket, entityId, 200L));
        assertThat(failure).as("a routed write against a demoted worker must fail").isNotNull();
        assertThat(CitusShardRouter.isFailoverFailure(failure))
                .as("the failure must carry a failover signature (25006 or connection acquisition), got: %s", failure)
                .isTrue();
        assertThat(refresher.refreshes).as("the failover trigger must have run one refresh").hasValue(1);

        // 3. That refresh attempted a rebuild but the endpoint is still read-only: the driver refuses the
        // replacement pool (targetServerType=primary), the old pool is kept, and writes keep failing fast.
        assertThat(registry.workerGroupIds()).as("the worker group must not be dropped while demoted").isNotEmpty();
        assertThat(router.forBucket(bucket))
                .as("a refused rebuild must keep the OLD pool (keep-a-running-app)")
                .isSameAs(poolTemplateBefore);
        long failingWriteStart = System.currentTimeMillis();
        RuntimeException stillFailing = catchRuntimeException(() -> upsert(bucket, entityId, 300L));
        long failingWriteMs = System.currentTimeMillis() - failingWriteStart;
        assertThat(stillFailing).as("writes must keep failing while the worker is read-only").isNotNull();
        assertThat(failingWriteMs)
                .as("a failing routed write must fail fast (borrow/connect budget is 2s), not hang")
                .isLessThan(10_000L);
        assertThat(refresher.refreshes).as("repeat failures inside the debounce window must not re-trigger").hasValue(1);

        // 4. Re-promote and invoke the refresh (in production: the next trigger window or the scheduled
        // tick); the writability probe passes again and routed writes succeed — full self-heal.
        revertDemotion();
        refresher.refresh();
        assertThat(upsert(bucket, entityId, 400L)).as("post-heal routed write must bump the version").isEqualTo(2);
        Long healedVersion = jdbcTemplate.queryForObject(
                "SELECT version FROM ts_kv_latest WHERE entity_id = ?::uuid AND key = 7", Long.class, entityId.toString());
        assertThat(healedVersion).as("the healed write must be visible cluster-wide").isEqualTo(2L);
    }

    private long upsert(int bucket, UUID entityId, long value) {
        return router.routedWrite(bucket, template -> template.queryForObject(
                UPSERT_SQL + " RETURNING version", Long.class, entityId.toString(), 7, value, value));
    }

    /**
     * Simulates a demotion: every NEW transaction on the worker becomes read-only, which is the signal
     * {@code targetServerType=primary}, the borrow validation and the reconcile probe all key off.
     */
    private void demoteWorker() {
        workerTemplate.execute("ALTER SYSTEM SET default_transaction_read_only = on");
        workerTemplate.execute("SELECT pg_reload_conf()");
        awaitWorkerDefaultReadOnly("on");
    }

    /**
     * Reverts the demotion. The maintenance session itself defaults to read-only after the flip and
     * {@code ALTER SYSTEM} is forbidden in a read-only transaction, so the session first SETs itself
     * writable (a session-level SET overrides the reloaded file value), then clears the file setting.
     * Idempotent — also runs as the {@code @AfterEach} safety net.
     */
    private void revertDemotion() {
        workerTemplate.execute("SET default_transaction_read_only = off");
        workerTemplate.execute("ALTER SYSTEM RESET default_transaction_read_only");
        workerTemplate.execute("SELECT pg_reload_conf()");
        // Drop the session override so the await below reads the reloaded file value, not the SET.
        workerTemplate.execute("RESET default_transaction_read_only");
        awaitWorkerDefaultReadOnly("off");
    }

    /** {@code pg_reload_conf()} is asynchronous; existing sessions apply the new default at next transaction start. */
    private void awaitWorkerDefaultReadOnly(String expected) {
        long deadline = System.currentTimeMillis() + 10_000L;
        while (true) {
            String current = workerTemplate.queryForObject("SHOW default_transaction_read_only", String.class);
            if (expected.equals(current)) {
                return;
            }
            if (System.currentTimeMillis() > deadline) {
                throw new IllegalStateException("Worker did not report default_transaction_read_only = " + expected +
                        " within 10s (still " + current + ")");
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for the worker config reload", e);
            }
        }
    }
}
