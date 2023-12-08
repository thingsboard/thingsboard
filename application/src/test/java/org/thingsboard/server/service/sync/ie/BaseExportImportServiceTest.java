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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.debug.TbMsgGeneratorNode;
import org.thingsboard.rule.engine.debug.TbMsgGeneratorNodeConfiguration;
import org.thingsboard.rule.engine.metadata.TbGetAttributesNode;
import org.thingsboard.rule.engine.metadata.TbGetAttributesNodeConfiguration;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.device.data.DefaultDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.common.data.sync.ie.EntityExportSettings;
import org.thingsboard.server.common.data.sync.ie.EntityImportResult;
import org.thingsboard.server.common.data.sync.ie.EntityImportSettings;
import org.thingsboard.server.common.data.util.ThrowingRunnable;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;
import org.thingsboard.server.service.sync.vc.data.SimpleEntitiesExportCtx;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class BaseExportImportServiceTest extends AbstractControllerTest {

    @Autowired
    protected EntitiesExportImportService exportImportService;
    @Autowired
    protected DeviceService deviceService;
    @Autowired
    protected OtaPackageService otaPackageService;
    @Autowired
    protected DeviceProfileService deviceProfileService;
    @Autowired
    protected AssetProfileService assetProfileService;
    @Autowired
    protected AssetService assetService;
    @Autowired
    protected CustomerService customerService;
    @Autowired
    protected RuleChainService ruleChainService;
    @Autowired
    protected DashboardService dashboardService;
    @Autowired
    protected RelationService relationService;
    @Autowired
    protected TenantService tenantService;
    @Autowired
    protected EntityViewService entityViewService;

    protected TenantId tenantId1;
    protected User tenantAdmin1;

    protected TenantId tenantId2;
    protected User tenantAdmin2;

    @Before
    public void beforeEach() throws Exception {
        loginSysAdmin();
        Tenant tenant1 = new Tenant();
        tenant1.setTitle("Tenant 1");
        tenant1.setEmail("tenant1@thingsboard.org");
        this.tenantId1 = tenantService.saveTenant(tenant1).getId();
        User tenantAdmin1 = new User();
        tenantAdmin1.setTenantId(tenantId1);
        tenantAdmin1.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin1.setEmail("tenant1-admin@thingsboard.org");
        this.tenantAdmin1 = createUser(tenantAdmin1, "12345678");
        Tenant tenant2 = new Tenant();
        tenant2.setTitle("Tenant 2");
        tenant2.setEmail("tenant2@thingsboard.org");
        this.tenantId2 = tenantService.saveTenant(tenant2).getId();
        User tenantAdmin2 = new User();
        tenantAdmin2.setTenantId(tenantId2);
        tenantAdmin2.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin2.setEmail("tenant2-admin@thingsboard.org");
        this.tenantAdmin2 = createUser(tenantAdmin2, "12345678");
    }

    @After
    public void afterEach() {
        tenantService.deleteTenant(tenantId1);
        tenantService.deleteTenant(tenantId2);
    }

    protected Device createDevice(TenantId tenantId, CustomerId customerId, DeviceProfileId deviceProfileId, String name) {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setCustomerId(customerId);
        device.setName(name);
        device.setLabel("lbl");
        device.setDeviceProfileId(deviceProfileId);
        DeviceData deviceData = new DeviceData();
        deviceData.setTransportConfiguration(new DefaultDeviceTransportConfiguration());
        device.setDeviceData(deviceData);
        return deviceService.saveDevice(device);
    }

    protected OtaPackage createOtaPackage(TenantId tenantId, DeviceProfileId deviceProfileId, OtaPackageType type) {
        OtaPackage otaPackage = new OtaPackage();
        otaPackage.setTenantId(tenantId);
        otaPackage.setDeviceProfileId(deviceProfileId);
        otaPackage.setType(type);
        otaPackage.setTitle("My " + type);
        otaPackage.setVersion("v1.0");
        otaPackage.setFileName("filename.txt");
        otaPackage.setContentType("text/plain");
        otaPackage.setChecksumAlgorithm(ChecksumAlgorithm.SHA256);
        otaPackage.setChecksum("4bf5122f344554c53bde2ebb8cd2b7e3d1600ad631c385a5d7cce23c7785459a");
        otaPackage.setDataSize(1L);
        otaPackage.setData(ByteBuffer.wrap(new byte[]{(int) 1}));
        return otaPackageService.saveOtaPackage(otaPackage);
    }

    protected void checkImportedDeviceData(Device initialDevice, Device importedDevice) {
        assertThat(importedDevice.getName()).isEqualTo(initialDevice.getName());
        assertThat(importedDevice.getType()).isEqualTo(initialDevice.getType());
        assertThat(importedDevice.getDeviceData()).isEqualTo(initialDevice.getDeviceData());
        assertThat(importedDevice.getLabel()).isEqualTo(initialDevice.getLabel());
    }

    protected DeviceProfile createDeviceProfile(TenantId tenantId, RuleChainId defaultRuleChainId, DashboardId defaultDashboardId, String name) {
        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setTenantId(tenantId);
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
        return deviceProfileService.saveDeviceProfile(deviceProfile);
    }

    protected void checkImportedDeviceProfileData(DeviceProfile initialProfile, DeviceProfile importedProfile) {
        assertThat(initialProfile.getName()).isEqualTo(importedProfile.getName());
        assertThat(initialProfile.getType()).isEqualTo(importedProfile.getType());
        assertThat(initialProfile.getTransportType()).isEqualTo(importedProfile.getTransportType());
        assertThat(initialProfile.getProfileData()).isEqualTo(importedProfile.getProfileData());
        assertThat(initialProfile.getDescription()).isEqualTo(importedProfile.getDescription());
    }

    protected AssetProfile createAssetProfile(TenantId tenantId, RuleChainId defaultRuleChainId, DashboardId defaultDashboardId, String name) {
        AssetProfile assetProfile = new AssetProfile();
        assetProfile.setTenantId(tenantId);
        assetProfile.setName(name);
        assetProfile.setDescription("dscrptn");
        assetProfile.setDefaultRuleChainId(defaultRuleChainId);
        assetProfile.setDefaultDashboardId(defaultDashboardId);
        return assetProfileService.saveAssetProfile(assetProfile);
    }

    protected void checkImportedAssetProfileData(AssetProfile initialProfile, AssetProfile importedProfile) {
        assertThat(initialProfile.getName()).isEqualTo(importedProfile.getName());
        assertThat(initialProfile.getDescription()).isEqualTo(importedProfile.getDescription());
    }

    protected Asset createAsset(TenantId tenantId, CustomerId customerId, AssetProfileId assetProfileId, String name) {
        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setCustomerId(customerId);
        asset.setAssetProfileId(assetProfileId);
        asset.setName(name);
        asset.setLabel("lbl");
        asset.setAdditionalInfo(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        return assetService.saveAsset(asset);
    }

    protected void checkImportedAssetData(Asset initialAsset, Asset importedAsset) {
        assertThat(importedAsset.getName()).isEqualTo(initialAsset.getName());
        assertThat(importedAsset.getType()).isEqualTo(initialAsset.getType());
        assertThat(importedAsset.getLabel()).isEqualTo(initialAsset.getLabel());
        assertThat(importedAsset.getAdditionalInfo()).isEqualTo(initialAsset.getAdditionalInfo());
    }

    protected Customer createCustomer(TenantId tenantId, String name) {
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setTitle(name);
        customer.setCountry("ua");
        customer.setAddress("abb");
        customer.setEmail("ccc@aa.org");
        customer.setAdditionalInfo(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        return customerService.saveCustomer(customer);
    }

    protected void checkImportedCustomerData(Customer initialCustomer, Customer importedCustomer) {
        assertThat(importedCustomer.getTitle()).isEqualTo(initialCustomer.getTitle());
        assertThat(importedCustomer.getCountry()).isEqualTo(initialCustomer.getCountry());
        assertThat(importedCustomer.getAddress()).isEqualTo(initialCustomer.getAddress());
        assertThat(importedCustomer.getEmail()).isEqualTo(initialCustomer.getEmail());
    }

    protected Dashboard createDashboard(TenantId tenantId, CustomerId customerId, String name) {
        Dashboard dashboard = new Dashboard();
        dashboard.setTenantId(tenantId);
        dashboard.setTitle(name);
        dashboard.setConfiguration(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        dashboard.setImage("abvregewrg");
        dashboard.setMobileHide(true);
        dashboard = dashboardService.saveDashboard(dashboard);
        if (customerId != null) {
            dashboardService.assignDashboardToCustomer(tenantId, dashboard.getId(), customerId);
            return dashboardService.findDashboardById(tenantId, dashboard.getId());
        }
        return dashboard;
    }

    protected Dashboard createDashboard(TenantId tenantId, CustomerId customerId, String name, AssetId assetForEntityAlias) {
        Dashboard dashboard = createDashboard(tenantId, customerId, name);
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
        return dashboardService.saveDashboard(dashboard);
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
    protected RuleChain createRuleChain(TenantId tenantId, String name, EntityId originatorId) {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setTenantId(tenantId);
        ruleChain.setName(name);
        ruleChain.setType(RuleChainType.CORE);
        ruleChain.setDebugMode(true);
        ruleChain.setConfiguration(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        ruleChain = ruleChainService.saveRuleChain(ruleChain);

        RuleChainMetaData metaData = new RuleChainMetaData();
        metaData.setRuleChainId(ruleChain.getId());

        RuleNode ruleNode1 = new RuleNode();
        ruleNode1.setName("Generator 1");
        ruleNode1.setType(TbMsgGeneratorNode.class.getName());
        ruleNode1.setDebugMode(true);
        TbMsgGeneratorNodeConfiguration configuration1 = new TbMsgGeneratorNodeConfiguration();
        configuration1.setOriginatorType(originatorId.getEntityType());
        configuration1.setOriginatorId(originatorId.getId().toString());
        ruleNode1.setConfiguration(JacksonUtil.valueToTree(configuration1));

        RuleNode ruleNode2 = new RuleNode();
        ruleNode2.setName("Simple Rule Node 2");
        ruleNode2.setType(org.thingsboard.rule.engine.metadata.TbGetAttributesNode.class.getName());
        ruleNode2.setConfigurationVersion(TbGetAttributesNode.class.getAnnotation(org.thingsboard.rule.engine.api.RuleNode.class).version());
        ruleNode2.setDebugMode(true);
        TbGetAttributesNodeConfiguration configuration2 = new TbGetAttributesNodeConfiguration();
        configuration2.setServerAttributeNames(Collections.singletonList("serverAttributeKey2"));
        ruleNode2.setConfiguration(JacksonUtil.valueToTree(configuration2));

        metaData.setNodes(Arrays.asList(ruleNode1, ruleNode2));
        metaData.setFirstNodeIndex(0);
        metaData.addConnectionInfo(0, 1, TbNodeConnectionType.SUCCESS);
        ruleChainService.saveRuleChainMetaData(tenantId, metaData, Function.identity());

        return ruleChainService.findRuleChainById(tenantId, ruleChain.getId());
    }

    protected RuleChain createRuleChain(TenantId tenantId, String name) {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setTenantId(tenantId);
        ruleChain.setName(name);
        ruleChain.setType(RuleChainType.CORE);
        ruleChain.setDebugMode(true);
        ruleChain.setConfiguration(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        ruleChain = ruleChainService.saveRuleChain(ruleChain);

        RuleChainMetaData metaData = new RuleChainMetaData();
        metaData.setRuleChainId(ruleChain.getId());

        RuleNode ruleNode1 = new RuleNode();
        ruleNode1.setName("Simple Rule Node 1");
        ruleNode1.setType(org.thingsboard.rule.engine.metadata.TbGetAttributesNode.class.getName());
        ruleNode1.setConfigurationVersion(TbGetAttributesNode.class.getAnnotation(org.thingsboard.rule.engine.api.RuleNode.class).version());
        ruleNode1.setDebugMode(true);
        TbGetAttributesNodeConfiguration configuration1 = new TbGetAttributesNodeConfiguration();
        configuration1.setServerAttributeNames(Collections.singletonList("serverAttributeKey1"));
        ruleNode1.setConfiguration(JacksonUtil.valueToTree(configuration1));

        RuleNode ruleNode2 = new RuleNode();
        ruleNode2.setName("Simple Rule Node 2");
        ruleNode2.setType(org.thingsboard.rule.engine.metadata.TbGetAttributesNode.class.getName());
        ruleNode2.setConfigurationVersion(TbGetAttributesNode.class.getAnnotation(org.thingsboard.rule.engine.api.RuleNode.class).version());
        ruleNode2.setDebugMode(true);
        TbGetAttributesNodeConfiguration configuration2 = new TbGetAttributesNodeConfiguration();
        configuration2.setServerAttributeNames(Collections.singletonList("serverAttributeKey2"));
        ruleNode2.setConfiguration(JacksonUtil.valueToTree(configuration2));

        metaData.setNodes(Arrays.asList(ruleNode1, ruleNode2));
        metaData.setFirstNodeIndex(0);
        metaData.addConnectionInfo(0, 1, TbNodeConnectionType.SUCCESS);
        ruleChainService.saveRuleChainMetaData(tenantId, metaData, Function.identity());

        return ruleChainService.findRuleChainById(tenantId, ruleChain.getId());
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

    protected EntityView createEntityView(TenantId tenantId, CustomerId customerId, EntityId entityId, String name) {
        EntityView entityView = new EntityView();
        entityView.setTenantId(tenantId);
        entityView.setEntityId(entityId);
        entityView.setCustomerId(customerId);
        entityView.setName(name);
        entityView.setType("A");
        return entityViewService.saveEntityView(entityView);
    }

    protected EntityRelation createRelation(EntityId from, EntityId to) {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(from);
        relation.setTo(to);
        relation.setType(EntityRelation.MANAGES_TYPE);
        relation.setAdditionalInfo(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        relationService.saveRelation(TenantId.SYS_TENANT_ID, relation);
        return relation;
    }

    protected <E extends ExportableEntity<?> & HasTenantId> void checkImportedEntity(TenantId tenantId1, E initialEntity, TenantId tenantId2, E importedEntity) {
        assertThat(initialEntity.getTenantId()).isEqualTo(tenantId1);
        assertThat(importedEntity.getTenantId()).isEqualTo(tenantId2);

        assertThat(importedEntity.getExternalId()).isEqualTo(initialEntity.getId());

        boolean sameTenant = tenantId1.equals(tenantId2);
        if (!sameTenant) {
            assertThat(importedEntity.getId()).isNotEqualTo(initialEntity.getId());
        } else {
            assertThat(importedEntity.getId()).isEqualTo(initialEntity.getId());
        }
    }


    protected <E extends ExportableEntity<I>, I extends EntityId> EntityExportData<E> exportEntity(User user, I entityId) throws Exception {
        return exportEntity(user, entityId, EntityExportSettings.builder()
                .exportCredentials(true)
                .build());
    }

    protected <E extends ExportableEntity<I>, I extends EntityId> EntityExportData<E> exportEntity(User user, I entityId, EntityExportSettings exportSettings) throws Exception {
        return exportImportService.exportEntity(new SimpleEntitiesExportCtx(getSecurityUser(user), null, null, exportSettings), entityId);
    }

    protected <E extends ExportableEntity<I>, I extends EntityId> EntityImportResult<E> importEntity(User user, EntityExportData<E> exportData) throws Exception {
        return importEntity(user, exportData, EntityImportSettings.builder()
                .saveCredentials(true)
                .build());
    }

    protected <E extends ExportableEntity<I>, I extends EntityId> EntityImportResult<E> importEntity(User user, EntityExportData<E> exportData, EntityImportSettings importSettings) throws Exception {
        EntitiesImportCtx ctx = new EntitiesImportCtx(UUID.randomUUID(), getSecurityUser(user), null, importSettings);
        ctx.setFinalImportAttempt(true);
        exportData = JacksonUtil.treeToValue(JacksonUtil.valueToTree(exportData), EntityExportData.class);
        EntityImportResult<E> importResult = exportImportService.importEntity(ctx, exportData);
        exportImportService.saveReferencesAndRelations(ctx);
        for (ThrowingRunnable throwingRunnable : ctx.getEventCallbacks()) {
            throwingRunnable.run();
        }
        return importResult;
    }

    protected SecurityUser getSecurityUser(User user) {
        return new SecurityUser(user, true, new UserPrincipal(UserPrincipal.Type.USER_NAME, user.getEmail()));
    }

}
