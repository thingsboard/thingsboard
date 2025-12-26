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
package org.thingsboard.server.service.sync.ie;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
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
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.SimpleCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.TimeSeriesOutput;
import org.thingsboard.server.common.data.device.data.DefaultDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.sync.ie.DeviceExportData;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.common.data.sync.ie.EntityExportSettings;
import org.thingsboard.server.common.data.sync.ie.EntityImportResult;
import org.thingsboard.server.common.data.sync.ie.EntityImportSettings;
import org.thingsboard.server.common.data.sync.ie.RuleChainExportData;
import org.thingsboard.server.common.data.util.ThrowingRunnable;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.service.action.EntityActionService;
import org.thingsboard.server.service.ota.OtaPackageStateService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;
import org.thingsboard.server.service.sync.vc.data.SimpleEntitiesExportCtx;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;

@DaoSqlTest
public class ExportImportServiceSqlTest extends AbstractControllerTest {

    @SpyBean
    private EntityActionService entityActionService;
    @SpyBean
    private OtaPackageStateService otaPackageStateService;

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
    @Autowired
    protected CalculatedFieldService calculatedFieldService;

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

    @SuppressWarnings({"rawTypes", "unchecked"})
    @Test
    public void testEntityEventsOnImport() throws Exception {
        Customer customer = createCustomer(tenantId1, "Customer 1");
        RuleChain ruleChain = createRuleChain(tenantId1, "Rule chain 1");
        Dashboard dashboard = createDashboard(tenantId1, null, "Dashboard 1");
        AssetProfile assetProfile = createAssetProfile(tenantId1, ruleChain.getId(), dashboard.getId(), "Asset profile 1");
        Asset asset = createAsset(tenantId1, null, assetProfile.getId(), "Asset 1");
        DeviceProfile deviceProfile = createDeviceProfile(tenantId1, ruleChain.getId(), dashboard.getId(), "Device profile 1");
        OtaPackage firmware = createOtaPackage(tenantId1, deviceProfile.getId(), OtaPackageType.FIRMWARE);
        Device device = createDevice(tenantId1, null, deviceProfile.getId(), "Device 1", firmware.getId(), null);
        CalculatedField calculatedField = createCalculatedField(tenantId1, device.getId(), asset.getId());

        Map<EntityType, EntityExportData> entitiesExportData = Stream.of(customer.getId(), asset.getId(), device.getId(),
                        ruleChain.getId(), dashboard.getId(), assetProfile.getId(), deviceProfile.getId(), firmware.getId())
                .map(entityId -> {
                    try {
                        return exportEntity(tenantAdmin1, entityId, EntityExportSettings.builder()
                                .exportCredentials(false)
                                .exportCalculatedFields(true)
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
        verify(tbClusterService).sendNotificationMsgToEdge(any(), any(), eq(importedCustomer.getId()), any(), any(), eq(EdgeEventActionType.UPDATED), any());

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
        verify(tbClusterService).sendNotificationMsgToEdge(any(), any(), eq(importedAssetProfile.getId()), any(), any(), eq(EdgeEventActionType.ADDED), any());

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
        verify(tbClusterService).sendNotificationMsgToEdge(any(), any(), eq(importedAsset.getId()), any(), any(), eq(EdgeEventActionType.UPDATED), any());

        DeviceProfile importedDeviceProfile = (DeviceProfile) importEntity(tenantAdmin2, getAndClone(entitiesExportData, EntityType.DEVICE_PROFILE)).getSavedEntity();
        verify(entityActionService).logEntityAction(any(), eq(importedDeviceProfile.getId()), eq(importedDeviceProfile),
                any(), eq(ActionType.ADDED), isNull());
        verify(tbClusterService).onDeviceProfileChange(eq(importedDeviceProfile), any(), any());
        verify(tbClusterService).sendNotificationMsgToEdge(any(), any(), eq(importedDeviceProfile.getId()), any(), any(), eq(EdgeEventActionType.ADDED), any());
        verify(otaPackageStateService).update(eq(importedDeviceProfile), eq(false), eq(false));

        OtaPackage importedFirmware = (OtaPackage) importEntity(tenantAdmin2, getAndClone(entitiesExportData, EntityType.OTA_PACKAGE)).getSavedEntity();
        verify(entityActionService).logEntityAction(any(), eq(importedFirmware.getId()), eq(importedFirmware),
                any(), eq(ActionType.ADDED), isNull());

        Device importedDevice = (Device) importEntity(tenantAdmin2, getAndClone(entitiesExportData, EntityType.DEVICE)).getSavedEntity();
        verify(entityActionService).logEntityAction(any(), eq(importedDevice.getId()), eq(importedDevice),
                any(), eq(ActionType.ADDED), isNull());
        verify(tbClusterService).onDeviceUpdated(eq(importedDevice), isNull());
        importEntity(tenantAdmin2, getAndClone(entitiesExportData, EntityType.DEVICE));
        verify(tbClusterService, Mockito.never()).onDeviceUpdated(eq(importedDevice), eq(importedDevice));
        assertThat(importedDevice.getFirmwareId()).isEqualTo(importedFirmware.getId());

        // calculated field of imported device:
        List<CalculatedField> calculatedFields = calculatedFieldService.findCalculatedFieldsByEntityId(tenantId2, importedDevice.getId());
        assertThat(calculatedFields.size()).isOne();
        var importedCalculatedField = calculatedFields.get(0);
        assertThat(importedCalculatedField.getName()).isEqualTo(calculatedField.getName());
        verify(tbClusterService).onCalculatedFieldUpdated(eq(importedCalculatedField), isNull(), any());

        EntityExportData<Device> updatedDeviceEntity = getAndClone(entitiesExportData, EntityType.DEVICE);
        updatedDeviceEntity.getEntity().setLabel("t" + updatedDeviceEntity.getEntity().getLabel());
        Device updatedDevice = importEntity(tenantAdmin2, updatedDeviceEntity).getSavedEntity();
        verify(tbClusterService).onDeviceUpdated(eq(updatedDevice), eq(importedDevice));

        // update calculated field:
        DeviceExportData deviceExportData = (DeviceExportData) getAndClone(entitiesExportData, EntityType.DEVICE);
        deviceExportData.setCalculatedFields(deviceExportData.getCalculatedFields().stream().peek(field -> field.setName("t_" + field.getName())).toList());
        importEntity(tenantAdmin2, deviceExportData).getSavedEntity();

        calculatedFields = calculatedFieldService.findCalculatedFieldsByEntityId(tenantId2, importedDevice.getId());
        assertThat(calculatedFields.size()).isOne();
        importedCalculatedField = calculatedFields.get(0);
        assertThat(importedCalculatedField.getName()).startsWith("t_");
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
        OtaPackage firmware = createOtaPackage(tenantId1, deviceProfile.getId(), OtaPackageType.FIRMWARE);
        Device device = createDevice(tenantId1, customer.getId(), deviceProfile.getId(), "Device 1", firmware.getId(), null);
        EntityView entityView = createEntityView(tenantId1, customer.getId(), device.getId(), "Entity view 1");

        CalculatedField calculatedField = createCalculatedField(tenantId1, device.getId(), device.getId());

        Map<EntityId, EntityId> ids = new HashMap<>();
        for (EntityId entityId : List.of(customer.getId(), ruleChain.getId(), dashboard.getId(), assetProfile.getId(), asset.getId(),
                deviceProfile.getId(), firmware.getId(), device.getId(), entityView.getId(), ruleChain.getId(), dashboard.getId())) {
            EntityExportData exportData = exportEntity(getSecurityUser(tenantAdmin1), entityId);
            EntityImportResult importResult = importEntity(getSecurityUser(tenantAdmin2), exportData, EntityImportSettings.builder()
                    .saveCredentials(false)
                    .saveCalculatedFields(true)
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

        OtaPackage exportedFirmware = (OtaPackage) exportEntity(tenantAdmin2, (OtaPackageId) ids.get(firmware.getId())).getEntity();
        assertThat(exportedFirmware.getDeviceProfileId()).isEqualTo(exportedDeviceProfile.getId());
        assertThat(exportedFirmware.getId()).isEqualTo(firmware.getId());

        EntityExportData<Device> entityExportData = exportEntity(tenantAdmin2, (DeviceId) ids.get(device.getId()));
        Device exportedDevice = entityExportData.getEntity();
        assertThat(exportedDevice.getCustomerId()).isEqualTo(customer.getId());
        assertThat(exportedDevice.getDeviceProfileId()).isEqualTo(deviceProfile.getId());
        assertThat(exportedDevice.getFirmwareId()).isEqualTo(firmware.getId());

        List<CalculatedField> calculatedFields = entityExportData.getCalculatedFields();
        assertThat(calculatedFields.size()).isOne();
        CalculatedField field = calculatedFields.get(0);
        assertThat(field.getName()).isEqualTo(calculatedField.getName());

        EntityView exportedEntityView = (EntityView) exportEntity(tenantAdmin2, (EntityViewId) ids.get(entityView.getId())).getEntity();
        assertThat(exportedEntityView.getCustomerId()).isEqualTo(customer.getId());
        assertThat(exportedEntityView.getEntityId()).isEqualTo(device.getId());

        deviceProfile.setDefaultDashboardId(null);
        deviceProfileService.saveDeviceProfile(deviceProfile);
        DeviceProfile importedDeviceProfile = deviceProfileService.findDeviceProfileById(tenantId2, (DeviceProfileId) ids.get(deviceProfile.getId()));
        importedDeviceProfile.setDefaultDashboardId(null);
        deviceProfileService.saveDeviceProfile(importedDeviceProfile);
    }

    protected Device createDevice(TenantId tenantId, CustomerId customerId, DeviceProfileId deviceProfileId, String name, OtaPackageId firmwareId, OtaPackageId softwareId) {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setCustomerId(customerId);
        device.setName(name);
        device.setLabel("lbl");
        device.setDeviceProfileId(deviceProfileId);
        device.setFirmwareId(firmwareId);
        device.setSoftwareId(softwareId);
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

    protected AssetProfile createAssetProfile(TenantId tenantId, RuleChainId defaultRuleChainId, DashboardId defaultDashboardId, String name) {
        AssetProfile assetProfile = new AssetProfile();
        assetProfile.setTenantId(tenantId);
        assetProfile.setName(name);
        assetProfile.setDescription("dscrptn");
        assetProfile.setDefaultRuleChainId(defaultRuleChainId);
        assetProfile.setDefaultDashboardId(defaultDashboardId);
        return assetProfileService.saveAssetProfile(assetProfile);
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
        TbMsgGeneratorNodeConfiguration configuration1 = new TbMsgGeneratorNodeConfiguration();
        configuration1.setOriginatorType(originatorId.getEntityType());
        configuration1.setOriginatorId(originatorId.getId().toString());
        ruleNode1.setConfiguration(JacksonUtil.valueToTree(configuration1));

        RuleNode ruleNode2 = new RuleNode();
        ruleNode2.setName("Simple Rule Node 2");
        ruleNode2.setType(org.thingsboard.rule.engine.metadata.TbGetAttributesNode.class.getName());
        ruleNode2.setConfigurationVersion(TbGetAttributesNode.class.getAnnotation(org.thingsboard.rule.engine.api.RuleNode.class).version());
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
        TbGetAttributesNodeConfiguration configuration1 = new TbGetAttributesNodeConfiguration();
        configuration1.setServerAttributeNames(Collections.singletonList("serverAttributeKey1"));
        ruleNode1.setConfiguration(JacksonUtil.valueToTree(configuration1));

        RuleNode ruleNode2 = new RuleNode();
        ruleNode2.setName("Simple Rule Node 2");
        ruleNode2.setType(org.thingsboard.rule.engine.metadata.TbGetAttributesNode.class.getName());
        ruleNode2.setConfigurationVersion(TbGetAttributesNode.class.getAnnotation(org.thingsboard.rule.engine.api.RuleNode.class).version());
        TbGetAttributesNodeConfiguration configuration2 = new TbGetAttributesNodeConfiguration();
        configuration2.setServerAttributeNames(Collections.singletonList("serverAttributeKey2"));
        ruleNode2.setConfiguration(JacksonUtil.valueToTree(configuration2));

        metaData.setNodes(Arrays.asList(ruleNode1, ruleNode2));
        metaData.setFirstNodeIndex(0);
        metaData.addConnectionInfo(0, 1, TbNodeConnectionType.SUCCESS);
        ruleChainService.saveRuleChainMetaData(tenantId, metaData, Function.identity());

        return ruleChainService.findRuleChainById(tenantId, ruleChain.getId());
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

    private CalculatedField createCalculatedField(TenantId tenantId, EntityId entityId, EntityId referencedEntityId) {
        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setTenantId(tenantId);
        calculatedField.setEntityId(entityId);
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setName("Test Calculated Field");
        calculatedField.setConfigurationVersion(1);
        calculatedField.setConfiguration(getCalculatedFieldConfig(referencedEntityId));
        calculatedField.setVersion(1L);
        return calculatedFieldService.save(calculatedField);
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

    protected <E extends ExportableEntity<I>, I extends EntityId> EntityExportData<E> exportEntity(User user, I entityId) throws Exception {
        return exportEntity(user, entityId, EntityExportSettings.builder()
                .exportCredentials(true)
                .exportCalculatedFields(true)
                .build());
    }

    protected <E extends ExportableEntity<I>, I extends EntityId> EntityExportData<E> exportEntity(User user, I entityId, EntityExportSettings exportSettings) throws Exception {
        return exportImportService.exportEntity(new SimpleEntitiesExportCtx(getSecurityUser(user), null, null, exportSettings), entityId);
    }

    protected <E extends ExportableEntity<I>, I extends EntityId> EntityImportResult<E> importEntity(User user, EntityExportData<E> exportData) throws Exception {
        return importEntity(user, exportData, EntityImportSettings.builder()
                .saveCredentials(true)
                .saveCalculatedFields(true)
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


    @SuppressWarnings("rawTypes")
    private static EntityExportData getAndClone(Map<EntityType, EntityExportData> map, EntityType entityType) {
        return JacksonUtil.clone(map.get(entityType));
    }

    protected SecurityUser getSecurityUser(User user) {
        return new SecurityUser(user, true, new UserPrincipal(UserPrincipal.Type.USER_NAME, user.getEmail()));
    }

}
