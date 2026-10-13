// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.grouppermission;

import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.TenantEntityDao;

import java.util.UUID;

public interface GroupPermissionDao extends Dao<GroupPermission>, TenantEntityDao<GroupPermission> {

    GroupPermission save(TenantId tenantId, GroupPermission groupPermission);

    PageData<GroupPermission> findGroupPermissionsByTenantId(UUID tenantId, PageLink pageLink);

    PageData<GroupPermission> findGroupPermissionsByTenantIdAndUserGroupId(UUID tenantId, UUID userGroupId, PageLink pageLink);

    PageData<GroupPermission> findGroupPermissionsByTenantIdAndUserGroupIdAndRoleId(UUID tenantId, UUID userGroupId, UUID roleId, PageLink pageLink);

    PageData<GroupPermission> findGroupPermissionsByTenantIdAndEntityGroupIdAndUserGroupIdAndRoleId(UUID tenantId, UUID entityGroupId, UUID userGroupId, UUID roleId, PageLink pageLink);

    PageData<GroupPermission> findGroupPermissionsByTenantIdAndEntityGroupId(UUID tenantId, UUID entityGroupId, PageLink pageLink);

    PageData<GroupPermission> findGroupPermissionsByTenantIdAndRoleId(UUID tenantId, UUID roleId, PageLink pageLink);

    boolean existsByUserGroupIdAndRoleId(EntityGroupId entityGroupId, RoleId RoleId);

}
