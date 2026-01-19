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
package org.thingsboard.server.controller;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.asset.AssetDao;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@ContextConfiguration(classes = {AssetControllerTest.Config.class})
@DaoSqlTest
public class AssetControllerTest extends AbstractControllerTest {

    private IdComparator<Asset> idComparator = new IdComparator<>();

    private Tenant savedTenant;
    private User tenantAdmin;

    @Autowired
    private AssetDao assetDao;

    static class Config {
        @Bean
        @Primary
        public AssetDao assetDao(AssetDao assetDao) {
            return Mockito.mock(AssetDao.class, AdditionalAnswers.delegatesTo(assetDao));
        }
    }

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = saveTenant(tenant);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        deleteTenant(savedTenant.getId());
    }

    @Test
    public void testSaveAsset() throws Exception {
        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");

        Mockito.reset(tbClusterService, auditLogService);

        Asset savedAsset = doPost("/api/asset", asset, Asset.class);

        testNotifyEntityAllOneTime(savedAsset, savedAsset.getId(), savedAsset.getId(), savedTenant.getId(),
                tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED);

        Assert.assertNotNull(savedAsset);
        Assert.assertNotNull(savedAsset.getId());
        Assert.assertTrue(savedAsset.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedAsset.getTenantId());
        Assert.assertNotNull(savedAsset.getCustomerId());
        Assert.assertEquals(NULL_UUID, savedAsset.getCustomerId().getId());
        Assert.assertEquals(asset.getName(), savedAsset.getName());

        Mockito.reset(tbClusterService, auditLogService);

        savedAsset.setName("My new asset");
        doPost("/api/asset", savedAsset, Asset.class);

        testNotifyEntityAllOneTime(savedAsset, savedAsset.getId(), savedAsset.getId(), savedTenant.getId(),
                tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.UPDATED);

        Asset foundAsset = doGet("/api/asset/" + savedAsset.getId().getId().toString(), Asset.class);
        Assert.assertEquals(foundAsset.getName(), savedAsset.getName());
    }

    @Test
    public void testSaveAssetWithViolationOfLengthValidation() throws Exception {
        Asset asset = new Asset();
        asset.setName(StringUtils.randomAlphabetic(300));
        asset.setType("default");

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = msgErrorFieldLength("name");
        doPost("/api/asset", asset)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(asset, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
        Mockito.reset(tbClusterService, auditLogService);

        asset.setName("Normal name");
        asset.setType(StringUtils.randomAlphabetic(300));
        msgError = msgErrorFieldLength("type");
        doPost("/api/asset", asset)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(asset, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
        Mockito.reset(tbClusterService, auditLogService);

        asset.setType("default");
        asset.setLabel(StringUtils.randomAlphabetic(300));
        msgError = msgErrorFieldLength("label");
        doPost("/api/asset", asset)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(asset, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testUpdateAssetFromDifferentTenant() throws Exception {
        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        Asset savedAsset = doPost("/api/asset", asset, Asset.class);

        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/asset", savedAsset)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(savedAsset.getId(), savedAsset);

        doDelete("/api/asset/" + savedAsset.getId().getId().toString())
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(savedAsset.getId(), savedAsset);

        deleteDifferentTenant();
    }

    @Test
    public void testSaveAssetWithProfileFromDifferentTenant() throws Exception {
        loginDifferentTenant();
        AssetProfile differentProfile = createAssetProfile("Different profile");
        differentProfile = doPost("/api/assetProfile", differentProfile, AssetProfile.class);

        loginTenantAdmin();
        Asset asset = new Asset();
        asset.setName("My device");
        asset.setAssetProfileId(differentProfile.getId());
        doPost("/api/asset", asset).andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Asset can`t be referencing to asset profile from different tenant!")));
    }

    @Test
    public void testFindAssetById() throws Exception {
        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        Asset savedAsset = doPost("/api/asset", asset, Asset.class);
        Asset foundAsset = doGet("/api/asset/" + savedAsset.getId().getId().toString(), Asset.class);
        Assert.assertNotNull(foundAsset);
        Assert.assertEquals(savedAsset, foundAsset);
    }

    @Test
    public void testFindAssetTypesByTenantId() throws Exception {
        AssetProfile assetProfile = createAssetProfile("typeB");
        assetProfile = doPost("/api/assetProfile", assetProfile, AssetProfile.class);

        Mockito.reset(tbClusterService, auditLogService);

        int cntTime = 3;
        for (int i = 0; i < cntTime; i++) {
            Asset asset = new Asset();
            asset.setName("My asset B" + i);
            asset.setType("typeB");
            asset.setAssetProfileId(assetProfile.getId());
            doPost("/api/asset", asset, Asset.class);
        }

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(new Asset(), new Asset(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, cntTime, cntTime, cntTime);

        for (int i = 0; i < 7; i++) {
            Asset asset = new Asset();
            asset.setName("My asset C" + i);
            asset.setType("typeC");
            doPost("/api/asset", asset, Asset.class);
        }
        for (int i = 0; i < 9; i++) {
            Asset asset = new Asset();
            asset.setName("My asset A" + i);
            asset.setType("typeA");
            doPost("/api/asset", asset, Asset.class);
        }
        List<EntitySubtype> assetTypes = doGetTyped("/api/asset/types",
                new TypeReference<List<EntitySubtype>>() {
                });

        Assert.assertNotNull(assetTypes);
        Assert.assertEquals(3, assetTypes.size());
        Assert.assertEquals("typeA", assetTypes.get(0).getType());
        Assert.assertEquals("typeB", assetTypes.get(1).getType());
        Assert.assertEquals("typeC", assetTypes.get(2).getType());
    }

    @Test
    public void testDeleteAsset() throws Exception {
        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        Asset savedAsset = doPost("/api/asset", asset, Asset.class);

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/asset/" + savedAsset.getId().getId().toString())
                .andExpect(status().isOk());

        testNotifyEntityAllOneTime(savedAsset, savedAsset.getId(), savedAsset.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.DELETED, savedAsset.getId().getId().toString());

        String assetIdStr = savedAsset.getId().getId().toString();
        doGet("/api/asset/" + assetIdStr)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Asset", assetIdStr))));
    }

    @Test
    public void testDeleteAssetWithAlarmsAndAlarmTypes() throws Exception {
        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        Asset savedAsset = doPost("/api/asset", asset, Asset.class);

        Alarm alarm = Alarm.builder()
                .tenantId(tenantId)
                .originator(savedAsset.getId())
                .severity(AlarmSeverity.CRITICAL)
                .type("test_type")
                .build();

        alarm = doPost("/api/alarm", alarm, Alarm.class);
        Assert.assertNotNull(alarm);
        AlarmId alarmId = alarm.getId();

        AlarmInfo foundAlarm = doGet("/api/alarm/info/" + alarmId, AlarmInfo.class);
        Assert.assertNotNull(foundAlarm);

        doDelete("/api/asset/" + savedAsset.getId().getId().toString())
                .andExpect(status().isOk());

        String assetIdStr = savedAsset.getId().getId().toString();
        doGet("/api/asset/" + assetIdStr)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Asset", assetIdStr))));

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            doGet("/api/alarm/info/" + alarmId)
                    .andExpect(status().isNotFound())
                    .andExpect(statusReason(containsString(msgErrorNoFound("Alarm", alarmId.getId().toString()))));
        });
    }

    @Test
    public void testDeleteAssetWithPropagatedAlarm() throws Exception {
        Device device = new Device();
        device.setTenantId(savedTenant.getTenantId());
        device.setName("Test device");
        device.setLabel("Label");
        device.setType("default");
        device = doPost("/api/device", device, Device.class);

        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        asset = doPost("/api/asset", asset, Asset.class);

        EntityRelation entityRelation = new EntityRelation(asset.getId(), device.getId(), "CONTAINS");
        doPost("/api/relation", entityRelation);

        //create alarm
        Alarm alarm = Alarm.builder()
                .tenantId(savedTenant.getTenantId())
                .originator(device.getId())
                .severity(AlarmSeverity.CRITICAL)
                .type("test_type")
                .propagate(true)
                .build();

        alarm = doPost("/api/alarm", alarm, Alarm.class);
        Assert.assertNotNull(alarm);

        PageData<AlarmInfo> deviceAlarms = doGetTyped("/api/alarm/DEVICE/" + device.getUuidId() + "?page=0&pageSize=10", new TypeReference<>() {
        });
        assertThat(deviceAlarms.getData()).hasSize(1);

        PageData<AlarmInfo> assetAlarms = doGetTyped("/api/alarm/ASSET/" + asset.getUuidId() + "?page=0&pageSize=10", new TypeReference<>() {
        });
        assertThat(assetAlarms.getData()).hasSize(1);

        //delete asset
        doDelete("/api/asset/" + asset.getId().getId().toString())
                .andExpect(status().isOk());

        //check device alarms
        PageData<AlarmInfo> deviceAlarmsAfterAssetDeletion = doGetTyped("/api/alarm/DEVICE/" + device.getUuidId() + "?page=0&pageSize=10", new TypeReference<PageData<AlarmInfo>>() {
        });
        assertThat(deviceAlarmsAfterAssetDeletion.getData()).hasSize(1);
    }

    @Test
    public void testDeleteAssetAssignedToEntityView() throws Exception {
        Asset asset1 = new Asset();
        asset1.setName("My asset 1");
        asset1.setType("default");
        Asset savedAsset1 = doPost("/api/asset", asset1, Asset.class);

        Asset asset2 = new Asset();
        asset2.setName("My asset 2");
        asset2.setType("default");
        Asset savedAsset2 = doPost("/api/asset", asset2, Asset.class);

        EntityView view = new EntityView();
        view.setEntityId(savedAsset1.getId());
        view.setTenantId(savedTenant.getId());
        view.setName("My entity view");
        view.setType("default");
        EntityView savedView = doPost("/api/entityView", view, EntityView.class);

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = "Can't delete asset that has entity views";
        doDelete("/api/asset/" + savedAsset1.getId().getId().toString())
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityIsNullOneTimeEdgeServiceNeverError(savedAsset1, savedTenant.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.DELETED, new DataValidationException(msgError), savedAsset1.getId().getId().toString());

        savedView.setEntityId(savedAsset2.getId());

        doPost("/api/entityView", savedView, EntityView.class);

        doDelete("/api/asset/" + savedAsset1.getId().getId().toString())
                .andExpect(status().isOk());

        String assetIdStr = savedAsset1.getId().getId().toString();
        doGet("/api/asset/" + assetIdStr)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Asset", assetIdStr))));
    }

    @Test
    public void testSaveAssetWithEmptyType() throws Exception {
        Asset asset = new Asset();
        asset.setName("My asset");

        Mockito.reset(tbClusterService, auditLogService);

        Asset savedAsset = doPost("/api/asset", asset, Asset.class);
        Assert.assertEquals("default", savedAsset.getType());

        testNotifyEntityAllOneTime(savedAsset, savedAsset.getId(), savedAsset.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED);
    }

    @Test
    public void testSaveAssetWithEmptyName() throws Exception {
        Asset asset = new Asset();
        asset.setType("default");

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = "Asset name " + msgErrorShouldBeSpecified;
        doPost("/api/asset", asset)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(asset, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testAssignUnassignAssetToCustomer() throws Exception {
        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        Asset savedAsset = doPost("/api/asset", asset, Asset.class);

        Customer customer = new Customer();
        customer.setTitle("My customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);

        Mockito.reset(tbClusterService, auditLogService);

        Asset assignedAsset = doPost("/api/customer/" + savedCustomer.getId().getId().toString()
                + "/asset/" + savedAsset.getId().getId().toString(), Asset.class);
        Assert.assertEquals(savedCustomer.getId(), assignedAsset.getCustomerId());

        testNotifyAssignUnassignEntityAllOneTime(assignedAsset, assignedAsset.getId(), assignedAsset.getId(),
                savedTenant.getId(), savedCustomer.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ASSIGNED_TO_CUSTOMER, ActionType.UPDATED, assignedAsset.getId().toString(), savedCustomer.getId().toString(), savedCustomer.getTitle());

        Asset foundAsset = doGet("/api/asset/" + savedAsset.getId().getId().toString(), Asset.class);
        Assert.assertEquals(savedCustomer.getId(), foundAsset.getCustomerId());

        Mockito.reset(tbClusterService, auditLogService);

        Asset unassignedAsset =
                doDelete("/api/customer/asset/" + savedAsset.getId().getId().toString(), Asset.class);
        Assert.assertEquals(ModelConstants.NULL_UUID, unassignedAsset.getCustomerId().getId());

        testNotifyAssignUnassignEntityAllOneTime(savedAsset, savedAsset.getId(), savedAsset.getId(),
                savedTenant.getId(), savedCustomer.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.UNASSIGNED_FROM_CUSTOMER, ActionType.UPDATED, savedAsset.getId().toString(), savedCustomer.getId().toString(), savedCustomer.getTitle());

        foundAsset = doGet("/api/asset/" + savedAsset.getId().getId().toString(), Asset.class);
        Assert.assertEquals(ModelConstants.NULL_UUID, foundAsset.getCustomerId().getId());
    }

    @Test
    public void testAssignUnassignAssetToPublicCustomer() throws Exception {
        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        Asset savedAsset = doPost("/api/asset", asset, Asset.class);

        Mockito.reset(tbClusterService, auditLogService);

        Asset assignedAsset = doPost("/api/customer/public/asset/" + savedAsset.getId().getId().toString(), Asset.class);

        Customer publicCustomer = doGet("/api/customer/" + assignedAsset.getCustomerId(), Customer.class);
        Assert.assertTrue(publicCustomer.isPublic());

        testNotifyAssignUnassignEntityAllOneTime(assignedAsset, assignedAsset.getId(), assignedAsset.getId(),
                savedTenant.getId(), publicCustomer.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ASSIGNED_TO_CUSTOMER, ActionType.UPDATED, assignedAsset.getId().toString(),
                publicCustomer.getId().toString(), publicCustomer.getTitle());

        Asset foundAsset = doGet("/api/asset/" + savedAsset.getId().getId().toString(), Asset.class);
        Assert.assertEquals(publicCustomer.getId(), foundAsset.getCustomerId());

        Mockito.reset(tbClusterService, auditLogService);

        Asset unassignedAsset =
                doDelete("/api/customer/asset/" + savedAsset.getId().getId().toString(), Asset.class);
        Assert.assertEquals(ModelConstants.NULL_UUID, unassignedAsset.getCustomerId().getId());

        testNotifyAssignUnassignEntityAllOneTime(savedAsset, savedAsset.getId(), savedAsset.getId(),
                savedTenant.getId(), publicCustomer.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.UNASSIGNED_FROM_CUSTOMER, ActionType.UPDATED, savedAsset.getId().toString(),
                publicCustomer.getId().toString(), publicCustomer.getTitle());

        foundAsset = doGet("/api/asset/" + savedAsset.getId().getId().toString(), Asset.class);
        Assert.assertEquals(ModelConstants.NULL_UUID, foundAsset.getCustomerId().getId());
    }

    @Test
    public void testAssignAssetToNonExistentCustomer() throws Exception {
        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        Asset savedAsset = doPost("/api/asset", asset, Asset.class);

        Mockito.reset(tbClusterService, auditLogService);

        String customerIdStr = Uuids.timeBased().toString();
        doPost("/api/customer/" + customerIdStr
                + "/asset/" + savedAsset.getId().getId().toString())
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Customer", customerIdStr))));

        testNotifyEntityNever(asset.getId(), asset);
    }

    @Test
    public void testAssignAssetToCustomerFromDifferentTenant() throws Exception {
        loginSysAdmin();

        Tenant tenant2 = new Tenant();
        tenant2.setTitle("Different tenant");
        Tenant savedTenant2 = saveTenant(tenant2);
        Assert.assertNotNull(savedTenant2);

        User tenantAdmin2 = new User();
        tenantAdmin2.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin2.setTenantId(savedTenant2.getId());
        tenantAdmin2.setEmail("tenant3@thingsboard.org");
        tenantAdmin2.setFirstName("Joe");
        tenantAdmin2.setLastName("Downs");

        createUserAndLogin(tenantAdmin2, "testPassword1");

        Customer customer = new Customer();
        customer.setTitle("Different customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);

        login(tenantAdmin.getEmail(), "testPassword1");

        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        Asset savedAsset = doPost("/api/asset", asset, Asset.class);

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/customer/" + savedCustomer.getId().getId().toString()
                + "/asset/" + savedAsset.getId().getId().toString())
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(savedAsset.getId(), savedAsset);

        loginSysAdmin();

        deleteTenant(savedTenant2.getId());
    }

    @Test
    public void testFindTenantAssets() throws Exception {
        List<Asset> assets = new ArrayList<>();
        int cntEntity = 178;

        Mockito.reset(tbClusterService, auditLogService);

        for (int i = 0; i < cntEntity; i++) {
            Asset asset = new Asset();
            asset.setName("Asset" + i);
            asset.setType("default");
            assets.add(doPost("/api/asset", asset, Asset.class));
        }
        List<Asset> loadedAssets = new ArrayList<>();
        PageLink pageLink = new PageLink(23);
        PageData<Asset> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/assets?",
                    new TypeReference<PageData<Asset>>() {
                    }, pageLink);
            loadedAssets.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(new Asset(), new Asset(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, cntEntity, cntEntity, cntEntity);

        assets.sort(idComparator);
        loadedAssets.sort(idComparator);

        Assert.assertEquals(assets, loadedAssets);
    }

    @Test
    public void testFindTenantAssetsByName() throws Exception {
        String title1 = "Asset title 1";
        List<Asset> assetsTitle1 = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            Asset asset = new Asset();
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType("default");
            assetsTitle1.add(doPost("/api/asset", asset, Asset.class));
        }
        String title2 = "Asset title 2";
        List<Asset> assetsTitle2 = new ArrayList<>();
        for (int i = 0; i < 75; i++) {
            Asset asset = new Asset();
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType("default");
            assetsTitle2.add(doPost("/api/asset", asset, Asset.class));
        }

        List<Asset> loadedAssetsTitle1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15, 0, title1);
        PageData<Asset> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/assets?",
                    new TypeReference<PageData<Asset>>() {
                    }, pageLink);
            loadedAssetsTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assetsTitle1.sort(idComparator);
        loadedAssetsTitle1.sort(idComparator);

        Assert.assertEquals(assetsTitle1, loadedAssetsTitle1);

        List<Asset> loadedAssetsTitle2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/assets?",
                    new TypeReference<PageData<Asset>>() {
                    }, pageLink);
            loadedAssetsTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assetsTitle2.sort(idComparator);
        loadedAssetsTitle2.sort(idComparator);

        Assert.assertEquals(assetsTitle2, loadedAssetsTitle2);

        for (Asset asset : loadedAssetsTitle1) {
            doDelete("/api/asset/" + asset.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, title1);
        pageData = doGetTypedWithPageLink("/api/tenant/assets?",
                new TypeReference<PageData<Asset>>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Asset asset : loadedAssetsTitle2) {
            doDelete("/api/asset/" + asset.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, title2);
        pageData = doGetTypedWithPageLink("/api/tenant/assets?",
                new TypeReference<PageData<Asset>>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindTenantAssetsByType() throws Exception {
        String title1 = "Asset title 1";
        String type1 = "typeA";
        List<Asset> assetsType1 = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            Asset asset = new Asset();
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType(type1);
            assetsType1.add(doPost("/api/asset", asset, Asset.class));
        }
        String title2 = "Asset title 2";
        String type2 = "typeB";
        List<Asset> assetsType2 = new ArrayList<>();
        for (int i = 0; i < 75; i++) {
            Asset asset = new Asset();
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType(type2);
            assetsType2.add(doPost("/api/asset", asset, Asset.class));
        }

        List<Asset> loadedAssetsType1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15);
        PageData<Asset> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/assets?type={type}&",
                    new TypeReference<PageData<Asset>>() {
                    }, pageLink, type1);
            loadedAssetsType1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assetsType1.sort(idComparator);
        loadedAssetsType1.sort(idComparator);

        Assert.assertEquals(assetsType1, loadedAssetsType1);

        List<Asset> loadedAssetsType2 = new ArrayList<>();
        pageLink = new PageLink(4);
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/assets?type={type}&",
                    new TypeReference<PageData<Asset>>() {
                    }, pageLink, type2);
            loadedAssetsType2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assetsType2.sort(idComparator);
        loadedAssetsType2.sort(idComparator);

        Assert.assertEquals(assetsType2, loadedAssetsType2);

        for (Asset asset : loadedAssetsType1) {
            doDelete("/api/asset/" + asset.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4);
        pageData = doGetTypedWithPageLink("/api/tenant/assets?type={type}&",
                new TypeReference<PageData<Asset>>() {
                }, pageLink, type1);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Asset asset : loadedAssetsType2) {
            doDelete("/api/asset/" + asset.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4);
        pageData = doGetTypedWithPageLink("/api/tenant/assets?type={type}&",
                new TypeReference<PageData<Asset>>() {
                }, pageLink, type2);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindCustomerAssets() throws Exception {
        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer = doPost("/api/customer", customer, Customer.class);
        CustomerId customerId = customer.getId();

        List<Asset> assets = new ArrayList<>();
        for (int i = 0; i < 128; i++) {
            Asset asset = new Asset();
            asset.setName("Asset" + i);
            asset.setType("default");
            asset = doPost("/api/asset", asset, Asset.class);
            assets.add(doPost("/api/customer/" + customerId.getId().toString()
                    + "/asset/" + asset.getId().getId().toString(), Asset.class));
        }

        List<Asset> loadedAssets = new ArrayList<>();
        PageLink pageLink = new PageLink(23);
        PageData<Asset> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/assets?",
                    new TypeReference<PageData<Asset>>() {
                    }, pageLink);
            loadedAssets.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assets.sort(idComparator);
        loadedAssets.sort(idComparator);

        Assert.assertEquals(assets, loadedAssets);
    }

    @Test
    public void testFindCustomerAssetsByName() throws Exception {
        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer = doPost("/api/customer", customer, Customer.class);
        CustomerId customerId = customer.getId();

        String title1 = "Asset title 1";
        List<Asset> assetsTitle1 = new ArrayList<>();
        for (int i = 0; i < 125; i++) {
            Asset asset = new Asset();
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType("default");
            asset = doPost("/api/asset", asset, Asset.class);
            assetsTitle1.add(doPost("/api/customer/" + customerId.getId().toString()
                    + "/asset/" + asset.getId().getId().toString(), Asset.class));
        }
        String title2 = "Asset title 2";
        List<Asset> assetsTitle2 = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            Asset asset = new Asset();
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType("default");
            asset = doPost("/api/asset", asset, Asset.class);
            assetsTitle2.add(doPost("/api/customer/" + customerId.getId().toString()
                    + "/asset/" + asset.getId().getId().toString(), Asset.class));
        }

        List<Asset> loadedAssetsTitle1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15, 0, title1);
        PageData<Asset> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/assets?",
                    new TypeReference<PageData<Asset>>() {
                    }, pageLink);
            loadedAssetsTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assetsTitle1.sort(idComparator);
        loadedAssetsTitle1.sort(idComparator);

        Assert.assertEquals(assetsTitle1, loadedAssetsTitle1);

        List<Asset> loadedAssetsTitle2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/assets?",
                    new TypeReference<PageData<Asset>>() {
                    }, pageLink);
            loadedAssetsTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assetsTitle2.sort(idComparator);
        loadedAssetsTitle2.sort(idComparator);

        Assert.assertEquals(assetsTitle2, loadedAssetsTitle2);

        for (Asset asset : loadedAssetsTitle1) {
            doDelete("/api/customer/asset/" + asset.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, title1);
        pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/assets?",
                new TypeReference<PageData<Asset>>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Asset asset : loadedAssetsTitle2) {
            doDelete("/api/customer/asset/" + asset.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, title2);
        pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/assets?",
                new TypeReference<PageData<Asset>>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindCustomerAssetsByType() throws Exception {
        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer = doPost("/api/customer", customer, Customer.class);
        CustomerId customerId = customer.getId();

        String title1 = "Asset title 1";
        String type1 = "typeC";
        List<Asset> assetsType1 = new ArrayList<>();
        for (int i = 0; i < 125; i++) {
            Asset asset = new Asset();
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType(type1);
            asset = doPost("/api/asset", asset, Asset.class);
            assetsType1.add(doPost("/api/customer/" + customerId.getId().toString()
                    + "/asset/" + asset.getId().getId().toString(), Asset.class));
        }
        String title2 = "Asset title 2";
        String type2 = "typeD";
        List<Asset> assetsType2 = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            Asset asset = new Asset();
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType(type2);
            asset = doPost("/api/asset", asset, Asset.class);
            assetsType2.add(doPost("/api/customer/" + customerId.getId().toString()
                    + "/asset/" + asset.getId().getId().toString(), Asset.class));
        }

        List<Asset> loadedAssetsType1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15);
        PageData<Asset> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/assets?type={type}&",
                    new TypeReference<PageData<Asset>>() {
                    }, pageLink, type1);
            loadedAssetsType1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assetsType1.sort(idComparator);
        loadedAssetsType1.sort(idComparator);

        Assert.assertEquals(assetsType1, loadedAssetsType1);

        List<Asset> loadedAssetsType2 = new ArrayList<>();
        pageLink = new PageLink(4);
        do {
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/assets?type={type}&",
                    new TypeReference<PageData<Asset>>() {
                    }, pageLink, type2);
            loadedAssetsType2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assetsType2.sort(idComparator);
        loadedAssetsType2.sort(idComparator);

        Assert.assertEquals(assetsType2, loadedAssetsType2);

        for (Asset asset : loadedAssetsType1) {
            doDelete("/api/customer/asset/" + asset.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4);
        pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/assets?type={type}&",
                new TypeReference<PageData<Asset>>() {
                }, pageLink, type1);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Asset asset : loadedAssetsType2) {
            doDelete("/api/customer/asset/" + asset.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4);
        pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/assets?type={type}&",
                new TypeReference<PageData<Asset>>() {
                }, pageLink, type2);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testAssignAssetToEdge() throws Exception {
        Edge edge = constructEdge("My edge", "default");
        Edge savedEdge = doPost("/api/edge", edge, Edge.class);

        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        Asset savedAsset = doPost("/api/asset", asset, Asset.class);

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/edge/" + savedEdge.getId().getId().toString()
                + "/asset/" + savedAsset.getId().getId().toString(), Asset.class);

        testNotifyEntityAllOneTime(savedAsset, savedAsset.getId(), savedAsset.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ASSIGNED_TO_EDGE,
                savedAsset.getId().getId().toString(), savedEdge.getId().getId().toString(), edge.getName());


        PageData<Asset> pageData = doGetTypedWithPageLink("/api/edge/" + savedEdge.getId().getId().toString() + "/assets?",
                new TypeReference<PageData<Asset>>() {
                }, new PageLink(100));

        Assert.assertEquals(1, pageData.getData().size());

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/edge/" + savedEdge.getId().getId().toString()
                + "/asset/" + savedAsset.getId().getId().toString(), Asset.class);


        testNotifyEntityAllOneTime(savedAsset, savedAsset.getId(), savedAsset.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.UNASSIGNED_FROM_EDGE, savedAsset.getId().getId().toString(), savedEdge.getId().getId().toString(), savedEdge.getName());

        pageData = doGetTypedWithPageLink("/api/edge/" + savedEdge.getId().getId().toString() + "/assets?",
                new TypeReference<PageData<Asset>>() {
                }, new PageLink(100));

        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testDeleteAssetWithDeleteRelationsOk() throws Exception {
        AssetId assetId = createAsset("Asset for Test WithRelationsOk").getId();
        testEntityDaoWithRelationsOk(savedTenant.getId(), assetId, "/api/asset/" + assetId);
    }

    @Ignore
    @Test
    public void testDeleteAssetExceptionWithRelationsTransactional() throws Exception {
        AssetId assetId = createAsset("Asset for Test WithRelations Transactional Exception").getId();
        testEntityDaoWithRelationsTransactionalException(assetDao, savedTenant.getId(), assetId, "/api/asset/" + assetId);
    }

    @Test
    public void testSaveAssetWithUniquifyStrategy() throws Exception {
        Asset asset = new Asset();
        asset.setName("My unique asset");
        asset.setType("default");
        doPost("/api/asset", asset, Asset.class);

        doPost("/api/asset", asset).andExpect(status().isBadRequest());

        doPost("/api/asset?nameConflictPolicy=FAIL", asset).andExpect(status().isBadRequest());

        Asset secondAsset = doPost("/api/asset?nameConflictPolicy=UNIQUIFY", asset, Asset.class);
        assertThat(secondAsset.getName()).startsWith("My unique asset_");

        Asset thirdAsset = doPost("/api/asset?nameConflictPolicy=UNIQUIFY&uniquifySeparator=-", asset, Asset.class);
        assertThat(thirdAsset.getName()).startsWith("My unique asset-");

        Asset fourthAsset = doPost("/api/asset?nameConflictPolicy=UNIQUIFY&uniquifyStrategy=INCREMENTAL", asset, Asset.class);
        assertThat(fourthAsset.getName()).isEqualTo("My unique asset_1");

        Asset fifthAsset = doPost("/api/asset?nameConflictPolicy=UNIQUIFY&uniquifyStrategy=INCREMENTAL", asset, Asset.class);
        assertThat(fifthAsset.getName()).isEqualTo("My unique asset_2");
    }

    private Asset createAsset(String name) {
        Asset asset = new Asset();
        asset.setName(name);
        asset.setType("default");
        return doPost("/api/asset", asset, Asset.class);
    }
}
