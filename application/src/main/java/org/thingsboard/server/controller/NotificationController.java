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
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.NotificationId;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationProcessingContext;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestInfo;
import org.thingsboard.server.common.data.notification.NotificationRequestPreview;
import org.thingsboard.server.common.data.notification.settings.NotificationSettings;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
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
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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

    @PutMapping("/notification/{id}/read")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public void markNotificationAsRead(@PathVariable UUID id,
                                       @AuthenticationPrincipal SecurityUser user) {
        NotificationId notificationId = new NotificationId(id);
        notificationCenter.markNotificationAsRead(user.getTenantId(), user.getId(), notificationId);
    }

    @DeleteMapping("/notification/{id}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public void deleteNotification(@PathVariable UUID id,
                                   @AuthenticationPrincipal SecurityUser user) {
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
        checkEntity(notificationRequest.getId(), notificationRequest, Resource.NOTIFICATION_REQUEST);

        notificationRequest.setOriginatorEntityId(user.getId());
        if (notificationRequest.getInfo() != null && notificationRequest.getInfo().getOriginatorType() != EntityType.USER) {
            throw new IllegalArgumentException("Unsupported notification info type");
        }
        notificationRequest.setRuleId(null);
        notificationRequest.setStatus(null);
        notificationRequest.setStats(null);

        return doSaveAndLog(EntityType.NOTIFICATION_REQUEST, notificationRequest, notificationCenter::processNotificationRequest);
    }

    @PostMapping("/notification/request/preview")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public NotificationRequestPreview getNotificationRequestPreview(@RequestBody @Valid NotificationRequest notificationRequest,
                                                                    @AuthenticationPrincipal SecurityUser user) {
        NotificationRequestPreview preview = new NotificationRequestPreview();

        notificationRequest.setOriginatorEntityId(user.getId());
        NotificationTemplate notificationTemplate = notificationTemplateService.findNotificationTemplateById(user.getTenantId(), notificationRequest.getTemplateId());
        NotificationProcessingContext mockProcessingCtx = NotificationProcessingContext.builder()
                .tenantId(user.getTenantId())
                .request(notificationRequest)
                .settings(null)
                .template(notificationTemplate)
                .build();
        mockProcessingCtx.init();

        Map<String, String> templateContext = mockProcessingCtx.createTemplateContext(user);
        Map<NotificationDeliveryMethod, DeliveryMethodNotificationTemplate> processedTemplates = mockProcessingCtx.getDeliveryMethods().stream()
                .collect(Collectors.toMap(m -> m, deliveryMethod -> {
                    return mockProcessingCtx.getProcessedTemplate(deliveryMethod, templateContext);
                }));
        preview.setProcessedTemplates(processedTemplates);

        Map<String, Integer> recipientsCountByTarget = new HashMap<>();
        notificationRequest.getTargets().forEach(targetId -> {
            NotificationTarget notificationTarget = notificationTargetService.findNotificationTargetById(user.getTenantId(), new NotificationTargetId(targetId));
            if (notificationTarget == null) {
                throw new IllegalArgumentException("Notification target with id " + targetId + " not found");
            }

            int recipientsCount = notificationTargetService.countRecipientsForNotificationTargetConfig(user.getTenantId(), notificationTarget.getConfiguration());
            recipientsCountByTarget.put(notificationTarget.getName(), recipientsCount);
        });
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
                                                         @AuthenticationPrincipal SecurityUser user) {
        TenantId tenantId = user.isSystemAdmin() ? TenantId.SYS_TENANT_ID : user.getTenantId();
        notificationSettingsService.saveNotificationSettings(tenantId, notificationSettings);
        return notificationSettings;
    }

    @GetMapping("/notification/settings")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public NotificationSettings getNotificationSettings(@AuthenticationPrincipal SecurityUser user) {
        TenantId tenantId = user.isSystemAdmin() ? TenantId.SYS_TENANT_ID : user.getTenantId();
        return notificationSettingsService.findNotificationSettings(tenantId);
    }

}
