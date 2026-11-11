// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.GroupPermissionId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.GROUP_PERMISSION_ENTITY_GROUP_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.GROUP_PERMISSION_ENTITY_GROUP_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.GROUP_PERMISSION_IS_PUBLIC_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.GROUP_PERMISSION_ROLE_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.GROUP_PERMISSION_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.GROUP_PERMISSION_USER_GROUP_ID_PROPERTY;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.GROUP_PERMISSION_TABLE_NAME)
@Slf4j
public class GroupPermissionEntity extends BaseSqlEntity<GroupPermission> {

    @Column(name = GROUP_PERMISSION_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = GROUP_PERMISSION_USER_GROUP_ID_PROPERTY)
    private UUID userGroupId;

    @Column(name = GROUP_PERMISSION_ENTITY_GROUP_ID_PROPERTY)
    private UUID entityGroupId;

    @Column(name = GROUP_PERMISSION_ROLE_ID_PROPERTY)
    private UUID roleId;

    @Enumerated(EnumType.STRING)
    @Column(name = GROUP_PERMISSION_ENTITY_GROUP_TYPE_PROPERTY)
    private EntityType entityGroupType;

    @Column(name = GROUP_PERMISSION_IS_PUBLIC_PROPERTY)
    private boolean isPublic;

    public GroupPermissionEntity() {
        super();
    }

    public GroupPermissionEntity(GroupPermission groupPermission) {
        this.createdTime = groupPermission.getCreatedTime();
        if (groupPermission.getId() != null) {
            this.setUuid(groupPermission.getId().getId());
        }
        if (groupPermission.getTenantId() != null) {
            this.tenantId = groupPermission.getTenantId().getId();
        }
        if (groupPermission.getRoleId() != null) {
            this.roleId = groupPermission.getRoleId().getId();
        }
        if (groupPermission.getUserGroupId() != null) {
            this.userGroupId = groupPermission.getUserGroupId().getId();
        }
        if (groupPermission.getEntityGroupId() != null) {
            this.entityGroupId = groupPermission.getEntityGroupId().getId();
            this.entityGroupType = groupPermission.getEntityGroupType();
        }
        this.isPublic = groupPermission.isPublic();
    }

    @Override
    public GroupPermission toData() {
        GroupPermission groupPermission = new GroupPermission(new GroupPermissionId(getUuid()));
        groupPermission.setCreatedTime(this.createdTime);
        if (tenantId != null) {
            groupPermission.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (roleId != null) {
            groupPermission.setRoleId(new RoleId(roleId));
        }
        if (userGroupId != null) {
            groupPermission.setUserGroupId(new EntityGroupId(userGroupId));
        }
        if (entityGroupId != null && entityGroupType != null) {
            groupPermission.setEntityGroupId(new EntityGroupId(entityGroupId));
            groupPermission.setEntityGroupType(entityGroupType);
        }
        groupPermission.setPublic(isPublic);
        return groupPermission;
    }
}
