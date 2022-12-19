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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
import org.thingsboard.rule.engine.api.NotificationManager;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.NotificationId;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationOriginatorType;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.settings.NotificationSettings;
import org.thingsboard.server.common.data.notification.settings.SlackNotificationDeliveryMethodConfig;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.notification.NotificationRequestService;
import org.thingsboard.server.dao.notification.NotificationService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.notification.NotificationManagerHelper;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;
import org.thingsboard.server.service.slack.SlackConversation;
import org.thingsboard.server.service.slack.SlackService;

import java.util.List;
import java.util.UUID;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class NotificationController extends BaseController {

    private final NotificationService notificationService;
    private final NotificationRequestService notificationRequestService;
    private final NotificationManager notificationManager;
    private final NotificationManagerHelper notificationManagerHelper;
    private final SlackService slackService;

    @GetMapping("/notifications")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public PageData<Notification> getNotifications(@RequestParam int pageSize,
                                                   @RequestParam int page,
                                                   @RequestParam(required = false) String textSearch,
                                                   @RequestParam(required = false) String sortProperty,
                                                   @RequestParam(required = false) String sortOrder,
                                                   @RequestParam(defaultValue = "false") boolean unreadOnly,
                                                   @AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return notificationService.findNotificationsByUserIdAndReadStatus(user.getTenantId(), user.getId(), unreadOnly, pageLink);
    }

    @PutMapping("/notification/{id}/read") // or maybe to NotificationUpdateRequest for the future
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public void markNotificationAsRead(@PathVariable UUID id,
                                       @AuthenticationPrincipal SecurityUser user) {
        NotificationId notificationId = new NotificationId(id);
        notificationManager.markNotificationAsRead(user.getTenantId(), user.getId(), notificationId);
    }

    // delete notification?

    @PostMapping("/notification/request")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public NotificationRequest createNotificationRequest(@RequestBody NotificationRequest notificationRequest,
                                                         @AuthenticationPrincipal SecurityUser user) throws Exception {
        if (notificationRequest.getId() != null) {
            throw new IllegalArgumentException("Notification request cannot be updated. You may only cancel/delete it");
        }
        checkEntity(notificationRequest.getId(), notificationRequest, Resource.NOTIFICATION_REQUEST);

        notificationRequest.setOriginatorType(NotificationOriginatorType.ADMIN);
        notificationRequest.setOriginatorEntityId(user.getId());
        if (StringUtils.isBlank(notificationRequest.getType())) {
            notificationRequest.setType("General");
        }
        if (notificationRequest.getInfo() != null && notificationRequest.getInfo().getOriginatorType() != null) {
            throw new IllegalArgumentException("Unsupported notification info type");
        }
        notificationRequest.setRuleId(null);
        notificationRequest.setStatus(null);
        notificationRequest.setStats(null);

        return doSaveAndLog(EntityType.NOTIFICATION_REQUEST, notificationRequest, notificationManager::processNotificationRequest);
    }

    @GetMapping("/notification/request/{id}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public NotificationRequest getNotificationRequestById(@PathVariable UUID id) throws ThingsboardException {
        NotificationRequestId notificationRequestId = new NotificationRequestId(id);
        return checkEntityId(notificationRequestId, notificationRequestService::findNotificationRequestById, Operation.READ);
    }

    @GetMapping("/notification/requests")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public PageData<NotificationRequest> getNotificationRequests(@RequestParam int pageSize,
                                                                 @RequestParam int page,
                                                                 @RequestParam(required = false) String textSearch,
                                                                 @RequestParam(required = false) String sortProperty,
                                                                 @RequestParam(required = false) String sortOrder,
                                                                 @AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return notificationRequestService.findNotificationRequestsByTenantId(user.getTenantId(), pageLink);
    }

    @DeleteMapping("/notification/request/{id}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public void deleteNotificationRequest(@PathVariable UUID id) throws Exception {
        NotificationRequestId notificationRequestId = new NotificationRequestId(id);
        NotificationRequest notificationRequest = checkEntityId(notificationRequestId, notificationRequestService::findNotificationRequestById, Operation.DELETE);
        doDeleteAndLog(EntityType.NOTIFICATION_REQUEST, notificationRequest, notificationManager::deleteNotificationRequest);
    }


    @PostMapping("/notification/settings")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public NotificationSettings saveNotificationSettings(@RequestBody NotificationSettings notificationSettings,
                                                         @AuthenticationPrincipal SecurityUser user) {
        notificationManagerHelper.saveNotificationSettings(user.getTenantId(), notificationSettings);
        return notificationSettings;
    }

    @GetMapping("/notification/settings")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public NotificationSettings getNotificationSettings(@AuthenticationPrincipal SecurityUser user) {
        return notificationManagerHelper.getNotificationSettings(user.getTenantId());
    }

    @GetMapping("/notification/slack/conversations")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public List<SlackConversation> listSlackConversations(@RequestParam SlackConversation.Type type,
                                                          @AuthenticationPrincipal SecurityUser user) throws Exception {
        NotificationSettings settings = getNotificationSettings(user);
        SlackNotificationDeliveryMethodConfig slackConfig = (SlackNotificationDeliveryMethodConfig)
                settings.getDeliveryMethodsConfigs().get(NotificationDeliveryMethod.SLACK);
        if (slackConfig == null) {
            throw new IllegalArgumentException("Slack is not configured");
        }

        return slackService.listConversations(user.getTenantId(), slackConfig.getBotToken(), type);
    }

}
