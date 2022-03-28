/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.dao.service.install.sql;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DaoSqlTest
public class EntitiesSchemaSqlTest extends AbstractServiceTest {

    @Value("${spring.datasource.url}")
    private String dbUrl;
    @Value("${spring.datasource.username}")
    private String dbUserName;
    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${classpath:sql/schema-entities.sql}")
    private Path installScriptPath;

    @Test
    public void testRepeatedInstall() throws IOException {
        String installScript = Files.readString(installScriptPath);
        try (Connection connection = getConnection()) {
            for (int i = 1; i <= 2; i++) {
                connection.createStatement().execute(installScript);
            }
        } catch (SQLException e) {
            Assertions.fail("Failed to execute reinstall", e);
        }
    }

    @Test
    public void testRepeatedInstall_badScript() throws SQLException {
        String illegalInstallScript = "CREATE TABLE IF NOT EXISTS qwerty ();\n" +
                "ALTER TABLE qwerty ADD COLUMN first VARCHAR(10);";

        Connection connection = getConnection();
        try {
            assertDoesNotThrow(() -> {
                connection.createStatement().execute(illegalInstallScript);
            });

            assertThatThrownBy(() -> {
                connection.createStatement().execute(illegalInstallScript);
            }).hasMessageContaining("column").hasMessageContaining("already exists");
        } finally {
            connection.createStatement().execute("DROP TABLE qwerty;");
            connection.close();
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUserName, dbPassword);
    }

}
