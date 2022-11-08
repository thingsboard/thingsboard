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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.NotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.NotificationTargetConfigType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.notification.NotificationTargetService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.UUID;

@RestController
@TbCoreComponent
@RequestMapping("/api/notification")
@RequiredArgsConstructor
@Slf4j
public class NotificationTargetController extends BaseController {

    private final NotificationTargetService notificationTargetService;

    @PostMapping("/target")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public NotificationTarget saveNotificationTarget(@RequestBody NotificationTarget notificationTarget,
                                                     @AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
        accessControlService.checkPermission(user, Resource.NOTIFICATION_TARGET, Operation.CREATE, null, notificationTarget);
        if (!user.isSystemAdmin()) {
            NotificationTargetConfig targetConfig = notificationTarget.getConfiguration();
            if (targetConfig.getType() == NotificationTargetConfigType.SINGLE_USER ||
                    targetConfig.getType() == NotificationTargetConfigType.USER_LIST) {
                PageData<User> recipients = notificationTargetService.findRecipientsForNotificationTargetConfig(user.getTenantId(), notificationTarget.getConfiguration(), null);
                for (User recipient : recipients.getData()) {
                    accessControlService.checkPermission(user, Resource.USER, Operation.READ, recipient.getId(), recipient);
                }
            }
        }

        try {
            NotificationTarget savedNotificationTarget = notificationTargetService.saveNotificationTarget(user.getTenantId(), notificationTarget);
            logEntityAction(user, EntityType.NOTIFICATION_TARGET, savedNotificationTarget,
                    notificationTarget.getId() == null ? ActionType.ADDED : ActionType.UPDATED);
            return savedNotificationTarget;
        } catch (Exception e) {
            logEntityAction(user, EntityType.NOTIFICATION_TARGET, notificationTarget, null,
                    notificationTarget.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);
            throw e;
        }
    }

    @GetMapping("/target/{id}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public NotificationTarget getNotificationTargetById(@PathVariable UUID id,
                                                        @AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
        NotificationTargetId notificationTargetId = new NotificationTargetId(id);
        NotificationTarget notificationTarget = notificationTargetService.findNotificationTargetById(user.getTenantId(), notificationTargetId);
        accessControlService.checkPermission(user, Resource.NOTIFICATION_TARGET, Operation.READ, notificationTargetId, notificationTarget);
        return notificationTarget;
    }

    @PostMapping("/target/recipients")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public PageData<User> getRecipientsForNotificationTargetConfig(@RequestBody NotificationTarget notificationTarget,
                                                                   @RequestParam int pageSize,
                                                                   @RequestParam int page,
                                                                   @AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, null, null, null);
        PageData<User> recipients = notificationTargetService.findRecipientsForNotificationTargetConfig(user.getTenantId(), notificationTarget.getConfiguration(), pageLink);
        if (!user.isSystemAdmin()) {
            for (User recipient : recipients.getData()) {
                accessControlService.checkPermission(user, Resource.USER, Operation.READ, recipient.getId(), recipient);
            }
        }
        return recipients;
    }

    @GetMapping("/targets")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public PageData<NotificationTarget> getNotificationTargets(@RequestParam int pageSize,
                                                               @RequestParam int page,
                                                               @RequestParam(required = false) String textSearch,
                                                               @RequestParam(required = false) String sortProperty,
                                                               @RequestParam(required = false) String sortOrder,
                                                               @AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return notificationTargetService.findNotificationTargetsByTenantId(user.getTenantId(), pageLink);
    }

    @DeleteMapping("/target/{id}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public void deleteNotificationTarget(@PathVariable UUID id,
                                         @AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
        NotificationTargetId notificationTargetId = new NotificationTargetId(id);
        NotificationTarget notificationTarget = checkNotNull(notificationTargetService.findNotificationTargetById(user.getTenantId(), notificationTargetId));
        accessControlService.checkPermission(user, Resource.NOTIFICATION_TARGET, Operation.DELETE, notificationTargetId, notificationTarget);

        try {
            notificationTargetService.deleteNotificationTarget(user.getTenantId(), notificationTargetId);
            logEntityAction(user, EntityType.NOTIFICATION_TARGET, notificationTarget, ActionType.DELETED);
        } catch (Exception e) {
            logEntityAction(user, EntityType.NOTIFICATION_TARGET, null, notificationTarget, ActionType.DELETED, e);
        }
    }

}
