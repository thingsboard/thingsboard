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
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
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
import org.thingsboard.rule.engine.api.notification.SlackService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.settings.NotificationSettings;
import org.thingsboard.server.common.data.notification.settings.SlackNotificationDeliveryMethodConfig;
import org.thingsboard.server.common.data.notification.targets.slack.SlackConversation;
import org.thingsboard.server.common.data.notification.targets.slack.SlackConversationType;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.notification.NotificationSettingsService;
import org.thingsboard.server.dao.notification.NotificationTemplateService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;

import java.util.List;
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
@RequiredArgsConstructor
@RequestMapping("/api/notification")
public class NotificationTemplateController extends BaseController {

    private final NotificationTemplateService notificationTemplateService;
    private final NotificationSettingsService notificationSettingsService;
    private final SlackService slackService;

    @ApiOperation(value = "Save notification template (saveNotificationTemplate)",
            notes = "Creates or updates notification template." + NEW_LINE +
                    "Here is an example of template to send notification via Web, SMS and Slack:\n" +
                    MARKDOWN_CODE_BLOCK_START +
                    "{\n" +
                    "  \"name\": \"Greetings\",\n" +
                    "  \"notificationType\": \"GENERAL\",\n" +
                    "  \"configuration\": {\n" +
                    "    \"deliveryMethodsTemplates\": {\n" +
                    "      \"WEB\": {\n" +
                    "        \"enabled\": true,\n" +
                    "        \"subject\": \"Greetings\",\n" +
                    "        \"body\": \"Hi there, ${recipientTitle}\",\n" +
                    "        \"additionalConfig\": {\n" +
                    "          \"icon\": {\n" +
                    "            \"enabled\": true,\n" +
                    "            \"icon\": \"back_hand\",\n" +
                    "            \"color\": \"#757575\"\n" +
                    "          },\n" +
                    "          \"actionButtonConfig\": {\n" +
                    "            \"enabled\": false\n" +
                    "          }\n" +
                    "        },\n" +
                    "        \"method\": \"WEB\"\n" +
                    "      },\n" +
                    "      \"SMS\": {\n" +
                    "        \"enabled\": true,\n" +
                    "        \"body\": \"Hi there, ${recipientTitle}\",\n" +
                    "        \"method\": \"SMS\"\n" +
                    "      },\n" +
                    "      \"SLACK\": {\n" +
                    "        \"enabled\": true,\n" +
                    "        \"body\": \"Hi there, @${recipientTitle}\",\n" +
                    "        \"method\": \"SLACK\"\n" +
                    "      }\n" +
                    "    }\n" +
                    "  }\n" +
                    "}" +
                    MARKDOWN_CODE_BLOCK_END +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PostMapping("/template")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public NotificationTemplate saveNotificationTemplate(@RequestBody @Valid NotificationTemplate notificationTemplate) throws Exception {
        notificationTemplate.setTenantId(getTenantId());
        checkEntity(notificationTemplate.getId(), notificationTemplate, NOTIFICATION);
        return doSaveAndLog(EntityType.NOTIFICATION_TEMPLATE, notificationTemplate, notificationTemplateService::saveNotificationTemplate);
    }

    @ApiOperation(value = "Get notification template by id (getNotificationTemplateById)",
            notes = "Fetches notification template by id." +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @GetMapping("/template/{id}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public NotificationTemplate getNotificationTemplateById(@PathVariable UUID id) throws ThingsboardException {
        NotificationTemplateId notificationTemplateId = new NotificationTemplateId(id);
        return checkEntityId(notificationTemplateId, notificationTemplateService::findNotificationTemplateById, Operation.READ);
    }

    @ApiOperation(value = "Get notification templates (getNotificationTemplates)",
            notes = "Returns the page of notification templates owned by sysadmin or tenant." + NEW_LINE +
                    PAGE_DATA_PARAMETERS +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @GetMapping("/templates")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public PageData<NotificationTemplate> getNotificationTemplates(@Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
                                                                   @RequestParam int pageSize,
                                                                   @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
                                                                   @RequestParam int page,
                                                                   @Parameter(description = "Case-insensitive 'substring' filter based on template's name and notification type")
                                                                   @RequestParam(required = false) String textSearch,
                                                                   @Parameter(description = SORT_PROPERTY_DESCRIPTION)
                                                                   @RequestParam(required = false) String sortProperty,
                                                                   @Parameter(description = SORT_ORDER_DESCRIPTION)
                                                                   @RequestParam(required = false) String sortOrder,
                                                                   @Parameter(description = "Comma-separated list of notification types to filter the templates", array = @ArraySchema(schema = @Schema(type = "string")))
                                                                   @RequestParam(required = false) NotificationType[] notificationTypes,
                                                                   @AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
        // PE: generic permission
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        if (notificationTypes == null || notificationTypes.length == 0) {
            notificationTypes = NotificationType.values();
        }
        return notificationTemplateService.findNotificationTemplatesByTenantIdAndNotificationTypes(user.getTenantId(),
                List.of(notificationTypes), pageLink);
    }

    @ApiOperation(value = "Delete notification template by id (deleteNotificationTemplateById",
            notes = "Deletes notification template by its id." + NEW_LINE +
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
            notes = "List available Slack conversations by type." +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @GetMapping("/slack/conversations")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public List<SlackConversation> listSlackConversations(@RequestParam SlackConversationType type,
                                                          @Parameter(description = "Slack bot token. If absent - system Slack settings will be used")
                                                          @RequestParam(required = false) String token,
                                                          @AuthenticationPrincipal SecurityUser user) {
        // PE: generic permission
        if (StringUtils.isEmpty(token)) {
            NotificationSettings settings = notificationSettingsService.findNotificationSettings(user.getTenantId());
            SlackNotificationDeliveryMethodConfig slackConfig = (SlackNotificationDeliveryMethodConfig)
                    settings.getDeliveryMethodsConfigs().get(NotificationDeliveryMethod.SLACK);
            if (slackConfig == null) {
                throw new IllegalArgumentException("Slack is not configured");
            }
            token = slackConfig.getBotToken();
        }

        return slackService.listConversations(user.getTenantId(), token, type);
    }

}
