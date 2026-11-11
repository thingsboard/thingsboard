// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.grouppermission;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.grouppermission.GroupPermissionDao;
import org.thingsboard.server.dao.model.sql.GroupPermissionEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;


@Component
@SqlDao
public class JpaGroupPermissionDao extends JpaAbstractDao<GroupPermissionEntity, GroupPermission> implements GroupPermissionDao {

    @Autowired
    private GroupPermissionRepository groupPermissionRepository;

    @Override
    protected Class<GroupPermissionEntity> getEntityClass() {
        return GroupPermissionEntity.class;
    }

    @Override
    protected JpaRepository<GroupPermissionEntity, UUID> getRepository() {
        return groupPermissionRepository;
    }

    @Override
    public PageData<GroupPermission> findGroupPermissionsByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(
                groupPermissionRepository.findByTenantId(
                        tenantId,
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<GroupPermission> findGroupPermissionsByTenantIdAndUserGroupId(UUID tenantId, UUID userGroupId, PageLink pageLink) {
        return DaoUtil.toPageData(
                groupPermissionRepository.findByTenantIdAndUserGroupId(
                        tenantId,
                        userGroupId,
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<GroupPermission> findGroupPermissionsByTenantIdAndUserGroupIdAndRoleId(UUID tenantId, UUID userGroupId, UUID roleId, PageLink pageLink) {
        return DaoUtil.toPageData(
                groupPermissionRepository.findByTenantIdAndUserGroupIdAndRoleId(
                        tenantId,
                        userGroupId,
                        roleId,
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<GroupPermission> findGroupPermissionsByTenantIdAndEntityGroupIdAndUserGroupIdAndRoleId(UUID tenantId, UUID entityGroupId, UUID userGroupId, UUID roleId, PageLink pageLink) {
        return DaoUtil.toPageData(
                groupPermissionRepository.findByTenantIdAndEntityGroupIdAndUserGroupIdAndRoleId(
                        tenantId,
                        entityGroupId,
                        userGroupId,
                        roleId,
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<GroupPermission> findGroupPermissionsByTenantIdAndEntityGroupId(UUID tenantId, UUID entityGroupId, PageLink pageLink) {
        return DaoUtil.toPageData(
                groupPermissionRepository.findByTenantIdAndEntityGroupId(
                        tenantId,
                        entityGroupId,
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<GroupPermission> findGroupPermissionsByTenantIdAndRoleId(UUID tenantId, UUID roleId, PageLink pageLink) {
        return DaoUtil.toPageData(
                groupPermissionRepository.findByTenantIdAndRoleId(
                        tenantId,
                        roleId,
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public boolean existsByUserGroupIdAndRoleId(EntityGroupId entityGroupId, RoleId roleId) {
        return groupPermissionRepository.existsByUserGroupIdAndRoleId(entityGroupId.getId(), roleId.getId());
    }

    @Override
    public PageData<GroupPermission> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        return findGroupPermissionsByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.GROUP_PERMISSION;
    }

}
