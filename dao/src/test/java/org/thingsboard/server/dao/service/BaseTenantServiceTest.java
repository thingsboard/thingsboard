/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.dao.service;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantInfo;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.tenant.TenantDao;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.thingsboard.server.common.data.ota.OtaPackageType.FIRMWARE;

public abstract class BaseTenantServiceTest extends AbstractServiceTest {

    private IdComparator<Tenant> idComparator = new IdComparator<>();

    @SpyBean
    protected TenantDao tenantDao;

    @Autowired
    CacheManager cacheManager;

    @Test
    public void testSaveTenant() {
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = tenantService.saveTenant(tenant);
        Assert.assertNotNull(savedTenant);
        Assert.assertNotNull(savedTenant.getId());
        Assert.assertTrue(savedTenant.getCreatedTime() > 0);
        Assert.assertEquals(tenant.getTitle(), savedTenant.getTitle());
        
        savedTenant.setTitle("My new tenant");
        tenantService.saveTenant(savedTenant);
        Tenant foundTenant = tenantService.findTenantById(savedTenant.getId());
        Assert.assertEquals(foundTenant.getTitle(), savedTenant.getTitle());
        
        tenantService.deleteTenant(savedTenant.getId());
    }
    
    @Test
    public void testFindTenantById() {
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = tenantService.saveTenant(tenant);
        Tenant foundTenant = tenantService.findTenantById(savedTenant.getId());
        Assert.assertNotNull(foundTenant);
        Assert.assertEquals(savedTenant, foundTenant);
        tenantService.deleteTenant(savedTenant.getId());
    }

    @Test
    public void testFindTenantInfoById() {
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = tenantService.saveTenant(tenant);
        TenantInfo foundTenant = tenantService.findTenantInfoById(savedTenant.getId());
        Assert.assertNotNull(foundTenant);
        Assert.assertEquals(new TenantInfo(savedTenant, "Default"), foundTenant);
        tenantService.deleteTenant(savedTenant.getId());
    }
    
    @Test(expected = DataValidationException.class)
    public void testSaveTenantWithEmptyTitle() {
        Tenant tenant = new Tenant();
        tenantService.saveTenant(tenant);
    }
    
    @Test(expected = DataValidationException.class)
    public void testSaveTenantWithInvalidEmail() {
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        tenant.setEmail("invalid@mail");
        tenantService.saveTenant(tenant);
    }
    
    @Test
    public void testDeleteTenant() {
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = tenantService.saveTenant(tenant);
        tenantService.deleteTenant(savedTenant.getId());
        Tenant foundTenant = tenantService.findTenantById(savedTenant.getId());
        Assert.assertNull(foundTenant);
    }
    
    @Test
    public void testFindTenants() {
        
        List<Tenant> tenants = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<Tenant> pageData = tenantService.findTenants(pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
        tenants.addAll(pageData.getData());
        
        for (int i=0;i<156;i++) {
            Tenant tenant = new Tenant();
            tenant.setTitle("Tenant"+i);
            tenants.add(tenantService.saveTenant(tenant));
        }
        
        List<Tenant> loadedTenants = new ArrayList<>();
        pageLink = new PageLink(17);
        do {
            pageData = tenantService.findTenants(pageLink);
            loadedTenants.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());
        
        Collections.sort(tenants, idComparator);
        Collections.sort(loadedTenants, idComparator);
        
        Assert.assertEquals(tenants, loadedTenants);
        
        for (Tenant tenant : loadedTenants) {
            tenantService.deleteTenant(tenant.getId());
        }
        
        pageLink = new PageLink(17);
        pageData = tenantService.findTenants(pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
        
    }
    
    @Test
    public void testFindTenantsByTitle() {
        String title1 = "Tenant title 1";
        List<Tenant> tenantsTitle1 = new ArrayList<>();
        for (int i=0;i<134;i++) {
            Tenant tenant = new Tenant();
            String suffix = RandomStringUtils.randomAlphanumeric((int)(Math.random()*15));
            String title = title1+suffix;
            title = i % 2 == 0 ? title.toLowerCase() : title.toUpperCase();
            tenant.setTitle(title);
            tenantsTitle1.add(tenantService.saveTenant(tenant));
        }
        String title2 = "Tenant title 2";
        List<Tenant> tenantsTitle2 = new ArrayList<>();
        for (int i=0;i<127;i++) {
            Tenant tenant = new Tenant();
            String suffix = RandomStringUtils.randomAlphanumeric((int)(Math.random()*15));
            String title = title2+suffix;
            title = i % 2 == 0 ? title.toLowerCase() : title.toUpperCase();
            tenant.setTitle(title);
            tenantsTitle2.add(tenantService.saveTenant(tenant));
        }
        
        List<Tenant> loadedTenantsTitle1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15, 0, title1);
        PageData<Tenant> pageData = null;
        do {
            pageData = tenantService.findTenants(pageLink);
            loadedTenantsTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());
        
        Collections.sort(tenantsTitle1, idComparator);
        Collections.sort(loadedTenantsTitle1, idComparator);
        
        Assert.assertEquals(tenantsTitle1, loadedTenantsTitle1);
        
        List<Tenant> loadedTenantsTitle2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = tenantService.findTenants(pageLink);
            loadedTenantsTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(tenantsTitle2, idComparator);
        Collections.sort(loadedTenantsTitle2, idComparator);
        
        Assert.assertEquals(tenantsTitle2, loadedTenantsTitle2);

        for (Tenant tenant : loadedTenantsTitle1) {
            tenantService.deleteTenant(tenant.getId());
        }
        
        pageLink = new PageLink(4, 0, title1);
        pageData = tenantService.findTenants(pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
        
        for (Tenant tenant : loadedTenantsTitle2) {
            tenantService.deleteTenant(tenant.getId());
        }
        
        pageLink = new PageLink(4, 0, title2);
        pageData = tenantService.findTenants(pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindTenantInfos() {

        List<TenantInfo> tenants = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<TenantInfo> pageData = tenantService.findTenantInfos(pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
        tenants.addAll(pageData.getData());

        for (int i=0;i<156;i++) {
            Tenant tenant = new Tenant();
            tenant.setTitle("Tenant"+i);
            tenants.add(new TenantInfo(tenantService.saveTenant(tenant), "Default"));
        }

        List<TenantInfo> loadedTenants = new ArrayList<>();
        pageLink = new PageLink(17);
        do {
            pageData = tenantService.findTenantInfos(pageLink);
            loadedTenants.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(tenants, idComparator);
        Collections.sort(loadedTenants, idComparator);

        Assert.assertEquals(tenants, loadedTenants);

        for (TenantInfo tenant : loadedTenants) {
            tenantService.deleteTenant(tenant.getId());
        }

        pageLink = new PageLink(17);
        pageData = tenantService.findTenantInfos(pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());

    }

    @Test(expected = DataValidationException.class)
    public void testSaveTenantWithIsolatedProfileInMonolithSetup() {
        TenantProfile tenantProfile = new TenantProfile();
        tenantProfile.setName("Isolated Tenant Profile");
        TenantProfileData profileData = new TenantProfileData();
        profileData.setConfiguration(new DefaultTenantProfileConfiguration());
        tenantProfile.setProfileData(profileData);
        tenantProfile.setDefault(false);
        tenantProfile.setIsolatedTbCore(true);
        tenantProfile.setIsolatedTbRuleEngine(true);
        TenantProfile isolatedTenantProfile = tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile);

        Tenant tenant = new Tenant();
        tenant.setTitle("Tenant");
        tenant.setTenantProfileId(isolatedTenantProfile.getId());
        tenantService.saveTenant(tenant);
    }

    @Test
    public void testGettingTenantAddItToCache() {
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = tenantService.saveTenant(tenant);

        Mockito.reset(tenantDao);
        Objects.requireNonNull(cacheManager.getCache(CacheConstants.TENANTS_CACHE), "Tenant cache manager is null").evict(savedTenant.getId());

        Mockito.verify(tenantDao, Mockito.times(0)).findById(any(), any());
        tenantService.findTenantById(savedTenant.getId());
        Mockito.verify(tenantDao, Mockito.times(1)).findById(eq(savedTenant.getId()), eq(savedTenant.getId().getId()));

        Cache.ValueWrapper cachedTenant =
                Objects.requireNonNull(cacheManager.getCache(CacheConstants.TENANTS_CACHE), "Cache manager is null!").get(savedTenant.getId());
        Assert.assertNotNull("Getting an existing Tenant doesn't add it to the cache!", cachedTenant);

        for (int i = 0; i < 100; i++) {
            tenantService.findTenantById(savedTenant.getId());
        }
        Mockito.verify(tenantDao, Mockito.times(1)).findById(eq(savedTenant.getId()), eq(savedTenant.getId().getId()));

        tenantService.deleteTenant(savedTenant.getId());
    }

    @Test
    public void testUpdatingExistingTenantEvictCache() {
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = tenantService.saveTenant(tenant);

        Cache.ValueWrapper cachedTenant =
                Objects.requireNonNull(cacheManager.getCache(CacheConstants.TENANTS_CACHE), "Cache manager is null!").get(savedTenant.getId());
        Assert.assertNotNull("Saving a Tenant doesn't add it to the cache!", cachedTenant);

        savedTenant.setTitle("My new tenant");
        savedTenant = tenantService.saveTenant(savedTenant);

        Mockito.reset(tenantDao);

        cachedTenant = Objects.requireNonNull(cacheManager.getCache(CacheConstants.TENANTS_CACHE), "Cache manager is null!").get(savedTenant.getId());
        Assert.assertNull("Updating a Tenant doesn't evict the cache!", cachedTenant);

        Mockito.verify(tenantDao, Mockito.times(0)).findById(any(), any());
        tenantService.findTenantById(savedTenant.getId());
        Mockito.verify(tenantDao, Mockito.times(1)).findById(eq(savedTenant.getId()), eq(savedTenant.getId().getId()));

        tenantService.deleteTenant(savedTenant.getId());
    }

    @Test
    public void testRemovingTenantEvictCache() {
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = tenantService.saveTenant(tenant);

        Cache.ValueWrapper cachedTenant =
                Objects.requireNonNull(cacheManager.getCache(CacheConstants.TENANTS_CACHE), "Cache manager is null!").get(savedTenant.getId());
        Assert.assertNotNull("Saving a Tenant doesn't add it to the cache!", cachedTenant);

        tenantService.deleteTenant(savedTenant.getId());
        cachedTenant = Objects.requireNonNull(cacheManager.getCache(CacheConstants.TENANTS_CACHE), "Cache manager is null!").get(savedTenant.getId());
        Assert.assertNull("Removing a Tenant doesn't evict the cache!", cachedTenant);
    }

    @Test
    public void testDeleteTenantDeletingAllRelatedEntities() throws Exception {
        TenantProfile savedProfile = createAndSaveTenantProfile();
        Tenant savedTenant = createAndSaveTenant(savedProfile);
        User savedUser = createAndSaveUserFor(savedTenant);
        Customer savedCustomer = createAndSaveCustomerFor(savedTenant);
        WidgetsBundle savedWidgetsBundle = createAndSaveWidgetBundleFor(savedTenant);
        DeviceProfile savedDeviceProfile = createAndSaveDeviceProfileWithProfileDataFor(savedTenant);
        Device savedDevice = createAndSaveDeviceFor(savedTenant, savedCustomer, savedDeviceProfile);
        EntityView savedEntityView = createAndSaveEntityViewFor(savedTenant, savedCustomer, savedDevice);
        Asset savedAsset = createAndSaveAssetFor(savedTenant, savedCustomer);
        Dashboard savedDashboard = createAndSaveDashboardFor(savedTenant, savedCustomer);
        RuleChain savedRuleChain = createAndSaveRuleChainFor(savedTenant);
        Edge savedEdge = createAndSaveEdgeFor(savedTenant);
        OtaPackage savedOtaPackage = createAndSaveOtaPackageFor(savedTenant, savedDeviceProfile);
        TbResource savedResource = createAndSaveResourceFor(savedTenant);
        Rpc savedRpc = createAndSaveRpcFor(savedTenant, savedDevice);

        tenantService.deleteTenant(savedTenant.getId());

        Assert.assertNull(tenantService.findTenantById(savedTenant.getId()));
        assertCustomerIsDeleted(savedTenant, savedCustomer);
        assertWidgetsBundleIsDeleted(savedTenant, savedWidgetsBundle);
        assertEntityViewIsDeleted(savedTenant, savedDevice, savedEntityView);
        assertAssetIsDeleted(savedTenant, savedAsset);
        assertDeviceIsDeleted(savedTenant, savedDevice);
        assertDeviceProfileIsDeleted(savedTenant, savedDeviceProfile);
        assertDashboardIsDeleted(savedTenant, savedDashboard);
        assertEdgeIsDeletd(savedTenant, savedEdge);
        assertTenantAdminIsDeleted(savedTenant);
        assertUserIsDeleted(savedTenant, savedUser);
        Assert.assertNull(ruleChainService.findRuleChainById(savedTenant.getId(), savedRuleChain.getId()));
        Assert.assertNull(apiUsageStateService.findTenantApiUsageState(savedTenant.getId()));
        assertResourceIsDeleted(savedTenant, savedResource);
        assertOtaPAckageIsDeleted(savedTenant, savedOtaPackage);
        Assert.assertNull(rpcService.findById(savedTenant.getId(), savedRpc.getId()));
    }

    private void assertOtaPAckageIsDeleted(Tenant savedTenant, OtaPackage savedOtaPackage) {
        Assert.assertNull(
                otaPackageService.findOtaPackageById(
                        savedTenant.getId(), savedOtaPackage.getId()
                )
        );
        PageLink pageLinkOta = new PageLink(1000);
        PageData<OtaPackageInfo> pageDataOta = otaPackageService.findTenantOtaPackagesByTenantId(savedTenant.getId(), pageLinkOta);
        Assert.assertFalse(pageDataOta.hasNext());
        Assert.assertEquals(0, pageDataOta.getTotalElements());
    }

    private void assertResourceIsDeleted(Tenant savedTenant, TbResource savedResource) {
        Assert.assertNull(resourceService.findResourceById(savedTenant.getId(), savedResource.getId()));
        PageLink pageLinkResources = new PageLink(1000);
        PageData<TbResourceInfo> tenantResources =
                resourceService.findAllTenantResourcesByTenantId(savedTenant.getId(), pageLinkResources);
        Assert.assertFalse(tenantResources.hasNext());
        Assert.assertEquals(0, tenantResources.getTotalElements());
    }

    private void assertUserIsDeleted(Tenant savedTenant, User savedUser) {
        Assert.assertNull(userService.findUserById(savedTenant.getId(), savedUser.getId()));
        PageLink pageLinkUsers = new PageLink(1000);
        PageData<User> users =
                userService.findUsersByTenantId(savedTenant.getId(), pageLinkUsers);
        Assert.assertFalse(users.hasNext());
        Assert.assertEquals(0, users.getTotalElements());
    }

    private void assertTenantAdminIsDeleted(Tenant savedTenant) {
        PageLink pageLinkTenantAdmins = new PageLink(1000);
        PageData<User> tenantAdmins =
                userService.findTenantAdmins(savedTenant.getId(), pageLinkTenantAdmins);
        Assert.assertFalse(tenantAdmins.hasNext());
        Assert.assertEquals(0, tenantAdmins.getTotalElements());
    }

    private void assertEdgeIsDeletd(Tenant savedTenant, Edge savedEdge) {
        Assert.assertNull(edgeService.findEdgeById(savedTenant.getId(), savedEdge.getId()));
        PageLink pageLinkEdges = new PageLink(1000);
        PageData<Edge> edges = edgeService.findEdgesByTenantId(savedTenant.getId(), pageLinkEdges);
        Assert.assertFalse(edges.hasNext());
        Assert.assertEquals(0, edges.getTotalElements());
    }

    private void assertDashboardIsDeleted(Tenant savedTenant, Dashboard savedDashboard) {
        Assert.assertNull(dashboardService.findDashboardById(
                savedTenant.getId(), savedDashboard.getId()
        ));
        PageLink pageLinkDashboards = new PageLink(1000);
        PageData<DashboardInfo> dashboards =
                dashboardService.findDashboardsByTenantId(savedTenant.getId(), pageLinkDashboards);
        Assert.assertFalse(dashboards.hasNext());
        Assert.assertEquals(0, dashboards.getTotalElements());
    }

    private void assertDeviceProfileIsDeleted(Tenant savedTenant, DeviceProfile savedDeviceProfile) {
        Assert.assertNull(deviceProfileService.findDeviceProfileById(
                savedTenant.getId(), savedDeviceProfile.getId()
        ));
        PageLink pageLinkDeviceProfiles = new PageLink(1000);
        PageData<DeviceProfile> profiles =
                deviceProfileService.findDeviceProfiles(savedTenant.getId(), pageLinkDeviceProfiles);
        Assert.assertFalse(profiles.hasNext());
        Assert.assertEquals(0, profiles.getTotalElements());
    }

    private void assertDeviceIsDeleted(Tenant savedTenant, Device savedDevice) {
        Assert.assertNull(deviceService.findDeviceById(
                savedTenant.getId(), savedDevice.getId()
        ));
        PageLink pageLinkDevices = new PageLink(1000);
        PageData<Device> devices =
                deviceService.findDevicesByTenantId(savedTenant.getId(), pageLinkDevices);
        Assert.assertFalse(devices.hasNext());
        Assert.assertEquals(0, devices.getTotalElements());
    }

    private void assertAssetIsDeleted(Tenant savedTenant, Asset savedAsset) {
        Assert.assertNull(assetService.findAssetById(
                savedTenant.getId(), savedAsset.getId()
        ));
        PageLink pageLinkAssets = new PageLink(1000);
        PageData<Asset> assets =
                assetService.findAssetsByTenantId(savedTenant.getId(), pageLinkAssets);
        Assert.assertFalse(assets.hasNext());
        Assert.assertEquals(0, assets.getTotalElements());
    }

    private void assertEntityViewIsDeleted(Tenant savedTenant, Device savedDevice, EntityView savedEntityView) {
        Assert.assertNull(entityViewService.findEntityViewById(
                savedTenant.getId(), savedEntityView.getId()
        ));
        List<EntityView> entityViews =
                entityViewService.findEntityViewsByTenantIdAndEntityId(
                        savedTenant.getId(), savedDevice.getId());
        Assert.assertTrue(entityViews.isEmpty());
    }

    private void assertWidgetsBundleIsDeleted(Tenant savedTenant, WidgetsBundle savedWidgetsBundle) {
        Assert.assertNull(
                widgetsBundleService.findWidgetsBundleById(savedTenant.getId(), savedWidgetsBundle.getId())
        );
        List<WidgetsBundle> widgetsBundlesByTenantId =
                widgetsBundleService.findAllTenantWidgetsBundlesByTenantId(savedTenant.getId());
        Assert.assertTrue(widgetsBundlesByTenantId.isEmpty());
    }

    private void assertCustomerIsDeleted(Tenant savedTenant, Customer savedCustomer) {
        Assert.assertNull(customerService.findCustomerById(savedTenant.getId(), savedCustomer.getId()));
        PageLink pageLinkCustomer = new PageLink(1000);
        PageData<Customer> pageDataCustomer = customerService
                .findCustomersByTenantId(savedTenant.getId(), pageLinkCustomer);
        Assert.assertFalse(pageDataCustomer.hasNext());
        Assert.assertEquals(0, pageDataCustomer.getTotalElements());
    }

    private Rpc createAndSaveRpcFor(Tenant savedTenant, Device savedDevice) {
        Rpc rpc = new Rpc();
        rpc.setTenantId(savedTenant.getId());
        rpc.setDeviceId(savedDevice.getId());
        rpc.setStatus(RpcStatus.QUEUED);
        rpc.setRequest(JacksonUtil.toJsonNode("{}"));
        return rpcService.save(rpc);
    }

    private TbResource createAndSaveResourceFor(Tenant savedTenant) {
        TbResource resource = new TbResource();
        resource.setTenantId(savedTenant.getId());
        resource.setTitle("Test resource");
        resource.setResourceType(ResourceType.LWM2M_MODEL);
        resource.setFileName("filename.txt");
        resource.setResourceKey("Test resource key");
        resource.setData("Some super test data");
        return resourceService.saveResource(resource);
    }

    private OtaPackage createAndSaveOtaPackageFor(Tenant savedTenant, DeviceProfile savedDeviceProfile) {
        OtaPackage otaPackage = createFirmware(savedTenant.getId(), savedDeviceProfile.getId());
        return otaPackageService.saveOtaPackage(otaPackage);
    }

    private Edge createAndSaveEdgeFor(Tenant savedTenant) {
        Edge edge = constructEdge(savedTenant.getId(), "Test edge", "Simple");
        return edgeService.saveEdge(edge, false);
    }

    private RuleChain createAndSaveRuleChainFor(Tenant savedTenant) {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setTenantId(savedTenant.getId());
        ruleChain.setName("Test rule chain");
        ruleChain.setType(RuleChainType.CORE);
        return ruleChainService.saveRuleChain(ruleChain);
    }

    private Dashboard createAndSaveDashboardFor(Tenant savedTenant, Customer savedCustomer) {
        Dashboard dashboard = new Dashboard();
        dashboard.setTenantId(savedTenant.getId());
        dashboard.setTitle("Test dashboard");
        dashboard.setAssignedCustomers(Set.of(savedCustomer.toShortCustomerInfo()));
        return dashboardService.saveDashboard(dashboard);
    }

    private Asset createAndSaveAssetFor(Tenant savedTenant, Customer savedCustomer) {
        Asset asset = new Asset();
        asset.setTenantId(savedTenant.getId());
        asset.setCustomerId(savedCustomer.getId());
        asset.setType("Test asset type");
        asset.setName("Test asset type");
        asset.setLabel("Test asset type");
        return assetService.saveAsset(asset);
    }

    private EntityView createAndSaveEntityViewFor(Tenant savedTenant, Customer savedCustomer, Device savedDevice) {
        EntityView entityView = new EntityView();
        entityView.setEntityId(savedDevice.getId());
        entityView.setTenantId(savedTenant.getId());
        entityView.setCustomerId(savedCustomer.getId());
        entityView.setType("Test type");
        entityView.setName("Test entity view");
        entityView.setStartTimeMs(0);
        entityView.setEndTimeMs(840000);
        return entityViewService.saveEntityView(entityView);
    }

    private Device createAndSaveDeviceFor(Tenant savedTenant, Customer savedCustomer, DeviceProfile savedDeviceProfile) {
        Device device = new Device();
        device.setCustomerId(savedCustomer.getId());
        device.setTenantId(savedTenant.getId());
        device.setType("Test type");
        device.setName("TestType");
        device.setLabel("Test type");
        device.setDeviceProfileId(savedDeviceProfile.getId());
        return deviceService.saveDevice(device);
    }

    private DeviceProfile createAndSaveDeviceProfileWithProfileDataFor(Tenant savedTenant) {
        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setTenantId(savedTenant.getId());
        deviceProfile.setTransportType(DeviceTransportType.MQTT);
        deviceProfile.setName("Test device profile");
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        DeviceProfileData profileData = new DeviceProfileData();
        profileData.setTransportConfiguration(new MqttDeviceProfileTransportConfiguration());
        deviceProfile.setProfileData(profileData);
        return deviceProfileService.saveDeviceProfile(deviceProfile);
    }

    private WidgetsBundle createAndSaveWidgetBundleFor(Tenant savedTenant) {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTenantId(savedTenant.getId());
        widgetsBundle.setTitle("Test widgets bundle");
        widgetsBundle.setAlias("TestWidgetsBundle");
        widgetsBundle.setDescription("Just a simple widgets bundle");
        return widgetsBundleService.saveWidgetsBundle(widgetsBundle);
    }

    private Customer createAndSaveCustomerFor(Tenant savedTenant) {
        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer.setTenantId(savedTenant.getId());
        customer.setEmail("testCustomer@test.com");
        return customerService.saveCustomer(customer);
    }

    private User createAndSaveUserFor(Tenant savedTenant) {
        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setEmail("tenantAdmin@test.com");
        user.setFirstName("tenantAdmin");
        user.setLastName("tenantAdmin");
        user.setTenantId(savedTenant.getId());
        return userService.saveUser(user);
    }

    private Tenant createAndSaveTenant(TenantProfile savedProfile) {
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        tenant.setTenantProfileId(savedProfile.getId());
        return tenantService.saveTenant(tenant);
    }

    private TenantProfile createAndSaveTenantProfile() {
        TenantProfile tenantProfile = new TenantProfile();
        tenantProfile.setName("Test tenant profile");
        return tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile);
    }

    private OtaPackage createFirmware(TenantId tenantId, DeviceProfileId deviceProfileId) {
        OtaPackage firmware = new OtaPackage();
        firmware.setTenantId(tenantId);
        firmware.setDeviceProfileId(deviceProfileId);
        firmware.setType(FIRMWARE);
        firmware.setTitle("My firmware");
        firmware.setVersion("1");
        firmware.setFileName("filename.txt");
        firmware.setContentType("text/plain");
        firmware.setChecksumAlgorithm(ChecksumAlgorithm.SHA256);
        firmware.setChecksum("4bf5122f344554c53bde2ebb8cd2b7e3d1600ad631c385a5d7cce23c7785459a");
        firmware.setData(ByteBuffer.wrap(new byte[]{(int) 1L}));
        firmware.setDataSize(1L);
        return otaPackageService.saveOtaPackage(firmware);
    }
}
