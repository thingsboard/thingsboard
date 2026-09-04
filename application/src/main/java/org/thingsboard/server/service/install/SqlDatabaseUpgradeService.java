// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.install;

import lombok.extern.slf4j.Slf4j;
import org.intellij.lang.annotations.Language;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.StatementCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.server.service.install.lts.LtsMigrationService;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLWarning;
import java.util.concurrent.TimeUnit;

@Service
@Profile("install")
@Slf4j
public class SqlDatabaseUpgradeService implements DatabaseEntitiesUpgradeService {

    private static final String SCHEMA_UPDATE_SQL = "schema_update.sql";

    private final InstallScripts installScripts;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final DatabaseSchemaSettingsService schemaSettingsService;
    private final LtsMigrationService ltsMigrationService;

    public SqlDatabaseUpgradeService(InstallScripts installScripts, JdbcTemplate jdbcTemplate,
                                     PlatformTransactionManager transactionManager,
                                     DatabaseSchemaSettingsService schemaSettingsService,
                                     LtsMigrationService ltsMigrationService) {
        this.installScripts = installScripts;
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setTimeout((int) TimeUnit.MINUTES.toSeconds(120));
        this.schemaSettingsService = schemaSettingsService;
        this.ltsMigrationService = ltsMigrationService;
    }

    @Override
    public void upgradeDatabase() {
        log.info("Updating schema...");
        loadSql(getSchemaUpdateFile("basic"));
        ltsMigrationService.runSchemaMigrations(schemaSettingsService.getDbSchemaVersion(), schemaSettingsService.getPackageSchemaVersion());
        log.info("Schema updated.");
    }

    private Path getSchemaUpdateFile(String version) {
        return Paths.get(installScripts.getDataDir(), "upgrade", version, SCHEMA_UPDATE_SQL);
    }

    private void loadSql(Path sqlFile) {
        String sql;
        try {
            sql = Files.readString(sqlFile);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        jdbcTemplate.execute((StatementCallback<Object>) stmt -> {
            stmt.execute(sql);
            printWarnings(stmt.getWarnings());
            return null;
        });
    }

    private void execute(@Language("sql") String... statements) {
        for (String statement : statements) {
            execute(statement, true);
        }
    }

    private void execute(@Language("sql") String statement, boolean ignoreErrors) {
        try {
            jdbcTemplate.execute(statement);
        } catch (Exception e) {
            if (!ignoreErrors) {
                throw e;
            }
        }
    }

    private void printWarnings(SQLWarning warnings) {
        if (warnings != null) {
            log.info("{}", warnings.getMessage());
            SQLWarning nextWarning = warnings.getNextWarning();
            while (nextWarning != null) {
                log.info("{}", nextWarning.getMessage());
                nextWarning = nextWarning.getNextWarning();
            }
        }
    }

}
