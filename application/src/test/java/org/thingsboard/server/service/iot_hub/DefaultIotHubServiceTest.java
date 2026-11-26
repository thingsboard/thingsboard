// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.iot_hub;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.AlarmCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.ArgumentsBasedCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.SimpleCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.TimeSeriesOutput;
import org.thingsboard.server.common.data.cf.configuration.geofencing.EntityCoordinates;
import org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingReportStrategy;
import org.thingsboard.server.common.data.cf.configuration.geofencing.ZoneGroupConfiguration;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.IotHubInstalledItemId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.iot_hub.AlarmRuleInstalledItemDescriptor;
import org.thingsboard.server.common.data.iot_hub.CalculatedFieldInstalledItemDescriptor;
import org.thingsboard.server.common.data.iot_hub.DeviceInstalledItemDescriptor;
import org.thingsboard.server.common.data.iot_hub.IotHubInstalledItem;
import org.thingsboard.server.common.data.iot_hub.RuleChainInstalledItemDescriptor;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.iot_hub.IotHubInstalledItemService;
import org.thingsboard.server.service.entitiy.cf.TbCalculatedFieldService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.service.entitiy.dashboard.TbDashboardService;
import org.thingsboard.server.service.entitiy.converter.TbConverterService;
import org.thingsboard.server.service.entitiy.device.TbDeviceService;
import org.thingsboard.server.service.entitiy.device.profile.TbDeviceProfileService;
import org.thingsboard.server.service.rule.TbRuleChainService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import static org.mockito.Mockito.lenient;
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
    private TbCalculatedFieldService tbCalculatedFieldService;
    @Mock
    private CalculatedFieldService calculatedFieldService;
    @Mock
    private DeviceService deviceService;
    @Mock
    private EntityService entityService;
    @Mock
    private TbDeviceService tbDeviceService;
    @Mock
    private DashboardService dashboardService;
    @Mock
    private TbDashboardService tbDashboardService;
    @Mock
    private IntegrationService integrationService;
    @Mock
    private DeviceProfileService deviceProfileService;
    @Mock
    private TbDeviceProfileService tbDeviceProfileService;
    @Mock
    private ConverterService converterService;
    @Mock
    private TbConverterService tbConverterService;
    @Mock
    private DevicePackageEntities devicePackageEntities;
    @Mock
    private RuleChainService ruleChainService;
    @Mock
    private TbRuleChainService tbRuleChainService;

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
        when(iotHubRestClient.getVersionInfo(versionId)).thenReturn(version(versionId, itemId, "RULE_CHAIN", "Root", "1.0"));
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
        ObjectNode root = version(versionId, rootItemId, "RULE_CHAIN", "Root", "1.0");
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
        ObjectNode root = version(versionId, rootItemId, "RULE_CHAIN", "Root", "1.0");
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
        ObjectNode root = version(versionId, rootItemId, "RULE_CHAIN", "Root", "1.0");
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
        RuleChainInstalledItemDescriptor rootDescriptor = new RuleChainInstalledItemDescriptor();
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
    void doInstallVersion_calculatedField_mapsTenantReferencesToInstallingTenant() throws Exception {
        mockTenant();
        String versionId = UUID.randomUUID().toString();
        when(iotHubRestClient.getVersionInfo(versionId))
                .thenReturn(version(versionId, UUID.randomUUID().toString(), "CALCULATED_FIELD", "CF", "1.0"));
        when(iotHubRestClient.getVersionFileData(versionId)).thenReturn(calculatedFieldFileData());
        when(tbCalculatedFieldService.save(any(), eq(user))).thenAnswer(invocation -> invocation.getArgument(0));
        when(iotHubInstalledItemService.save(eq(tenantId), any())).thenAnswer(invocation -> invocation.getArgument(1));

        service.doInstallVersion(user, versionId, null, null);

        ArgumentCaptor<CalculatedField> captor = ArgumentCaptor.forClass(CalculatedField.class);
        verify(tbCalculatedFieldService).save(captor.capture(), eq(user));
        assertThat(((ArgumentsBasedCalculatedFieldConfiguration) captor.getValue().getConfiguration()).getArguments().get("T").getRefEntityId()).isEqualTo(tenantId);
    }

    @Test
    void updateItemVersion_calculatedField_mapsTenantReferencesToInstallingTenant() throws Exception {
        mockTenant();
        String versionId = UUID.randomUUID().toString();
        CalculatedFieldId calculatedFieldId = new CalculatedFieldId(UUID.randomUUID());
        CalculatedFieldInstalledItemDescriptor descriptor = new CalculatedFieldInstalledItemDescriptor();
        descriptor.setCalculatedFieldId(calculatedFieldId);
        IotHubInstalledItem item = installedItem(UUID.randomUUID());
        item.setItemType("CALCULATED_FIELD");
        item.setItemVersionId(UUID.randomUUID());
        item.setDescriptor(descriptor);
        when(iotHubInstalledItemService.findById(tenantId, item.getId())).thenReturn(item);
        when(iotHubRestClient.getVersionInfo(anyString()))
                .thenReturn(version(versionId, UUID.randomUUID().toString(), "CALCULATED_FIELD", "CF", "2.0"));
        when(iotHubRestClient.getVersionFileData(versionId)).thenReturn(calculatedFieldFileData());
        CalculatedField existing = new CalculatedField(calculatedFieldId);
        existing.setTenantId(tenantId);
        when(calculatedFieldService.findById(tenantId, calculatedFieldId)).thenReturn(existing);

        UpdateItemVersionResult result = service.updateItemVersion(user, item.getId(), versionId, true, null);

        assertThat(result.isSuccess()).isTrue();
        ArgumentCaptor<CalculatedField> captor = ArgumentCaptor.forClass(CalculatedField.class);
        verify(tbCalculatedFieldService).save(captor.capture(), eq(user));
        assertThat(((ArgumentsBasedCalculatedFieldConfiguration) captor.getValue().getConfiguration()).getArguments().get("T").getRefEntityId()).isEqualTo(tenantId);
    }

    @Test
    void doInstallVersion_alarmRule_mapsTenantReferencesToInstallingTenant() throws Exception {
        mockTenant();
        String versionId = UUID.randomUUID().toString();
        when(iotHubRestClient.getVersionInfo(versionId))
                .thenReturn(version(versionId, UUID.randomUUID().toString(), "ALARM_RULE", "Alarm", "1.0"));
        when(iotHubRestClient.getVersionFileData(versionId)).thenReturn(alarmRuleFileData());
        when(tbCalculatedFieldService.save(any(), eq(user))).thenAnswer(invocation -> invocation.getArgument(0));
        when(iotHubInstalledItemService.save(eq(tenantId), any())).thenAnswer(invocation -> invocation.getArgument(1));

        service.doInstallVersion(user, versionId, null, null);

        ArgumentCaptor<CalculatedField> captor = ArgumentCaptor.forClass(CalculatedField.class);
        verify(tbCalculatedFieldService).save(captor.capture(), eq(user));
        assertThat(((ArgumentsBasedCalculatedFieldConfiguration) captor.getValue().getConfiguration()).getArguments().get("T").getRefEntityId()).isEqualTo(tenantId);
    }

    @Test
    void updateItemVersion_alarmRule_mapsTenantReferencesToInstallingTenant() throws Exception {
        mockTenant();
        String versionId = UUID.randomUUID().toString();
        CalculatedFieldId calculatedFieldId = new CalculatedFieldId(UUID.randomUUID());
        AlarmRuleInstalledItemDescriptor descriptor = new AlarmRuleInstalledItemDescriptor();
        descriptor.setCalculatedFieldId(calculatedFieldId);
        IotHubInstalledItem item = installedItem(UUID.randomUUID());
        item.setItemType("ALARM_RULE");
        item.setItemVersionId(UUID.randomUUID());
        item.setDescriptor(descriptor);
        when(iotHubInstalledItemService.findById(tenantId, item.getId())).thenReturn(item);
        when(iotHubRestClient.getVersionInfo(anyString()))
                .thenReturn(version(versionId, UUID.randomUUID().toString(), "ALARM_RULE", "Alarm", "2.0"));
        when(iotHubRestClient.getVersionFileData(versionId)).thenReturn(alarmRuleFileData());
        CalculatedField existing = new CalculatedField(calculatedFieldId);
        existing.setTenantId(tenantId);
        when(calculatedFieldService.findById(tenantId, calculatedFieldId)).thenReturn(existing);

        UpdateItemVersionResult result = service.updateItemVersion(user, item.getId(), versionId, true, null);

        assertThat(result.isSuccess()).isTrue();
        ArgumentCaptor<CalculatedField> captor = ArgumentCaptor.forClass(CalculatedField.class);
        verify(tbCalculatedFieldService).save(captor.capture(), eq(user));
        assertThat(((ArgumentsBasedCalculatedFieldConfiguration) captor.getValue().getConfiguration()).getArguments().get("T").getRefEntityId()).isEqualTo(tenantId);
    }

    @Test
    void doInstallVersion_geofencingCalculatedField_mapsZoneGroupTenantReferencesToInstallingTenant() throws Exception {
        mockTenant();
        String versionId = UUID.randomUUID().toString();
        when(iotHubRestClient.getVersionInfo(versionId))
                .thenReturn(version(versionId, UUID.randomUUID().toString(), "CALCULATED_FIELD", "Geofencing", "1.0"));
        when(iotHubRestClient.getVersionFileData(versionId)).thenReturn(geofencingFileData());
        when(tbCalculatedFieldService.save(any(), eq(user))).thenAnswer(invocation -> invocation.getArgument(0));
        when(iotHubInstalledItemService.save(eq(tenantId), any())).thenAnswer(invocation -> invocation.getArgument(1));

        service.doInstallVersion(user, versionId, null, null);

        ArgumentCaptor<CalculatedField> captor = ArgumentCaptor.forClass(CalculatedField.class);
        verify(tbCalculatedFieldService).save(captor.capture(), eq(user));
        assertThat(((GeofencingCalculatedFieldConfiguration) captor.getValue().getConfiguration()).getZoneGroups().get("zones").getRefEntityId()).isEqualTo(tenantId);
    }

    private byte[] calculatedFieldFileData() {
        SimpleCalculatedFieldConfiguration configuration = new SimpleCalculatedFieldConfiguration();
        configuration.setArguments(Map.of("T", otherTenantArgument()));
        configuration.setExpression("T");
        return fileData(CalculatedFieldType.SIMPLE, configuration);
    }

    private byte[] alarmRuleFileData() {
        AlarmCalculatedFieldConfiguration configuration = new AlarmCalculatedFieldConfiguration();
        configuration.setArguments(Map.of("T", otherTenantArgument()));
        configuration.setCreateRules(Map.of());
        return fileData(CalculatedFieldType.ALARM, configuration);
    }

    private byte[] geofencingFileData() {
        ZoneGroupConfiguration zoneGroup = new ZoneGroupConfiguration("perimeter", GeofencingReportStrategy.REPORT_PRESENCE_STATUS_ONLY, false);
        zoneGroup.setRefEntityId(TenantId.fromUUID(UUID.randomUUID()));
        GeofencingCalculatedFieldConfiguration configuration = new GeofencingCalculatedFieldConfiguration();
        configuration.setEntityCoordinates(new EntityCoordinates("latitude", "longitude"));
        configuration.setZoneGroups(Map.of("zones", zoneGroup));
        configuration.setOutput(new TimeSeriesOutput());
        return fileData(CalculatedFieldType.GEOFENCING, configuration);
    }

    private Argument otherTenantArgument() {
        Argument argument = new Argument();
        argument.setRefEntityId(TenantId.fromUUID(UUID.randomUUID()));
        argument.setRefEntityKey(new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null));
        return argument;
    }

    private byte[] fileData(CalculatedFieldType type, CalculatedFieldConfiguration configuration) {
        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(new DeviceId(UUID.randomUUID()));
        calculatedField.setType(type);
        calculatedField.setName("CF");
        calculatedField.setConfiguration(configuration);
        return JacksonUtil.toString(calculatedField).getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void deleteInstalledItem_devicePackage_removesEachEntityBeforeWhatItRefersTo() throws Exception {
        mockTenant();
        DeviceProfile profile = new DeviceProfile(new DeviceProfileId(UUID.randomUUID()));
        Dashboard dashboard = new Dashboard(new DashboardId(UUID.randomUUID()));
        profile.setDefaultDashboardId(dashboard.getId());
        Integration integration = new Integration(new IntegrationId(UUID.randomUUID()));
        Converter converter = new Converter(new ConverterId(UUID.randomUUID()));
        profile.setTenantId(tenantId);
        dashboard.setTenantId(tenantId);
        integration.setTenantId(tenantId);
        converter.setTenantId(tenantId);
        // The profile and the integration listed first: in reverse order of creation, the dashboard would go before the
        // profile that refers to it, and the converter before the integration that uses it.
        IotHubInstalledItem deleted = devicePackage(profile.getId(), dashboard.getId(), integration.getId(), converter.getId());
        when(iotHubInstalledItemService.findById(tenantId, deleted.getId())).thenReturn(deleted);
        when(iotHubInstalledItemService.findByTenantId(eq(tenantId), eq(List.of("DEVICE")), isNull(), any()))
                .thenReturn(new PageData<>(List.of(deleted), 1, 1, false));
        when(deviceProfileService.findDeviceProfileById(tenantId, profile.getId())).thenReturn(profile);
        noDevices(profile.getId());
        when(dashboardService.findDashboardById(tenantId, dashboard.getId())).thenReturn(dashboard);
        when(integrationService.findIntegrationById(tenantId, integration.getId())).thenReturn(integration);
        when(converterService.findConverterById(tenantId, converter.getId())).thenReturn(converter);

        service.deleteInstalledItem(user, deleted.getId());

        InOrder order = inOrder(tbDeviceProfileService, tbDashboardService, integrationService, tbConverterService);
        order.verify(tbDeviceProfileService).delete(profile, user);
        order.verify(tbDashboardService).delete(dashboard, user);
        order.verify(integrationService).deleteIntegration(tenantId, integration.getId());
        order.verify(tbConverterService).delete(converter, user);
    }

    @Test
    void deleteInstalledItem_devicePackage_keepsEntitiesAnotherDevicePackageUses() {
        mockTenant();
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        DashboardId sharedDashboardId = new DashboardId(UUID.randomUUID());
        IotHubInstalledItem deleted = devicePackage(deviceId, sharedDashboardId);
        IotHubInstalledItem other = devicePackage(sharedDashboardId);
        other.setItemName("Other package");
        when(iotHubInstalledItemService.findById(tenantId, deleted.getId())).thenReturn(deleted);
        when(iotHubInstalledItemService.findByTenantId(eq(tenantId), eq(List.of("DEVICE")), isNull(), any()))
                .thenReturn(new PageData<>(List.of(deleted, other), 1, 2, false));
        Device device = new Device(deviceId);
        device.setTenantId(tenantId);
        when(deviceService.findDeviceById(tenantId, deviceId)).thenReturn(device);

        service.deleteInstalledItem(user, deleted.getId());

        verify(tbDeviceService).delete(device, user);
        verify(dashboardService, never()).findDashboardById(any(), any());
        verify(tbDashboardService, never()).delete(any(), any());
        verify(iotHubInstalledItemService).deleteById(tenantId, deleted.getId());
    }

    @Test
    void deleteInstalledItem_devicePackage_removesItsMetadataEntriesFromTheKeptIntegration() {
        mockTenant();
        IntegrationId integrationId = new IntegrationId(UUID.randomUUID());
        IotHubInstalledItem deleted = devicePackage(integrationId);
        setIntegrationState(deleted, integrationId, Map.of("profile.milesight-iot-am308", "am308-custom"));
        IotHubInstalledItem other = devicePackage(integrationId);
        setIntegrationState(other, integrationId, Map.of("profile.lansitec-badge", "Badge"));
        when(iotHubInstalledItemService.findById(tenantId, deleted.getId())).thenReturn(deleted);
        when(iotHubInstalledItemService.findByTenantId(eq(tenantId), eq(List.of("DEVICE")), isNull(), any()))
                .thenReturn(new PageData<>(List.of(deleted, other), 1, 2, false));
        Integration current = integration(integrationId, Map.of("profile.milesight-iot-am308", "am308-custom",
                "profile.lansitec-badge", "Badge", "profile.added-by-user", "Mine"));
        when(integrationService.findIntegrationById(tenantId, integrationId)).thenReturn(current);

        service.deleteInstalledItem(user, deleted.getId());

        ArgumentCaptor<Integration> saved = ArgumentCaptor.forClass(Integration.class);
        verify(integrationService).saveIntegration(saved.capture());
        verify(integrationService, never()).deleteIntegration(any(), any());
        JsonNode metadata = saved.getValue().getConfiguration().get("metadata");
        assertThat(metadata.has("profile.milesight-iot-am308")).isFalse();
        assertThat(metadata.get("profile.lansitec-badge").asText()).isEqualTo("Badge");
        assertThat(metadata.get("profile.added-by-user").asText()).isEqualTo("Mine");
        // A copy is saved: the integration read may be the cache's own instance
        assertThat(current.getConfiguration().get("metadata").has("profile.milesight-iot-am308")).isTrue();
    }

    @Test
    void deleteInstalledItem_devicePackage_keepsTheMetadataEntryAnotherPackageAddedToo() {
        mockTenant();
        IntegrationId integrationId = new IntegrationId(UUID.randomUUID());
        IotHubInstalledItem deleted = devicePackage(integrationId);
        setIntegrationState(deleted, integrationId, Map.of("profile.milesight-iot-am308", "am308-custom"));
        IotHubInstalledItem other = devicePackage(integrationId);
        setIntegrationState(other, integrationId, Map.of("profile.milesight-iot-am308", "am308-custom"));
        when(iotHubInstalledItemService.findById(tenantId, deleted.getId())).thenReturn(deleted);
        when(iotHubInstalledItemService.findByTenantId(eq(tenantId), eq(List.of("DEVICE")), isNull(), any()))
                .thenReturn(new PageData<>(List.of(deleted, other), 1, 2, false));

        service.deleteInstalledItem(user, deleted.getId());

        // Nothing is left to remove once the other package's entry is subtracted: the integration isn't even read
        verify(integrationService, never()).findIntegrationById(any(), any());
        verify(integrationService, never()).saveIntegration(any());
        verify(integrationService, never()).deleteIntegration(any(), any());
    }

    @Test
    void deleteInstalledItem_devicePackage_removesItsMetadataEntriesFromTheIntegrationItTookAsItWas() {
        mockTenant();
        IntegrationId integrationId = new IntegrationId(UUID.randomUUID());
        IotHubInstalledItem deleted = devicePackage(integrationId);
        setIntegrationState(deleted, integrationId, Map.of("profile.milesight-iot-am308", "am308-custom"));
        ((DeviceInstalledItemDescriptor) deleted.getDescriptor()).setUsedExistingEntityIds(List.of(integrationId));
        when(iotHubInstalledItemService.findById(tenantId, deleted.getId())).thenReturn(deleted);
        when(iotHubInstalledItemService.findByTenantId(eq(tenantId), eq(List.of("DEVICE")), isNull(), any()))
                .thenReturn(new PageData<>(List.of(deleted), 1, 1, false));
        when(integrationService.findIntegrationById(tenantId, integrationId))
                .thenReturn(integration(integrationId, Map.of("profile.milesight-iot-am308", "am308-custom", "profile.added-by-user", "Mine")));

        service.deleteInstalledItem(user, deleted.getId());

        ArgumentCaptor<Integration> saved = ArgumentCaptor.forClass(Integration.class);
        verify(integrationService).saveIntegration(saved.capture());
        verify(integrationService, never()).deleteIntegration(any(), any());
        JsonNode metadata = saved.getValue().getConfiguration().get("metadata");
        assertThat(metadata.has("profile.milesight-iot-am308")).isFalse();
        assertThat(metadata.get("profile.added-by-user").asText()).isEqualTo("Mine");
    }

    @Test
    void deleteInstalledItem_devicePackage_leavesTheKeptIntegrationWithoutItsEntriesAsItIs() {
        mockTenant();
        IntegrationId integrationId = new IntegrationId(UUID.randomUUID());
        IotHubInstalledItem deleted = devicePackage(integrationId);
        setIntegrationState(deleted, integrationId, Map.of("profile.milesight-iot-am308", "am308-custom"));
        ((DeviceInstalledItemDescriptor) deleted.getDescriptor()).setUsedExistingEntityIds(List.of(integrationId));
        when(iotHubInstalledItemService.findById(tenantId, deleted.getId())).thenReturn(deleted);
        when(iotHubInstalledItemService.findByTenantId(eq(tenantId), eq(List.of("DEVICE")), isNull(), any()))
                .thenReturn(new PageData<>(List.of(deleted), 1, 1, false));
        when(integrationService.findIntegrationById(tenantId, integrationId))
                .thenReturn(integration(integrationId, Map.of("profile.lansitec-badge", "Badge")));

        service.deleteInstalledItem(user, deleted.getId());

        verify(integrationService, never()).saveIntegration(any());
        verify(integrationService, never()).deleteIntegration(any(), any());
    }

    @Test
    void deleteInstalledItem_devicePackageInstalledWithStateByName_deletesTheConverterWhoseStateWasReplaced() throws Exception {
        mockTenant();
        IntegrationId integrationId = new IntegrationId(UUID.randomUUID());
        Converter converter = new Converter(new ConverterId(UUID.randomUUID()));
        converter.setTenantId(tenantId);
        when(converterService.findConverterById(tenantId, converter.getId())).thenReturn(converter);
        IotHubInstalledItem deleted = devicePackage(converter.getId(), integrationId);
        // The converter and the integration steps shared the name: the integration's state replaced the converter's
        ObjectNode state = JacksonUtil.newObjectNode().put("resolution", "use-existing");
        state.set("entityOutput", JacksonUtil.newObjectNode().put("id", integrationId.getId().toString()));
        state.set("metadata", JacksonUtil.newObjectNode().put("profile.milesight-iot-am308", "AM308"));
        DeviceInstalledItemDescriptor descriptor = (DeviceInstalledItemDescriptor) deleted.getDescriptor();
        descriptor.setInstallState(Map.of("${integrationName}", state));
        descriptor.setUsedExistingEntityIds(List.of(integrationId));
        when(iotHubInstalledItemService.findById(tenantId, deleted.getId())).thenReturn(deleted);
        when(iotHubInstalledItemService.findByTenantId(eq(tenantId), eq(List.of("DEVICE")), isNull(), any()))
                .thenReturn(new PageData<>(List.of(deleted), 1, 1, false));
        when(integrationService.findIntegrationById(tenantId, integrationId))
                .thenReturn(integration(integrationId, Map.of("profile.milesight-iot-am308", "AM308", "profile.added-by-user", "Mine")));

        service.deleteInstalledItem(user, deleted.getId());

        // Found by createdEntityIds, not by the install state
        verify(tbConverterService).delete(converter, user);
        ArgumentCaptor<Integration> saved = ArgumentCaptor.forClass(Integration.class);
        verify(integrationService).saveIntegration(saved.capture());
        verify(integrationService, never()).deleteIntegration(any(), any());
        JsonNode metadata = saved.getValue().getConfiguration().get("metadata");
        assertThat(metadata.has("profile.milesight-iot-am308")).isFalse();
        assertThat(metadata.get("profile.added-by-user").asText()).isEqualTo("Mine");
    }

    @Test
    void findKeptEntities_devicePackage_namesTheSharedEntitiesAndCountsTheOtherPackages() {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        DashboardId sharedDashboardId = new DashboardId(UUID.randomUUID());
        DeviceProfileId sharedProfileId = new DeviceProfileId(UUID.randomUUID());
        IotHubInstalledItem checked = devicePackage(sharedProfileId, deviceId, sharedDashboardId);
        IotHubInstalledItem first = devicePackage(sharedProfileId, sharedDashboardId);
        IotHubInstalledItem second = devicePackage(sharedDashboardId);
        IotHubInstalledItem unrelated = devicePackage(new DeviceId(UUID.randomUUID()));
        when(iotHubInstalledItemService.findById(tenantId, checked.getId())).thenReturn(checked);
        when(iotHubInstalledItemService.findByTenantId(eq(tenantId), eq(List.of("DEVICE")), isNull(), any()))
                .thenReturn(new PageData<>(List.of(checked, first, second, unrelated), 1, 4, false));
        DeviceProfile sharedProfile = new DeviceProfile(sharedProfileId);
        sharedProfile.setTenantId(tenantId);
        when(deviceProfileService.findDeviceProfileById(tenantId, sharedProfileId)).thenReturn(sharedProfile);
        Dashboard sharedDashboard = new Dashboard(sharedDashboardId);
        sharedDashboard.setTenantId(tenantId);
        when(dashboardService.findDashboardById(tenantId, sharedDashboardId)).thenReturn(sharedDashboard);
        when(entityService.fetchEntityName(tenantId, sharedProfileId)).thenReturn(Optional.of("Shared profile"));
        when(entityService.fetchEntityName(tenantId, sharedDashboardId)).thenReturn(Optional.of("Shared dashboard"));

        InstalledItemKeptEntities kept = service.findKeptEntities(tenantId, checked.getId());

        assertThat(kept.getEntities()).containsExactly(
                new InstalledItemKeptEntities.KeptEntity(sharedProfileId, "Shared profile"),
                new InstalledItemKeptEntities.KeptEntity(sharedDashboardId, "Shared dashboard"));
        assertThat(kept.getUsedByItemsCount()).isEqualTo(2);
    }

    @Test
    void deleteInstalledItem_devicePackage_keepsEntitiesTheUserHadBefore() {
        mockTenant();
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        DashboardId usersDashboardId = new DashboardId(UUID.randomUUID());
        IotHubInstalledItem deleted = devicePackage(deviceId, usersDashboardId);
        ((DeviceInstalledItemDescriptor) deleted.getDescriptor()).setUsedExistingEntityIds(List.of(usersDashboardId));
        when(iotHubInstalledItemService.findById(tenantId, deleted.getId())).thenReturn(deleted);
        when(iotHubInstalledItemService.findByTenantId(eq(tenantId), eq(List.of("DEVICE")), isNull(), any()))
                .thenReturn(new PageData<>(List.of(deleted), 1, 1, false));
        Device device = new Device(deviceId);
        device.setTenantId(tenantId);
        when(deviceService.findDeviceById(tenantId, deviceId)).thenReturn(device);

        service.deleteInstalledItem(user, deleted.getId());

        verify(tbDeviceService).delete(device, user);
        verify(tbDashboardService, never()).delete(any(), any());
    }

    @Test
    void findKeptEntities_devicePackage_namesTheEntitiesTheUserHadBefore() {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        DashboardId usersDashboardId = new DashboardId(UUID.randomUUID());
        IotHubInstalledItem checked = devicePackage(deviceId, usersDashboardId);
        ((DeviceInstalledItemDescriptor) checked.getDescriptor()).setUsedExistingEntityIds(List.of(usersDashboardId));
        when(iotHubInstalledItemService.findById(tenantId, checked.getId())).thenReturn(checked);
        when(iotHubInstalledItemService.findByTenantId(eq(tenantId), eq(List.of("DEVICE")), isNull(), any()))
                .thenReturn(new PageData<>(List.of(checked), 1, 1, false));
        Dashboard usersDashboard = new Dashboard(usersDashboardId);
        usersDashboard.setTenantId(tenantId);
        when(dashboardService.findDashboardById(tenantId, usersDashboardId)).thenReturn(usersDashboard);
        when(entityService.fetchEntityName(tenantId, usersDashboardId)).thenReturn(Optional.of("My dashboard"));

        InstalledItemKeptEntities kept = service.findKeptEntities(tenantId, checked.getId());

        assertThat(kept.getEntities()).containsExactly(new InstalledItemKeptEntities.KeptEntity(usersDashboardId, "My dashboard"));
        assertThat(kept.getUsedByItemsCount()).isZero();
        assertThat(kept.getUsedExistingCount()).isEqualTo(1);
    }

    @Test
    void deleteInstalledItem_devicePackageInstalledBefore441_keepsEntitiesTheUserHadBefore() {
        mockTenant();
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        DashboardId usersDashboardId = new DashboardId(UUID.randomUUID());
        IotHubInstalledItem deleted = devicePackageInstalledBefore441(deviceId, usersDashboardId);
        when(iotHubInstalledItemService.findById(tenantId, deleted.getId())).thenReturn(deleted);
        when(iotHubInstalledItemService.findByTenantId(eq(tenantId), eq(List.of("DEVICE")), isNull(), any()))
                .thenReturn(new PageData<>(List.of(deleted), 1, 1, false));
        // Found, so only the use-existing rule keeps it
        Dashboard usersDashboard = new Dashboard(usersDashboardId);
        usersDashboard.setTenantId(tenantId);
        lenient().when(dashboardService.findDashboardById(tenantId, usersDashboardId)).thenReturn(usersDashboard);
        Device device = new Device(deviceId);
        device.setTenantId(tenantId);
        when(deviceService.findDeviceById(tenantId, deviceId)).thenReturn(device);

        service.deleteInstalledItem(user, deleted.getId());

        verify(tbDeviceService).delete(device, user);
        verify(tbDashboardService, never()).delete(any(), any());
    }

    @Test
    void findKeptEntities_devicePackageInstalledBefore441_namesTheEntitiesTheUserHadBefore() {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        DashboardId usersDashboardId = new DashboardId(UUID.randomUUID());
        IotHubInstalledItem checked = devicePackageInstalledBefore441(deviceId, usersDashboardId);
        when(iotHubInstalledItemService.findById(tenantId, checked.getId())).thenReturn(checked);
        when(iotHubInstalledItemService.findByTenantId(eq(tenantId), eq(List.of("DEVICE")), isNull(), any()))
                .thenReturn(new PageData<>(List.of(checked), 1, 1, false));
        Dashboard usersDashboard = new Dashboard(usersDashboardId);
        usersDashboard.setTenantId(tenantId);
        lenient().when(dashboardService.findDashboardById(tenantId, usersDashboardId)).thenReturn(usersDashboard);
        when(entityService.fetchEntityName(tenantId, usersDashboardId)).thenReturn(Optional.of("My dashboard"));

        InstalledItemKeptEntities kept = service.findKeptEntities(tenantId, checked.getId());

        assertThat(kept.getEntities()).containsExactly(new InstalledItemKeptEntities.KeptEntity(usersDashboardId, "My dashboard"));
        assertThat(kept.getUsedExistingCount()).isEqualTo(1);
    }

    /** An install made before 4.4.1: the dashboard the user took as it was is only in the install state, under the step name. */
    private IotHubInstalledItem devicePackageInstalledBefore441(DeviceId deviceId, DashboardId usersDashboardId) {
        IotHubInstalledItem installedItem = devicePackage(deviceId, usersDashboardId);
        ObjectNode device = JacksonUtil.newObjectNode().put("resolution", "created");
        device.set("entityOutput", JacksonUtil.newObjectNode().put("id", deviceId.getId().toString()));
        ObjectNode dashboard = JacksonUtil.newObjectNode().put("resolution", "use-existing");
        dashboard.set("entityOutput", JacksonUtil.newObjectNode().put("id", usersDashboardId.getId().toString()));
        ((DeviceInstalledItemDescriptor) installedItem.getDescriptor()).setInstallState(Map.of("Device", device, "Dashboard", dashboard));
        return installedItem;
    }

    @Test
    void findKeptEntities_devicePackageSharingNothing_keepsNothing() {
        IotHubInstalledItem checked = devicePackage(new DeviceId(UUID.randomUUID()));
        IotHubInstalledItem other = devicePackage(new DeviceId(UUID.randomUUID()));
        when(iotHubInstalledItemService.findById(tenantId, checked.getId())).thenReturn(checked);
        when(iotHubInstalledItemService.findByTenantId(eq(tenantId), eq(List.of("DEVICE")), isNull(), any()))
                .thenReturn(new PageData<>(List.of(checked, other), 1, 2, false));

        InstalledItemKeptEntities kept = service.findKeptEntities(tenantId, checked.getId());

        assertThat(kept.getEntities()).isEmpty();
        assertThat(kept.getUsedByItemsCount()).isZero();
    }

    @Test
    void updateItemVersion_devicePackage_deletesNothing() {
        mockTenant();
        IotHubInstalledItem installed = devicePackage(new DeviceId(UUID.randomUUID()), new DashboardId(UUID.randomUUID()));
        installed.setItemType("DEVICE");
        when(iotHubInstalledItemService.findById(tenantId, installed.getId())).thenReturn(installed);

        UpdateItemVersionResult result = service.updateItemVersion(user, installed.getId(), UUID.randomUUID().toString(), true, null);

        assertThat(result.isSuccess()).isFalse();
        verify(tbDeviceService, never()).delete(any(Device.class), any());
        verify(tbDashboardService, never()).delete(any(), any());
        verify(iotHubInstalledItemService, never()).save(any(), any());
    }

    private void setIntegrationState(IotHubInstalledItem item, IntegrationId integrationId, Map<String, String> addedMetadata) {
        ObjectNode state = JacksonUtil.newObjectNode().put("resolution", "created");
        state.set("entityOutput", JacksonUtil.newObjectNode().put("id", integrationId.getId().toString()));
        state.set("metadata", JacksonUtil.valueToTree(addedMetadata));
        ((DeviceInstalledItemDescriptor) item.getDescriptor()).setInstallState(Map.of("TTN", state));
    }

    private Integration integration(IntegrationId id, Map<String, String> metadata) {
        Integration integration = new Integration(id);
        integration.setTenantId(tenantId);
        ObjectNode configuration = JacksonUtil.newObjectNode();
        configuration.set("metadata", JacksonUtil.valueToTree(metadata));
        integration.setConfiguration(configuration);
        return integration;
    }

    @Test
    void findKeptEntities_devicePackage_namesTheProfileDevicesUseAndWhatItRefersTo_apartFromTheShared() {
        DeviceProfile profile = new DeviceProfile(new DeviceProfileId(UUID.randomUUID()));
        profile.setTenantId(tenantId);
        RuleChain ruleChain = new RuleChain(new RuleChainId(UUID.randomUUID()));
        ruleChain.setTenantId(tenantId);
        RuleChainId ruleChainId = ruleChain.getId();
        Dashboard dashboard = new Dashboard(new DashboardId(UUID.randomUUID()));
        dashboard.setTenantId(tenantId);
        DashboardId dashboardId = dashboard.getId();
        profile.setDefaultRuleChainId(ruleChainId);
        profile.setDefaultDashboardId(dashboardId);
        // The user's, not the package's: not listed
        profile.setDefaultEdgeRuleChainId(new RuleChainId(UUID.randomUUID()));
        Device packageDevice = new Device(new DeviceId(UUID.randomUUID()));
        packageDevice.setDeviceProfileId(profile.getId());
        IotHubInstalledItem checked = devicePackage(ruleChainId, dashboardId, profile.getId(), packageDevice.getId());
        when(iotHubInstalledItemService.findById(tenantId, checked.getId())).thenReturn(checked);
        when(iotHubInstalledItemService.findByTenantId(eq(tenantId), eq(List.of("DEVICE")), isNull(), any()))
                .thenReturn(new PageData<>(List.of(checked), 1, 1, false));
        // 3 devices on the profile: the package's own, which the delete removes first, and 2 the integration created
        when(deviceService.findDeviceIdsByTenantIdAndDeviceProfileId(eq(tenantId), eq(profile.getId()), any()))
                .thenReturn(new PageData<>(List.of(), 3, 3, true));
        when(deviceService.findDeviceById(tenantId, packageDevice.getId())).thenReturn(packageDevice);
        when(deviceProfileService.findDeviceProfileById(tenantId, profile.getId())).thenReturn(profile);
        when(ruleChainService.findRuleChainById(tenantId, ruleChainId)).thenReturn(ruleChain);
        when(dashboardService.findDashboardById(tenantId, dashboardId)).thenReturn(dashboard);
        when(entityService.fetchEntityName(tenantId, profile.getId())).thenReturn(Optional.of("smartico-l2-lr"));
        when(entityService.fetchEntityName(tenantId, ruleChainId)).thenReturn(Optional.of("Smartico L2-LR"));
        when(entityService.fetchEntityName(tenantId, dashboardId)).thenReturn(Optional.of("Smartico L2-LR Fleet"));

        InstalledItemKeptEntities kept = service.findKeptEntities(tenantId, checked.getId());

        // An update reads entities as shared with other packages: the profile is not among them
        assertThat(kept.getEntities()).isEmpty();
        assertThat(kept.getReferencedEntities()).containsExactly(
                new InstalledItemKeptEntities.KeptEntity(profile.getId(), "smartico-l2-lr"),
                new InstalledItemKeptEntities.KeptEntity(ruleChainId, "Smartico L2-LR"),
                new InstalledItemKeptEntities.KeptEntity(dashboardId, "Smartico L2-LR Fleet"));
    }

    @Test
    void findKeptEntities_devicePackage_aKeptDeviceKeepsItsProfile() {
        DeviceProfileId profileId = new DeviceProfileId(UUID.randomUUID());
        DeviceId sharedDeviceId = new DeviceId(UUID.randomUUID());
        IotHubInstalledItem checked = devicePackage(profileId, sharedDeviceId);
        IotHubInstalledItem other = devicePackage(sharedDeviceId);
        when(iotHubInstalledItemService.findById(tenantId, checked.getId())).thenReturn(checked);
        when(iotHubInstalledItemService.findByTenantId(eq(tenantId), eq(List.of("DEVICE")), isNull(), any()))
                .thenReturn(new PageData<>(List.of(checked, other), 1, 2, false));
        // The only device on the profile is the one another package uses: the delete keeps it, so it keeps the profile
        when(deviceService.findDeviceIdsByTenantIdAndDeviceProfileId(eq(tenantId), eq(profileId), any()))
                .thenReturn(new PageData<>(List.of(sharedDeviceId), 1, 1, false));

        InstalledItemKeptEntities kept = service.findKeptEntities(tenantId, checked.getId());

        assertThat(kept.getReferencedEntities()).extracting(InstalledItemKeptEntities.KeptEntity::getEntityId).containsExactly(profileId);
    }

    @Test
    void findKeptEntities_devicePackage_aSharedProfileKeepsWhatItRefersTo() {
        DeviceProfile profile = new DeviceProfile(new DeviceProfileId(UUID.randomUUID()));
        RuleChainId ruleChainId = new RuleChainId(UUID.randomUUID());
        RuleChainId edgeRuleChainId = new RuleChainId(UUID.randomUUID());
        profile.setDefaultRuleChainId(ruleChainId);
        profile.setDefaultEdgeRuleChainId(edgeRuleChainId);
        IotHubInstalledItem checked = devicePackage(ruleChainId, edgeRuleChainId, profile.getId());
        IotHubInstalledItem other = devicePackage(profile.getId());
        when(iotHubInstalledItemService.findById(tenantId, checked.getId())).thenReturn(checked);
        when(iotHubInstalledItemService.findByTenantId(eq(tenantId), eq(List.of("DEVICE")), isNull(), any()))
                .thenReturn(new PageData<>(List.of(checked, other), 1, 2, false));
        when(deviceProfileService.findDeviceProfileById(tenantId, profile.getId())).thenReturn(profile);

        InstalledItemKeptEntities kept = service.findKeptEntities(tenantId, checked.getId());

        assertThat(kept.getEntities()).extracting(InstalledItemKeptEntities.KeptEntity::getEntityId).containsExactly(profile.getId());
        assertThat(kept.getReferencedEntities()).extracting(InstalledItemKeptEntities.KeptEntity::getEntityId)
                .containsExactly(ruleChainId, edgeRuleChainId);
    }

    @Test
    void deleteInstalledItem_devicePackage_keepsTheProfileDevicesUseAndWhatItRefersTo() throws Exception {
        mockTenant();
        DeviceProfile profile = new DeviceProfile(new DeviceProfileId(UUID.randomUUID()));
        RuleChainId ruleChainId = new RuleChainId(UUID.randomUUID());
        DashboardId dashboardId = new DashboardId(UUID.randomUUID());
        profile.setDefaultRuleChainId(ruleChainId);
        profile.setDefaultDashboardId(dashboardId);
        IotHubInstalledItem deleted = devicePackage(ruleChainId, dashboardId, profile.getId());
        when(iotHubInstalledItemService.findById(tenantId, deleted.getId())).thenReturn(deleted);
        when(iotHubInstalledItemService.findByTenantId(eq(tenantId), eq(List.of("DEVICE")), isNull(), any()))
                .thenReturn(new PageData<>(List.of(deleted), 1, 1, false));
        when(deviceService.findDeviceIdsByTenantIdAndDeviceProfileId(eq(tenantId), eq(profile.getId()), any()))
                .thenReturn(new PageData<>(List.of(new DeviceId(UUID.randomUUID())), 1, 1, false));
        when(deviceProfileService.findDeviceProfileById(tenantId, profile.getId())).thenReturn(profile);

        service.deleteInstalledItem(user, deleted.getId());

        verify(tbDeviceProfileService, never()).delete(any(), any());
        verify(tbRuleChainService, never()).delete(any(), any());
        verify(tbDashboardService, never()).delete(any(), any());
        verify(iotHubInstalledItemService).deleteById(tenantId, deleted.getId());
    }

    @Test
    void deleteInstalledItem_devicePackage_aSharedProfileKeepsItsRuleChain() throws Exception {
        mockTenant();
        DeviceProfile profile = new DeviceProfile(new DeviceProfileId(UUID.randomUUID()));
        RuleChainId ruleChainId = new RuleChainId(UUID.randomUUID());
        profile.setDefaultRuleChainId(ruleChainId);
        IotHubInstalledItem deleted = devicePackage(ruleChainId, profile.getId());
        IotHubInstalledItem other = devicePackage(profile.getId());
        when(iotHubInstalledItemService.findById(tenantId, deleted.getId())).thenReturn(deleted);
        when(iotHubInstalledItemService.findByTenantId(eq(tenantId), eq(List.of("DEVICE")), isNull(), any()))
                .thenReturn(new PageData<>(List.of(deleted, other), 1, 2, false));
        when(deviceProfileService.findDeviceProfileById(tenantId, profile.getId())).thenReturn(profile);

        service.deleteInstalledItem(user, deleted.getId());

        verify(tbDeviceProfileService, never()).delete(any(), any());
        verify(tbRuleChainService, never()).delete(any(), any());
    }

    @Test
    void deleteInstalledItem_devicePackage_anEntitySomethingElseRefersToStays_loggedAsKept() throws Exception {
        // A profile the user made after the install uses the package's rule chain
        List<ILoggingEvent> logs = deleteRuleChainFailingWith(
                new DataValidationException("The rule chain referenced by the device profiles cannot be deleted!"));

        assertThat(logs).anySatisfy(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.INFO);
            assertThat(event.getFormattedMessage()).contains("Kept RULE_CHAIN").contains("referenced by the device profiles");
        });
        assertThat(logs).noneMatch(event -> event.getLevel() == Level.ERROR);
    }

    @Test
    void deleteInstalledItem_devicePackage_aFailedDelete_staysAnError() throws Exception {
        List<ILoggingEvent> logs = deleteRuleChainFailingWith(new RuntimeException("Database is down"));

        assertThat(logs).anySatisfy(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.ERROR);
            assertThat(event.getFormattedMessage()).contains("Failed to delete device package entity");
        });
    }

    /** Deletes a package whose only entity is a rule chain that fails to delete with {@code failure}; the service's logs. */
    private List<ILoggingEvent> deleteRuleChainFailingWith(Exception failure) throws Exception {
        mockTenant();
        RuleChain ruleChain = new RuleChain(new RuleChainId(UUID.randomUUID()));
        ruleChain.setTenantId(tenantId);
        IotHubInstalledItem deleted = devicePackage(ruleChain.getId());
        when(iotHubInstalledItemService.findById(tenantId, deleted.getId())).thenReturn(deleted);
        when(iotHubInstalledItemService.findByTenantId(eq(tenantId), eq(List.of("DEVICE")), isNull(), any()))
                .thenReturn(new PageData<>(List.of(deleted), 1, 1, false));
        when(ruleChainService.findRuleChainById(tenantId, ruleChain.getId())).thenReturn(ruleChain);
        doThrow(failure).when(tbRuleChainService).delete(ruleChain, user);
        Logger logger = (Logger) LoggerFactory.getLogger(DefaultIotHubService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        Level level = logger.getLevel();
        logger.setLevel(Level.INFO);
        try {
            service.deleteInstalledItem(user, deleted.getId());
        } finally {
            logger.setLevel(level);
            logger.detachAppender(appender);
        }
        // The delete goes on either way
        verify(iotHubInstalledItemService).deleteById(tenantId, deleted.getId());
        return appender.list;
    }

    private void noDevices(DeviceProfileId profileId) {
        when(deviceService.findDeviceIdsByTenantIdAndDeviceProfileId(eq(tenantId), eq(profileId), any()))
                .thenReturn(new PageData<>(List.of(), 0, 0, false));
    }

    private IotHubInstalledItem devicePackage(EntityId... createdEntityIds) {
        DeviceInstalledItemDescriptor descriptor = new DeviceInstalledItemDescriptor();
        descriptor.setCreatedEntityIds(List.of(createdEntityIds));
        IotHubInstalledItem item = installedItem(UUID.randomUUID());
        item.setDescriptor(descriptor);
        return item;
    }

    private Dashboard dashboard(TenantId owner) {
        Dashboard dashboard = new Dashboard(new DashboardId(UUID.randomUUID()));
        dashboard.setTenantId(owner);
        return dashboard;
    }

    private Device device(TenantId owner) {
        Device device = new Device(new DeviceId(UUID.randomUUID()));
        device.setTenantId(owner);
        return device;
    }

    private DeviceInstalledItemDescriptor devicePackageDescriptor(EntityId... createdEntityIds) {
        DeviceInstalledItemDescriptor descriptor = new DeviceInstalledItemDescriptor();
        descriptor.setCreatedEntityIds(List.of(createdEntityIds));
        return descriptor;
    }

    @Test
    void registerDeviceInstall_unknownEntity_rejectedAndNotSaved() {
        mockTenant();
        String versionId = UUID.randomUUID().toString();
        when(iotHubRestClient.getVersionInfo(versionId))
                .thenReturn(version(versionId, UUID.randomUUID().toString(), "DEVICE", "Pkg", "1.0"));
        Device knownDevice = device(tenantId);
        Dashboard unknownDashboard = dashboard(TenantId.fromUUID(UUID.randomUUID()));
        when(deviceService.findDeviceById(tenantId, knownDevice.getId())).thenReturn(knownDevice);
        when(dashboardService.findDashboardById(tenantId, unknownDashboard.getId())).thenReturn(unknownDashboard);

        InstallItemVersionResult result = service.registerDeviceInstall(user, versionId,
                devicePackageDescriptor(knownDevice.getId(), unknownDashboard.getId()));

        assertThat(result.isSuccess()).isFalse();
        verify(iotHubInstalledItemService, never()).save(any(), any());
        verify(tbDeviceService, never()).delete(any(Device.class), any());
        verify(tbDashboardService, never()).delete(any(Dashboard.class), any());
    }

    @Test
    void registerDeviceInstall_unknownDashboardId_rejectedAndNotSaved() {
        mockTenant();
        String versionId = UUID.randomUUID().toString();
        when(iotHubRestClient.getVersionInfo(versionId))
                .thenReturn(version(versionId, UUID.randomUUID().toString(), "DEVICE", "Pkg", "1.0"));
        Dashboard unknownDashboard = dashboard(TenantId.fromUUID(UUID.randomUUID()));
        when(dashboardService.findDashboardById(tenantId, unknownDashboard.getId())).thenReturn(unknownDashboard);
        DeviceInstalledItemDescriptor descriptor = devicePackageDescriptor();
        descriptor.setDashboardId(unknownDashboard.getId());

        InstallItemVersionResult result = service.registerDeviceInstall(user, versionId, descriptor);

        assertThat(result.isSuccess()).isFalse();
        verify(iotHubInstalledItemService, never()).save(any(), any());
    }

    @Test
    void registerDeviceInstall_knownEntities_saved() {
        mockTenant();
        String versionId = UUID.randomUUID().toString();
        when(iotHubRestClient.getVersionInfo(versionId))
                .thenReturn(version(versionId, UUID.randomUUID().toString(), "DEVICE", "Pkg", "1.0"));
        Device knownDevice = device(tenantId);
        Dashboard knownDashboard = dashboard(tenantId);
        when(deviceService.findDeviceById(tenantId, knownDevice.getId())).thenReturn(knownDevice);
        when(dashboardService.findDashboardById(tenantId, knownDashboard.getId())).thenReturn(knownDashboard);
        DeviceInstalledItemDescriptor descriptor = devicePackageDescriptor(knownDevice.getId(), knownDashboard.getId());
        descriptor.setDashboardId(knownDashboard.getId());
        when(iotHubInstalledItemService.save(eq(tenantId), any())).thenAnswer(inv -> inv.getArgument(1));

        InstallItemVersionResult result = service.registerDeviceInstall(user, versionId, descriptor);

        assertThat(result.isSuccess()).isTrue();
        verify(iotHubInstalledItemService).save(eq(tenantId), any(IotHubInstalledItem.class));
    }

    @Test
    void deleteInstalledItem_devicePackage_skipsEntityNotFound() throws Exception {
        mockTenant();
        Device knownDevice = device(tenantId);
        Dashboard unknownDashboard = dashboard(TenantId.fromUUID(UUID.randomUUID()));
        when(deviceService.findDeviceById(tenantId, knownDevice.getId())).thenReturn(knownDevice);
        when(dashboardService.findDashboardById(tenantId, unknownDashboard.getId())).thenReturn(unknownDashboard);
        UUID id = UUID.randomUUID();
        IotHubInstalledItem item = installedItem(id);
        item.setDescriptor(devicePackageDescriptor(knownDevice.getId(), unknownDashboard.getId()));
        when(iotHubInstalledItemService.findById(tenantId, new IotHubInstalledItemId(id))).thenReturn(item);
        when(iotHubInstalledItemService.findByTenantId(eq(tenantId), eq(List.of("DEVICE")), isNull(), any()))
                .thenReturn(new PageData<>(List.of(item), 1, 1, false));

        service.deleteInstalledItem(user, new IotHubInstalledItemId(id));

        verify(tbDeviceService).delete(knownDevice, user);
        verify(tbDashboardService, never()).delete(any(Dashboard.class), any());
        verify(iotHubInstalledItemService).deleteById(tenantId, new IotHubInstalledItemId(id));
    }

    @Test
    void deleteInstalledItem_devicePackage_deletesItsIntegrationAndConverter() throws Exception {
        mockTenant();
        Integration integration = new Integration(new IntegrationId(UUID.randomUUID()));
        integration.setTenantId(tenantId);
        Converter converter = new Converter(new ConverterId(UUID.randomUUID()));
        converter.setTenantId(tenantId);
        Integration unknownIntegration = new Integration(new IntegrationId(UUID.randomUUID()));
        unknownIntegration.setTenantId(TenantId.fromUUID(UUID.randomUUID()));
        when(integrationService.findIntegrationById(tenantId, integration.getId())).thenReturn(integration);
        when(integrationService.findIntegrationById(tenantId, unknownIntegration.getId())).thenReturn(unknownIntegration);
        when(converterService.findConverterById(tenantId, converter.getId())).thenReturn(converter);
        UUID id = UUID.randomUUID();
        IotHubInstalledItem item = installedItem(id);
        item.setDescriptor(devicePackageDescriptor(converter.getId(), integration.getId(), unknownIntegration.getId()));
        when(iotHubInstalledItemService.findById(tenantId, new IotHubInstalledItemId(id))).thenReturn(item);
        when(iotHubInstalledItemService.findByTenantId(eq(tenantId), eq(List.of("DEVICE")), isNull(), any()))
                .thenReturn(new PageData<>(List.of(item), 1, 1, false));

        service.deleteInstalledItem(user, new IotHubInstalledItemId(id));

        verify(integrationService).deleteIntegration(tenantId, integration.getId());
        verify(integrationService, never()).deleteIntegration(tenantId, unknownIntegration.getId());
        verify(tbConverterService).delete(converter, user);
        verify(iotHubInstalledItemService).deleteById(tenantId, new IotHubInstalledItemId(id));
    }

    @Test
    void registerDeviceInstall_usedExistingEntityOfAnotherTenant_rejectedAndNotSaved() {
        mockTenant();
        String versionId = UUID.randomUUID().toString();
        when(iotHubRestClient.getVersionInfo(versionId))
                .thenReturn(version(versionId, UUID.randomUUID().toString(), "DEVICE", "Pkg", "1.0"));
        Device knownDevice = device(tenantId);
        when(deviceService.findDeviceById(tenantId, knownDevice.getId())).thenReturn(knownDevice);
        Dashboard otherTenantDashboard = dashboard(TenantId.fromUUID(UUID.randomUUID()));
        when(dashboardService.findDashboardById(tenantId, otherTenantDashboard.getId())).thenReturn(otherTenantDashboard);
        DeviceInstalledItemDescriptor descriptor = devicePackageDescriptor(knownDevice.getId());
        descriptor.setUsedExistingEntityIds(List.of(otherTenantDashboard.getId()));

        InstallItemVersionResult result = service.registerDeviceInstall(user, versionId, descriptor);

        assertThat(result.isSuccess()).isFalse();
        verify(iotHubInstalledItemService, never()).save(any(), any());
    }

    @Test
    void deleteInstalledItem_devicePackage_leavesTheIntegrationOfAnotherTenantAsItIs() {
        mockTenant();
        IntegrationId integrationId = new IntegrationId(UUID.randomUUID());
        IotHubInstalledItem deleted = devicePackage(integrationId);
        setIntegrationState(deleted, integrationId, Map.of("profile.milesight-iot-am308", "am308-custom"));
        ((DeviceInstalledItemDescriptor) deleted.getDescriptor()).setUsedExistingEntityIds(List.of(integrationId));
        when(iotHubInstalledItemService.findById(tenantId, deleted.getId())).thenReturn(deleted);
        when(iotHubInstalledItemService.findByTenantId(eq(tenantId), eq(List.of("DEVICE")), isNull(), any()))
                .thenReturn(new PageData<>(List.of(deleted), 1, 1, false));
        Integration otherTenantIntegration = integration(integrationId, Map.of("profile.milesight-iot-am308", "am308-custom"));
        otherTenantIntegration.setTenantId(TenantId.fromUUID(UUID.randomUUID()));
        when(integrationService.findIntegrationById(tenantId, integrationId)).thenReturn(otherTenantIntegration);

        service.deleteInstalledItem(user, deleted.getId());

        verify(integrationService, never()).saveIntegration(any());
        verify(integrationService, never()).deleteIntegration(any(), any());
    }

    @Test
    void findKeptEntities_devicePackage_namesNoEntityOfAnotherTenant() {
        DashboardId otherTenantDashboardId = new DashboardId(UUID.randomUUID());
        IotHubInstalledItem checked = devicePackage(new DeviceId(UUID.randomUUID()), otherTenantDashboardId);
        ((DeviceInstalledItemDescriptor) checked.getDescriptor()).setUsedExistingEntityIds(List.of(otherTenantDashboardId));
        when(iotHubInstalledItemService.findById(tenantId, checked.getId())).thenReturn(checked);
        when(iotHubInstalledItemService.findByTenantId(eq(tenantId), eq(List.of("DEVICE")), isNull(), any()))
                .thenReturn(new PageData<>(List.of(checked), 1, 1, false));
        Dashboard otherTenantDashboard = new Dashboard(otherTenantDashboardId);
        otherTenantDashboard.setTenantId(TenantId.fromUUID(UUID.randomUUID()));
        when(dashboardService.findDashboardById(tenantId, otherTenantDashboardId)).thenReturn(otherTenantDashboard);

        InstalledItemKeptEntities kept = service.findKeptEntities(tenantId, checked.getId());

        assertThat(kept.getEntities()).containsExactly(new InstalledItemKeptEntities.KeptEntity(otherTenantDashboardId, null));
        verify(entityService, never()).fetchEntityName(any(), any());
    }

}
