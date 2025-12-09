/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.asset.AssetProfileInfo;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.asset.AssetProfileDao;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.DataConstants.DEFAULT_DEVICE_TYPE;

@ContextConfiguration(classes = {AssetProfileControllerTest.Config.class})
@DaoSqlTest
public class AssetProfileControllerTest extends AbstractControllerTest {

    private IdComparator<AssetProfile> idComparator = new IdComparator<>();
    private IdComparator<AssetProfileInfo> assetProfileInfoIdComparator = new IdComparator<>();

    private Tenant savedTenant;
    private User tenantAdmin;

    @Autowired
    private AssetProfileDao assetProfileDao;

    static class Config {
        @Bean
        @Primary
        public AssetProfileDao assetProfileDao(AssetProfileDao assetProfileDao) {
            return Mockito.mock(AssetProfileDao.class, AdditionalAnswers.delegatesTo(assetProfileDao));
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
    public void testSaveAssetProfile() throws Exception {
        AssetProfile assetProfile = this.createAssetProfile("Asset Profile");

        Mockito.reset(tbClusterService, auditLogService);

        AssetProfile savedAssetProfile = doPost("/api/assetProfile", assetProfile, AssetProfile.class);
        Assert.assertNotNull(savedAssetProfile);
        Assert.assertNotNull(savedAssetProfile.getId());
        Assert.assertTrue(savedAssetProfile.getCreatedTime() > 0);
        Assert.assertEquals(assetProfile.getName(), savedAssetProfile.getName());
        Assert.assertEquals(assetProfile.getDescription(), savedAssetProfile.getDescription());
        Assert.assertEquals(assetProfile.isDefault(), savedAssetProfile.isDefault());
        Assert.assertEquals(assetProfile.getDefaultRuleChainId(), savedAssetProfile.getDefaultRuleChainId());

        testNotifyEntityBroadcastEntityStateChangeEventOneTime(savedAssetProfile, savedAssetProfile.getId(), savedAssetProfile.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED);

        savedAssetProfile.setName("New asset profile");
        doPost("/api/assetProfile", savedAssetProfile, AssetProfile.class);
        AssetProfile foundAssetProfile = doGet("/api/assetProfile/" + savedAssetProfile.getId().getId().toString(), AssetProfile.class);
        Assert.assertEquals(savedAssetProfile.getName(), foundAssetProfile.getName());

        testNotifyEntityBroadcastEntityStateChangeEventOneTime(foundAssetProfile, foundAssetProfile.getId(), foundAssetProfile.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.UPDATED);
    }

    @Test
    public void saveAssetProfileWithViolationOfValidation() throws Exception {
        String msgError = msgErrorFieldLength("name");

        Mockito.reset(tbClusterService, auditLogService);

        AssetProfile createAssetProfile = this.createAssetProfile(StringUtils.randomAlphabetic(300));
        doPost("/api/assetProfile", createAssetProfile)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(createAssetProfile, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testFindAssetProfileById() throws Exception {
        AssetProfile assetProfile = this.createAssetProfile("Asset Profile");
        AssetProfile savedAssetProfile = doPost("/api/assetProfile", assetProfile, AssetProfile.class);
        AssetProfile foundAssetProfile = doGet("/api/assetProfile/" + savedAssetProfile.getId().getId().toString(), AssetProfile.class);
        Assert.assertNotNull(foundAssetProfile);
        Assert.assertEquals(savedAssetProfile, foundAssetProfile);
    }

    @Test
    public void whenGetAssetProfileById_thenPermissionsAreChecked() throws Exception {
        AssetProfile assetProfile = createAssetProfile("Asset profile 1");
        assetProfile = doPost("/api/assetProfile", assetProfile, AssetProfile.class);

        loginDifferentTenant();

        doGet("/api/assetProfile/" + assetProfile.getId())
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));
    }

    @Test
    public void testFindAssetProfileInfoById() throws Exception {
        AssetProfile assetProfile = this.createAssetProfile("Asset Profile");
        AssetProfile savedAssetProfile = doPost("/api/assetProfile", assetProfile, AssetProfile.class);
        AssetProfileInfo foundAssetProfileInfo = doGet("/api/assetProfileInfo/" + savedAssetProfile.getId().getId().toString(), AssetProfileInfo.class);
        Assert.assertNotNull(foundAssetProfileInfo);
        Assert.assertEquals(savedAssetProfile.getId(), foundAssetProfileInfo.getId());
        Assert.assertEquals(savedAssetProfile.getName(), foundAssetProfileInfo.getName());

        Customer customer = new Customer();
        customer.setTitle("Customer");
        customer.setTenantId(savedTenant.getId());
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);

        User customerUser = new User();
        customerUser.setAuthority(Authority.CUSTOMER_USER);
        customerUser.setTenantId(savedTenant.getId());
        customerUser.setCustomerId(savedCustomer.getId());
        customerUser.setEmail("customer2@thingsboard.org");

        createUserAndLogin(customerUser, "customer");

        foundAssetProfileInfo = doGet("/api/assetProfileInfo/" + savedAssetProfile.getId().getId().toString(), AssetProfileInfo.class);
        Assert.assertNotNull(foundAssetProfileInfo);
        Assert.assertEquals(savedAssetProfile.getId(), foundAssetProfileInfo.getId());
        Assert.assertEquals(savedAssetProfile.getName(), foundAssetProfileInfo.getName());
    }

    @Test
    public void testFindAssetProfileInfoByIds() throws Exception {
        List<AssetProfileInfo> assetProfiles = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            AssetProfile assetProfile = this.createAssetProfile("Asset Profile" + i);
            assetProfile.setTenantId(savedTenant.getId());
            assetProfiles.add(doPost("/api/assetProfile", assetProfile, AssetProfileInfo.class));
        }

        List<AssetProfileInfo> expected = assetProfiles.subList(5, 15);

        String idsParam = expected.stream()
                .map(ap -> ap.getId().getId().toString())
                .collect(Collectors.joining(","));
        AssetProfileInfo[] foundAssetProfileInfos = doGet("/api/assetProfileInfos?assetProfileIds=" + idsParam, AssetProfileInfo[].class);

        Assert.assertNotNull(foundAssetProfileInfos);
        Assert.assertEquals(expected.size(), foundAssetProfileInfos.length);

        Map<UUID, AssetProfileInfo> infoById = Arrays.stream(foundAssetProfileInfos)
                .collect(Collectors.toMap(info -> info.getId().getId(), Function.identity()));

        for (AssetProfileInfo assetProfileInfo : expected) {
            UUID id = assetProfileInfo.getId().getId();
            AssetProfileInfo info = infoById.get(id);
            Assert.assertNotNull("AssetProfileInfo not found for id " + id, info);

            Assert.assertEquals(assetProfileInfo.getId(), info.getId());
            Assert.assertEquals(assetProfileInfo.getName(), info.getName());
        }
    }

    @Test
    public void whenGetAssetProfileInfoById_thenPermissionsAreChecked() throws Exception {
        AssetProfile assetProfile = createAssetProfile("Asset profile 1");
        assetProfile = doPost("/api/assetProfile", assetProfile, AssetProfile.class);

        loginDifferentTenant();
        doGet("/api/assetProfileInfo/" + assetProfile.getId())
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));
    }

    @Test
    public void testFindDefaultAssetProfileInfo() throws Exception {
        AssetProfileInfo foundDefaultAssetProfileInfo = doGet("/api/assetProfileInfo/default", AssetProfileInfo.class);
        Assert.assertNotNull(foundDefaultAssetProfileInfo);
        Assert.assertNotNull(foundDefaultAssetProfileInfo.getId());
        Assert.assertNotNull(foundDefaultAssetProfileInfo.getName());
        Assert.assertEquals("default", foundDefaultAssetProfileInfo.getName());
    }

    @Test
    public void testSetDefaultAssetProfile() throws Exception {
        AssetProfile assetProfile = this.createAssetProfile("Asset Profile 1");
        AssetProfile savedAssetProfile = doPost("/api/assetProfile", assetProfile, AssetProfile.class);

        Mockito.reset(tbClusterService, auditLogService);

        AssetProfile defaultAssetProfile = doPost("/api/assetProfile/" + savedAssetProfile.getId().getId().toString() + "/default", AssetProfile.class);
        Assert.assertNotNull(defaultAssetProfile);
        AssetProfileInfo foundDefaultAssetProfile = doGet("/api/assetProfileInfo/default", AssetProfileInfo.class);
        Assert.assertNotNull(foundDefaultAssetProfile);
        Assert.assertEquals(savedAssetProfile.getName(), foundDefaultAssetProfile.getName());
        Assert.assertEquals(savedAssetProfile.getId(), foundDefaultAssetProfile.getId());

        testNotifyEntityOneTimeMsgToEdgeServiceNever(defaultAssetProfile, defaultAssetProfile.getId(), defaultAssetProfile.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.UPDATED);
    }

    @Test
    public void testSaveAssetProfileWithEmptyName() throws Exception {
        AssetProfile assetProfile = new AssetProfile();

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = "Asset profile name " + msgErrorShouldBeSpecified;
        doPost("/api/assetProfile", assetProfile)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(assetProfile, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testSaveAssetProfileWithSameName() throws Exception {
        AssetProfile assetProfile = this.createAssetProfile("Asset Profile");
        doPost("/api/assetProfile", assetProfile).andExpect(status().isOk());
        AssetProfile assetProfile2 = this.createAssetProfile("Asset Profile");

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = "Asset profile with such name already exists";
        doPost("/api/assetProfile", assetProfile2)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(assetProfile, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testDeleteAssetProfileWithExistingAsset() throws Exception {
        AssetProfile assetProfile = this.createAssetProfile("Asset Profile");
        AssetProfile savedAssetProfile = doPost("/api/assetProfile", assetProfile, AssetProfile.class);

        Asset asset = new Asset();
        asset.setName("Test asset");
        asset.setAssetProfileId(savedAssetProfile.getId());

        doPost("/api/asset", asset, Asset.class);

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/assetProfile/" + savedAssetProfile.getId().getId().toString())
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("The asset profile referenced by the assets cannot be deleted")));

        testNotifyEntityNever(savedAssetProfile.getId(), savedAssetProfile);
    }

    @Test
    public void testSaveAssetProfileWithRuleChainFromDifferentTenant() throws Exception {
        loginDifferentTenant();
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName("Different rule chain");
        RuleChain savedRuleChain = doPost("/api/ruleChain", ruleChain, RuleChain.class);

        loginTenantAdmin();

        AssetProfile assetProfile = this.createAssetProfile("Asset Profile");
        assetProfile.setDefaultRuleChainId(savedRuleChain.getId());
        doPost("/api/assetProfile", assetProfile).andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Can't assign rule chain from different tenant!")));
    }

    @Test
    public void testSaveAssetProfileWithDashboardFromDifferentTenant() throws Exception {
        loginDifferentTenant();
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("Different dashboard");
        Dashboard savedDashboard = doPost("/api/dashboard", dashboard, Dashboard.class);

        loginTenantAdmin();

        AssetProfile assetProfile = this.createAssetProfile("Asset Profile");
        assetProfile.setDefaultDashboardId(savedDashboard.getId());
        doPost("/api/assetProfile", assetProfile).andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Can't assign dashboard from different tenant!")));
    }

    @Test
    public void testDeleteAssetProfile() throws Exception {
        AssetProfile assetProfile = this.createAssetProfile("Asset Profile");
        AssetProfile savedAssetProfile = doPost("/api/assetProfile", assetProfile, AssetProfile.class);

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/assetProfile/" + savedAssetProfile.getId().getId().toString())
                .andExpect(status().isOk());

        String savedAssetProfileIdStr = savedAssetProfile.getId().getId().toString();
        testNotifyEntityBroadcastEntityStateChangeEventOneTime(savedAssetProfile, savedAssetProfile.getId(), savedAssetProfile.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.DELETED, savedAssetProfileIdStr);

        doGet("/api/assetProfile/" + savedAssetProfile.getId().getId().toString())
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Asset profile", savedAssetProfileIdStr))));
    }

    @Test
    public void testFindAssetProfiles() throws Exception {
        List<AssetProfile> assetProfiles = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<AssetProfile> pageData = doGetTypedWithPageLink("/api/assetProfiles?",
                new TypeReference<>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
        assetProfiles.addAll(pageData.getData());

        Mockito.reset(tbClusterService, auditLogService);

        int cntEntity = 28;
        for (int i = 0; i < cntEntity; i++) {
            AssetProfile assetProfile = this.createAssetProfile("Asset Profile" + i);
            assetProfiles.add(doPost("/api/assetProfile", assetProfile, AssetProfile.class));
        }

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(new AssetProfile(), new AssetProfile(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, cntEntity, cntEntity, cntEntity);
        Mockito.reset(tbClusterService, auditLogService);

        List<AssetProfile> loadedAssetProfiles = new ArrayList<>();
        pageLink = new PageLink(17);
        do {
            pageData = doGetTypedWithPageLink("/api/assetProfiles?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedAssetProfiles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetProfiles, idComparator);
        Collections.sort(loadedAssetProfiles, idComparator);

        Assert.assertEquals(assetProfiles, loadedAssetProfiles);

        for (AssetProfile assetProfile : loadedAssetProfiles) {
            if (!assetProfile.isDefault()) {
                doDelete("/api/assetProfile/" + assetProfile.getId().getId().toString())
                        .andExpect(status().isOk());
            }
        }

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(loadedAssetProfiles.get(0), loadedAssetProfiles.get(0),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.DELETED, cntEntity, cntEntity, cntEntity, loadedAssetProfiles.get(0).getId().getId().toString());

        pageLink = new PageLink(17);
        pageData = doGetTypedWithPageLink("/api/assetProfiles?",
                new TypeReference<>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
    }

    @Test
    public void testFindAssetProfileInfos() throws Exception {
        List<AssetProfile> assetProfiles = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<AssetProfile> assetProfilePageData = doGetTypedWithPageLink("/api/assetProfiles?",
                new TypeReference<PageData<AssetProfile>>() {
                }, pageLink);
        Assert.assertFalse(assetProfilePageData.hasNext());
        Assert.assertEquals(1, assetProfilePageData.getTotalElements());
        assetProfiles.addAll(assetProfilePageData.getData());

        for (int i = 0; i < 28; i++) {
            AssetProfile assetProfile = this.createAssetProfile("Asset Profile" + i);
            assetProfiles.add(doPost("/api/assetProfile", assetProfile, AssetProfile.class));
        }

        List<AssetProfileInfo> loadedAssetProfileInfos = new ArrayList<>();
        pageLink = new PageLink(17);
        PageData<AssetProfileInfo> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/assetProfileInfos?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedAssetProfileInfos.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetProfiles, idComparator);
        Collections.sort(loadedAssetProfileInfos, assetProfileInfoIdComparator);

        List<AssetProfileInfo> assetProfileInfos = assetProfiles.stream().map(assetProfile -> new AssetProfileInfo(assetProfile.getId(),
                assetProfile.getTenantId(),
                assetProfile.getName(), assetProfile.getImage(), assetProfile.getDefaultDashboardId())).collect(Collectors.toList());

        Assert.assertEquals(assetProfileInfos, loadedAssetProfileInfos);

        for (AssetProfile assetProfile : assetProfiles) {
            if (!assetProfile.isDefault()) {
                doDelete("/api/assetProfile/" + assetProfile.getId().getId().toString())
                        .andExpect(status().isOk());
            }
        }

        pageLink = new PageLink(17);
        pageData = doGetTypedWithPageLink("/api/assetProfileInfos?",
                new TypeReference<PageData<AssetProfileInfo>>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
    }

    @Test
    public void testDeleteAssetProfileWithDeleteRelationsOk() throws Exception {
        AssetProfileId assetProfileId = savedAssetProfile("AssetProfile for Test WithRelationsOk").getId();
        testEntityDaoWithRelationsOk(savedTenant.getId(), assetProfileId, "/api/assetProfile/" + assetProfileId);
    }

    @Test
    public void testDeleteAssetProfileExceptionWithRelationsTransactional() throws Exception {
        AssetProfileId assetProfileId = savedAssetProfile("AssetProfile for Test WithRelations Transactional Exception").getId();
        testEntityDaoWithRelationsTransactionalException(assetProfileDao, savedTenant.getId(), assetProfileId, "/api/assetProfile/" + assetProfileId);
    }

    @Test
    public void testGetAssetProfileNames() throws Exception {
        var pageLink = new PageLink(Integer.MAX_VALUE);
        var assetProfileInfos = doGetTypedWithPageLink("/api/assetProfileInfos?",
                new TypeReference<PageData<AssetProfileInfo>>() {
                }, pageLink);
        Assert.assertNotNull("Asset Profile Infos page data is null!", assetProfileInfos);
        Assert.assertEquals("Asset Profile Infos Page data is empty! Expected to have default profile created!", 1, assetProfileInfos.getTotalElements());
        List<EntityInfo> expectedAssetProfileNames = assetProfileInfos.getData().stream()
                .map(info -> new EntityInfo(info.getId(), info.getName()))
                .sorted(Comparator.comparing(EntityInfo::getName))
                .collect(Collectors.toList());
        var assetProfileNames = doGetTyped("/api/assetProfile/names", new TypeReference<List<EntityInfo>>() {
        });
        Assert.assertNotNull("Asset Profile Names list is null!", assetProfileNames);
        Assert.assertFalse("Asset Profile Names list is empty!", assetProfileNames.isEmpty());
        Assert.assertEquals(expectedAssetProfileNames, assetProfileNames);
        Assert.assertEquals(1, assetProfileNames.size());
        Assert.assertEquals(DEFAULT_DEVICE_TYPE, assetProfileNames.get(0).getName());

        int count = 3;
        for (int i = 0; i < count; i++) {
            Asset asset = new Asset();
            asset.setName("AssetName" + i);
            asset.setType("AssetProfileName" + i);
            Asset savedAsset = doPost("/api/asset", asset, Asset.class);
            Assert.assertNotNull(savedAsset);
        }
        assetProfileInfos = doGetTypedWithPageLink("/api/assetProfileInfos?",
                new TypeReference<>() {
                }, pageLink);
        Assert.assertNotNull("Asset Profile Infos page data is null!", assetProfileInfos);
        Assert.assertEquals("Asset Profile Infos Page data is empty! Expected to have default profile created + count value!", 1 + count, assetProfileInfos.getTotalElements());
        expectedAssetProfileNames = assetProfileInfos.getData().stream()
                .map(info -> new EntityInfo(info.getId(), info.getName()))
                .sorted(Comparator.comparing(EntityInfo::getName))
                .collect(Collectors.toList());

        assetProfileNames = doGetTyped("/api/assetProfile/names", new TypeReference<>() {
        });
        Assert.assertNotNull("Asset Profile Names list is null!", assetProfileNames);
        Assert.assertFalse("Asset Profile Names list is empty!", assetProfileNames.isEmpty());
        Assert.assertEquals(expectedAssetProfileNames, assetProfileNames);
        Assert.assertEquals(1 + count, assetProfileNames.size());

        assetProfileNames = doGetTyped("/api/assetProfile/names?activeOnly=true", new TypeReference<>() {
        });
        Assert.assertNotNull("Asset Profile Names list is null!", assetProfileNames);
        Assert.assertFalse("Asset Profile Names list is empty!", assetProfileNames.isEmpty());
        var expectedAssetProfileNamesWithoutDefault = expectedAssetProfileNames.stream()
                .filter(entityInfo -> !entityInfo.getName().equals(DEFAULT_DEVICE_TYPE))
                .collect(Collectors.toList());
        Assert.assertEquals(expectedAssetProfileNamesWithoutDefault, assetProfileNames);
        Assert.assertEquals(count, assetProfileNames.size());
    }

    private AssetProfile savedAssetProfile(String name) {
        AssetProfile assetProfile = createAssetProfile(name);
        return doPost("/api/assetProfile", assetProfile, AssetProfile.class);
    }
}
