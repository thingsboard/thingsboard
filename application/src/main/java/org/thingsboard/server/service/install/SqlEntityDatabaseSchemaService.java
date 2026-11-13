// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.install;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

@Service
@Profile("install")
@Slf4j
public class SqlEntityDatabaseSchemaService extends SqlAbstractDatabaseSchemaService implements EntityDatabaseSchemaService {

    public static final String SCHEMA_ENTITIES_SQL = "schema-entities.sql";
    public static final String SCHEMA_ENTITIES_IDX_SQL = "schema-entities-idx.sql";
    public static final String SCHEMA_ENTITIES_IDX_PSQL_ADDON_SQL = "schema-entities-idx-psql-addon.sql";
    public static final String SCHEMA_VIEWS_SQL = "schema-views.sql";
    public static final String SCHEMA_FUNCTIONS_SQL = "schema-functions.sql";

    public SqlEntityDatabaseSchemaService() {
        super(SCHEMA_ENTITIES_SQL, SCHEMA_ENTITIES_IDX_SQL);
    }

    @Override
    public void createDatabaseIndexes() throws Exception {
        super.createDatabaseIndexes();
        log.info("Installing SQL DataBase schema PostgreSQL specific indexes part: " + SCHEMA_ENTITIES_IDX_PSQL_ADDON_SQL);
        executeQueryFromFile(SCHEMA_ENTITIES_IDX_PSQL_ADDON_SQL);
    }

    @Override
    public void createOrUpdateDeviceInfoView(boolean activityStateInTelemetry) {
        String sourceViewName = activityStateInTelemetry ? "device_info_active_ts_view" : "device_info_active_attribute_view";
        executeQuery("DROP VIEW IF EXISTS device_info_view CASCADE;");
        executeQuery("CREATE OR REPLACE VIEW device_info_view AS SELECT * FROM " + sourceViewName + ";");
    }

    @Override
    public void createOrUpdateViewsAndFunctions() throws Exception {
        log.info("Installing SQL DataBase schema views: " + SCHEMA_VIEWS_SQL);
        executeQueryFromFile(SCHEMA_VIEWS_SQL);
        log.info("Installing SQL DataBase schema functions: " + SCHEMA_FUNCTIONS_SQL);
        executeQueryFromFile(SCHEMA_FUNCTIONS_SQL);
    }

    @Override
    public void generateClusterIdIfNotExist() {
        var clusterId = UUID.randomUUID();
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword);
             PreparedStatement statement = conn.prepareStatement(
                     "INSERT INTO tb_cluster (cluster_id) SELECT ?::uuid WHERE NOT EXISTS (SELECT 1 FROM tb_cluster) ON CONFLICT DO NOTHING")) {
            statement.setString(1, clusterId.toString());
            int insertedRows = statement.executeUpdate();
            UUID storedClusterId = insertedRows > 0 ? clusterId : readClusterId(conn);
            if (storedClusterId != null) {
                logClusterId(storedClusterId);
            }
        } catch (SQLException e) {
            log.error("Failed to generate Cluster id", e);
            throw new RuntimeException("Failed to generate Cluster id", e);
        }
    }

    private UUID readClusterId(Connection conn) throws SQLException {
        try (Statement statement = conn.createStatement();
             ResultSet rs = statement.executeQuery("SELECT cluster_id FROM tb_cluster")) {
            return rs.next() ? rs.getObject(1, UUID.class) : null;
        }
    }

    private void logClusterId(UUID clusterId) {
        String line = ":: Cluster Id: " + clusterId + " ::";
        String border = "=".repeat(line.length());
        // Logged at JVM shutdown so it follows the rest of the install output.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> log.info("\n{}\n{}\n{}\n", border, line, border)));
    }

}
