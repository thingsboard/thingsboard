// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.install;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.thingsboard.server.service.install.InstallTestSupport.dataDir;
import static org.thingsboard.server.service.install.InstallTestSupport.readResource;

/**
 * {@code basic/schema_update.sql} runs before {@code schema-entities.sql}, so it must not reference a table
 * that only the latter creates. The fixture schema predates {@code tb_cluster}.
 */
class SchemaUpdateSqlUpgradeOrderTest extends AbstractInstallPostgresTest {

    @BeforeEach
    void setUp() throws SQLException {
        execute("DROP SCHEMA public CASCADE; CREATE SCHEMA public;");
        execute(readResource("install/pre-upgrade-schema-entities.sql"));
    }

    @Test
    void testSchemaUpdateRunsCleanlyBeforeTbClusterExists() throws IOException {
        String sql = Files.readString(schemaUpdateSqlFile());

        assertThatCode(() -> execute(sql)).doesNotThrowAnyException();
    }

    @Test
    void testALegitimateStatementAgainstALongStandingTableIsNotFlagged() throws IOException {
        String sql = Files.readString(schemaUpdateSqlFile())
                + "\nALTER TABLE device ADD COLUMN IF NOT EXISTS probe_column varchar(1);\n";

        assertThatCode(() -> execute(sql)).doesNotThrowAnyException();
    }

    @Test
    void testLtsSchemaUpdateCreatesTbClusterBeforeItsIndex() throws IOException {
        String sql = Files.readString(ltsSchemaUpdateSqlFile("4.3.1.6"));

        assertThatCode(() -> execute(sql)).doesNotThrowAnyException();
    }

    private static Path schemaUpdateSqlFile() {
        return dataDir().resolve(Paths.get("upgrade", "basic", "schema_update.sql"));
    }

    private static Path ltsSchemaUpdateSqlFile(String version) {
        return dataDir().resolve(Paths.get("upgrade", "lts", version, "schema_update.sql"));
    }

}
