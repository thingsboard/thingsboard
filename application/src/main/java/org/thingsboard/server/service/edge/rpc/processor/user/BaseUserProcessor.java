// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.edge.rpc.processor.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.gen.edge.v1.UserCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UserUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;
import org.thingsboard.server.service.security.permission.UserPermissionsService;

import java.util.UUID;

import java.util.Objects;

@Slf4j
public abstract class BaseUserProcessor extends BaseEdgeProcessor {

    @Autowired
    private UserPermissionsService userPermissionsService;

    @Autowired
    private DataValidator<User> userValidator;

    protected Pair<Boolean, Boolean> saveOrUpdateUser(TenantId tenantId, UserId userId, UserUpdateMsg userUpdateMsg) {
        boolean isCreated = false;
        boolean userEmailUpdated = false;

        try {
            User user = JacksonUtil.fromString(userUpdateMsg.getEntity(), User.class, true);
            if (user == null) {
                throw new IllegalArgumentException(String.format("[%s] Failed to parse User from UserUpdateMsg for user %s", tenantId, userId));
            }
            if (user.getAuthority() != Authority.TENANT_ADMIN && user.getAuthority() != Authority.CUSTOMER_USER) {
                throw new DataValidationException("User authority " + user.getAuthority() + " is not allowed!");
            }
            user.setTenantId(tenantId);

            User userById = edgeCtx.getUserService().findUserById(tenantId, userId);
            if (userById != null && !tenantId.equals(userById.getTenantId())) {
                throw new DataValidationException("User " + userId + " belongs to a different tenant!");
            }
            if (userById == null) {
                isCreated = true;
                user.setId(null);
            } else {
                changeOwnerIfRequired(tenantId, user.getCustomerId(), userById.getId());
                user.setId(userId);
            }
            if (isSaveRequired(userById, user)) {
                userEmailUpdated = updateUserEmailIfDuplicateExists(tenantId, userId, user);
                setCustomerId(tenantId, isCreated ? null : userById.getCustomerId(), user, userUpdateMsg);

                userValidator.validate(user, User::getTenantId);

                if (isCreated) {
                    user.setId(userId);
                }

                User savedUser = edgeCtx.getUserService().saveUser(tenantId, user, false);
                if (isCreated) {
                    edgeCtx.getEntityGroupService().addEntityToEntityGroupAll(savedUser.getTenantId(), savedUser.getOwnerId(), savedUser.getId());
                }
                userPermissionsService.onUserUpdatedOrRemoved(savedUser);
            }
            safeAddToEntityGroup(tenantId, userUpdateMsg, userId);
        } catch (Exception e) {
            log.error("[{}] Failed to process user update msg for user [{}]", tenantId, userId, e);
            throw new RuntimeException(e);
        }

        return Pair.of(isCreated, userEmailUpdated);
    }

    private void safeAddToEntityGroup(TenantId tenantId, UserUpdateMsg userUpdateMsg, UserId userId) {
        if (userUpdateMsg.hasEntityGroupIdMSB() && userUpdateMsg.hasEntityGroupIdLSB()) {
            UUID entityGroupUUID = safeGetUUID(userUpdateMsg.getEntityGroupIdMSB(),
                    userUpdateMsg.getEntityGroupIdLSB());
            safeAddEntityToGroup(tenantId, new EntityGroupId(entityGroupUUID), userId);
        }
    }

    private boolean updateUserEmailIfDuplicateExists(TenantId tenantId, UserId userId, User user) {
        String email = user.getEmail();
        // email is unique across all tenants
        User userByEmail = edgeCtx.getUserService().findUserByEmail(tenantId, email);

        if (userByEmail != null && !userByEmail.getId().equals(user.getId())) {
            String[] splitEmail = email.split("@");
            String newEmail = splitEmail[0] + "_" + StringUtils.randomAlphanumeric(15) + "@" + splitEmail[1];
            log.warn("[{}] User with email {} already exists. Renaming User email to {}", tenantId, user.getEmail(), newEmail);
            user.setEmail(newEmail);
            return true;
        }
        return false;
    }

    protected void deleteUserAndPushEntityDeletedEventToRuleEngine(TenantId tenantId, UserId userId) throws ThingsboardException {
        deleteUserAndPushEntityDeletedEventToRuleEngine(tenantId, userId, null);
    }

    protected void deleteUserAndPushEntityDeletedEventToRuleEngine(TenantId tenantId, UserId userId, Edge edge) throws ThingsboardException {
        User removedUser = deleteUser(tenantId, userId);
        if (removedUser == null) {
            return;
        }
        CustomerId userCustomerId = removedUser.getCustomerId();
        String userAsString = JacksonUtil.toString(removedUser);
        TbMsgMetaData msgMetaData = edge == null ? new TbMsgMetaData() : getEdgeActionTbMsgMetaData(edge, userCustomerId);
        addRemovedUserMetadata(msgMetaData, removedUser);
        pushEntityEventToRuleEngine(tenantId, userId, userCustomerId, TbMsgType.ENTITY_DELETED, userAsString, msgMetaData);
    }

    private User deleteUser(TenantId tenantId, UserId userId) throws ThingsboardException {
        User userById = edgeCtx.getUserService().findUserById(tenantId, userId);
        if (userById == null || !tenantId.equals(userById.getTenantId())) {
            log.trace("[{}] User with id {} does not exist", tenantId, userId);
            return null;
        }
        edgeCtx.getUserService().deleteUser(tenantId, userById);
        userPermissionsService.onUserUpdatedOrRemoved(userById);
        return userById;
    }

    protected void updateUserCredentials(TenantId tenantId, UserCredentialsUpdateMsg updateMsg) {
        UserCredentials edgeUserCredentials = JacksonUtil.fromString(updateMsg.getEntity(), UserCredentials.class, true);
        if (edgeUserCredentials == null) {
            throw new IllegalArgumentException(String.format("[%s] Failed to parse UserCredentials from UserCredentialsUpdateMsg", tenantId));
        }
        User user = edgeCtx.getUserService().findUserById(tenantId, edgeUserCredentials.getUserId());
        if (user == null || !tenantId.equals(user.getTenantId())) {
            log.warn("[{}] Can't find user by id [{}] skipping credentials update", tenantId, edgeUserCredentials.getUserId());
            return;
        }
        if (edgeUserCredentials.getId() != null) {
            UserCredentials userCredentialsById = edgeCtx.getUserService().findUserCredentialsById(tenantId, edgeUserCredentials.getId());
            if (userCredentialsById != null && !user.getId().equals(userCredentialsById.getUserId())) {
                log.warn("[{}] User credentials [{}] belong to a different user, skipping credentials update", tenantId, edgeUserCredentials.getId());
                return;
            }
        }
        log.debug("[{}] Updating user credentials for user [{}]. New credentials Id [{}], enabled [{}]",
                tenantId, user.getName(), edgeUserCredentials.getId(), edgeUserCredentials.isEnabled());
        try {
            UserCredentials savedUserCredentials = edgeCtx.getUserService().findUserCredentialsByUserId(tenantId, user.getId());
            if (savedUserCredentials != null && !savedUserCredentials.getId().equals(edgeUserCredentials.getId())) {
                edgeCtx.getUserService().deleteUserCredentials(tenantId, savedUserCredentials);
            }
            // only password and enabled flag come from edge, tokens, login attempts and password history stay as on cloud
            UserCredentials updatedUserCredentials = savedUserCredentials != null ? savedUserCredentials : new UserCredentials();
            boolean passwordChanged = !Objects.equals(updatedUserCredentials.getPassword(), edgeUserCredentials.getPassword());
            updatedUserCredentials.setId(edgeUserCredentials.getId());
            updatedUserCredentials.setUserId(user.getId());
            updatedUserCredentials.setEnabled(edgeUserCredentials.isEnabled());
            updatedUserCredentials.setPassword(edgeUserCredentials.getPassword());
            if (passwordChanged && updatedUserCredentials.getPassword() != null) {
                edgeCtx.getUserService().updatePasswordHistory(updatedUserCredentials);
            }
            if (updatedUserCredentials.isEnabled()) {
                // user is activated on edge
                updatedUserCredentials.setActivateToken(null);
                updatedUserCredentials.setActivateTokenExpTime(null);
            } else if (edgeUserCredentials.getActivateToken() != null && updatedUserCredentials.getActivateToken() == null) {
                // user activation is pending on edge, cloud uses its own token
                edgeCtx.getUserService().generateUserActivationToken(updatedUserCredentials);
            }
            edgeCtx.getUserService().saveUserCredentials(tenantId, updatedUserCredentials, false);
        } catch (Exception e) {
            log.error("[{}] Can't update user credentials for user [{}]", tenantId, user.getName(), e);
            throw new RuntimeException(e);
        }
    }

    private void addRemovedUserMetadata(TbMsgMetaData metaData, User removedUser) {
        metaData.putValue("userId", removedUser.getId().toString());
        metaData.putValue("userName", removedUser.getName());
        metaData.putValue("userEmail", removedUser.getEmail());
        if (removedUser.getFirstName() != null) {
            metaData.putValue("userFirstName", removedUser.getFirstName());
        }
        if (removedUser.getLastName() != null) {
            metaData.putValue("userLastName", removedUser.getLastName());
        }
    }

    protected abstract void setCustomerId(TenantId tenantId, CustomerId customerId, User user, UserUpdateMsg userUpdateMsg);

}
