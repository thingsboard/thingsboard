// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.iot_hub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientException;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.exception.EntityVersionMismatchException;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.IotHubInstalledItemId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.subscription.SubscriptionErrorCode;
import org.thingsboard.server.common.data.subscription.SubscriptionException;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.server.common.data.iot_hub.DeviceInstalledItemDescriptor;
import org.thingsboard.server.common.data.iot_hub.IotHubInstalledItem;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.iot_hub.IotHubInstalledItemService;
import org.thingsboard.server.dao.subscription.PlatformFeature;
import org.thingsboard.server.dao.subscription.SubscriptionService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.service.entitiy.TbLogEntityActionService;
import org.thingsboard.server.service.entitiy.integration.TbIntegrationService;
import org.thingsboard.server.service.install.ProjectInfo;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DefaultDevicePackageServiceTest {

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
    private final SecurityUser user = mock(SecurityUser.class);
    private final UUID itemId = UUID.randomUUID();
    private final String versionId = UUID.randomUUID().toString();
    private final UUID installedVersionId = UUID.randomUUID();
    private final DeviceProfileId profileId = new DeviceProfileId(UUID.randomUUID());
    private final DashboardId dashboardId = new DashboardId(UUID.randomUUID());
    private final DeviceId deviceId = new DeviceId(UUID.randomUUID());
    private final IntegrationId integrationId = new IntegrationId(UUID.randomUUID());

    @Mock
    private IotHubInstalledItemService installedItemService;
    @Mock
    private IotHubRestClient iotHubRestClient;
    @Mock
    private DevicePackageEntities packageEntities;
    @Mock
    private DeviceProfileService deviceProfileService;
    @Mock
    private DashboardService dashboardService;
    @Mock
    private DeviceService deviceService;
    @Mock
    private DeviceCredentialsService deviceCredentialsService;
    @Mock
    private ConverterService converterService;
    @Mock
    private IntegrationService integrationService;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private EntityService entityService;
    @Mock
    private TbLogEntityActionService logEntityActionService;
    @Mock
    private TbIntegrationService tbIntegrationService;
    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private TelemetrySubscriptionService telemetryService;
    @Mock
    private IotHubService iotHubService;
    @Mock
    private ProjectInfo projectInfo;
    @Mock
    private CustomerService customerService;
    @Mock
    private EntityGroupService entityGroupService;

    @InjectMocks
    private DefaultDevicePackageService service;

    private IotHubInstalledItem installedItem;
    // Set when the transaction callback threw, so the transaction would roll back
    private final List<RuntimeException> rolledBack = new ArrayList<>();
    private boolean inTransaction;
    private final List<String> savedInTransaction = new ArrayList<>();
    // What must run after commit: the audit log, the Hub install report and the attributes, each marked if it ran inside
    private final List<String> afterCommitCalls = new ArrayList<>();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "entityService", entityService);
        ReflectionTestUtils.setField(service, "logEntityActionService", logEntityActionService);
        ReflectionTestUtils.setField(service, "customerService", customerService);
        ReflectionTestUtils.setField(service, "entityGroupService", entityGroupService);
        when(user.getTenantId()).thenReturn(tenantId);

        DeviceInstalledItemDescriptor descriptor = new DeviceInstalledItemDescriptor();
        descriptor.setSelectedInstallMethod("INTEGRATION_TTN");
        descriptor.setCreatedEntityIds(List.of(profileId, deviceId, dashboardId, integrationId));
        descriptor.setDashboardId(dashboardId);
        // The package created these entities
        descriptor.setInstallState(Map.of("Profile", stateOf(profileId, "created"), "AM308", stateOf(deviceId, "created"),
                "Dashboard", stateOf(dashboardId, "created"), "Integration", stateOf(integrationId, "created")));
        installedItem = new IotHubInstalledItem();
        installedItem.setId(new IotHubInstalledItemId(UUID.randomUUID()));
        installedItem.setTenantId(tenantId);
        installedItem.setItemId(itemId);
        installedItem.setItemType("DEVICE");
        installedItem.setVersion("1.0.0");
        installedItem.setItemVersionId(installedVersionId);
        installedItem.setDescriptor(descriptor);
        when(installedItemService.findById(tenantId, installedItem.getId())).thenReturn(installedItem);
        when(installedItemService.findByIdForUpdate(tenantId, installedItem.getId())).thenReturn(installedItem);

        ObjectNode versionInfo = JacksonUtil.newObjectNode();
        versionInfo.put("itemId", itemId.toString());
        versionInfo.put("name", "AM308");
        versionInfo.put("version", "1.0.1");
        when(iotHubRestClient.getVersionInfo(versionId)).thenReturn(versionInfo);

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            inTransaction = true;
            try {
                return invocation.<TransactionCallback<?>>getArgument(0).doInTransaction(null);
            } catch (RuntimeException e) {
                rolledBack.add(e);
                throw e;
            } finally {
                inTransaction = false;
            }
        });
        when(packageEntities.checksum(any(), any())).thenReturn("checksum");
        when(entityService.fetchEntity(any(), any())).thenReturn(Optional.empty());
        Device device = new Device(deviceId);
        device.setTenantId(tenantId);
        device.setName("AM308");
        when(deviceService.findDeviceById(tenantId, deviceId)).thenReturn(device);
        when(installedItemService.findByTenantId(any(), any(), any(), any())).thenReturn(new PageData<>());
    }

    @Test
    void overwritesTheEntitiesAndRecordsTheNewVersionInOneTransaction() {
        DeviceProfile profile = profile(3L, "AM308 renamed");
        when(deviceProfileService.findDeviceProfileById(tenantId, profileId)).thenReturn(profile);
        when(deviceProfileService.saveDeviceProfile(any())).thenAnswer(i -> i.getArgument(0));
        Dashboard dashboard = dashboard(7L);
        dashboard.setCustomerId(new CustomerId(UUID.randomUUID()));
        when(dashboardService.findDashboardById(tenantId, dashboardId)).thenReturn(dashboard);
        when(dashboardService.saveDashboard(any(Dashboard.class))).thenAnswer(i -> i.getArgument(0));

        DevicePackageResult result = service.update(user, installedItem.getId(), request(
                overwrite("AM308 profile", "DEVICE_PROFILE", profileId, 3L, JacksonUtil.newObjectNode().put("name", "milesight-iot-am308").put("description", "new")),
                keep("AM308", "DEVICE", deviceId),
                overwrite("Dashboard", "DASHBOARD", dashboardId, 7L, JacksonUtil.newObjectNode().put("title", "Template title"))));

        assertThat(result.isSuccess()).isTrue();
        ArgumentCaptor<DeviceProfile> savedProfile = ArgumentCaptor.forClass(DeviceProfile.class);
        verify(deviceProfileService).saveDeviceProfile(savedProfile.capture());
        // Same id, the name the user gave it, the version they reviewed
        assertThat(savedProfile.getValue().getId()).isEqualTo(profileId);
        assertThat(savedProfile.getValue().getName()).isEqualTo("AM308 renamed");
        assertThat(savedProfile.getValue().getDescription()).isEqualTo("new");
        assertThat(savedProfile.getValue().getVersion()).isEqualTo(3L);
        ArgumentCaptor<Dashboard> savedDashboard = ArgumentCaptor.forClass(Dashboard.class);
        verify(dashboardService).saveDashboard(savedDashboard.capture());
        assertThat(savedDashboard.getValue().getCustomerId()).isEqualTo(dashboard.getCustomerId());

        verify(installedItemService).save(tenantId, installedItem);
        assertThat(installedItem.getVersion()).isEqualTo("1.0.1");
        assertThat(installedItem.getItemVersionId()).isEqualTo(UUID.fromString(versionId));
        DeviceInstalledItemDescriptor descriptor = (DeviceInstalledItemDescriptor) installedItem.getDescriptor();
        // Of the saved dashboard, not read back by id: inside the transaction that may come from the cache
        assertThat(descriptor.getInstallState().get("DASHBOARD:Dashboard").get("checksum").asText())
                .isEqualTo(EntityChecksum.of(savedDashboard.getValue()));
        assertThat(descriptor.getCreatedEntityIds()).contains(profileId, deviceId, dashboardId);
    }

    @Test
    void keptEntity_keepsTheChecksumThePackageLeft() {
        // The user changed the dashboard since: its current checksum must not become the package's
        ObjectNode dashboardState = JacksonUtil.newObjectNode().put("checksum", "installed").put("resolution", "created");
        dashboardState.set("entityOutput", JacksonUtil.newObjectNode().put("id", dashboardId.getId().toString()));
        ((DeviceInstalledItemDescriptor) installedItem.getDescriptor()).setInstallState(Map.of("Dashboard", dashboardState));
        when(dashboardService.findDashboardById(tenantId, dashboardId)).thenReturn(dashboard(9L));
        DevicePackageRequest request = request(keep("Dashboard", "DASHBOARD", dashboardId));
        request.setSelectedInstallMethod("INTEGRATION_CHIRPSTACK");

        DevicePackageResult result = service.update(user, installedItem.getId(), request);

        assertThat(result.isSuccess()).isTrue();
        DeviceInstalledItemDescriptor descriptor = (DeviceInstalledItemDescriptor) installedItem.getDescriptor();
        assertThat(descriptor.getInstallState().get("DASHBOARD:Dashboard").get("checksum").asText()).isEqualTo("installed");
        assertThat(descriptor.getSelectedInstallMethod()).isEqualTo("INTEGRATION_CHIRPSTACK");
        verify(dashboardService, never()).saveDashboard(any(Dashboard.class));
    }

    @Test
    void keptEntityOfAStepWithAnId_keepsItsChecksumUnderTheId() {
        // Installed by a version whose step had no id: the entry is under the step's name
        ObjectNode dashboardState = JacksonUtil.newObjectNode().put("checksum", "installed").put("resolution", "created");
        dashboardState.set("entityOutput", JacksonUtil.newObjectNode().put("id", dashboardId.getId().toString()));
        ((DeviceInstalledItemDescriptor) installedItem.getDescriptor()).setInstallState(Map.of("Dashboard", dashboardState));
        when(dashboardService.findDashboardById(tenantId, dashboardId)).thenReturn(dashboard(9L));
        DevicePackageRequest.Step overview = keep("Overview", "DASHBOARD", dashboardId);
        overview.setId("main-dashboard");

        DevicePackageResult result = service.update(user, installedItem.getId(), request(overview));

        assertThat(result.isSuccess()).isTrue();
        DeviceInstalledItemDescriptor descriptor = (DeviceInstalledItemDescriptor) installedItem.getDescriptor();
        assertThat(descriptor.getInstallState().get("main-dashboard").get("checksum").asText()).isEqualTo("installed");
        assertThat(descriptor.getInstallState()).containsOnlyKeys("main-dashboard");
    }

    @Test
    void keptEntity_doesNotTakeTheChecksumOfAnotherEntity() {
        // The entry under the step's key was recorded for an entity deleted since
        ObjectNode dashboardState = JacksonUtil.newObjectNode().put("checksum", "of-the-deleted-one").put("resolution", "created");
        dashboardState.set("entityOutput", JacksonUtil.newObjectNode().put("id", UUID.randomUUID().toString()));
        ((DeviceInstalledItemDescriptor) installedItem.getDescriptor()).setInstallState(Map.of("Dashboard", dashboardState));
        when(dashboardService.findDashboardById(tenantId, dashboardId)).thenReturn(dashboard(9L));

        DevicePackageResult result = service.update(user, installedItem.getId(), request(keep("Dashboard", "DASHBOARD", dashboardId)));

        assertThat(result.isSuccess()).isTrue();
        DeviceInstalledItemDescriptor descriptor = (DeviceInstalledItemDescriptor) installedItem.getDescriptor();
        assertThat(descriptor.getInstallState().get("DASHBOARD:Dashboard").has("checksum")).isFalse();
    }

    @Test
    void useExistingOfAPackageEntity_staysThePackages() {
        when(dashboardService.findDashboardById(tenantId, dashboardId)).thenReturn(dashboard(9L));
        DevicePackageRequest.Step step = keep("Overview", "DASHBOARD", dashboardId);
        step.setResolution("use-existing");

        DevicePackageResult result = service.update(user, installedItem.getId(), request(step));

        assertThat(result.isSuccess()).isTrue();
        DeviceInstalledItemDescriptor descriptor = (DeviceInstalledItemDescriptor) installedItem.getDescriptor();
        assertThat(descriptor.getInstallState().get("DASHBOARD:Overview").get("resolution").asText()).isEqualTo("created");
        assertThat(descriptor.getCreatedEntityIds()).contains(dashboardId);
    }

    @Test
    void useExistingAtInstall_staysUseExisting() {
        // At install the user picked their own dashboard: the install adds it to createdEntityIds too
        ((DeviceInstalledItemDescriptor) installedItem.getDescriptor()).setInstallState(Map.of("Dashboard", stateOf(dashboardId, "use-existing")));
        when(dashboardService.findDashboardById(tenantId, dashboardId)).thenReturn(dashboard(9L));
        DevicePackageRequest.Step step = keep("Dashboard", "DASHBOARD", dashboardId);
        step.setResolution("use-existing");

        DevicePackageResult result = service.update(user, installedItem.getId(), request(step));

        assertThat(result.isSuccess()).isTrue();
        DeviceInstalledItemDescriptor descriptor = (DeviceInstalledItemDescriptor) installedItem.getDescriptor();
        assertThat(descriptor.getInstallState().get("DASHBOARD:Dashboard").get("resolution").asText()).isEqualTo("use-existing");
    }

    @Test
    void overwritingAnEntityUsedAtInstall_needsAConfirmation() {
        ((DeviceInstalledItemDescriptor) installedItem.getDescriptor()).setInstallState(Map.of("Dashboard", stateOf(dashboardId, "use-existing")));
        DevicePackageRequest.Step step = overwrite("Dashboard", "DASHBOARD", dashboardId, 9L, JacksonUtil.newObjectNode().put("title", "Dashboard"));

        DevicePackageResult result = service.update(user, installedItem.getId(), request(step));

        assertThat(result.isSuccess()).isFalse();
        verify(installedItemService, never()).save(any(), any());
    }

    @Test
    void sharedEntityOverwrittenByAnotherPackage_isNotChangedBySomeoneElse() {
        Dashboard shared = dashboard(9L);
        DevicePackageEntities.Snapshot snapshot = new DevicePackageEntities.Snapshot(shared, null);
        when(packageEntities.load(tenantId, dashboardId)).thenReturn(snapshot);
        when(iotHubService.findKeptEntities(tenantId, installedItem.getId())).thenReturn(new InstalledItemKeptEntities(List.of(), 0, 0, List.of()));
        // This package recorded the dashboard as it left it; the other one overwrote it since and recorded the new one
        ObjectNode own = stateOf(dashboardId, "created").put("checksum", "as-this-package-left-it");
        ((DeviceInstalledItemDescriptor) installedItem.getDescriptor()).setInstallState(Map.of("Dashboard", own));
        IotHubInstalledItem other = otherPackage(Map.of("Dashboard", stateOf(dashboardId, "created").put("checksum", snapshot.checksum())));
        when(installedItemService.findByTenantId(any(), any(), any(), any()))
                .thenReturn(new PageData<>(List.of(installedItem, other), 1, 2, false));

        assertThat(service.check(user, installedItem.getId()).get(0).getModified()).isFalse();

        shared.setConfiguration(JacksonUtil.newObjectNode().put("description", "Changed by hand"));
        assertThat(service.check(user, installedItem.getId()).get(0).getModified()).isTrue();
    }

    @Test
    void entityOfAStepAVersionDropped_staysThePackages() {
        // v2 dropped the dashboard step: the install state no longer has it, createdEntityIds does
        ((DeviceInstalledItemDescriptor) installedItem.getDescriptor()).setInstallState(Map.of());
        when(dashboardService.findDashboardById(tenantId, dashboardId)).thenReturn(dashboard(9L));
        DevicePackageRequest.Step step = keep("Dashboard", "DASHBOARD", dashboardId);
        step.setResolution("use-existing");

        DevicePackageResult result = service.update(user, installedItem.getId(), request(step));

        assertThat(result.isSuccess()).isTrue();
        DeviceInstalledItemDescriptor descriptor = (DeviceInstalledItemDescriptor) installedItem.getDescriptor();
        assertThat(descriptor.getInstallState().get("DASHBOARD:Dashboard").get("resolution").asText()).isEqualTo("created");
    }

    @Test
    void entityUsedAtInstall_staysTheUsersAfterAVersionDropsItsStep() {
        // Recorded at install; the step is gone from the install state since
        DeviceInstalledItemDescriptor installed = (DeviceInstalledItemDescriptor) installedItem.getDescriptor();
        installed.setUsedExistingEntityIds(List.of(dashboardId));
        installed.setInstallState(Map.of());
        DevicePackageRequest.Step step = overwrite("Dashboard", "DASHBOARD", dashboardId, 9L, JacksonUtil.newObjectNode().put("title", "Dashboard"));

        DevicePackageResult result = service.update(user, installedItem.getId(), request(step));

        assertThat(result.isSuccess()).isFalse();
        verify(installedItemService, never()).save(any(), any());
    }

    @Test
    void entitiesUsedAtInstall_areCarriedIntoTheDescriptor() {
        // Installed before the descriptor kept them: the install state has them
        ((DeviceInstalledItemDescriptor) installedItem.getDescriptor()).setInstallState(Map.of("Dashboard", stateOf(dashboardId, "use-existing")));
        when(dashboardService.findDashboardById(tenantId, dashboardId)).thenReturn(dashboard(9L));
        when(entityService.fetchEntity(any(), any())).thenReturn(Optional.of(dashboard(9L)));

        DevicePackageResult result = service.update(user, installedItem.getId(), request(keep("AM308", "DEVICE", deviceId)));

        assertThat(result.isSuccess()).isTrue();
        assertThat(((DeviceInstalledItemDescriptor) installedItem.getDescriptor()).getUsedExistingEntityIds()).containsExactly(dashboardId);
    }

    @Test
    void keptIntegration_getsTheStepsMetadataEntries() {
        // Taken as it was at install: other packages' entries stay, the step's are merged, a null one is removed
        ((DeviceInstalledItemDescriptor) installedItem.getDescriptor()).setInstallState(Map.of("TTN", stateOf(integrationId, "use-existing")));
        Integration current = new Integration();
        current.setId(integrationId);
        current.setTenantId(tenantId);
        current.setName("TTN my-app");
        ObjectNode configuration = JacksonUtil.newObjectNode();
        configuration.set("metadata", JacksonUtil.newObjectNode().put("profile.other-model", "Other").put("profile.milesight-iot-am308", "AM308"));
        current.setConfiguration(configuration);
        when(integrationService.findIntegrationById(tenantId, integrationId)).thenReturn(current);
        when(integrationService.saveIntegration(any())).thenAnswer(i -> i.getArgument(0));
        DevicePackageRequest.Step step = keep("TTN", "INTEGRATION", integrationId);
        step.setResolution("use-existing");
        ObjectNode entries = JacksonUtil.newObjectNode().put("profile.lansitec-badge", "Badge");
        entries.putNull("profile.milesight-iot-am308");
        step.setMetadata(entries);

        DevicePackageResult result = service.update(user, installedItem.getId(), request(step));

        assertThat(result.isSuccess()).isTrue();
        ArgumentCaptor<Integration> saved = ArgumentCaptor.forClass(Integration.class);
        verify(integrationService).saveIntegration(saved.capture());
        JsonNode metadata = saved.getValue().getConfiguration().get("metadata");
        assertThat(metadata.get("profile.other-model").asText()).isEqualTo("Other");
        assertThat(metadata.get("profile.lansitec-badge").asText()).isEqualTo("Badge");
        assertThat(metadata.has("profile.milesight-iot-am308")).isFalse();
        // A copy is saved: the integration read, possibly the cache's own instance, stays as it is until the commit
        assertThat(saved.getValue()).isNotSameAs(current);
        assertThat(current.getConfiguration().get("metadata").has("profile.lansitec-badge")).isFalse();
        JsonNode state = ((DeviceInstalledItemDescriptor) installedItem.getDescriptor()).getInstallState().get("INTEGRATION:TTN");
        assertThat(state.get("resolution").asText()).isEqualTo("use-existing");
        // The entries the step added, which a delete of the package removes; not the one it removed
        assertThat(state.get("metadata")).isEqualTo(JacksonUtil.newObjectNode().put("profile.lansitec-badge", "Badge"));
    }

    @Test
    void integrationOfAStepAVersionDropped_keepsItsMetadataEntries() {
        // Taken as it was at install with the package's entries; the new version has no step for it
        ObjectNode installed = stateOf(integrationId, "use-existing");
        installed.set("metadata", JacksonUtil.newObjectNode().put("profile.milesight-iot-am308", "AM308"));
        ((DeviceInstalledItemDescriptor) installedItem.getDescriptor()).setInstallState(Map.of("TTN", installed));
        Integration integration = new Integration();
        integration.setId(integrationId);
        when(entityService.fetchEntity(tenantId, integrationId)).thenReturn(Optional.of(integration));

        DevicePackageResult result = service.update(user, installedItem.getId(), request(keep("AM308", "DEVICE", deviceId)));

        assertThat(result.isSuccess()).isTrue();
        DeviceInstalledItemDescriptor descriptor = (DeviceInstalledItemDescriptor) installedItem.getDescriptor();
        assertThat(descriptor.getCreatedEntityIds()).contains(integrationId);
        // A later delete that keeps the integration removes these entries
        assertThat(descriptor.getInstallState().get("TTN").get("metadata"))
                .isEqualTo(JacksonUtil.newObjectNode().put("profile.milesight-iot-am308", "AM308"));
    }

    @Test
    void integrationUsedByAStepNewToThePackage_isListedAsUsedAsItWas() {
        // The installed version has no integration step; the new one takes the tenant's integration as it is
        DeviceInstalledItemDescriptor installed = (DeviceInstalledItemDescriptor) installedItem.getDescriptor();
        installed.setCreatedEntityIds(List.of(profileId, deviceId, dashboardId));
        installed.setInstallState(Map.of("Profile", stateOf(profileId, "created"), "AM308", stateOf(deviceId, "created"),
                "Dashboard", stateOf(dashboardId, "created")));
        IntegrationId tenantsIntegrationId = new IntegrationId(UUID.randomUUID());
        Integration current = new Integration();
        current.setId(tenantsIntegrationId);
        current.setTenantId(tenantId);
        current.setName("TTN my-app");
        ObjectNode configuration = JacksonUtil.newObjectNode();
        configuration.set("metadata", JacksonUtil.newObjectNode().put("profile.other-model", "Other"));
        current.setConfiguration(configuration);
        when(integrationService.findIntegrationById(tenantId, tenantsIntegrationId)).thenReturn(current);
        when(integrationService.saveIntegration(any())).thenAnswer(i -> i.getArgument(0));
        DevicePackageRequest.Step step = keep("TTN", "INTEGRATION", tenantsIntegrationId);
        step.setResolution("use-existing");
        step.setMetadata(JacksonUtil.newObjectNode().put("profile.milesight-iot-am308", "AM308"));

        DevicePackageResult result = service.update(user, installedItem.getId(), request(step));

        assertThat(result.isSuccess()).isTrue();
        DeviceInstalledItemDescriptor descriptor = (DeviceInstalledItemDescriptor) installedItem.getDescriptor();
        // A delete walks createdEntityIds and keeps what usedExistingEntityIds lists, removing only the package's
        // entries from it (DefaultIotHubServiceTest#deleteInstalledItem_devicePackage_removesItsMetadataEntriesFromTheIntegrationItTookAsItWas)
        assertThat(descriptor.getCreatedEntityIds()).containsOnlyOnce(tenantsIntegrationId);
        assertThat(descriptor.getUsedExistingEntityIds()).containsExactly(tenantsIntegrationId);
        JsonNode state = descriptor.getInstallState().get("INTEGRATION:TTN");
        assertThat(state.get("resolution").asText()).isEqualTo("use-existing");
        assertThat(state.get("metadata")).isEqualTo(JacksonUtil.newObjectNode().put("profile.milesight-iot-am308", "AM308"));
    }

    @Test
    void keptIntegrationWithTheEntriesItHas_isNotSaved() {
        Integration current = new Integration();
        current.setId(integrationId);
        current.setTenantId(tenantId);
        current.setName("TTN my-app");
        ObjectNode configuration = JacksonUtil.newObjectNode();
        configuration.set("metadata", JacksonUtil.newObjectNode().put("profile.lansitec-badge", "Badge"));
        current.setConfiguration(configuration);
        when(integrationService.findIntegrationById(tenantId, integrationId)).thenReturn(current);
        DevicePackageRequest.Step step = keep("TTN", "INTEGRATION", integrationId);
        step.setMetadata(JacksonUtil.newObjectNode().put("profile.lansitec-badge", "Badge"));

        DevicePackageResult result = service.update(user, installedItem.getId(), request(step));

        assertThat(result.isSuccess()).isTrue();
        verify(integrationService, never()).saveIntegration(any());
    }

    @Test
    void twoStepsWithOneKey_areRejected() {
        DevicePackageRequest.Step first = keep("Overview", "DASHBOARD", dashboardId);
        first.setId("main-dashboard");
        DevicePackageRequest.Step second = keep("Details", "DASHBOARD", dashboardId);
        second.setId("main-dashboard");

        DevicePackageResult result = service.update(user, installedItem.getId(), request(first, second));

        assertThat(result.isSuccess()).isFalse();
        verify(installedItemService, never()).save(any(), any());
    }

    @Test
    void twoStepsOfOneTypeAndName_areRejected() {
        DevicePackageResult result = service.update(user, installedItem.getId(), request(
                keep("Dashboard", "DASHBOARD", dashboardId), keep("Dashboard", "DASHBOARD", dashboardId)));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Duplicate step: DASHBOARD:Dashboard");
        verify(transactionTemplate, never()).execute(any());
    }

    @Test
    void stepWithTheKeyOfAForm_isRejected() {
        DevicePackageRequest.Step step = keep("Dashboard", "DASHBOARD", dashboardId);
        step.setId("Settings");
        DevicePackageRequest request = request(step);
        request.setInstallState(Map.of("Settings", JacksonUtil.newObjectNode().set("formValues", JacksonUtil.newObjectNode().put("interval", 10))));

        DevicePackageResult result = service.update(user, installedItem.getId(), request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Duplicate step: Settings");
        verify(transactionTemplate, never()).execute(any());
    }

    @Test
    void stepsOfOneNameAndAnotherType_eachKeepTheirOwnState() {
        // Installed when the state was keyed by name: the integration's state replaced the converter's
        ConverterId converterId = new ConverterId(UUID.randomUUID());
        Converter converter = new Converter(converterId);
        converter.setTenantId(tenantId);
        converter.setName("TTN my-app");
        when(converterService.findConverterById(tenantId, converterId)).thenReturn(converter);
        Integration integration = new Integration(integrationId);
        integration.setTenantId(tenantId);
        integration.setName("TTN my-app");
        ObjectNode configuration = JacksonUtil.newObjectNode();
        configuration.set("metadata", JacksonUtil.newObjectNode().put("profile.milesight-iot-am308", "AM308"));
        integration.setConfiguration(configuration);
        when(integrationService.findIntegrationById(tenantId, integrationId)).thenReturn(integration);
        DeviceInstalledItemDescriptor installed = (DeviceInstalledItemDescriptor) installedItem.getDescriptor();
        installed.setCreatedEntityIds(List.of(profileId, deviceId, converterId, integrationId));
        ObjectNode legacy = stateOf(integrationId, "created").put("checksum", "installed");
        legacy.set("metadata", JacksonUtil.newObjectNode().put("profile.milesight-iot-am308", "AM308"));
        installed.setInstallState(Map.of("${integrationName}", legacy, "${profileName}", stateOf(profileId, "created"),
                "Configuration", JacksonUtil.newObjectNode().set("formValues", JacksonUtil.newObjectNode().put("integrationName", "TTN my-app"))));
        DevicePackageRequest.Step uplink = keep("${integrationName}", "UPLINK_CONVERTER", converterId);
        uplink.setDefaultConverter(true);
        DevicePackageRequest.Step ttn = keep("${integrationName}", "INTEGRATION", integrationId);
        ttn.setMetadata(JacksonUtil.newObjectNode().put("profile.milesight-iot-am308", "AM308"));
        DevicePackageRequest request = request(uplink, ttn);
        request.setInstallState(Map.of("Configuration", installed.getInstallState().get("Configuration")));

        DevicePackageResult result = service.update(user, installedItem.getId(), request);

        assertThat(result.isSuccess()).isTrue();
        Map<String, JsonNode> state = ((DeviceInstalledItemDescriptor) installedItem.getDescriptor()).getInstallState();
        // The state under the name is gone: each step's is under its own key now
        assertThat(state).containsOnlyKeys("Configuration", "UPLINK_CONVERTER:${integrationName}", "INTEGRATION:${integrationName}");
        JsonNode converterState = state.get("UPLINK_CONVERTER:${integrationName}");
        assertThat(converterState.get("entityOutput").get("id").asText()).isEqualTo(converterId.getId().toString());
        assertThat(converterState.get("defaultConverter").asBoolean()).isTrue();
        JsonNode integrationState = state.get("INTEGRATION:${integrationName}");
        assertThat(integrationState.get("entityOutput").get("id").asText()).isEqualTo(integrationId.getId().toString());
        assertThat(integrationState.get("checksum").asText()).isEqualTo("installed");
        assertThat(integrationState.get("metadata")).isEqualTo(JacksonUtil.newObjectNode().put("profile.milesight-iot-am308", "AM308"));
        assertThat(result.getOutputs().get("UPLINK_CONVERTER:${integrationName}").get("id").asText()).isEqualTo(converterId.getId().toString());
        assertThat(result.getOutputs().get("INTEGRATION:${integrationName}").get("id").asText()).isEqualTo(integrationId.getId().toString());
    }

    @Test
    void failedStep_rollsBackAndReportsTheStep() {
        when(deviceProfileService.findDeviceProfileById(tenantId, profileId)).thenReturn(profile(3L, "AM308"));
        when(deviceProfileService.saveDeviceProfile(any())).thenAnswer(i -> i.getArgument(0));
        when(dashboardService.findDashboardById(tenantId, dashboardId)).thenReturn(dashboard(7L));
        when(dashboardService.saveDashboard(any(Dashboard.class))).thenThrow(new DataValidationException("Dashboards limit reached"));

        DevicePackageResult result = service.update(user, installedItem.getId(), request(
                overwrite("AM308 profile", "DEVICE_PROFILE", profileId, 3L, JacksonUtil.newObjectNode().put("name", "p")),
                overwrite("Dashboard", "DASHBOARD", dashboardId, 7L, JacksonUtil.newObjectNode().put("title", "d"))));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailedStep()).isEqualTo("DASHBOARD:Dashboard");
        assertThat(result.getErrorMessage()).isEqualTo("Dashboards limit reached");
        assertThat(rolledBack).hasSize(1);
        verify(installedItemService, never()).save(any(), any());
        verifyNoInteractions(logEntityActionService);
        assertThat(installedItem.getVersion()).isEqualTo("1.0.0");
    }

    @Test
    void entityChangedSinceTheCheck_isAVersionConflict() {
        when(dashboardService.findDashboardById(tenantId, dashboardId)).thenReturn(dashboard(8L));

        DevicePackageResult result = service.update(user, installedItem.getId(), request(
                overwrite("Dashboard", "DASHBOARD", dashboardId, 7L, JacksonUtil.newObjectNode().put("title", "d"))));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailedStep()).isEqualTo("DASHBOARD:Dashboard");
        assertThat(result.getErrorMessage()).contains("Check the package again");
        verify(dashboardService, never()).saveDashboard(any(Dashboard.class));
    }

    @Test
    void integrationGetsTheIdOfTheConverterCreatedInTheSameUpdate() throws Exception {
        ConverterId converterId = new ConverterId(UUID.randomUUID());
        when(converterService.saveConverter(any())).thenAnswer(i -> {
            Converter converter = i.getArgument(0);
            converter.setId(converterId);
            return converter;
        });
        Integration current = new Integration();
        current.setId(integrationId);
        current.setTenantId(tenantId);
        current.setName("TTN my-app");
        current.setRoutingKey("routing-key");
        current.setVersion(2L);
        when(integrationService.findIntegrationById(tenantId, integrationId)).thenReturn(current);
        when(integrationService.saveIntegration(any())).thenAnswer(i -> i.getArgument(0));
        ObjectNode integration = JacksonUtil.newObjectNode().put("name", "Template name").put("type", "TTN").put("edgeTemplate", true);
        integration.set("defaultConverterId", JacksonUtil.newObjectNode().put("entityType", "CONVERTER").put("id", "${uplinkConverter.id}"));

        DevicePackageResult result = service.update(user, installedItem.getId(), request(
                create("Uplink", "UPLINK_CONVERTER", JacksonUtil.newObjectNode().put("name", "Uplink for TTN")),
                overwrite("TTN", "INTEGRATION", integrationId, 2L, integration)));

        assertThat(result.isSuccess()).isTrue();
        ArgumentCaptor<Integration> saved = ArgumentCaptor.forClass(Integration.class);
        verify(integrationService).saveIntegration(saved.capture());
        assertThat(saved.getValue().getDefaultConverterId()).isEqualTo(converterId);
        // The devices keep sending to the same endpoint
        assertThat(saved.getValue().getRoutingKey()).isEqualTo("routing-key");
        assertThat(saved.getValue().getName()).isEqualTo("TTN my-app");
        assertThat(result.getOutputs().get("INTEGRATION:TTN").get("routingKey").asText()).isEqualTo("routing-key");
    }

    @Test
    void overwrittenSharedConverter_isRecordedAsDefaultAndOnlyThePackagesOwnRowIsWritten() {
        ConverterId converterId = new ConverterId(UUID.randomUUID());
        Converter current = new Converter(converterId);
        current.setTenantId(tenantId);
        current.setName("Uplink data converter for TTN my-app");
        current.setVersion(4L);
        when(converterService.findConverterById(tenantId, converterId)).thenReturn(current);
        when(converterService.saveConverter(any())).thenAnswer(i -> i.getArgument(0));
        // The package that created the converter records its checksum; one that took it as it was records none
        ObjectNode created = JacksonUtil.newObjectNode().put("checksum", "before").put("resolution", "created");
        created.set("entityOutput", JacksonUtil.newObjectNode().put("id", converterId.getId().toString()));
        ObjectNode usedExisting = JacksonUtil.newObjectNode().put("resolution", "use-existing");
        usedExisting.set("entityOutput", JacksonUtil.newObjectNode().put("id", converterId.getId().toString()));
        IotHubInstalledItem creator = otherPackage(Map.of("Uplink", created));
        IotHubInstalledItem taker = otherPackage(Map.of("Uplink", usedExisting));
        when(installedItemService.findByTenantId(any(), any(), any(), any()))
                .thenReturn(new PageData<>(List.of(installedItem, creator, taker), 1, 3, false));
        DevicePackageRequest.Step step = overwrite("Uplink", "UPLINK_CONVERTER", converterId, 4L,
                JacksonUtil.newObjectNode().put("name", "Uplink").set("configuration", JacksonUtil.newObjectNode().put("tbelDecoder", "return {};")));
        step.setConfirmed(true);
        step.setDefaultConverter(true);

        DevicePackageResult result = service.update(user, installedItem.getId(), request(step));

        assertThat(result.isSuccess()).isTrue();
        JsonNode state = ((DeviceInstalledItemDescriptor) installedItem.getDescriptor()).getInstallState().get("UPLINK_CONVERTER:Uplink");
        assertThat(state.get("defaultConverter").asBoolean()).isTrue();
        // The other packages keep what they recorded: check() takes the checksum of any installed package
        assertThat(created.get("checksum").asText()).isEqualTo("before");
        verify(installedItemService, never()).save(tenantId, creator);
        verify(installedItemService, never()).save(tenantId, taker);
    }

    @Test
    void updateOfTheInstalledVersion_keepsTheInstallStateOfTheStepsItLeavesOut() {
        ConverterId converterId = new ConverterId(UUID.randomUUID());
        Converter current = new Converter(converterId);
        current.setTenantId(tenantId);
        current.setName("Uplink data converter for TTN my-app");
        current.setVersion(4L);
        when(converterService.findConverterById(tenantId, converterId)).thenReturn(current);
        when(converterService.saveConverter(any())).thenAnswer(i -> i.getArgument(0));
        DeviceInstalledItemDescriptor installed = (DeviceInstalledItemDescriptor) installedItem.getDescriptor();
        installed.setCreatedEntityIds(List.of(profileId, deviceId, dashboardId, integrationId, converterId));
        installed.setInstallState(Map.of("Integration", stateOf(integrationId, "created"), "uplink", stateOf(converterId, "created"),
                "Configuration", JacksonUtil.newObjectNode().set("formValues", JacksonUtil.newObjectNode().put("profileName", "am308"))));
        ObjectNode versionInfo = JacksonUtil.newObjectNode().put("itemId", itemId.toString()).put("name", "AM308").put("version", "1.0.0");
        when(iotHubRestClient.getVersionInfo(installedVersionId.toString())).thenReturn(versionInfo);
        // ThingsBoard's default converter changed: only the converter step is sent
        DevicePackageRequest.Step step = overwrite("Uplink data converter", "UPLINK_CONVERTER", converterId, 4L,
                JacksonUtil.newObjectNode().put("name", "Uplink").set("configuration", JacksonUtil.newObjectNode().put("tbelDecoder", "return {};")));
        step.setId("uplink");
        step.setDefaultConverter(true);
        DevicePackageRequest request = request(step);
        request.setVersionId(installedVersionId.toString());

        DevicePackageResult result = service.update(user, installedItem.getId(), request);

        assertThat(result.isSuccess()).isTrue();
        Map<String, JsonNode> state = ((DeviceInstalledItemDescriptor) installedItem.getDescriptor()).getInstallState();
        ArgumentCaptor<Converter> saved = ArgumentCaptor.forClass(Converter.class);
        verify(converterService).saveConverter(saved.capture());
        assertThat(state.get("uplink").get("checksum").asText()).isEqualTo(EntityChecksum.of(saved.getValue()));
        assertThat(state.get("uplink").get("defaultConverter").asBoolean()).isTrue();
        assertThat(state.get("Integration")).isEqualTo(stateOf(integrationId, "created"));
        assertThat(state.get("Configuration").get("formValues").get("profileName").asText()).isEqualTo("am308");
        verify(integrationService, never()).saveIntegration(any());
        assertThat(installedItem.getItemVersionId()).isEqualTo(installedVersionId);
    }

    @Test
    void updateToAnotherVersion_dropsTheInstallStateOfTheStepsItLeavesOut() {
        when(dashboardService.findDashboardById(tenantId, dashboardId)).thenReturn(dashboard(9L));

        DevicePackageResult result = service.update(user, installedItem.getId(), request(keep("Dashboard", "DASHBOARD", dashboardId)));

        assertThat(result.isSuccess()).isTrue();
        assertThat(((DeviceInstalledItemDescriptor) installedItem.getDescriptor()).getInstallState()).containsOnlyKeys("DASHBOARD:Dashboard");
    }

    @Test
    void overwritingAnEntityOfAnotherPackage_needsAConfirmation() {
        DashboardId foreign = new DashboardId(UUID.randomUUID());

        DevicePackageResult result = service.update(user, installedItem.getId(), request(
                overwrite("Dashboard", "DASHBOARD", foreign, 1L, JacksonUtil.newObjectNode().put("title", "d"))));

        assertThat(result.isSuccess()).isFalse();
        verify(transactionTemplate, never()).execute(any());
    }

    @Test
    void theDeviceIsNeverOverwritten() {
        DevicePackageResult result = service.update(user, installedItem.getId(), request(
                overwrite("AM308", "DEVICE", deviceId, 1L, JacksonUtil.newObjectNode().put("name", "d"))));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("The device and its credentials don't change on update");
        verify(transactionTemplate, never()).execute(any());
    }

    @Test
    void versionOfAnotherItem_isRejected() {
        ObjectNode other = JacksonUtil.newObjectNode().put("itemId", UUID.randomUUID().toString());
        when(iotHubRestClient.getVersionInfo(versionId)).thenReturn(other);

        DevicePackageResult result = service.update(user, installedItem.getId(), request(keep("AM308", "DEVICE", deviceId)));

        assertThat(result.isSuccess()).isFalse();
        verify(transactionTemplate, never()).execute(any());
    }

    @Test
    void anotherUpdateCommittedMeanwhile_failsWithoutChanges() {
        // E.g. a second click: it waits for the first one's lock and finds the package on another version
        IotHubInstalledItem updated = new IotHubInstalledItem();
        updated.setId(installedItem.getId());
        updated.setItemVersionId(UUID.fromString(versionId));
        when(installedItemService.findByIdForUpdate(tenantId, installedItem.getId())).thenReturn(updated);

        DevicePackageResult result = service.update(user, installedItem.getId(), request(
                create("Uplink", "UPLINK_CONVERTER", JacksonUtil.newObjectNode().put("name", "Uplink for TTN"))));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Check the update again");
        verify(converterService, never()).saveConverter(any());
        verify(installedItemService, never()).save(any(), any());
    }

    @Test
    void planOfAnotherInstalledVersion_isRejected() {
        DevicePackageRequest request = request(keep("AM308", "DEVICE", deviceId));
        request.setFromVersionId(UUID.randomUUID());

        DevicePackageResult result = service.update(user, installedItem.getId(), request);

        assertThat(result.isSuccess()).isFalse();
        verify(installedItemService, never()).save(any(), any());
    }

    @Test
    void integrationsNotInThePlan_areRefused() {
        doThrow(new SubscriptionException("Integrations feature is disabled!", SubscriptionErrorCode.FEATURE_DISABLED))
                .when(subscriptionService).checkFeatureAllowed(tenantId, PlatformFeature.INTEGRATIONS);

        DevicePackageResult result = service.update(user, installedItem.getId(), request(
                create("Uplink", "UPLINK_CONVERTER", JacksonUtil.newObjectNode().put("name", "Uplink for TTN"))));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Integrations feature is disabled!");
        verify(transactionTemplate, never()).execute(any());
    }

    @Test
    void attributesOfAnOverwrittenEntity_areSavedAfterCommit() {
        when(dashboardService.findDashboardById(tenantId, dashboardId)).thenReturn(dashboard(7L));
        when(dashboardService.saveDashboard(any(Dashboard.class))).thenAnswer(i -> i.getArgument(0));
        DevicePackageRequest.Step step = overwrite("Dashboard", "DASHBOARD", dashboardId, 7L, JacksonUtil.newObjectNode().put("title", "d"));
        step.setServerAttributes(JacksonUtil.newObjectNode().put("deviceId", "${device.id}"));

        DevicePackageResult result = service.update(user, installedItem.getId(), request(keep("AM308", "DEVICE", deviceId), step));

        assertThat(result.isSuccess()).isTrue();
        ArgumentCaptor<AttributesSaveRequest> saved = ArgumentCaptor.forClass(AttributesSaveRequest.class);
        verify(telemetryService).saveAttributes(saved.capture());
        assertThat(saved.getValue().getEntityId()).isEqualTo(dashboardId);
        assertThat(saved.getValue().getScope()).isEqualTo(AttributeScope.SERVER_SCOPE);
        assertThat(saved.getValue().getEntries().get(0).getValueAsString()).isEqualTo(deviceId.getId().toString());
    }

    @Test
    void checksum_ignoresDebugSettingsAndTheDefaultProfile() {
        Converter converter = new Converter();
        converter.setName("Uplink");
        converter.setConfiguration(JacksonUtil.newObjectNode().put("decoder", "return {};"));
        String installed = EntityChecksum.of(converter);
        converter.setDebugMode(true);
        converter.setDebugSettings(DebugSettings.all());
        assertThat(EntityChecksum.of(converter)).isEqualTo(installed);
        converter.setConfiguration(JacksonUtil.newObjectNode().put("decoder", "return {a: 1};"));
        assertThat(EntityChecksum.of(converter)).isNotEqualTo(installed);

        DeviceProfile profile = profile(1L, "AM308");
        String profileChecksum = EntityChecksum.of(profile);
        profile.setDefault(true);
        assertThat(EntityChecksum.of(profile)).isEqualTo(profileChecksum);
    }

    @Test
    void checksum_ignoresTheMetadataOfAnIntegration() {
        Integration integration = new Integration();
        integration.setName("TTN my-app");
        ObjectNode configuration = JacksonUtil.newObjectNode().put("host", "eu1.cloud.thethings.network");
        configuration.set("metadata", JacksonUtil.newObjectNode());
        integration.setConfiguration(configuration);
        String installed = EntityChecksum.of(integration);
        // Another package on the integration adds its profile name
        ((ObjectNode) configuration.get("metadata")).put("profile.milesight-iot-am308", "AM308");
        assertThat(EntityChecksum.of(integration)).isEqualTo(installed);
        assertThat(integration.getConfiguration().get("metadata").has("profile.milesight-iot-am308")).isTrue();
        configuration.put("host", "nam1.cloud.thethings.network");
        assertThat(EntityChecksum.of(integration)).isNotEqualTo(installed);
    }

    @Test
    void userMessage_showsValidationMessagesAndHidesInternalOnes() {
        assertThat(DefaultDevicePackageService.userMessage(new DataValidationException("Rule chains limit reached")))
                .isEqualTo("Rule chains limit reached");
        assertThat(DefaultDevicePackageService.userMessage(new EntityVersionMismatchException(EntityType.DASHBOARD, null)))
                .contains("Check the package again");
        assertThat(DefaultDevicePackageService.userMessage(new IllegalStateException("could not execute statement; SQL [n/a]")))
                .isEqualTo("Failed to save the device package");
    }

    @Test
    void resolvePlaceholders_leavesUnknownAliasesAsTheyAre() {
        ObjectNode device = JacksonUtil.newObjectNode().put("id", "123");
        ObjectNode node = JacksonUtil.newObjectNode().put("a", "${device.id}").put("b", "x ${device.id} ${gateway.id}");

        ObjectNode resolved = (ObjectNode) DefaultDevicePackageService.resolvePlaceholders(node, Map.of("device", device));

        assertThat(resolved.get("a").asText()).isEqualTo("123");
        assertThat(resolved.get("b").asText()).isEqualTo("x 123 ${gateway.id}");
    }

    @Test
    void installSavesTheEntitiesAndTheInstalledItemInOneTransaction() {
        mockInstall();
        DevicePackageRequest.Step integrationStep = keep("TTN", "INTEGRATION", integrationId);
        integrationStep.setResolution("use-existing");

        DevicePackageResult result = service.install(user, request(
                create("AM308 profile", "DEVICE_PROFILE", JacksonUtil.newObjectNode().put("name", "milesight-iot-am308")),
                integrationStep,
                create("Dashboard", "DASHBOARD", JacksonUtil.newObjectNode().put("title", "AM308 dashboard"))));

        assertThat(result.isSuccess()).isTrue();
        verify(transactionTemplate).execute(any());
        assertThat(savedInTransaction).containsExactly("DEVICE_PROFILE", "DASHBOARD", "INSTALLED_ITEM");
        ArgumentCaptor<IotHubInstalledItem> saved = ArgumentCaptor.forClass(IotHubInstalledItem.class);
        verify(installedItemService).save(eq(tenantId), saved.capture());
        IotHubInstalledItem item = saved.getValue();
        assertThat(item.getTenantId()).isEqualTo(tenantId);
        assertThat(item.getItemId()).isEqualTo(itemId);
        assertThat(item.getItemVersionId()).isEqualTo(UUID.fromString(versionId));
        assertThat(item.getVersion()).isEqualTo("1.0.1");
        assertThat(item.getItemName()).isEqualTo("AM308");
        assertThat(item.getItemType()).isEqualTo("DEVICE");
        DeviceInstalledItemDescriptor descriptor = (DeviceInstalledItemDescriptor) item.getDescriptor();
        assertThat(descriptor.getCreatedEntityIds()).containsExactly(profileId, integrationId, dashboardId);
        assertThat(descriptor.getUsedExistingEntityIds()).containsExactly(integrationId);
        assertThat(descriptor.getDashboardId()).isEqualTo(dashboardId);
        Map<String, JsonNode> state = descriptor.getInstallState();
        assertThat(state.get("DEVICE_PROFILE:AM308 profile").get("entityOutput").get("id").asText()).isEqualTo(profileId.getId().toString());
        assertThat(state.get("DEVICE_PROFILE:AM308 profile").get("checksum").isTextual()).isTrue();
        assertThat(state.get("DASHBOARD:Dashboard").get("entityOutput").get("id").asText()).isEqualTo(dashboardId.getId().toString());
        assertThat(state.get("DASHBOARD:Dashboard").get("checksum").isTextual()).isTrue();
        assertThat(state.get("INTEGRATION:TTN").get("entityOutput").get("id").asText()).isEqualTo(integrationId.getId().toString());
        assertThat(state.get("INTEGRATION:TTN").get("resolution").asText()).isEqualTo("use-existing");
        assertThat(result.getDescriptor()).isSameAs(descriptor);
        assertThat(result.getOutputs()).containsOnlyKeys("DEVICE_PROFILE:AM308 profile", "INTEGRATION:TTN", "DASHBOARD:Dashboard");
        assertThat(afterCommitCalls).containsExactly("AUDIT", "AUDIT", "REPORT");
    }

    @Test
    void installFailureOnALaterStepSavesNothing() {
        mockInstall();
        when(dashboardService.saveDashboard(any(Dashboard.class))).thenThrow(new DataValidationException("Dashboards limit reached"));

        DevicePackageResult result = service.install(user, request(
                create("AM308 profile", "DEVICE_PROFILE", JacksonUtil.newObjectNode().put("name", "milesight-iot-am308")),
                create("Dashboard", "DASHBOARD", JacksonUtil.newObjectNode().put("title", "AM308 dashboard"))));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailedStep()).isEqualTo("DASHBOARD:Dashboard");
        assertThat(result.getErrorMessage()).isEqualTo("Dashboards limit reached");
        assertThat(rolledBack).hasSize(1);
        verify(installedItemService, never()).save(any(), any());
        verifyNoInteractions(logEntityActionService, telemetryService);
        verify(iotHubRestClient, never()).reportVersionInstalled(any(), any());
    }

    @Test
    void installOverwriteOfAnExistingEntityNeedsAConfirmation() {
        mockInstall();
        when(dashboardService.findDashboardById(tenantId, dashboardId)).thenReturn(dashboard(7L));

        DevicePackageResult result = service.install(user, request(
                overwrite("Dashboard", "DASHBOARD", dashboardId, 7L, JacksonUtil.newObjectNode().put("title", "d"))));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Overwriting an entity of another package needs a confirmation: Dashboard");
        verify(transactionTemplate, never()).execute(any());
        verify(dashboardService, never()).saveDashboard(any(Dashboard.class));
        verify(installedItemService, never()).save(any(), any());
    }

    @Test
    void installWithBadIntegrationCredentialsSavesNothing() throws Exception {
        mockInstall();
        doThrow(new ExecutionException(new IllegalStateException("Wrong API key")))
                .when(tbIntegrationService).validateIntegrationConfiguration(eq(tenantId), any());

        DevicePackageResult result = service.install(user, request(
                create("TTN", "INTEGRATION", JacksonUtil.newObjectNode().put("name", "TTN").put("type", "TTN"))));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailedStep()).isEqualTo("INTEGRATION:TTN");
        assertThat(result.getErrorMessage()).isEqualTo("Wrong API key");
        verify(transactionTemplate, never()).execute(any());
        verify(installedItemService, never()).save(any(), any());
    }

    @Test
    void installWhenHubIsUnavailableSavesNothing() {
        mockInstall();
        when(iotHubRestClient.getVersionInfo(versionId)).thenThrow(new RestClientException("Connection refused"));

        DevicePackageResult result = service.install(user, request(
                create("Dashboard", "DASHBOARD", JacksonUtil.newObjectNode().put("title", "AM308 dashboard"))));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("IoT Hub is unavailable. Try again later.");
        verify(transactionTemplate, never()).execute(any());
        verify(installedItemService, never()).save(any(), any());
    }

    @Test
    void installReportsToTheHubAfterCommitAndIgnoresAReportFailure() {
        mockInstall();
        doAnswer(i -> {
            recordAfterCommit("REPORT");
            throw new RestClientException("Connection refused");
        }).when(iotHubRestClient).reportVersionInstalled(eq(versionId), any());

        DevicePackageResult result = service.install(user, request(
                create("Dashboard", "DASHBOARD", JacksonUtil.newObjectNode().put("title", "AM308 dashboard"))));

        assertThat(result.isSuccess()).isTrue();
        InOrder order = inOrder(installedItemService, logEntityActionService, iotHubRestClient);
        order.verify(installedItemService).save(eq(tenantId), any());
        order.verify(logEntityActionService).logEntityAction(eq(tenantId), eq(dashboardId), any(), any(), eq(ActionType.ADDED), eq(user));
        order.verify(iotHubRestClient).reportVersionInstalled(eq(versionId), any());
        assertThat(afterCommitCalls).containsExactly("AUDIT", "REPORT");
    }

    @Test
    void installRejectsAFromVersion() {
        mockInstall();
        DevicePackageRequest request = request(create("Dashboard", "DASHBOARD", JacksonUtil.newObjectNode().put("title", "d")));
        request.setFromVersionId(installedVersionId);

        DevicePackageResult result = service.install(user, request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Invalid install request");
        verify(transactionTemplate, never()).execute(any());
        verify(installedItemService, never()).save(any(), any());
    }

    @Test
    void installKeepStepMustUseExisting() {
        mockInstall();
        DevicePackageRequest.Step integrationStep = keep("TTN", "INTEGRATION", integrationId);
        integrationStep.setResolution("created");

        DevicePackageResult result = service.install(user, request(integrationStep));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Invalid install step: TTN");
        verify(transactionTemplate, never()).execute(any());
        verify(installedItemService, never()).save(any(), any());
    }

    @Test
    void installRejectsAMalformedVersionId() {
        mockInstall();
        DevicePackageRequest request = request(create("Dashboard", "DASHBOARD", JacksonUtil.newObjectNode().put("title", "d")));
        request.setVersionId("not-a-uuid");

        DevicePackageResult result = service.install(user, request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Invalid install request");
        verify(iotHubRestClient, never()).getVersionInfo(any());
        verify(transactionTemplate, never()).execute(any());
    }

    @Test
    void installAssignsTheCustomerToTheDeviceAndTheDashboard() {
        mockInstall();
        CustomerId customerId = customerOf(tenantId);
        DevicePackageRequest request = request(
                create("AM308", "DEVICE", JacksonUtil.newObjectNode().put("name", "AM308")),
                create("Dashboard", "DASHBOARD", JacksonUtil.newObjectNode().put("title", "AM308 dashboard")));
        request.setCustomerId(customerId.getId());

        DevicePackageResult result = service.install(user, request);

        assertThat(result.isSuccess()).isTrue();
        ArgumentCaptor<Device> device = ArgumentCaptor.forClass(Device.class);
        verify(deviceService).saveDevice(device.capture());
        assertThat(device.getValue().getCustomerId()).isEqualTo(customerId);
        ArgumentCaptor<Dashboard> dashboard = ArgumentCaptor.forClass(Dashboard.class);
        verify(dashboardService).saveDashboard(dashboard.capture());
        assertThat(dashboard.getValue().getCustomerId()).isEqualTo(customerId);
    }

    @Test
    void installAddsTheDeviceToTheEntityGroup() {
        mockInstall();
        EntityGroup group = new EntityGroup();
        group.setId(new EntityGroupId(UUID.randomUUID()));
        group.setType(EntityType.DEVICE);
        group.setName("Warehouse sensors");
        group.setOwnerId(tenantId);
        when(entityGroupService.findEntityGroupById(tenantId, group.getId())).thenReturn(group);
        DevicePackageRequest request = request(
                create("AM308", "DEVICE", JacksonUtil.newObjectNode().put("name", "AM308")),
                create("Dashboard", "DASHBOARD", JacksonUtil.newObjectNode().put("title", "AM308 dashboard")));
        request.setEntityGroupId(group.getId().getId());

        DevicePackageResult result = service.install(user, request);

        assertThat(result.isSuccess()).isTrue();
        verify(entityGroupService).addEntityToEntityGroup(tenantId, group.getId(), deviceId);
        verify(entityGroupService, never()).addEntityToEntityGroup(any(), any(), eq(dashboardId));
        assertThat(savedInTransaction).contains("ENTITY_GROUP");
        verify(logEntityActionService).logEntityAction(eq(tenantId), eq(deviceId), any(Device.class), any(), eq(ActionType.ADDED_TO_ENTITY_GROUP),
                eq(user), eq(deviceId.toString()), eq(group.getId().toString()), eq("Warehouse sensors"));
        assertThat(afterCommitCalls).containsExactly("AUDIT", "AUDIT", "AUDIT", "REPORT");
    }

    @Test
    void installRejectsACustomerOfAnotherTenant() {
        mockInstall();
        CustomerId customerId = customerOf(TenantId.fromUUID(UUID.randomUUID()));
        DevicePackageRequest request = request(create("Dashboard", "DASHBOARD", JacksonUtil.newObjectNode().put("title", "d")));
        request.setCustomerId(customerId.getId());

        DevicePackageResult result = service.install(user, request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Invalid install request");
        verify(transactionTemplate, never()).execute(any());
        verify(installedItemService, never()).save(any(), any());
    }

    @Test
    void updateIgnoresCustomerAndEntityGroup() {
        when(dashboardService.findDashboardById(tenantId, dashboardId)).thenReturn(dashboard(7L));
        when(dashboardService.saveDashboard(any(Dashboard.class))).thenAnswer(i -> i.getArgument(0));
        DevicePackageRequest request = request(overwrite("Dashboard", "DASHBOARD", dashboardId, 7L, JacksonUtil.newObjectNode().put("title", "d")));
        request.setCustomerId(UUID.randomUUID());
        request.setEntityGroupId(UUID.randomUUID());

        DevicePackageResult result = service.update(user, installedItem.getId(), request);

        assertThat(result.isSuccess()).isTrue();
        ArgumentCaptor<Dashboard> dashboard = ArgumentCaptor.forClass(Dashboard.class);
        verify(dashboardService).saveDashboard(dashboard.capture());
        assertThat(dashboard.getValue().getCustomerId()).isNull();
        verifyNoInteractions(customerService, entityGroupService);
    }

    @Test
    void updateOverwriteOfACustomerDeviceThatChangesItsOwnerFails() {
        DeviceId otherId = new DeviceId(UUID.randomUUID());
        Device current = new Device(otherId);
        current.setTenantId(tenantId);
        current.setName("AM308 spare");
        current.setCustomerId(customerOf(tenantId));
        when(deviceService.findDeviceById(tenantId, otherId)).thenReturn(current);
        DevicePackageRequest.Step step = overwrite("AM308 spare", "DEVICE", otherId, null, JacksonUtil.newObjectNode().put("name", "AM308 spare"));
        step.setConfirmed(true);

        DevicePackageResult result = service.update(user, installedItem.getId(), request(keep("AM308", "DEVICE", deviceId), step));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailedStep()).isEqualTo("DEVICE:AM308 spare");
        assertThat(result.getErrorMessage()).isEqualTo("AM308 spare belongs to another owner");
        verify(deviceService, never()).saveDevice(any(Device.class));
        verify(installedItemService, never()).save(any(), any());
    }

    @Test
    void updateOverwriteOfAnotherDeviceKeepsItsCredentials() {
        DeviceId otherId = new DeviceId(UUID.randomUUID());
        Device current = new Device(otherId);
        current.setTenantId(tenantId);
        current.setName("AM308 spare");
        when(deviceService.findDeviceById(tenantId, otherId)).thenReturn(current);
        when(deviceService.saveDevice(any(Device.class))).thenAnswer(i -> i.getArgument(0));
        DeviceCredentials credentials = new DeviceCredentials();
        credentials.setDeviceId(otherId);
        credentials.setCredentialsId("existing-token");
        when(deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, otherId)).thenReturn(credentials);
        DevicePackageRequest.Step step = overwrite("AM308 spare", "DEVICE", otherId, null, JacksonUtil.newObjectNode().put("name", "AM308 spare"));
        step.setConfirmed(true);
        step.setCredentials(JacksonUtil.newObjectNode().put("credentialsType", "ACCESS_TOKEN").put("credentialsId", "package-token"));

        DevicePackageResult result = service.update(user, installedItem.getId(), request(keep("AM308", "DEVICE", deviceId), step));

        assertThat(result.isSuccess()).isTrue();
        verify(deviceService).saveDevice(any(Device.class));
        verify(deviceCredentialsService, never()).updateDeviceCredentials(any(), any());
        assertThat(result.getOutputs().get("DEVICE:AM308 spare").get("token").asText()).isEqualTo("existing-token");
    }

    @Test
    void installRejectsAnOverwriteOfADeviceOrGateway() {
        mockInstall();
        for (String type : new String[]{"DEVICE", "GATEWAY"}) {
            DevicePackageRequest.Step step = overwrite("AM308", type, deviceId, 1L, JacksonUtil.newObjectNode().put("name", "AM308"));
            step.setConfirmed(true);

            DevicePackageResult result = service.install(user, request(step));

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).isEqualTo("Invalid install step: AM308");
        }
        verify(transactionTemplate, never()).execute(any());
        verify(deviceService, never()).saveDevice(any(Device.class));
        verify(installedItemService, never()).save(any(), any());
    }

    @Test
    void installWritesStepAttributesOnAUsedExistingEntityAfterCommit() {
        mockInstall();
        DevicePackageRequest.Step deviceStep = keep("AM308", "DEVICE", deviceId);
        deviceStep.setResolution("use-existing");
        deviceStep.setServerAttributes(JacksonUtil.newObjectNode().put("model", "AM308"));
        deviceStep.setSharedAttributes(JacksonUtil.newObjectNode().put("deviceId", "${device.id}"));

        DevicePackageResult result = service.install(user, request(deviceStep,
                create("Dashboard", "DASHBOARD", JacksonUtil.newObjectNode().put("title", "AM308 dashboard"))));

        assertThat(result.isSuccess()).isTrue();
        ArgumentCaptor<AttributesSaveRequest> saved = ArgumentCaptor.forClass(AttributesSaveRequest.class);
        verify(telemetryService, times(2)).saveAttributes(saved.capture());
        assertThat(saved.getAllValues()).extracting(AttributesSaveRequest::getEntityId).containsOnly(deviceId);
        assertThat(saved.getAllValues()).extracting(AttributesSaveRequest::getScope).containsExactly(AttributeScope.SERVER_SCOPE, AttributeScope.SHARED_SCOPE);
        assertThat(saved.getAllValues().get(0).getEntries().get(0).getValueAsString()).isEqualTo("AM308");
        assertThat(saved.getAllValues().get(1).getEntries().get(0).getValueAsString()).isEqualTo(deviceId.getId().toString());
        // The kept device isn't saved, so only the dashboard is audited
        assertThat(afterCommitCalls).containsExactly("AUDIT", "ATTRIBUTES", "ATTRIBUTES", "REPORT");
    }

    @Test
    void installIntoACustomerGroupGivesTheDeviceTheGroupsCustomer() {
        mockInstall();
        CustomerId groupCustomer = customerOf(tenantId);
        EntityGroup group = new EntityGroup();
        group.setId(new EntityGroupId(UUID.randomUUID()));
        group.setType(EntityType.DEVICE);
        group.setName("Customer sensors");
        group.setOwnerId(groupCustomer);
        when(entityGroupService.findEntityGroupById(tenantId, group.getId())).thenReturn(group);
        DevicePackageRequest request = request(
                create("AM308", "DEVICE", JacksonUtil.newObjectNode().put("name", "AM308")),
                create("Dashboard", "DASHBOARD", JacksonUtil.newObjectNode().put("title", "AM308 dashboard")));
        request.setEntityGroupId(group.getId().getId());

        DevicePackageResult result = service.install(user, request);

        assertThat(result.isSuccess()).isTrue();
        ArgumentCaptor<Device> device = ArgumentCaptor.forClass(Device.class);
        verify(deviceService).saveDevice(device.capture());
        assertThat(device.getValue().getCustomerId()).isEqualTo(groupCustomer);
        ArgumentCaptor<Dashboard> dashboard = ArgumentCaptor.forClass(Dashboard.class);
        verify(dashboardService).saveDashboard(dashboard.capture());
        assertThat(dashboard.getValue().getOwnerId()).isEqualTo(tenantId);
        verify(entityGroupService).addEntityToEntityGroup(tenantId, group.getId(), deviceId);
        verify(logEntityActionService).logEntityAction(eq(tenantId), eq(deviceId), any(Device.class), eq(groupCustomer), eq(ActionType.ADDED), eq(user));
    }

    private CustomerId customerOf(TenantId owner) {
        Customer customer = new Customer(new CustomerId(UUID.randomUUID()));
        customer.setTenantId(owner);
        when(customerService.findCustomerById(tenantId, customer.getId())).thenReturn(customer);
        return customer.getId();
    }

    /** The new entities get these ids; each save records that it ran inside the transaction. */
    private void mockInstall() {
        when(user.getId()).thenReturn(new UserId(UUID.randomUUID()));
        when(projectInfo.getProjectVersion()).thenReturn("4.4.1");
        when(projectInfo.getProductType()).thenReturn("PE");
        when(deviceProfileService.saveDeviceProfile(any())).thenAnswer(i -> {
            recordSave("DEVICE_PROFILE");
            DeviceProfile profile = i.getArgument(0);
            profile.setId(profileId);
            return profile;
        });
        when(dashboardService.saveDashboard(any(Dashboard.class))).thenAnswer(i -> {
            recordSave("DASHBOARD");
            Dashboard dashboard = i.getArgument(0);
            dashboard.setId(dashboardId);
            return dashboard;
        });
        when(deviceService.saveDevice(any(Device.class))).thenAnswer(i -> {
            recordSave("DEVICE");
            Device device = i.getArgument(0);
            device.setId(deviceId);
            return device;
        });
        doAnswer(i -> {
            recordSave("ENTITY_GROUP");
            return null;
        }).when(entityGroupService).addEntityToEntityGroup(any(), any(), any());
        when(installedItemService.save(any(), any())).thenAnswer(i -> {
            recordSave("INSTALLED_ITEM");
            return i.getArgument(1);
        });
        Integration integration = new Integration();
        integration.setId(integrationId);
        integration.setTenantId(tenantId);
        integration.setName("TTN my-app");
        when(integrationService.findIntegrationById(tenantId, integrationId)).thenReturn(integration);
        doAnswer(i -> {
            recordAfterCommit("AUDIT");
            return null;
        }).when(logEntityActionService).logEntityAction(any(TenantId.class), any(EntityId.class), any(HasName.class), nullable(CustomerId.class),
                any(ActionType.class), any(User.class), any(Object[].class));
        doAnswer(i -> {
            recordAfterCommit("REPORT");
            return null;
        }).when(iotHubRestClient).reportVersionInstalled(any(), any());
        doAnswer(i -> {
            recordAfterCommit("ATTRIBUTES");
            return null;
        }).when(telemetryService).saveAttributes(any(AttributesSaveRequest.class));
    }

    private void recordSave(String what) {
        if (inTransaction) {
            savedInTransaction.add(what);
        }
    }

    private void recordAfterCommit(String what) {
        afterCommitCalls.add(inTransaction ? what + " in transaction" : what);
    }

    private IotHubInstalledItem otherPackage(Map<String, JsonNode> installState) {
        DeviceInstalledItemDescriptor descriptor = new DeviceInstalledItemDescriptor();
        descriptor.setInstallState(installState);
        IotHubInstalledItem item = new IotHubInstalledItem();
        item.setId(new IotHubInstalledItemId(UUID.randomUUID()));
        item.setTenantId(tenantId);
        item.setItemType("DEVICE");
        item.setDescriptor(descriptor);
        return item;
    }

    private DeviceProfile profile(Long version, String name) {
        DeviceProfile profile = new DeviceProfile(profileId);
        profile.setTenantId(tenantId);
        profile.setName(name);
        profile.setVersion(version);
        return profile;
    }

    private Dashboard dashboard(Long version) {
        Dashboard dashboard = new Dashboard(dashboardId);
        dashboard.setTenantId(tenantId);
        dashboard.setTitle("AM308 dashboard");
        dashboard.setVersion(version);
        return dashboard;
    }

    private DevicePackageRequest request(DevicePackageRequest.Step... steps) {
        DevicePackageRequest request = new DevicePackageRequest();
        request.setVersionId(versionId);
        request.setSteps(List.of(steps));
        return request;
    }

    private static DevicePackageRequest.Step keep(String name, String type, EntityId id) {
        DevicePackageRequest.Step step = step(name, type, DevicePackageRequest.Action.KEEP);
        step.setEntityId(id.getId());
        return step;
    }

    private static DevicePackageRequest.Step overwrite(String name, String type, EntityId id, Long version, ObjectNode entity) {
        DevicePackageRequest.Step step = step(name, type, DevicePackageRequest.Action.OVERWRITE);
        step.setEntityId(id.getId());
        step.setExpectedVersion(version);
        step.setEntity(entity);
        return step;
    }

    private static DevicePackageRequest.Step create(String name, String type, ObjectNode entity) {
        DevicePackageRequest.Step step = step(name, type, DevicePackageRequest.Action.CREATE);
        step.setEntity(entity);
        return step;
    }

    private static DevicePackageRequest.Step step(String name, String type, DevicePackageRequest.Action action) {
        DevicePackageRequest.Step step = new DevicePackageRequest.Step();
        step.setName(name);
        step.setType(type);
        step.setAction(action);
        return step;
    }


    private static ObjectNode stateOf(EntityId id, String resolution) {
        ObjectNode state = JacksonUtil.newObjectNode().put("resolution", resolution);
        state.set("entityOutput", JacksonUtil.newObjectNode().put("id", id.getId().toString()));
        return state;
    }

}
