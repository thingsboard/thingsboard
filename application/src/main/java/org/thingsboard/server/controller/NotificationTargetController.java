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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.security.access.AccessDeniedException;
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
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.NotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.NotificationTargetType;
import org.thingsboard.server.common.data.notification.targets.platform.CustomerUsersFilter;
import org.thingsboard.server.common.data.notification.targets.platform.PlatformUsersNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.platform.TenantAdministratorsFilter;
import org.thingsboard.server.common.data.notification.targets.platform.UserListFilter;
import org.thingsboard.server.common.data.notification.targets.platform.UsersFilter;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.notification.NotificationTargetService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
public class NotificationTargetController extends BaseController {

    private final NotificationTargetService notificationTargetService;

    @ApiOperation(value = "Save notification target (saveNotificationTarget)",
            notes = "Creates or updates notification target." + NEW_LINE +
                    "Available `configuration` types are `PLATFORM_USERS` and `SLACK`.\n" +
                    "For `PLATFORM_USERS` the `usersFilter` must be specified. " +
                    "For tenant, there are following users filter types available: " +
                    "`USER_LIST`, `CUSTOMER_USERS`, `TENANT_ADMINISTRATORS`, `ALL_USERS`, " +
                    "`ORIGINATOR_ENTITY_OWNER_USERS`, `AFFECTED_USER`.\n" +
                    "For sysadmin: `TENANT_ADMINISTRATORS`, `AFFECTED_TENANT_ADMINISTRATORS`, " +
                    "`SYSTEM_ADMINISTRATORS`, `ALL_USERS`." + NEW_LINE +
                    "Here is an example of tenant-level notification target to send notification to customer's users:\n" +
                    MARKDOWN_CODE_BLOCK_START +
                    "{\n" +
                    "  \"name\": \"Users of Customer A\",\n" +
                    "  \"configuration\": {\n" +
                    "    \"type\": \"PLATFORM_USERS\",\n" +
                    "    \"usersFilter\": {\n" +
                    "      \"type\": \"CUSTOMER_USERS\",\n" +
                    "      \"customerId\": \"32499a20-d785-11ed-a06c-21dd57dd88ca\"\n" +
                    "    },\n" +
                    "    \"description\": \"Users of Customer A\"\n" +
                    "  }\n" +
                    "}" +
                    MARKDOWN_CODE_BLOCK_END +
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
            notes = "Fetches notification target by id." +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @GetMapping("/target/{id}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public NotificationTarget getNotificationTargetById(@PathVariable UUID id) throws ThingsboardException {
        NotificationTargetId notificationTargetId = new NotificationTargetId(id);
        return checkNotificationTargetId(notificationTargetId, Operation.READ);
    }

    @ApiOperation(value = "Get recipients for notification target config (getRecipientsForNotificationTargetConfig)",
            notes = "Returns the page of recipients for such notification target configuration." +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PostMapping("/target/recipients")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public PageData<User> getRecipientsForNotificationTargetConfig(@RequestBody NotificationTarget notificationTarget,
                                                                   @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
                                                                   @RequestParam int pageSize,
                                                                   @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
                                                                   @RequestParam int page,
                                                                   @AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
        // PE: generic permission
        NotificationTargetConfig targetConfig = notificationTarget.getConfiguration();
        if (targetConfig.getType() == NotificationTargetType.PLATFORM_USERS) {
            checkTargetUsers(user, targetConfig);
        } else {
            throw new IllegalArgumentException("Target type is not platform users");
        }

        PageLink pageLink = createPageLink(pageSize, page, null, null, null);
        return notificationTargetService.findRecipientsForNotificationTargetConfig(user.getTenantId(), (PlatformUsersNotificationTargetConfig) notificationTarget.getConfiguration(), pageLink);
    }

    @ApiOperation(value = "Get notification targets by ids (getNotificationTargetsByIds)",
            notes = "Returns the list of notification targets found by provided ids." +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @GetMapping(value = "/targets", params = {"ids"})
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public List<NotificationTarget> getNotificationTargetsByIds(@Parameter(description = "Comma-separated list of uuids representing targets ids", array = @ArraySchema(schema = @Schema(type = "string")), required = true)
                                                                @RequestParam("ids") UUID[] ids,
                                                                @AuthenticationPrincipal SecurityUser user) {
        // PE: generic permission
        List<NotificationTargetId> targetsIds = Arrays.stream(ids).map(NotificationTargetId::new).collect(Collectors.toList());
        return notificationTargetService.findNotificationTargetsByTenantIdAndIds(user.getTenantId(), targetsIds);
    }

    @ApiOperation(value = "Get notification targets (getNotificationTargets)",
            notes = "Returns the page of notification targets owned by sysadmin or tenant." + NEW_LINE +
                    PAGE_DATA_PARAMETERS +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @GetMapping("/targets")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public PageData<NotificationTarget> getNotificationTargets(@Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
                                                               @RequestParam int pageSize,
                                                               @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
                                                               @RequestParam int page,
                                                               @Parameter(description = "Case-insensitive 'substring' filed based on the target's name")
                                                               @RequestParam(required = false) String textSearch,
                                                               @Parameter(description = SORT_PROPERTY_DESCRIPTION)
                                                               @RequestParam(required = false) String sortProperty,
                                                               @Parameter(description = SORT_ORDER_DESCRIPTION)
                                                               @RequestParam(required = false) String sortOrder,
                                                               @AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
        // PE: generic permission
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return notificationTargetService.findNotificationTargetsByTenantId(user.getTenantId(), pageLink);
    }

    @ApiOperation(value = "Get notification targets by supported notification type (getNotificationTargetsBySupportedNotificationType)",
            notes = "Returns the page of notification targets filtered by notification type that they can be used for." + NEW_LINE +
                    PAGE_DATA_PARAMETERS +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @GetMapping(value = "/targets", params = "notificationType")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public PageData<NotificationTarget> getNotificationTargetsBySupportedNotificationType(@RequestParam int pageSize,
                                                                                          @RequestParam int page,
                                                                                          @RequestParam(required = false) String textSearch,
                                                                                          @RequestParam(required = false) String sortProperty,
                                                                                          @RequestParam(required = false) String sortOrder,
                                                                                          @RequestParam(required = false) NotificationType notificationType,
                                                                                          @AuthenticationPrincipal SecurityUser user) throws ThingsboardException {
        // PE: generic permission
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return notificationTargetService.findNotificationTargetsByTenantIdAndSupportedNotificationType(user.getTenantId(), notificationType, pageLink);
    }

    @ApiOperation(value = "Delete notification target by id (deleteNotificationTargetById)",
            notes = "Deletes notification target by its id." + NEW_LINE +
                    "This target cannot be referenced by existing scheduled notification requests or any notification rules." +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @DeleteMapping("/target/{id}")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    public void deleteNotificationTargetById(@PathVariable UUID id) throws Exception {
        NotificationTargetId notificationTargetId = new NotificationTargetId(id);
        NotificationTarget notificationTarget = checkNotificationTargetId(notificationTargetId, Operation.DELETE);
        doDeleteAndLog(EntityType.NOTIFICATION_TARGET, notificationTarget, notificationTargetService::deleteNotificationTargetById);
    }

    private void checkTargetUsers(SecurityUser user, NotificationTargetConfig targetConfig) throws ThingsboardException {
        if (user.isSystemAdmin()) {
            return;
        }
        // PE: generic permission for users
        UsersFilter usersFilter = ((PlatformUsersNotificationTargetConfig) targetConfig).getUsersFilter();
        switch (usersFilter.getType()) {
            case USER_LIST:
                for (UUID recipientId : ((UserListFilter) usersFilter).getUsersIds()) {
                    checkUserId(new UserId(recipientId), Operation.READ);
                }
                break;
            case CUSTOMER_USERS:
                CustomerId customerId = new CustomerId(((CustomerUsersFilter) usersFilter).getCustomerId());
                checkEntityId(customerId, Operation.READ);
                break;
            case TENANT_ADMINISTRATORS:
                if (CollectionUtils.isNotEmpty(((TenantAdministratorsFilter) usersFilter).getTenantsIds()) ||
                        CollectionUtils.isNotEmpty(((TenantAdministratorsFilter) usersFilter).getTenantProfilesIds())) {
                    throw new AccessDeniedException("");
                }
                break;
            case SYSTEM_ADMINISTRATORS:
                throw new AccessDeniedException("");
        }
    }

}
