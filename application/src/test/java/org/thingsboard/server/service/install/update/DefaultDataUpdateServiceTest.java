// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.install.update;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.service.component.ComponentDiscoveryService;
import org.thingsboard.server.service.install.DatabaseSchemaSettingsService;
import org.thingsboard.server.service.install.DbUpgradeExecutorService;
import org.thingsboard.server.service.install.SystemDataLoaderService;
import org.thingsboard.server.service.install.lts.LtsMigrationService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The CE-to-PE conversion is a step on the way to the package version, not an alternative to it: the LTS data
 * migrations have to run on a converted CE database too, or the conversion silently skips every data migration
 * between the CE source version and the package version.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DefaultDataUpdateServiceTest {

    private static final String DB_VERSION = "4.3.1.4";
    private static final String PACKAGE_VERSION = "4.4.0.0";

    @Mock
    private TenantService tenantService;
    @Mock
    private RelationService relationService;
    @Mock
    private RuleChainService ruleChainService;
    @Mock
    private IntegrationService integrationService;
    @Mock
    private EntityGroupService entityGroupService;
    @Mock
    private UserService userService;
    @Mock
    private WhiteLabelingService whiteLabelingService;
    @Mock
    private CustomerService customerService;
    @Mock
    private AssetService assetService;
    @Mock
    private DeviceService deviceService;
    @Mock
    private DashboardService dashboardService;
    @Mock
    private EntityViewService entityViewService;
    @Mock
    private EdgeService edgeService;
    @Mock
    private SystemDataLoaderService systemDataLoaderService;
    @Mock
    private ComponentDiscoveryService componentDiscoveryService;
    @Mock
    private DbUpgradeExecutorService executorService;
    @Mock
    private DatabaseSchemaSettingsService schemaSettingsService;
    @Mock
    private LtsMigrationService ltsMigrationService;

    @InjectMocks
    private DefaultDataUpdateService service;

    @Test
    void ceToPeUpgradeRunsTheLtsDataMigrationsAfterTheConversion() throws Exception {
        givenAnEmptyDatabase();

        service.updateData(true);

        InOrder inOrder = inOrder(tenantService, ltsMigrationService);
        inOrder.verify(tenantService, atLeastOnce()).findTenants(any(PageLink.class));
        inOrder.verify(ltsMigrationService).runDataMigrations(DB_VERSION, PACKAGE_VERSION, false);
    }

    @Test
    void peUpgradeRunsOnlyTheLtsDataMigrations() throws Exception {
        givenAnEmptyDatabase();

        service.updateData(false);

        verify(tenantService, never()).findTenants(any(PageLink.class));
        verify(ltsMigrationService).runDataMigrations(DB_VERSION, PACKAGE_VERSION, false);
    }

    @Test
    void aForcedReUpgradeIsCarriedIntoTheLtsMigrations() throws Exception {
        // The only test that can tell the flag apart from a hard-coded false: everywhere else it is Mockito's
        // default that the assertion reads back.
        givenAnEmptyDatabase();
        when(schemaSettingsService.isForcedReUpgrade()).thenReturn(true);

        service.updateData(false);

        verify(ltsMigrationService).runDataMigrations(DB_VERSION, PACKAGE_VERSION, true);
    }

    private void givenAnEmptyDatabase() {
        when(tenantService.findTenants(any(PageLink.class))).thenReturn(new PageData<Tenant>());
        when(whiteLabelingService.findMailTemplatesByTenantId(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID))
                .thenReturn(JacksonUtil.newObjectNode());
        when(schemaSettingsService.getDbSchemaVersion()).thenReturn(DB_VERSION);
        when(schemaSettingsService.getPackageSchemaVersion()).thenReturn(PACKAGE_VERSION);
    }

}
