/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserCredentialsId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.security.event.UserCredentialsInvalidationEvent;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.entity.TbEntityService;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;
import static org.thingsboard.server.dao.service.Validator.validateString;

@Service("UserDaoService")
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl extends AbstractEntityService implements UserService {

    public static final String USER_PASSWORD_HISTORY = "userPasswordHistory";

    private static final String LAST_LOGIN_TS = "lastLoginTs";
    private static final String FAILED_LOGIN_ATTEMPTS = "failedLoginAttempts";

    private static final int DEFAULT_TOKEN_LENGTH = 30;
    public static final String INCORRECT_USER_ID = "Incorrect userId ";
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    private static final String USER_CREDENTIALS_ENABLED = "userCredentialsEnabled";

    @Value("${security.user_login_case_sensitive:true}")
    private boolean userLoginCaseSensitive;

    private final UserDao userDao;
    private final UserCredentialsDao userCredentialsDao;
    private final UserAuthSettingsDao userAuthSettingsDao;
    private final DataValidator<User> userValidator;
    private final DataValidator<UserCredentials> userCredentialsValidator;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public User findUserByEmail(TenantId tenantId, String email) {
        log.trace("Executing findUserByEmail [{}]", email);
        validateString(email, "Incorrect email " + email);
        if (userLoginCaseSensitive) {
            return userDao.findByEmail(tenantId, email);
        } else {
            return userDao.findByEmail(tenantId, email.toLowerCase());
        }
    }

    @Override
    public User findUserByTenantIdAndEmail(TenantId tenantId, String email) {
        log.trace("Executing findUserByTenantIdAndEmail [{}][{}]", tenantId, email);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateString(email, "Incorrect email " + email);
        return userDao.findByTenantIdAndEmail(tenantId, email);
    }

    @Override
    public User findUserById(TenantId tenantId, UserId userId) {
        log.trace("Executing findUserById [{}]", userId);
        validateId(userId, INCORRECT_USER_ID + userId);
        return userDao.findById(tenantId, userId.getId());
    }

    @Override
    public ListenableFuture<User> findUserByIdAsync(TenantId tenantId, UserId userId) {
        log.trace("Executing findUserByIdAsync [{}]", userId);
        validateId(userId, INCORRECT_USER_ID + userId);
        return userDao.findByIdAsync(tenantId, userId.getId());
    }

    @Override
    public User saveUser(User user) {
        log.trace("Executing saveUser [{}]", user);
        userValidator.validate(user, User::getTenantId);
        if (!userLoginCaseSensitive) {
            user.setEmail(user.getEmail().toLowerCase());
        }
        User savedUser = userDao.save(user.getTenantId(), user);
        if (user.getId() == null) {
            UserCredentials userCredentials = new UserCredentials();
            userCredentials.setEnabled(false);
            userCredentials.setActivateToken(StringUtils.randomAlphanumeric(DEFAULT_TOKEN_LENGTH));
            userCredentials.setUserId(new UserId(savedUser.getUuidId()));
            saveUserCredentialsAndPasswordHistory(user.getTenantId(), userCredentials);
        }
        return savedUser;
    }

    @Override
    public UserCredentials findUserCredentialsByUserId(TenantId tenantId, UserId userId) {
        log.trace("Executing findUserCredentialsByUserId [{}]", userId);
        validateId(userId, INCORRECT_USER_ID + userId);
        return userCredentialsDao.findByUserId(tenantId, userId.getId());
    }

    @Override
    public UserCredentials findUserCredentialsByActivateToken(TenantId tenantId, String activateToken) {
        log.trace("Executing findUserCredentialsByActivateToken [{}]", activateToken);
        validateString(activateToken, "Incorrect activateToken " + activateToken);
        return userCredentialsDao.findByActivateToken(tenantId, activateToken);
    }

    @Override
    public UserCredentials findUserCredentialsByResetToken(TenantId tenantId, String resetToken) {
        log.trace("Executing findUserCredentialsByResetToken [{}]", resetToken);
        validateString(resetToken, "Incorrect resetToken " + resetToken);
        return userCredentialsDao.findByResetToken(tenantId, resetToken);
    }

    @Override
    public UserCredentials saveUserCredentials(TenantId tenantId, UserCredentials userCredentials) {
        log.trace("Executing saveUserCredentials [{}]", userCredentials);
        userCredentialsValidator.validate(userCredentials, data -> tenantId);
        return saveUserCredentialsAndPasswordHistory(tenantId, userCredentials);
    }

    @Override
    public UserCredentials activateUserCredentials(TenantId tenantId, String activateToken, String password) {
        log.trace("Executing activateUserCredentials activateToken [{}], password [{}]", activateToken, password);
        validateString(activateToken, "Incorrect activateToken " + activateToken);
        validateString(password, "Incorrect password " + password);
        UserCredentials userCredentials = userCredentialsDao.findByActivateToken(tenantId, activateToken);
        if (userCredentials == null) {
            throw new IncorrectParameterException(String.format("Unable to find user credentials by activateToken [%s]", activateToken));
        }
        if (userCredentials.isEnabled()) {
            throw new IncorrectParameterException("User credentials already activated");
        }
        userCredentials.setEnabled(true);
        userCredentials.setActivateToken(null);
        userCredentials.setPassword(password);

        return saveUserCredentials(tenantId, userCredentials);
    }

    @Override
    public UserCredentials requestPasswordReset(TenantId tenantId, String email) {
        log.trace("Executing requestPasswordReset email [{}]", email);
        DataValidator.validateEmail(email);
        User user = findUserByEmail(tenantId, email);
        if (user == null) {
            throw new UsernameNotFoundException(String.format("Unable to find user by email [%s]", email));
        }
        UserCredentials userCredentials = userCredentialsDao.findByUserId(tenantId, user.getUuidId());
        if (!userCredentials.isEnabled()) {
            throw new DisabledException(String.format("User credentials not enabled [%s]", email));
        }
        userCredentials.setResetToken(StringUtils.randomAlphanumeric(DEFAULT_TOKEN_LENGTH));
        return saveUserCredentials(tenantId, userCredentials);
    }

    @Override
    public UserCredentials requestExpiredPasswordReset(TenantId tenantId, UserCredentialsId userCredentialsId) {
        UserCredentials userCredentials = userCredentialsDao.findById(tenantId, userCredentialsId.getId());
        if (!userCredentials.isEnabled()) {
            throw new IncorrectParameterException("Unable to reset password for inactive user");
        }
        userCredentials.setResetToken(StringUtils.randomAlphanumeric(DEFAULT_TOKEN_LENGTH));
        return saveUserCredentials(tenantId, userCredentials);
    }

    @Override
    public UserCredentials replaceUserCredentials(TenantId tenantId, UserCredentials userCredentials) {
        log.trace("Executing replaceUserCredentials [{}]", userCredentials);
        userCredentialsValidator.validate(userCredentials, data -> tenantId);
        userCredentialsDao.removeById(tenantId, userCredentials.getUuidId());
        userCredentials.setId(null);
        return saveUserCredentialsAndPasswordHistory(tenantId, userCredentials);
    }

    @Override
    @Transactional
    public void deleteUser(TenantId tenantId, UserId userId) {
        log.trace("Executing deleteUser [{}]", userId);
        validateId(userId, INCORRECT_USER_ID + userId);
        UserCredentials userCredentials = userCredentialsDao.findByUserId(tenantId, userId.getId());
        userCredentialsDao.removeById(tenantId, userCredentials.getUuidId());
        userAuthSettingsDao.removeByUserId(userId);
        deleteEntityRelations(tenantId, userId);
        userDao.removeById(tenantId, userId.getId());
        eventPublisher.publishEvent(new UserCredentialsInvalidationEvent(userId));
    }

    @Override
    public PageData<User> findUsersByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findUsersByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        return userDao.findByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<User> findTenantAdmins(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findTenantAdmins, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        return userDao.findTenantAdmins(tenantId.getId(), pageLink);
    }

    @Override
    public void deleteTenantAdmins(TenantId tenantId) {
        log.trace("Executing deleteTenantAdmins, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantAdminsRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public PageData<User> findCustomerUsers(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findCustomerUsers, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, "Incorrect customerId " + customerId);
        validatePageLink(pageLink);
        return userDao.findCustomerUsers(tenantId.getId(), customerId.getId(), pageLink);
    }

    @Override
    public void deleteCustomerUsers(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing deleteCustomerUsers, customerId [{}]", customerId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, "Incorrect customerId " + customerId);
        customerUsersRemover.removeEntities(tenantId, customerId);
    }

    @Override
    public void setUserCredentialsEnabled(TenantId tenantId, UserId userId, boolean enabled) {
        log.trace("Executing setUserCredentialsEnabled [{}], [{}]", userId, enabled);
        validateId(userId, INCORRECT_USER_ID + userId);
        UserCredentials userCredentials = userCredentialsDao.findByUserId(tenantId, userId.getId());
        userCredentials.setEnabled(enabled);
        saveUserCredentials(tenantId, userCredentials);

        User user = findUserById(tenantId, userId);
        JsonNode additionalInfo = user.getAdditionalInfo();
        if (!(additionalInfo instanceof ObjectNode)) {
            additionalInfo = JacksonUtil.newObjectNode();
        }
        ((ObjectNode) additionalInfo).put(USER_CREDENTIALS_ENABLED, enabled);
        user.setAdditionalInfo(additionalInfo);
        if (enabled) {
            resetFailedLoginAttempts(user);
        }
        userDao.save(user.getTenantId(), user);
    }


    @Override
    public void resetFailedLoginAttempts(TenantId tenantId, UserId userId) {
        log.trace("Executing onUserLoginSuccessful [{}]", userId);
        User user = findUserById(tenantId, userId);
        resetFailedLoginAttempts(user);
        saveUser(user);
    }

    private void resetFailedLoginAttempts(User user) {
        JsonNode additionalInfo = user.getAdditionalInfo();
        if (!(additionalInfo instanceof ObjectNode)) {
            additionalInfo = JacksonUtil.newObjectNode();
        }
        ((ObjectNode) additionalInfo).put(FAILED_LOGIN_ATTEMPTS, 0);
        user.setAdditionalInfo(additionalInfo);
    }

    @Override
    public void setLastLoginTs(TenantId tenantId, UserId userId) {
        User user = findUserById(tenantId, userId);
        JsonNode additionalInfo = user.getAdditionalInfo();
        if (!(additionalInfo instanceof ObjectNode)) {
            additionalInfo = JacksonUtil.newObjectNode();
        }
        ((ObjectNode) additionalInfo).put(LAST_LOGIN_TS, System.currentTimeMillis());
        user.setAdditionalInfo(additionalInfo);
        saveUser(user);
    }

    @Override
    public int increaseFailedLoginAttempts(TenantId tenantId, UserId userId) {
        log.trace("Executing onUserLoginIncorrectCredentials [{}]", userId);
        User user = findUserById(tenantId, userId);
        int failedLoginAttempts = increaseFailedLoginAttempts(user);
        saveUser(user);
        return failedLoginAttempts;
    }

    private int increaseFailedLoginAttempts(User user) {
        JsonNode additionalInfo = user.getAdditionalInfo();
        if (!(additionalInfo instanceof ObjectNode)) {
            additionalInfo = JacksonUtil.newObjectNode();
        }
        int failedLoginAttempts = 0;
        if (additionalInfo.has(FAILED_LOGIN_ATTEMPTS)) {
            failedLoginAttempts = additionalInfo.get(FAILED_LOGIN_ATTEMPTS).asInt();
        }
        failedLoginAttempts = failedLoginAttempts + 1;
        ((ObjectNode) additionalInfo).put(FAILED_LOGIN_ATTEMPTS, failedLoginAttempts);
        user.setAdditionalInfo(additionalInfo);
        return failedLoginAttempts;
    }

    private UserCredentials saveUserCredentialsAndPasswordHistory(TenantId tenantId, UserCredentials userCredentials) {
        UserCredentials result = userCredentialsDao.save(tenantId, userCredentials);
        User user = findUserById(tenantId, userCredentials.getUserId());
        if (userCredentials.getPassword() != null) {
            updatePasswordHistory(user, userCredentials);
        }
        return result;
    }

    private void updatePasswordHistory(User user, UserCredentials userCredentials) {
        JsonNode additionalInfo = user.getAdditionalInfo();
        if (!(additionalInfo instanceof ObjectNode)) {
            additionalInfo = JacksonUtil.newObjectNode();
        }
        Map<String, String> userPasswordHistoryMap = null;
        JsonNode userPasswordHistoryJson;
        if (additionalInfo.has(USER_PASSWORD_HISTORY)) {
            userPasswordHistoryJson = additionalInfo.get(USER_PASSWORD_HISTORY);
            userPasswordHistoryMap = JacksonUtil.convertValue(userPasswordHistoryJson, new TypeReference<>() {
            });
        }
        if (userPasswordHistoryMap != null) {
            userPasswordHistoryMap.put(Long.toString(System.currentTimeMillis()), userCredentials.getPassword());
            userPasswordHistoryJson = JacksonUtil.valueToTree(userPasswordHistoryMap);
            ((ObjectNode) additionalInfo).replace(USER_PASSWORD_HISTORY, userPasswordHistoryJson);
        } else {
            userPasswordHistoryMap = new HashMap<>();
            userPasswordHistoryMap.put(Long.toString(System.currentTimeMillis()), userCredentials.getPassword());
            userPasswordHistoryJson = JacksonUtil.valueToTree(userPasswordHistoryMap);
            ((ObjectNode) additionalInfo).set(USER_PASSWORD_HISTORY, userPasswordHistoryJson);
        }
        user.setAdditionalInfo(additionalInfo);
        saveUser(user);
    }

    private final PaginatedRemover<TenantId, User> tenantAdminsRemover = new PaginatedRemover<>() {
        @Override
        protected PageData<User> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
            return userDao.findTenantAdmins(id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, User entity) {
            deleteUser(tenantId, new UserId(entity.getUuidId()));
        }
    };

    private final PaginatedRemover<CustomerId, User> customerUsersRemover = new PaginatedRemover<>() {
        @Override
        protected PageData<User> findEntities(TenantId tenantId, CustomerId id, PageLink pageLink) {
            return userDao.findCustomerUsers(tenantId.getId(), id.getId(), pageLink);

        }

        @Override
        protected void removeEntity(TenantId tenantId, User entity) {
            deleteUser(tenantId, new UserId(entity.getUuidId()));
        }
    };

    @Override
    public Optional<HasId<?>> fetchEntity(TenantId tenantId, EntityId entityId) {
        return Optional.of(findUserById(tenantId, new UserId(entityId.getId())));
    }

    @Override
    public CustomerId getCustomerId(TenantId tenantId, EntityId entityId) {
        User user = findUserById(tenantId, new UserId(entityId.getId()));
        return user != null ? user.getCustomerId() : TbEntityService.NULL_CUSTOMER_ID;
    }

}
