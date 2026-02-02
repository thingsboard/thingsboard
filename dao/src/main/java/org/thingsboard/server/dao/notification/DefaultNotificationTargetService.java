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
package org.thingsboard.server.dao.notification;

import com.google.common.util.concurrent.FluentFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.NotificationRequestStatus;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.info.RuleOriginatedNotificationInfo;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.NotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.platform.CustomerUsersFilter;
import org.thingsboard.server.common.data.notification.targets.platform.PlatformUsersNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.platform.TenantAdministratorsFilter;
import org.thingsboard.server.common.data.notification.targets.platform.UserListFilter;
import org.thingsboard.server.common.data.notification.targets.platform.UsersFilter;
import org.thingsboard.server.common.data.notification.targets.platform.UsersFilterType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.entity.EntityDaoService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.user.UserService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultNotificationTargetService extends AbstractEntityService implements NotificationTargetService, EntityDaoService {

    private final NotificationTargetDao notificationTargetDao;
    private final NotificationRequestDao notificationRequestDao;
    private final NotificationRuleDao notificationRuleDao;
    private final UserService userService;

    @Override
    public NotificationTarget saveNotificationTarget(TenantId tenantId, NotificationTarget notificationTarget) {
        try {
            NotificationTarget savedTarget = notificationTargetDao.saveAndFlush(tenantId, notificationTarget);
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId).entityId(savedTarget.getId())
                    .created(notificationTarget.getId() == null).build());
            return savedTarget;
        } catch (Exception e) {
            checkConstraintViolation(e, "uq_notification_target_name", "Recipients group with such name already exists");
            throw e;
        }
    }

    @Override
    public NotificationTarget findNotificationTargetById(TenantId tenantId, NotificationTargetId id) {
        return notificationTargetDao.findById(tenantId, id.getId());
    }

    @Override
    public PageData<NotificationTarget> findNotificationTargetsByTenantId(TenantId tenantId, PageLink pageLink) {
        return notificationTargetDao.findByTenantIdAndPageLink(tenantId, pageLink);
    }

    @Override
    public PageData<NotificationTarget> findNotificationTargetsByTenantIdAndSupportedNotificationType(TenantId tenantId, NotificationType notificationType, PageLink pageLink) {
        return notificationTargetDao.findByTenantIdAndSupportedNotificationTypeAndPageLink(tenantId, notificationType, pageLink);
    }

    @Override
    public List<NotificationTarget> findNotificationTargetsByTenantIdAndIds(TenantId tenantId, List<NotificationTargetId> ids) {
        return notificationTargetDao.findByTenantIdAndIds(tenantId, ids);
    }

    @Override
    public List<NotificationTarget> findNotificationTargetsByTenantIdAndUsersFilterType(TenantId tenantId, UsersFilterType filterType) {
        return notificationTargetDao.findByTenantIdAndUsersFilterType(tenantId, filterType);
    }

    @Override
    public PageData<User> findRecipientsForNotificationTarget(TenantId tenantId, CustomerId customerId, NotificationTargetId targetId, PageLink pageLink) {
        NotificationTarget notificationTarget = findNotificationTargetById(tenantId, targetId);
        Objects.requireNonNull(notificationTarget, "Notification target [" + targetId + "] not found");
        NotificationTargetConfig configuration = notificationTarget.getConfiguration();
        return findRecipientsForNotificationTargetConfig(notificationTarget.getTenantId(), (PlatformUsersNotificationTargetConfig) configuration, pageLink);
    }

    @Override
    public PageData<User> findRecipientsForNotificationTargetConfig(TenantId tenantId, PlatformUsersNotificationTargetConfig targetConfig, PageLink pageLink) {
        UsersFilter usersFilter = targetConfig.getUsersFilter();
        switch (usersFilter.getType()) {
            case USER_LIST: {
                List<User> users = ((UserListFilter) usersFilter).getUsersIds().stream()
                        .limit(pageLink.getPageSize())
                        .map(UserId::new).map(userId -> userService.findUserById(tenantId, userId))
                        .filter(Objects::nonNull).collect(Collectors.toList());
                return new PageData<>(users, 1, users.size(), false);
            }
            case CUSTOMER_USERS: {
                if (tenantId.equals(TenantId.SYS_TENANT_ID)) {
                    throw new IllegalArgumentException("Customer users target is not supported for system administrator");
                }
                CustomerUsersFilter filter = (CustomerUsersFilter) usersFilter;
                return userService.findCustomerUsers(tenantId, new CustomerId(filter.getCustomerId()), pageLink);
            }
            case TENANT_ADMINISTRATORS: {
                TenantAdministratorsFilter filter = (TenantAdministratorsFilter) usersFilter;
                if (!tenantId.equals(TenantId.SYS_TENANT_ID)) {
                    return userService.findTenantAdmins(tenantId, pageLink);
                } else {
                    if (isNotEmpty(filter.getTenantsIds())) {
                        return userService.findTenantAdminsByTenantsIds(filter.getTenantsIds().stream()
                                .map(TenantId::fromUUID).collect(Collectors.toList()), pageLink);
                    } else if (isNotEmpty(filter.getTenantProfilesIds())) {
                        return userService.findTenantAdminsByTenantProfilesIds(filter.getTenantProfilesIds().stream()
                                .map(TenantProfileId::new).collect(Collectors.toList()), pageLink);
                    } else {
                        return userService.findAllTenantAdmins(pageLink);
                    }
                }
            }
            case SYSTEM_ADMINISTRATORS:
                return userService.findSysAdmins(pageLink);
            case ALL_USERS: {
                if (!tenantId.equals(TenantId.SYS_TENANT_ID)) {
                    return userService.findUsersByTenantId(tenantId, pageLink);
                } else {
                    return userService.findAllUsers(pageLink);
                }
            }
            default:
                throw new IllegalArgumentException("Recipient type not supported");
        }
    }

    @Override
    public PageData<User> findRecipientsForRuleNotificationTargetConfig(TenantId tenantId, PlatformUsersNotificationTargetConfig targetConfig, RuleOriginatedNotificationInfo info, PageLink pageLink) {
        switch (targetConfig.getUsersFilter().getType()) {
            case ORIGINATOR_ENTITY_OWNER_USERS -> {
                CustomerId customerId = info.getAffectedCustomerId();
                if (customerId != null && !customerId.isNullUid()) {
                    return userService.findCustomerUsers(tenantId, customerId, pageLink);
                } else {
                    return userService.findTenantAdmins(tenantId, pageLink);
                }
            }
            case AFFECTED_USER -> {
                UserId userId = info.getAffectedUserId();
                if (userId != null) {
                    return new PageData<>(List.of(userService.findUserById(tenantId, userId)), 1, 1, false);
                }
            }
            case AFFECTED_TENANT_ADMINISTRATORS -> {
                TenantId affectedTenantId = info.getAffectedTenantId();
                if (affectedTenantId == null) {
                    affectedTenantId = tenantId;
                }
                if (!affectedTenantId.isNullUid()) {
                    return userService.findTenantAdmins(affectedTenantId, pageLink);
                }
            }
            default -> throw new IllegalArgumentException("Recipient type not supported");
        }
        return new PageData<>();
    }

    @Override
    public void deleteNotificationTargetById(TenantId tenantId, NotificationTargetId id) {
        deleteEntity(tenantId, id, false);
    }

    @Override
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        NotificationTargetId targetId = (NotificationTargetId) id;
        if (!force && notificationRequestDao.existsByTenantIdAndStatusAndTargetId(tenantId, NotificationRequestStatus.SCHEDULED, targetId)) {
            throw new IllegalArgumentException("Recipients group is referenced by scheduled notification request");
        }
        if (!force && notificationRuleDao.existsByTenantIdAndTargetId(tenantId, targetId)) {
            throw new IllegalArgumentException("Recipients group is being used in notification rule");
        }
        notificationTargetDao.removeById(tenantId, id.getId());
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(id).build());
    }

    @Override
    public void deleteNotificationTargetsByTenantId(TenantId tenantId) {
        notificationTargetDao.removeByTenantId(tenantId);
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        deleteNotificationTargetsByTenantId(tenantId);
    }

    @Override
    public long countNotificationTargetsByTenantId(TenantId tenantId) {
        return notificationTargetDao.countByTenantId(tenantId);
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findNotificationTargetById(tenantId, new NotificationTargetId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        return FluentFuture.from(notificationTargetDao.findByIdAsync(tenantId, entityId.getId()))
                .transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.NOTIFICATION_TARGET;
    }

}
