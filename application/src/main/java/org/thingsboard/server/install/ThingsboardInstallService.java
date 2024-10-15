/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
import org.thingsboard.server.service.install.EntityDatabaseSchemaService;
import org.thingsboard.server.service.install.InstallScripts;
import org.thingsboard.server.service.install.NoSqlKeyspaceService;
import org.thingsboard.server.service.install.SystemDataLoaderService;
import org.thingsboard.server.service.install.TsDatabaseSchemaService;
import org.thingsboard.server.service.install.TsLatestDatabaseSchemaService;
import org.thingsboard.server.service.install.migrate.TsLatestMigrateService;
import org.thingsboard.server.service.install.update.CacheCleanupService;
import org.thingsboard.server.service.install.update.DataUpdateService;

import static org.thingsboard.server.service.install.update.DefaultDataUpdateService.getEnv;

@Service
@Profile("install")
@Slf4j
public class ThingsboardInstallService {

    @Value("${install.upgrade:false}")
    private Boolean isUpgrade;

    @Value("${install.upgrade.from_version:1.2.3}")
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

    public void performInstall() {
        try {
            if (isUpgrade) {
                log.info("Starting ThingsBoard Upgrade from version {} ...", upgradeFromVersion);

                cacheCleanupService.clearCache(upgradeFromVersion);

                if ("cassandra-latest-to-postgres".equals(upgradeFromVersion)) {
                    log.info("Migrating ThingsBoard latest timeseries data from cassandra to SQL database ...");
                    latestMigrateService.migrate();
                } else if (upgradeFromVersion.equals("3.6.2-images")) {
                    installScripts.updateImages();
                } else {
                    switch (upgradeFromVersion) {
                        case "3.5.0":
                            log.info("Upgrading ThingsBoard from version 3.5.0 to 3.5.1 ...");
                            databaseEntitiesUpgradeService.upgradeDatabase("3.5.0");
                        case "3.5.1":
                            log.info("Upgrading ThingsBoard from version 3.5.1 to 3.6.0 ...");
                            databaseEntitiesUpgradeService.upgradeDatabase("3.5.1");
                            dataUpdateService.updateData("3.5.1");
                            systemDataLoaderService.updateDefaultNotificationConfigs(true);
                        case "3.6.0":
                            log.info("Upgrading ThingsBoard from version 3.6.0 to 3.6.1 ...");
                            databaseEntitiesUpgradeService.upgradeDatabase("3.6.0");
                            dataUpdateService.updateData("3.6.0");
                        case "3.6.1":
                            log.info("Upgrading ThingsBoard from version 3.6.1 to 3.6.2 ...");
                            databaseEntitiesUpgradeService.upgradeDatabase("3.6.1");
                            if (!getEnv("SKIP_IMAGES_MIGRATION", false)) {
                                installScripts.setUpdateImages(true);
                            } else {
                                log.info("Skipping images migration. Run the upgrade with fromVersion as '3.6.2-images' to migrate");
                            }
                        case "3.6.2":
                            log.info("Upgrading ThingsBoard from version 3.6.2 to 3.6.3 ...");
                            databaseEntitiesUpgradeService.upgradeDatabase("3.6.2");
                            systemDataLoaderService.updateDefaultNotificationConfigs(true);
                        case "3.6.3":
                            log.info("Upgrading ThingsBoard from version 3.6.3 to 3.6.4 ...");
                            databaseEntitiesUpgradeService.upgradeDatabase("3.6.3");
                        case "3.6.4":
                            log.info("Upgrading ThingsBoard from version 3.6.4 to 3.7.0 ...");
                            databaseEntitiesUpgradeService.upgradeDatabase("3.6.4");
                            dataUpdateService.updateData("3.6.4");
                            entityDatabaseSchemaService.createCustomerTitleUniqueConstraintIfNotExists();
                            systemDataLoaderService.updateDefaultNotificationConfigs(false);
                            systemDataLoaderService.updateSecuritySettings();
                        case "3.7.0":
                            log.info("Upgrading ThingsBoard from version 3.7.0 to 3.8.0 ...");
                            databaseEntitiesUpgradeService.upgradeDatabase("3.7.0");
                        case "3.8.0":
                            log.info("Upgrading ThingsBoard from version 3.8.0 to 3.8.1 ...");
                            //TODO DON'T FORGET to update switch statement in the CacheCleanupService if you need to clear the cache
                            break;
                        default:
                            throw new RuntimeException("Unable to upgrade ThingsBoard, unsupported fromVersion: " + upgradeFromVersion);
                    }
                    entityDatabaseSchemaService.createOrUpdateViewsAndFunctions();
                    entityDatabaseSchemaService.createOrUpdateDeviceInfoView(persistToTelemetry);
                    log.info("Updating system data...");
                    dataUpdateService.upgradeRuleNodes();
                    systemDataLoaderService.loadSystemWidgets();
                    installScripts.loadSystemLwm2mResources();
                    installScripts.loadSystemImages();
                    if (installScripts.isUpdateImages()) {
                        installScripts.updateImages();
                    }
                }
                log.info("Upgrade finished successfully!");

            } else {

                log.info("Starting ThingsBoard Installation...");

                log.info("Installing DataBase schema for entities...");

                entityDatabaseSchemaService.createDatabaseSchema();

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
                installScripts.loadSystemImages();

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

