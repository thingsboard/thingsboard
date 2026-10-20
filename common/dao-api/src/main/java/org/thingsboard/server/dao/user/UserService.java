// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.user;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.UserAuthDetails;
import org.thingsboard.server.common.data.UserInfo;
import org.thingsboard.server.common.data.id.CustomMenuId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.id.UserCredentialsId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.mobile.MobileSessionInfo;
import org.thingsboard.server.common.data.notification.targets.platform.SystemLevelUsersFilter;
import org.thingsboard.server.common.data.notification.targets.platform.UsersFilter;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;
import java.util.Map;

public interface UserService extends EntityDaoService {

    User findUserById(TenantId tenantId, UserId userId);

    UserInfo findUserInfoById(TenantId tenantId, UserId userId);

    ListenableFuture<User> findUserByIdAsync(TenantId tenantId, UserId userId);

    ListenableFuture<List<User>> findUsersByTenantIdAndIdsAsync(TenantId tenantId, List<UserId> userIds);

    List<User> findUsersByTenantIdAndIds(TenantId tenantId, List<UserId> userIds);

    User findUserByEmail(TenantId tenantId, String email);

    User findUserByTenantIdAndEmail(TenantId tenantId, String email);

    ListenableFuture<User> findUserByTenantIdAndEmailAsync(TenantId tenantId, String email);

    User changeOwner(User user, EntityId targetOwnerId);

    User saveUser(TenantId tenantId, User user);

    User saveUser(TenantId tenantId, User user, boolean doValidate);

    UserCredentials findUserCredentialsByUserId(TenantId tenantId, UserId userId);

    UserCredentials findUserCredentialsByActivateToken(TenantId tenantId, String activateToken);

    UserCredentials findUserCredentialsByResetToken(TenantId tenantId, String resetToken);

    UserCredentials saveUserCredentials(TenantId tenantId, UserCredentials userCredentials);

    UserCredentials saveUserCredentials(TenantId tenantId, UserCredentials userCredentials, boolean doValidate);

    UserCredentials activateUserCredentials(TenantId tenantId, String activateToken, String password);

    UserCredentials requestPasswordReset(TenantId tenantId, String email);

    UserCredentials requestExpiredPasswordReset(TenantId tenantId, UserCredentialsId userCredentialsId);

    UserCredentials generatePasswordResetToken(UserCredentials userCredentials);

    UserCredentials generateUserActivationToken(UserCredentials userCredentials);

    UserCredentials checkUserActivationToken(TenantId tenantId, UserCredentials userCredentials);

    UserCredentials replaceUserCredentials(TenantId tenantId, UserCredentials userCredentials);

    void deleteUserCredentials(TenantId tenantId, UserCredentials userCredentials);

    void deleteUser(TenantId tenantId, UserId userId);

    void deleteUser(TenantId tenantId, User user);

    PageData<User> findTenantAdmins(TenantId tenantId, PageLink pageLink);

    PageData<User> findUsersByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<User> findSysAdmins(PageLink pageLink);

    boolean existsByAuthority(Authority authority);

    PageData<User> findAllTenantAdmins(PageLink pageLink);

    PageData<User> findTenantAdminsByTenantsIds(List<TenantId> tenantsIds, PageLink pageLink);

    PageData<User> findTenantAdminsByTenantProfilesIds(List<TenantProfileId> tenantProfilesIds, PageLink pageLink);

    PageData<User> findAllUsers(PageLink pageLink);

    void deleteTenantAdmins(TenantId tenantId);

    PageData<User> findAllCustomerUsers(TenantId tenantId, PageLink pageLink);

    void deleteAllByTenantId(TenantId tenantId);

    PageData<User> findCustomerUsers(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    PageData<User> findUsersByCustomerIds(TenantId tenantId, List<CustomerId> customerIds, PageLink pageLink);

    void deleteCustomerUsers(TenantId tenantId, CustomerId customerId);

    PageData<User> findUsersByEntityGroupId(EntityGroupId groupId, PageLink pageLink);

    PageData<User> findUsersByEntityGroupIds(List<EntityGroupId> groupIds, PageLink pageLink);

    PageData<User> findUsersByTenantIdAndRoles(TenantId tenantId, List<RoleId> roles, PageLink pageLink);

    PageData<User> findUsersByTenantsIdsAndRoleId(List<TenantId> tenantsIds, RoleId roleId, PageLink pageLink);

    PageData<User> findUsersByTenantProfilesIdsAndRoleId(List<TenantProfileId> tenantProfilesIds, RoleId roleId, PageLink pageLink);

    PageData<User> findAllUsersByRoleId(RoleId roleId, PageLink pageLink);

    int countUsersByTenantIdAndRoleIdAndIdNotIn(TenantId tenantId, RoleId roleId, List<UserId> userIds);

    void setUserCredentialsEnabled(TenantId tenantId, UserId userId, boolean enabled);

    void resetFailedLoginAttempts(TenantId tenantId, UserId userId);

    int increaseFailedLoginAttempts(TenantId tenantId, UserId userId);

    void updateLastLoginTs(TenantId tenantId, UserId userId);

    PageData<UserInfo> findUserInfosByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<UserInfo> findTenantUserInfosByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<UserInfo> findUserInfosByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    PageData<UserInfo> findUserInfosByTenantIdAndCustomerIdIncludingSubCustomers(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    void saveMobileSession(TenantId tenantId, UserId userId, String mobileToken, MobileSessionInfo sessionInfo);

    Map<String, MobileSessionInfo> findMobileSessions(TenantId tenantId, UserId userId);

    MobileSessionInfo findMobileSession(TenantId tenantId, UserId userId, String mobileToken);

    void removeMobileSession(TenantId tenantId, String mobileToken);

    List<User> findUsersByCustomMenuId(CustomMenuId customMenuId);

    void updateUsersCustomMenuId(List<UserId> ids, CustomMenuId customMenuId);

    boolean existsInEntityGroup(UserId id, EntityGroupId entityGroupId);

    PageData<User> findUsersByFilter(TenantId tenantId, UsersFilter filter, PageLink pageLink);

    boolean matchesFilter(TenantId tenantId, SystemLevelUsersFilter filter, User user);

    UserAuthDetails findUserAuthDetailsByUserId(TenantId tenantId, UserId userId);

}
