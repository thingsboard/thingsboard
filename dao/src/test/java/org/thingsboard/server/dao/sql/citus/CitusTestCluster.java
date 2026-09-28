// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.lifecycle.Startables;
import org.thingsboard.server.dao.PostgreSqlInitializer;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.springframework.test.util.ReflectionTestUtils.setField;

/**
 * Process-wide singleton that stands up a real Citus cluster (coordinator + one worker), installs the
 * real ThingsBoard schema on the coordinator and applies the real production
 * {@link DefaultCitusSchemaService#applyDistribution()} — exactly once per JVM.
 * <p>
 * It mirrors the proven two-container layout of {@code AbstractCitusContainerTest} (which it intentionally
 * does not extend or modify): the {@link CitusClusterSupport#CITUS_IMAGE} for both containers on a shared
 * {@link Network}, {@code CREATE EXTENSION citus} on both, and the worker registered against the
 * coordinator via {@code citus_set_coordinator_host} + {@code citus_add_node}. A single-node Citus setup
 * is not viable (Citus 12 rejects coordinator self-registration), so two containers are required. The shared
 * image, aliases and registration sequence live in {@link CitusClusterSupport}.
 * <p>
 * The one-time schema install and distribution run over a dedicated {@link SingleConnectionDataSource}
 * (constructed with {@code suppressClose = true}) so that Citus session GUCs persist across the JdbcTemplate
 * calls. Normal DAO traffic in the tests goes through the Spring Hikari pool, which is fine for Citus.
 * <p>
 * The containers are {@code static} and never stopped, so they survive Spring context teardown
 * ({@code @DirtiesContext}) and are reused across every Citus test class in the JVM.
 */
@Slf4j
public final class CitusTestCluster {

    private static final int PG_PORT = CitusClusterSupport.PG_PORT;
    private static final int TEST_SHARD_COUNT = 4;

    private static volatile CitusTestCluster instance;

    private final Network network;
    private final JdbcDatabaseContainer<?> coordinator;
    private final JdbcDatabaseContainer<?> worker;

    /**
     * JDBC connection coordinates for the coordinator, exposed so the Spring datasource can connect over the
     * plain {@code org.postgresql.Driver}.
     */
    @Getter
    private final String jdbcUrl;
    @Getter
    private final String username;
    @Getter
    private final String password;

    /**
     * Override entry for {@code database.citus.smart_routing.worker_host_overrides}, mapping the worker's
     * {@code pg_dist_node} nodename ({@link CitusClusterSupport#WORKER_ALIAS}, the alias {@code citus_add_node} registered it under)
     * to the host-mapped {@code host:port} of the worker container. The advertised in-Docker {@code worker:5432}
     * is unreachable from the host JVM, so the smart-routing worker registry must remap it to reach the worker
     * for smart routing.
     */
    public String getWorkerHostOverride() {
        return CitusClusterSupport.WORKER_ALIAS + "=" + worker.getHost() + ":" + worker.getMappedPort(PG_PORT);
    }

    /**
     * Returns the started, schema-installed and distributed cluster, starting it on first call. Subsequent calls
     * (e.g. from a second Spring context) return the same instance without re-starting or re-distributing.
     */
    public static CitusTestCluster getInstance() {
        CitusTestCluster local = instance;
        if (local == null) {
            synchronized (CitusTestCluster.class) {
                local = instance;
                if (local == null) {
                    local = new CitusTestCluster();
                    instance = local;
                }
            }
        }
        return local;
    }

    private CitusTestCluster() {
        network = Network.newNetwork();

        worker = CitusClusterSupport.newCitusContainer(network, CitusClusterSupport.WORKER_ALIAS);
        coordinator = CitusClusterSupport.newCitusContainer(network, CitusClusterSupport.COORDINATOR_ALIAS);

        // The single shared coordinator serves EVERY pooled Spring context in the JVM: each cached test context
        // (@DirtiesContext discards a context but Testcontainers keeps this coordinator alive) holds its own Hikari
        // pool against it, and pools are not released until JVM exit. Several dozen cached contexts x pool size
        // overruns the image default max_connections=100, surfacing as "sorry, too many clients already". Raise the
        // ceiling on the coordinator so cached_contexts x pool_size stays comfortably under it. The image configures
        // Citus via files (shared_preload_libraries), not CLI args, so overriding only the command is safe.
        coordinator.withCommand("postgres", "-c", "max_connections=300");

        // The two nodes are independent until registerCluster() runs, so boot them in parallel — this
        // once-per-JVM bring-up is on the critical path of every Citus test run, and sequential starts
        // would double it.
        Startables.deepStart(List.of(worker, coordinator)).join();

        jdbcUrl = coordinator.getJdbcUrl();
        username = coordinator.getUsername();
        password = coordinator.getPassword();

        // If any of the following init steps throws AFTER the containers have started, instance stays null
        // and the already-started containers are left running. They are reaped by Testcontainers' Ryuk at
        // JVM exit rather than reused; getInstance() will not retry cleanly. Acceptable for a test harness.
        registerCluster();
        installSchema();
        applyDistribution();
    }

    private void registerCluster() {
        // Acquire both datasources INSIDE the try so the finally always covers them. newReadyDataSource(worker)
        // can throw (60s connect deadline / interrupt) after coordinatorDs is already live; if it were acquired
        // before the try the try block would never be entered and coordinatorDs would leak.
        SingleConnectionDataSource coordinatorDs = null;
        SingleConnectionDataSource workerDs = null;
        try {
            coordinatorDs = CitusClusterSupport.newReadyDataSource(coordinator);
            workerDs = CitusClusterSupport.newReadyDataSource(worker);
            CitusClusterSupport.registerCluster(new JdbcTemplate(coordinatorDs), new JdbcTemplate(workerDs));
        } finally {
            // destroy each independently so a failure closing one does not skip the other; either may be null
            // if newReadyDataSource threw before it was assigned.
            try {
                if (coordinatorDs != null) {
                    coordinatorDs.destroy();
                }
            } finally {
                if (workerDs != null) {
                    workerDs.destroy();
                }
            }
        }
    }

    private void installSchema() {
        log.info("Installing ThingsBoard schema on the Citus coordinator...");
        SingleConnectionDataSource ds = CitusClusterSupport.newReadyDataSource(coordinator);
        try {
            Connection conn = ds.getConnection();
            PostgreSqlInitializer.initDb(conn);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to install the ThingsBoard schema on the Citus coordinator", e);
        } finally {
            ds.destroy();
        }
        log.info("ThingsBoard schema installed on the Citus coordinator");
    }

    private void applyDistribution() {
        log.info("Applying Citus distribution on the coordinator...");
        SingleConnectionDataSource ds = CitusClusterSupport.newReadyDataSource(coordinator);
        try {
            CitusSettings settings = new CitusSettings();
            setField(settings, "enabled", true);
            setField(settings, "shardCount", TEST_SHARD_COUNT);
            new DefaultCitusSchemaService(new JdbcTemplate(ds), settings).applyDistribution();
        } finally {
            ds.destroy();
        }
        log.info("Citus distribution applied on the coordinator");
    }
}
