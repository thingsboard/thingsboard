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
 * chunks; and (4) recording the version, only once (2) and (3) have succeeded.
 * <p>
 * Two upgrade strategies compose those parts differently:
 * <ul>
 *   <li><b>No-downtime</b> ({@link #applyMigrations}) -- a running node upgrades itself (see SystemPatchApplier). DDL +
 *   apply() run atomically, then a caller-supplied replay of stored views/functions, then each backfill + version
 *   record. A crash before a version is recorded re-runs that migration on the next startup, which is why every part
 *   must be idempotent and resumable.</li>
 *   <li><b>Offline major upgrade</b> -- runs in the install/upgrade process (node not serving), split across that
 *   process's schema and data phases: {@link #runSchemaMigrations} (DDL only) runs with the rest of the schema
 *   upgrade, then {@link #runDataMigrations} (apply() + backfill) runs later, after the install flow has created the
 *   new tables, views/functions and indexes the data migrations may depend on. Neither records the LTS version; the
 *   offline upgrade flow tracks the package version itself.</li>
 * </ul>
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
     * No-downtime path (see class doc). Phase 1 runs each selected migration's DDL + apply() atomically; then
     * {@code afterSchemaPhase} replays stored views/functions against the now-complete schema (a migration may have
     * added a column they reference); then phase 2 runs each backfill and records its version.
     */
    public void applyMigrations(String fromVersion, String toVersion, Runnable afterSchemaPhase) {
        List<VersionedMigration> selected = select(fromVersion, toVersion);
        for (VersionedMigration versionedMigration : selected) {
            LtsMigration migration = versionedMigration.migration();
            transactionTemplate.executeWithoutResult(status -> {
                executeSchemaSql(migration.getVersion());
                migration.apply();
            });
            log.info("Applied LTS schema update {}", migration.getVersion());
        }
        afterSchemaPhase.run();
        for (VersionedMigration versionedMigration : selected) {
            LtsMigration migration = versionedMigration.migration();
            // Backfill outside the schema transaction, version recorded only after it returns (crash-resumable -- see class doc).
            migration.applyAfterCommit();
            schemaSettingsService.updateSchemaVersion(migration.getVersion());
            log.info("Applied LTS migration {}", migration.getVersion());
        }
    }

    /** Offline major upgrade, schema phase: each migration's DDL only, run with the rest of the schema upgrade; apply() is deferred to {@link #runDataMigrations}. */
    public void runSchemaMigrations(String fromVersion, String toVersion) {
        for (VersionedMigration versionedMigration : select(fromVersion, toVersion)) {
            String version = versionedMigration.migration().getVersion();
            transactionTemplate.executeWithoutResult(status -> executeSchemaSql(version));
            log.info("Applied LTS schema migration {}", version);
        }
    }

    /** Offline major upgrade, data phase: each migration's apply() + backfill, run after the install flow has set up the new tables/views/indexes. Records no version. */
    public void runDataMigrations(String fromVersion, String toVersion) {
        for (VersionedMigration versionedMigration : select(fromVersion, toVersion)) {
            LtsMigration migration = versionedMigration.migration();
            migration.apply();
            migration.applyAfterCommit();
            log.info("Applied LTS data migration {}", migration.getVersion());
        }
    }

    private List<VersionedMigration> select(String fromVersion, String toVersion) {
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
        return migrations.stream()
                .filter(vm -> vm.version().isInRange(from, to))
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
