/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.service.sync.vc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Streams;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.debug.TbMsgGeneratorNode;
import org.thingsboard.rule.engine.debug.TbMsgGeneratorNodeConfiguration;
import org.thingsboard.rule.engine.metadata.TbGetAttributesNode;
import org.thingsboard.rule.engine.metadata.TbGetAttributesNodeConfiguration;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.SimpleCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.TimeSeriesOutput;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.device.data.DefaultDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.sync.vc.EntityTypeLoadResult;
import org.thingsboard.server.common.data.sync.vc.EntityVersion;
import org.thingsboard.server.common.data.sync.vc.RepositoryAuthMethod;
import org.thingsboard.server.common.data.sync.vc.RepositorySettings;
import org.thingsboard.server.common.data.sync.vc.VersionCreationResult;
import org.thingsboard.server.common.data.sync.vc.VersionLoadResult;
import org.thingsboard.server.common.data.sync.vc.request.create.ComplexVersionCreateRequest;
import org.thingsboard.server.common.data.sync.vc.request.create.EntityTypeVersionCreateConfig;
import org.thingsboard.server.common.data.sync.vc.request.create.SyncStrategy;
import org.thingsboard.server.common.data.sync.vc.request.create.VersionCreateRequest;
import org.thingsboard.server.common.data.sync.vc.request.load.EntityTypeVersionLoadConfig;
import org.thingsboard.server.common.data.sync.vc.request.load.EntityTypeVersionLoadRequest;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.controller.TbResourceControllerTest.JS_TEST_FILE_NAME;
import static org.thingsboard.server.controller.TbResourceControllerTest.TEST_DATA;

@DaoSqlTest
public class VersionControlTest extends AbstractControllerTest {

    @Autowired
    private EntitiesVersionControlService versionControlService;
    @Autowired
    private OtaPackageService otaPackageService;

    private TenantId tenantId1;
    protected User tenantAdmin1;

    private TenantId tenantId2;
    protected User tenantAdmin2;

    private String repoKey;
    private String branch;

    @Before
    public void beforeEach() throws Exception {
        loginSysAdmin();
        Tenant tenant1 = new Tenant();
        tenant1.setTitle("Tenant 1");
        tenant1.setEmail("tenant1@thingsboard.org");
        tenant1 = saveTenant(tenant1);
        this.tenantId1 = tenant1.getId();
        User tenantAdmin1 = new User();
        tenantAdmin1.setTenantId(tenantId1);
        tenantAdmin1.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin1.setEmail("tenant1-admin@thingsboard.org");
        this.tenantAdmin1 = createUser(tenantAdmin1, tenantAdmin1.getEmail());

        Tenant tenant2 = new Tenant();
        tenant2.setTitle("Tenant 2");
        tenant2.setEmail("tenant2@thingsboard.org");
        tenant2 = saveTenant(tenant2);
        this.tenantId2 = tenant2.getId();
        User tenantAdmin2 = new User();
        tenantAdmin2.setTenantId(tenantId2);
        tenantAdmin2.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin2.setEmail("tenant2-admin@thingsboard.org");
        this.tenantAdmin2 = createUser(tenantAdmin2, tenantAdmin2.getEmail());

        this.repoKey = UUID.randomUUID().toString();
        this.branch = "test_" + repoKey;
        configureRepository(tenantId1);
        configureRepository(tenantId2);

        loginTenant1();
    }

    @Test
    public void testAssetVc_withProfile_betweenTenants() throws Exception {
        AssetProfile assetProfile = createAssetProfile(null, null, "Asset profile of tenant 1");
        Asset asset = createAsset(null, assetProfile.getId(), "Asset of tenant 1");
        String versionId = createVersion("assets and profiles", EntityType.ASSET, EntityType.ASSET_PROFILE);
        assertThat(listVersions()).extracting(EntityVersion::getName).containsExactly("assets and profiles");

        loginTenant2();
        Map<EntityType, EntityTypeLoadResult> result = loadVersion(versionId, EntityType.ASSET, EntityType.ASSET_PROFILE);
        assertThat(result.get(EntityType.ASSET).getCreated()).isEqualTo(1);
        assertThat(result.get(EntityType.ASSET_PROFILE).getCreated()).isEqualTo(1);

        Asset importedAsset = findAsset(asset.getName());
        checkImportedEntity(tenantId1, asset, tenantId2, importedAsset);
        checkImportedAssetData(asset, importedAsset);

        AssetProfile importedAssetProfile = findAssetProfile(assetProfile.getName());
        checkImportedEntity(tenantId1, assetProfile, tenantId2, importedAssetProfile);
        checkImportedAssetProfileData(assetProfile, importedAssetProfile);

        assertThat(importedAsset.getAssetProfileId()).isEqualTo(importedAssetProfile.getId());
    }

    @Test
    public void testAssetVc_sameTenant() throws Exception {
        AssetProfile assetProfile = createAssetProfile(null, null, "Asset profile v1.0");
        Asset asset = createAsset(null, assetProfile.getId(), "Asset v1.0");
        String versionId = createVersion("assets", EntityType.ASSET);

        loadVersion(versionId, EntityType.ASSET);
        Asset importedAsset = findAsset(asset.getName());
        checkImportedEntity(tenantId1, asset, tenantId1, importedAsset);
        checkImportedAssetData(asset, importedAsset);
    }

    @Test
    public void testAssetVc_sameTenant_withCustomer() throws Exception {
        AssetProfile assetProfile = createAssetProfile(null, null, "Asset profile v1.0");
        Customer customer = createCustomer("My customer");
        Asset asset = createAsset(customer.getId(), assetProfile.getId(), "My asset");
        String versionId = createVersion("assets", EntityType.ASSET);

        loadVersion(versionId, EntityType.ASSET);
        Asset importedAsset = findAsset(asset.getName());
        assertThat(importedAsset.getCustomerId()).isEqualTo(asset.getCustomerId());
    }

    @Test
    public void testCustomerVc_sameTenant() throws Exception {
        Customer customer = createCustomer("Customer v1.0");
        String versionId = createVersion("customers", EntityType.CUSTOMER);

        loadVersion(versionId, EntityType.CUSTOMER);
        Customer importedCustomer = findCustomer(customer.getName());
        checkImportedEntity(tenantId1, customer, tenantId1, importedCustomer);
        checkImportedCustomerData(customer, importedCustomer);
    }

    @Test
    public void testCustomerVc_betweenTenants() throws Exception {
        Customer customer = createCustomer("Customer of tenant 1");
        String versionId = createVersion("customers", EntityType.CUSTOMER);

        loginTenant2();
        loadVersion(versionId, EntityType.CUSTOMER);
        Customer importedCustomer = findCustomer(customer.getName());
        checkImportedEntity(tenantId1, customer, tenantId2, importedCustomer);
        checkImportedCustomerData(customer, importedCustomer);
    }

    @Test
    public void testDeviceVc_sameTenant() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile(null, null, "Device profile v1.0");
        OtaPackage firmware = createOtaPackage(tenantId1, deviceProfile.getId(), OtaPackageType.FIRMWARE);
        OtaPackage software = createOtaPackage(tenantId1, deviceProfile.getId(), OtaPackageType.SOFTWARE);
        Device device = createDevice(deviceProfile.getId(), "Device v1.0", "test1", newDevice -> {
            newDevice.setFirmwareId(firmware.getId());
            newDevice.setSoftwareId(software.getId());
        });
        DeviceCredentials deviceCredentials = findDeviceCredentials(device.getId());
        String versionId = createVersion("devices", EntityType.DEVICE);

        loadVersion(versionId, EntityType.DEVICE);
        Device importedDevice = findDevice(device.getName());

        checkImportedEntity(tenantId1, device, tenantId1, importedDevice);
        assertThat(importedDevice.getDeviceProfileId()).isEqualTo(device.getDeviceProfileId());
        assertThat(findDeviceCredentials(device.getId())).isEqualToIgnoringGivenFields(deviceCredentials, "version");
        assertThat(importedDevice.getFirmwareId()).isEqualTo(firmware.getId());
        assertThat(importedDevice.getSoftwareId()).isEqualTo(software.getId());
    }

    @Test
    public void testDeviceVc_withProfileAndOtaPackage_betweenTenants() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile(null, null, "Device profile of tenant 1");
        createVersion("profiles", EntityType.DEVICE_PROFILE);
        OtaPackage firmware = createOtaPackage(tenantId1, deviceProfile.getId(), OtaPackageType.FIRMWARE);
        OtaPackage software = createOtaPackage(tenantId1, deviceProfile.getId(), OtaPackageType.SOFTWARE);
        Device device = createDevice(deviceProfile.getId(), "Device of tenant 1", "test1", newDevice -> {
            newDevice.setFirmwareId(firmware.getId());
            newDevice.setSoftwareId(software.getId());
        });
        String versionId = createVersion("devices with ota", EntityType.DEVICE, EntityType.OTA_PACKAGE);
        DeviceCredentials deviceCredentials = findDeviceCredentials(device.getId());
        DeviceCredentials newCredentials = new DeviceCredentials(deviceCredentials);
        newCredentials.setCredentialsId("new access token"); // updating access token to avoid constraint errors on import
        doPost("/api/device/credentials", newCredentials, DeviceCredentials.class);
        assertThat(listVersions()).extracting(EntityVersion::getName).containsExactly("devices with ota", "profiles");

        loginTenant2();
        Map<EntityType, EntityTypeLoadResult> result = loadVersion(versionId, EntityType.DEVICE, EntityType.DEVICE_PROFILE, EntityType.OTA_PACKAGE);
        assertThat(result.get(EntityType.DEVICE).getCreated()).isEqualTo(1);
        assertThat(result.get(EntityType.DEVICE_PROFILE).getCreated()).isEqualTo(1);

        Device importedDevice = findDevice(device.getName());
        checkImportedEntity(tenantId1, device, tenantId2, importedDevice);
        checkImportedDeviceData(device, importedDevice);

        DeviceProfile importedDeviceProfile = findDeviceProfile(deviceProfile.getName());
        checkImportedEntity(tenantId1, deviceProfile, tenantId2, importedDeviceProfile);
        checkImportedDeviceProfileData(deviceProfile, importedDeviceProfile);

        assertThat(importedDevice.getDeviceProfileId()).isEqualTo(importedDeviceProfile.getId());

        DeviceCredentials importedCredentials = findDeviceCredentials(importedDevice.getId());
        assertThat(importedCredentials.getId()).isNotEqualTo(deviceCredentials.getId());
        assertThat(importedCredentials.getCredentialsId()).isEqualTo(deviceCredentials.getCredentialsId());
        assertThat(importedCredentials.getCredentialsValue()).isEqualTo(deviceCredentials.getCredentialsValue());
        assertThat(importedCredentials.getCredentialsType()).isEqualTo(deviceCredentials.getCredentialsType());

        OtaPackage importedFirmwareOta = findOtaPackage(firmware.getTitle());
        OtaPackage importedSoftwareOta = findOtaPackage(software.getTitle());
        checkImportedEntity(tenantId1, firmware, tenantId2, importedFirmwareOta);
        checkImportedOtaPackageData(firmware, importedFirmwareOta);
        checkImportedEntity(tenantId1, software, tenantId2, importedSoftwareOta);
        checkImportedOtaPackageData(software, importedSoftwareOta);
    }

    @Test
    public void testDashboardVc_betweenTenants() throws Exception {
        Dashboard dashboard = createDashboard(null, "Dashboard of tenant 1");
        String versionId = createVersion("dashboards", EntityType.DASHBOARD);

        loginTenant2();
        loadVersion(versionId, EntityType.DASHBOARD);
        Dashboard importedDashboard = findDashboard(dashboard.getName());
        checkImportedEntity(tenantId1, dashboard, tenantId2, importedDashboard);
        checkImportedDashboardData(dashboard, importedDashboard);
    }

    @Test
    public void testDashboardVc_sameTenant() throws Exception {
        Dashboard dashboard = createDashboard(null, "Dashboard v1.0");
        String versionId = createVersion("dashboards", EntityType.DASHBOARD);

        loadVersion(versionId, EntityType.DASHBOARD);
        Dashboard importedDashboard = findDashboard(dashboard.getName());
        checkImportedEntity(tenantId1, dashboard, tenantId1, importedDashboard);
        checkImportedDashboardData(dashboard, importedDashboard);
    }

    @Test
    public void testDashboardVc_betweenTenants_withCustomer_updated() throws Exception {
        Dashboard dashboard = createDashboard(null, "Dashboard of tenant 1");
        String versionId = createVersion("dashboards", EntityType.DASHBOARD);

        loginTenant2();
        loadVersion(versionId, EntityType.DASHBOARD);
        Dashboard importedDashboard = findDashboard(dashboard.getName());
        checkImportedEntity(tenantId1, dashboard, tenantId2, importedDashboard);

        loginTenant1();
        Customer customer = createCustomer("Customer 1");
        versionId = createVersion("customers", EntityType.CUSTOMER);
        assignDashboardToCustomer(dashboard.getId(), customer.getId());
        versionId = createVersion("assign dashboard", EntityType.DASHBOARD);

        loginTenant2();
        loadVersion(versionId, EntityType.DASHBOARD, EntityType.CUSTOMER);
        Customer importedCustomer = findCustomer(customer.getName());
        importedDashboard = findDashboard(dashboard.getName());
        assertThat(importedDashboard.getAssignedCustomers()).hasOnlyOneElementSatisfying(customerInfo -> {
            assertThat(customerInfo.getCustomerId()).isEqualTo(importedCustomer.getId());
        });
    }

    @Test
    public void testDashboardVc_betweenTenants_withEntityAliases() throws Exception {
        AssetProfile assetProfile = createAssetProfile(null, null, "A");
        Asset asset1 = createAsset(null, assetProfile.getId(), "Asset 1");
        Asset asset2 = createAsset(null, assetProfile.getId(), "Asset 2");
        Dashboard dashboard = createDashboard(null, "Dashboard 1");
        Dashboard otherDashboard = createDashboard(null, "Dashboard 2");
        loginTenant2();
        DeviceProfile existingDeviceProfile = createDeviceProfile(null, null, "Existing");

        loginTenant1();
        String aliasId = "23c4185d-1497-9457-30b2-6d91e69a5b2c";
        String unknownUuid = "ea0dc8b0-3d85-11ed-9200-77fc04fa14fa";
        String entityAliases = "{\n" +
                "\"" + aliasId + "\": {\n" +
                "\"alias\": \"assets\",\n" +
                "\"filter\": {\n" +
                "   \"entityList\": [\n" +
                "   \"" + asset1.getId() + "\",\n" +
                "   \"" + asset2.getId() + "\",\n" +
                "   \"" + tenantId1.getId() + "\",\n" +
                "   \"" + existingDeviceProfile.getId() + "\",\n" +
                "   \"" + unknownUuid + "\"\n" +
                "   ],\n" +
                "   \"id\":\"" + asset1.getId() + "\",\n" +
                "   \"resolveMultiple\": true\n" +
                "},\n" +
                "\"id\": \"" + aliasId + "\"\n" +
                "}\n" +
                "}";
        String widgetId = "ea8f34a0-264a-f11f-cde3-05201bb4ff4b";
        String actionId = "4a8e6efa-3e68-fa59-7feb-d83366130cae";
        String widgets = "{\n" +
                "  \"" + widgetId + "\": {\n" +
                "    \"config\": {\n" +
                "      \"actions\": {\n" +
                "        \"rowClick\": [\n" +
                "          {\n" +
                "            \"name\": \"go to dashboard\",\n" +
                "            \"targetDashboardId\": \"" + otherDashboard.getId() + "\",\n" +
                "            \"id\": \"" + actionId + "\"\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    },\n" +
                "    \"row\": 0,\n" +
                "    \"col\": 0,\n" +
                "    \"id\": \"" + widgetId + "\"\n" +
                "  }\n" +
                "}";

        ObjectNode dashboardConfiguration = JacksonUtil.newObjectNode();
        dashboardConfiguration.set("entityAliases", JacksonUtil.toJsonNode(entityAliases));
        dashboardConfiguration.set("widgets", JacksonUtil.toJsonNode(widgets));
        dashboardConfiguration.set("description", new TextNode("hallo"));
        dashboard.setConfiguration(dashboardConfiguration);
        dashboard = doPost("/api/dashboard", dashboard, Dashboard.class);

        String versionId = createVersion("dashboard with related", EntityType.ASSET, EntityType.ASSET_PROFILE, EntityType.DASHBOARD);

        loginTenant2();
        loadVersion(versionId, EntityType.ASSET, EntityType.ASSET_PROFILE, EntityType.DASHBOARD);

        AssetProfile importedProfile = findAssetProfile(assetProfile.getName());
        Asset importedAsset1 = findAsset(asset1.getName());
        Asset importedAsset2 = findAsset(asset2.getName());
        Dashboard importedOtherDashboard = findDashboard(otherDashboard.getName());
        Dashboard importedDashboard = findDashboard(dashboard.getName());

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
        assertThat(entityAlias.getValue().get("filter").get("id").asText()).as("external asset 1 was replaced with imported one")
                .isEqualTo(importedAsset1.getId().toString());

        ObjectNode widgetConfig = importedDashboard.getWidgetsConfig().get(0);
        assertThat(widgetConfig.get("id").asText()).as("widget id is not replaced")
                .isEqualTo(widgetId);
        JsonNode actionConfig = widgetConfig.get("config").get("actions").get("rowClick").get(0);
        assertThat(actionConfig.get("id").asText()).as("action id is not replaced")
                .isEqualTo(actionId);
        assertThat(actionConfig.get("targetDashboardId").asText()).as("dashboard id is replaced with imported one")
                .isEqualTo(importedOtherDashboard.getId().toString());
    }

    @Test
    public void testRuleChainVc_betweenTenants() throws Exception {
        RuleChain ruleChain = createRuleChain("Rule chain of tenant 1");
        RuleChainMetaData metaData = findRuleChainMetaData(ruleChain.getId());
        String versionId = createVersion("rule chains", EntityType.RULE_CHAIN);

        loginTenant2();
        loadVersion(versionId, EntityType.RULE_CHAIN);
        RuleChain importedRuleChain = findRuleChain(ruleChain.getName());
        RuleChainMetaData importedMetaData = findRuleChainMetaData(importedRuleChain.getId());

        checkImportedEntity(tenantId1, ruleChain, tenantId2, importedRuleChain);
        checkImportedRuleChainData(ruleChain, metaData, importedRuleChain, importedMetaData);
    }

    @Test
    public void testRuleChainVc_sameTenant() throws Exception {
        RuleChain ruleChain = createRuleChain("Rule chain v1.0");
        RuleChainMetaData metaData = findRuleChainMetaData(ruleChain.getId());
        String versionId = createVersion("rule chains", EntityType.RULE_CHAIN);

        loadVersion(versionId, EntityType.RULE_CHAIN);
        RuleChain importedRuleChain = findRuleChain(ruleChain.getName());
        RuleChainMetaData importedMetaData = findRuleChainMetaData(importedRuleChain.getId());

        checkImportedEntity(tenantId1, ruleChain, tenantId1, importedRuleChain);
        checkImportedRuleChainData(ruleChain, metaData, importedRuleChain, importedMetaData);
    }

    @Test
    public void testRuleChainVc_ruleNodesConfigs() throws Exception {
        Customer customer = createCustomer("Customer 1");
        RuleChain ruleChain = createRuleChain("Rule chain 1");
        RuleChainMetaData metaData = findRuleChainMetaData(ruleChain.getId());

        List<RuleNode> nodes = new ArrayList<>(metaData.getNodes());
        RuleNode generatorNode = new RuleNode();
        generatorNode.setName("Generator");
        generatorNode.setType(TbMsgGeneratorNode.class.getName());
        TbMsgGeneratorNodeConfiguration generatorNodeConfig = new TbMsgGeneratorNodeConfiguration();
        generatorNodeConfig.setOriginatorType(EntityType.ASSET_PROFILE);
        generatorNodeConfig.setOriginatorId(customer.getId().toString());
        generatorNodeConfig.setPeriodInSeconds(5);
        generatorNodeConfig.setMsgCount(1);
        generatorNodeConfig.setScriptLang(ScriptLanguage.JS);
        UUID someUuid = UUID.randomUUID();
        generatorNodeConfig.setJsScript("var msg = { temp: 42, humidity: 77 };\n" +
                "var metadata = { data: 40 };\n" +
                "var msgType = \"POST_TELEMETRY_REQUEST\";\n" +
                "var someUuid = \"" + someUuid + "\";\n" +
                "return { msg: msg, metadata: metadata, msgType: msgType };");
        generatorNode.setConfiguration(JacksonUtil.valueToTree(generatorNodeConfig));
        nodes.add(generatorNode);
        metaData.setNodes(nodes);
        doPost("/api/ruleChain/metadata", metaData, RuleChainMetaData.class);

        String versionId = createVersion("rule chains with customers", EntityType.RULE_CHAIN, EntityType.CUSTOMER);

        loginTenant2();
        loadVersion(versionId, EntityType.RULE_CHAIN, EntityType.CUSTOMER);
        Customer importedCustomer = findCustomer(customer.getName());
        RuleChain importedRuleChain = findRuleChain(ruleChain.getName());
        RuleChainMetaData importedMetaData = findRuleChainMetaData(importedRuleChain.getId());

        TbMsgGeneratorNodeConfiguration importedGeneratorNodeConfig = JacksonUtil.treeToValue(importedMetaData.getNodes().stream()
                .filter(node -> node.getName().equals(generatorNode.getName()))
                .findFirst().get().getConfiguration(), TbMsgGeneratorNodeConfiguration.class);
        assertThat(importedGeneratorNodeConfig.getOriginatorId()).isEqualTo(importedCustomer.getId().toString());
        assertThat(importedGeneratorNodeConfig.getJsScript()).contains("var someUuid = \"" + someUuid + "\";");
    }

    @Test
    public void testVcWithRelations_betweenTenants() throws Exception {
        Asset asset = createAsset(null, null, "Asset 1");
        Device device = createDevice("Device 1", "test1");
        EntityRelation relation = createRelation(asset.getId(), device.getId());
        String versionId = createVersion("assets and devices", EntityType.ASSET, EntityType.DEVICE, EntityType.DEVICE_PROFILE, EntityType.ASSET_PROFILE);

        loginTenant2();
        loadVersion(versionId, config -> {
            config.setLoadCredentials(false);
        }, EntityType.ASSET, EntityType.DEVICE, EntityType.DEVICE_PROFILE, EntityType.ASSET_PROFILE);

        Asset importedAsset = findAsset(asset.getName());
        Device importedDevice = findDevice(device.getName());
        checkImportedEntity(tenantId1, device, tenantId2, importedDevice);
        checkImportedEntity(tenantId1, asset, tenantId2, importedAsset);

        List<EntityRelation> importedRelations = findRelationsByTo(importedDevice.getId());
        assertThat(importedRelations).size().isOne();
        assertThat(importedRelations.get(0)).satisfies(importedRelation -> {
            assertThat(importedRelation.getFrom()).isEqualTo(importedAsset.getId());
            assertThat(importedRelation.getType()).isEqualTo(relation.getType());
            assertThat(importedRelation.getAdditionalInfo()).isEqualTo(relation.getAdditionalInfo());
        });
    }

    @Test
    public void testVcWithRelations_sameTenant() throws Exception {
        Asset asset = createAsset(null, null, "Asset 1");
        Device device1 = createDevice("Device 1", "test1");
        EntityRelation relation1 = createRelation(device1.getId(), asset.getId());
        String versionId = createVersion("assets", EntityType.ASSET);

        Device device2 = createDevice("Device 2", "test2");
        EntityRelation relation2 = createRelation(device2.getId(), asset.getId());
        List<EntityRelation> relations = findRelationsByTo(asset.getId());
        assertThat(relations).contains(relation1, relation2);

        loadVersion(versionId, EntityType.ASSET);

        relations = findRelationsByTo(asset.getId());
        assertThat(relations).contains(relation1);
        assertThat(relations).doesNotContain(relation2);
    }

    @Test
    public void testDefaultDeviceProfileVc_betweenTenants_findExisting() throws Exception {
        DeviceProfile defaultDeviceProfile = findDeviceProfile("default");
        defaultDeviceProfile.setName("non-default-name");
        doPost("/api/deviceProfile", defaultDeviceProfile, DeviceProfile.class);
        String versionId = createVersion("device profiles", EntityType.DEVICE_PROFILE);

        loginTenant2();
        loadVersion(versionId, config -> {
            config.setFindExistingEntityByName(false);
        }, EntityType.DEVICE_PROFILE);

        DeviceProfile importedDeviceProfile = findDeviceProfile(defaultDeviceProfile.getName());
        assertThat(importedDeviceProfile.isDefault()).isTrue();
        assertThat(importedDeviceProfile.getName()).isEqualTo(defaultDeviceProfile.getName());
        checkImportedEntity(tenantId1, defaultDeviceProfile, tenantId2, importedDeviceProfile);
    }

    @Test
    public void testVcWithCalculatedFields_betweenTenants() throws Exception {
        Asset asset = createAsset(null, null, "Asset 1");
        Device device = createDevice("Device 1", "test1");
        CalculatedField calculatedField = createCalculatedField("CalculatedField1", device.getId(), asset.getId());
        String versionId = createVersion("calculated fields of asset and device", EntityType.ASSET, EntityType.DEVICE, EntityType.DEVICE_PROFILE, EntityType.ASSET_PROFILE);

        loginTenant2();
        loadVersion(versionId, config -> {
            config.setLoadCredentials(false);
        }, EntityType.ASSET, EntityType.DEVICE, EntityType.DEVICE_PROFILE, EntityType.ASSET_PROFILE);

        Asset importedAsset = findAsset(asset.getName());
        Device importedDevice = findDevice(device.getName());
        checkImportedEntity(tenantId1, device, tenantId2, importedDevice);
        checkImportedEntity(tenantId1, asset, tenantId2, importedAsset);

        List<CalculatedField> importedCalculatedFields = findCalculatedFieldsByEntityId(importedDevice.getId());
        assertThat(importedCalculatedFields).size().isOne();
        assertThat(importedCalculatedFields.get(0)).satisfies(importedField -> {
            assertThat(importedField.getName()).isEqualTo(calculatedField.getName());
            assertThat(importedField.getType()).isEqualTo(calculatedField.getType());
            assertThat(importedField.getId()).isNotEqualTo(calculatedField.getId());
        });
    }

    @Test
    public void testVcWithReferencedCalculatedFields_betweenTenants() throws Exception {
        Asset asset = createAsset(null, null, "Asset 1");
        Device device = createDevice("Device 1", "test1");
        CalculatedField deviceCalculatedField = createCalculatedField("CalculatedField1", device.getId(), asset.getId());
        CalculatedField assetCalculatedField = createCalculatedField("CalculatedField2", asset.getId(), device.getId());
        String versionId = createVersion("calculated fields of asset and device", EntityType.ASSET, EntityType.DEVICE, EntityType.DEVICE_PROFILE, EntityType.ASSET_PROFILE);

        loginTenant2();
        loadVersion(versionId, config -> {
            config.setLoadCredentials(false);
        }, EntityType.ASSET, EntityType.DEVICE, EntityType.DEVICE_PROFILE, EntityType.ASSET_PROFILE);

        Asset importedAsset = findAsset(asset.getName());
        Device importedDevice = findDevice(device.getName());
        checkImportedEntity(tenantId1, device, tenantId2, importedDevice);
        checkImportedEntity(tenantId1, asset, tenantId2, importedAsset);

        List<CalculatedField> importedDeviceCalculatedFields = findCalculatedFieldsByEntityId(importedDevice.getId());
        assertThat(importedDeviceCalculatedFields).size().isOne();
        assertThat(importedDeviceCalculatedFields.get(0)).satisfies(importedField -> {
            assertThat(importedField.getName()).isEqualTo(deviceCalculatedField.getName());
            assertThat(importedField.getType()).isEqualTo(deviceCalculatedField.getType());
            assertThat(importedField.getId()).isNotEqualTo(deviceCalculatedField.getId());
            assertThat(importedField.getConfiguration()).isInstanceOf(SimpleCalculatedFieldConfiguration.class);
            SimpleCalculatedFieldConfiguration simpleCfg = (SimpleCalculatedFieldConfiguration) importedField.getConfiguration();
            assertThat(simpleCfg.getArguments().get("T").getRefEntityId()).isEqualTo(importedAsset.getId());
        });

        List<CalculatedField> importedAssetCalculatedFields = findCalculatedFieldsByEntityId(importedAsset.getId());
        assertThat(importedAssetCalculatedFields).size().isOne();
        assertThat(importedAssetCalculatedFields.get(0)).satisfies(importedField -> {
            assertThat(importedField.getName()).isEqualTo(assetCalculatedField.getName());
            assertThat(importedField.getType()).isEqualTo(assetCalculatedField.getType());
            assertThat(importedField.getId()).isNotEqualTo(assetCalculatedField.getId());
            assertThat(importedField.getConfiguration()).isInstanceOf(SimpleCalculatedFieldConfiguration.class);
            SimpleCalculatedFieldConfiguration simpleCfg = (SimpleCalculatedFieldConfiguration) importedField.getConfiguration();
            assertThat(simpleCfg.getArguments().get("T").getRefEntityId()).isEqualTo(importedDevice.getId());
        });
    }

    @Test
    public void testVcWithCalculatedFields_sameTenant() throws Exception {
        Asset asset = createAsset(null, null, "Asset 1");
        CalculatedField calculatedField = createCalculatedField("CalculatedField", asset.getId(), asset.getId());
        String versionId = createVersion("asset and field", EntityType.ASSET);

        loadVersion(versionId, EntityType.ASSET);
        CalculatedField importedCalculatedField = findCalculatedFieldByEntityId(asset.getId());
        assertThat(importedCalculatedField.getId()).isEqualTo(calculatedField.getId());
        assertThat(importedCalculatedField.getName()).isEqualTo(calculatedField.getName());
        assertThat(importedCalculatedField.getConfiguration()).isEqualTo(calculatedField.getConfiguration());
        assertThat(importedCalculatedField.getType()).isEqualTo(calculatedField.getType());
    }

    @Test
    public void testOtaPackageVc_sameTenant() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile(null, null, "Device profile v1.0");
        OtaPackage firmware = createOtaPackage(tenantId1, deviceProfile.getId(), OtaPackageType.FIRMWARE);
        OtaPackage software = createOtaPackage(tenantId1, deviceProfile.getId(), OtaPackageType.SOFTWARE);
        String versionId = createVersion("ota packages", EntityType.OTA_PACKAGE);

        OtaPackage firmwareOta = findOtaPackage(firmware.getTitle());
        OtaPackage softwareOta = findOtaPackage(software.getTitle());

        loadVersion(versionId, EntityType.OTA_PACKAGE);
        OtaPackage importedFirmwareOta = findOtaPackage(firmwareOta.getTitle());
        OtaPackage importedSoftwareOta = findOtaPackage(softwareOta.getTitle());
        checkImportedEntity(tenantId1, firmwareOta, tenantId1, importedFirmwareOta);
        checkImportedOtaPackageData(firmwareOta, importedFirmwareOta);
        checkImportedEntity(tenantId1, softwareOta, tenantId1, importedSoftwareOta);
        checkImportedOtaPackageData(softwareOta, importedSoftwareOta);
    }

    @Test
    public void testOtaPackageVcWithProfile_betweenTenants() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile(null, null, "Device profile v1.0");
        OtaPackage firmware = createOtaPackage(tenantId1, deviceProfile.getId(), OtaPackageType.FIRMWARE);
        OtaPackage software = createOtaPackage(tenantId1, deviceProfile.getId(), OtaPackageType.SOFTWARE);
        deviceProfile.setFirmwareId(firmware.getId());
        deviceProfile.setSoftwareId(software.getId());
        deviceProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);
        String versionId = createVersion("ota packages", EntityType.DEVICE_PROFILE, EntityType.OTA_PACKAGE);

        loginTenant2();
        loadVersion(versionId, EntityType.DEVICE_PROFILE, EntityType.OTA_PACKAGE);
        DeviceProfile importedProfile = findDeviceProfile(deviceProfile.getName());
        OtaPackage importedFirmwareOta = findOtaPackage(firmware.getTitle());
        OtaPackage importedSoftwareOta = findOtaPackage(software.getTitle());
        checkImportedEntity(tenantId1, deviceProfile, tenantId2, importedProfile);
        checkImportedDeviceProfileData(deviceProfile, importedProfile);
        checkImportedEntity(tenantId1, firmware, tenantId2, importedFirmwareOta);
        checkImportedOtaPackageData(firmware, importedFirmwareOta);
        checkImportedEntity(tenantId1, software, tenantId2, importedSoftwareOta);
        checkImportedOtaPackageData(software, importedSoftwareOta);
        assertThat(importedProfile.getFirmwareId()).isEqualTo(importedFirmwareOta.getId());
        assertThat(importedProfile.getSoftwareId()).isEqualTo(importedSoftwareOta.getId());
    }

    protected void checkImportedOtaPackageData(OtaPackage otaPackage, OtaPackage importedOtaPackage) {
        assertThat(importedOtaPackage.getName()).isEqualTo(otaPackage.getName());
        assertThat(importedOtaPackage.getTag()).isEqualTo(otaPackage.getTag());
        assertThat(importedOtaPackage.getType()).isEqualTo(otaPackage.getType());
        assertThat(importedOtaPackage.getFileName()).isEqualTo(otaPackage.getFileName());
    }

    @Test
    public void testResourceVc_sameTenant() throws Exception {
        TbResourceInfo resourceInfo = createResource("Test resource");
        String versionId = createVersion("resources", EntityType.TB_RESOURCE);

        TbResource resource = findResource(resourceInfo.getName());

        loadVersion(versionId, EntityType.TB_RESOURCE);
        TbResource importedResource = findResource(resource.getName());
        checkImportedEntity(tenantId1, resource, tenantId1, importedResource);
        checkImportedResourceData(resource, importedResource);
    }

    protected void checkImportedResourceData(TbResource resource, TbResource importedResource) {
        assertThat(importedResource.getName()).isEqualTo(resource.getName());
        assertThat(importedResource.getData()).isEqualTo(resource.getData());
        assertThat(importedResource.getResourceKey()).isEqualTo(resource.getResourceKey());
        assertThat(importedResource.getResourceType()).isEqualTo(resource.getResourceType());
    }

    private <E extends ExportableEntity<?> & HasTenantId> void checkImportedEntity(TenantId tenantId1, E initialEntity, TenantId tenantId2, E importedEntity) {
        assertThat(initialEntity.getTenantId()).isEqualTo(tenantId1);
        assertThat(importedEntity.getTenantId()).isEqualTo(tenantId2);
        assertThat(importedEntity.getExternalId()).isEqualTo(initialEntity.getId());
        boolean sameTenant = tenantId1.equals(tenantId2);
        if (sameTenant) {
            assertThat(importedEntity.getId()).isEqualTo(initialEntity.getId());
        } else {
            assertThat(importedEntity.getId()).isNotEqualTo(initialEntity.getId());
        }
    }

    protected void checkImportedAssetData(Asset initialAsset, Asset importedAsset) {
        assertThat(importedAsset.getName()).isEqualTo(initialAsset.getName());
        assertThat(importedAsset.getType()).isEqualTo(initialAsset.getType());
        assertThat(importedAsset.getLabel()).isEqualTo(initialAsset.getLabel());
        assertThat(importedAsset.getAdditionalInfo()).isEqualTo(initialAsset.getAdditionalInfo());
    }

    protected void checkImportedAssetProfileData(AssetProfile initialProfile, AssetProfile importedProfile) {
        assertThat(initialProfile.getName()).isEqualTo(importedProfile.getName());
        assertThat(initialProfile.getDescription()).isEqualTo(importedProfile.getDescription());
    }

    protected void checkImportedDeviceData(Device initialDevice, Device importedDevice) {
        assertThat(importedDevice.getName()).isEqualTo(initialDevice.getName());
        assertThat(importedDevice.getType()).isEqualTo(initialDevice.getType());
        assertThat(importedDevice.getDeviceData()).isEqualTo(initialDevice.getDeviceData());
        assertThat(importedDevice.getLabel()).isEqualTo(initialDevice.getLabel());
    }

    protected void checkImportedDeviceProfileData(DeviceProfile initialProfile, DeviceProfile importedProfile) {
        assertThat(initialProfile.getName()).isEqualTo(importedProfile.getName());
        assertThat(initialProfile.getType()).isEqualTo(importedProfile.getType());
        assertThat(initialProfile.getTransportType()).isEqualTo(importedProfile.getTransportType());
        assertThat(initialProfile.getProfileData()).isEqualTo(importedProfile.getProfileData());
        assertThat(initialProfile.getDescription()).isEqualTo(importedProfile.getDescription());
    }

    protected void checkImportedCustomerData(Customer initialCustomer, Customer importedCustomer) {
        assertThat(importedCustomer.getTitle()).isEqualTo(initialCustomer.getTitle());
        assertThat(importedCustomer.getCountry()).isEqualTo(initialCustomer.getCountry());
        assertThat(importedCustomer.getAddress()).isEqualTo(initialCustomer.getAddress());
        assertThat(importedCustomer.getEmail()).isEqualTo(initialCustomer.getEmail());
    }

    protected void checkImportedDashboardData(Dashboard initialDashboard, Dashboard importedDashboard) {
        assertThat(importedDashboard.getTitle()).isEqualTo(initialDashboard.getTitle());
        assertThat(importedDashboard.getConfiguration()).isEqualTo(initialDashboard.getConfiguration());
        assertThat(importedDashboard.getImage()).isEqualTo(initialDashboard.getImage());
        assertThat(importedDashboard.isMobileHide()).isEqualTo(initialDashboard.isMobileHide());
        if (initialDashboard.getAssignedCustomers() != null) {
            assertThat(importedDashboard.getAssignedCustomers()).containsAll(initialDashboard.getAssignedCustomers());
        }
    }

    private String createVersion(String name, EntityType... entityTypes) throws Exception {
        ComplexVersionCreateRequest request = new ComplexVersionCreateRequest();
        request.setVersionName(name);
        request.setBranch(branch);
        request.setSyncStrategy(SyncStrategy.MERGE);
        request.setEntityTypes(Arrays.stream(entityTypes).collect(Collectors.toMap(t -> t, entityType -> {
            EntityTypeVersionCreateConfig config = new EntityTypeVersionCreateConfig();
            config.setAllEntities(true);
            config.setSaveRelations(true);
            config.setSaveAttributes(true);
            config.setSaveCredentials(true);
            config.setSaveCalculatedFields(true);
            return config;
        })));

        UUID requestId = doPostAsync("/api/entities/vc/version", request, UUID.class, status().isOk());
        VersionCreationResult result = await().atMost(30, TimeUnit.SECONDS)
                .until(() -> doGet("/api/entities/vc/version/" + requestId + "/status", VersionCreationResult.class), r -> {
                    if (r.getError() != null) {
                        throw new RuntimeException("Failed to create version '" + name + "': " + r.getError());
                    }
                    return r.isDone();
                });
        assertThat(result.getVersion()).isNotNull();
        return result.getVersion().getId();
    }

    private String createVersion(String name, EntityId... entities) throws Exception {
        ComplexVersionCreateRequest request = new ComplexVersionCreateRequest();
        request.setVersionName(name);
        request.setBranch(branch);
        request.setSyncStrategy(SyncStrategy.MERGE);
        request.setEntityTypes(new HashMap<>());
        Map<EntityType, List<EntityId>> entitiesByType = Arrays.stream(entities)
                .collect(Collectors.groupingBy(EntityId::getEntityType));
        entitiesByType.forEach((entityType, ids) -> {
            EntityTypeVersionCreateConfig config = new EntityTypeVersionCreateConfig();
            config.setAllEntities(false);
            config.setEntityIds(ids.stream().map(EntityId::getId).toList());

            config.setSaveRelations(true);
            config.setSaveAttributes(true);
            config.setSaveCredentials(true);
            request.getEntityTypes().put(entityType, config);
        });

        return createVersion(request);
    }

    private String createVersion(VersionCreateRequest request) throws Exception {
        UUID requestId = doPostAsync("/api/entities/vc/version", request, UUID.class, status().isOk());
        VersionCreationResult result = await().atMost(60, TimeUnit.SECONDS)
                .until(() -> doGet("/api/entities/vc/version/" + requestId + "/status", VersionCreationResult.class), r -> {
                    if (r.getError() != null) {
                        throw new RuntimeException("Failed to create version '" + request.getVersionName() + "': " + r.getError());
                    }
                    return r.isDone();
                });
        assertThat(result.getVersion()).isNotNull();
        return result.getVersion().getId();
    }

    private Map<EntityType, EntityTypeLoadResult> loadVersion(String versionId, EntityType... entityTypes) throws Exception {
        return loadVersion(versionId, config -> {}, entityTypes);
    }

    private Map<EntityType, EntityTypeLoadResult> loadVersion(String versionId, Consumer<EntityTypeVersionLoadConfig> configModifier, EntityType... entityTypes) throws Exception {
        assertThat(listVersions()).extracting(EntityVersion::getId).contains(versionId);

        EntityTypeVersionLoadRequest request = new EntityTypeVersionLoadRequest();
        request.setVersionId(versionId);
        request.setRollbackOnError(true);
        request.setEntityTypes(Arrays.stream(entityTypes).collect(Collectors.toMap(t -> t, entityType -> {
            EntityTypeVersionLoadConfig config = new EntityTypeVersionLoadConfig();
            config.setLoadAttributes(true);
            config.setLoadRelations(true);
            config.setLoadCredentials(true);
            config.setLoadCalculatedFields(true);
            config.setRemoveOtherEntities(false);
            config.setFindExistingEntityByName(true);
            configModifier.accept(config);
            return config;
        })));

        UUID requestId = doPost("/api/entities/vc/entity", request, UUID.class);
        VersionLoadResult result = await().atMost(60, TimeUnit.SECONDS)
                .until(() -> doGet("/api/entities/vc/entity/" + requestId + "/status", VersionLoadResult.class), VersionLoadResult::isDone);
        if (result.getError() != null) {
            throw new RuntimeException("Failed to load version: " + result);
        }
        return result.getResult().stream().collect(Collectors.toMap(EntityTypeLoadResult::getEntityType, r -> r));
    }

    private List<EntityVersion> listVersions() throws Exception {
        PageData<EntityVersion> versions = doGetAsyncTyped("/api/entities/vc/version?branch=" + branch + "&pageSize=100&page=0&sortProperty=timestamp&sortOrder=DESC", new TypeReference<PageData<EntityVersion>>() {});
        return versions.getData();
    }

    private void configureRepository(TenantId tenantId) throws Exception {
        RepositorySettings repositorySettings = new RepositorySettings();
        repositorySettings.setLocalOnly(true);
        repositorySettings.setDefaultBranch(branch);
        repositorySettings.setAuthMethod(RepositoryAuthMethod.USERNAME_PASSWORD);
        repositorySettings.setRepositoryUri(repoKey);
        versionControlService.saveVersionControlSettings(tenantId, repositorySettings).get();
    }

    private void loginTenant1() throws Exception {
        login(tenantAdmin1.getEmail(), tenantAdmin1.getEmail());
    }

    private void loginTenant2() throws Exception {
        login(tenantAdmin2.getEmail(), tenantAdmin2.getEmail());
    }

    private Device createDevice(DeviceProfileId deviceProfileId, String name, String accessToken, Consumer<Device>... modifiers) {
        Device device = new Device();
        device.setName(name);
        device.setLabel("lbl");
        device.setDeviceProfileId(deviceProfileId);
        DeviceData deviceData = new DeviceData();
        deviceData.setTransportConfiguration(new DefaultDeviceTransportConfiguration());
        device.setDeviceData(deviceData);
        for (Consumer<Device> modifier : modifiers) {
            modifier.accept(device);
        }
        return doPost("/api/device?accessToken=" + accessToken, device, Device.class);
    }

    private DeviceProfile createDeviceProfile(RuleChainId defaultRuleChainId, DashboardId defaultDashboardId, String name) {
        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setName(name);
        deviceProfile.setDescription("dscrptn");
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        deviceProfile.setTransportType(DeviceTransportType.DEFAULT);
        deviceProfile.setDefaultRuleChainId(defaultRuleChainId);
        deviceProfile.setDefaultDashboardId(defaultDashboardId);
        DeviceProfileData profileData = new DeviceProfileData();
        profileData.setConfiguration(new DefaultDeviceProfileConfiguration());
        profileData.setTransportConfiguration(new DefaultDeviceProfileTransportConfiguration());
        deviceProfile.setProfileData(profileData);
        return doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);
    }

    protected EntityView createEntityView(CustomerId customerId, EntityId entityId, String name) {
        EntityView entityView = new EntityView();
        entityView.setTenantId(tenantId);
        entityView.setEntityId(entityId);
        entityView.setCustomerId(customerId);
        entityView.setName(name);
        entityView.setType("A");
        return doPost("/api/entityView", entityView, EntityView.class);
    }

    private AssetProfile createAssetProfile(RuleChainId defaultRuleChainId, DashboardId defaultDashboardId, String name) {
        AssetProfile assetProfile = new AssetProfile();
        assetProfile.setName(name);
        assetProfile.setDescription("dscrptn");
        assetProfile.setDefaultRuleChainId(defaultRuleChainId);
        assetProfile.setDefaultDashboardId(defaultDashboardId);
        return saveAssetProfile(assetProfile);
    }

    private AssetProfile saveAssetProfile(AssetProfile assetProfile) {
        return doPost("/api/assetProfile", assetProfile, AssetProfile.class);
    }

    private Asset createAsset(CustomerId customerId, AssetProfileId assetProfileId, String name) {
        Asset asset = new Asset();
        asset.setCustomerId(customerId);
        asset.setAssetProfileId(assetProfileId);
        asset.setName(name);
        asset.setLabel("lbl");
        asset.setAdditionalInfo(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        return doPost("/api/asset", asset, Asset.class);
    }

    protected Customer createCustomer(String name) {
        Customer customer = new Customer();
        customer.setTitle(name);
        customer.setCountry("ua");
        customer.setAddress("abb");
        customer.setEmail("ccc@aa.org");
        customer.setAdditionalInfo(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        return doPost("/api/customer", customer, Customer.class);
    }

    protected OtaPackage createOtaPackage(TenantId tenantId, DeviceProfileId deviceProfileId, OtaPackageType type) {
        OtaPackage otaPackage = new OtaPackage();
        otaPackage.setTenantId(tenantId);
        otaPackage.setDeviceProfileId(deviceProfileId);
        otaPackage.setType(type);
        otaPackage.setTitle("My " + type);
        otaPackage.setTag("My " + type);
        otaPackage.setVersion("v1.0");
        otaPackage.setFileName("filename.txt");
        otaPackage.setContentType("text/plain");
        otaPackage.setChecksumAlgorithm(ChecksumAlgorithm.SHA256);
        otaPackage.setChecksum("4bf5122f344554c53bde2ebb8cd2b7e3d1600ad631c385a5d7cce23c7785459a");
        otaPackage.setDataSize(1L);
        otaPackage.setData(ByteBuffer.wrap(new byte[]{(int) 1}));
        return otaPackageService.saveOtaPackage(otaPackage);
    }

    private OtaPackage findOtaPackage(String title) throws Exception {
        return doGetTypedWithPageLink("/api/otaPackages?", new TypeReference<PageData<OtaPackage>>() {}, new PageLink(100, 0, title)).getData().get(0);
    }

    protected Dashboard createDashboard(CustomerId customerId, String name) {
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle(name);
        dashboard.setConfiguration(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        dashboard.setImage("abvregewrg");
        dashboard.setMobileHide(true);
        dashboard = doPost("/api/dashboard", dashboard, Dashboard.class);
        if (customerId != null) {
            return assignDashboardToCustomer(dashboard.getId(), customerId);
        }
        return dashboard;
    }

    protected Dashboard createDashboard(CustomerId customerId, String name, AssetId assetForEntityAlias) {
        Dashboard dashboard = createDashboard(customerId, name);
        String entityAliases = "{\n" +
                "\t\"23c4185d-1497-9457-30b2-6d91e69a5b2c\": {\n" +
                "\t\t\"alias\": \"assets\",\n" +
                "\t\t\"filter\": {\n" +
                "\t\t\t\"entityList\": [\n" +
                "\t\t\t\t\"" + assetForEntityAlias.getId().toString() + "\"\n" +
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
        return doPost("/api/dashboard", dashboard, Dashboard.class);
    }

    protected RuleChain createRuleChain(String name, EntityId originatorId) throws Exception {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName(name);
        ruleChain.setType(RuleChainType.CORE);
        ruleChain.setDebugMode(true);
        ruleChain.setConfiguration(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        ruleChain = doPost("/api/ruleChain", ruleChain, RuleChain.class);

        RuleChainMetaData metaData = new RuleChainMetaData();
        metaData.setRuleChainId(ruleChain.getId());

        RuleNode ruleNode1 = new RuleNode();
        ruleNode1.setName("Generator 1");
        ruleNode1.setType(TbMsgGeneratorNode.class.getName());
        ruleNode1.setDebugSettings(DebugSettings.all());
        TbMsgGeneratorNodeConfiguration configuration1 = new TbMsgGeneratorNodeConfiguration();
        configuration1.setOriginatorType(originatorId.getEntityType());
        configuration1.setOriginatorId(originatorId.getId().toString());
        ruleNode1.setConfiguration(JacksonUtil.valueToTree(configuration1));

        RuleNode ruleNode2 = new RuleNode();
        ruleNode2.setName("Simple Rule Node 2");
        ruleNode2.setType(org.thingsboard.rule.engine.metadata.TbGetAttributesNode.class.getName());
        ruleNode2.setConfigurationVersion(TbGetAttributesNode.class.getAnnotation(org.thingsboard.rule.engine.api.RuleNode.class).version());
        ruleNode2.setDebugSettings(DebugSettings.all());
        TbGetAttributesNodeConfiguration configuration2 = new TbGetAttributesNodeConfiguration();
        configuration2.setServerAttributeNames(Collections.singletonList("serverAttributeKey2"));
        ruleNode2.setConfiguration(JacksonUtil.valueToTree(configuration2));

        metaData.setNodes(Arrays.asList(ruleNode1, ruleNode2));
        metaData.setFirstNodeIndex(0);
        metaData.addConnectionInfo(0, 1, TbNodeConnectionType.SUCCESS);
        doPost("/api/ruleChain/metadata", metaData, RuleChainMetaData.class);

        return doGet("/api/ruleChain/" + ruleChain.getUuidId(), RuleChain.class);
    }

    protected RuleChain createRuleChain(String name) throws Exception {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName(name);
        ruleChain.setType(RuleChainType.CORE);
        ruleChain.setDebugMode(true);
        ruleChain.setConfiguration(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        ruleChain = doPost("/api/ruleChain", ruleChain, RuleChain.class);

        RuleChainMetaData metaData = new RuleChainMetaData();
        metaData.setRuleChainId(ruleChain.getId());

        RuleNode ruleNode1 = new RuleNode();
        ruleNode1.setName("Simple Rule Node 1");
        ruleNode1.setType(org.thingsboard.rule.engine.metadata.TbGetAttributesNode.class.getName());
        ruleNode1.setConfigurationVersion(TbGetAttributesNode.class.getAnnotation(org.thingsboard.rule.engine.api.RuleNode.class).version());
        ruleNode1.setDebugSettings(DebugSettings.all());
        TbGetAttributesNodeConfiguration configuration1 = new TbGetAttributesNodeConfiguration();
        configuration1.setServerAttributeNames(Collections.singletonList("serverAttributeKey1"));
        ruleNode1.setConfiguration(JacksonUtil.valueToTree(configuration1));

        RuleNode ruleNode2 = new RuleNode();
        ruleNode2.setName("Simple Rule Node 2");
        ruleNode2.setType(org.thingsboard.rule.engine.metadata.TbGetAttributesNode.class.getName());
        ruleNode2.setConfigurationVersion(TbGetAttributesNode.class.getAnnotation(org.thingsboard.rule.engine.api.RuleNode.class).version());
        ruleNode2.setDebugSettings(DebugSettings.all());
        TbGetAttributesNodeConfiguration configuration2 = new TbGetAttributesNodeConfiguration();
        configuration2.setServerAttributeNames(Collections.singletonList("serverAttributeKey2"));
        ruleNode2.setConfiguration(JacksonUtil.valueToTree(configuration2));

        metaData.setNodes(Arrays.asList(ruleNode1, ruleNode2));
        metaData.setFirstNodeIndex(0);
        metaData.addConnectionInfo(0, 1, TbNodeConnectionType.SUCCESS);
        doPost("/api/ruleChain/metadata", metaData, RuleChainMetaData.class);

        return doGet("/api/ruleChain/" + ruleChain.getUuidId(), RuleChain.class);
    }

    protected EntityRelation createRelation(EntityId from, EntityId to) throws Exception {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(from);
        relation.setTo(to);
        relation.setType(EntityRelation.MANAGES_TYPE);
        relation.setAdditionalInfo(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        return doPost("/api/v2/relation", relation, EntityRelation.class);
    }

    private CalculatedField createCalculatedField(String name, EntityId entityId, EntityId referencedEntityId) {
        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(entityId);
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setName(name);
        calculatedField.setConfigurationVersion(1);
        calculatedField.setConfiguration(getCalculatedFieldConfig(referencedEntityId));
        calculatedField.setVersion(1L);
        return doPost("/api/calculatedField", calculatedField, CalculatedField.class);
    }

    private CalculatedFieldConfiguration getCalculatedFieldConfig(EntityId referencedEntityId) {
        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();

        Argument argument = new Argument();
        argument.setRefEntityId(referencedEntityId);
        ReferencedEntityKey refEntityKey = new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null);
        argument.setRefEntityKey(refEntityKey);

        config.setArguments(Map.of("T", argument));

        config.setExpression("T - (100 - H) / 5");

        TimeSeriesOutput output = new TimeSeriesOutput();
        output.setName("output");

        config.setOutput(output);

        return config;
    }

    protected void checkImportedRuleChainData(RuleChain initialRuleChain, RuleChainMetaData initialMetaData, RuleChain importedRuleChain, RuleChainMetaData importedMetaData) {
        assertThat(importedRuleChain.getType()).isEqualTo(initialRuleChain.getType());
        assertThat(importedRuleChain.getName()).isEqualTo(initialRuleChain.getName());
        assertThat(importedRuleChain.isDebugMode()).isEqualTo(initialRuleChain.isDebugMode());
        assertThat(importedRuleChain.getConfiguration()).isEqualTo(initialRuleChain.getConfiguration());

        assertThat(importedMetaData.getConnections()).isEqualTo(initialMetaData.getConnections());
        assertThat(importedMetaData.getFirstNodeIndex()).isEqualTo(initialMetaData.getFirstNodeIndex());
        for (int i = 0; i < initialMetaData.getNodes().size(); i++) {
            RuleNode initialNode = initialMetaData.getNodes().get(i);
            RuleNode importedNode = importedMetaData.getNodes().get(i);
            assertThat(importedNode.getRuleChainId()).isEqualTo(importedRuleChain.getId());
            assertThat(importedNode.getName()).isEqualTo(initialNode.getName());
            assertThat(importedNode.getType()).isEqualTo(initialNode.getType());
            assertThat(importedNode.getConfiguration()).isEqualTo(initialNode.getConfiguration());
            assertThat(importedNode.getAdditionalInfo()).isEqualTo(initialNode.getAdditionalInfo());
        }
    }

    private Dashboard assignDashboardToCustomer(DashboardId dashboardId, CustomerId customerId) {
        return doPost("/api/customer/" + customerId + "/dashboard/" + dashboardId, Dashboard.class);
    }

    private Asset findAsset(String name) throws Exception {
        return doGetTypedWithPageLink("/api/tenant/assets?", new TypeReference<PageData<Asset>>() {}, new PageLink(100, 0, name)).getData().get(0);
    }

    private AssetProfile findAssetProfile(String name) throws Exception {
        return doGetTypedWithPageLink("/api/assetProfiles?", new TypeReference<PageData<AssetProfile>>() {}, new PageLink(100, 0, name)).getData().get(0);
    }

    private DeviceProfile findDeviceProfile(String name) throws Exception {
        return doGetTypedWithPageLink("/api/deviceProfiles?", new TypeReference<PageData<DeviceProfile>>() {}, new PageLink(100, 0, name)).getData().get(0);
    }

    private Device findDevice(String name) throws Exception {
        return doGetTypedWithPageLink("/api/tenant/devices?", new TypeReference<PageData<Device>>() {}, new PageLink(100, 0, name)).getData().get(0);
    }

    private DeviceCredentials findDeviceCredentials(DeviceId deviceId) throws Exception {
        return doGet("/api/device/" + deviceId + "/credentials", DeviceCredentials.class);
    }

    private Customer findCustomer(String name) throws Exception {
        return doGetTypedWithPageLink("/api/customers?", new TypeReference<PageData<Customer>>() {}, new PageLink(100, 0, name)).getData().get(0);
    }

    private Dashboard findDashboard(String name) throws Exception {
        DashboardInfo dashboardInfo = doGetTypedWithPageLink("/api/tenant/dashboards?", new TypeReference<PageData<DashboardInfo>>() {}, new PageLink(100, 0, name)).getData().get(0);
        return doGet("/api/dashboard/" + dashboardInfo.getUuidId(), Dashboard.class);
    }

    private RuleChain findRuleChain(String name) throws Exception {
        return doGetTypedWithPageLink("/api/ruleChains?", new TypeReference<PageData<RuleChain>>() {}, new PageLink(100, 0, name)).getData().get(0);
    }

    private RuleChainMetaData findRuleChainMetaData(RuleChainId ruleChainId) throws Exception {
        return doGet("/api/ruleChain/" + ruleChainId + "/metadata", RuleChainMetaData.class);
    }

    private CalculatedField findCalculatedFieldByEntityId(EntityId entityId) throws Exception {
        return doGetTypedWithPageLink("/api/" + entityId.getEntityType() + "/" + entityId.getId() + "/calculatedFields?", new TypeReference<PageData<CalculatedField>>() {}, new PageLink(100, 0)).getData().get(0);
    }

    private List<CalculatedField> findCalculatedFieldsByEntityId(EntityId entityId) throws Exception {
        return doGetTypedWithPageLink("/api/" + entityId.getEntityType() + "/" + entityId.getId() + "/calculatedFields?", new TypeReference<PageData<CalculatedField>>() {}, new PageLink(100, 0)).getData();
    }

    private TbResourceInfo createResource(String name) {
        TbResource resource = new TbResource();
        resource.setResourceType(ResourceType.JKS);
        resource.setTitle(name);
        resource.setFileName(JS_TEST_FILE_NAME);
        resource.setEncodedData(TEST_DATA);

        return saveTbResource(resource);
    }

    private TbResourceInfo saveTbResource(TbResource tbResource) {
        return doPost("/api/resource", tbResource, TbResourceInfo.class);
    }

    private TbResource findResource(String name) throws Exception {
        return doGetTypedWithPageLink("/api/resource?", new TypeReference<PageData<TbResource>>() {}, new PageLink(100, 0, name)).getData().get(0);
    }

}
