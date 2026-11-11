// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.grouppermission;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.GroupPermissionEntity;

import java.util.UUID;

public interface GroupPermissionRepository extends JpaRepository<GroupPermissionEntity, UUID>, JpaSpecificationExecutor<GroupPermissionEntity> {

    @Query("SELECT g FROM GroupPermissionEntity g WHERE " +
            "g.tenantId = :tenantId"
    )
    Page<GroupPermissionEntity> findByTenantId(
            @Param("tenantId") UUID tenantId,
            Pageable pageable);

    @Query("SELECT g FROM GroupPermissionEntity g WHERE " +
            "g.tenantId = :tenantId " +
            "AND g.userGroupId = :userGroupId"
    )
    Page<GroupPermissionEntity> findByTenantIdAndUserGroupId(
            @Param("tenantId") UUID tenantId,
            @Param("userGroupId") UUID userGroupId,
            Pageable pageable);

    @Query("SELECT g FROM GroupPermissionEntity g WHERE " +
            "g.tenantId = :tenantId " +
            "AND g.userGroupId = :userGroupId " +
            "AND g.roleId = :roleId"
    )
    Page<GroupPermissionEntity> findByTenantIdAndUserGroupIdAndRoleId(
            @Param("tenantId") UUID tenantId,
            @Param("userGroupId") UUID userGroupId,
            @Param("roleId") UUID roleId,
            Pageable pageable);

    @Query("SELECT g FROM GroupPermissionEntity g WHERE " +
            "g.tenantId = :tenantId " +
            "AND g.entityGroupId = :entityGroupId " +
            "AND g.userGroupId = :userGroupId " +
            "AND g.roleId = :roleId"
    )
    Page<GroupPermissionEntity> findByTenantIdAndEntityGroupIdAndUserGroupIdAndRoleId(
            @Param("tenantId") UUID tenantId,
            @Param("entityGroupId") UUID entityGroupId,
            @Param("userGroupId") UUID userGroupId,
            @Param("roleId") UUID roleId,
            Pageable pageable);

    @Query("SELECT g FROM GroupPermissionEntity g WHERE " +
            "g.tenantId = :tenantId " +
            "AND g.entityGroupId = :entityGroupId"
    )
    Page<GroupPermissionEntity> findByTenantIdAndEntityGroupId(
            @Param("tenantId") UUID tenantId,
            @Param("entityGroupId") UUID entityGroupId,
            Pageable pageable);

    @Query("SELECT g FROM GroupPermissionEntity g WHERE " +
            "g.tenantId = :tenantId " +
            "AND g.roleId = :roleId"
    )
    Page<GroupPermissionEntity> findByTenantIdAndRoleId(
            @Param("tenantId") UUID tenantId,
            @Param("roleId") UUID roleId,
            Pageable pageable);

    boolean existsByUserGroupIdAndRoleId(UUID userGroupId, UUID roleId);
}
