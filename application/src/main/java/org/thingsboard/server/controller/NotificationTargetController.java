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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.NotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.NotificationTargetType;
import org.thingsboard.server.common.data.notification.targets.platform.CustomerUsersFilter;
import org.thingsboard.server.common.data.notification.targets.platform.PlatformUsersNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.platform.UsersFilter;
import org.thingsboard.server.common.data.notification.targets.platform.UsersFilterType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.notification.NotificationTargetService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.service.security.permission.Resource.NOTIFICATION;

@RestController
@TbCoreComponent
@RequestMapping("/api/notification")
@RequiredArgsConstructor
@Slf4j
public class NotificationTargetController extends BaseController {

    private final NotificationTargetService notificationTargetService;

    @ApiOperation(value = "Save notification target (saveNotificationTarget)",
            notes = "Create or update notification target.\n\n" +
//                    "Examples with different configuration types:\n" +
//                    "- USER_LIST:\n" +
//                    "```\n{\n  \"name\": \"Special users\",\n  \"configuration\": {\n    \"type\": \"USER_LIST\",\n    \"usersIds\": [\n      \"ea31a460-3d85-11ed-9200-77fc04fa14fa\",\n      \"86f7b260-3d88-11ed-ad72-ad2ee0f70ba1\"\n    ]\n  }\n}\n```\n" +
//                    "- CUSTOMER_USERS (not accessible to system administrator):\n" +
//                    "```\n{\n  \"name\": \"Users of my customer\",\n  \"configuration\": {\n    \"type\": \"CUSTOMER_USERS\",\n    \"customerId\": \"ea31a460-3d85-11ed-9200-77fc04fa14fa\"\n  }\n}\n```\n" +
//                    "or if you would like to use the target in notification rule and get customerId from alarm:\n" +
//                    "```\n{\n  \"name\": \"Alarm's customer users\",\n  \"configuration\": {\n    \"type\": \"CUSTOMER_USERS\",\n    \"customerId\": null,\n    \"getCustomerIdFromOriginatorEntity\": true\n  }\n}\n```\n" +
//                    "- ALL_USERS:\n" +
//                    "```\n{\n  \"name\": \"All my users\",\n  \"configuration\": {\n    \"type\": \"ALL_USERS\"\n  }\n}\n```\n\n" +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PostMapping("/target")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public NotificationTarget saveNotificationTarget(@RequestBody @Valid NotificationTarget notificationTarget,
                                                     @AuthenticationPrincipal SecurityUser user) throws Exception {
        notificationTarget.setTenantId(user.getTenantId());
        checkEntity(notificationTarget.getId(), notificationTarget, NOTIFICATION);

        NotificationTargetConfig targetConfig = notificationTarget.getConfiguration();
        if (targetConfig.getType() == NotificationTargetType.PLATFORM_USERS) {
            checkTargetUsers(user, targetConfig);
        }

        return doSaveAndLog(EntityType.NOTIFICATION_TARGET, notificationTarget, notificationTargetService::saveNotificationTarget);
    }

    @ApiOperation(value = "Get notification target by id (getNotificationTargetById)",
            notes = "Fetch saved notification target by id." +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @GetMapping("/target/{id}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public NotificationTarget getNotificationTargetById(@PathVariable UUID id) throws ThingsboardException {
        NotificationTargetId notificationTargetId = new NotificationTargetId(id);
        return checkEntityId(NOTIFICATION, Operation.READ, notificationTargetId, notificationTargetService::findNotificationTargetById);
    }

    @ApiOperation(value = "Get recipients for notification target config (getRecipientsForNotificationTargetConfig)",
            notes = "Get the list (page) of recipients (users) for such notification target configuration." +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PostMapping("/target/recipients")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public PageData<User> getRecipientsForNotificationTargetConfig(@RequestBody NotificationTarget notificationTarget,
                                                                   @RequestParam int pageSize,
                                                                   @RequestParam int page,
                                                                   @AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
        // generic permission
        NotificationTargetConfig targetConfig = notificationTarget.getConfiguration();
        if (targetConfig.getType() == NotificationTargetType.PLATFORM_USERS) {
            checkTargetUsers(user, targetConfig);
        } else {
            throw new IllegalArgumentException("Target type is not platform users");
        }

        PageLink pageLink = createPageLink(pageSize, page, null, null, null);
        return notificationTargetService.findRecipientsForNotificationTargetConfig(user.getTenantId(), null, notificationTarget.getConfiguration(), pageLink);
    }

    @GetMapping(value = "/targets", params = {"ids"})
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public List<NotificationTarget> getNotificationTargetsByIds(@RequestParam("ids") UUID[] ids,
                                                                @AuthenticationPrincipal SecurityUser user) {
        // generic permission
        List<NotificationTargetId> targetsIds = Arrays.stream(ids).map(NotificationTargetId::new).collect(Collectors.toList());
        return notificationTargetService.findNotificationTargetsByTenantIdAndIds(user.getTenantId(), targetsIds);
    }

    @ApiOperation(value = "Get notification targets (getNotificationTargets)",
            notes = "Fetch the page of notification targets owned by sysadmin or tenant." +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @GetMapping("/targets")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public PageData<NotificationTarget> getNotificationTargets(@RequestParam int pageSize,
                                                               @RequestParam int page,
                                                               @RequestParam(required = false) String textSearch,
                                                               @RequestParam(required = false) String sortProperty,
                                                               @RequestParam(required = false) String sortOrder,
                                                               @AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
        // generic permission
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return notificationTargetService.findNotificationTargetsByTenantId(user.getTenantId(), pageLink);
    }

    @ApiOperation(value = "Delete notification target by id (deleteNotificationTargetById)",
            notes = "Delete notification target by its id.\n\n" +
                    "This target cannot be referenced by existing scheduled notification requests or any notification rules." +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @DeleteMapping("/target/{id}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public void deleteNotificationTargetById(@PathVariable UUID id) throws Exception {
        NotificationTargetId notificationTargetId = new NotificationTargetId(id);
        NotificationTarget notificationTarget = checkEntityId(NOTIFICATION, Operation.DELETE, notificationTargetId, notificationTargetService::findNotificationTargetById);
        doDeleteAndLog(EntityType.NOTIFICATION_TARGET, notificationTarget, notificationTargetService::deleteNotificationTargetById);
    }

    private void checkTargetUsers(SecurityUser user, NotificationTargetConfig targetConfig) throws ThingsboardException {
        if (user.isSystemAdmin()) {
            return;
        }
        // generic permission for users
        UsersFilter usersFilter = ((PlatformUsersNotificationTargetConfig) targetConfig).getUsersFilter();
        if (usersFilter.getType() == UsersFilterType.USER_LIST) {
            PageDataIterable<User> recipients = new PageDataIterable<>(pageLink -> {
                return notificationTargetService.findRecipientsForNotificationTargetConfig(user.getTenantId(), null, targetConfig, pageLink);
            }, 200);
            for (User recipient : recipients) {
                accessControlService.checkPermission(user, Resource.USER, Operation.READ, recipient.getId(), recipient);
            }
        } else if (usersFilter.getType() == UsersFilterType.CUSTOMER_USERS) {
            CustomerId customerId = new CustomerId(((CustomerUsersFilter) usersFilter).getCustomerId());
            checkEntityId(customerId, Operation.READ);
        }
    }

}
