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

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.rule.NotificationRuleInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.notification.NotificationRuleService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;

import java.util.UUID;

import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_END;
import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_START;
import static org.thingsboard.server.controller.ControllerConstants.NEW_LINE;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.service.security.permission.Resource.NOTIFICATION;

@RestController
@TbCoreComponent
@RequestMapping("/api/notification")
@RequiredArgsConstructor
@Slf4j
public class NotificationRuleController extends BaseController {

    private final NotificationRuleService notificationRuleService;

    @ApiOperation(value = "Save notification rule (saveNotificationRule)",
            notes = "Creates or updates notification rule. " + NEW_LINE +
                    "Mandatory properties are `name`, `templateId` (of a template with `notificationType` matching to rule's `triggerType`), " +
                    "`triggerType`, `triggerConfig` and `recipientConfig`. Additionally, you may specify rule `description` " +
                    "inside of `additionalConfig`." + NEW_LINE +
                    "Trigger type of the rule cannot be changed. " +
                    "Available trigger types for tenant: `ENTITY_ACTION`, `ALARM`, `ALARM_COMMENT`, `ALARM_ASSIGNMENT`, " +
                    "`DEVICE_ACTIVITY`, `RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT`.\n" +
                    "For sysadmin, there are following trigger types available: `ENTITIES_LIMIT`, `API_USAGE_LIMIT`, " +
                    "`NEW_PLATFORM_VERSION`." + NEW_LINE +
                    "Here is an example of notification rule to send notification when a " +
                    "device, asset or customer is created or deleted:\n" +
                    MARKDOWN_CODE_BLOCK_START +
                    "{\n" +
                    "  \"name\": \"Entity action\",\n" +
                    "  \"templateId\": {\n" +
                    "    \"entityType\": \"NOTIFICATION_TEMPLATE\",\n" +
                    "    \"id\": \"32117320-d785-11ed-a06c-21dd57dd88ca\"\n" +
                    "  },\n" +
                    "  \"triggerType\": \"ENTITY_ACTION\",\n" +
                    "  \"triggerConfig\": {\n" +
                    "    \"entityTypes\": [\n" +
                    "      \"CUSTOMER\",\n" +
                    "      \"DEVICE\",\n" +
                    "      \"ASSET\"\n" +
                    "    ],\n" +
                    "    \"created\": true,\n" +
                    "    \"updated\": false,\n" +
                    "    \"deleted\": true,\n" +
                    "    \"triggerType\": \"ENTITY_ACTION\"\n" +
                    "  },\n" +
                    "  \"recipientsConfig\": {\n" +
                    "    \"targets\": [\n" +
                    "      \"320f2930-d785-11ed-a06c-21dd57dd88ca\"\n" +
                    "    ],\n" +
                    "    \"triggerType\": \"ENTITY_ACTION\"\n" +
                    "  },\n" +
                    "  \"additionalConfig\": {\n" +
                    "    \"description\": \"Send notification to tenant admins or customer users when a device, asset or customer is created\"\n" +
                    "  },\n" +
                    "  \"templateName\": \"Entity action notification\",\n" +
                    "  \"deliveryMethods\": [\n" +
                    "    \"WEB\"\n" +
                    "  ]\n" +
                    "}" +
                    MARKDOWN_CODE_BLOCK_END +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PostMapping("/rule")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public NotificationRule saveNotificationRule(@RequestBody @Valid NotificationRule notificationRule,
                                                 @AuthenticationPrincipal SecurityUser user) throws Exception {
        notificationRule.setTenantId(user.getTenantId());
        checkEntity(notificationRule.getId(), notificationRule, NOTIFICATION);

        NotificationRuleTriggerType triggerType = notificationRule.getTriggerType();
        if ((user.isTenantAdmin() && !triggerType.isTenantLevel()) || (user.isSystemAdmin() && triggerType.isTenantLevel())) {
            throw new IllegalArgumentException("Trigger type " + triggerType + " is not available");
        }

        return doSaveAndLog(EntityType.NOTIFICATION_RULE, notificationRule, notificationRuleService::saveNotificationRule);
    }

    @ApiOperation(value = "Get notification rule by id (getNotificationRuleById)",
            notes = "Fetches notification rule info by rule's id.\n" +
                    "In addition to regular notification rule fields, " +
                    "there are `templateName` and `deliveryMethods` in the response." +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @GetMapping("/rule/{id}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public NotificationRuleInfo getNotificationRuleById(@PathVariable UUID id) throws ThingsboardException {
        NotificationRuleId notificationRuleId = new NotificationRuleId(id);
        return checkEntityId(notificationRuleId, notificationRuleService::findNotificationRuleInfoById, Operation.READ);
    }

    @ApiOperation(value = "Get notification rules (getNotificationRules)",
            notes = "Returns the page of notification rules." + NEW_LINE +
                    PAGE_DATA_PARAMETERS +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @GetMapping("/rules")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public PageData<NotificationRuleInfo> getNotificationRules(@Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
                                                               @RequestParam int pageSize,
                                                               @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
                                                               @RequestParam int page,
                                                               @Parameter(description = "Case-insensitive 'substring' filter based on rule's name")
                                                               @RequestParam(required = false) String textSearch,
                                                               @Parameter(description = SORT_PROPERTY_DESCRIPTION)
                                                               @RequestParam(required = false) String sortProperty,
                                                               @Parameter(description = SORT_ORDER_DESCRIPTION)
                                                               @RequestParam(required = false) String sortOrder,
                                                               @AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
        // PE: generic permission
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return notificationRuleService.findNotificationRulesInfosByTenantId(user.getTenantId(), pageLink);
    }

    @ApiOperation(value = "Delete notification rule (deleteNotificationRule)",
            notes = "Deletes notification rule by id.\n" +
                    "Cancels all related scheduled notification requests (e.g. due to escalation table)" +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @DeleteMapping("/rule/{id}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public void deleteNotificationRule(@PathVariable UUID id,
                                       @AuthenticationPrincipal SecurityUser user) throws Exception {
        NotificationRuleId notificationRuleId = new NotificationRuleId(id);
        NotificationRule notificationRule = checkEntityId(notificationRuleId, notificationRuleService::findNotificationRuleById, Operation.DELETE);
        doDeleteAndLog(EntityType.NOTIFICATION_RULE, notificationRule, notificationRuleService::deleteNotificationRuleById);
    }

}
