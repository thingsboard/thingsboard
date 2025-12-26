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
package org.thingsboard.server.dao.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.settings.NotificationSettings;
import org.thingsboard.server.common.data.notification.settings.UserNotificationSettings;
import org.thingsboard.server.common.data.notification.settings.UserNotificationSettings.NotificationPref;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.platform.AffectedTenantAdministratorsFilter;
import org.thingsboard.server.common.data.notification.targets.platform.AffectedUserFilter;
import org.thingsboard.server.common.data.notification.targets.platform.AllUsersFilter;
import org.thingsboard.server.common.data.notification.targets.platform.OriginatorEntityOwnerUsersFilter;
import org.thingsboard.server.common.data.notification.targets.platform.PlatformUsersNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.platform.SystemAdministratorsFilter;
import org.thingsboard.server.common.data.notification.targets.platform.TenantAdministratorsFilter;
import org.thingsboard.server.common.data.notification.targets.platform.UsersFilter;
import org.thingsboard.server.common.data.notification.targets.platform.UsersFilterType;
import org.thingsboard.server.common.data.notification.template.EmailDeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplateConfig;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.settings.UserSettings;
import org.thingsboard.server.common.data.settings.UserSettingsType;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.user.UserSettingsService;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultNotificationSettingsService implements NotificationSettingsService {

    private final AdminSettingsService adminSettingsService;
    private final NotificationTargetService notificationTargetService;
    private final NotificationTemplateService notificationTemplateService;
    private final DefaultNotifications defaultNotifications;
    private final UserSettingsService userSettingsService;

    private static final String SETTINGS_KEY = "notifications";

    @CacheEvict(cacheNames = CacheConstants.NOTIFICATION_SETTINGS_CACHE, key = "#tenantId")
    @Override
    public void saveNotificationSettings(TenantId tenantId, NotificationSettings settings) {
        if (!tenantId.isSysTenantId() && settings.getDeliveryMethodsConfigs().containsKey(NotificationDeliveryMethod.MOBILE_APP)) {
            throw new IllegalArgumentException("Mobile settings can only be configured by system administrator");
        }
        AdminSettings adminSettings = Optional.ofNullable(adminSettingsService.findAdminSettingsByTenantIdAndKey(tenantId, SETTINGS_KEY))
                .orElseGet(() -> {
                    AdminSettings newAdminSettings = new AdminSettings();
                    newAdminSettings.setTenantId(tenantId);
                    newAdminSettings.setKey(SETTINGS_KEY);
                    return newAdminSettings;
                });
        adminSettings.setJsonValue(JacksonUtil.valueToTree(settings));
        adminSettingsService.saveAdminSettings(tenantId, adminSettings);
    }

    @Cacheable(cacheNames = CacheConstants.NOTIFICATION_SETTINGS_CACHE, key = "#tenantId")
    @Override
    public NotificationSettings findNotificationSettings(TenantId tenantId) {
        return Optional.ofNullable(adminSettingsService.findAdminSettingsByTenantIdAndKey(tenantId, SETTINGS_KEY))
                .map(adminSettings -> JacksonUtil.treeToValue(adminSettings.getJsonValue(), NotificationSettings.class))
                .orElseGet(() -> {
                    NotificationSettings settings = new NotificationSettings();
                    settings.setDeliveryMethodsConfigs(Collections.emptyMap());
                    return settings;
                });
    }

    @CacheEvict(cacheNames = CacheConstants.NOTIFICATION_SETTINGS_CACHE, key = "#tenantId")
    @Override
    public void deleteNotificationSettings(TenantId tenantId) {
        adminSettingsService.deleteAdminSettingsByTenantIdAndKey(tenantId, SETTINGS_KEY);
    }

    @Override
    public UserNotificationSettings saveUserNotificationSettings(TenantId tenantId, UserId userId, UserNotificationSettings settings) {
        UserSettings userSettings = new UserSettings();
        userSettings.setUserId(userId);
        userSettings.setType(UserSettingsType.NOTIFICATIONS);
        userSettings.setSettings(JacksonUtil.valueToTree(settings));
        userSettingsService.saveUserSettings(tenantId, userSettings);
        return formatUserNotificationSettings(settings);
    }

    @Override
    public UserNotificationSettings getUserNotificationSettings(TenantId tenantId, UserId userId, boolean format) {
        UserSettings userSettings = userSettingsService.findUserSettings(tenantId, userId, UserSettingsType.NOTIFICATIONS);
        UserNotificationSettings settings = null;
        if (userSettings != null) {
            try {
                settings = JacksonUtil.treeToValue(userSettings.getSettings(), UserNotificationSettings.class);
            } catch (Exception e) {
                log.warn("Failed to parse notification settings for user {}", userId, e);
            }
        }
        if (settings == null) {
            settings = UserNotificationSettings.DEFAULT;
        }
        if (format) {
            settings = formatUserNotificationSettings(settings);
        }
        return settings;
    }

    private UserNotificationSettings formatUserNotificationSettings(UserNotificationSettings settings) {
        Map<NotificationType, NotificationPref> prefs = new EnumMap<>(NotificationType.class);
        if (settings != null) {
            prefs.putAll(settings.getPrefs());
        }
        NotificationPref defaultPref = NotificationPref.createDefault();
        for (NotificationType notificationType : NotificationType.values()) {
            NotificationPref pref = prefs.get(notificationType);
            if (pref == null) {
                prefs.put(notificationType, defaultPref);
            } else {
                var enabledDeliveryMethods = new LinkedHashMap<>(pref.getEnabledDeliveryMethods());
                // in case a new delivery method was added to the platform
                UserNotificationSettings.deliveryMethods.forEach(deliveryMethod -> {
                    enabledDeliveryMethods.putIfAbsent(deliveryMethod, true);
                });
                pref.setEnabledDeliveryMethods(enabledDeliveryMethods);
            }
        }
        return new UserNotificationSettings(prefs);
    }

    @Transactional
    @Override
    public void createDefaultNotificationConfigs(TenantId tenantId) {
        NotificationTarget allUsers = createTarget(tenantId, "All users", new AllUsersFilter(),
                tenantId.isSysTenantId() ? "All platform users" : "All users in scope of the tenant");
        NotificationTarget tenantAdmins = createTarget(tenantId, "Tenant administrators", new TenantAdministratorsFilter(),
                tenantId.isSysTenantId() ? "All tenant administrators" : "Tenant administrators");

        defaultNotifications.create(tenantId, DefaultNotifications.maintenanceWork);

        if (tenantId.isSysTenantId()) {
            NotificationTarget sysAdmins = createTarget(tenantId, "System administrators", new SystemAdministratorsFilter(), "All system administrators");
            NotificationTarget affectedTenantAdmins = createTarget(tenantId, "Affected tenant's administrators", new AffectedTenantAdministratorsFilter(), "");

            defaultNotifications.create(tenantId, DefaultNotifications.entitiesLimitForSysadmin, sysAdmins.getId());
            defaultNotifications.create(tenantId, DefaultNotifications.entitiesLimitForTenant, affectedTenantAdmins.getId());
            defaultNotifications.create(tenantId, DefaultNotifications.entitiesLimitIncreaseRequest, sysAdmins.getId());
            defaultNotifications.create(tenantId, DefaultNotifications.apiFeatureWarningForSysadmin, sysAdmins.getId());
            defaultNotifications.create(tenantId, DefaultNotifications.apiFeatureWarningForTenant, affectedTenantAdmins.getId());
            defaultNotifications.create(tenantId, DefaultNotifications.apiFeatureDisabledForSysadmin, sysAdmins.getId());
            defaultNotifications.create(tenantId, DefaultNotifications.apiFeatureDisabledForTenant, affectedTenantAdmins.getId());
            defaultNotifications.create(tenantId, DefaultNotifications.exceededRateLimits, affectedTenantAdmins.getId());
            defaultNotifications.create(tenantId, DefaultNotifications.exceededPerEntityRateLimits, affectedTenantAdmins.getId());
            defaultNotifications.create(tenantId, DefaultNotifications.exceededRateLimitsForSysadmin, sysAdmins.getId());
            defaultNotifications.create(tenantId, DefaultNotifications.newPlatformVersion, sysAdmins.getId());
            defaultNotifications.create(tenantId, DefaultNotifications.taskProcessingFailure, tenantAdmins.getId());
            defaultNotifications.create(tenantId, DefaultNotifications.resourcesShortage, sysAdmins.getId());
            return;
        }

        NotificationTarget originatorEntityOwnerUsers = createTarget(tenantId, "Users of the entity owner", new OriginatorEntityOwnerUsersFilter(),
                "In case trigger entity (e.g. created device or alarm) is owned by customer, then recipients are this customer's users, otherwise tenant admins");
        NotificationTarget affectedUser = createTarget(tenantId, "Affected user", new AffectedUserFilter(),
                "If rule trigger is an action that affects some user (e.g. alarm assigned to user) - this user");

        defaultNotifications.create(tenantId, DefaultNotifications.newAlarm, tenantAdmins.getId());
        defaultNotifications.create(tenantId, DefaultNotifications.alarmUpdate, tenantAdmins.getId());
        defaultNotifications.create(tenantId, DefaultNotifications.entityAction, tenantAdmins.getId());
        defaultNotifications.create(tenantId, DefaultNotifications.deviceActivity, tenantAdmins.getId());
        defaultNotifications.create(tenantId, DefaultNotifications.alarmComment, tenantAdmins.getId());
        defaultNotifications.create(tenantId, DefaultNotifications.alarmAssignment, affectedUser.getId());
        defaultNotifications.create(tenantId, DefaultNotifications.ruleEngineComponentLifecycleFailure, tenantAdmins.getId());
        defaultNotifications.create(tenantId, DefaultNotifications.edgeConnection, tenantAdmins.getId());
        defaultNotifications.create(tenantId, DefaultNotifications.edgeCommunicationFailures, tenantAdmins.getId());
    }

    @Override
    public void updateDefaultNotificationConfigs(TenantId tenantId) {
        if (tenantId.isSysTenantId()) {
            NotificationTarget sysAdmins = notificationTargetService.findNotificationTargetsByTenantIdAndUsersFilterType(tenantId, UsersFilterType.SYSTEM_ADMINISTRATORS).stream()
                    .findFirst().orElseGet(() -> createTarget(tenantId, "System administrators", new SystemAdministratorsFilter(), "All system administrators"));
            NotificationTarget affectedTenantAdmins = notificationTargetService.findNotificationTargetsByTenantIdAndUsersFilterType(tenantId, UsersFilterType.AFFECTED_TENANT_ADMINISTRATORS).stream()
                    .findFirst().orElseGet(() -> createTarget(tenantId, "Affected tenant's administrators", new AffectedTenantAdministratorsFilter(), ""));

            if (!isNotificationConfigured(tenantId, NotificationType.RATE_LIMITS)) {
                defaultNotifications.create(tenantId, DefaultNotifications.exceededRateLimits, affectedTenantAdmins.getId());
                defaultNotifications.create(tenantId, DefaultNotifications.exceededPerEntityRateLimits, affectedTenantAdmins.getId());
                defaultNotifications.create(tenantId, DefaultNotifications.exceededRateLimitsForSysadmin, sysAdmins.getId());
            }
            if (!isNotificationConfigured(tenantId, NotificationType.TASK_PROCESSING_FAILURE)) {
                defaultNotifications.create(tenantId, DefaultNotifications.taskProcessingFailure, sysAdmins.getId());
            }
            if (!isNotificationConfigured(tenantId, NotificationType.RESOURCES_SHORTAGE)) {
                defaultNotifications.create(tenantId, DefaultNotifications.resourcesShortage, sysAdmins.getId());
            }
            if (!isNotificationConfigured(tenantId, NotificationType.ENTITIES_LIMIT_INCREASE_REQUEST)) {
                defaultNotifications.create(tenantId, DefaultNotifications.entitiesLimitIncreaseRequest, sysAdmins.getId());
            }
        } else {
            var requiredNotificationTypes = List.of(NotificationType.EDGE_CONNECTION, NotificationType.EDGE_COMMUNICATION_FAILURE);
            var existingNotificationTypes = notificationTemplateService.findNotificationTemplatesByTenantIdAndNotificationTypes(
                            tenantId, requiredNotificationTypes, new PageLink(2))
                    .getData()
                    .stream()
                    .map(NotificationTemplate::getNotificationType)
                    .collect(Collectors.toSet());

            if (existingNotificationTypes.containsAll(requiredNotificationTypes)) {
                return;
            }

            NotificationTarget tenantAdmins = notificationTargetService.findNotificationTargetsByTenantIdAndUsersFilterType(tenantId, UsersFilterType.TENANT_ADMINISTRATORS)
                    .stream()
                    .findFirst()
                    .orElseGet(() -> createTarget(tenantId, "Tenant administrators", new TenantAdministratorsFilter(), "Tenant administrators"));

            for (NotificationType type : requiredNotificationTypes) {
                if (!existingNotificationTypes.contains(type)) {
                    switch (type) {
                        case EDGE_CONNECTION:
                            defaultNotifications.create(tenantId, DefaultNotifications.edgeConnection, tenantAdmins.getId());
                            break;
                        case EDGE_COMMUNICATION_FAILURE:
                            defaultNotifications.create(tenantId, DefaultNotifications.edgeCommunicationFailures, tenantAdmins.getId());
                            break;
                    }
                }
            }
        }
    }

    @Override
    public void moveMailTemplatesToNotificationCenter(TenantId tenantId, JsonNode mailTemplates, Map<String, NotificationType> mailTemplatesNames) {
        mailTemplatesNames.forEach((mailTemplateName, notificationType) -> {
            moveMailTemplateToNotificationCenter(tenantId, mailTemplates, mailTemplateName, notificationType);
        });
    }

    private void moveMailTemplateToNotificationCenter(TenantId tenantId, JsonNode mailTemplates, String mailTemplateName, NotificationType notificationType) {
        JsonNode mailTemplate = mailTemplates.get(mailTemplateName);
        if (mailTemplate == null || mailTemplate.isNull() || !mailTemplate.has("subject") || !mailTemplate.has("body")) {
            return;
        }

        String subject = mailTemplate.get("subject").asText();
        String body = mailTemplate.get("body").asText();
        body = body.replace("targetEmail", "recipientEmail");

        NotificationTemplate notificationTemplate = null;
        if (tenantId.isSysTenantId()) {
            // updating system notification template, not touching tenants' templates
            notificationTemplate = notificationTemplateService.findNotificationTemplateByTenantIdAndType(tenantId, notificationType).orElse(null);
        }
        if (notificationTemplate == null) {
            log.debug("[{}] Creating {} template", tenantId, notificationType);
            notificationTemplate = new NotificationTemplate();
        } else {
            log.debug("[{}] Updating {} template", tenantId, notificationType);
        }
        String name = StringUtils.capitalize(notificationType.name().toLowerCase().replaceAll("_", " ")) + " notification";
        notificationTemplate.setName(name);
        notificationTemplate.setTenantId(tenantId);
        notificationTemplate.setNotificationType(notificationType);
        NotificationTemplateConfig notificationTemplateConfig = new NotificationTemplateConfig();

        EmailDeliveryMethodNotificationTemplate emailNotificationTemplate = new EmailDeliveryMethodNotificationTemplate();
        emailNotificationTemplate.setEnabled(true);
        emailNotificationTemplate.setSubject(subject);
        emailNotificationTemplate.setBody(body);

        notificationTemplateConfig.setDeliveryMethodsTemplates(Map.of(NotificationDeliveryMethod.EMAIL, emailNotificationTemplate));
        notificationTemplate.setConfiguration(notificationTemplateConfig);
        notificationTemplateService.saveNotificationTemplate(tenantId, notificationTemplate);

        ((ObjectNode) mailTemplates).remove(mailTemplateName);
    }

    private boolean isNotificationConfigured(TenantId tenantId, NotificationType... notificationTypes) {
        return notificationTemplateService.countNotificationTemplatesByTenantIdAndNotificationTypes(tenantId, List.of(notificationTypes)) > 0;
    }

    private NotificationTarget createTarget(TenantId tenantId, String name, UsersFilter filter, String description) {
        NotificationTarget target = new NotificationTarget();
        target.setTenantId(tenantId);
        target.setName(name);

        PlatformUsersNotificationTargetConfig targetConfig = new PlatformUsersNotificationTargetConfig();
        targetConfig.setUsersFilter(filter);
        targetConfig.setDescription(description);
        target.setConfiguration(targetConfig);
        return notificationTargetService.saveNotificationTarget(tenantId, target);
    }

}
