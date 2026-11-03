// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service.validator;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.dao.group.EntityGroupDao;
import org.thingsboard.server.dao.grouppermission.GroupPermissionDao;
import org.thingsboard.server.dao.owner.OwnerService;
import org.thingsboard.server.dao.role.RoleDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.List;

@Component
@AllArgsConstructor
public class GroupPermissionDataValidator extends DataValidator<GroupPermission> {

    public static final String USER_GROUP_OUTSIDE_OWNER_HIERARCHY_ERROR =
            "Group Permission can't grant access to a user group that is not owned by the grantee user group's owner or its sub-customers!";

    private final TenantService tenantService;
    private final EntityGroupDao entityGroupDao;
    private final RoleDao roleDao;
    private final GroupPermissionDao groupPermissionDao;
    @Lazy
    private final OwnerService ownerService;

    public static void validateSelfRegistrationPermissions(TenantId tenantId, List<GroupPermission> permissions, EntityGroupDao entityGroupDao) {
        if (permissions == null) {
            return;
        }
        for (GroupPermission permission : permissions) {
            if (permission.getEntityGroupId() == null || permission.getEntityGroupId().isNullUid()) {
                continue;
            }
            EntityGroup entityGroup = entityGroupDao.findById(tenantId, permission.getEntityGroupId().getId());
            if (entityGroup != null && entityGroup.getType() == EntityType.USER) {
                throw new DataValidationException(USER_GROUP_OUTSIDE_OWNER_HIERARCHY_ERROR);
            }
        }
    }

    @Override
    protected GroupPermission validateUpdate(TenantId tenantId, GroupPermission groupPermission) {
        return groupPermissionDao.findById(tenantId, groupPermission.getId().getId());
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, GroupPermission groupPermission) {
        if (StringUtils.isEmpty(groupPermission.getName())) {
            throw new DataValidationException("Group Permission name should be specified!");
        }
        if (groupPermission.getTenantId() == null) {
            throw new DataValidationException("Group Permission should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(groupPermission.getTenantId())) {
                throw new DataValidationException("Group Permission is referencing to non-existent tenant!");
            }
        }
        if (groupPermission.getUserGroupId() == null || groupPermission.getUserGroupId().isNullUid()) {
            throw new DataValidationException("Group Permission userGroupId should be specified!");
        }
        EntityGroup userGroup = entityGroupDao.findById(tenantId, groupPermission.getUserGroupId().getId());
        if (userGroup == null) {
            throw new DataValidationException("Group Permission is referencing to non-existent user group!");
        }
        if (userGroup.getType() != EntityType.USER) {
            throw new DataValidationException("Group Permission is referencing to user group with non user group type!");
        }
        if (groupPermission.getRoleId() == null || groupPermission.getRoleId().isNullUid()) {
            throw new DataValidationException("Group Permission roleId should be specified!");
        } else {
            Role role = roleDao.findById(tenantId, groupPermission.getRoleId().getId());
            if (role == null) {
                throw new DataValidationException("Group Permission is referencing to non-existent role!");
            }
        }
        if (groupPermission.getEntityGroupId() == null) {
            groupPermission.setEntityGroupId(new EntityGroupId(EntityId.NULL_UUID));
        }

        if (!groupPermission.getEntityGroupId().isNullUid()) {
            EntityGroup entityGroup = entityGroupDao.findById(tenantId, groupPermission.getEntityGroupId().getId());
            if (entityGroup == null) {
                throw new DataValidationException("Group Permission is referencing to non-existent entity group!");
            }
            if (entityGroup.getType() == EntityType.USER
                    && !ownerService.fetchOwnersHierarchy(tenantId, entityGroup.getOwnerId()).contains(userGroup.getOwnerId())) {
                throw new DataValidationException(USER_GROUP_OUTSIDE_OWNER_HIERARCHY_ERROR);
            }
            groupPermission.setEntityGroupType(entityGroup.getType());
        } else {
            groupPermission.setEntityGroupType(null);
        }
    }

}
