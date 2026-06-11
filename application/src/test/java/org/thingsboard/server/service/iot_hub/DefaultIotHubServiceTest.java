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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.iot_hub.IotHubInstalledItemService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

    @InjectMocks
    private DefaultIotHubService service;

    private void mockTenant() {
        when(user.getTenantId()).thenReturn(tenantId);
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
        when(iotHubInstalledItemService.findInstalledItemIdsByTenantId(tenantId)).thenReturn(List.of());

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
        when(iotHubInstalledItemService.findInstalledItemIdsByTenantId(tenantId)).thenReturn(List.of());

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
        when(iotHubInstalledItemService.findInstalledItemIdsByTenantId(tenantId)).thenReturn(List.of());

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
        when(iotHubInstalledItemService.findInstalledItemIdsByTenantId(tenantId)).thenReturn(List.of());

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
                .thenReturn(version(versionId, rootItemId.toString(), "DASHBOARD", "Root", "1.0"));
        when(iotHubInstalledItemService.findInstalledItemIdsByTenantId(tenantId)).thenReturn(List.of(rootItemId));

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
        verify(iotHubInstalledItemService, never()).findInstalledItemIdsByTenantId(any());
    }

    @Test
    void installPlan_missingEntry_collectedAndNotInstalled() {
        mockTenant();
        InstallPlanEntry missing = new InstallPlanEntry();
        missing.setItemId(UUID.randomUUID().toString());
        missing.setStatus(InstallPlanEntry.Status.MISSING);
        when(iotHubInstalledItemService.findInstalledItemIdsByTenantId(tenantId)).thenReturn(List.of());

        InstallPlanResult result = service.installPlan(user, new InstallPlan(null, List.of(missing)), null, null);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMissingItemIds()).containsExactly(missing.getItemId());
        assertThat(result.getRootDescriptor()).isNull();
        verify(iotHubRestClient, never()).getVersionInfo(anyString());
    }

    @Test
    void installPlan_stalePlan_skipsItemInstalledSinceResolution() {
        mockTenant();
        UUID itemId = UUID.randomUUID();
        InstallPlanEntry entry = new InstallPlanEntry();
        entry.setItemId(itemId.toString());
        entry.setVersionId(UUID.randomUUID().toString());
        entry.setStatus(InstallPlanEntry.Status.WILL_INSTALL);
        entry.setRoot(true);
        // The item is reported as already installed at install time, even though the plan says WILL_INSTALL.
        when(iotHubInstalledItemService.findInstalledItemIdsByTenantId(tenantId)).thenReturn(List.of(itemId));

        InstallPlanResult result = service.installPlan(user, new InstallPlan(null, List.of(entry)), null, null);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getEntries().get(0).getStatus()).isEqualTo(InstallPlanEntry.Status.ALREADY_INSTALLED);
        // No marketplace fetch happened — the duplicate install was avoided.
        verify(iotHubRestClient, never()).getVersionInfo(anyString());
    }
}
