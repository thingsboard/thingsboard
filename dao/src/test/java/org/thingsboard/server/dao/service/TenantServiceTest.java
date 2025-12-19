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
package org.thingsboard.server.dao.service;

import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cache.TbTransactionalCache;
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
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.TbResourceInfoFilter;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantInfo;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.rpc.RpcService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.tenant.TenantDao;
import org.thingsboard.server.dao.tenant.TenantProfileService;
import org.thingsboard.server.dao.usagerecord.ApiUsageStateService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@DaoSqlTest
public class TenantServiceTest extends AbstractServiceTest {

    @SpyBean
    TenantDao tenantDao;

    @Autowired
    ApiUsageStateService apiUsageStateService;
    @Autowired
    AssetService assetService;
    @Autowired
    CustomerService customerService;
    @Autowired
    DashboardService dashboardService;
    @Autowired
    DeviceProfileService deviceProfileService;
    @Autowired
    DeviceService deviceService;
    @Autowired
    EdgeService edgeService;
    @Autowired
    EntityViewService entityViewService;
    @Autowired
    OtaPackageService otaPackageService;
    @Autowired
    ResourceService resourceService;
    @Autowired
    RpcService rpcService;
    @Autowired
    RuleChainService ruleChainService;
    @Autowired
    TbTransactionalCache<TenantId, Boolean> existsTenantCache;
    @Autowired
    TbTransactionalCache<TenantId, Tenant> cache;
    @Autowired
    TenantProfileService tenantProfileService;
    @Autowired
    UserService userService;
    @Autowired
    WidgetsBundleService widgetsBundleService;

    private final IdComparator<Tenant> idComparator = new IdComparator<>();

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

    @Test
    public void testSaveTenantWithEmptyTitle() {
        Tenant tenant = new Tenant();
        Assertions.assertThrows(DataValidationException.class, () -> {
            tenantService.saveTenant(tenant);
        });
    }

    @Test
    public void testSaveTenantWithInvalidEmail() {
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        tenant.setEmail("invalid@mail");
        Assertions.assertThrows(DataValidationException.class, () -> {
            tenantService.saveTenant(tenant);
        });
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
        tenantService.deleteTenants();
        List<Tenant> tenants = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<Tenant> pageData = tenantService.findTenants(pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
        tenants.addAll(pageData.getData());

        for (int i = 0; i < 156; i++) {
            Tenant tenant = new Tenant();
            tenant.setTitle("Tenant" + i);
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
        for (int i = 0; i < 134; i++) {
            Tenant tenant = new Tenant();
            String suffix = StringUtils.randomAlphanumeric((int) (Math.random() * 15));
            String title = title1 + suffix;
            title = i % 2 == 0 ? title.toLowerCase() : title.toUpperCase();
            tenant.setTitle(title);
            tenantsTitle1.add(tenantService.saveTenant(tenant));
        }
        String title2 = "Tenant title 2";
        List<Tenant> tenantsTitle2 = new ArrayList<>();
        for (int i = 0; i < 127; i++) {
            Tenant tenant = new Tenant();
            String suffix = StringUtils.randomAlphanumeric((int) (Math.random() * 15));
            String title = title2 + suffix;
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
        tenantService.deleteTenants();
        List<TenantInfo> tenants = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<TenantInfo> pageData = tenantService.findTenantInfos(pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
        tenants.addAll(pageData.getData());

        for (int i = 0; i < 156; i++) {
            Tenant tenant = new Tenant();
            tenant.setTitle("Tenant" + i);
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

    @Test
    public void testGettingTenantAddingItToCache() {
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = tenantService.saveTenant(tenant);

        Mockito.reset(tenantDao);

        verify(tenantDao, Mockito.times(0)).findById(any(), any());
        tenantService.findTenantById(savedTenant.getId());
        verify(tenantDao, Mockito.times(1)).findById(eq(savedTenant.getId()), eq(savedTenant.getId().getId()));

        var cachedTenant = cache.get(savedTenant.getId());
        Assert.assertNotNull("Getting an existing Tenant doesn't add it to the cache!", cachedTenant);
        Assert.assertEquals(savedTenant, cachedTenant.get());

        for (int i = 0; i < 100; i++) {
            tenantService.findTenantById(savedTenant.getId());
        }
        verify(tenantDao, Mockito.times(1)).findById(eq(savedTenant.getId()), eq(savedTenant.getId().getId()));

        tenantService.deleteTenant(savedTenant.getId());
    }

    @Test
    public void testExistsTenantAddingResultToCache() {
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = tenantService.saveTenant(tenant);

        Mockito.reset(tenantDao);
        //fromIdExists invoked from device profile validator
        existsTenantCache.evict(savedTenant.getTenantId());

        verify(tenantDao, Mockito.times(0)).existsById(any(), any());
        tenantService.tenantExists(savedTenant.getId());
        verify(tenantDao, Mockito.times(1)).existsById(eq(savedTenant.getId()), eq(savedTenant.getId().getId()));

        var isExists = existsTenantCache.get(savedTenant.getId());
        Assert.assertNotNull("Getting an existing Tenant doesn't add it to the cache!", isExists);

        for (int i = 0; i < 100; i++) {
            tenantService.tenantExists(savedTenant.getId());
        }
        verify(tenantDao, Mockito.times(1)).existsById(eq(savedTenant.getId()), eq(savedTenant.getId().getId()));

        tenantService.deleteTenant(savedTenant.getId());
    }

    @Test
    public void testUpdatingExistingTenantEvictCache() {
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = tenantService.saveTenant(tenant);

        tenantService.findTenantById(savedTenant.getId());

        var cachedTenant = cache.get(savedTenant.getId());
        Assert.assertNotNull("Saving a Tenant doesn't add it to the cache!", cachedTenant);
        Assert.assertEquals(savedTenant, cachedTenant.get());

        savedTenant.setTitle("My new tenant");
        savedTenant = tenantService.saveTenant(savedTenant);

        Mockito.reset(tenantDao);

        cachedTenant = cache.get(savedTenant.getId());
        Assert.assertNull("Updating a Tenant doesn't evict the cache!", cachedTenant);

        verify(tenantDao, Mockito.times(0)).findById(any(), any());
        tenantService.findTenantById(savedTenant.getId());
        verify(tenantDao, Mockito.times(1)).findById(eq(savedTenant.getId()), eq(savedTenant.getId().getId()));

        tenantService.deleteTenant(savedTenant.getId());
    }

    @Test
    public void testRemovingTenantEvictCache() {
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = tenantService.saveTenant(tenant);

        tenantService.findTenantById(savedTenant.getId());
        tenantService.tenantExists(savedTenant.getId());

        var cachedTenant =
                cache.get(savedTenant.getId());
        var cachedExists =
                existsTenantCache.get(savedTenant.getId());
        Assert.assertNotNull("Saving a Tenant doesn't add it to the cache!", cachedTenant);
        Assert.assertNotNull("Saving a Tenant doesn't add it to the cache!", cachedExists);

        tenantService.deleteTenant(savedTenant.getId());
        cachedTenant =
                cache.get(savedTenant.getId());
        cachedExists =
                existsTenantCache.get(savedTenant.getId());


        Assert.assertNull("Removing a Tenant doesn't evict the cache!", cachedTenant);
        Assert.assertNull("Removing a Tenant doesn't evict the cache!", cachedExists);
    }

    @Test
    public void testDeleteTenantDeletingAllRelatedEntities() throws Exception {
        TenantProfile profile = createAndSaveTenantProfile();
        Tenant tenant = createAndSaveTenant(profile);
        User user = createAndSaveUserFor(tenant);
        Customer customer = createAndSaveCustomerFor(tenant);
        WidgetsBundle widgetsBundle = createAndSaveWidgetBundleFor(tenant);
        DeviceProfile deviceProfile = createAndSaveDeviceProfileWithProfileDataFor(tenant);
        Device device = createAndSaveDeviceFor(tenant, customer, deviceProfile);
        EntityView entityView = createAndSaveEntityViewFor(tenant, customer, device);
        Asset asset = createAndSaveAssetFor(tenant, customer);
        Dashboard dashboard = createAndSaveDashboardFor(tenant, customer);
        RuleChain ruleChain = createAndSaveRuleChainFor(tenant);
        Edge edge = createAndSaveEdgeFor(tenant);
        OtaPackage otaPackage = createAndSaveOtaPackageFor(tenant, deviceProfile);
        TbResource resource = createAndSaveResourceFor(tenant);
        Rpc rpc = createAndSaveRpcFor(tenant, device);

        tenantService.deleteTenant(tenant.getId());

        Assert.assertNull(tenantService.findTenantById(tenant.getId()));
        assertCustomerIsDeleted(tenant, customer);
        assertWidgetsBundleIsDeleted(tenant, widgetsBundle);
        assertEntityViewIsDeleted(tenant, device, entityView);
        assertAssetIsDeleted(tenant, asset);
        assertDeviceIsDeleted(tenant, device);
        assertDeviceProfileIsDeleted(tenant, deviceProfile);
        assertDashboardIsDeleted(tenant, dashboard);
        assertEdgeIsDeleted(tenant, edge);
        assertTenantAdminIsDeleted(tenant);
        assertUserIsDeleted(tenant, user);
        Assert.assertNull(ruleChainService.findRuleChainById(tenant.getId(), ruleChain.getId()));
        Assert.assertNull(apiUsageStateService.findTenantApiUsageState(tenant.getId()));
        assertResourceIsDeleted(tenant, resource);
        assertOtaPackageIsDeleted(tenant, otaPackage);
        Assert.assertNull(rpcService.findById(tenant.getId(), rpc.getId()));

        tenantProfileService.deleteTenantProfile(TenantId.SYS_TENANT_ID, profile.getId());
    }

    private void assertOtaPackageIsDeleted(Tenant tenant, OtaPackage otaPackage) {
        assertThat(otaPackageService.findOtaPackageById(tenant.getId(), otaPackage.getId()))
                .as("otaPackage").isNull();
        PageLink pageLinkOta = new PageLink(1);
        PageData<OtaPackageInfo> pageDataOta = otaPackageService.findTenantOtaPackagesByTenantId(tenant.getId(), pageLinkOta);
        Assert.assertEquals(0, pageDataOta.getTotalElements());
    }

    private void assertResourceIsDeleted(Tenant tenant, TbResource resource) {
        assertThat(resourceService.findResourceById(tenant.getId(), resource.getId()))
                .as("resource").isNull();
        PageLink pageLinkResources = new PageLink(1);
        TbResourceInfoFilter filter = TbResourceInfoFilter.builder()
                .tenantId(tenantId)
                .build();
        PageData<TbResourceInfo> tenantResources =
                resourceService.findAllTenantResourcesByTenantId(filter, pageLinkResources);
        Assert.assertEquals(0, tenantResources.getTotalElements());
    }

    private void assertUserIsDeleted(Tenant tenant, User user) {
        assertThat(userService.findUserById(tenant.getId(), user.getId()))
                .as("user").isNull();
        PageLink pageLinkUsers = new PageLink(1);
        PageData<User> users =
                userService.findUsersByTenantId(tenant.getId(), pageLinkUsers);
        Assert.assertEquals(0, users.getTotalElements());
    }

    private void assertTenantAdminIsDeleted(Tenant savedTenant) {
        PageLink pageLinkTenantAdmins = new PageLink(1);
        PageData<User> tenantAdmins =
                userService.findTenantAdmins(savedTenant.getId(), pageLinkTenantAdmins);
        Assert.assertEquals(0, tenantAdmins.getTotalElements());
    }

    private void assertEdgeIsDeleted(Tenant tenant, Edge edge) {
        assertThat(edgeService.findEdgeById(tenant.getId(), edge.getId()))
                .as("edge").isNull();
        PageLink pageLinkEdges = new PageLink(1);
        PageData<Edge> edges = edgeService.findEdgesByTenantId(tenant.getId(), pageLinkEdges);
        Assert.assertEquals(0, edges.getTotalElements());
    }

    private void assertDashboardIsDeleted(Tenant tenant, Dashboard dashboard) {
        assertThat(dashboardService.findDashboardById(tenant.getId(), dashboard.getId()))
                .as("dashboard").isNull();
        PageLink pageLinkDashboards = new PageLink(1);
        PageData<DashboardInfo> dashboards =
                dashboardService.findDashboardsByTenantId(tenant.getId(), pageLinkDashboards);
        Assert.assertEquals(0, dashboards.getTotalElements());
    }

    private void assertDeviceProfileIsDeleted(Tenant tenant, DeviceProfile deviceProfile) {
        assertThat(deviceProfileService.findDeviceProfileById(tenant.getId(), deviceProfile.getId()))
                .as("deviceProfile").isNull();
        PageLink pageLinkDeviceProfiles = new PageLink(1);
        PageData<DeviceProfile> profiles =
                deviceProfileService.findDeviceProfiles(tenant.getId(), pageLinkDeviceProfiles);
        Assert.assertEquals(0, profiles.getTotalElements());
    }

    private void assertDeviceIsDeleted(Tenant tenant, Device device) {
        assertThat(deviceService.findDeviceById(tenant.getId(), device.getId()))
                .as("device").isNull();
        PageLink pageLinkDevices = new PageLink(1);
        PageData<Device> devices =
                deviceService.findDevicesByTenantId(tenant.getId(), pageLinkDevices);
        Assert.assertEquals(0, devices.getTotalElements());
    }

    private void assertAssetIsDeleted(Tenant tenant, Asset asset) {
        assertThat(assetService.findAssetById(tenant.getId(), asset.getId()))
                .as("asset").isNull();
        PageLink pageLinkAssets = new PageLink(1);
        PageData<Asset> assets =
                assetService.findAssetsByTenantId(tenant.getId(), pageLinkAssets);
        Assert.assertEquals(0, assets.getTotalElements());
    }

    private void assertEntityViewIsDeleted(Tenant tenant, Device device, EntityView entityView) {
        assertThat(entityViewService.findEntityViewById(tenant.getId(), entityView.getId()))
                .as("entityView").isNull();
        List<EntityView> entityViews =
                entityViewService.findEntityViewsByTenantIdAndEntityId(tenant.getId(), device.getId());
        Assert.assertTrue(entityViews.isEmpty());
    }

    private void assertWidgetsBundleIsDeleted(Tenant tenant, WidgetsBundle widgetsBundle) {
        assertThat(widgetsBundleService.findWidgetsBundleById(tenant.getId(), widgetsBundle.getId()))
                .as("widgetBundle").isNull();
        List<WidgetsBundle> widgetsBundlesByTenantId =
                widgetsBundleService.findAllTenantWidgetsBundlesByTenantId(tenant.getId());
        Assert.assertTrue(widgetsBundlesByTenantId.isEmpty());
    }

    private void assertCustomerIsDeleted(Tenant tenant, Customer customer) {
        assertThat(customerService.findCustomerById(tenant.getId(), customer.getId()))
                .as("customer").isNull();
        PageLink pageLinkCustomer = new PageLink(1);
        PageData<Customer> pageDataCustomer = customerService
                .findCustomersByTenantId(tenant.getId(), pageLinkCustomer);
        Assert.assertEquals(0, pageDataCustomer.getTotalElements());
    }

    private Rpc createAndSaveRpcFor(Tenant tenant, Device device) {
        Rpc rpc = new Rpc();
        rpc.setTenantId(tenant.getId());
        rpc.setDeviceId(device.getId());
        rpc.setStatus(RpcStatus.QUEUED);
        rpc.setRequest(JacksonUtil.toJsonNode("{}"));
        return rpcService.save(rpc);
    }

    private TbResource createAndSaveResourceFor(Tenant tenant) {
        TbResource resource = new TbResource();
        resource.setTenantId(tenant.getId());
        resource.setTitle("Test resource");
        resource.setResourceType(ResourceType.LWM2M_MODEL);
        resource.setFileName("filename.txt");
        resource.setResourceKey("Test resource key");
        resource.setData("Some super test data".getBytes(StandardCharsets.UTF_8));
        return resourceService.saveResource(resource);
    }

    private OtaPackage createAndSaveOtaPackageFor(Tenant tenant, DeviceProfile deviceProfile) {
        return otaPackageService.saveOtaPackage(
                OtaPackageServiceTest.createFirmware(
                        tenant.getId(), "2", deviceProfile.getId())
        );
    }

    private Edge createAndSaveEdgeFor(Tenant tenant) {
        Edge edge = constructEdge(tenant.getId(), "Test edge", "Simple");
        return edgeService.saveEdge(edge);
    }

    private RuleChain createAndSaveRuleChainFor(Tenant tenant) {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setTenantId(tenant.getId());
        ruleChain.setName("Test rule chain");
        ruleChain.setType(RuleChainType.CORE);
        return ruleChainService.saveRuleChain(ruleChain);
    }

    private Dashboard createAndSaveDashboardFor(Tenant tenant, Customer customer) {
        Dashboard dashboard = new Dashboard();
        dashboard.setTenantId(tenant.getId());
        dashboard.setTitle("Test dashboard");
        dashboard.setAssignedCustomers(Set.of(customer.toShortCustomerInfo()));
        return dashboardService.saveDashboard(dashboard);
    }

    private Asset createAndSaveAssetFor(Tenant tenant, Customer customer) {
        Asset asset = new Asset();
        asset.setTenantId(tenant.getId());
        asset.setCustomerId(customer.getId());
        asset.setType("Test asset type");
        asset.setName("Test asset type");
        asset.setLabel("Test asset type");
        return assetService.saveAsset(asset);
    }

    private EntityView createAndSaveEntityViewFor(Tenant tenant, Customer customer, Device device) {
        EntityView entityView = new EntityView();
        entityView.setEntityId(device.getId());
        entityView.setTenantId(tenant.getId());
        entityView.setCustomerId(customer.getId());
        entityView.setType("Test type");
        entityView.setName("Test entity view");
        entityView.setStartTimeMs(0);
        entityView.setEndTimeMs(840000);
        return entityViewService.saveEntityView(entityView);
    }

    private Device createAndSaveDeviceFor(Tenant tenant, Customer customer, DeviceProfile deviceProfile) {
        Device device = new Device();
        device.setCustomerId(customer.getId());
        device.setTenantId(tenant.getId());
        device.setType("Test type");
        device.setName("TestType");
        device.setLabel("Test type");
        device.setDeviceProfileId(deviceProfile.getId());
        return deviceService.saveDevice(device);
    }

    private DeviceProfile createAndSaveDeviceProfileWithProfileDataFor(Tenant tenant) {
        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setTenantId(tenant.getId());
        deviceProfile.setTransportType(DeviceTransportType.MQTT);
        deviceProfile.setName("Test device profile");
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        DeviceProfileData profileData = new DeviceProfileData();
        profileData.setTransportConfiguration(new MqttDeviceProfileTransportConfiguration());
        deviceProfile.setProfileData(profileData);
        return deviceProfileService.saveDeviceProfile(deviceProfile);
    }

    private WidgetsBundle createAndSaveWidgetBundleFor(Tenant tenant) {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTenantId(tenant.getId());
        widgetsBundle.setTitle("Test widgets bundle");
        widgetsBundle.setAlias("TestWidgetsBundle");
        widgetsBundle.setDescription("Just a simple widgets bundle");
        return widgetsBundleService.saveWidgetsBundle(widgetsBundle);
    }

    private Customer createAndSaveCustomerFor(Tenant tenant) {
        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer.setTenantId(tenant.getId());
        customer.setEmail("testCustomer@test.com");
        return customerService.saveCustomer(customer);
    }

    private User createAndSaveUserFor(Tenant tenant) {
        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setEmail("tenantAdmin@test.com");
        user.setFirstName("tenantAdmin");
        user.setLastName("tenantAdmin");
        user.setTenantId(tenant.getId());
        return userService.saveUser(TenantId.SYS_TENANT_ID, user);
    }

    private Tenant createAndSaveTenant(TenantProfile tenantProfile) {
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        tenant.setTenantProfileId(tenantProfile.getId());
        return tenantService.saveTenant(tenant);
    }

    private TenantProfile createAndSaveTenantProfile() {
        TenantProfile tenantProfile = new TenantProfile();
        tenantProfile.setName("Test tenant profile");
        return tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile);
    }
}
