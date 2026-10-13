// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.install;

import com.google.common.util.concurrent.Futures;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.agent.AgentProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.service.component.ComponentDiscoveryService;
import org.thingsboard.server.service.install.lts.LtsMigrationService;
import org.thingsboard.server.service.install.update.DefaultDataUpdateService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Two parts of the offline upgrade driven against a real database: the CE-to-PE data conversion
 * ({@code updateData(true)}) replayed the way a crashed conversion replays it, and a PE database stamped at a
 * pre-4.4 version taken to the package version by the real LTS chain ({@code upgradeDatabase(false)}), from the
 * position the chain now runs from for both sources.
 * <p>
 * What is not here is the CE schema fork, {@code upgradeDatabase(true)}. The fixture below builds CE-shaped
 * <i>data</i> on a PE schema; nothing in this class constructs a CE-shaped schema or runs the CE-to-PE schema
 * delta, which would need a real CE dump. So this class says nothing about whether a schema change is safe for
 * a CE source - the narrow coverage of that delta is in {@link PeUpgradeClusterIdentityIntegrationTest}.
 * <p>
 * The container of this class's own is not incidental. {@code updateDataFromCe()} walks every tenant in the
 * database and creates the Group-All groups a CE install has none of, so against the shared {@code DaoSqlTest}
 * database it would hand groups to tenants other test classes left behind, and the row counts below would be
 * measuring those leftovers as much as this test's own fixture.
 */
@TestPropertySource(
        // @DaoSqlTest's locations, plus a database name of this class's own: the testcontainers JDBC driver keys
        // its containers on the URL, so a different name starts a second container instead of joining the suite's.
        // Everything but that name is copied from sql-test.properties - the image tag lives in both files and has
        // to be bumped in lockstep, or this test keeps replaying the upgrade chain on a stale engine.
        locations = {"classpath:application-test.properties", "classpath:sql-test.properties"},
        properties = "spring.datasource.url=jdbc:tc:postgresql:18:///peupgradechain?TC_DAEMON=true&TC_TMPFS=/testtmpfs:rw&?TC_INITFUNCTION=org.thingsboard.server.dao.PostgreSqlInitializer::initDb")
public class PeUpgradeChainIntegrationTest extends AbstractControllerTest {

    private static final String PACKAGE_VERSION = "4.4.0.0";
    private static final List<String> COUNTED_TABLES = List.of(
            "entity_group", "relation", "tenant", "customer", "tb_user", "device", "asset", "dashboard");

    // Columns added by the LTS files the chain selects. report is the PE-only table the whole ordering fix exists
    // for, so its two 4.4 columns are in the set even though report_info_view's SELECT r.* captures them.
    private static final List<Column> V_4_3_1_2_COLUMNS = List.of(
            new Column("calculated_field", "additional_info", "varchar"),
            new Column("role", "excluded_permissions", "varchar(1000000)"));
    private static final List<Column> V_4_4_0_0_COLUMNS = List.of(
            new Column("rule_chain", "notes", "varchar(1000000)"),
            new Column("job", "customer_id", "uuid"),
            new Column("report", "public_key", "varchar(32)"),
            new Column("report", "is_public", "boolean DEFAULT false"));

    private record Column(String table, String name, String type) {
        @Override
        public String toString() {
            return table + "." + name;
        }
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private InstallScripts installScripts;
    @Autowired
    private LtsMigrationService ltsMigrationService;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private TenantService tenantService;
    @Autowired
    private RelationService relationService;
    @Autowired
    private RuleChainService ruleChainService;
    @Autowired
    private IntegrationService integrationService;
    @Autowired
    private EntityGroupService entityGroupService;
    @Autowired
    private UserService userService;
    @Autowired
    private WhiteLabelingService whiteLabelingService;
    @Autowired
    private CustomerService customerService;
    @Autowired
    private AssetService assetService;
    @Autowired
    private DeviceService deviceService;
    @Autowired
    private DashboardService dashboardService;
    @Autowired
    private EntityViewService entityViewService;
    @Autowired
    private EdgeService edgeService;

    /** Held rather than created inline, so the backfill this test drives can be verified against it. */
    private final AgentProfileService agentProfileService = mock(AgentProfileService.class);

    private TenantId convertedTenantId;

    @After
    public void deleteConvertedTenant() {
        if (convertedTenantId != null) {
            tenantService.deleteTenant(convertedTenantId);
            convertedTenantId = null;
        }
    }

    /**
     * The {@code product} marker flips only at the final {@code updateSchemaVersion()}, so a conversion that dies
     * anywhere after the delta leaves a database that already carries the PE schema still stamped {@code 'CE'} -
     * and the next run, now that the branch is chosen by that marker rather than by an operator flag, re-enters
     * the conversion unattended and replays {@code updateDataFromCe()} over its own output.
     */
    @Test
    public void theCeToPeDataConversionIsIdempotentWhenAnInterruptedUpgradeIsRerun() throws Exception {
        givenATenantInTheCeShape();

        DefaultDataUpdateService dataUpdateService = ceDataUpdateService();
        dataUpdateService.updateData(true);

        List<UUID> groupsAfterFirstRun = entityGroupIdsOf(convertedTenantId);
        Map<String, Long> countsAfterFirstRun = rowCounts();
        // The conversion is what puts the Group-All groups on a CE tenant that had none: without them the
        // idempotence assertion below would be vacuously true.
        assertThat(groupsAfterFirstRun).as("the conversion must create the Group-All groups").isNotEmpty();

        dataUpdateService.updateData(true);

        // The same groups, not merely the same number of them: a second set would have had to replace the first.
        assertThat(entityGroupIdsOf(convertedTenantId)).containsExactlyInAnyOrderElementsOf(groupsAfterFirstRun);
        assertThat(rowCounts()).isEqualTo(countsAfterFirstRun);

        // The per-tenant backfill now runs outside the CE branch, so this is the only place that pins it
        // reaching a converted tenant at all.
        verify(agentProfileService, atLeastOnce()).findOrCreateAgentProfile(eq(convertedTenantId), any());
    }

    /**
     * A PE database at the oldest supported source version, taken to the package version by the real chain from
     * the position it now runs from - after the CE-to-PE fork rather than inside the {@code else} of it. The
     * range is (4.3.0.0, 4.4.0.0], so both the 4.3.1.x files and the 4.4 baseline run.
     */
    @Test
    public void aPe430DatabaseReachesThe44SchemaThroughTheRealChain() throws Exception {
        List<Column> expected = new ArrayList<>(V_4_3_1_2_COLUMNS);
        expected.addAll(V_4_4_0_0_COLUMNS);
        assertRealChainRestores(expected, "4.3.0.0");
    }

    /**
     * The newest supported source, where the range collapses to {@code {4.4.0.0}} - the range a converted CE
     * 4.3.1.4 database selects too. Only the 4.4 baseline runs: the half-open range must not replay 4.3.1.2 on a
     * source that has already passed it, so its columns are deliberately not in the expected set.
     */
    @Test
    public void aPe4314DatabaseReachesThe44SchemaThroughTheRealChain() throws Exception {
        assertRealChainRestores(V_4_4_0_0_COLUMNS, "4.3.1.4");
    }

    private void assertRealChainRestores(List<Column> columns, String fromVersion) throws Exception {
        try {
            // Reconstruct the pre-upgrade PE schema: drop what the files in range add on top of it. CASCADE
            // because report_info_view is SELECT r.*, so it holds report's columns; the real upgrade replays the
            // views for the same reason, right after the schema phase.
            columns.forEach(column -> {
                jdbcTemplate.execute("ALTER TABLE " + column.table() + " DROP COLUMN IF EXISTS " + column.name() + " CASCADE");
                assertThat(columnExists(column.table(), column.name())).as("%s dropped", column).isFalse();
            });

            upgradeService(fromVersion).upgradeDatabase(false);
            SchemaViews.recreate(jdbcTemplate);

            columns.forEach(column -> assertThat(columnExists(column.table(), column.name()))
                    .as("%s must be restored by the chain from %s", column, fromVersion).isTrue());
            assertThat(viewExists("report_info_view"))
                    .as("the CASCADE takes report_info_view with report's columns, so the replay must bring it back")
                    .isTrue();

            // Every statement in the chain is guarded, so the whole upgrade replays over its own output - which is
            // what a re-run after a crash between the chain and updateSchemaVersion() does.
            upgradeService(fromVersion).upgradeDatabase(false);
            SchemaViews.recreate(jdbcTemplate);
            columns.forEach(column -> assertThat(columnExists(column.table(), column.name()))
                    .as("%s after a replay", column).isTrue());
        } finally {
            // Unconditional: a chain that threw before its ALTERs ran would otherwise leave the columns - and the
            // views the CASCADE took with them - missing, and break every later test in this class.
            columns.forEach(column -> jdbcTemplate.execute(
                    "ALTER TABLE " + column.table() + " ADD COLUMN IF NOT EXISTS " + column.name() + " " + column.type()));
            SchemaViews.recreate(jdbcTemplate);
        }
    }

    /**
     * A tenant with entities and no entity groups at all - the shape a CE database arrives in, and one the PE
     * services never produce on their own.
     */
    private void givenATenantInTheCeShape() {
        Tenant tenant = new Tenant();
        tenant.setTitle("CE conversion tenant");
        tenant.setEmail("ce-conversion@thingsboard.org");
        convertedTenantId = tenantService.saveTenant(tenant).getId();

        Customer customer = new Customer();
        customer.setTenantId(convertedTenantId);
        customer.setTitle("CE customer");
        CustomerId customerId = customerService.saveCustomer(customer).getId();

        User tenantAdmin = new User();
        tenantAdmin.setTenantId(convertedTenantId);
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setEmail("ce-conversion-admin@thingsboard.org");
        userService.saveUser(convertedTenantId, tenantAdmin);

        Device device = new Device();
        device.setTenantId(convertedTenantId);
        device.setName(StringUtils.randomAlphabetic(10));
        device.setType("default");
        deviceService.saveDevice(device);

        Asset asset = new Asset();
        asset.setTenantId(convertedTenantId);
        asset.setName(StringUtils.randomAlphabetic(10));
        asset.setType("default");
        assetService.saveAsset(asset);

        Dashboard dashboard = new Dashboard();
        dashboard.setTenantId(convertedTenantId);
        dashboard.setTitle("CE dashboard");
        dashboardService.saveDashboard(dashboard);

        // Strip the groups the PE services created along the way: a CE database has none, and building them is
        // what the conversion is for. Through the service, so its caches follow the rows.
        entityGroupService.deleteAllEntityGroups(convertedTenantId, customerId);
        entityGroupService.deleteAllEntityGroups(convertedTenantId, convertedTenantId);
        assertThat(entityGroupIdsOf(convertedTenantId)).isEmpty();
    }

    /**
     * The real conversion, wired to the real DAO services. Only the collaborators outside it are stood in for:
     * the two install-profile beans a test context does not have, and the LTS data phase, which
     * {@link org.thingsboard.server.service.install.lts.LtsMigrationIntegrationTest} owns.
     */
    private DefaultDataUpdateService ceDataUpdateService() {
        return new DefaultDataUpdateService(tenantService, relationService, ruleChainService, integrationService,
                entityGroupService, userService, whiteLabelingService, customerService, assetService, deviceService,
                dashboardService, entityViewService, edgeService, agentProfileService,
                mock(SystemDataLoaderService.class), mock(ComponentDiscoveryService.class),
                inlineUpgradeExecutor(), schemaSettingsStubbedAt("4.3.1.4"),
                mock(LtsMigrationService.class));
    }

    /**
     * {@code submit} is a default method on {@link org.thingsboard.common.util.ListeningExecutor}, so a bare mock
     * answers it with {@code null} and the tenant backfill hands those nulls to {@code Futures.allAsList}. Running
     * the task on the calling thread also keeps the backfill ordered against the assertions that follow it.
     */
    private DbUpgradeExecutorService inlineUpgradeExecutor() {
        DbUpgradeExecutorService executor = mock(DbUpgradeExecutorService.class);
        when(executor.submit(any(Runnable.class))).thenAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return Futures.immediateVoidFuture();
        });
        return executor;
    }

    /** The real schema upgrade, with only the version pair it is driven over stubbed. */
    private SqlDatabaseUpgradeService upgradeService(String fromVersion) {
        return new SqlDatabaseUpgradeService(installScripts, jdbcTemplate, transactionManager,
                schemaSettingsStubbedAt(fromVersion), ltsMigrationService);
    }

    private DatabaseSchemaSettingsService schemaSettingsStubbedAt(String dbVersion) {
        DatabaseSchemaSettingsService schemaSettingsService = mock(DatabaseSchemaSettingsService.class);
        when(schemaSettingsService.getDbSchemaVersion()).thenReturn(dbVersion);
        when(schemaSettingsService.getPackageSchemaVersion()).thenReturn(PACKAGE_VERSION);
        return schemaSettingsService;
    }

    private List<UUID> entityGroupIdsOf(TenantId tenantId) {
        return jdbcTemplate.queryForList("SELECT id FROM entity_group WHERE owner_id = ?", UUID.class, tenantId.getId());
    }

    private Map<String, Long> rowCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String table : COUNTED_TABLES) {
            counts.put(table, jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Long.class));
        }
        return counts;
    }

    private boolean viewExists(String view) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM information_schema.views WHERE table_name = ?)", Boolean.class, view));
    }

    private boolean columnExists(String table, String column) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = ? AND column_name = ?)",
                Boolean.class, table, column));
    }

}
