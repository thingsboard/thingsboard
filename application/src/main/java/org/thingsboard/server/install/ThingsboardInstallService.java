/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.install;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.service.component.ComponentDiscoveryService;
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

    @Value("${install.load_demo:false}")
    private Boolean loadDemo;

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

    public void performInstall() {
        try {
            if (isUpgrade) {
                if ("cassandra-latest-to-postgres".equals(upgradeFromVersion)) {
                    log.info("Migrating ThingsBoard latest timeseries data from cassandra to SQL database ...");
                    latestMigrateService.migrate();
                } else {
                    // TODO DON'T FORGET to update SUPPORTED_VERSIONS_FROM in DefaultDatabaseSchemaSettingsService
                    databaseSchemaVersionService.validateSchemaSettings();
                    String fromVersion = databaseSchemaVersionService.getDbSchemaVersion();
                    String toVersion = databaseSchemaVersionService.getPackageSchemaVersion();
                    log.info("Upgrading ThingsBoard from version {} to {} ...", fromVersion, toVersion);
                    cacheCleanupService.clearCache();
                    // Apply the schema_update.sql script. The script may include DDL statements to change structure
                    // of *existing* tables and DML statements to manipulate the DB records.
                    databaseEntitiesUpgradeService.upgradeDatabase();
                    // All new tables that do not have any data will be automatically created here.
                    entityDatabaseSchemaService.createDatabaseSchema(false);
                    // Re-create all views, functions.
                    entityDatabaseSchemaService.createOrUpdateViewsAndFunctions();
                    entityDatabaseSchemaService.createOrUpdateDeviceInfoView(persistToTelemetry);
                    // Creates missing indexes.
                    entityDatabaseSchemaService.createDatabaseIndexes();

                    // TODO: cleanup update code after each release
                    systemDataLoaderService.updateDefaultNotificationConfigs(false);

                    // Runs upgrade scripts that are not possible in plain SQL.
                    dataUpdateService.updateData();
                    log.info("Updating system data...");
                    dataUpdateService.upgradeRuleNodes();
                    systemDataLoaderService.loadSystemWidgets();
                    installScripts.loadSystemLwm2mResources();
                    installScripts.loadSystemImagesAndResources();
                    databaseSchemaVersionService.updateSchemaVersion();
                }
                log.info("Upgrade finished successfully!");

            } else {

                log.info("Starting ThingsBoard Installation...");

                log.info("Installing DataBase schema for entities...");

                entityDatabaseSchemaService.createDatabaseSchema();
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

                systemDataLoaderService.createSysAdmin();
                systemDataLoaderService.createDefaultTenantProfiles();
                systemDataLoaderService.createAdminSettings();
                systemDataLoaderService.createRandomJwtSettings();
                systemDataLoaderService.loadSystemWidgets();
                systemDataLoaderService.createOAuth2Templates();
                systemDataLoaderService.createQueues();
                systemDataLoaderService.createDefaultNotificationConfigs();

//                systemDataLoaderService.loadSystemPlugins();
//                systemDataLoaderService.loadSystemRules();
                installScripts.loadSystemLwm2mResources();
                installScripts.loadSystemImagesAndResources();

                if (loadDemo) {
                    log.info("Loading demo data...");
                    systemDataLoaderService.loadDemoData();
                }
                log.info("Installation finished successfully!");
            }
        } catch (Exception e) {
            log.error("Unexpected error during ThingsBoard installation!", e);
            throw new ThingsboardInstallException("Unexpected error during ThingsBoard installation!", e);
        } finally {
            SpringApplication.exit(context);
        }
    }

}
