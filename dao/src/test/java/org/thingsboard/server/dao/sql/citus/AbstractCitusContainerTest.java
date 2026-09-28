// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;
import org.thingsboard.server.dao.service.CitusTestSupport;

import java.util.List;

/**
 * Base class for Citus integration tests.
 * <p>
 * Spins up a real Citus cluster consisting of a coordinator and a single worker, both running the
 * official {@code citusdata/citus} image and sharing a Testcontainers {@link Network}. The worker is
 * registered against the coordinator via {@code citus_add_node} so that {@code create_distributed_table}
 * produces real shards.
 * <p>
 * Implementation note: a single-node setup (coordinator self-registered as its own worker via
 * {@code citus_add_node('localhost', 5432)}) was attempted first, but Citus 12.x treats the coordinator
 * host/port as already registered and returns the coordinator node (groupid = 0) instead of adding a
 * worker, so {@code create_distributed_table} fails with "replication_factor (1) exceeds number of
 * worker nodes (0)". The two-container layout below is therefore used. Subclasses interact with the
 * coordinator through {@link #jdbcTemplate}.
 * <p>
 * Visibility note: {@link #CITUS_IMAGE}, {@link #PG_PORT}, {@link #NETWORK} and {@link #newJdbcTemplate}
 * are {@code protected} so a subclass can additively stand up an extra worker on the same {@link Network}
 * (e.g. for rebalance testing). The shared harness itself still starts exactly one worker.
 */
@Slf4j
public abstract class AbstractCitusContainerTest {

    /** Re-exported from {@link CitusClusterSupport} so subclasses can additively stand up an extra worker. */
    protected static final DockerImageName CITUS_IMAGE = CitusClusterSupport.CITUS_IMAGE;

    protected static final int PG_PORT = CitusClusterSupport.PG_PORT;

    /** The shared single worker's {@code pg_dist_node} nodename, re-exported so subclasses need not re-declare the literal. */
    protected static final String WORKER_ALIAS = CitusClusterSupport.WORKER_ALIAS;

    protected static final Network NETWORK = Network.newNetwork();

    protected static final JdbcDatabaseContainer<?> WORKER =
            CitusClusterSupport.newCitusContainer(NETWORK, CitusClusterSupport.WORKER_ALIAS);

    protected static final JdbcDatabaseContainer<?> CITUS =
            CitusClusterSupport.newCitusContainer(NETWORK, CitusClusterSupport.COORDINATOR_ALIAS);

    protected static JdbcTemplate jdbcTemplate;

    @BeforeAll
    static void startContainer() {
        // The two nodes are independent until registerCluster() runs, so boot them in parallel — every forked test
        // JVM pays this bring-up, and sequential starts double it. deepStart is a no-op for already-running
        // containers, subsuming the previous isRunning() guards.
        Startables.deepStart(List.of(WORKER, CITUS)).join();

        // These are the FIRST connections after start(): the Citus JDBC endpoint occasionally is not yet accepting
        // connections at that instant, so both templates are built on readiness-probed datasources (the same
        // guard CitusTestCluster applies to its init-phase connections).
        jdbcTemplate = new JdbcTemplate(CitusClusterSupport.newReadyDataSource(CITUS));
        JdbcTemplate workerJdbcTemplate = new JdbcTemplate(CitusClusterSupport.newReadyDataSource(WORKER));

        CitusClusterSupport.registerCluster(jdbcTemplate, workerJdbcTemplate);
    }

    /**
     * Builds an enabled {@link CitusSettings} with the given shard count. The settings are {@code @Value}-injected
     * in production, so tests construct them through the reflective factory in {@link CitusTestSupport} (the single
     * home of the field-name literals).
     */
    protected static CitusSettings newCitusSettings(int shardCount) {
        return CitusTestSupport.citusSettings(true, shardCount);
    }

    /**
     * The {@code pg_dist_partition.partmethod} of {@code table} on the shared coordinator: {@code "h"} for a
     * hash-distributed table, {@code "n"} for a reference table. Throws when the table is not Citus-managed.
     */
    protected static String partMethod(String table) {
        return jdbcTemplate.queryForObject(
                "select partmethod from pg_dist_partition where logicalrelid = ?::regclass", String.class, table);
    }

    /** The Citus co-location group id of {@code table}; two tables are co-located iff their colocation ids match. */
    protected static Integer colocationId(String table) {
        return jdbcTemplate.queryForObject(
                "select colocationid from pg_dist_partition where logicalrelid = ?::regclass", Integer.class, table);
    }

    /**
     * Builds a {@link JdbcTemplate} backed by a {@link SingleConnectionDataSource} (constructed with
     * {@code suppressClose = true}) so that one physical connection is reused across all operations.
     * <p>
     * This is required because Citus session GUCs (e.g. {@code SET citus.shard_count}) must persist across
     * JdbcTemplate calls — a connection-per-operation datasource (such as {@code DriverManagerDataSource})
     * would open a fresh connection for each statement and silently discard the GUC, so a subsequent
     * {@code create_distributed_table} would fall back to the default shard count.
     * <p>
     * Autocommit is left at the driver default (not disabled) and operations are not wrapped in an explicit
     * transaction, so a failed statement in one test method does not poison subsequent statements with an
     * aborted transaction.
     */
    protected static JdbcTemplate newJdbcTemplate(JdbcDatabaseContainer<?> container) {
        SingleConnectionDataSource ds = CitusClusterSupport.newSingleConnectionDataSource(container);
        return new JdbcTemplate(ds);
    }

    /**
     * Builds an additional Citus worker container on the shared {@link #NETWORK} under {@code alias}. The caller
     * owns the returned container's lifecycle (typically a {@code static final} field registered via
     * {@link #registerWorker} and torn down via {@link #deregisterAndStopWorker}). Only the container is created
     * here; it is not started and not registered against the coordinator.
     */
    protected static JdbcDatabaseContainer<?> newWorker(String alias) {
        return CitusClusterSupport.newCitusContainer(NETWORK, alias);
    }

    /**
     * Starts {@code worker} (if not already running), installs the Citus extension on it, and registers it against
     * the shared coordinator under {@code alias} so {@code create_distributed_table} / a rebalance can place shards
     * on it. Idempotent: registration is skipped when {@code pg_dist_node} already knows {@code alias}, so calling
     * this from a re-run or a second context is a no-op.
     */
    protected static void registerWorker(JdbcDatabaseContainer<?> worker, String alias) {
        if (!worker.isRunning()) {
            worker.start();
        }
        // First connection to the freshly started worker: readiness-probed for the same post-start() race as above.
        new JdbcTemplate(CitusClusterSupport.newReadyDataSource(worker)).execute("CREATE EXTENSION IF NOT EXISTS citus");
        Integer registered = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_dist_node WHERE nodename = ?", Integer.class, alias);
        if (registered == null || registered == 0) {
            jdbcTemplate.execute(String.format("SELECT citus_add_node('%s', %d)", alias, PG_PORT));
        }
    }

    /** The Citus {@code groupid} advertised for {@code alias} in {@code pg_dist_node}, or {@code null} if it is not registered. */
    protected static Integer workerGroupId(String alias) {
        List<Integer> groups = jdbcTemplate.queryForList(
                "SELECT groupid FROM pg_dist_node WHERE nodename = ?", Integer.class, alias);
        return groups.isEmpty() ? null : groups.get(0);
    }

    /**
     * Deregisters {@code worker} from the shared coordinator and stops its container. The coordinator and primary
     * worker are static containers reused by every Citus test class in the JVM, so an additional worker MUST be
     * removed from {@code pg_dist_node} before its container dies — otherwise sibling classes that run afterwards
     * fail trying to reach the dead host.
     * <p>
     * {@code citus_remove_node} refuses to drop a node that still owns shard placements. The caller's own tables are
     * dropped first ({@code tablesToDrop}), then — as hardening against a sibling class having left a distributed or
     * reference table whose placements sit on this worker (e.g. a stale {@code ts_kv_latest} a rebalance moved
     * here) — EVERY table still placed on this worker's group is dropped. A registered-but-dead node poisons every
     * later Citus test in the JVM, so {@code citus_remove_node} is required to succeed: if it still fails this
     * method fails loudly (the exception propagates) rather than warning and continuing. The container is stopped in
     * a {@code finally} regardless.
     */
    protected static void deregisterAndStopWorker(JdbcDatabaseContainer<?> worker, String alias, String... tablesToDrop) {
        try {
            for (String table : tablesToDrop) {
                try {
                    jdbcTemplate.execute("DROP TABLE IF EXISTS " + table + " CASCADE");
                } catch (Exception e) {
                    log.warn("Failed to drop {} while deregistering Citus worker {}", table, alias, e);
                }
            }
            Integer groupId = workerGroupId(alias);
            if (groupId != null) {
                List<String> leftovers = jdbcTemplate.query(
                        "SELECT DISTINCT s.logicalrelid::regclass::text AS relname FROM pg_dist_placement p " +
                                "JOIN pg_dist_shard s ON s.shardid = p.shardid WHERE p.groupid = ?",
                        (rs, rowNum) -> rs.getString("relname"), groupId);
                for (String table : leftovers) {
                    jdbcTemplate.execute("DROP TABLE IF EXISTS " + table + " CASCADE");
                }
                // Only registered nodes need removing; a worker that never completed registration (null groupId) has
                // nothing in pg_dist_node, and calling citus_remove_node on it would raise a misleading secondary
                // error masking the original failure. Such a worker is just stopped in the finally below.
                jdbcTemplate.execute(String.format("SELECT citus_remove_node('%s', %d)", alias, PG_PORT));
            }
        } finally {
            if (worker.isRunning()) {
                worker.stop();
            }
        }
    }
}
