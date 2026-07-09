/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.IotHubInstalledItemId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.iot_hub.DashboardInstalledItemDescriptor;
import org.thingsboard.server.common.data.iot_hub.IotHubInstalledItem;
import org.thingsboard.server.dao.iot_hub.IotHubInstalledItemService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;
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
}
