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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.NotificationId;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationStatus;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.notification.NotificationService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.notification.NotificationProcessingService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.UUID;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class NotificationController extends BaseController {

    private final NotificationService notificationService;
    private final NotificationProcessingService notificationProcessingService;

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
        return notificationService.findNotificationsByUserIdAndReadStatusAndPageLink(user.getTenantId(), user.getId(), unreadOnly, pageLink);
    }

    @PutMapping("/notification/{id}/read") // or maybe to NotificationUpdateRequest for the future
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    public void markNotificationAsRead(@PathVariable UUID id,
                                       @AuthenticationPrincipal SecurityUser user) {
        NotificationId notificationId = new NotificationId(id);
        notificationService.updateNotificationStatus(user.getTenantId(), notificationId, NotificationStatus.READ);
    }


    @PostMapping("/notification/request")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public NotificationRequest createNotificationRequest(@RequestBody NotificationRequest notificationRequest,
                                                         @AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
        accessControlService.checkPermission(user, Resource.NOTIFICATION, Operation.CREATE);
        return notificationProcessingService.processNotificationRequest(user, notificationRequest);
    }

    @GetMapping("/notification/requests")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public PageData<NotificationRequest> getNotificationRequests(@RequestParam int pageSize,
                                                                 @RequestParam int page,
                                                                 @RequestParam(required = false) String textSearch,
                                                                 @RequestParam(required = false) String sortProperty,
                                                                 @RequestParam(required = false) String sortOrder,
                                                                 @AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
        accessControlService.checkPermission(user, Resource.NOTIFICATION, Operation.CREATE);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return notificationService.findNotificationRequestsByTenantIdAndPageLink(user.getTenantId(), pageLink);
    }

    // delete request and sent notifications
    public void deleteNotificationRequest() {

    }

}
