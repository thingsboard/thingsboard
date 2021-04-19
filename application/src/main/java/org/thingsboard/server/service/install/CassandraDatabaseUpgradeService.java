/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.service.install;

import com.datastax.driver.core.KeyspaceMetadata;
import com.datastax.driver.core.exceptions.InvalidQueryException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.util.NoSqlDao;
import org.thingsboard.server.service.install.cql.CassandraDbHelper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.thingsboard.server.service.install.DatabaseHelper.ADDITIONAL_INFO;
import static org.thingsboard.server.service.install.DatabaseHelper.ASSET;
import static org.thingsboard.server.service.install.DatabaseHelper.ASSIGNED_CUSTOMERS;
import static org.thingsboard.server.service.install.DatabaseHelper.CONFIGURATION;
import static org.thingsboard.server.service.install.DatabaseHelper.CUSTOMER_ID;
import static org.thingsboard.server.service.install.DatabaseHelper.DASHBOARD;
import static org.thingsboard.server.service.install.DatabaseHelper.DEVICE;
import static org.thingsboard.server.service.install.DatabaseHelper.END_TS;
import static org.thingsboard.server.service.install.DatabaseHelper.ENTITY_ID;
import static org.thingsboard.server.service.install.DatabaseHelper.ENTITY_TYPE;
import static org.thingsboard.server.service.install.DatabaseHelper.ENTITY_VIEW;
import static org.thingsboard.server.service.install.DatabaseHelper.ENTITY_VIEWS;
import static org.thingsboard.server.service.install.DatabaseHelper.ID;
import static org.thingsboard.server.service.install.DatabaseHelper.KEYS;
import static org.thingsboard.server.service.install.DatabaseHelper.NAME;
import static org.thingsboard.server.service.install.DatabaseHelper.RULE_CHAIN;
import static org.thingsboard.server.service.install.DatabaseHelper.SEARCH_TEXT;
import static org.thingsboard.server.service.install.DatabaseHelper.START_TS;
import static org.thingsboard.server.service.install.DatabaseHelper.TENANT_ID;
import static org.thingsboard.server.service.install.DatabaseHelper.TITLE;
import static org.thingsboard.server.service.install.DatabaseHelper.TYPE;

@Service
@NoSqlDao
@Profile("install")
@Slf4j
public class CassandraDatabaseUpgradeService extends AbstractCassandraDatabaseUpgradeService implements DatabaseEntitiesUpgradeService {

    private static final String SCHEMA_UPDATE_CQL = "schema_update.cql";

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private InstallScripts installScripts;

    @Override
    public void upgradeDatabase(String fromVersion) throws Exception {

        switch (fromVersion) {
            case "1.2.3":

                log.info("Upgrading Cassandara DataBase from version {} to 1.3.0 ...", fromVersion);

                //Dump devices, assets and relations

                cluster.getSession();

                KeyspaceMetadata ks = cluster.getCluster().getMetadata().getKeyspace(cluster.getKeyspaceName());

                log.info("Dumping devices ...");
                Path devicesDump = CassandraDbHelper.dumpCfIfExists(ks, cluster.getSession(), DEVICE,
                        new String[]{"id", TENANT_ID, CUSTOMER_ID, "name", SEARCH_TEXT, ADDITIONAL_INFO, "type"},
                        new String[]{"", "", "", "", "", "", "default"},
                        "tb-devices");
                log.info("Devices dumped.");

                log.info("Dumping assets ...");
                Path assetsDump = CassandraDbHelper.dumpCfIfExists(ks, cluster.getSession(), ASSET,
                        new String[]{"id", TENANT_ID, CUSTOMER_ID, "name", SEARCH_TEXT, ADDITIONAL_INFO, "type"},
                        new String[]{"", "", "", "", "", "", "default"},
                        "tb-assets");
                log.info("Assets dumped.");

                log.info("Dumping relations ...");
                Path relationsDump = CassandraDbHelper.dumpCfIfExists(ks, cluster.getSession(), "relation",
                        new String[]{"from_id", "from_type", "to_id", "to_type", "relation_type", ADDITIONAL_INFO, "relation_type_group"},
                        new String[]{"", "", "", "", "", "", "COMMON"},
                        "tb-relations");
                log.info("Relations dumped.");

                log.info("Updating schema ...");
                Path schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "1.3.0", SCHEMA_UPDATE_CQL);
                loadCql(schemaUpdateFile);
                log.info("Schema updated.");

                //Restore devices, assets and relations

                log.info("Restoring devices ...");
                if (devicesDump != null) {
                    CassandraDbHelper.loadCf(ks, cluster.getSession(), DEVICE,
                            new String[]{"id", TENANT_ID, CUSTOMER_ID, "name", SEARCH_TEXT, ADDITIONAL_INFO, "type"}, devicesDump);
                    Files.deleteIfExists(devicesDump);
                }
                log.info("Devices restored.");

                log.info("Dumping device types ...");
                Path deviceTypesDump = CassandraDbHelper.dumpCfIfExists(ks, cluster.getSession(), DEVICE,
                        new String[]{TENANT_ID, "type"},
                        new String[]{"", ""},
                        "tb-device-types");
                if (deviceTypesDump != null) {
                    CassandraDbHelper.appendToEndOfLine(deviceTypesDump, "DEVICE");
                }
                log.info("Device types dumped.");
                log.info("Loading device types ...");
                if (deviceTypesDump != null) {
                    CassandraDbHelper.loadCf(ks, cluster.getSession(), "entity_subtype",
                            new String[]{TENANT_ID, "type", "entity_type"}, deviceTypesDump);
                    Files.deleteIfExists(deviceTypesDump);
                }
                log.info("Device types loaded.");

                log.info("Restoring assets ...");
                if (assetsDump != null) {
                    CassandraDbHelper.loadCf(ks, cluster.getSession(), ASSET,
                            new String[]{"id", TENANT_ID, CUSTOMER_ID, "name", SEARCH_TEXT, ADDITIONAL_INFO, "type"}, assetsDump);
                    Files.deleteIfExists(assetsDump);
                }
                log.info("Assets restored.");

                log.info("Dumping asset types ...");
                Path assetTypesDump = CassandraDbHelper.dumpCfIfExists(ks, cluster.getSession(), ASSET,
                        new String[]{TENANT_ID, "type"},
                        new String[]{"", ""},
                        "tb-asset-types");
                if (assetTypesDump != null) {
                    CassandraDbHelper.appendToEndOfLine(assetTypesDump, "ASSET");
                }
                log.info("Asset types dumped.");
                log.info("Loading asset types ...");
                if (assetTypesDump != null) {
                    CassandraDbHelper.loadCf(ks, cluster.getSession(), "entity_subtype",
                            new String[]{TENANT_ID, "type", "entity_type"}, assetTypesDump);
                    Files.deleteIfExists(assetTypesDump);
                }
                log.info("Asset types loaded.");

                log.info("Restoring relations ...");
                if (relationsDump != null) {
                    CassandraDbHelper.loadCf(ks, cluster.getSession(), "relation",
                            new String[]{"from_id", "from_type", "to_id", "to_type", "relation_type", ADDITIONAL_INFO, "relation_type_group"}, relationsDump);
                    Files.deleteIfExists(relationsDump);
                }
                log.info("Relations restored.");

                break;
            case "1.3.0":
                break;
            case "1.3.1":

                cluster.getSession();

                ks = cluster.getCluster().getMetadata().getKeyspace(cluster.getKeyspaceName());

                log.info("Dumping dashboards ...");
                Path dashboardsDump = CassandraDbHelper.dumpCfIfExists(ks, cluster.getSession(), DASHBOARD,
                        new String[]{ID, TENANT_ID, CUSTOMER_ID, TITLE, SEARCH_TEXT, ASSIGNED_CUSTOMERS, CONFIGURATION},
                        new String[]{"", "", "", "", "", "", ""},
                        "tb-dashboards", true);
                log.info("Dashboards dumped.");


                log.info("Updating schema ...");
                schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "1.4.0", SCHEMA_UPDATE_CQL);
                loadCql(schemaUpdateFile);
                log.info("Schema updated.");

                log.info("Restoring dashboards ...");
                if (dashboardsDump != null) {
                    CassandraDbHelper.loadCf(ks, cluster.getSession(), DASHBOARD,
                            new String[]{ID, TENANT_ID, TITLE, SEARCH_TEXT, CONFIGURATION}, dashboardsDump, true);
                    DatabaseHelper.upgradeTo40_assignDashboards(dashboardsDump, dashboardService, false);
                    Files.deleteIfExists(dashboardsDump);
                }
                log.info("Dashboards restored.");
                break;
            case "1.4.0":

                log.info("Updating schema ...");
                schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "2.0.0", SCHEMA_UPDATE_CQL);
                loadCql(schemaUpdateFile);
                log.info("Schema updated.");

                break;

            case "2.0.0":

                log.info("Updating schema ...");
                schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "2.1.1", SCHEMA_UPDATE_CQL);
                loadCql(schemaUpdateFile);
                log.info("Schema updated.");

                break;

            case "2.1.1":

                log.info("Upgrading Cassandra DataBase from version {} to 2.1.2 ...", fromVersion);

                cluster.getSession();

                ks = cluster.getCluster().getMetadata().getKeyspace(cluster.getKeyspaceName());

                log.info("Dumping entity views ...");
                Path entityViewsDump = CassandraDbHelper.dumpCfIfExists(ks, cluster.getSession(), ENTITY_VIEWS,
                        new String[]{ID, ENTITY_ID, ENTITY_TYPE, TENANT_ID, CUSTOMER_ID, NAME, TYPE, KEYS, START_TS, END_TS, SEARCH_TEXT, ADDITIONAL_INFO},
                        new String[]{"", "", "", "", "", "", "default", "", "0", "0", "", ""},
                        "tb-entity-views");
                log.info("Entity views dumped.");

                log.info("Updating schema ...");
                schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "2.1.2", SCHEMA_UPDATE_CQL);
                loadCql(schemaUpdateFile);
                log.info("Schema updated.");

                log.info("Restoring entity views ...");
                if (entityViewsDump != null) {
                    CassandraDbHelper.loadCf(ks, cluster.getSession(), ENTITY_VIEW,
                            new String[]{ID, ENTITY_ID, ENTITY_TYPE, TENANT_ID, CUSTOMER_ID, NAME, TYPE, KEYS, START_TS, END_TS, SEARCH_TEXT, ADDITIONAL_INFO}, entityViewsDump);
                    Files.deleteIfExists(entityViewsDump);
                }
                log.info("Entity views restored.");

                break;
            case "2.1.3":
                break;
            case "2.3.0":
                break;
            case "2.3.1":
                log.info("Updating schema ...");
                String updateDeviceTableStmt = "alter table device add label text";
                try {
                    cluster.getSession().execute(updateDeviceTableStmt);
                    Thread.sleep(2500);
                } catch (InvalidQueryException e) {
                }
                log.info("Schema updated.");
                break;
            case "2.4.1":
                log.info("Updating schema ...");
                String updateAssetTableStmt = "alter table asset add label text";
                try {
                    log.info("Updating assets ...");
                    cluster.getSession().execute(updateAssetTableStmt);
                    Thread.sleep(2500);
                    log.info("Assets updated.");
                } catch (InvalidQueryException e) {
                }
                log.info("Schema updated.");
                break;
            case "2.4.2":
                log.info("Updating schema ...");
                String updateAlarmTableStmt = "alter table alarm add propagate_relation_types text";
                try {
                    log.info("Updating alarms ...");
                    cluster.getSession().execute(updateAlarmTableStmt);
                    Thread.sleep(2500);
                    log.info("Alarms updated.");
                } catch (InvalidQueryException e) {
                }
                log.info("Schema updated.");
                break;
            case "2.4.3":
                log.info("Updating schema ...");
                String updateAttributeKvTableStmt = "alter table attributes_kv_cf add json_v text";
                try {
                    log.info("Updating attributes ...");
                    cluster.getSession().execute(updateAttributeKvTableStmt);
                    Thread.sleep(2500);
                    log.info("Attributes updated.");
                } catch (InvalidQueryException e) {
                }

                String updateTenantCoreTableStmt = "alter table tenant add isolated_tb_core boolean";
                String updateTenantRuleEngineTableStmt = "alter table tenant add isolated_tb_rule_engine boolean";

                try {
                    log.info("Updating tenant...");
                    cluster.getSession().execute(updateTenantCoreTableStmt);
                    Thread.sleep(2500);

                    cluster.getSession().execute(updateTenantRuleEngineTableStmt);
                    Thread.sleep(2500);
                    log.info("Tenant updated.");
                } catch (InvalidQueryException e) {
                }
                log.info("Schema updated.");
                break;
            case "2.5.6":

                log.info("Upgrading Cassandra DataBase from version {} to 2.6.0 ...", fromVersion);

                // Dump rule chains

                cluster.getSession();

                ks = cluster.getCluster().getMetadata().getKeyspace(cluster.getKeyspaceName());

                log.info("Dumping rule chains ...");
                Path ruleChainsDump = CassandraDbHelper.dumpCfIfExists(ks, cluster.getSession(), RULE_CHAIN,
                        new String[]{ID, TENANT_ID, NAME, SEARCH_TEXT, "first_rule_node_id", "root", "debug_mode", CONFIGURATION, ADDITIONAL_INFO, TYPE},
                        new String[]{"", "", "", "", "", "", "", "", "", RuleChainType.CORE.name()},
                        "tb-rule-chains");
                log.info("Rule chains dumped.");

                log.info("Updating schema ...");
                schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "2.6.0", SCHEMA_UPDATE_CQL);
                loadCql(schemaUpdateFile);
                log.info("Schema updated.");

                // Restore rule chains

                log.info("Restoring rule chains ...");
                if (ruleChainsDump != null) {
                    CassandraDbHelper.loadCf(ks, cluster.getSession(), RULE_CHAIN,
                            new String[]{ID, TENANT_ID, NAME, SEARCH_TEXT, "first_rule_node_id", "root", "debug_mode", CONFIGURATION, ADDITIONAL_INFO, TYPE}, ruleChainsDump);
                    Files.deleteIfExists(ruleChainsDump);
                }
                log.info("Rule chains restored.");
                break;

            default:
                throw new RuntimeException("Unable to upgrade Cassandra database, unsupported fromVersion: " + fromVersion);
        }
    }

}
