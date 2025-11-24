/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.user;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.UserAuthDetails;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.id.UserCredentialsId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.mobile.MobileSessionInfo;
import org.thingsboard.server.common.data.notification.targets.platform.SystemLevelUsersFilter;
import org.thingsboard.server.common.data.notification.targets.platform.UsersFilter;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;
import java.util.Map;

public interface UserService extends EntityDaoService {

    User findUserById(TenantId tenantId, UserId userId);

    ListenableFuture<User> findUserByIdAsync(TenantId tenantId, UserId userId);

    User findUserByEmail(TenantId tenantId, String email);

    User findUserByTenantIdAndEmail(TenantId tenantId, String email);

    ListenableFuture<User> findUserByTenantIdAndEmailAsync(TenantId tenantId, String email);

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

    UserCredentials replaceUserCredentials(TenantId tenantId, UserCredentials userCredentials,
                                           UserCredentialsId oldUserCredentialsId, boolean doValidate);

    void deleteUser(TenantId tenantId, User user);

    PageData<User> findUsersByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<User> findTenantAdmins(TenantId tenantId, PageLink pageLink);

    PageData<User> findSysAdmins(PageLink pageLink);

    PageData<User> findAllTenantAdmins(PageLink pageLink);

    PageData<User> findTenantAdminsByTenantsIds(List<TenantId> tenantsIds, PageLink pageLink);

    PageData<User> findTenantAdminsByTenantProfilesIds(List<TenantProfileId> tenantProfilesIds, PageLink pageLink);

    PageData<User> findAllUsers(PageLink pageLink);

    void deleteTenantAdmins(TenantId tenantId);

    void deleteAllByTenantId(TenantId tenantId);

    PageData<User> findCustomerUsers(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    PageData<User> findUsersByCustomerIds(TenantId tenantId, List<CustomerId> customerIds, PageLink pageLink);

    void deleteCustomerUsers(TenantId tenantId, CustomerId customerId);

    void setUserCredentialsEnabled(TenantId tenantId, UserId userId, boolean enabled);

    void resetFailedLoginAttempts(TenantId tenantId, UserId userId);

    int increaseFailedLoginAttempts(TenantId tenantId, UserId userId);

    void updateLastLoginTs(TenantId tenantId, UserId userId);

    void saveMobileSession(TenantId tenantId, UserId userId, String mobileToken, MobileSessionInfo sessionInfo);

    Map<String, MobileSessionInfo> findMobileSessions(TenantId tenantId, UserId userId);

    MobileSessionInfo findMobileSession(TenantId tenantId, UserId userId, String mobileToken);

    void removeMobileSession(TenantId tenantId, String mobileToken);

    int countTenantAdmins(TenantId tenantId);

    PageData<User> findUsersByFilter(TenantId tenantId, UsersFilter filter, PageLink pageLink);

    boolean matchesFilter(TenantId tenantId, SystemLevelUsersFilter filter, User user);

    UserAuthDetails findUserAuthDetailsByUserId(TenantId tenantId, UserId userId);

}
