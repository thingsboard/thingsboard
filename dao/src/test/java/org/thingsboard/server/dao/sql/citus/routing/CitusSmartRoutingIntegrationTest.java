// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus.routing;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.thingsboard.server.dao.service.CitusTestSupport;
import org.thingsboard.server.dao.sql.attributes.AttributeKvInsertRepository;
import org.thingsboard.server.dao.sql.citus.AbstractCitusContainerTest;
import org.thingsboard.server.dao.sql.citus.CitusShardLocator;
import org.thingsboard.server.dao.sqlts.insert.latest.sql.SqlLatestInsertTsRepository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Multi-node integration test for Citus smart-client shard routing.
 * <p>
 * Stands up a two-worker Citus cluster (the shared single worker from {@link AbstractCitusContainerTest}
 * plus a second worker on the inherited {@link #NETWORK}, à la {@code CitusRebalanceIntegrationTest}) so
 * KV shards spread across both workers, then exercises the real routing stack end-to-end:
 * {@link CitusShardLocator} -> {@link CitusShardPlacement} -> {@link CitusWorkerRegistry} -> {@link CitusShardRouter}.
 * <p>
 * <b>Host overrides.</b> {@code pg_dist_node} advertises the docker-internal nodenames ({@code worker},
 * {@code worker2}) on port 5432, which are unreachable from the test JVM. The registry's
 * {@code worker_host_overrides} feature is wired here to map each nodename to the testcontainers
 * published {@code host:mappedPort(5432)}, so the per-worker Hikari pools connect over the mapped host
 * ports and the fail-fast reachability gate passes. This both makes the test work AND exercises the
 * override feature.
 */
@Slf4j
class CitusSmartRoutingIntegrationTest extends AbstractCitusContainerTest {

    private static final String WORKER2_ALIAS = "worker2";
    private static final int SHARD_COUNT = 16;

    private static final JdbcDatabaseContainer<?> WORKER2 = newWorker(WORKER2_ALIAS);

    /** The production Citus-mode upsert SQL, read from the real repositories so this test cannot drift from them. */
    private static final String TS_KV_LATEST_UPSERT_SQL = citusTsKvLatestUpsertSql();
    private static final String ATTRIBUTE_KV_UPSERT_SQL = citusAttributeKvUpsertSql();

    private CitusShardLocator locator;
    private CitusShardPlacement placement;
    private CitusWorkerRegistry registry;
    private CitusShardRouter router;

    @BeforeAll
    static void startWorker2() {
        // register worker2 on the shared coordinator so shards spread across two workers
        registerWorker(WORKER2, WORKER2_ALIAS);
    }

    @AfterAll
    static void stopWorker2() {
        deregisterAndStopWorker(WORKER2, WORKER2_ALIAS, "attribute_kv", "ts_kv_latest", "key_dictionary");
    }

    @BeforeEach
    void setUp() {
        // Co-located KV family: a key_dictionary reference table + attribute_kv / ts_kv_latest distributed
        // on entity_id, co-located on the same hash ranges.
        jdbcTemplate.execute("DROP TABLE IF EXISTS attribute_kv CASCADE");
        jdbcTemplate.execute("DROP TABLE IF EXISTS ts_kv_latest CASCADE");
        jdbcTemplate.execute("DROP TABLE IF EXISTS key_dictionary CASCADE");

        jdbcTemplate.execute("CREATE TABLE key_dictionary (key varchar(255) NOT NULL, key_id int NOT NULL, " +
                "CONSTRAINT key_dictionary_pkey PRIMARY KEY (key))");
        jdbcTemplate.execute("SELECT create_reference_table('key_dictionary')");

        jdbcTemplate.execute("CREATE TABLE attribute_kv (entity_id uuid, attribute_type int, attribute_key int, " +
                "str_v varchar(10000000), long_v bigint, dbl_v double precision, bool_v boolean, json_v json, " +
                "last_update_ts bigint, version bigint, " +
                "CONSTRAINT attribute_kv_pkey PRIMARY KEY (entity_id, attribute_type, attribute_key))");
        jdbcTemplate.execute("SELECT create_distributed_table('attribute_kv','entity_id', shard_count => " + SHARD_COUNT + ")");

        jdbcTemplate.execute("CREATE TABLE ts_kv_latest (entity_id uuid, key int, ts bigint, bool_v boolean, " +
                "str_v varchar(10000000), long_v bigint, dbl_v double precision, json_v json, version bigint, " +
                "CONSTRAINT ts_kv_latest_pkey PRIMARY KEY (entity_id, key))");
        jdbcTemplate.execute("SELECT create_distributed_table('ts_kv_latest','entity_id', colocate_with => 'attribute_kv')");

        locator = new CitusShardLocator(jdbcTemplate, newCitusSettings(SHARD_COUNT), "attribute_kv");
        locator.refresh();

        placement = new CitusShardPlacement(jdbcTemplate, "attribute_kv");
        placement.refresh();

        CitusSmartRoutingSettings routingSettings = new CitusSmartRoutingSettings();
        ReflectionTestUtils.setField(routingSettings, "enabled", true);
        ReflectionTestUtils.setField(routingSettings, "workerPoolSize", 4);
        ReflectionTestUtils.setField(routingSettings, "workerConnectionTimeoutMs", 10000L);
        ReflectionTestUtils.setField(routingSettings, "workerHostOverridesRaw", workerHostOverrides());

        registry = new CitusWorkerRegistry(jdbcTemplate, routingSettings, CITUS.getJdbcUrl(), "postgres", "postgres");
        registry.start();

        router = new CitusShardRouter(locator, placement, registry, routingSettings);
    }

    /** Builds the comma-separated {@code nodename=host:port} override string for both workers. */
    private static String workerHostOverrides() {
        return WORKER_ALIAS + "=" + WORKER.getHost() + ":" + WORKER.getMappedPort(PG_PORT) + "," +
                WORKER2_ALIAS + "=" + WORKER2.getHost() + ":" + WORKER2.getMappedPort(PG_PORT);
    }

    @AfterEach
    void tearDown() {
        // Runs even if @BeforeEach threw partway after registry.start() and even if the test body threw,
        // so the per-worker Hikari pools never leak against the shared static containers. closeRegistry() is
        // idempotent (CitusWorkerRegistry.close() tolerates a double close).
        closeRegistry();
    }

    private void closeRegistry() {
        if (registry != null) {
            registry.close();
        }
    }

    /**
     * #1 + #2 + cross-check: a routed write lands on the shard's true owner and reads back the same row;
     * the same row is visible cluster-wide through the coordinator; and a routed read returns identical
     * values to the coordinator (parity). UUIDs are picked to span both workers.
     */
    @Test
    void routedReadWriteLandsOnOwnerAndMatchesCoordinator() {
        List<UUID> ids = idsSpanningBothWorkers(2);

        for (UUID id : ids) {
            int bucket = locator.bucket(id);
            JdbcTemplate t = router.forBucket(bucket);
            assertThat(t).as("forBucket and forKey must resolve to the same template").isSameAs(router.forKey(id));

            CitusWorkerNode owner = placement.workerForBucket(bucket);
            int routedGroup = groupIdOfTemplate(t);
            assertThat(routedGroup)
                    .as("routed template for %s must be the shard owner", id)
                    .isEqualTo(owner.groupId());

            long ts = 1000L;
            long expectedLong = 42L;
            // Citus-mode upsert: insert version=1, on conflict bump version, RETURNING version.
            Long version = upsertTsKvLatest(t, id, 7, ts, expectedLong);
            assertThat(version).isEqualTo(1L);

            // read back via the same worker template
            Long longV = t.queryForObject(
                    "SELECT long_v FROM ts_kv_latest WHERE entity_id = ?::uuid AND key = 7", Long.class, id.toString());
            assertThat(longV).as("worker-local read for %s", id).isEqualTo(expectedLong);

            // cross-check: visible through the coordinator (Citus sees it cluster-wide)
            Long coordLongV = jdbcTemplate.queryForObject(
                    "SELECT long_v FROM ts_kv_latest WHERE entity_id = ?::uuid AND key = 7", Long.class, id.toString());
            assertThat(coordLongV).as("coordinator read for %s", id).isEqualTo(expectedLong);

            // #2 parity: full-row read on worker == on coordinator
            Map<String, Object> workerRow = t.queryForMap(
                    "SELECT entity_id, key, ts, long_v, version FROM ts_kv_latest WHERE entity_id = ?::uuid AND key = 7",
                    id.toString());
            Map<String, Object> coordRow = jdbcTemplate.queryForMap(
                    "SELECT entity_id, key, ts, long_v, version FROM ts_kv_latest WHERE entity_id = ?::uuid AND key = 7",
                    id.toString());
            assertThat(workerRow).as("routed read must match coordinator read for %s", id).isEqualTo(coordRow);
        }
    }

    /**
     * #3 version+1 progression: issuing the Citus-mode upsert twice for the same (entity,key) via the
     * worker template moves version 1 -> 2.
     */
    @Test
    void versionProgressesOnRepeatedUpsert() {
        UUID id = idsSpanningBothWorkers(1).get(0);
        JdbcTemplate t = router.forKey(id);

        Long v1 = upsertTsKvLatest(t, id, 3, 100L, 1L);
        assertThat(v1).as("first upsert inserts version 1").isEqualTo(1L);

        Long v2 = upsertTsKvLatest(t, id, 3, 200L, 2L);
        assertThat(v2).as("second upsert bumps version to 2").isEqualTo(2L);

        // also verify attribute_kv upsert version progression (same co-location group)
        Long av1 = upsertAttributeKv(t, id, 1, 5, 10L);
        assertThat(av1).isEqualTo(1L);
        Long av2 = upsertAttributeKv(t, id, 1, 5, 20L);
        assertThat(av2).isEqualTo(2L);
    }

    /**
     * #4 bucket alignment: placement.shardCount() == locator.shardCount(), and for every bucket the
     * placement's owning group matches the owner Citus reports for the same shard (cross-checked against
     * pg_dist_shard / pg_dist_placement ordered by (shardminvalue)::int, the same ordering the locator uses).
     */
    @Test
    void bucketAlignmentMatchesCitusCatalog() {
        assertThat(placement.shardCount()).as("placement vs locator shard count").isEqualTo(locator.shardCount());
        assertThat(placement.shardCount()).isEqualTo(SHARD_COUNT);
        // does not throw
        CitusShardRouter.assertBucketAlignment(locator.shardCount(), placement.shardCount(), "attribute_kv");

        // Owner groups in the same ascending-by-(shardminvalue)::int order the placement uses.
        List<Integer> catalogOwners = jdbcTemplate.query(
                "SELECT p.groupid AS groupid FROM pg_dist_shard s " +
                        "JOIN pg_dist_placement p ON p.shardid = s.shardid " +
                        "JOIN pg_dist_node n ON n.groupid = p.groupid " +
                        "WHERE s.logicalrelid = 'attribute_kv'::regclass AND p.shardstate = 1 " +
                        "AND n.noderole = 'primary' AND n.isactive AND n.groupid <> 0 " +
                        "ORDER BY (s.shardminvalue)::int",
                (rs, rowNum) -> rs.getInt("groupid"));
        assertThat(catalogOwners).hasSize(SHARD_COUNT);

        for (int bucket = 0; bucket < SHARD_COUNT; bucket++) {
            assertThat(placement.workerForBucket(bucket).groupId())
                    .as("placement owner of bucket %s must match catalog", bucket)
                    .isEqualTo(catalogOwners.get(bucket));
        }

        // sanity: the placement actually spreads across both registered worker groups
        assertThat(placement.bucketOwners().stream().map(CitusWorkerNode::groupId).distinct().count())
                .as("shards should span both workers").isEqualTo(2L);
    }

    /**
     * #5 stale placement via MX forwarding: deliberately route a single-entity read to the WRONG worker
     * (a non-owner group) and assert it STILL returns the correct row, because Citus MX forwards a query
     * on a distributed table to the true shard owner. This proves stale placements are correctness-safe.
     * If worker->worker forwarding is not permitted in the container cluster, the assertion documents and
     * skips by tolerating the access error.
     */
    @Test
    void readOnNonOwnerStillReturnsCorrectRowViaMxForwarding() {
        UUID id = idsSpanningBothWorkers(1).get(0);
        int bucket = locator.bucket(id);
        int ownerGroup = placement.workerForBucket(bucket).groupId();

        // write through the true owner
        upsertTsKvLatest(router.forBucket(bucket), id, 9, 500L, 77L);

        // pick a different (non-owner) worker group
        Integer nonOwnerGroup = registry.workerGroupIds().stream()
                .filter(g -> g != ownerGroup)
                .findFirst()
                .orElse(null);
        assertThat(nonOwnerGroup).as("need a second worker group for the non-owner read").isNotNull();

        JdbcTemplate nonOwner = registry.templateForGroup(nonOwnerGroup);
        Long longV;
        try {
            longV = nonOwner.queryForObject(
                    "SELECT long_v FROM ts_kv_latest WHERE entity_id = ?::uuid AND key = 9", Long.class, id.toString());
        } catch (DataAccessException e) {
            // A DataAccessException here means the forwarded query could not run at all. Distinguish the
            // environment limitation ("MX forwarding not available in this container") from a genuine failure:
            // the former is a connection / node-to-node-auth / MX-not-enabled error and warrants a SKIP;
            // anything else is a real failure and must fail the test (re-thrown). A query that ran but returned
            // the wrong value does NOT land here — it fails the assertThat below with an AssertionError.
            if (isMxForwardingUnavailable(e)) {
                log.warn("Worker->worker MX forwarding not available in this container cluster; skipping #5 assertion. " +
                        "Error from non-owner group {}: {}", nonOwnerGroup, e.getMessage());
                Assumptions.assumeTrue(false,
                        "Citus MX worker->worker forwarding not available in this container cluster: " + e.getMessage());
            }
            throw e;
        }
        assertThat(longV)
                .as("Citus MX should forward a distributed-table read on a non-owner to the true owner")
                .isEqualTo(77L);
        log.info("MX forwarding works: non-owner group {} returned the row owned by group {}", nonOwnerGroup, ownerGroup);
    }

    /**
     * Heuristic for distinguishing "Citus MX forwarding is not available in this container cluster" (an
     * environment limitation we SKIP on) from a genuine query failure (which must FAIL the test). The
     * not-available case manifests as a connection / node-to-node-auth failure when the non-owner worker
     * tries to reach the true shard owner: SQLState class 08 (connection exception) / 28 (invalid
     * authorization), or a Citus message indicating MX is disabled or the node connection could not be
     * established. Everything else is treated as a real failure.
     */
    private static boolean isMxForwardingUnavailable(DataAccessException e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SQLException sqlEx) {
                String state = sqlEx.getSQLState();
                if (state != null && (state.startsWith("08") || state.startsWith("28"))) {
                    return true;
                }
            }
            String msg = t.getMessage();
            if (msg != null) {
                String lower = msg.toLowerCase();
                if (lower.contains("could not connect")
                        || lower.contains("connection to the remote node")
                        || lower.contains("connection failed")
                        || lower.contains("no pg_hba.conf entry")
                        || lower.contains("password authentication failed")
                        || lower.contains("metadata is not synced")
                        || lower.contains("citus.enable_metadata_sync")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * #6 down/unreachable worker surfaces an error (no silent fallback). Two facets, both non-destructive
     * to the shared Citus cluster (stopping the shared worker2 container leaves the coordinator's
     * distributed-table metadata pointing at a dead host, which corrupts sibling tests' DROP/refresh —
     * so this exercises the registry's no-fallback contract directly instead):
     * <ol>
     *   <li><b>Fail-fast gate.</b> A registry built with an override pointing worker2 at a dead port must
     *       throw from {@link CitusWorkerRegistry#start()} naming the unreachable node — it does NOT
     *       silently degrade to coordinator-only.</li>
     *   <li><b>Closed pool, no fallback (smoke).</b> {@code registry.close()} closes EVERY pool; routing
     *       any bucket afterwards must throw rather than fall back to another worker / the coordinator.
     *       Deliberately a close-only smoke, NOT the {@code refresh()}-driven partial-retirement scenario
     *       (the retired group's pool closed while the survivor stays pooled and routing) — that property
     *       is pinned by the {@code CitusWorkerRegistryTest} retirement unit test
     *       ({@code groupGoneFromCatalogIsRetiredWhileSurvivorPoolIsUntouched}).</li>
     * </ol>
     */
    @Test
    void unreachableWorkerSurfacesErrorNoFallback() {
        Integer worker2Group = workerGroupId(WORKER2_ALIAS);
        assertThat(worker2Group).as("worker2 must be registered").isNotNull();

        // facet 1: fail-fast gate at start() when a worker override points at a dead port.
        CitusSmartRoutingSettings deadSettings = new CitusSmartRoutingSettings();
        ReflectionTestUtils.setField(deadSettings, "enabled", true);
        ReflectionTestUtils.setField(deadSettings, "workerPoolSize", 2);
        ReflectionTestUtils.setField(deadSettings, "workerConnectionTimeoutMs", 2000L);
        // worker reachable, worker2 -> dead port (1 is never a live postgres) => start() must fail fast.
        String deadOverrides = WORKER_ALIAS + "=" + WORKER.getHost() + ":" + WORKER.getMappedPort(PG_PORT) + "," +
                WORKER2_ALIAS + "=" + WORKER2.getHost() + ":1";
        ReflectionTestUtils.setField(deadSettings, "workerHostOverridesRaw", deadOverrides);
        CitusWorkerRegistry deadRegistry =
                new CitusWorkerRegistry(jdbcTemplate, deadSettings, CITUS.getJdbcUrl(), "postgres", "postgres");
        assertThatThrownBy(deadRegistry::start)
                .as("an unreachable worker must fail the boot reachability gate, not silently degrade")
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(WORKER2_ALIAS);
        // deadRegistry is a local registry not tracked by the `registry` field that @AfterEach closes, so close it
        // here. Production start() already rolls back pools opened during the failed fail-fast pass (the reachable
        // `worker` pool is closed internally), so this close() is belt-and-suspenders.
        deadRegistry.close();

        // facet 2: a routed op to a bucket whose pool is closed surfaces an error (no fallback) — close-only smoke.
        int targetBucket = -1;
        for (int b = 0; b < placement.shardCount(); b++) {
            if (placement.workerForBucket(b).groupId() == worker2Group) {
                targetBucket = b;
                break;
            }
        }
        assertThat(targetBucket).as("worker2 must own at least one bucket").isGreaterThanOrEqualTo(0);

        // sanity: routed op works while the pool is present
        assertThat(router.forBucket(targetBucket).queryForObject("SELECT 1", Integer.class)).isEqualTo(1);

        // close ALL pools. This does not mirror refresh()-driven partial retirement (where the survivor
        // keeps routing — covered by CitusWorkerRegistryTest.groupGoneFromCatalogIsRetiredWhileSurvivorPoolIsUntouched);
        // it only smokes that a closed-pool bucket surfaces an error instead of falling back.
        registry.close();
        int finalTargetBucket = targetBucket;
        assertThatThrownBy(() -> router.forBucket(finalTargetBucket))
                .as("a routed op to a closed worker pool must surface an error, not silently fall back")
                .isInstanceOf(IllegalStateException.class);
    }

    // ---- helpers ----

    /**
     * Generates random UUIDs until {@code count} buckets owned by DISTINCT worker groups are found, so the
     * returned ids provably span both workers. Returns one id per distinct owner group (up to count).
     */
    private List<UUID> idsSpanningBothWorkers(int count) {
        Map<Integer, UUID> byGroup = new LinkedHashMap<>();
        for (int i = 0; i < 100000 && byGroup.size() < count; i++) {
            UUID id = UUID.randomUUID();
            int group = placement.workerForBucket(locator.bucket(id)).groupId();
            byGroup.putIfAbsent(group, id);
        }
        assertThat(byGroup.size()).as("could not find %s ids spanning distinct workers", count).isGreaterThanOrEqualTo(count);
        return new ArrayList<>(byGroup.values()).subList(0, count);
    }

    /**
     * Resolves the worker group id behind a routed template by matching it against the registry. Relies on the
     * registry returning a STABLE per-group JdbcTemplate instance, so the reference equality (==) below is intentional.
     */
    private int groupIdOfTemplate(JdbcTemplate template) {
        Set<Integer> groups = registry.workerGroupIds();
        for (Integer group : groups) {
            if (registry.templateForGroup(group) == template) {
                return group;
            }
        }
        throw new IllegalStateException("template not found among registry worker groups " + groups);
    }

    /**
     * Executes the REAL production Citus-mode {@code ts_kv_latest} upsert ({@link #TS_KV_LATEST_UPSERT_SQL}, read from
     * {@link SqlLatestInsertTsRepository}) through the routed worker template: INSERT version 1, ON CONFLICT bump
     * {@code version = ts_kv_latest.version + 1} guarded by {@code ts_kv_latest.ts <= ?}, RETURNING version. Params are
     * bound in the exact positional order the repository's {@code setOnInsertOrUpdateValues} uses; every column other
     * than the ones under assertion (entity_id/key/ts/long_v/version) is left null, as production binds typed nulls.
     */
    private Long upsertTsKvLatest(JdbcTemplate t, UUID entityId, int key, long ts, long longV) {
        return t.queryForObject(TS_KV_LATEST_UPSERT_SQL, Long.class,
                entityId, key, ts, null, null, longV, null, null,
                ts, null, null, longV, null, null,
                ts);
    }

    /**
     * Executes the REAL production Citus-mode {@code attribute_kv} upsert ({@link #ATTRIBUTE_KV_UPSERT_SQL}, read from
     * {@link AttributeKvInsertRepository}) through the routed worker template: INSERT version 1, ON CONFLICT bump
     * {@code version = attribute_kv.version + 1}, RETURNING version. Params follow the repository's
     * {@code setOnInsertOrUpdateValues} order; unasserted columns are left null.
     */
    private Long upsertAttributeKv(JdbcTemplate t, UUID entityId, int attributeType, int attributeKey, long longV) {
        return t.queryForObject(ATTRIBUTE_KV_UPSERT_SQL, Long.class,
                entityId, attributeType, attributeKey, null, longV, null, null, null, 0L,
                null, longV, null, null, null, 0L);
    }

    /** Builds the production Citus-mode ts_kv_latest upsert SQL from the real repository (update_by_latest_ts = true). */
    private static String citusTsKvLatestUpsertSql() {
        SqlLatestInsertTsRepository repository = new SqlLatestInsertTsRepository();
        ReflectionTestUtils.setField(repository, "citusSettings", CitusTestSupport.citusSettings(true));
        ReflectionTestUtils.setField(repository, "updateByLatestTs", true);
        ReflectionTestUtils.invokeMethod(repository, "init");
        return (String) ReflectionTestUtils.invokeMethod(repository, "getInsertOrUpdateQuery");
    }

    /** Builds the production Citus-mode attribute_kv upsert SQL from the real repository. */
    private static String citusAttributeKvUpsertSql() {
        AttributeKvInsertRepository repository = new AttributeKvInsertRepository();
        ReflectionTestUtils.setField(repository, "citusSettings", CitusTestSupport.citusSettings(true));
        ReflectionTestUtils.invokeMethod(repository, "initQueries");
        return (String) ReflectionTestUtils.invokeMethod(repository, "getInsertOrUpdateQuery");
    }
}
