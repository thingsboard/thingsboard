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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    void skipsMigrationsOutsideTargetFamilyOnCrossFamilyUpgrade() {
        List<String> applied = new ArrayList<>();
        // 4.2.2.3 is carried onto a 4.3 branch by the release-merge cascade but must stay dormant:
        // a cross-family 4.2 -> 4.3 upgrade is handled entirely by the 4.3-family migrations.
        LtsMigrationService service = service(List.of(
                migration("4.2.2.3", applied),
                migration("4.3.1.2", applied),
                migration("4.3.1.3", applied)));

        service.runDataMigrations("4.2.2.2", "4.3.1.3");

        assertEquals(List.of("4.3.1.2", "4.3.1.3"), applied);
    }

    @Test
    void applyMigrationsSkipsMigrationsOutsideTargetFamilyOnCrossFamilyUpgrade() {
        List<String> applied = new ArrayList<>();
        // The dormant 4.2.2.3 bean must not apply or record on a cross-family 4.2 -> 4.3 upgrade.
        LtsMigrationService service = service(List.of(
                migration("4.2.2.3", applied),
                migration("4.3.1.2", applied),
                migration("4.3.1.3", applied)));

        service.applyMigrations("4.2.2.2", "4.3.1.3");

        assertEquals(List.of("4.3.1.2", "4.3.1.3"), applied);
        verify(schemaSettingsService, never()).updateSchemaVersion("4.2.2.3");
        verify(schemaSettingsService).updateSchemaVersion("4.3.1.2");
        verify(schemaSettingsService).updateSchemaVersion("4.3.1.3");
    }

    @Test
    void runSchemaMigrationsSkipsMigrationsOutsideTargetFamilyOnCrossFamilyUpgrade() throws Exception {
        List<String> applied = new ArrayList<>();
        writeSql("4.2.2.3", "SELECT 1;");
        writeSql("4.3.1.2", "SELECT 2;");
        writeSql("4.3.1.3", "SELECT 3;");
        LtsMigrationService service = service(List.of(
                migration("4.2.2.3", applied),
                migration("4.3.1.2", applied),
                migration("4.3.1.3", applied)));

        service.runSchemaMigrations("4.2.2.2", "4.3.1.3");

        // The dormant 4.2.2.3 SQL must not run; the 4.3-family SQL must.
        verify(jdbcTemplate, never()).execute("SELECT 1;");
        verify(jdbcTemplate).execute("SELECT 2;");
        verify(jdbcTemplate).execute("SELECT 3;");
    }

    @Test
    void isInRangeForTargetFamilyPredicate() {
        LtsVersion from = LtsVersion.parse("4.3.1.1");
        LtsVersion to = LtsVersion.parse("4.3.1.3");
        // same-family in range
        assertTrue(LtsMigrationService.isInRangeForTargetFamily(LtsVersion.parse("4.3.1.2"), from, to));
        // older-family bean within the numeric range but wrong family
        assertFalse(LtsMigrationService.isInRangeForTargetFamily(LtsVersion.parse("4.2.2.3"), from, to));
        // the target itself (upper boundary, inclusive)
        assertTrue(LtsMigrationService.isInRangeForTargetFamily(to, from, to));
        // at from (lower boundary, exclusive)
        assertFalse(LtsMigrationService.isInRangeForTargetFamily(from, from, to));
        // below from
        assertFalse(LtsMigrationService.isInRangeForTargetFamily(LtsVersion.parse("4.3.1.0"), from, to));
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
