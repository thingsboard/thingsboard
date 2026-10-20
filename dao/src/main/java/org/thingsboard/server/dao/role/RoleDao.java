// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.role;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableEntityDao;
import org.thingsboard.server.dao.TenantEntityDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleDao extends Dao<Role>, TenantEntityDao<Role>, ExportableEntityDao<RoleId, Role> {

    Role save(TenantId tenantId, Role role);

    PageData<Role> findRolesByTenantId(UUID tenantId, PageLink pageLink);

    PageData<Role> findRolesByTenantIdAndType(UUID tenantId, RoleType type, PageLink pageLink);

    Optional<Role> findRoleByTenantIdAndName(UUID tenantId, String name);

    Optional<Role> findRoleByByTenantIdAndCustomerIdAndName(UUID tenantId, UUID customerId, String name);

    PageData<Role> findRolesByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink);

    PageData<Role> findRolesByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, RoleType type, PageLink pageLink);

    /**
     * Find roles by tenantId and role Ids.
     *
     * @param tenantId the tenantId
     * @param roleIds the role Ids
     * @return the list of role objects
     */
    ListenableFuture<List<Role>> findRolesByTenantIdAndIdsAsync(UUID tenantId, List<UUID> roleIds);

}
