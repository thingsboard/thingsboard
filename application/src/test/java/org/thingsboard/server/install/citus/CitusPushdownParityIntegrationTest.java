// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.install.citus;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.dao.sql.alarm.AlarmRepository;
import org.thingsboard.server.dao.sql.asset.AssetRepository;
import org.thingsboard.server.dao.sql.blob.BlobEntityRepository;
import org.thingsboard.server.dao.sql.customer.CustomerRepository;
import org.thingsboard.server.dao.sql.dashboard.DashboardRepository;
import org.thingsboard.server.dao.sql.device.DeviceRepository;
import org.thingsboard.server.dao.sql.edge.EdgeRepository;
import org.thingsboard.server.dao.sql.entityview.EntityViewRepository;
import org.thingsboard.server.dao.sql.group.EntityGroupRepository;
import org.thingsboard.server.dao.sql.report.ReportRepository;
import org.thingsboard.server.dao.sql.report.ReportTemplateInfoRepository;
import org.thingsboard.server.dao.sql.role.RoleRepository;
import org.thingsboard.server.dao.sql.scheduler.SchedulerEventRepository;
import org.thingsboard.server.dao.sql.user.UserRepository;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.MergedGroupTypePermissionInfo;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.DeviceTypeFilter;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.NumericFilterPredicate;
import org.thingsboard.server.dao.sql.citus.AbstractCitusContainerTest;
import org.thingsboard.server.dao.sql.query.DefaultEntityQueryRepository;
import org.thingsboard.server.dao.sql.query.DefaultEntityQueryRepository.EntityDataSql;
import org.thingsboard.server.dao.sql.query.DefaultQueryLogComponent;
import org.thingsboard.server.dao.sql.query.EntityKeyMapping;
import org.thingsboard.server.dao.sql.query.SqlQueryContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Acceptance test for the co-located pushdown rewrite of the entity-data query.
 * <p>
 * {@code DefaultEntityQueryRepository}/{@code EntityKeyMapping} emit, for Citus, a
 * co-located <em>pushdown</em> form of the entity-data query whenever the filter resolves to a single
 * entity table (see {@code DefaultEntityQueryRepository#isSingleEntityTableFilter}). Instead of the older
 * Citus form -- which wraps the filtered entities in a {@code WITH entities AS (...)} CTE that Citus
 * recursively plans / materialises on the coordinator and then re-funnels -- the pushdown form inlines the
 * filtered anchor table as a derived {@code (...) entities} relation and joins the latest-value tables
 * straight on {@code entities.id} (the distribution column). Because {@code device} is hash-distributed on
 * {@code id} and co-located with {@code attribute_kv} / {@code ts_kv_latest}, the planner ships the join +
 * sort + limit to the workers as a distributed top-N rather than dragging every shard's KV rows back to the
 * coordinator.
 * <p>
 * This test pins the TWO guarantees the rewrite must hold against a real multi-worker Citus cluster:
 * <ol>
 *   <li><b>Parity</b> -- the pushdown form returns byte-for-byte the SAME ids+values, in the SAME order, as
 *       the legacy non-pushdown Citus CTE form, for the product-owner dashboard scenarios. It must not
 *       silently drop, duplicate, or re-order rows.</li>
 *   <li><b>Distribution</b> -- {@code EXPLAIN (ANALYZE, VERBOSE)} of the pushdown SQL shows a fan-out to all
 *       shards with a per-shard sort/limit, NOT a single-task coordinator funnel that {@code Seq Scan}s the
 *       whole {@code attribute_kv} table.</li>
 * </ol>
 *
 * <h3>Test seam (parity) -- drive the REAL generator</h3>
 * Rather than hand-transcribing the two generator arms into SQL templates (prone to silent drift from the
 * generator), this test drives the ACTUAL production generator: {@link DefaultEntityQueryRepository#buildEntityDataSql}
 * assembles the exact count + data SQL strings that {@code findEntityDataByQuery} would run, WITHOUT executing
 * anything (it only reads from {@code ctx}/{@code query}/{@code readPermissions} and calls the internal
 * string-builders). We construct the repository with mock collaborators (it never touches them for SQL
 * assembly), build a real {@link EntityDataQuery} + {@link SqlQueryContext}, and emit the data SQL TWICE per
 * variant -- once with {@link SqlQueryContext#setPushdownEligible}{@code (true)} (the co-located pushdown arm)
 * and once with it {@code false} (the legacy Citus CTE arm). Both forms are then run over the same seeded
 * co-located cluster via a {@link NamedParameterJdbcTemplate} (the generated SQL uses {@code :named} params
 * and {@code ctx} IS the {@link org.springframework.jdbc.core.namedparam.SqlParameterSource}, exactly as
 * production binds it), asserting identical rows.
 * <p>
 * THREE latest-join arms are covered: the {@code attribute_kv} SERVER_SCOPE attribute arm
 * ({@link EntityKeyType#SERVER_ATTRIBUTE}), the {@code ts_kv_latest} time-series arm
 * ({@link EntityKeyType#TIME_SERIES}), and the any-scope attribute DISTINCT ON arm
 * ({@link EntityKeyType#ATTRIBUTE}, with a value key filter). The seed populates both KV tables so each arm has data.
 *
 * <h3>Debugging failures</h3>
 * <ul>
 *   <li><b>If parity fails</b> (pushdownRows != cteRows): the two generator arms diverge on join semantics.
 *       Because the SQL is produced by the real generator, a parity failure points straight at a generator
 *       bug -- compare {@code EntityKeyMapping#toLatestJoin} pushdown vs. citus arms (the inner-vs-left join
 *       decision, the {@code entities.attr_read = 1} / {@code entities.ts_read = 1} permission flag in the ON
 *       clause, the sort tiebreak) and {@code buildEntitiesFromClause} (derived-table vs. CTE).</li>
 *   <li><b>If EXPLAIN shows {@code Task Count: 1} or a full {@code Seq Scan on attribute_kv}</b>: the join is
 *       NOT co-locating, so Citus fell back to a coordinator funnel. Revisit the inlined-{@code entities}
 *       subquery shape (it must stay a plain derived table, never a CTE) and the any-scope attribute
 *       {@code DISTINCT ON} arm.</li>
 * </ul>
 * The EXPLAIN matchers in {@link #explainShowsDistributedPerShardTopN()} are Citus-12.x flavoured; tune the
 * exact plan strings to the live cluster's output WITHOUT weakening the intent
 * (Task Count &gt; 1, a per-shard Sort/Limit, and no whole-table {@code attribute_kv} funnel).
 */
@Slf4j
class CitusPushdownParityIntegrationTest extends AbstractCitusContainerTest {

    /**
     * 32 shards matches the production default ({@code CitusSettings} default shard count was lowered to 32) and
     * the EXPLAIN "Task Count: 32" assertion below. A second worker is registered so the fan-out is genuinely
     * spread across nodes (single-worker would still report 32 tasks but would not prove cross-node distribution).
     */
    private static final int SHARD_COUNT = 32;
    private static final String WORKER2_ALIAS = "worker2";

    /** Seed size: enough rows per shard that a top-50 sort is meaningful and spread across all 32 shards. */
    private static final int DEVICE_COUNT = 2000;

    /** Every even-indexed seeded device is a Thermostat, so the type filter matches exactly half the seed. */
    private static final int THERMOSTAT_COUNT = DEVICE_COUNT / 2;

    private static final String TYPE_THERMOSTAT = "Thermostat";
    private static final String TYPE_SENSOR = "Sensor";

    private static final String TEMPERATURE_KEY = "temperature";
    // attribute_kv coordinates: SERVER_SCOPE/CLIENT_SCOPE attribute_type ids, and the key_dictionary id assigned
    // to "temperature".
    private static final int SERVER_SCOPE = 2;
    private static final int CLIENT_SCOPE = 1;
    private static final int TEMPERATURE_KEY_ID = 1;

    // Any-scope value-filter scenario: every seeded SERVER_SCOPE temperature is < 997, so a "> 2000" filter fails
    // the newest row of every device; a subset of devices gets an OLDER CLIENT_SCOPE row that passes.
    private static final long ANY_SCOPE_FILTER_THRESHOLD = 2000;
    private static final int ANY_SCOPE_PASSING_DEVICE_COUNT = 40;
    private static final UUID TENANT_ID = UUID.randomUUID();

    private static final int PAGE_SIZE = 50;

    private static final JdbcDatabaseContainer<?> WORKER2 = newWorker(WORKER2_ALIAS);

    private static NamedParameterJdbcTemplate namedJdbcTemplate;

    @BeforeAll
    static void setUpClusterAndSeed() {
        // A second worker so the pushdown fan-out genuinely spreads across nodes. Distribution + the 2000-device seed
        // (Step 1 fixture: production topology -- device distributed-on-id co-located with the KV tables, dimension
        // tables reference -- each device carrying a temperature server attribute AND a temperature ts_kv_latest row
        // so both KV pushdown arms have data) are built ONCE here: every test reads the same immutable fixture, so
        // re-distributing + re-seeding per method was pure waste. The one seed-mutating test cleans up after itself
        // (see removeClientScopeSeedMutations). The generated SQL uses :named params bound from ctx, so it is run the
        // way production does -- namedJdbcTemplate.queryForList(dataQuery, ctx) -- over the live-cluster DataSource.
        registerWorker(WORKER2, WORKER2_ALIAS);
        applyDistribution();
        seedDevices();
        namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate.getDataSource());
    }

    @AfterAll
    static void stopWorker2() {
        deregisterAndStopWorker(WORKER2, WORKER2_ALIAS,
                "attribute_kv", "ts_kv_latest", "device", "device_profile", "customer", "key_dictionary", "relation");
    }

    @AfterEach
    void removeClientScopeSeedMutations() {
        // Only parityAnyScopeAttributeValueFilterWithOlderPassingScopeRow adds CLIENT_SCOPE temperature rows; remove
        // them after every test so the shared @BeforeAll seed stays pristine and the tests remain order-independent.
        // The other tests never touch CLIENT_SCOPE, so this is a no-op after them.
        jdbcTemplate.update("DELETE FROM attribute_kv WHERE attribute_type = ? AND attribute_key = ?",
                CLIENT_SCOPE, TEMPERATURE_KEY_ID);
    }

    /**
     * Stands up the NEW production distribution: {@code attribute_kv} (anchor) + {@code ts_kv_latest} + {@code device}
     * are hash-distributed and co-located ({@code device} on {@code id}); the dimension tables
     * ({@code key_dictionary}, {@code device_profile}, {@code customer}, {@code relation}) are reference tables.
     * This mirrors what {@code DefaultCitusSchemaService#applyDistribution()} produces for these tables; it is open-
     * coded here (rather than invoking the schema service) so the representative schema can stay minimal -- the
     * service would attempt the full ~25-table reference set.
     */
    private static void applyDistribution() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS attribute_kv, ts_kv_latest, device, " +
                "device_profile, customer, key_dictionary, relation CASCADE");

        // distributed anchor + co-located KV/device
        jdbcTemplate.execute("CREATE TABLE attribute_kv (entity_id uuid, attribute_type int, attribute_key int, " +
                "bool_v boolean, str_v varchar, long_v bigint, dbl_v double precision, json_v json, " +
                "last_update_ts bigint, version bigint, " +
                "CONSTRAINT attribute_kv_pkey PRIMARY KEY (entity_id, attribute_type, attribute_key))");
        jdbcTemplate.execute("CREATE TABLE ts_kv_latest (entity_id uuid, key int, ts bigint, " +
                "bool_v boolean, str_v varchar, long_v bigint, dbl_v double precision, json_v json, version bigint, " +
                "CONSTRAINT ts_kv_latest_pkey PRIMARY KEY (entity_id, key))");
        jdbcTemplate.execute("CREATE TABLE device (id uuid PRIMARY KEY, created_time bigint, tenant_id uuid, " +
                "customer_id uuid, name varchar, type varchar, label varchar, additional_info varchar)");

        // reference dimension tables
        jdbcTemplate.execute("CREATE TABLE key_dictionary (key varchar(255) NOT NULL, key_id serial, " +
                "CONSTRAINT key_dictionary_id_pkey PRIMARY KEY (key_id), " +
                "CONSTRAINT key_dictionary_key_unq_key UNIQUE (key))");
        jdbcTemplate.execute("CREATE TABLE device_profile (id uuid PRIMARY KEY, tenant_id uuid, name varchar)");
        jdbcTemplate.execute("CREATE TABLE customer (id uuid PRIMARY KEY, tenant_id uuid, title varchar)");
        jdbcTemplate.execute("CREATE TABLE relation (from_id uuid, from_type varchar, to_id uuid, to_type varchar, " +
                "relation_type_group varchar, relation_type varchar, additional_info varchar, version bigint)");

        jdbcTemplate.execute("SELECT create_reference_table('key_dictionary')");
        jdbcTemplate.execute("SELECT create_reference_table('device_profile')");
        jdbcTemplate.execute("SELECT create_reference_table('customer')");
        jdbcTemplate.execute("SELECT create_reference_table('relation')");

        jdbcTemplate.execute("SELECT create_distributed_table('attribute_kv','entity_id', shard_count => " + SHARD_COUNT + ")");
        jdbcTemplate.execute("SELECT create_distributed_table('ts_kv_latest','entity_id', colocate_with => 'attribute_kv')");
        jdbcTemplate.execute("SELECT create_distributed_table('device','id', colocate_with => 'attribute_kv')");

        // sanity: device must be co-located with the KV anchor or the pushdown join cannot ship to the workers
        assertThat(colocationId("device")).as("device must co-locate with attribute_kv for pushdown")
                .isEqualTo(colocationId("attribute_kv"));
    }

    private static void seedDevices() {
        jdbcTemplate.update("INSERT INTO key_dictionary (key, key_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                TEMPERATURE_KEY, TEMPERATURE_KEY_ID);

        List<Object[]> deviceRows = new ArrayList<>(DEVICE_COUNT);
        List<Object[]> attrRows = new ArrayList<>(DEVICE_COUNT);
        List<Object[]> tsRows = new ArrayList<>(DEVICE_COUNT);
        for (int i = 0; i < DEVICE_COUNT; i++) {
            UUID id = UUID.randomUUID();
            // alternate types so ~half the population is Thermostat, spread across all shards
            String type = (i % 2 == 0) ? TYPE_THERMOSTAT : TYPE_SENSOR;
            String name = String.format("%s-%04d", type, i);
            deviceRows.add(new Object[]{id, (long) i, TENANT_ID, name, type});
            // temperature spread across a wide range so a top-50 sort is unambiguous and spread across shards
            long temperature = (long) (i % 997);
            attrRows.add(new Object[]{id, SERVER_SCOPE, TEMPERATURE_KEY_ID, temperature, (long) i, 1L});
            // mirror the attribute value into ts_kv_latest so the TIME_SERIES arm sorts over the same distribution
            tsRows.add(new Object[]{id, TEMPERATURE_KEY_ID, (long) i, temperature, 1L});
        }
        jdbcTemplate.batchUpdate(
                "INSERT INTO device (id, created_time, tenant_id, name, type) VALUES (?, ?, ?, ?, ?)", deviceRows);
        jdbcTemplate.batchUpdate(
                "INSERT INTO attribute_kv (entity_id, attribute_type, attribute_key, long_v, last_update_ts, version) " +
                        "VALUES (?, ?, ?, ?, ?, ?)", attrRows);
        jdbcTemplate.batchUpdate(
                "INSERT INTO ts_kv_latest (entity_id, key, ts, long_v, version) VALUES (?, ?, ?, ?, ?)", tsRows);
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Step 2 -- parity: pushdown form vs legacy CTE form (BOTH from the real generator) return identical rows.
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    void parityServerAttributeTopFiftyByTemperature() {
        List<Long> temperatures = assertGeneratedParity(
                EntityKeyType.SERVER_ATTRIBUTE, firstPageQuery(EntityKeyType.SERVER_ATTRIBUTE), PAGE_SIZE, THERMOSTAT_COUNT);
        // independent ground truth from the seed formula: guards against a bug UPSTREAM of the pushdown fork,
        // which arm-vs-arm equality alone cannot see
        assertThat(temperatures).isEqualTo(expectedTemperaturesPage(0));
    }

    @Test
    void parityTimeSeriesTopFiftyByTemperature() {
        List<Long> temperatures = assertGeneratedParity(
                EntityKeyType.TIME_SERIES, firstPageQuery(EntityKeyType.TIME_SERIES), PAGE_SIZE, THERMOSTAT_COUNT);
        assertThat(temperatures).isEqualTo(expectedTemperaturesPage(0));
    }

    /**
     * #30 variant (a): a non-zero OFFSET page (page index 1 -> OFFSET 50). The pushdown and CTE arms must agree
     * byte-for-byte on the SECOND page too, exercising the OFFSET term of the distributed top-N.
     */
    @Test
    void parityServerAttributeSecondPageByTemperature() {
        List<Long> temperatures = assertGeneratedParity(
                EntityKeyType.SERVER_ATTRIBUTE, secondPageQuery(EntityKeyType.SERVER_ATTRIBUTE), PAGE_SIZE, THERMOSTAT_COUNT);
        assertThat(temperatures).isEqualTo(expectedTemperaturesPage(1));
    }

    /**
     * #30 variant (b): an EMPTY result set. A filter that matches no entity (a device type that was never seeded)
     * must produce zero rows from BOTH arms -- the pushdown derived-table form and the legacy CTE form must agree
     * that the result is empty (and neither must error on the empty anchor).
     */
    @Test
    void parityEmptyResultSet() {
        assertGeneratedParity(EntityKeyType.SERVER_ATTRIBUTE, emptyQuery(EntityKeyType.SERVER_ATTRIBUTE), 0, 0);
    }

    /**
     * Value key filter on an ANY-SCOPE attribute where the newest row fails the filter but an older row passes.
     * attribute_kv holds one row per (entity, scope, key); the any-scope arm competes rows from ALL scopes for the
     * same key, and DISTINCT ON picks the newest by last_update_ts. Correct semantics (the plain LATERAL and Citus
     * CTE arms) apply the value filter INSIDE the subquery, BEFORE picking the latest row -- so a device whose
     * newest (SERVER_SCOPE) row fails "> 2000" but whose older CLIENT_SCOPE row passes IS returned, carrying the
     * older passing value. A pushdown arm that appends the filter to the join ON clause (pick-latest-then-filter)
     * would drop every such device; this parity run pins the pushdown arm to the filter-then-pick-latest semantics.
     */
    @Test
    void parityAnyScopeAttributeValueFilterWithOlderPassingScopeRow() {
        seedOlderPassingClientScopeAttributes();
        assertGeneratedParity(EntityKeyType.ATTRIBUTE, anyScopeValueFilterQuery(),
                ANY_SCOPE_PASSING_DEVICE_COUNT, ANY_SCOPE_PASSING_DEVICE_COUNT);
    }

    /**
     * Gives the first {@link #ANY_SCOPE_PASSING_DEVICE_COUNT} Thermostat devices a CLIENT_SCOPE temperature row that
     * is strictly OLDER than their SERVER_SCOPE row (last_update_ts = created_time) and passes the "> 2000" filter,
     * with distinct values so the DESC sort order is deterministic across both arms.
     */
    private static void seedOlderPassingClientScopeAttributes() {
        List<Map<String, Object>> devices = jdbcTemplate.queryForList(
                "SELECT id, created_time FROM device WHERE type = ? ORDER BY created_time LIMIT " + ANY_SCOPE_PASSING_DEVICE_COUNT,
                TYPE_THERMOSTAT);
        assertThat(devices).hasSize(ANY_SCOPE_PASSING_DEVICE_COUNT);
        List<Object[]> clientRows = new ArrayList<>(devices.size());
        long passingValue = ANY_SCOPE_FILTER_THRESHOLD + 1;
        for (Map<String, Object> device : devices) {
            long createdTime = ((Number) device.get("created_time")).longValue();
            clientRows.add(new Object[]{device.get("id"), CLIENT_SCOPE, TEMPERATURE_KEY_ID, passingValue++, createdTime - 1, 1L});
        }
        jdbcTemplate.batchUpdate(
                "INSERT INTO attribute_kv (entity_id, attribute_type, attribute_key, long_v, last_update_ts, version) " +
                        "VALUES (?, ?, ?, ?, ?, ?)", clientRows);
    }

    /**
     * Runs the pushdown and CTE arms of both the data AND the count query (production executes the count for
     * {@code totalElements}, so a count-side divergence would otherwise be invisible here), asserting arm-vs-arm
     * equality plus the externally-known totals. Returns the latest-key column of the parity-checked page (parsed
     * from the generator-assigned value alias) so callers can pin it against seed-formula ground truth.
     */
    private List<Long> assertGeneratedParity(EntityKeyType latestKeyType, EntityDataQuery query, int expectedSize, int expectedTotal) {
        DefaultEntityQueryRepository repo = newGenerator();

        SqlQueryContext pushdownCtx = newCtx(query);
        pushdownCtx.setPushdownEligible(true);
        EntityDataSql pushdownSql = repo.buildEntityDataSql(pushdownCtx, query, permissiveReadPermissions());

        SqlQueryContext cteCtx = newCtx(query);
        cteCtx.setPushdownEligible(false);
        EntityDataSql cteSql = repo.buildEntityDataSql(cteCtx, query, permissiveReadPermissions());

        List<Map<String, Object>> pushdownRows = namedJdbcTemplate.queryForList(pushdownSql.dataQuery(), pushdownCtx);
        List<Map<String, Object>> cteRows = namedJdbcTemplate.queryForList(cteSql.dataQuery(), cteCtx);

        assertThat(pushdownRows).isEqualTo(cteRows); // identical ordering + values
        assertThat(pushdownRows).hasSize(expectedSize);

        Integer pushdownCount = namedJdbcTemplate.queryForObject(pushdownSql.countQuery(), pushdownCtx, Integer.class);
        Integer cteCount = namedJdbcTemplate.queryForObject(cteSql.countQuery(), cteCtx, Integer.class);
        assertThat(pushdownCount).as("count-query parity (paging metadata)").isEqualTo(cteCount);
        assertThat(pushdownCount).as("count-query ground truth").isEqualTo(expectedTotal);

        String latestValueAlias = pushdownSql.selectionMapping().stream()
                .filter(EntityKeyMapping::isLatest)
                .findFirst().orElseThrow()
                .getValueAlias();
        return pushdownRows.stream()
                .map(row -> Long.parseLong((String) row.get(latestValueAlias)))
                .toList();
    }

    /**
     * The temperature column the top-N tests must return for page {@code page}, computed independently from the
     * seed formula (temperature = i % 997 over the even-indexed = Thermostat devices), sorted DESC. Equal values
     * are adjacent in a sorted list, so list equality is insensitive to the id tiebreak order at ties.
     */
    private static List<Long> expectedTemperaturesPage(int page) {
        List<Long> temperatures = new ArrayList<>();
        for (int i = 0; i < DEVICE_COUNT; i += 2) {
            temperatures.add((long) (i % 997));
        }
        temperatures.sort(Comparator.reverseOrder());
        int fromIndex = page * PAGE_SIZE;
        return temperatures.subList(fromIndex, fromIndex + PAGE_SIZE);
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Step 3 -- EXPLAIN: the pushdown plan must fan out to all shards with a per-shard top-N, not funnel.
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    void explainShowsDistributedPerShardTopN() {
        DefaultEntityQueryRepository repo = newGenerator();
        EntityDataQuery query = firstPageQuery(EntityKeyType.SERVER_ATTRIBUTE);

        SqlQueryContext pushdownCtx = newCtx(query);
        pushdownCtx.setPushdownEligible(true);
        EntityDataSql pushdownSql = repo.buildEntityDataSql(pushdownCtx, query, permissiveReadPermissions());

        // EXPLAIN must bind the same :named params as the data query, so wrap it through the named template too.
        String plan = String.join("\n",
                namedJdbcTemplate.queryForList(
                        "EXPLAIN (ANALYZE, VERBOSE) " + pushdownSql.dataQuery(), pushdownCtx, String.class));

        log.debug("Citus pushdown EXPLAIN plan:\n{}", plan);

        // STRUCTURAL EXPLAIN CHECK. The intent of the pushdown rewrite is a DISTRIBUTED top-N: every shard
        // computes its own ORDER BY ... LIMIT and ships only its local top-N up, where the coordinator merges those
        // partial results -- as opposed to a coordinator FUNNEL that pulls every shard's KV rows back and sorts on
        // the coordinator. The real Citus-12.1 plan for this query (captured live) has this shape:
        //
        //   Limit                                   <- coordinator merge: bounded
        //     -> Sort  (Sort Method: top-N heapsort)
        //          Output: remote_scan.*            <- merging the per-shard partial results, NOT raw table rows
        //          -> Custom Scan (Citus Adaptive)
        //               Task Count: 32              <- fan-out to ALL shards
        //               Tasks Shown: One of 32
        //               -> Task
        //                    Query: SELECT ... ORDER BY ... LIMIT '50'   <- per-shard top-N pushed to the worker
        //                    Node: host=...                              <- runs on a worker node
        //                    -> Limit -> ... -> Index Scan using attribute_kv_pkey_<shard> ...
        //
        // So the matcher pins, WITHOUT being version-fragile on cosmetic wording:
        //   (1) fan-out to all shards: "Task Count: <SHARD_COUNT>";
        //   (2) the coordinator-side bounded merge over the SHARD partial results: a Sort whose Output is
        //       remote_scan.* (the merged shard columns) under a Limit -- not a sort over physical KV columns;
        //   (3) the PER-SHARD top-N: the worker Task "Query:" line itself carries both an ORDER BY and a LIMIT
        //       (each shard limits locally before shipping);
        //   (4) NO whole-table funnel: attribute_kv is reached by an index scan on the per-shard relation, never a
        //       "Seq Scan on attribute_kv" of the whole distributed table dragged to the coordinator.
        // NOTE: a "Seq Scan on public.device_<shard> e" over the small anchor table IS expected (and fine); the
        // guard is specifically about the KV table not being funnelled.

        assertThat(plan).as("fan-out to all shards").contains("Task Count: " + SHARD_COUNT);
        assertThat(plan).as("coordinator merges partial results, does not funnel raw rows")
                .contains("Custom Scan (Citus Adaptive)");

        // (2) coordinator-side bounded merge: a Limit, and a Sort whose Output references the merged shard columns
        // (remote_scan.*), proving the coordinator only re-merges per-shard partial results rather than sorting the
        // physical KV rows itself.
        assertThat(plan).as("coordinator bounded merge").contains("Limit");
        assertThat(plan).as("coordinator sort is over per-shard merged columns (remote_scan), not raw KV rows")
                .containsPattern("(?s)Sort\\b.*?Output:\\s+remote_scan\\.");

        // (3) per-shard top-N: isolate the worker Task "Query:" subplan text and assert it carries its OWN ORDER BY
        // + LIMIT (every shard bounds locally before shipping). This is the load-bearing distributed-top-N signal.
        int queryIdx = plan.indexOf("Query: ");
        assertThat(queryIdx).as("a per-shard Task Query subplan is present").isGreaterThanOrEqualTo(0);
        int queryEnd = plan.indexOf("\n", queryIdx);
        String workerQuery = queryEnd >= 0 ? plan.substring(queryIdx, queryEnd) : plan.substring(queryIdx);
        assertThat(workerQuery).as("per-shard subquery sorts locally").contains("ORDER BY");
        assertThat(workerQuery).as("per-shard subquery limits locally before shipping").contains("LIMIT");
        assertThat(workerQuery).as("per-shard subquery scans the co-located device anchor shard, not a global table")
                .containsPattern("device_\\d+");

        // (4) whole-table funnel guard: the KV table must NOT be seq-scanned as the whole distributed relation.
        assertThat(plan).as("no whole-table attribute_kv funnel").doesNotContain("Seq Scan on attribute_kv");
        assertThat(plan).as("no whole-table ts_kv_latest funnel").doesNotContain("Seq Scan on ts_kv_latest");
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Generator wiring -- the real DefaultEntityQueryRepository, with mock collaborators (buildEntityDataSql never
    // touches them: it only assembles SQL strings from ctx/query/readPermissions).
    // ----------------------------------------------------------------------------------------------------------------

    private DefaultEntityQueryRepository newGenerator() {
        DefaultEntityQueryRepository repo = new DefaultEntityQueryRepository(
                mock(NamedParameterJdbcTemplate.class),
                mock(TransactionTemplate.class),
                mock(AssetRepository.class),
                mock(CustomerRepository.class),
                mock(DeviceRepository.class),
                mock(EntityViewRepository.class),
                mock(EdgeRepository.class),
                mock(UserRepository.class),
                mock(DashboardRepository.class),
                mock(EntityGroupRepository.class),
                mock(SchedulerEventRepository.class),
                mock(RoleRepository.class),
                mock(AlarmRepository.class),
                mock(BlobEntityRepository.class),
                mock(ReportTemplateInfoRepository.class),
                mock(ReportRepository.class),
                mock(DefaultQueryLogComponent.class));
        // citusEnabled is a @Value field injected after construction in production; set it directly here so the
        // generator emits the Citus arms (pushdown vs. CTE then toggled per-ctx via setPushdownEligible).
        ReflectionTestUtils.setField(repo, "citusEnabled", true);
        // nullsOrderStrategy is likewise a @Value field (default "default"); without it resolveNullsOrder() switches
        // over a null String and NPEs. Set the production default so the ORDER BY matches the real generator.
        ReflectionTestUtils.setField(repo, "nullsOrderStrategy", "default");
        // initSelectArms() runs in @PostConstruct (validateNullsOrderStrategy) once citusEnabled is known; invoke it
        // explicitly so the distributed select arms are populated for the generator.
        ReflectionTestUtils.invokeMethod(repo, "initSelectArms");
        return repo;
    }

    /**
     * A Thermostat device-type query selecting/sorting on the temperature {@code latestKeyType}, page {@code page}.
     * Selecting + sorting on the latest key forces the corresponding latest-join arm into the generated SQL.
     */
    private EntityDataQuery thermostatQuery(EntityKeyType latestKeyType, int page) {
        DeviceTypeFilter filter = new DeviceTypeFilter(List.of(TYPE_THERMOSTAT), "");
        EntityKey latestKey = new EntityKey(latestKeyType, TEMPERATURE_KEY);
        EntityDataSortOrder sortOrder = new EntityDataSortOrder(latestKey, EntityDataSortOrder.Direction.DESC);
        EntityDataPageLink pageLink = new EntityDataPageLink(PAGE_SIZE, page, null, sortOrder);
        EntityKey nameField = new EntityKey(EntityKeyType.ENTITY_FIELD, "name");
        return new EntityDataQuery(filter, pageLink, List.of(nameField), List.of(latestKey), null);
    }

    private EntityDataQuery firstPageQuery(EntityKeyType latestKeyType) {
        return thermostatQuery(latestKeyType, 0);
    }

    private EntityDataQuery secondPageQuery(EntityKeyType latestKeyType) {
        return thermostatQuery(latestKeyType, 1);
    }

    /**
     * A Thermostat query selecting/sorting on the ANY-SCOPE temperature attribute with a numeric "> threshold"
     * value key filter on that same key, first page.
     */
    private EntityDataQuery anyScopeValueFilterQuery() {
        DeviceTypeFilter filter = new DeviceTypeFilter(List.of(TYPE_THERMOSTAT), "");
        EntityKey latestKey = new EntityKey(EntityKeyType.ATTRIBUTE, TEMPERATURE_KEY);
        EntityDataSortOrder sortOrder = new EntityDataSortOrder(latestKey, EntityDataSortOrder.Direction.DESC);
        EntityDataPageLink pageLink = new EntityDataPageLink(PAGE_SIZE, 0, null, sortOrder);
        EntityKey nameField = new EntityKey(EntityKeyType.ENTITY_FIELD, "name");
        KeyFilter valueFilter = new KeyFilter();
        valueFilter.setKey(latestKey);
        valueFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate predicate = new NumericFilterPredicate();
        predicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        predicate.setValue(new FilterPredicateValue<>((double) ANY_SCOPE_FILTER_THRESHOLD));
        valueFilter.setPredicate(predicate);
        return new EntityDataQuery(filter, pageLink, List.of(nameField), List.of(latestKey), List.of(valueFilter));
    }

    /**
     * A query whose device-type filter matches NO seeded device (only Thermostat/Sensor are seeded), so both arms
     * must yield an empty page.
     */
    private EntityDataQuery emptyQuery(EntityKeyType latestKeyType) {
        DeviceTypeFilter filter = new DeviceTypeFilter(List.of("NoSuchTypeEverSeeded"), "");
        EntityKey latestKey = new EntityKey(latestKeyType, TEMPERATURE_KEY);
        EntityDataSortOrder sortOrder = new EntityDataSortOrder(latestKey, EntityDataSortOrder.Direction.DESC);
        EntityDataPageLink pageLink = new EntityDataPageLink(PAGE_SIZE, 0, null, sortOrder);
        EntityKey nameField = new EntityKey(EntityKeyType.ENTITY_FIELD, "name");
        return new EntityDataQuery(filter, pageLink, List.of(nameField), List.of(latestKey), null);
    }

    /**
     * A SqlQueryContext for a SYS-tenant-ish permissive read: building the QueryContext with
     * {@code ignorePermissionCheck = true} grants {@code Resource.ALL}, so buildCommonEntitiesQuery takes the
     * "all read permissions" simple arm (literal {@code 1 as attr_read, 1 as ts_read}) -- matching the seed where
     * every device is readable. citusEnabled = true so the Citus arms are emitted; pushdown is toggled per-call.
     */
    private SqlQueryContext newCtx(EntityDataQuery query) {
        QueryContext securityCtx = new QueryContext(
                TenantId.fromUUID(TENANT_ID), null, EntityType.DEVICE,
                MergedUserPermissions.ALL, query.getEntityFilter(), true);
        return new SqlQueryContext(securityCtx, true);
    }

    /** Non-restrictive read permissions (generic read, no group restrictions) -- the value buildCommonEntitiesQuery reads. */
    private MergedGroupTypePermissionInfo permissiveReadPermissions() {
        return MergedGroupTypePermissionInfo.MERGED_GROUP_TYPE_PERMISSION_INFO_EMPTY_GROUPS_HAS_GENERIC_READ_TRUE;
    }
}
