// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.group;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ShortEntityView;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.group.ColumnConfiguration;
import org.thingsboard.server.common.data.group.ColumnType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupConfiguration;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.group.SortOrder;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.query.AliasEntityId;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityGroupFilter;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.entity.EntityQueryDao;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.relation.RelationDao;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.sql.JpaExecutorService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.dao.DaoUtil.extractConstraintViolationException;
import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.service.Validator.checkNotNull;
import static org.thingsboard.server.dao.service.Validator.validateEntityId;
import static org.thingsboard.server.dao.service.Validator.validateEntityIds;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateIds;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;
import static org.thingsboard.server.dao.service.Validator.validateString;

@Slf4j
@Service("EntityGroupDaoService")
public class BaseEntityGroupService extends AbstractCachedEntityService<EntityGroupCacheKey, EntityGroup, EntityGroupEvictEvent> implements EntityGroupService {

    public static final String ENTITY_GROUP_RELATION_PREFIX = "ENTITY_GROUP_";
    public static final String INCORRECT_PARENT_ENTITY_ID = "Incorrect parentEntityId ";

    public static final String INCORRECT_OWNER_ENTITY_IDS = "Incorrect ownerEntityIds ";
    public static final String INCORRECT_GROUP_TYPE = "Incorrect groupType ";
    public static final String INCORRECT_ENTITY_GROUP_ID = "Incorrect entityGroupId ";
    public static final String INCORRECT_ENTITY_ID = "Incorrect entityId ";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String UNABLE_TO_FIND_ENTITY_GROUP_BY_ID = "Unable to find entity group by id ";
    public static final String EDGE_ENTITY_GROUP_RELATION_PREFIX = "EDGE_ENTITY_GROUP_";

    private static final ReentrantLock roleCreationLock = new ReentrantLock();

    private final EntityGroupValidator validator = new EntityGroupValidator();

    private RoleId tenantAdminRoleId;

    @Autowired
    private EntityGroupDao entityGroupDao;

    @Autowired
    private EntityGroupInfoDao entityGroupInfoDao;

    @Autowired
    private RelationDao relationDao;

    @Autowired
    private RoleService roleService;

    @Autowired
    private GroupPermissionService groupPermissionService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private EntityQueryDao entityQueryDao;

    @Autowired
    private EdgeService edgeService;

    @Autowired
    private JpaExecutorService executorService;

    @Autowired
    @Lazy
    private UserService userService;

    @Override
    public EntityGroup findEntityGroupById(TenantId tenantId, EntityGroupId entityGroupId) {
        log.trace("Executing findEntityGroupById [{}]", entityGroupId);
        validateId(entityGroupId, id -> INCORRECT_ENTITY_GROUP_ID + id);
        return entityGroupDao.findById(tenantId, entityGroupId.getId());
    }

    @Override
    public EntityGroupInfo findEntityGroupInfoById(TenantId tenantId, EntityGroupId entityGroupId) {
        log.trace("Executing findEntityGroupInfoById [{}]", entityGroupId);
        validateId(entityGroupId, id -> INCORRECT_ENTITY_GROUP_ID + id);
        return entityGroupInfoDao.findById(tenantId, entityGroupId.getId());
    }

    @Override
    public EntityInfo findEntityGroupEntityInfoById(TenantId tenantId, EntityGroupId entityGroupId) {
        log.trace("Executing findEntityGroupEntityInfoById [{}]", entityGroupId);
        validateId(entityGroupId, id -> INCORRECT_ENTITY_GROUP_ID + id);
        return entityGroupInfoDao.findEntityGroupEntityInfoById(tenantId, entityGroupId.getId());
    }

    @Override
    public ListenableFuture<EntityGroup> findEntityGroupByIdAsync(TenantId tenantId, EntityGroupId entityGroupId) {
        log.trace("Executing findEntityGroupByIdAsync [{}]", entityGroupId);
        validateId(entityGroupId, id -> INCORRECT_ENTITY_GROUP_ID + id);
        return entityGroupDao.findByIdAsync(tenantId, entityGroupId.getId());
    }

    @Override
    public ListenableFuture<EntityGroupInfo> findEntityGroupInfoByIdAsync(TenantId tenantId, EntityGroupId entityGroupId) {
        log.trace("Executing findEntityGroupInfoByIdAsync [{}]", entityGroupId);
        validateId(entityGroupId, id -> INCORRECT_ENTITY_GROUP_ID + id);
        return entityGroupInfoDao.findByIdAsync(tenantId, entityGroupId.getId());
    }

    @Override
    public EntityGroup saveEntityGroup(TenantId tenantId, EntityId parentEntityId, EntityGroup entityGroup) {
        log.trace("Executing saveEntityGroup [{}]", entityGroup);
        validateEntityId(parentEntityId, id -> INCORRECT_PARENT_ENTITY_ID + id);
        entityGroup = new EntityGroup(entityGroup);
        if (entityGroup.getId() == null) {
            entityGroup.setOwnerId(parentEntityId);
        }
        EntityGroup old = validator.validate(entityGroup, data -> tenantId);
        EntityGroupEvictEvent event = new EntityGroupEvictEvent(old != null ? old.getOwnerId() : entityGroup.getOwnerId(), entityGroup.getType(), entityGroup.getName(), old != null ? old.getName() : null);
        if (entityGroup.getId() == null && entityGroup.getConfiguration() == null) {
            EntityGroupConfiguration entityGroupConfiguration =
                    EntityGroupConfiguration.createDefaultEntityGroupConfiguration(entityGroup.getType());
            ObjectNode jsonConfiguration = (ObjectNode) JacksonUtil.valueToTree(entityGroupConfiguration);
            jsonConfiguration.putObject("settings");
            jsonConfiguration.putObject("actions");
            entityGroup.setConfiguration(jsonConfiguration);
        }
        EntityGroup savedEntityGroup;
        try {
            savedEntityGroup = entityGroupDao.saveAndFlush(tenantId, entityGroup);
            publishEvictEvent(event);
        } catch (Exception t) {
            handleEvictEvent(event);
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && DaoUtil.constraintNameMatches(e.getConstraintName(), "group_name_per_owner_unq_key")) {
                throw new DataValidationException("Entity Group with such name, type and owner already exists!");
            } else {
                throw t;
            }
        }
        eventPublisher.publishEvent(SaveEntityEvent.builder()
                .tenantId(tenantId)
                .entityId(savedEntityGroup.getId())
                .entity(savedEntityGroup)
                .created(entityGroup.getId() == null)
                .build());
        return savedEntityGroup;
    }

    @Override
    public EntityGroup createEntityGroupAll(TenantId tenantId, EntityId parentEntityId, EntityType groupType) {
        log.trace("Executing createEntityGroupAll, parentEntityId [{}], groupType [{}]", parentEntityId, groupType);
        validateEntityId(parentEntityId, id -> INCORRECT_PARENT_ENTITY_ID + id);
        if (groupType == null) {
            throw new IncorrectParameterException(INCORRECT_GROUP_TYPE + groupType);
        }
        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setName(EntityGroup.GROUP_ALL_NAME);
        entityGroup.setType(groupType);
        return saveEntityGroup(tenantId, parentEntityId, entityGroup);
    }

    @Transactional
    @Override
    public void createDefaultTenantEntityGroups(TenantId tenantId) {
        createEntityGroupsAll(tenantId, tenantId);
        findOrCreateTenantUsersGroup(tenantId);
        findOrCreateTenantAdminsGroup(tenantId);
    }

    @Transactional
    @Override
    public void createDefaultCustomerEntityGroups(TenantId tenantId, Customer customer) {
        createEntityGroupsAll(tenantId, customer.getId());
        if (!customer.isPublic()) {
            findOrCreateCustomerUsersGroup(tenantId, customer.getId(), customer.getParentCustomerId());
            findOrCreateCustomerAdminsGroup(tenantId, customer.getId(), customer.getParentCustomerId());
        } else {
            findOrCreatePublicUsersGroup(tenantId, customer.getId());
        }
    }

    private void createEntityGroupsAll(TenantId tenantId, EntityId parentEntityId) {
        Arrays.stream(EntityType.values())
                .filter(EntityType::isGroupEntityType)
                .forEach(entityGroupType -> {
                    createEntityGroupAll(tenantId, parentEntityId, entityGroupType);
                });
    }

    @Override
    public EntityGroup findOrCreateUserGroup(TenantId tenantId, EntityId parentEntityId, String groupName, String description) {
        log.trace("Executing findOrCreateUserGroup, parentEntityId [{}], groupName [{}]", parentEntityId, groupName);
        return findOrCreateEntityGroup(tenantId, parentEntityId, EntityType.USER, groupName, description, null);
    }

    @Override
    public EntityGroup findOrCreateEntityGroup(TenantId tenantId, EntityId parentEntityId, EntityType groupType, String groupName,
                                               String description, CustomerId publicCustomerId) {
        log.trace("Executing findOrCreateEntityGroup, parentEntityId [{}], groupType [{}], groupName [{}]", parentEntityId, groupType, groupName);
        try {
            Optional<EntityGroup> entityGroupOptional = findEntityGroupByTypeAndName(tenantId, parentEntityId, groupType, groupName, false);
            if (entityGroupOptional.isPresent()) {
                return entityGroupOptional.get();
            } else {
                EntityGroup entityGroup = new EntityGroup();
                entityGroup.setName(groupName);
                entityGroup.setType(groupType);
                JsonNode additionalInfo = entityGroup.getAdditionalInfo();
                if (additionalInfo == null) {
                    additionalInfo = JacksonUtil.newObjectNode();
                }
                ((ObjectNode) additionalInfo).put("description", description);
                if (publicCustomerId != null && !publicCustomerId.isNullUid()) {
                    ((ObjectNode) additionalInfo).put("isPublic", true);
                    ((ObjectNode) additionalInfo).put("publicCustomerId", publicCustomerId.getId().toString());
                }
                entityGroup.setAdditionalInfo(additionalInfo);
                return saveEntityGroup(tenantId, parentEntityId, entityGroup);
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable find or create entity group!", e);
        }
    }

    @Override
    public Optional<EntityGroupInfo> findOwnerEntityGroupInfo(TenantId tenantId, EntityId parentEntityId, EntityType groupType, String groupName) {
        log.trace("Executing findOwnerEntityGroupInfo, parentEntityId [{}], groupType [{}], groupName [{}]", parentEntityId, groupType, groupName);
        try {
            return findEntityGroupInfoByTypeAndName(tenantId, parentEntityId, groupType, groupName);
        } catch (Exception e) {
            throw new RuntimeException("Entity group with name: " + groupName + " and type: " + groupType + " doesn't exist!", e);
        }
    }

    @Override
    public EntityGroup findOrCreateTenantUsersGroup(TenantId tenantId) {

        // User Group 'Tenant Users' -> 'Tenant User' role -> Read only permissions

        EntityGroup tenantUsers = findOrCreateUserGroup(tenantId,
                tenantId, EntityGroup.GROUP_TENANT_USERS_NAME, "Autogenerated Tenant Users group with read-only permissions.");
        roleCreationLock.lock();
        try {
            log.trace("Executing findOrCreateTenantUserRole, TenantId [{}]", tenantId);
            Role tenantUserRole = roleService.findOrCreateTenantUserRole();
            log.trace("Executing findOrCreateUserGroupPermission, ParentEntityId [{}], groupType [{}], groupName [{}]", tenantId, tenantUsers.getType(), tenantUsers.getName());
            findOrCreateUserGroupPermission(tenantId, tenantUsers.getId(), tenantUserRole.getId());
        } catch (Exception e) {
            log.trace("Unexpected error during execution findOrCreateTenantUserRole & findOrCreateUserGroupPermission: ", e);
        } finally {
            roleCreationLock.unlock();
        }
        return tenantUsers;
    }

    @Override
    public EntityGroup findOrCreateTenantAdminsGroup(TenantId tenantId) {

        // User Group 'Tenant Administrators' -> 'Tenant Administrator' role -> All permissions

        EntityGroup tenantAdmins = findOrCreateUserGroup(tenantId,
                tenantId, EntityGroup.GROUP_TENANT_ADMINS_NAME, "Autogenerated Tenant Administrators group with all permissions.");
        roleCreationLock.lock();
        try {
            log.trace("Executing findOrCreateTenantAdminRole, TenantId [{}]", tenantId);
            Role tenantAdminRole = roleService.findOrCreateTenantAdminRole();
            log.trace("Executing findOrCreateUserGroupPermission, ParentEntityId [{}], groupType [{}], groupName [{}]", tenantId, tenantAdmins.getType(), tenantAdmins.getName());
            findOrCreateUserGroupPermission(tenantId, tenantAdmins.getId(), tenantAdminRole.getId());
        } catch (Exception e) {
            log.trace("Unexpected error during execution findOrCreateTenantAdminRole & findOrCreateUserGroupPermission: ", e);
        } finally {
            roleCreationLock.unlock();
        }
        return tenantAdmins;
    }

    @Override
    public EntityGroup findOrCreateCustomerUsersGroup(TenantId tenantId, CustomerId customerId, CustomerId parentCustomerId) {

        // User Group 'Customer Users' -> 'Customer User' role -> Read only permissions

        EntityGroup customerUsers = findOrCreateUserGroup(tenantId, customerId,
                EntityGroup.GROUP_CUSTOMER_USERS_NAME, "Autogenerated Customer Users group with read-only permissions.");
        roleCreationLock.lock();
        try {
            log.trace("Executing findOrCreateCustomerUserRole, TenantId [{}]", tenantId);
            Role customerUserRole = roleService.findOrCreateCustomerUserRole(tenantId, parentCustomerId);
            log.trace("Executing findOrCreateUserGroupPermission, ParentEntityId [{}], groupType [{}], groupName [{}]", tenantId, customerUsers.getType(), customerUsers.getName());
            findOrCreateUserGroupPermission(tenantId, customerUsers.getId(), customerUserRole.getId());
        } catch (Exception e) {
            log.trace("Unexpected error during execution findOrCreateCustomerUserRole & findOrCreateUserGroupPermission: ", e);
        } finally {
            roleCreationLock.unlock();
        }
        return customerUsers;
    }

    @Override
    public EntityGroup findOrCreateCustomerAdminsGroup(TenantId tenantId, CustomerId customerId, CustomerId parentCustomerId) {

        // User Group 'Customer Administrators' -> 'Customer Administrator' role -> All permissions

        EntityGroup customerAdmins = findOrCreateUserGroup(tenantId, customerId,
                EntityGroup.GROUP_CUSTOMER_ADMINS_NAME, "Autogenerated Customer Administrators group with all permissions.");
        roleCreationLock.lock();
        try {
            log.trace("Executing findOrCreateCustomerAdminRole, TenantId [{}]", tenantId);
            Role customerAdminRole = roleService.findOrCreateCustomerAdminRole(tenantId, parentCustomerId);
            log.trace("Executing findOrCreateUserGroupPermission, ParentEntityId [{}], groupType [{}], groupName [{}]", tenantId, customerAdmins.getType(), customerAdmins.getName());
            findOrCreateUserGroupPermission(tenantId, customerAdmins.getId(), customerAdminRole.getId());
        } catch (Exception e) {
            log.trace("Unexpected error during execution findOrCreateCustomerAdminRole & findOrCreateUserGroupPermission: ", e);
        } finally {
            roleCreationLock.unlock();
        }
        return customerAdmins;
    }

    @Override
    public EntityGroup findOrCreatePublicUsersGroup(TenantId tenantId, CustomerId customerId) {
        EntityGroup publicUsers = findOrCreateUserGroup(tenantId, customerId,
                EntityGroup.GROUP_PUBLIC_USERS_NAME, "Autogenerated Public Users group with read-only permissions.");
        roleCreationLock.lock();
        try {
            log.trace("Executing findOrCreatePublicUserRole, TenantId [{}]", tenantId);
            Role publicUserRole = roleService.findOrCreatePublicUserRole(tenantId, customerId);
            log.trace("Executing findOrCreateUserGroupPermission, ParentEntityId [{}], groupType [{}], groupName [{}]", tenantId, publicUsers.getType(), publicUsers.getName());
            findOrCreateUserGroupPermission(tenantId, publicUsers.getId(), publicUserRole.getId());
        } catch (Exception e) {
            log.trace("Unexpected error during execution findOrCreatePublicUserRole & findOrCreateUserGroupPermission: ", e);
        } finally {
            roleCreationLock.unlock();
        }
        return publicUsers;
    }

    @Override
    public EntityGroup findOrCreateReadOnlyEntityGroupForCustomer(TenantId tenantId, CustomerId customerId, EntityType groupType) {

        Customer customer = customerService.findCustomerById(tenantId, customerId);
        if (customer == null) {
            throw new RuntimeException("Customer with id '" + customerId.getId().toString() + "' is not present in database!");
        }

        String groupName = customer.getTitle();
        String description = "Autogenerated ";
        if (customer.isPublic()) {
            description += "Public ";
        }
        switch (groupType) {
            case DEVICE -> {
                groupName += " Devices";
                description += "Device";
            }
            case ASSET -> {
                groupName += " Assets";
                description += "Asset";
            }
            case ENTITY_VIEW -> {
                groupName += " Entity Views";
                description += "Entity View";
            }
            case EDGE -> {
                groupName += " Edges";
                description += "Edge";
            }
            case DASHBOARD -> {
                groupName += " Dashboards";
                description += "Dashboard";
            }
            case AGENT -> {
                groupName += " Agents";
                description += "Agent";
            }
            default -> throw new RuntimeException("Invalid entity group type '" + groupType + "' specified for read-only entity group for customer!");
        }
        if (customer.isPublic()) {
            description += " group";
        } else {
            description += " group with read-only access for customer '" + customer.getTitle() + "'";
        }
        EntityGroup group;
        if (customer.isPublic()) {
            group = findOrCreateEntityGroup(tenantId, tenantId, groupType, groupName, description, customer.getId());
            EntityGroup publicUsers = findOrCreatePublicUsersGroup(tenantId, customer.getId());
            Role publicUserEntityGroupRole = roleService.findOrCreatePublicUsersEntityGroupRole(tenantId, customer.getId());
            findOrCreateEntityGroupPermission(tenantId, group.getId(), group.getType(), publicUsers.getId(), publicUserEntityGroupRole.getId(), true);
        } else {
            group = findOrCreateEntityGroup(tenantId, tenantId, groupType, groupName, description, null);
            Role readOnlyGroupRole = roleService.findOrCreateReadOnlyEntityGroupRole(tenantId, null);
            EntityGroup customerUsers = findOrCreateCustomerUsersGroup(tenantId, customer.getId(), null);
            findOrCreateEntityGroupPermission(tenantId, group.getId(), group.getType(), customerUsers.getId(), readOnlyGroupRole.getId(), false);
        }
        return group;
    }

    @Override
    public ListenableFuture<Optional<EntityGroup>> findPublicUserGroupAsync(TenantId tenantId, CustomerId publicCustomerId) {
        log.trace("Executing findPublicUserGroupAsync, tenantId [{}], publicCustomerId [{}]", tenantId, publicCustomerId);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validateId(publicCustomerId, id -> INCORRECT_CUSTOMER_ID + id);
        return findEntityGroupByTypeAndNameAsync(tenantId, publicCustomerId, EntityType.USER, EntityGroup.GROUP_PUBLIC_USERS_NAME);
    }

    private GroupPermission findOrCreateUserGroupPermission(TenantId tenantId, EntityGroupId userGroupId, RoleId roleId) {
        List<GroupPermission> userGroupPermissions =
                groupPermissionService.findGroupPermissionByTenantIdAndUserGroupIdAndRoleId(tenantId,
                        userGroupId, roleId, new PageLink(Integer.MAX_VALUE)).getData();
        if (userGroupPermissions.isEmpty()) {
            GroupPermission userGroupPermission = new GroupPermission();
            userGroupPermission.setTenantId(tenantId);
            userGroupPermission.setUserGroupId(userGroupId);
            userGroupPermission.setRoleId(roleId);
            return groupPermissionService.saveGroupPermission(tenantId, userGroupPermission);
        } else {
            return userGroupPermissions.get(0);
        }
    }

    private GroupPermission findOrCreateEntityGroupPermission(TenantId tenantId, EntityGroupId entityGroupId,
                                                              EntityType entityGroupType,
                                                              EntityGroupId userGroupId, RoleId roleId, boolean isPublic) {
        List<GroupPermission> entityGroupPermissions =
                groupPermissionService.findGroupPermissionByTenantIdAndEntityGroupIdAndUserGroupIdAndRoleId(tenantId,
                        entityGroupId, userGroupId, roleId, new PageLink(Integer.MAX_VALUE)).getData();
        if (entityGroupPermissions.isEmpty()) {
            GroupPermission entityGroupPermission = new GroupPermission();
            entityGroupPermission.setTenantId(tenantId);
            entityGroupPermission.setEntityGroupId(entityGroupId);
            entityGroupPermission.setEntityGroupType(entityGroupType);
            entityGroupPermission.setUserGroupId(userGroupId);
            entityGroupPermission.setRoleId(roleId);
            entityGroupPermission.setPublic(isPublic);
            return groupPermissionService.saveGroupPermission(tenantId, entityGroupPermission);
        } else {
            return entityGroupPermissions.get(0);
        }
    }

    @Override
    public void deleteEntityGroup(TenantId tenantId, EntityGroupId entityGroupId) {
        log.trace("Executing deleteEntityGroup [{}]", entityGroupId);
        validateId(entityGroupId, id -> INCORRECT_ENTITY_GROUP_ID + id);
        deleteEntityGroup(tenantId, entityGroupId, false);
    }

    private void deleteEntityGroup(TenantId tenantId, EntityGroupId entityGroupId, boolean force) {
        EntityGroup entityGroup = findEntityGroupById(tenantId, entityGroupId);
        if (entityGroup == null) {
            if (force) {
                return;
            } else {
                throw new IncorrectParameterException("Unable to delete non-existent entity group.");
            }
        }
        groupPermissionService.deleteGroupPermissionsByTenantIdAndUserGroupId(tenantId, entityGroupId);
        groupPermissionService.deleteGroupPermissionsByTenantIdAndEntityGroupId(tenantId, entityGroupId);
        entityGroupDao.removeById(tenantId, entityGroupId.getId());
        publishEvictEvent(new EntityGroupEvictEvent(entityGroup.getOwnerId(), entityGroup.getType(), entityGroup.getName(), null));
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(entityGroupId).build());
    }

    @Override
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        deleteEntityGroup(tenantId, (EntityGroupId) id, force);
    }

    @Override
    public PageData<EntityGroup> findAllEntityGroupsByParentRelation(TenantId tenantId, EntityId parentEntityId, PageLink pageLink) {
        log.trace("Executing findAllEntityGroupsByParentRelation, parentEntityId [{}], pageLink [{}]", parentEntityId, pageLink);
        validateEntityId(parentEntityId, id -> INCORRECT_PARENT_ENTITY_ID + id);
        validatePageLink(pageLink);
        return this.entityGroupDao.findAllEntityGroupsByParentRelation(tenantId.getId(), parentEntityId.getId(), parentEntityId.getEntityType(), pageLink);
    }

    @Override
    public void deleteAllEntityGroups(TenantId tenantId, EntityId parentEntityId) {
        log.trace("Executing deleteAllEntityGroups, parentEntityId [{}]", parentEntityId);
        validateEntityId(parentEntityId, id -> INCORRECT_PARENT_ENTITY_ID + id);
        entityGroupsRemover.removeEntities(tenantId, parentEntityId);
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        deleteAllEntityGroups(tenantId, tenantId);
    }

    @Override
    public PageData<EntityGroup> findEntityGroupsByType(TenantId tenantId, EntityId parentEntityId,
                                                        EntityType groupType, PageLink pageLink) {
        log.trace("Executing findEntityGroupsByType, parentEntityId [{}], groupType [{}], pageLink [{}]", parentEntityId, groupType, pageLink);
        validateEntityId(parentEntityId, id -> INCORRECT_PARENT_ENTITY_ID + id);
        if (groupType == null) {
            throw new IncorrectParameterException(INCORRECT_GROUP_TYPE + groupType);
        }
        validatePageLink(pageLink);
        return this.entityGroupDao.findEntityGroupsByType(tenantId.getId(), parentEntityId.getId(),
                parentEntityId.getEntityType(), groupType, pageLink);
    }

    @Override
    public PageData<EntityGroupInfo> findEntityGroupInfosByType(TenantId tenantId, EntityId parentEntityId, EntityType groupType, PageLink pageLink) {
        log.trace("Executing findEntityGroupInfosByType, parentEntityId [{}], groupType [{}], pageLink [{}]", parentEntityId, groupType, pageLink);
        validateEntityId(parentEntityId, id -> INCORRECT_PARENT_ENTITY_ID + id);
        if (groupType == null) {
            throw new IncorrectParameterException(INCORRECT_GROUP_TYPE + groupType);
        }
        validatePageLink(pageLink);
        return this.entityGroupInfoDao.findEntityGroupsByType(tenantId.getId(), parentEntityId.getId(),
                parentEntityId.getEntityType(), groupType, pageLink);
    }

    @Override
    public PageData<EntityInfo> findEntityGroupEntityInfosByType(TenantId tenantId, EntityId parentEntityId, EntityType groupType, PageLink pageLink) {
        log.trace("Executing findEntityGroupEntityInfosByType, parentEntityId [{}], groupType [{}], pageLink [{}]", parentEntityId, groupType, pageLink);
        validateEntityId(parentEntityId, id -> INCORRECT_PARENT_ENTITY_ID + id);
        if (groupType == null) {
            throw new IncorrectParameterException(INCORRECT_GROUP_TYPE + groupType);
        }
        validatePageLink(pageLink);
        return this.entityGroupInfoDao.findEntityGroupEntityInfosByType(tenantId.getId(), parentEntityId.getId(),
                parentEntityId.getEntityType(), groupType, pageLink);
    }

    @Override
    public PageData<EntityGroupInfo> findEntityGroupInfosByOwnersAndType(TenantId tenantId, List<EntityId> ownerIds, EntityType groupType, PageLink pageLink) {
        log.trace("Executing findEntityGroupInfosByOwnersAndType, ownerIds [{}], groupType [{}], pageLink [{}]", ownerIds, groupType, pageLink);
        validateEntityIds(ownerIds, ids -> INCORRECT_OWNER_ENTITY_IDS + ids);
        if (groupType == null) {
            throw new IncorrectParameterException(INCORRECT_GROUP_TYPE + groupType);
        }
        validatePageLink(pageLink);
        return this.entityGroupInfoDao.findEntityGroupsByOwnerIdsAndType(tenantId.getId(),
                ownerIds.stream().map(EntityId::getId).collect(Collectors.toList()), groupType, pageLink);
    }

    @Override
    public PageData<EntityInfo> findEntityGroupEntityInfosByOwnersAndType(TenantId tenantId, List<EntityId> ownerIds, EntityType groupType, PageLink pageLink) {
        log.trace("Executing findEntityGroupEntityInfosByOwnersAndType, ownerIds [{}], groupType [{}], pageLink [{}]", ownerIds, groupType, pageLink);
        validateEntityIds(ownerIds, ids -> INCORRECT_OWNER_ENTITY_IDS + ids);
        if (groupType == null) {
            throw new IncorrectParameterException(INCORRECT_GROUP_TYPE + groupType);
        }
        validatePageLink(pageLink);
        return this.entityGroupInfoDao.findEntityGroupEntityInfosByOwnerIdsAndType(tenantId.getId(),
                ownerIds.stream().map(EntityId::getId).collect(Collectors.toList()), groupType, pageLink);
    }

    @Override
    public PageData<EntityGroupInfo> findEntityGroupInfosByIds(TenantId tenantId, List<EntityGroupId> entityGroupIds, PageLink pageLink) {
        log.trace("Executing findEntityGroupInfosByIds, entityGroupIds [{}], pageLink [{}]", entityGroupIds, pageLink);
        validateIds(entityGroupIds, ids -> "Incorrect entityGroupIds " + ids);
        validatePageLink(pageLink);
        return entityGroupInfoDao.findEntityGroupsByIds(tenantId.getId(), toUUIDs(entityGroupIds), pageLink);
    }

    @Override
    public PageData<EntityInfo> findEntityGroupEntityInfosByIds(TenantId tenantId, List<EntityGroupId> entityGroupIds, PageLink pageLink) {
        log.trace("Executing findEntityGroupEntityInfosByIds, entityGroupIds [{}], pageLink [{}]", entityGroupIds, pageLink);
        validateIds(entityGroupIds, ids -> "Incorrect entityGroupIds " + ids);
        validatePageLink(pageLink);
        return entityGroupInfoDao.findEntityGroupEntityInfosByIds(tenantId.getId(), toUUIDs(entityGroupIds), pageLink);
    }

    @Override
    public PageData<EntityGroupInfo> findEntityGroupInfosByTypeOrIds(TenantId tenantId, EntityId parentEntityId, EntityType groupType,
                                                                     List<EntityGroupId> entityGroupIds, PageLink pageLink) {
        log.trace("Executing findEntityGroupInfosByTypeOrIds, parentEntityId [{}], groupType [{}], entityGroupIds [{}], pageLink [{}]",
                parentEntityId, groupType, entityGroupIds, pageLink);
        validateEntityId(parentEntityId, id -> INCORRECT_PARENT_ENTITY_ID + id);
        if (groupType == null) {
            throw new IncorrectParameterException(INCORRECT_GROUP_TYPE + groupType);
        }
        validateIds(entityGroupIds, ids -> "Incorrect entityGroupIds " + ids);
        validatePageLink(pageLink);
        return this.entityGroupInfoDao.findEntityGroupsByTypeOrIds(tenantId.getId(), parentEntityId.getId(),
                parentEntityId.getEntityType(), groupType, toUUIDs(entityGroupIds), pageLink);
    }

    @Override
    public PageData<EntityInfo> findEntityGroupEntityInfosByTypeOrIds(TenantId tenantId, EntityId parentEntityId, EntityType groupType, List<EntityGroupId> entityGroupIds, PageLink pageLink) {
        log.trace("Executing findEntityGroupEntityInfosByTypeOrIds, parentEntityId [{}], groupType [{}], entityGroupIds [{}], pageLink [{}]",
                parentEntityId, groupType, entityGroupIds, pageLink);
        validateEntityId(parentEntityId, id -> INCORRECT_PARENT_ENTITY_ID + id);
        if (groupType == null) {
            throw new IncorrectParameterException(INCORRECT_GROUP_TYPE + groupType);
        }
        validateIds(entityGroupIds, ids -> "Incorrect entityGroupIds " + ids);
        validatePageLink(pageLink);
        return this.entityGroupInfoDao.findEntityGroupEntityInfosByTypeOrIds(tenantId.getId(), parentEntityId.getId(),
                parentEntityId.getEntityType(), groupType, toUUIDs(entityGroupIds), pageLink);
    }

    @Override
    public PageData<EntityGroupInfo> findEdgeEntityGroupInfosByOwnerIdType(TenantId tenantId, EdgeId edgeId, EntityId ownerId, EntityType groupType, PageLink pageLink) {
        log.trace("[{}] Executing findEdgeEntityGroupInfosByOwnerIdType, edgeId [{}], ownerId [{}], groupType [{}], pageLink [{}]", tenantId, edgeId, ownerId, groupType, pageLink);
        Validator.validateId(tenantId, id -> "Incorrect tenantId " + id);
        Validator.validateId(edgeId, id -> "Incorrect edgeId " + id);
        Validator.validateEntityId(ownerId, id -> "Incorrect ownerId " + id);
        if (groupType == null) {
            throw new IncorrectParameterException(INCORRECT_GROUP_TYPE + groupType);
        }
        validatePageLink(pageLink);
        String relationType = EDGE_ENTITY_GROUP_RELATION_PREFIX + groupType.name();
        return this.entityGroupInfoDao.findEdgeEntityGroupsByOwnerIdAndType(tenantId.getId(), edgeId.getId(), ownerId.getId(), ownerId.getEntityType(), relationType, pageLink);
    }

    @Override
    public PageData<EntityGroup> findEntityGroupsByType(TenantId tenantId, EntityType groupType, PageLink pageLink) {
        log.trace("Executing findEntityGroupsByType, tenantId [{}], groupType [{}], pageLink [{}]", tenantId, groupType, pageLink);
        validateEntityId(tenantId, id -> INCORRECT_TENANT_ID + id);
        if (groupType == null) {
            throw new IncorrectParameterException(INCORRECT_GROUP_TYPE + groupType);
        }
        validatePageLink(pageLink);
        return this.entityGroupDao.findEntityGroupsByType(tenantId.getId(), groupType, pageLink);
    }

    @Override
    public Optional<EntityGroup> findEntityGroupByTypeAndName(TenantId tenantId, EntityId parentEntityId, EntityType groupType, String name) {
        return findEntityGroupByTypeAndName(tenantId, parentEntityId, groupType, name, true);
    }

    @Override
    public Optional<EntityGroup> findEntityGroupByTypeAndName(TenantId tenantId, EntityId parentEntityId, EntityType groupType, String name, boolean putInCache) {
        log.trace("Executing findEntityGroupByTypeAndName, parentEntityId [{}], groupType [{}], name [{}]", parentEntityId, groupType, name);
        return Optional.ofNullable(cache.getOrFetchFromDB(new EntityGroupCacheKey(parentEntityId, groupType, name),
                () -> entityGroupDao.findEntityGroupByTypeAndName(tenantId.getId(), parentEntityId.getId(),
                        parentEntityId.getEntityType(), groupType, name).orElse(null), true, putInCache));
    }

    @Override
    public Optional<EntityGroupInfo> findEntityGroupInfoByTypeAndName(TenantId tenantId, EntityId parentEntityId, EntityType groupType, String name) {
        log.trace("Executing findEntityGroupInfoByTypeAndName, parentEntityId [{}], groupType [{}], name [{}]", parentEntityId, groupType, name);
        return this.entityGroupInfoDao.findEntityGroupByTypeAndName(tenantId.getId(), parentEntityId.getId(),
                parentEntityId.getEntityType(), groupType, name);
    }

    @Override
    public ListenableFuture<Optional<EntityGroup>> findEntityGroupByTypeAndNameAsync(TenantId tenantId, EntityId parentEntityId, EntityType groupType, String name) {
        log.trace("Executing findEntityGroupByTypeAndNameAsync, parentEntityId [{}], groupType [{}], name [{}]", parentEntityId, groupType, name);
        validateAndComposeRelationType(parentEntityId, groupType, name);
        return executorService.submit(() -> findEntityGroupByTypeAndName(tenantId, parentEntityId, groupType, name));
    }

    private String validateAndComposeRelationType(EntityId parentEntityId, EntityType groupType, String name) {
        validateEntityId(parentEntityId, id -> INCORRECT_PARENT_ENTITY_ID + id);
        if (groupType == null) {
            throw new IncorrectParameterException(INCORRECT_GROUP_TYPE + groupType);
        }
        validateString(name, n -> "Incorrect name " + n);
        return ENTITY_GROUP_RELATION_PREFIX + groupType.name();
    }

    @Override
    public void addEntityToEntityGroup(TenantId tenantId, EntityGroupId entityGroupId, EntityId entityId) {
        log.trace("Executing addEntityToEntityGroup, entityGroupId [{}], entityId [{}]", entityGroupId, entityId);
        validateId(entityGroupId, id -> INCORRECT_ENTITY_GROUP_ID + id);
        validateEntityId(entityId, id -> INCORRECT_ENTITY_ID + id);
        validator.validateIfGroupEntity(entityId);
        EntityGroup entityGroup = entityGroupService.findEntityGroupById(tenantId, entityGroupId);
        checkNotNull(entityGroup, "Entity group with id '" + entityGroupId + "' does not exist.");
        validator.validateEntityTypesMatch(entityId, entityGroup);
        EntityRelation entityRelation = new EntityRelation();
        entityRelation.setFrom(entityGroupId);
        entityRelation.setTo(entityId);
        entityRelation.setTypeGroup(RelationTypeGroup.FROM_ENTITY_GROUP);
        entityRelation.setType(EntityRelation.CONTAINS_TYPE);
        relationService.saveRelation(tenantId, entityRelation);
        eventPublisher.publishEvent(ActionEntityEvent.builder()
                .tenantId(tenantId)
                .entityId(entityId)
                .actionType(ActionType.ADDED_TO_ENTITY_GROUP)
                .entityGroup(entityGroup).build());
    }

    @Override
    public void addEntitiesToEntityGroup(TenantId tenantId, EntityGroupId entityGroupId, List<EntityId> entityIds) {
        log.trace("Executing addEntitiesToEntityGroup, entityGroupId [{}], entityIds [{}]", entityGroupId, entityIds);
        validateId(entityGroupId, id -> INCORRECT_ENTITY_GROUP_ID + id);
        EntityGroup entityGroup = entityGroupService.findEntityGroupById(tenantId, entityGroupId);
        checkNotNull(entityGroup, "Entity group with id '" + entityGroupId + "' does not exist.");
        for (EntityId entityId : entityIds) {
            validateEntityId(entityId, id -> INCORRECT_ENTITY_ID + id);
            validator.validateIfGroupEntity(entityId);
            validator.validateEntityTypesMatch(entityId, entityGroup);
        }
        var relations = entityIds.stream().map(entityId -> {
            EntityRelation entityRelation = new EntityRelation();
            entityRelation.setFrom(entityGroupId);
            entityRelation.setTo(entityId);
            entityRelation.setTypeGroup(RelationTypeGroup.FROM_ENTITY_GROUP);
            entityRelation.setType(EntityRelation.CONTAINS_TYPE);
            return entityRelation;
        }).collect(Collectors.toList());
        relationService.saveRelations(tenantId, relations);
        for (EntityId entityId : entityIds) {
            eventPublisher.publishEvent(ActionEntityEvent.builder()
                    .tenantId(tenantId)
                    .entityId(entityId)
                    .actionType(ActionType.ADDED_TO_ENTITY_GROUP)
                    .entityGroup(entityGroup).build());
        }
    }

    @Override
    public void addEntityToEntityGroupAll(TenantId tenantId, EntityId parentEntityId, EntityId entityId) {
        log.trace("Executing addEntityToEntityGroupAll, parentEntityId [{}], entityId [{}]", parentEntityId, entityId);
        validateEntityId(parentEntityId, id -> INCORRECT_PARENT_ENTITY_ID + id);
        validateEntityId(entityId, id -> INCORRECT_ENTITY_ID + id);
        try {
            Optional<EntityGroup> entityGroup = findEntityGroupByTypeAndName(tenantId, parentEntityId, entityId.getEntityType(), EntityGroup.GROUP_ALL_NAME);
            if (entityGroup.isPresent()) {
                addEntityToEntityGroup(tenantId, entityGroup.get().getId(), entityId);
            } else {
                throw new DataValidationException("Group All of type " + entityId.getEntityType() + " is absent for entityId " + parentEntityId);
            }
        } catch (Exception e) {
            log.error("Unable to add entity to group All", e);
        }
    }

    @Override
    public void removeEntityFromEntityGroup(TenantId tenantId, EntityGroupId entityGroupId, EntityId entityId) {
        removeEntitiesFromEntityGroup(tenantId, entityGroupId, Collections.singletonList(entityId));
    }

    @Override
    public void removeEntitiesFromEntityGroup(TenantId tenantId, EntityGroupId entityGroupId, List<EntityId> entityIds) {
        log.trace("Executing removeEntitiesFromEntityGroup, entityGroupId [{}], entityIds [{}]", entityGroupId, entityIds);
        validateId(entityGroupId, id -> INCORRECT_ENTITY_GROUP_ID + id);
        EntityGroup entityGroup = entityGroupService.findEntityGroupById(tenantId, entityGroupId);
        if (isTenantAdminUserGroup(entityGroup)) {
            if (containsLastTenantAdmin(tenantId, entityIds.stream().map(UserId.class::cast).collect(Collectors.toList()))) {
                throw new IncorrectParameterException("At least one tenant administrator must remain!");
            }
        }
        entityIds.forEach(entityId -> removeEntityFromEntityGroup(tenantId, entityId, entityGroup));
    }

    private void removeEntityFromEntityGroup(TenantId tenantId, EntityId entityId, EntityGroup entityGroup) {
        log.trace("Executing removeEntityFromEntityGroup, entityGroupId [{}], entityId [{}]", entityGroup.getId(), entityId);
        validateId(entityGroup.getId(), id -> INCORRECT_ENTITY_GROUP_ID + id);
        validateEntityId(entityId, id -> INCORRECT_ENTITY_ID + id);
        relationService.deleteRelation(tenantId, entityGroup.getId(), entityId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.FROM_ENTITY_GROUP);
        eventPublisher.publishEvent(ActionEntityEvent.builder()
                .tenantId(tenantId)
                .entityId(entityId)
                .actionType(ActionType.REMOVED_FROM_ENTITY_GROUP)
                .entityGroup(entityGroup).build());
    }

    @Override
    public ShortEntityView findGroupEntity(TenantId tenantId, CustomerId customerId, MergedUserPermissions userPermissions, EntityGroupId entityGroupId, EntityId entityId) {
        log.trace("Executing findGroupEntity, entityGroupId [{}], entityId [{}]", entityGroupId, entityId);
        validateId(entityGroupId, id -> INCORRECT_ENTITY_GROUP_ID + id);
        validateEntityId(entityId, id -> INCORRECT_ENTITY_ID + id);

        if (!isEntityInGroup(tenantId, entityId, entityGroupId)) {
            throw new IncorrectParameterException(String.format("Entity %s not present in entity group %s.", entityId, entityGroupId));
        }

        EntityGroup entityGroup = findEntityGroupById(tenantId, entityGroupId);
        if (entityGroup == null) {
            throw new IncorrectParameterException(UNABLE_TO_FIND_ENTITY_GROUP_BY_ID + entityGroupId);
        }
        List<ColumnConfiguration> columns = getEntityGroupColumns(entityGroup);

        SingleEntityFilter singleEntityFilter = new SingleEntityFilter();
        singleEntityFilter.setSingleEntity(AliasEntityId.fromEntityId(entityId));

        List<ColumnConfiguration> entityFieldsColumns = new ArrayList<>();
        List<ColumnConfiguration> latestValuesColumns = new ArrayList<>();

        columns.forEach(column -> {
            if (column.getType().equals(ColumnType.ENTITY_FIELD)) {
                entityFieldsColumns.add(column);
            } else {
                latestValuesColumns.add(column);
            }
        });

        List<EntityKey> entityFields = entityFieldsColumns.stream().map(this::columnToEntityKey).collect(Collectors.toList());
        List<EntityKey> latestValues = latestValuesColumns.stream().map(this::columnToEntityKey).collect(Collectors.toList());

        EntityDataQuery dataQuery = new EntityDataQuery(singleEntityFilter, new EntityDataPageLink(), entityFields, latestValues, Collections.emptyList());
        PageData<EntityData> entityDataByQuery = entityQueryDao.findEntityDataByQuery(tenantId, customerId, userPermissions, dataQuery);

        return entityDataToShortEntityView(entityDataByQuery.getData().get(0));
    }

    @Override
    public PageData<ShortEntityView> findGroupEntities(TenantId tenantId, CustomerId customerId, MergedUserPermissions userPermissions, EntityGroupId entityGroupId, PageLink pageLink) {
        log.trace("Executing findGroupEntities, entityGroupId [{}], pageLink [{}]", entityGroupId, pageLink);
        validateId(entityGroupId, id -> INCORRECT_ENTITY_GROUP_ID + id);
        validatePageLink(pageLink);
        EntityGroup entityGroup = findEntityGroupById(tenantId, entityGroupId);
        if (entityGroup == null) {
            throw new IncorrectParameterException(UNABLE_TO_FIND_ENTITY_GROUP_BY_ID + entityGroupId);
        }
        List<ColumnConfiguration> columns = getEntityGroupColumns(entityGroup);

        EntityGroupFilter entityGroupFilter = new EntityGroupFilter();
        entityGroupFilter.setEntityGroup(entityGroupId.getId().toString());
        entityGroupFilter.setGroupType(entityGroup.getType());

        List<ColumnConfiguration> entityFieldsColumns = new ArrayList<>();
        List<ColumnConfiguration> latestValuesColumns = new ArrayList<>();

        columns.forEach(column -> {
            if (column.getType().equals(ColumnType.ENTITY_FIELD)) {
                entityFieldsColumns.add(column);
            } else {
                latestValuesColumns.add(column);
            }
        });

        List<EntityKey> entityFields = entityFieldsColumns.stream().map(this::columnToEntityKey).collect(Collectors.toList());
        List<EntityKey> latestValues = latestValuesColumns.stream().map(this::columnToEntityKey).collect(Collectors.toList());

        EntityDataSortOrder sortOrder = null;
        if (pageLink.getSortOrder() != null && !StringUtils.isEmpty(pageLink.getSortOrder().getProperty())) {
            String property = pageLink.getSortOrder().getProperty();
            for (ColumnConfiguration column : columns) {
                if (column.getKey().equals(property)) {
                    sortOrder = new EntityDataSortOrder(columnToEntityKey(column), EntityDataSortOrder.Direction.valueOf(pageLink.getSortOrder().getDirection().name()));
                    break;
                }
            }
        } else {
            for (ColumnConfiguration column : columns) {
                if (column.getSortOrder() != null && !column.getSortOrder().equals(SortOrder.NONE)) {
                    sortOrder = new EntityDataSortOrder(columnToEntityKey(column), EntityDataSortOrder.Direction.valueOf(column.getSortOrder().name()));
                    break;
                }
            }
        }

        EntityDataPageLink entityDataPageLink = new EntityDataPageLink(pageLink.getPageSize(), pageLink.getPage(), pageLink.getTextSearch(), sortOrder);
        EntityDataQuery dataQuery = new EntityDataQuery(entityGroupFilter, entityDataPageLink, entityFields, latestValues, Collections.emptyList());
        PageData<EntityData> entityDataByQuery = entityQueryDao.findEntityDataByQuery(tenantId, customerId, userPermissions, dataQuery);

        return new PageData<>(
                entityDataByQuery.getData().stream().map(this::entityDataToShortEntityView).collect(Collectors.toList()),
                entityDataByQuery.getTotalPages(),
                entityDataByQuery.getTotalElements(),
                entityDataByQuery.hasNext());
    }

    private ShortEntityView entityDataToShortEntityView(EntityData entityData) {
        ShortEntityView entityView = new ShortEntityView(entityData.getEntityId());
        entityData.getLatest().forEach((type, map) -> map.forEach((k, v) -> {
            String key;
            switch (type) {
                case ENTITY_FIELD -> {
                    EntityType entityType = entityData.getEntityId().getEntityType();
                    if (k.equals("type") && (entityType.equals(EntityType.DEVICE) || entityType.equals(EntityType.ASSET))) {
                        key = getProfileColumnKeyByType(entityType);
                    } else {
                        key = entityDataKeyToShortEntityViewKeyMap.getOrDefault(k, k);
                    }
                }
                case CLIENT_ATTRIBUTE -> key = "client_" + k;
                case SHARED_ATTRIBUTE -> key = "shared_" + k;
                case SERVER_ATTRIBUTE -> key = "server_" + k;
                default -> key = k;
            }
            entityView.put(key, v.getValue());
        }));
        return entityView;
    }

    private EntityKey columnToEntityKey(ColumnConfiguration column) {
        EntityKeyType entityKeyType = column.getType().getEntityKeyType();
        String key;

        if (entityKeyType.equals(EntityKeyType.ENTITY_FIELD)) {
            key = columnToEntityKeyMap.getOrDefault(column.getKey(), column.getKey());
        } else {
            key = column.getKey();
        }

        return new EntityKey(entityKeyType, key);
    }

    private final Map<String, String> entityDataKeyToShortEntityViewKeyMap = new HashMap<>();

    private final Map<String, String> columnToEntityKeyMap = new HashMap<>();

    {
        columnToEntityKeyMap.put("created_time", "createdTime");
        columnToEntityKeyMap.put("assigned_customer", "assignedCustomer");
        columnToEntityKeyMap.put("first_name", "firstName");
        columnToEntityKeyMap.put("last_name", "lastName");
        columnToEntityKeyMap.put("device_profile", "type");
        columnToEntityKeyMap.put("asset_profile", "type");

        entityDataKeyToShortEntityViewKeyMap.put("createdTime", "created_time");
        entityDataKeyToShortEntityViewKeyMap.put("assignedCustomer", "assigned_customer");
        entityDataKeyToShortEntityViewKeyMap.put("firstName", "first_name");
        entityDataKeyToShortEntityViewKeyMap.put("lastName", "last_name");
    }

    @Override
    public ListenableFuture<List<EntityId>> findAllEntityIdsAsync(TenantId tenantId, EntityGroupId entityGroupId, PageLink pageLink) {
        log.trace("Executing findAllEntityIdsAsync, entityGroupId [{}], pageLink [{}]", entityGroupId, pageLink);
        validateId(entityGroupId, id -> INCORRECT_ENTITY_GROUP_ID + id);
        EntityGroup entityGroup = findEntityGroupById(tenantId, entityGroupId);
        if (entityGroup == null) {
            throw new IncorrectParameterException(UNABLE_TO_FIND_ENTITY_GROUP_BY_ID + entityGroupId);
        }
        return findEntityIds(tenantId, entityGroupId, entityGroup.getType(), pageLink);
    }

    @Override
    public PageData<EntityId> findEntityIds(TenantId tenantId, EntityType entityType, EntityGroupId entityGroupId, PageLink pageLink) {
        log.trace("Executing findEntitiesSync, entityGroupId [{}], pageLink [{}]", entityGroupId, pageLink);
        return entityGroupDao.findGroupEntityIdsSync(entityType, entityGroupId.getId(), pageLink);
    }

    @Override
    public ListenableFuture<List<EntityGroupId>> findEntityGroupsForEntityAsync(TenantId tenantId, EntityId entityId) {
        return executorService.submit(() -> {
            var relations = relationDao.findAllByToAndType(tenantId, entityId,
                    EntityRelation.CONTAINS_TYPE, RelationTypeGroup.FROM_ENTITY_GROUP);
            List<EntityGroupId> entityGroupIds = new ArrayList<>(relations.size());
            for (EntityRelation relation : relations) {
                entityGroupIds.add(new EntityGroupId(relation.getFrom().getId()));
            }
            return entityGroupIds;
        });
    }

    @Override
    public EntityGroup assignEntityGroupToEdge(TenantId tenantId, EntityGroupId entityGroupId, EdgeId edgeId, EntityType groupType) {
        EntityGroup entityGroup = findEntityGroupById(tenantId, entityGroupId);
        Edge edge = edgeService.findEdgeById(tenantId, edgeId);
        if (edge == null) {
            throw new DataValidationException("Can't assign entity group to non-existent edge!");
        }
        try {
            String relationType = EDGE_ENTITY_GROUP_RELATION_PREFIX + groupType.name();
            createRelation(tenantId, new EntityRelation(edgeId, entityGroupId, relationType, RelationTypeGroup.EDGE));
        } catch (Exception e) {
            log.warn("[{}] Failed to create entity group relation. Edge Id: [{}]", entityGroupId, edgeId);
            throw new RuntimeException(e);
        }
        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).edgeId(edgeId).entityId(entityGroupId)
                .actionType(ActionType.ASSIGNED_TO_EDGE).entityGroup(entityGroup).build());
        return entityGroup;
    }

    @Override
    public EntityGroup unassignEntityGroupFromEdge(TenantId tenantId, EntityGroupId entityGroupId, EdgeId edgeId, EntityType groupType) {
        EntityGroup entityGroup = findEntityGroupById(tenantId, entityGroupId);
        Edge edge = edgeService.findEdgeById(tenantId, edgeId);
        if (edge == null) {
            throw new DataValidationException("Can't unassign entity group from non-existent edge!");
        }
        try {
            String relationType = EDGE_ENTITY_GROUP_RELATION_PREFIX + groupType.name();
            deleteRelation(tenantId, new EntityRelation(edgeId, entityGroupId, relationType, RelationTypeGroup.EDGE));
        } catch (Exception e) {
            log.warn("[{}] Failed to delete entity group relation. Edge id: [{}]", entityGroupId, edgeId);
            throw new RuntimeException(e);
        }
        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).edgeId(edgeId).entityId(entityGroupId)
                .actionType(ActionType.UNASSIGNED_FROM_EDGE).entityGroup(entityGroup).build());
        return entityGroup;
    }

    @Override
    public PageData<EntityGroup> findEdgeEntityGroupsByType(TenantId tenantId, EdgeId edgeId, EntityType groupType, PageLink pageLink) {
        log.trace("[{}] Executing findEdgeEntityGroupsByType, edgeId [{}], groupType [{}], pageLink [{}]", tenantId, edgeId, groupType, pageLink);
        Validator.validateId(tenantId, id -> "Incorrect tenantId " + id);
        Validator.validateId(edgeId, id -> "Incorrect edgeId " + id);
        if (groupType == null) {
            throw new IncorrectParameterException(INCORRECT_GROUP_TYPE + groupType);
        }
        validatePageLink(pageLink);
        String relationType = EDGE_ENTITY_GROUP_RELATION_PREFIX + groupType.name();
        return this.entityGroupDao.findEdgeEntityGroupsByType(tenantId.getId(), edgeId.getId(), relationType, pageLink);
    }

    @Override
    public PageData<EntityGroup> findEdgeEntityGroupsByOwnerIdAndType(TenantId tenantId, EdgeId edgeId, EntityId ownerId, EntityType groupType, PageLink pageLink) {
        log.trace("[{}] Executing findEdgeEntityGroupsByOwnerIdAndType, edgeId [{}], ownerId [{}], groupType [{}], pageLink [{}]", tenantId, edgeId, ownerId, groupType, pageLink);
        Validator.validateId(tenantId, id -> "Incorrect tenantId " + id);
        Validator.validateId(edgeId, id -> "Incorrect edgeId " + id);
        validateEntityId(ownerId, id -> INCORRECT_ENTITY_ID + id);
        if (groupType == null) {
            throw new IncorrectParameterException(INCORRECT_GROUP_TYPE + groupType);
        }
        validatePageLink(pageLink);
        String relationType = EDGE_ENTITY_GROUP_RELATION_PREFIX + groupType.name();
        return this.entityGroupDao.findEdgeEntityGroupsByOwnerIdAndType(tenantId.getId(), edgeId.getId(), ownerId.getId(), relationType, pageLink);
    }

    @Override
    public ListenableFuture<Boolean> checkEntityGroupAssignedToEdgeAsync(TenantId tenantId, EdgeId edgeId, EntityGroupId entityGroupId, EntityType groupType) {
        log.trace("Executing checkEntityGroupAssignedToEdgeAsync, tenantId [{}], edgeId [{}], entityGroupId [{}]", tenantId, edgeId, entityGroupId);
        validateEntityId(entityGroupId, id -> INCORRECT_ENTITY_GROUP_ID + id);
        return relationService.checkRelationAsync(tenantId, edgeId, entityGroupId,
                EDGE_ENTITY_GROUP_RELATION_PREFIX + groupType.name()
                , RelationTypeGroup.EDGE);
    }

    @Override
    public ListenableFuture<EntityGroup> findOrCreateEdgeAllGroupAsync(TenantId tenantId, Edge edge, String edgeName, EntityType entityOwnerType, EntityType groupType) {
        log.trace("Executing findOrCreateEdgeAllGroupAsync, tenantId [{}], edge [{}], edgeName [{}], groupType [{}]", tenantId, edge, edgeName, groupType);
        Customer customer = null;
        if (EntityType.CUSTOMER.equals(edge.getOwnerId().getEntityType()) && EntityType.CUSTOMER.equals(entityOwnerType)) {
            customer = customerService.findCustomerById(tenantId, new CustomerId(edge.getOwnerId().getId()));
        }
        String customerName = customer != null ? customer.getName() : null;
        String entityGroupName = EdgeUtils.getEdgeGroupAllName(customerName, edgeName);
        ListenableFuture<Optional<EntityGroup>> futureEntityGroup = entityGroupService
                .findEntityGroupByTypeAndNameAsync(tenantId, edge.getOwnerId(), groupType, entityGroupName);
        return Futures.transformAsync(futureEntityGroup, optionalEntityGroup -> {
            if (optionalEntityGroup != null && optionalEntityGroup.isPresent()) {
                ListenableFuture<Boolean> groupAssignedToEdgeFuture =
                        entityGroupService.checkEntityGroupAssignedToEdgeAsync(tenantId, edge.getId(), optionalEntityGroup.get().getId(), groupType);
                return Futures.transformAsync(groupAssignedToEdgeFuture, groupAssignedToEdge -> {
                    if (!groupAssignedToEdge) {
                        entityGroupService.assignEntityGroupToEdge(tenantId, optionalEntityGroup.get().getId(),
                                edge.getId(), groupType);
                    }
                    return Futures.immediateFuture(optionalEntityGroup.get());
                }, MoreExecutors.directExecutor());
            } else {
                try {
                    ListenableFuture<Optional<EntityGroup>> currentEntityGroupFuture = entityGroupService
                            .findEntityGroupByTypeAndNameAsync(tenantId, edge.getOwnerId(), groupType, entityGroupName);
                    return Futures.transformAsync(currentEntityGroupFuture, currentEntityGroup -> {
                        if (currentEntityGroup.isEmpty()) {
                            EntityGroup entityGroup = createEntityGroup(entityGroupName, edge.getOwnerId(), tenantId, groupType);
                            entityGroupService.assignEntityGroupToEdge(tenantId, entityGroup.getId(),
                                    edge.getId(), groupType);
                            return Futures.immediateFuture(entityGroup);
                        } else {
                            return Futures.immediateFuture(currentEntityGroup.get());
                        }
                    }, MoreExecutors.directExecutor());
                } catch (Exception e) {
                    log.error("[{}] Can't get entity group by name edge owner id [{}], groupType [{}], entityGroupName [{}]",
                            tenantId, edge.getOwnerId(), groupType, entityGroupName, e);
                    throw new RuntimeException(e);
                }
            }
        }, MoreExecutors.directExecutor());
    }

    private EntityGroup createEntityGroup(String entityGroupName, EntityId parentEntityId, TenantId tenantId, EntityType groupType) {
        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setName(entityGroupName);
        entityGroup.setType(groupType);
        return entityGroupService.saveEntityGroup(tenantId, parentEntityId, entityGroup);
    }

    @Override
    public boolean isEntityInGroup(TenantId tenantId, EntityId entityId, EntityGroupId entityGroupId) {
        return relationService.checkRelation(tenantId, entityGroupId, entityId,
                EntityRelation.CONTAINS_TYPE
                , RelationTypeGroup.FROM_ENTITY_GROUP);
    }

    private ListenableFuture<List<EntityId>> findEntityIds(TenantId tenantId,
                                                           EntityGroupId entityGroupId, EntityType groupType, PageLink pageLink) {
        ListenableFuture<PageData<EntityId>> pageData = entityGroupDao.findGroupEntityIds(groupType, entityGroupId.getId(), pageLink);
        return Futures.transform(pageData, PageData::getData, MoreExecutors.directExecutor());
    }

    private List<ColumnConfiguration> getEntityGroupColumns(EntityGroup entityGroup) {
        JsonNode jsonConfiguration = entityGroup.getConfiguration();
        List<ColumnConfiguration> columns = null;
        if (jsonConfiguration != null) {
            try {
                EntityGroupConfiguration entityGroupConfiguration =
                        JacksonUtil.treeToValue(jsonConfiguration, EntityGroupConfiguration.class);
                columns = entityGroupConfiguration.getColumns();
            } catch (IllegalArgumentException e) {
                log.error("Unable to read entity group configuration", e);
                throw new RuntimeException("Unable to read entity group configuration", e);
            }
        }
        if (columns == null) {
            columns = Collections.emptyList();
        }
        return columns;
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findEntityGroupById(tenantId, new EntityGroupId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        return FluentFuture.from(findEntityGroupByIdAsync(tenantId, new EntityGroupId(entityId.getId())))
                .transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ENTITY_GROUP;
    }

    @Override
    @TransactionalEventListener
    public void handleEvictEvent(EntityGroupEvictEvent event) {
        List<EntityGroupCacheKey> keys = new ArrayList<>(2);
        keys.add(new EntityGroupCacheKey(event.ownerId(), event.entityType(), event.newGroupName()));
        if (StringUtils.isNotEmpty(event.oldGroupName()) && !event.oldGroupName().equals(event.newGroupName())) {
            keys.add(new EntityGroupCacheKey(event.ownerId(), event.entityType(), event.oldGroupName()));
        }
        cache.evict(keys);
    }

    private class EntityGroupValidator extends DataValidator<EntityGroup> {

        @Override
        protected void validateCreate(TenantId tenantId, EntityGroup entityGroup) {
            if (entityGroup.getExternalId() != null) {
                EntityGroup other = entityGroupDao.findByTenantIdAndExternalId(tenantId.getId(), entityGroup.getExternalId().getId());
                if (other != null) {
                    throw new DataValidationException("Entity group with such external id already exists!");
                }
            }
        }

        @Override
        protected EntityGroup validateUpdate(TenantId tenantId, EntityGroup data) {
            EntityGroup old = entityGroupDao.findById(tenantId, data.getId().getId());
            if (old == null) {
                throw new DataValidationException("Can`t update non existing entity group!");
            }
            return old;
        }

        @Override
        protected void validateDataImpl(TenantId tenantId, EntityGroup entityGroup) {
            if (entityGroup.getType() == null) {
                throw new DataValidationException("Entity group type should be specified!");
            }
            if (StringUtils.isEmpty(entityGroup.getName())) {
                throw new DataValidationException("Entity group name should be specified!");
            }
            if (entityGroup.getOwnerId() == null || entityGroup.getOwnerId().isNullUid()) {
                throw new DataValidationException("Entity group ownerId should be specified!");
            }
        }

        protected void validateIfGroupEntity(EntityId entityId) {
            if (!entityId.getEntityType().isGroupEntityType()) {
                throw new DataValidationException("Entity '" + entityId.getEntityType() + "' is not a group entity!");
            }
        }

        protected void validateEntityTypesMatch(EntityId entityId, EntityGroup entityGroup) {
            if (!entityId.getEntityType().equals(entityGroup.getType())) {
                throw new DataValidationException("Entity type should match the entity group type!");
            }
        }
    }

    private final PaginatedRemover<EntityId, EntityGroup> entityGroupsRemover = new PaginatedRemover<>() {

        @Override
        protected PageData<EntityGroup> findEntities(TenantId tenantId, EntityId id, PageLink pageLink) {
            return entityGroupDao.findAllEntityGroups(tenantId.getId(), id.getId(), id.getEntityType(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, EntityGroup entity) {
            deleteEntityGroup(tenantId, new EntityGroupId(entity.getId().getId()));
        }
    };

    private String getProfileColumnKeyByType(EntityType entityType) {
        return switch (entityType) {
            case ASSET -> "asset_profile";
            case DEVICE -> "device_profile";
            default -> throw new IllegalArgumentException("Wrong entity type: " + entityType);
        };
    }

    @Override
    public boolean isTenantAdminUserGroup(EntityGroup entityGroup) {
        return EntityType.USER.equals(entityGroup.getType()) && (entityGroup.getOwnerId() instanceof TenantId)
                && groupPermissionService.existsByUserGroupIdAndRoleId(entityGroup.getId(), getTenantAdminRoleId());
    }

    @Override
    public boolean containsLastTenantAdmin(TenantId tenantId, List<UserId> usersToRemove) {
        return userService.countUsersByTenantIdAndRoleIdAndIdNotIn(tenantId, getTenantAdminRoleId(), usersToRemove) == 0;
    }

    @Override
    public Set<String> findUserGroupNamesByUserId(TenantId tenantId, UserId userId) {
        return entityGroupDao.findUserGroupNamesByUserId(tenantId, userId);
    }

    private RoleId getTenantAdminRoleId() {
        if (tenantAdminRoleId == null) {
            roleCreationLock.lock();
            try {
                if (tenantAdminRoleId == null) {
                    tenantAdminRoleId = roleService.findOrCreateTenantAdminRole().getId();
                }
            } finally {
                roleCreationLock.unlock();
            }
        }
        return tenantAdminRoleId;
    }

}
