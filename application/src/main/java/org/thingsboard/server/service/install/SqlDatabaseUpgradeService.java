/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.alarm.rule.AlarmRuleOriginatorTargetEntity;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleDeviceTypeEntityFilter;
import org.thingsboard.server.common.data.device.profile.AlarmRuleConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.queue.ProcessingStrategy;
import org.thingsboard.server.common.data.queue.ProcessingStrategyType;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.queue.SubmitStrategy;
import org.thingsboard.server.common.data.queue.SubmitStrategyType;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.alarm.rule.AlarmRuleDao;
import org.thingsboard.server.dao.asset.AssetDao;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.model.sql.DeviceProfileEntity;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.sql.device.DeviceProfileRepository;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.usagerecord.ApiUsageStateService;
import org.thingsboard.server.queue.settings.TbRuleEngineQueueConfiguration;
import org.thingsboard.server.service.alarm.rule.state.PersistedAlarmState;
import org.thingsboard.server.service.alarm.rule.state.PersistedEntityState;
import org.thingsboard.server.service.alarm.rule.store.RedisAlarmRuleEntityStateStore;
import org.thingsboard.server.service.install.sql.SqlDbHelper;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.service.install.DatabaseHelper.ADDITIONAL_INFO;
import static org.thingsboard.server.service.install.DatabaseHelper.ASSIGNED_CUSTOMERS;
import static org.thingsboard.server.service.install.DatabaseHelper.CONFIGURATION;
import static org.thingsboard.server.service.install.DatabaseHelper.CUSTOMER_ID;
import static org.thingsboard.server.service.install.DatabaseHelper.DASHBOARD;
import static org.thingsboard.server.service.install.DatabaseHelper.END_TS;
import static org.thingsboard.server.service.install.DatabaseHelper.ENTITY_ID;
import static org.thingsboard.server.service.install.DatabaseHelper.ENTITY_TYPE;
import static org.thingsboard.server.service.install.DatabaseHelper.ENTITY_VIEW;
import static org.thingsboard.server.service.install.DatabaseHelper.ENTITY_VIEWS;
import static org.thingsboard.server.service.install.DatabaseHelper.ID;
import static org.thingsboard.server.service.install.DatabaseHelper.KEYS;
import static org.thingsboard.server.service.install.DatabaseHelper.NAME;
import static org.thingsboard.server.service.install.DatabaseHelper.SEARCH_TEXT;
import static org.thingsboard.server.service.install.DatabaseHelper.START_TS;
import static org.thingsboard.server.service.install.DatabaseHelper.TENANT_ID;
import static org.thingsboard.server.service.install.DatabaseHelper.TITLE;
import static org.thingsboard.server.service.install.DatabaseHelper.TYPE;

@Service
@Profile("install")
@Slf4j
public class SqlDatabaseUpgradeService implements DatabaseEntitiesUpgradeService {

    private static final String SCHEMA_UPDATE_SQL = "schema_update.sql";

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUserName;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private InstallScripts installScripts;

    @Autowired
    private SystemDataLoaderService systemDataLoaderService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private AlarmRuleDao alarmRuleService;

    @Autowired
    private Optional<RedisAlarmRuleEntityStateStore> stateStore;

    @Autowired
    private RuleChainService ruleChainService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private AssetDao assetDao;

    @Autowired
    private DeviceProfileService deviceProfileService;

    @Autowired
    private DeviceProfileRepository deviceProfileRepository;

    @Autowired
    private AssetProfileService assetProfileService;

    @Autowired
    private ApiUsageStateService apiUsageStateService;

    @Lazy
    @Autowired
    private QueueService queueService;

    @Autowired
    private TbRuleEngineQueueConfigService queueConfig;

    @Autowired
    private DbUpgradeExecutorService dbUpgradeExecutor;

    @Override
    public void upgradeDatabase(String fromVersion) throws Exception {
        switch (fromVersion) {
            case "1.3.0":
                log.info("Updating schema ...");
                Path schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "1.3.1", SCHEMA_UPDATE_SQL);
                try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
                    loadSql(schemaUpdateFile, conn);
                }
                log.info("Schema updated.");
                break;
            case "1.3.1":
                try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {

                    log.info("Dumping dashboards ...");
                    Path dashboardsDump = SqlDbHelper.dumpTableIfExists(conn, DASHBOARD,
                            new String[]{ID, TENANT_ID, CUSTOMER_ID, TITLE, SEARCH_TEXT, ASSIGNED_CUSTOMERS, CONFIGURATION},
                            new String[]{"", "", "", "", "", "", ""},
                            "tb-dashboards", true);
                    log.info("Dashboards dumped.");

                    log.info("Updating schema ...");
                    schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "1.4.0", SCHEMA_UPDATE_SQL);
                    loadSql(schemaUpdateFile, conn);
                    log.info("Schema updated.");

                    log.info("Restoring dashboards ...");
                    if (dashboardsDump != null) {
                        SqlDbHelper.loadTable(conn, DASHBOARD,
                                new String[]{ID, TENANT_ID, TITLE, SEARCH_TEXT, CONFIGURATION}, dashboardsDump, true);
                        DatabaseHelper.upgradeTo40_assignDashboards(dashboardsDump, dashboardService, true);
                        Files.deleteIfExists(dashboardsDump);
                    }
                    log.info("Dashboards restored.");
                }
                break;
            case "1.4.0":
                try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
                    log.info("Updating schema ...");
                    schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "2.0.0", SCHEMA_UPDATE_SQL);
                    loadSql(schemaUpdateFile, conn);
                    log.info("Schema updated.");
                }
                break;
            case "2.0.0":
                try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
                    log.info("Updating schema ...");
                    schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "2.1.1", SCHEMA_UPDATE_SQL);
                    loadSql(schemaUpdateFile, conn);
                    log.info("Schema updated.");
                }
                break;
            case "2.1.1":
                try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {

                    log.info("Dumping entity views ...");
                    Path entityViewsDump = SqlDbHelper.dumpTableIfExists(conn, ENTITY_VIEWS,
                            new String[]{ID, ENTITY_ID, ENTITY_TYPE, TENANT_ID, CUSTOMER_ID, TYPE, NAME, KEYS, START_TS, END_TS, SEARCH_TEXT, ADDITIONAL_INFO},
                            new String[]{"", "", "", "", "", "default", "", "", "0", "0", "", ""},
                            "tb-entity-views", true);
                    log.info("Entity views dumped.");

                    log.info("Updating schema ...");
                    schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "2.1.2", SCHEMA_UPDATE_SQL);
                    loadSql(schemaUpdateFile, conn);
                    log.info("Schema updated.");

                    log.info("Restoring entity views ...");
                    if (entityViewsDump != null) {
                        SqlDbHelper.loadTable(conn, ENTITY_VIEW,
                                new String[]{ID, ENTITY_ID, ENTITY_TYPE, TENANT_ID, CUSTOMER_ID, TYPE, NAME, KEYS, START_TS, END_TS, SEARCH_TEXT, ADDITIONAL_INFO}, entityViewsDump, true);
                        Files.deleteIfExists(entityViewsDump);
                    }
                    log.info("Entity views restored.");
                }
                break;
            case "2.1.3":
                try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
                    log.info("Updating schema ...");
                    schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "2.2.0", SCHEMA_UPDATE_SQL);
                    loadSql(schemaUpdateFile, conn);
                    log.info("Schema updated.");
                }
                break;
            case "2.3.0":
                try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
                    log.info("Updating schema ...");
                    schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "2.3.1", SCHEMA_UPDATE_SQL);
                    loadSql(schemaUpdateFile, conn);
                    log.info("Schema updated.");
                }
                break;
            case "2.3.1":
                try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
                    log.info("Updating schema ...");
                    schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "2.4.0", SCHEMA_UPDATE_SQL);
                    loadSql(schemaUpdateFile, conn);
                    try {
                        conn.createStatement().execute("ALTER TABLE device ADD COLUMN label varchar(255)"); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
                    } catch (Exception e) {
                    }
                    log.info("Schema updated.");
                }
                break;
            case "2.4.1":
                try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
                    log.info("Updating schema ...");
                    try {
                        conn.createStatement().execute("ALTER TABLE asset ADD COLUMN label varchar(255)"); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
                    } catch (Exception e) {
                    }
                    schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "2.4.2", SCHEMA_UPDATE_SQL);
                    loadSql(schemaUpdateFile, conn);
                    try {
                        conn.createStatement().execute("ALTER TABLE device ADD CONSTRAINT device_name_unq_key UNIQUE (tenant_id, name)"); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
                    } catch (Exception e) {
                    }
                    try {
                        conn.createStatement().execute("ALTER TABLE device_credentials ADD CONSTRAINT device_credentials_id_unq_key UNIQUE (credentials_id)"); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
                    } catch (Exception e) {
                    }
                    try {
                        conn.createStatement().execute("ALTER TABLE asset ADD CONSTRAINT asset_name_unq_key UNIQUE (tenant_id, name)"); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
                    } catch (Exception e) {
                    }
                    log.info("Schema updated.");
                }
                break;
            case "2.4.2":
                try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
                    log.info("Updating schema ...");
                    try {
                        conn.createStatement().execute("ALTER TABLE alarm ADD COLUMN propagate_relation_types varchar"); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
                    } catch (Exception e) {
                    }
                    log.info("Schema updated.");
                }
                break;
            case "2.4.3":
                try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
                    log.info("Updating schema ...");
                    try {
                        conn.createStatement().execute("ALTER TABLE attribute_kv ADD COLUMN json_v json;");
                    } catch (Exception e) {
                        if (e instanceof SQLSyntaxErrorException) {
                            try {
                                conn.createStatement().execute("ALTER TABLE attribute_kv ADD COLUMN json_v varchar(10000000);");
                            } catch (Exception e1) {
                            }
                        }
                    }
                    try {
                        conn.createStatement().execute("ALTER TABLE tenant ADD COLUMN isolated_tb_core boolean DEFAULT (false), ADD COLUMN isolated_tb_rule_engine boolean DEFAULT (false)");
                    } catch (Exception e) {
                    }
                    try {
                        long ts = System.currentTimeMillis();
                        conn.createStatement().execute("ALTER TABLE event ADD COLUMN ts bigint DEFAULT " + ts + ";"); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
                    } catch (Exception e) {
                    }
                    log.info("Schema updated.");
                }
                break;
            case "3.0.1":
                try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
                    log.info("Updating schema ...");
                    if (isOldSchema(conn, 3000001)) {
                        String[] tables = new String[]{"admin_settings", "alarm", "asset", "audit_log", "attribute_kv",
                                "component_descriptor", "customer", "dashboard", "device", "device_credentials", "event",
                                "relation", "tb_user", "tenant", "user_credentials", "widget_type", "widgets_bundle",
                                "rule_chain", "rule_node", "entity_view"};
                        schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "3.0.1", "schema_update_to_uuid.sql");
                        loadSql(schemaUpdateFile, conn);

                        conn.createStatement().execute("call drop_all_idx()");

                        log.info("Optimizing alarm relations...");
                        conn.createStatement().execute("DELETE from relation WHERE relation_type_group = 'ALARM' AND relation_type <> 'ALARM_ANY';");
                        conn.createStatement().execute("DELETE from relation WHERE relation_type_group = 'ALARM' AND relation_type = 'ALARM_ANY' " +
                                "AND exists(SELECT * FROM alarm WHERE alarm.id = relation.to_id AND alarm.originator_id = relation.from_id)");
                        log.info("Alarm relations optimized.");

                        for (String table : tables) {
                            log.info("Updating table {}.", table);
                            Statement statement = conn.createStatement();
                            statement.execute("call update_" + table + "();");

                            SQLWarning warnings = statement.getWarnings();
                            if (warnings != null) {
                                log.info("{}", warnings.getMessage());
                                SQLWarning nextWarning = warnings.getNextWarning();
                                while (nextWarning != null) {
                                    log.info("{}", nextWarning.getMessage());
                                    nextWarning = nextWarning.getNextWarning();
                                }
                            }

                            conn.createStatement().execute("DROP PROCEDURE update_" + table);
                            log.info("Table {} updated.", table);
                        }
                        conn.createStatement().execute("call create_all_idx()");

                        conn.createStatement().execute("DROP PROCEDURE drop_all_idx");
                        conn.createStatement().execute("DROP PROCEDURE create_all_idx");
                        conn.createStatement().execute("DROP FUNCTION column_type_to_uuid");

                        log.info("Updating alarm relations...");
                        conn.createStatement().execute("UPDATE relation SET relation_type = 'ANY' WHERE relation_type_group = 'ALARM' AND relation_type = 'ALARM_ANY';");
                        log.info("Alarm relations updated.");

                        conn.createStatement().execute("UPDATE tb_schema_settings SET schema_version = 3001000;");

                        conn.createStatement().execute("VACUUM FULL");
                    }
                    log.info("Schema updated.");
                } catch (Exception e) {
                    log.error("Failed updating schema!!!", e);
                }
                break;
            case "3.1.0":
                try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
                    log.info("Updating schema ...");
                    schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "3.1.0", SCHEMA_UPDATE_SQL);
                    loadSql(schemaUpdateFile, conn);
                    log.info("Schema updated.");
                }
                break;
            case "3.1.1":
                try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
                    log.info("Updating schema ...");
                    if (isOldSchema(conn, 3001000)) {

                        try {
                            conn.createStatement().execute("ALTER TABLE device ADD COLUMN device_profile_id uuid, ADD COLUMN device_data jsonb");
                        } catch (Exception e) {
                        }

                        try {
                            conn.createStatement().execute("ALTER TABLE tenant ADD COLUMN tenant_profile_id uuid");
                        } catch (Exception e) {
                        }

                        try {
                            conn.createStatement().execute("CREATE TABLE IF NOT EXISTS rule_node_state (" +
                                    " id uuid NOT NULL CONSTRAINT rule_node_state_pkey PRIMARY KEY," +
                                    " created_time bigint NOT NULL," +
                                    " rule_node_id uuid NOT NULL," +
                                    " entity_type varchar(32) NOT NULL," +
                                    " entity_id uuid NOT NULL," +
                                    " state_data varchar(16384) NOT NULL," +
                                    " CONSTRAINT rule_node_state_unq_key UNIQUE (rule_node_id, entity_id)," +
                                    " CONSTRAINT fk_rule_node_state_node_id FOREIGN KEY (rule_node_id) REFERENCES rule_node(id) ON DELETE CASCADE)");
                        } catch (Exception e) {
                        }

                        try {
                            conn.createStatement().execute("CREATE TABLE IF NOT EXISTS api_usage_state (" +
                                    " id uuid NOT NULL CONSTRAINT usage_record_pkey PRIMARY KEY," +
                                    " created_time bigint NOT NULL," +
                                    " tenant_id uuid," +
                                    " entity_type varchar(32)," +
                                    " entity_id uuid," +
                                    " transport varchar(32)," +
                                    " db_storage varchar(32)," +
                                    " re_exec varchar(32)," +
                                    " js_exec varchar(32)," +
                                    " email_exec varchar(32)," +
                                    " sms_exec varchar(32)," +
                                    " CONSTRAINT api_usage_state_unq_key UNIQUE (tenant_id, entity_id)\n" +
                                    ");");
                        } catch (Exception e) {
                        }

                        schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "3.1.1", "schema_update_before.sql");
                        loadSql(schemaUpdateFile, conn);

                        log.info("Creating default tenant profiles...");
                        systemDataLoaderService.createDefaultTenantProfiles();

                        log.info("Updating tenant profiles...");
                        conn.createStatement().execute("call update_tenant_profiles()");

                        log.info("Creating default device profiles...");
                        PageLink pageLink = new PageLink(100);
                        PageData<Tenant> pageData;
                        do {
                            pageData = tenantService.findTenants(pageLink);
                            for (Tenant tenant : pageData.getData()) {
                                try {
                                    apiUsageStateService.createDefaultApiUsageState(tenant.getId(), null);
                                } catch (Exception e) {
                                }
                                List<EntitySubtype> deviceTypes = deviceService.findDeviceTypesByTenantId(tenant.getId()).get();
                                try {
                                    deviceProfileService.createDefaultDeviceProfile(tenant.getId());
                                } catch (Exception e) {
                                }
                                for (EntitySubtype deviceType : deviceTypes) {
                                    try {
                                        deviceProfileService.findOrCreateDeviceProfile(tenant.getId(), deviceType.getType());
                                    } catch (Exception e) {
                                    }
                                }
                            }
                            pageLink = pageLink.nextPageLink();
                        } while (pageData.hasNext());

                        log.info("Updating device profiles...");
                        conn.createStatement().execute("call update_device_profiles()");

                        schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "3.1.1", "schema_update_after.sql");
                        loadSql(schemaUpdateFile, conn);

                        conn.createStatement().execute("UPDATE tb_schema_settings SET schema_version = 3002000;");
                    }
                    log.info("Schema updated.");
                } catch (Exception e) {
                    log.error("Failed updating schema!!!", e);
                }
                break;
            case "3.2.0":
                try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
                    log.info("Updating schema ...");
                    try {
                        conn.createStatement().execute("CREATE INDEX IF NOT EXISTS idx_device_device_profile_id ON device(tenant_id, device_profile_id);");
                        conn.createStatement().execute("ALTER TABLE dashboard ALTER COLUMN configuration TYPE varchar;");
                        conn.createStatement().execute("UPDATE tb_schema_settings SET schema_version = 3002001;");
                    } catch (Exception e) {
                        log.error("Failed updating schema!!!", e);
                    }
                    log.info("Schema updated.");
                }
                break;
            case "3.2.1":
                try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
                    log.info("Updating schema ...");
                    conn.createStatement().execute("CREATE INDEX IF NOT EXISTS idx_audit_log_tenant_id_and_created_time ON audit_log(tenant_id, created_time);");
                    schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "3.2.1", SCHEMA_UPDATE_SQL);
                    loadSql(schemaUpdateFile, conn);
                    conn.createStatement().execute("UPDATE tb_schema_settings SET schema_version = 3002002;");
                    log.info("Schema updated.");
                } catch (Exception e) {
                    log.error("Failed updating schema!!!", e);
                }
                break;
            case "3.2.2":
                try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
                    log.info("Updating schema ...");
                    try {
                        conn.createStatement().execute("ALTER TABLE rule_chain ADD COLUMN type varchar(255) DEFAULT 'CORE'"); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
                    } catch (Exception ignored) {
                    }
                    schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "3.2.2", SCHEMA_UPDATE_SQL);
                    loadSql(schemaUpdateFile, conn);
                    log.info("Load Edge TTL functions ...");
                    schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "3.2.2", "schema_update_ttl.sql");
                    loadSql(schemaUpdateFile, conn);
                    log.info("Edge TTL functions successfully loaded!");
                    log.info("Updating indexes and TTL procedure for event table...");
                    schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "3.2.2", "schema_update_event.sql");
                    loadSql(schemaUpdateFile, conn);
                    log.info("Updating schema settings...");
                    conn.createStatement().execute("UPDATE tb_schema_settings SET schema_version = 3003000;");
                    log.info("Schema updated.");
                } catch (Exception e) {
                    log.error("Failed updating schema!!!", e);
                }
                break;
            case "3.3.2":
                try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
                    log.info("Updating schema ...");
                    schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "3.3.2", SCHEMA_UPDATE_SQL);
                    loadSql(schemaUpdateFile, conn);
                    try {
                        conn.createStatement().execute("ALTER TABLE alarm ADD COLUMN propagate_to_owner boolean DEFAULT false;"); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
                        conn.createStatement().execute("ALTER TABLE alarm ADD COLUMN propagate_to_tenant boolean DEFAULT false;"); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
                    } catch (Exception ignored) {
                    }

                    try {
                        conn.createStatement().execute("insert into entity_alarm(tenant_id, entity_id, created_time, alarm_type, customer_id, alarm_id)" +
                                " select tenant_id, originator_id, created_time, type, customer_id, id from alarm ON CONFLICT DO NOTHING;");
                        conn.createStatement().execute("insert into entity_alarm(tenant_id, entity_id, created_time, alarm_type, customer_id, alarm_id)" +
                                " select a.tenant_id, r.from_id, created_time, type, customer_id, id" +
                                " from alarm a inner join relation r on r.relation_type_group = 'ALARM' and r.relation_type = 'ANY' and a.id = r.to_id ON CONFLICT DO NOTHING;");
                        conn.createStatement().execute("delete from relation r where r.relation_type_group = 'ALARM';");
                    } catch (Exception e) {
                        log.error("Failed to update alarm relations!!!", e);
                    }

                    log.info("Updating lwm2m device profiles ...");
                    try {
                        schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "3.3.2", "schema_update_lwm2m_bootstrap.sql");
                        loadSql(schemaUpdateFile, conn);
                        log.info("Updating server`s public key from HexDec to Base64 in profile for LWM2M...");
                        conn.createStatement().execute("call update_profile_bootstrap();");
                        log.info("Server`s public key from HexDec to Base64 in profile for LWM2M updated.");
                        log.info("Updating client`s public key and secret key from HexDec to Base64 for LWM2M...");
                        conn.createStatement().execute("call update_device_credentials_to_base64_and_bootstrap();");
                        log.info("Client`s public key and secret key from HexDec to Base64 for LWM2M updated.");
                    } catch (Exception e) {
                        log.error("Failed to update lwm2m profiles!!!", e);
                    }
                    log.info("Updating schema settings...");
                    conn.createStatement().execute("UPDATE tb_schema_settings SET schema_version = 3003003;");
                    log.info("Schema updated.");
                } catch (Exception e) {
                    log.error("Failed updating schema!!!", e);
                }
                break;
            case "3.3.3":
                try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
                    log.info("Updating schema ...");
                    try {
                        conn.createStatement().execute("ALTER TABLE edge DROP COLUMN edge_license_key;"); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
                        conn.createStatement().execute("ALTER TABLE edge DROP COLUMN cloud_endpoint;"); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
                    } catch (Exception ignored) {
                    }

                    log.info("Updating TTL cleanup procedure for the event table...");
                    schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "3.3.3", "schema_event_ttl_procedure.sql");
                    loadSql(schemaUpdateFile, conn);
                    schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "3.3.3", SCHEMA_UPDATE_SQL);
                    loadSql(schemaUpdateFile, conn);

                    log.info("Updating schema settings...");
                    conn.createStatement().execute("UPDATE tb_schema_settings SET schema_version = 3003004;");
                    log.info("Schema updated.");
                } catch (Exception e) {
                    log.error("Failed updating schema!!!", e);
                }
                break;
            case "3.3.4":
                try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
                    log.info("Updating schema ...");
                    schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "3.3.4", SCHEMA_UPDATE_SQL);
                    loadSql(schemaUpdateFile, conn);

                    log.info("Loading queues...");
                    try {
                        if (!CollectionUtils.isEmpty(queueConfig.getQueues())) {
                            queueConfig.getQueues().forEach(queueSettings -> {
                                Queue queue = queueConfigToQueue(queueSettings);
                                Queue existing = queueService.findQueueByTenantIdAndName(queue.getTenantId(), queue.getName());
                                if (existing == null) {
                                    queueService.saveQueue(queue);
                                }
                            });
                        } else {
                            systemDataLoaderService.createQueues();
                        }
                    } catch (Exception e) {
                    }

                    log.info("Updating schema settings...");
                    conn.createStatement().execute("UPDATE tb_schema_settings SET schema_version = 3004000;");
                    log.info("Schema updated.");
                } catch (Exception e) {
                    log.error("Failed updating schema!!!", e);
                }
                break;
            case "3.4.0":
                try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
                    log.info("Updating schema ...");
                    schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "3.4.0", SCHEMA_UPDATE_SQL);
                    loadSql(schemaUpdateFile, conn);
                    log.info("Updating schema settings...");
                    conn.createStatement().execute("UPDATE tb_schema_settings SET schema_version = 3004001;");
                    log.info("Schema updated.");
                } catch (Exception e) {
                    log.error("Failed updating schema!!!", e);
                }
                break;
            case "3.4.1":
                try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
                    log.info("Updating schema ...");
                    runSchemaUpdateScript(conn, "3.4.1");
                    if (isOldSchema(conn, 3004001)) {
                        try {
                            conn.createStatement().execute("ALTER TABLE asset ADD COLUMN asset_profile_id uuid");
                        } catch (Exception e) {
                        }

                        schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "3.4.1", "schema_update_before.sql");
                        loadSql(schemaUpdateFile, conn);

                        conn.createStatement().execute("DELETE FROM asset a WHERE NOT exists(SELECT id FROM tenant WHERE id = a.tenant_id);");

                        log.info("Creating default asset profiles...");

                        PageLink pageLink = new PageLink(1000);
                        PageData<TenantId> tenantIds;
                        do {
                            List<ListenableFuture<?>> futures = new ArrayList<>();
                            tenantIds = tenantService.findTenantsIds(pageLink);
                            for (TenantId tenantId : tenantIds.getData()) {
                                futures.add(dbUpgradeExecutor.submit(() -> {
                                    try {
                                        assetProfileService.createDefaultAssetProfile(tenantId);
                                    } catch (Exception e) {
                                    }
                                }));
                            }
                            Futures.allAsList(futures).get();
                            pageLink = pageLink.nextPageLink();
                        } while (tenantIds.hasNext());

                        pageLink = new PageLink(1000);
                        PageData<TbPair<UUID, String>> pairs;
                        do {
                            List<ListenableFuture<?>> futures = new ArrayList<>();
                            pairs = assetDao.getAllAssetTypes(pageLink);
                            for (TbPair<UUID, String> pair : pairs.getData()) {
                                TenantId tenantId = new TenantId(pair.getFirst());
                                String assetType = pair.getSecond();
                                if (!"default".equals(assetType)) {
                                    futures.add(dbUpgradeExecutor.submit(() -> {
                                        try {
                                            assetProfileService.findOrCreateAssetProfile(tenantId, assetType);
                                        } catch (Exception e) {
                                        }
                                    }));
                                }
                            }
                            Futures.allAsList(futures).get();
                            pageLink = pageLink.nextPageLink();
                        } while (pairs.hasNext());

                        log.info("Updating asset profiles...");
                        conn.createStatement().execute("call update_asset_profiles()");

                        schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "3.4.1", "schema_update_after.sql");
                        loadSql(schemaUpdateFile, conn);

                        conn.createStatement().execute("UPDATE tb_schema_settings SET schema_version = 3004002;");
                    }
                    log.info("Schema updated.");
                } catch (Exception e) {
                    log.error("Failed updating schema!!!", e);
                }
                break;
            case "3.4.4":
                try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
                    log.info("Updating schema ...");
                    if (isOldSchema(conn, 3004002)) {
                        schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "3.4.4", SCHEMA_UPDATE_SQL);
                        loadSql(schemaUpdateFile, conn);

                        try {
                            conn.createStatement().execute("VACUUM FULL ANALYZE alarm;"); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
                        } catch (Exception e) {
                        }

                        try {
                            conn.createStatement().execute("ALTER TABLE asset_profile ADD COLUMN default_edge_rule_chain_id uuid"); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
                        } catch (Exception e) {
                        }
                        try {
                            conn.createStatement().execute("ALTER TABLE device_profile ADD COLUMN default_edge_rule_chain_id uuid"); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
                        } catch (Exception e) {
                        }
                        try {
                            conn.createStatement().execute("ALTER TABLE asset_profile ADD CONSTRAINT fk_default_edge_rule_chain_asset_profile FOREIGN KEY (default_edge_rule_chain_id) REFERENCES rule_chain(id)"); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
                        } catch (Exception e) {
                        }
                        try {
                            conn.createStatement().execute("ALTER TABLE device_profile ADD CONSTRAINT fk_default_edge_rule_chain_device_profile FOREIGN KEY (default_edge_rule_chain_id) REFERENCES rule_chain(id)"); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
                        } catch (Exception e) {
                        }

                        log.info("Alarm Rules migration ...");

                        PageLink pageLink = new PageLink(1000);
                        PageData<TenantId> tenantIds;
                        do {
                            List<ListenableFuture<?>> futures = new ArrayList<>();
                            tenantIds = tenantService.findTenantsIds(pageLink);
                            for (TenantId tenantId : tenantIds.getData()) {
                                futures.add(dbUpgradeExecutor.submit(() -> {
                                    try {
                                        PageLink profilePageLink = new PageLink(1000);
                                        PageData<DeviceProfileEntity> profiles;
                                        do {
                                            profiles = DaoUtil.pageToPageData(deviceProfileRepository.findDeviceProfilesWIthAlarmRules(
                                                    tenantId.getId(),
                                                    DaoUtil.toPageable(profilePageLink)));

                                            try {
                                                RuleChainId rootRuleChainId;
                                                try {
                                                    rootRuleChainId = Optional
                                                            .ofNullable(ruleChainService.getRootTenantRuleChain(tenantId))
                                                            .map(RuleChain::getId)
                                                            .orElse(null);
                                                } catch (Exception e) {
                                                    rootRuleChainId = null;
                                                }

                                                for (DeviceProfileEntity deviceProfile : profiles.getData()) {
                                                    Map<String, String> alarmRuleIdMapping = new HashMap<>();

                                                    JsonNode profileData = deviceProfile.getProfileData();
                                                    ArrayNode alarms = (ArrayNode) profileData.get("alarms");

                                                    for (JsonNode alarm : alarms) {
                                                        try {
                                                            AlarmRule alarmRule = new AlarmRule();
                                                            alarmRule.setTenantId(tenantId);
                                                            alarmRule.setEnabled(true);
                                                            String alarmType = alarm.get("alarmType").asText();
                                                            alarmRule.setAlarmType(alarmType);
                                                            alarmRule.setName(alarmType);

                                                            AlarmRuleConfiguration configuration = JacksonUtil.convertValue(alarm, AlarmRuleConfiguration.class);
                                                            configuration.setSourceEntityFilters(Collections.singletonList(new AlarmRuleDeviceTypeEntityFilter(new DeviceProfileId(deviceProfile.getId()))));
                                                            configuration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());
                                                            alarmRule.setConfiguration(configuration);

                                                            AlarmRule savedRule = alarmRuleService.save(tenantId, alarmRule);

                                                            alarmRuleIdMapping.put(alarm.get("id").asText(), savedRule.getId().toString());
                                                        } catch (Exception e) {
                                                        }
                                                    }

                                                    RuleChainId ruleChainId =
                                                            Optional.ofNullable(deviceProfile.getDefaultRuleChainId()).map(RuleChainId::new).orElse(rootRuleChainId);

                                                    if (rootRuleChainId == null || stateStore.isEmpty()) {
                                                        continue;
                                                    }

                                                    List<JsonNode> states = alarmRuleService.findRuleNodeStatesByRuleChainIdAndType(new DeviceProfileId(deviceProfile.getId()), ruleChainId, "org.thingsboard.rule.engine.action.TbDeviceProfileNode");
                                                    states.forEach(stateNode -> {
                                                        Map<String, PersistedAlarmState> alarmStates = new HashMap<>();
                                                        DeviceId deviceId = new DeviceId(UUID.fromString(stateNode.get("entity_id").asText()));
                                                        RuleNodeId ruleNodeId = new RuleNodeId(UUID.fromString(stateNode.get("rule_node_id").asText()));
                                                        boolean debugMode = stateNode.get("debug_mode").asBoolean();

                                                        PersistedEntityState persistedEntityState = JacksonUtil.fromString(stateNode.get("state_data").asText(), PersistedEntityState.class);
                                                        persistedEntityState.setTenantId(tenantId);
                                                        persistedEntityState.setEntityId(deviceId);

                                                        persistedEntityState.getAlarmStates().forEach((id, alarmState) -> {
                                                            String newId = alarmRuleIdMapping.get(id);
                                                            alarmState.setLastRuleNodeId(ruleNodeId);
                                                            alarmState.setLastRuleChainId(ruleChainId);
                                                            alarmState.setLastRuleNodeDebugMode(debugMode);
                                                            alarmStates.put(newId, alarmState);
                                                        });

                                                        persistedEntityState.setAlarmStates(alarmStates);

                                                        stateStore.get().put(persistedEntityState);
                                                    });
                                                }
                                            } catch (Exception e) {
                                            }

                                            profilePageLink = profilePageLink.nextPageLink();
                                        } while (profiles.hasNext());
                                    } catch (Exception e) {
                                    }
                                }));
                            }
                            Futures.allAsList(futures).get();
                            pageLink = pageLink.nextPageLink();
                        } while (tenantIds.hasNext());

                        log.info("Updating device profile nodes...");

                        try {
                            conn.createStatement().execute("UPDATE rule_node rn SET type = 'org.thingsboard.rule.engine.action.TbAlarmRulesNode', configuration = '{}' WHERE rn.type = 'org.thingsboard.rule.engine.profile.TbDeviceProfileNode';");
                        } catch (Exception e) {
                        }

                        try {
                            conn.createStatement().execute("UPDATE rule_node rn SET name = 'Alarm Rules Node', search_text = 'alarm rules node' WHERE rn.type = 'org.thingsboard.rule.engine.action.TbAlarmRulesNode' AND rn.name = 'Device Profile Node';");
                        } catch (Exception e) {
                        }

                        try {
                            conn.createStatement().execute("DROP TABLE IF EXISTS rule_node_state;");
                        } catch (Exception e) {
                        }

                        log.info("Updating device profiles...");
                        try {

                        } catch (Exception e) {
                            conn.createStatement().execute("UPDATE device_profile d SET profile_data = d.profile_data - 'alarms';");
                        }

                        log.info("Updating schema settings...");
                        conn.createStatement().execute("UPDATE tb_schema_settings SET schema_version = 3005000;");
                    }
                    log.info("Schema updated.");
                } catch (Exception e) {
                    log.error("Failed updating schema!!!", e);
                }
                break;
            default:
                throw new RuntimeException("Unable to upgrade SQL database, unsupported fromVersion: " + fromVersion);
        }
    }

    private void runSchemaUpdateScript(Connection connection, String version) throws Exception {
        Path schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", version, SCHEMA_UPDATE_SQL);
        loadSql(schemaUpdateFile, connection);
    }

    private void loadSql(Path sqlFile, Connection conn) throws Exception {
        String sql = new String(Files.readAllBytes(sqlFile), Charset.forName("UTF-8"));
        Statement st = conn.createStatement();
        st.setQueryTimeout((int) TimeUnit.HOURS.toSeconds(3));
        st.execute(sql);//NOSONAR, ignoring because method used to execute thingsboard database upgrade script
        printWarnings(st);
        Thread.sleep(5000);
    }

    protected void printWarnings(Statement statement) throws SQLException {
        SQLWarning warnings = statement.getWarnings();
        if (warnings != null) {
            log.info("{}", warnings.getMessage());
            SQLWarning nextWarning = warnings.getNextWarning();
            while (nextWarning != null) {
                log.info("{}", nextWarning.getMessage());
                nextWarning = nextWarning.getNextWarning();
            }
        }
    }

    protected boolean isOldSchema(Connection conn, long fromVersion) {
        boolean isOldSchema = true;
        try {
            Statement statement = conn.createStatement();
            statement.execute("CREATE TABLE IF NOT EXISTS tb_schema_settings ( schema_version bigint NOT NULL, CONSTRAINT tb_schema_settings_pkey PRIMARY KEY (schema_version));");
            Thread.sleep(1000);
            ResultSet resultSet = statement.executeQuery("SELECT schema_version FROM tb_schema_settings;");
            if (resultSet.next()) {
                isOldSchema = resultSet.getLong(1) <= fromVersion;
            } else {
                resultSet.close();
                statement.execute("INSERT INTO tb_schema_settings (schema_version) VALUES (" + fromVersion + ")");
            }
            statement.close();
        } catch (InterruptedException | SQLException e) {
            log.info("Failed to check current PostgreSQL schema due to: {}", e.getMessage());
        }
        return isOldSchema;
    }

    private Queue queueConfigToQueue(TbRuleEngineQueueConfiguration queueSettings) {
        Queue queue = new Queue();
        queue.setTenantId(TenantId.SYS_TENANT_ID);
        queue.setName(queueSettings.getName());
        queue.setTopic(queueSettings.getTopic());
        queue.setPollInterval(queueSettings.getPollInterval());
        queue.setPartitions(queueSettings.getPartitions());
        queue.setPackProcessingTimeout(queueSettings.getPackProcessingTimeout());
        SubmitStrategy submitStrategy = new SubmitStrategy();
        submitStrategy.setBatchSize(queueSettings.getSubmitStrategy().getBatchSize());
        submitStrategy.setType(SubmitStrategyType.valueOf(queueSettings.getSubmitStrategy().getType()));
        queue.setSubmitStrategy(submitStrategy);
        ProcessingStrategy processingStrategy = new ProcessingStrategy();
        processingStrategy.setType(ProcessingStrategyType.valueOf(queueSettings.getProcessingStrategy().getType()));
        processingStrategy.setRetries(queueSettings.getProcessingStrategy().getRetries());
        processingStrategy.setFailurePercentage(queueSettings.getProcessingStrategy().getFailurePercentage());
        processingStrategy.setPauseBetweenRetries(queueSettings.getProcessingStrategy().getPauseBetweenRetries());
        processingStrategy.setMaxPauseBetweenRetries(queueSettings.getProcessingStrategy().getMaxPauseBetweenRetries());
        queue.setProcessingStrategy(processingStrategy);
        queue.setConsumerPerPartition(queueSettings.isConsumerPerPartition());
        return queue;
    }

}
