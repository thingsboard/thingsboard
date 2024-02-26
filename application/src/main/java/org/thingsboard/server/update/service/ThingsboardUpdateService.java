/*
Author Ahmet Ertuğrul KAYA
*/
package org.thingsboard.server.update.service;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.service.install.*;
import org.thingsboard.server.service.install.update.CacheCleanupService;
import org.thingsboard.server.service.install.update.DataUpdateService;
import org.thingsboard.server.update.exception.ThingsboardUpdateException;

import java.util.List;

import static org.thingsboard.server.update.service.DefaultDataUpdateService.getEnv;

@Service
@Profile("update")
@Slf4j
public class ThingsboardUpdateService {

    private final static String INSTALL_UPGRADE_ENV_NAME = "install.upgrade";

    @Value("${" + INSTALL_UPGRADE_ENV_NAME + ":false}")
    private Boolean isUpgrade;

    @Value("${install.upgrade.from_version:3.2.1}")
    private String upgradeFromVersion;

    @Value("${state.persistToTelemetry:false}")
    private boolean persistToTelemetry;

    @Autowired
    private EntityDatabaseSchemaService entityDatabaseSchemaService;

    @Autowired
    private DatabaseEntitiesUpgradeService databaseEntitiesUpgradeService;

    @Autowired(required = false)
    private DatabaseTsUpgradeService databaseTsUpgradeService;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private SystemDataLoaderService systemDataLoaderService;

    @Autowired
    private DataUpdateService dataUpdateService;

    @Autowired
    private CacheCleanupService cacheCleanupService;

    @Autowired
    private InstallScripts installScripts;

    public void performInstall() {
        try {
            if (!isUpgrade) {
                Throwable e = new Throwable("Value of " + INSTALL_UPGRADE_ENV_NAME + " is " + isUpgrade.toString());
                throw new ThingsboardUpdateException("Value of " + INSTALL_UPGRADE_ENV_NAME + " is not set to true", e);
            }

            log.info("Starting ThingsBoard Upgrade from version {} ...", upgradeFromVersion);

            cacheCleanupService.clearCache(upgradeFromVersion);

            List<String> versions = Lists.newArrayList("3.2.1", "3.2.2", "3.3.2", "3.3.3", "3.3.4", "3.4.0", "3.4.1", "3.4.4", "3.5.0", "3.5.1", "3.6.0", "3.6.1", "3.6.2");

            String currentVersion = versions.get(0); //TODO: bunu db'den oku

            if (!versions.contains(currentVersion)) {
                Throwable e = new Throwable("Supported versions are " + versions);
                throw new ThingsboardUpdateException("Current version is " + currentVersion + " and not supported", e);
            }

            for (String version : versions) {
                switch (version) {
                    case "3.2.1":
                        log.info("Upgrading ThingsBoard from version 3.2.1 to 3.2.2 ...");
                        if (databaseTsUpgradeService != null) {
                            databaseTsUpgradeService.upgradeDatabase("3.2.1");
                        }
                        databaseEntitiesUpgradeService.upgradeDatabase("3.2.1");
                        break;
                    case "3.2.2":
                        log.info("Upgrading ThingsBoard from version 3.2.2 to 3.3.0 ...");
                        if (databaseTsUpgradeService != null) {
                            databaseTsUpgradeService.upgradeDatabase("3.2.2");
                        }
                        databaseEntitiesUpgradeService.upgradeDatabase("3.2.2");
                        systemDataLoaderService.createOAuth2Templates();
                        break;
                    case "3.3.2":
                        log.info("Upgrading ThingsBoard from version 3.3.2 to 3.3.3 ...");
                        databaseEntitiesUpgradeService.upgradeDatabase("3.3.2");
                        dataUpdateService.updateData("3.3.2");
                        break;
                    case "3.3.3":
                        log.info("Upgrading ThingsBoard from version 3.3.3 to 3.3.4 ...");
                        databaseEntitiesUpgradeService.upgradeDatabase("3.3.3");
                        break;
                    case "3.3.4":
                        log.info("Upgrading ThingsBoard from version 3.3.4 to 3.4.0 ...");
                        databaseEntitiesUpgradeService.upgradeDatabase("3.3.4");
                        dataUpdateService.updateData("3.3.4");
                        break;
                    case "3.4.0":
                        log.info("Upgrading ThingsBoard from version 3.4.0 to 3.4.1 ...");
                        databaseEntitiesUpgradeService.upgradeDatabase("3.4.0");
                        dataUpdateService.updateData("3.4.0");
                        break;
                    case "3.4.1":
                        log.info("Upgrading ThingsBoard from version 3.4.1 to 3.4.2 ...");
                        databaseEntitiesUpgradeService.upgradeDatabase("3.4.1");
                        dataUpdateService.updateData("3.4.1");
                        break;
                    case "3.4.4":
                        log.info("Upgrading ThingsBoard from version 3.4.4 to 3.5.0 ...");
                        databaseEntitiesUpgradeService.upgradeDatabase("3.4.4");
                        break;
                    case "3.5.0":
                        log.info("Upgrading ThingsBoard from version 3.5.0 to 3.5.1 ...");
                        databaseEntitiesUpgradeService.upgradeDatabase("3.5.0");
                        break;
                    case "3.5.1":
                        log.info("Upgrading ThingsBoard from version 3.5.1 to 3.6.0 ...");
                        databaseEntitiesUpgradeService.upgradeDatabase("3.5.1");
                        dataUpdateService.updateData("3.5.1");
                        systemDataLoaderService.updateDefaultNotificationConfigs();
                        break;
                    case "3.6.0":
                        log.info("Upgrading ThingsBoard from version 3.6.0 to 3.6.1 ...");
                        databaseEntitiesUpgradeService.upgradeDatabase("3.6.0");
                        dataUpdateService.updateData("3.6.0");
                        break;
                    case "3.6.1":
                        log.info("Upgrading ThingsBoard from version 3.6.1 to 3.6.2 ...");
                        databaseEntitiesUpgradeService.upgradeDatabase("3.6.1");
                        if (!getEnv("SKIP_IMAGES_MIGRATION", false)) {
                            installScripts.setUpdateImages(true);
                        } else {
                            log.info("Skipping images migration. Run the upgrade with fromVersion as '3.6.2-images' to migrate");
                        }
                        break;
                    case "3.6.2":
                        log.info("Upgrading ThingsBoard from version 3.6.2 to 3.6.3 ...");
                        databaseEntitiesUpgradeService.upgradeDatabase("3.6.2");
                        systemDataLoaderService.updateDefaultNotificationConfigs();
                        installScripts.updateImages();
                        //TODO DON'T FORGET to update switch statement in the CacheCleanupService if you need to clear the cache
                        break;
                    default:
                        throw new RuntimeException("Unable to upgrade ThingsBoard, unsupported fromVersion: " + upgradeFromVersion);
                }
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
            log.info("Upgrade finished successfully!");

        } catch (Exception e) {
            log.error("Unexpected error during ThingsBoard installation!", e);
            throw new ThingsboardUpdateException("Unexpected error during ThingsBoard installation!", e);
        } finally {
            SpringApplication.exit(context);
        }
    }

}

