// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.iot_hub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IotHubInstalledItemId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.iot_hub.DashboardInstalledItemDescriptor;
import org.thingsboard.server.common.data.iot_hub.DeviceInstalledItemDescriptor;
import org.thingsboard.server.common.data.iot_hub.IotHubInstalledItem;
import org.thingsboard.server.common.data.iot_hub.RuleChainInstalledItemDescriptor;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.iot_hub.IotHubInstalledItemService;
import org.thingsboard.server.service.entitiy.dashboard.TbDashboardService;
import org.thingsboard.server.service.entitiy.device.TbDeviceService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultIotHubServiceTest {

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
    private final SecurityUser user = mock(SecurityUser.class);

    @Mock
    private IotHubRestClient iotHubRestClient;
    @Mock
    private IotHubInstalledItemService iotHubInstalledItemService;
    @Mock
    private DeviceService deviceService;
    @Mock
    private DeviceProfileService deviceProfileService;
    @Mock
    private TbDeviceService tbDeviceService;
    @Mock
    private DashboardService dashboardService;
    @Mock
    private TbDashboardService tbDashboardService;

    @Spy
    @InjectMocks
    private DefaultIotHubService service;

    private void mockTenant() {
        when(user.getTenantId()).thenReturn(tenantId);
    }

    private IotHubInstalledItem installedItem(UUID id) {
        IotHubInstalledItem item = new IotHubInstalledItem();
        item.setId(new IotHubInstalledItemId(id));
        item.setTenantId(tenantId);
        return item;
    }

    private InstallPlanEntry willInstall(String versionId, boolean root) {
        InstallPlanEntry entry = new InstallPlanEntry();
        entry.setItemId(UUID.randomUUID().toString());
        entry.setVersionId(versionId);
        entry.setName(root ? "Root" : "Dep");
        entry.setStatus(InstallPlanEntry.Status.WILL_INSTALL);
        entry.setRoot(root);
        return entry;
    }

    private ObjectNode version(String id, String itemId, String type, String name, String ver) {
        ObjectNode node = JacksonUtil.newObjectNode();
        node.put("id", id);
        node.put("itemId", itemId);
        node.put("type", type);
        node.put("name", name);
        node.put("version", ver);
        return node;
    }

    @Test
    void resolveInstallPlan_rootOnly_noRelated_marksWillInstall() {
        mockTenant();
        String versionId = UUID.randomUUID().toString();
        String itemId = UUID.randomUUID().toString();
        when(iotHubRestClient.getVersionInfo(versionId)).thenReturn(version(versionId, itemId, "DASHBOARD", "Root", "1.0"));
        when(iotHubInstalledItemService.findInstalledItemIdsByTenantIdAndItemIdIn(eq(tenantId), any())).thenReturn(List.of());

        InstallPlan plan = service.resolveInstallPlan(user, versionId);

        assertThat(plan.getEntries()).hasSize(1);
        InstallPlanEntry root = plan.getEntries().get(0);
        assertThat(root.isRoot()).isTrue();
        assertThat(root.getStatus()).isEqualTo(InstallPlanEntry.Status.WILL_INSTALL);
        assertThat(root.getItemId()).isEqualTo(itemId);
    }

    @Test
    void resolveInstallPlan_ordersDependenciesBeforeRoot() {
        mockTenant();
        String versionId = UUID.randomUUID().toString();
        String rootItemId = UUID.randomUUID().toString();
        String relatedItemId = UUID.randomUUID().toString();
        ObjectNode root = version(versionId, rootItemId, "DASHBOARD", "Root", "1.0");
        root.putArray("relatedItems").add(relatedItemId);
        when(iotHubRestClient.getVersionInfo(versionId)).thenReturn(root);
        when(iotHubRestClient.getPublishedVersionByItemId(relatedItemId))
                .thenReturn(version(UUID.randomUUID().toString(), relatedItemId, "WIDGET", "Dep", "2.0"));
        when(iotHubInstalledItemService.findInstalledItemIdsByTenantIdAndItemIdIn(eq(tenantId), any())).thenReturn(List.of());

        InstallPlan plan = service.resolveInstallPlan(user, versionId);

        assertThat(plan.getEntries()).hasSize(2);
        assertThat(plan.getEntries().get(0).getItemId()).isEqualTo(relatedItemId);
        assertThat(plan.getEntries().get(0).isRoot()).isFalse();
        assertThat(plan.getEntries().get(1).getItemId()).isEqualTo(rootItemId);
        assertThat(plan.getEntries().get(1).isRoot()).isTrue();
    }

    @Test
    void resolveInstallPlan_missingRelated_recordedAsMissing() {
        mockTenant();
        String versionId = UUID.randomUUID().toString();
        String rootItemId = UUID.randomUUID().toString();
        String relatedItemId = UUID.randomUUID().toString();
        ObjectNode root = version(versionId, rootItemId, "DASHBOARD", "Root", "1.0");
        root.putArray("relatedItems").add(relatedItemId);
        when(iotHubRestClient.getVersionInfo(versionId)).thenReturn(root);
        when(iotHubRestClient.getPublishedVersionByItemId(relatedItemId)).thenReturn(null);
        when(iotHubInstalledItemService.findInstalledItemIdsByTenantIdAndItemIdIn(eq(tenantId), any())).thenReturn(List.of());

        InstallPlan plan = service.resolveInstallPlan(user, versionId);

        InstallPlanEntry missing = plan.getEntries().get(0);
        assertThat(missing.getStatus()).isEqualTo(InstallPlanEntry.Status.MISSING);
        assertThat(missing.getItemId()).isEqualTo(relatedItemId);
        assertThat(missing.getErrorMessage()).isNotBlank();
    }

    @Test
    void resolveInstallPlan_relatedFetchThrows_recordedAsMissing() {
        mockTenant();
        String versionId = UUID.randomUUID().toString();
        String rootItemId = UUID.randomUUID().toString();
        String relatedItemId = UUID.randomUUID().toString();
        ObjectNode root = version(versionId, rootItemId, "DASHBOARD", "Root", "1.0");
        root.putArray("relatedItems").add(relatedItemId);
        when(iotHubRestClient.getVersionInfo(versionId)).thenReturn(root);
        when(iotHubRestClient.getPublishedVersionByItemId(relatedItemId)).thenThrow(new RuntimeException("boom"));
        when(iotHubInstalledItemService.findInstalledItemIdsByTenantIdAndItemIdIn(eq(tenantId), any())).thenReturn(List.of());

        InstallPlan plan = service.resolveInstallPlan(user, versionId);

        assertThat(plan.getEntries().get(0).getStatus()).isEqualTo(InstallPlanEntry.Status.MISSING);
        assertThat(plan.getEntries().get(0).getErrorMessage()).contains("boom");
    }

    @Test
    void resolveInstallPlan_alreadyInstalledRoot_marksAlreadyInstalled() {
        mockTenant();
        String versionId = UUID.randomUUID().toString();
        UUID rootItemId = UUID.randomUUID();
        when(iotHubRestClient.getVersionInfo(versionId))
                .thenReturn(version(versionId, rootItemId.toString(), "WIDGET", "Root", "1.0"));
        when(iotHubInstalledItemService.findInstalledItemIdsByTenantIdAndItemIdIn(eq(tenantId), any())).thenReturn(List.of(rootItemId));

        InstallPlan plan = service.resolveInstallPlan(user, versionId);

        assertThat(plan.getEntries().get(0).getStatus()).isEqualTo(InstallPlanEntry.Status.ALREADY_INSTALLED);
    }

    @Test
    void resolveInstallPlan_nullVersion_throws() {
        mockTenant();
        String versionId = UUID.randomUUID().toString();
        when(iotHubRestClient.getVersionInfo(versionId)).thenReturn(null);

        assertThatThrownBy(() -> service.resolveInstallPlan(user, versionId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void installPlan_emptyPlan_returnsFailureWithoutInstalling() {
        InstallPlanResult result = service.installPlan(user, new InstallPlan(null, List.of()), null, null);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("empty");
        verify(iotHubInstalledItemService, never()).findInstalledItemIdsByTenantIdAndItemIdIn(any(), any());
    }

    @Test
    void installPlan_missingEntry_collectedAndNotInstalled() {
        mockTenant();
        InstallPlanEntry missing = new InstallPlanEntry();
        missing.setItemId(UUID.randomUUID().toString());
        missing.setStatus(InstallPlanEntry.Status.MISSING);

        InstallPlanResult result = service.installPlan(user, new InstallPlan(null, List.of(missing)), null, null);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMissingItemIds()).containsExactly(missing.getItemId());
        assertThat(result.getRootDescriptor()).isNull();
        verify(iotHubRestClient, never()).getVersionInfo(anyString());
        // A plan with nothing to install never hits the DB to check what is already installed.
        verify(iotHubInstalledItemService, never()).findInstalledItemIdsByTenantIdAndItemIdIn(any(), any());
    }

    @Test
    void installPlan_stalePlan_skipsItemInstalledSinceResolution() {
        mockTenant();
        UUID itemId = UUID.randomUUID();
        InstallPlanEntry entry = new InstallPlanEntry();
        entry.setItemId(itemId.toString());
        entry.setVersionId(UUID.randomUUID().toString());
        entry.setType("WIDGET");
        entry.setStatus(InstallPlanEntry.Status.WILL_INSTALL);
        entry.setRoot(true);
        // The item is reported as already installed at install time, even though the plan says WILL_INSTALL.
        when(iotHubInstalledItemService.findInstalledItemIdsByTenantIdAndItemIdIn(eq(tenantId), any())).thenReturn(List.of(itemId));

        InstallPlanResult result = service.installPlan(user, new InstallPlan(null, List.of(entry)), null, null);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getEntries().get(0).getStatus()).isEqualTo(InstallPlanEntry.Status.ALREADY_INSTALLED);
        // No marketplace fetch happened — the duplicate install was avoided.
        verify(iotHubRestClient, never()).getVersionInfo(anyString());
    }

    @Test
    void installPlan_duplicateItemId_installsOnce() throws Exception {
        mockTenant();
        HttpServletRequest request = mock(HttpServletRequest.class);
        UUID itemId = UUID.randomUUID();
        String firstVersionId = UUID.randomUUID().toString();
        String secondVersionId = UUID.randomUUID().toString();
        // Two entries point at the same item — a client-supplied plan is not de-duplicated, so the
        // cascade itself must not install the same item twice.
        InstallPlanEntry first = new InstallPlanEntry();
        first.setItemId(itemId.toString());
        first.setVersionId(firstVersionId);
        first.setStatus(InstallPlanEntry.Status.WILL_INSTALL);
        first.setRoot(true);
        InstallPlanEntry duplicate = new InstallPlanEntry();
        duplicate.setItemId(itemId.toString());
        duplicate.setVersionId(secondVersionId);
        duplicate.setStatus(InstallPlanEntry.Status.WILL_INSTALL);
        duplicate.setRoot(false);

        // Nothing is installed yet when the plan is submitted.
        when(iotHubInstalledItemService.findInstalledItemIdsByTenantIdAndItemIdIn(eq(tenantId), any())).thenReturn(List.of());
        doReturn(installedItem(UUID.randomUUID())).when(service).doInstallVersion(eq(user), eq(firstVersionId), any(), any());

        InstallPlanResult result = service.installPlan(user, new InstallPlan(firstVersionId, List.of(first, duplicate)), null, request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getEntries()).hasSize(2);
        assertThat(result.getEntries().get(0).getStatus()).isEqualTo(InstallPlanEntry.Status.WILL_INSTALL);
        // The duplicate is recognised as freshly installed and skipped.
        assertThat(result.getEntries().get(1).getStatus()).isEqualTo(InstallPlanEntry.Status.ALREADY_INSTALLED);
        verify(service).doInstallVersion(eq(user), eq(firstVersionId), any(), any());
        verify(service, never()).doInstallVersion(eq(user), eq(secondVersionId), any(), any());
    }

    @Test
    void installPlan_cascade_installsDependencyBeforeRoot_andRoutesDataToRootOnly() throws Exception {
        mockTenant();
        HttpServletRequest request = mock(HttpServletRequest.class);
        JsonNode data = JacksonUtil.newObjectNode().put("entityId", UUID.randomUUID().toString());
        String depVersionId = UUID.randomUUID().toString();
        String rootVersionId = UUID.randomUUID().toString();
        InstallPlanEntry dep = willInstall(depVersionId, false);
        InstallPlanEntry root = willInstall(rootVersionId, true);

        when(iotHubInstalledItemService.findInstalledItemIdsByTenantIdAndItemIdIn(eq(tenantId), any())).thenReturn(List.of());
        IotHubInstalledItem depItem = installedItem(UUID.randomUUID());
        IotHubInstalledItem rootItem = installedItem(UUID.randomUUID());
        DashboardInstalledItemDescriptor rootDescriptor = new DashboardInstalledItemDescriptor();
        rootItem.setDescriptor(rootDescriptor);
        doReturn(depItem).when(service).doInstallVersion(eq(user), eq(depVersionId), any(), any());
        doReturn(rootItem).when(service).doInstallVersion(eq(user), eq(rootVersionId), any(), any());

        InstallPlanResult result = service.installPlan(user, new InstallPlan(rootVersionId, List.of(dep, root)), data, request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isRolledBack()).isFalse();
        assertThat(result.getRootDescriptor()).isSameAs(rootDescriptor);
        assertThat(result.getMissingItemIds()).isEmpty();
        assertThat(result.getEntries()).hasSize(2);

        // The dependency installs before the root, and only the root receives the user's install data.
        InOrder inOrder = inOrder(service);
        inOrder.verify(service).doInstallVersion(eq(user), eq(depVersionId), isNull(), eq(request));
        inOrder.verify(service).doInstallVersion(eq(user), eq(rootVersionId), eq(data), eq(request));
        verify(service, never()).deleteInstalledItem(any(), any());
    }

    @Test
    void installPlan_dependencyFails_rollsBackInstalledItemsInReverseOrder() throws Exception {
        mockTenant();
        HttpServletRequest request = mock(HttpServletRequest.class);
        String dep1VersionId = UUID.randomUUID().toString();
        String dep2VersionId = UUID.randomUUID().toString();
        String rootVersionId = UUID.randomUUID().toString();
        InstallPlanEntry dep1 = willInstall(dep1VersionId, false);
        InstallPlanEntry dep2 = willInstall(dep2VersionId, false);
        InstallPlanEntry root = willInstall(rootVersionId, true);

        when(iotHubInstalledItemService.findInstalledItemIdsByTenantIdAndItemIdIn(eq(tenantId), any())).thenReturn(List.of());
        IotHubInstalledItem dep1Item = installedItem(UUID.randomUUID());
        IotHubInstalledItem dep2Item = installedItem(UUID.randomUUID());
        doReturn(dep1Item).when(service).doInstallVersion(eq(user), eq(dep1VersionId), any(), any());
        doReturn(dep2Item).when(service).doInstallVersion(eq(user), eq(dep2VersionId), any(), any());
        doThrow(new IllegalStateException("install failed")).when(service).doInstallVersion(eq(user), eq(rootVersionId), any(), any());
        doNothing().when(service).deleteInstalledItem(eq(user), any());

        InstallPlanResult result = service.installPlan(user, new InstallPlan(rootVersionId, List.of(dep1, dep2, root)), null, request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.isRolledBack()).isTrue();
        assertThat(result.getErrorMessage()).contains("install failed");

        // Successfully installed dependencies are rolled back in reverse install order (dep2 before dep1).
        InOrder inOrder = inOrder(service);
        inOrder.verify(service).deleteInstalledItem(user, dep2Item.getId());
        inOrder.verify(service).deleteInstalledItem(user, dep1Item.getId());
    }

    @Test
    void installPlan_errorMessageIsTheFailureItselfWithoutAnyPrefix() throws Exception {
        mockTenant();
        HttpServletRequest request = mock(HttpServletRequest.class);
        String rootVersionId = UUID.randomUUID().toString();
        InstallPlanEntry root = willInstall(rootVersionId, true);

        when(iotHubInstalledItemService.findInstalledItemIdsByTenantIdAndItemIdIn(eq(tenantId), any())).thenReturn(List.of());
        doThrow(new IllegalStateException("install failed")).when(service).doInstallVersion(eq(user), eq(rootVersionId), any(), any());

        InstallPlanResult result = service.installPlan(user, new InstallPlan(rootVersionId, List.of(root)), null, request);

        // the sentence around the failure belongs to the dialog, which localizes it
        assertThat(result.getErrorMessage()).isEqualTo("install failed");
    }

    @Test
    void installPlan_dependencyFails_failureIsReportedOnTheEntryAndAsTheResultMessage() throws Exception {
        mockTenant();
        HttpServletRequest request = mock(HttpServletRequest.class);
        String depVersionId = UUID.randomUUID().toString();
        String rootVersionId = UUID.randomUUID().toString();
        InstallPlanEntry dep = willInstall(depVersionId, false);
        InstallPlanEntry root = willInstall(rootVersionId, true);

        when(iotHubInstalledItemService.findInstalledItemIdsByTenantIdAndItemIdIn(eq(tenantId), any())).thenReturn(List.of());
        doThrow(new IllegalStateException("install failed")).when(service).doInstallVersion(eq(user), eq(depVersionId), any(), any());

        InstallPlanResult result = service.installPlan(user, new InstallPlan(rootVersionId, List.of(dep, root)), null, request);

        // the dialog names the failing dependency itself, from the entry it finds in the result
        assertThat(result.getErrorMessage()).isEqualTo("install failed");
        assertThat(result.getEntries()).anySatisfy(entry -> {
            assertThat(entry.getName()).isEqualTo("Dep");
            assertThat(entry.isRoot()).isFalse();
            assertThat(entry.getErrorMessage()).isEqualTo("install failed");
        });
        verify(service, never()).doInstallVersion(eq(user), eq(rootVersionId), any(), any());
    }

    @Test
    void installPlan_failureWithoutMessage_reportsTheExceptionType() throws Exception {
        mockTenant();
        HttpServletRequest request = mock(HttpServletRequest.class);
        String rootVersionId = UUID.randomUUID().toString();
        InstallPlanEntry root = willInstall(rootVersionId, true);

        when(iotHubInstalledItemService.findInstalledItemIdsByTenantIdAndItemIdIn(eq(tenantId), any())).thenReturn(List.of());
        doThrow(new NullPointerException()).when(service).doInstallVersion(eq(user), eq(rootVersionId), any(), any());

        InstallPlanResult result = service.installPlan(user, new InstallPlan(rootVersionId, List.of(root)), null, request);

        // the dialog looks the failing entry up by its error message, so it must never be blank
        assertThat(result.getErrorMessage()).isEqualTo("NullPointerException");
        assertThat(result.getEntries()).allSatisfy(entry ->
                assertThat(entry.getErrorMessage()).isEqualTo("NullPointerException"));
    }

    @Test
    void installPlan_rollbackFailure_reportsNotFullyRolledBack() throws Exception {
        mockTenant();
        HttpServletRequest request = mock(HttpServletRequest.class);
        String depVersionId = UUID.randomUUID().toString();
        String rootVersionId = UUID.randomUUID().toString();
        InstallPlanEntry dep = willInstall(depVersionId, false);
        InstallPlanEntry root = willInstall(rootVersionId, true);

        when(iotHubInstalledItemService.findInstalledItemIdsByTenantIdAndItemIdIn(eq(tenantId), any())).thenReturn(List.of());
        IotHubInstalledItem depItem = installedItem(UUID.randomUUID());
        doReturn(depItem).when(service).doInstallVersion(eq(user), eq(depVersionId), any(), any());
        doThrow(new IllegalStateException("install failed")).when(service).doInstallVersion(eq(user), eq(rootVersionId), any(), any());
        // Rolling back the one installed dependency itself fails — the result must report a partial rollback.
        doThrow(new RuntimeException("delete failed")).when(service).deleteInstalledItem(user, depItem.getId());

        InstallPlanResult result = service.installPlan(user, new InstallPlan(rootVersionId, List.of(dep, root)), null, request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.isRolledBack()).isFalse();
        assertThat(result.getErrorMessage()).contains("install failed");
    }

    @Test
    void updateItemVersion_device_refusedAndRemovesNothing() {
        mockTenant();
        DeviceInstalledItemDescriptor descriptor = new DeviceInstalledItemDescriptor();
        descriptor.setCreatedEntityIds(List.<EntityId>of(new DeviceId(UUID.randomUUID())));
        IotHubInstalledItem item = installedItem(UUID.randomUUID());
        item.setItemType("DEVICE");
        item.setItemVersionId(UUID.randomUUID());
        item.setDescriptor(descriptor);
        when(iotHubInstalledItemService.findById(tenantId, item.getId())).thenReturn(item);

        // The install wizard of the new version does the update. A client that called this as an update before
        // got success while the package was removed and nothing was installed again.
        UpdateItemVersionResult result = service.updateItemVersion(user, item.getId(), UUID.randomUUID().toString(), true, null);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("install wizard");
        verify(tbDeviceService, never()).delete(any(Device.class), any());
        verify(iotHubInstalledItemService, never()).deleteById(any(), any());
        verify(iotHubInstalledItemService, never()).save(any(), any());
    }

    @Test
    void removeDevicePackageEntities_removesEntitiesInReverseOrderAndKeepsTheItemVersion() throws Exception {
        mockTenant();
        Device device = new Device(new DeviceId(UUID.randomUUID()));
        Dashboard dashboard = new Dashboard(new DashboardId(UUID.randomUUID()));
        DeviceInstalledItemDescriptor descriptor = new DeviceInstalledItemDescriptor();
        descriptor.setCreatedEntityIds(List.<EntityId>of(device.getId(), dashboard.getId()));
        descriptor.setDashboardId(dashboard.getId());
        IotHubInstalledItem item = installedItem(UUID.randomUUID());
        item.setItemType("DEVICE");
        UUID versionId = UUID.randomUUID();
        item.setItemVersionId(versionId);
        item.setVersion("1.0.1");
        item.setDescriptor(descriptor);
        descriptor.setInstallState(new LinkedHashMap<>(Map.of(
                "Configuration", JacksonUtil.toJsonNode("{\"formValues\":{\"gatewayName\":\"GW\"}}"),
                "Gateway", JacksonUtil.toJsonNode("{\"entityOutput\":{\"id\":\"x\"},\"resolution\":\"created\"}"))));
        when(iotHubInstalledItemService.findById(tenantId, item.getId())).thenReturn(item);
        String newVersionId = UUID.randomUUID().toString();
        item.setItemId(UUID.randomUUID());
        when(iotHubRestClient.getVersionInfo(newVersionId))
                .thenReturn(version(newVersionId, item.getItemId().toString(), "DEVICE", "Gateway", "1.0.2"));
        device.setTenantId(tenantId);
        dashboard.setTenantId(tenantId);
        when(deviceService.findDeviceById(tenantId, device.getId())).thenReturn(device);
        when(dashboardService.findDashboardById(tenantId, dashboard.getId())).thenReturn(dashboard);

        service.removeDevicePackageEntities(user, item.getId(), newVersionId);

        InOrder order = inOrder(tbDashboardService, tbDeviceService, iotHubInstalledItemService);
        order.verify(tbDashboardService).delete(dashboard, user);
        order.verify(tbDeviceService).delete(device, user);
        order.verify(iotHubInstalledItemService).save(tenantId, item);
        // The item keeps its version until the wizard registers the new one, and lists nothing to remove again.
        assertThat(item.getItemVersionId()).isEqualTo(versionId);
        assertThat(item.getVersion()).isEqualTo("1.0.1");
        assertThat(descriptor.getCreatedEntityIds()).isNull();
        assertThat(descriptor.getDashboardId()).isNull();
        // The form values stay for the wizard of the new version; the removed entities' outputs go.
        assertThat(descriptor.getInstallState()).containsOnlyKeys("Configuration");
        assertThat(descriptor.getInstallState().get("Configuration").get("formValues").get("gatewayName").asText()).isEqualTo("GW");
        verify(iotHubInstalledItemService, never()).deleteById(any(), any());
    }

    @Test
    void registerDeviceInstall_replace_updatesTheSameItemToTheNewVersion() {
        mockTenant();
        IotHubInstalledItem item = installedItem(UUID.randomUUID());
        item.setItemType("DEVICE");
        item.setItemVersionId(UUID.randomUUID());
        item.setVersion("1.0.1");
        item.setDescriptor(new DeviceInstalledItemDescriptor());
        String newVersionId = UUID.randomUUID().toString();
        String itemId = UUID.randomUUID().toString();
        item.setItemId(UUID.fromString(itemId));
        when(iotHubInstalledItemService.findById(tenantId, item.getId())).thenReturn(item);
        when(iotHubRestClient.getVersionInfo(newVersionId)).thenReturn(version(newVersionId, itemId, "DEVICE", "Gateway", "1.0.2"));
        DeviceInstalledItemDescriptor newDescriptor = new DeviceInstalledItemDescriptor();
        newDescriptor.setCreatedEntityIds(List.<EntityId>of(new DeviceId(UUID.randomUUID())));

        when(iotHubInstalledItemService.save(tenantId, item)).thenReturn(item);

        InstallItemVersionResult result = service.registerDeviceInstall(user, newVersionId, newDescriptor, item.getId());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getInstalledItemId()).isEqualTo(item.getId().getId());
        verify(iotHubInstalledItemService).save(tenantId, item);
        assertThat(item.getId()).isNotNull();
        assertThat(item.getItemVersionId()).isEqualTo(UUID.fromString(newVersionId));
        assertThat(item.getVersion()).isEqualTo("1.0.2");
        assertThat(item.getDescriptor()).isSameAs(newDescriptor);
    }

    @Test
    void removeDevicePackageEntities_notADevicePackage_refused() {
        mockTenant();
        IotHubInstalledItem item = installedItem(UUID.randomUUID());
        item.setItemType("RULE_CHAIN");
        item.setDescriptor(new RuleChainInstalledItemDescriptor());
        when(iotHubInstalledItemService.findById(tenantId, item.getId())).thenReturn(item);

        assertThatThrownBy(() -> service.removeDevicePackageEntities(user, item.getId(), UUID.randomUUID().toString()))
                .isInstanceOf(IllegalArgumentException.class);
        verify(iotHubInstalledItemService, never()).save(any(), any());
    }

    @Test
    void registerDeviceInstall_replace_keepsTrackingTheEntitiesTheItemTracks() {
        mockTenant();
        String itemId = UUID.randomUUID().toString();
        EntityId gateway = new DeviceId(UUID.randomUUID());
        DashboardId dashboard = new DashboardId(UUID.randomUUID());
        DeviceInstalledItemDescriptor tracked = new DeviceInstalledItemDescriptor();
        tracked.setCreatedEntityIds(List.of(gateway, dashboard));
        tracked.setDashboardId(dashboard);
        IotHubInstalledItem item = installedItem(UUID.randomUUID());
        item.setItemType("DEVICE");
        item.setItemId(UUID.fromString(itemId));
        item.setDescriptor(tracked);
        String newVersionId = UUID.randomUUID().toString();
        when(iotHubInstalledItemService.findById(tenantId, item.getId())).thenReturn(item);
        when(iotHubRestClient.getVersionInfo(newVersionId)).thenReturn(version(newVersionId, itemId, "DEVICE", "Gateway", "1.0.2"));
        when(iotHubInstalledItemService.save(tenantId, item)).thenReturn(item);
        EntityId device = new DeviceId(UUID.randomUUID());
        DeviceInstalledItemDescriptor next = new DeviceInstalledItemDescriptor();
        next.setCreatedEntityIds(List.of(device));

        InstallItemVersionResult result = service.registerDeviceInstall(user, newVersionId, next, item.getId());

        // A registration that leaves tracked entities out (another group, another connection method) still tracks them.
        assertThat(result.isSuccess()).isTrue();
        assertThat(next.getCreatedEntityIds()).containsExactlyInAnyOrder(device, gateway, dashboard);
        assertThat(next.getDashboardId()).isEqualTo(dashboard);
    }

    @Test
    void registerDeviceInstall_replacedItemGone_savesANewItem() {
        mockTenant();
        IotHubInstalledItemId goneId = new IotHubInstalledItemId(UUID.randomUUID());
        String versionId = UUID.randomUUID().toString();
        when(iotHubRestClient.getVersionInfo(versionId))
                .thenReturn(version(versionId, UUID.randomUUID().toString(), "DEVICE", "Gateway", "1.0.2"));
        when(iotHubInstalledItemService.save(eq(tenantId), any())).thenAnswer(inv -> {
            IotHubInstalledItem saved = inv.getArgument(1);
            saved.setId(new IotHubInstalledItemId(UUID.randomUUID()));
            return saved;
        });

        InstallItemVersionResult result = service.registerDeviceInstall(user, versionId, new DeviceInstalledItemDescriptor(), goneId);

        // Deleted from another tab during the update: the new entities are tracked by a new item, Retry is not stuck.
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getInstalledItemId()).isNotEqualTo(goneId.getId());
    }

    @Test
    void registerDeviceInstall_replace_versionOfAnotherItem_refused() {
        mockTenant();
        IotHubInstalledItem item = installedItem(UUID.randomUUID());
        item.setItemType("DEVICE");
        item.setItemId(UUID.randomUUID());
        item.setDescriptor(new DeviceInstalledItemDescriptor());
        String newVersionId = UUID.randomUUID().toString();
        when(iotHubInstalledItemService.findById(tenantId, item.getId())).thenReturn(item);
        when(iotHubRestClient.getVersionInfo(newVersionId))
                .thenReturn(version(newVersionId, UUID.randomUUID().toString(), "DEVICE", "Other", "2.0.0"));

        InstallItemVersionResult result = service.registerDeviceInstall(user, newVersionId, new DeviceInstalledItemDescriptor(), item.getId());

        assertThat(result.isSuccess()).isFalse();
        verify(iotHubInstalledItemService, never()).save(any(), any());
    }

    @Test
    void registerDeviceInstall_replace_nextGroupWithAllTrackedEntities_updatesTheItem() {
        mockTenant();
        String itemId = UUID.randomUUID().toString();
        EntityId gateway = new DeviceId(UUID.randomUUID());
        EntityId dashboard = new DashboardId(UUID.randomUUID());
        DeviceInstalledItemDescriptor firstGroup = new DeviceInstalledItemDescriptor();
        firstGroup.setCreatedEntityIds(List.of(gateway));
        IotHubInstalledItem item = installedItem(UUID.randomUUID());
        item.setItemType("DEVICE");
        item.setItemId(UUID.fromString(itemId));
        item.setDescriptor(firstGroup);
        String versionId = UUID.randomUUID().toString();
        when(iotHubInstalledItemService.findById(tenantId, item.getId())).thenReturn(item);
        when(iotHubRestClient.getVersionInfo(versionId)).thenReturn(version(versionId, itemId, "DEVICE", "Gateway", "1.0.2"));
        when(iotHubInstalledItemService.save(tenantId, item)).thenReturn(item);
        // The wizard registers after each group of entities with everything it created so far.
        DeviceInstalledItemDescriptor bothGroups = new DeviceInstalledItemDescriptor();
        bothGroups.setCreatedEntityIds(List.of(gateway, dashboard));

        InstallItemVersionResult result = service.registerDeviceInstall(user, versionId, bothGroups, item.getId());

        assertThat(result.isSuccess()).isTrue();
        assertThat(item.getDescriptor()).isSameAs(bothGroups);
    }

    @Test
    void removeDevicePackageEntities_entityOfAnotherTenant_notDeleted() {
        mockTenant();
        Device foreignDevice = new Device(new DeviceId(UUID.randomUUID()));
        foreignDevice.setTenantId(TenantId.fromUUID(UUID.randomUUID()));
        DeviceInstalledItemDescriptor descriptor = new DeviceInstalledItemDescriptor();
        descriptor.setCreatedEntityIds(List.<EntityId>of(foreignDevice.getId()));
        IotHubInstalledItem item = installedItem(UUID.randomUUID());
        item.setItemType("DEVICE");
        item.setDescriptor(descriptor);
        when(iotHubInstalledItemService.findById(tenantId, item.getId())).thenReturn(item);
        String newVersionId = UUID.randomUUID().toString();
        item.setItemId(UUID.randomUUID());
        when(iotHubRestClient.getVersionInfo(newVersionId))
                .thenReturn(version(newVersionId, item.getItemId().toString(), "DEVICE", "Gateway", "1.0.2"));
        when(deviceService.findDeviceById(tenantId, foreignDevice.getId())).thenReturn(foreignDevice);

        service.removeDevicePackageEntities(user, item.getId(), newVersionId);

        verify(tbDeviceService, never()).delete(any(Device.class), any());
    }

    @Test
    void removeDevicePackageEntities_entityNotRemoved_staysTrackedAndFails() throws Exception {
        mockTenant();
        Device device = new Device(new DeviceId(UUID.randomUUID()));
        device.setTenantId(tenantId);
        Dashboard dashboard = new Dashboard(new DashboardId(UUID.randomUUID()));
        dashboard.setTenantId(tenantId);
        DeviceInstalledItemDescriptor descriptor = new DeviceInstalledItemDescriptor();
        descriptor.setCreatedEntityIds(List.<EntityId>of(device.getId(), dashboard.getId()));
        descriptor.setDashboardId(dashboard.getId());
        IotHubInstalledItem item = installedItem(UUID.randomUUID());
        item.setItemType("DEVICE");
        item.setVersion("1.0.1");
        item.setItemId(UUID.randomUUID());
        item.setDescriptor(descriptor);
        when(iotHubInstalledItemService.findById(tenantId, item.getId())).thenReturn(item);
        String newVersionId = UUID.randomUUID().toString();
        when(iotHubRestClient.getVersionInfo(newVersionId))
                .thenReturn(version(newVersionId, item.getItemId().toString(), "DEVICE", "Gateway", "1.0.2"));
        when(deviceService.findDeviceById(tenantId, device.getId())).thenReturn(device);
        when(dashboardService.findDashboardById(tenantId, dashboard.getId())).thenReturn(dashboard);
        doThrow(new RuntimeException("dashboard is in use")).when(tbDashboardService).delete(dashboard, user);

        assertThatThrownBy(() -> service.removeDevicePackageEntities(user, item.getId(), newVersionId))
                .hasMessage("not removed: DASHBOARD (dashboard is in use)");

        // The rest is removed; the one left stays tracked, so Retry, a later update or Delete removes it.
        verify(tbDeviceService).delete(device, user);
        verify(iotHubInstalledItemService).save(tenantId, item);
        assertThat(descriptor.getCreatedEntityIds()).containsExactly(dashboard.getId());
        assertThat(descriptor.getDashboardId()).isEqualTo(dashboard.getId());
        assertThat(item.getVersion()).isEqualTo("1.0.1");
    }

    @Test
    void removeDevicePackageEntities_saveFailsAfterRemoving_notARefusal() throws Exception {
        mockTenant();
        Device device = new Device(new DeviceId(UUID.randomUUID()));
        device.setTenantId(tenantId);
        DeviceInstalledItemDescriptor descriptor = new DeviceInstalledItemDescriptor();
        descriptor.setCreatedEntityIds(List.<EntityId>of(device.getId()));
        IotHubInstalledItem item = installedItem(UUID.randomUUID());
        item.setItemType("DEVICE");
        item.setVersion("1.0.1");
        item.setItemId(UUID.randomUUID());
        item.setDescriptor(descriptor);
        when(iotHubInstalledItemService.findById(tenantId, item.getId())).thenReturn(item);
        String newVersionId = UUID.randomUUID().toString();
        when(iotHubRestClient.getVersionInfo(newVersionId))
                .thenReturn(version(newVersionId, item.getItemId().toString(), "DEVICE", "Gateway", "1.0.2"));
        when(deviceService.findDeviceById(tenantId, device.getId())).thenReturn(device);
        doThrow(new IllegalArgumentException("invalid item")).when(iotHubInstalledItemService).save(tenantId, item);

        // The device is already removed: the failure must not look like a refusal (a 400) to the wizard.
        assertThatThrownBy(() -> service.removeDevicePackageEntities(user, item.getId(), newVersionId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("invalid item");
        verify(tbDeviceService).delete(device, user);
    }

    @Test
    void removeDevicePackageEntities_entityUsedAsItWas_neitherRemovedNorTracked() throws Exception {
        mockTenant();
        Device device = new Device(new DeviceId(UUID.randomUUID()));
        device.setTenantId(tenantId);
        DeviceProfileId defaultProfileId = new DeviceProfileId(UUID.randomUUID());
        DeviceInstalledItemDescriptor descriptor = new DeviceInstalledItemDescriptor();
        descriptor.setCreatedEntityIds(List.<EntityId>of(defaultProfileId, device.getId()));
        descriptor.setInstallState(new LinkedHashMap<>(Map.of(
                "default", JacksonUtil.toJsonNode("{\"entityOutput\":{\"id\":\"" + defaultProfileId.getId() + "\"},\"resolution\":\"use-existing\"}"),
                "Gateway", JacksonUtil.toJsonNode("{\"entityOutput\":{\"id\":\"" + device.getId().getId() + "\"},\"resolution\":\"created\"}"))));
        IotHubInstalledItem item = installedItem(UUID.randomUUID());
        item.setItemType("DEVICE");
        item.setItemId(UUID.randomUUID());
        item.setDescriptor(descriptor);
        when(iotHubInstalledItemService.findById(tenantId, item.getId())).thenReturn(item);
        String newVersionId = UUID.randomUUID().toString();
        when(iotHubRestClient.getVersionInfo(newVersionId))
                .thenReturn(version(newVersionId, item.getItemId().toString(), "DEVICE", "Gateway", "1.0.2"));
        when(deviceService.findDeviceById(tenantId, device.getId())).thenReturn(device);

        service.removeDevicePackageEntities(user, item.getId(), newVersionId);

        // The profile the user chose to use as it was (here the default one, which cannot be removed) is the user's.
        verify(tbDeviceService).delete(device, user);
        verify(deviceProfileService, never()).findDeviceProfileById(any(), any());
        assertThat(descriptor.getCreatedEntityIds()).isNull();
    }

    @Test
    void deleteInstalledItem_devicePackage_keepsEntityUsedAsItWas() {
        mockTenant();
        DeviceProfileId usersProfileId = new DeviceProfileId(UUID.randomUUID());
        DeviceInstalledItemDescriptor descriptor = new DeviceInstalledItemDescriptor();
        descriptor.setCreatedEntityIds(List.<EntityId>of(usersProfileId));
        descriptor.setInstallState(new LinkedHashMap<>(Map.of(
                "Profile", JacksonUtil.toJsonNode("{\"entityOutput\":{\"id\":\"" + usersProfileId.getId() + "\"},\"resolution\":\"use-existing\"}"))));
        IotHubInstalledItem item = installedItem(UUID.randomUUID());
        item.setItemType("DEVICE");
        item.setDescriptor(descriptor);
        when(iotHubInstalledItemService.findById(tenantId, item.getId())).thenReturn(item);

        service.deleteInstalledItem(user, item.getId());

        verify(deviceProfileService, never()).findDeviceProfileById(any(), any());
        verify(iotHubInstalledItemService).deleteById(tenantId, item.getId());
    }

    @Test
    void removeDevicePackageEntities_versionOfAnotherItem_refusedBeforeRemoving() {
        mockTenant();
        DeviceInstalledItemDescriptor descriptor = new DeviceInstalledItemDescriptor();
        descriptor.setCreatedEntityIds(List.<EntityId>of(new DeviceId(UUID.randomUUID())));
        IotHubInstalledItem item = installedItem(UUID.randomUUID());
        item.setItemType("DEVICE");
        item.setItemId(UUID.randomUUID());
        item.setDescriptor(descriptor);
        String otherVersionId = UUID.randomUUID().toString();
        when(iotHubInstalledItemService.findById(tenantId, item.getId())).thenReturn(item);
        when(iotHubRestClient.getVersionInfo(otherVersionId))
                .thenReturn(version(otherVersionId, UUID.randomUUID().toString(), "DEVICE", "Other", "2.0.0"));

        // The registration of that version would be refused: nothing may be removed for it.
        assertThatThrownBy(() -> service.removeDevicePackageEntities(user, item.getId(), otherVersionId))
                .isInstanceOf(IllegalArgumentException.class);
        verify(tbDeviceService, never()).delete(any(Device.class), any());
        verify(iotHubInstalledItemService, never()).save(any(), any());
    }

    @Test
    void removeDevicePackageEntities_iotHubUnavailable_refusedBeforeRemoving() {
        mockTenant();
        DeviceInstalledItemDescriptor descriptor = new DeviceInstalledItemDescriptor();
        descriptor.setCreatedEntityIds(List.<EntityId>of(new DeviceId(UUID.randomUUID())));
        IotHubInstalledItem item = installedItem(UUID.randomUUID());
        item.setItemType("DEVICE");
        item.setItemId(UUID.randomUUID());
        item.setDescriptor(descriptor);
        String newVersionId = UUID.randomUUID().toString();
        when(iotHubInstalledItemService.findById(tenantId, item.getId())).thenReturn(item);
        when(iotHubRestClient.getVersionInfo(newVersionId)).thenThrow(new ResourceAccessException("Connection refused"));

        // A 400: nothing was removed, so the wizard does not warn that entities are gone.
        assertThatThrownBy(() -> service.removeDevicePackageEntities(user, item.getId(), newVersionId))
                .isInstanceOf(IllegalArgumentException.class);
        verify(tbDeviceService, never()).delete(any(Device.class), any());
        verify(iotHubInstalledItemService, never()).save(any(), any());
    }
}
