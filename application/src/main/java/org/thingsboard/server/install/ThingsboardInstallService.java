// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.install;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.sql.citus.CitusSchemaService;
import org.thingsboard.server.dao.subscription.EntityCapExceededException;
import org.thingsboard.server.service.component.ComponentDiscoveryService;
import org.thingsboard.server.service.install.LicenseCapacityUpgradePreflight;
import org.thingsboard.server.service.install.DatabaseEntitiesUpgradeService;
import org.thingsboard.server.service.install.DatabaseSchemaSettingsService;
import org.thingsboard.server.service.install.EntityDatabaseSchemaService;
import org.thingsboard.server.service.install.InstallScripts;
import org.thingsboard.server.service.install.NoSqlKeyspaceService;
import org.thingsboard.server.service.install.SystemDataLoaderService;
import org.thingsboard.server.service.install.TsDatabaseSchemaService;
import org.thingsboard.server.service.install.TsLatestDatabaseSchemaService;
import org.thingsboard.server.service.install.migrate.TsLatestMigrateService;
import org.thingsboard.server.service.install.update.CacheCleanupService;
import org.thingsboard.server.service.install.update.DataUpdateService;

@Service
@Profile("install")
@Slf4j
public class ThingsboardInstallService {

    @Value("${install.upgrade:false}")
    private Boolean isUpgrade;

    @Value("${install.upgrade.from_version:}")
    private String upgradeFromVersion;

    @Value("${state.persistToTelemetry:false}")
    private boolean persistToTelemetry;

    @Autowired
    private EntityDatabaseSchemaService entityDatabaseSchemaService;

    @Autowired(required = false)
    private NoSqlKeyspaceService noSqlKeyspaceService;

    @Autowired
    private TsDatabaseSchemaService tsDatabaseSchemaService;

    @Autowired(required = false)
    private TsLatestDatabaseSchemaService tsLatestDatabaseSchemaService;

    @Autowired
    private DatabaseEntitiesUpgradeService databaseEntitiesUpgradeService;

    @Autowired
    private ComponentDiscoveryService componentDiscoveryService;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private SystemDataLoaderService systemDataLoaderService;

    @Autowired
    private DataUpdateService dataUpdateService;

    @Autowired
    private CacheCleanupService cacheCleanupService;

    @Autowired(required = false)
    private TsLatestMigrateService latestMigrateService;

    @Autowired
    private InstallScripts installScripts;

    @Autowired
    private DatabaseSchemaSettingsService databaseSchemaVersionService;

    @Autowired(required = false)
    private CitusSchemaService citusSchemaService;

    @Autowired
    private LicenseCapacityUpgradePreflight licenseCapacityUpgradePreflight;

    public void performInstall() {
        try {
            if (isUpgrade) {
                if ("cassandra-latest-to-postgres".equals(upgradeFromVersion)) {
                    log.info("Migrating ThingsBoard latest timeseries data from cassandra to SQL database ...");
                    latestMigrateService.migrate();
                } else if ("postgres-to-citus".equals(upgradeFromVersion)) {
                    if (citusSchemaService == null) {
                        throw new IllegalStateException("postgres-to-citus conversion requires DATABASE_CITUS_ENABLED=true");
                    }
                    // Fail fast and legibly if the database schema is not at the package version: the conversion
                    // assumes the current schema and would otherwise die deep inside the distribution DDL with a raw
                    // SQL error about a missing column or table. Equality is required (validateSchemaSettings would
                    // reject it): the regular upgrade must have already brought the database to the package version.
                    String dbSchemaVersion = databaseSchemaVersionService.getDbSchemaVersion();
                    String packageSchemaVersion = databaseSchemaVersionService.getPackageSchemaVersion();
                    if (!packageSchemaVersion.equals(dbSchemaVersion)) {
                        throw new IllegalStateException("postgres-to-citus conversion requires the database schema version to match the package version: " +
                                "database is at " + dbSchemaVersion + ", package is " + packageSchemaVersion + ". Upgrade to " + packageSchemaVersion + " first.");
                    }
                    log.info("Starting in-place PostgreSQL -> Citus conversion (full downtime) ...");
                    long citusConversionStart = System.currentTimeMillis();
                    citusSchemaService.applyDistribution();
                    log.info("PostgreSQL -> Citus conversion finished in {} ms", System.currentTimeMillis() - citusConversionStart);
                    // Re-create views so they pick up the Citus-safe definitions now that attribute_kv/ts_kv_latest are
                    // distributed (e.g. integration_info must not run a correlated subquery into the distributed attribute_kv).
                    entityDatabaseSchemaService.createOrUpdateViewsAndFunctions();
                    entityDatabaseSchemaService.createOrUpdateDeviceInfoView(persistToTelemetry);
                } else {
                    // TODO DON'T FORGET to update SUPPORTED_VERSIONS_FOR_UPGRADE and MIN_CE_VERSION_FOR_UPGRADE in DefaultDatabaseSchemaSettingsService
                    databaseSchemaVersionService.validateSchemaSettings();
                    // The database's own product marker selects the branch, so a CE database can never be taken
                    // down the PE path by a missing flag. --fromVersion=CE is still accepted, and now a no-op.
                    var updateFromCE = databaseSchemaVersionService.isUpgradeFromCe();
                    // Before anything is written, so an upgrade the resulting license would not cover is
                    // refused now rather than on an already converted database.
                    licenseCapacityUpgradePreflight.check();
                    if (updateFromCE) {
                        log.info("Upgrading ThingsBoard from version CE to PE ...");
                    } else {
                        String fromVersion = databaseSchemaVersionService.getDbSchemaVersion();
                        String toVersion = databaseSchemaVersionService.getPackageSchemaVersion();
                        log.info("Upgrading ThingsBoard from version {} to {} ...", fromVersion, toVersion);
                    }
                    cacheCleanupService.clearCache();
                    if (updateFromCE) {
                        // The LTS chain references PE-only tables and runs each of its files as a single transaction,
                        // so a converted CE database must get those tables before the chain rather than after it.
                        entityDatabaseSchemaService.createDatabaseSchema(false);
                    }
                    // Apply the schema_update.sql script. The script may include DDL statements to change structure
                    // of *existing* tables and DML statements to manipulate the DB records.
                    databaseEntitiesUpgradeService.upgradeDatabase(updateFromCE);
                    // All new tables that do not have any data will be automatically created here.
                    entityDatabaseSchemaService.createDatabaseSchema(false);
                    // Re-create all views, functions.
                    entityDatabaseSchemaService.createOrUpdateViewsAndFunctions();
                    entityDatabaseSchemaService.createOrUpdateDeviceInfoView(persistToTelemetry);
                    // Creates missing indexes.
                    entityDatabaseSchemaService.createDatabaseIndexes();
                    // Not per-release cleanup: the only path that creates default notifications missing from an
                    // upgraded database (PE-only ones for CE -> PE, license ones for PE 4.3 -> 4.4). Only CE tenants
                    // can lack the tenant-level ones.
                    systemDataLoaderService.updateDefaultNotificationConfigs(updateFromCE);

                    // TODO: cleanup update code after each release

                    // Runs upgrade scripts that are not possible in plain SQL.
                    dataUpdateService.updateData(updateFromCE);
                    log.info("Updating system data...");
                    dataUpdateService.upgradeRuleNodes();
                    systemDataLoaderService.loadSystemWidgets();
                    installScripts.loadSystemLwm2mResources();
                    installScripts.loadSystemImagesAndResources();
                    systemDataLoaderService.createDefaultCustomMenu();
                    installScripts.updateSystemNotificationTemplates();
                    // Must run before the schema version is stamped: a failure after stamping would leave the
                    // database marked as upgraded and a re-run refused.
                    entityDatabaseSchemaService.generateClusterIdIfNotExist(); //Need for offline build
                    databaseSchemaVersionService.updateSchemaVersion();

                    // is needed as separate of dataUpdateService.updateData because needs to process some data made by systemDataLoaderService.loadSystemWidgets
                    dataUpdateService.postUpdateData();
                }
                log.info("Upgrade finished successfully!");

            } else {

                log.info("Starting ThingsBoard Installation...");

                log.info("Installing DataBase schema for entities...");

                entityDatabaseSchemaService.createDatabaseSchema();
                if (citusSchemaService != null) {
                    log.info("Citus enabled: distributing KV tables and creating reference tables...");
                    citusSchemaService.applyDistribution();
                }
                databaseSchemaVersionService.createSchemaSettings();

                entityDatabaseSchemaService.createOrUpdateViewsAndFunctions();
                entityDatabaseSchemaService.createOrUpdateDeviceInfoView(persistToTelemetry);

                log.info("Installing DataBase schema for timeseries...");

                if (noSqlKeyspaceService != null) {
                    noSqlKeyspaceService.createDatabaseSchema();
                }

                tsDatabaseSchemaService.createDatabaseSchema();

                if (tsLatestDatabaseSchemaService != null) {
                    tsLatestDatabaseSchemaService.createDatabaseSchema();
                }

                log.info("Loading system data...");

                componentDiscoveryService.discoverComponents();

                systemDataLoaderService.createDefaultTenantProfiles();
                systemDataLoaderService.createAdminSettings();
                systemDataLoaderService.createRandomJwtSettings();
                systemDataLoaderService.loadSystemWidgets();
                systemDataLoaderService.createOAuth2Templates();
                systemDataLoaderService.createQueues();
                systemDataLoaderService.createDefaultNotificationConfigs();
                installScripts.updateSystemNotificationTemplates();
                systemDataLoaderService.createDefaultCustomMenu();

//                systemDataLoaderService.loadSystemPlugins();
//                systemDataLoaderService.loadSystemRules();
                installScripts.loadSystemLwm2mResources();
                installScripts.loadSystemImagesAndResources();
                installScripts.generateSysAdminEncryptionKey();

                entityDatabaseSchemaService.generateClusterIdIfNotExist(); //Need for offline build

                log.info("Installation finished successfully!");
            }
        } catch (EntityCapExceededException e) {
            // A deliberate pre-flight refusal: report the plain policy message, not an unexpected failure.
            throw new ThingsboardInstallException(e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during ThingsBoard installation!", e);
            throw new ThingsboardInstallException("Unexpected error during ThingsBoard installation!", e);
        } finally {
            SpringApplication.exit(context);
        }
    }

}
