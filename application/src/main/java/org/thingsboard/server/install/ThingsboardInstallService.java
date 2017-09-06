/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
import org.thingsboard.server.service.install.DatabaseSchemaService;
import org.thingsboard.server.service.install.DatabaseUpgradeService;
import org.thingsboard.server.service.install.SystemDataLoaderService;

import java.nio.file.Files;
import java.nio.file.Paths;

@Service
@Profile("install")
@Slf4j
public class ThingsboardInstallService {

    @Value("${install.upgrade:false}")
    private Boolean isUpgrade;

    @Value("${install.upgrade.from_version:1.2.3}")
    private String upgradeFromVersion;

    @Value("${install.data_dir}")
    private String dataDir;

    @Value("${install.load_demo:false}")
    private Boolean loadDemo;

    @Autowired
    private DatabaseSchemaService databaseSchemaService;

    @Autowired
    private DatabaseUpgradeService databaseUpgradeService;

    @Autowired
    private ComponentDiscoveryService componentDiscoveryService;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private SystemDataLoaderService systemDataLoaderService;

    public void performInstall() {
        try {
            if (isUpgrade) {
                log.info("Starting ThingsBoard Upgrade from version {} ...", upgradeFromVersion);

                switch (upgradeFromVersion) {
                    case "1.2.3":
                        log.info("Upgrading ThingsBoard from version 1.2.3 to 1.3.0 ...");

                        databaseUpgradeService.upgradeDatabase(upgradeFromVersion);

                        log.info("Updating system data...");

                        systemDataLoaderService.deleteSystemWidgetBundle("charts");
                        systemDataLoaderService.deleteSystemWidgetBundle("cards");
                        systemDataLoaderService.deleteSystemWidgetBundle("maps");
                        systemDataLoaderService.deleteSystemWidgetBundle("analogue_gauges");
                        systemDataLoaderService.deleteSystemWidgetBundle("digital_gauges");
                        systemDataLoaderService.deleteSystemWidgetBundle("gpio_widgets");
                        systemDataLoaderService.deleteSystemWidgetBundle("alarm_widgets");

                    case "1.3.0":
                        log.info("Upgrading ThingsBoard from version 1.3.0 to 1.3.1 ...");

                        log.info("Updating system data...");

                        systemDataLoaderService.deleteSystemWidgetBundle("charts");
                        systemDataLoaderService.deleteSystemWidgetBundle("cards");
                        systemDataLoaderService.deleteSystemWidgetBundle("maps");
                        systemDataLoaderService.deleteSystemWidgetBundle("analogue_gauges");
                        systemDataLoaderService.deleteSystemWidgetBundle("digital_gauges");
                        systemDataLoaderService.deleteSystemWidgetBundle("gpio_widgets");
                        systemDataLoaderService.deleteSystemWidgetBundle("alarm_widgets");
                        systemDataLoaderService.deleteSystemWidgetBundle("control_widgets");
                        systemDataLoaderService.deleteSystemWidgetBundle("maps_v2");

                        systemDataLoaderService.loadSystemWidgets();

                        break;
                    default:
                        throw new RuntimeException("Unable to upgrade ThingsBoard, unsupported fromVersion: " + upgradeFromVersion);

                }
                log.info("Upgrade finished successfully!");

            } else {

                log.info("Starting ThingsBoard Installation...");

                if (this.dataDir == null) {
                    throw new RuntimeException("'install.data_dir' property should specified!");
                }
                if (!Files.isDirectory(Paths.get(this.dataDir))) {
                    throw new RuntimeException("'install.data_dir' property value is not a valid directory!");
                }

                log.info("Installing DataBase schema...");

                databaseSchemaService.createDatabaseSchema();

                log.info("Loading system data...");

                componentDiscoveryService.discoverComponents();

                systemDataLoaderService.createSysAdmin();
                systemDataLoaderService.createAdminSettings();
                systemDataLoaderService.loadSystemWidgets();
                systemDataLoaderService.loadSystemPlugins();
                systemDataLoaderService.loadSystemRules();

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
