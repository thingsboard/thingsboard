/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.rule.engine.api.NotificationCenter;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.NotificationId;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestInfo;
import org.thingsboard.server.common.data.notification.NotificationRequestPreview;
import org.thingsboard.server.common.data.notification.settings.NotificationSettings;
import org.thingsboard.server.common.data.notification.targets.NotificationRecipient;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.NotificationTargetType;
import org.thingsboard.server.common.data.notification.targets.platform.PlatformUsersNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.slack.SlackNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.template.DeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.notification.NotificationRequestService;
import org.thingsboard.server.dao.notification.NotificationService;
import org.thingsboard.server.dao.notification.NotificationSettingsService;
import org.thingsboard.server.dao.notification.NotificationTargetService;
import org.thingsboard.server.dao.notification.NotificationTemplateService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.notification.NotificationProcessingContext;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.thingsboard.server.service.security.permission.Resource.NOTIFICATION;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class NotificationController extends BaseController {

    private final NotificationService notificationService;
    private final NotificationRequestService notificationRequestService;
    private final NotificationTemplateService notificationTemplateService;
    private final NotificationTargetService notificationTargetService;
    private final NotificationCenter notificationCenter;
    private final NotificationSettingsService notificationSettingsService;

    @ApiOperation(value = "Get notifications (getNotifications)",
            notes = "**WebSocket API**:\n\n" +
                    "There are 2 types of subscriptions: one for unread notifications count, another for unread notifications themselves.\n\n" +
                    "The URI for opening WS session for notifications: `/api/ws/plugins/notifications`.\n\n" +
                    "Subscription command for unread notifications count:\n" +
                    "```\n{\n  \"unreadCountSubCmd\": {\n    \"cmdId\": 1234\n  }\n}\n```\n" +
                    "To subscribe for latest unread notifications:\n" +
                    "```\n{\n  \"unreadSubCmd\": {\n    \"cmdId\": 1234,\n    \"limit\": 10\n  }\n}\n```\n" +
                    "To unsubscribe from any subscription:\n" +
                    "```\n{\n  \"unsubCmd\": {\n    \"cmdId\": 1234\n  }\n}\n```\n" +
                    "To mark certain notifications as read, use following command:\n" +
                    "```\n{\n  \"markAsReadCmd\": {\n    \"cmdId\": 1234,\n    \"notifications\": [\n      \"6f860330-7fc2-11ed-b855-7dd3b7d2faa9\",\n      \"5b6dfee0-8d0d-11ed-b61f-35a57b03dade\"\n    ]\n  }\n}\n\n```\n" +
                    "To mark all notifications as read:\n" +
                    "```\n{\n  \"markAllAsReadCmd\": {\n    \"cmdId\": 1234\n  }\n}\n```\n" +
                    "\n\n" +
                    "Update structure for unread **notifications count subscription**:\n" +
                    "```\n{\n  \"cmdId\": 1234,\n  \"totalUnreadCount\": 55\n}\n```\n" +
                    "For **notifications subscription**:\n" +
                    "- full update of latest unread notifications:\n" +
                    "```\n{\n" +
                    "  \"cmdId\": 1234,\n" +
                    "  \"notifications\": [\n" +
                    "    {\n" +
                    "      \"id\": {\n" +
                    "        \"entityType\": \"NOTIFICATION\",\n" +
                    "        \"id\": \"6f860330-7fc2-11ed-b855-7dd3b7d2faa9\"\n" +
                    "      },\n" +
                    "      ...\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"totalUnreadCount\": 1\n" +
                    "}\n```\n" +
                    "- when new notification arrives or shown notification is updated:\n" +
                    "```\n{\n" +
                    "  \"cmdId\": 1234,\n" +
                    "  \"update\": {\n" +
                    "    \"id\": {\n" +
                    "      \"entityType\": \"NOTIFICATION\",\n" +
                    "      \"id\": \"6f860330-7fc2-11ed-b855-7dd3b7d2faa9\"\n" +
                    "    },\n" +
                    "    # updated notification info, text, subject etc.\n" +
                    "    ...\n" +
                    "  },\n" +
                    "  \"totalUnreadCount\": 2\n" +
                    "}\n```\n" +
                    "- when unread notifications count changes:\n" +
                    "```\n{\n" +
                    "  \"cmdId\": 1234,\n" +
                    "  \"totalUnreadCount\": 5\n" +
                    "}\n```")
    @GetMapping("/notifications")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public PageData<Notification> getNotifications(@RequestParam int pageSize,
                                                   @RequestParam int page,
                                                   @RequestParam(required = false) String textSearch,
                                                   @RequestParam(required = false) String sortProperty,
                                                   @RequestParam(required = false) String sortOrder,
                                                   @RequestParam(defaultValue = "false") boolean unreadOnly,
                                                   @AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
        // no permissions
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return notificationService.findNotificationsByRecipientIdAndReadStatus(user.getTenantId(), user.getId(), unreadOnly, pageLink);
    }

    @PutMapping("/notification/{id}/read")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public void markNotificationAsRead(@PathVariable UUID id,
                                       @AuthenticationPrincipal SecurityUser user) {
        // no permissions
        NotificationId notificationId = new NotificationId(id);
        notificationCenter.markNotificationAsRead(user.getTenantId(), user.getId(), notificationId);
    }

    @PutMapping("/notifications/read")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public void markAllNotificationsAsRead(@AuthenticationPrincipal SecurityUser user) {
        // no permissions
        notificationCenter.markAllNotificationsAsRead(user.getTenantId(), user.getId());
    }

    @DeleteMapping("/notification/{id}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public void deleteNotification(@PathVariable UUID id,
                                   @AuthenticationPrincipal SecurityUser user) {
        // no permissions
        NotificationId notificationId = new NotificationId(id);
        notificationCenter.deleteNotification(user.getTenantId(), user.getId(), notificationId);
    }

    @PostMapping("/notification/request")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public NotificationRequest createNotificationRequest(@RequestBody @Valid NotificationRequest notificationRequest,
                                                         @AuthenticationPrincipal SecurityUser user) throws Exception {
        if (notificationRequest.getId() != null) {
            throw new IllegalArgumentException("Notification request cannot be updated. You may only cancel/delete it");
        }
        notificationRequest.setTenantId(user.getTenantId());
        checkEntity(notificationRequest.getId(), notificationRequest, NOTIFICATION);

        notificationRequest.setOriginatorEntityId(user.getId());
        notificationRequest.setInfo(null);
        notificationRequest.setRuleId(null);
        notificationRequest.setStatus(null);
        notificationRequest.setStats(null);

        return doSaveAndLog(EntityType.NOTIFICATION_REQUEST, notificationRequest, notificationCenter::processNotificationRequest);
    }

    @PostMapping("/notification/request/preview")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public NotificationRequestPreview getNotificationRequestPreview(@RequestBody @Valid NotificationRequest request,
                                                                    @RequestParam(defaultValue = "20") int recipientsPreviewSize,
                                                                    @AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
        NotificationRequestPreview preview = new NotificationRequestPreview();

        request.setOriginatorEntityId(user.getId());
        NotificationTemplate template;
        if (request.getTemplateId() != null) {
            template = checkEntityId(request.getTemplateId(), notificationTemplateService::findNotificationTemplateById, Operation.READ);
        } else {
            template = request.getTemplate();
        }
        if (template == null) {
            throw new IllegalArgumentException("Template is missing");
        }
        NotificationProcessingContext tmpProcessingCtx = NotificationProcessingContext.builder()
                .tenantId(user.getTenantId())
                .request(request)
                .template(template)
                .settings(null)
                .build();

        Map<NotificationDeliveryMethod, DeliveryMethodNotificationTemplate> processedTemplates = tmpProcessingCtx.getDeliveryMethods().stream()
                .collect(Collectors.toMap(m -> m, deliveryMethod -> {
                    NotificationRecipient recipient = null;
                    if (NotificationTargetType.PLATFORM_USERS.getSupportedDeliveryMethods().contains(deliveryMethod)) {
                        recipient = userService.findUserById(user.getTenantId(), user.getId());
                    }
                    return tmpProcessingCtx.getProcessedTemplate(deliveryMethod, recipient);
                }));
        preview.setProcessedTemplates(processedTemplates);

        // generic permission
        Set<String> recipientsPreview = new LinkedHashSet<>();
        Map<String, Integer> recipientsCountByTarget = new HashMap<>();

        List<NotificationTarget> targets = notificationTargetService.findNotificationTargetsByTenantIdAndIds(user.getTenantId(),
                request.getTargets().stream().map(NotificationTargetId::new).collect(Collectors.toList()));
        for (NotificationTarget target : targets) {
            int recipientsCount;
            List<NotificationRecipient> recipientsPart;
            if (target.getConfiguration().getType() == NotificationTargetType.PLATFORM_USERS) {
                PageData<User> recipients = notificationTargetService.findRecipientsForNotificationTargetConfig(user.getTenantId(),
                        (PlatformUsersNotificationTargetConfig) target.getConfiguration(), new PageLink(recipientsPreviewSize));
                recipientsCount = (int) recipients.getTotalElements();
                recipientsPart = recipients.getData().stream().map(r -> (NotificationRecipient) r).collect(Collectors.toList());
            } else {
                recipientsCount = 1;
                recipientsPart = List.of(((SlackNotificationTargetConfig) target.getConfiguration()).getConversation());
            }

            for (NotificationRecipient recipient : recipientsPart) {
                if (recipientsPreview.size() < recipientsPreviewSize) {
                    recipientsPreview.add(recipient.getTitle());
                } else {
                    break;
                }
            }
            recipientsCountByTarget.put(target.getName(), recipientsCount);
        }

        preview.setRecipientsPreview(recipientsPreview);
        preview.setRecipientsCountByTarget(recipientsCountByTarget);
        preview.setTotalRecipientsCount(recipientsCountByTarget.values().stream().mapToInt(Integer::intValue).sum());

        return preview;
    }

    @GetMapping("/notification/request/{id}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public NotificationRequestInfo getNotificationRequestById(@PathVariable UUID id) throws ThingsboardException {
        NotificationRequestId notificationRequestId = new NotificationRequestId(id);
        return checkEntityId(notificationRequestId, notificationRequestService::findNotificationRequestInfoById, Operation.READ);
    }

    @GetMapping("/notification/requests")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public PageData<NotificationRequestInfo> getNotificationRequests(@RequestParam int pageSize,
                                                                     @RequestParam int page,
                                                                     @RequestParam(required = false) String textSearch,
                                                                     @RequestParam(required = false) String sortProperty,
                                                                     @RequestParam(required = false) String sortOrder,
                                                                     @AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
        // generic permission
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return notificationRequestService.findNotificationRequestsInfosByTenantIdAndOriginatorType(user.getTenantId(), EntityType.USER, pageLink);
    }

    @DeleteMapping("/notification/request/{id}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public void deleteNotificationRequest(@PathVariable UUID id) throws Exception {
        NotificationRequestId notificationRequestId = new NotificationRequestId(id);
        NotificationRequest notificationRequest = checkEntityId(notificationRequestId, notificationRequestService::findNotificationRequestById, Operation.DELETE);
        doDeleteAndLog(EntityType.NOTIFICATION_REQUEST, notificationRequest, notificationCenter::deleteNotificationRequest);
    }


    @PostMapping("/notification/settings")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public NotificationSettings saveNotificationSettings(@RequestBody @Valid NotificationSettings notificationSettings,
                                                         @AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
        accessControlService.checkPermission(user, Resource.ADMIN_SETTINGS, Operation.WRITE);
        TenantId tenantId = user.isSystemAdmin() ? TenantId.SYS_TENANT_ID : user.getTenantId();
        notificationSettingsService.saveNotificationSettings(tenantId, notificationSettings);
        return notificationSettings;
    }

    @GetMapping("/notification/settings")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public NotificationSettings getNotificationSettings(@AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
        accessControlService.checkPermission(user, Resource.ADMIN_SETTINGS, Operation.READ);
        TenantId tenantId = user.isSystemAdmin() ? TenantId.SYS_TENANT_ID : user.getTenantId();
        return notificationSettingsService.findNotificationSettings(tenantId);
    }

    @GetMapping("/notification/deliveryMethods")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public Set<NotificationDeliveryMethod> getAvailableDeliveryMethods(@AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
        accessControlService.checkPermission(user, Resource.ADMIN_SETTINGS, Operation.READ);
        return notificationCenter.getAvailableDeliveryMethods(user.getTenantId());
    }

}
