/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
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
import org.thingsboard.rule.engine.api.slack.SlackService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.settings.NotificationSettings;
import org.thingsboard.server.common.data.notification.settings.SlackNotificationDeliveryMethodConfig;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.notification.template.SlackConversation;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.notification.NotificationSettingsService;
import org.thingsboard.server.dao.notification.NotificationTemplateService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH;

@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api/notification")
public class NotificationTemplateController extends BaseController {

    private final NotificationTemplateService notificationTemplateService;
    private final NotificationSettingsService notificationSettingsService;
    private final SlackService slackService;

    @ApiOperation(value = "Save notification template (saveNotificationTemplate)",
            notes = "Create or update notification template.\n\n" +
                    "Example:\n" +
                    "```\n{\n  \"name\": \"Hello to all my users\",\n" +
                    "  \"notificationType\": \"Message from administrator\",\n" +
                    "  \"configuration\": {\n" +
                    "    \"defaultTextTemplate\": \"Hello everyone\",  # required if any of the templates' bodies is not set\n" +
                    "    \"templates\": {\n" +
                    "      \"PUSH\": {\n        \"method\": \"PUSH\",\n        \"body\": null  # defaultTextTemplate will be used if body is not set\n      },\n" +
                    "      \"SMS\": {\n        \"method\": \"SMS\",\n        \"body\": null\n      },\n" +
                    "      \"EMAIL\": {\n        \"method\": \"EMAIL\",\n        \"body\": \"Non-default value for email notification: <body>Hello everyone</body>\",\n        \"subject\": \"Message from administrator\"\n      },\n" +
                    "      \"SLACK\": {\n        \"method\": \"SLACK\",\n        \"body\": null,\n        \"conversationType\": \"PUBLIC_CHANNEL\",\n        \"conversationId\": \"U02LD7BJOU2\"  # received from listSlackConversations API method\n      }\n" +
                    "    }\n" +
                    "  }\n}\n```" +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PostMapping("/template")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public NotificationTemplate saveNotificationTemplate(@RequestBody @Valid NotificationTemplate notificationTemplate) throws Exception {
        notificationTemplate.setTenantId(getTenantId());
        checkEntity(notificationTemplate.getId(), notificationTemplate, Resource.NOTIFICATION_TEMPLATE);
        return doSaveAndLog(EntityType.NOTIFICATION_TEMPLATE, notificationTemplate, notificationTemplateService::saveNotificationTemplate);
    }

    @ApiOperation(value = "Get notification template by id (getNotificationTemplateById)",
            notes = "Fetch notification template by id." +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @GetMapping("/template/{id}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public NotificationTemplate getNotificationTemplateById(@PathVariable UUID id) throws ThingsboardException {
        NotificationTemplateId notificationTemplateId = new NotificationTemplateId(id);
        return checkEntityId(notificationTemplateId, notificationTemplateService::findNotificationTemplateById, Operation.READ);
    }

    @ApiOperation(value = "Get notification templates (getNotificationTemplates)",
            notes = "Fetch the page of notification templates owned by sysadmin or tenant." +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @GetMapping("/templates")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public PageData<NotificationTemplate> getNotificationTemplates(@RequestParam int pageSize,
                                                                   @RequestParam int page,
                                                                   @RequestParam(required = false) String textSearch,
                                                                   @RequestParam(required = false) String sortProperty,
                                                                   @RequestParam(required = false) String sortOrder,
                                                                   @AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return notificationTemplateService.findNotificationTemplatesByTenantId(user.getTenantId(), pageLink);
    }

    @ApiOperation(value = "Delete notification template by id (deleteNotificationTemplateById",
            notes = "Delete notification template by its id.\n\n" +
                    "This template cannot be referenced by existing scheduled notification requests or any notification rules." +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @DeleteMapping("/template/{id}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public void deleteNotificationTemplateById(@PathVariable UUID id) throws Exception {
        NotificationTemplateId notificationTemplateId = new NotificationTemplateId(id);
        NotificationTemplate notificationTemplate = checkEntityId(notificationTemplateId, notificationTemplateService::findNotificationTemplateById, Operation.DELETE);
        doDeleteAndLog(EntityType.NOTIFICATION_TEMPLATE, notificationTemplate, notificationTemplateService::deleteNotificationTemplateById);
    }

    @ApiOperation(value = "List Slack conversations (listSlackConversations)",
            notes = "List available Slack conversations by type to use in notification template.\n\n" +
                    "Slack must be configured in notification settings." +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @GetMapping("/slack/conversations")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public List<SlackConversation> listSlackConversations(@RequestParam SlackConversation.Type type,
                                                          @AuthenticationPrincipal SecurityUser user) {
        NotificationSettings settings = notificationSettingsService.findNotificationSettings(user.getTenantId());
        SlackNotificationDeliveryMethodConfig slackConfig = (SlackNotificationDeliveryMethodConfig)
                settings.getDeliveryMethodsConfigs().get(NotificationDeliveryMethod.SLACK);
        if (slackConfig == null) {
            throw new IllegalArgumentException("Slack is not configured");
        }

        return slackService.listConversations(user.getTenantId(), slackConfig.getBotToken(), type);
    }

}
