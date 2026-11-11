// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.grouppermission;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.GroupPermissionId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.GroupPermissionInfo;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.group.EntityGroupDao;
import org.thingsboard.server.dao.role.RoleDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;

@Slf4j
@Service("GroupPermissionDaoService")
public class GroupPermissionServiceImpl extends AbstractEntityService implements GroupPermissionService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_USER_GROUP_ID = "Incorrect userGroupId ";
    public static final String INCORRECT_ENTITY_GROUP_ID = "Incorrect entityGroupId ";
    public static final String INCORRECT_ROLE_ID = "Incorrect roleId ";
    public static final String INCORRECT_GROUP_PERMISSION_ID = "Incorrect groupPermissionId ";

    @Autowired
    private GroupPermissionDao groupPermissionDao;

    @Autowired
    private EntityGroupDao entityGroupDao;

    @Autowired
    private RoleDao roleDao;

    @Autowired
    private EntityService entityService;

    @Autowired
    private DataValidator<GroupPermission> groupPermissionValidator;

    @Override
    public GroupPermission saveGroupPermission(TenantId tenantId, GroupPermission groupPermission) {
        log.trace("Executing save groupPermission [{}]", groupPermission);
        groupPermissionValidator.validate(groupPermission, GroupPermission::getTenantId);
        GroupPermission savedGroupPermission = groupPermissionDao.save(tenantId, groupPermission);
        eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId).entityId(savedGroupPermission.getId())
                .created(groupPermission.getId() == null).build());
        return savedGroupPermission;
    }

    @Override
    public GroupPermission findGroupPermissionById(TenantId tenantId, GroupPermissionId groupPermissionId) {
        log.trace("Executing findGroupPermissionById [{}]", groupPermissionId);
        validateId(groupPermissionId, id -> INCORRECT_GROUP_PERMISSION_ID + id);
        return groupPermissionDao.findById(tenantId, groupPermissionId.getId());
    }

    @Override
    public ListenableFuture<GroupPermissionInfo> findGroupPermissionInfoByIdAsync(TenantId tenantId,
                                                                                  GroupPermissionId groupPermissionId,
                                                                                  boolean isUserGroup) {
        log.trace("Executing findGroupPermissionInfoByIdAsync [{}]", groupPermissionId);
        validateId(groupPermissionId, id -> INCORRECT_GROUP_PERMISSION_ID + id);
        GroupPermission groupPermission = groupPermissionDao.findById(tenantId, groupPermissionId.getId());
        if (isUserGroup) {
            return fetchUserGroupPermissionInfoAsync(tenantId, groupPermission);
        } else {
            return fetchEntityGroupPermissionInfoAsync(tenantId, groupPermission);
        }
    }

    @Override
    public PageData<GroupPermission> findGroupPermissionByTenantIdAndUserGroupId(TenantId tenantId, EntityGroupId userGroupId, PageLink pageLink) {
        log.trace("Executing findGroupPermissionByTenantIdAndUserGroupId, tenantId [{}], userGroupId [{}], pageLink [{}]", tenantId, userGroupId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(userGroupId, id -> INCORRECT_USER_GROUP_ID + id);
        validatePageLink(pageLink);
        return groupPermissionDao.findGroupPermissionsByTenantIdAndUserGroupId(tenantId.getId(), userGroupId.getId(), pageLink);
    }

    @Override
    public PageData<GroupPermission> findGroupPermissionByTenantIdAndUserGroupIdAndRoleId(TenantId tenantId, EntityGroupId userGroupId, RoleId roleId, PageLink pageLink) {
        log.trace("Executing findGroupPermissionByTenantIdAndUserGroupIdAndRoleId, tenantId [{}], userGroupId [{}], roleId [{}], pageLink [{}]", tenantId, userGroupId, roleId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(userGroupId, id -> INCORRECT_USER_GROUP_ID + id);
        validateId(roleId, id -> INCORRECT_ROLE_ID + id);
        validatePageLink(pageLink);
        return groupPermissionDao.findGroupPermissionsByTenantIdAndUserGroupIdAndRoleId(tenantId.getId(),
                userGroupId.getId(), roleId.getId(), pageLink);
    }

    @Override
    public PageData<GroupPermission> findGroupPermissionByTenantIdAndEntityGroupIdAndUserGroupIdAndRoleId(TenantId tenantId,
                                                                                                          EntityGroupId entityGroupId,
                                                                                                          EntityGroupId userGroupId,
                                                                                                          RoleId roleId, PageLink pageLink) {
        log.trace("Executing findGroupPermissionByTenantIdAndEntityGroupIdAndUserGroupIdAndRoleId, tenantId [{}], entityGroupId [{}], " +
                "userGroupId [{}], roleId [{}], pageLink [{}]", tenantId, entityGroupId, userGroupId, roleId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(entityGroupId, id -> INCORRECT_ENTITY_GROUP_ID + id);
        validateId(userGroupId, id -> INCORRECT_USER_GROUP_ID + id);
        validateId(roleId, id -> INCORRECT_ROLE_ID + id);
        validatePageLink(pageLink);
        return groupPermissionDao.findGroupPermissionsByTenantIdAndEntityGroupIdAndUserGroupIdAndRoleId(tenantId.getId(),
                entityGroupId.getId(), userGroupId.getId(), roleId.getId(), pageLink);
    }

    @Override
    public ListenableFuture<List<GroupPermissionInfo>> findGroupPermissionInfoListByTenantIdAndUserGroupIdAsync(TenantId tenantId, EntityGroupId userGroupId) {
        log.trace("Executing findGroupPermissionInfoListByTenantIdAndUserGroupIdAsync, tenantId [{}], userGroupId [{}]", tenantId, userGroupId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(userGroupId, id -> INCORRECT_USER_GROUP_ID + id);
        PageData<GroupPermission> groupPermissions = groupPermissionDao.findGroupPermissionsByTenantIdAndUserGroupId(tenantId.getId(), userGroupId.getId(), new PageLink(Integer.MAX_VALUE));
        return toGroupPermissionInfoListAsync(tenantId, groupPermissions.getData(), true);
    }

    @Override
    public ListenableFuture<List<GroupPermissionInfo>> loadUserGroupPermissionInfoListAsync(TenantId tenantId, List<GroupPermission> permissions) {
        log.trace("Executing loadUserGroupPermissionInfoListAsync, tenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return toGroupPermissionInfoListAsync(tenantId, permissions, true);
    }

    @Override
    public List<GroupPermission> findGroupPermissionListByTenantIdAndUserGroupId(TenantId tenantId, EntityGroupId userGroupId) {
        log.trace("Executing findGroupPermissionListByTenantIdAndUserGroupId, tenantId [{}], userGroupId [{}]", tenantId, userGroupId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(userGroupId, id -> INCORRECT_USER_GROUP_ID + id);
        return groupPermissionDao.findGroupPermissionsByTenantIdAndUserGroupId(tenantId.getId(), userGroupId.getId(), new PageLink(Integer.MAX_VALUE)).getData();
    }

    @Override
    public PageData<GroupPermission> findGroupPermissionByTenantIdAndEntityGroupId(TenantId tenantId, EntityGroupId entityGroupId, PageLink pageLink) {
        log.trace("Executing findGroupPermissionByTenantIdAndEntityGroupId, tenantId [{}], entityGroupId [{}], pageLink [{}]", tenantId, entityGroupId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(entityGroupId, id -> INCORRECT_ENTITY_GROUP_ID + id);
        validatePageLink(pageLink);
        return groupPermissionDao.findGroupPermissionsByTenantIdAndEntityGroupId(tenantId.getId(), entityGroupId.getId(), pageLink);
    }

    @Override
    public Optional<GroupPermission> findPublicGroupPermissionByTenantIdAndEntityGroupId(TenantId tenantId, EntityGroupId entityGroupId) {
        log.trace("Executing findPublicGroupPermissionByTenantIdAndEntityGroupId, tenantId [{}], entityGroupId [{}]", tenantId, entityGroupId);
        List<GroupPermission> groupPermissions = groupPermissionDao.findGroupPermissionsByTenantIdAndEntityGroupId(tenantId.getId(), entityGroupId.getId(), new PageLink(Integer.MAX_VALUE)).getData();
        List<GroupPermission> permissions = groupPermissions.stream().filter((GroupPermission::isPublic)).collect(Collectors.toList());
        if (permissions.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(permissions.get(0));
        }
    }

    @Override
    public ListenableFuture<List<GroupPermissionInfo>> findGroupPermissionInfoListByTenantIdAndEntityGroupIdAsync(TenantId tenantId, EntityGroupId entityGroupId) {
        log.trace("Executing findGroupPermissionInfoListByTenantIdAndEntityGroupIdAsync, tenantId [{}], entityGroupId [{}]", tenantId, entityGroupId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(entityGroupId, id -> INCORRECT_ENTITY_GROUP_ID + id);
        PageData<GroupPermission> groupPermissions = groupPermissionDao.findGroupPermissionsByTenantIdAndEntityGroupId(tenantId.getId(), entityGroupId.getId(), new PageLink(Integer.MAX_VALUE));
        return toGroupPermissionInfoListAsync(tenantId, groupPermissions.getData(), false);
    }

    @Override
    public PageData<GroupPermission> findGroupPermissionByTenantIdAndRoleId(TenantId tenantId, RoleId roleId, PageLink pageLink) {
        log.trace("Executing findGroupPermissionByTenantIdAndRuleId, tenantId [{}], ruleId [{}], pageLink [{}]", tenantId, roleId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(roleId, id -> INCORRECT_ROLE_ID + id);
        validatePageLink(pageLink);
        return groupPermissionDao.findGroupPermissionsByTenantIdAndRoleId(tenantId.getId(), roleId.getId(), pageLink);
    }

    @Override
    public ListenableFuture<GroupPermission> findGroupPermissionByIdAsync(TenantId tenantId, GroupPermissionId groupPermissionId) {
        log.trace("Executing findGroupPermissionByIdAsync [{}]", groupPermissionId);
        validateId(groupPermissionId, id -> INCORRECT_GROUP_PERMISSION_ID + id);
        return groupPermissionDao.findByIdAsync(tenantId, groupPermissionId.getId());
    }

    @Transactional
    @Override
    public EntityGroup deleteGroupPermission(TenantId tenantId, GroupPermissionId groupPermissionId) {
        log.trace("Executing deleteGroupPermission [{}]", groupPermissionId);
        validateId(groupPermissionId, id -> INCORRECT_GROUP_PERMISSION_ID + id);
        return deleteGroupPermission(tenantId, groupPermissionId, false);
    }

    private EntityGroup deleteGroupPermission(TenantId tenantId, GroupPermissionId groupPermissionId, boolean force) {
        GroupPermission groupPermission = groupPermissionDao.findById(tenantId, groupPermissionId.getId());
        if (groupPermission == null) {
            if (force) {
                return null;
            } else {
                throw new IncorrectParameterException("Unable to delete non-existent group permission.");
            }
        }

        EntityGroup entityGroup = null;
        if (!force && groupPermission.isPublic() && groupPermission.getEntityGroupId() != null) {
            entityGroup = entityGroupDao.findById(tenantId, groupPermission.getEntityGroupId().getId());
            if (entityGroup != null) {
                JsonNode additionalInfo = entityGroup.getAdditionalInfo();
                if (additionalInfo == null) {
                    additionalInfo = JacksonUtil.newObjectNode();
                }
                ((ObjectNode) additionalInfo).put("isPublic", false);
                ((ObjectNode) additionalInfo).put("publicCustomerId", "");
                entityGroup.setAdditionalInfo(additionalInfo);
                entityGroup = entityGroupDao.save(tenantId, entityGroup);
            }
        }
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(groupPermissionId).build());
        groupPermissionDao.removeById(tenantId, groupPermissionId.getId());
        return entityGroup; // returning entity group in case it was updated
    }

    @Override
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        deleteGroupPermission(tenantId, (GroupPermissionId) id, force);
    }

    @Override
    public void deleteGroupPermissionsByTenantId(TenantId tenantId) {
        log.trace("Executing deleteGroupPermissionsByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        tenantGroupPermissionRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        deleteGroupPermissionsByTenantId(tenantId);
    }

    @Override
    public void deleteGroupPermissionsByTenantIdAndUserGroupId(TenantId tenantId, EntityGroupId userGroupId) {
        log.trace("Executing deleteGroupPermissionsByTenantIdAndUserGroupId, tenantId [{}], userGroupId [{}]", tenantId, userGroupId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(userGroupId, id -> INCORRECT_USER_GROUP_ID + id);
        userGroupPermissionRemover.removeEntities(tenantId, userGroupId);
    }

    @Override
    public void deleteGroupPermissionsByTenantIdAndEntityGroupId(TenantId tenantId, EntityGroupId entityGroupId) {
        log.trace("Executing deleteGroupPermissionsByTenantIdAndEntityGroupId, tenantId [{}], entityGroupId [{}]", tenantId, entityGroupId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(entityGroupId, id -> INCORRECT_ENTITY_GROUP_ID + id);
        entityGroupPermissionRemover.removeEntities(tenantId, entityGroupId);
    }

    @Override
    public void deleteGroupPermissionsByTenantIdAndRoleId(TenantId tenantId, RoleId roleId) {
        log.trace("Executing deleteGroupPermissionsByTenantIdAndRoleId, tenantId [{}], roleId [{}]", tenantId, roleId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(roleId, id -> INCORRECT_ROLE_ID + id);
        rolePermissionRemover.removeEntities(tenantId, roleId);
    }

    @Override
    public boolean existsByUserGroupIdAndRoleId(EntityGroupId userGroupId, RoleId roleId) {
        return groupPermissionDao.existsByUserGroupIdAndRoleId(userGroupId, roleId);
    }

    private ListenableFuture<List<GroupPermissionInfo>> toGroupPermissionInfoListAsync(TenantId tenantId, List<GroupPermission> groupPermissions, boolean isUserGroup) {
        List<ListenableFuture<GroupPermissionInfo>> groupPermissionInfoFutureList = new ArrayList<>();
        groupPermissions.forEach(groupPermission -> {
            if (isUserGroup) {
                groupPermissionInfoFutureList.add(fetchUserGroupPermissionInfoAsync(tenantId, groupPermission));
            } else {
                groupPermissionInfoFutureList.add(fetchEntityGroupPermissionInfoAsync(tenantId, groupPermission));
            }
        });
        return Futures.successfulAsList(groupPermissionInfoFutureList);
    }

    private ListenableFuture<GroupPermissionInfo> fetchUserGroupPermissionInfoAsync(TenantId tenantId, GroupPermission groupPermission) {
        ListenableFuture<Role> roleFuture = roleDao.findByIdAsync(tenantId, groupPermission.getRoleId().getId());
        return Futures.transformAsync(roleFuture, role -> {
            GroupPermissionInfo groupPermissionInfo = new GroupPermissionInfo(groupPermission);
            groupPermissionInfo.setRole(role);
            if (groupPermission.getEntityGroupId() != null && !groupPermission.getEntityGroupId().isNullUid()) {
                ListenableFuture<EntityGroup> entityGroup = entityGroupService.findEntityGroupByIdAsync(tenantId, groupPermission.getEntityGroupId());
                return Futures.transform(entityGroup, entityGroup1 -> {
                    groupPermissionInfo.setEntityGroupName(entityGroup1.getName());
                    groupPermissionInfo.setEntityGroupType(entityGroup1.getType());
                    EntityId ownerId = entityGroup1.getOwnerId();
                    groupPermissionInfo.setEntityGroupOwnerId(ownerId);
                    groupPermissionInfo.setEntityGroupOwnerName(entityService.fetchEntityName(tenantId, ownerId).orElse("N/A"));
                    return groupPermissionInfo;
                }, MoreExecutors.directExecutor());
            } else {
                return Futures.immediateFuture(groupPermissionInfo);
            }
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<GroupPermissionInfo> fetchEntityGroupPermissionInfoAsync(TenantId tenantId, GroupPermission groupPermission) {
        ListenableFuture<Role> roleFuture = roleDao.findByIdAsync(tenantId, groupPermission.getRoleId().getId());
        return Futures.transformAsync(roleFuture, role -> {
            GroupPermissionInfo groupPermissionInfo = new GroupPermissionInfo(groupPermission);
            groupPermissionInfo.setRole(role);
            ListenableFuture<EntityGroup> userGroup = entityGroupService.findEntityGroupByIdAsync(tenantId, groupPermission.getUserGroupId());
            return Futures.transform(userGroup, userGroup1 -> {
                groupPermissionInfo.setUserGroupName(userGroup1.getName());
                EntityId ownerId = userGroup1.getOwnerId();
                groupPermissionInfo.setUserGroupOwnerId(ownerId);
                groupPermissionInfo.setUserGroupOwnerName(entityService.fetchEntityName(tenantId, ownerId).orElse("N/A"));
                return groupPermissionInfo;
            }, MoreExecutors.directExecutor());
        }, MoreExecutors.directExecutor());
    }

    private final PaginatedRemover<TenantId, GroupPermission> tenantGroupPermissionRemover = new PaginatedRemover<>() {

        @Override
        protected PageData<GroupPermission> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
            return groupPermissionDao.findGroupPermissionsByTenantId(tenantId.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, GroupPermission entity) {
            deleteGroupPermission(tenantId, entity.getId());
        }

    };

    private final PaginatedRemover<EntityGroupId, GroupPermission> userGroupPermissionRemover = new PaginatedRemover<>() {

        @Override
        protected PageData<GroupPermission> findEntities(TenantId tenantId, EntityGroupId userGroupId, PageLink pageLink) {
            return groupPermissionDao.findGroupPermissionsByTenantIdAndUserGroupId(tenantId.getId(), userGroupId.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, GroupPermission entity) {
            deleteGroupPermission(tenantId, entity.getId());
        }

    };

    private final PaginatedRemover<EntityGroupId, GroupPermission> entityGroupPermissionRemover = new PaginatedRemover<>() {

        @Override
        protected PageData<GroupPermission> findEntities(TenantId tenantId, EntityGroupId entityGroupId, PageLink pageLink) {
            return groupPermissionDao.findGroupPermissionsByTenantIdAndEntityGroupId(tenantId.getId(), entityGroupId.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, GroupPermission entity) {
            deleteGroupPermission(tenantId, entity.getId());
        }

    };

    private final PaginatedRemover<RoleId, GroupPermission> rolePermissionRemover = new PaginatedRemover<>() {

        @Override
        protected PageData<GroupPermission> findEntities(TenantId tenantId, RoleId roleId, PageLink pageLink) {
            return groupPermissionDao.findGroupPermissionsByTenantIdAndRoleId(tenantId.getId(), roleId.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, GroupPermission entity) {
            deleteGroupPermission(tenantId, entity.getId());
        }

    };


    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findGroupPermissionById(tenantId, new GroupPermissionId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        return FluentFuture.from(groupPermissionDao.findByIdAsync(tenantId, entityId.getId()))
                .transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.GROUP_PERMISSION;
    }

}
