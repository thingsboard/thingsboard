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
    void selectsInRangeOlderFamilyBeansOnCrossFamilyUpgrade() {
        List<String> applied = new ArrayList<>();
        // A cross-family 4.3 -> 4.4 offline upgrade now runs the real in-range 4.3.1.x beans (the source has
        // not passed them) alongside the new 4.4 baseline bean -- each exactly once. No 4.4-family bean
        // reproduces the 4.3.1.x work anymore.
        LtsMigrationService service = service(List.of(
                migration("4.2.2.3", applied),
                migration("4.3.1.2", applied),
                migration("4.3.1.3", applied),
                migration("4.4.0.0", applied)));

        service.runDataMigrations("4.3.0.0", "4.4.0.0");

        // 4.2.2.3 sits below the supported-source floor (4.3.0.0), so it is out of range and never selected.
        assertEquals(List.of("4.3.1.2", "4.3.1.3", "4.4.0.0"), applied);
    }

    @Test
    void applyMigrationsRunsAndRecordsEachInRangeVersionOnCrossFamilyUpgrade() {
        List<String> applied = new ArrayList<>();
        LtsMigrationService service = service(List.of(
                migration("4.2.2.3", applied),
                migration("4.3.1.2", applied),
                migration("4.3.1.3", applied),
                migration("4.4.0.0", applied)));

        service.applyMigrations("4.3.0.0", "4.4.0.0");

        assertEquals(List.of("4.3.1.2", "4.3.1.3", "4.4.0.0"), applied);
        // The below-floor 4.2.2.3 duplicate must not apply or record.
        verify(schemaSettingsService, never()).updateSchemaVersion("4.2.2.3");
        verify(schemaSettingsService).updateSchemaVersion("4.3.1.2");
        verify(schemaSettingsService).updateSchemaVersion("4.3.1.3");
        verify(schemaSettingsService).updateSchemaVersion("4.4.0.0");
    }

    @Test
    void runSchemaMigrationsRunsEachInRangeSqlOnCrossFamilyUpgrade() throws Exception {
        List<String> applied = new ArrayList<>();
        writeSql("4.2.2.3", "SELECT 1;");
        writeSql("4.3.1.2", "SELECT 2;");
        writeSql("4.3.1.3", "SELECT 3;");
        writeSql("4.4.0.0", "SELECT 4;");
        LtsMigrationService service = service(List.of(
                migration("4.2.2.3", applied),
                migration("4.3.1.2", applied),
                migration("4.3.1.3", applied),
                migration("4.4.0.0", applied)));

        service.runSchemaMigrations("4.3.0.0", "4.4.0.0");

        // The below-floor 4.2.2.3 SQL must not run; every in-range bean's SQL must.
        verify(jdbcTemplate, never()).execute("SELECT 1;");
        verify(jdbcTemplate).execute("SELECT 2;");
        verify(jdbcTemplate).execute("SELECT 3;");
        verify(jdbcTemplate).execute("SELECT 4;");
    }

    @Test
    void isInRangePredicate() {
        LtsVersion from = LtsVersion.parse("4.3.1.1");
        LtsVersion to = LtsVersion.parse("4.3.1.3");
        // within-family in range
        assertTrue(LtsVersion.parse("4.3.1.2").isInRange(from, to));
        // the target itself (upper boundary, inclusive)
        assertTrue(to.isInRange(from, to));
        // at from (lower boundary, exclusive)
        assertFalse(from.isInRange(from, to));
        // below from
        assertFalse(LtsVersion.parse("4.3.1.0").isInRange(from, to));
        // above to
        assertFalse(LtsVersion.parse("4.3.1.4").isInRange(from, to));

        // Cross-family: an in-range older-family bean IS now selected (the whole point of the loosened filter),
        // as is the target-family baseline; only a bean below `from` stays out of range.
        LtsVersion crossFrom = LtsVersion.parse("4.3.0.0");
        LtsVersion crossTo = LtsVersion.parse("4.4.0.0");
        assertTrue(LtsVersion.parse("4.3.1.3").isInRange(crossFrom, crossTo));
        assertTrue(crossTo.isInRange(crossFrom, crossTo));
        assertFalse(LtsVersion.parse("4.2.2.3").isInRange(crossFrom, crossTo));
    }

    @Test
    void reproductionDuplicateBeanSitsBelowSupportedSourceFloor() {
        // Load-bearing invariant (see LtsMigrationService.select): the only reproduction-duplicate pair is
        // 4.2.2.3 <-> 4.3.1.3 (both do the solution-template / widget-bundle work). For the loosened (from, to]
        // filter to never select both in a single supported upgrade, the duplicate 4.2.2.3 must sit strictly
        // below the minimum supported upgrade source. That floor is 4.3.0.0 (SUPPORTED_VERSIONS_FOR_UPGRADE).
        LtsVersion duplicate = LtsVersion.parse("4.2.2.3");
        LtsVersion supportedSourceFloor = LtsVersion.parse("4.3.0.0");
        assertTrue(duplicate.compareTo(supportedSourceFloor) < 0);
        // So on any supported cross-family upgrade (from >= floor), the duplicate is out of range.
        assertFalse(duplicate.isInRange(supportedSourceFloor, LtsVersion.parse("4.4.0.0")));
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
