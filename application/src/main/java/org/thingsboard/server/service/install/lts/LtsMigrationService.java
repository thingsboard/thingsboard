// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.install.lts;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.install.DatabaseSchemaSettingsService;
import org.thingsboard.server.service.install.InstallScripts;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Applies {@link LtsMigration} beans (in version order) for an LTS upgrade. Each migration has up to four idempotent
 * parts: (1) its {@code schema_update.sql} DDL, run via {@link #executeSchemaSql} (absent file = no-op); (2)
 * {@link LtsMigration#apply()}, schema-coupled changes that must be atomic with the DDL; (3)
 * {@link LtsMigration#applyAfterCommit()}, the heavy backfill, run outside the DDL transaction and self-committing in
 * chunks; and (4) recording the version, which callers must delay until (2) and (3) -- and anything else they
 * interpose -- have succeeded (see {@link #recordVersions}).
 * <p>
 * Two upgrade strategies compose those parts differently:
 * <ul>
 *   <li><b>No-downtime</b> ({@link #applyMigrations}) -- a running node upgrades itself (see SystemPatchApplier). DDL +
 *   apply() run atomically, then a caller-supplied callback (e.g. a views/functions replay), then each backfill.
 *   {@link #applyMigrations} itself records no version; it returns the applied versions for the caller to pass to
 *   {@link #recordVersions} once it has done the same for whatever else it interposed. A crash before a version is
 *   recorded re-runs that migration on the next startup, which is why every part must be idempotent and
 *   resumable.</li>
 *   <li><b>Offline major upgrade</b> -- runs in the install/upgrade process (node not serving), split across that
 *   process's schema and data phases: {@link #runSchemaMigrations} (DDL only) runs with the rest of the schema
 *   upgrade, then {@link #runDataMigrations} (apply() + backfill) runs later, after the install flow has created the
 *   new tables, views/functions and indexes the data migrations may depend on. Neither records the LTS version; the
 *   offline upgrade flow tracks the package version itself.</li>
 * </ul>
 * <p>
 * Both strategies select the migrations to run over the same range, and both can be forced past their version check by
 * an operator flag (SKIP_SCHEMA_VERSION_CHECK offline, SKIP_PATCH_VERSION_CHECK for the no-downtime path). Each caller
 * passes that decision in as {@code forcedRun}, which widens the selection from {@code (from, to]} to the closed
 * {@code [from, to]}: a forced run is normally made on a database already at the package version, where the half-open
 * range is empty and nothing at all would run. Re-running the source version's own migration is safe for the same
 * reason a crash mid-upgrade is: every part is idempotent and resumable.
 */
@Slf4j
@Component
@TbCoreComponent
public class LtsMigrationService {

    private static final String SCHEMA_UPDATE_SQL = "schema_update.sql";

    /** A migration paired with its parsed version, so the version is parsed exactly once per bean. */
    private record VersionedMigration(LtsVersion version, LtsMigration migration) {}

    private final JdbcTemplate jdbcTemplate;
    private final InstallScripts installScripts;
    private final DatabaseSchemaSettingsService schemaSettingsService;
    private final TransactionTemplate transactionTemplate;
    private final List<VersionedMigration> migrations;

    public LtsMigrationService(JdbcTemplate jdbcTemplate,
                               InstallScripts installScripts,
                               DatabaseSchemaSettingsService schemaSettingsService,
                               PlatformTransactionManager transactionManager,
                               List<LtsMigration> migrations) {
        this.jdbcTemplate = jdbcTemplate;
        this.installScripts = installScripts;
        this.schemaSettingsService = schemaSettingsService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.migrations = validateAndSort(migrations);
        log.info("Discovered {} LTS migration(s): {}", this.migrations.size(),
                this.migrations.stream().map(versionedMigration -> versionedMigration.migration().getVersion()).toList());
    }

    private static List<VersionedMigration> validateAndSort(List<LtsMigration> migrations) {
        Set<String> seen = new HashSet<>();
        List<VersionedMigration> versioned = new ArrayList<>();
        for (LtsMigration migration : migrations) {
            LtsVersion version = LtsVersion.parse(migration.getVersion()); // fail loud on unparseable version
            if (!seen.add(migration.getVersion())) {
                throw new IllegalStateException("Duplicate LTS migration version: " + migration.getVersion());
            }
            versioned.add(new VersionedMigration(version, migration));
        }
        return versioned.stream()
                .sorted(Comparator.comparing(VersionedMigration::version))
                .toList();
    }

    /**
     * No-downtime path (see class doc). {@code afterSchemaPhase} runs between the schema phase and the backfills.
     *
     * @param forcedRun the caller got here through SKIP_PATCH_VERSION_CHECK rather than an actual version change, so
     *                  the selection includes {@code fromVersion} itself (see class doc). Unforced, it must stay
     *                  exclusive: the source version is already applied and re-selecting it would repeat its backfill
     *                  on every ordinary patch.
     * @return the versions applied, in the order {@link #recordVersions} should record them
     */
    public List<String> applyMigrations(String fromVersion, String toVersion, boolean forcedRun, Runnable afterSchemaPhase) {
        List<VersionedMigration> selected = select(fromVersion, toVersion, forcedRun);
        for (VersionedMigration versionedMigration : selected) {
            LtsMigration migration = versionedMigration.migration();
            transactionTemplate.executeWithoutResult(status -> {
                executeSchemaSql(migration.getVersion());
                migration.apply();
            });
            log.info("Applied LTS schema update {}", migration.getVersion());
        }
        afterSchemaPhase.run();
        List<String> appliedVersions = new ArrayList<>();
        for (VersionedMigration versionedMigration : selected) {
            LtsMigration migration = versionedMigration.migration();
            migration.applyAfterCommit(); // outside the schema transaction (see class doc)
            appliedVersions.add(migration.getVersion());
        }
        return appliedVersions;
    }

    /**
     * Records the versions {@link #applyMigrations} returned, once the caller's own post-schema work has succeeded.
     * Stamped together rather than one per backfill, so a later failure re-runs the earlier migrations' backfills on
     * the next startup - which {@link LtsMigration#applyAfterCommit} is required to tolerate.
     */
    public void recordVersions(List<String> versions) {
        for (String version : versions) {
            schemaSettingsService.updateSchemaVersion(version);
            log.info("Applied LTS migration {}", version);
        }
    }

    /**
     * Offline major upgrade, schema phase: each migration's DDL only, run with the rest of the schema upgrade; apply()
     * is deferred to {@link #runDataMigrations}.
     *
     * @param forcedRun the operator forced the upgrade with SKIP_SCHEMA_VERSION_CHECK, so the selection includes
     *                  {@code fromVersion} itself (see class doc).
     */
    public void runSchemaMigrations(String fromVersion, String toVersion, boolean forcedRun) {
        for (VersionedMigration versionedMigration : select(fromVersion, toVersion, forcedRun)) {
            String version = versionedMigration.migration().getVersion();
            transactionTemplate.executeWithoutResult(status -> executeSchemaSql(version));
            log.info("Applied LTS schema migration {}", version);
        }
    }

    /**
     * Offline major upgrade, data phase: each migration's apply() + backfill, run after the install flow has set up the
     * new tables/views/indexes. Records no version.
     *
     * @param forcedRun the operator forced the upgrade with SKIP_SCHEMA_VERSION_CHECK, so the selection includes
     *                  {@code fromVersion} itself (see class doc).
     */
    public void runDataMigrations(String fromVersion, String toVersion, boolean forcedRun) {
        for (VersionedMigration versionedMigration : select(fromVersion, toVersion, forcedRun)) {
            LtsMigration migration = versionedMigration.migration();
            migration.apply();
            migration.applyAfterCommit();
            log.info("Applied LTS data migration {}", migration.getVersion());
        }
    }

    private List<VersionedMigration> select(String fromVersion, String toVersion, boolean forcedRun) {
        LtsVersion from = LtsVersion.parse(fromVersion);
        LtsVersion to = LtsVersion.parse(toVersion);
        // Select every migration in the (from, to] range, regardless of family. On a cross-family offline
        // upgrade (e.g. 4.3.x -> 4.4) this is what makes the real in-range older-family beans run: it picks the
        // 4.3.1.x schema/data changes the source has not yet passed AND the new target-family beans, each exactly
        // once -- the half-open (from, to] range skips anything the source already applied. One logical migration
        // is thus authored once (one bean + one lts/<version>/schema_update.sql) and reused by both the offline
        // and no-downtime paths; nothing is reproduced into a newer family.
        //
        // Load-bearing invariant: no two beans may reproduce the same change within a single supported upgrade
        // range. A reproduction-duplicate bean (one that re-does an older bean's work on a newer-family branch)
        // must sit STRICTLY BELOW the minimum supported upgrade source, so it can never be selected together with
        // the bean it duplicates. The pairs today are 4.2.2.3 <-> 4.3.1.3 and 4.2.2.4 <-> 4.3.1.4, and the
        // supported-source floor is 4.3.0.0 (SUPPORTED_VERSIONS_FOR_UPGRADE), well above both 4.2.2.x beans.
        // LtsMigrationServiceTest guards this.
        //
        // A forced run takes the closed [from, to] instead, so the source version's own migration re-runs rather than
        // the range being empty -- see the class doc. It does not weaken that invariant in any new way: forcing
        // already bypasses the supported-source floor that upholds it (see
        // DefaultDatabaseSchemaSettingsService.validateSchemaSettings), and including `from` only extends the
        // already-accepted cost from a source below the duplicate's version to one exactly at it.
        return migrations.stream()
                .filter(versionedMigration -> forcedRun
                        ? versionedMigration.version().isInClosedRange(from, to)
                        : versionedMigration.version().isInRange(from, to))
                .toList();
    }

    private void executeSchemaSql(String version) {
        Path sqlFile = Paths.get(installScripts.getDataDir(), "upgrade", "lts", version, SCHEMA_UPDATE_SQL);
        if (!Files.exists(sqlFile)) {
            log.trace("No LTS schema update file for version {} at {}", version, sqlFile);
            return;
        }
        try {
            jdbcTemplate.execute(Files.readString(sqlFile));
            log.info("Applied LTS SQL schema update from {}", sqlFile);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read LTS schema update file: " + sqlFile, e);
        }
    }
}
