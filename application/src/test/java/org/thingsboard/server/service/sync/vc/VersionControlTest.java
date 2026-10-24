// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.sync.vc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Streams;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
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
import org.thingsboard.server.common.data.HasOwnerId;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.alarm.rule.condition.SimpleAlarmCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.TbelAlarmConditionExpression;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.AlarmCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.SimpleCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.TimeSeriesOutput;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.device.data.DefaultDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.ota.DeviceGroupOtaPackage;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.GroupPermissionInfo;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.query.DeviceTypeFilter;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.report.ReportConfig;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.ReportTemplateInfo;
import org.thingsboard.server.common.data.report.ReportTemplateType;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.common.data.report.configuration.CsvReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.DataSourceType;
import org.thingsboard.server.common.data.report.configuration.EntityAlias;
import org.thingsboard.server.common.data.report.configuration.components.EntityTableComponent;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.scheduler.SchedulerEventWithCustomerInfo;
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
import org.thingsboard.server.common.data.sync.vc.request.create.SingleEntityVersionCreateRequest;
import org.thingsboard.server.common.data.sync.vc.request.create.SyncStrategy;
import org.thingsboard.server.common.data.sync.vc.request.create.VersionCreateConfig;
import org.thingsboard.server.common.data.sync.vc.request.create.VersionCreateRequest;
import org.thingsboard.server.common.data.sync.vc.request.load.EntityTypeVersionLoadConfig;
import org.thingsboard.server.common.data.sync.vc.request.load.EntityTypeVersionLoadRequest;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.user.UserService;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.controller.TbResourceControllerTest.JS_TEST_FILE_NAME;
import static org.thingsboard.server.controller.TbResourceControllerTest.TEST_DATA;

@DaoSqlTest
@TestPropertySource(properties = {
        "service.integrations.supported=ALL",
})
public class VersionControlTest extends AbstractControllerTest {

    @Autowired
    private EntitiesVersionControlService versionControlService;
    @Autowired
    private OtaPackageService otaPackageService;
    @Autowired
    private UserService userService;

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
    public void testCustomerAndUsersVc_betweenTenants() throws Exception {
        Customer customer = createCustomer("Customer v1.0");
        String versionId = createVersion("customers", EntityType.ROLE, EntityType.CUSTOMER, EntityType.USER);

        loginTenant2();
        loadVersion(versionId, EntityType.ROLE, EntityType.CUSTOMER, EntityType.USER);
        Customer importedCustomer = findCustomer(customer.getName());
        checkImportedEntity(tenantId1, customer, tenantId2, importedCustomer);
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
    public void testDeviceVc_withAlarmRules_betweenTenants() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile(null, null, "Device profile of tenant 1");
        Dashboard dashboard = createDashboard(null, "Mobile dashboard");
        createAlarmRule(deviceProfile.getId(), "Profile alarm rule", dashboard.getId());
        Device device = createDevice(deviceProfile.getId(), "Device of tenant 1", "test1");
        createAlarmRule(device.getId(), "Device alarm rule", dashboard.getId());
        String version = createVersion("devices, profiles and dashboards", EntityType.DEVICE, EntityType.DEVICE_PROFILE, EntityType.DASHBOARD);

        loginTenant2();
        Map<EntityType, EntityTypeLoadResult> result = loadVersion(version, config -> {
            config.setLoadCredentials(false);
        }, EntityType.DEVICE, EntityType.DEVICE_PROFILE, EntityType.DASHBOARD);
        assertThat(result.get(EntityType.DEVICE).getCreated()).isEqualTo(1);
        assertThat(result.get(EntityType.DEVICE_PROFILE).getCreated()).isEqualTo(1);
        assertThat(result.get(EntityType.DASHBOARD).getCreated()).isEqualTo(1);

        Device importedDevice = findDevice(device.getName());
        checkImportedEntity(tenantId1, device, tenantId2, importedDevice);
        checkImportedDeviceData(device, importedDevice);

        DeviceProfile importedDeviceProfile = findDeviceProfile(deviceProfile.getName());
        checkImportedEntity(tenantId1, deviceProfile, tenantId2, importedDeviceProfile);
        checkImportedDeviceProfileData(deviceProfile, importedDeviceProfile);
        assertThat(importedDevice.getDeviceProfileId()).isEqualTo(importedDeviceProfile.getId());

        Dashboard importedDashboard = findDashboard(dashboard.getName());
        checkImportedEntity(tenantId1, dashboard, tenantId2, importedDashboard);
        checkImportedDashboardData(dashboard, importedDashboard);

        getCalculatedFields(CalculatedFieldType.ALARM, EntityType.DEVICE_PROFILE,
                List.of(importedDeviceProfile.getUuidId()), null).forEach(alarmRuleCf -> {
            assertThat(alarmRuleCf.getName()).isEqualTo("Profile alarm rule");
            AlarmCalculatedFieldConfiguration config = (AlarmCalculatedFieldConfiguration) alarmRuleCf.getConfiguration();
            config.getAllRules().map(Pair::getValue).forEach(alarmRule -> {
                assertThat(alarmRule.getDashboardId()).isEqualTo(importedDashboard.getId());
            });
        });
        getCalculatedFields(CalculatedFieldType.ALARM, EntityType.DEVICE_PROFILE,
                List.of(importedDevice.getUuidId()), null).forEach(alarmRuleCf -> {
            assertThat(alarmRuleCf.getName()).isEqualTo("Device alarm rule");
            AlarmCalculatedFieldConfiguration config = (AlarmCalculatedFieldConfiguration) alarmRuleCf.getConfiguration();
            config.getAllRules().map(Pair::getValue).forEach(alarmRule -> {
                assertThat(alarmRule.getDashboardId()).isEqualTo(importedDashboard.getId());
            });
        });
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

        Map.Entry<String, JsonNode> entityAlias = importedDashboard.getConfiguration().get("entityAliases").properties().iterator().next();
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
        generatorNodeConfig.setJsScript("""
                var msg = { temp: 42, humidity: 77 };
                var metadata = { data: 40 };
                var msgType = "POST_TELEMETRY_REQUEST";
                var someUuid = "%s";
                return { msg: msg, metadata: metadata, msgType: msgType };""".formatted(someUuid));
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
    public void testIntegrationVcWithConverter_betweenTenants() throws Exception {
        Converter converter = createConverter(ConverterType.DOWNLINK, "Converter 1");
        Integration integration = createIntegration(converter.getId(), IntegrationType.HTTP, "Integration 1");
        String versionId = createVersion("converters and integrations", EntityType.CONVERTER, EntityType.INTEGRATION);

        loginTenant2();
        loadVersion(versionId, config -> {
            config.setAutoGenerateIntegrationKey(true);
        }, EntityType.CONVERTER, EntityType.INTEGRATION);

        Converter importedConverter = findConverter(converter.getName());
        checkImportedEntity(tenantId1, converter, tenantId2, importedConverter);
        checkImportedConverterData(converter, importedConverter);

        Integration importedIntegration = findIntegration(integration.getName());
        checkImportedEntity(tenantId1, integration, tenantId2, importedIntegration);
        checkImportedIntegrationData(integration, importedIntegration);
    }

    @Test
    public void testIntegrationVcWithConverter_sameTenant() throws Exception {
        Converter converter = createConverter(ConverterType.DOWNLINK, "Converter 1");
        Integration integration = createIntegration(converter.getId(), IntegrationType.HTTP, "Integration 1");
        String versionId = createVersion("converters and integrations", EntityType.CONVERTER, EntityType.INTEGRATION);

        loadVersion(versionId, EntityType.CONVERTER, EntityType.INTEGRATION);

        Converter importedConverter = findConverter(converter.getName());
        checkImportedEntity(tenantId1, converter, tenantId1, importedConverter);
        checkImportedConverterData(converter, importedConverter);

        Integration importedIntegration = findIntegration(integration.getName());
        checkImportedEntity(tenantId1, integration, tenantId1, importedIntegration);
        checkImportedIntegrationData(integration, importedIntegration);
    }

    @Test
    public void testEntityGroupVc_betweenTenants() throws Exception {
        List<EntityGroup> entityGroups = new ArrayList<>();
        for (EntityType groupType : EntityGroup.groupTypes) {
            if (groupType == EntityType.EDGE || groupType == EntityType.AGENT) {
                continue;
            }
            EntityGroup entityGroup = createEntityGroup(tenantId1, groupType, groupType + " group");
            entityGroups.add(entityGroup);
        }
        String versionId = createVersion("entity groups", EntityGroup.groupTypes);

        loginTenant2();
        loadVersion(versionId, EntityGroup.groupTypes);

        for (EntityGroup entityGroup : entityGroups) {
            EntityGroup importedEntityGroup = findEntityGroup(entityGroup.getName(), entityGroup.getType());
            checkImportedEntity(tenantId1, tenantId1, entityGroup, tenantId2, tenantId2, importedEntityGroup);
            checkImportedEntityGroupData(entityGroup, importedEntityGroup);
        }
    }

    @Test
    public void testEntityGroupVc_sameTenant() throws Exception {
        List<EntityGroup> entityGroups = new ArrayList<>();
        for (EntityType groupType : EntityGroup.groupTypes) {
            if (groupType == EntityType.EDGE || groupType == EntityType.AGENT) {
                continue;
            }
            EntityGroup entityGroup = createEntityGroup(tenantId1, groupType, groupType + " group");
            entityGroups.add(entityGroup);
        }
        String versionId = createVersion("entity groups", EntityGroup.groupTypes);

        loadVersion(versionId, EntityGroup.groupTypes);

        for (EntityGroup entityGroup : entityGroups) {
            EntityGroup importedEntityGroup = findEntityGroup(entityGroup.getName(), entityGroup.getType());
            checkImportedEntity(tenantId1, tenantId1, entityGroup, tenantId1, tenantId1, importedEntityGroup);
            checkImportedEntityGroupData(entityGroup, importedEntityGroup);
        }
    }

    @Test
    public void testEntityGroupVcWithPermissions_betweenTenants() throws Exception {
        EntityGroup userGroup = createEntityGroup(tenantId1, EntityType.USER, "User group 1");
        EntityGroup deviceGroup = createEntityGroup(tenantId1, EntityType.DEVICE, "My devices");
        Role role = createGroupRole(null, "Role for User group 1", List.of(Operation.READ));
        createGroupPermission(userGroup.getId(), role.getId(), deviceGroup.getId(), EntityType.DEVICE);
        String versionId = createVersion("groups", EntityType.USER, EntityType.DEVICE, EntityType.ROLE);

        loginTenant2();
        loadVersion(versionId, EntityType.USER, EntityType.DEVICE, EntityType.ROLE);

        Role importedRole = findRole(role.getName());
        checkImportedEntity(tenantId1, role, tenantId2, importedRole);
        assertThat(importedRole.getName()).isEqualTo(role.getName());
        assertThat(importedRole.getPermissions()).isEqualTo(role.getPermissions());

        EntityGroup importedDeviceGroup = findEntityGroup(deviceGroup.getName(), EntityType.DEVICE);
        checkImportedEntity(tenantId1, tenantId1, deviceGroup, tenantId2, tenantId2, importedDeviceGroup);

        EntityGroup importedUserGroup = findEntityGroup(userGroup.getName(), EntityType.USER);
        checkImportedEntity(tenantId1, tenantId1, userGroup, tenantId2, tenantId2, importedUserGroup);

        List<GroupPermissionInfo> importedGroupPermissions = findGroupPermissions(importedUserGroup.getId());
        assertThat(importedGroupPermissions).singleElement().satisfies(importedGroupPermission -> {
            assertThat(importedGroupPermission.getRoleId()).isEqualTo(importedRole.getId());
            assertThat(importedGroupPermission.getEntityGroupId()).isEqualTo(importedDeviceGroup.getId());
            assertThat(importedGroupPermission.getEntityGroupType()).isEqualTo(EntityType.DEVICE);
        });
    }

    // Regression guard: USER entities are intentionally excluded from version control.
    // User export/import is exclusively a Solution Export/Import feature; VC commits must contain
    // neither serialized User entities nor user-group memberIds, and VC load must never create or
    // update User entities on the target tenant.
    @Test
    public void testUserEntitiesExcludedFromVc_betweenTenants() throws Exception {
        Customer customer = createCustomer("Customer with VC-excluded users");
        EntityGroupInfo customerAdminsGroup = findCustomerAdminsGroup(customer.getId());

        User customerUser = new User();
        customerUser.setTenantId(tenantId1);
        customerUser.setCustomerId(customer.getId());
        customerUser.setAuthority(Authority.CUSTOMER_USER);
        customerUser.setEmail("vc-excluded-user@example.com");
        createUser(customerUser, "vc-excluded", customerAdminsGroup.getId());

        EntityGroup standaloneUserGroup = createEntityGroup(tenantId1, EntityType.USER, "Standalone user group");

        String versionId = createVersion("user-exclusion regression",
                EntityType.ROLE, EntityType.CUSTOMER, EntityType.USER);

        loginTenant2();
        long usersOnTenant2Before = userService.findUsersByTenantId(tenantId2, new PageLink(1000)).getTotalElements();
        Map<EntityType, EntityTypeLoadResult> result = loadVersion(versionId,
                EntityType.ROLE, EntityType.CUSTOMER, EntityType.USER);
        long usersOnTenant2After = userService.findUsersByTenantId(tenantId2, new PageLink(1000)).getTotalElements();

        EntityTypeLoadResult userResult = result.get(EntityType.USER);
        if (userResult != null) {
            assertThat(userResult.getCreated()).as("VC must not create User entities").isZero();
            assertThat(userResult.getUpdated()).as("VC must not update User entities").isZero();
            assertThat(userResult.getDeleted()).as("VC must not delete User entities").isZero();
        }
        assertThat(usersOnTenant2After)
                .as("Tenant2 user count must be unchanged by VC import")
                .isEqualTo(usersOnTenant2Before);

        EntityGroup importedStandaloneGroup = findEntityGroup(standaloneUserGroup.getName(), EntityType.USER);
        assertThat(userService.findUsersByEntityGroupId(importedStandaloneGroup.getId(), new PageLink(100)).getData())
                .as("VC must not embed memberIds for user groups")
                .isEmpty();

        Customer importedCustomer = findCustomer(customer.getName());
        EntityGroupInfo importedCustomerAdminsGroup = findCustomerAdminsGroup(importedCustomer.getId());
        assertThat(userService.findUsersByEntityGroupId(importedCustomerAdminsGroup.getId(), new PageLink(100)).getData())
                .as("VC import must not place users into customer admin groups on the target tenant")
                .isEmpty();
    }

    @Test
    public void testDeviceGroupVcWithOtaPackage_betweenTenants() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile(null, null, "Device profile for OTA");
        OtaPackage firmware = createOtaPackage(tenantId1, deviceProfile.getId(), OtaPackageType.FIRMWARE);
        EntityGroup deviceGroup = createEntityGroup(tenantId1, EntityType.DEVICE, "Device group for OTA");

        DeviceGroupOtaPackage deviceGroupOtaPackage = new DeviceGroupOtaPackage();
        deviceGroupOtaPackage.setGroupId(deviceGroup.getId());
        deviceGroupOtaPackage.setOtaPackageType(OtaPackageType.FIRMWARE);
        deviceGroupOtaPackage.setOtaPackageId(firmware.getId());

        doPost("/api/deviceGroupOtaPackage", deviceGroupOtaPackage, DeviceGroupOtaPackage.class);

        String versionId = createVersion("device group with ota", EntityType.DEVICE, EntityType.DEVICE_PROFILE, EntityType.OTA_PACKAGE);

        loginTenant2();
        loadVersion(versionId, EntityType.DEVICE, EntityType.DEVICE_PROFILE, EntityType.OTA_PACKAGE);

        EntityGroup importedDeviceGroup = findEntityGroup(deviceGroup.getName(), EntityType.DEVICE);
        checkImportedEntity(tenantId1, tenantId1, deviceGroup, tenantId2, tenantId2, importedDeviceGroup);

        DeviceProfile importedDeviceProfile = findDeviceProfile(deviceProfile.getName());
        checkImportedEntity(tenantId1, deviceProfile, tenantId2, importedDeviceProfile);

        OtaPackage importedFirmware = findOtaPackage(firmware.getTitle());
        checkImportedEntity(tenantId1, firmware, tenantId2, importedFirmware);

        DeviceGroupOtaPackage importedDeviceGroupOtaPackage = findDeviceGroupOtaPackage(importedDeviceGroup.getId(), OtaPackageType.FIRMWARE);
        assertThat(importedDeviceGroupOtaPackage).isNotNull();
        assertThat(importedDeviceGroupOtaPackage.getGroupId()).isEqualTo(importedDeviceGroup.getId());
        assertThat(importedDeviceGroupOtaPackage.getOtaPackageId()).isEqualTo(importedFirmware.getId());
        assertThat(importedDeviceGroupOtaPackage.getOtaPackageType()).isEqualTo(OtaPackageType.FIRMWARE);
    }

    @Test
    public void testDeviceGroupVcWithoutEntities_betweenTenants() throws Exception {
        EntityGroup deviceGroup = createEntityGroup(tenantId1, EntityType.DEVICE, "Device group");
        Device device = createDevice("Test device", "test1");
        assignEntityToGroup(deviceGroup.getId(), device.getId());

        SingleEntityVersionCreateRequest request = new SingleEntityVersionCreateRequest();
        request.setEntityId(deviceGroup.getId());
        VersionCreateConfig config = new VersionCreateConfig();
        config.setSaveGroupEntities(false);
        config.setSaveAttributes(true);
        config.setSaveRelations(false);
        config.setSavePermissions(false);
        config.setSaveCredentials(false);
        config.setSaveCalculatedFields(false);
        request.setConfig(config);
        request.setVersionName("device group without entities");
        request.setBranch(branch);
        String versionId = createVersion(request);

        loginTenant2();
        loadVersion(versionId, EntityType.DEVICE, EntityType.DEVICE_PROFILE);

        EntityGroup importedDeviceGroup = findEntityGroup(deviceGroup.getName(), EntityType.DEVICE);
        checkImportedEntity(tenantId1, tenantId1, deviceGroup, tenantId2, tenantId2, importedDeviceGroup);
    }

    private void assignEntityToGroup(EntityGroupId id, EntityId entityId) throws Exception {
        doPost("/api/entityGroup/" + id.getId() + "/addEntities", List.of(entityId.getId().toString()));
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
    public void testEntityGroupVcWithPermissions_sameTenant() throws Exception {
        EntityGroup userGroup = createEntityGroup(tenantId1, EntityType.USER, "User group 1");
        EntityGroup deviceGroup = createEntityGroup(tenantId1, EntityType.DEVICE, "My devices");
        Role role = createGroupRole(null, "Role for User group 1", List.of(Operation.READ));
        GroupPermission groupPermission = createGroupPermission(userGroup.getId(), role.getId(), deviceGroup.getId(), EntityType.DEVICE);
        String versionId = createVersion("user groups", EntityType.USER);

        loadVersion(versionId, EntityType.USER);

        EntityGroup importedUserGroup = findEntityGroup(userGroup.getName(), EntityType.USER);
        checkImportedEntity(tenantId1, tenantId1, userGroup, tenantId1, tenantId1, importedUserGroup);

        List<GroupPermissionInfo> importedGroupPermissions = findGroupPermissions(importedUserGroup.getId());
        assertThat(importedGroupPermissions).singleElement().satisfies(permission -> {
            assertThat(new GroupPermission(permission)).isEqualTo(groupPermission);
        });
    }

    @Test
    public void testEntityGroupVcWithPermissions_betweenTenants_permissionsUpdated() throws Exception {
        EntityGroup deviceGroup = createEntityGroup(tenantId1, EntityType.DEVICE, "My devices");
        Role role = createGroupRole(null, "Role for User group 1", List.of(Operation.READ));
        EntityGroup userGroup = createEntityGroup(tenantId1, EntityType.USER, "User group 1");
        GroupPermission groupPermission = createGroupPermission(userGroup.getId(), role.getId(), deviceGroup.getId(), EntityType.DEVICE);
        String versionId = createVersion("groups", EntityType.USER, EntityType.DEVICE, EntityType.ROLE);

        loginTenant2();
        loadVersion(versionId, EntityType.USER, EntityType.DEVICE, EntityType.ROLE);

        Role importedRole = findRole(role.getName());
        EntityGroup importedDeviceGroup = findEntityGroup(deviceGroup.getName(), EntityType.DEVICE);
        EntityGroup importedUserGroup = findEntityGroup(userGroup.getName(), EntityType.USER);

        List<GroupPermissionInfo> importedGroupPermissions = findGroupPermissions(importedUserGroup.getId());
        assertThat(importedGroupPermissions).singleElement().satisfies(importedGroupPermission -> {
            assertThat(importedGroupPermission.getRoleId()).isEqualTo(importedRole.getId());
            assertThat(importedGroupPermission.getEntityGroupId()).isEqualTo(importedDeviceGroup.getId());
            assertThat(importedGroupPermission.getEntityGroupType()).isEqualTo(EntityType.DEVICE);
        });

        loginTenant1();
        doDelete("/api/groupPermission/" + groupPermission.getId()).andExpect(status().isOk());
        assertThat(findGroupPermissions(userGroup.getId())).isEmpty();
        role = createGenericRole(null, "Read devices", Map.of(
                Resource.DEVICE, List.of(Operation.READ),
                Resource.DEVICE_GROUP, List.of(Operation.READ)
        ));
        groupPermission = createGroupPermission(userGroup.getId(), role.getId());
        versionId = createVersion("groups 2", EntityType.ROLE, EntityType.USER);

        loginTenant2();
        loadVersion(versionId, EntityType.ROLE, EntityType.USER);

        Role newImportedRole = findRole(role.getName());
        List<GroupPermissionInfo> updatedGroupPermissions = findGroupPermissions(importedUserGroup.getId());
        assertThat(updatedGroupPermissions).singleElement().satisfies(newGroupPermission -> {
            assertThat(newGroupPermission.getEntityGroupId()).matches(entityGroupId -> entityGroupId == null || entityGroupId.isNullUid());
            assertThat(newGroupPermission.getRoleId()).isEqualTo(newImportedRole.getId());
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

    @Test
    public void testSchedulerEventVc_sameTenant() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile(null, null, "Device profile v1.0");
        SchedulerEvent schedulerEvent = createSchedulerEvent(tenantId1, deviceProfile.getId(), "General", "general", JacksonUtil.newObjectNode());
        String versionId = createVersion("scheduler event", EntityType.SCHEDULER_EVENT);

        loadVersion(versionId, EntityType.SCHEDULER_EVENT);
        SchedulerEvent importedEvent = findSchedulerEvent(schedulerEvent.getName());
        checkImportedEntity(tenantId1, schedulerEvent, tenantId1, importedEvent);
        checkImportedSchedulerEventData(schedulerEvent, importedEvent);
    }

    @Test
    public void testSchedulerEventOtaConfigForVcWithDeviceProfileOriginator_betweenTenants() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile(null, null, "Device profile v1.0");
        OtaPackage firmware = createOtaPackage(tenantId1, deviceProfile.getId(), OtaPackageType.FIRMWARE);
        OtaPackage software = createOtaPackage(tenantId1, deviceProfile.getId(), OtaPackageType.SOFTWARE);
        SchedulerEvent firmwareEvent = createSchedulerEventForOtaPackageType(tenantId1, deviceProfile.getId(), "Firmware", "updateFirmware", firmware.getId());
        SchedulerEvent softwareEvent = createSchedulerEventForOtaPackageType(tenantId1, deviceProfile.getId(), "Software", "updateSoftware", software.getId());
        String versionId = createVersion("scheduler event with ota", EntityType.DEVICE_PROFILE, EntityType.OTA_PACKAGE, EntityType.SCHEDULER_EVENT);

        OtaPackage firmwareOta = findOtaPackage(firmware.getTitle());
        OtaPackage softwareOta = findOtaPackage(software.getTitle());

        loginTenant2();
        loadVersion(versionId, EntityType.DEVICE_PROFILE, EntityType.OTA_PACKAGE, EntityType.SCHEDULER_EVENT);
        OtaPackage importedFirmwareOta = findOtaPackage(firmwareOta.getTitle());
        OtaPackage importedSoftwareOta = findOtaPackage(softwareOta.getTitle());
        SchedulerEvent importedFirmwareEvent = findSchedulerEvent(firmwareEvent.getName());
        SchedulerEvent importedSoftwareEvent = findSchedulerEvent(softwareEvent.getName());

        checkImportedEntity(tenantId1, firmwareOta, tenantId2, importedFirmwareOta);
        checkImportedOtaPackageData(firmwareOta, importedFirmwareOta);
        checkImportedEntity(tenantId1, softwareOta, tenantId2, importedSoftwareOta);
        checkImportedOtaPackageData(softwareOta, importedSoftwareOta);

        checkImportedEntity(tenantId1, firmwareEvent, tenantId2, importedFirmwareEvent);
        checkImportedSchedulerEventData(firmwareEvent, importedFirmwareEvent, importedFirmwareOta.getId());
        checkImportedEntity(tenantId1, softwareEvent, tenantId2, importedSoftwareEvent);
        checkImportedSchedulerEventData(softwareEvent, importedSoftwareEvent, importedSoftwareOta.getId());
    }

    @Test
    public void testSchedulerEventOtaConfigForVcWithDeviceGroupOriginator_betweenTenants() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile(null, null, "Device profile v1.0");
        OtaPackage firmware = createOtaPackage(tenantId1, deviceProfile.getId(), OtaPackageType.FIRMWARE);
        OtaPackage software = createOtaPackage(tenantId1, deviceProfile.getId(), OtaPackageType.SOFTWARE);

        EntityGroup deviceGroup = createEntityGroup(tenantId1, EntityType.DEVICE, "Device group for OTA");
        DeviceGroupOtaPackage deviceGroupOtaPackageFirmware = new DeviceGroupOtaPackage();
        deviceGroupOtaPackageFirmware.setGroupId(deviceGroup.getId());
        deviceGroupOtaPackageFirmware.setOtaPackageType(OtaPackageType.FIRMWARE);
        deviceGroupOtaPackageFirmware.setOtaPackageId(firmware.getId());
        doPost("/api/deviceGroupOtaPackage", deviceGroupOtaPackageFirmware, DeviceGroupOtaPackage.class);

        DeviceGroupOtaPackage deviceGroupOtaPackageSoftware = new DeviceGroupOtaPackage();
        deviceGroupOtaPackageSoftware.setGroupId(deviceGroup.getId());
        deviceGroupOtaPackageSoftware.setOtaPackageType(OtaPackageType.SOFTWARE);
        deviceGroupOtaPackageSoftware.setOtaPackageId(software.getId());
        doPost("/api/deviceGroupOtaPackage", deviceGroupOtaPackageSoftware, DeviceGroupOtaPackage.class);

        SchedulerEvent firmwareEvent = createSchedulerEventForOtaPackageType(tenantId1, deviceGroup.getId(), "Firmware", "updateFirmware", firmware.getId());
        SchedulerEvent softwareEvent = createSchedulerEventForOtaPackageType(tenantId1, deviceGroup.getId(), "Software", "updateSoftware", software.getId());
        String versionId = createVersion("scheduler event with ota", EntityType.DEVICE, EntityType.DEVICE_PROFILE, EntityType.OTA_PACKAGE, EntityType.SCHEDULER_EVENT);

        OtaPackage firmwareOta = findOtaPackage(firmware.getTitle());
        OtaPackage softwareOta = findOtaPackage(software.getTitle());

        loginTenant2();
        loadVersion(versionId, EntityType.DEVICE_PROFILE, EntityType.OTA_PACKAGE, EntityType.DEVICE, EntityType.SCHEDULER_EVENT);
        OtaPackage importedFirmwareOta = findOtaPackage(firmwareOta.getTitle());
        OtaPackage importedSoftwareOta = findOtaPackage(softwareOta.getTitle());
        SchedulerEvent importedFirmwareEvent = findSchedulerEvent(firmwareEvent.getName());
        SchedulerEvent importedSoftwareEvent = findSchedulerEvent(softwareEvent.getName());

        checkImportedEntity(tenantId1, firmwareOta, tenantId2, importedFirmwareOta);
        checkImportedOtaPackageData(firmwareOta, importedFirmwareOta);
        checkImportedEntity(tenantId1, softwareOta, tenantId2, importedSoftwareOta);
        checkImportedOtaPackageData(softwareOta, importedSoftwareOta);

        checkImportedEntity(tenantId1, firmwareEvent, tenantId2, importedFirmwareEvent);
        checkImportedSchedulerEventData(firmwareEvent, importedFirmwareEvent, importedFirmwareOta.getId());
        checkImportedEntity(tenantId1, softwareEvent, tenantId2, importedSoftwareEvent);
        checkImportedSchedulerEventData(softwareEvent, importedSoftwareEvent, importedSoftwareOta.getId());

        EntityGroup importedDeviceGroup = findEntityGroup(deviceGroup.getName(), EntityType.DEVICE);
        assertThat(importedFirmwareEvent.getOriginatorId()).isEqualTo(importedDeviceGroup.getId());
        assertThat(importedSoftwareEvent.getOriginatorId()).isEqualTo(importedDeviceGroup.getId());
        assertThat(deviceGroup.getId()).isNotEqualTo(importedDeviceGroup.getId());
    }

    @Test
    public void testSchedulerEventWithoutExistingDeviceGroupOriginator_betweenTenants() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile(null, null, "Device profile v1.0");
        OtaPackage firmware = createOtaPackage(tenantId1, deviceProfile.getId(), OtaPackageType.FIRMWARE);

        EntityGroup deviceGroup = createEntityGroup(tenantId1, EntityType.DEVICE, "Device group for OTA");
        DeviceGroupOtaPackage deviceGroupOtaPackageFirmware = new DeviceGroupOtaPackage();
        deviceGroupOtaPackageFirmware.setGroupId(deviceGroup.getId());
        deviceGroupOtaPackageFirmware.setOtaPackageType(OtaPackageType.FIRMWARE);
        deviceGroupOtaPackageFirmware.setOtaPackageId(firmware.getId());
        doPost("/api/deviceGroupOtaPackage", deviceGroupOtaPackageFirmware, DeviceGroupOtaPackage.class);

        createSchedulerEventForOtaPackageType(tenantId1, deviceGroup.getId(), "Firmware", "updateFirmware", firmware.getId());

        String versionId = createVersion("scheduler event with ota", EntityType.DEVICE_PROFILE, EntityType.OTA_PACKAGE, EntityType.SCHEDULER_EVENT);

        loginTenant2();
        assertThatThrownBy(() -> loadVersion(versionId, EntityType.DEVICE_PROFILE, EntityType.OTA_PACKAGE, EntityType.SCHEDULER_EVENT))
                .isInstanceOf(RuntimeException.class)
                .hasMessageMatching("Failed to load version:.*MissingEntityException.*");
    }

    @Test
    public void testReportTemplateVc_sameTenant() throws Exception {
        Device device = createDevice("Device 1", "test1");
        ReportTemplate reportTemplate = createReportTemplate(tenantId1, null, "Weekly report", device.getId());
        String versionId = createVersion("report template", EntityType.REPORT_TEMPLATE);

        loadVersion(versionId, EntityType.REPORT_TEMPLATE);
        ReportTemplate importedTemplate = findReportTemplate(reportTemplate.getName());
        checkImportedEntity(tenantId1, reportTemplate, tenantId1, importedTemplate);

        assertThat(importedTemplate.getName()).isEqualTo(reportTemplate.getName());
        assertThat(importedTemplate.getType()).isEqualTo(reportTemplate.getType());
        assertThat(importedTemplate.getConfiguration()).isEqualTo(reportTemplate.getConfiguration());
    }

    @Test
    public void testReportTemplateVc_betweenTenants() throws Exception {
        Device device = createDevice("Device 1", "test1");
        ReportTemplate reportTemplate = createReportTemplate(tenantId1, null, "Weekly report", device.getId());
        String versionId = createVersion("report template", EntityType.REPORT_TEMPLATE);

        loginTenant2();
        loadVersion(versionId, EntityType.REPORT_TEMPLATE);
        ReportTemplate importedTemplate = findReportTemplate(reportTemplate.getName());
        checkImportedEntity(tenantId1, reportTemplate, tenantId2, importedTemplate);

        assertThat(importedTemplate.getName()).isEqualTo(reportTemplate.getName());
        assertThat(importedTemplate.getType()).isEqualTo(reportTemplate.getType());
        assertThat(importedTemplate.getConfiguration()).isEqualTo(reportTemplate.getConfiguration());
    }

    @Test
    public void testSchedulerEventGenerateReportForVc_betweenTenants() throws Exception {
        Dashboard dashboard = createDashboard(null, "Test Dashboard");
        SchedulerEvent reportEvent = createSchedulerEventForGenerateReportType(tenantId1, null, "Report", dashboard.getId());
        String versionId = createVersion("scheduler event with report", EntityType.DASHBOARD, EntityType.SCHEDULER_EVENT);

        loginTenant2();
        loadVersion(versionId, EntityType.DASHBOARD, EntityType.SCHEDULER_EVENT);
        Dashboard importedDashboard = findDashboard(dashboard.getTitle());
        SchedulerEvent importedReportEvent = findSchedulerEvent(reportEvent.getName());

        checkImportedEntity(tenantId1, dashboard, tenantId2, importedDashboard);
        checkImportedDashboardData(dashboard, importedDashboard);

        checkImportedEntity(tenantId1, reportEvent, tenantId2, importedReportEvent);
        checkImportedSchedulerEventData(reportEvent, importedReportEvent, importedDashboard.getId(), tenantAdmin2.getId());
    }

    @Test
    public void testSchedulerEventGenerateReportV2ForVc_betweenTenants() throws Exception {
        createDeviceProfile(null, null, "Device profile v1.0");
        Device device = createDevice("Device 1", "test1");
        ReportTemplate reportTemplate = createReportTemplate(tenantId1, null, "Weekly report", device.getId());
        SchedulerEvent reportEvent = createSchedulerEventForGenerateReportType(tenantId1, null, "Report V2", reportTemplate.getId(), tenantAdmin1.getId());
        String versionId = createVersion("scheduler event with report V2", EntityType.DEVICE_PROFILE, EntityType.DEVICE, EntityType.REPORT_TEMPLATE, EntityType.SCHEDULER_EVENT);

        loginTenant2();
        loadVersion(versionId, config -> {
            config.setLoadCredentials(false);
        }, EntityType.DEVICE_PROFILE, EntityType.DEVICE, EntityType.REPORT_TEMPLATE, EntityType.SCHEDULER_EVENT);
        ReportTemplate importedReportTemplate = findReportTemplate(reportTemplate.getName());

        SchedulerEvent importedReportEvent = findSchedulerEvent(reportEvent.getName());

        checkImportedEntity(tenantId1, reportTemplate, tenantId2, importedReportTemplate);
        checkImportedReportTemplateData(importedReportTemplate, importedReportTemplate);

        checkImportedEntity(tenantId1, reportEvent, tenantId2, importedReportEvent);
        checkImportedSchedulerEventData(reportEvent, importedReportEvent, importedReportTemplate.getId(), tenantAdmin2.getId());
    }

    // --- VC-specific permission tests ---

    @Test
    public void testSaveEntitiesVersion_deniedOnOwnerCustomer() throws Exception {
        // Regression test for the owner-walk leak: exporting a device whose owner is a Customer
        // must fail if the caller lacks CUSTOMER READ. Single-entity export ctx exports related
        // customers (SimpleEntitiesExportCtx -> exportRelatedCustomers=true), so the walk fires.
        Customer customer = createCustomer("Owner Walk Customer");
        DeviceProfile deviceProfile = createDeviceProfile(null, null, "Owner Walk DP");
        Device device = createDevice(deviceProfile.getId(), "Owner Walk Device", "owner-walk-token");
        doPost("/api/owner/CUSTOMER/" + customer.getId().getId() + "/DEVICE/" + device.getId().getId())
                .andExpect(status().isOk());

        loginAsRestrictedTenantAdmin(tenantId1,
                Map.of(Resource.VERSION_CONTROL, List.of(Operation.READ, Operation.WRITE),
                        Resource.DEVICE_PROFILE, List.of(Operation.READ),
                        Resource.DEVICE, List.of(Operation.READ))); // no CUSTOMER READ

        SingleEntityVersionCreateRequest request = new SingleEntityVersionCreateRequest();
        request.setVersionName("owner walk version");
        request.setBranch(branch);
        request.setEntityId(device.getId());
        VersionCreateConfig config = new VersionCreateConfig();
        config.setSaveRelations(true);
        config.setSaveAttributes(true);
        config.setSaveCredentials(true);
        config.setSavePermissions(true);
        request.setConfig(config);

        VersionCreationResult result = pollVersionCreate(request);
        assertThat(result.getError()).as("Owner-walk should be blocked by missing CUSTOMER READ").isNotNull();
        assertThat(result.getVersion()).isNull();
        assertThat(listVersions()).extracting(EntityVersion::getName).doesNotContain("owner walk version");
    }

    @Test
    public void testSaveEntitiesVersion_complexRequest_deniedPartial() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile(null, null, "Complex Deny DP");
        createDevice(deviceProfile.getId(), "Complex Deny Device", "complex-deny-token");
        createDashboard(null, "Complex Deny Dash");

        // no DASHBOARD READ — the dashboard sweep must fail.
        loginAsRestrictedTenantAdmin(tenantId1,
                Map.of(Resource.VERSION_CONTROL, List.of(Operation.READ, Operation.WRITE),
                        Resource.DEVICE_PROFILE, List.of(Operation.READ, Operation.CREATE, Operation.WRITE),
                        Resource.DEVICE, List.of(Operation.READ, Operation.CREATE, Operation.WRITE)));

        ComplexVersionCreateRequest request = buildComplexCreateRequest("complex partial deny",
                EntityType.DEVICE, EntityType.DEVICE_PROFILE, EntityType.DASHBOARD);
        VersionCreationResult result = pollVersionCreate(request);
        assertThat(result.getError()).as("Complex sweep must fail when one type is denied").isNotNull();
        assertThat(result.getVersion()).isNull();
        assertThat(listVersions()).extracting(EntityVersion::getName).doesNotContain("complex partial deny");
    }

    @Test
    public void testLoadVersion_reimportPass_stillEnforcesPermissions() throws Exception {
        // Narrowed scope: instead of replicating the circular-reference reimport setup verbatim,
        // we exercise the second-pass (UPDATE) by pre-creating both target entities on tenant2
        // with matching externalIds. The restricted admin holds READ+CREATE but no WRITE — the
        // import-side WRITE check fires for both types.
        RuleChain rcA = createRuleChain("Reimport RC A");
        DeviceProfile deviceProfile = createDeviceProfile(null, null, "Reimport DP");
        String versionId = createVersion("reimport version", EntityType.RULE_CHAIN, EntityType.DEVICE_PROFILE);

        loginTenant2();
        // Pre-create matching entities with same externalId so import enters WRITE branch.
        RuleChain t2Rc = new RuleChain();
        t2Rc.setName("Reimport RC A");
        t2Rc.setType(RuleChainType.CORE);
        t2Rc.setExternalId(rcA.getId());
        doPost("/api/ruleChain", t2Rc, RuleChain.class);

        DeviceProfile t2Dp = new DeviceProfile();
        t2Dp.setName("Reimport DP");
        t2Dp.setType(DeviceProfileType.DEFAULT);
        t2Dp.setTransportType(DeviceTransportType.DEFAULT);
        DeviceProfileData profileData = new DeviceProfileData();
        profileData.setConfiguration(new DefaultDeviceProfileConfiguration());
        profileData.setTransportConfiguration(new DefaultDeviceProfileTransportConfiguration());
        t2Dp.setProfileData(profileData);
        t2Dp.setExternalId(deviceProfile.getId());
        doPost("/api/deviceProfile", t2Dp, DeviceProfile.class);

        loginAsRestrictedTenantAdmin(tenantId2,
                Map.of(Resource.VERSION_CONTROL, List.of(Operation.READ, Operation.WRITE),
                        Resource.RULE_CHAIN, List.of(Operation.READ, Operation.CREATE),
                        Resource.DEVICE_PROFILE, List.of(Operation.READ, Operation.CREATE))); // no WRITE

        VersionLoadResult result = pollVersionLoad(versionId, EntityType.RULE_CHAIN, EntityType.DEVICE_PROFILE);
        assertThat(result.getError()).as("WRITE denial during reimport pass must surface as error").isNotNull();
    }

    @Test
    public void testLoadVersion_rollbackPreservesPriorState() throws Exception {
        createRuleChain("VC Rollback RC");
        createDashboard(null, "VC Rollback Dash");
        DeviceProfile deviceProfile = createDeviceProfile(null, null, "VC Rollback DP");
        Device device = createDevice(deviceProfile.getId(), "VC Rollback Device", "rollback-token");
        String versionId = createVersion("vc rollback version",
                EntityType.RULE_CHAIN, EntityType.DASHBOARD, EntityType.DEVICE_PROFILE, EntityType.DEVICE);

        loginTenant2();
        // Pre-create a DeviceProfile on tenant2 with same externalId so it would have been the
        // matched WRITE target — its content should be unchanged after the rollback.
        DeviceProfile priorDp = new DeviceProfile();
        priorDp.setName("VC Rollback DP");
        priorDp.setType(DeviceProfileType.DEFAULT);
        priorDp.setTransportType(DeviceTransportType.DEFAULT);
        priorDp.setDescription("prior description");
        DeviceProfileData priorProfileData = new DeviceProfileData();
        priorProfileData.setConfiguration(new DefaultDeviceProfileConfiguration());
        priorProfileData.setTransportConfiguration(new DefaultDeviceProfileTransportConfiguration());
        priorDp.setProfileData(priorProfileData);
        priorDp.setExternalId(deviceProfile.getId());
        DeviceProfile savedPriorDp = doPost("/api/deviceProfile", priorDp, DeviceProfile.class);
        UUID priorDpId = savedPriorDp.getUuidId();

        // Pre-create the device on tenant2 with same externalId so import enters the WRITE branch
        // (the restricted admin has CREATE but no WRITE — WRITE denial is what we want to surface).
        Device t2Device = new Device();
        t2Device.setName("VC Rollback Device");
        t2Device.setType("default");
        t2Device.setExternalId(device.getId());
        doPost("/api/device", t2Device, Device.class);

        loginAsRestrictedTenantAdmin(tenantId2,
                Map.of(Resource.VERSION_CONTROL, List.of(Operation.READ, Operation.WRITE),
                        Resource.RULE_CHAIN, List.of(Operation.READ, Operation.CREATE, Operation.WRITE),
                        Resource.DASHBOARD, List.of(Operation.READ, Operation.CREATE, Operation.WRITE),
                        Resource.DEVICE_PROFILE, List.of(Operation.READ, Operation.CREATE, Operation.WRITE),
                        Resource.DEVICE, List.of(Operation.READ, Operation.CREATE))); // no DEVICE WRITE

        VersionLoadResult result = pollVersionLoad(versionId,
                EntityType.RULE_CHAIN, EntityType.DASHBOARD, EntityType.DEVICE_PROFILE, EntityType.DEVICE);
        assertThat(result.getError()).as("Mid-flight DEVICE WRITE denial must surface as error").isNotNull();

        loginTenant2();
        // After rollback, RuleChain and Dashboard must not have been persisted.
        assertThat(doGetTypedWithPageLink("/api/ruleChains?",
                new TypeReference<PageData<RuleChain>>() {}, new PageLink(100, 0, "VC Rollback RC")).getData())
                .as("RuleChain must not be persisted after rollback").isEmpty();
        assertThat(doGetTypedWithPageLink("/api/tenant/dashboards?",
                new TypeReference<PageData<DashboardInfo>>() {}, new PageLink(100, 0, "VC Rollback Dash")).getData())
                .as("Dashboard must not be persisted after rollback").isEmpty();
        // Pre-existing DeviceProfile must still exist with its prior id and prior description.
        DeviceProfile stillThere = doGetTypedWithPageLink("/api/deviceProfiles?",
                new TypeReference<PageData<DeviceProfile>>() {}, new PageLink(100, 0, "VC Rollback DP")).getData().get(0);
        assertThat(stillThere.getUuidId()).isEqualTo(priorDpId);
        assertThat(stillThere.getDescription()).isEqualTo("prior description");
    }

    // --- Per-type permission tests: DEVICE ---

    @Test
    public void testSaveEntitiesVersion_device_deniedWithoutReadPermission() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile(null, null, "Dev VC Read Deny DP");
        Device device = createDevice(deviceProfile.getId(), "Dev VC Read Deny", "dev-read-deny-token");

        loginAsRestrictedTenantAdmin(tenantId1,
                Map.of(Resource.VERSION_CONTROL, List.of(Operation.READ, Operation.WRITE),
                        Resource.DEVICE_PROFILE, List.of(Operation.READ))); // no DEVICE READ

        VersionCreationResult result = pollVersionCreate(buildComplexCreateRequestForIds("dev vc read deny", device.getId()));
        assertThat(result.getError()).isNotNull();
        assertThat(result.getVersion()).isNull();
        assertThat(listVersions()).extracting(EntityVersion::getName).doesNotContain("dev vc read deny");
    }

    @Test
    public void testLoadVersion_device_deniedWithoutCreatePermission() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile(null, null, "Dev VC Create Deny DP");
        createDevice(deviceProfile.getId(), "Dev VC Create Deny", "dev-create-deny-token");
        String versionId = createVersion("dev vc create deny", EntityType.DEVICE_PROFILE, EntityType.DEVICE);

        loginTenant2();
        loginAsRestrictedTenantAdmin(tenantId2,
                Map.of(Resource.VERSION_CONTROL, List.of(Operation.READ, Operation.WRITE),
                        Resource.DEVICE_PROFILE, List.of(Operation.READ, Operation.CREATE, Operation.WRITE),
                        Resource.DEVICE, List.of(Operation.READ))); // no CREATE

        VersionLoadResult result = pollVersionLoad(versionId, EntityType.DEVICE_PROFILE, EntityType.DEVICE);
        assertThat(result.getError()).isNotNull();

        loginTenant2();
        assertThat(doGetTypedWithPageLink("/api/tenant/devices?",
                new TypeReference<PageData<Device>>() {}, new PageLink(100, 0, "Dev VC Create Deny")).getData())
                .as("Device must not be persisted on permission denial").isEmpty();
    }

    @Test
    public void testLoadVersion_device_deniedWithoutWritePermission() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile(null, null, "Dev VC Write Deny DP");
        Device device = createDevice(deviceProfile.getId(), "Dev VC Write Deny", "dev-write-deny-token");
        String versionId = createVersion("dev vc write deny", EntityType.DEVICE_PROFILE, EntityType.DEVICE);

        loginTenant2();
        Device t2Device = new Device();
        t2Device.setName("Dev VC Write Deny");
        t2Device.setType("default");
        t2Device.setExternalId(device.getId());
        Device savedT2Device = doPost("/api/device", t2Device, Device.class);
        UUID priorId = savedT2Device.getUuidId();

        loginAsRestrictedTenantAdmin(tenantId2,
                Map.of(Resource.VERSION_CONTROL, List.of(Operation.READ, Operation.WRITE),
                        Resource.DEVICE_PROFILE, List.of(Operation.READ, Operation.CREATE, Operation.WRITE),
                        Resource.DEVICE, List.of(Operation.READ, Operation.CREATE))); // no WRITE

        VersionLoadResult result = pollVersionLoad(versionId, EntityType.DEVICE_PROFILE, EntityType.DEVICE);
        assertThat(result.getError()).isNotNull();

        loginTenant2();
        Device persisted = doGetTypedWithPageLink("/api/tenant/devices?",
                new TypeReference<PageData<Device>>() {}, new PageLink(100, 0, "Dev VC Write Deny")).getData().get(0);
        assertThat(persisted.getUuidId()).isEqualTo(priorId);
        assertThat(persisted.getExternalId()).isEqualTo(device.getId());
    }

    @Test
    public void testSaveAndLoadVersion_device_withPermissions() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile(null, null, "Dev VC Allow DP");
        Device device = createDevice(deviceProfile.getId(), "Dev VC Allow", "dev-allow-token");
        String versionId = createVersion("dev vc allow", EntityType.DEVICE_PROFILE, EntityType.DEVICE);

        loginTenant2();
        // DEVICE_GROUP needed because VC import re-syncs the auto-managed "All" entity group's
        // configuration/additionalInfo (UI columns, isPublic) — group-level WRITE.
        loginAsRestrictedTenantAdmin(tenantId2,
                Set.of(),
                Set.of(Resource.VERSION_CONTROL, Resource.DEVICE_PROFILE, Resource.DEVICE, Resource.DEVICE_GROUP));

        // Credentials are tenant-globally unique by access token; skip credentials propagation here
        // (the test verifies permission flow, not credentials).
        Map<EntityType, EntityTypeLoadResult> loadResult = loadVersion(versionId,
                config -> config.setLoadCredentials(false),
                EntityType.DEVICE_PROFILE, EntityType.DEVICE);
        assertThat(loadResult.get(EntityType.DEVICE).getCreated()).isEqualTo(1);

        Device imported = findDevice(device.getName());
        assertThat(imported.getTenantId()).isEqualTo(tenantId2);
        assertThat(imported.getExternalId()).isEqualTo(device.getId());
    }

    // --- Per-type permission tests: ASSET_PROFILE ---
    // (USER is intentionally excluded from VC scope — see testUserEntitiesExcludedFromVc_betweenTenants.
    //  Permission coverage uses a tenant-level type that actually flows through VC.)

    @Test
    public void testSaveEntitiesVersion_assetProfile_deniedWithoutReadPermission() throws Exception {
        AssetProfile assetProfile = createAssetProfile(null, null, "AP VC Read Deny");

        loginAsRestrictedTenantAdmin(tenantId1,
                Map.of(Resource.VERSION_CONTROL, List.of(Operation.READ, Operation.WRITE))); // no ASSET_PROFILE READ

        VersionCreationResult result = pollVersionCreate(buildComplexCreateRequestForIds("ap vc read deny", assetProfile.getId()));
        assertThat(result.getError()).isNotNull();
        assertThat(result.getVersion()).isNull();
        assertThat(listVersions()).extracting(EntityVersion::getName).doesNotContain("ap vc read deny");
    }

    @Test
    public void testLoadVersion_assetProfile_deniedWithoutCreatePermission() throws Exception {
        createAssetProfile(null, null, "AP VC Create Deny");
        String versionId = createVersion("ap vc create deny", EntityType.ASSET_PROFILE);

        loginTenant2();
        loginAsRestrictedTenantAdmin(tenantId2,
                Map.of(Resource.VERSION_CONTROL, List.of(Operation.READ, Operation.WRITE),
                        Resource.ASSET_PROFILE, List.of(Operation.READ))); // no CREATE

        VersionLoadResult result = pollVersionLoad(versionId, EntityType.ASSET_PROFILE);
        assertThat(result.getError()).isNotNull();

        loginTenant2();
        assertThat(doGetTypedWithPageLink("/api/assetProfiles?",
                new TypeReference<PageData<AssetProfile>>() {}, new PageLink(100, 0, "AP VC Create Deny")).getData())
                .as("AssetProfile must not be persisted on permission denial").isEmpty();
    }

    @Test
    public void testLoadVersion_assetProfile_deniedWithoutWritePermission() throws Exception {
        AssetProfile assetProfile = createAssetProfile(null, null, "AP VC Write Deny");
        String versionId = createVersion("ap vc write deny", EntityType.ASSET_PROFILE);

        loginTenant2();
        AssetProfile t2AssetProfile = new AssetProfile();
        t2AssetProfile.setName("AP VC Write Deny");
        t2AssetProfile.setExternalId(assetProfile.getId());
        AssetProfile savedT2AssetProfile = doPost("/api/assetProfile", t2AssetProfile, AssetProfile.class);
        UUID priorId = savedT2AssetProfile.getUuidId();

        loginAsRestrictedTenantAdmin(tenantId2,
                Map.of(Resource.VERSION_CONTROL, List.of(Operation.READ, Operation.WRITE),
                        Resource.ASSET_PROFILE, List.of(Operation.READ, Operation.CREATE))); // no WRITE

        VersionLoadResult result = pollVersionLoad(versionId, EntityType.ASSET_PROFILE);
        assertThat(result.getError()).isNotNull();

        loginTenant2();
        AssetProfile persisted = findAssetProfile("AP VC Write Deny");
        assertThat(persisted.getUuidId()).isEqualTo(priorId);
        assertThat(persisted.getExternalId()).isEqualTo(assetProfile.getId());
    }

    @Test
    public void testSaveAndLoadVersion_assetProfile_withPermissions() throws Exception {
        AssetProfile assetProfile = createAssetProfile(null, null, "AP VC Allow");
        String versionId = createVersion("ap vc allow", EntityType.ASSET_PROFILE);

        loginTenant2();
        loginAsRestrictedTenantAdmin(tenantId2,
                Set.of(),
                Set.of(Resource.VERSION_CONTROL, Resource.ASSET_PROFILE));

        Map<EntityType, EntityTypeLoadResult> loadResult = loadVersion(versionId, EntityType.ASSET_PROFILE);
        assertThat(loadResult.get(EntityType.ASSET_PROFILE).getCreated()).isEqualTo(1);

        AssetProfile imported = findAssetProfile(assetProfile.getName());
        assertThat(imported.getTenantId()).isEqualTo(tenantId2);
        assertThat(imported.getExternalId()).isEqualTo(assetProfile.getId());
    }

    // --- Per-type permission tests: RULE_CHAIN ---

    @Test
    public void testSaveEntitiesVersion_ruleChain_deniedWithoutReadPermission() throws Exception {
        RuleChain rc = createRuleChain("RC VC Read Deny");

        loginAsRestrictedTenantAdmin(tenantId1,
                Map.of(Resource.VERSION_CONTROL, List.of(Operation.READ, Operation.WRITE))); // no RULE_CHAIN READ

        VersionCreationResult result = pollVersionCreate(buildComplexCreateRequestForIds("rc vc read deny", rc.getId()));
        assertThat(result.getError()).isNotNull();
        assertThat(result.getVersion()).isNull();
        assertThat(listVersions()).extracting(EntityVersion::getName).doesNotContain("rc vc read deny");
    }

    @Test
    public void testLoadVersion_ruleChain_deniedWithoutCreatePermission() throws Exception {
        createRuleChain("RC VC Create Deny");
        String versionId = createVersion("rc vc create deny", EntityType.RULE_CHAIN);

        loginTenant2();
        loginAsRestrictedTenantAdmin(tenantId2,
                Map.of(Resource.VERSION_CONTROL, List.of(Operation.READ, Operation.WRITE),
                        Resource.RULE_CHAIN, List.of(Operation.READ))); // no CREATE

        VersionLoadResult result = pollVersionLoad(versionId, EntityType.RULE_CHAIN);
        assertThat(result.getError()).isNotNull();

        loginTenant2();
        assertThat(doGetTypedWithPageLink("/api/ruleChains?",
                new TypeReference<PageData<RuleChain>>() {}, new PageLink(100, 0, "RC VC Create Deny")).getData())
                .as("RuleChain must not be persisted on permission denial").isEmpty();
    }

    @Test
    public void testLoadVersion_ruleChain_deniedWithoutWritePermission() throws Exception {
        RuleChain rc = createRuleChain("RC VC Write Deny");
        String versionId = createVersion("rc vc write deny", EntityType.RULE_CHAIN);

        loginTenant2();
        RuleChain t2Rc = new RuleChain();
        t2Rc.setName("RC VC Write Deny");
        t2Rc.setType(RuleChainType.CORE);
        t2Rc.setExternalId(rc.getId());
        RuleChain savedT2Rc = doPost("/api/ruleChain", t2Rc, RuleChain.class);
        UUID priorId = savedT2Rc.getUuidId();

        loginAsRestrictedTenantAdmin(tenantId2,
                Map.of(Resource.VERSION_CONTROL, List.of(Operation.READ, Operation.WRITE),
                        Resource.RULE_CHAIN, List.of(Operation.READ, Operation.CREATE))); // no WRITE

        VersionLoadResult result = pollVersionLoad(versionId, EntityType.RULE_CHAIN);
        assertThat(result.getError()).isNotNull();

        loginTenant2();
        RuleChain persisted = doGetTypedWithPageLink("/api/ruleChains?",
                new TypeReference<PageData<RuleChain>>() {}, new PageLink(100, 0, "RC VC Write Deny")).getData().get(0);
        assertThat(persisted.getUuidId()).isEqualTo(priorId);
        assertThat(persisted.getExternalId()).isEqualTo(rc.getId());
    }

    @Test
    public void testSaveAndLoadVersion_ruleChain_withPermissions() throws Exception {
        RuleChain rc = createRuleChain("RC VC Allow");
        String versionId = createVersion("rc vc allow", EntityType.RULE_CHAIN);

        loginTenant2();
        loginAsRestrictedTenantAdmin(tenantId2,
                Set.of(),
                Set.of(Resource.VERSION_CONTROL, Resource.RULE_CHAIN));

        Map<EntityType, EntityTypeLoadResult> loadResult = loadVersion(versionId, EntityType.RULE_CHAIN);
        assertThat(loadResult.get(EntityType.RULE_CHAIN).getCreated()).isEqualTo(1);

        RuleChain imported = findRuleChain(rc.getName());
        assertThat(imported.getTenantId()).isEqualTo(tenantId2);
        assertThat(imported.getExternalId()).isEqualTo(rc.getId());
    }

    // --- Per-type permission tests: INTEGRATION ---

    @Test
    public void testSaveEntitiesVersion_integration_deniedWithoutReadPermission() throws Exception {
        Converter converter = createConverter(ConverterType.DOWNLINK, "Int VC Read Deny Converter");
        Integration integration = createIntegration(converter.getId(), IntegrationType.HTTP, "Int VC Read Deny");

        loginAsRestrictedTenantAdmin(tenantId1,
                Map.of(Resource.VERSION_CONTROL, List.of(Operation.READ, Operation.WRITE),
                        Resource.CONVERTER, List.of(Operation.READ))); // no INTEGRATION READ

        VersionCreationResult result = pollVersionCreate(buildComplexCreateRequestForIds("int vc read deny",
                converter.getId(), integration.getId()));
        assertThat(result.getError()).isNotNull();
        assertThat(result.getVersion()).isNull();
        assertThat(listVersions()).extracting(EntityVersion::getName).doesNotContain("int vc read deny");
    }

    @Test
    public void testLoadVersion_integration_deniedWithoutCreatePermission() throws Exception {
        Converter converter = createConverter(ConverterType.DOWNLINK, "Int VC Create Deny Converter");
        createIntegration(converter.getId(), IntegrationType.HTTP, "Int VC Create Deny");
        String versionId = createVersion("int vc create deny", EntityType.CONVERTER, EntityType.INTEGRATION);

        loginTenant2();
        loginAsRestrictedTenantAdmin(tenantId2,
                Map.of(Resource.VERSION_CONTROL, List.of(Operation.READ, Operation.WRITE),
                        Resource.CONVERTER, List.of(Operation.READ, Operation.CREATE, Operation.WRITE),
                        Resource.INTEGRATION, List.of(Operation.READ))); // no CREATE

        VersionLoadResult result = pollVersionLoad(versionId, EntityType.CONVERTER, EntityType.INTEGRATION);
        assertThat(result.getError()).isNotNull();

        loginTenant2();
        assertThat(doGetTypedWithPageLink("/api/integrations?",
                new TypeReference<PageData<Integration>>() {}, new PageLink(100, 0, "Int VC Create Deny")).getData())
                .as("Integration must not be persisted on permission denial").isEmpty();
    }

    @Test
    public void testLoadVersion_integration_deniedWithoutWritePermission() throws Exception {
        Converter converter = createConverter(ConverterType.DOWNLINK, "Int VC Write Deny Converter");
        Integration integration = createIntegration(converter.getId(), IntegrationType.HTTP, "Int VC Write Deny");
        String versionId = createVersion("int vc write deny", EntityType.CONVERTER, EntityType.INTEGRATION);

        loginTenant2();
        Converter t2Converter = createConverter(ConverterType.DOWNLINK, "Int VC Write Deny T2 Converter");
        Integration t2Integration = new Integration();
        t2Integration.setName("Int VC Write Deny");
        t2Integration.setType(IntegrationType.HTTP);
        t2Integration.setDefaultConverterId(t2Converter.getId());
        t2Integration.setRoutingKey("t2-vc-write-deny-rk");
        t2Integration.setSecret("scrt");
        t2Integration.setEnabled(false);
        t2Integration.setConfiguration(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        t2Integration.setAdditionalInfo(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        t2Integration.setExternalId(integration.getId());
        Integration savedT2Integration = doPost("/api/integration", t2Integration, Integration.class);
        UUID priorId = savedT2Integration.getUuidId();

        loginAsRestrictedTenantAdmin(tenantId2,
                Map.of(Resource.VERSION_CONTROL, List.of(Operation.READ, Operation.WRITE),
                        Resource.CONVERTER, List.of(Operation.READ, Operation.CREATE, Operation.WRITE),
                        Resource.INTEGRATION, List.of(Operation.READ, Operation.CREATE))); // no WRITE

        VersionLoadResult result = pollVersionLoad(versionId, EntityType.CONVERTER, EntityType.INTEGRATION);
        assertThat(result.getError()).isNotNull();

        loginTenant2();
        Integration persisted = findIntegration("Int VC Write Deny");
        assertThat(persisted.getUuidId()).isEqualTo(priorId);
        assertThat(persisted.getExternalId()).isEqualTo(integration.getId());
    }

    @Test
    public void testSaveAndLoadVersion_integration_withPermissions() throws Exception {
        Converter converter = createConverter(ConverterType.DOWNLINK, "Int VC Allow Converter");
        Integration integration = createIntegration(converter.getId(), IntegrationType.HTTP, "Int VC Allow");
        String versionId = createVersion("int vc allow", EntityType.CONVERTER, EntityType.INTEGRATION);

        loginTenant2();
        loginAsRestrictedTenantAdmin(tenantId2,
                Set.of(),
                Set.of(Resource.VERSION_CONTROL, Resource.CONVERTER, Resource.INTEGRATION));

        Map<EntityType, EntityTypeLoadResult> loadResult = loadVersion(versionId, config -> {
            config.setAutoGenerateIntegrationKey(true);
        }, EntityType.CONVERTER, EntityType.INTEGRATION);
        assertThat(loadResult.get(EntityType.INTEGRATION).getCreated()).isEqualTo(1);

        Integration imported = findIntegration(integration.getName());
        assertThat(imported.getTenantId()).isEqualTo(tenantId2);
        assertThat(imported.getExternalId()).isEqualTo(integration.getId());
    }

    // --- Per-type permission tests: DASHBOARD ---

    @Test
    public void testSaveEntitiesVersion_dashboard_deniedWithoutReadPermission() throws Exception {
        Dashboard dashboard = createDashboard(null, "Dash VC Read Deny");

        loginAsRestrictedTenantAdmin(tenantId1,
                Map.of(Resource.VERSION_CONTROL, List.of(Operation.READ, Operation.WRITE))); // no DASHBOARD READ

        VersionCreationResult result = pollVersionCreate(buildComplexCreateRequestForIds("dash vc read deny", dashboard.getId()));
        assertThat(result.getError()).isNotNull();
        assertThat(result.getVersion()).isNull();
        assertThat(listVersions()).extracting(EntityVersion::getName).doesNotContain("dash vc read deny");
    }

    @Test
    public void testLoadVersion_dashboard_deniedWithoutCreatePermission() throws Exception {
        createDashboard(null, "Dash VC Create Deny");
        String versionId = createVersion("dash vc create deny", EntityType.DASHBOARD);

        loginTenant2();
        loginAsRestrictedTenantAdmin(tenantId2,
                Map.of(Resource.VERSION_CONTROL, List.of(Operation.READ, Operation.WRITE),
                        Resource.DASHBOARD, List.of(Operation.READ))); // no CREATE

        VersionLoadResult result = pollVersionLoad(versionId, EntityType.DASHBOARD);
        assertThat(result.getError()).isNotNull();

        loginTenant2();
        assertThat(doGetTypedWithPageLink("/api/tenant/dashboards?",
                new TypeReference<PageData<DashboardInfo>>() {}, new PageLink(100, 0, "Dash VC Create Deny")).getData())
                .as("Dashboard must not be persisted on permission denial").isEmpty();
    }

    @Test
    public void testLoadVersion_dashboard_deniedWithoutWritePermission() throws Exception {
        Dashboard dashboard = createDashboard(null, "Dash VC Write Deny");
        String versionId = createVersion("dash vc write deny", EntityType.DASHBOARD);

        loginTenant2();
        Dashboard t2Dashboard = new Dashboard();
        t2Dashboard.setTitle("Dash VC Write Deny");
        t2Dashboard.setExternalId(dashboard.getId());
        Dashboard savedT2Dashboard = doPost("/api/dashboard", t2Dashboard, Dashboard.class);
        UUID priorId = savedT2Dashboard.getUuidId();

        loginAsRestrictedTenantAdmin(tenantId2,
                Map.of(Resource.VERSION_CONTROL, List.of(Operation.READ, Operation.WRITE),
                        Resource.DASHBOARD, List.of(Operation.READ, Operation.CREATE))); // no WRITE

        VersionLoadResult result = pollVersionLoad(versionId, EntityType.DASHBOARD);
        assertThat(result.getError()).isNotNull();

        loginTenant2();
        Dashboard persisted = findDashboard("Dash VC Write Deny");
        assertThat(persisted.getUuidId()).isEqualTo(priorId);
        assertThat(persisted.getExternalId()).isEqualTo(dashboard.getId());
    }

    @Test
    public void testSaveAndLoadVersion_dashboard_withPermissions() throws Exception {
        Dashboard dashboard = createDashboard(null, "Dash VC Allow");
        String versionId = createVersion("dash vc allow", EntityType.DASHBOARD);

        loginTenant2();
        // DASHBOARD_GROUP needed because VC import re-syncs the auto-managed "All" entity group's
        // configuration/additionalInfo (UI columns, sort, isPublic) — that's a group-level WRITE.
        loginAsRestrictedTenantAdmin(tenantId2,
                Set.of(),
                Set.of(Resource.VERSION_CONTROL, Resource.DASHBOARD, Resource.DASHBOARD_GROUP));

        Map<EntityType, EntityTypeLoadResult> loadResult = loadVersion(versionId, EntityType.DASHBOARD);
        assertThat(loadResult.get(EntityType.DASHBOARD).getCreated()).isEqualTo(1);

        Dashboard imported = findDashboard(dashboard.getTitle());
        assertThat(imported.getTenantId()).isEqualTo(tenantId2);
        assertThat(imported.getExternalId()).isEqualTo(dashboard.getId());
    }

    // --- Permission-test helpers (inline-poll variants that surface getError() instead of throwing) ---

    private VersionCreationResult pollVersionCreate(VersionCreateRequest request) throws Exception {
        UUID requestId = doPostAsync("/api/entities/vc/version", request, UUID.class, status().isOk());
        return await().atMost(30, TimeUnit.SECONDS)
                .until(() -> doGet("/api/entities/vc/version/" + requestId + "/status", VersionCreationResult.class),
                        VersionCreationResult::isDone);
    }

    private VersionLoadResult pollVersionLoad(String versionId, EntityType... entityTypes) throws Exception {
        EntityTypeVersionLoadRequest request = new EntityTypeVersionLoadRequest();
        request.setVersionId(versionId);
        request.setRollbackOnError(true);
        request.setEntityTypes(Arrays.stream(entityTypes).collect(Collectors.toMap(t -> t, entityType -> {
            EntityTypeVersionLoadConfig config = new EntityTypeVersionLoadConfig();
            config.setLoadAttributes(true);
            config.setLoadRelations(true);
            config.setLoadCredentials(true);
            config.setLoadCalculatedFields(true);
            config.setLoadPermissions(true);
            config.setLoadGroupEntities(true);
            config.setRemoveOtherEntities(false);
            config.setFindExistingEntityByName(true);
            return config;
        })));
        UUID requestId = doPost("/api/entities/vc/entity", request, UUID.class);
        return await().atMost(60, TimeUnit.SECONDS)
                .until(() -> doGet("/api/entities/vc/entity/" + requestId + "/status", VersionLoadResult.class),
                        VersionLoadResult::isDone);
    }

    private ComplexVersionCreateRequest buildComplexCreateRequest(String name, EntityType... entityTypes) {
        ComplexVersionCreateRequest request = new ComplexVersionCreateRequest();
        request.setVersionName(name);
        request.setBranch(branch);
        request.setSyncStrategy(SyncStrategy.MERGE);
        request.setEntityTypes(Arrays.stream(entityTypes).collect(Collectors.toMap(t -> t, entityType -> {
            EntityTypeVersionCreateConfig config = new EntityTypeVersionCreateConfig();
            config.setAllEntities(true);
            config.setSaveGroupEntities(true);
            config.setSaveRelations(true);
            config.setSaveAttributes(true);
            config.setSaveCredentials(true);
            config.setSaveCalculatedFields(true);
            config.setSavePermissions(true);
            return config;
        })));
        return request;
    }

    private ComplexVersionCreateRequest buildComplexCreateRequestForIds(String name, EntityId... entityIds) {
        ComplexVersionCreateRequest request = new ComplexVersionCreateRequest();
        request.setVersionName(name);
        request.setBranch(branch);
        request.setSyncStrategy(SyncStrategy.MERGE);
        request.setEntityTypes(new HashMap<>());
        Map<EntityType, List<EntityId>> entitiesByType = Arrays.stream(entityIds)
                .collect(Collectors.groupingBy(EntityId::getEntityType));
        entitiesByType.forEach((entityType, ids) -> {
            EntityTypeVersionCreateConfig config = new EntityTypeVersionCreateConfig();
            config.setAllEntities(false);
            config.setEntityIds(ids.stream().map(EntityId::getId).toList());
            config.setSaveRelations(true);
            config.setSaveAttributes(true);
            config.setSaveCredentials(true);
            config.setSavePermissions(true);
            config.setSaveGroupEntities(true);
            request.getEntityTypes().put(entityType, config);
        });
        return request;
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

    protected <E extends ExportableEntity<?> & HasOwnerId> void checkImportedEntity(TenantId tenantId1, EntityId ownerId1, E initialEntity,
                                                                                    TenantId tenantId2, EntityId ownerId2, E importedEntity) {
        if (initialEntity instanceof HasTenantId) {
            assertThat(((HasTenantId) initialEntity).getTenantId()).isEqualTo(tenantId1);
            assertThat(((HasTenantId) importedEntity).getTenantId()).isEqualTo(tenantId2);
        }
        assertThat(initialEntity.getOwnerId()).isEqualTo(ownerId1);
        assertThat(importedEntity.getOwnerId()).isEqualTo(ownerId2);

        assertThat(importedEntity.getExternalId()).isEqualTo(initialEntity.getId());

        boolean sameTenant = tenantId1.equals(tenantId2);
        if (!sameTenant) {
            assertThat(importedEntity.getId()).isNotEqualTo(initialEntity.getId());
            assertThat(importedEntity.getOwnerId()).isNotEqualTo(initialEntity.getOwnerId());
        } else {
            assertThat(importedEntity.getId()).isEqualTo(initialEntity.getId());
            assertThat(importedEntity.getOwnerId()).isEqualTo(initialEntity.getOwnerId());
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

    protected void checkImportedReportTemplateData(ReportTemplate initialTemplate, ReportTemplate importedTemplate) {
        assertThat(importedTemplate.getName()).isEqualTo(initialTemplate.getName());
        assertThat(importedTemplate.getConfiguration()).isEqualTo(initialTemplate.getConfiguration());
    }

    protected void checkImportedEntityGroupData(EntityGroup initialEntityGroup, EntityGroup importedEntityGroup) {
        assertThat(importedEntityGroup.getType()).isEqualTo(initialEntityGroup.getType());
        assertThat(importedEntityGroup.getName()).isEqualTo(initialEntityGroup.getName());
        assertThat(importedEntityGroup.getConfiguration()).isEqualTo(initialEntityGroup.getConfiguration());
        assertThat(importedEntityGroup.getAdditionalInfo()).isEqualTo(initialEntityGroup.getAdditionalInfo());
    }

    protected void checkImportedConverterData(Converter initialConverter, Converter importedConverter) {
        assertThat(importedConverter.getType()).isEqualTo(initialConverter.getType());
        assertThat(importedConverter.getName()).isEqualTo(initialConverter.getName());
        assertThat(importedConverter.getConfiguration()).isEqualTo(initialConverter.getConfiguration());
        assertThat(importedConverter.getAdditionalInfo()).isEqualTo(initialConverter.getAdditionalInfo());
        assertThat(importedConverter.getDebugSettings()).isEqualTo(initialConverter.getDebugSettings());
    }

    protected void checkImportedIntegrationData(Integration initialIntegration, Integration importedIntegration) {
        assertThat(importedIntegration.getName()).isEqualTo(initialIntegration.getName());
        assertThat(importedIntegration.getType()).isEqualTo(initialIntegration.getType());
        assertThat(importedIntegration.getConfiguration()).isEqualTo(initialIntegration.getConfiguration());
        assertThat(importedIntegration.getAdditionalInfo()).isEqualTo(initialIntegration.getAdditionalInfo());
        assertThat(importedIntegration.getSecret()).isEqualTo(initialIntegration.getSecret());
    }

    private String createVersion(String name, EntityType... entityTypes) throws Exception {
        ComplexVersionCreateRequest request = new ComplexVersionCreateRequest();
        request.setVersionName(name);
        request.setBranch(branch);
        request.setSyncStrategy(SyncStrategy.MERGE);
        request.setEntityTypes(Arrays.stream(entityTypes).collect(Collectors.toMap(t -> t, entityType -> {
            EntityTypeVersionCreateConfig config = new EntityTypeVersionCreateConfig();
            config.setAllEntities(true);
            config.setSaveGroupEntities(true);
            config.setSaveRelations(true);
            config.setSaveAttributes(true);
            config.setSaveCredentials(true);
            config.setSaveCalculatedFields(true);
            config.setSavePermissions(true);
            config.setSaveGroupEntities(true);
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
            config.setSavePermissions(true);
            config.setSaveGroupEntities(true);
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
            config.setLoadPermissions(true);
            config.setLoadGroupEntities(true);
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
        String entityAliases = """
                {
                  "23c4185d-1497-9457-30b2-6d91e69a5b2c": {
                    "alias": "assets",
                    "filter": {
                      "entityList": [
                        "%s"
                      ],
                      "entityType": "ASSET",
                      "resolveMultiple": true,
                      "type": "entityList"
                    },
                    "id": "23c4185d-1497-9457-30b2-6d91e69a5b2c"
                  }
                }""".formatted(assetForEntityAlias.getId().toString());
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

    private CalculatedField createAlarmRule(EntityId entityId, String alarmType, DashboardId mobileDashboardId) {
        Argument temperatureArgument = new Argument();
        temperatureArgument.setRefEntityKey(new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null));
        temperatureArgument.setDefaultValue("0");
        Map<String, Argument> arguments = Map.of(
                "temperature", temperatureArgument
        );

        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(entityId);
        calculatedField.setName(alarmType);
        calculatedField.setType(CalculatedFieldType.ALARM);
        AlarmCalculatedFieldConfiguration configuration = new AlarmCalculatedFieldConfiguration();
        configuration.setArguments(arguments);
        SimpleAlarmCondition createCondition = new SimpleAlarmCondition();
        createCondition.setExpression(new TbelAlarmConditionExpression("return temperature >= 50;"));
        configuration.setCreateRules(Map.of(
                AlarmSeverity.CRITICAL, new AlarmRule(createCondition, "", mobileDashboardId)
        ));
        SimpleAlarmCondition clearCondition = new SimpleAlarmCondition();
        clearCondition.setExpression(new TbelAlarmConditionExpression("return temperature < 50;"));
        configuration.setClearRule(new AlarmRule(clearCondition, "", mobileDashboardId));
        calculatedField.setConfiguration(configuration);
        calculatedField.setDebugSettings(DebugSettings.all());
        return saveCalculatedField(calculatedField);
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

    protected EntityGroup createEntityGroup(EntityId ownerId, EntityType groupType, String name) {
        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setOwnerId(ownerId);
        entityGroup.setType(groupType);
        entityGroup.setName(name);
        entityGroup.setAdditionalInfo(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        return entityGroupService.saveEntityGroup(TenantId.SYS_TENANT_ID, ownerId, entityGroup);
    }

    protected Converter createConverter(ConverterType type, String name) {
        Converter converter = new Converter();
        converter.setType(type);
        converter.setName(name);
        converter.setConfiguration(JacksonUtil.newObjectNode()
                .<ObjectNode>set("encoder", new TextNode("b"))
                .set("decoder", new TextNode("c")));
        converter.setDebugSettings(DebugSettings.all());
        converter.setAdditionalInfo(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        return doPost("/api/converter", converter, Converter.class);
    }

    protected Integration createIntegration(ConverterId converterId, IntegrationType type, String name) {
        Integration integration = new Integration();
        integration.setType(type);
        integration.setName(name);
        integration.setDefaultConverterId(converterId);
        integration.setRoutingKey("abc");
        integration.setSecret("scrt");
        integration.setEnabled(false);
        integration.setConfiguration(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        integration.setAdditionalInfo(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        return doPost("/api/integration", integration, Integration.class);
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

    private void checkImportedSchedulerEventData(SchedulerEvent initialEvent, SchedulerEvent importedEvent, DashboardId dashboardId, UserId currentUserId) {
        checkImportedSchedulerEventData(initialEvent, importedEvent);
        ObjectNode config = (ObjectNode) importedEvent.getConfiguration().path("msgBody").path("reportConfig");
        String oldDash = config.path("dashboardId").asText(null);
        assertThat(oldDash).isNotNull();
        assertThat(oldDash).isEqualTo(dashboardId.toString());
        String oldUser = config.path("userId").asText(null);
        assertThat(oldUser).isNotNull();
        assertThat(oldUser).isEqualTo(currentUserId.toString()); // userId on import is set to current user
    }

    private void checkImportedSchedulerEventData(SchedulerEvent initialEvent, SchedulerEvent importedEvent, ReportTemplateId templateId, UserId currentUserId) {
        checkImportedSchedulerEventData(initialEvent, importedEvent);
        ObjectNode config = (ObjectNode) importedEvent.getConfiguration();
        String oldTemplate = config.path("reportTemplateId").path("id").asText(null);
        assertThat(oldTemplate).isNotNull();
        assertThat(oldTemplate).isEqualTo(templateId.toString());
        String oldUser = config.path("userId").path("id").asText(null);
        assertThat(oldUser).isNotNull();
        assertThat(oldUser).isEqualTo(currentUserId.toString()); // userId on import is set to current user
    }

    private void checkImportedSchedulerEventData(SchedulerEvent initialEvent, SchedulerEvent importedEvent, OtaPackageId otaPackageId) {
        checkImportedSchedulerEventData(initialEvent, importedEvent);
        JsonNode importedConfig = importedEvent.getConfiguration();
        ObjectNode config = (ObjectNode) importedConfig.get("msgBody");
        OtaPackageId importedOtaPackageId = JacksonUtil.convertValue(config, OtaPackageId.class);
        assertThat(importedOtaPackageId).isNotNull();
        assertThat(importedOtaPackageId.getId()).isEqualTo(otaPackageId.getId());
    }

    private void checkImportedSchedulerEventData(SchedulerEvent initialEvent, SchedulerEvent importedEvent) {
        assertThat(importedEvent.getName()).isEqualTo(initialEvent.getName());
        assertThat(importedEvent.getType()).isEqualTo(initialEvent.getType());
        assertThat(importedEvent.getSchedule()).isEqualTo(initialEvent.getSchedule());
    }

    private SchedulerEvent createSchedulerEvent(TenantId tenantId, EntityId originatorId, String name, String type, JsonNode configuration) {
        SchedulerEvent schedulerEvent = new SchedulerEvent();
        schedulerEvent.setTenantId(tenantId);
        schedulerEvent.setOwnerId(tenantId);
        schedulerEvent.setOriginatorId(originatorId);
        schedulerEvent.setConfiguration(configuration);
        schedulerEvent.setName(name);
        schedulerEvent.setType(type);
        ObjectNode schedule = JacksonUtil.newObjectNode();
        schedule.put("startTime", Long.MAX_VALUE);
        schedule.put("timezone", "UTC");
        schedulerEvent.setSchedule(schedule);
        return doPost("/api/schedulerEvent", schedulerEvent, SchedulerEvent.class);
    }

    private ReportTemplate createReportTemplate(TenantId tenantId, CustomerId customerId, String name, DeviceId deviceId) {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setTenantId(tenantId);
        reportTemplate.setCustomerId(customerId);
        reportTemplate.setName(name);
        reportTemplate.setType(ReportTemplateType.REPORT);
        reportTemplate.setFormat(TbReportFormat.CSV);

        String devicesAliasId = StringUtils.randomAlphabetic(10);
        EntityAlias entityAlias = buildDeviceTypeEntityAlias(devicesAliasId);

        EntityTableComponent tableComponent = new EntityTableComponent();
        List<DataKey> dataKeys = List.of(
                DataKey.builder().name("createdTime").type("entityField").label("CREATED TIME").usePostProcessing(false).build(),
                DataKey.builder().name("name").type("entityField").label("NAME").usePostProcessing(false).build(),
                DataKey.builder().name("type").type("entityField").label("TYPE").usePostProcessing(false).build(),
                DataKey.builder().name("temperature").type("timeseries").label("TEMPERATURE").usePostProcessing(false).units("K").decimals(2).build(),
                DataKey.builder().name("threshold").type("attribute").label("THRESHOLD").usePostProcessing(false).build()
        );
        tableComponent.setDataSources(List.of(DataSource.builder()
                .type(DataSourceType.DEVICE)
                .deviceId(deviceId.getId().toString())
                .dataKeys(dataKeys)
                .build()));

        CsvReportTemplateConfig configuration = CsvReportTemplateConfig.builder()
                .entityAliases(List.of(entityAlias))
                .components(List.of(tableComponent))
                .build();
        reportTemplate.setConfiguration(configuration);
        return doPost("/api/reportTemplate", reportTemplate, ReportTemplate.class);
    }

    private static EntityAlias buildDeviceTypeEntityAlias(String aliasId) {
        DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceTypes(List.of("default"));
        filter.setDeviceNameFilter("");
        return new EntityAlias(aliasId, "devices", filter);
    }

    private SchedulerEvent createSchedulerEventForOtaPackageType(TenantId tenantId, EntityId originatorId, String name, String type, OtaPackageId otaPackageId) {
        ObjectNode cfg = JacksonUtil.newObjectNode();
        cfg.put("msgType", type);
        ObjectNode msgBody = JacksonUtil.newObjectNode();
        msgBody.put("entityType", "OTA_PACKAGE");
        msgBody.put("id", otaPackageId.toString());
        cfg.set("msgBody", msgBody);
        cfg.set("metadata", JacksonUtil.newObjectNode());

        return createSchedulerEvent(tenantId, originatorId, name, type, cfg);
    }

    private SchedulerEvent createSchedulerEventForGenerateReportType(TenantId tenantId, EntityId originatorId, String name, DashboardId dashboardId) {
        ObjectNode reportConfig = JacksonUtil.newObjectNode();
        reportConfig.put("baseUrl", "http://localhost:8081");
        reportConfig.put("useDashboardTimewindow", true);
        ObjectNode history = JacksonUtil.newObjectNode();
        history.put("historyType", 0);
        history.put("interval", 1000);
        history.put("timewindowMs", 86_400_000);
        ObjectNode timewindow = JacksonUtil.newObjectNode();
        timewindow.put("selectedTab", 1);
        timewindow.set("history", history);
        reportConfig.set("timewindow", timewindow);
        reportConfig.put("namePattern", "report-%d{yyyy-MM-dd_HH:mm:ss}");
        reportConfig.put("type", "pdf");
        reportConfig.put("timezone", "Europe/Kiev");
        reportConfig.put("useCurrentUserCredentials", true);
        reportConfig.put("userId", "7a306270-4820-11f0-bc58-39b3596e763d");
        reportConfig.put("dashboardId", dashboardId.toString());
        reportConfig.put("state", "");

        ObjectNode msgBody = JacksonUtil.newObjectNode();
        msgBody.set("reportConfig", reportConfig);
        msgBody.put("sendEmail", false);

        ObjectNode cfg = JacksonUtil.newObjectNode();
        cfg.set("msgBody", msgBody);
        cfg.set("metadata", JacksonUtil.newObjectNode());

        return createSchedulerEvent(tenantId, originatorId, name, "generateDashboardReport", cfg);
    }

    private SchedulerEvent createSchedulerEventForGenerateReportType(TenantId tenantId, EntityId originatorId, String name, ReportTemplateId reportTemplateId, UserId userId) {
        ReportConfig reportConfig = new ReportConfig();
        reportConfig.setReportTemplateId(reportTemplateId);
        reportConfig.setTimezone("Europe/Kiev");
        reportConfig.setUserId(userId);
        return createSchedulerEvent(tenantId, originatorId, name, "generateReport", JacksonUtil.valueToTree(reportConfig));
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

    private Integration findIntegration(String name) throws Exception {
        return doGetTypedWithPageLink("/api/integrations?", new TypeReference<PageData<Integration>>() {}, new PageLink(100, 0, name)).getData().get(0);
    }

    private Converter findConverter(String name) throws Exception {
        return doGetTypedWithPageLink("/api/converters?", new TypeReference<PageData<Converter>>() {}, new PageLink(100, 0, name)).getData().get(0);
    }

    private EntityGroup findEntityGroup(String name, EntityType groupType) throws Exception {
        return doGetTypedWithPageLink("/api/entityGroups/" + groupType + "?", new TypeReference<PageData<EntityGroup>>() {}, new PageLink(100, 0, name)).getData().get(0);
    }

    private Role findRole(String name) throws Exception {
        return doGetTypedWithPageLink("/api/roles?", new TypeReference<PageData<Role>>() {}, new PageLink(100, 0, name)).getData().get(0);
    }

    private List<GroupPermissionInfo> findGroupPermissions(EntityGroupId userGroupId) throws Exception {
        return doGetTyped("/api/userGroup/" + userGroupId + "/groupPermissions?", new TypeReference<>() {});
    }

    private CalculatedField findCalculatedFieldByEntityId(EntityId entityId) throws Exception {
        return doGetTypedWithPageLink("/api/" + entityId.getEntityType() + "/" + entityId.getId() + "/calculatedFields?", new TypeReference<PageData<CalculatedField>>() {}, new PageLink(100, 0)).getData().get(0);
    }

    private List<CalculatedField> findCalculatedFieldsByEntityId(EntityId entityId) throws Exception {
        return doGetTypedWithPageLink("/api/" + entityId.getEntityType() + "/" + entityId.getId() + "/calculatedFields?", new TypeReference<PageData<CalculatedField>>() {}, new PageLink(100, 0)).getData();
    }

    private DeviceGroupOtaPackage findDeviceGroupOtaPackage(EntityGroupId groupId, OtaPackageType otaPackageType) throws Exception {
        return doGet("/api/deviceGroupOtaPackage/" + groupId.getId() + "/" + otaPackageType, DeviceGroupOtaPackage.class);
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

    private SchedulerEvent findSchedulerEvent(String name) throws Exception {
        SchedulerEventWithCustomerInfo eventInfo = doGetTyped("/api/schedulerEvents?", new TypeReference<List<SchedulerEventWithCustomerInfo>>() {}).stream()
                .filter(event -> event.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Scheduler event with name " + name + " not found"));
        return doGet("/api/schedulerEvent/" + eventInfo.getId().getId(), SchedulerEvent.class);
    }

    private ReportTemplate findReportTemplate(String name) throws Exception {
        ReportTemplateInfo reportTemplate = doGetTypedWithPageLink("/api/reportTemplateInfos/all?", new TypeReference<PageData<ReportTemplateInfo>>() {}, new PageLink(100, 0, name)).getData()
                .stream()
                .filter(template -> template.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Report template with name " + name + " not found"));
        return doGet("/api/reportTemplate/" + reportTemplate.getId().getId(), ReportTemplate.class);
    }

}
