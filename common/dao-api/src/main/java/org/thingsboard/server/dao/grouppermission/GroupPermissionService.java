// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.grouppermission;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.GroupPermissionId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.GroupPermissionInfo;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;
import java.util.Optional;

public interface GroupPermissionService extends EntityDaoService {

    GroupPermission saveGroupPermission(TenantId tenantId, GroupPermission groupPermission);

    GroupPermission findGroupPermissionById(TenantId tenantId, GroupPermissionId groupPermissionId);

    ListenableFuture<GroupPermissionInfo> findGroupPermissionInfoByIdAsync(TenantId tenantId, GroupPermissionId groupPermissionId, boolean isUserGroup);

    PageData<GroupPermission> findGroupPermissionByTenantIdAndUserGroupId(TenantId tenantId, EntityGroupId userGroupId, PageLink pageLink);

    PageData<GroupPermission> findGroupPermissionByTenantIdAndUserGroupIdAndRoleId(TenantId tenantId, EntityGroupId userGroupId, RoleId roleId, PageLink pageLink);

    PageData<GroupPermission> findGroupPermissionByTenantIdAndEntityGroupIdAndUserGroupIdAndRoleId(TenantId tenantId, EntityGroupId entityGroupId, EntityGroupId userGroupId, RoleId roleId, PageLink pageLink);

    ListenableFuture<List<GroupPermissionInfo>> findGroupPermissionInfoListByTenantIdAndUserGroupIdAsync(TenantId tenantId, EntityGroupId userGroupId);

    ListenableFuture<List<GroupPermissionInfo>> loadUserGroupPermissionInfoListAsync(TenantId tenantId, List<GroupPermission> permissions);

    List<GroupPermission> findGroupPermissionListByTenantIdAndUserGroupId(TenantId tenantId, EntityGroupId entityGroupId);

    PageData<GroupPermission> findGroupPermissionByTenantIdAndEntityGroupId(TenantId tenantId, EntityGroupId entityGroupId, PageLink pageLink);

    Optional<GroupPermission> findPublicGroupPermissionByTenantIdAndEntityGroupId(TenantId tenantId, EntityGroupId entityGroupId);

    ListenableFuture<List<GroupPermissionInfo>> findGroupPermissionInfoListByTenantIdAndEntityGroupIdAsync(TenantId tenantId, EntityGroupId entityGroupId);

    PageData<GroupPermission> findGroupPermissionByTenantIdAndRoleId(TenantId tenantId, RoleId roleId, PageLink pageLink);

    ListenableFuture<GroupPermission> findGroupPermissionByIdAsync(TenantId tenantId, GroupPermissionId groupPermissionId);

    EntityGroup deleteGroupPermission(TenantId tenantId, GroupPermissionId groupPermissionId);

    void deleteGroupPermissionsByTenantId(TenantId tenantId);

    void deleteGroupPermissionsByTenantIdAndUserGroupId(TenantId tenantId, EntityGroupId userGroupId);

    void deleteGroupPermissionsByTenantIdAndEntityGroupId(TenantId tenantId, EntityGroupId entityGroupId);

    void deleteGroupPermissionsByTenantIdAndRoleId(TenantId tenantId, RoleId roleId);

    boolean existsByUserGroupIdAndRoleId(EntityGroupId userGroupId, RoleId roleId);

}
