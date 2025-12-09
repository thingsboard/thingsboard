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

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.rule.DefaultNotificationRuleRecipientsConfig;
import org.thingsboard.server.common.data.notification.rule.EscalatedNotificationRuleRecipientsConfig;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.rule.NotificationRuleConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.AlarmAssignmentNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.AlarmCommentNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.AlarmNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.AlarmNotificationRuleTriggerConfig.AlarmAction;
import org.thingsboard.server.common.data.notification.rule.trigger.config.ApiUsageLimitNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.DeviceActivityNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.DeviceActivityNotificationRuleTriggerConfig.DeviceEvent;
import org.thingsboard.server.common.data.notification.rule.trigger.config.EdgeCommunicationFailureNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.EdgeConnectionNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.EdgeConnectionNotificationRuleTriggerConfig.EdgeConnectivityEvent;
import org.thingsboard.server.common.data.notification.rule.trigger.config.EntitiesLimitNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.EntityActionNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NewPlatformVersionNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;
import org.thingsboard.server.common.data.notification.rule.trigger.config.RateLimitsNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.ResourcesShortageNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.RuleEngineComponentLifecycleEventNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.TaskProcessingFailureNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.template.DeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.EmailDeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplateConfig;
import org.thingsboard.server.common.data.notification.template.WebDeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;
import static org.thingsboard.common.util.JacksonUtil.newObjectNode;
import static org.thingsboard.server.dao.DaoUtil.toUUIDs;

@Service
@RequiredArgsConstructor
public class DefaultNotifications {

    private static final String YELLOW_COLOR = "#F9D916";
    private static final String RED_COLOR = "#e91a1a";

    public static final DefaultNotification maintenanceWork = DefaultNotification.builder()
            .name("Maintenance work notification")
            .subject("Infrastructure maintenance")
            .text("Maintenance work is scheduled for tomorrow (7:00 a.m. - 9:00 a.m. UTC)")
            .build();

    public static final DefaultNotification entitiesLimitForSysadmin = DefaultNotification.builder()
            .name("Entities count limit notification for sysadmin")
            .type(NotificationType.ENTITIES_LIMIT)
            .subject("${entityType}s limit will be reached soon for tenant ${tenantName}")
            .text("${entityType}s usage: ${currentCount}/${limit} (${percents}%)")
            .icon("warning").color(YELLOW_COLOR)
            .rule(DefaultRule.builder()
                    .name("Entities count limit (sysadmin)")
                    .triggerConfig(EntitiesLimitNotificationRuleTriggerConfig.builder()
                            .entityTypes(null).threshold(0.8f)
                            .build())
                    .description("Send notification to system admins when count of entities of some type reached 80% threshold of the limit for a tenant")
                    .build())
            .build();
    public static final DefaultNotification entitiesLimitForTenant = entitiesLimitForSysadmin.toBuilder()
            .name("Entities count limit notification for tenant")
            .subject("WARNING: ${entityType}s limit will be reached soon")
            .rule(entitiesLimitForSysadmin.getRule().toBuilder()
                    .name("Entities count limit")
                    .description("Send notification to tenant admins when count of entities of some type reached 80% threshold of the limit")
                    .build())
            .build();
    public static final DefaultNotification entitiesLimitIncreaseRequest = DefaultNotification.builder()
            .name("Entities limit increase request")
            .type(NotificationType.ENTITIES_LIMIT_INCREASE_REQUEST)
            .subject("${entityType} limit increase request")
            .text("${userEmail} has reached the maximum number of ${entityType:lowerCase}s allowed and is requesting an increase to the ${entityType:lowerCase} limit.")
            .button("${increaseLimitActionLabel}").link("${increaseLimitLink}")
            .emailTemplate(DefaultEmailTemplate.builder()
                    .subject("${entityType} limit increase request")
                    .body("""
                            <table style="box-sizing: border-box; border-radius: 3px; width: 100%; background-color: #f6f6f6; margin: 0px auto;" cellspacing="0" cellpadding="0" bgcolor="#f6f6f6">
                            <tbody>
                            <tr style="box-sizing: border-box; margin: 0px;">
                            <td style="box-sizing: border-box; vertical-align: middle; margin: 0px; padding: 40px;" align="center" valign="middle">
                            <table style="box-sizing: border-box; border: 1px solid #E0E0E0; border-radius: 3px; margin: 0px; background-color: #ffffff; max-width: 600px !important;" cellspacing="0" cellpadding="0">
                            <tbody>
                            <tr style="box-sizing: border-box; margin: 0px;">
                            <td style="box-sizing: border-box; vertical-align: middle; border-bottom: 1px solid #E0E0E0; margin: 0px; padding: 20px; color: #212121; font-family: Arial; font-size: 20px; line-height: 20px; font-style: normal; font-weight: bold;" valign="middle">${entityType} limit increase request</td>
                            </tr>
                            <tr style="box-sizing: border-box; margin: 0px;">
                            <td style="box-sizing: border-box; vertical-align: top; margin: 0px; padding: 16px 24px; color: #212121; font-family: Arial; font-size: 16px; line-height: 24px; font-weight: 400;" valign="top">${userEmail} has reached the maximum number of ${entityType:lowerCase}s allowed and is requesting an increase to the ${entityType:lowerCase} limit.</td>
                            </tr>
                            <tr style="box-sizing: border-box; margin: 0px;">
                            <td style="box-sizing: border-box; vertical-align: top; margin: 0px; padding: 0 24px 16px 24px; color: #212121; font-family: Arial; font-size: 16px; line-height: 24px; font-weight: 400;" valign="top">
                            <a style="display: inline-block; padding: 10px 16px; border-radius: 4px; background: #106CC8; color: #fff; font-family: Arial; font-size: 14px; line-height: 20px; font-weight: bold; text-decoration: none;" href="${baseUrl}${increaseLimitLink}">${increaseLimitActionLabel}</a>
                            </td>
                            </tr>
                            </tbody>
                            </table>
                            </td>
                            </tr>
                            </tbody>
                            </table>""")
                    .build())
            .build();
    public static final DefaultNotification apiFeatureWarningForSysadmin = DefaultNotification.builder()
            .name("API feature warning notification for sysadmin")
            .type(NotificationType.API_USAGE_LIMIT)
            .subject("${feature} feature will be disabled soon for tenant ${tenantName}")
            .text("Usage: ${currentValue} out of ${limit} ${unitLabel}s")
            .icon("warning").color(YELLOW_COLOR)
            .rule(DefaultRule.builder()
                    .name("API feature warning (sysadmin)")
                    .triggerConfig(ApiUsageLimitNotificationRuleTriggerConfig.builder()
                            .apiFeatures(null)
                            .notifyOn(Set.of(ApiUsageStateValue.WARNING))
                            .build())
                    .description("Send notification to system admins on API feature usage WARNING state for a tenant")
                    .build())
            .build();
    public static final DefaultNotification apiFeatureWarningForTenant = apiFeatureWarningForSysadmin.toBuilder()
            .name("API feature warning notification for tenant")
            .subject("WARNING: ${feature} feature will be disabled soon")
            .rule(apiFeatureWarningForSysadmin.getRule().toBuilder()
                    .name("API feature warning")
                    .description("Send notification to tenant admins on API feature usage WARNING state")
                    .build())
            .build();
    public static final DefaultNotification apiFeatureDisabledForSysadmin = DefaultNotification.builder()
            .name("API feature disabled notification for sysadmin")
            .type(NotificationType.API_USAGE_LIMIT)
            .subject("${feature} feature was disabled for tenant ${tenantName}")
            .text("Used ${currentValue} out of ${limit} ${unitLabel}s")
            .icon("block").color(RED_COLOR)
            .rule(DefaultRule.builder()
                    .name("API feature disabled (sysadmin)")
                    .triggerConfig(ApiUsageLimitNotificationRuleTriggerConfig.builder()
                            .apiFeatures(null)
                            .notifyOn(Set.of(ApiUsageStateValue.DISABLED))
                            .build())
                    .description("Send notification to system admins when API feature is disabled for a tenant")
                    .build())
            .build();
    public static final DefaultNotification apiFeatureDisabledForTenant = apiFeatureDisabledForSysadmin.toBuilder()
            .name("API feature disabled notification for tenant")
            .subject("${feature} feature was disabled")
            .rule(apiFeatureDisabledForSysadmin.getRule().toBuilder()
                    .name("API feature disabled")
                    .description("Send notification to tenant admins when API feature is disabled")
                    .build())
            .build();

    public static final DefaultNotification exceededRateLimits = DefaultNotification.builder()
            .name("Exceeded per-tenant rate limits notification for tenant")
            .type(NotificationType.RATE_LIMITS)
            .subject("Rate limits exceeded")
            .text("Rate limits for ${api} exceeded")
            .icon("block").color(RED_COLOR)
            .rule(DefaultRule.builder()
                    .name("Per-tenant rate limits exceeded")
                    .triggerConfig(RateLimitsNotificationRuleTriggerConfig.builder()
                            .apis(Arrays.stream(LimitedApi.values())
                                    .filter(LimitedApi::isPerTenant)
                                    .filter(api -> api.getLabel() != null)
                                    .collect(Collectors.toSet()))
                            .build())
                    .description("Send notification to tenant admins when some per-tenant rate limit is exceeded")
                    .build())
            .build();
    public static final DefaultNotification exceededPerEntityRateLimits = DefaultNotification.builder()
            .name("Exceeded per-entity rate limits notification for tenant")
            .type(NotificationType.RATE_LIMITS)
            .subject("Rate limits exceeded")
            .text("Rate limits for ${api} exceeded for '${limitLevelEntityName}'")
            .icon("block").color(RED_COLOR)
            .rule(DefaultRule.builder()
                    .name("Per-entity rate limits exceeded")
                    .triggerConfig(RateLimitsNotificationRuleTriggerConfig.builder()
                            .apis(Arrays.stream(LimitedApi.values())
                                    .filter(not(LimitedApi::isPerTenant))
                                    .filter(api -> api.getLabel() != null)
                                    .collect(Collectors.toSet()))
                            .build())
                    .description("Send notification to tenant admins when some per-entity rate limit is exceeded for an entity")
                    .build())
            .build();
    public static final DefaultNotification exceededRateLimitsForSysadmin = exceededRateLimits.toBuilder()
            .name("Exceeded per-tenant rate limits notification for sysadmin")
            .subject("Rate limits exceeded for tenant ${tenantName}")
            .button("Go to tenant").link("/tenants/${tenantId}")
            .rule(exceededRateLimits.getRule().toBuilder()
                    .name("Per-tenant rate limits exceeded (sysadmin)")
                    .description("Send notification to system admins when a tenant exceeds some per-tenant rate limit")
                    .build())
            .build();

    public static final DefaultNotification newPlatformVersion = DefaultNotification.builder()
            .name("New platform version notification")
            .type(NotificationType.NEW_PLATFORM_VERSION)
            .subject("New version <b>${latestVersion}</b> is available")
            .text("Current platform version is ${currentVersion}")
            .button("Open release notes").link("${latestVersionReleaseNotesUrl}")
            .rule(DefaultRule.builder()
                    .name("New platform version")
                    .triggerConfig(new NewPlatformVersionNotificationRuleTriggerConfig())
                    .description("Send notification to system admins when new platform version is available")
                    .build())
            .build();

    public static final DefaultNotification newAlarm = DefaultNotification.builder()
            .name("New alarm notification")
            .type(NotificationType.ALARM)
            .subject("New alarm '${alarmType}'")
            .text("Severity: ${alarmSeverity}, originator: ${alarmOriginatorEntityType} '${alarmOriginatorName}'")
            .icon("notifications").color(null)
            .rule(DefaultRule.builder()
                    .name("New alarm")
                    .triggerConfig(AlarmNotificationRuleTriggerConfig.builder()
                            .alarmTypes(null)
                            .alarmSeverities(null)
                            .notifyOn(Set.of(AlarmAction.CREATED))
                            .build())
                    .description("Send notification to tenant admins when an alarm is created")
                    .build())
            .build();
    public static final DefaultNotification alarmUpdate = DefaultNotification.builder()
            .name("Alarm update notification")
            .type(NotificationType.ALARM)
            .subject("Alarm '${alarmType}' - ${action}")
            .text("Severity: ${alarmSeverity}, originator: ${alarmOriginatorEntityType} '${alarmOriginatorName}'")
            .icon("notifications").color(null)
            .rule(DefaultRule.builder()
                    .name("Alarm update")
                    .triggerConfig(AlarmNotificationRuleTriggerConfig.builder()
                            .alarmTypes(null)
                            .alarmSeverities(null)
                            .notifyOn(Set.of(AlarmAction.SEVERITY_CHANGED, AlarmAction.ACKNOWLEDGED, AlarmAction.CLEARED))
                            .build())
                    .description("Send notification to tenant admins when any alarm is updated or cleared")
                    .build())
            .build();
    public static final DefaultNotification entityAction = DefaultNotification.builder()
            .name("Entity action notification")
            .type(NotificationType.ENTITY_ACTION)
            .subject("${entityType} was ${actionType}")
            .text("${entityType} '${entityName}' was ${actionType} by user ${userEmail}")
            .icon("info").color(null)
            .button("Go to ${entityType:lowerCase}").link("/${entityType:lowerCase}s/${entityId}")
            .rule(DefaultRule.builder()
                    .name("Device created")
                    .triggerConfig(EntityActionNotificationRuleTriggerConfig.builder()
                            .entityTypes(Set.of(EntityType.DEVICE))
                            .created(true)
                            .updated(false)
                            .deleted(false)
                            .build())
                    .description("Send notification to tenant admins when device is created")
                    .build())
            .build();
    public static final DefaultNotification deviceActivity = DefaultNotification.builder()
            .name("Device activity notification")
            .type(NotificationType.DEVICE_ACTIVITY)
            .subject("Device '${deviceName}' became ${eventType}")
            .text("Device '${deviceName}' of type '${deviceType}' is now ${eventType}")
            .icon("info").color(null)
            .button("Go to device").link("/devices/${deviceId}")
            .rule(DefaultRule.builder()
                    .name("Device activity status change")
                    .enabled(false)
                    .triggerConfig(DeviceActivityNotificationRuleTriggerConfig.builder()
                            .devices(null)
                            .deviceProfiles(null)
                            .notifyOn(Set.of(DeviceEvent.ACTIVE, DeviceEvent.INACTIVE))
                            .build())
                    .description("Send notification to tenant admins when any device changes its activity state")
                    .build())
            .build();
    public static final DefaultNotification alarmComment = DefaultNotification.builder()
            .name("Alarm comment notification")
            .type(NotificationType.ALARM_COMMENT)
            .subject("Comment on '${alarmType}' alarm")
            .text("${userEmail} ${action} comment: ${comment}")
            .icon("people").color(null)
            .rule(DefaultRule.builder()
                    .name("Comment on active alarm")
                    .triggerConfig(AlarmCommentNotificationRuleTriggerConfig.builder()
                            .alarmTypes(null)
                            .alarmSeverities(null)
                            .alarmStatuses(Set.of(AlarmSearchStatus.ACTIVE))
                            .onlyUserComments(true)
                            .notifyOnCommentUpdate(false)
                            .build())
                    .description("Send notification to tenant admins when comment is added by user on active alarm")
                    .build())
            .build();
    public static final DefaultNotification alarmAssignment = DefaultNotification.builder()
            .name("Alarm assigned notification")
            .type(NotificationType.ALARM_ASSIGNMENT)
            .subject("Alarm '${alarmType}' (${alarmSeverity}) was assigned to user")
            .text("${userEmail} assigned alarm on ${alarmOriginatorEntityType} '${alarmOriginatorName}' to ${assigneeEmail}")
            .icon("person").color(null)
            .rule(DefaultRule.builder()
                    .name("Alarm assignment")
                    .triggerConfig(AlarmAssignmentNotificationRuleTriggerConfig.builder()
                            .alarmTypes(null)
                            .alarmSeverities(null)
                            .alarmStatuses(null)
                            .notifyOn(Set.of(AlarmAssignmentNotificationRuleTriggerConfig.Action.ASSIGNED))
                            .build())
                    .description("Send notification to user when any alarm was assigned to him")
                    .build())
            .build();
    public static final DefaultNotification ruleEngineComponentLifecycleFailure = DefaultNotification.builder()
            .name("Rule chain/node lifecycle failure notification")
            .type(NotificationType.RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT)
            .subject("${action:capitalize} failure in Rule chain '${ruleChainName}'")
            .text("${componentType} '${componentName}' failed to ${action}")
            .icon("warning").color(null)
            .button("Go to rule chain").link("/ruleChains/${ruleChainId}")
            .rule(DefaultRule.builder()
                    .name("Rule node initialization failure")
                    .triggerConfig(RuleEngineComponentLifecycleEventNotificationRuleTriggerConfig.builder()
                            .ruleChains(null)
                            .ruleChainEvents(Set.of(ComponentLifecycleEvent.STARTED, ComponentLifecycleEvent.UPDATED, ComponentLifecycleEvent.STOPPED))
                            .onlyRuleChainLifecycleFailures(true)
                            .trackRuleNodeEvents(true)
                            .ruleNodeEvents(Set.of(ComponentLifecycleEvent.STARTED, ComponentLifecycleEvent.UPDATED, ComponentLifecycleEvent.STOPPED))
                            .onlyRuleNodeLifecycleFailures(true)
                            .build())
                    .description("Send notification to tenant admins when any Rule chain or Rule node failed to start, update or stop")
                    .build())
            .build();
    public static final DefaultNotification edgeConnection = DefaultNotification.builder()
            .name("Edge connection notification")
            .type(NotificationType.EDGE_CONNECTION)
            .subject("Edge connection status change")
            .text("Edge '${edgeName}' is now ${eventType}")
            .icon("info").color(null)
            .button("Go to Edge").link("/edgeManagement/instances/${edgeId}")
            .rule(DefaultRule.builder()
                    .name("Edge connection status change")
                    .triggerConfig(EdgeConnectionNotificationRuleTriggerConfig.builder()
                            .edges(null)
                            .notifyOn(Set.of(EdgeConnectivityEvent.CONNECTED, EdgeConnectivityEvent.DISCONNECTED))
                            .build())
                    .description("Send notification to tenant admins when the connection status between TB and Edge changes")
                    .build())
            .build();
    public static final DefaultNotification edgeCommunicationFailures = DefaultNotification.builder()
            .name("Edge communication failure notification")
            .type(NotificationType.EDGE_COMMUNICATION_FAILURE)
            .subject("Edge '${edgeName}' communication failure occurred")
            .text("Failure message: '${failureMsg}'")
            .icon("error").color(RED_COLOR)
            .button("Go to Edge").link("/edgeManagement/instances/${edgeId}")
            .rule(DefaultRule.builder()
                    .name("Edge communication failure")
                    .triggerConfig(EdgeCommunicationFailureNotificationRuleTriggerConfig.builder().edges(null).build())
                    .description("Send notification to tenant admins when communication failures occur")
                    .build())
            .build();

    public static final DefaultNotification taskProcessingFailure = DefaultNotification.builder()
            .name("Task processing failure notification")
            .type(NotificationType.TASK_PROCESSING_FAILURE)
            .subject("Failed to process ${taskType}")
            .text("Failed to process ${taskDescription} for tenant ${tenantId}: ${error}")
            .icon("warning").color(YELLOW_COLOR)
            .rule(DefaultRule.builder()
                    .name("Task processing failure")
                    .triggerConfig(TaskProcessingFailureNotificationRuleTriggerConfig.builder().build())
                    .description("Send notification to system admins when task processing fails")
                    .build())
            .build();

    public static final DefaultNotification resourcesShortage = DefaultNotification.builder()
            .name("Resources shortage notification")
            .type(NotificationType.RESOURCES_SHORTAGE)
            .subject("Warning: ${resource} shortage for ${serviceId}")
            .text("${resource} usage is at ${usage}%.")
            .icon("warning")
            .rule(DefaultRule.builder()
                    .name("Resources shortage")
                    .triggerConfig(ResourcesShortageNotificationRuleTriggerConfig.builder().cpuThreshold(0.8f).storageThreshold(0.8f).ramThreshold(0.8f).build())
                    .description("Send notification to system admins on resource shortage")
                    .build())
            .color(RED_COLOR)
            .build();

    private final NotificationTemplateService templateService;
    private final NotificationRuleService ruleService;

    public final void create(TenantId tenantId, DefaultNotification defaultNotification, NotificationTargetId... targets) {
        NotificationTemplate template = defaultNotification.toTemplate();
        template.setTenantId(tenantId);
        template = templateService.saveNotificationTemplate(tenantId, template);

        if (defaultNotification.getRule() != null && targets.length > 0) {
            NotificationRule rule = defaultNotification.toRule(template.getId(), targets);
            rule.setTenantId(tenantId);
            ruleService.saveNotificationRule(tenantId, rule);
        }
    }

    @Data
    @Builder(toBuilder = true)
    public static class DefaultNotification {

        private final String name;
        private final NotificationType type;
        private final String subject;
        private final String text;
        private final String icon;
        private final String color;
        private final String button;
        private final String link;

        private final DefaultEmailTemplate emailTemplate;

        private final DefaultRule rule;

        public NotificationTemplate toTemplate() {
            NotificationTemplate template = new NotificationTemplate();
            template.setName(name);
            template.setNotificationType(type != null ? type : NotificationType.GENERAL);

            NotificationTemplateConfig templateConfig = new NotificationTemplateConfig();
            WebDeliveryMethodNotificationTemplate webTemplate = new WebDeliveryMethodNotificationTemplate();
            webTemplate.setSubject(subject);
            webTemplate.setBody(text);
            ObjectNode additionalConfig = newObjectNode();
            ObjectNode iconConfig = newObjectNode();
            additionalConfig.set("icon", iconConfig);
            ObjectNode buttonConfig = newObjectNode();
            additionalConfig.set("actionButtonConfig", buttonConfig);
            if (icon != null) {
                iconConfig.put("enabled", true)
                        .put("icon", icon)
                        .put("color", color != null ? color : "#757575");
            } else {
                iconConfig.put("enabled", false);
            }
            if (button != null) {
                buttonConfig.put("enabled", true)
                        .put("text", button)
                        .put("linkType", "LINK")
                        .put("link", link);
            } else {
                buttonConfig.put("enabled", false);
            }
            webTemplate.setAdditionalConfig(additionalConfig);
            webTemplate.setEnabled(true);

            Map<NotificationDeliveryMethod, DeliveryMethodNotificationTemplate> deliveryMethodsTemplates = new HashMap<>();
            deliveryMethodsTemplates.put(NotificationDeliveryMethod.WEB, webTemplate);
            if (this.emailTemplate != null) {
                EmailDeliveryMethodNotificationTemplate emailTemplate = new EmailDeliveryMethodNotificationTemplate();
                emailTemplate.setSubject(this.emailTemplate.getSubject());
                emailTemplate.setBody(this.emailTemplate.getBody());
                emailTemplate.setEnabled(true);
                deliveryMethodsTemplates.put(NotificationDeliveryMethod.EMAIL, emailTemplate);
            }
            templateConfig.setDeliveryMethodsTemplates(deliveryMethodsTemplates);
            template.setConfiguration(templateConfig);
            return template;
        }

        public NotificationRule toRule(NotificationTemplateId templateId, NotificationTargetId... targets) {
            DefaultRule defaultRule = this.rule;
            NotificationRule rule = new NotificationRule();
            rule.setName(defaultRule.getName());
            rule.setEnabled(defaultRule.getEnabled() == null || defaultRule.getEnabled());
            rule.setTemplateId(templateId);
            rule.setTriggerType(defaultRule.getTriggerConfig().getTriggerType());
            rule.setTriggerConfig(defaultRule.getTriggerConfig());
            if (rule.getTriggerType() == NotificationRuleTriggerType.ALARM) {
                EscalatedNotificationRuleRecipientsConfig recipientsConfig = new EscalatedNotificationRuleRecipientsConfig();
                recipientsConfig.setTriggerType(rule.getTriggerType());
                recipientsConfig.setEscalationTable(Map.of(0, toUUIDs(List.of(targets))));
                rule.setRecipientsConfig(recipientsConfig);
            } else {
                DefaultNotificationRuleRecipientsConfig recipientsConfig = new DefaultNotificationRuleRecipientsConfig();
                recipientsConfig.setTriggerType(rule.getTriggerType());
                recipientsConfig.setTargets(toUUIDs(List.of(targets)));
                rule.setRecipientsConfig(recipientsConfig);
            }
            NotificationRuleConfig additionalConfig = new NotificationRuleConfig();
            additionalConfig.setDescription(defaultRule.getDescription());
            rule.setAdditionalConfig(additionalConfig);
            return rule;
        }

    }

    @Data
    @Builder(toBuilder = true)
    public static class DefaultEmailTemplate {
        private final String subject;
        private final String body;
    }

    @Data
    @Builder(toBuilder = true)
    public static class DefaultRule {
        private final String name;
        private final Boolean enabled;
        private final NotificationRuleTriggerConfig triggerConfig;
        private final String description;
    }

}
