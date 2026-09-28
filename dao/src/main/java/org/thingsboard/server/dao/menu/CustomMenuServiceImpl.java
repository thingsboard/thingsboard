// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.menu;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.CustomMenuDeleteResult;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomMenuId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.menu.CMAssigneeType;
import org.thingsboard.server.common.data.menu.CMScope;
import org.thingsboard.server.common.data.menu.CustomMenu;
import org.thingsboard.server.common.data.menu.CustomMenuConfig;
import org.thingsboard.server.common.data.menu.CustomMenuFilter;
import org.thingsboard.server.common.data.menu.CustomMenuInfo;
import org.thingsboard.server.common.data.menu.CustomMenuItem;
import org.thingsboard.server.common.data.menu.DefaultMenuItem;
import org.thingsboard.server.common.data.menu.MenuItem;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.owner.OwnerService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.service.Validator.checkNotNull;
import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomMenuServiceImpl extends AbstractCachedEntityService<CustomMenuId, CustomMenu, CustomMenuCacheEvictEvent> implements CustomMenuService {

    private static final String INCORRECT_CUSTOM_MENU_ID = "Incorrect customMenuId ";
    private final CustomerService customerService;
    private final UserService userService;
    private final EntityGroupService entityGroupService;
    private final OwnerService ownerService;
    private final ApplicationEventPublisher eventPublisher;
    private final CustomMenuDao customMenuDao;
    private final DataValidator<CustomMenuInfo> customMenuInfoValidator;

    @Override
    public CustomMenu createCustomMenu(CustomMenuInfo customMenuInfo, List<EntityId> assignToList, boolean force) throws ThingsboardException {
        log.trace("Executing createCustomMenu [{}]", customMenuInfo);
        return saveCustomMenu(new CustomMenu(customMenuInfo, new CustomMenuConfig()), assignToList, force);
    }

    @Override
    public CustomMenu updateCustomMenu(CustomMenu customMenu, boolean force) throws ThingsboardException {
        log.trace("Executing updateCustomMenu [{}] ", customMenu);
        return saveCustomMenu(customMenu, null, force);
    }

    @Override
    public void updateAssigneeList(CustomMenu customMenu, CMAssigneeType newAssigneeType, List<EntityId> newAssignToList, String[] newUserGroupNames, boolean force) throws ThingsboardException {
        log.trace("Executing updateAssigneeList customMenuId [{}], newAssigneeType [{}], newAssignToList [{}], force [{}]", customMenu.getId(), newAssigneeType, newAssignToList, force);
        List<EntityId> existingEntityIds = findCustomMenuAssigneeList(customMenu)
                .stream()
                .map(EntityInfo::getId)
                .toList();

        List<EntityId> toRemoveEntityIds = existingEntityIds.stream()
                .filter(entityId -> !newAssignToList.contains(entityId))
                .toList();
        List<EntityId> toAddEntityIds = newAssignToList.stream()
                .filter(entityId -> !existingEntityIds.contains(entityId))
                .toList();

        CMAssigneeType oldAssigneeType = customMenu.getAssigneeType();
        assignCustomMenu(customMenu.getId(), newAssigneeType, toAddEntityIds);
        unassignCustomMenu(oldAssigneeType, toRemoveEntityIds);
        if (oldAssigneeType != newAssigneeType || !Arrays.equals(customMenu.getUserGroupNames(), newUserGroupNames)) {
            CustomMenu newCustomMenu = new CustomMenu(customMenu);
            newCustomMenu.setAssigneeType(newAssigneeType);
            newCustomMenu.setUserGroupNames(newUserGroupNames);
            updateCustomMenu(newCustomMenu, force);
        } else {
            publishEvictEvent(new CustomMenuCacheEvictEvent(customMenu.getTenantId(), customMenu.getId()));
            eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(customMenu.getTenantId()).entityId(getEntityIdForEvent(customMenu.getTenantId(), customMenu.getCustomerId()))
                    .edgeEventType(EdgeEventType.CUSTOM_MENU).actionType(ActionType.UPDATED).build());
        }
    }

    @Override
    public PageData<CustomMenu> findCustomMenusByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findCustomMenusByTenantId [{}]", tenantId);
        return customMenuDao.findByTenantId(tenantId, pageLink);
    }

    @Override
    public CustomMenuInfo findCustomMenuInfoById(TenantId tenantId, CustomMenuId customMenuId) {
        log.trace("Executing findCustomMenuInfoById [{}]", customMenuId);
        validateId(customMenuId, id -> INCORRECT_CUSTOM_MENU_ID + id);
        return customMenuDao.findInfoById(customMenuId);
    }

    @Override
    public CustomMenu findCustomMenuById(TenantId tenantId, CustomMenuId customMenuId) {
        log.trace("Executing findCustomMenuById [{}]", customMenuId);
        validateId(customMenuId, id -> INCORRECT_CUSTOM_MENU_ID + id);
        return cache.getAndPutInTransaction(customMenuId, () -> customMenuDao.findById(tenantId, customMenuId.getId()), true);
    }

    @Override
    public PageData<CustomMenuInfo> findCustomMenuInfos(TenantId tenantId, CustomMenuFilter customMenuFilter, PageLink pageLink) {
        log.trace("Executing findCustomMenuInfos [{}]", customMenuFilter);
        return customMenuDao.findInfosByFilter(tenantId, customMenuFilter, pageLink);
    }

    @Override
    public CustomMenuConfig findSystemAdminCustomMenuConfig() {
        log.trace("Executing getSystemAdminCustomMenu");
        CustomMenu customMenu = findDefaultCustomMenuByScope(TenantId.SYS_TENANT_ID, null, CMScope.SYSTEM);
        return getVisibleMenuItems(customMenu);
    }

    @Override
    public CustomMenuConfig findTenantUserCustomMenuConfig(TenantId tenantId, UserId userId) {
        log.trace("Executing getTenantUserCustomMenu userId [{}] ", userId);
        CustomMenu result = findCustomMenuByUserId(tenantId, userId);
        if (result == null) {
            result = findDefaultCustomMenuByScope(tenantId, null, CMScope.TENANT);
            if (result == null) {
                result = findDefaultCustomMenuByScope(TenantId.SYS_TENANT_ID, null, CMScope.TENANT);
            }
        }
        return getVisibleMenuItems(result);
    }

    @Override
    public CustomMenuConfig findCustomerUserCustomMenuConfig(TenantId tenantId, CustomerId customerId, UserId userId) {
        log.trace("Executing getCustomerUserCustomMenu userId [{}] ", userId);
        CustomMenu result = findCustomMenuByUserId(tenantId, userId);
        if (result == null) {
            result = findCustomerHierarchyCustomMenu(tenantId, customerId);
            if (result == null) {
                result = findDefaultCustomMenuByScope(tenantId, null, CMScope.CUSTOMER);
            }
            if (result == null) {
                result = findDefaultCustomMenuByScope(TenantId.SYS_TENANT_ID, null, CMScope.CUSTOMER);
            }
        }
        return getVisibleMenuItems(result);
    }

    @Override
    public CustomMenu findDefaultCustomMenuByScope(TenantId tenantId, CustomerId customerId, CMScope scope) {
        log.trace("Executing findDefaultCustomMenuByScope [{}] [{}] [{}]", tenantId, customerId, scope);
        checkNotNull(scope, "Scope could not be null");
        return customMenuDao.findDefaultMenuByScope(tenantId, customerId, scope);
    }

    @Override
    public List<EntityInfo> findCustomMenuAssigneeList(CustomMenuInfo customMenuInfo) {
        log.trace("Executing findCustomMenuAssigneeList customMenuId [{}] ", customMenuInfo.getId());
        return switch (customMenuInfo.getAssigneeType()) {
            case NO_ASSIGN, ALL, USER_GROUPS -> Collections.emptyList();
            case CUSTOMERS -> customerService.findCustomersByCustomMenuId(customMenuInfo.getId()).stream()
                    .map(customer -> new EntityInfo(customer.getId(), customer.getName())).toList();
            case USERS -> userService.findUsersByCustomMenuId(customMenuInfo.getId()).stream()
                    .map(user -> new EntityInfo(user.getId(), user.getName())).toList();
            default ->
                    throw new RuntimeException("Invalid custom menu assignee type '" + customMenuInfo.getAssigneeType() + "' specified for custom menu!");
        };
    }

    public Optional<CustomMenu> findFirstByScopeAndUserGroupNames(TenantId tenantId, CustomerId customerId, CMScope scope, Set<String> userGroupNames) {
        log.trace("Executing findByUserGroupNames [{}] ", userGroupNames);
        return customMenuDao.findFirstByScopeAndUserGroupNames(tenantId, customerId, scope, userGroupNames);
    }

    @Override
    public CustomMenuDeleteResult deleteCustomMenu(CustomMenu customMenu, boolean force) {
        log.trace("Executing deleteCustomMenu customMenuId [{}]", customMenu.getId());
        List<EntityInfo> existingAssigneeList = findCustomMenuAssigneeList(customMenu);
        if (customMenu.getTenantId().isSysTenantId() && customMenu.getAssigneeType() == CMAssigneeType.ALL) {
            throw new DataValidationException("System default menu can not be deleted");
        }
        CustomMenuDeleteResult.CustomMenuDeleteResultBuilder result = CustomMenuDeleteResult.builder()
                .assigneeType(customMenu.getAssigneeType());
        boolean success = true;
        if (!force && !existingAssigneeList.isEmpty()) {
            success = false;
            result.assigneeList(existingAssigneeList);
        }
        if (success) {
            if (!existingAssigneeList.isEmpty()) {
                List<EntityId> entityIds = existingAssigneeList.stream().map(EntityInfo::getId).toList();
                unassignCustomMenu(customMenu.getAssigneeType(), entityIds);
            }
            deleteCustomMenu(customMenu);
        }
        return result.success(success).build();
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        log.trace("Executing deleteByTenantId, tenantId [{}]", tenantId);
        customMenusRemover.removeEntities(tenantId, tenantId);
    }

    private final PaginatedRemover<TenantId, CustomMenu> customMenusRemover = new PaginatedRemover<>() {
        @Override
        protected PageData<CustomMenu> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
            return findCustomMenusByTenantId(tenantId, pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, CustomMenu customMenu) {
            deleteCustomMenu(customMenu, true);
        }
    };

    private CustomMenu saveCustomMenu(CustomMenu customMenu, List<EntityId> assignToList, boolean force) throws ThingsboardException {
        customMenuInfoValidator.validate(customMenu, CustomMenuInfo::getTenantId);
        if (customMenu.getAssigneeType() == CMAssigneeType.ALL) {
            CustomMenu existingDefaultCustomMenu = findDefaultCustomMenuByScope(customMenu.getTenantId(), customMenu.getCustomerId(), customMenu.getScope());
            if (existingDefaultCustomMenu != null && !existingDefaultCustomMenu.getId().equals(customMenu.getId())) {
                if (force) {
                    existingDefaultCustomMenu.setAssigneeType(CMAssigneeType.NO_ASSIGN);
                    updateCustomMenu(existingDefaultCustomMenu,  true);
                } else {
                    throw new DataValidationException("There is already default menu for scope " + customMenu.getScope());
                }
            }
        }
        CustomMenu savedCustomMenu = customMenuDao.save(customMenu.getTenantId(), customMenu);
        if (CollectionUtils.isNotEmpty(assignToList)) {
            assignCustomMenu(savedCustomMenu.getId(), customMenu.getAssigneeType(), assignToList);
        }
        publishEvictEvent(new CustomMenuCacheEvictEvent(savedCustomMenu.getTenantId(), savedCustomMenu.getId()));
        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(savedCustomMenu.getTenantId()).entityId(getEntityIdForEvent(customMenu.getTenantId(), customMenu.getCustomerId()))
                .body(JacksonUtil.toString(savedCustomMenu)).edgeEventType(EdgeEventType.CUSTOM_MENU).actionType(ActionType.UPDATED).build());
        return savedCustomMenu;
    }

    private void assignCustomMenu(CustomMenuId customMenuId, CMAssigneeType assigneeType, List<EntityId> entityIdsToAssign) {
        processCustomMenuAssignment(customMenuId, assigneeType, entityIdsToAssign, false);
    }

    private void unassignCustomMenu(CMAssigneeType assigneeType, List<EntityId> entityIdsToUnassign) {
        processCustomMenuAssignment(null, assigneeType, entityIdsToUnassign, true);
    }

    private void processCustomMenuAssignment(CustomMenuId customMenuId, CMAssigneeType assigneeType, List<EntityId> entityIds, boolean isUnassign) {
        if (CollectionUtils.isEmpty(entityIds)) {
            return;
        }
        switch (assigneeType) {
            case ALL:
            case NO_ASSIGN:
                break;
            case CUSTOMERS:
                List<CustomerId> customerIds = entityIds.stream().map(CustomerId.class::cast).toList();
                customerService.updateCustomersCustomMenuId(customerIds, isUnassign ? null : customMenuId);
                break;
            case USERS:
                List<UserId> userIds = entityIds.stream().map(UserId.class::cast).toList();
                userService.updateUsersCustomMenuId(userIds, isUnassign ? null : customMenuId);
                break;
            default:
                throw new IncorrectParameterException("Unsupported assignee type!");
        }
    }

    private void deleteCustomMenu(CustomMenu customMenu) {
        customMenuDao.removeById(customMenu.getTenantId(), customMenu.getId().getId());
        publishEvictEvent(new CustomMenuCacheEvictEvent(customMenu.getTenantId(), customMenu.getId()));
        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(customMenu.getTenantId()).entityId(getEntityIdForEvent(customMenu.getTenantId(), customMenu.getCustomerId()))
                .body(JacksonUtil.toString(customMenu)).edgeEventType(EdgeEventType.CUSTOM_MENU).actionType(ActionType.DELETED).build());
    }

    private static CustomMenuConfig getVisibleMenuItems(CustomMenu customMenu) {
        if (customMenu == null || customMenu.getConfig() == null) {
            return null;
        }
        return new CustomMenuConfig(filterVisibleMenuItems(customMenu.getConfig().getItems()));
    }

    private static <T extends MenuItem> List<T> filterVisibleMenuItems(List<T> menuItems) {
        return menuItems.stream().filter(MenuItem::isVisible).map(CustomMenuServiceImpl::filterVisiblePages).collect(Collectors.toList());
    }

    private static <T extends MenuItem> T filterVisiblePages(T item) {
        switch (item.getType()) {
            case HOME, DEFAULT -> {
                var defaultItemPages = ((DefaultMenuItem) item).getPages();
                if (defaultItemPages != null) {
                    ((DefaultMenuItem) item).setPages(filterVisibleMenuItems(defaultItemPages));
                }
            }
            case CUSTOM -> {
                var customItemPages = ((CustomMenuItem) item).getPages();
                if (customItemPages != null) {
                    ((CustomMenuItem) item).setPages(filterVisibleMenuItems(customItemPages));
                }
            }
        }
        return item;
    }

    private CustomMenu findCustomMenuByUserId(TenantId tenantId, UserId userId) {
        User user = userService.findUserById(tenantId, userId);
        if (user == null) {
            return null;
        }
        if (user.getCustomMenuId() != null) {
            CustomMenu customMenu = findCustomMenuById(tenantId, user.getCustomMenuId());
            if (customMenu != null) {
                return customMenu;
            }
        }
        Set<String> userGroupNames = entityGroupService.findUserGroupNamesByUserId(tenantId, userId);
        CMScope scope = user.getCustomerId().isNullUid() ? CMScope.TENANT : CMScope.CUSTOMER;

        Optional<CustomMenu> ownerCustomMenu;
        if (scope == CMScope.TENANT) {
            ownerCustomMenu = findFirstByScopeAndUserGroupNames(tenantId, new CustomerId(CustomerId.NULL_UUID), scope, userGroupNames);
            if (ownerCustomMenu.isPresent()) {
                return ownerCustomMenu.get();
            }
        } else {
            for (EntityId owner : ownerService.fetchOwnersHierarchy(tenantId, user.getOwnerId())) {
                ownerCustomMenu = findFirstByScopeAndUserGroupNames(
                        tenantId,
                        owner instanceof TenantId ? new CustomerId(CustomerId.NULL_UUID) : new CustomerId(owner.getId()),
                        scope,
                        userGroupNames
                );
                if (ownerCustomMenu.isPresent()) {
                    return ownerCustomMenu.get();
                }
            }
        }

        return null;
    }

    private CustomMenu findCustomerHierarchyCustomMenu(TenantId tenantId, CustomerId customerId) {
        CustomMenu result = findDefaultCustomMenuByScope(tenantId, customerId, CMScope.CUSTOMER);
        if (result == null) {
            Customer customer = customerService.findCustomerById(tenantId, customerId);
            CustomMenuId customerCustomMenuId = customer.getCustomMenuId();
            if (customerCustomMenuId != null) {
                result = findCustomMenuById(tenantId, customerCustomMenuId);
            }
            if (result == null) {
                if (customer.isSubCustomer()) {
                    result = findCustomerHierarchyCustomMenu(tenantId, customer.getParentCustomerId());
                }
            }
        }
        return result;
    }

    private static EntityId getEntityIdForEvent(TenantId tenantId, CustomerId customerId) {
        return customerId != null && !customerId.isNullUid() ? customerId : tenantId;
    }

    @Override
    protected void publishEvictEvent(CustomMenuCacheEvictEvent customMenuCacheEvictEvent) {
        eventPublisher.publishEvent(customMenuCacheEvictEvent);
    }

    @TransactionalEventListener(classes = CustomMenuCacheEvictEvent.class, fallbackExecution = true)
    @Override
    public void handleEvictEvent(CustomMenuCacheEvictEvent event) {
        cache.evict(event.customMenuId());
    }

}
