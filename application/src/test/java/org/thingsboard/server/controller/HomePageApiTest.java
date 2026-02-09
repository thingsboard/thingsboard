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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.rule.engine.api.SmsService;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.FeaturesInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.UsageInfo;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.domain.Domain;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.OAuth2Client;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.ApiUsageStateFilter;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityTypeFilter;
import org.thingsboard.server.common.data.query.TsValue;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.stats.TbApiUsageStateClient;
import org.thingsboard.server.dao.domain.DomainService;
import org.thingsboard.server.dao.oauth2.OAuth2ClientService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityCountCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityCountUpdate;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityDataUpdate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@DaoSqlTest
public class HomePageApiTest extends AbstractControllerTest {

    @Autowired
    private TbApiUsageStateClient apiUsageStateClient;

    @Autowired
    private TbTenantProfileCache tenantProfileCache;

    @Autowired
    private AdminSettingsService adminSettingsService;

    @Autowired
    private DomainService domainService;

    @Autowired
    private OAuth2ClientService oAuth2ClientService;

    @MockBean
    private MailService mailService;

    @MockBean
    private SmsService smsService;

    private static final int DEFAULT_DASHBOARDS_COUNT = 0;

    //For system administrator
    @Test
    public void testTenantsCountWsCmd() throws Exception {
        Long initialCount = getInitialEntityCount(EntityType.TENANT);

        List<Tenant> tenants = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Tenant tenant = new Tenant();
            tenant.setTitle("tenant" + i);
            tenants.add(saveTenant(tenant));
        }

        EntityTypeFilter ef = new EntityTypeFilter();
        ef.setEntityType(EntityType.TENANT);
        EntityCountCmd cmd = new EntityCountCmd(1, new EntityCountQuery(ef, Collections.emptyList()));
        getWsClient().send(cmd);
        EntityCountUpdate update = getWsClient().parseCountReply(getWsClient().waitForReply());
        Assert.assertEquals(1, update.getCmdId());
        Assert.assertEquals(initialCount + 100, update.getCount());

        for (Tenant tenant : tenants) {
            deleteTenant(tenant.getId());
        }
    }

    @Test
    public void testTenantProfilesCountWsCmd() throws Exception {
        Long initialCount = getInitialEntityCount(EntityType.TENANT_PROFILE);

        List<TenantProfile> tenantProfiles = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            TenantProfile tenantProfile = new TenantProfile();
            tenantProfile.setName("tenantProfile" + i);
            tenantProfiles.add(doPost("/api/tenantProfile", tenantProfile, TenantProfile.class));
        }

        EntityTypeFilter ef = new EntityTypeFilter();
        ef.setEntityType(EntityType.TENANT_PROFILE);
        EntityCountCmd cmd = new EntityCountCmd(1, new EntityCountQuery(ef, Collections.emptyList()));
        getWsClient().send(cmd);
        EntityCountUpdate update = getWsClient().parseCountReply(getWsClient().waitForReply());
        Assert.assertEquals(1, update.getCmdId());
        Assert.assertEquals(initialCount + 100, update.getCount());

        for (TenantProfile tenantProfile : tenantProfiles) {
            doDelete("/api/tenantProfile/" + tenantProfile.getId().toString());
        }
    }

    @Test
    public void testUsersCountWsCmd() throws Exception {
        Long initialCount = getInitialEntityCount(EntityType.USER);

        List<User> users = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            User user = new User();
            user.setEmail(i + "user@thingsboard.org");
            user.setTenantId(tenantId);
            user.setAuthority(Authority.TENANT_ADMIN);
            users.add(doPost("/api/user", user, User.class));
        }

        EntityTypeFilter ef = new EntityTypeFilter();
        ef.setEntityType(EntityType.USER);
        EntityCountCmd cmd = new EntityCountCmd(1, new EntityCountQuery(ef, Collections.emptyList()));
        getWsClient().send(cmd);
        EntityCountUpdate update = getWsClient().parseCountReply(getWsClient().waitForReply());
        Assert.assertEquals(1, update.getCmdId());
        Assert.assertEquals(initialCount + 100, update.getCount());

        for (User user : users) {
            doDelete("/api/user/" + user.getId().toString());
        }
    }

    @Test
    public void testCustomersCountWsCmd() throws Exception {
        Long initialCount = getInitialEntityCount(EntityType.CUSTOMER);
        loginTenantAdmin();

        List<Customer> customers = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Customer customer = new Customer();
            customer.setTitle("customer" + i);
            customers.add(doPost("/api/customer", customer, Customer.class));
        }

        loginSysAdmin();
        EntityTypeFilter ef = new EntityTypeFilter();
        ef.setEntityType(EntityType.CUSTOMER);
        EntityCountCmd cmd = new EntityCountCmd(1, new EntityCountQuery(ef, Collections.emptyList()));
        getWsClient().send(cmd);
        EntityCountUpdate update = getWsClient().parseCountReply(getWsClient().waitForReply());
        Assert.assertEquals(1, update.getCmdId());
        Assert.assertEquals(initialCount + 100, update.getCount());

        loginTenantAdmin();
        for (Customer customer : customers) {
            doDelete("/api/customer/" + customer.getId().toString());
        }
    }

    @Test
    public void testDevicesCountWsCmd() throws Exception {
        Long initialCount = getInitialEntityCount(EntityType.DEVICE);
        loginTenantAdmin();

        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Device device = new Device();
            device.setName("device" + i);
            devices.add(doPost("/api/device", device, Device.class));
        }

        loginSysAdmin();
        EntityTypeFilter ef = new EntityTypeFilter();
        ef.setEntityType(EntityType.DEVICE);
        EntityCountCmd cmd = new EntityCountCmd(1, new EntityCountQuery(ef, Collections.emptyList()));
        getWsClient().send(cmd);
        EntityCountUpdate update = getWsClient().parseCountReply(getWsClient().waitForReply());
        Assert.assertEquals(1, update.getCmdId());
        Assert.assertEquals(initialCount + 100, update.getCount());

        loginTenantAdmin();
        for (Device device : devices) {
            doDelete("/api/device/" + device.getId().toString());
        }
    }

    @Test
    public void testAssetsCountWsCmd() throws Exception {
        Long initialCount = getInitialEntityCount(EntityType.ASSET);
        loginTenantAdmin();

        List<Asset> assets = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Asset asset = new Asset();
            asset.setName("asset" + i);
            assets.add(doPost("/api/asset", asset, Asset.class));
        }

        loginSysAdmin();
        EntityTypeFilter ef = new EntityTypeFilter();
        ef.setEntityType(EntityType.ASSET);
        EntityCountCmd cmd = new EntityCountCmd(1, new EntityCountQuery(ef, Collections.emptyList()));
        getWsClient().send(cmd);
        EntityCountUpdate update = getWsClient().parseCountReply(getWsClient().waitForReply());
        Assert.assertEquals(1, update.getCmdId());
        Assert.assertEquals(initialCount + 100, update.getCount());

        loginTenantAdmin();
        for (Asset asset : assets) {
            doDelete("/api/asset/" + asset.getId().toString());
        }
    }

    @Test
    public void testSystemInfoTimeSeriesWsCmd() throws Exception {
        ApiUsageState apiUsageState = apiUsageStateClient.getApiUsageState(TenantId.SYS_TENANT_ID);
        Assert.assertNotNull(apiUsageState);

        loginSysAdmin();
        long now = System.currentTimeMillis();

        EntityDataUpdate update = getWsClient().sendEntityDataQuery(new ApiUsageStateFilter());

        Assert.assertEquals(1, update.getCmdId());
        PageData<EntityData> pageData = update.getData();
        Assert.assertNotNull(pageData);
        Assert.assertEquals(1, pageData.getData().size());
        Assert.assertEquals(apiUsageState.getId(), pageData.getData().get(0).getEntityId());

        List<String> metrics = List.of("cpuUsage", "memoryUsage", "discUsage", "cpuCount", "totalMemory", "totalDiscSpace");
        update = getWsClient().subscribeTsUpdate(
                metrics,
                now, TimeUnit.HOURS.toMillis(1));
        Assert.assertEquals(1, update.getCmdId());
        List<EntityData> listData = update.getUpdate();
        Assert.assertNotNull(listData);
        Assert.assertEquals(1, listData.size());
        Assert.assertEquals(apiUsageState.getId(), listData.get(0).getEntityId());
        Assert.assertEquals(metrics.size(), listData.get(0).getTimeseries().size());

        for (TsValue[] tsv : listData.get(0).getTimeseries().values()) {
            Assert.assertTrue(tsv.length > 0);
        }
    }

    @Test
    public void testGetFeaturesInfo() throws Exception {
        String mail = "test@thingsboard.org";
        Mockito.doAnswer(invocation -> {
            AdminSettings mailSettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "mail");
            JsonNode mailFrom = mailSettings.getJsonValue().get("mailFrom");
            if (!mailFrom.asText().equals(mail)) {
                throw new Exception();
            }
            return null;
        }).when(mailService).testConnection(TenantId.SYS_TENANT_ID);

        Mockito.when(smsService.isConfigured(TenantId.SYS_TENANT_ID))
                .then(a -> adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "sms") != null);

        loginSysAdmin();

        FeaturesInfo featuresInfo = doGet("/api/admin/featuresInfo", FeaturesInfo.class);
        Assert.assertNotNull(featuresInfo);
        Assert.assertFalse(featuresInfo.isEmailEnabled());
        Assert.assertFalse(featuresInfo.isSmsEnabled());
        Assert.assertFalse(featuresInfo.isTwoFaEnabled());
        Assert.assertFalse(featuresInfo.isNotificationEnabled());
        Assert.assertFalse(featuresInfo.isOauthEnabled());

        AdminSettings mailSettings = doGet("/api/admin/settings/mail", AdminSettings.class);

        JsonNode jsonValue = mailSettings.getJsonValue();
        ((ObjectNode) jsonValue).put("mailFrom", mail);
        mailSettings.setJsonValue(jsonValue);

        doPost("/api/admin/settings", mailSettings).andExpect(status().isOk());

        featuresInfo = doGet("/api/admin/featuresInfo", FeaturesInfo.class);
        Assert.assertTrue(featuresInfo.isEmailEnabled());
        Assert.assertFalse(featuresInfo.isSmsEnabled());
        Assert.assertFalse(featuresInfo.isTwoFaEnabled());
        Assert.assertFalse(featuresInfo.isNotificationEnabled());
        Assert.assertFalse(featuresInfo.isOauthEnabled());

        AdminSettings smsSettings = new AdminSettings();
        smsSettings.setKey("sms");
        smsSettings.setJsonValue(JacksonUtil.newObjectNode());
        doPost("/api/admin/settings", smsSettings).andExpect(status().isOk());

        featuresInfo = doGet("/api/admin/featuresInfo", FeaturesInfo.class);
        Assert.assertTrue(featuresInfo.isEmailEnabled());
        Assert.assertTrue(featuresInfo.isSmsEnabled());
        Assert.assertFalse(featuresInfo.isTwoFaEnabled());
        Assert.assertFalse(featuresInfo.isNotificationEnabled());
        Assert.assertFalse(featuresInfo.isOauthEnabled());

        AdminSettings twoFaSettingsSettings = new AdminSettings();
        twoFaSettingsSettings.setKey("twoFaSettings");

        var twoFaSettings = JacksonUtil.newObjectNode();
        var providers = JacksonUtil.newArrayNode();
        providers.add(JacksonUtil.newObjectNode());
        twoFaSettings.set("providers", providers);
        twoFaSettingsSettings.setJsonValue(twoFaSettings);
        doPost("/api/admin/settings", twoFaSettingsSettings).andExpect(status().isOk());

        featuresInfo = doGet("/api/admin/featuresInfo", FeaturesInfo.class);
        Assert.assertTrue(featuresInfo.isEmailEnabled());
        Assert.assertTrue(featuresInfo.isSmsEnabled());
        Assert.assertTrue(featuresInfo.isTwoFaEnabled());
        Assert.assertFalse(featuresInfo.isNotificationEnabled());
        Assert.assertFalse(featuresInfo.isOauthEnabled());

        AdminSettings notificationsSettings = new AdminSettings();
        notificationsSettings.setKey("notifications");

        var notificationSettings = JacksonUtil.newObjectNode();
        var deliveryMethodsConfigs = JacksonUtil.newObjectNode();
        deliveryMethodsConfigs.set("SLACK", JacksonUtil.newObjectNode());
        notificationSettings.set("deliveryMethodsConfigs", deliveryMethodsConfigs);

        notificationsSettings.setJsonValue(notificationSettings);
        doPost("/api/admin/settings", notificationsSettings).andExpect(status().isOk());

        featuresInfo = doGet("/api/admin/featuresInfo", FeaturesInfo.class);
        Assert.assertTrue(featuresInfo.isEmailEnabled());
        Assert.assertTrue(featuresInfo.isSmsEnabled());
        Assert.assertTrue(featuresInfo.isTwoFaEnabled());
        Assert.assertTrue(featuresInfo.isNotificationEnabled());
        Assert.assertFalse(featuresInfo.isOauthEnabled());

        OAuth2Client oAuth2Client = createOauth2Client(TenantId.SYS_TENANT_ID, "test google client");
        OAuth2Client savedOAuth2Client = doPost("/api/oauth2/client", oAuth2Client, OAuth2Client.class);

        Domain domain = createDomain(TenantId.SYS_TENANT_ID, "my.home.domain", true, true);
        doPost("/api/domain?oauth2ClientIds=" + savedOAuth2Client.getId().getId(), domain, Domain.class);

        featuresInfo = doGet("/api/admin/featuresInfo", FeaturesInfo.class);
        Assert.assertNotNull(featuresInfo);
        Assert.assertTrue(featuresInfo.isEmailEnabled());
        Assert.assertTrue(featuresInfo.isSmsEnabled());
        Assert.assertTrue(featuresInfo.isTwoFaEnabled());
        Assert.assertTrue(featuresInfo.isNotificationEnabled());
        Assert.assertTrue(featuresInfo.isOauthEnabled());

        adminSettingsService.deleteAdminSettingsByTenantIdAndKey(TenantId.SYS_TENANT_ID, "notifications");
        adminSettingsService.deleteAdminSettingsByTenantIdAndKey(TenantId.SYS_TENANT_ID, "twoFaSettings");
        adminSettingsService.deleteAdminSettingsByTenantIdAndKey(TenantId.SYS_TENANT_ID, "sms");
        oAuth2ClientService.deleteOauth2ClientsByTenantId(TenantId.SYS_TENANT_ID);
        domainService.deleteDomainsByTenantId(TenantId.SYS_TENANT_ID);
    }

    @Test
    public void testUsageInfo() throws Exception {
        loginTenantAdmin();

        TenantProfile tenantProfile = tenantProfileCache.get(tenantId);

        Assert.assertNotNull(tenantProfile);

        DefaultTenantProfileConfiguration configuration = (DefaultTenantProfileConfiguration) tenantProfile.getProfileData().getConfiguration();

        UsageInfo usageInfo = doGet("/api/usage", UsageInfo.class);
        Assert.assertNotNull(usageInfo);
        Assert.assertEquals(0, usageInfo.getDevices());
        Assert.assertEquals(configuration.getMaxDevices(), usageInfo.getMaxDevices());

        Assert.assertEquals(0, usageInfo.getAssets());
        Assert.assertEquals(configuration.getMaxAssets(), usageInfo.getMaxAssets());

        Assert.assertEquals(1, usageInfo.getCustomers());
        Assert.assertEquals(configuration.getMaxCustomers(), usageInfo.getMaxCustomers());

        Assert.assertEquals(2, usageInfo.getUsers());
        Assert.assertEquals(configuration.getMaxUsers(), usageInfo.getMaxUsers());

        Assert.assertEquals(DEFAULT_DASHBOARDS_COUNT, usageInfo.getDashboards());
        Assert.assertEquals(configuration.getMaxDashboards(), usageInfo.getMaxDashboards());

        Assert.assertEquals(0, usageInfo.getEdges());
        Assert.assertEquals(configuration.getMaxEdges(), usageInfo.getMaxEdges());

        Assert.assertEquals(0, usageInfo.getTransportMessages());
        Assert.assertEquals(configuration.getMaxTransportMessages(), usageInfo.getMaxTransportMessages());

        Assert.assertEquals(0, usageInfo.getJsExecutions());
        Assert.assertEquals(configuration.getMaxJSExecutions(), usageInfo.getMaxJsExecutions());

        Assert.assertEquals(0, usageInfo.getTbelExecutions());
        Assert.assertEquals(configuration.getMaxTbelExecutions(), usageInfo.getMaxTbelExecutions());

        Assert.assertEquals(0, usageInfo.getEmails());
        Assert.assertEquals(configuration.getMaxEmails(), usageInfo.getMaxEmails());

        Assert.assertEquals(0, usageInfo.getSms());
        Assert.assertEquals(configuration.getMaxSms(), usageInfo.getMaxSms());

        Assert.assertEquals(0, usageInfo.getAlarms());
        Assert.assertEquals(configuration.getMaxCreatedAlarms(), usageInfo.getMaxAlarms());

        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 97; i++) {
            Device device = new Device();
            device.setName("device" + i);
            devices.add(doPost("/api/device", device, Device.class));
        }

        usageInfo = doGet("/api/usage", UsageInfo.class);
        Assert.assertEquals(devices.size(), usageInfo.getDevices());

        List<Asset> assets = new ArrayList<>();
        for (int i = 0; i < 97; i++) {
            Asset asset = new Asset();
            asset.setName("asset" + i);
            assets.add(doPost("/api/asset", asset, Asset.class));
        }

        usageInfo = doGet("/api/usage", UsageInfo.class);
        Assert.assertEquals(assets.size(), usageInfo.getAssets());

        List<Customer> customers = new ArrayList<>();
        for (int i = 0; i < 97; i++) {
            Customer customer = new Customer();
            customer.setTitle("customer" + i);
            customers.add(doPost("/api/customer", customer, Customer.class));
        }

        usageInfo = doGet("/api/usage", UsageInfo.class);
        Assert.assertEquals(customers.size() + 1, usageInfo.getCustomers());

        List<User> users = new ArrayList<>();
        for (int i = 0; i < 97; i++) {
            User user = new User();
            user.setAuthority(Authority.TENANT_ADMIN);
            user.setEmail(i + "user@thingsboard.org");
            users.add(doPost("/api/user", user, User.class));
        }

        usageInfo = doGet("/api/usage", UsageInfo.class);
        Assert.assertEquals(users.size() + 2, usageInfo.getUsers());

        List<Dashboard> dashboards = new ArrayList<>();
        for (int i = 0; i < 97; i++) {
            Dashboard dashboard = new Dashboard();
            dashboard.setTitle("dashboard" + i);
            dashboards.add(doPost("/api/dashboard", dashboard, Dashboard.class));
        }

        usageInfo = doGet("/api/usage", UsageInfo.class);
        int expectedDashboardsCount = dashboards.size() + DEFAULT_DASHBOARDS_COUNT;
        Assert.assertEquals(expectedDashboardsCount, usageInfo.getDashboards());
    }

    private Long getInitialEntityCount(EntityType entityType) throws Exception {
        loginSysAdmin();

        EntityTypeFilter allEntityFilter = new EntityTypeFilter();
        allEntityFilter.setEntityType(entityType);
        EntityCountQuery query = new EntityCountQuery(allEntityFilter);
        return doPostWithResponse("/api/entitiesQuery/count", query, Long.class);
    }

    private Domain createDomain(TenantId tenantId, String domainName, boolean oauth2Enabled, boolean edgeEnabled) {
        Domain domain = new Domain();
        domain.setTenantId(tenantId);
        domain.setName(domainName);
        domain.setOauth2Enabled(oauth2Enabled);
        domain.setPropagateToEdge(edgeEnabled);
        return domain;
    }

}
