// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.iot_hub;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.exception.EntityVersionMismatchException;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.IotHubInstalledItemId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.iot_hub.DeviceInstalledItemDescriptor;
import org.thingsboard.server.common.data.iot_hub.IotHubInstalledItem;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.iot_hub.IotHubInstalledItemService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.service.entitiy.TbLogEntityActionService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DefaultDevicePackageUpdateServiceTest {

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
    private final SecurityUser user = mock(SecurityUser.class);
    private final UUID itemId = UUID.randomUUID();
    private final String versionId = UUID.randomUUID().toString();
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

    @InjectMocks
    private DefaultDevicePackageUpdateService service;

    private IotHubInstalledItem installedItem;
    // Set when the transaction callback threw, so the transaction would roll back
    private final List<RuntimeException> rolledBack = new ArrayList<>();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "entityService", entityService);
        ReflectionTestUtils.setField(service, "logEntityActionService", logEntityActionService);
        when(user.getTenantId()).thenReturn(tenantId);

        DeviceInstalledItemDescriptor descriptor = new DeviceInstalledItemDescriptor();
        descriptor.setSelectedInstallMethod("INTEGRATION_TTN");
        descriptor.setCreatedEntityIds(List.of(profileId, deviceId, dashboardId, integrationId));
        descriptor.setDashboardId(dashboardId);
        descriptor.setInstallState(Map.of());
        installedItem = new IotHubInstalledItem();
        installedItem.setId(new IotHubInstalledItemId(UUID.randomUUID()));
        installedItem.setTenantId(tenantId);
        installedItem.setItemId(itemId);
        installedItem.setItemType("DEVICE");
        installedItem.setVersion("1.0.0");
        installedItem.setDescriptor(descriptor);
        when(installedItemService.findById(tenantId, installedItem.getId())).thenReturn(installedItem);

        ObjectNode versionInfo = JacksonUtil.newObjectNode();
        versionInfo.put("itemId", itemId.toString());
        versionInfo.put("name", "AM308");
        versionInfo.put("version", "1.0.1");
        when(iotHubRestClient.getVersionInfo(versionId)).thenReturn(versionInfo);

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            try {
                return invocation.<TransactionCallback<?>>getArgument(0).doInTransaction(null);
            } catch (RuntimeException e) {
                rolledBack.add(e);
                throw e;
            }
        });
        when(packageEntities.checksum(any(), any())).thenReturn("checksum");
        when(entityService.fetchEntity(any(), any())).thenReturn(Optional.empty());
        Device device = new Device(deviceId);
        device.setTenantId(tenantId);
        device.setName("AM308");
        when(deviceService.findDeviceById(tenantId, deviceId)).thenReturn(device);
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

        DeviceUpdateResult result = service.apply(user, installedItem.getId(), request(
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
        assertThat(descriptor.getInstallState().get("Dashboard").get("checksum").asText())
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
        DeviceUpdateRequest request = request(keep("Dashboard", "DASHBOARD", dashboardId));
        request.setSelectedInstallMethod("INTEGRATION_CHIRPSTACK");

        DeviceUpdateResult result = service.apply(user, installedItem.getId(), request);

        assertThat(result.isSuccess()).isTrue();
        DeviceInstalledItemDescriptor descriptor = (DeviceInstalledItemDescriptor) installedItem.getDescriptor();
        assertThat(descriptor.getInstallState().get("Dashboard").get("checksum").asText()).isEqualTo("installed");
        assertThat(descriptor.getSelectedInstallMethod()).isEqualTo("INTEGRATION_CHIRPSTACK");
        verify(dashboardService, never()).saveDashboard(any(Dashboard.class));
    }

    @Test
    void failedStep_rollsBackAndReportsTheStep() {
        when(deviceProfileService.findDeviceProfileById(tenantId, profileId)).thenReturn(profile(3L, "AM308"));
        when(deviceProfileService.saveDeviceProfile(any())).thenAnswer(i -> i.getArgument(0));
        when(dashboardService.findDashboardById(tenantId, dashboardId)).thenReturn(dashboard(7L));
        when(dashboardService.saveDashboard(any(Dashboard.class))).thenThrow(new DataValidationException("Dashboards limit reached"));

        DeviceUpdateResult result = service.apply(user, installedItem.getId(), request(
                overwrite("AM308 profile", "DEVICE_PROFILE", profileId, 3L, JacksonUtil.newObjectNode().put("name", "p")),
                overwrite("Dashboard", "DASHBOARD", dashboardId, 7L, JacksonUtil.newObjectNode().put("title", "d"))));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailedStep()).isEqualTo("Dashboard");
        assertThat(result.getErrorMessage()).isEqualTo("Dashboards limit reached");
        assertThat(rolledBack).hasSize(1);
        verify(installedItemService, never()).save(any(), any());
        verifyNoInteractions(logEntityActionService);
        assertThat(installedItem.getVersion()).isEqualTo("1.0.0");
    }

    @Test
    void entityChangedSinceTheCheck_isAVersionConflict() {
        when(dashboardService.findDashboardById(tenantId, dashboardId)).thenReturn(dashboard(8L));

        DeviceUpdateResult result = service.apply(user, installedItem.getId(), request(
                overwrite("Dashboard", "DASHBOARD", dashboardId, 7L, JacksonUtil.newObjectNode().put("title", "d"))));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailedStep()).isEqualTo("Dashboard");
        assertThat(result.getErrorMessage()).contains("Check the update again");
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

        DeviceUpdateResult result = service.apply(user, installedItem.getId(), request(
                create("Uplink", "UPLINK_CONVERTER", JacksonUtil.newObjectNode().put("name", "Uplink for TTN")),
                overwrite("TTN", "INTEGRATION", integrationId, 2L, integration)));

        assertThat(result.isSuccess()).isTrue();
        ArgumentCaptor<Integration> saved = ArgumentCaptor.forClass(Integration.class);
        verify(integrationService).saveIntegration(saved.capture());
        assertThat(saved.getValue().getDefaultConverterId()).isEqualTo(converterId);
        // The devices keep sending to the same endpoint
        assertThat(saved.getValue().getRoutingKey()).isEqualTo("routing-key");
        assertThat(saved.getValue().getName()).isEqualTo("TTN my-app");
        assertThat(result.getOutputs().get("TTN").get("routingKey").asText()).isEqualTo("routing-key");
    }

    @Test
    void overwritingAnEntityOfAnotherPackage_needsAConfirmation() {
        DashboardId foreign = new DashboardId(UUID.randomUUID());

        DeviceUpdateResult result = service.apply(user, installedItem.getId(), request(
                overwrite("Dashboard", "DASHBOARD", foreign, 1L, JacksonUtil.newObjectNode().put("title", "d"))));

        assertThat(result.isSuccess()).isFalse();
        verify(transactionTemplate, never()).execute(any());
    }

    @Test
    void theDeviceIsNeverOverwritten() {
        DeviceUpdateResult result = service.apply(user, installedItem.getId(), request(
                overwrite("AM308", "DEVICE", deviceId, 1L, JacksonUtil.newObjectNode().put("name", "d"))));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("The device and its credentials don't change on update");
        verify(transactionTemplate, never()).execute(any());
    }

    @Test
    void versionOfAnotherItem_isRejected() {
        ObjectNode other = JacksonUtil.newObjectNode().put("itemId", UUID.randomUUID().toString());
        when(iotHubRestClient.getVersionInfo(versionId)).thenReturn(other);

        DeviceUpdateResult result = service.apply(user, installedItem.getId(), request(keep("AM308", "DEVICE", deviceId)));

        assertThat(result.isSuccess()).isFalse();
        verify(transactionTemplate, never()).execute(any());
    }

    @Test
    void userMessage_showsValidationMessagesAndHidesInternalOnes() {
        assertThat(DefaultDevicePackageUpdateService.userMessage(new DataValidationException("Rule chains limit reached")))
                .isEqualTo("Rule chains limit reached");
        assertThat(DefaultDevicePackageUpdateService.userMessage(new EntityVersionMismatchException(EntityType.DASHBOARD, null)))
                .contains("Check the update again");
        assertThat(DefaultDevicePackageUpdateService.userMessage(new IllegalStateException("could not execute statement; SQL [n/a]")))
                .isEqualTo("Failed to update the device package");
    }

    @Test
    void resolvePlaceholders_leavesUnknownAliasesAsTheyAre() {
        ObjectNode device = JacksonUtil.newObjectNode().put("id", "123");
        ObjectNode node = JacksonUtil.newObjectNode().put("a", "${device.id}").put("b", "x ${device.id} ${gateway.id}");

        ObjectNode resolved = (ObjectNode) DefaultDevicePackageUpdateService.resolvePlaceholders(node, Map.of("device", device));

        assertThat(resolved.get("a").asText()).isEqualTo("123");
        assertThat(resolved.get("b").asText()).isEqualTo("x 123 ${gateway.id}");
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

    private DeviceUpdateRequest request(DeviceUpdateRequest.Step... steps) {
        DeviceUpdateRequest request = new DeviceUpdateRequest();
        request.setVersionId(versionId);
        request.setSteps(List.of(steps));
        return request;
    }

    private static DeviceUpdateRequest.Step keep(String name, String type, EntityId id) {
        DeviceUpdateRequest.Step step = step(name, type, DeviceUpdateRequest.Action.KEEP);
        step.setEntityId(id.getId());
        return step;
    }

    private static DeviceUpdateRequest.Step overwrite(String name, String type, EntityId id, Long version, ObjectNode entity) {
        DeviceUpdateRequest.Step step = step(name, type, DeviceUpdateRequest.Action.OVERWRITE);
        step.setEntityId(id.getId());
        step.setExpectedVersion(version);
        step.setEntity(entity);
        return step;
    }

    private static DeviceUpdateRequest.Step create(String name, String type, ObjectNode entity) {
        DeviceUpdateRequest.Step step = step(name, type, DeviceUpdateRequest.Action.CREATE);
        step.setEntity(entity);
        return step;
    }

    private static DeviceUpdateRequest.Step step(String name, String type, DeviceUpdateRequest.Action action) {
        DeviceUpdateRequest.Step step = new DeviceUpdateRequest.Step();
        step.setName(name);
        step.setType(type);
        step.setAction(action);
        return step;
    }

}
