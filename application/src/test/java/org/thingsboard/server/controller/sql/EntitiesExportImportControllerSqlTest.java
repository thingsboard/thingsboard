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
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.DeviceProfileId;
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
import java.util.function.Consumer;

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
    public void testExportImport_singleEntityOneByOne_betweenTenants() throws Exception {
        Asset asset = createAsset(tenantId1, null, "AB", "Asset of tenant 1");
        testExportImportBetweenTenants(asset, importedAsset -> {
            assertThat(importedAsset.getName()).isEqualTo(asset.getName());
            assertThat(importedAsset.getType()).isEqualTo(asset.getType());
            assertThat(importedAsset.getLabel()).isEqualTo(asset.getLabel());
            assertThat(importedAsset.getAdditionalInfo()).isEqualTo(asset.getAdditionalInfo());
        });

        Customer customer = createCustomer(tenantId1, "Customer of tenant 1");
        testExportImportBetweenTenants(customer, importedCustomer -> {
            assertThat(importedCustomer.getTitle()).isEqualTo(customer.getTitle());
            assertThat(importedCustomer.getCountry()).isEqualTo(customer.getCountry());
            assertThat(importedCustomer.getAddress()).isEqualTo(customer.getAddress());
            assertThat(importedCustomer.getEmail()).isEqualTo(customer.getEmail());
        });

        DeviceProfile deviceProfile = createDeviceProfile(tenantId1, "Device profile of tenant 1");
        DeviceProfileId importedDeviceProfileId = testExportImportBetweenTenants(deviceProfile, importedDeviceProfile -> {
            assertThat(importedDeviceProfile.getName()).isEqualTo(deviceProfile.getName());
            assertThat(importedDeviceProfile.getType()).isEqualTo(deviceProfile.getType());
            assertThat(importedDeviceProfile.getTransportType()).isEqualTo(deviceProfile.getTransportType());
            assertThat(importedDeviceProfile.getProfileData()).isEqualTo(deviceProfile.getProfileData());
            assertThat(importedDeviceProfile.getDescription()).isEqualTo(deviceProfile.getDescription());
        }).getSavedEntity().getId();

        Device device = createDevice(tenantId1, null, deviceProfile.getId(), "Device of tenant 1");
        DeviceCredentials credentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId1, device.getId());
        this.<Device, DeviceExportData>testExportImportBetweenTenants(device, deviceExportData -> {
            assertThat(deviceExportData.getCredentials()).isEqualTo(credentials);
        }, importedDevice -> {
            assertThat(importedDevice.getName()).isEqualTo(device.getName());
            assertThat(importedDevice.getType()).isEqualTo(device.getType());
            assertThat(importedDevice.getDeviceData()).isEqualTo(device.getDeviceData());
            assertThat(importedDevice.getDeviceProfileId()).isEqualTo(importedDeviceProfileId);
            assertThat(importedDevice.getLabel()).isEqualTo(device.getLabel());

            DeviceCredentials importedCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId2, importedDevice.getId());
            assertThat(importedCredentials.getCredentialsId()).isEqualTo(credentials.getCredentialsId());
            assertThat(importedCredentials.getCredentialsValue()).isEqualTo(credentials.getCredentialsValue());
            assertThat(importedCredentials.getCredentialsType()).isEqualTo(credentials.getCredentialsType());
        });

        RuleChain ruleChain = createRuleChain(tenantId1, "Rule chain of tenant 1");
        RuleChainMetaData metaData = ruleChainService.loadRuleChainMetaData(tenantId1, ruleChain.getId());
        this.<RuleChain, RuleChainExportData>testExportImportBetweenTenants(ruleChain, ruleChainExportData -> {
            assertThat(ruleChainExportData.getMetaData()).isEqualTo(metaData);
        }, importedRuleChain -> {
            assertThat(importedRuleChain.getType()).isEqualTo(ruleChain.getType());
            assertThat(importedRuleChain.getName()).isEqualTo(ruleChain.getName());
            assertThat(importedRuleChain.isDebugMode()).isEqualTo(ruleChain.isDebugMode());
            assertThat(importedRuleChain.getConfiguration()).isEqualTo(ruleChain.getConfiguration());
            RuleChainMetaData importedMetaData = ruleChainService.loadRuleChainMetaData(tenantId2, importedRuleChain.getId());
            assertThat(importedMetaData.getConnections()).isEqualTo(metaData.getConnections());
            assertThat(importedMetaData.getFirstNodeIndex()).isEqualTo(metaData.getFirstNodeIndex());
            for (int i = 0; i < metaData.getNodes().size(); i++) {
                RuleNode initialNode = metaData.getNodes().get(i);
                RuleNode importedNode = importedMetaData.getNodes().get(i);
                assertThat(importedNode.getName()).isEqualTo(initialNode.getName());
                assertThat(importedNode.getType()).isEqualTo(initialNode.getType());
                assertThat(importedNode.getConfiguration()).isEqualTo(initialNode.getConfiguration());
                assertThat(importedNode.getAdditionalInfo()).isEqualTo(initialNode.getAdditionalInfo());
            }
        });

        Dashboard dashboard = createDashboard(tenantId1, null, "Dashboard of tenant 1");
        testExportImportBetweenTenants(dashboard, importedDashboard -> {
            assertThat(importedDashboard.getTitle()).isEqualTo(dashboard.getTitle());
            assertThat(importedDashboard.getConfiguration()).isEqualTo(dashboard.getConfiguration());
            assertThat(importedDashboard.getImage()).isEqualTo(dashboard.getImage());
            assertThat(importedDashboard.isMobileHide()).isEqualTo(dashboard.isMobileHide());
        });
    }

    private <E extends ExportableEntity<?>, D extends EntityExportData<E>> EntityImportResult<E> testExportImportBetweenTenants(E entity, Consumer<D> exportDataRequirements, Consumer<E> importedEntityRequirements) throws Exception {
        logInAsTenantAdmin1();
        D exportData = readResponse(doPost("/api/entities/export/" + entity.getId().getEntityType() + "/" + entity.getId())
                .andExpect(status().isOk()), new TypeReference<D>() {});
        assertThat(exportData.getEntity()).isEqualTo(entity);
        exportDataRequirements.accept(exportData);

        logInAsTenantAdmin2();
        EntityImportResult<E> importResult = importEntities(List.of(exportData)).get(0);

        E importedEntity = (E) exportableEntitiesService.findEntityById(tenantId2, importResult.getSavedEntity().getId());
        assertThat(importedEntity).isNotNull();
        assertThat(importResult.getSavedEntity()).isEqualTo(importedEntity);
        assertThat(importResult.getOldEntity()).isNull();

        assertThat(importedEntity.getId()).isNotEqualTo(entity.getId());
        assertThat(importedEntity.getExternalId()).isEqualTo(entity.getId());
        assertThat(importedEntity.getTenantId()).isEqualTo(tenantId2);
        importedEntityRequirements.accept(importedEntity);
        return importResult;
    }

    private <E extends ExportableEntity<?>> EntityImportResult<E> testExportImportBetweenTenants(E entity, Consumer<E> importedEntityRequirements) throws Exception {
        return testExportImportBetweenTenants(entity, exportData -> {}, importedEntityRequirements);
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

    private void logInAsTenantAdmin1() throws Exception {
        login(tenantAdmin1.getEmail(), "12345678");
    }

    private void logInAsTenantAdmin2() throws Exception {
        login(tenantAdmin2.getEmail(), "12345678");
    }

}
