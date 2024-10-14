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

import lombok.extern.slf4j.Slf4j;
import org.intellij.lang.annotations.Language;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.StatementCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.server.service.install.update.DefaultDataUpdateService;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLWarning;
import java.util.concurrent.TimeUnit;

@Service
@Profile("install")
@Slf4j
public class SqlDatabaseUpgradeService implements DatabaseEntitiesUpgradeService {

    private static final String SCHEMA_UPDATE_SQL = "schema_update.sql";

    private final InstallScripts installScripts;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public SqlDatabaseUpgradeService(InstallScripts installScripts, JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager) {
        this.installScripts = installScripts;
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setTimeout((int) TimeUnit.MINUTES.toSeconds(120));
    }

    @Override
    public void upgradeDatabase(String fromVersion) {
        switch (fromVersion) {
            case "3.5.0" -> updateSchema("3.5.0", 3005000, "3.5.1", 3005001);
            case "3.5.1" -> {
                updateSchema("3.5.1", 3005001, "3.6.0", 3006000);

                String[] tables = new String[]{"device", "component_descriptor", "customer", "dashboard", "rule_chain", "rule_node", "ota_package",
                        "asset_profile", "asset", "device_profile", "tb_user", "tenant_profile", "tenant", "widgets_bundle", "entity_view", "edge"};
                for (String table : tables) {
                    execute("ALTER TABLE " + table + " DROP COLUMN IF EXISTS search_text CASCADE");
                }
                execute(
                        "ALTER TABLE component_descriptor ADD COLUMN IF NOT EXISTS configuration_version int DEFAULT 0;",
                        "ALTER TABLE rule_node ADD COLUMN IF NOT EXISTS configuration_version int DEFAULT 0;",
                        "CREATE INDEX IF NOT EXISTS idx_rule_node_type_configuration_version ON rule_node(type, configuration_version);",
                        "UPDATE rule_node SET " +
                                "configuration = (configuration::jsonb || '{\"updateAttributesOnlyOnValueChange\": \"false\"}'::jsonb)::varchar, " +
                                "configuration_version = 1 " +
                                "WHERE type = 'org.thingsboard.rule.engine.telemetry.TbMsgAttributesNode' AND configuration_version < 1;",
                        "CREATE INDEX IF NOT EXISTS idx_notification_recipient_id_unread ON notification(recipient_id) WHERE status <> 'READ';"
                );
            }
            case "3.6.0" -> updateSchema("3.6.0", 3006000, "3.6.1", 3006001);
            case "3.6.1" -> {
                updateSchema("3.6.1", 3006001, "3.6.2", 3006002);

                try {
                    Path saveAttributesNodeUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "3.6.1", "save_attributes_node_update.sql");
                    loadSql(saveAttributesNodeUpdateFile);
                } catch (Exception e) {
                    log.warn("Failed to execute update script for save attributes rule nodes due to: ", e);
                }
                execute("CREATE INDEX IF NOT EXISTS idx_asset_profile_id ON asset(tenant_id, asset_profile_id);");
            }
            case "3.6.2" -> updateSchema("3.6.2", 3006002, "3.6.3", 3006003);
            case "3.6.3" -> updateSchema("3.6.3", 3006003, "3.6.4", 3006004);
            case "3.6.4" -> updateSchema("3.6.4", 3006004, "3.7.0", 3007000);
            case "3.7.0" -> {
                updateSchema("3.7.0", 3007000, "3.8.0", 3008000);

                try {
                    execute("UPDATE rule_node SET " +
                            "configuration = CASE " +
                            "  WHEN (configuration::jsonb ->> 'persistAlarmRulesState') = 'false'" +
                            "  THEN (configuration::jsonb || '{\"fetchAlarmRulesStateOnStart\": \"false\"}'::jsonb)::varchar " +
                            "  ELSE configuration " +
                            "END, " +
                            "configuration_version = 1 " +
                            "WHERE type = 'org.thingsboard.rule.engine.profile.TbDeviceProfileNode' " +
                            "AND configuration_version < 1;", false);
                } catch (Exception e) {
                    log.warn("Failed to execute update script for device profile rule nodes due to: ", e);
                }
            }
            case "3.8.1" -> updateSchema("3.8.1", 3008001, "3.9.0", 3009000);
            default -> throw new RuntimeException("Unsupported fromVersion '" + fromVersion + "'");
        }
    }

    private void updateSchema(String oldVersionStr, int oldVersion, String newVersionStr, int newVersion) {
        try {
            transactionTemplate.executeWithoutResult(ts -> {
                log.info("Updating schema ...");
                if (isOldSchema(oldVersion)) {
                    loadSql(getSchemaUpdateFile(oldVersionStr));
                    jdbcTemplate.execute("UPDATE tb_schema_settings SET schema_version = " + newVersion);
                    log.info("Schema updated to version {}", newVersionStr);
                } else {
                    log.info("Skip schema re-update to version {}. Use env flag 'SKIP_SCHEMA_VERSION_CHECK' to force the re-update.", newVersionStr);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to update schema", e);
        }
    }

    private Path getSchemaUpdateFile(String version) {
        return Paths.get(installScripts.getDataDir(), "upgrade", version, SCHEMA_UPDATE_SQL);
    }

    private void loadSql(Path sqlFile) {
        String sql;
        try {
            sql = Files.readString(sqlFile);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        jdbcTemplate.execute((StatementCallback<Object>) stmt -> {
            stmt.execute(sql);
            printWarnings(stmt.getWarnings());
            return null;
        });
    }

    private void execute(@Language("sql") String... statements) {
        for (String statement : statements) {
            execute(statement, true);
        }
    }

    private void execute(@Language("sql") String statement, boolean ignoreErrors) {
        try {
            jdbcTemplate.execute(statement);
        } catch (Exception e) {
            if (!ignoreErrors) {
                throw e;
            }
        }
    }

    private void printWarnings(SQLWarning warnings) {
        if (warnings != null) {
            log.info("{}", warnings.getMessage());
            SQLWarning nextWarning = warnings.getNextWarning();
            while (nextWarning != null) {
                log.info("{}", nextWarning.getMessage());
                nextWarning = nextWarning.getNextWarning();
            }
        }
    }

    private boolean isOldSchema(long fromVersion) {
        if (DefaultDataUpdateService.getEnv("SKIP_SCHEMA_VERSION_CHECK", false)) {
            log.info("Skipped DB schema version check due to SKIP_SCHEMA_VERSION_CHECK set to true!");
            return true;
        }
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS tb_schema_settings (schema_version bigint NOT NULL, CONSTRAINT tb_schema_settings_pkey PRIMARY KEY (schema_version))");
        Long schemaVersion = jdbcTemplate.queryForList("SELECT schema_version FROM tb_schema_settings", Long.class).stream().findFirst().orElse(null);
        boolean isOldSchema = true;
        if (schemaVersion != null) {
            isOldSchema = schemaVersion <= fromVersion;
        } else {
            jdbcTemplate.execute("INSERT INTO tb_schema_settings (schema_version) VALUES (" + fromVersion + ")");
        }
        return isOldSchema;
    }

}
