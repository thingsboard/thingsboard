// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.install;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.dao.sql.citus.CitusSchemaService;
import org.thingsboard.server.dao.subscription.EntityCapExceededException;
import org.thingsboard.server.service.component.ComponentDiscoveryService;
import org.thingsboard.server.service.install.LicenseCapacityUpgradePreflight;
import org.thingsboard.server.service.install.DatabaseEntitiesUpgradeService;
import org.thingsboard.server.service.install.DatabaseSchemaSettingsService;
import org.thingsboard.server.service.install.EntityDatabaseSchemaService;
import org.thingsboard.server.service.install.InstallScripts;
import org.thingsboard.server.service.install.SystemDataLoaderService;
import org.thingsboard.server.service.install.TsDatabaseSchemaService;
import org.thingsboard.server.service.install.update.CacheCleanupService;
import org.thingsboard.server.service.install.update.DataUpdateService;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ThingsboardInstallServiceTest {

    @Mock
    private EntityDatabaseSchemaService entityDatabaseSchemaService;
    @Mock
    private TsDatabaseSchemaService tsDatabaseSchemaService;
    @Mock
    private DatabaseEntitiesUpgradeService databaseEntitiesUpgradeService;
    @Mock
    private ComponentDiscoveryService componentDiscoveryService;
    @Mock
    private ApplicationContext context;
    @Mock
    private SystemDataLoaderService systemDataLoaderService;
    @Mock
    private DataUpdateService dataUpdateService;
    @Mock
    private CacheCleanupService cacheCleanupService;
    @Mock
    private InstallScripts installScripts;
    @Mock
    private DatabaseSchemaSettingsService databaseSchemaVersionService;
    @Mock
    private CitusSchemaService citusSchemaService;
    @Mock
    private LicenseCapacityUpgradePreflight licenseCapacityUpgradePreflight;

    private ThingsboardInstallService installService;

    @BeforeEach
    void setUp() {
        installService = new ThingsboardInstallService();
        ReflectionTestUtils.setField(installService, "entityDatabaseSchemaService", entityDatabaseSchemaService);
        ReflectionTestUtils.setField(installService, "tsDatabaseSchemaService", tsDatabaseSchemaService);
        ReflectionTestUtils.setField(installService, "databaseEntitiesUpgradeService", databaseEntitiesUpgradeService);
        ReflectionTestUtils.setField(installService, "componentDiscoveryService", componentDiscoveryService);
        ReflectionTestUtils.setField(installService, "context", context);
        ReflectionTestUtils.setField(installService, "systemDataLoaderService", systemDataLoaderService);
        ReflectionTestUtils.setField(installService, "dataUpdateService", dataUpdateService);
        ReflectionTestUtils.setField(installService, "cacheCleanupService", cacheCleanupService);
        ReflectionTestUtils.setField(installService, "installScripts", installScripts);
        ReflectionTestUtils.setField(installService, "databaseSchemaVersionService", databaseSchemaVersionService);
        ReflectionTestUtils.setField(installService, "citusSchemaService", citusSchemaService);
        ReflectionTestUtils.setField(installService, "licenseCapacityUpgradePreflight", licenseCapacityUpgradePreflight);
        ReflectionTestUtils.setField(installService, "persistToTelemetry", false);
    }

    @Test
    void postgresToCitusUpgradeDistributesThenRecreatesViews() throws Exception {
        ReflectionTestUtils.setField(installService, "isUpgrade", true);
        ReflectionTestUtils.setField(installService, "upgradeFromVersion", "postgres-to-citus");
        // The conversion requires the database schema to already be at the package version.
        when(databaseSchemaVersionService.getDbSchemaVersion()).thenReturn("4.3.1.4");
        when(databaseSchemaVersionService.getPackageSchemaVersion()).thenReturn("4.3.1.4");

        installService.performInstall();

        // The distribution must happen first, and only then the views/functions are re-created so they pick up the
        // Citus-safe definitions against the now-distributed KV tables.
        InOrder inOrder = inOrder(citusSchemaService, entityDatabaseSchemaService);
        inOrder.verify(citusSchemaService).applyDistribution();
        inOrder.verify(entityDatabaseSchemaService).createOrUpdateViewsAndFunctions();
        inOrder.verify(entityDatabaseSchemaService).createOrUpdateDeviceInfoView(false);
        // The plain upgrade path must not be taken on the postgres-to-citus branch.
        verify(databaseEntitiesUpgradeService, never()).upgradeDatabase(anyBoolean());
    }

    @Test
    void aRegularUpgradeRunsTheLicensePreflightBeforeAnythingIsWritten() throws Exception {
        ReflectionTestUtils.setField(installService, "isUpgrade", true);
        ReflectionTestUtils.setField(installService, "upgradeFromVersion", "4.3.1.4");

        installService.performInstall();

        // The whole point of the pre-flight is that it runs before the first write: the schema settings are
        // validated, the licence capacity is checked, and only then the cache is cleared and the upgrade SQL
        // applied. Moving the check below upgradeDatabase would break the feature with a green build.
        InOrder inOrder = inOrder(databaseSchemaVersionService, licenseCapacityUpgradePreflight,
                cacheCleanupService, databaseEntitiesUpgradeService, entityDatabaseSchemaService);
        inOrder.verify(databaseSchemaVersionService).validateSchemaSettings();
        inOrder.verify(licenseCapacityUpgradePreflight).check();
        inOrder.verify(cacheCleanupService).clearCache();
        inOrder.verify(databaseEntitiesUpgradeService).upgradeDatabase(false);
        inOrder.verify(entityDatabaseSchemaService).generateClusterIdIfNotExist();
        inOrder.verify(databaseSchemaVersionService).updateSchemaVersion();
    }

    @Test
    void aCeToPeUpgradeCreatesTheMissingPeTablesBeforeTheUpgradeChain() throws Exception {
        ReflectionTestUtils.setField(installService, "isUpgrade", true);
        // No flag: the CE branch is selected by the product marker the database carries.
        ReflectionTestUtils.setField(installService, "upgradeFromVersion", "");
        when(databaseSchemaVersionService.isUpgradeFromCe()).thenReturn(true);

        installService.performInstall();

        // The LTS files the chain applies reference PE-only tables and each of them runs as a single transaction,
        // so a converted CE database must be given those tables before the chain runs rather than after it.
        InOrder inOrder = inOrder(entityDatabaseSchemaService, databaseEntitiesUpgradeService, dataUpdateService);
        inOrder.verify(entityDatabaseSchemaService).createDatabaseSchema(false);
        inOrder.verify(databaseEntitiesUpgradeService).upgradeDatabase(true);
        inOrder.verify(entityDatabaseSchemaService).createDatabaseSchema(false);
        inOrder.verify(dataUpdateService).updateData(true);
        verify(entityDatabaseSchemaService, times(2)).createDatabaseSchema(false);
    }

    @Test
    void aPeUpgradeCreatesTheSchemaOnlyAfterTheUpgradeChain() throws Exception {
        ReflectionTestUtils.setField(installService, "isUpgrade", true);
        ReflectionTestUtils.setField(installService, "upgradeFromVersion", "");
        when(databaseSchemaVersionService.isUpgradeFromCe()).thenReturn(false);

        installService.performInstall();

        // The PE-to-PE order is the one that ships today and must stay untouched by the CE fix above.
        InOrder inOrder = inOrder(databaseEntitiesUpgradeService, entityDatabaseSchemaService, dataUpdateService);
        inOrder.verify(databaseEntitiesUpgradeService).upgradeDatabase(false);
        inOrder.verify(entityDatabaseSchemaService).createDatabaseSchema(false);
        inOrder.verify(dataUpdateService).updateData(false);
        verify(entityDatabaseSchemaService, times(1)).createDatabaseSchema(false);
    }

    @Test
    void theLegacyCeFlagCannotSendAPeDatabaseDownTheConversionBranch() throws Exception {
        ReflectionTestUtils.setField(installService, "isUpgrade", true);
        ReflectionTestUtils.setField(installService, "upgradeFromVersion", "CE");
        when(databaseSchemaVersionService.isUpgradeFromCe()).thenReturn(false);

        installService.performInstall();

        // The flag is kept for compatibility with the documented upgrade command and decides nothing: a PE
        // database run with it must not have the CE-to-PE delta applied on top of it.
        verify(databaseEntitiesUpgradeService).upgradeDatabase(false);
        verify(dataUpdateService).updateData(false);
        verify(entityDatabaseSchemaService, times(1)).createDatabaseSchema(false);
    }

    @Test
    void aRefusedLicensePreflightAbortsTheUpgradeBeforeTheDatabaseIsTouched() throws Exception {
        ReflectionTestUtils.setField(installService, "isUpgrade", true);
        ReflectionTestUtils.setField(installService, "upgradeFromVersion", "4.3.1.4");
        doThrow(new EntityCapExceededException("too many devices"))
                .when(licenseCapacityUpgradePreflight).check();

        assertThatThrownBy(() -> installService.performInstall())
                .isInstanceOf(ThingsboardInstallException.class)
                .hasCauseInstanceOf(EntityCapExceededException.class);
        verify(cacheCleanupService, never()).clearCache();
        verify(databaseEntitiesUpgradeService, never()).upgradeDatabase(anyBoolean());
    }

    @Test
    void postgresToCitusUpgradeFailsWhenCitusSchemaServiceMissing() {
        ReflectionTestUtils.setField(installService, "isUpgrade", true);
        ReflectionTestUtils.setField(installService, "upgradeFromVersion", "postgres-to-citus");
        ReflectionTestUtils.setField(installService, "citusSchemaService", null);

        // performInstall() wraps every failure in ThingsboardInstallException; the postgres-to-citus branch guards
        // the missing dependency with an IllegalStateException.
        assertThatThrownBy(() -> installService.performInstall())
                .isInstanceOf(ThingsboardInstallException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void postgresToCitusUpgradeFailsWhenDbSchemaVersionDiffersFromPackage() {
        ReflectionTestUtils.setField(installService, "isUpgrade", true);
        ReflectionTestUtils.setField(installService, "upgradeFromVersion", "postgres-to-citus");
        when(databaseSchemaVersionService.getDbSchemaVersion()).thenReturn("4.3.1.3");
        when(databaseSchemaVersionService.getPackageSchemaVersion()).thenReturn("4.3.1.4");

        // An out-of-date schema must fail fast with a clear message instead of dying inside the distribution DDL.
        assertThatThrownBy(() -> installService.performInstall())
                .isInstanceOf(ThingsboardInstallException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
        verify(citusSchemaService, never()).applyDistribution();
    }

    @Test
    void freshInstallDistributesBeforeRecordingSchemaSettings() throws Exception {
        ReflectionTestUtils.setField(installService, "isUpgrade", false);

        installService.performInstall();

        // Fresh Citus install: schema is created, then distributed, then the settings row is written and the
        // views/functions are created against the distributed tables.
        InOrder inOrder = inOrder(entityDatabaseSchemaService, citusSchemaService, databaseSchemaVersionService);
        inOrder.verify(entityDatabaseSchemaService).createDatabaseSchema();
        inOrder.verify(citusSchemaService).applyDistribution();
        inOrder.verify(databaseSchemaVersionService).createSchemaSettings();
        inOrder.verify(entityDatabaseSchemaService).createOrUpdateViewsAndFunctions();
        // The upgrade path must not run on a fresh install.
        verify(databaseEntitiesUpgradeService, never()).upgradeDatabase(anyBoolean());
    }

    /**
     * The anonymous-takeover guard the whole first-time setup flow rests on: a fresh install seeds system data
     * and stops there, leaving an instance with no system administrator, so the first one is created by the
     * setup wizard ({@code SetupDataService#createSysAdmin}) once an operator has proven they are the one
     * standing in front of the new instance. An installer that created a sysadmin of its own would hand that
     * account to whoever reached the instance first.
     * <p>
     * What is assertable here is narrower than that intent, and deliberately so: {@code SetupDataService} is not
     * a collaborator of this service, so there is no {@code createSysAdmin} call to refuse with {@code never()},
     * and making one possible would mean wiring a dependency into production code for the test's benefit.
     * Pinning the exact set of data-loading calls is the tripwire that is available: a sysadmin-creating step
     * added to either loader fails here rather than passing unnoticed. A sysadmin created through a collaborator
     * this service does not hold today would be missed, and the review of that new dependency has to catch it.
     */
    @Test
    void freshInstallLoadsSystemDataWithoutCreatingASystemAdministrator() throws Exception {
        ReflectionTestUtils.setField(installService, "isUpgrade", false);

        installService.performInstall();

        verify(systemDataLoaderService).createDefaultTenantProfiles();
        verify(systemDataLoaderService).createAdminSettings();
        verify(systemDataLoaderService).createRandomJwtSettings();
        verify(systemDataLoaderService).loadSystemWidgets();
        verify(systemDataLoaderService).createOAuth2Templates();
        verify(systemDataLoaderService).createQueues();
        verify(systemDataLoaderService).createDefaultNotificationConfigs();
        verify(systemDataLoaderService).createDefaultCustomMenu();
        verify(installScripts).updateSystemNotificationTemplates();
        verify(installScripts).loadSystemLwm2mResources();
        verify(installScripts).loadSystemImagesAndResources();
        // The sysadmin encryption key is a key, not an account: it is generated for the sysadmin the setup
        // wizard is yet to create.
        verify(installScripts).generateSysAdminEncryptionKey();
        verifyNoMoreInteractions(systemDataLoaderService, installScripts);
    }

    @Test
    void freshInstallMintsTheClusterId() throws Exception {
        ReflectionTestUtils.setField(installService, "isUpgrade", false);

        installService.performInstall();

        // The upgrade half of this is pinned by the ordering assertion in
        // aRegularUpgradeRunsTheLicensePreflightBeforeAnythingIsWritten; a fresh install has no schema version
        // to stamp, so all that matters here is that the identity is minted at all.
        verify(entityDatabaseSchemaService).generateClusterIdIfNotExist();
    }

    @Test
    void freshInstallWithoutCitusSkipsDistribution() throws Exception {
        // The default CE/plain-PG install: the optional CitusSchemaService bean is absent.
        ReflectionTestUtils.setField(installService, "isUpgrade", false);
        ReflectionTestUtils.setField(installService, "citusSchemaService", null);

        installService.performInstall();

        // The install completes normally and never touches the distribution.
        verify(entityDatabaseSchemaService).createDatabaseSchema();
        verify(databaseSchemaVersionService).createSchemaSettings();
        verify(entityDatabaseSchemaService).createOrUpdateViewsAndFunctions();
        verify(citusSchemaService, never()).applyDistribution();
        verify(databaseEntitiesUpgradeService, never()).upgradeDatabase(anyBoolean());
    }
}
