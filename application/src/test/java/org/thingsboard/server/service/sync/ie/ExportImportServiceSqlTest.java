// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.sync.ie;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
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
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.device.data.DefaultDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.query.AliasEntityId;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.ReportTemplateType;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.common.data.report.configuration.CsvReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.EntityAlias;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
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
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.report.ReportTemplateService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.service.action.EntityActionService;
import org.thingsboard.server.dao.ota.OtaPackageStateService;
import org.thingsboard.server.service.scheduler.SchedulerService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.security.permission.AccessControlService;
import org.thingsboard.server.service.security.permission.UserPermissionsService;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;
import org.thingsboard.server.service.sync.vc.data.SimpleEntitiesExportCtx;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.verify;

@DaoSqlTest
public class ExportImportServiceSqlTest extends AbstractControllerTest {

    @MockitoSpyBean
    private EntityActionService entityActionService;
    @MockitoSpyBean
    private OtaPackageStateService otaPackageStateService;
    @MockitoSpyBean
    private UserPermissionsService userPermissionsService;
    @MockitoSpyBean
    private AccessControlService accessControlService;
    @MockitoSpyBean
    private SchedulerService schedulerService;

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
    @Autowired
    protected GroupPermissionService groupPermissionService;
    @Autowired
    protected IntegrationService integrationService;
    @Autowired
    protected ConverterService converterService;
    @Autowired
    protected RoleService roleService;
    @Autowired
    protected SchedulerEventService schedulerEventService;
    @Autowired
    protected ReportTemplateService reportTemplateService;

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
        Customer customer = createCustomer(tenantId1, null, "Customer 1");
        RuleChain ruleChain = createRuleChain(tenantId1, "Rule chain 1");
        Dashboard dashboard = createDashboard(tenantId1, null, "Dashboard 1");
        AssetProfile assetProfile = createAssetProfile(tenantId1, ruleChain.getId(), dashboard.getId(), "Asset profile 1");
        Asset asset = createAsset(tenantId1, null, assetProfile.getId(), null, "Asset 1");
        DeviceProfile deviceProfile = createDeviceProfile(tenantId1, ruleChain.getId(), dashboard.getId(), "Device profile 1");
        OtaPackage firmware = createOtaPackage(tenantId1, deviceProfile.getId(), OtaPackageType.FIRMWARE);
        Device device = createDevice(tenantId1, null, deviceProfile.getId(), null, "Device 1", firmware.getId());
        CalculatedField calculatedField = createCalculatedField(tenantId1, device.getId(), asset.getId());
        Converter converter = createConverter(tenantId1, ConverterType.DOWNLINK, "Converter 1");
        Integration integration = createIntegration(tenantId1, converter.getId(), IntegrationType.HTTP, "Integration 1");
        Role role = createGenericRole(tenantId1, null, "Role 1", Map.of(Resource.DEVICE, List.of(Operation.READ)));
        EntityGroup userGroup = createEntityGroup(tenantId1, EntityType.USER, "User group 1");
        createGroupPermission(tenantId1, userGroup.getId(), role.getId());
        SchedulerEvent schedulerEvent = createSchedulerEvent(tenantId1, device.getId(), "Scheduler Event 1", "report");
        ReportTemplate reportTemplate = createReportTemplate(tenantId1, null, "Weekly report template", device.getId());

        Map<EntityType, EntityExportData> entitiesExportData = Stream.of(customer.getId(), asset.getId(), device.getId(),
                        ruleChain.getId(), dashboard.getId(), assetProfile.getId(), deviceProfile.getId(), converter.getId(),
                        integration.getId(), role.getId(), userGroup.getId(), firmware.getId(), schedulerEvent.getId(), reportTemplate.getId())
                .map(entityId -> {
                    try {
                        return exportEntity(tenantAdmin1, entityId, EntityExportSettings.builder()
                                .exportCredentials(false)
                                .exportCalculatedFields(true)
                                .exportPermissions(true)
                                .build());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toMap(EntityExportData::getEntityType, d -> d));

        Mockito.reset(entityActionService, tbClusterService);
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
        verify(tbClusterService).sendNotificationMsgToEdge(any(), any(), eq(importedCustomer.getId()), any(), any(), eq(EdgeEventActionType.UPDATED), any(), any(), any());

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
        verify(tbClusterService).sendNotificationMsgToEdge(any(), any(), eq(importedAsset.getId()), any(), any(), eq(EdgeEventActionType.UPDATED), any(), any(), any());

        DeviceProfile importedDeviceProfile = (DeviceProfile) importEntity(tenantAdmin2, getAndClone(entitiesExportData, EntityType.DEVICE_PROFILE)).getSavedEntity();
        verify(entityActionService).logEntityAction(any(), eq(importedDeviceProfile.getId()), eq(importedDeviceProfile),
                any(), eq(ActionType.ADDED), isNull());
        verify(tbClusterService).onDeviceProfileChange(eq(importedDeviceProfile), any(), any());
        verify(tbClusterService).sendNotificationMsgToEdge(any(), any(), eq(importedDeviceProfile.getId()), any(), any(), eq(EdgeEventActionType.ADDED), any(), any(), any());
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

        Converter importedConverter = (Converter) importEntity(tenantAdmin2, entitiesExportData.get(EntityType.CONVERTER)).getSavedEntity();
        verify(tbClusterService).broadcastEntityStateChangeEvent(any(), eq(importedConverter.getId()), eq(ComponentLifecycleEvent.CREATED));
        verify(entityActionService).logEntityAction(any(), eq(importedConverter.getId()), eq(importedConverter), any(), eq(ActionType.ADDED), isNull());

        Integration importedIntegration = (Integration) importEntity(tenantAdmin2, entitiesExportData.get(EntityType.INTEGRATION), EntityImportSettings.builder()
                .autoGenerateIntegrationKey(true)
                .build()).getSavedEntity();
        verify(tbClusterService).broadcastEntityStateChangeEvent(any(), eq(importedIntegration.getId()), eq(ComponentLifecycleEvent.CREATED));
        verify(entityActionService).logEntityAction(any(), eq(importedIntegration.getId()), eq(importedIntegration), any(), eq(ActionType.ADDED), isNull());

        Role importedRole = (Role) importEntity(tenantAdmin2, entitiesExportData.get(EntityType.ROLE)).getSavedEntity();
        verify(userPermissionsService).onRoleUpdated(argThat(r -> r.getId().equals(importedRole.getId())));
        verify(entityActionService).logEntityAction(any(), eq(importedRole.getId()), notNull(), any(), eq(ActionType.ADDED), isNull());

        SchedulerEvent importedSchedulerEvent = (SchedulerEvent) importEntity(tenantAdmin2, entitiesExportData.get(EntityType.SCHEDULER_EVENT)).getSavedEntity();
        verify(schedulerService).onSchedulerEventAdded(argThat(se -> se.getId().equals(importedSchedulerEvent.getId())));
        verify(entityActionService).logEntityAction(any(), eq(importedSchedulerEvent.getId()), notNull(), any(), eq(ActionType.ADDED), isNull());

        EntityExportData<SchedulerEvent> updatedSchedulerEventEntity = getAndClone(entitiesExportData, EntityType.SCHEDULER_EVENT);
        updatedSchedulerEventEntity.getEntity().setName("t" + updatedSchedulerEventEntity.getEntity().getName());
        SchedulerEvent updatedSchedulerEvent = importEntity(tenantAdmin2, updatedSchedulerEventEntity).getSavedEntity();
        verify(schedulerService).onSchedulerEventUpdated(argThat(se -> se.getId().equals(updatedSchedulerEvent.getId())));

        ReportTemplate importedReportTemplate = (ReportTemplate) importEntity(tenantAdmin2, entitiesExportData.get(EntityType.REPORT_TEMPLATE)).getSavedEntity();
        verify(entityActionService).logEntityAction(any(), eq(importedReportTemplate.getId()), notNull(), any(), eq(ActionType.ADDED), isNull());
    }

    @Test
    public void testExternalIdsInExportData() throws Exception {
        Customer customer = createCustomer(tenantId1, null, "Customer 1");
        AssetProfile assetProfile = createAssetProfile(tenantId1, null, null, "Asset profile 1");
        Asset asset = createAsset(tenantId1, customer.getId(), assetProfile.getId(), null, "Asset 1");
        RuleChain ruleChain = createRuleChain(tenantId1, "Rule chain 1", asset.getId());
        Dashboard dashboard = createDashboard(tenantId1, customer.getId(), "Dashboard 1", asset.getId());

        assetProfile.setDefaultRuleChainId(ruleChain.getId());
        assetProfile.setDefaultDashboardId(dashboard.getId());
        assetProfile = assetProfileService.saveAssetProfile(assetProfile);

        DeviceProfile deviceProfile = createDeviceProfile(tenantId1, ruleChain.getId(), dashboard.getId(), "Device profile 1");
        OtaPackage firmware = createOtaPackage(tenantId1, deviceProfile.getId(), OtaPackageType.FIRMWARE);
        Device device = createDevice(tenantId1, customer.getId(), deviceProfile.getId(), null, "Device 1", firmware.getId());
        EntityView entityView = createEntityView(tenantId1, customer.getId(), device.getId(), "Entity view 1");
        Converter converter = createConverter(tenantId1, ConverterType.UPLINK, "Converter 1");
        Integration integration = createIntegration(tenantId1, converter.getId(), IntegrationType.HTTP, "Integration 1");
        SchedulerEvent schedulerEvent = createSchedulerEvent(tenantId1, device.getId(), "Scheduler Event 1", "report");
        ReportTemplate reportTemplate = createReportTemplate(tenantId1, customer.getId(), "Weekly report", device.getId());

        CalculatedField calculatedField = createCalculatedField(tenantId1, device.getId(), device.getId());

        Map<EntityId, EntityId> ids = new HashMap<>();
        for (EntityId entityId : List.of(customer.getId(), ruleChain.getId(), dashboard.getId(), assetProfile.getId(), asset.getId(),
                deviceProfile.getId(), firmware.getId(), device.getId(), entityView.getId(), converter.getId(), integration.getId(),
                ruleChain.getId(), dashboard.getId(), schedulerEvent.getId(), reportTemplate.getId())) {
            EntityExportData exportData = exportEntity(getSecurityUser(tenantAdmin1), entityId);
            EntityImportResult importResult = importEntity(getSecurityUser(tenantAdmin2), exportData, EntityImportSettings.builder()
                    .saveCredentials(false)
                    .saveCalculatedFields(true)
                    .autoGenerateIntegrationKey(true)
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
        assertThat(exportedDashboard.getCustomerId()).isEqualTo(customer.getId());
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

        Integration exportedIntegration = (Integration) exportEntity(tenantAdmin2, (IntegrationId) ids.get(integration.getId())).getEntity();
        assertThat(exportedIntegration.getDefaultConverterId()).isEqualTo(converter.getId());

        SchedulerEvent exportedSchedulerEvent = (SchedulerEvent) exportEntity(tenantAdmin2, (SchedulerEventId) ids.get(schedulerEvent.getId())).getEntity();
        assertThat(exportedSchedulerEvent.getOriginatorId()).isEqualTo(schedulerEvent.getOriginatorId());

        ReportTemplate exportedReportTemplate = (ReportTemplate) exportEntity(tenantAdmin2, (ReportTemplateId) ids.get(reportTemplate.getId())).getEntity();
        assertThat(exportedReportTemplate.getCustomerId()).isEqualTo(reportTemplate.getCustomerId());
        assertThat(((SingleEntityFilter)exportedReportTemplate.getConfiguration().getEntityAliases().get(0).getFilter()).getSingleEntity()).isEqualTo(device.getId());

        deviceProfile.setDefaultDashboardId(null);
        deviceProfileService.saveDeviceProfile(deviceProfile);
        DeviceProfile importedDeviceProfile = deviceProfileService.findDeviceProfileById(tenantId2, (DeviceProfileId) ids.get(deviceProfile.getId()));
        importedDeviceProfile.setDefaultDashboardId(null);
        deviceProfileService.saveDeviceProfile(importedDeviceProfile);
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

    protected Asset createAsset(TenantId tenantId, CustomerId customerId, AssetProfileId assetProfileId, EntityGroupId entityGroupId, String name) {
        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setCustomerId(customerId);
        asset.setAssetProfileId(assetProfileId);
        asset.setName(name);
        asset.setLabel("lbl");
        asset.setAdditionalInfo(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        asset = assetService.saveAsset(asset);
        if (entityGroupId != null) {
            entityGroupService.addEntityToEntityGroup(tenantId, entityGroupId, asset.getId());
        }
        return asset;
    }

    protected Device createDevice(TenantId tenantId, CustomerId customerId, DeviceProfileId deviceProfileId, EntityGroupId entityGroupId, String name, OtaPackageId firmwareId) {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setCustomerId(customerId);
        device.setName(name);
        device.setLabel("lbl");
        device.setDeviceProfileId(deviceProfileId);
        device.setFirmwareId(firmwareId);
        DeviceData deviceData = new DeviceData();
        deviceData.setTransportConfiguration(new DefaultDeviceTransportConfiguration());
        device.setDeviceData(deviceData);
        device = deviceService.saveDevice(device);
        if (entityGroupId != null) {
            entityGroupService.addEntityToEntityGroup(tenantId, entityGroupId, device.getId());
        }
        return device;
    }

    protected Customer createCustomer(TenantId tenantId, EntityGroupId entityGroupId, String name) {
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        if (entityGroupId != null) {
            EntityGroup customerGroup = entityGroupService.findEntityGroupById(tenantId, entityGroupId);
            if (customerGroup.getOwnerId() instanceof CustomerId) {
                customer.setParentCustomerId((CustomerId) customerGroup.getOwnerId());
            }
        }
        customer.setTitle(name);
        customer.setCountry("ua");
        customer.setAddress("abb");
        customer.setEmail("ccc@aa.org");
        customer.setAdditionalInfo(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        customer = customerService.saveCustomer(customer);
        if (entityGroupId != null) {
            entityGroupService.addEntityToEntityGroup(tenantId, entityGroupId, customer.getId());
        }
        return customer;
    }

    protected Dashboard createDashboard(TenantId tenantId, CustomerId customerId, String name) {
        Dashboard dashboard = new Dashboard();
        dashboard.setTenantId(tenantId);
        dashboard.setCustomerId(customerId);
        dashboard.setTitle(name);
        dashboard.setConfiguration(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        dashboard.setImage("abvregewrg");
        dashboard.setMobileHide(true);
        dashboard = dashboardService.saveDashboard(dashboard);
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

    protected EntityGroup createEntityGroup(EntityId ownerId, EntityType groupType, String name) {
        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setOwnerId(ownerId);
        entityGroup.setType(groupType);
        entityGroup.setName(name);
        entityGroup.setAdditionalInfo(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        return entityGroupService.saveEntityGroup(TenantId.SYS_TENANT_ID, ownerId, entityGroup);
    }

    protected Converter createConverter(TenantId tenantId, ConverterType type, String name) {
        Converter converter = new Converter();
        converter.setTenantId(tenantId);
        converter.setType(type);
        converter.setName(name);
        converter.setConfiguration(JacksonUtil.newObjectNode()
                .<ObjectNode>set("encoder", new TextNode("b"))
                .set("decoder", new TextNode("c")));
        converter.setDebugSettings(DebugSettings.all());
        converter.setAdditionalInfo(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        return converterService.saveConverter(converter);
    }

    protected Integration createIntegration(TenantId tenantId, ConverterId converterId, IntegrationType type, String name) {
        Integration integration = new Integration();
        integration.setTenantId(tenantId);
        integration.setType(type);
        integration.setName(name);
        integration.setDefaultConverterId(converterId);
        integration.setRoutingKey("abc");
        integration.setSecret("scrt");
        integration.setEnabled(false);
        integration.setConfiguration(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        integration.setAdditionalInfo(JacksonUtil.newObjectNode().set("a", new TextNode("b")));
        return integrationService.saveIntegration(integration);
    }

    protected Role createGenericRole(TenantId tenantId, CustomerId customerId, String name, Map<Resource, List<Operation>> permissions) {
        return createRole(tenantId, customerId, name, RoleType.GENERIC, permissions);
    }

    private Role createRole(TenantId tenantId, CustomerId customerId, String name, RoleType roleType, Object permissions) {
        Role role = new Role();
        role.setTenantId(tenantId);
        role.setCustomerId(customerId);
        role.setName(name);
        role.setType(roleType);
        role.setPermissions(JacksonUtil.valueToTree(permissions));
        return roleService.saveRole(tenantId, role);
    }

    protected GroupPermission createGroupPermission(TenantId tenantId, EntityGroupId userGroupId, RoleId genericRoleId) {
        return createGroupPermission(tenantId, userGroupId, genericRoleId, null, null);
    }

    protected GroupPermission createGroupPermission(TenantId tenantId, EntityGroupId userGroupId, RoleId roleId, EntityGroupId entityGroupId, EntityType entityGroupType) {
        GroupPermission groupPermission = new GroupPermission();
        groupPermission.setTenantId(tenantId);
        groupPermission.setUserGroupId(userGroupId);
        groupPermission.setRoleId(roleId);
        groupPermission.setEntityGroupId(entityGroupId);
        groupPermission.setEntityGroupType(entityGroupType);
        return groupPermissionService.saveGroupPermission(tenantId, groupPermission);
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

    private SchedulerEvent createSchedulerEvent(TenantId tenantId, EntityId originatorId, String name, String type) {
        SchedulerEvent schedulerEvent = new SchedulerEvent();
        schedulerEvent.setTenantId(tenantId);
        schedulerEvent.setOwnerId(tenantId);
        schedulerEvent.setOriginatorId(originatorId);
        schedulerEvent.setConfiguration(JacksonUtil.newObjectNode());
        schedulerEvent.setName(name);
        schedulerEvent.setType(type);
        ObjectNode schedule = JacksonUtil.newObjectNode();
        schedule.put("startTime", Long.MAX_VALUE);
        schedule.put("timezone", "UTC");
        schedulerEvent.setSchedule(schedule);
        return schedulerEventService.saveSchedulerEvent(schedulerEvent);
    }

    private ReportTemplate createReportTemplate(TenantId tenantId, CustomerId customerId, String name, EntityId entityId) {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setTenantId(tenantId);
        reportTemplate.setCustomerId(customerId);
        reportTemplate.setFormat(TbReportFormat.CSV);
        CsvReportTemplateConfig configuration = new CsvReportTemplateConfig();
        configuration.setComponents(Collections.emptyList());
        SingleEntityFilter filter = new SingleEntityFilter();
        filter.setSingleEntity(AliasEntityId.fromEntityId(entityId));
        configuration.setEntityAliases(List.of(new EntityAlias(UUID.randomUUID().toString(), "by device id", filter)));
        reportTemplate.setConfiguration(configuration);
        reportTemplate.setType(ReportTemplateType.REPORT);
        reportTemplate.setName(name);
        return reportTemplateService.saveReportTemplate(reportTemplate);
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
                .autoGenerateIntegrationKey(true)
                .build());
    }

    protected <E extends ExportableEntity<I>, I extends EntityId> EntityImportResult<E> importEntity(User user, EntityExportData<E> exportData, EntityImportSettings importSettings) throws Exception {
        EntitiesImportCtx ctx = new EntitiesImportCtx(UUID.randomUUID(), getSecurityUser(user), null, importSettings);
        ctx.setFinalImportAttempt(true);
        exportData = JacksonUtil.clone(exportData);
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
        return new SecurityUser(user, true, new UserPrincipal(UserPrincipal.Type.USER_NAME, user.getEmail()),
                new MergedUserPermissions(Map.of(Resource.ALL, Set.of(Operation.ALL)), Collections.emptyMap()));
    }

}
