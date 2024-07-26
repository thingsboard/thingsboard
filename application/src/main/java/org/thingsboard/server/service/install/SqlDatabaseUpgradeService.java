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
package org.thingsboard.server.service.install;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.alarm.rule.utils.AlarmRuleMigrator;
import org.thingsboard.server.common.data.device.profile.alarm.rule.DeviceProfileAlarm;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.dao.alarm.rule.AlarmRuleDao;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.service.alarm.rule.state.PersistedAlarmState;
import org.thingsboard.server.service.alarm.rule.state.PersistedEntityState;
import org.thingsboard.server.service.alarm.rule.store.RedisAlarmRuleEntityStateStore;
import org.thingsboard.server.service.install.update.DefaultDataUpdateService;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.thingsboard.server.common.data.DataConstants.ALARM_RULES_NODE_TYPE;
import static org.thingsboard.server.common.data.DataConstants.DEVICE_PROFILE_NODE_TYPE;

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
    private TenantService tenantService;

    @Autowired
    private InstallScripts installScripts;

    @Autowired
    private AlarmRuleDao alarmRuleService;

    @Autowired
    private Optional<RedisAlarmRuleEntityStateStore> stateStore;

    @Autowired
    private RuleChainService ruleChainService;

    @Autowired
    private DbUpgradeExecutorService dbUpgradeExecutor;

    @Autowired
    private DeviceProfileService deviceProfileService;

    @Override
    public void upgradeDatabase(String fromVersion) throws Exception {
        switch (fromVersion) {
            case "3.5.0":
                updateSchema("3.5.0", 3005000, "3.5.1", 3005001, null);
                break;
            case "3.5.1":
                updateSchema("3.5.1", 3005001, "3.6.0", 3006000, conn -> {
                    String[] entityNames = new String[]{"device", "component_descriptor", "customer", "dashboard", "rule_chain", "rule_node", "ota_package", "asset_profile", "asset", "device_profile", "tb_user", "tenant_profile", "tenant", "widgets_bundle", "entity_view", "edge"};
                    for (String entityName : entityNames) {
                        try {
                            conn.createStatement().execute("ALTER TABLE " + entityName + " DROP COLUMN search_text CASCADE");
                        } catch (Exception e) {
                        }
                    }
                    try {
                        conn.createStatement().execute("ALTER TABLE component_descriptor ADD COLUMN IF NOT EXISTS configuration_version int DEFAULT 0;");
                    } catch (Exception e) {
                    }
                    try {
                        conn.createStatement().execute("ALTER TABLE rule_node ADD COLUMN IF NOT EXISTS configuration_version int DEFAULT 0;");
                    } catch (Exception e) {
                    }
                    try {
                        conn.createStatement().execute("CREATE INDEX IF NOT EXISTS idx_rule_node_type_configuration_version ON rule_node(type, configuration_version);");
                    } catch (Exception e) {
                    }
                    try {
                        conn.createStatement().execute("UPDATE rule_node SET " + "configuration = (configuration::jsonb || '{\"updateAttributesOnlyOnValueChange\": \"false\"}'::jsonb)::varchar, " + "configuration_version = 1 " + "WHERE type = 'org.thingsboard.rule.engine.telemetry.TbMsgAttributesNode' AND configuration_version < 1;");
                    } catch (Exception e) {
                    }
                    try {
                        conn.createStatement().execute("CREATE INDEX IF NOT EXISTS idx_notification_recipient_id_unread ON notification(recipient_id) WHERE status <> 'READ';");
                    } catch (Exception e) {
                    }
                });
                break;
            case "3.6.0":
                updateSchema("3.6.0", 3006000, "3.6.1", 3006001, null);
                break;
            case "3.6.1":
                updateSchema("3.6.1", 3006001, "3.6.2", 3006002, connection -> {
                    try {
                        Path saveAttributesNodeUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "3.6.1", "save_attributes_node_update.sql");
                        loadSql(saveAttributesNodeUpdateFile, connection);
                    } catch (Exception e) {
                        log.warn("Failed to execute update script for save attributes rule nodes due to: ", e);
                    }
                    try {
                        connection.createStatement().execute("CREATE INDEX IF NOT EXISTS idx_asset_profile_id ON asset(tenant_id, asset_profile_id);");
                    } catch (Exception e) {
                    }
                });
                break;
            case "3.6.2":
                updateSchema("3.6.2", 3006002, "3.6.3", 3006003, null);
                break;
            case "3.6.3":
                updateSchema("3.6.3", 3006003, "3.6.4", 3006004, null);
                break;
            case "3.6.4":
                updateSchema("3.6.4", 3006004, "3.7.0", 3007000, null);
                break;
            case "3.7.0":
                updateSchema("3.7.0", 3007000, "3.7.1", 3007001, null);
                break;
            case "3.7.1":
                updateSchema("3.7.1", 3007001, "3.8.0", 3008000, conn -> {
                    log.info("Alarm Rules migration ...");

                    List<ListenableFuture<?>> futures = new ArrayList<>();
                    PageDataIterable<TenantId> tenantIds = new PageDataIterable<>(pageLink -> tenantService.findTenantsIds(pageLink), 1024);
                    for (TenantId tenantId : tenantIds) {
                        futures.add(dbUpgradeExecutor.submit(() -> {
                            PageLink profilePageLink = new PageLink(1000);
                            PageData<DeviceProfile> profiles;
                            do {
                                profiles = deviceProfileService.findDeviceProfilesWithAlarmRules(tenantId, profilePageLink);

                                try {
                                    RuleChainId rootRuleChainId;
                                    try {
                                        rootRuleChainId = Optional.ofNullable(ruleChainService.getRootTenantRuleChain(tenantId)).map(RuleChain::getId).orElse(null);
                                    } catch (Exception e) {
                                        rootRuleChainId = null;
                                    }

                                    for (DeviceProfile deviceProfile : profiles.getData()) {
                                        Map<String, String> alarmRuleIdMapping = new HashMap<>();
                                        DeviceProfileId deviceProfileId = deviceProfile.getId();
                                        for (DeviceProfileAlarm alarm : deviceProfile.getProfileData().getAlarms()) {
                                            AlarmRule savedRule = alarmRuleService.save(tenantId, AlarmRuleMigrator.migrate(tenantId, deviceProfile, alarm));
                                            alarmRuleIdMapping.put(alarm.getId(), savedRule.getId().toString());
                                        }

                                        deviceProfile.getProfileData().setAlarms(null);
                                        deviceProfileService.saveDeviceProfile(deviceProfile);

                                        RuleChainId ruleChainId = Optional.ofNullable(deviceProfile.getDefaultRuleChainId()).orElse(rootRuleChainId);

                                        if (ruleChainId == null || stateStore.isEmpty()) {
                                            continue;
                                        }

                                        List<JsonNode> states = alarmRuleService.findRuleNodeStatesByRuleChainIdAndType(deviceProfileId, ruleChainId, DEVICE_PROFILE_NODE_TYPE);
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
                                    log.warn("Failed to migrate AlarmRules or Persisted States!", e);
                                }
                            } while (profiles.hasNext());
                        }));
                    }

                    try {
                        Futures.allAsList(futures).get();
                    } catch (InterruptedException | ExecutionException e) {
                        log.warn("Failed to await rules migration!!!", e);
                    }

                    log.info("Updating device profile nodes...");

                    try {
                        conn.createStatement().execute(String.format("UPDATE rule_node rn SET type = '%s' WHERE rn.type = '%s';", ALARM_RULES_NODE_TYPE, DEVICE_PROFILE_NODE_TYPE));
                    } catch (Exception e) {
                    }

                    try {
                        conn.createStatement().execute("DROP TABLE IF EXISTS rule_node_state;");
                    } catch (Exception e) {
                    }
                });
                break;
            default:
                throw new RuntimeException("Unable to upgrade SQL database, unsupported fromVersion: " + fromVersion);
        }
    }

    private void updateSchema(String oldVersionStr, int oldVersion, String newVersionStr, int newVersion, Consumer<Connection> additionalAction) {
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
            log.info("Updating schema ...");
            if (isOldSchema(conn, oldVersion)) {
                Path schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", oldVersionStr, SCHEMA_UPDATE_SQL);
                loadSql(schemaUpdateFile, conn);
                if (additionalAction != null) {
                    additionalAction.accept(conn);
                }
                conn.createStatement().execute("UPDATE tb_schema_settings SET schema_version = " + newVersion + ";");
                log.info("Schema updated to version {}", newVersionStr);
            } else {
                log.info("Skip schema re-update to version {}. Use env flag 'SKIP_SCHEMA_VERSION_CHECK' to force the re-update.", newVersionStr);
            }
        } catch (Exception e) {
            log.error("Failed updating schema!!!", e);
        }
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
        if (DefaultDataUpdateService.getEnv("SKIP_SCHEMA_VERSION_CHECK", false)) {
            log.info("Skipped DB schema version check due to SKIP_SCHEMA_VERSION_CHECK set to true!");
            return true;
        }
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

}
