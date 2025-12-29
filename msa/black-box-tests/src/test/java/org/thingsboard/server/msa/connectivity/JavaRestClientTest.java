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
package org.thingsboard.server.msa.connectivity;

import com.google.gson.JsonObject;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.HostnameVerificationPolicy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.ssl.SSLContexts;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rest.client.RestClient;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileInfo;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.asset.AssetProfileInfo;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.domain.Domain;
import org.thingsboard.server.common.data.domain.DomainInfo;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.mobile.app.MobileApp;
import org.thingsboard.server.common.data.mobile.app.MobileAppStatus;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundle;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundleInfo;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestConfig;
import org.thingsboard.server.common.data.notification.NotificationRequestInfo;
import org.thingsboard.server.common.data.notification.NotificationRequestPreview;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.settings.NotificationSettings;
import org.thingsboard.server.common.data.notification.settings.SlackNotificationDeliveryMethodConfig;
import org.thingsboard.server.common.data.notification.settings.UserNotificationSettings;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.platform.PlatformUsersNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.platform.UserListFilter;
import org.thingsboard.server.common.data.notification.template.DeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.EmailDeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.HasSubject;
import org.thingsboard.server.common.data.notification.template.MobileAppDeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplateConfig;
import org.thingsboard.server.common.data.notification.template.SmsDeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.WebDeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.common.data.pat.ApiKeyInfo;
import org.thingsboard.server.common.data.query.AvailableEntityKeys;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityTypeFilter;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.TestProperties;

import javax.net.ssl.SSLContext;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.common.data.notification.NotificationDeliveryMethod.EMAIL;
import static org.thingsboard.server.common.data.notification.NotificationDeliveryMethod.MICROSOFT_TEAMS;
import static org.thingsboard.server.common.data.notification.NotificationDeliveryMethod.WEB;
import static org.thingsboard.server.msa.prototypes.DevicePrototypes.defaultDevicePrototype;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultTenantAdmin;

public class JavaRestClientTest extends AbstractContainerTest {

    public static final String DEFAULT_NOTIFICATION_SUBJECT = "Just a test";
    public static final NotificationType DEFAULT_NOTIFICATION_TYPE = NotificationType.GENERAL;
    private RestClient restClient;
    private Tenant tenant1;
    private Tenant tenant2;
    private User tenantAdmin1;
    private User tenantAdmin2;

    @BeforeClass
    public void beforeClass() throws Exception {
        SSLContext ssl = SSLContexts.custom()
                .loadTrustMaterial((chain, authType) -> true)
                .build();

        var tls = new DefaultClientTlsStrategy(
                ssl,
                HostnameVerificationPolicy.CLIENT,
                NoopHostnameVerifier.INSTANCE
        );

        HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                .setTlsSocketStrategy(tls)
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .build();

        RestTemplate rt = new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
        restClient = new RestClient(rt, TestProperties.getBaseUrl());
    }

    @BeforeMethod
    public void setUp() throws Exception {
        restClient.login("sysadmin@thingsboard.org", "sysadmin");

        // create tenant 1 and tenant admin 1
        tenant1 = new Tenant();
        tenant1.setTitle("Java Rest Client Test Tenant " + RandomStringUtils.insecure().randomAlphabetic(5));
        tenant1 = restClient.saveTenant(tenant1);

        String email1 = RandomStringUtils.insecure().randomAlphabetic(5) + "@gmail.com";
        tenantAdmin1 = restClient.saveUser(defaultTenantAdmin(tenant1.getId(), email1), false);
        restClient.activateUser(tenantAdmin1.getId(), "password123", false);

        // create tenant 2 and tenant admin 2
        tenant2 = new Tenant();
        tenant2.setTitle("Java Rest Client Test Tenant " + RandomStringUtils.insecure().randomAlphabetic(5));
        tenant2 = restClient.saveTenant(tenant2);

        String email2 = RandomStringUtils.insecure().randomAlphabetic(5) + "@gmail.com";
        tenantAdmin2 = restClient.saveUser(defaultTenantAdmin(tenant2.getId(), email2), false);
        restClient.activateUser(tenantAdmin2.getId(), "password123", false);

        // tenant 1 tenant admin by default
        restClient.login(tenantAdmin1.getEmail(), "password123");
    }

    @AfterMethod
    public void tearDown() {
        restClient.login("sysadmin@thingsboard.org", "sysadmin");
        if (tenant1 != null) {
            restClient.deleteTenant(tenant1.getId());
        }
        if (tenant2 != null) {
            restClient.deleteTenant(tenant2.getId());
        }
    }

    @Test
    public void testGetAlarmsV2() {
        Device device = restClient.saveDevice(defaultDevicePrototype(RandomStringUtils.insecure().randomAlphabetic(5)));
        assertThat(device).isNotNull();

        String type = "High temp" + RandomStringUtils.insecure().randomAlphabetic(5);
        Alarm alarm = Alarm.builder()
                .originator(device.getId())
                .severity(AlarmSeverity.CRITICAL)
                .type(type)
                .build();
        restClient.saveAlarm(alarm);

        // get /api/v2/alarm
        PageData<AlarmInfo> alarmsV2 = restClient.getAlarmsV2(device.getId(), null, null, List.of(type), null, new TimePageLink(10, 0));
        assertThat(alarmsV2.getData()).hasSize(1);

        PageData<AlarmInfo> activeAlarms = restClient.getAlarmsV2(device.getId(), List.of(AlarmSearchStatus.ACTIVE), null, List.of(type), null, new TimePageLink(10, 0));
        assertThat(activeAlarms.getData()).hasSize(1);

        PageData<AlarmInfo> cleared = restClient.getAlarmsV2(device.getId(), List.of(AlarmSearchStatus.CLEARED), null, List.of(type), null, new TimePageLink(10, 0));
        assertThat(cleared.getData()).hasSize(0);

        PageData<AlarmInfo> activeAndClearedAlarms = restClient.getAlarmsV2(device.getId(), List.of(AlarmSearchStatus.CLEARED, AlarmSearchStatus.ACTIVE), null, null, null, new TimePageLink(10, 0));
        assertThat(activeAndClearedAlarms.getData()).hasSize(1);

        // get /api/v2/alarms
        PageData<AlarmInfo> allAlarmsV2 = restClient.getAllAlarmsV2(List.of(AlarmSearchStatus.ACTIVE), null, List.of(type), null, new TimePageLink(10, 0));
        assertThat(allAlarmsV2.getData()).hasSize(1);

        PageData<AlarmInfo> allClearedAlarmsV2 = restClient.getAllAlarmsV2(List.of(AlarmSearchStatus.CLEARED), null, List.of(type), null, new TimePageLink(10, 0));
        assertThat(allClearedAlarmsV2.getData()).hasSize(0);

        // get /api/alarms
        PageData<AlarmInfo> allAlarms = restClient.getAllAlarms(AlarmSearchStatus.ACTIVE, null, new TimePageLink(10, 0), null);
        assertThat(allAlarms.getData()).hasSize(1);

        PageData<AlarmInfo> allClearedAlarms = restClient.getAllAlarms(AlarmSearchStatus.CLEARED, null, new TimePageLink(10, 0), null);
        assertThat(allClearedAlarms.getData()).hasSize(0);
    }

    @Test
    public void testTimeSeriesByReadTsKvQueries() {
        Device device = restClient.saveDevice(defaultDevicePrototype(RandomStringUtils.insecure().randomAlphabetic(5)));
        assertThat(device).isNotNull();

        DeviceCredentials deviceCredentials = restClient.getDeviceCredentialsByDeviceId(device.getId()).get();
        for (int i = 0; i < 3; i++) {
            JsonObject values = new JsonObject();
            values.addProperty("temperature", i + 25);
            testRestClient.postTelemetry(deviceCredentials.getCredentialsId(), JacksonUtil.toJsonNode(createPayload().toString()));
        }

        restClient.saveEntityTelemetry(device.getId(), "ts", JacksonUtil.toJsonNode("{\"temperature\": 25, \"humidity\": 60}"));
        restClient.saveEntityTelemetry(device.getId(), "ts", JacksonUtil.toJsonNode("{\"temperature\": 27, \"humidity\": 59}"));
        restClient.saveEntityTelemetry(device.getId(), "ts", JacksonUtil.toJsonNode("{\"temperature\": 33, \"humidity\": 62}"));

        EntityTypeFilter filter = new EntityTypeFilter();
        filter.setEntityType(EntityType.DEVICE);
        var pageLink = new EntityDataPageLink(20, 0, null, new EntityDataSortOrder(new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"), EntityDataSortOrder.Direction.DESC), false);

        var entityFields = Arrays.asList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"), new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"));

        EntityDataQuery entityDataQuery = new EntityDataQuery(filter, pageLink, entityFields, null, null);
        AvailableEntityKeys availableEntityKeys = restClient.findAvailableEntityKeysByQuery(entityDataQuery, true, true, null);
        assertThat(availableEntityKeys).isNotNull();
        assertThat(availableEntityKeys.timeseries()).contains("temperature", "humidity");
    }

    @Test
    public void testFindNotifications() {
        NotificationTarget notificationTarget = createNotificationTarget(tenantAdmin1.getId());
        String notificationText1 = "Notification 1";
        NotificationTemplate notificationTemplate = createNotificationTemplate(DEFAULT_NOTIFICATION_TYPE, DEFAULT_NOTIFICATION_SUBJECT, notificationText1, new NotificationDeliveryMethod[]{WEB});
        NotificationRequest notificationRequest = submitNotificationRequest(notificationTarget.getId(), notificationTemplate.getId());

        String notificationText2 = "Notification 2";
        NotificationTemplate notificationTemplate2 = createNotificationTemplate(DEFAULT_NOTIFICATION_TYPE, DEFAULT_NOTIFICATION_SUBJECT, notificationText2, new NotificationDeliveryMethod[]{WEB});
        NotificationRequest notificationRequest2 = submitNotificationRequest(notificationTarget.getId(), notificationTemplate2.getId());

        PageData<NotificationRequestInfo> initialRequests = restClient.getNotificationRequests(new PageLink(30));
        assertThat(initialRequests.getTotalElements()).isGreaterThanOrEqualTo(2);

        NotificationRequestInfo notificationRequestInfo = restClient.getNotificationRequestById(notificationRequest.getId()).get();
        assertThat(notificationRequestInfo.getName()).isEqualTo(notificationRequest.getName());
        assertThat(notificationRequestInfo.getTemplateName()).isEqualTo(notificationTemplate.getName());

        NotificationRequestPreview requestPreview = restClient.getNotificationRequestPreview(notificationRequest, 10);
        assertThat(requestPreview.getTotalRecipientsCount()).isEqualTo(1);
        assertThat(requestPreview.getRecipientsPreview()).isEqualTo(List.of(tenantAdmin1.getEmail()));

        PageData<Notification> notifications = restClient.getNotifications(false, WEB, new PageLink(30));
        assertThat(notifications.getTotalElements()).isEqualTo(2);

        Integer unreadCount = restClient.getUnreadNotificationsCount(WEB);
        assertThat(unreadCount).isEqualTo(2);

        restClient.markNotificationAsRead(notifications.getData().get(0).getId());

        Integer unreadCountAfterRead = restClient.getUnreadNotificationsCount(WEB);
        assertThat(unreadCountAfterRead).isEqualTo(1);

        restClient.markAllNotificationsAsRead(WEB);

        Integer unreadCountAfterAllRead = restClient.getUnreadNotificationsCount(WEB);
        assertThat(unreadCountAfterAllRead).isEqualTo(0);

        restClient.deleteNotification(notifications.getData().get(0).getId());
        notifications = restClient.getNotifications(false, WEB, new PageLink(30));
        assertThat(notifications.getTotalElements()).isEqualTo(1);

        restClient.deleteNotificationRequest(notificationRequest.getId());
        PageData<NotificationRequestInfo> requestsAfterUpdate = restClient.getNotificationRequests(new PageLink(30));
        assertThat(requestsAfterUpdate.getTotalElements()).isEqualTo(initialRequests.getTotalElements() - 1);

        List<NotificationDeliveryMethod> availableDeliveryMethods = restClient.getAvailableDeliveryMethods();
        assertThat(availableDeliveryMethods).contains(WEB, EMAIL, MICROSOFT_TEAMS);
    }

    @Test
    public void testSaveNotificationSettings() {
        NotificationSettings settings = new NotificationSettings();
        SlackNotificationDeliveryMethodConfig slackConfig = new SlackNotificationDeliveryMethodConfig();
        String slackToken = "xoxb-123123123";
        slackConfig.setBotToken(slackToken);
        settings.setDeliveryMethodsConfigs(Map.of(
                NotificationDeliveryMethod.SLACK, slackConfig
        ));

        restClient.saveNotificationSettings(settings);

        NotificationSettings savedSettings = restClient.getNotificationSettings().get();
        assertThat(savedSettings.getDeliveryMethodsConfigs()).hasSize(1);
        assertThat(savedSettings.getDeliveryMethodsConfigs().get(slackConfig.getMethod())).isEqualTo(slackConfig);

        // save user notification settings
        var entityActionNotificationPref = new UserNotificationSettings.NotificationPref();
        entityActionNotificationPref.setEnabled(true);
        entityActionNotificationPref.setEnabledDeliveryMethods(Map.of(
                NotificationDeliveryMethod.WEB, true,
                NotificationDeliveryMethod.SMS, false,
                NotificationDeliveryMethod.EMAIL, false
        ));

        UserNotificationSettings userNotificationSettings = new UserNotificationSettings(Map.of(
                NotificationType.ENTITY_ACTION, entityActionNotificationPref
        ));
        UserNotificationSettings saved = restClient.saveUserNotificationSettings(userNotificationSettings);
        UserNotificationSettings retrieved = restClient.getUserNotificationSettings().get();
        assertThat(retrieved).isEqualTo(saved);
    }

    @Test
    public void testSaveDomain() {
        restClient.login("sysadmin@thingsboard.org", "sysadmin");

        Domain domain = new Domain();
        String prefix = RandomStringUtils.insecure().randomAlphabetic(5).toLowerCase();
        domain.setName(prefix + ".test.com");
        Domain savedDomain = restClient.saveDomain(domain);
        assertThat(savedDomain.getName()).isEqualTo(domain.getName());

        PageData<DomainInfo> domainInfos = restClient.getTenantDomainInfos(new PageLink(10, 0, prefix));
        assertThat(domainInfos.getData()).hasSize(1);
    }

    @Test
    public void testSaveMobileApp() {
        restClient.login("sysadmin@thingsboard.org", "sysadmin");

        MobileApp mobileApp = new MobileApp();
        String prefix = RandomStringUtils.insecure().randomAlphabetic(5).toLowerCase();
        mobileApp.setPkgName(prefix + "test.app.apple");
        mobileApp.setPlatformType(PlatformType.ANDROID);
        mobileApp.setAppSecret(RandomStringUtils.insecure().randomAlphabetic(20));
        mobileApp.setStatus(MobileAppStatus.DRAFT);

        MobileApp savedMobileApp = restClient.saveMobileApp(mobileApp);
        assertThat(savedMobileApp.getName()).isEqualTo(mobileApp.getName());

        PageData<MobileApp> retrieved = restClient.getTenantMobileApps(new PageLink(10, 0, prefix));
        assertThat(retrieved.getData()).hasSize(1);

        MobileAppBundle mobileAppBundle = new MobileAppBundle();
        String bundlePrefix = RandomStringUtils.insecure().randomAlphabetic(5).toLowerCase();
        mobileAppBundle.setTitle(bundlePrefix + "Test Bundle");
        mobileAppBundle.setAndroidAppId(savedMobileApp.getId());

        MobileAppBundle savedMobileAppBundle = restClient.saveMobileBundle(mobileAppBundle);
        PageData<MobileAppBundleInfo> bundleInfos = restClient.getTenantMobileBundleInfos(new PageLink(10, 0, bundlePrefix));
        assertThat(bundleInfos.getData()).hasSize(1);
    }

    private NotificationTarget createNotificationTarget(UserId... usersIds) {
        UserListFilter filter = new UserListFilter();
        filter.setUsersIds(Arrays.stream(usersIds).map(UUIDBased::getId).toList());

        NotificationTarget notificationTarget = new NotificationTarget();
        notificationTarget.setName(filter + RandomStringUtils.insecure().randomNumeric(5));
        PlatformUsersNotificationTargetConfig targetConfig = new PlatformUsersNotificationTargetConfig();
        targetConfig.setUsersFilter(filter);
        notificationTarget.setConfiguration(targetConfig);
        return restClient.saveNotificationTarget(notificationTarget);
    }

    private NotificationTemplate createNotificationTemplate(NotificationType notificationType, String subject,
                                                            String text, NotificationDeliveryMethod... deliveryMethods) {
        NotificationTemplate notificationTemplate = new NotificationTemplate();
        notificationTemplate.setName("Notification template: " + RandomStringUtils.insecure().randomAlphabetic(5));
        notificationTemplate.setNotificationType(notificationType);
        NotificationTemplateConfig config = new NotificationTemplateConfig();
        config.setDeliveryMethodsTemplates(new HashMap<>());
        for (NotificationDeliveryMethod deliveryMethod : deliveryMethods) {
            DeliveryMethodNotificationTemplate deliveryMethodNotificationTemplate;
            switch (deliveryMethod) {
                case WEB: {
                    deliveryMethodNotificationTemplate = new WebDeliveryMethodNotificationTemplate();
                    break;
                }
                case EMAIL: {
                    deliveryMethodNotificationTemplate = new EmailDeliveryMethodNotificationTemplate();
                    break;
                }
                case SMS: {
                    deliveryMethodNotificationTemplate = new SmsDeliveryMethodNotificationTemplate();
                    break;
                }
                case MOBILE_APP:
                    deliveryMethodNotificationTemplate = new MobileAppDeliveryMethodNotificationTemplate();
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported delivery method " + deliveryMethod);
            }
            deliveryMethodNotificationTemplate.setEnabled(true);
            deliveryMethodNotificationTemplate.setBody(text);
            if (deliveryMethodNotificationTemplate instanceof HasSubject) {
                ((HasSubject) deliveryMethodNotificationTemplate).setSubject(subject);
            }
            config.getDeliveryMethodsTemplates().put(deliveryMethod, deliveryMethodNotificationTemplate);
        }
        notificationTemplate.setConfiguration(config);
        return restClient.saveNotificationTemplate(notificationTemplate);
    }

    private NotificationRequest submitNotificationRequest(NotificationTargetId targetId, NotificationTemplateId notificationTemplateId) {
        NotificationRequestConfig config = new NotificationRequestConfig();
        config.setSendingDelayInSec(0);
        NotificationRequest notificationRequest = NotificationRequest.builder()
                .targets(Stream.of(targetId).map(UUIDBased::getId).collect(Collectors.toList()))
                .templateId(notificationTemplateId)
                .additionalConfig(config)
                .build();
        return restClient.saveNotificationRequest(notificationRequest);
    }

    @Test
    public void testApiKeyOperations() {
        // Create an API key
        ApiKeyInfo apiKeyInfo = new ApiKeyInfo();
        apiKeyInfo.setDescription("Test API Key " + RandomStringUtils.insecure().randomAlphabetic(5));
        apiKeyInfo.setEnabled(true);
        apiKeyInfo.setUserId(tenantAdmin1.getId());
        apiKeyInfo.setExpirationTime(0);

        ApiKey savedApiKey = restClient.saveApiKey(apiKeyInfo);
        assertThat(savedApiKey).isNotNull();
        assertThat(savedApiKey.getId()).isNotNull();
        assertThat(savedApiKey.getDescription()).isEqualTo(apiKeyInfo.getDescription());
        assertThat(savedApiKey.isEnabled()).isTrue();
        assertThat(savedApiKey.getUserId()).isEqualTo(tenantAdmin1.getId());
        assertThat(savedApiKey.getTenantId()).isEqualTo(tenant1.getId());
        assertThat(savedApiKey.getValue()).isNotNull();

        // Get user API keys
        PageData<ApiKeyInfo> apiKeys = restClient.getUserApiKeys(tenantAdmin1.getId(), new PageLink(10));
        assertThat(apiKeys).isNotNull();
        assertThat(apiKeys.getData()).hasSize(1);
        assertThat(apiKeys.getData().get(0).getId()).isEqualTo(savedApiKey.getId());

        // Update API key description
        String updatedDescription = "Updated description " + RandomStringUtils.insecure().randomAlphabetic(5);
        ApiKeyInfo updatedApiKeyInfo = restClient.updateApiKeyDescription(savedApiKey.getId(), updatedDescription);
        assertThat(updatedApiKeyInfo).isNotNull();
        assertThat(updatedApiKeyInfo.getDescription()).isEqualTo(updatedDescription);

        // Disable the API key
        ApiKeyInfo disabledApiKey = restClient.enableApiKey(savedApiKey.getId(), false);
        assertThat(disabledApiKey).isNotNull();
        assertThat(disabledApiKey.isEnabled()).isFalse();

        // Enable the API key
        ApiKeyInfo enabledApiKey = restClient.enableApiKey(savedApiKey.getId(), true);
        assertThat(enabledApiKey).isNotNull();
        assertThat(enabledApiKey.isEnabled()).isTrue();

        // Delete the API key
        restClient.deleteApiKey(savedApiKey.getId());

        // Verify the API key is deleted
        PageData<ApiKeyInfo> apiKeysAfterDelete = restClient.getUserApiKeys(tenantAdmin1.getId(), new PageLink(10));
        assertThat(apiKeysAfterDelete.getData()).isEmpty();
    }

    @Test
    public void testGetDeviceProfileInfosByIds() {
        var profileData = new DeviceProfileData();
        profileData.setConfiguration(new DefaultDeviceProfileConfiguration());
        profileData.setTransportConfiguration(new DefaultDeviceProfileTransportConfiguration());

        // Create a device profile in tenant1 (current tenant)
        var deviceProfile1 = new DeviceProfile();
        deviceProfile1.setTenantId(tenant1.getId());
        deviceProfile1.setName("Device Profile 1");
        deviceProfile1.setType(DeviceProfileType.DEFAULT);
        deviceProfile1.setTransportType(DeviceTransportType.DEFAULT);
        deviceProfile1.setProfileData(profileData);
        deviceProfile1 = restClient.saveDeviceProfile(deviceProfile1);

        var deviceProfile2 = new DeviceProfile();
        deviceProfile2.setTenantId(tenant1.getId());
        deviceProfile2.setName("Device Profile 2");
        deviceProfile2.setType(DeviceProfileType.DEFAULT);
        deviceProfile2.setTransportType(DeviceTransportType.DEFAULT);
        deviceProfile2.setProfileData(profileData);
        deviceProfile2 = restClient.saveDeviceProfile(deviceProfile2);

        // Create two more device profiles in tenant2 (different tenant)
        restClient.login(tenantAdmin2.getEmail(), "password123");

        var deviceProfile3 = new DeviceProfile();
        deviceProfile3.setTenantId(tenant2.getId());
        deviceProfile3.setName("Device Profile 3");
        deviceProfile3.setType(DeviceProfileType.DEFAULT);
        deviceProfile3.setTransportType(DeviceTransportType.DEFAULT);
        deviceProfile3.setProfileData(profileData);
        deviceProfile3 = restClient.saveDeviceProfile(deviceProfile3);

        var deviceProfile4 = new DeviceProfile();
        deviceProfile4.setTenantId(tenant2.getId());
        deviceProfile4.setName("Device Profile 4");
        deviceProfile4.setType(DeviceProfileType.DEFAULT);
        deviceProfile4.setTransportType(DeviceTransportType.DEFAULT);
        deviceProfile4.setProfileData(profileData);
        deviceProfile4 = restClient.saveDeviceProfile(deviceProfile4);

        // Attempt to fetch profiles 1 and 3 while acting as Tenant 2.
        // - Profile 1: Filtered out because it belongs to a different tenant.
        // - Profile 2: Filtered out because it belongs to a different tenant and was not requested.
        // - Profile 3: Should be returned.
        // - Profile 4: Filtered out; it belongs to the correct tenant, but was not requested by ID.
        List<DeviceProfileInfo> profiles = restClient.getDeviceProfileInfosByIds(Set.of(deviceProfile1.getUuidId(), deviceProfile3.getUuidId()));
        assertThat(profiles).hasSize(1);
        assertThat(profiles.get(0).getId()).isEqualTo(deviceProfile3.getId());
    }

    @Test
    public void testGetAssetProfilesByIds() {
        // Create two asset profiles in tenant1 (current tenant)
        var assetProfile1 = new AssetProfile();
        assetProfile1.setTenantId(tenant1.getId());
        assetProfile1.setName("Asset Profile 1");
        assetProfile1 = restClient.saveAssetProfile(assetProfile1);

        var assetProfile2 = new AssetProfile();
        assetProfile2.setTenantId(tenant1.getId());
        assetProfile2.setName("Asset Profile 2");
        assetProfile2 = restClient.saveAssetProfile(assetProfile2);

        // Create two more asset profiles in tenant2 (different tenant)
        restClient.login(tenantAdmin2.getEmail(), "password123");

        var assetProfile3 = new AssetProfile();
        assetProfile3.setTenantId(tenant2.getId());
        assetProfile3.setName("Asset Profile 3");
        assetProfile3 = restClient.saveAssetProfile(assetProfile3);

        var assetProfile4 = new AssetProfile();
        assetProfile4.setTenantId(tenant2.getId());
        assetProfile4.setName("Asset Profile 4");
        assetProfile4 = restClient.saveAssetProfile(assetProfile4);

        // Attempt to fetch profiles 1 and 3 while acting as Tenant 2.
        // - Profile 1: Filtered out because it belongs to a different tenant.
        // - Profile 2: Filtered out because it belongs to a different tenant and was not requested.
        // - Profile 3: Should be returned.
        // - Profile 4: Filtered out; it belongs to the correct tenant, but was not requested by ID.
        List<AssetProfileInfo> profiles = restClient.getAssetProfilesByIds(Set.of(assetProfile1.getUuidId(), assetProfile3.getUuidId()));
        assertThat(profiles).hasSize(1);
        assertThat(profiles.get(0).getId()).isEqualTo(assetProfile3.getId());
    }

}
