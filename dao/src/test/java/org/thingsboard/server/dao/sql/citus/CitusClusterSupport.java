// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared building blocks for standing up a real Citus cluster (coordinator + worker) in tests.
 * <p>
 * Two harnesses bootstrap a Citus cluster with the same image, aliases and registration sequence but with
 * different lifecycles: {@link AbstractCitusContainerTest} (a JUnit5 base class whose containers are managed
 * by {@code @BeforeAll}) and {@link CitusTestCluster} (a process-wide JUnit4-context singleton). The bits that
 * are genuinely identical between them — the {@link #CITUS_IMAGE} pin, the coordinator/worker aliases and
 * port, the container factory, the {@link #registerCluster} sequence and the connect-readiness probe — live
 * here so they are defined exactly once. Residual per-harness wiring is kept in each harness because their
 * container/datasource lifecycles differ and are intentionally not unified.
 * <p>
 * The {@link #CITUS_IMAGE} pin is mirrored (it cannot be imported) by the black-box compose file
 * {@code msa/black-box-tests/src/test/resources/advanced/docker-compose.citus.yml}; keep them in sync.
 */
@Slf4j
final class CitusClusterSupport {

    /**
     * The single source of truth for the Citus image used by every Java test harness. The black-box
     * {@code docker-compose.citus.yml} pins the same tag by hand (YAML cannot reference this constant).
     */
    static final DockerImageName CITUS_IMAGE =
            DockerImageName.parse("citusdata/citus:12.1").asCompatibleSubstituteFor("postgres");

    static final String COORDINATOR_ALIAS = "coordinator";
    static final String WORKER_ALIAS = "worker";
    static final int PG_PORT = 5432;

    /** Total time to keep retrying the first JDBC connection of an init phase before giving up. */
    static final long CONNECT_RETRY_DEADLINE_MS = 60_000L;
    /** Backoff between connection-readiness probe attempts. */
    static final long CONNECT_RETRY_BACKOFF_MS = 1_500L;

    private CitusClusterSupport() {
    }

    /**
     * Builds a Citus container ({@link #CITUS_IMAGE}) for the {@code thingsboard} database on the given
     * {@link Network} under the given alias. The caller is responsible for starting and stopping it.
     */
    @SuppressWarnings("resource")
    static JdbcDatabaseContainer<?> newCitusContainer(Network network, String alias) {
        return new PostgreSQLContainer<>(CITUS_IMAGE)
                .withDatabaseName("thingsboard")
                .withUsername("postgres")
                .withPassword("postgres")
                .withNetwork(network)
                .withNetworkAliases(alias);
    }

    /**
     * Installs the Citus extension on both nodes and registers the worker against the coordinator using the
     * in-network aliases/ports, so the coordinator can reach the worker for shard placement. Idempotent: the
     * worker is only added when {@code pg_dist_node} has no primary worker yet, so re-running against an
     * already-registered cluster is a no-op.
     */
    static void registerCluster(JdbcTemplate coordinator, JdbcTemplate worker) {
        coordinator.execute("CREATE EXTENSION IF NOT EXISTS citus");
        worker.execute("CREATE EXTENSION IF NOT EXISTS citus");

        coordinator.execute(String.format("SELECT citus_set_coordinator_host('%s', %d)", COORDINATOR_ALIAS, PG_PORT));
        Integer workers = coordinator.queryForObject(
                "select count(*) from pg_dist_node where noderole = 'primary' and groupid <> 0", Integer.class);
        if (workers == null || workers == 0) {
            coordinator.execute(String.format("SELECT citus_add_node('%s', %d)", WORKER_ALIAS, PG_PORT));
        }
    }

    /**
     * Builds a {@link SingleConnectionDataSource} (constructed with {@code suppressClose = true}) so one
     * physical connection is reused across all operations. This is required because Citus session GUCs (e.g.
     * {@code SET citus.shard_count}) must persist across JdbcTemplate calls — a connection-per-operation
     * datasource would open a fresh connection per statement and silently discard the GUC.
     */
    static SingleConnectionDataSource newSingleConnectionDataSource(JdbcDatabaseContainer<?> container) {
        SingleConnectionDataSource ds = new SingleConnectionDataSource(
                container.getJdbcUrl(), container.getUsername(), container.getPassword(), true);
        ds.setDriverClassName("org.postgresql.Driver");
        return ds;
    }

    /**
     * Builds a {@link SingleConnectionDataSource} and verifies it can actually serve a connection before
     * returning it. Despite Testcontainers {@code start()} having returned, the Citus coordinator/worker JDBC
     * endpoint occasionally is not yet accepting connections at the instant the first init phase connects,
     * which surfaces as a {@code CannotGetJdbcConnectionException}. This probes {@code SELECT 1} in a retry
     * loop with a short backoff until {@link #CONNECT_RETRY_DEADLINE_MS}, rebuilding the datasource on each
     * attempt so a cached-but-broken single connection cannot poison later retries. The returned datasource
     * holds a live connection (the probe leaves it open) ready for the real work.
     */
    static SingleConnectionDataSource newReadyDataSource(JdbcDatabaseContainer<?> container) {
        long deadline = System.currentTimeMillis() + CONNECT_RETRY_DEADLINE_MS;
        int attempt = 0;
        RuntimeException lastFailure = null;
        while (true) {
            attempt++;
            SingleConnectionDataSource ds = newSingleConnectionDataSource(container);
            try {
                new JdbcTemplate(ds).queryForObject("SELECT 1", Integer.class);
                return ds;
            } catch (RuntimeException e) {
                lastFailure = e;
                try {
                    ds.destroy();
                } catch (RuntimeException ignored) {
                    // best-effort cleanup of the failed datasource before the next attempt
                }
                if (System.currentTimeMillis() >= deadline) {
                    throw new IllegalStateException(
                            "Citus container at " + container.getJdbcUrl()
                                    + " did not accept JDBC connections within " + CONNECT_RETRY_DEADLINE_MS
                                    + "ms (" + attempt + " attempts)", lastFailure);
                }
                log.warn("Citus container at {} not accepting JDBC connections yet (attempt {}); retrying in {}ms",
                        container.getJdbcUrl(), attempt, CONNECT_RETRY_BACKOFF_MS);
                try {
                    Thread.sleep(CONNECT_RETRY_BACKOFF_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for Citus container readiness", ie);
                }
            }
        }
    }
}
