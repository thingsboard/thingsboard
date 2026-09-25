// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.install;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.service.component.ComponentDiscoveryService;
import org.thingsboard.server.service.install.DatabaseEntitiesUpgradeService;
import org.thingsboard.server.service.install.DatabaseSchemaSettingsService;
import org.thingsboard.server.service.install.EntityDatabaseSchemaService;
import org.thingsboard.server.service.install.InstallScripts;
import org.thingsboard.server.service.install.SystemDataLoaderService;
import org.thingsboard.server.service.install.TsDatabaseSchemaService;
import org.thingsboard.server.service.install.update.CacheCleanupService;
import org.thingsboard.server.service.install.update.DataUpdateService;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
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
        ReflectionTestUtils.setField(installService, "loadDemo", false);
        ReflectionTestUtils.setField(installService, "persistToTelemetry", false);
    }

    @Test
    void testUpgradeMintsTheClusterIdBeforeStampingTheSchemaVersion() {
        ReflectionTestUtils.setField(installService, "isUpgrade", true);
        ReflectionTestUtils.setField(installService, "upgradeFromVersion", "4.3.1.3");

        installService.performInstall();

        InOrder inOrder = inOrder(entityDatabaseSchemaService, databaseSchemaVersionService);
        inOrder.verify(entityDatabaseSchemaService).generateClusterIdIfNotExist();
        inOrder.verify(databaseSchemaVersionService).updateSchemaVersion();
    }

    @Test
    void testFreshInstallMintsTheClusterId() {
        ReflectionTestUtils.setField(installService, "isUpgrade", false);

        installService.performInstall();

        verify(entityDatabaseSchemaService).generateClusterIdIfNotExist();
    }

}
