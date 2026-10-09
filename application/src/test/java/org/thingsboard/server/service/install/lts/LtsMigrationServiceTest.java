// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
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

    /** Records apply() and applyAfterCommit() calls (distinctly tagged) into a single shared ordered list. */
    private LtsMigration migrationWithAfterCommit(String version, List<String> sequence) {
        return new LtsMigration() {
            @Override public String getVersion() { return version; }
            @Override public void apply() { sequence.add("apply(" + version + ")"); }
            @Override public void applyAfterCommit() { sequence.add("applyAfterCommit(" + version + ")"); }
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

        List<String> appliedVersions = service.applyMigrations("4.2.2.2", "4.2.2.3", false, () -> {});

        // only 4.2.2.3 is in (4.2.2.2, 4.2.2.3]
        assertEquals(List.of("4.2.2.3"), applied);
        assertEquals(List.of("4.2.2.3"), appliedVersions);
        verify(jdbcTemplate).execute("SELECT 1;");
        verify(jdbcTemplate, never()).execute("SELECT 2;");
    }

    @Test
    void appliesAllInRangeAndReturnsEachVersion() throws Exception {
        List<String> applied = new ArrayList<>();
        writeSql("4.2.2.3", "SELECT 1;");
        writeSql("4.2.2.4", "SELECT 2;");
        LtsMigrationService service = service(List.of(
                migration("4.2.2.3", applied), migration("4.2.2.4", applied)));

        List<String> appliedVersions = service.applyMigrations("4.2.2.2", "4.2.2.4", false, () -> {});

        assertEquals(List.of("4.2.2.3", "4.2.2.4"), applied);
        assertEquals(List.of("4.2.2.3", "4.2.2.4"), appliedVersions);
        verify(jdbcTemplate).execute("SELECT 1;");
        verify(jdbcTemplate).execute("SELECT 2;");
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

        service.runDataMigrations("4.3.0.0", "4.4.0.0", false);

        // 4.2.2.3 sits below the supported-source floor (4.3.0.0), so it is out of range and never selected.
        assertEquals(List.of("4.3.1.2", "4.3.1.3", "4.4.0.0"), applied);
    }

    @Test
    void applyMigrationsRunsAndReturnsEachInRangeVersionOnCrossFamilyUpgrade() {
        List<String> applied = new ArrayList<>();
        LtsMigrationService service = service(List.of(
                migration("4.2.2.3", applied),
                migration("4.3.1.2", applied),
                migration("4.3.1.3", applied),
                migration("4.4.0.0", applied)));

        List<String> appliedVersions = service.applyMigrations("4.3.0.0", "4.4.0.0", false, () -> {});

        // The below-floor 4.2.2.3 duplicate must not apply or be returned for recording.
        assertEquals(List.of("4.3.1.2", "4.3.1.3", "4.4.0.0"), applied);
        assertEquals(List.of("4.3.1.2", "4.3.1.3", "4.4.0.0"), appliedVersions);
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

        service.runSchemaMigrations("4.3.0.0", "4.4.0.0", false);

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

        List<String> appliedVersions = service.applyMigrations("4.2.2.3", "4.2.2.3", false, () -> {});

        assertEquals(List.of(), applied);
        assertEquals(List.of(), appliedVersions);
        verify(jdbcTemplate, never()).execute(anyString());
    }

    @Test
    void forcedRunAtCurrentVersionReRunsThatVersionSchemaOnly() throws Exception {
        List<String> applied = new ArrayList<>();
        writeSql("4.3.1.3", "SELECT 1;");
        writeSql("4.4.0.0", "SELECT 2;");
        writeSql("4.4.0.1", "SELECT 3;");
        LtsMigrationService service = service(List.of(
                migration("4.3.1.3", applied), migration("4.4.0.0", applied), migration("4.4.0.1", applied)));

        // SKIP_SCHEMA_VERSION_CHECK on a database already at the package version: the point of the flag is that this
        // re-runs the stored version's own migration instead of selecting an empty (4.4.0.0, 4.4.0.0] range.
        service.runSchemaMigrations("4.4.0.0", "4.4.0.0", true);

        verify(jdbcTemplate).execute("SELECT 2;");
        verify(jdbcTemplate, never()).execute("SELECT 1;");
        verify(jdbcTemplate, never()).execute("SELECT 3;");
    }

    @Test
    void forcedRunAtCurrentVersionReRunsThatVersionDataOnly() {
        List<String> applied = new ArrayList<>();
        LtsMigrationService service = service(List.of(
                migration("4.3.1.3", applied), migration("4.4.0.0", applied), migration("4.4.0.1", applied)));

        service.runDataMigrations("4.4.0.0", "4.4.0.0", true);

        assertEquals(List.of("4.4.0.0"), applied);
    }

    @Test
    void forcedRunOverARealRangeAlsoIncludesTheSourceVersion() {
        List<String> unforced = new ArrayList<>();
        service(List.of(
                migration("4.2.2.3", unforced), migration("4.3.1.2", unforced),
                migration("4.3.1.3", unforced), migration("4.4.0.0", unforced),
                migration("4.4.0.1", unforced)))
                .runDataMigrations("4.3.1.2", "4.4.0.0", false);

        List<String> forced = new ArrayList<>();
        service(List.of(
                migration("4.2.2.3", forced), migration("4.3.1.2", forced),
                migration("4.3.1.3", forced), migration("4.4.0.0", forced),
                migration("4.4.0.1", forced)))
                .runDataMigrations("4.3.1.2", "4.4.0.0", true);

        // Forcing adds exactly the source version's own migration -- nothing below `from`, nothing above `to`.
        assertEquals(List.of("4.3.1.3", "4.4.0.0"), unforced);
        assertEquals(List.of("4.3.1.2", "4.3.1.3", "4.4.0.0"), forced);
    }

    @Test
    void forcedApplyMigrationsAtCurrentVersionReRunsAndReturnsThatVersion() {
        List<String> applied = new ArrayList<>();
        LtsMigrationService service = service(List.of(
                migration("4.3.1.3", applied), migration("4.4.0.0", applied), migration("4.4.0.1", applied)));

        // The no-downtime twin of the above: SKIP_PATCH_VERSION_CHECK got the patch past its version check with the
        // database already at the package version, so the run must still apply that version's migration.
        List<String> appliedVersions = service.applyMigrations("4.4.0.0", "4.4.0.0", true, () -> {});

        assertEquals(List.of("4.4.0.0"), applied);
        assertEquals(List.of("4.4.0.0"), appliedVersions);
    }

    @Test
    void applyMigrationsIncludesTheSourceVersionOnlyWhenForced() {
        List<String> unforced = new ArrayList<>();
        service(List.of(
                migration("4.3.1.2", unforced), migration("4.3.1.3", unforced), migration("4.4.0.0", unforced)))
                .applyMigrations("4.3.1.2", "4.3.1.3", false, () -> {});

        List<String> forced = new ArrayList<>();
        service(List.of(
                migration("4.3.1.2", forced), migration("4.3.1.3", forced), migration("4.4.0.0", forced)))
                .applyMigrations("4.3.1.2", "4.3.1.3", true, () -> {});

        // Unforced, isVersionChanged() has already guaranteed from != to and 4.3.1.2 is known applied: re-selecting it
        // would repeat its backfill on every ordinary patch.
        assertEquals(List.of("4.3.1.3"), unforced);
        assertEquals(List.of("4.3.1.2", "4.3.1.3"), forced);
    }

    @Test
    void isInClosedRangePredicate() {
        LtsVersion from = LtsVersion.parse("4.3.1.1");
        LtsVersion to = LtsVersion.parse("4.3.1.3");
        // both boundaries inclusive, unlike isInRange
        assertTrue(from.isInClosedRange(from, to));
        assertTrue(to.isInClosedRange(from, to));
        assertTrue(LtsVersion.parse("4.3.1.2").isInClosedRange(from, to));
        assertFalse(LtsVersion.parse("4.3.1.0").isInClosedRange(from, to));
        assertFalse(LtsVersion.parse("4.3.1.4").isInClosedRange(from, to));
        // a single-version range holds exactly that version
        assertTrue(to.isInClosedRange(to, to));
        assertFalse(from.isInClosedRange(to, to));
    }

    @Test
    void runSchemaMigrationsRunsSqlButNeverAppliesOrRecords() throws Exception {
        List<String> applied = new ArrayList<>();
        writeSql("4.2.2.3", "SELECT 1;");
        LtsMigrationService service = service(List.of(migration("4.2.2.3", applied)));

        service.runSchemaMigrations("4.2.2.2", "4.2.2.3", false);

        verify(jdbcTemplate).execute("SELECT 1;");
        assertEquals(List.of(), applied);
        verify(schemaSettingsService, never()).updateSchemaVersion(anyString());
    }

    @Test
    void runDataMigrationsAppliesButNeverRunsSqlOrRecords() throws Exception {
        List<String> applied = new ArrayList<>();
        writeSql("4.2.2.3", "SELECT 1;");
        LtsMigrationService service = service(List.of(migration("4.2.2.3", applied)));

        service.runDataMigrations("4.2.2.2", "4.2.2.3", false);

        assertEquals(List.of("4.2.2.3"), applied);
        verify(jdbcTemplate, never()).execute(anyString());
        verify(schemaSettingsService, never()).updateSchemaVersion(anyString());
    }

    @Test
    void runDataMigrationsAppliesAndAppliesAfterCommitButNeverRunsSqlOrRecords() throws Exception {
        List<String> sequence = new ArrayList<>();
        writeSql("4.2.2.3", "SELECT 1;");
        LtsMigrationService service = service(List.of(migrationWithAfterCommit("4.2.2.3", sequence)));

        service.runDataMigrations("4.2.2.2", "4.2.2.3", false);

        assertEquals(List.of("apply(4.2.2.3)", "applyAfterCommit(4.2.2.3)"), sequence);
        verify(jdbcTemplate, never()).execute(anyString());
        verify(schemaSettingsService, never()).updateSchemaVersion(anyString());
    }

    @Test
    void migrationWithoutSqlFileStillAppliesAndReturnsVersion() {
        List<String> applied = new ArrayList<>();
        LtsMigrationService service = service(List.of(migration("4.2.2.3", applied)));

        List<String> appliedVersions = service.applyMigrations("4.2.2.2", "4.2.2.3", false, () -> {});

        assertEquals(List.of("4.2.2.3"), applied);
        assertEquals(List.of("4.2.2.3"), appliedVersions);
        verify(jdbcTemplate, never()).execute(anyString());
    }

    @Test
    void applyMigrationsNeverCallsUpdateSchemaVersionItself() {
        List<String> applied = new ArrayList<>();
        LtsMigrationService service = service(List.of(migration("4.2.2.3", applied)));

        service.applyMigrations("4.2.2.2", "4.2.2.3", false, () -> {});

        // applyMigrations only returns applied versions; recordVersions (below) is what stamps them.
        verify(schemaSettingsService, never()).updateSchemaVersion(anyString());
    }

    @Test
    void recordVersionsStampsEachVersionInOrder() {
        List<String> events = new ArrayList<>();
        Mockito.doAnswer(invocation -> events.add("record:" + invocation.getArgument(0)))
                .when(schemaSettingsService).updateSchemaVersion(anyString());
        LtsMigrationService service = service(List.of());

        service.recordVersions(List.of("4.3.1.2", "4.3.1.3"));

        assertEquals(List.of("record:4.3.1.2", "record:4.3.1.3"), events);
    }

    // Records apply() and applyAfterCommit() into a shared, ordered event log so their sequence can be asserted.
    private LtsMigration recordingMigration(String version, List<String> events) {
        return new LtsMigration() {
            @Override public String getVersion() { return version; }
            @Override public void apply() { events.add("apply:" + version); }
            @Override public void applyAfterCommit() { events.add("afterCommit:" + version); }
        };
    }

    @Test
    void applyMigrationsRunsApplyThenAfterCommitAndReturnsTheAppliedVersion() {
        List<String> events = new ArrayList<>();
        LtsMigrationService service = service(List.of(recordingMigration("4.3.1.3", events)));

        List<String> appliedVersions = service.applyMigrations("4.3.1.2", "4.3.1.3", false, () -> {});

        // applyAfterCommit() runs after apply(); the returned version is recordVersions' input, not stamped here.
        assertEquals(List.of("apply:4.3.1.3", "afterCommit:4.3.1.3"), events);
        assertEquals(List.of("4.3.1.3"), appliedVersions);
    }

    @Test
    void applyMigrationsPropagatesAnAfterCommitFailureAndStampsNoVersion() {
        LtsMigrationService service = service(List.of(new LtsMigration() {
            @Override public String getVersion() { return "4.3.1.3"; }
            @Override public void applyAfterCommit() { throw new IllegalStateException("backfill failed"); }
        }));

        assertThrows(IllegalStateException.class, () -> service.applyMigrations("4.3.1.2", "4.3.1.3", false, () -> {}));

        // The crash-resume invariant: an unstamped version re-runs its whole migration on the next startup.
        verify(schemaSettingsService, never()).updateSchemaVersion(anyString());
    }

    @Test
    void applyMigrationsWithReplayRunsSchemaPhaseThenReplayThenBackfillPhaseAndReturnsAppliedVersions() throws Exception {
        List<String> events = new ArrayList<>();
        writeSql("4.3.1.2", "SELECT 1;");
        writeSql("4.3.1.3", "SELECT 2;");
        LtsMigrationService service = service(List.of(
                recordingMigration("4.3.1.2", events), recordingMigration("4.3.1.3", events)));
        Runnable afterSchemaPhase = () -> events.add("replay");

        List<String> appliedVersions = service.applyMigrations("4.3.1.1", "4.3.1.3", false, afterSchemaPhase);

        // Both migrations' schema/apply run first, THEN the afterSchemaPhase replay, THEN both backfills; recording
        // each version is recordVersions' job, so no stamp appears in this event sequence.
        assertEquals(List.of(
                "apply:4.3.1.2", "apply:4.3.1.3",
                "replay",
                "afterCommit:4.3.1.2", "afterCommit:4.3.1.3"), events);
        assertEquals(List.of("4.3.1.2", "4.3.1.3"), appliedVersions);
        verify(jdbcTemplate).execute("SELECT 1;");
        verify(jdbcTemplate).execute("SELECT 2;");
    }

    @Test
    void applyMigrationsWithReplayNeverRunsBackfillOrReturnsWhenReplayThrows() {
        List<String> events = new ArrayList<>();
        LtsMigrationService service = service(List.of(
                recordingMigration("4.3.1.2", events), recordingMigration("4.3.1.3", events)));
        Runnable afterSchemaPhase = () -> { throw new IllegalStateException("replay failed"); };

        // The replay runs between the phases and fails, so no backfill runs and nothing is returned to record:
        // every selected migration is re-run on the next startup.
        assertThrows(IllegalStateException.class,
                () -> service.applyMigrations("4.3.1.1", "4.3.1.3", false, afterSchemaPhase));
        assertEquals(List.of("apply:4.3.1.2", "apply:4.3.1.3"), events);
    }

    @Test
    void applyMigrationsReturnsNothingWhenALaterBackfillFailsEvenIfAnEarlierOneSucceeded() {
        List<String> events = new ArrayList<>();
        LtsMigration first = recordingMigration("4.3.1.2", events);
        LtsMigration second = new LtsMigration() {
            @Override public String getVersion() { return "4.3.1.3"; }
            @Override public void applyAfterCommit() { throw new IllegalStateException("backfill failed"); }
        };
        LtsMigrationService service = service(List.of(first, second));

        // applyMigrations only returns its applied-versions list on a normal return; the second backfill throws
        // before that return, so the caller never learns the first one succeeded either, and recordVersions is
        // reached for neither version this run. Both re-run their (idempotent, resumable) backfill on the next
        // boot -- coarser than the old per-migration recording, but still crash-safe.
        assertThrows(IllegalStateException.class,
                () -> service.applyMigrations("4.3.1.1", "4.3.1.3", false, () -> {}));
        assertEquals(List.of("apply:4.3.1.2", "afterCommit:4.3.1.2"), events);
        verify(schemaSettingsService, never()).updateSchemaVersion(anyString());
    }

    @Test
    void runDataMigrationsRunsApplyThenAfterCommit() {
        List<String> events = new ArrayList<>();
        LtsMigrationService service = service(List.of(recordingMigration("4.3.1.3", events)));

        service.runDataMigrations("4.3.1.2", "4.3.1.3", false);

        assertEquals(List.of("apply:4.3.1.3", "afterCommit:4.3.1.3"), events);
        verify(schemaSettingsService, never()).updateSchemaVersion(anyString());
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

    @Test
    void applyMigrationsRunsAllApplyThenAfterSchemaPhaseThenAllApplyAfterCommit() {
        List<String> sequence = new ArrayList<>();
        LtsMigrationService service = service(List.of(
                migrationWithAfterCommit("4.2.2.3", sequence),
                migrationWithAfterCommit("4.2.2.4", sequence)));
        Runnable afterSchemaPhase = () -> sequence.add("afterSchemaPhase");

        List<String> appliedVersions = service.applyMigrations("4.2.2.2", "4.2.2.4", false, afterSchemaPhase);

        assertEquals(List.of(
                "apply(4.2.2.3)", "apply(4.2.2.4)",
                "afterSchemaPhase",
                "applyAfterCommit(4.2.2.3)", "applyAfterCommit(4.2.2.4)"), sequence);
        assertEquals(List.of("4.2.2.3", "4.2.2.4"), appliedVersions);
    }

}
