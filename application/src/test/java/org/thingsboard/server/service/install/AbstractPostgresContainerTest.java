// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.install;

import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Base for the install-time tests that need a real PostgreSQL to run raw DDL against.
 * <p>
 * The container is a static field, so a forked test JVM starts one instance no matter how many of these
 * classes it runs - which is the whole point of the base class, since every one of them otherwise pays a
 * full container bring-up. {@link #startDatabase()} is idempotent for that reason, and nothing stops the
 * container: Testcontainers' own reaper removes it when the JVM exits, the same way
 * {@code AbstractCitusContainerTest} handles its cluster.
 * <p>
 * The base deliberately owns no {@code @BeforeEach}: subclasses reset the schema differently (one drops the
 * whole {@code public} schema, another only {@code tb_cluster}), and a shared reset would have to be the
 * union of both, which is slower and hides what each test actually needs.
 */
public abstract class AbstractPostgresContainerTest {

    protected static final String POSTGRES_IMAGE = "postgres:18";

    protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGRES_IMAGE);

    @BeforeAll
    static void startDatabase() {
        if (!POSTGRES.isRunning()) {
            POSTGRES.start();
        }
    }

    protected static Connection newConnection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    protected static void execute(String sql) throws SQLException {
        try (Connection connection = newConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
