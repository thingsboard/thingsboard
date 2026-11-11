// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.citus;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.MergedGroupTypePermissionInfo;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.EntityTypeFilter;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.NumericFilterPredicate;
import org.thingsboard.server.dao.sql.alarm.AlarmRepository;
import org.thingsboard.server.dao.sql.asset.AssetRepository;
import org.thingsboard.server.dao.sql.blob.BlobEntityRepository;
import org.thingsboard.server.dao.sql.citus.AbstractCitusContainerTest;
import org.thingsboard.server.dao.sql.customer.CustomerRepository;
import org.thingsboard.server.dao.sql.dashboard.DashboardRepository;
import org.thingsboard.server.dao.sql.device.DeviceRepository;
import org.thingsboard.server.dao.sql.edge.EdgeRepository;
import org.thingsboard.server.dao.sql.entityview.EntityViewRepository;
import org.thingsboard.server.dao.sql.group.EntityGroupRepository;
import org.thingsboard.server.dao.sql.query.DefaultEntityQueryRepository;
import org.thingsboard.server.dao.sql.query.DefaultEntityQueryRepository.EntityDataSql;
import org.thingsboard.server.dao.sql.query.DefaultQueryLogComponent;
import org.thingsboard.server.dao.sql.query.EntityKeyMapping;
import org.thingsboard.server.dao.sql.query.SqlQueryContext;
import org.thingsboard.server.dao.sql.report.ReportRepository;
import org.thingsboard.server.dao.sql.report.ReportTemplateInfoRepository;
import org.thingsboard.server.dao.sql.role.RoleRepository;
import org.thingsboard.server.dao.sql.scheduler.SchedulerEventRepository;
import org.thingsboard.server.dao.sql.user.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Coordinator-local anchor coverage for the Citus entity-data query path.
 * <p>
 * Not every entity table is distributed or replicated under Citus. Per {@code CitusTables} the partitioned
 * {@code blob_entity} / {@code report} tables stay plain coordinator-local Postgres tables (Citus rejects
 * {@code create_reference_table} on partitioned tables and nothing FKs into them), while the KV fact tables
 * {@code attribute_kv} / {@code ts_kv_latest} are hash-distributed by {@code entity_id} and the dimension tables
 * (e.g. {@code key_dictionary}, {@code device_profile}) are reference tables replicated to every worker.
 * <p>
 * This is the topology that motivates the pushdown eligibility guard in {@code DefaultEntityQueryRepository}: the
 * co-located latest-value pushdown join (anchor {@code id} joined straight to the distribution column of a distributed
 * KV table) is only legal when the anchor table itself exists on every worker. For a coordinator-local anchor
 * ({@code blob_entity} / {@code report}) that co-location does not exist, so such a query must keep the coordinator-side
 * CTE join form (the filtered anchor set is materialized and broadcast once as the {@code entities} CTE, and each shard
 * probes only that id set via {@code entity_id in (select id from entities)}).
 * <p>
 * The two CTE-form tests below drive the REAL generator, exactly like {@code CitusPushdownParityIntegrationTest}:
 * {@link DefaultEntityQueryRepository#buildEntityDataSql} assembles the SQL for a {@code blob_entity} / {@code report}
 * entity-type filter with {@link SqlQueryContext#setPushdownEligible}{@code (false)} — the eligibility the production
 * guard computes for these coordinator-local anchors — and the produced CTE-form SQL is executed against the live
 * cluster, asserting it runs WITHOUT a distributed-planner error and returns the CORRECT rows. Driving the generator
 * (rather than hand-transcribing its output) means these tests cannot silently drift from the SQL production runs.
 * <p>
 * The third test intentionally stays hand-written: it pins the planner behaviour of the raw pushdown-derived-table
 * shape over a coordinator-local anchor, a form the generator NEVER emits for these anchors (the guard exists
 * precisely to avoid it), so there is no generator arm to drive.
 */
class CitusCoordinatorLocalAnchorTest extends AbstractCitusContainerTest {

    private static final UUID TENANT_ID = UUID.randomUUID();

    // 3 blob entities and 3 reports under the tenant.
    private static final UUID BLOB1 = UUID.randomUUID();
    private static final UUID BLOB2 = UUID.randomUUID();
    private static final UUID BLOB3 = UUID.randomUUID();
    private static final UUID REPORT1 = UUID.randomUUID();
    private static final UUID REPORT2 = UUID.randomUUID();
    private static final UUID REPORT3 = UUID.randomUUID();

    // key_dictionary coordinates of the two latest-value keys under test. The generator resolves key names via a
    // "(select key_id from key_dictionary where key = :...)" subquery, so the dictionary is part of the fixture.
    private static final String TEMPERATURE_KEY = "temperature";
    private static final int TEMPERATURE_KEY_ID = 1;
    private static final String ACTIVE_KEY = "active";
    private static final int ACTIVE_KEY_ID = 5;

    // attribute_kv attribute_type id the generator hardcodes for SERVER_ATTRIBUTE (AttributeScope.SERVER_SCOPE).
    private static final int SERVER_SCOPE = 2;

    private static final int PAGE_SIZE = 10;

    private static NamedParameterJdbcTemplate namedJdbcTemplate;

    @BeforeAll
    static void setUpFixture() {
        // The three tests are read-only over this identical fixture, so it is built ONCE for the class (a per-method
        // rebuild would re-distribute the KV tables — two create_distributed_table re-shards — three times for
        // nothing). Everything created here is dropped in @AfterAll so no tables leak into the shared coordinator's
        // public schema that sibling Citus test classes read.
        jdbcTemplate.execute("DROP TABLE IF EXISTS attribute_kv CASCADE");
        jdbcTemplate.execute("DROP TABLE IF EXISTS ts_kv_latest CASCADE");
        jdbcTemplate.execute("DROP TABLE IF EXISTS blob_entity CASCADE");
        jdbcTemplate.execute("DROP TABLE IF EXISTS report CASCADE");
        jdbcTemplate.execute("DROP TABLE IF EXISTS key_dictionary CASCADE");

        // blob_entity / report: representative coordinator-local anchor tables. In production these are partitioned
        // and therefore stay plain coordinator-local Postgres tables (never distributed nor reference); a plain table
        // here carries the identical placement property under test (not present on the workers). Columns cover what
        // the generated entities query selects for these entity types (id + the name entity field).
        jdbcTemplate.execute("CREATE TABLE blob_entity (" +
                "id uuid PRIMARY KEY, tenant_id uuid, customer_id uuid, name varchar, type varchar)");
        jdbcTemplate.execute("CREATE TABLE report (" +
                "id uuid PRIMARY KEY, tenant_id uuid, customer_id uuid, name varchar, type varchar)");

        // The KV tables carry every column the generator's latest-value selections read (bool_v/str_v/long_v/dbl_v/
        // json_v plus the ts columns), mirroring CitusPushdownParityIntegrationTest's fixture.
        jdbcTemplate.execute("CREATE TABLE attribute_kv (entity_id uuid, attribute_type int, attribute_key int, " +
                "bool_v boolean, str_v varchar, long_v bigint, dbl_v double precision, json_v json, " +
                "last_update_ts bigint, version bigint, " +
                "CONSTRAINT attribute_kv_pkey PRIMARY KEY (entity_id, attribute_type, attribute_key))");
        jdbcTemplate.execute("CREATE TABLE ts_kv_latest (entity_id uuid, key int, ts bigint, " +
                "bool_v boolean, str_v varchar, long_v bigint, dbl_v double precision, json_v json, version bigint, " +
                "CONSTRAINT ts_kv_latest_pkey PRIMARY KEY (entity_id, key))");
        jdbcTemplate.execute("CREATE TABLE key_dictionary (key varchar(255) NOT NULL, key_id serial, " +
                "CONSTRAINT key_dictionary_id_pkey PRIMARY KEY (key_id), " +
                "CONSTRAINT key_dictionary_key_unq_key UNIQUE (key))");

        // Distribute (and co-locate) only the KV fact tables on entity_id; key_dictionary becomes a reference table
        // (the workers must evaluate the key-id subquery). blob_entity / report are left untouched, so they remain
        // coordinator-local — the anchor-side ⋈ distributed-KV joins below therefore genuinely exercise the
        // coordinator-local ⋈ distributed planning path (recursive planning / CTE broadcast), not a co-located pushdown.
        jdbcTemplate.execute("SELECT create_reference_table('key_dictionary')");
        jdbcTemplate.execute("SELECT create_distributed_table('attribute_kv','entity_id', shard_count => 8)");
        jdbcTemplate.execute("SELECT create_distributed_table('ts_kv_latest','entity_id', colocate_with => 'attribute_kv')");

        jdbcTemplate.update("INSERT INTO key_dictionary (key, key_id) VALUES (?, ?)", TEMPERATURE_KEY, TEMPERATURE_KEY_ID);
        jdbcTemplate.update("INSERT INTO key_dictionary (key, key_id) VALUES (?, ?)", ACTIVE_KEY, ACTIVE_KEY_ID);

        // Seed blob entities.
        jdbcTemplate.update("INSERT INTO blob_entity (id, tenant_id, name, type) VALUES (?, ?, ?, ?)",
                BLOB1, TENANT_ID, "Blob 1", "default");
        jdbcTemplate.update("INSERT INTO blob_entity (id, tenant_id, name, type) VALUES (?, ?, ?, ?)",
                BLOB2, TENANT_ID, "Blob 2", "default");
        jdbcTemplate.update("INSERT INTO blob_entity (id, tenant_id, name, type) VALUES (?, ?, ?, ?)",
                BLOB3, TENANT_ID, "Blob 3", "default");

        // Seed reports.
        jdbcTemplate.update("INSERT INTO report (id, tenant_id, name, type) VALUES (?, ?, ?, ?)",
                REPORT1, TENANT_ID, "Report 1", "default");
        jdbcTemplate.update("INSERT INTO report (id, tenant_id, name, type) VALUES (?, ?, ?, ?)",
                REPORT2, TENANT_ID, "Report 2", "default");
        jdbcTemplate.update("INSERT INTO report (id, tenant_id, name, type) VALUES (?, ?, ?, ?)",
                REPORT3, TENANT_ID, "Report 3", "default");

        // ts_kv_latest over blob entities: blob1 -> 25, blob2 -> 5 (blob3 has no latest).
        jdbcTemplate.update("INSERT INTO ts_kv_latest (entity_id, key, ts, long_v, version) VALUES (?, ?, ?, ?, ?)",
                BLOB1, TEMPERATURE_KEY_ID, 1000L, 25L, 1L);
        jdbcTemplate.update("INSERT INTO ts_kv_latest (entity_id, key, ts, long_v, version) VALUES (?, ?, ?, ?, ?)",
                BLOB2, TEMPERATURE_KEY_ID, 1000L, 5L, 1L);

        // attribute_kv over reports: report1 active=1, report3 active=0 (report2 has no attribute).
        jdbcTemplate.update("INSERT INTO attribute_kv (entity_id, attribute_type, attribute_key, long_v, last_update_ts, version) " +
                "VALUES (?, ?, ?, ?, ?, ?)", REPORT1, SERVER_SCOPE, ACTIVE_KEY_ID, 1L, 1000L, 1L);
        jdbcTemplate.update("INSERT INTO attribute_kv (entity_id, attribute_type, attribute_key, long_v, last_update_ts, version) " +
                "VALUES (?, ?, ?, ?, ?, ?)", REPORT3, SERVER_SCOPE, ACTIVE_KEY_ID, 0L, 1000L, 1L);

        namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate.getDataSource());
    }

    @AfterAll
    static void dropFixture() {
        if (jdbcTemplate != null) {
            jdbcTemplate.execute("DROP TABLE IF EXISTS attribute_kv CASCADE");
            jdbcTemplate.execute("DROP TABLE IF EXISTS ts_kv_latest CASCADE");
            jdbcTemplate.execute("DROP TABLE IF EXISTS blob_entity CASCADE");
            jdbcTemplate.execute("DROP TABLE IF EXISTS report CASCADE");
            jdbcTemplate.execute("DROP TABLE IF EXISTS key_dictionary CASCADE");
        }
    }

    @Test
    void coordinatorLocalBlobAnchorJoinedToLatestViaGeneratedCteExecutesAndReturnsCorrectRows() {
        // The real generator's CTE arm (the form the guard selects for a coordinator-local anchor) over a
        // blob_entity anchor with a ts_kv_latest latest-value read. All 3 blobs returned; only blob1 (25) and
        // blob2 (5) have a latest value, blob3 comes back with the empty rendered value (left-join miss).
        EntityDataQuery query = latestKeyQuery(EntityType.BLOB_ENTITY, EntityKeyType.TIME_SERIES, TEMPERATURE_KEY, null);

        GeneratedCteResult result = runGeneratedCteForm(EntityType.BLOB_ENTITY, query);

        assertThat(result.rows()).extracting(row -> (UUID) row.get("id"))
                .containsExactlyInAnyOrder(BLOB1, BLOB2, BLOB3);
        assertThat(result.latestValueOf(BLOB1)).isEqualTo("25");
        assertThat(result.latestValueOf(BLOB2)).isEqualTo("5");
        // the latest-value selection renders a missing (left-join miss) row as the empty concatenation
        assertThat(result.latestValueOf(BLOB3)).isEmpty();
    }

    @Test
    void coordinatorLocalReportAnchorJoinedToAttributeViaGeneratedCteExecutesAndReturnsCorrectRows() {
        // The generator's CTE arm over a coordinator-local report anchor with a SERVER_SCOPE attribute read and a
        // value key filter active = 1 (the filter makes the generator emit the INNER latest join). Only report1
        // qualifies.
        KeyFilter activeEqualsOne = new KeyFilter();
        EntityKey activeKey = new EntityKey(EntityKeyType.SERVER_ATTRIBUTE, ACTIVE_KEY);
        activeEqualsOne.setKey(activeKey);
        activeEqualsOne.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate predicate = new NumericFilterPredicate();
        predicate.setOperation(NumericFilterPredicate.NumericOperation.EQUAL);
        predicate.setValue(new FilterPredicateValue<>(1.0));
        activeEqualsOne.setPredicate(predicate);
        EntityDataQuery query = latestKeyQuery(EntityType.REPORT, EntityKeyType.SERVER_ATTRIBUTE, ACTIVE_KEY, activeEqualsOne);

        GeneratedCteResult result = runGeneratedCteForm(EntityType.REPORT, query);

        assertThat(result.rows()).extracting(row -> (UUID) row.get("id")).containsExactly(REPORT1);
        assertThat(result.latestValueOf(REPORT1)).isEqualTo("1");
    }

    @Test
    void coordinatorLocalBlobAnchorPushdownShapedJoinBehaviour() {
        // Raw pushdown-derived-table shape (anchor joined STRAIGHT to the distributed ts_kv_latest, no id-set
        // materialization) over a coordinator-local anchor. This is the shape the eligibility guard AVOIDS for
        // coordinator-local anchors precisely because the anchor is NOT co-located with ts_kv_latest — the generator
        // never emits it for these anchors, so it is pinned with hand-written SQL: it must not silently return wrong
        // rows. If a future Citus version rejects or misplans this shape, that failure is the evidence the guard is
        // load-bearing for correctness (flip this to an assertThatThrownBy); today it resolves via recursive planning
        // and returns the same rows as the CTE form.
        List<AnchorLatest> rows = jdbcTemplate.query(
                "SELECT r.id, r.long_v FROM (" +
                        "  SELECT entities.id AS id, tkv.long_v AS long_v " +
                        "  FROM (SELECT id, 1 AS ts_read FROM blob_entity WHERE tenant_id = ?) entities " +
                        "  LEFT JOIN ts_kv_latest tkv " +
                        "  ON tkv.entity_id = entities.id AND entities.ts_read = 1 AND tkv.key = ?" +
                        ") r",
                (rs, rowNum) -> {
                    long longV = rs.getLong("long_v");
                    Long value = rs.wasNull() ? null : longV;
                    return new AnchorLatest(UUID.fromString(rs.getString("id")), value);
                },
                TENANT_ID, TEMPERATURE_KEY_ID);

        assertThat(rows).extracting(AnchorLatest::id).containsExactlyInAnyOrder(BLOB1, BLOB2, BLOB3);
        assertThat(rows).filteredOn(r -> r.id().equals(BLOB1)).singleElement()
                .satisfies(r -> assertThat(r.value()).isEqualTo(25L));
        assertThat(rows).filteredOn(r -> r.id().equals(BLOB3)).singleElement()
                .satisfies(r -> assertThat(r.value()).isNull());
    }

    /**
     * Builds the entity-data SQL for {@code query} with the REAL generator, pinned to the CTE arm
     * ({@code pushdownEligible = false} — exactly what the production guard computes for a coordinator-local
     * {@code blob_entity} / {@code report} anchor), and executes it against the live cluster the way production
     * does ({@code :named} params bound from the {@link SqlQueryContext}).
     */
    private GeneratedCteResult runGeneratedCteForm(EntityType entityType, EntityDataQuery query) {
        DefaultEntityQueryRepository repo = newGenerator();
        QueryContext securityCtx = new QueryContext(
                TenantId.fromUUID(TENANT_ID), null, entityType,
                MergedUserPermissions.ALL, query.getEntityFilter(), true);
        SqlQueryContext ctx = new SqlQueryContext(securityCtx, true);
        ctx.setPushdownEligible(false);

        EntityDataSql sql = repo.buildEntityDataSql(ctx, query, permissiveReadPermissions());

        List<Map<String, Object>> rows = namedJdbcTemplate.queryForList(sql.dataQuery(), ctx);
        String latestValueAlias = sql.selectionMapping().stream()
                .filter(EntityKeyMapping::isLatest)
                .findFirst().orElseThrow()
                .getValueAlias();
        return new GeneratedCteResult(rows, latestValueAlias);
    }

    /**
     * An entity-type filter query over {@code entityType} selecting the {@code name} entity field plus
     * {@code latestKey}, sorted by the latest key DESC — the same query shape the parity test drives for the
     * distributed device anchor, here aimed at the coordinator-local anchors.
     */
    private EntityDataQuery latestKeyQuery(EntityType entityType, EntityKeyType latestKeyType, String key, KeyFilter valueFilter) {
        EntityTypeFilter filter = new EntityTypeFilter();
        filter.setEntityType(entityType);
        EntityKey latestKey = new EntityKey(latestKeyType, key);
        EntityDataSortOrder sortOrder = new EntityDataSortOrder(latestKey, EntityDataSortOrder.Direction.DESC);
        EntityDataPageLink pageLink = new EntityDataPageLink(PAGE_SIZE, 0, null, sortOrder);
        EntityKey nameField = new EntityKey(EntityKeyType.ENTITY_FIELD, "name");
        return new EntityDataQuery(filter, pageLink, List.of(nameField), List.of(latestKey),
                valueFilter == null ? null : List.of(valueFilter));
    }

    /**
     * The real {@link DefaultEntityQueryRepository} with mock collaborators — {@code buildEntityDataSql} never
     * touches them (it only assembles SQL strings from ctx/query/readPermissions). Same wiring as
     * {@code CitusPushdownParityIntegrationTest#newGenerator()}.
     */
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
        // generator emits the Citus arms (the CTE arm is then selected via setPushdownEligible(false)).
        ReflectionTestUtils.setField(repo, "citusEnabled", true);
        // nullsOrderStrategy is likewise a @Value field (default "default"); without it resolveNullsOrder() switches
        // over a null String and NPEs. Set the production default so the ORDER BY matches the real generator.
        ReflectionTestUtils.setField(repo, "nullsOrderStrategy", "default");
        // initSelectArms() runs in @PostConstruct (validateNullsOrderStrategy) once citusEnabled is known; invoke it
        // explicitly so the distributed select arms are populated for the generator.
        ReflectionTestUtils.invokeMethod(repo, "initSelectArms");
        return repo;
    }

    /** Non-restrictive read permissions (generic read, no group restrictions) -- the value buildCommonEntitiesQuery reads. */
    private MergedGroupTypePermissionInfo permissiveReadPermissions() {
        return MergedGroupTypePermissionInfo.MERGED_GROUP_TYPE_PERMISSION_INFO_EMPTY_GROUPS_HAS_GENERIC_READ_TRUE;
    }

    /** The generated data-query rows plus the generator-assigned value alias of the latest-key selection. */
    private record GeneratedCteResult(List<Map<String, Object>> rows, String latestValueAlias) {

        /** The rendered latest value of the (single) row with {@code id}, as the generator's varchar concatenation. */
        String latestValueOf(UUID id) {
            List<Map<String, Object>> matches = rows.stream().filter(row -> id.equals(row.get("id"))).toList();
            assertThat(matches).hasSize(1);
            return (String) matches.get(0).get(latestValueAlias);
        }
    }

    private record AnchorLatest(UUID id, Long value) {
    }
}
