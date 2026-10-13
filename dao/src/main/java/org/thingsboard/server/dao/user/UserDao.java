// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.user;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.UserAuthDetails;
import org.thingsboard.server.common.data.id.CustomMenuId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableEntityDao;
import org.thingsboard.server.dao.TenantEntityDao;

import java.util.List;
import java.util.UUID;

public interface UserDao extends Dao<User>, TenantEntityDao<User>, ExportableEntityDao<UserId, User> {

    /**
     * Save or update user object
     *
     * @param user the user object
     * @return saved user entity
     */
    User save(TenantId tenantId, User user);

    /**
     * Find user by email.
     *
     * @param email the email
     * @return the user entity
     */
    User findByEmail(TenantId tenantId, String email);

    /**
     * Find user by tenant id and email.
     *
     * @param tenantId the tenant id
     * @param email the email
     * @return the user entity
     */
    User findByTenantIdAndEmail(TenantId tenantId, String email);

    /**
     * Find users by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of user entities
     */
    PageData<User> findByTenantId(UUID tenantId, PageLink pageLink);

    /**
     * Find tenant admin users by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of user entities
     */
    PageData<User> findTenantAdmins(UUID tenantId, PageLink pageLink);

    /**
     * Find customer users by tenantId, customerId and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param pageLink the page link
     * @return the list of user entities
     */
    PageData<User> findCustomerUsers(UUID tenantId, UUID customerId, PageLink pageLink);

    /**
     * Find users for alarm assignment by tenantId, customerId and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param pageLink the page link
     * @return the list of user entities
     */
    PageData<User> findUsersByCustomerIds(UUID tenantId, List<CustomerId> customerIds, PageLink pageLink);

    /**
     * Find all customer users by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of user entities
     */
    PageData<User> findAllCustomerUsers(UUID tenantId, PageLink pageLink);

    /**
     * Find users by tenantId and user Ids.
     *
     * @param tenantId the tenantId
     * @param userIds the user Ids
     * @return the list of user objects
     */
    ListenableFuture<List<User>> findUsersByTenantIdAndIdsAsync(UUID tenantId, List<UUID> userIds);

    List<User> findUsersByTenantIdAndIds(UUID tenantId, List<UUID> userIds);

    PageData<User> findUsersByEntityGroupId(UUID groupId, PageLink pageLink);

    PageData<User> findUsersByEntityGroupIds(List<UUID> groupIds, PageLink pageLink);

    PageData<User> findUsersByTenantIdAndRolesIds(TenantId tenantId, List<RoleId> rolesIds, PageLink pageLink);

    PageData<User> findUsersByTenantsIdsAndRoleId(List<TenantId> tenantsIds, RoleId roleId, PageLink pageLink);

    boolean existsByTenantsIdsAndRoleIdAndUserId(List<TenantId> tenantsIds, RoleId roleId, UserId userId);

    PageData<User> findUsersByTenantProfilesIdsAndRoleId(List<TenantProfileId> tenantProfilesIds, RoleId roleId, PageLink pageLink);

    boolean existsByTenantProfilesIdsAndRoleIdAndUserId(List<TenantProfileId> tenantProfilesIds, RoleId roleId, UserId userId);

    PageData<User> findAllUsersByRoleId(RoleId roleId, PageLink pageLink);

    boolean existsByRoleIdAndUserId(RoleId roleId, UserId userId);

    int countUsersByTenantIdAndRoleIdAndIdNotIn(TenantId tenantId, RoleId roleId, List<UserId> userIds);

    PageData<User> findAll(PageLink pageLink);

    PageData<User> findAllByAuthority(Authority authority, PageLink pageLink);

    boolean existsByAuthority(Authority authority);

    PageData<User> findByAuthorityAndTenantsIds(Authority authority, List<TenantId> tenantsIds, PageLink pageLink);

    PageData<User> findByAuthorityAndTenantProfilesIds(Authority authority, List<TenantProfileId> tenantProfilesIds, PageLink pageLink);

    List<User> findUsersByCustomMenuId(CustomMenuId id);

    void updateUsersCustomMenuId(List<UserId> ids, CustomMenuId customMenuId);

    boolean existsInEntityGroup(UserId id, EntityGroupId entityGroupId);

    UserAuthDetails findUserAuthDetailsByUserId(UUID tenantId, UUID userId);

}
