// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus;

import java.util.Map;

/**
 * Single source of truth for the Spring property overrides that repoint a test datasource at the shared
 * {@link CitusTestCluster} coordinator (over the plain {@code org.postgresql.Driver}) and enable Citus mode
 * with smart routing.
 * <p>
 * Both {@link CitusDaoTestContextInitializer} (per-class opt-in via {@code @CitusDaoSqlTest}) and
 * {@link CitusAllTestsContextCustomizerFactory} (global opt-in via {@code -Dtb.citus.alltests=true}) build the
 * exact same property map from here so the two entry points never drift.
 */
final class CitusDaoTestProperties {

    /** Name of the {@link org.springframework.core.env.MapPropertySource} added via {@code addFirst}. */
    static final String PROPERTY_SOURCE_NAME = "citusTestDataSource";

    private CitusDaoTestProperties() {
    }

    /**
     * Builds the highest-precedence Citus datasource property overrides for the given cluster. Added via
     * {@code addFirst} so they override the {@code spring.datasource.*} values declared in the
     * {@code @TestPropertySource} files (notably the Testcontainers {@code jdbc:tc:postgresql} URL).
     */
    static Map<String, Object> asMap(CitusTestCluster cluster) {
        return Map.of(
                "spring.datasource.url", cluster.getJdbcUrl(),
                "spring.datasource.username", cluster.getUsername(),
                "spring.datasource.password", cluster.getPassword(),
                "spring.datasource.driverClassName", "org.postgresql.Driver",
                "database.citus.enabled", "true",
                "database.citus.smart_routing.enabled", "true",
                "database.citus.smart_routing.worker_host_overrides", cluster.getWorkerHostOverride(),
                // Namespace the Citus contexts' Redis cache keys. In RedisSqlTestSuite / RedisClusterSqlTestSuite every
                // test class shares ONE Redis (AbstractRedisContainer / AbstractRedisClusterContainer), and constant
                // cache keys such as the default-tenant-profile key (tenantProfiles::default) carry no datasource
                // discriminator. A plain-Postgres *ServiceSqlTest that creates the default profile in its own DB would
                // otherwise poison that key, so a later Citus test gets a cache hit and skips creating the profile in
                // the Citus cluster -> tenant FK violation. A distinct key prefix keeps the Citus contexts' cache
                // keyspace separate from the plain-Postgres contexts'. Unlike redis.db, a key prefix also works in
                // Redis Cluster mode, where only logical database 0 exists.
                "cache.key_prefix", "citus:");
    }
}
