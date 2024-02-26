/*
Author Ahmet Ertuğrul KAYA
*/
package org.thingsboard.server.update.service;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.queue.*;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.dao.asset.AssetDao;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.service.install.DatabaseEntitiesUpgradeService;
import org.thingsboard.server.service.install.InstallScripts;
import org.thingsboard.server.service.install.SystemDataLoaderService;
import org.thingsboard.server.service.install.update.DefaultDataUpdateService;
import org.thingsboard.server.update.configuration.TbRuleEngineQueueConfiguration;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service
@Profile("update")
@Slf4j
public class PostgreSqlDatabaseUpgradeService implements DatabaseEntitiesUpgradeService {

    private static final String SCHEMA_UPDATE_SQL = "schema_update.sql";

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUserName;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Autowired
    private InstallScripts installScripts;

    @Autowired
    private SystemDataLoaderService systemDataLoaderService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private AssetDao assetDao;

    @Autowired
    private AssetProfileService assetProfileService;

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
            case "3.2.1":
                try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
                    log.info("Updating schema ...");
                    conn.createStatement().execute("CREATE INDEX IF NOT EXISTS idx_audit_log_tenant_id_and_created_time ON audit_log(tenant_id, created_time);");
                    Path schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "3.2.1", SCHEMA_UPDATE_SQL);
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
                    Path schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "3.2.2", SCHEMA_UPDATE_SQL);
                    loadSql(schemaUpdateFile, conn);
                    log.info("Load Edge TTL functions ...");
                    schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "3.2.2", "schema_update_ttl.sql");
                    loadSql(schemaUpdateFile, conn);
                    log.info("Edge TTL functions successfully loaded!");
                    log.info("Updating indexes and TTL procedure for event table...");
                    schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "3.2.2", "schema_update_event.sql");
                    loadSql(schemaUpdateFile, conn);
                    log.info("Updating schema settings...");
                    conn.createStatement().execute("UPDATE tb_schema_settings SET schema_version = 3003002;");
                    log.info("Schema updated.");
                } catch (Exception e) {
                    log.error("Failed updating schema!!!", e);
                }
                break;
            case "3.3.2":
                try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
                    log.info("Updating schema ...");
                    Path schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "3.3.2", SCHEMA_UPDATE_SQL);
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
                    Path schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "3.3.3", "schema_event_ttl_procedure.sql");
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
                    Path schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "3.3.4", SCHEMA_UPDATE_SQL);
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
                    Path schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "3.4.0", SCHEMA_UPDATE_SQL);
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

                        Path schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "3.4.1", "schema_update_before.sql");
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
                        Path schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "3.4.4", SCHEMA_UPDATE_SQL);
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

                        conn.createStatement().execute("UPDATE tb_schema_settings SET schema_version = 3005000;");
                    }
                    log.info("Schema updated.");
                } catch (Exception e) {
                    log.error("Failed updating schema!!!", e);
                }
                break;
            case "3.5.0":
                updateSchema("3.5.0", 3005000, "3.5.1", 3005001, null);
                break;
            case "3.5.1":
                updateSchema("3.5.1", 3005001, "3.6.0", 3006000, conn -> {
                    String[] entityNames = new String[]{"device", "component_descriptor", "customer", "dashboard", "rule_chain", "rule_node", "ota_package",
                            "asset_profile", "asset", "device_profile", "tb_user", "tenant_profile", "tenant", "widgets_bundle", "entity_view", "edge"};
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
                        conn.createStatement().execute("UPDATE rule_node SET " +
                                "configuration = (configuration::jsonb || '{\"updateAttributesOnlyOnValueChange\": \"false\"}'::jsonb)::varchar, " +
                                "configuration_version = 1 " +
                                "WHERE type = 'org.thingsboard.rule.engine.telemetry.TbMsgAttributesNode' AND configuration_version < 1;");
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

    private void runSchemaUpdateScript(Connection connection, String version) throws Exception {
        Path schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", version, SCHEMA_UPDATE_SQL);
        loadSql(schemaUpdateFile, connection);
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
