// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.role;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;
import java.util.Optional;

public interface RoleService extends EntityDaoService {

    Role saveRole(TenantId tenantId, Role role);

    Role findRoleById(TenantId tenantId, RoleId roleId);

    ListenableFuture<List<Role>> findRolesByIdsAsync(TenantId tenantId, List<RoleId> roleIds);

    Optional<Role> findRoleByTenantIdAndName(TenantId tenantId, String name);

    ListenableFuture<Optional<Role>> findRoleByTenantIdAndNameAsync(TenantId tenantId, String name);

    Optional<Role> findRoleByByTenantIdAndCustomerIdAndName(TenantId tenantId, CustomerId customerId, String name);

    PageData<Role> findRolesByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<Role> findRolesByTenantIdAndType(TenantId tenantId, PageLink pageLink, RoleType type);

    ListenableFuture<Role> findRoleByIdAsync(TenantId tenantId, RoleId roleId);

    void deleteRole(TenantId tenantId, RoleId roleId);

    void deleteRolesByTenantId(TenantId tenantId);

    void deleteRolesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId);

    Role findOrCreateRole(TenantId tenantId, CustomerId customerId, RoleType type,
                          String name, Object permissions, String description);

    Role findOrCreateTenantUserRole();

    Role findOrCreateTenantAdminRole();

    Role findOrCreateCustomerUserRole(TenantId tenantId, CustomerId customerId);

    Role findOrCreateCustomerAdminRole(TenantId tenantId, CustomerId customerId);

    Role findOrCreatePublicUsersEntityGroupRole(TenantId tenantId, CustomerId customerId);

    Role findOrCreatePublicUserRole(TenantId tenantId, CustomerId customerId);

    Role findOrCreateReadOnlyEntityGroupRole(TenantId tenantId, CustomerId customerId);

    Role findOrCreateWriteEntityGroupRole(TenantId tenantId, CustomerId customerId);

    PageData<Role> findRolesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    PageData<Role> findRolesByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, RoleType type, PageLink pageLink);
}
