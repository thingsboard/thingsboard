/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.controller.sql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Streams;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.controller.BaseEntitiesExportImportControllerTest;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.action.EntityActionService;
import org.thingsboard.server.service.ota.OtaPackageStateService;
import org.thingsboard.server.service.sync.exportimport.exporting.data.DeviceExportData;
import org.thingsboard.server.service.sync.exportimport.exporting.data.EntityExportData;
import org.thingsboard.server.service.sync.exportimport.exporting.data.RuleChainExportData;
import org.thingsboard.server.service.sync.vc.data.request.create.EntitiesByCustomFilterVersionCreateConfig;
import org.thingsboard.server.service.sync.exportimport.exporting.data.EntityExportSettings;
import org.thingsboard.server.service.sync.vc.data.request.create.EntityListVersionCreateConfig;
import org.thingsboard.server.service.sync.vc.data.request.create.EntityTypeVersionCreateConfig;
import org.thingsboard.server.service.sync.vc.data.request.create.VersionCreateConfig;
import org.thingsboard.server.service.sync.vc.data.request.create.SingleEntityVersionCreateConfig;
import org.thingsboard.server.service.sync.exportimport.importing.data.EntityImportResult;
import org.thingsboard.server.service.sync.exportimport.importing.data.EntityImportSettings;
import org.thingsboard.server.service.sync.exportimport.importing.data.ImportRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;

@DaoSqlTest
public class EntitiesExportImportControllerSqlTest extends BaseEntitiesExportImportControllerTest {

    @Autowired
    private DeviceCredentialsService deviceCredentialsService;
    @SpyBean
    private EntityActionService entityActionService;
    @SpyBean
    private TbClusterService clusterService;
    @SpyBean
    private OtaPackageStateService otaPackageStateService;

    @Test
    public void testExportImportAsset_betweenTenants() throws Exception {
        logInAsTenantAdmin1();
        Asset asset = createAsset(tenantId1, null, "AB", "Asset of tenant 1");
        EntityExportData<Asset> exportData = exportSingleEntity(asset.getId());
        assertThat(exportData.getEntity()).isEqualTo(asset);

        logInAsTenantAdmin2();
        EntityImportResult<Asset> importResult = importEntity(exportData);

        checkImportedEntity(tenantId1, asset, tenantId2, importResult.getSavedEntity());
        checkImportedAssetData(asset, importResult.getSavedEntity());
    }

    @Test
    public void testExportImportAsset_sameTenant() throws Exception {
        logInAsTenantAdmin1();
        Asset asset = createAsset(tenantId1, null, "AB", "Asset v1.0");
        EntityExportData<Asset> exportData = exportSingleEntity(asset.getId());

        EntityImportResult<Asset> importResult = importEntity(exportData);

        checkImportedEntity(tenantId1, asset, tenantId1, importResult.getSavedEntity());
        checkImportedAssetData(asset, importResult.getSavedEntity());
    }

    @Test
    public void testExportImportAsset_sameTenant_withCustomer() throws Exception {
        logInAsTenantAdmin1();
        Customer customer = createCustomer(tenantId1, "My customer");
        Asset asset = createAsset(tenantId1, customer.getId(), "AB", "My asset");

        Asset importedAsset = importEntity(this.<Asset, AssetId>exportSingleEntity(asset.getId())).getSavedEntity();

        assertThat(importedAsset.getCustomerId()).isEqualTo(asset.getCustomerId());
    }


    @Test
    public void testExportImportCustomer_betweenTenants() throws Exception {
        logInAsTenantAdmin1();
        Customer customer = createCustomer(tenantAdmin1.getTenantId(), "Customer of tenant 1");
        EntityExportData<Customer> exportData = exportSingleEntity(customer.getId());
        assertThat(exportData.getEntity()).isEqualTo(customer);

        logInAsTenantAdmin2();
        EntityImportResult<Customer> importResult = importEntity(exportData);

        checkImportedEntity(tenantId1, customer, tenantId2, importResult.getSavedEntity());
        checkImportedCustomerData(customer, importResult.getSavedEntity());
    }

    @Test
    public void testExportImportCustomer_sameTenant() throws Exception {
        logInAsTenantAdmin1();
        Customer customer = createCustomer(tenantAdmin1.getTenantId(), "Customer v1.0");
        EntityExportData<Customer> exportData = exportSingleEntity(customer.getId());

        EntityImportResult<Customer> importResult = importEntity(exportData);

        checkImportedEntity(tenantId1, customer, tenantId1, importResult.getSavedEntity());
        checkImportedCustomerData(customer, importResult.getSavedEntity());
    }


    @Test
    public void testExportImportDeviceWithProfile_betweenTenants() throws Exception {
        logInAsTenantAdmin1();
        DeviceProfile deviceProfile = createDeviceProfile(tenantId1, null, null, "Device profile of tenant 1");
        Device device = createDevice(tenantId1, null, deviceProfile.getId(), "Device of tenant 1");
        DeviceCredentials credentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId1, device.getId());

        EntityExportData<DeviceProfile> profileExportData = exportSingleEntity(deviceProfile.getId());
        assertThat(profileExportData.getEntity()).isEqualTo(deviceProfile);

        EntityExportData<Device> deviceExportData = exportSingleEntity(device.getId());
        assertThat(deviceExportData.getEntity()).isEqualTo(device);
        assertThat(((DeviceExportData) deviceExportData).getCredentials()).isEqualTo(credentials);
        DeviceCredentials exportedCredentials = ((DeviceExportData) deviceExportData).getCredentials();
        exportedCredentials.setCredentialsId(credentials.getCredentialsId() + "a");

        logInAsTenantAdmin2();
        EntityImportResult<DeviceProfile> profileImportResult = importEntity(profileExportData);
        checkImportedEntity(tenantId1, deviceProfile, tenantId2, profileImportResult.getSavedEntity());
        checkImportedDeviceProfileData(deviceProfile, profileImportResult.getSavedEntity());


        EntityImportResult<Device> deviceImportResult = importEntity(deviceExportData);
        Device importedDevice = deviceImportResult.getSavedEntity();
        checkImportedEntity(tenantId1, device, tenantId2, deviceImportResult.getSavedEntity());
        checkImportedDeviceData(device, importedDevice);

        assertThat(importedDevice.getDeviceProfileId()).isEqualTo(profileImportResult.getSavedEntity().getId());

        DeviceCredentials importedCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId2, importedDevice.getId());
        assertThat(importedCredentials.getId()).isNotEqualTo(credentials.getId());
        assertThat(importedCredentials.getCredentialsId()).isEqualTo(exportedCredentials.getCredentialsId());
        assertThat(importedCredentials.getCredentialsValue()).isEqualTo(credentials.getCredentialsValue());
        assertThat(importedCredentials.getCredentialsType()).isEqualTo(credentials.getCredentialsType());
    }

    @Test
    public void testExportImportDevice_sameTenant() throws Exception {
        logInAsTenantAdmin1();
        DeviceProfile deviceProfile = createDeviceProfile(tenantId1, null, null, "Device profile v1.0");
        OtaPackage firmware = createOtaPackage(tenantId1, deviceProfile.getId(), OtaPackageType.FIRMWARE);
        OtaPackage software = createOtaPackage(tenantId1, deviceProfile.getId(), OtaPackageType.SOFTWARE);
        Device device = createDevice(tenantId1, null, deviceProfile.getId(), "Device v1.0");
        device.setFirmwareId(firmware.getId());
        device.setSoftwareId(software.getId());
        device = deviceService.saveDevice(device);

        DeviceCredentials credentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId1, device.getId());

        EntityExportData<Device> deviceExportData = exportSingleEntity(device.getId());

        EntityImportResult<Device> importResult = importEntity(deviceExportData);
        Device importedDevice = importResult.getSavedEntity();

        checkImportedEntity(tenantId1, device, tenantId1, importResult.getSavedEntity());
        assertThat(importedDevice.getDeviceProfileId()).isEqualTo(device.getDeviceProfileId());
        assertThat(deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId1, device.getId())).isEqualTo(credentials);
        assertThat(importedDevice.getFirmwareId()).isEqualTo(firmware.getId());
        assertThat(importedDevice.getSoftwareId()).isEqualTo(software.getId());
    }


    @Test
    public void testExportImportDashboard_betweenTenants() throws Exception {
        logInAsTenantAdmin1();
        Dashboard dashboard = createDashboard(tenantAdmin1.getTenantId(), null, "Dashboard of tenant 1");

        EntityExportData<Dashboard> exportData = exportSingleEntity(dashboard.getId());
        assertThat(exportData.getEntity()).isEqualTo(dashboard);

        logInAsTenantAdmin2();
        EntityImportResult<Dashboard> importResult = importEntity(exportData);
        checkImportedEntity(tenantId1, dashboard, tenantId2, importResult.getSavedEntity());
        checkImportedDashboardData(dashboard, importResult.getSavedEntity());
    }

    @Test
    public void testExportImportDashboard_sameTenant() throws Exception {
        logInAsTenantAdmin1();
        Dashboard dashboard = createDashboard(tenantAdmin1.getTenantId(), null, "Dashboard v1.0");

        EntityExportData<Dashboard> exportData = exportSingleEntity(dashboard.getId());

        EntityImportResult<Dashboard> importResult = importEntity(exportData);
        checkImportedEntity(tenantId1, dashboard, tenantId1, importResult.getSavedEntity());
        checkImportedDashboardData(dashboard, importResult.getSavedEntity());
    }

    @Test
    public void testExportImportDashboard_betweenTenants_withCustomer_updated() throws Exception {
        logInAsTenantAdmin1();
        Dashboard dashboard = createDashboard(tenantAdmin1.getTenantId(), null, "Dashboard of tenant 1");

        EntityExportData<Dashboard> exportData = exportSingleEntity(dashboard.getId());

        logInAsTenantAdmin2();
        Dashboard importedDashboard = (Dashboard) importEntities(List.of(exportData)).get(0).getSavedEntity();
        checkImportedEntity(tenantId1, dashboard, tenantId2, importedDashboard);

        logInAsTenantAdmin1();
        Customer customer = createCustomer(tenantId1, "Customer 1");
        EntityExportData<Customer> customerExportData = exportSingleEntity(customer.getId());
        dashboardService.assignDashboardToCustomer(tenantId1, dashboard.getId(), customer.getId());
        exportData = exportSingleEntity(dashboard.getId());

        logInAsTenantAdmin2();
        Customer importedCustomer = (Customer) importEntities(List.of(customerExportData)).get(0).getSavedEntity();
        importedDashboard = (Dashboard) importEntities(List.of(exportData)).get(0).getSavedEntity();
        assertThat(importedDashboard.getAssignedCustomers()).hasOnlyOneElementSatisfying(customerInfo -> {
            assertThat(customerInfo.getCustomerId()).isEqualTo(importedCustomer.getId());
        });
    }

    @Test
    public void testExportImportDashboard_betweenTenants_withEntityAliases() throws Exception {
        logInAsTenantAdmin1();
        Asset asset1 = createAsset(tenantId1, null, "A", "Asset 1");
        Asset asset2 = createAsset(tenantId1, null, "A", "Asset 2");
        Dashboard dashboard = createDashboard(tenantId1, null, "Dashboard 1");

        String entityAliases = "{\n" +
                "\t\"23c4185d-1497-9457-30b2-6d91e69a5b2c\": {\n" +
                "\t\t\"alias\": \"assets\",\n" +
                "\t\t\"filter\": {\n" +
                "\t\t\t\"entityList\": [\n" +
                "\t\t\t\t\"" + asset1.getId().toString() + "\",\n" +
                "\t\t\t\t\"" + asset2.getId().toString() + "\"\n" +
                "\t\t\t],\n" +
                "\t\t\t\"entityType\": \"ASSET\",\n" +
                "\t\t\t\"resolveMultiple\": true,\n" +
                "\t\t\t\"type\": \"entityList\"\n" +
                "\t\t},\n" +
                "\t\t\"id\": \"23c4185d-1497-9457-30b2-6d91e69a5b2c\"\n" +
                "\t}\n" +
                "}";
        ObjectNode dashboardConfiguration = JacksonUtil.newObjectNode();
        dashboardConfiguration.set("entityAliases", JacksonUtil.toJsonNode(entityAliases));
        dashboardConfiguration.set("description", new TextNode("hallo"));
        dashboard.setConfiguration(dashboardConfiguration);
        dashboard = dashboardService.saveDashboard(dashboard);

        EntityTypeVersionCreateConfig assetsExportRequest = new EntityTypeVersionCreateConfig();
        assetsExportRequest.setEntityType(EntityType.ASSET);
        assetsExportRequest.setPageSize(10);
        assetsExportRequest.setExportSettings(new EntityExportSettings());
        EntityTypeVersionCreateConfig dashboardsExportRequest = new EntityTypeVersionCreateConfig();
        dashboardsExportRequest.setEntityType(EntityType.DASHBOARD);
        dashboardsExportRequest.setPageSize(10);
        dashboardsExportRequest.setExportSettings(new EntityExportSettings());
        List<EntityExportData<?>> exportDataList = exportEntities(List.of(assetsExportRequest, dashboardsExportRequest));

        logInAsTenantAdmin2();
        Map<EntityType, List<EntityImportResult<?>>> importResults = importEntities(exportDataList).stream().collect(Collectors.groupingBy(EntityImportResult::getEntityType));
        Asset importedAsset1 = (Asset) importResults.get(EntityType.ASSET).get(0).getSavedEntity();
        Asset importedAsset2 = (Asset) importResults.get(EntityType.ASSET).get(1).getSavedEntity();
        Dashboard importedDashboard = (Dashboard) importResults.get(EntityType.DASHBOARD).get(0).getSavedEntity();

        Set<String> entityAliasEntitiesIds = Streams.stream(importedDashboard.getConfiguration()
                        .get("entityAliases").elements().next().get("filter").get("entityList").elements())
                .map(JsonNode::asText).collect(Collectors.toSet());
        assertThat(entityAliasEntitiesIds).doesNotContain(asset1.getId().toString(), asset2.getId().toString());
        assertThat(entityAliasEntitiesIds).contains(importedAsset1.getId().toString(), importedAsset2.getId().toString());
    }


    @Test
    public void testExportImportRuleChain_betweenTenants() throws Exception {
        logInAsTenantAdmin1();
        RuleChain ruleChain = createRuleChain(tenantId1, "Rule chain of tenant 1");
        RuleChainMetaData metaData = ruleChainService.loadRuleChainMetaData(tenantId1, ruleChain.getId());

        EntityExportData<RuleChain> exportData = exportSingleEntity(ruleChain.getId());
        assertThat(exportData.getEntity()).isEqualTo(ruleChain);
        assertThat(((RuleChainExportData) exportData).getMetaData()).isEqualTo(metaData);

        logInAsTenantAdmin2();
        EntityImportResult<RuleChain> importResult = importEntity(exportData);
        RuleChain importedRuleChain = importResult.getSavedEntity();
        RuleChainMetaData importedMetaData = ruleChainService.loadRuleChainMetaData(tenantId2, importedRuleChain.getId());

        checkImportedEntity(tenantId1, ruleChain, tenantId2, importResult.getSavedEntity());
        checkImportedRuleChainData(ruleChain, metaData, importedRuleChain, importedMetaData);
    }

    @Test
    public void testExportImportRuleChain_sameTenant() throws Exception {
        logInAsTenantAdmin1();
        RuleChain ruleChain = createRuleChain(tenantId1, "Rule chain v1.0");
        RuleChainMetaData metaData = ruleChainService.loadRuleChainMetaData(tenantId1, ruleChain.getId());

        EntityExportData<RuleChain> exportData = exportSingleEntity(ruleChain.getId());

        EntityImportResult<RuleChain> importResult = importEntity(exportData);
        RuleChain importedRuleChain = importResult.getSavedEntity();
        RuleChainMetaData importedMetaData = ruleChainService.loadRuleChainMetaData(tenantId1, importedRuleChain.getId());

        checkImportedEntity(tenantId1, ruleChain, tenantId1, importResult.getSavedEntity());
        checkImportedRuleChainData(ruleChain, metaData, importedRuleChain, importedMetaData);
    }


    @Test
    public void testExportImportBatch_betweenTenants() throws Exception {
        logInAsTenantAdmin1();

        Customer customer = createCustomer(tenantId1, "Customer 1");
        Asset asset = createAsset(tenantId1, customer.getId(), "A", "Customer 1 - Asset 1");
        RuleChain ruleChain = createRuleChain(tenantId1, "Rule chain 1");
        Dashboard dashboard = createDashboard(tenantId1, customer.getId(), "Customer 1 - Dashboard 1");
        DeviceProfile deviceProfile = createDeviceProfile(tenantId1, ruleChain.getId(), dashboard.getId(), "Device profile 1");
        Device device = createDevice(tenantId1, customer.getId(), deviceProfile.getId(), "Customer 1 - Device 1");

        EntityListVersionCreateConfig exportRequest = new EntityListVersionCreateConfig();
        exportRequest.setExportSettings(new EntityExportSettings());
        exportRequest.setEntitiesIds(List.of(customer.getId(), asset.getId(), ruleChain.getId(), deviceProfile.getId(), dashboard.getId()));
        List<EntityExportData<?>> exportDataList = exportEntities(exportRequest);
        exportRequest.setEntitiesIds(List.of(device.getId()));
        DeviceExportData deviceExportData = (DeviceExportData) exportEntities(exportRequest).get(0);
        deviceExportData.getCredentials().setCredentialsId(RandomStringUtils.randomAlphanumeric(10));
        exportDataList.add(deviceExportData);

        logInAsTenantAdmin2();
        ImportRequest importRequest = new ImportRequest();
        importRequest.setImportSettings(new EntityImportSettings());
        importRequest.setExportDataList(exportDataList);
        Map<EntityType, EntityImportResult<?>> importResults = importEntities(importRequest).stream()
                .collect(Collectors.toMap(EntityImportResult::getEntityType, r -> r));

        Customer importedCustomer = (Customer) importResults.get(EntityType.CUSTOMER).getSavedEntity();
        checkImportedEntity(tenantId1, customer, tenantId2, importedCustomer);

        Asset importedAsset = (Asset) importResults.get(EntityType.ASSET).getSavedEntity();
        checkImportedEntity(tenantId1, asset, tenantId2, importedAsset);
        assertThat(importedAsset.getCustomerId()).isEqualTo(importedCustomer.getId());

        RuleChain importedRuleChain = (RuleChain) importResults.get(EntityType.RULE_CHAIN).getSavedEntity();
        checkImportedEntity(tenantId1, ruleChain, tenantId2, importedRuleChain);

        Dashboard importedDashboard = (Dashboard) importResults.get(EntityType.DASHBOARD).getSavedEntity();
        checkImportedEntity(tenantId1, dashboard, tenantId2, importedDashboard);
        assertThat(importedDashboard.getAssignedCustomers()).size().isOne();
        assertThat(importedDashboard.getAssignedCustomers()).hasOnlyOneElementSatisfying(customerInfo -> {
            assertThat(customerInfo.getCustomerId()).isEqualTo(importedCustomer.getId());
        });

        DeviceProfile importedDeviceProfile = (DeviceProfile) importResults.get(EntityType.DEVICE_PROFILE).getSavedEntity();
        checkImportedEntity(tenantId1, deviceProfile, tenantId2, importedDeviceProfile);
        assertThat(importedDeviceProfile.getDefaultRuleChainId()).isEqualTo(importedRuleChain.getId());
        assertThat(importedDeviceProfile.getDefaultDashboardId()).isEqualTo(importedDashboard.getId());

        Device importedDevice = (Device) importResults.get(EntityType.DEVICE).getSavedEntity();
        checkImportedEntity(tenantId1, device, tenantId2, importedDevice);
        assertThat(importedDevice.getCustomerId()).isEqualTo(importedCustomer.getId());
        assertThat(importedDevice.getDeviceProfileId()).isEqualTo(importedDeviceProfile.getId());
    }


    @Test
    public void testExportImportWithInboundRelations_betweenTenants() throws Exception {
        logInAsTenantAdmin1();

        Asset asset = createAsset(tenantId1, null, "A", "Asset 1");
        Device device = createDevice(tenantId1, null, null, "Device 1");
        EntityRelation relation = createRelation(asset.getId(), device.getId());

        EntityListVersionCreateConfig exportRequest = new EntityListVersionCreateConfig();
        exportRequest.setEntitiesIds(List.of(asset.getId(), device.getId()));
        exportRequest.setExportSettings(EntityExportSettings.builder()
                .exportRelations(true)
                .build());
        List<EntityExportData<?>> exportDataList = exportEntities(exportRequest);

        EntityExportData<?> deviceExportData = exportDataList.stream().filter(exportData -> exportData.getEntityType() == EntityType.DEVICE).findFirst().orElse(null);
        assertThat(deviceExportData.getRelations()).size().isOne();
        assertThat(deviceExportData.getRelations().get(0)).matches(entityRelation -> {
            return entityRelation.getFrom().equals(asset.getId()) && entityRelation.getTo().equals(device.getId());
        });
        ((DeviceExportData) deviceExportData).getCredentials().setCredentialsId("ab");
        ((Device) deviceExportData.getEntity()).setDeviceProfileId(null);

        logInAsTenantAdmin2();

        ImportRequest importRequest = new ImportRequest();
        importRequest.setExportDataList(exportDataList);
        importRequest.setImportSettings(EntityImportSettings.builder()
                .updateRelations(true)
                .build());
        Map<EntityType, EntityImportResult<?>> importResults = importEntities(importRequest).stream().collect(Collectors.toMap(EntityImportResult::getEntityType, r -> r));

        Device importedDevice = (Device) importResults.get(EntityType.DEVICE).getSavedEntity();
        Asset importedAsset = (Asset) importResults.get(EntityType.ASSET).getSavedEntity();
        checkImportedEntity(tenantId1, device, tenantId2, importedDevice);
        checkImportedEntity(tenantId1, asset, tenantId2, importedAsset);

        List<EntityRelation> importedRelations = relationService.findByTo(TenantId.SYS_TENANT_ID, importedDevice.getId(), RelationTypeGroup.COMMON);
        assertThat(importedRelations).size().isOne();
        assertThat(importedRelations.get(0)).satisfies(importedRelation -> {
            assertThat(importedRelation.getFrom()).isEqualTo(importedAsset.getId());
            assertThat(importedRelation.getType()).isEqualTo(relation.getType());
            assertThat(importedRelation.getAdditionalInfo()).isEqualTo(relation.getAdditionalInfo());
        });
    }

    @Test
    public void testExportImportWithRelations_betweenTenants() throws Exception {
        logInAsTenantAdmin1();

        Asset asset = createAsset(tenantId1, null, "A", "Asset 1");
        Device device = createDevice(tenantId1, null, null, "Device 1");
        EntityRelation relation = createRelation(asset.getId(), device.getId());

        EntityListVersionCreateConfig exportRequest = new EntityListVersionCreateConfig();
        exportRequest.setEntitiesIds(List.of(asset.getId(), device.getId()));
        exportRequest.setExportSettings(EntityExportSettings.builder()
                .exportRelations(true)
                .build());
        List<EntityExportData<?>> exportDataList = exportEntities(exportRequest);

        assertThat(exportDataList).allMatch(exportData -> exportData.getRelations().size() == 1);

        EntityExportData<?> deviceExportData = exportDataList.stream().filter(exportData -> exportData.getEntityType() == EntityType.DEVICE).findFirst().orElse(null);
        ((DeviceExportData) deviceExportData).getCredentials().setCredentialsId("ab");
        ((Device) deviceExportData.getEntity()).setDeviceProfileId(null);

        logInAsTenantAdmin2();

        ImportRequest importRequest = new ImportRequest();
        importRequest.setExportDataList(exportDataList);
        importRequest.setImportSettings(EntityImportSettings.builder()
                .updateRelations(true)
                .build());
        Map<EntityType, EntityImportResult<?>> importResults = importEntities(importRequest).stream().collect(Collectors.toMap(EntityImportResult::getEntityType, r -> r));

        Device importedDevice = (Device) importResults.get(EntityType.DEVICE).getSavedEntity();
        Asset importedAsset = (Asset) importResults.get(EntityType.ASSET).getSavedEntity();

        List<EntityRelation> importedRelations = relationService.findByTo(TenantId.SYS_TENANT_ID, importedDevice.getId(), RelationTypeGroup.COMMON);
        assertThat(importedRelations).size().isOne();
        assertThat(importedRelations.get(0)).satisfies(importedRelation -> {
            assertThat(importedRelation.getFrom()).isEqualTo(importedAsset.getId());
            assertThat(importedRelation.getType()).isEqualTo(relation.getType());
            assertThat(importedRelation.getAdditionalInfo()).isEqualTo(relation.getAdditionalInfo());
        });
    }

    @Test
    public void testExportImportWithRelations_sameTenant() throws Exception {
        logInAsTenantAdmin1();

        Asset asset = createAsset(tenantId1, null, "A", "Asset 1");
        Device device1 = createDevice(tenantId1, null, null, "Device 1");
        EntityRelation relation1 = createRelation(asset.getId(), device1.getId());

        SingleEntityVersionCreateConfig exportRequest = new SingleEntityVersionCreateConfig();
        exportRequest.setEntityId(asset.getId());
        exportRequest.setExportSettings(EntityExportSettings.builder()
                .exportRelations(true)
                .build());
        EntityExportData<Asset> assetExportData = (EntityExportData<Asset>) exportEntities(exportRequest).get(0);
        assertThat(assetExportData.getRelations()).size().isOne();

        Device device2 = createDevice(tenantId1, null, null, "Device 2");
        EntityRelation relation2 = createRelation(asset.getId(), device2.getId());

        ImportRequest importRequest = new ImportRequest();
        importRequest.setExportDataList(List.of(assetExportData));
        importRequest.setImportSettings(EntityImportSettings.builder()
                .updateRelations(true)
                .build());

        importEntities(importRequest);

        List<EntityRelation> relations = relationService.findByFrom(TenantId.SYS_TENANT_ID, asset.getId(), RelationTypeGroup.COMMON);
        assertThat(relations).contains(relation1);
        assertThat(relations).doesNotContain(relation2);
    }

    @Test
    public void textExportImportWithRelations_sameTenant_removeExisting() throws Exception {
        logInAsTenantAdmin1();

        Asset asset1 = createAsset(tenantId1, null, "A", "Asset 1");
        Device device = createDevice(tenantId1, null, null, "Device 1");
        EntityRelation relation1 = createRelation(asset1.getId(), device.getId());

        SingleEntityVersionCreateConfig exportRequest = new SingleEntityVersionCreateConfig();
        exportRequest.setEntityId(device.getId());
        exportRequest.setExportSettings(EntityExportSettings.builder()
                .exportRelations(true)
                .build());
        EntityExportData<?> deviceExportData = exportEntities(exportRequest).get(0);
        assertThat(deviceExportData.getRelations()).size().isOne();

        Asset asset2 = createAsset(tenantId1, null, "A", "Asset 2");
        EntityRelation relation2 = createRelation(asset2.getId(), device.getId());

        ImportRequest importRequest = new ImportRequest();
        importRequest.setExportDataList(List.of(deviceExportData));
        importRequest.setImportSettings(EntityImportSettings.builder()
                .updateRelations(true)
                .build());

        importEntities(importRequest);

        List<EntityRelation> relations = relationService.findByTo(TenantId.SYS_TENANT_ID, device.getId(), RelationTypeGroup.COMMON);
        assertThat(relations).contains(relation1);
        assertThat(relations).doesNotContain(relation2);
    }


    @Test
    public void testExportImportDeviceProfile_betweenTenants_findExistingByName() throws Exception {
        logInAsTenantAdmin1();
        DeviceProfile defaultDeviceProfile = deviceProfileService.findDefaultDeviceProfile(tenantId1);

        EntityListVersionCreateConfig exportRequest = new EntityListVersionCreateConfig();
        exportRequest.setEntitiesIds(List.of(defaultDeviceProfile.getId()));
        exportRequest.setExportSettings(new EntityExportSettings());
        List<EntityExportData<?>> exportDataList = exportEntities(exportRequest);

        logInAsTenantAdmin2();
        ImportRequest importRequest = new ImportRequest();
        importRequest.setExportDataList(exportDataList);
        importRequest.setImportSettings(EntityImportSettings.builder()
                .findExistingByName(false)
                .build());
        assertThatThrownBy(() -> {
            importEntities(importRequest);
        }).hasMessageContaining("default device profile is present");

        importRequest.getImportSettings().setFindExistingByName(true);
        importEntities(importRequest);
        checkImportedEntity(tenantId1, defaultDeviceProfile, tenantId2, deviceProfileService.findDefaultDeviceProfile(tenantId2));
    }


    @Test
    public void testExportRequests() throws Exception {
        logInAsTenantAdmin1();

        Device device = createDevice(tenantId1, null, null, "Device 1");
        DeviceProfile deviceProfile = createDeviceProfile(tenantId1, null, null, "Device profile 1");
        RuleChain ruleChain = createRuleChain(tenantId1, "Rule chain 1");
        Asset asset = createAsset(tenantId1, null, "A", "Asset 1");
        Dashboard dashboard = createDashboard(tenantId1, null, "Dashboard 1");
        Customer customer = createCustomer(tenantId1, "Customer 1");

        Map<EntityType, ExportableEntity<?>> entities = Map.of(
                EntityType.DEVICE, device, EntityType.DEVICE_PROFILE, deviceProfile,
                EntityType.RULE_CHAIN, ruleChain, EntityType.ASSET, asset,
                EntityType.DASHBOARD, dashboard, EntityType.CUSTOMER, customer
        );

        for (ExportableEntity<?> entity : entities.values()) {
            testEntityTypeExportRequest(entity);
            testCustomEntityFilterExportRequest(entity);
        }
    }

    private void testEntityTypeExportRequest(ExportableEntity<?> entity) throws Exception {
        EntityTypeVersionCreateConfig exportRequest = new EntityTypeVersionCreateConfig();
        exportRequest.setExportSettings(new EntityExportSettings());
        exportRequest.setPageSize(10);
        exportRequest.setEntityType(entity.getId().getEntityType());

        List<EntityExportData<?>> exportDataList = exportEntities(exportRequest);
        assertThat(exportDataList).size().isNotZero();
        assertThat(exportDataList).anySatisfy(exportData -> {
            assertThat(exportData.getEntity()).isEqualTo(entity);
        });
    }

    private void testCustomEntityFilterExportRequest(ExportableEntity<?> entity) throws Exception {
        EntitiesByCustomFilterVersionCreateConfig exportRequest = new EntitiesByCustomFilterVersionCreateConfig();
        exportRequest.setExportSettings(new EntityExportSettings());
        exportRequest.setPageSize(10);

        org.thingsboard.server.common.data.query.EntityListFilter filter = new org.thingsboard.server.common.data.query.EntityListFilter();
        filter.setEntityType(entity.getId().getEntityType());
        filter.setEntityList(List.of(entity.getId().toString()));
        exportRequest.setFilter(filter);

        List<EntityExportData<?>> exportDataList = exportEntities(exportRequest);
        assertThat(exportDataList).hasOnlyOneElementSatisfying(exportData -> {
            assertThat(exportData.getEntity()).isEqualTo(entity);
        });
    }


    @Test
    public void testExportImportCustomerEntities_betweenTenants() throws Exception {
        logInAsTenantAdmin1();
        Customer customer = createCustomer(tenantId1, "Customer 1");

        Device tenantDevice = createDevice(tenantId1, null, null, "Tenant device 1");
        Device customerDevice = createDevice(tenantId1, customer.getId(), null, "Customer device 1");
        Asset tenantAsset = createAsset(tenantId1, null, "A", "Tenant asset 1");
        Asset customerAsset = createAsset(tenantId1, customer.getId(), "A", "Customer asset 1");

        List<VersionCreateConfig> exportRequests = new ArrayList<>();

        for (EntityType entityType : Set.of(EntityType.DEVICE, EntityType.ASSET)) {
            EntityTypeVersionCreateConfig exportRequest = new EntityTypeVersionCreateConfig();
            exportRequest.setExportSettings(new EntityExportSettings());
            exportRequest.setPageSize(10);
            exportRequest.setEntityType(entityType);
            exportRequest.setCustomerId(customer.getUuidId());
            exportRequests.add(exportRequest);
        }

        List<EntityExportData<?>> exportDataList = exportEntities(exportRequests);
        assertThat(exportDataList).size().isEqualTo(2);
        assertThat(exportDataList).anySatisfy(exportData -> {
            assertThat(exportData.getEntity()).isEqualTo(customerDevice);
        });
        assertThat(exportDataList).anySatisfy(exportData -> {
            assertThat(exportData.getEntity()).isEqualTo(customerAsset);
        });
    }


    @Test
    public void testEntityEventsOnImport() throws Exception {
        logInAsTenantAdmin1();

        Customer customer = createCustomer(tenantId1, "Customer 1");
        Asset asset = createAsset(tenantId1, null, "A", "Asset 1");
        RuleChain ruleChain = createRuleChain(tenantId1, "Rule chain 1");
        Dashboard dashboard = createDashboard(tenantId1, null, "Dashboard 1");
        DeviceProfile deviceProfile = createDeviceProfile(tenantId1, ruleChain.getId(), dashboard.getId(), "Device profile 1");
        Device device = createDevice(tenantId1, null, deviceProfile.getId(), "Device 1");

        EntityListVersionCreateConfig exportRequest = new EntityListVersionCreateConfig();
        exportRequest.setEntitiesIds(List.of(customer.getId(), asset.getId(), device.getId(), ruleChain.getId(), dashboard.getId(), deviceProfile.getId()));
        exportRequest.setExportSettings(new EntityExportSettings());

        Map<EntityType, EntityExportData> entitiesExportData = exportEntities(exportRequest).stream()
                .collect(Collectors.toMap(EntityExportData::getEntityType, r -> r));

        logInAsTenantAdmin2();

        Customer importedCustomer = (Customer) importEntity(entitiesExportData.get(EntityType.CUSTOMER)).getSavedEntity();
        verify(entityActionService).logEntityAction(any(), eq(importedCustomer.getId()), eq(importedCustomer),
                any(), eq(ActionType.ADDED), isNull());
        importEntity(entitiesExportData.get(EntityType.CUSTOMER));
        verify(entityActionService).logEntityAction(any(), eq(importedCustomer.getId()), eq(importedCustomer),
                any(), eq(ActionType.UPDATED), isNull());
        verify(clusterService).sendNotificationMsgToEdgeService(any(), any(), eq(importedCustomer.getId()), any(), any(), eq(EdgeEventActionType.UPDATED));

        Asset importedAsset = (Asset) importEntity(entitiesExportData.get(EntityType.ASSET)).getSavedEntity();
        verify(entityActionService).logEntityAction(any(), eq(importedAsset.getId()), eq(importedAsset),
                any(), eq(ActionType.ADDED), isNull());
        importEntity(entitiesExportData.get(EntityType.ASSET));
        verify(entityActionService).logEntityAction(any(), eq(importedAsset.getId()), eq(importedAsset),
                any(), eq(ActionType.UPDATED), isNull());
        verify(clusterService).sendNotificationMsgToEdgeService(any(), any(), eq(importedAsset.getId()), any(), any(), eq(EdgeEventActionType.UPDATED));

        RuleChain importedRuleChain = (RuleChain) importEntity(entitiesExportData.get(EntityType.RULE_CHAIN)).getSavedEntity();
        verify(entityActionService).logEntityAction(any(), eq(importedRuleChain.getId()), eq(importedRuleChain),
                any(), eq(ActionType.ADDED), isNull());
        verify(clusterService).broadcastEntityStateChangeEvent(any(), eq(importedRuleChain.getId()), eq(ComponentLifecycleEvent.CREATED));

        Dashboard importedDashboard = (Dashboard) importEntity(entitiesExportData.get(EntityType.DASHBOARD)).getSavedEntity();
        verify(entityActionService).logEntityAction(any(), eq(importedDashboard.getId()), eq(importedDashboard),
                any(), eq(ActionType.ADDED), isNull());

        DeviceProfile importedDeviceProfile = (DeviceProfile) importEntity(entitiesExportData.get(EntityType.DEVICE_PROFILE)).getSavedEntity();
        verify(entityActionService).logEntityAction(any(), eq(importedDeviceProfile.getId()), eq(importedDeviceProfile),
                any(), eq(ActionType.ADDED), isNull());
        verify(clusterService).onDeviceProfileChange(eq(importedDeviceProfile), any());
        verify(clusterService).broadcastEntityStateChangeEvent(any(), eq(importedDeviceProfile.getId()), eq(ComponentLifecycleEvent.CREATED));
        verify(clusterService).sendNotificationMsgToEdgeService(any(), any(), eq(importedDeviceProfile.getId()), any(), any(), eq(EdgeEventActionType.ADDED));
        verify(otaPackageStateService).update(eq(importedDeviceProfile), eq(false), eq(false));

        ((DeviceExportData) entitiesExportData.get(EntityType.DEVICE)).getCredentials().setCredentialsId("abc");
        Device importedDevice = (Device) importEntity(entitiesExportData.get(EntityType.DEVICE)).getSavedEntity();
        verify(entityActionService).logEntityAction(any(), eq(importedDevice.getId()), eq(importedDevice),
                any(), eq(ActionType.ADDED), isNull());
        verify(clusterService).onDeviceUpdated(eq(importedDevice), isNull());
        importEntity(entitiesExportData.get(EntityType.DEVICE));
        verify(clusterService).onDeviceUpdated(eq(importedDevice), eq(importedDevice));
    }

}
