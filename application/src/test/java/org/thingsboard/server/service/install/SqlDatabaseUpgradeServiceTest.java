// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.install;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.StatementCallback;
import org.springframework.transaction.PlatformTransactionManager;
import org.thingsboard.server.service.install.lts.LtsMigrationService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Statement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A converted CE database is at a CE version, not at the package version, so the CE-to-PE delta cannot be the whole
 * schema upgrade: the delta gives the database the PE shape of the version it is already at, and the LTS chain then
 * has to take it the rest of the way exactly as it does for a PE source.
 */
@ExtendWith(MockitoExtension.class)
class SqlDatabaseUpgradeServiceTest {

    private static final String PE_DELTA_SQL = "-- the CE to PE delta\n";
    private static final String BASIC_SQL = "-- the basic schema update\n";
    private static final String DB_VERSION = "4.3.1.4";
    private static final String PACKAGE_VERSION = "4.4.0.0";

    @Mock
    private InstallScripts installScripts;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private DatabaseSchemaSettingsService schemaSettingsService;
    @Mock
    private LtsMigrationService ltsMigrationService;
    @Mock
    private Statement statement;

    @TempDir
    Path dataDir;

    private SqlDatabaseUpgradeService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws IOException {
        writeSchemaUpdate("pe", PE_DELTA_SQL);
        writeSchemaUpdate("basic", BASIC_SQL);
        when(installScripts.getDataDir()).thenReturn(dataDir.toString());
        when(schemaSettingsService.getDbSchemaVersion()).thenReturn(DB_VERSION);
        when(schemaSettingsService.getPackageSchemaVersion()).thenReturn(PACKAGE_VERSION);
        // Run the callback the service passes, so the SQL it hands to the statement can be asserted on.
        when(jdbcTemplate.execute(any(StatementCallback.class)))
                .thenAnswer(invocation -> ((StatementCallback<Object>) invocation.getArgument(0)).doInStatement(statement));
        service = new SqlDatabaseUpgradeService(installScripts, jdbcTemplate, transactionManager, schemaSettingsService, ltsMigrationService);
    }

    @Test
    void ceToPeUpgradeAppliesTheDeltaAndThenTheRestOfTheChain() throws Exception {
        service.upgradeDatabase(true);

        InOrder inOrder = inOrder(statement, ltsMigrationService);
        inOrder.verify(statement).execute(PE_DELTA_SQL);
        inOrder.verify(statement).execute(BASIC_SQL);
        inOrder.verify(ltsMigrationService).runSchemaMigrations(DB_VERSION, PACKAGE_VERSION, false);
    }

    @Test
    void peUpgradeDoesNotApplyTheCeToPeDelta() throws Exception {
        service.upgradeDatabase(false);

        verify(statement, never()).execute(PE_DELTA_SQL);
        InOrder inOrder = inOrder(statement, ltsMigrationService);
        inOrder.verify(statement).execute(BASIC_SQL);
        inOrder.verify(ltsMigrationService).runSchemaMigrations(DB_VERSION, PACKAGE_VERSION, false);
    }

    @Test
    void aForcedReUpgradeIsCarriedIntoTheLtsMigrations() throws Exception {
        // The only test that can tell the flag apart from a hard-coded false: everywhere else it is Mockito's
        // default that the assertion reads back.
        when(schemaSettingsService.isForcedReUpgrade()).thenReturn(true);

        service.upgradeDatabase(false);

        verify(ltsMigrationService).runSchemaMigrations(DB_VERSION, PACKAGE_VERSION, true);
    }

    private void writeSchemaUpdate(String directory, String sql) throws IOException {
        Path file = dataDir.resolve("upgrade").resolve(directory).resolve("schema_update.sql");
        Files.createDirectories(file.getParent());
        Files.writeString(file, sql);
    }

}
