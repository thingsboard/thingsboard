// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.install;

import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

abstract class AbstractInstallPostgresTest {

    private static final String POSTGRES_IMAGE = "postgres:18";

    protected static PostgreSQLContainer<?> postgres;

    @BeforeAll
    static void startDatabase() {
        if (postgres == null) {
            postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE);
            postgres.start();
        }
    }

    protected static void execute(String sql) throws SQLException {
        try (Connection conn = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement statement = conn.createStatement()) {
            statement.execute(sql);
        }
    }

}
