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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cache.user.UserCacheEvictEvent;
import org.thingsboard.server.cache.user.UserCacheKey;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.id.UserCredentialsId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.mobile.MobileSessionInfo;
import org.thingsboard.server.common.data.mobile.UserMobileSessionInfo;
import org.thingsboard.server.common.data.notification.targets.platform.CustomerUsersFilter;
import org.thingsboard.server.common.data.notification.targets.platform.SystemLevelUsersFilter;
import org.thingsboard.server.common.data.notification.targets.platform.TenantAdministratorsFilter;
import org.thingsboard.server.common.data.notification.targets.platform.UserListFilter;
import org.thingsboard.server.common.data.notification.targets.platform.UsersFilter;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.security.event.UserCredentialsInvalidationEvent;
import org.thingsboard.server.common.data.settings.UserSettings;
import org.thingsboard.server.common.data.settings.UserSettingsType;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.entity.EntityCountService;
import org.thingsboard.server.dao.eventsourcing.ActionCause;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.settings.SecuritySettingsService;
import org.thingsboard.server.dao.sql.JpaExecutorService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.thingsboard.server.common.data.StringUtils.generateSafeToken;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;
import static org.thingsboard.server.dao.service.Validator.validateString;

@Service("UserDaoService")
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl extends AbstractCachedEntityService<UserCacheKey, User, UserCacheEvictEvent> implements UserService {

    public static final String USER_PASSWORD_HISTORY = "userPasswordHistory";

    public static final int DEFAULT_TOKEN_LENGTH = 30;
    public static final String INCORRECT_USER_ID = "Incorrect userId ";
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    @Value("${security.user_login_case_sensitive:true}")
    private boolean userLoginCaseSensitive;

    private final UserDao userDao;
    private final UserCredentialsDao userCredentialsDao;
    private final UserAuthSettingsDao userAuthSettingsDao;
    private final UserSettingsService userSettingsService;
    private final UserSettingsDao userSettingsDao;
    private final SecuritySettingsService securitySettingsService;
    private final TbTenantProfileCache tenantProfileCache;
    private final DataValidator<User> userValidator;
    private final DataValidator<UserCredentials> userCredentialsValidator;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityCountService countService;
    private final JpaExecutorService executor;

    @Override
    @TransactionalEventListener
    public void handleEvictEvent(UserCacheEvictEvent event) {
        List<UserCacheKey> keys = new ArrayList<>(2);
        keys.add(new UserCacheKey(event.tenantId(), event.newEmail()));
        if (StringUtils.isNotEmpty(event.oldEmail()) && !event.oldEmail().equals(event.newEmail())) {
            keys.add(new UserCacheKey(event.tenantId(), event.oldEmail()));
        }
        cache.evict(keys);
    }

    @Override
    public User findUserByEmail(TenantId tenantId, String email) {
        log.trace("Executing findUserByEmail [{}]", email);
        validateString(email, e -> "Incorrect email " + e);
        if (userLoginCaseSensitive) {
            return userDao.findByEmail(tenantId, email);
        } else {
            return userDao.findByEmail(tenantId, email.toLowerCase());
        }
    }

    @Override
    public User findUserByTenantIdAndEmail(TenantId tenantId, String email) {
        log.trace("Executing findUserByTenantIdAndEmail [{}][{}]", tenantId, email);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateString(email, e -> "Incorrect email " + e);
        return cache.getAndPutInTransaction(new UserCacheKey(tenantId, email),
                () -> userDao.findByTenantIdAndEmail(tenantId, email), true);
    }

    @Override
    public ListenableFuture<User> findUserByTenantIdAndEmailAsync(TenantId tenantId, String email) {
        log.trace("Executing findUserByTenantIdAndEmailAsync [{}][{}]", tenantId, email);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateString(email, e -> "Incorrect email " + e);
        return executor.submit(() -> findUserByTenantIdAndEmail(tenantId, email));
    }

    @Override
    public User findUserById(TenantId tenantId, UserId userId) {
        log.trace("Executing findUserById [{}]", userId);
        validateId(userId, id -> INCORRECT_USER_ID + id);
        return userDao.findById(tenantId, userId.getId());
    }

    @Override
    public ListenableFuture<User> findUserByIdAsync(TenantId tenantId, UserId userId) {
        log.trace("Executing findUserByIdAsync [{}]", userId);
        validateId(userId, id -> INCORRECT_USER_ID + id);
        return userDao.findByIdAsync(tenantId, userId.getId());
    }

    @Override
    @Transactional
    public User saveUser(TenantId tenantId, User user) {
        return saveUser(tenantId, user, true);
    }

    @Override
    @Transactional
    public User saveUser(TenantId tenantId, User user, boolean doValidate) {
        return saveEntity(user, () -> doSaveUser(tenantId, user, doValidate));
    }

    private User doSaveUser(TenantId tenantId, User user, boolean doValidate) {
        log.trace("Executing saveUser [{}]", user);
        User oldUser = null;
        if (doValidate) {
            oldUser = userValidator.validate(user, User::getTenantId);
        } else if (user.getId() != null) {
            oldUser = findUserById(user.getTenantId(), user.getId());
        }
        if (!userLoginCaseSensitive) {
            user.setEmail(user.getEmail().toLowerCase());
        }
        var evictEvent = new UserCacheEvictEvent(user.getTenantId(), user.getEmail(), oldUser != null ? oldUser.getEmail() : null);
        User savedUser;
        try {
            savedUser = userDao.saveAndFlush(user.getTenantId(), user);
            publishEvictEvent(evictEvent);
            if (user.getId() == null) {
                countService.publishCountEntityEvictEvent(savedUser.getTenantId(), EntityType.USER);
                UserCredentials userCredentials = new UserCredentials();
                userCredentials.setEnabled(false);
                userCredentials.setUserId(new UserId(savedUser.getUuidId()));
                userCredentials.setAdditionalInfo(JacksonUtil.newObjectNode());
                userCredentials = generateUserActivationToken(userCredentials);
                userCredentialsDao.save(user.getTenantId(), userCredentials);
            }
            eventPublisher.publishEvent(SaveEntityEvent.builder()
                    .tenantId(savedUser.getTenantId())
                    .entity(savedUser)
                    .oldEntity(oldUser)
                    .entityId(savedUser.getId())
                    .created(user.getId() == null).build());
        } catch (Exception t) {
            handleEvictEvent(evictEvent);
            checkConstraintViolation(t, "tb_user_email_key", "User with email '" + user.getEmail() + "' already present in database!");
            throw t;
        }
        return savedUser;
    }

    @Override
    public UserCredentials findUserCredentialsByUserId(TenantId tenantId, UserId userId) {
        log.trace("Executing findUserCredentialsByUserId [{}]", userId);
        validateId(userId, id -> INCORRECT_USER_ID + id);
        return userCredentialsDao.findByUserId(tenantId, userId.getId());
    }

    @Override
    public UserCredentials findUserCredentialsByActivateToken(TenantId tenantId, String activateToken) {
        log.trace("Executing findUserCredentialsByActivateToken [{}]", activateToken);
        validateString(activateToken, t -> "Incorrect activateToken " + t);
        return userCredentialsDao.findByActivateToken(tenantId, activateToken);
    }

    @Override
    public UserCredentials findUserCredentialsByResetToken(TenantId tenantId, String resetToken) {
        log.trace("Executing findUserCredentialsByResetToken [{}]", resetToken);
        validateString(resetToken, t -> "Incorrect resetToken " + t);
        return userCredentialsDao.findByResetToken(tenantId, resetToken);
    }

    @Override
    public UserCredentials saveUserCredentials(TenantId tenantId, UserCredentials userCredentials) {
        return saveUserCredentials(tenantId, userCredentials, true);
    }

    @Override
    public UserCredentials saveUserCredentials(TenantId tenantId, UserCredentials userCredentials, boolean doValidate) {
        log.trace("Executing saveUserCredentials [{}]", userCredentials);
        if (doValidate) {
            userCredentialsValidator.validate(userCredentials, data -> tenantId);
        }
        UserCredentials result = userCredentialsDao.save(tenantId, userCredentials);
        eventPublisher.publishEvent(ActionEntityEvent.builder()
                .tenantId(tenantId)
                .entityId(userCredentials.getUserId())
                .actionType(ActionType.CREDENTIALS_UPDATED).build());
        return result;
    }

    @Override
    public UserCredentials activateUserCredentials(TenantId tenantId, String activateToken, String password) {
        log.trace("Executing activateUserCredentials activateToken [{}], password [{}]", activateToken, password);
        validateString(activateToken, t -> "Incorrect activateToken " + t);
        validateString(password, p -> "Incorrect password " + p);
        UserCredentials userCredentials = userCredentialsDao.findByActivateToken(tenantId, activateToken);
        if (userCredentials == null) {
            throw new IncorrectParameterException(String.format("Unable to find user credentials by activateToken [%s]", activateToken));
        }
        if (userCredentials.isEnabled()) {
            throw new IncorrectParameterException("User credentials already activated");
        }
        if (userCredentials.isActivationTokenExpired()) {
            throw new IncorrectParameterException("Activation token expired");
        }
        userCredentials.setEnabled(true);
        userCredentials.setActivateToken(null);
        userCredentials.setActivateTokenExpTime(null);
        userCredentials.setPassword(password);
        if (userCredentials.getPassword() != null) {
            updatePasswordHistory(userCredentials);
        }
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
        userCredentials = generatePasswordResetToken(userCredentials);
        return saveUserCredentials(tenantId, userCredentials);
    }

    @Override
    public UserCredentials requestExpiredPasswordReset(TenantId tenantId, UserCredentialsId userCredentialsId) {
        UserCredentials userCredentials = userCredentialsDao.findById(tenantId, userCredentialsId.getId());
        if (!userCredentials.isEnabled()) {
            throw new IncorrectParameterException("Unable to reset password for inactive user");
        }
        userCredentials = generatePasswordResetToken(userCredentials);
        return saveUserCredentials(tenantId, userCredentials);
    }

    @Override
    public UserCredentials generatePasswordResetToken(UserCredentials userCredentials) {
        userCredentials.setResetToken(generateSafeToken(DEFAULT_TOKEN_LENGTH));
        int ttlHours = securitySettingsService.getSecuritySettings().getPasswordResetTokenTtl();
        userCredentials.setResetTokenExpTime(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(ttlHours));
        return userCredentials;
    }

    @Override
    public UserCredentials generateUserActivationToken(UserCredentials userCredentials) {
        userCredentials.setActivateToken(generateSafeToken(DEFAULT_TOKEN_LENGTH));
        int ttlHours = securitySettingsService.getSecuritySettings().getUserActivationTokenTtl();
        userCredentials.setActivateTokenExpTime(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(ttlHours));
        return userCredentials;
    }

    @Override
    public UserCredentials checkUserActivationToken(TenantId tenantId, UserCredentials userCredentials) {
        if (userCredentials.getActivationTokenTtl() < TimeUnit.MINUTES.toMillis(15)) { // renew link if less than 15 minutes before expiration
            userCredentials = generateUserActivationToken(userCredentials);
            userCredentials = saveUserCredentials(tenantId, userCredentials);
            log.debug("[{}][{}] Regenerated expired user activation token", tenantId, userCredentials.getUserId());
        }
        return userCredentials;
    }

    @Override
    public UserCredentials replaceUserCredentials(TenantId tenantId, UserCredentials userCredentials) {
        return replaceUserCredentialsInternal(tenantId, userCredentials, userCredentials.getUuidId(), true);
    }

    @Override
    public UserCredentials replaceUserCredentials(TenantId tenantId, UserCredentials userCredentials,
                                                  UserCredentialsId oldUserCredentialsId, boolean doValidate) {
        return replaceUserCredentialsInternal(tenantId, userCredentials, oldUserCredentialsId.getId(), doValidate);
    }

    private UserCredentials replaceUserCredentialsInternal(TenantId tenantId, UserCredentials userCredentials,
                                                           UUID oldCredentialsUuid, boolean doValidate) {
        log.trace("[{}] Replacing user credentials for user [{}], old credentials ID [{}]",
                tenantId, userCredentials.getUserId(), oldCredentialsUuid);

        if (doValidate) {
            userCredentialsValidator.validate(userCredentials, data -> tenantId);
        }

        try {
            userCredentialsDao.removeById(tenantId, oldCredentialsUuid);

            if (userCredentials.getPassword() != null) {
                updatePasswordHistory(userCredentials);
            }

            UserCredentials savedCredentials = userCredentialsDao.save(tenantId, userCredentials);

            eventPublisher.publishEvent(ActionEntityEvent.builder()
                    .tenantId(tenantId)
                    .entityId(userCredentials.getUserId())
                    .actionType(ActionType.CREDENTIALS_UPDATED)
                    .build());

            return savedCredentials;
        } catch (Exception e) {
            log.error("[{}] Failed to replace user credentials for user [{}]", tenantId, userCredentials.getUserId(), e);
            throw new RuntimeException("Failed to replace user credentials", e);
        }
    }

    @Override
    @Transactional
    public void deleteUser(TenantId tenantId, User user) {
        deleteUser(tenantId, user, null);
    }

    private void deleteUser(TenantId tenantId, User user, ActionCause cause) {
        Objects.requireNonNull(user, "User is null");
        UserId userId = user.getId();
        log.trace("[{}] Executing deleteUser [{}]", tenantId, userId);
        validateId(userId, id -> INCORRECT_USER_ID + id);
        userCredentialsDao.removeByUserId(tenantId, userId);
        userAuthSettingsDao.removeByUserId(userId);
        publishEvictEvent(new UserCacheEvictEvent(user.getTenantId(), user.getEmail(), null));
        userSettingsDao.removeByUserId(tenantId, userId);
        userDao.removeById(tenantId, userId.getId());
        eventPublisher.publishEvent(new UserCredentialsInvalidationEvent(userId));
        countService.publishCountEntityEvictEvent(tenantId, EntityType.USER);
        eventPublisher.publishEvent(DeleteEntityEvent.builder()
                .tenantId(user.getTenantId())
                .entityId(userId)
                .entity(user)
                .cause(cause)
                .build());
    }

    @Override
    public PageData<User> findUsersByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findUsersByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        return userDao.findByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<User> findTenantAdmins(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findTenantAdmins, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        return userDao.findTenantAdmins(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<User> findSysAdmins(PageLink pageLink) {
        return userDao.findAllByAuthority(Authority.SYS_ADMIN, pageLink);
    }

    @Override
    public PageData<User> findAllTenantAdmins(PageLink pageLink) {
        return userDao.findAllByAuthority(Authority.TENANT_ADMIN, pageLink);
    }

    @Override
    public PageData<User> findTenantAdminsByTenantsIds(List<TenantId> tenantsIds, PageLink pageLink) {
        return userDao.findByAuthorityAndTenantsIds(Authority.TENANT_ADMIN, tenantsIds, pageLink);
    }

    @Override
    public PageData<User> findTenantAdminsByTenantProfilesIds(List<TenantProfileId> tenantProfilesIds, PageLink pageLink) {
        return userDao.findByAuthorityAndTenantProfilesIds(Authority.TENANT_ADMIN, tenantProfilesIds, pageLink);
    }

    @Override
    public PageData<User> findAllUsers(PageLink pageLink) {
        return userDao.findAll(pageLink);
    }

    @Override
    public void deleteTenantAdmins(TenantId tenantId) {
        log.trace("Executing deleteTenantAdmins, tenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        tenantAdminsRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public void deleteAllByTenantId(TenantId tenantId) {
        log.trace("Executing deleteByTenantId, tenantId [{}]", tenantId);
        usersRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        deleteAllByTenantId(tenantId);
    }

    @Override
    public PageData<User> findCustomerUsers(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findCustomerUsers, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(customerId, id -> "Incorrect customerId " + id);
        validatePageLink(pageLink);
        return userDao.findCustomerUsers(tenantId.getId(), customerId.getId(), pageLink);
    }

    @Override
    public PageData<User> findUsersByCustomerIds(TenantId tenantId, List<CustomerId> customerIds, PageLink pageLink) {
        log.trace("Executing findTenantAndCustomerUsers, tenantId [{}], customerIds [{}], pageLink [{}]", tenantId, customerIds, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        customerIds.forEach(customerId -> validateId(customerId, id -> "Incorrect customerId " + id));
        return userDao.findUsersByCustomerIds(tenantId.getId(), customerIds, pageLink);
    }

    @Override
    public void deleteCustomerUsers(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing deleteCustomerUsers, customerId [{}]", customerId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(customerId, id -> "Incorrect customerId " + id);
        customerUsersRemover.removeEntities(tenantId, customerId);
    }

    @Transactional
    @Override
    public void setUserCredentialsEnabled(TenantId tenantId, UserId userId, boolean enabled) {
        log.trace("Executing setUserCredentialsEnabled [{}], [{}]", userId, enabled);
        validateId(userId, id -> INCORRECT_USER_ID + id);
        UserCredentials userCredentials = userCredentialsDao.findByUserId(tenantId, userId.getId());
        userCredentials.setEnabled(enabled);
        if (enabled) {
            userCredentials.setFailedLoginAttempts(0);
        }
        saveUserCredentials(tenantId, userCredentials);
    }

    @Override
    public void resetFailedLoginAttempts(TenantId tenantId, UserId userId) {
        log.trace("Executing resetFailedLoginAttempts [{}]", userId);
        userCredentialsDao.setFailedLoginAttempts(tenantId, userId, 0);
    }

    @Override
    public void updateLastLoginTs(TenantId tenantId, UserId userId) {
        userCredentialsDao.setLastLoginTs(tenantId, userId, System.currentTimeMillis());
    }

    @Override
    public void saveMobileSession(TenantId tenantId, UserId userId, String mobileToken, MobileSessionInfo sessionInfo) {
        removeMobileSession(tenantId, mobileToken); // unassigning fcm token from other users, in case we didn't clean up it on log out or mobile app uninstall

        UserMobileSessionInfo mobileInfo = findMobileSessionInfo(tenantId, userId).orElseGet(() -> {
            UserMobileSessionInfo newMobileInfo = new UserMobileSessionInfo();
            newMobileInfo.setSessions(new HashMap<>());
            return newMobileInfo;
        });
        mobileInfo.getSessions().put(mobileToken, sessionInfo);
        userSettingsService.updateUserSettings(tenantId, userId, UserSettingsType.MOBILE, JacksonUtil.valueToTree(mobileInfo));
    }

    @Override
    public Map<String, MobileSessionInfo> findMobileSessions(TenantId tenantId, UserId userId) {
        return findMobileSessionInfo(tenantId, userId).map(UserMobileSessionInfo::getSessions).orElse(Collections.emptyMap());
    }

    @Override
    public MobileSessionInfo findMobileSession(TenantId tenantId, UserId userId, String mobileToken) {
        return findMobileSessionInfo(tenantId, userId).map(mobileInfo -> mobileInfo.getSessions().get(mobileToken)).orElse(null);
    }

    @Override
    public void removeMobileSession(TenantId tenantId, String mobileToken) {
        for (UserSettings userSettings : userSettingsDao.findByTypeAndPath(tenantId, UserSettingsType.MOBILE, "sessions", mobileToken)) {
            ((ObjectNode) userSettings.getSettings().get("sessions")).remove(mobileToken);
            userSettingsService.saveUserSettings(tenantId, userSettings);
        }
    }

    @Override
    public int countTenantAdmins(TenantId tenantId) {
        return userDao.countTenantAdmins(tenantId.getId());
    }

    private Optional<UserMobileSessionInfo> findMobileSessionInfo(TenantId tenantId, UserId userId) {
        return Optional.ofNullable(userSettingsService.findUserSettings(tenantId, userId, UserSettingsType.MOBILE))
                .map(UserSettings::getSettings).map(settings -> JacksonUtil.treeToValue(settings, UserMobileSessionInfo.class));
    }

    @Override
    public int increaseFailedLoginAttempts(TenantId tenantId, UserId userId) {
        log.trace("Executing increaseFailedLoginAttempts [{}]", userId);
        return userCredentialsDao.incrementFailedLoginAttempts(tenantId, userId);
    }

    @Override
    public PageData<User> findUsersByFilter(TenantId tenantId, UsersFilter filter, PageLink pageLink) {
        switch (filter.getType()) {
            case USER_LIST -> {
                List<User> users = ((UserListFilter) filter).getUsersIds().stream()
                        .limit(pageLink.getPageSize())
                        .map(UserId::new).map(userId -> findUserById(tenantId, userId))
                        .filter(Objects::nonNull).collect(Collectors.toList());
                return new PageData<>(users, 1, users.size(), false);
            }
            case CUSTOMER_USERS -> {
                if (tenantId.equals(TenantId.SYS_TENANT_ID)) {
                    throw new IllegalArgumentException("Customer users target is not supported for system administrator");
                }
                CustomerUsersFilter customerUsersFilter = (CustomerUsersFilter) filter;
                return findCustomerUsers(tenantId, new CustomerId(customerUsersFilter.getCustomerId()), pageLink);
            }
            case TENANT_ADMINISTRATORS -> {
                TenantAdministratorsFilter tenantAdministratorsFilter = (TenantAdministratorsFilter) filter;
                if (!tenantId.equals(TenantId.SYS_TENANT_ID)) {
                    return findTenantAdmins(tenantId, pageLink);
                } else {
                    if (isNotEmpty(tenantAdministratorsFilter.getTenantsIds())) {
                        return findTenantAdminsByTenantsIds(tenantAdministratorsFilter.getTenantsIds().stream()
                                .map(TenantId::fromUUID).collect(Collectors.toList()), pageLink);
                    } else if (isNotEmpty(tenantAdministratorsFilter.getTenantProfilesIds())) {
                        return findTenantAdminsByTenantProfilesIds(tenantAdministratorsFilter.getTenantProfilesIds().stream()
                                .map(TenantProfileId::new).collect(Collectors.toList()), pageLink);
                    } else {
                        return findAllTenantAdmins(pageLink);
                    }
                }
            }
            case SYSTEM_ADMINISTRATORS -> {
                return findSysAdmins(pageLink);
            }
            case ALL_USERS -> {
                if (!tenantId.equals(TenantId.SYS_TENANT_ID)) {
                    return findUsersByTenantId(tenantId, pageLink);
                } else {
                    return findAllUsers(pageLink);
                }
            }
            default -> throw new IllegalArgumentException("Recipient type not supported");
        }
    }

    @Override
    public boolean matchesFilter(TenantId tenantId, SystemLevelUsersFilter filter, User user) {
        switch (filter.getType()) {
            case TENANT_ADMINISTRATORS -> {
                if (user.isSystemAdmin() || user.isCustomerUser()) {
                    return false;
                }
                TenantAdministratorsFilter tenantAdministratorsFilter = (TenantAdministratorsFilter) filter;
                if (isNotEmpty(tenantAdministratorsFilter.getTenantsIds())) {
                    return tenantAdministratorsFilter.getTenantsIds().contains(user.getTenantId().getId());
                } else if (isNotEmpty(tenantAdministratorsFilter.getTenantProfilesIds())) {
                    return tenantAdministratorsFilter.getTenantProfilesIds().contains(tenantProfileCache.get(user.getTenantId()).getUuidId());
                } else {
                    return user.getAuthority() == Authority.TENANT_ADMIN;
                }
            }
            case SYSTEM_ADMINISTRATORS -> {
                return user.getAuthority() == Authority.SYS_ADMIN;
            }
            case ALL_USERS -> {
                return true;
            }
            default -> throw new IllegalArgumentException("Recipient type not supported");
        }

    }

    private void updatePasswordHistory(UserCredentials userCredentials) {
        JsonNode additionalInfo = userCredentials.getAdditionalInfo();
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
        userCredentials.setAdditionalInfo(additionalInfo);
    }

    private final PaginatedRemover<TenantId, User> tenantAdminsRemover = new PaginatedRemover<>() {
        @Override
        protected PageData<User> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
            return userDao.findTenantAdmins(id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, User user) {
            deleteUser(tenantId, user);
        }
    };

    private final PaginatedRemover<CustomerId, User> customerUsersRemover = new PaginatedRemover<>() {
        @Override
        protected PageData<User> findEntities(TenantId tenantId, CustomerId id, PageLink pageLink) {
            return userDao.findCustomerUsers(tenantId.getId(), id.getId(), pageLink);

        }

        @Override
        protected void removeEntity(TenantId tenantId, User entity) {
            deleteUser(tenantId, entity);
        }
    };

    private final PaginatedRemover<TenantId, User> usersRemover = new PaginatedRemover<>() {
        @Override
        protected PageData<User> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
            return findUsersByTenantId(tenantId, pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, User user) {
            deleteUser(tenantId, user, ActionCause.TENANT_DELETION);
        }
    };

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findUserById(tenantId, new UserId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        return FluentFuture.from(findUserByIdAsync(tenantId, new UserId(entityId.getId())))
                .transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public long countByTenantId(TenantId tenantId) {
        return userDao.countByTenantId(tenantId);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.USER;
    }

}
