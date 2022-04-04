/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.controller.sql;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.controller.BaseEntitiesExportImportControllerTest;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.service.sync.exporting.ExportableEntitiesService;
import org.thingsboard.server.service.sync.exporting.data.DeviceExportData;
import org.thingsboard.server.service.sync.exporting.data.EntityExportData;
import org.thingsboard.server.service.sync.exporting.data.RuleChainExportData;
import org.thingsboard.server.service.sync.importing.EntityImportResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class EntitiesExportImportControllerSqlTest extends BaseEntitiesExportImportControllerTest {

    @Autowired
    private TenantService tenantService;
    @Autowired
    private DeviceCredentialsService deviceCredentialsService;
    @Autowired
    private ExportableEntitiesService exportableEntitiesService;

    private TenantId tenantId1;
    private User tenantAdmin1;

    private TenantId tenantId2;
    private User tenantAdmin2;

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


    @Test
    public void testExportImportSingleAsset_betweenTenants() throws Exception {
        logInAsTenantAdmin1();
        Asset asset = createAsset(tenantId1, null, "AB", "Asset of tenant 1");
        EntityExportData<Asset> exportData = exportSingleEntity(asset.getId());
        assertThat(exportData.getEntity()).isEqualTo(asset);

        logInAsTenantAdmin2();
        EntityImportResult<Asset> importResult = importEntities(List.of(exportData)).get(0);

        checkImportedEntity(tenantId1, asset, tenantId2, importResult);
        checkImportedAssetData(asset, importResult.getSavedEntity());
    }

    @Test
    public void testExportImportSingleAsset_sameTenant() throws Exception {
        logInAsTenantAdmin1();
        Asset asset = createAsset(tenantId1, null, "AB", "Asset v1.0");
        EntityExportData<Asset> exportData = exportSingleEntity(asset.getId());

        EntityImportResult<Asset> importResult = importEntities(List.of(exportData)).get(0);

        checkImportedEntity(tenantId1, asset, tenantId1, importResult);
        checkImportedAssetData(asset, importResult.getSavedEntity());
    }

    @Test
    public void testExportImportAsset_withCustomer_betweenTenants() throws Exception {
        logInAsTenantAdmin1();
        Customer customer = createCustomer(tenantId1, "My customer");
        Asset asset = createAsset(tenantId1, customer.getId(), "AB", "My asset");

        EntityExportData<Customer> customerExportData = exportSingleEntity(customer.getId());
        EntityExportData<Asset> assetExportData = exportSingleEntity(asset.getId());

        logInAsTenantAdmin2();
        Customer importedCustomer = importEntities(List.of(customerExportData)).get(0).getSavedEntity();
        Asset importedAsset = importEntities(List.of(assetExportData)).get(0).getSavedEntity();

        assertThat(importedAsset.getCustomerId()).isEqualTo(importedCustomer.getId());
    }

    @Test
    public void testExportImportAsset_withCustomer_sameTenant() throws Exception {
        logInAsTenantAdmin1();
        Customer customer = createCustomer(tenantId1, "My customer");
        Asset asset = createAsset(tenantId1, customer.getId(), "AB", "My asset");

        Asset importedAsset = this.<Asset>importEntities(List.of(exportSingleEntity(asset.getId()))).get(0).getSavedEntity();

        assertThat(importedAsset.getCustomerId()).isEqualTo(asset.getCustomerId());
    }

    private void checkImportedAssetData(Asset initialAsset, Asset importedAsset) {
        assertThat(importedAsset.getName()).isEqualTo(initialAsset.getName());
        assertThat(importedAsset.getType()).isEqualTo(initialAsset.getType());
        assertThat(importedAsset.getLabel()).isEqualTo(initialAsset.getLabel());
        assertThat(importedAsset.getAdditionalInfo()).isEqualTo(initialAsset.getAdditionalInfo());
    }


    @Test
    public void testExportImportSingleCustomer_betweenTenants() throws Exception {
        logInAsTenantAdmin1();
        Customer customer = createCustomer(tenantAdmin1.getTenantId(), "Customer of tenant 1");
        EntityExportData<Customer> exportData = exportSingleEntity(customer.getId());
        assertThat(exportData.getEntity()).isEqualTo(customer);

        logInAsTenantAdmin2();
        EntityImportResult<Customer> importResult = importEntities(List.of(exportData)).get(0);

        checkImportedEntity(tenantId1, customer, tenantId2, importResult);
        checkImportedCustomerData(customer, importResult.getSavedEntity());
    }

    @Test
    public void testExportImportSingleCustomer_sameTenant() throws Exception {
        logInAsTenantAdmin1();
        Customer customer = createCustomer(tenantAdmin1.getTenantId(), "Customer v1.0");
        EntityExportData<Customer> exportData = exportSingleEntity(customer.getId());

        EntityImportResult<Customer> importResult = importEntities(List.of(exportData)).get(0);

        checkImportedEntity(tenantId1, customer, tenantId1, importResult);
        checkImportedCustomerData(customer, importResult.getSavedEntity());
    }

    private void checkImportedCustomerData(Customer initialCustomer, Customer importedCustomer) {
        assertThat(importedCustomer.getTitle()).isEqualTo(initialCustomer.getTitle());
        assertThat(importedCustomer.getCountry()).isEqualTo(initialCustomer.getCountry());
        assertThat(importedCustomer.getAddress()).isEqualTo(initialCustomer.getAddress());
        assertThat(importedCustomer.getEmail()).isEqualTo(initialCustomer.getEmail());
    }


    @Test
    public void testExportImportDeviceWithProfile_betweenTenants() throws Exception {
        logInAsTenantAdmin1();
        DeviceProfile deviceProfile = createDeviceProfile(tenantId1, "Device profile of tenant 1");
        Device device = createDevice(tenantId1, null, deviceProfile.getId(), "Device of tenant 1");
        DeviceCredentials credentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId1, device.getId());

        EntityExportData<DeviceProfile> profileExportData = exportSingleEntity(deviceProfile.getId());
        assertThat(profileExportData.getEntity()).isEqualTo(deviceProfile);

        EntityExportData<Device> deviceExportData = exportSingleEntity(device.getId());
        assertThat(deviceExportData.getEntity()).isEqualTo(device);
        assertThat(((DeviceExportData) deviceExportData).getCredentials()).isEqualTo(credentials);
        DeviceCredentials exportedCredentials = ((DeviceExportData) deviceExportData).getCredentials();
        exportedCredentials.setCredentialsId(credentials.getCredentialsId() + "a");

        logInAsTenantAdmin2();
        EntityImportResult<DeviceProfile> profileImportResult = importEntities(List.of(profileExportData)).get(0);
        checkImportedEntity(tenantId1, deviceProfile, tenantId2, profileImportResult);
        checkImportedDeviceProfileData(deviceProfile, profileImportResult.getSavedEntity());


        EntityImportResult<Device> deviceImportResult = importEntities(List.of(deviceExportData)).get(0);
        Device importedDevice = deviceImportResult.getSavedEntity();
        checkImportedEntity(tenantId1, device, tenantId2, deviceImportResult);
        checkImportedDeviceData(device, importedDevice);

        assertThat(importedDevice.getDeviceProfileId()).isEqualTo(profileImportResult.getSavedEntity().getId());

        DeviceCredentials importedCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId2, importedDevice.getId());
        assertThat(importedCredentials.getId()).isNotEqualTo(credentials.getId());
        assertThat(importedCredentials.getCredentialsId()).isEqualTo(exportedCredentials.getCredentialsId());
        assertThat(importedCredentials.getCredentialsValue()).isEqualTo(credentials.getCredentialsValue());
        assertThat(importedCredentials.getCredentialsType()).isEqualTo(credentials.getCredentialsType());
    }

    @Test
    public void testExportImportDevice_sameTenant() throws Exception {
        logInAsTenantAdmin1();
        DeviceProfile deviceProfile = createDeviceProfile(tenantId1, "Device profile v1.0");
        Device device = createDevice(tenantId1, null, deviceProfile.getId(), "Device v1.0");
        DeviceCredentials credentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId1, device.getId());

        EntityExportData<Device> deviceExportData = exportSingleEntity(device.getId());

        EntityImportResult<Device> importResult = importEntities(List.of(deviceExportData)).get(0);
        Device importedDevice = importResult.getSavedEntity();

        checkImportedEntity(tenantId1, device, tenantId1, importResult);
        assertThat(importedDevice.getDeviceProfileId()).isEqualTo(device.getDeviceProfileId());
        assertThat(deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId1, device.getId())).isEqualTo(credentials);
    }

    private void checkImportedDeviceProfileData(DeviceProfile initialProfile, DeviceProfile importedProfile) {
        assertThat(initialProfile.getName()).isEqualTo(importedProfile.getName());
        assertThat(initialProfile.getType()).isEqualTo(importedProfile.getType());
        assertThat(initialProfile.getTransportType()).isEqualTo(importedProfile.getTransportType());
        assertThat(initialProfile.getProfileData()).isEqualTo(importedProfile.getProfileData());
        assertThat(initialProfile.getDescription()).isEqualTo(importedProfile.getDescription());
    }

    private void checkImportedDeviceData(Device initialDevice, Device importedDevice) {
        assertThat(importedDevice.getName()).isEqualTo(initialDevice.getName());
        assertThat(importedDevice.getType()).isEqualTo(initialDevice.getType());
        assertThat(importedDevice.getDeviceData()).isEqualTo(initialDevice.getDeviceData());
        assertThat(importedDevice.getLabel()).isEqualTo(initialDevice.getLabel());
    }


    @Test
    public void testExportImportSingleRuleChain_betweenTenants() throws Exception {
        logInAsTenantAdmin1();
        RuleChain ruleChain = createRuleChain(tenantId1, "Rule chain of tenant 1");
        RuleChainMetaData metaData = ruleChainService.loadRuleChainMetaData(tenantId1, ruleChain.getId());

        EntityExportData<RuleChain> exportData = exportSingleEntity(ruleChain.getId());
        assertThat(exportData.getEntity()).isEqualTo(ruleChain);
        assertThat(((RuleChainExportData) exportData).getMetaData()).isEqualTo(metaData);

        logInAsTenantAdmin2();
        EntityImportResult<RuleChain> importResult = importEntities(List.of(exportData)).get(0);
        RuleChain importedRuleChain = importResult.getSavedEntity();
        RuleChainMetaData importedMetaData = ruleChainService.loadRuleChainMetaData(tenantId2, importedRuleChain.getId());

        checkImportedEntity(tenantId1, ruleChain, tenantId2, importResult);
        checkImportedRuleChainData(ruleChain, metaData, importedRuleChain, importedMetaData);
    }

    @Test
    public void testExportImportSingleRuleChain_sameTenant() throws Exception {
        logInAsTenantAdmin1();
        RuleChain ruleChain = createRuleChain(tenantId1, "Rule chain v1.0");
        RuleChainMetaData metaData = ruleChainService.loadRuleChainMetaData(tenantId1, ruleChain.getId());

        EntityExportData<RuleChain> exportData = exportSingleEntity(ruleChain.getId());

        EntityImportResult<RuleChain> importResult = importEntities(List.of(exportData)).get(0);
        RuleChain importedRuleChain = importResult.getSavedEntity();
        RuleChainMetaData importedMetaData = ruleChainService.loadRuleChainMetaData(tenantId1, importedRuleChain.getId());

        checkImportedEntity(tenantId1, ruleChain, tenantId1, importResult);
        checkImportedRuleChainData(ruleChain, metaData, importedRuleChain, importedMetaData);
    }

    private void checkImportedRuleChainData(RuleChain initialRuleChain, RuleChainMetaData initialMetaData, RuleChain importedRuleChain, RuleChainMetaData importedMetaData) {
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


    @Test
    public void testExportImportSingleDashboard_betweenTenants() throws Exception {
        logInAsTenantAdmin1();
        Dashboard dashboard = createDashboard(tenantAdmin1.getTenantId(), null, "Dashboard of tenant 1");

        EntityExportData<Dashboard> exportData = exportSingleEntity(dashboard.getId());
        assertThat(exportData.getEntity()).isEqualTo(dashboard);

        logInAsTenantAdmin2();
        EntityImportResult<Dashboard> importResult = importEntities(List.of(exportData)).get(0);
        checkImportedEntity(tenantId1, dashboard, tenantId2, importResult);
        checkImportedDashboardData(dashboard, importResult.getSavedEntity());
    }

    @Test
    public void testExportImportSingleDashboard_sameTenant() throws Exception {
        logInAsTenantAdmin1();
        Dashboard dashboard = createDashboard(tenantAdmin1.getTenantId(), null, "Dashboard v1.0");

        EntityExportData<Dashboard> exportData = exportSingleEntity(dashboard.getId());

        EntityImportResult<Dashboard> importResult = importEntities(List.of(exportData)).get(0);
        checkImportedEntity(tenantId1, dashboard, tenantId1, importResult);
        checkImportedDashboardData(dashboard, importResult.getSavedEntity());
    }

    private void checkImportedDashboardData(Dashboard initialDashboard, Dashboard importedDashboard) {
        assertThat(importedDashboard.getTitle()).isEqualTo(initialDashboard.getTitle());
        assertThat(importedDashboard.getConfiguration()).isEqualTo(initialDashboard.getConfiguration());
        assertThat(importedDashboard.getImage()).isEqualTo(initialDashboard.getImage());
        assertThat(importedDashboard.isMobileHide()).isEqualTo(initialDashboard.isMobileHide());
        if (initialDashboard.getAssignedCustomers() != null) {
            assertThat(importedDashboard.getAssignedCustomers()).containsAll(initialDashboard.getAssignedCustomers());
        }
    }


    private <E extends ExportableEntity<?> & HasTenantId> void checkImportedEntity(TenantId tenantId1, E initialEntity, TenantId tenantId2, EntityImportResult<E> importResult) {
        E importedEntity = importResult.getSavedEntity();

        assertThat(initialEntity.getTenantId()).isEqualTo(tenantId1);
        assertThat(importedEntity.getTenantId()).isEqualTo(tenantId2);

        assertThat(importedEntity.getExternalId()).isEqualTo(initialEntity.getId());

        boolean sameTenant = tenantId1.equals(tenantId2);
        if (!sameTenant) {
            assertThat(importedEntity.getId()).isNotEqualTo(initialEntity.getId());
        } else {
            assertThat(importedEntity.getId()).isEqualTo(initialEntity.getId());
            assertThat(importResult.getOldEntity()).isEqualTo(initialEntity);
        }
    }


    @SneakyThrows
    private <E extends ExportableEntity<? extends EntityId>> List<EntityImportResult<E>> importEntities(List<EntityExportData<E>> exportDataList) {
        String requestJson = mapper.writerFor(new TypeReference<List<EntityExportData<E>>>() {}).writeValueAsString(exportDataList);
        ResultActions resultActions = doPost("/api/entities/import", (Object) requestJson);

        try {
            String responseJson = resultActions.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            JavaType type = mapper.getTypeFactory().constructCollectionType(List.class,
                    mapper.getTypeFactory().constructParametricType(EntityImportResult.class, exportDataList.get(0).getEntity().getClass()));
            return mapper.readValue(responseJson, type);
        } catch (AssertionError e) {
            throw new AssertionError(readResponse(resultActions, String.class), e);
        }
    }

    private <E extends ExportableEntity<I>, I extends EntityId> EntityExportData<E> exportSingleEntity(I entityId) throws Exception {
        return readResponse(doPost("/api/entities/export/" + entityId.getEntityType() + "/" + entityId.getId().toString())
                .andExpect(status().isOk()), new TypeReference<EntityExportData<E>>() {});
    }


    private void logInAsTenantAdmin1() throws Exception {
        login(tenantAdmin1.getEmail(), "12345678");
    }

    private void logInAsTenantAdmin2() throws Exception {
        login(tenantAdmin2.getEmail(), "12345678");
    }

}
