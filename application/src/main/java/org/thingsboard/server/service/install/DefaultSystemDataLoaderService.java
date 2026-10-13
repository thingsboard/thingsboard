// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.install;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.menu.CMAssigneeType;
import org.thingsboard.server.common.data.menu.CMScope;
import org.thingsboard.server.common.data.menu.CustomMenuInfo;
import org.thingsboard.server.common.data.mobile.app.MobileApp;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.queue.ProcessingStrategy;
import org.thingsboard.server.common.data.queue.ProcessingStrategyType;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.queue.SubmitStrategy;
import org.thingsboard.server.common.data.queue.SubmitStrategyType;
import org.thingsboard.server.common.data.security.model.JwtSettings;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileQueueConfiguration;
import org.thingsboard.server.dao.device.DeviceConnectivityConfiguration;
import org.thingsboard.server.dao.menu.CustomMenuService;
import org.thingsboard.server.dao.mobile.MobileAppDao;
import org.thingsboard.server.dao.notification.NotificationSettingsService;
import org.thingsboard.server.dao.notification.NotificationTargetService;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.tenant.TenantProfileService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.service.security.auth.jwt.settings.JwtSettingsService;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.thingsboard.common.util.DebugModeUtil.DEBUG_MODE_DEFAULT_DURATION_MINUTES;
import static org.thingsboard.server.service.security.auth.jwt.settings.DefaultJwtSettingsService.isSigningKeyDefault;
import static org.thingsboard.server.service.security.auth.jwt.settings.DefaultJwtSettingsService.validateKeyLength;

@Service
@Profile("install")
@Slf4j
@RequiredArgsConstructor
public class DefaultSystemDataLoaderService implements SystemDataLoaderService {

    private final InstallScripts installScripts;
    private final AdminSettingsService adminSettingsService;
    private final TenantService tenantService;
    private final TenantProfileService tenantProfileService;
    private final DeviceConnectivityConfiguration connectivityConfiguration;
    private final QueueService queueService;
    private final JwtSettingsService jwtSettingsService;
    private final MobileAppDao mobileAppDao;
    private final NotificationSettingsService notificationSettingsService;
    private final NotificationTargetService notificationTargetService;
    private final CustomMenuService customMenuService;

    @Value("${security.jwt.tokenExpirationTime:9000}")
    private Integer tokenExpirationTime;
    @Value("${security.jwt.refreshTokenExpTime:604800}")
    private Integer refreshTokenExpTime;
    @Value("${security.jwt.tokenIssuer:thingsboard.io}")
    private String tokenIssuer;
    @Value("${security.jwt.tokenSigningKey:thingsboardDefaultSigningKey}")
    private String tokenSigningKey;

    @Override
    public void createDefaultTenantProfiles() throws Exception {
        tenantProfileService.findOrCreateDefaultTenantProfile(TenantId.SYS_TENANT_ID);

        TenantProfileData isolatedRuleEngineTenantProfileData = new TenantProfileData();
        DefaultTenantProfileConfiguration configuration = new DefaultTenantProfileConfiguration();
        configuration.setMaxDebugModeDurationMinutes(DEBUG_MODE_DEFAULT_DURATION_MINUTES);
        isolatedRuleEngineTenantProfileData.setConfiguration(configuration);

        TenantProfileQueueConfiguration mainQueueConfiguration = new TenantProfileQueueConfiguration();
        mainQueueConfiguration.setName(DataConstants.MAIN_QUEUE_NAME);
        mainQueueConfiguration.setTopic(DataConstants.MAIN_QUEUE_TOPIC);
        mainQueueConfiguration.setPollInterval(25);
        mainQueueConfiguration.setPartitions(10);
        mainQueueConfiguration.setConsumerPerPartition(true);
        mainQueueConfiguration.setPackProcessingTimeout(2000);
        SubmitStrategy mainQueueSubmitStrategy = new SubmitStrategy();
        mainQueueSubmitStrategy.setType(SubmitStrategyType.BURST);
        mainQueueSubmitStrategy.setBatchSize(1000);
        mainQueueConfiguration.setSubmitStrategy(mainQueueSubmitStrategy);
        ProcessingStrategy mainQueueProcessingStrategy = new ProcessingStrategy();
        mainQueueProcessingStrategy.setType(ProcessingStrategyType.SKIP_ALL_FAILURES);
        mainQueueProcessingStrategy.setRetries(3);
        mainQueueProcessingStrategy.setFailurePercentage(0);
        mainQueueProcessingStrategy.setPauseBetweenRetries(3);
        mainQueueProcessingStrategy.setMaxPauseBetweenRetries(3);
        mainQueueConfiguration.setProcessingStrategy(mainQueueProcessingStrategy);

        isolatedRuleEngineTenantProfileData.setQueueConfiguration(Collections.singletonList(mainQueueConfiguration));

        TenantProfile isolatedTbRuleEngineProfile = new TenantProfile();
        isolatedTbRuleEngineProfile.setDefault(false);
        isolatedTbRuleEngineProfile.setName("Isolated TB Rule Engine");
        isolatedTbRuleEngineProfile.setDescription("Isolated TB Rule Engine tenant profile");
        isolatedTbRuleEngineProfile.setIsolatedTbRuleEngine(true);
        isolatedTbRuleEngineProfile.setProfileData(isolatedRuleEngineTenantProfileData);

        try {
            tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, isolatedTbRuleEngineProfile);
        } catch (DataValidationException e) {
            log.warn(e.getMessage());
        }
    }

    @Override
    public void createAdminSettings() throws Exception {
        AdminSettings generalSettings = new AdminSettings();
        generalSettings.setTenantId(TenantId.SYS_TENANT_ID);
        generalSettings.setKey("general");
        ObjectNode node = JacksonUtil.newObjectNode();
        node.put("baseUrl", "http://localhost:8080");
        node.put("prohibitDifferentUrl", false);
        generalSettings.setJsonValue(node);
        adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, generalSettings);

        AdminSettings mailSettings = new AdminSettings();
        mailSettings.setTenantId(TenantId.SYS_TENANT_ID);
        mailSettings.setKey("mail");
        node = JacksonUtil.newObjectNode();
        node.put("mailFrom", "ThingsBoard <sysadmin@localhost.localdomain>");
        node.put("smtpProtocol", "smtp");
        node.put("smtpHost", "localhost");
        node.put("smtpPort", "25");
        node.put("timeout", "10000");
        node.put("enableTls", false);
        node.put("username", "");
        node.put("password", "");
        node.put("tlsVersion", "TLSv1.2");//NOSONAR, key used to identify password field (not password value itself)
        node.put("enableProxy", false);
        node.put("showChangePassword", false);
        mailSettings.setJsonValue(node);
        adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, mailSettings);

        AdminSettings connectivitySettings = new AdminSettings();
        connectivitySettings.setTenantId(TenantId.SYS_TENANT_ID);
        connectivitySettings.setKey("connectivity");
        connectivitySettings.setJsonValue(JacksonUtil.valueToTree(connectivityConfiguration.getConnectivity()));
        adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, connectivitySettings);

        loadMailTemplates();
    }

    @Override
    public void loadMailTemplates() throws Exception {
        installScripts.loadMailTemplates();
    }

    @Override
    public void updateMailTemplates(JsonNode value) throws Exception {
        installScripts.updateMailTemplates(value);
    }

    @Override
    public void createRandomJwtSettings() throws Exception {
        if (jwtSettingsService.getJwtSettings() == null) {
            log.info("Creating JWT admin settings...");
            var jwtSettings = new JwtSettings(this.tokenExpirationTime, this.refreshTokenExpTime, this.tokenIssuer, this.tokenSigningKey);
            if (isSigningKeyDefault(jwtSettings) || !validateKeyLength(jwtSettings.getTokenSigningKey())) {
                jwtSettings.setTokenSigningKey(generateRandomKey());
            }
            jwtSettingsService.saveJwtSettings(jwtSettings);
        } else {
            log.info("Skip creating JWT admin settings because they already exist.");
        }
    }

    @Override
    public void updateSecuritySettings() {
        JwtSettings jwtSettings = jwtSettingsService.getJwtSettings();
        boolean invalidSignKey = false;
        String warningMessage = null;

        if (isSigningKeyDefault(jwtSettings)) {
            warningMessage = "The platform is using the default JWT Signing Key, which is a security risk.";
            invalidSignKey = true;
        } else if (!validateKeyLength(jwtSettings.getTokenSigningKey())) {
            warningMessage = "The JWT Signing Key is shorter than 512 bits, which is a security risk.";
            invalidSignKey = true;
        }

        if (invalidSignKey) {
            log.warn("WARNING: {}. A new JWT Signing Key has been added automatically. " +
                     "You can change the JWT Signing Key using the Web UI: " +
                     "Navigate to \"System settings -> Security settings\" while logged in as a System Administrator.", warningMessage);

            jwtSettings.setTokenSigningKey(generateRandomKey());
            jwtSettingsService.saveJwtSettings(jwtSettings);
        }

        List<MobileApp> mobiles = mobileAppDao.findByTenantId(TenantId.SYS_TENANT_ID, null, new PageLink(Integer.MAX_VALUE, 0)).getData();
        if (CollectionUtils.isNotEmpty(mobiles)) {
            mobiles.stream()
                    .filter(mobileApp -> !validateKeyLength(mobileApp.getAppSecret()))
                    .forEach(mobileApp -> {
                        log.warn("WARNING: The App secret is shorter than 512 bits, which is a security risk. " +
                                 "A new Application Secret has been added automatically for Mobile Application [{}]. " +
                                 "You can change the Application Secret using the Web UI: " +
                                 "Navigate to \"Security settings -> OAuth2 -> Mobile applications\" while logged in as a System Administrator.", mobileApp.getPkgName());
                        mobileApp.setAppSecret(generateRandomKey());
                        mobileAppDao.save(TenantId.SYS_TENANT_ID, mobileApp);
                    });
        }
    }

    private String generateRandomKey() {
        return Base64.getEncoder().encodeToString(
                RandomStringUtils.secure().nextAlphanumeric(64).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void createOAuth2Templates() throws Exception {
        installScripts.createOAuth2Templates();
    }

    @Override
    public void loadSystemWidgets() throws Exception {
        installScripts.loadSystemWidgets();
    }


    @Override
    public void createQueues() {
        Queue mainQueue = queueService.findQueueByTenantIdAndName(TenantId.SYS_TENANT_ID, DataConstants.MAIN_QUEUE_NAME);
        if (mainQueue == null) {
            mainQueue = new Queue();
            mainQueue.setTenantId(TenantId.SYS_TENANT_ID);
            mainQueue.setName(DataConstants.MAIN_QUEUE_NAME);
            mainQueue.setTopic(DataConstants.MAIN_QUEUE_TOPIC);
            mainQueue.setPollInterval(25);
            mainQueue.setPartitions(10);
            mainQueue.setConsumerPerPartition(true);
            mainQueue.setPackProcessingTimeout(2000);
            SubmitStrategy mainQueueSubmitStrategy = new SubmitStrategy();
            mainQueueSubmitStrategy.setType(SubmitStrategyType.BURST);
            mainQueueSubmitStrategy.setBatchSize(1000);
            mainQueue.setSubmitStrategy(mainQueueSubmitStrategy);
            ProcessingStrategy mainQueueProcessingStrategy = new ProcessingStrategy();
            mainQueueProcessingStrategy.setType(ProcessingStrategyType.SKIP_ALL_FAILURES);
            mainQueueProcessingStrategy.setRetries(3);
            mainQueueProcessingStrategy.setFailurePercentage(0);
            mainQueueProcessingStrategy.setPauseBetweenRetries(3);
            mainQueueProcessingStrategy.setMaxPauseBetweenRetries(3);
            mainQueue.setProcessingStrategy(mainQueueProcessingStrategy);
            queueService.saveQueue(mainQueue);
        }

        Queue highPriorityQueue = queueService.findQueueByTenantIdAndName(TenantId.SYS_TENANT_ID, DataConstants.HP_QUEUE_NAME);
        if (highPriorityQueue == null) {
            highPriorityQueue = new Queue();
            highPriorityQueue.setTenantId(TenantId.SYS_TENANT_ID);
            highPriorityQueue.setName(DataConstants.HP_QUEUE_NAME);
            highPriorityQueue.setTopic(DataConstants.HP_QUEUE_TOPIC);
            highPriorityQueue.setPollInterval(25);
            highPriorityQueue.setPartitions(10);
            highPriorityQueue.setConsumerPerPartition(true);
            highPriorityQueue.setPackProcessingTimeout(2000);
            SubmitStrategy highPriorityQueueSubmitStrategy = new SubmitStrategy();
            highPriorityQueueSubmitStrategy.setType(SubmitStrategyType.BURST);
            highPriorityQueueSubmitStrategy.setBatchSize(100);
            highPriorityQueue.setSubmitStrategy(highPriorityQueueSubmitStrategy);
            ProcessingStrategy highPriorityQueueProcessingStrategy = new ProcessingStrategy();
            highPriorityQueueProcessingStrategy.setType(ProcessingStrategyType.RETRY_FAILED_AND_TIMED_OUT);
            highPriorityQueueProcessingStrategy.setRetries(0);
            highPriorityQueueProcessingStrategy.setFailurePercentage(0);
            highPriorityQueueProcessingStrategy.setPauseBetweenRetries(5);
            highPriorityQueueProcessingStrategy.setMaxPauseBetweenRetries(5);
            highPriorityQueue.setProcessingStrategy(highPriorityQueueProcessingStrategy);
            queueService.saveQueue(highPriorityQueue);
        }

        Queue sequentialByOriginatorQueue = queueService.findQueueByTenantIdAndName(TenantId.SYS_TENANT_ID, DataConstants.SQ_QUEUE_NAME);
        if (sequentialByOriginatorQueue == null) {
            sequentialByOriginatorQueue = new Queue();
            sequentialByOriginatorQueue.setTenantId(TenantId.SYS_TENANT_ID);
            sequentialByOriginatorQueue.setName(DataConstants.SQ_QUEUE_NAME);
            sequentialByOriginatorQueue.setTopic(DataConstants.SQ_QUEUE_TOPIC);
            sequentialByOriginatorQueue.setPollInterval(25);
            sequentialByOriginatorQueue.setPartitions(10);
            sequentialByOriginatorQueue.setPackProcessingTimeout(2000);
            sequentialByOriginatorQueue.setConsumerPerPartition(true);
            SubmitStrategy sequentialByOriginatorQueueSubmitStrategy = new SubmitStrategy();
            sequentialByOriginatorQueueSubmitStrategy.setType(SubmitStrategyType.SEQUENTIAL_BY_ORIGINATOR);
            sequentialByOriginatorQueueSubmitStrategy.setBatchSize(100);
            sequentialByOriginatorQueue.setSubmitStrategy(sequentialByOriginatorQueueSubmitStrategy);
            ProcessingStrategy sequentialByOriginatorQueueProcessingStrategy = new ProcessingStrategy();
            sequentialByOriginatorQueueProcessingStrategy.setType(ProcessingStrategyType.RETRY_FAILED_AND_TIMED_OUT);
            sequentialByOriginatorQueueProcessingStrategy.setRetries(3);
            sequentialByOriginatorQueueProcessingStrategy.setFailurePercentage(0);
            sequentialByOriginatorQueueProcessingStrategy.setPauseBetweenRetries(5);
            sequentialByOriginatorQueueProcessingStrategy.setMaxPauseBetweenRetries(5);
            sequentialByOriginatorQueue.setProcessingStrategy(sequentialByOriginatorQueueProcessingStrategy);
            queueService.saveQueue(sequentialByOriginatorQueue);
        }
    }

    @Override
    @SneakyThrows
    public void createDefaultNotificationConfigs() {
        log.info("Creating default notification configs for system admin");
        if (notificationTargetService.countNotificationTargetsByTenantId(TenantId.SYS_TENANT_ID) == 0) {
            notificationSettingsService.createDefaultNotificationConfigs(TenantId.SYS_TENANT_ID);
        }
        PageDataIterable<TenantId> tenants = new PageDataIterable<>(tenantService::findTenantsIds, 500);
        ExecutorService executor = Executors.newFixedThreadPool(Math.max(Runtime.getRuntime().availableProcessors(), 4));
        log.info("Creating default notification configs for all tenants");
        AtomicInteger count = new AtomicInteger();
        for (TenantId tenantId : tenants) {
            executor.submit(() -> {
                if (notificationTargetService.countNotificationTargetsByTenantId(tenantId) == 0) {
                    notificationSettingsService.createDefaultNotificationConfigs(tenantId);
                    int n = count.incrementAndGet();
                    if (n % 500 == 0) {
                        log.info("{} tenants processed", n);
                    }
                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
    }

    @Override
    @SneakyThrows
    public void updateDefaultNotificationConfigs(boolean updateTenants) {
        log.info("Updating notification configs...");
        notificationSettingsService.updateDefaultNotificationConfigs(TenantId.SYS_TENANT_ID);

        if (updateTenants) {
            PageDataIterable<TenantId> tenants = new PageDataIterable<>(tenantService::findTenantsIds, 500);
            ExecutorService executor = Executors.newFixedThreadPool(Math.max(Runtime.getRuntime().availableProcessors(), 4));
            AtomicInteger count = new AtomicInteger();
            for (TenantId tenantId : tenants) {
                executor.submit(() -> {
                    notificationSettingsService.updateDefaultNotificationConfigs(tenantId);
                    int n = count.incrementAndGet();
                    if (n % 500 == 0) {
                        log.info("{} tenants processed", n);
                    }
                });
            }
            executor.shutdown();
            executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
        }
    }

    @Override
    public void createDefaultCustomMenu() {
        CustomMenuInfo defaultSystemCustomMenu =
                customMenuService.findDefaultCustomMenuByScope(TenantId.SYS_TENANT_ID, new CustomerId(CustomerId.NULL_UUID), CMScope.SYSTEM);
        if (defaultSystemCustomMenu == null) {
            try {
                customMenuService.createCustomMenu(createDefaultMenu("System default menu", CMScope.SYSTEM), null, false);
            } catch (ThingsboardException e) {
                log.warn("Failed to create System default menu", e);
            }
        }
        CustomMenuInfo defaultTenantCustomMenu =
                customMenuService.findDefaultCustomMenuByScope(TenantId.SYS_TENANT_ID, new CustomerId(CustomerId.NULL_UUID), CMScope.TENANT);
        if (defaultTenantCustomMenu == null) {
            try {
                customMenuService.createCustomMenu(createDefaultMenu("Tenant default menu", CMScope.TENANT), null, false);
            } catch (ThingsboardException e) {
                log.warn("Failed to create Tenant default menu", e);
            }
        }
        CustomMenuInfo defaultCustomerCustomMenu =
                customMenuService.findDefaultCustomMenuByScope(TenantId.SYS_TENANT_ID, new CustomerId(CustomerId.NULL_UUID), CMScope.CUSTOMER);
        if (defaultCustomerCustomMenu == null) {
            try {
                customMenuService.createCustomMenu(createDefaultMenu("Customer default menu", CMScope.CUSTOMER), null, false);
            } catch (ThingsboardException e) {
                log.warn("Failed to create Customer default menu", e);
            }
        }
    }

    private static CustomMenuInfo createDefaultMenu(String name, CMScope scope) {
        CustomMenuInfo customMenuInfo = new CustomMenuInfo();
        customMenuInfo.setTenantId(TenantId.SYS_TENANT_ID);
        customMenuInfo.setCustomerId(new CustomerId(CustomerId.NULL_UUID));
        customMenuInfo.setName(name);
        customMenuInfo.setScope(scope);
        customMenuInfo.setAssigneeType(CMAssigneeType.ALL);
        return customMenuInfo;
    }

}
