/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
