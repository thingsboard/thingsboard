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
                this.migrations.stream().map(vm -> vm.migration().getVersion()).toList());
    }

    private static List<VersionedMigration> validateAndSort(List<LtsMigration> migrations) {
        Set<String> seen = new HashSet<>();
        List<VersionedMigration> versioned = new ArrayList<>();
        for (LtsMigration m : migrations) {
            LtsVersion version = LtsVersion.parse(m.getVersion()); // fail loud on unparseable version
            if (!seen.add(m.getVersion())) {
                throw new IllegalStateException("Duplicate LTS migration version: " + m.getVersion());
            }
            versioned.add(new VersionedMigration(version, m));
        }
        return versioned.stream()
                .sorted(Comparator.comparing(VersionedMigration::version))
                .toList();
    }

    /** No-downtime path: per migration in (from, to] run SQL, then apply(), then record the version. */
    public void applyMigrations(String fromVersion, String toVersion) {
        for (VersionedMigration vm : select(fromVersion, toVersion)) {
            LtsMigration migration = vm.migration();
            String version = migration.getVersion();
            transactionTemplate.executeWithoutResult(status -> {
                runSchemaUpdate(version);
                migration.apply();
                schemaSettingsService.updateSchemaVersion(version);
            });
            log.info("Applied LTS migration {}", version);
        }
    }

    /** Offline major-upgrade schema phase: per migration in (from, to] run SQL only. No version record. */
    public void runSchemaMigrations(String fromVersion, String toVersion) {
        for (VersionedMigration vm : select(fromVersion, toVersion)) {
            String version = vm.migration().getVersion();
            transactionTemplate.executeWithoutResult(status -> runSchemaUpdate(version));
            log.info("Applied LTS schema migration {}", version);
        }
    }

    /** Offline major-upgrade data phase: per migration in (from, to] run apply() only. No SQL, no version record. */
    public void runDataMigrations(String fromVersion, String toVersion) {
        for (VersionedMigration vm : select(fromVersion, toVersion)) {
            vm.migration().apply();
            log.info("Applied LTS data migration {}", vm.migration().getVersion());
        }
    }

    private List<VersionedMigration> select(String fromVersion, String toVersion) {
        LtsVersion from = LtsVersion.parse(fromVersion);
        LtsVersion to = LtsVersion.parse(toVersion);
        return migrations.stream()
                .filter(vm -> isInRangeForTargetFamily(vm.version(), from, to))
                .toList();
    }

    // Run only migrations whose family matches the target version. Older-family
    // migrations (e.g. 4.2.x) ride onto newer-family branches (e.g. 4.3.x) via the
    // release-merge cascade, but each branch's own family of migrations is
    // self-contained (newer-family copies reproduce the older schema/data changes),
    // so a cross-family upgrade is fully handled by the target-family migrations.
    // Excluding the dormant older-family beans avoids double-processing.
    static boolean isInRangeForTargetFamily(LtsVersion version, LtsVersion from, LtsVersion to) {
        return version.sameFamily(to) && version.isInRange(from, to);
    }

    private void runSchemaUpdate(String version) {
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
