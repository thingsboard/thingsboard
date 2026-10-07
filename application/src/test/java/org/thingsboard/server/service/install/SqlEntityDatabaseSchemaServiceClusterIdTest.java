// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.install;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SqlEntityDatabaseSchemaServiceClusterIdTest extends AbstractPostgresContainerTest {

    private SqlEntityDatabaseSchemaService schemaService;

    @BeforeEach
    void setUp() throws Exception {
        execute("DROP TABLE IF EXISTS tb_cluster;");
        for (String statement : productionClusterDdl()) {
            execute(statement);
        }
        schemaService = new SqlEntityDatabaseSchemaService();
        ReflectionTestUtils.setField(schemaService, "dbUrl", POSTGRES.getJdbcUrl());
        ReflectionTestUtils.setField(schemaService, "dbUserName", POSTGRES.getUsername());
        ReflectionTestUtils.setField(schemaService, "dbPassword", POSTGRES.getPassword());
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

    // The race the "ON CONFLICT DO NOTHING" in the mint statement exists for: two installs run at once,
    // both find the table empty, and both attempt the insert. The loser must come back as a silent no-op -
    // without the clause it fails on the single-row unique index and aborts an install that has nothing
    // wrong with it.
    @Test
    void testAConcurrentInsertThatLosesTheRaceIsACleanNoOp() throws Exception {
        UUID winner = UUID.randomUUID();
        UUID loser = UUID.randomUUID();
        try (Connection winnerConnection = newConnection();
             Connection loserConnection = newConnection()) {
            winnerConnection.setAutoCommit(false);
            loserConnection.setAutoCommit(false);

            assertThat(mint(winnerConnection, winner)).isOne();

            // Started while the winning row is still uncommitted, so this statement's own "WHERE NOT EXISTS"
            // sees an empty table and proceeds to the insert, where it parks on the unique index until the
            // winner commits - exactly what two simultaneous installs do.
            CompletableFuture<Integer> losingInsert = CompletableFuture.supplyAsync(() -> {
                try {
                    return mint(loserConnection, loser);
                } catch (SQLException e) {
                    throw new IllegalStateException(e);
                }
            });
            // Give the losing insert time to reach the index before the winner commits; if it has not, the
            // assertions below still hold, they just stop exercising the race.
            Thread.sleep(500);
            winnerConnection.commit();

            assertThat(losingInsert.get(30, TimeUnit.SECONDS)).isZero();
            loserConnection.commit();
        }
        assertThat(readClusterIds()).containsExactly(winner);
    }

    private static int mint(Connection connection, UUID clusterId) throws SQLException {
        try (PreparedStatement statement =
                     connection.prepareStatement(TbClusterSchema.INSERT_CLUSTER_ID_QUERY)) {
            statement.setString(1, clusterId.toString());
            return statement.executeUpdate();
        }
    }

    @Test
    void testConnectionFailurePropagatesInsteadOfBeingSwallowed() {
        ReflectionTestUtils.setField(schemaService, "dbUrl", "jdbc:postgresql://localhost:1/does-not-exist");

        assertThatThrownBy(() -> schemaService.generateClusterIdIfNotExist())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to generate Cluster id")
                .hasCauseInstanceOf(SQLException.class);
    }

    /** The production DDL, not retyped: LtsMigrationIntegrationTest pins these constants against schema-entities.sql. */
    private static List<String> productionClusterDdl() {
        return List.of(TbClusterSchema.CREATE_CLUSTER_TABLE_QUERY, TbClusterSchema.CREATE_CLUSTER_SINGLE_ROW_INDEX_QUERY);
    }

    private static List<UUID> readClusterIds() throws SQLException {
        List<UUID> ids = new ArrayList<>();
        try (Connection conn = newConnection();
             Statement statement = conn.createStatement();
             ResultSet rs = statement.executeQuery("SELECT cluster_id FROM tb_cluster ORDER BY cluster_id")) {
            while (rs.next()) {
                ids.add(rs.getObject("cluster_id", UUID.class));
            }
        }
        return ids;
    }
}
