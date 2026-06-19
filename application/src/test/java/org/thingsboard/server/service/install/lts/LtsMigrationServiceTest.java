/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.install.lts;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.thingsboard.server.service.install.DatabaseSchemaSettingsService;
import org.thingsboard.server.service.install.InstallScripts;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LtsMigrationServiceTest {

    private JdbcTemplate jdbcTemplate;
    private InstallScripts installScripts;
    private DatabaseSchemaSettingsService schemaSettingsService;
    private PlatformTransactionManager txManager;

    @TempDir
    Path dataDir;

    @BeforeEach
    void setUp() {
        jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        installScripts = Mockito.mock(InstallScripts.class);
        schemaSettingsService = Mockito.mock(DatabaseSchemaSettingsService.class);
        txManager = Mockito.mock(PlatformTransactionManager.class);
        when(txManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(installScripts.getDataDir()).thenReturn(dataDir.toString());
    }

    private void writeSql(String version, String sql) throws Exception {
        Path dir = dataDir.resolve("upgrade").resolve("lts").resolve(version);
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("schema_update.sql"), sql);
    }

    /** Records which apply() hooks fired, in order. */
    private LtsMigration migration(String version, List<String> applied) {
        return new LtsMigration() {
            @Override public String getVersion() { return version; }
            @Override public void apply() { applied.add(version); }
        };
    }

    private LtsMigrationService service(List<LtsMigration> migrations) {
        return new LtsMigrationService(jdbcTemplate, installScripts, schemaSettingsService, txManager, migrations);
    }

    @Test
    void selectsOnlyInRangeMigrationsInAscendingOrder() throws Exception {
        List<String> applied = new ArrayList<>();
        writeSql("4.2.2.3", "SELECT 1;");
        writeSql("4.2.2.4", "SELECT 2;");
        // intentionally unsorted input; service must sort ascending
        LtsMigrationService service = service(List.of(
                migration("4.2.2.4", applied),
                migration("4.2.2.2", applied),
                migration("4.2.2.3", applied)));

        service.applyMigrations("4.2.2.2", "4.2.2.3");

        // only 4.2.2.3 is in (4.2.2.2, 4.2.2.3]
        assertEquals(List.of("4.2.2.3"), applied);
        verify(jdbcTemplate).execute("SELECT 1;");
        verify(jdbcTemplate, never()).execute("SELECT 2;");
        verify(schemaSettingsService).updateSchemaVersion("4.2.2.3");
    }

    @Test
    void appliesAllInRangeAndRecordsEachVersion() throws Exception {
        List<String> applied = new ArrayList<>();
        writeSql("4.2.2.3", "SELECT 1;");
        writeSql("4.2.2.4", "SELECT 2;");
        LtsMigrationService service = service(List.of(
                migration("4.2.2.3", applied), migration("4.2.2.4", applied)));

        service.applyMigrations("4.2.2.2", "4.2.2.4");

        assertEquals(List.of("4.2.2.3", "4.2.2.4"), applied);
        verify(jdbcTemplate).execute("SELECT 1;");
        verify(jdbcTemplate).execute("SELECT 2;");
        verify(schemaSettingsService).updateSchemaVersion("4.2.2.3");
        verify(schemaSettingsService).updateSchemaVersion("4.2.2.4");
    }

    @Test
    void reRunAtCurrentVersionIsNoOp() throws Exception {
        List<String> applied = new ArrayList<>();
        writeSql("4.2.2.3", "SELECT 1;");
        LtsMigrationService service = service(List.of(migration("4.2.2.3", applied)));

        service.applyMigrations("4.2.2.3", "4.2.2.3");

        assertEquals(List.of(), applied);
        verify(jdbcTemplate, never()).execute(anyString());
        verify(schemaSettingsService, never()).updateSchemaVersion(anyString());
    }

    @Test
    void runSchemaMigrationsRunsSqlButNeverAppliesOrRecords() throws Exception {
        List<String> applied = new ArrayList<>();
        writeSql("4.2.2.3", "SELECT 1;");
        LtsMigrationService service = service(List.of(migration("4.2.2.3", applied)));

        service.runSchemaMigrations("4.2.2.2", "4.2.2.3");

        verify(jdbcTemplate).execute("SELECT 1;");
        assertEquals(List.of(), applied);
        verify(schemaSettingsService, never()).updateSchemaVersion(anyString());
    }

    @Test
    void runDataMigrationsAppliesButNeverRunsSqlOrRecords() throws Exception {
        List<String> applied = new ArrayList<>();
        writeSql("4.2.2.3", "SELECT 1;");
        LtsMigrationService service = service(List.of(migration("4.2.2.3", applied)));

        service.runDataMigrations("4.2.2.2", "4.2.2.3");

        assertEquals(List.of("4.2.2.3"), applied);
        verify(jdbcTemplate, never()).execute(anyString());
        verify(schemaSettingsService, never()).updateSchemaVersion(anyString());
    }

    @Test
    void migrationWithoutSqlFileStillAppliesAndRecords() {
        List<String> applied = new ArrayList<>();
        LtsMigrationService service = service(List.of(migration("4.2.2.3", applied)));

        service.applyMigrations("4.2.2.2", "4.2.2.3");

        assertEquals(List.of("4.2.2.3"), applied);
        verify(jdbcTemplate, never()).execute(anyString());
        verify(schemaSettingsService).updateSchemaVersion("4.2.2.3");
    }

    @Test
    void failsLoudOnDuplicateVersion() {
        List<String> applied = new ArrayList<>();
        assertThrows(IllegalStateException.class, () -> service(List.of(
                migration("4.2.2.3", applied), migration("4.2.2.3", applied))));
    }

    @Test
    void failsLoudOnUnparseableVersion() {
        List<String> applied = new ArrayList<>();
        assertThrows(IllegalArgumentException.class, () -> service(List.of(migration("nope", applied))));
    }
}
