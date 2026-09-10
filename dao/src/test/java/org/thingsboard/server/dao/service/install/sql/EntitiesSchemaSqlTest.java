// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.service.install.sql;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DaoSqlTest
public class EntitiesSchemaSqlTest extends AbstractServiceTest {

    @Value("${classpath:sql/schema-entities.sql}")
    private Path installEntitiesPath;
    @Value("${classpath:sql/schema-views.sql}")
    private Path installViewsPath;
    @Value("${classpath:sql/schema-functions.sql}")
    private Path installFunctionsPath;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testRepeatedInstall() throws IOException {
        String entitiesScript = Files.readString(installEntitiesPath);
        String viewsScript = Files.readString(installViewsPath);
        String functionsScript = Files.readString(installFunctionsPath);
        try {
            for (int i = 1; i <= 2; i++) {
                jdbcTemplate.execute(entitiesScript);
                jdbcTemplate.execute(viewsScript);
                jdbcTemplate.execute(functionsScript);
            }
        } catch (Exception e) {
            Assertions.fail("Failed to execute reinstall", e);
        }
    }

    @Test
    public void testRepeatedInstall_badScript() {
        String illegalInstallScript = "CREATE TABLE IF NOT EXISTS qwerty ();\n" +
                "ALTER TABLE qwerty ADD COLUMN first VARCHAR(10);";

        assertDoesNotThrow(() -> {
            jdbcTemplate.execute(illegalInstallScript);
        });

        try {
            assertThatThrownBy(() -> {
                jdbcTemplate.execute(illegalInstallScript);
            }).getCause().hasMessageContaining("column").hasMessageContaining("already exists");
        } finally {
            jdbcTemplate.execute("DROP TABLE qwerty;");
        }
    }

}
