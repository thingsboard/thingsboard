/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.service.sync.ie;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Streams;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.debug.TbMsgGeneratorNode;
import org.thingsboard.rule.engine.debug.TbMsgGeneratorNodeConfiguration;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.sync.ie.DeviceExportData;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.common.data.sync.ie.EntityExportSettings;
import org.thingsboard.server.common.data.sync.ie.EntityImportResult;
import org.thingsboard.server.common.data.sync.ie.EntityImportSettings;
import org.thingsboard.server.common.data.sync.ie.RuleChainExportData;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.action.EntityActionService;
import org.thingsboard.server.service.ota.OtaPackageStateService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;

@DaoSqlTest
public class ExportImportServiceSqlTest extends BaseExportImportServiceTest {

    @Autowired
    private DeviceCredentialsService deviceCredentialsService;
    @SpyBean
    private EntityActionService entityActionService;
    @SpyBean
    private OtaPackageStateService otaPackageStateService;

    @Test
    public void testExportImportAssetWithProfile_betweenTenants() throws Exception {
        AssetProfile assetProfile = createAssetProfile(tenantId1, null, null, "Asset profile of tenant 1");
        Asset asset = createAsset(tenantId1, null, assetProfile.getId(), "Asset of tenant 1");

        EntityExportData<AssetProfile> profileExportData = exportEntity(tenantAdmin1, assetProfile.getId());

        EntityExportData<Asset> assetExportData = exportEntity(tenantAdmin1, asset.getId());

        EntityImportResult<AssetProfile> profileImportResult = importEntity(tenantAdmin2, profileExportData);
        checkImportedEntity(tenantId1, assetProfile, tenantId2, profileImportResult.getSavedEntity());
        checkImportedAssetProfileData(assetProfile, profileImportResult.getSavedEntity());

        EntityImportResult<Asset> assetImportResult = importEntity(tenantAdmin2, assetExportData);
        Asset importedAsset = assetImportResult.getSavedEntity();
        checkImportedEntity(tenantId1, asset, tenantId2, importedAsset);
        checkImportedAssetData(asset, importedAsset);

        assertThat(importedAsset.getAssetProfileId()).isEqualTo(profileImportResult.getSavedEntity().getId());
    }

    @Test
    public void testExportImportAsset_sameTenant() throws Exception {
        AssetProfile assetProfile = createAssetProfile(tenantId1, null, null, "Asset profile v1.0");
        Asset asset = createAsset(tenantId1, null, assetProfile.getId(), "Asset v1.0");
        EntityExportData<Asset> exportData = exportEntity(tenantAdmin1, asset.getId());

        EntityImportResult<Asset> importResult = importEntity(tenantAdmin1, exportData);
        checkImportedEntity(tenantId1, asset, tenantId1, importResult.getSavedEntity());
        checkImportedAssetData(asset, importResult.getSavedEntity());
    }

    @Test
    public void testExportImportAsset_sameTenant_withCustomer() throws Exception {
        AssetProfile assetProfile = createAssetProfile(tenantId1, null, null, "Asset profile v1.0");
        Customer customer = createCustomer(tenantId1, "My customer");
        Asset asset = createAsset(tenantId1, customer.getId(), assetProfile.getId(), "My asset");

        Asset importedAsset = importEntity(tenantAdmin1, this.<Asset, AssetId>exportEntity(tenantAdmin1, asset.getId())).getSavedEntity();
        assertThat(importedAsset.getCustomerId()).isEqualTo(asset.getCustomerId());
    }


    @Test
    public void testExportImportCustomer_betweenTenants() throws Exception {
        Customer customer = createCustomer(tenantAdmin1.getTenantId(), "Customer of tenant 1");
        EntityExportData<Customer> exportData = exportEntity(tenantAdmin1, customer.getId());

        EntityImportResult<Customer> importResult = importEntity(tenantAdmin2, exportData);
        checkImportedEntity(tenantId1, customer, tenantId2, importResult.getSavedEntity());
        checkImportedCustomerData(customer, importResult.getSavedEntity());
    }

    @Test
    public void testExportImportCustomer_sameTenant() throws Exception {
        Customer customer = createCustomer(tenantAdmin1.getTenantId(), "Customer v1.0");
        EntityExportData<Customer> exportData = exportEntity(tenantAdmin1, customer.getId());

        EntityImportResult<Customer> importResult = importEntity(tenantAdmin1, exportData);
        checkImportedEntity(tenantId1, customer, tenantId1, importResult.getSavedEntity());
        checkImportedCustomerData(customer, importResult.getSavedEntity());
    }


    @Test
    public void testExportImportDeviceWithProfile_betweenTenants() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile(tenantId1, null, null, "Device profile of tenant 1");
        Device device = createDevice(tenantId1, null, deviceProfile.getId(), "Device of tenant 1");
        DeviceCredentials credentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId1, device.getId());

        EntityExportData<DeviceProfile> profileExportData = exportEntity(tenantAdmin1, deviceProfile.getId());

        EntityExportData<Device> deviceExportData = exportEntity(tenantAdmin1, device.getId());
        DeviceCredentials exportedCredentials = ((DeviceExportData) deviceExportData).getCredentials();
        exportedCredentials.setCredentialsId(credentials.getCredentialsId() + "a");

        EntityImportResult<DeviceProfile> profileImportResult = importEntity(tenantAdmin2, profileExportData);
        checkImportedEntity(tenantId1, deviceProfile, tenantId2, profileImportResult.getSavedEntity());
        checkImportedDeviceProfileData(deviceProfile, profileImportResult.getSavedEntity());

        EntityImportResult<Device> deviceImportResult = importEntity(tenantAdmin2, deviceExportData);
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
        DeviceProfile deviceProfile = createDeviceProfile(tenantId1, null, null, "Device profile v1.0");
        OtaPackage firmware = createOtaPackage(tenantId1, deviceProfile.getId(), OtaPackageType.FIRMWARE);
        OtaPackage software = createOtaPackage(tenantId1, deviceProfile.getId(), OtaPackageType.SOFTWARE);
        Device device = createDevice(tenantId1, null, deviceProfile.getId(), "Device v1.0");
        device.setFirmwareId(firmware.getId());
        device.setSoftwareId(software.getId());
        device = deviceService.saveDevice(device);

        DeviceCredentials credentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId1, device.getId());

        EntityExportData<Device> deviceExportData = exportEntity(tenantAdmin1, device.getId());

        EntityImportResult<Device> importResult = importEntity(tenantAdmin1, deviceExportData);
        Device importedDevice = importResult.getSavedEntity();

        checkImportedEntity(tenantId1, device, tenantId1, importResult.getSavedEntity());
        assertThat(importedDevice.getDeviceProfileId()).isEqualTo(device.getDeviceProfileId());
        assertThat(deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId1, device.getId())).isEqualTo(credentials);
        assertThat(importedDevice.getFirmwareId()).isEqualTo(firmware.getId());
        assertThat(importedDevice.getSoftwareId()).isEqualTo(software.getId());
    }


    @Test
    public void testExportImportDashboard_betweenTenants() throws Exception {
        Dashboard dashboard = createDashboard(tenantAdmin1.getTenantId(), null, "Dashboard of tenant 1");
        EntityExportData<Dashboard> exportData = exportEntity(tenantAdmin1, dashboard.getId());

        EntityImportResult<Dashboard> importResult = importEntity(tenantAdmin2, exportData);
        checkImportedEntity(tenantId1, dashboard, tenantId2, importResult.getSavedEntity());
        checkImportedDashboardData(dashboard, importResult.getSavedEntity());
    }

    @Test
    public void testExportImportDashboard_sameTenant() throws Exception {
        Dashboard dashboard = createDashboard(tenantAdmin1.getTenantId(), null, "Dashboard v1.0");
        EntityExportData<Dashboard> exportData = exportEntity(tenantAdmin1, dashboard.getId());

        EntityImportResult<Dashboard> importResult = importEntity(tenantAdmin1, exportData);
        checkImportedEntity(tenantId1, dashboard, tenantId1, importResult.getSavedEntity());
        checkImportedDashboardData(dashboard, importResult.getSavedEntity());
    }

    @Test
    public void testExportImportDashboard_betweenTenants_withCustomer_updated() throws Exception {
        Dashboard dashboard = createDashboard(tenantAdmin1.getTenantId(), null, "Dashboard of tenant 1");
        EntityExportData<Dashboard> exportData = exportEntity(tenantAdmin1, dashboard.getId());

        Dashboard importedDashboard = importEntity(tenantAdmin2, exportData).getSavedEntity();
        checkImportedEntity(tenantId1, dashboard, tenantId2, importedDashboard);

        Customer customer = createCustomer(tenantId1, "Customer 1");
        EntityExportData<Customer> customerExportData = exportEntity(tenantAdmin1, customer.getId());
        dashboardService.assignDashboardToCustomer(tenantId1, dashboard.getId(), customer.getId());
        exportData = exportEntity(tenantAdmin1, dashboard.getId());

        Customer importedCustomer = importEntity(tenantAdmin2, customerExportData).getSavedEntity();
        importedDashboard = importEntity(tenantAdmin2, exportData).getSavedEntity();
        assertThat(importedDashboard.getAssignedCustomers()).hasOnlyOneElementSatisfying(customerInfo -> {
            assertThat(customerInfo.getCustomerId()).isEqualTo(importedCustomer.getId());
        });
    }

    @Test
    public void testExportImportDashboard_betweenTenants_withEntityAliases() throws Exception {
        AssetProfile assetProfile = createAssetProfile(tenantId1, null, null, "A");
        Asset asset1 = createAsset(tenantId1, null, assetProfile.getId(), "Asset 1");
        Asset asset2 = createAsset(tenantId1, null, assetProfile.getId(), "Asset 2");
        Dashboard dashboard = createDashboard(tenantId1, null, "Dashboard 1");
        DeviceProfile existingDeviceProfile = createDeviceProfile(tenantId2, null, null, "Existing");

        String aliasId = "23c4185d-1497-9457-30b2-6d91e69a5b2c";
        String unknownUuid = "ea0dc8b0-3d85-11ed-9200-77fc04fa14fa";
        String entityAliases = "{\n" +
                "\"" + aliasId + "\": {\n" +
                "\"alias\": \"assets\",\n" +
                "\"filter\": {\n" +
                "\"entityList\": [\n" +
                "\"" + asset1.getId().toString() + "\",\n" +
                "\"" + asset2.getId().toString() + "\",\n" +
                "\"" + tenantId1.getId().toString() + "\",\n" +
                "\"" + existingDeviceProfile.getId().toString() + "\",\n" +
                "\"" + unknownUuid + "\"\n" +
                "],\n" +
                "\"resolveMultiple\": true\n" +
                "},\n" +
                "\"id\": \"" + aliasId + "\"\n" +
                "}\n" +
                "}";
        ObjectNode dashboardConfiguration = JacksonUtil.newObjectNode();
        dashboardConfiguration.set("entityAliases", JacksonUtil.toJsonNode(entityAliases));
        dashboardConfiguration.set("description", new TextNode("hallo"));
        dashboard.setConfiguration(dashboardConfiguration);
        dashboard = dashboardService.saveDashboard(dashboard);

        EntityExportData<AssetProfile> profileExportData = exportEntity(tenantAdmin1, assetProfile.getId());

        EntityExportData<Asset> asset1ExportData = exportEntity(tenantAdmin1, asset1.getId());
        EntityExportData<Asset> asset2ExportData = exportEntity(tenantAdmin1, asset2.getId());
        EntityExportData<Dashboard> dashboardExportData = exportEntity(tenantAdmin1, dashboard.getId());

        AssetProfile importedProfile = importEntity(tenantAdmin2, profileExportData).getSavedEntity();
        Asset importedAsset1 = importEntity(tenantAdmin2, asset1ExportData).getSavedEntity();
        Asset importedAsset2 = importEntity(tenantAdmin2, asset2ExportData).getSavedEntity();
        Dashboard importedDashboard = importEntity(tenantAdmin2, dashboardExportData).getSavedEntity();

        Map.Entry<String, JsonNode> entityAlias = importedDashboard.getConfiguration().get("entityAliases").fields().next();
        assertThat(entityAlias.getKey()).isEqualTo(aliasId);
        assertThat(entityAlias.getValue().get("id").asText()).isEqualTo(aliasId);

        List<String> aliasEntitiesIds = Streams.stream(entityAlias.getValue().get("filter").get("entityList").elements())
                .map(JsonNode::asText).collect(Collectors.toList());
        assertThat(aliasEntitiesIds).size().isEqualTo(5);
        assertThat(aliasEntitiesIds).element(0).as("external asset 1 was replaced with imported one")
                .isEqualTo(importedAsset1.getId().toString());
        assertThat(aliasEntitiesIds).element(1).as("external asset 2 was replaced with imported one")
                .isEqualTo(importedAsset2.getId().toString());
        assertThat(aliasEntitiesIds).element(2).as("external tenant id was replaced with new tenant id")
                .isEqualTo(tenantId2.toString());
        assertThat(aliasEntitiesIds).element(3).as("existing device profile id was left as is")
                .isEqualTo(existingDeviceProfile.getId().toString());
        assertThat(aliasEntitiesIds).element(4).as("unresolved uuid was replaced with tenant id")
                .isEqualTo(tenantId2.toString());
    }


    @Test
    public void testExportImportRuleChain_betweenTenants() throws Exception {
        RuleChain ruleChain = createRuleChain(tenantId1, "Rule chain of tenant 1");
        RuleChainMetaData metaData = ruleChainService.loadRuleChainMetaData(tenantId1, ruleChain.getId());
        EntityExportData<RuleChain> exportData = exportEntity(tenantAdmin1, ruleChain.getId());

        EntityImportResult<RuleChain> importResult = importEntity(tenantAdmin2, exportData);
        RuleChain importedRuleChain = importResult.getSavedEntity();
        RuleChainMetaData importedMetaData = ruleChainService.loadRuleChainMetaData(tenantId2, importedRuleChain.getId());

        checkImportedEntity(tenantId1, ruleChain, tenantId2, importResult.getSavedEntity());
        checkImportedRuleChainData(ruleChain, metaData, importedRuleChain, importedMetaData);
    }

    @Test
    public void testExportImportRuleChain_sameTenant() throws Exception {
        RuleChain ruleChain = createRuleChain(tenantId1, "Rule chain v1.0");
        RuleChainMetaData metaData = ruleChainService.loadRuleChainMetaData(tenantId1, ruleChain.getId());
        EntityExportData<RuleChain> exportData = exportEntity(tenantAdmin1, ruleChain.getId());

        EntityImportResult<RuleChain> importResult = importEntity(tenantAdmin1, exportData);
        RuleChain importedRuleChain = importResult.getSavedEntity();
        RuleChainMetaData importedMetaData = ruleChainService.loadRuleChainMetaData(tenantId1, importedRuleChain.getId());

        checkImportedEntity(tenantId1, ruleChain, tenantId1, importResult.getSavedEntity());
        checkImportedRuleChainData(ruleChain, metaData, importedRuleChain, importedMetaData);
    }


    @Test
    public void testExportImportWithInboundRelations_betweenTenants() throws Exception {
        Asset asset = createAsset(tenantId1, null, null, "Asset 1");
        Device device = createDevice(tenantId1, null, null, "Device 1");
        EntityRelation relation = createRelation(asset.getId(), device.getId());

        EntityExportData<Asset> assetExportData = exportEntity(tenantAdmin1, asset.getId());
        EntityExportData<Device> deviceExportData = exportEntity(tenantAdmin1, device.getId(), EntityExportSettings.builder()
                .exportRelations(true)
                .exportCredentials(false)
                .build());

        assertThat(deviceExportData.getRelations()).size().isOne();
        assertThat(deviceExportData.getRelations().get(0)).matches(entityRelation -> {
            return entityRelation.getFrom().equals(asset.getId()) && entityRelation.getTo().equals(device.getId());
        });
        ((Asset) assetExportData.getEntity()).setAssetProfileId(null);
        ((Device) deviceExportData.getEntity()).setDeviceProfileId(null);

        Asset importedAsset = importEntity(tenantAdmin2, assetExportData).getSavedEntity();
        Device importedDevice = importEntity(tenantAdmin2, deviceExportData, EntityImportSettings.builder()
                .updateRelations(true)
                .build()).getSavedEntity();
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
        Asset asset = createAsset(tenantId1, null, null, "Asset 1");
        Device device = createDevice(tenantId1, null, null, "Device 1");
        EntityRelation relation = createRelation(asset.getId(), device.getId());

        EntityExportData<Asset> assetExportData = exportEntity(tenantAdmin1, asset.getId());
        EntityExportData<Device> deviceExportData = exportEntity(tenantAdmin1, device.getId(), EntityExportSettings.builder()
                .exportRelations(true)
                .exportCredentials(false)
                .build());
        assetExportData.getEntity().setAssetProfileId(null);
        deviceExportData.getEntity().setDeviceProfileId(null);

        Asset importedAsset = importEntity(tenantAdmin2, assetExportData).getSavedEntity();
        Device importedDevice = importEntity(tenantAdmin2, deviceExportData, EntityImportSettings.builder()
                .updateRelations(true)
                .build()).getSavedEntity();

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
        Asset asset = createAsset(tenantId1, null, null, "Asset 1");
        Device device1 = createDevice(tenantId1, null, null, "Device 1");
        EntityRelation relation1 = createRelation(asset.getId(), device1.getId());

        EntityExportData<Asset> assetExportData = exportEntity(tenantAdmin1, asset.getId(), EntityExportSettings.builder()
                .exportRelations(true)
                .build());
        assertThat(assetExportData.getRelations()).size().isOne();

        Device device2 = createDevice(tenantId1, null, null, "Device 2");
        EntityRelation relation2 = createRelation(asset.getId(), device2.getId());

        importEntity(tenantAdmin1, assetExportData, EntityImportSettings.builder()
                .updateRelations(true)
                .build());

        List<EntityRelation> relations = relationService.findByFrom(TenantId.SYS_TENANT_ID, asset.getId(), RelationTypeGroup.COMMON);
        assertThat(relations).contains(relation1);
        assertThat(relations).doesNotContain(relation2);
    }

    @Test
    public void textExportImportWithRelations_sameTenant_removeExisting() throws Exception {
        Asset asset1 = createAsset(tenantId1, null, null, "Asset 1");
        Device device = createDevice(tenantId1, null, null, "Device 1");
        EntityRelation relation1 = createRelation(asset1.getId(), device.getId());

        EntityExportData<Device> deviceExportData = exportEntity(tenantAdmin1, device.getId(), EntityExportSettings.builder()
                .exportRelations(true)
                .build());
        assertThat(deviceExportData.getRelations()).size().isOne();

        Asset asset2 = createAsset(tenantId1, null, null, "Asset 2");
        EntityRelation relation2 = createRelation(asset2.getId(), device.getId());

        importEntity(tenantAdmin1, deviceExportData, EntityImportSettings.builder()
                .updateRelations(true)
                .build());

        List<EntityRelation> relations = relationService.findByTo(TenantId.SYS_TENANT_ID, device.getId(), RelationTypeGroup.COMMON);
        assertThat(relations).contains(relation1);
        assertThat(relations).doesNotContain(relation2);
    }


    @Test
    public void testExportImportDeviceProfile_betweenTenants_findExistingByName() throws Exception {
        DeviceProfile defaultDeviceProfile = deviceProfileService.findDefaultDeviceProfile(tenantId1);
        EntityExportData<DeviceProfile> deviceProfileExportData = exportEntity(tenantAdmin1, defaultDeviceProfile.getId());

        assertThatThrownBy(() -> {
            importEntity(tenantAdmin2, deviceProfileExportData, EntityImportSettings.builder()
                    .findExistingByName(false)
                    .build());
        }).hasMessageContaining("default device profile is present");

        importEntity(tenantAdmin2, deviceProfileExportData, EntityImportSettings.builder()
                .findExistingByName(true)
                .build());
        checkImportedEntity(tenantId1, defaultDeviceProfile, tenantId2, deviceProfileService.findDefaultDeviceProfile(tenantId2));
    }


    @SuppressWarnings("rawTypes")
    private static EntityExportData getAndClone(Map<EntityType, EntityExportData> map, EntityType entityType) {
        return JacksonUtil.clone(map.get(entityType));
    }

    @SuppressWarnings({"rawTypes", "unchecked"})
    @Test
    public void testEntityEventsOnImport() throws Exception {
        Customer customer = createCustomer(tenantId1, "Customer 1");
        RuleChain ruleChain = createRuleChain(tenantId1, "Rule chain 1");
        Dashboard dashboard = createDashboard(tenantId1, null, "Dashboard 1");
        AssetProfile assetProfile = createAssetProfile(tenantId1, ruleChain.getId(), dashboard.getId(), "Asset profile 1");
        Asset asset = createAsset(tenantId1, null, assetProfile.getId(), "Asset 1");
        DeviceProfile deviceProfile = createDeviceProfile(tenantId1, ruleChain.getId(), dashboard.getId(), "Device profile 1");
        Device device = createDevice(tenantId1, null, deviceProfile.getId(), "Device 1");

        Map<EntityType, EntityExportData> entitiesExportData = Stream.of(customer.getId(), asset.getId(), device.getId(),
                        ruleChain.getId(), dashboard.getId(), assetProfile.getId(), deviceProfile.getId())
                .map(entityId -> {
                    try {
                        return exportEntity(tenantAdmin1, entityId, EntityExportSettings.builder()
                                .exportCredentials(false)
                                .build());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toMap(EntityExportData::getEntityType, d -> d));

        Mockito.reset(entityActionService);
        Customer importedCustomer = (Customer) importEntity(tenantAdmin2, getAndClone(entitiesExportData, EntityType.CUSTOMER)).getSavedEntity();
        verify(entityActionService).logEntityAction(any(), eq(importedCustomer.getId()), eq(importedCustomer),
                any(), eq(ActionType.ADDED), isNull());
        Mockito.reset(entityActionService);
        importEntity(tenantAdmin2, getAndClone(entitiesExportData, EntityType.CUSTOMER));
        verify(entityActionService, Mockito.never()).logEntityAction(any(), eq(importedCustomer.getId()), eq(importedCustomer),
                any(), eq(ActionType.UPDATED), isNull());

        EntityExportData<Customer> updatedCustomerEntity = getAndClone(entitiesExportData, EntityType.CUSTOMER);
        updatedCustomerEntity.getEntity().setEmail("t" + updatedCustomerEntity.getEntity().getEmail());
        Customer updatedCustomer = importEntity(tenantAdmin2, updatedCustomerEntity).getSavedEntity();
        verify(entityActionService).logEntityAction(any(), eq(importedCustomer.getId()), eq(updatedCustomer),
                any(), eq(ActionType.UPDATED), isNull());
        verify(tbClusterService).sendNotificationMsgToEdge(any(), any(), eq(importedCustomer.getId()), any(), any(), eq(EdgeEventActionType.UPDATED));

        Mockito.reset(entityActionService);

        RuleChain importedRuleChain = (RuleChain) importEntity(tenantAdmin2, getAndClone(entitiesExportData, EntityType.RULE_CHAIN)).getSavedEntity();
        verify(entityActionService).logEntityAction(any(), eq(importedRuleChain.getId()), eq(importedRuleChain),
                any(), eq(ActionType.ADDED), isNull());
        verify(tbClusterService).broadcastEntityStateChangeEvent(any(), eq(importedRuleChain.getId()), eq(ComponentLifecycleEvent.CREATED));

        Dashboard importedDashboard = (Dashboard) importEntity(tenantAdmin2, getAndClone(entitiesExportData, EntityType.DASHBOARD)).getSavedEntity();
        verify(entityActionService).logEntityAction(any(), eq(importedDashboard.getId()), eq(importedDashboard),
                any(), eq(ActionType.ADDED), isNull());

        AssetProfile importedAssetProfile = (AssetProfile) importEntity(tenantAdmin2, getAndClone(entitiesExportData, EntityType.ASSET_PROFILE)).getSavedEntity();
        verify(entityActionService).logEntityAction(any(), eq(importedAssetProfile.getId()), eq(importedAssetProfile),
                any(), eq(ActionType.ADDED), isNull());
        verify(tbClusterService).broadcastEntityStateChangeEvent(any(), eq(importedAssetProfile.getId()), eq(ComponentLifecycleEvent.CREATED));
        verify(tbClusterService).sendNotificationMsgToEdge(any(), any(), eq(importedAssetProfile.getId()), any(), any(), eq(EdgeEventActionType.ADDED));

        Asset importedAsset = (Asset) importEntity(tenantAdmin2, getAndClone(entitiesExportData, EntityType.ASSET)).getSavedEntity();
        verify(entityActionService).logEntityAction(any(), eq(importedAsset.getId()), eq(importedAsset),
                any(), eq(ActionType.ADDED), isNull());
        importEntity(tenantAdmin2, entitiesExportData.get(EntityType.ASSET));
        verify(entityActionService, Mockito.never()).logEntityAction(any(), eq(importedAsset.getId()), eq(importedAsset),
                any(), eq(ActionType.UPDATED), isNull());


        EntityExportData<Asset> updatedAssetEntity = getAndClone(entitiesExportData, EntityType.ASSET);
        updatedAssetEntity.getEntity().setLabel("t" + updatedAssetEntity.getEntity().getLabel());
        Asset updatedAsset = importEntity(tenantAdmin2, updatedAssetEntity).getSavedEntity();

        verify(entityActionService).logEntityAction(any(), eq(importedAsset.getId()), eq(updatedAsset),
                any(), eq(ActionType.UPDATED), isNull());
        verify(tbClusterService).sendNotificationMsgToEdge(any(), any(), eq(importedAsset.getId()), any(), any(), eq(EdgeEventActionType.UPDATED));

        DeviceProfile importedDeviceProfile = (DeviceProfile) importEntity(tenantAdmin2, getAndClone(entitiesExportData, EntityType.DEVICE_PROFILE)).getSavedEntity();
        verify(entityActionService).logEntityAction(any(), eq(importedDeviceProfile.getId()), eq(importedDeviceProfile),
                any(), eq(ActionType.ADDED), isNull());
        verify(tbClusterService).onDeviceProfileChange(eq(importedDeviceProfile), any());
        verify(tbClusterService).broadcastEntityStateChangeEvent(any(), eq(importedDeviceProfile.getId()), eq(ComponentLifecycleEvent.CREATED));
        verify(tbClusterService).sendNotificationMsgToEdge(any(), any(), eq(importedDeviceProfile.getId()), any(), any(), eq(EdgeEventActionType.ADDED));
        verify(otaPackageStateService).update(eq(importedDeviceProfile), eq(false), eq(false));

        Device importedDevice = (Device) importEntity(tenantAdmin2, getAndClone(entitiesExportData, EntityType.DEVICE)).getSavedEntity();
        verify(entityActionService).logEntityAction(any(), eq(importedDevice.getId()), eq(importedDevice),
                any(), eq(ActionType.ADDED), isNull());
        verify(tbClusterService).onDeviceUpdated(eq(importedDevice), isNull());
        importEntity(tenantAdmin2, getAndClone(entitiesExportData, EntityType.DEVICE));
        verify(tbClusterService, Mockito.never()).onDeviceUpdated(eq(importedDevice), eq(importedDevice));

        EntityExportData<Device> updatedDeviceEntity = getAndClone(entitiesExportData, EntityType.DEVICE);
        updatedDeviceEntity.getEntity().setLabel("t" + updatedDeviceEntity.getEntity().getLabel());
        Device updatedDevice = importEntity(tenantAdmin2, updatedDeviceEntity).getSavedEntity();
        verify(tbClusterService).onDeviceUpdated(eq(updatedDevice), eq(importedDevice));
    }

    @Test
    public void testExternalIdsInExportData() throws Exception {
        Customer customer = createCustomer(tenantId1, "Customer 1");
        AssetProfile assetProfile = createAssetProfile(tenantId1, null, null, "Asset profile 1");
        Asset asset = createAsset(tenantId1, customer.getId(), assetProfile.getId(), "Asset 1");
        RuleChain ruleChain = createRuleChain(tenantId1, "Rule chain 1", asset.getId());
        Dashboard dashboard = createDashboard(tenantId1, customer.getId(), "Dashboard 1", asset.getId());

        assetProfile.setDefaultRuleChainId(ruleChain.getId());
        assetProfile.setDefaultDashboardId(dashboard.getId());
        assetProfile = assetProfileService.saveAssetProfile(assetProfile);

        DeviceProfile deviceProfile = createDeviceProfile(tenantId1, ruleChain.getId(), dashboard.getId(), "Device profile 1");
        Device device = createDevice(tenantId1, customer.getId(), deviceProfile.getId(), "Device 1");
        EntityView entityView = createEntityView(tenantId1, customer.getId(), device.getId(), "Entity view 1");

        Map<EntityId, EntityId> ids = new HashMap<>();
        for (EntityId entityId : List.of(customer.getId(), ruleChain.getId(), dashboard.getId(), assetProfile.getId(), asset.getId(),
                deviceProfile.getId(), device.getId(), entityView.getId(), ruleChain.getId(), dashboard.getId())) {
            EntityExportData exportData = exportEntity(getSecurityUser(tenantAdmin1), entityId);
            EntityImportResult importResult = importEntity(getSecurityUser(tenantAdmin2), exportData, EntityImportSettings.builder()
                    .saveCredentials(false)
                    .build());
            ids.put(entityId, (EntityId) importResult.getSavedEntity().getId());
        }

        AssetProfile exportedAssetProfile = (AssetProfile) exportEntity(tenantAdmin2, (AssetProfileId) ids.get(assetProfile.getId())).getEntity();
        assertThat(exportedAssetProfile.getDefaultRuleChainId()).isEqualTo(ruleChain.getId());
        assertThat(exportedAssetProfile.getDefaultDashboardId()).isEqualTo(dashboard.getId());

        Asset exportedAsset = (Asset) exportEntity(tenantAdmin2, (AssetId) ids.get(asset.getId())).getEntity();
        assertThat(exportedAsset.getCustomerId()).isEqualTo(customer.getId());

        EntityExportData<RuleChain> ruleChainExportData = exportEntity(tenantAdmin2, (RuleChainId) ids.get(ruleChain.getId()));
        TbMsgGeneratorNodeConfiguration exportedRuleNodeConfig = ((RuleChainExportData) ruleChainExportData).getMetaData().getNodes().stream()
                .filter(node -> node.getType().equals(TbMsgGeneratorNode.class.getName())).findFirst()
                .map(RuleNode::getConfiguration).map(config -> JacksonUtil.treeToValue(config, TbMsgGeneratorNodeConfiguration.class)).orElse(null);
        assertThat(exportedRuleNodeConfig.getOriginatorId()).isEqualTo(asset.getId().toString());

        Dashboard exportedDashboard = (Dashboard) exportEntity(tenantAdmin2, (DashboardId) ids.get(dashboard.getId())).getEntity();
        assertThat(exportedDashboard.getAssignedCustomers()).hasOnlyOneElementSatisfying(shortCustomerInfo -> {
            assertThat(shortCustomerInfo.getCustomerId()).isEqualTo(customer.getId());
        });
        String exportedEntityAliasAssetId = exportedDashboard.getConfiguration().get("entityAliases").elements().next()
                .get("filter").get("entityList").elements().next().asText();
        assertThat(exportedEntityAliasAssetId).isEqualTo(asset.getId().toString());

        DeviceProfile exportedDeviceProfile = (DeviceProfile) exportEntity(tenantAdmin2, (DeviceProfileId) ids.get(deviceProfile.getId())).getEntity();
        assertThat(exportedDeviceProfile.getDefaultRuleChainId()).isEqualTo(ruleChain.getId());
        assertThat(exportedDeviceProfile.getDefaultDashboardId()).isEqualTo(dashboard.getId());

        Device exportedDevice = (Device) exportEntity(tenantAdmin2, (DeviceId) ids.get(device.getId())).getEntity();
        assertThat(exportedDevice.getCustomerId()).isEqualTo(customer.getId());
        assertThat(exportedDevice.getDeviceProfileId()).isEqualTo(deviceProfile.getId());

        EntityView exportedEntityView = (EntityView) exportEntity(tenantAdmin2, (EntityViewId) ids.get(entityView.getId())).getEntity();
        assertThat(exportedEntityView.getCustomerId()).isEqualTo(customer.getId());
        assertThat(exportedEntityView.getEntityId()).isEqualTo(device.getId());

        deviceProfile.setDefaultDashboardId(null);
        deviceProfileService.saveDeviceProfile(deviceProfile);
        DeviceProfile importedDeviceProfile = deviceProfileService.findDeviceProfileById(tenantId2, (DeviceProfileId) ids.get(deviceProfile.getId()));
        importedDeviceProfile.setDefaultDashboardId(null);
        deviceProfileService.saveDeviceProfile(importedDeviceProfile);
    }

}
