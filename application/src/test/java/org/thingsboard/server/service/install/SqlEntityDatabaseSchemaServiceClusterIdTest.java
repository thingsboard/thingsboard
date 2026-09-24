// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.install;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.thingsboard.server.service.install.InstallTestSupport.readResource;
import static org.thingsboard.server.service.install.InstallTestSupport.tbClusterStatements;

class SqlEntityDatabaseSchemaServiceClusterIdTest extends AbstractInstallPostgresTest {

    private SqlEntityDatabaseSchemaService schemaService;

    @BeforeEach
    void setUp() throws SQLException {
        execute("DROP TABLE IF EXISTS tb_cluster;");
        for (String statement : productionClusterDdl()) {
            execute(statement);
        }
        schemaService = new SqlEntityDatabaseSchemaService();
        ReflectionTestUtils.setField(schemaService, "dbUrl", postgres.getJdbcUrl());
        ReflectionTestUtils.setField(schemaService, "dbUserName", postgres.getUsername());
        ReflectionTestUtils.setField(schemaService, "dbPassword", postgres.getPassword());
    }

    @Test
    void testFreshInstallMintsExactlyOneClusterId() throws SQLException {
        schemaService.generateClusterIdIfNotExist();
        List<UUID> ids = readClusterIds();
        assertThat(ids).hasSize(1);
        assertThat(ids.get(0)).isNotNull();
    }

    @Test
    void testSecondCallIsANoOp() throws SQLException {
        schemaService.generateClusterIdIfNotExist();
        UUID first = readClusterIds().get(0);
        schemaService.generateClusterIdIfNotExist();
        schemaService.generateClusterIdIfNotExist();
        List<UUID> ids = readClusterIds();
        assertThat(ids).containsExactly(first);
    }

    @Test
    void testExistingClusterIdSurvivesAnUpgrade() throws SQLException {
        UUID existing = UUID.randomUUID();
        execute("INSERT INTO tb_cluster (cluster_id) VALUES ('" + existing + "');");
        schemaService.generateClusterIdIfNotExist();
        assertThat(readClusterIds()).containsExactly(existing);
    }

    @Test
    void testSecondRowInsertIsRejectedByUniqueIndex() throws SQLException {
        UUID existing = UUID.randomUUID();
        execute("INSERT INTO tb_cluster (cluster_id) VALUES ('" + existing + "');");

        UUID other = UUID.randomUUID();
        assertThatThrownBy(() -> execute("INSERT INTO tb_cluster (cluster_id) VALUES ('" + other + "');"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("tb_cluster_single_row");

        assertThat(readClusterIds()).containsExactly(existing);
    }

    @Test
    void testConnectionFailurePropagatesInsteadOfBeingSwallowed() {
        ReflectionTestUtils.setField(schemaService, "dbUrl", "jdbc:postgresql://localhost:1/does-not-exist");

        assertThatThrownBy(() -> schemaService.generateClusterIdIfNotExist())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to generate Cluster id")
                .hasCauseInstanceOf(SQLException.class);
    }

    private static List<String> productionClusterDdl() {
        String resourceName = "sql/" + SqlEntityDatabaseSchemaService.SCHEMA_ENTITIES_SQL;
        List<String> statements = tbClusterStatements(readResource(resourceName));
        assertThat(statements).as("tb_cluster DDL found in %s", resourceName).hasSize(2);
        return statements;
    }

    private static List<UUID> readClusterIds() throws SQLException {
        List<UUID> ids = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement statement = conn.createStatement();
             ResultSet rs = statement.executeQuery("SELECT cluster_id FROM tb_cluster ORDER BY cluster_id")) {
            while (rs.next()) {
                ids.add(rs.getObject("cluster_id", UUID.class));
            }
        }
        return ids;
    }
}
