// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.install;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.service.install.InstallTestSupport.dataDir;
import static org.thingsboard.server.service.install.InstallTestSupport.readFile;
import static org.thingsboard.server.service.install.InstallTestSupport.readResource;
import static org.thingsboard.server.service.install.InstallTestSupport.tbClusterStatements;

/**
 * When {@code tb_cluster} changes, put the {@code ALTER} in a new upgrade script and point this test at it:
 * the 4.3.1.6 script is {@code IF NOT EXISTS}, so editing it is a no-op on databases that already ran it.
 */
class TbClusterDdlDriftTest {

    @Test
    void testLtsSchemaUpdateTbClusterDdlMatchesSchemaEntities() {
        String schemaEntities = "sql/" + SqlEntityDatabaseSchemaService.SCHEMA_ENTITIES_SQL;
        List<String> fromSchemaEntities = normalized(tbClusterStatements(readResource(schemaEntities)));
        List<String> fromLtsScript = normalized(tbClusterStatements(readFile(
                dataDir().resolve(Paths.get("upgrade", "lts", "4.3.1.6", "schema_update.sql")))));

        assertThat(fromSchemaEntities).as("tb_cluster statements found in %s", schemaEntities).hasSize(2);
        assertThat(fromLtsScript)
                .as("tb_cluster DDL in the 4.3.1.6 upgrade script matches %s", schemaEntities)
                .isEqualTo(fromSchemaEntities);
    }

    private static List<String> normalized(List<String> statements) {
        return statements.stream().map(statement -> statement.replaceAll("\\s+", " ").trim()).toList();
    }

}
