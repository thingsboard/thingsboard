// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.sync.solution;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.metadata.TbGetAttributesNode;
import org.thingsboard.rule.engine.metadata.TbGetAttributesNodeConfiguration;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.CustomMenuId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.platform.AllUsersFilter;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainConnectionInfo;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.common.data.sync.ie.EntityExportSettings;
import org.thingsboard.server.common.data.sync.ie.EntityGroupExportData;
import org.thingsboard.server.common.data.sync.solution.SolutionData;
import org.thingsboard.server.common.data.sync.solution.SolutionExportRequest;
import org.thingsboard.server.common.data.sync.solution.SolutionExportResponse;
import org.thingsboard.server.common.data.sync.solution.SolutionImportResult;
import org.thingsboard.server.common.data.sync.solution.SolutionValidationResult;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.service.sync.ie.exporting.ExportableEntitiesService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
@TestPropertySource(properties = {
        "service.integrations.supported=ALL",
})
public class SolutionExportImportControllerTest extends AbstractControllerTest {

    @Autowired
    private ExportableEntitiesService exportableEntitiesService;
    @Autowired
    private RuleChainService ruleChainService;
    @Autowired
    private DashboardService dashboardService;
    @Autowired
    private RelationService relationService;
    @Autowired
    private TenantService tenantService;
    @Autowired
    private UserService userService;
    @Autowired
    private EntityGroupService entityGroupService;

    private TenantId tenantId1;
    private TenantId tenantId2;
    private String tenant1AdminEmail;
    private String tenant2AdminEmail;

    @Before
    public void beforeEach() throws Exception {
        loginSysAdmin();

        Tenant tenant1 = new Tenant();
        tenant1.setTitle("Solution Test Tenant 1");
        tenant1.setEmail("sol-t1@thingsboard.org");
        this.tenantId1 = tenantService.saveTenant(tenant1).getId();
        User admin1 = new User();
        admin1.setTenantId(tenantId1);
        admin1.setAuthority(Authority.TENANT_ADMIN);
        admin1.setEmail("sol-t1-admin@thingsboard.org");
        this.tenant1AdminEmail = admin1.getEmail();
        createUser(admin1, "12345678");

        Tenant tenant2 = new Tenant();
        tenant2.setTitle("Solution Test Tenant 2");
        tenant2.setEmail("sol-t2@thingsboard.org");
        this.tenantId2 = tenantService.saveTenant(tenant2).getId();
        User admin2 = new User();
        admin2.setTenantId(tenantId2);
        admin2.setAuthority(Authority.TENANT_ADMIN);
        admin2.setEmail("sol-t2-admin@thingsboard.org");
        this.tenant2AdminEmail = admin2.getEmail();
        createUser(admin2, "12345678");
    }

    @After
    public void afterEach() {
        tenantService.deleteTenant(tenantId1);
        tenantService.deleteTenant(tenantId2);
    }

    private void loginTenant1() throws Exception {
        login(tenant1AdminEmail, "12345678");
    }

    private void loginTenant2() throws Exception {
        login(tenant2AdminEmail, "12345678");
    }

    // --- Export tests ---

    @Test
    public void testExportSolution_basicEntities() throws Exception {
        loginTenant1();
        RuleChain ruleChain = saveRuleChain("Export RC");
        DeviceProfile dp = doPost("/api/deviceProfile", createDeviceProfile("Export DP"), DeviceProfile.class);
        Dashboard dashboard = saveDashboard("Export Dash");

        SolutionExportResponse response = doExport(ruleChain.getId(), dp.getId(), dashboard.getId());

        SolutionData solution = response.getSolution();
        assertThat(solution.getEntities()).containsKeys(EntityType.RULE_CHAIN, EntityType.DASHBOARD, EntityType.DEVICE_PROFILE);
        assertThat(solution.getEntities().get(EntityType.RULE_CHAIN)).hasSize(1);
        assertThat(solution.getEntities().get(EntityType.DASHBOARD)).hasSize(1);
        assertThat(solution.getEntities().get(EntityType.DEVICE_PROFILE)).hasSize(1);
    }

    @Test
    public void testExportSolution_dependencyWarnings() throws Exception {
        loginTenant1();
        RuleChain rc = saveRuleChain("Referenced RC");
        DeviceProfile dp = createDeviceProfile("DP With Ref");
        dp.setDefaultRuleChainId(rc.getId());
        dp = doPost("/api/deviceProfile", dp, DeviceProfile.class);

        // Export only the device profile, not the rule chain
        SolutionExportResponse response = doExport(dp.getId());

        assertThat(response.getWarnings()).isNotEmpty();
        assertThat(response.getWarnings().get(0)).contains("DEVICE_PROFILE").contains("rule chain");
    }

    @Test
    public void testExportSolution_entityNotBelongToTenant() throws Exception {
        loginTenant1();
        RuleChain rc = saveRuleChain("Tenant1 RC");

        // Switch to tenant2 and try to export tenant1's entity
        loginTenant2();
        doPost("/api/solution/export", buildExportRequest(rc.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testExportSolution_bothFieldsEmpty_returnsBadRequest() throws Exception {
        loginTenant1();

        // Empty request body: neither internalIds nor externalIds populated.
        SolutionExportRequest request = SolutionExportRequest.builder()
                .settings(defaultExportSettings())
                .build();

        doPost("/api/solution/export", request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(
                        containsString("internalIds")))
                .andExpect(jsonPath("$.message").value(
                        containsString("externalIds")));
    }

    @Test
    public void testExportSolution_oldEntityIdsKeyNotAccepted() throws Exception {
        // Wire-level breaking change: the old 'entityIds' field was renamed to 'internalIds' with no
        // @JsonAlias kept. A request body that only carries the legacy 'entityIds' key must be treated
        // as having neither internalIds nor externalIds and rejected with 400.
        loginTenant1();
        RuleChain rc = saveRuleChain("Legacy Key RC");

        ObjectNode legacyBody = JacksonUtil.newObjectNode();
        ArrayNode legacyEntityIds = legacyBody.putArray("entityIds");
        ObjectNode legacyId = legacyEntityIds.addObject();
        legacyId.put("entityType", rc.getId().getEntityType().name());
        legacyId.put("id", rc.getId().getId().toString());
        legacyBody.set("settings", JacksonUtil.valueToTree(defaultExportSettings()));

        doPost("/api/solution/export", legacyBody)
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testExportSolution_byExternalIds_resolution() throws Exception {
        // Create an entity on tenant1 whose externalId is set; request via externalIds and confirm
        // the server resolves it back to the local entity.
        loginTenant1();
        DashboardId externalDashboardId = new DashboardId(UUID.randomUUID());
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("External Lookup Dash");
        dashboard.setExternalId(externalDashboardId);
        dashboard = doPost("/api/dashboard", dashboard, Dashboard.class);

        SolutionExportRequest request = SolutionExportRequest.builder()
                .externalIds(Set.of(externalDashboardId))
                .settings(defaultExportSettings())
                .build();

        SolutionExportResponse response = doPost("/api/solution/export", request, SolutionExportResponse.class);

        List<EntityExportData<?>> dashboards = response.getSolution().getEntities().get(EntityType.DASHBOARD);
        assertThat(dashboards).hasSize(1);
        // The exported entity carries the externalId we supplied; its internal id matches the local record.
        assertThat(dashboards.get(0).getExternalId()).isEqualTo(externalDashboardId);
        assertThat(((Dashboard) dashboards.get(0).getEntity()).getTitle()).isEqualTo("External Lookup Dash");
        assertThat(dashboard.getExternalId()).isEqualTo(externalDashboardId);
    }

    @Test
    public void testExportSolution_entityTypeNamespacing_sameUuidAsInternalAndExternal() throws Exception {
        // The same UUID can legitimately be the internal id of one entity and the external id of another.
        // Both must resolve independently — internalIds picks the first, externalIds picks the second.
        loginTenant1();
        Dashboard dashA = saveDashboard("Namespacing Dash A");
        DashboardId sharedUuid = dashA.getId();

        // Second dashboard whose externalId equals the first dashboard's internal id.
        Dashboard dashB = new Dashboard();
        dashB.setTitle("Namespacing Dash B");
        dashB.setExternalId(sharedUuid);
        dashB = doPost("/api/dashboard", dashB, Dashboard.class);

        SolutionExportRequest request = SolutionExportRequest.builder()
                .internalIds(Set.of(sharedUuid))
                .externalIds(Set.of(sharedUuid))
                .settings(defaultExportSettings())
                .build();
        SolutionExportResponse response = doPost("/api/solution/export", request, SolutionExportResponse.class);

        List<EntityExportData<?>> dashboards = response.getSolution().getEntities().get(EntityType.DASHBOARD);
        assertThat(dashboards).as("both dashboards resolved through their respective lookup fields").hasSize(2);
        assertThat(dashboards).extracting(d -> ((Dashboard) d.getEntity()).getTitle())
                .containsExactlyInAnyOrder("Namespacing Dash A", "Namespacing Dash B");
        assertThat(dashB.getId()).as("dashB has a distinct internal id from the shared UUID").isNotEqualTo(sharedUuid);
    }

    @Test
    public void testExportSolution_unionDeduplicatesAcrossInternalAndExternal() throws Exception {
        // Same entity named via both internalIds (its internal id) and externalIds (its externalId)
        // → the response contains it exactly once.
        loginTenant1();
        DashboardId externalDashboardId = new DashboardId(UUID.randomUUID());
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("Union Dedup Dash");
        dashboard.setExternalId(externalDashboardId);
        dashboard = doPost("/api/dashboard", dashboard, Dashboard.class);

        SolutionExportRequest request = SolutionExportRequest.builder()
                .internalIds(Set.of(dashboard.getId()))
                .externalIds(Set.of(externalDashboardId))
                .settings(defaultExportSettings())
                .build();
        SolutionExportResponse response = doPost("/api/solution/export", request, SolutionExportResponse.class);

        List<EntityExportData<?>> dashboards = response.getSolution().getEntities().get(EntityType.DASHBOARD);
        assertThat(dashboards).as("entity named via both sides must appear only once").hasSize(1);
        assertThat(((Dashboard) dashboards.get(0).getEntity()).getTitle()).isEqualTo("Union Dedup Dash");
    }

    @Test
    public void testExportSolution_unresolvedInternalId_returnsNotFound() throws Exception {
        loginTenant1();
        DashboardId bogusInternalDashboardId = new DashboardId(UUID.randomUUID());

        SolutionExportRequest request = SolutionExportRequest.builder()
                .internalIds(Set.of(bogusInternalDashboardId))
                .settings(defaultExportSettings())
                .build();

        doPost("/api/solution/export", request)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("Internal ids not found")))
                .andExpect(jsonPath("$.message").value(containsString(EntityType.DASHBOARD.name())))
                .andExpect(jsonPath("$.message").value(containsString(bogusInternalDashboardId.getId().toString())));
    }

    @Test
    public void testExportSolution_unresolvedExternalId_returnsNotFound() throws Exception {
        loginTenant1();
        RuleChainId bogusExternalRuleChainId = new RuleChainId(UUID.randomUUID());

        SolutionExportRequest request = SolutionExportRequest.builder()
                .externalIds(Set.of(bogusExternalRuleChainId))
                .settings(defaultExportSettings())
                .build();

        doPost("/api/solution/export", request)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("External ids not found")))
                .andExpect(jsonPath("$.message").value(containsString(EntityType.RULE_CHAIN.name())))
                .andExpect(jsonPath("$.message").value(containsString(bogusExternalRuleChainId.getId().toString())));
    }

    @Test
    public void testExportSolution_multipleUnresolvedIds_returnsAllInMessage() throws Exception {
        loginTenant1();
        DashboardId bogusInternalDashboardId = new DashboardId(UUID.randomUUID());
        DashboardId bogusInternalDashboardId2 = new DashboardId(UUID.randomUUID());
        RuleChainId bogusExternalRuleChainId = new RuleChainId(UUID.randomUUID());

        SolutionExportRequest request = SolutionExportRequest.builder()
                .internalIds(Set.of(bogusInternalDashboardId, bogusInternalDashboardId2))
                .externalIds(Set.of(bogusExternalRuleChainId))
                .settings(defaultExportSettings())
                .build();

        doPost("/api/solution/export", request)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("Internal ids not found")))
                .andExpect(jsonPath("$.message").value(containsString(EntityType.DASHBOARD.name())))
                .andExpect(jsonPath("$.message").value(containsString(bogusInternalDashboardId.getId().toString())))
                .andExpect(jsonPath("$.message").value(containsString(bogusInternalDashboardId2.getId().toString())))
                .andExpect(jsonPath("$.message").value(containsString("External ids not found")))
                .andExpect(jsonPath("$.message").value(containsString(EntityType.RULE_CHAIN.name())))
                .andExpect(jsonPath("$.message").value(containsString(bogusExternalRuleChainId.getId().toString())));
    }

    @Test
    public void testExportSolution_externalIds_tenantIsolation() throws Exception {
        // An externalId that matches an entity in tenant1 must not be reachable from tenant2.
        loginTenant1();
        DashboardId crossTenantExternalId = new DashboardId(UUID.randomUUID());
        Dashboard t1Dashboard = new Dashboard();
        t1Dashboard.setTitle("Cross Tenant External Dash");
        t1Dashboard.setExternalId(crossTenantExternalId);
        doPost("/api/dashboard", t1Dashboard, Dashboard.class);

        loginTenant2();
        SolutionExportRequest request = SolutionExportRequest.builder()
                .externalIds(Set.of(crossTenantExternalId))
                .settings(defaultExportSettings())
                .build();

        doPost("/api/solution/export", request)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("External ids not found")))
                .andExpect(jsonPath("$.message").value(containsString(EntityType.DASHBOARD.name())))
                .andExpect(jsonPath("$.message").value(containsString(crossTenantExternalId.getId().toString())));
    }

    // --- Import round-trip tests ---

    @Test
    public void testExportImportRoundTrip_simpleEntities() throws Exception {
        loginTenant1();
        RuleChain rc = saveRuleChain("Simple RC");
        DeviceProfile dp = createDeviceProfile("Simple DP");
        dp.setDefaultRuleChainId(rc.getId());
        dp = doPost("/api/deviceProfile", dp, DeviceProfile.class);

        SolutionExportResponse exportResp = doExport(rc.getId(), dp.getId());

        // Import on tenant2
        loginTenant2();
        SolutionImportResult result = doImport(exportResp.getSolution());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getCreated()).containsEntry(EntityType.RULE_CHAIN, 1);
        assertThat(result.getCreated()).containsEntry(EntityType.DEVICE_PROFILE, 1);

        RuleChain importedRc = findRuleChainByName(tenantId2, "Simple RC");
        DeviceProfile importedDp = exportableEntitiesService.findEntityByTenantIdAndName(tenantId2, EntityType.DEVICE_PROFILE, "Simple DP");

        assertThat(importedRc).isNotNull();
        assertThat(importedRc.getTenantId()).isEqualTo(tenantId2);
        assertThat(importedRc.getId()).isNotEqualTo(rc.getId());

        assertThat(importedDp).isNotNull();
        assertThat(importedDp.getTenantId()).isEqualTo(tenantId2);
        assertThat(importedDp.getDefaultRuleChainId()).isEqualTo(importedRc.getId());

        // Verify external-to-internal ID mapping
        Map<UUID, UUID> idMap = result.getIdMapping();
        assertThat(idMap).isNotEmpty();
        assertThat(idMap.get(rc.getUuidId())).isEqualTo(importedRc.getUuidId());
        assertThat(idMap.get(dp.getUuidId())).isEqualTo(importedDp.getUuidId());
    }

    @Test
    public void testExportImportRoundTrip_fullSolution() throws Exception {
        loginTenant1();

        RuleChain rc = saveRuleChain("Full RC");
        Dashboard dashboard = saveDashboard("Full Dash");
        Customer customer = saveCustomer("Full Customer");

        AssetProfile ap = createAssetProfile("Full AP");
        ap.setDefaultRuleChainId(rc.getId());
        ap.setDefaultDashboardId(dashboard.getId());
        ap = doPost("/api/assetProfile", ap, AssetProfile.class);

        DeviceProfile dp = createDeviceProfile("Full DP");
        dp.setDefaultRuleChainId(rc.getId());
        dp.setDefaultDashboardId(dashboard.getId());
        dp = doPost("/api/deviceProfile", dp, DeviceProfile.class);

        Asset asset = new Asset();
        asset.setName("Full Asset");
        asset.setAssetProfileId(ap.getId());
        asset.setCustomerId(customer.getId());
        asset = doPost("/api/asset", asset, Asset.class);

        Device device = new Device();
        device.setName("Full Device");
        device.setDeviceProfileId(dp.getId());
        device.setCustomerId(customer.getId());
        device = doPost("/api/device", device, Device.class);

        // Create relation
        EntityRelation relation = new EntityRelation(asset.getId(), device.getId(),
                EntityRelation.MANAGES_TYPE, RelationTypeGroup.COMMON);
        doPost("/api/relation", relation).andExpect(status().isOk());

        SolutionExportResponse exportResp = doExport(
                customer.getId(), rc.getId(), dashboard.getId(),
                ap.getId(), dp.getId(), asset.getId(), device.getId());

        // Import on tenant2
        loginTenant2();
        SolutionImportResult result = doImport(exportResp.getSolution());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getCreated()).containsKeys(
                EntityType.CUSTOMER, EntityType.RULE_CHAIN, EntityType.DASHBOARD,
                EntityType.ASSET_PROFILE, EntityType.DEVICE_PROFILE, EntityType.ASSET, EntityType.DEVICE);

        Customer importedCustomer = exportableEntitiesService.findEntityByTenantIdAndName(tenantId2, EntityType.CUSTOMER, "Full Customer");
        RuleChain importedRc = findRuleChainByName(tenantId2, "Full RC");
        Dashboard importedDash = findDashboardByTitle(tenantId2, "Full Dash");
        AssetProfile importedAp = exportableEntitiesService.findEntityByTenantIdAndName(tenantId2, EntityType.ASSET_PROFILE, "Full AP");
        DeviceProfile importedDp = exportableEntitiesService.findEntityByTenantIdAndName(tenantId2, EntityType.DEVICE_PROFILE, "Full DP");
        Asset importedAsset = exportableEntitiesService.findEntityByTenantIdAndName(tenantId2, EntityType.ASSET, "Full Asset");
        Device importedDevice = exportableEntitiesService.findEntityByTenantIdAndName(tenantId2, EntityType.DEVICE, "Full Device");

        assertThat(importedCustomer).isNotNull();
        assertThat(importedRc).isNotNull();
        assertThat(importedDash).isNotNull();

        // Profiles reference imported rule chain and dashboard
        assertThat(importedAp.getDefaultRuleChainId()).isEqualTo(importedRc.getId());
        assertThat(importedAp.getDefaultDashboardId()).isEqualTo(importedDash.getId());
        assertThat(importedDp.getDefaultRuleChainId()).isEqualTo(importedRc.getId());
        assertThat(importedDp.getDefaultDashboardId()).isEqualTo(importedDash.getId());

        // Instances reference imported profiles and customer
        assertThat(importedAsset.getAssetProfileId()).isEqualTo(importedAp.getId());
        assertThat(importedAsset.getCustomerId()).isEqualTo(importedCustomer.getId());
        assertThat(importedDevice.getDeviceProfileId()).isEqualTo(importedDp.getId());
        assertThat(importedDevice.getCustomerId()).isEqualTo(importedCustomer.getId());

        // Relations preserved
        List<EntityRelation> importedRelations = relationService.findByTo(tenantId2, importedDevice.getId(), RelationTypeGroup.COMMON);
        assertThat(importedRelations).hasSize(1);
        assertThat(importedRelations.get(0).getFrom()).isEqualTo(importedAsset.getId());
        assertThat(importedRelations.get(0).getType()).isEqualTo(EntityRelation.MANAGES_TYPE);

        // Verify external-to-internal ID mapping
        Map<UUID, UUID> idMap = result.getIdMapping();
        assertThat(idMap).isNotEmpty();
        assertThat(idMap.get(rc.getUuidId())).isEqualTo(importedRc.getUuidId());
        assertThat(idMap.get(dashboard.getUuidId())).isEqualTo(importedDash.getUuidId());
        assertThat(idMap.get(customer.getUuidId())).isEqualTo(importedCustomer.getUuidId());
        assertThat(idMap.get(ap.getUuidId())).isEqualTo(importedAp.getUuidId());
        assertThat(idMap.get(dp.getUuidId())).isEqualTo(importedDp.getUuidId());
        assertThat(idMap.get(asset.getUuidId())).isEqualTo(importedAsset.getUuidId());
        assertThat(idMap.get(device.getUuidId())).isEqualTo(importedDevice.getUuidId());
    }

    @Test
    public void testExportImportRoundTrip_circularRuleChainReferences() throws Exception {
        loginTenant1();

        // Create two bare rule chains via service (need fine-grained metadata control)
        RuleChain chainA = new RuleChain();
        chainA.setName("Chain A");
        chainA.setType(RuleChainType.CORE);
        chainA = doPost("/api/ruleChain", chainA, RuleChain.class);

        RuleChain chainB = new RuleChain();
        chainB.setName("Chain B");
        chainB.setType(RuleChainType.CORE);
        chainB = doPost("/api/ruleChain", chainB, RuleChain.class);

        // Chain A -> Chain B
        saveRuleChainMetaWithConnection(chainA.getId(), chainB.getId(), "Node A", tenantId1);
        // Chain B -> Chain A
        saveRuleChainMetaWithConnection(chainB.getId(), chainA.getId(), "Node B", tenantId1);

        SolutionExportResponse exportResp = doExport(chainA.getId(), chainB.getId());

        loginTenant2();
        SolutionImportResult result = doImport(exportResp.getSolution());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getCreated().get(EntityType.RULE_CHAIN)).isEqualTo(2);

        RuleChain importedA = findRuleChainByName(tenantId2, "Chain A");
        RuleChain importedB = findRuleChainByName(tenantId2, "Chain B");
        assertThat(importedA).isNotNull();
        assertThat(importedB).isNotNull();

        // Rule chain connections are stored as TbRuleChainInputNode nodes with ruleChainId in configuration
        RuleChainMetaData metaA = ruleChainService.loadRuleChainMetaData(tenantId2, importedA.getId());
        RuleChainMetaData metaB = ruleChainService.loadRuleChainMetaData(tenantId2, importedB.getId());

        assertThat(findInputNodeTargetChainId(metaA)).isEqualTo(importedB.getId());
        assertThat(findInputNodeTargetChainId(metaB)).isEqualTo(importedA.getId());
    }

    @Test
    public void testImportSolution_emptyEntities() throws Exception {
        SolutionData solution = new SolutionData();
        solution.setEntities(Map.of());

        loginTenant2();
        doImportExpectStatus(solution, status().isBadRequest());
    }

    // --- Validation tests ---

    @Test
    public void testValidateSolution_valid() throws Exception {
        loginTenant1();
        RuleChain rc = saveRuleChain("Valid RC");
        DeviceProfile dp = doPost("/api/deviceProfile", createDeviceProfile("Valid DP"), DeviceProfile.class);
        SolutionExportResponse exportResp = doExport(rc.getId(), dp.getId());

        loginTenant2();
        SolutionValidationResult validation = doValidate(exportResp.getSolution());

        assertThat(validation.isValid()).isTrue();
        assertThat(validation.getEntitySummary()).containsEntry(EntityType.RULE_CHAIN, 1);
        assertThat(validation.getEntitySummary()).containsEntry(EntityType.DEVICE_PROFILE, 1);
        assertThat(validation.getConflicts()).isEmpty();
    }

    @Test
    public void testValidateSolution_withDependencyWarnings() throws Exception {
        loginTenant1();
        RuleChain rc = saveRuleChain("Ref RC");
        DeviceProfile dp = createDeviceProfile("Ref DP");
        dp.setDefaultRuleChainId(rc.getId());
        dp = doPost("/api/deviceProfile", dp, DeviceProfile.class);

        // Export only the device profile
        SolutionExportResponse exportResp = doExport(dp.getId());

        loginTenant2();
        SolutionValidationResult validation = doValidate(exportResp.getSolution());

        assertThat(validation.isValid()).isTrue();
        assertThat(validation.getWarnings()).isNotEmpty();
        assertThat(validation.getWarnings().get(0)).contains("rule chain");
    }

    @Test
    public void testValidateSolution_invalidEntity() throws Exception {
        loginTenant1();
        RuleChain rc = saveRuleChain("Constraint RC");
        SolutionExportResponse exportResp = doExport(rc.getId());

        // Corrupt the entity: set name to null (violates @NoXss/@Length constraints path)
        SolutionData solution = exportResp.getSolution();
        ((RuleChain) solution.getEntities().get(EntityType.RULE_CHAIN).get(0).getEntity()).setName(null);

        loginTenant2();
        SolutionValidationResult validation = doValidate(solution);

        assertThat(validation.isValid()).isFalse();
        assertThat(validation.getConflicts()).isNotEmpty();
        assertThat(validation.getConflicts().get(0)).contains("RULE_CHAIN");
    }

    @Test
    public void testValidateSolution_invalidCalculatedField() throws Exception {
        loginTenant1();
        DeviceProfile dp = doPost("/api/deviceProfile", createDeviceProfile("CF Validate DP"), DeviceProfile.class);
        SolutionExportResponse exportResp = doExport(dp.getId());

        // Add a calculated field with null configuration
        SolutionData solution = exportResp.getSolution();
        CalculatedField cf = new CalculatedField();
        cf.setName("Bad CF");
        cf.setType(CalculatedFieldType.SIMPLE);
        cf.setConfiguration(null); // violates @NotNull
        solution.getEntities().get(EntityType.DEVICE_PROFILE).get(0).setCalculatedFields(List.of(cf));

        loginTenant2();
        SolutionValidationResult validation = doValidate(solution);

        assertThat(validation.isValid()).isFalse();
        assertThat(validation.getConflicts()).isNotEmpty();
        assertThat(validation.getConflicts().get(0)).contains("DEVICE_PROFILE").contains("calculated field").contains("Bad CF");
    }

    @Test
    public void testValidateSolution_invalidRelation() throws Exception {
        loginTenant1();
        RuleChain rc = saveRuleChain("Rel Validate RC");
        SolutionExportResponse exportResp = doExport(rc.getId());

        // Add a relation with null type (violates @NotBlank)
        SolutionData solution = exportResp.getSolution();
        EntityRelation badRelation = new EntityRelation();
        badRelation.setFrom(rc.getId());
        badRelation.setTo(rc.getId());
        badRelation.setType(null);
        badRelation.setTypeGroup(RelationTypeGroup.COMMON);
        solution.getEntities().get(EntityType.RULE_CHAIN).get(0).setRelations(List.of(badRelation));

        loginTenant2();
        SolutionValidationResult validation = doValidate(solution);

        assertThat(validation.isValid()).isFalse();
        assertThat(validation.getConflicts()).isNotEmpty();
        assertThat(validation.getConflicts().get(0)).contains("RULE_CHAIN").contains("relation");
    }

    // --- Authorization tests ---

    @Test
    public void testExportSolution_sysAdminForbidden() throws Exception {
        loginTenant1();
        RuleChain rc = saveRuleChain("Auth RC");
        EntityId rcId = rc.getId();

        loginSysAdmin();
        doPost("/api/solution/export", buildExportRequest(rcId))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testImportSolution_sysAdminForbidden() throws Exception {
        loginTenant1();
        RuleChain rc = saveRuleChain("Auth Import RC");
        SolutionExportResponse exportResp = doExport(rc.getId());

        loginSysAdmin();
        doImportExpectStatus(exportResp.getSolution(), status().isForbidden());
    }

    @Test
    public void testExportSolution_customerUser_forbidden() throws Exception {
        loginTenant1();
        RuleChain rc = saveRuleChain("Customer Auth RC");
        Customer customer = saveCustomer("Customer Auth Customer");
        User customerUser = new User();
        customerUser.setTenantId(customer.getTenantId());
        customerUser.setCustomerId(customer.getId());
        customerUser.setEmail("customer-auth@example.com");
        customerUser.setAuthority(Authority.CUSTOMER_USER);
        createUser(customerUser, "Password!1");

        login("customer-auth@example.com", "Password!1");
        doPost("/api/solution/export", buildExportRequest(rc.getId()))
                .andExpect(status().isForbidden());
    }

    // --- Per-entity permission tests ---

    @Test
    public void testExportSolution_deniedOnFirstForbiddenEntity_amongMany() throws Exception {
        loginTenant1();
        RuleChain rc = saveRuleChain("Perm Export RC");
        DeviceProfile dp = doPost("/api/deviceProfile", createDeviceProfile("Perm Export DP"), DeviceProfile.class);
        Device device = createDeviceOnTenant1(dp.getId(), "Perm Export Device");

        loginAsRestrictedTenantAdmin(tenantId1,
                Map.of(Resource.VERSION_CONTROL, List.of(Operation.READ),
                        Resource.RULE_CHAIN, List.of(Operation.READ)));

        doPost("/api/solution/export", buildExportRequest(rc.getId(), device.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testImportSolution_rollbackPreservesPriorState_onPermissionDenied() throws Exception {
        loginTenant1();
        RuleChain rc = saveRuleChain("Rollback RC");
        Dashboard dashboard = saveDashboard("Rollback Dash");
        DeviceProfile dp = doPost("/api/deviceProfile", createDeviceProfile("Rollback DP"), DeviceProfile.class);
        Device device = createDeviceOnTenant1(dp.getId(), "Rollback Device");
        SolutionExportResponse exportResp = doExport(rc.getId(), dashboard.getId(), dp.getId(), device.getId());

        loginTenant2();
        // Pre-create matching Device on tenant2 with same externalId so import enters the WRITE branch.
        Device t2Device = new Device();
        t2Device.setTenantId(tenantId2);
        t2Device.setName("Rollback Device");
        t2Device.setType("default");
        t2Device.setExternalId(device.getId());
        t2Device = doPost("/api/device", t2Device, Device.class);

        loginAsRestrictedTenantAdmin(tenantId2,
                Map.of(Resource.VERSION_CONTROL, List.of(Operation.READ, Operation.WRITE),
                        Resource.RULE_CHAIN, List.of(Operation.READ, Operation.CREATE, Operation.WRITE),
                        Resource.DASHBOARD, List.of(Operation.READ, Operation.CREATE, Operation.WRITE),
                        Resource.DEVICE_PROFILE, List.of(Operation.READ, Operation.CREATE, Operation.WRITE),
                        Resource.DEVICE, List.of(Operation.READ, Operation.CREATE))); // CREATE on DEVICE but no WRITE

        doImportExpectStatus(exportResp.getSolution(), status().isForbidden());

        loginSysAdmin();
        assertThat(findRuleChainByName(tenantId2, "Rollback RC")).as("RuleChain must not be persisted on rollback").isNull();
        assertThat(findDashboardByTitle(tenantId2, "Rollback Dash")).as("Dashboard must not be persisted on rollback").isNull();
        DeviceProfile persistedProfile = exportableEntitiesService.findEntityByTenantIdAndName(tenantId2, EntityType.DEVICE_PROFILE, "Rollback DP");
        assertThat(persistedProfile).as("DeviceProfile must not be persisted on rollback").isNull();
    }

    @Test
    public void testImportSolution_notificationTypes_allBlockedBySameResource() throws Exception {
        loginTenant1();
        NotificationTemplate template = createNotificationTemplate(NotificationType.GENERAL, "Notif perm subject", "body", NotificationDeliveryMethod.WEB);
        // Use AllUsersFilter (not SystemAdministratorsFilter — that's rejected for non-sysadmin callers).
        NotificationTarget target = createNotificationTarget(new AllUsersFilter());
        SolutionExportResponse exportResp = doExport(template.getId(), target.getId());

        loginTenant2();
        loginAsRestrictedTenantAdmin(tenantId2,
                Map.of(Resource.VERSION_CONTROL, List.of(Operation.READ, Operation.WRITE)));

        doImportExpectStatus(exportResp.getSolution(), status().isForbidden());

        loginSysAdmin();
        NotificationTemplate persistedTemplate = exportableEntitiesService.findEntityByTenantIdAndName(tenantId2, EntityType.NOTIFICATION_TEMPLATE, template.getName());
        NotificationTarget persistedTarget = exportableEntitiesService.findEntityByTenantIdAndName(tenantId2, EntityType.NOTIFICATION_TARGET, target.getName());
        assertThat(persistedTemplate).isNull();
        assertThat(persistedTarget).isNull();
    }

    @Test
    public void testValidateSolution_doesNotRequirePerEntityPermissions() throws Exception {
        loginTenant1();
        DeviceProfile dp = doPost("/api/deviceProfile", createDeviceProfile("Validate Perm DP"), DeviceProfile.class);
        Device device = createDeviceOnTenant1(dp.getId(), "Validate Perm Device");
        SolutionExportResponse exportResp = doExport(dp.getId(), device.getId());

        loginTenant2();
        loginAsRestrictedTenantAdmin(tenantId2,
                Map.of(Resource.VERSION_CONTROL, List.of(Operation.READ, Operation.WRITE)));

        SolutionValidationResult validation = doValidate(exportResp.getSolution());
        assertThat(validation).isNotNull();
        assertThat(validation.getEntitySummary()).containsEntry(EntityType.DEVICE, 1);
    }

    // --- Per-type permission tests: DEVICE ---

    @Test
    public void testExportSolution_device_deniedWithoutReadPermission() throws Exception {
        loginTenant1();
        DeviceProfile dp = doPost("/api/deviceProfile", createDeviceProfile("Dev Read Deny DP"), DeviceProfile.class);
        Device device = createDeviceOnTenant1(dp.getId(), "Dev Read Deny");

        loginAsRestrictedTenantAdmin(tenantId1,
                Map.of(Resource.VERSION_CONTROL, List.of(Operation.READ),
                        Resource.DEVICE_PROFILE, List.of(Operation.READ)));

        doPost("/api/solution/export", buildExportRequest(dp.getId(), device.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testImportSolution_device_deniedWithoutCreatePermission() throws Exception {
        loginTenant1();
        DeviceProfile dp = doPost("/api/deviceProfile", createDeviceProfile("Dev Create Deny DP"), DeviceProfile.class);
        Device device = createDeviceOnTenant1(dp.getId(), "Dev Create Deny");
        SolutionExportResponse exportResp = doExport(dp.getId(), device.getId());

        loginTenant2();
        loginAsRestrictedTenantAdmin(tenantId2,
                Map.of(Resource.VERSION_CONTROL, List.of(Operation.READ, Operation.WRITE),
                        Resource.DEVICE_PROFILE, List.of(Operation.READ, Operation.CREATE, Operation.WRITE),
                        Resource.DEVICE, List.of(Operation.READ))); // no CREATE

        doImportExpectStatus(exportResp.getSolution(), status().isForbidden());

        loginSysAdmin();
        Device persistedDevice = exportableEntitiesService.findEntityByTenantIdAndName(tenantId2, EntityType.DEVICE, "Dev Create Deny");
        assertThat(persistedDevice).isNull();
    }

    @Test
    public void testImportSolution_device_deniedWithoutWritePermission() throws Exception {
        loginTenant1();
        DeviceProfile dp = doPost("/api/deviceProfile", createDeviceProfile("Dev Write Deny DP"), DeviceProfile.class);
        Device device = createDeviceOnTenant1(dp.getId(), "Dev Write Deny");
        SolutionExportResponse exportResp = doExport(dp.getId(), device.getId());

        loginTenant2();
        // Pre-create on tenant2 with matching externalId so import enters WRITE branch.
        Device t2Device = new Device();
        t2Device.setTenantId(tenantId2);
        t2Device.setName("Dev Write Deny");
        t2Device.setType("default");
        t2Device.setExternalId(device.getId());
        t2Device = doPost("/api/device", t2Device, Device.class);
        UUID priorVersion = t2Device.getUuidId();

        loginAsRestrictedTenantAdmin(tenantId2,
                Map.of(Resource.VERSION_CONTROL, List.of(Operation.READ, Operation.WRITE),
                        Resource.DEVICE_PROFILE, List.of(Operation.READ, Operation.CREATE, Operation.WRITE),
                        Resource.DEVICE, List.of(Operation.READ, Operation.CREATE))); // no WRITE

        doImportExpectStatus(exportResp.getSolution(), status().isForbidden());

        loginSysAdmin();
        Device persisted = exportableEntitiesService.findEntityByTenantIdAndName(tenantId2, EntityType.DEVICE, "Dev Write Deny");
        assertThat(persisted).isNotNull();
        assertThat(persisted.getUuidId()).isEqualTo(priorVersion);
        assertThat(persisted.getExternalId()).isEqualTo(device.getId());
    }

    @Test
    public void testImportSolution_sameTenant_assignsExternalIdOnExistingEntity() throws Exception {
        // Reproduce: export a locally-created entity (externalId=null in DB), then import the same
        // payload back into the originating tenant. The import must persist externalId on the
        // existing entity so a subsequent export-by-externalId can find it.
        loginTenant1();
        DeviceProfile dp = doPost("/api/deviceProfile", createDeviceProfile("Same Tenant DP"), DeviceProfile.class);
        Device device = createDeviceOnTenant1(dp.getId(), "Same Tenant Device");
        assertThat(device.getExternalId()).isNull();

        SolutionExportResponse exportResp = doExport(dp.getId(), device.getId());
        SolutionImportResult result = doImport(exportResp.getSolution());
        assertThat(result.isSuccess()).isTrue();

        Device persisted = exportableEntitiesService.findEntityByTenantIdAndName(tenantId1, EntityType.DEVICE, "Same Tenant Device");
        assertThat(persisted).isNotNull();
        assertThat(persisted.getExternalId())
                .as("import into originating tenant must fill in externalId so later export-by-externalId works")
                .isEqualTo(device.getId());

        // Subsequent export by externalId must resolve the entity (no 404).
        SolutionExportRequest byExternal = SolutionExportRequest.builder()
                .externalIds(Set.of(device.getId()))
                .settings(defaultExportSettings())
                .build();
        SolutionExportResponse roundTrip = doPost("/api/solution/export", byExternal, SolutionExportResponse.class);
        List<EntityExportData<?>> devices = roundTrip.getSolution().getEntities().get(EntityType.DEVICE);
        assertThat(devices).hasSize(1);
        assertThat(devices.get(0).getEntity().getName()).isEqualTo("Same Tenant Device");
    }

    @Test
    public void testExportImportRoundTrip_device_withPermissions() throws Exception {
        loginTenant1();
        DeviceProfile dp = doPost("/api/deviceProfile", createDeviceProfile("Dev Allow DP"), DeviceProfile.class);
        Device device = createDeviceOnTenant1(dp.getId(), "Dev Allow");
        SolutionExportResponse exportResp = doExport(dp.getId(), device.getId());

        loginTenant2();
        loginAsRestrictedTenantAdmin(tenantId2,
                Set.of(),
                Set.of(Resource.VERSION_CONTROL, Resource.DEVICE_PROFILE, Resource.DEVICE));

        SolutionImportResult result = doImport(exportResp.getSolution());
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getCreated()).containsEntry(EntityType.DEVICE, 1);

        Device imported = exportableEntitiesService.findEntityByTenantIdAndName(tenantId2, EntityType.DEVICE, "Dev Allow");
        assertThat(imported).isNotNull();
        assertThat(imported.getTenantId()).isEqualTo(tenantId2);
        assertThat(imported.getExternalId()).isEqualTo(device.getId());
    }

    // --- Per-type permission tests: USER ---

    @Test
    public void testExportSolution_user_deniedWithoutReadPermission() throws Exception {
        loginTenant1();
        Customer customer = saveCustomer("User Read Deny Customer");
        User user = createCustomerUser(customer, "user-read-deny@example.com", Authority.CUSTOMER_USER);

        loginAsRestrictedTenantAdmin(tenantId1,
                Map.of(Resource.VERSION_CONTROL, List.of(Operation.READ),
                        Resource.CUSTOMER, List.of(Operation.READ)));

        doPost("/api/solution/export", buildExportRequest(customer.getId(), user.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testImportSolution_user_deniedWithoutCreatePermission() throws Exception {
        loginTenant1();
        Customer customer = saveCustomer("User Create Deny Customer");
        User user = createCustomerUser(customer, "user-create-deny@example.com", Authority.CUSTOMER_USER);
        SolutionExportResponse exportResp = doExport(customer.getId(), user.getId());
        userService.deleteUser(tenantId1, user.getId());

        loginTenant2();
        loginAsRestrictedTenantAdmin(tenantId2,
                Map.of(Resource.VERSION_CONTROL, List.of(Operation.READ, Operation.WRITE),
                        Resource.CUSTOMER, List.of(Operation.READ, Operation.CREATE, Operation.WRITE),
                        Resource.USER, List.of(Operation.READ))); // no CREATE

        doImportExpectStatus(exportResp.getSolution(), status().isForbidden());

        loginSysAdmin();
        assertThat(userService.findUserByTenantIdAndEmail(tenantId2, "user-create-deny@example.com"))
                .as("USER must not be persisted on permission denial").isNull();
    }

    @Test
    public void testImportSolution_user_deniedWithoutWritePermission() throws Exception {
        loginTenant1();
        Customer customer = saveCustomer("User Write Deny Customer");
        User user = createCustomerUser(customer, "user-write-deny@example.com", Authority.CUSTOMER_USER);
        UserId sourceUserId = user.getId();
        SolutionExportResponse exportResp = doExport(customer.getId(), user.getId());
        userService.deleteUser(tenantId1, sourceUserId);

        loginTenant2();
        Customer t2Customer = saveCustomer("User Write Deny T2 Customer");
        User t2User = new User();
        t2User.setTenantId(tenantId2);
        t2User.setCustomerId(t2Customer.getId());
        t2User.setEmail("user-write-deny@example.com");
        t2User.setAuthority(Authority.CUSTOMER_USER);
        t2User.setExternalId(sourceUserId);
        t2User = userService.saveUser(tenantId2, t2User);
        long priorVersion = t2User.getVersion();

        loginAsRestrictedTenantAdmin(tenantId2,
                Map.of(Resource.VERSION_CONTROL, List.of(Operation.READ, Operation.WRITE),
                        Resource.CUSTOMER, List.of(Operation.READ, Operation.CREATE, Operation.WRITE),
                        Resource.USER, List.of(Operation.READ, Operation.CREATE))); // no WRITE

        doImportExpectStatus(exportResp.getSolution(), status().isForbidden());

        loginSysAdmin();
        User persisted = userService.findUserByTenantIdAndEmail(tenantId2, "user-write-deny@example.com");
        assertThat(persisted).isNotNull();
        assertThat(persisted.getId()).isEqualTo(t2User.getId());
        assertThat(persisted.getVersion()).as("WRITE denial must leave existing user untouched").isEqualTo(priorVersion);
    }

    @Test
    public void testExportImportRoundTrip_user_withPermissions() throws Exception {
        loginTenant1();
        Customer customer = saveCustomer("User Allow Customer");
        User user = createCustomerUser(customer, "user-allow@example.com", Authority.CUSTOMER_USER);
        UserId sourceUserId = user.getId();
        SolutionExportResponse exportResp = doExport(customer.getId(), user.getId());
        userService.deleteUser(tenantId1, sourceUserId);

        loginTenant2();
        loginAsRestrictedTenantAdmin(tenantId2,
                Set.of(),
                Set.of(Resource.VERSION_CONTROL, Resource.CUSTOMER, Resource.USER));

        SolutionImportResult result = doImport(exportResp.getSolution());
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getCreated()).containsEntry(EntityType.USER, 1);

        User imported = userService.findUserByTenantIdAndEmail(tenantId2, "user-allow@example.com");
        assertThat(imported).isNotNull();
        assertThat(imported.getTenantId()).isEqualTo(tenantId2);
        assertThat(imported.getExternalId()).isEqualTo(sourceUserId);
    }

    // --- Per-type permission tests: RULE_CHAIN ---

    @Test
    public void testExportSolution_ruleChain_deniedWithoutReadPermission() throws Exception {
        loginTenant1();
        RuleChain rc = saveRuleChain("RC Read Deny");

        loginAsRestrictedTenantAdmin(tenantId1,
                Map.of(Resource.VERSION_CONTROL, List.of(Operation.READ)));

        doPost("/api/solution/export", buildExportRequest(rc.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testImportSolution_ruleChain_deniedWithoutCreatePermission() throws Exception {
        loginTenant1();
        RuleChain rc = saveRuleChain("RC Create Deny");
        SolutionExportResponse exportResp = doExport(rc.getId());

        loginTenant2();
        loginAsRestrictedTenantAdmin(tenantId2,
                Map.of(Resource.VERSION_CONTROL, List.of(Operation.READ, Operation.WRITE),
                        Resource.RULE_CHAIN, List.of(Operation.READ))); // no CREATE

        doImportExpectStatus(exportResp.getSolution(), status().isForbidden());

        loginSysAdmin();
        assertThat(findRuleChainByName(tenantId2, "RC Create Deny"))
                .as("RuleChain must not be persisted on permission denial").isNull();
    }

    @Test
    public void testImportSolution_ruleChain_deniedWithoutWritePermission() throws Exception {
        loginTenant1();
        RuleChain rc = saveRuleChain("RC Write Deny");
        SolutionExportResponse exportResp = doExport(rc.getId());

        loginTenant2();
        // Pre-create matching RuleChain on tenant2 with same externalId so import enters WRITE branch.
        RuleChain t2Rc = new RuleChain();
        t2Rc.setName("RC Write Deny");
        t2Rc.setType(RuleChainType.CORE);
        t2Rc.setExternalId(rc.getId());
        t2Rc = doPost("/api/ruleChain", t2Rc, RuleChain.class);
        UUID priorId = t2Rc.getUuidId();

        loginAsRestrictedTenantAdmin(tenantId2,
                Map.of(Resource.VERSION_CONTROL, List.of(Operation.READ, Operation.WRITE),
                        Resource.RULE_CHAIN, List.of(Operation.READ, Operation.CREATE))); // no WRITE

        doImportExpectStatus(exportResp.getSolution(), status().isForbidden());

        loginSysAdmin();
        RuleChain persisted = findRuleChainByName(tenantId2, "RC Write Deny");
        assertThat(persisted).isNotNull();
        assertThat(persisted.getUuidId()).isEqualTo(priorId);
        assertThat(persisted.getExternalId()).isEqualTo(rc.getId());
    }

    @Test
    public void testExportImportRoundTrip_ruleChain_withPermissions() throws Exception {
        loginTenant1();
        RuleChain rc = saveRuleChain("RC Allow");
        SolutionExportResponse exportResp = doExport(rc.getId());

        loginTenant2();
        loginAsRestrictedTenantAdmin(tenantId2,
                Set.of(),
                Set.of(Resource.VERSION_CONTROL, Resource.RULE_CHAIN));

        SolutionImportResult result = doImport(exportResp.getSolution());
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getCreated()).containsEntry(EntityType.RULE_CHAIN, 1);

        RuleChain imported = findRuleChainByName(tenantId2, "RC Allow");
        assertThat(imported).isNotNull();
        assertThat(imported.getTenantId()).isEqualTo(tenantId2);
        assertThat(imported.getExternalId()).isEqualTo(rc.getId());
    }

    // --- Per-type permission tests: INTEGRATION ---

    @Test
    public void testExportSolution_integration_deniedWithoutReadPermission() throws Exception {
        loginTenant1();
        Converter converter = createConverterOnCurrentTenant("Int Read Deny Converter");
        Integration integration = createIntegrationOnCurrentTenant(converter.getId(), "Int Read Deny");

        loginAsRestrictedTenantAdmin(tenantId1,
                Map.of(Resource.VERSION_CONTROL, List.of(Operation.READ),
                        Resource.CONVERTER, List.of(Operation.READ)));

        doPost("/api/solution/export", buildExportRequest(converter.getId(), integration.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testImportSolution_integration_deniedWithoutCreatePermission() throws Exception {
        loginTenant1();
        Converter converter = createConverterOnCurrentTenant("Int Create Deny Converter");
        Integration integration = createIntegrationOnCurrentTenant(converter.getId(), "Int Create Deny");
        SolutionExportResponse exportResp = doExport(converter.getId(), integration.getId());

        loginTenant2();
        loginAsRestrictedTenantAdmin(tenantId2,
                Map.of(Resource.VERSION_CONTROL, List.of(Operation.READ, Operation.WRITE),
                        Resource.CONVERTER, List.of(Operation.READ, Operation.CREATE, Operation.WRITE),
                        Resource.INTEGRATION, List.of(Operation.READ))); // no CREATE

        doImportExpectStatus(exportResp.getSolution(), status().isForbidden());

        loginSysAdmin();
        Integration persisted = exportableEntitiesService.findEntityByTenantIdAndName(tenantId2, EntityType.INTEGRATION, "Int Create Deny");
        assertThat(persisted).as("Integration must not be persisted on permission denial").isNull();
    }

    @Test
    public void testImportSolution_integration_deniedWithoutWritePermission() throws Exception {
        loginTenant1();
        Converter converter = createConverterOnCurrentTenant("Int Write Deny Converter");
        Integration integration = createIntegrationOnCurrentTenant(converter.getId(), "Int Write Deny");
        SolutionExportResponse exportResp = doExport(converter.getId(), integration.getId());

        loginTenant2();
        // Pre-create a converter on tenant2 (FK target) with a different name to avoid collisions.
        Converter t2Converter = createConverterOnCurrentTenant("Int Write Deny T2 Converter");
        Integration t2Integration = new Integration();
        t2Integration.setName("Int Write Deny");
        t2Integration.setType(IntegrationType.HTTP);
        t2Integration.setDefaultConverterId(t2Converter.getId());
        t2Integration.setRoutingKey("t2-write-deny-rk");
        t2Integration.setSecret("scrt");
        t2Integration.setEnabled(false);
        t2Integration.setConfiguration(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        t2Integration.setAdditionalInfo(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        t2Integration.setExternalId(integration.getId());
        t2Integration = doPost("/api/integration", t2Integration, Integration.class);
        UUID priorId = t2Integration.getUuidId();

        loginAsRestrictedTenantAdmin(tenantId2,
                Map.of(Resource.VERSION_CONTROL, List.of(Operation.READ, Operation.WRITE),
                        Resource.CONVERTER, List.of(Operation.READ, Operation.CREATE, Operation.WRITE),
                        Resource.INTEGRATION, List.of(Operation.READ, Operation.CREATE))); // no WRITE

        doImportExpectStatus(exportResp.getSolution(), status().isForbidden());

        loginSysAdmin();
        Integration persisted = exportableEntitiesService.findEntityByTenantIdAndName(tenantId2, EntityType.INTEGRATION, "Int Write Deny");
        assertThat(persisted).isNotNull();
        assertThat(persisted.getUuidId()).isEqualTo(priorId);
        assertThat(persisted.getExternalId()).isEqualTo(integration.getId());
    }

    @Test
    public void testExportImportRoundTrip_integration_withPermissions() throws Exception {
        loginTenant1();
        Converter converter = createConverterOnCurrentTenant("Int Allow Converter");
        Integration integration = createIntegrationOnCurrentTenant(converter.getId(), "Int Allow");
        SolutionExportResponse exportResp = doExport(converter.getId(), integration.getId());

        loginTenant2();
        loginAsRestrictedTenantAdmin(tenantId2,
                Set.of(),
                Set.of(Resource.VERSION_CONTROL, Resource.CONVERTER, Resource.INTEGRATION));

        SolutionImportResult result = doImport(exportResp.getSolution());
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getCreated()).containsEntry(EntityType.INTEGRATION, 1);

        Integration imported = exportableEntitiesService.findEntityByTenantIdAndName(tenantId2, EntityType.INTEGRATION, "Int Allow");
        assertThat(imported).isNotNull();
        assertThat(imported.getTenantId()).isEqualTo(tenantId2);
        assertThat(imported.getExternalId()).isEqualTo(integration.getId());
    }

    // --- Per-type permission tests: DASHBOARD ---

    @Test
    public void testExportSolution_dashboard_deniedWithoutReadPermission() throws Exception {
        loginTenant1();
        Dashboard dashboard = saveDashboard("Dash Read Deny");

        loginAsRestrictedTenantAdmin(tenantId1,
                Map.of(Resource.VERSION_CONTROL, List.of(Operation.READ)));

        doPost("/api/solution/export", buildExportRequest(dashboard.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testImportSolution_dashboard_deniedWithoutCreatePermission() throws Exception {
        loginTenant1();
        Dashboard dashboard = saveDashboard("Dash Create Deny");
        SolutionExportResponse exportResp = doExport(dashboard.getId());

        loginTenant2();
        loginAsRestrictedTenantAdmin(tenantId2,
                Map.of(Resource.VERSION_CONTROL, List.of(Operation.READ, Operation.WRITE),
                        Resource.DASHBOARD, List.of(Operation.READ))); // no CREATE

        doImportExpectStatus(exportResp.getSolution(), status().isForbidden());

        loginSysAdmin();
        assertThat(findDashboardByTitle(tenantId2, "Dash Create Deny"))
                .as("Dashboard must not be persisted on permission denial").isNull();
    }

    @Test
    public void testImportSolution_dashboard_deniedWithoutWritePermission() throws Exception {
        loginTenant1();
        Dashboard dashboard = saveDashboard("Dash Write Deny");
        SolutionExportResponse exportResp = doExport(dashboard.getId());

        loginTenant2();
        // Pre-create matching Dashboard on tenant2 with same externalId so import enters WRITE branch.
        Dashboard t2Dashboard = new Dashboard();
        t2Dashboard.setTitle("Dash Write Deny");
        t2Dashboard.setExternalId(dashboard.getId());
        t2Dashboard = doPost("/api/dashboard", t2Dashboard, Dashboard.class);
        UUID priorId = t2Dashboard.getUuidId();

        loginAsRestrictedTenantAdmin(tenantId2,
                Map.of(Resource.VERSION_CONTROL, List.of(Operation.READ, Operation.WRITE),
                        Resource.DASHBOARD, List.of(Operation.READ, Operation.CREATE))); // no WRITE

        doImportExpectStatus(exportResp.getSolution(), status().isForbidden());

        loginSysAdmin();
        Dashboard persisted = findDashboardByTitle(tenantId2, "Dash Write Deny");
        assertThat(persisted).isNotNull();
        assertThat(persisted.getUuidId()).isEqualTo(priorId);
        assertThat(persisted.getExternalId()).isEqualTo(dashboard.getId());
    }

    @Test
    public void testExportImportRoundTrip_dashboard_withPermissions() throws Exception {
        loginTenant1();
        Dashboard dashboard = saveDashboard("Dash Allow");
        SolutionExportResponse exportResp = doExport(dashboard.getId());

        loginTenant2();
        loginAsRestrictedTenantAdmin(tenantId2,
                Set.of(),
                Set.of(Resource.VERSION_CONTROL, Resource.DASHBOARD));

        SolutionImportResult result = doImport(exportResp.getSolution());
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getCreated()).containsEntry(EntityType.DASHBOARD, 1);

        Dashboard imported = findDashboardByTitle(tenantId2, "Dash Allow");
        assertThat(imported).isNotNull();
        assertThat(imported.getTenantId()).isEqualTo(tenantId2);
        assertThat(imported.getExternalId()).isEqualTo(dashboard.getId());
    }

    // --- REST helper methods ---

    private SolutionExportResponse doExport(EntityId... entityIds) {
        return doExport(defaultExportSettings(), entityIds);
    }

    private static EntityExportSettings defaultExportSettings() {
        return EntityExportSettings.builder()
                .exportRelations(true).exportAttributes(true)
                .exportCredentials(false).exportCalculatedFields(true)
                .exportPermissions(true).exportGroupEntities(true)
                .embedGroupMembers(true).build();
    }

    private SolutionExportResponse doExport(EntityExportSettings settings, EntityId... entityIds) {
        SolutionExportRequest request = SolutionExportRequest.builder()
                .internalIds(Set.of(entityIds))
                .settings(settings)
                .build();
        return doPost("/api/solution/export", request, SolutionExportResponse.class);
    }

    private SolutionImportResult doImport(SolutionData solution) {
        return doPost("/api/solution/import", solution, SolutionImportResult.class);
    }

    private void doImportExpectStatus(SolutionData solution, org.springframework.test.web.servlet.ResultMatcher expectedStatus) throws Exception {
        doPost("/api/solution/import", solution).andExpect(expectedStatus);
    }

    private SolutionValidationResult doValidate(SolutionData solution) {
        return doPost("/api/solution/validate", solution, SolutionValidationResult.class);
    }

    private SolutionExportRequest buildExportRequest(EntityId... entityIds) {
        return SolutionExportRequest.builder()
                .internalIds(Set.of(entityIds))
                .settings(defaultExportSettings())
                .build();
    }

    private RuleChain findRuleChainByName(TenantId tenantId, String name) {
        Collection<RuleChain> chains = ruleChainService.findTenantRuleChainsByTypeAndName(tenantId, RuleChainType.CORE, name);
        return chains.isEmpty() ? null : chains.iterator().next();
    }

    private RuleChainId findInputNodeTargetChainId(RuleChainMetaData metaData) {
        return metaData.getNodes().stream()
                .filter(n -> "org.thingsboard.rule.engine.flow.TbRuleChainInputNode".equals(n.getType()))
                .map(n -> new RuleChainId(java.util.UUID.fromString(n.getConfiguration().get("ruleChainId").asText())))
                .findFirst().orElse(null);
    }

    private Dashboard findDashboardByTitle(TenantId tenantId, String title) {
        List<Dashboard> dashboards = dashboardService.findTenantDashboardsByTitle(tenantId, title);
        return dashboards.isEmpty() ? null : dashboards.get(0);
    }

    // --- Entity creation helpers ---

    private RuleChain saveRuleChain(String name) {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName(name);
        ruleChain.setType(RuleChainType.CORE);
        return doPost("/api/ruleChain", ruleChain, RuleChain.class);
    }

    private Dashboard saveDashboard(String title) {
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle(title);
        return doPost("/api/dashboard", dashboard, Dashboard.class);
    }

    private Customer saveCustomer(String title) {
        Customer customer = new Customer();
        customer.setTitle(title);
        return doPost("/api/customer", customer, Customer.class);
    }

    private Device createDeviceOnTenant1(DeviceProfileId deviceProfileId, String name) {
        Device device = new Device();
        device.setName(name);
        device.setDeviceProfileId(deviceProfileId);
        return doPost("/api/device", device, Device.class);
    }

    private Converter createConverterOnCurrentTenant(String name) {
        Converter converter = new Converter();
        converter.setType(ConverterType.DOWNLINK);
        converter.setName(name);
        converter.setConfiguration(JacksonUtil.newObjectNode()
                .<ObjectNode>set("encoder", new TextNode("b"))
                .set("decoder", new TextNode("c")));
        converter.setDebugSettings(DebugSettings.all());
        converter.setAdditionalInfo(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        return doPost("/api/converter", converter, Converter.class);
    }

    private Integration createIntegrationOnCurrentTenant(ConverterId converterId, String name) {
        Integration integration = new Integration();
        integration.setType(IntegrationType.HTTP);
        integration.setName(name);
        integration.setDefaultConverterId(converterId);
        integration.setRoutingKey("abc");
        integration.setSecret("scrt");
        integration.setEnabled(false);
        integration.setConfiguration(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        integration.setAdditionalInfo(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        return doPost("/api/integration", integration, Integration.class);
    }

    // --- User tests ---

    @Test
    public void testExportSolution_user_basicFields() throws Exception {
        loginTenant1();
        Customer customer = saveCustomer("User Test Customer");
        User user = createCustomerUser(customer, "user-basic@example.com", Authority.CUSTOMER_USER);

        SolutionExportResponse exportResp = doExport(customer.getId(), user.getId());

        List<EntityExportData<?>> users = exportResp.getSolution().getEntities().get(EntityType.USER);
        assertThat(users).hasSize(1);
        User exported = (User) users.get(0).getEntity();
        assertThat(exported.getEmail()).isEqualTo("user-basic@example.com");
        assertThat(exported.getAuthority()).isEqualTo(Authority.CUSTOMER_USER);
        assertThat(exported.getCustomMenuId()).isNull();
        assertThat(exported.getCustomerId().getId()).isEqualTo(customer.getId().getId());
    }

    @Test
    public void testExportSolution_user_dashboardRefsRemapped() throws Exception {
        loginTenant1();
        Dashboard defaultDash = saveDashboard("Default Dash");
        Dashboard homeDash = saveDashboard("Home Dash");
        Customer customer = saveCustomer("Dash User Customer");
        User user = createCustomerUserWithDashboards(customer, "user-dash@example.com",
                defaultDash.getId().getId(), homeDash.getId().getId());

        SolutionExportResponse exportResp = doExport(customer.getId(), defaultDash.getId(), homeDash.getId(), user.getId());

        User exported = (User) exportResp.getSolution().getEntities().get(EntityType.USER).get(0).getEntity();
        // External ids equal source internal ids on first export
        assertThat(exported.getAdditionalInfo().get("defaultDashboardId").asText())
                .isEqualTo(defaultDash.getId().getId().toString());
        assertThat(exported.getAdditionalInfo().get("homeDashboardId").asText())
                .isEqualTo(homeDash.getId().getId().toString());
    }

    @Test
    public void testExportSolution_user_dashboardRefWarning() throws Exception {
        loginTenant1();
        Dashboard dashboard = saveDashboard("Excluded Dash");
        Customer customer = saveCustomer("Dash Warn Customer");
        User user = createCustomerUserWithDashboards(customer, "user-warn@example.com",
                dashboard.getId().getId(), null);

        // Export user (and customer) but NOT the dashboard
        SolutionExportResponse exportResp = doExport(customer.getId(), user.getId());

        assertThat(exportResp.getWarnings())
                .anyMatch(w -> w.contains("USER") && w.contains("defaultDashboardId") && w.contains(dashboard.getId().getId().toString()));
    }

    @Test
    public void testExportImportRoundTrip_users() throws Exception {
        loginTenant1();
        Customer customer = saveCustomer("RoundTrip Customer");
        User user = createCustomerUser(customer, "user-roundtrip@example.com", Authority.CUSTOMER_USER);
        UserId sourceUserId = user.getId();

        SolutionExportResponse exportResp = doExport(customer.getId(), sourceUserId);

        // Email is globally unique; delete source user before importing into another tenant.
        userService.deleteUser(tenantId1, sourceUserId);

        loginTenant2();
        SolutionImportResult result = doImport(exportResp.getSolution());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getCreated()).containsEntry(EntityType.USER, 1);

        User imported = userService.findUserByTenantIdAndEmail(tenantId2, "user-roundtrip@example.com");
        assertThat(imported).isNotNull();
        assertThat(imported.getTenantId()).isEqualTo(tenantId2);
        assertThat(imported.getAuthority()).isEqualTo(Authority.CUSTOMER_USER);
        assertThat(imported.getExternalId()).isEqualTo(sourceUserId);

        Customer importedCustomer = exportableEntitiesService.findEntityByTenantIdAndName(tenantId2, EntityType.CUSTOMER, "RoundTrip Customer");
        assertThat(imported.getCustomerId()).isEqualTo(importedCustomer.getId());

        UserCredentials credentials = userService.findUserCredentialsByUserId(tenantId2, imported.getId());
        assertThat(credentials).isNotNull();
        assertThat(credentials.isEnabled()).isFalse();
        assertThat(credentials.getActivateToken()).isNotBlank();

        Map<UUID, UUID> idMap = result.getIdMapping();
        assertThat(idMap.get(sourceUserId.getId())).isEqualTo(imported.getUuidId());
    }

    @Test
    public void testImportSolution_userEmailConflict() throws Exception {
        // Build a synthetic payload by exporting from tenant1, deleting the source,
        // then mutating the payload's email to collide with a user that exists on tenant2.
        loginTenant1();
        Customer t1Customer = saveCustomer("T1 Conflict Customer");
        User t1User = createCustomerUser(t1Customer, "t1-conflict-source@example.com", Authority.CUSTOMER_USER);
        SolutionExportResponse exportResp = doExport(t1Customer.getId(), t1User.getId());
        userService.deleteUser(tenantId1, t1User.getId());

        loginTenant2();
        Customer t2Customer = saveCustomer("T2 Conflict Customer");
        createCustomerUser(t2Customer, "conflict@example.com", Authority.CUSTOMER_USER);

        // Mutate exported payload to use email that already exists on tenant2 (different externalId)
        SolutionData solution = exportResp.getSolution();
        ((User) solution.getEntities().get(EntityType.USER).get(0).getEntity()).setEmail("conflict@example.com");

        doImportExpectStatus(solution, status().isConflict());
    }

    @Test
    public void testImportSolution_userIdempotentReimport() throws Exception {
        loginTenant1();
        Customer customer = saveCustomer("Idem Customer");
        User user = createCustomerUser(customer, "user-idem@example.com", Authority.CUSTOMER_USER);

        SolutionExportResponse exportResp = doExport(customer.getId(), user.getId());

        // Same-tenant re-import: existing user is matched by id; saveUser updates without touching credentials.
        SolutionImportResult firstResult = doImport(exportResp.getSolution());
        assertThat(firstResult.isSuccess()).isTrue();
        UserId firstId = (UserId) userService.findUserByTenantIdAndEmail(tenantId1, "user-idem@example.com").getId();
        UserCredentials firstCreds = userService.findUserCredentialsByUserId(tenantId1, firstId);

        SolutionImportResult secondResult = doImport(exportResp.getSolution());
        assertThat(secondResult.isSuccess()).isTrue();
        UserId secondId = (UserId) userService.findUserByTenantIdAndEmail(tenantId1, "user-idem@example.com").getId();
        UserCredentials secondCreds = userService.findUserCredentialsByUserId(tenantId1, secondId);

        assertThat(secondId).isEqualTo(firstId);
        assertThat(secondCreds.getActivateToken()).isEqualTo(firstCreds.getActivateToken());
    }

    @Test
    public void testImportSolution_userAuthorityChangeRejected() throws Exception {
        loginTenant1();
        Customer customer = saveCustomer("Auth Change Customer");
        User user = createCustomerUser(customer, "user-auth@example.com", Authority.CUSTOMER_USER);
        UserId sourceUserId = user.getId();
        SolutionExportResponse exportResp = doExport(customer.getId(), sourceUserId);
        userService.deleteUser(tenantId1, sourceUserId);

        loginTenant2();
        // Pre-create on tenant2 with TENANT_ADMIN authority + matching externalId.
        User existing = new User();
        existing.setTenantId(tenantId2);
        existing.setEmail("user-auth@example.com");
        existing.setAuthority(Authority.TENANT_ADMIN);
        existing.setExternalId(sourceUserId);
        userService.saveUser(tenantId2, existing);

        doImportExpectStatus(exportResp.getSolution(), status().isConflict());
    }

    @Test
    public void testImportSolution_userSysAdminRejected() throws Exception {
        loginTenant1();
        Customer customer = saveCustomer("Sys Customer");
        User user = createCustomerUser(customer, "user-sys@example.com", Authority.CUSTOMER_USER);
        SolutionExportResponse exportResp = doExport(customer.getId(), user.getId());
        userService.deleteUser(tenantId1, user.getId());

        // Mutate the payload to use SYS_ADMIN authority
        SolutionData solution = exportResp.getSolution();
        ((User) solution.getEntities().get(EntityType.USER).get(0).getEntity()).setAuthority(Authority.SYS_ADMIN);

        loginTenant2();
        doImportExpectStatus(solution, status().isConflict());
    }

    @Test
    public void testValidateSolution_userConflicts() throws Exception {
        loginTenant1();
        Customer customer = saveCustomer("Validate Customer");
        User user1 = createCustomerUser(customer, "validate-user@example.com", Authority.CUSTOMER_USER);
        User user2 = createCustomerUser(customer, "validate-user-2@example.com", Authority.CUSTOMER_USER);
        SolutionExportResponse exportResp = doExport(customer.getId(), user1.getId(), user2.getId());

        // Inject duplicate email by setting user2's email to user1's (entity list order is not guaranteed, so locate by email).
        SolutionData solution = exportResp.getSolution();
        solution.getEntities().get(EntityType.USER).stream()
                .map(d -> (User) d.getEntity())
                .filter(u -> "validate-user-2@example.com".equals(u.getEmail()))
                .findFirst().orElseThrow()
                .setEmail("validate-user@example.com");

        loginTenant2();
        SolutionValidationResult validation = doValidate(solution);
        assertThat(validation.isValid()).isFalse();
        assertThat(validation.getConflicts()).anyMatch(c -> c.contains("duplicate email"));
    }

    @Test
    public void testExportSolution_userCannotExportFromOtherTenant() throws Exception {
        loginTenant1();
        Customer customer = saveCustomer("Cross Customer");
        User user = createCustomerUser(customer, "cross-tenant@example.com", Authority.CUSTOMER_USER);

        loginTenant2();
        doPost("/api/solution/export", buildExportRequest(user.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testExportImportRoundTrip_userInGroup() throws Exception {
        loginTenant1();
        Customer customer = saveCustomer("Group User Customer");
        EntityGroup userGroup = createUserGroupOnCustomer(customer.getId(), "Test User Group");
        User user = createCustomerUser(customer, "user-in-group@example.com", Authority.CUSTOMER_USER);
        entityGroupService.addEntityToEntityGroup(tenantId1, userGroup.getId(), user.getId());

        SolutionExportResponse exportResp = doExport(customer.getId(), userGroup.getId(), user.getId());
        userService.deleteUser(tenantId1, user.getId());

        loginTenant2();
        SolutionImportResult result = doImport(exportResp.getSolution());
        assertThat(result.isSuccess()).isTrue();

        User importedUser = userService.findUserByTenantIdAndEmail(tenantId2, "user-in-group@example.com");
        Customer importedCustomer = exportableEntitiesService.findEntityByTenantIdAndName(tenantId2, EntityType.CUSTOMER, "Group User Customer");
        EntityGroup importedGroup = entityGroupService.findEntityGroupByTypeAndName(tenantId2, importedCustomer.getId(), EntityType.USER, "Test User Group").orElseThrow();

        assertThat(entityGroupService.isEntityInGroup(tenantId2, importedUser.getId(), importedGroup.getId())).isTrue();
    }

    @Test
    public void testExportImportRoundTrip_assetInGroup() throws Exception {
        loginTenant1();
        Customer customer = saveCustomer("Asset Group Customer");
        EntityGroup assetGroup = createAssetGroupOnCustomer(customer.getId(), "Test Asset Group");

        AssetProfile ap = doPost("/api/assetProfile", createAssetProfile("Group AP"), AssetProfile.class);
        Asset asset = new Asset();
        asset.setName("Group Asset");
        asset.setAssetProfileId(ap.getId());
        asset.setCustomerId(customer.getId());
        asset = doPost("/api/asset", asset, Asset.class);
        entityGroupService.addEntityToEntityGroup(tenantId1, assetGroup.getId(), asset.getId());

        SolutionExportResponse exportResp = doExport(customer.getId(), assetGroup.getId(), ap.getId(), asset.getId());

        loginTenant2();
        SolutionImportResult result = doImport(exportResp.getSolution());
        assertThat(result.isSuccess()).isTrue();

        Asset importedAsset = exportableEntitiesService.findEntityByTenantIdAndName(tenantId2, EntityType.ASSET, "Group Asset");
        Customer importedCustomer = exportableEntitiesService.findEntityByTenantIdAndName(tenantId2, EntityType.CUSTOMER, "Asset Group Customer");
        EntityGroup importedGroup = entityGroupService.findEntityGroupByTypeAndName(tenantId2, importedCustomer.getId(), EntityType.ASSET, "Test Asset Group").orElseThrow();

        assertThat(entityGroupService.isEntityInGroup(tenantId2, importedAsset.getId(), importedGroup.getId())).isTrue();
    }

    @Test
    public void testImportSolution_groupMissingMember() throws Exception {
        loginTenant1();
        Customer customer = saveCustomer("Missing Member Customer");
        EntityGroup userGroup = createUserGroupOnCustomer(customer.getId(), "Missing Member Group");
        User user = createCustomerUser(customer, "missing-member@example.com", Authority.CUSTOMER_USER);
        entityGroupService.addEntityToEntityGroup(tenantId1, userGroup.getId(), user.getId());

        SolutionExportResponse exportResp = doExport(customer.getId(), userGroup.getId(), user.getId());

        // Tamper: replace memberIds with a random UUID not in solution
        SolutionData solution = exportResp.getSolution();
        EntityGroupExportData groupData = (EntityGroupExportData) solution.getEntities().get(EntityType.ENTITY_GROUP).get(0);
        groupData.setMemberIds(List.of(UUID.randomUUID()));
        // Also drop the user from the solution to ensure resolution fails
        solution.getEntities().remove(EntityType.USER);

        loginTenant2();
        SolutionValidationResult validation = doValidate(solution);
        assertThat(validation.isValid()).isFalse();
        assertThat(validation.getConflicts())
                .anyMatch(c -> c.contains("USER entity group 'Missing Member Group'")
                               && c.contains("not included in the solution"));
    }

    @Test
    public void testExportSolution_embedGroupMembersDisabled() throws Exception {
        // Regression guard for the VC backward-compat fix: with embedGroupMembers=false
        // (the default and the VC configuration), exported groups must not carry memberIds.
        loginTenant1();
        Customer customer = saveCustomer("VC Compat Customer");
        EntityGroup userGroup = createUserGroupOnCustomer(customer.getId(), "VC Compat Group");
        User user = createCustomerUser(customer, "vc-compat@example.com", Authority.CUSTOMER_USER);
        entityGroupService.addEntityToEntityGroup(tenantId1, userGroup.getId(), user.getId());

        EntityExportSettings vcLikeSettings = EntityExportSettings.builder()
                .exportRelations(true).exportAttributes(true)
                .exportCredentials(false).exportCalculatedFields(true)
                .exportPermissions(true).exportGroupEntities(true)
                .embedGroupMembers(false).build();
        SolutionExportResponse exportResp = doExport(vcLikeSettings, customer.getId(), userGroup.getId(), user.getId());

        EntityGroupExportData groupData = (EntityGroupExportData) exportResp.getSolution()
                .getEntities().get(EntityType.ENTITY_GROUP).get(0);
        assertThat(groupData.getMemberIds()).as("memberIds must be null when embedGroupMembers is disabled").isNull();
        assertThat(groupData.isGroupEntities()).as("groupEntities flag still reflects exportGroupEntities setting").isTrue();
    }

    @Test
    public void testExportSolution_memberIdsNullWhenGroupEntitiesDisabled() throws Exception {
        // Even with embedGroupMembers=true, exportGroupEntities=false must suppress memberIds.
        loginTenant1();
        Customer customer = saveCustomer("No Group Entities Customer");
        EntityGroup userGroup = createUserGroupOnCustomer(customer.getId(), "No Group Entities Group");
        User user = createCustomerUser(customer, "no-group-entities@example.com", Authority.CUSTOMER_USER);
        entityGroupService.addEntityToEntityGroup(tenantId1, userGroup.getId(), user.getId());

        EntityExportSettings settings = EntityExportSettings.builder()
                .exportRelations(true).exportAttributes(true)
                .exportCredentials(false).exportCalculatedFields(true)
                .exportPermissions(true).exportGroupEntities(false)
                .embedGroupMembers(true).build();
        SolutionExportResponse exportResp = doExport(settings, customer.getId(), userGroup.getId(), user.getId());

        EntityGroupExportData groupData = (EntityGroupExportData) exportResp.getSolution()
                .getEntities().get(EntityType.ENTITY_GROUP).get(0);
        assertThat(groupData.getMemberIds()).isNull();
        assertThat(groupData.isGroupEntities()).isFalse();
    }

    @Test
    public void testExportSolution_groupAllNeverEmbedsMembers() throws Exception {
        // The 'All' group's membership is implicit/platform-managed and must never be encoded.
        loginTenant1();
        Customer customer = saveCustomer("GroupAll Customer");
        EntityGroup allUserGroup = entityGroupService
                .findEntityGroupByTypeAndName(tenantId1, customer.getId(), EntityType.USER, "All")
                .orElseThrow();
        User user = createCustomerUser(customer, "group-all@example.com", Authority.CUSTOMER_USER);

        SolutionExportResponse exportResp = doExport(customer.getId(), allUserGroup.getId(), user.getId());

        EntityGroupExportData groupData = (EntityGroupExportData) exportResp.getSolution()
                .getEntities().get(EntityType.ENTITY_GROUP).get(0);
        assertThat(groupData.getEntity().isGroupAll()).isTrue();
        assertThat(groupData.getMemberIds()).as("All group must never carry memberIds").isNull();
    }

    @Test
    public void testExportSolution_userCustomMenuIdStripped() throws Exception {
        loginTenant1();
        Customer customer = saveCustomer("CustomMenu Customer");
        User user = new User();
        user.setTenantId(tenantId1);
        user.setCustomerId(customer.getId());
        user.setEmail("custom-menu@example.com");
        user.setAuthority(Authority.CUSTOMER_USER);
        user.setCustomMenuId(new CustomMenuId(UUID.randomUUID()));
        user = userService.saveUser(tenantId1, user);

        SolutionExportResponse exportResp = doExport(customer.getId(), user.getId());

        User exported = (User) exportResp.getSolution().getEntities().get(EntityType.USER).get(0).getEntity();
        assertThat(exported.getCustomMenuId()).as("customMenuId must be stripped on export").isNull();
    }

    @Test
    public void testImportSolution_memberResolvedByExternalId() throws Exception {
        // Branch 2 of wireGroupMembers: member is not in the solution, but exists on the target
        // tenant with a matching externalId.
        loginTenant1();
        Customer customer = saveCustomer("ExtId Resolve Customer");
        EntityGroup userGroup = createUserGroupOnCustomer(customer.getId(), "ExtId Resolve Group");
        User user = createCustomerUser(customer, "extid-resolve@example.com", Authority.CUSTOMER_USER);
        entityGroupService.addEntityToEntityGroup(tenantId1, userGroup.getId(), user.getId());
        UserId sourceUserId = user.getId();

        SolutionExportResponse exportResp = doExport(customer.getId(), userGroup.getId(), user.getId());

        SolutionData solution = exportResp.getSolution();
        solution.getEntities().remove(EntityType.USER);
        userService.deleteUser(tenantId1, sourceUserId);

        loginTenant2();
        Customer t2Customer = saveCustomer("ExtId Resolve T2 Customer");
        User t2User = new User();
        t2User.setTenantId(tenantId2);
        t2User.setCustomerId(t2Customer.getId());
        t2User.setEmail("extid-resolve@example.com");
        t2User.setAuthority(Authority.CUSTOMER_USER);
        t2User.setExternalId(sourceUserId);
        t2User = userService.saveUser(tenantId2, t2User);

        SolutionImportResult result = doImport(solution);
        assertThat(result.isSuccess()).isTrue();

        Customer importedCustomer = exportableEntitiesService.findEntityByTenantIdAndName(tenantId2, EntityType.CUSTOMER, "ExtId Resolve Customer");
        EntityGroup importedGroup = entityGroupService.findEntityGroupByTypeAndName(tenantId2, importedCustomer.getId(), EntityType.USER, "ExtId Resolve Group").orElseThrow();
        assertThat(entityGroupService.isEntityInGroup(tenantId2, t2User.getId(), importedGroup.getId()))
                .as("group wired via externalId fallback").isTrue();
    }

    @Test
    public void testImportSolution_memberResolvedByInternalId() throws Exception {
        // Branch 3 of wireGroupMembers: member not in solution, no externalId match, but
        // the memberId UUID happens to be the internal id of an existing target-tenant entity.
        loginTenant1();
        Customer customer = saveCustomer("IntId Resolve Customer");
        EntityGroup userGroup = createUserGroupOnCustomer(customer.getId(), "IntId Resolve Group");
        SolutionExportResponse exportResp = doExport(customer.getId(), userGroup.getId());

        loginTenant2();
        Customer t2Customer = saveCustomer("IntId Resolve T2 Customer");
        User t2User = createCustomerUser(t2Customer, "intid-resolve@example.com", Authority.CUSTOMER_USER);

        // Inject memberIds pointing at the tenant2 user's *internal* id (no externalId match).
        SolutionData solution = exportResp.getSolution();
        EntityGroupExportData groupData = (EntityGroupExportData) solution.getEntities()
                .get(EntityType.ENTITY_GROUP).get(0);
        groupData.setMemberIds(List.of(t2User.getId().getId()));

        SolutionImportResult result = doImport(solution);
        assertThat(result.isSuccess()).isTrue();

        Customer importedCustomer = exportableEntitiesService.findEntityByTenantIdAndName(tenantId2, EntityType.CUSTOMER, "IntId Resolve Customer");
        EntityGroup importedGroup = entityGroupService.findEntityGroupByTypeAndName(tenantId2, importedCustomer.getId(), EntityType.USER, "IntId Resolve Group").orElseThrow();
        assertThat(entityGroupService.isEntityInGroup(tenantId2, t2User.getId(), importedGroup.getId()))
                .as("group wired via internal-id fallback").isTrue();
    }

    @Test
    public void testImportSolution_userUpdatePath() throws Exception {
        // Re-importing a modified payload UPDATES the existing user record — same internal id,
        // mutated fields persisted, no duplicate creation.
        loginTenant1();
        Customer customer = saveCustomer("Update Path Customer");
        User user = createCustomerUser(customer, "user-update@example.com", Authority.CUSTOMER_USER);
        user.setFirstName("Original");
        user = userService.saveUser(tenantId1, user);
        UserId sourceUserId = user.getId();

        SolutionExportResponse exportResp = doExport(customer.getId(), sourceUserId);

        SolutionData solution = exportResp.getSolution();
        ((User) solution.getEntities().get(EntityType.USER).get(0).getEntity()).setFirstName("Updated");

        SolutionImportResult result = doImport(solution);
        assertThat(result.isSuccess()).isTrue();

        User updatedUser = userService.findUserByTenantIdAndEmail(tenantId1, "user-update@example.com");
        assertThat(updatedUser.getId()).as("same internal id (updated, not duplicated)").isEqualTo(sourceUserId);
        assertThat(updatedUser.getFirstName()).isEqualTo("Updated");
        assertThat(result.getCreated()).as("USER must not be reported as created on re-import").doesNotContainKey(EntityType.USER);
    }

    @Test
    public void testImportSolution_groupMembershipUpdatePath() throws Exception {
        // First import puts Alice in the group; second import (with Bob added to memberIds)
        // wires Bob in without recreating the group or duplicating Alice.
        loginTenant1();
        Customer customer = saveCustomer("Group Update Customer");
        EntityGroup userGroup = createUserGroupOnCustomer(customer.getId(), "Group Update Group");
        User alice = createCustomerUser(customer, "alice-update@example.com", Authority.CUSTOMER_USER);
        User bob = createCustomerUser(customer, "bob-update@example.com", Authority.CUSTOMER_USER);
        entityGroupService.addEntityToEntityGroup(tenantId1, userGroup.getId(), alice.getId());

        SolutionExportResponse firstExport = doExport(customer.getId(), userGroup.getId(), alice.getId(), bob.getId());

        entityGroupService.addEntityToEntityGroup(tenantId1, userGroup.getId(), bob.getId());
        SolutionExportResponse secondExport = doExport(customer.getId(), userGroup.getId(), alice.getId(), bob.getId());

        userService.deleteUser(tenantId1, alice.getId());
        userService.deleteUser(tenantId1, bob.getId());

        loginTenant2();
        SolutionImportResult firstResult = doImport(firstExport.getSolution());
        assertThat(firstResult.isSuccess()).isTrue();
        assertThat(firstResult.getCreated()).containsEntry(EntityType.ENTITY_GROUP, 1);

        Customer importedCustomer = exportableEntitiesService.findEntityByTenantIdAndName(tenantId2, EntityType.CUSTOMER, "Group Update Customer");
        EntityGroup importedGroup = entityGroupService.findEntityGroupByTypeAndName(tenantId2, importedCustomer.getId(), EntityType.USER, "Group Update Group").orElseThrow();
        EntityGroupId importedGroupId = importedGroup.getId();
        User importedAlice = userService.findUserByTenantIdAndEmail(tenantId2, "alice-update@example.com");
        User importedBob = userService.findUserByTenantIdAndEmail(tenantId2, "bob-update@example.com");
        assertThat(entityGroupService.isEntityInGroup(tenantId2, importedAlice.getId(), importedGroupId)).isTrue();
        assertThat(entityGroupService.isEntityInGroup(tenantId2, importedBob.getId(), importedGroupId)).isFalse();

        SolutionImportResult secondResult = doImport(secondExport.getSolution());
        assertThat(secondResult.isSuccess()).isTrue();
        assertThat(secondResult.getCreated()).as("group must not be re-created").doesNotContainKey(EntityType.ENTITY_GROUP);

        // Group itself is the same record; membership now contains both users.
        EntityGroup reFetched = entityGroupService.findEntityGroupByTypeAndName(tenantId2, importedCustomer.getId(), EntityType.USER, "Group Update Group").orElseThrow();
        assertThat(reFetched.getId()).isEqualTo(importedGroupId);
        assertThat(entityGroupService.isEntityInGroup(tenantId2, importedAlice.getId(), importedGroupId)).isTrue();
        assertThat(entityGroupService.isEntityInGroup(tenantId2, importedBob.getId(), importedGroupId)).isTrue();
    }

    // --- Helpers for user/group tests ---

    private User createCustomerUser(Customer customer, String email, Authority authority) {
        User user = new User();
        user.setTenantId(customer.getTenantId());
        user.setCustomerId(customer.getId());
        user.setEmail(email);
        user.setAuthority(authority);
        return userService.saveUser(customer.getTenantId(), user);
    }

    private User createCustomerUserWithDashboards(Customer customer, String email, UUID defaultDashboardId, UUID homeDashboardId) {
        User user = new User();
        user.setTenantId(customer.getTenantId());
        user.setCustomerId(customer.getId());
        user.setEmail(email);
        user.setAuthority(Authority.CUSTOMER_USER);
        ObjectNode additionalInfo = JacksonUtil.newObjectNode();
        if (defaultDashboardId != null) {
            additionalInfo.put("defaultDashboardId", defaultDashboardId.toString());
        }
        if (homeDashboardId != null) {
            additionalInfo.put("homeDashboardId", homeDashboardId.toString());
        }
        user.setAdditionalInfo(additionalInfo);
        return userService.saveUser(customer.getTenantId(), user);
    }

    private EntityGroup createUserGroupOnCustomer(CustomerId customerId, String name) {
        EntityGroup group = new EntityGroup();
        group.setName(name);
        group.setType(EntityType.USER);
        group.setOwnerId(customerId);
        return entityGroupService.saveEntityGroup(tenantId1, customerId, group);
    }

    private EntityGroup createAssetGroupOnCustomer(CustomerId customerId, String name) {
        EntityGroup group = new EntityGroup();
        group.setName(name);
        group.setType(EntityType.ASSET);
        group.setOwnerId(customerId);
        return entityGroupService.saveEntityGroup(tenantId1, customerId, group);
    }

    private void saveRuleChainMetaWithConnection(org.thingsboard.server.common.data.id.RuleChainId sourceId,
                                                 org.thingsboard.server.common.data.id.RuleChainId targetId,
                                                 String nodeName, TenantId tenantId) {
        RuleChainMetaData meta = new RuleChainMetaData();
        meta.setRuleChainId(sourceId);
        RuleNode node = new RuleNode();
        node.setName(nodeName);
        node.setType(TbGetAttributesNode.class.getName());
        node.setConfigurationVersion(TbGetAttributesNode.class.getAnnotation(org.thingsboard.rule.engine.api.RuleNode.class).version());
        TbGetAttributesNodeConfiguration config = new TbGetAttributesNodeConfiguration();
        config.setServerAttributeNames(Collections.singletonList("key"));
        node.setConfiguration(JacksonUtil.valueToTree(config));
        meta.setNodes(new ArrayList<>(List.of(node)));
        meta.setFirstNodeIndex(0);
        RuleChainConnectionInfo conn = new RuleChainConnectionInfo();
        conn.setFromIndex(0);
        conn.setTargetRuleChainId(targetId);
        conn.setType(TbNodeConnectionType.SUCCESS);
        conn.setAdditionalInfo(JacksonUtil.newObjectNode());
        meta.setRuleChainConnections(new ArrayList<>(List.of(conn)));
        ruleChainService.saveRuleChainMetaData(tenantId, meta, Function.identity());
    }

}
