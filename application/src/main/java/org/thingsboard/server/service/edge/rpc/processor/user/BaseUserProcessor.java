/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.rpc.processor.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.gen.edge.v1.UserCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UserUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Slf4j
public abstract class BaseUserProcessor extends BaseEdgeProcessor {

    @Autowired
    private DataValidator<User> userValidator;

    @Autowired
    private TbTenantProfileCache tenantProfileCache;

    protected Pair<Boolean, Boolean> saveOrUpdateUser(TenantId tenantId, UserId userId, UserUpdateMsg userUpdateMsg) {
        boolean isCreated = false;
        boolean userEmailUpdated = false;

        try {
            User user = JacksonUtil.fromString(userUpdateMsg.getEntity(), User.class, true);
            if (user == null) {
                throw new IllegalArgumentException(String.format("[%s] Failed to parse User from UserUpdateMsg: %s", tenantId, userUpdateMsg));
            }

            User userById = edgeCtx.getUserService().findUserById(tenantId, userId);
            if (userById == null) {
                isCreated = true;
                user.setId(null);
            } else {
                user.setId(userId);
            }
            if (isSaveRequired(userById, user)) {
                if (!isCreated && keepStoredEmail(tenantId, userById, user)) {
                    userEmailUpdated = true;
                } else {
                    userEmailUpdated = updateUserEmailIfDuplicateExists(tenantId, userId, user);
                }
                setCustomerId(tenantId, isCreated ? null : userById.getCustomerId(), user, userUpdateMsg);

                userValidator.validate(user, User::getTenantId);

                if (isCreated) {
                    user.setId(userId);
                }

                edgeCtx.getUserService().saveUser(tenantId, user, false);
            }
        } catch (Exception e) {
            log.error("[{}] Failed to process user update msg [{}]", tenantId, userUpdateMsg, e);
            throw e;
        }

        return Pair.of(isCreated, userEmailUpdated);
    }

    // An edge is controlled by the tenant admin, so an edge-driven email change is the very change that
    // UserDataValidator forbids for a restricted tenant: the stored address wins. Reported as an email
    // update so the caller pushes the stored address back and the edge stops resending its own.
    private boolean keepStoredEmail(TenantId tenantId, User storedUser, User user) {
        if (storedUser.getEmail().equals(user.getEmail()) || !tenantProfileCache.isRestricted(tenantId)) {
            return false;
        }
        log.warn("[{}] Ignoring edge-driven email change of user {} to {}", tenantId, storedUser.getId(), user.getEmail());
        user.setEmail(storedUser.getEmail());
        return true;
    }

    private boolean updateUserEmailIfDuplicateExists(TenantId tenantId, UserId userId, User user) {
        String email = user.getEmail();
        User userByEmail = edgeCtx.getUserService().findUserByTenantIdAndEmail(tenantId, email);

        if (userByEmail != null && !userByEmail.getId().equals(user.getId())) {
            String[] splitEmail = email.split("@");
            String newEmail = splitEmail[0] + "_" + StringUtils.randomAlphanumeric(15) + "@" + splitEmail[1];
            log.warn("[{}] User with email {} already exists. Renaming User email to {}", tenantId, user.getEmail(), newEmail);
            user.setEmail(newEmail);
            return true;
        }
        return false;
    }

    protected void deleteUserAndPushEntityDeletedEventToRuleEngine(TenantId tenantId, UserId userId) {
        deleteUserAndPushEntityDeletedEventToRuleEngine(tenantId, userId, null);
    }

    protected void deleteUserAndPushEntityDeletedEventToRuleEngine(TenantId tenantId, UserId userId, Edge edge) {
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

    private User deleteUser(TenantId tenantId, UserId userId) {
        User userById = edgeCtx.getUserService().findUserById(tenantId, userId);
        if (userById == null) {
            log.trace("[{}] User with id {} does not exist", tenantId, userId);
            return null;
        }
        edgeCtx.getUserService().deleteUser(tenantId, userById);
        return userById;
    }

    protected void updateUserCredentials(TenantId tenantId, UserCredentialsUpdateMsg updateMsg) {
        UserCredentials userCredentials = JacksonUtil.fromString(updateMsg.getEntity(), UserCredentials.class, true);
        if (userCredentials == null) {
            throw new IllegalArgumentException(String.format("[%s] Failed to parse UserCredentials from updateMsg: %s", tenantId, updateMsg));
        }
        User user = edgeCtx.getUserService().findUserById(tenantId, userCredentials.getUserId());
        if (user == null) {
            log.warn("[{}] Can't find user by id [{}] skipping credentials update. UserCredentialsUpdateMsg [{}]",
                    tenantId, userCredentials.getUserId(), updateMsg);
            return;
        }
        log.debug("[{}] Updating user credentials for user [{}]. New credentials Id [{}], enabled [{}]",
                tenantId, user.getName(), userCredentials.getId(), userCredentials.isEnabled());
        try {
            UserCredentials userCredentialsByUserId = edgeCtx.getUserService().findUserCredentialsByUserId(tenantId, user.getId());
            if (tenantProfileCache.isRestricted(tenantId)) {
                updateRestrictedTenantUserCredentials(tenantId, user, userCredentialsByUserId, userCredentials);
                return;
            }
            if (userCredentialsByUserId != null && !userCredentialsByUserId.getId().equals(userCredentials.getId())) {
                edgeCtx.getUserService().deleteUserCredentials(tenantId, userCredentialsByUserId);
            }
            edgeCtx.getUserService().saveUserCredentials(tenantId, userCredentials, false);
        } catch (Exception e) {
            log.error("[{}] Can't update user credentials for user [{}], userCredentialsUpdateMsg [{}]",
                    tenantId, user.getName(), updateMsg, e);
            throw new RuntimeException(e);
        }
    }

    // The uplink is tenant-controlled and saved without validation, so a restricted tenant could otherwise
    // mark any address activated and whitelist it for the mail recipient allow-list. The password is the only
    // field taken from the edge; everything else, the activation state above all, stays the cloud's.
    private void updateRestrictedTenantUserCredentials(TenantId tenantId, User user, UserCredentials storedCredentials, UserCredentials userCredentials) {
        if (storedCredentials == null) {
            // An edge-created user has no credentials row yet, since saveUser keeps the edge's own id and so
            // never generates one. Built here rather than adjusted in place: the message's own credentials id
            // would otherwise be persisted, and the save is a primary key upsert, so a later uplink naming an
            // existing id would repoint that row at another user.
            UserCredentials createdCredentials = new UserCredentials();
            createdCredentials.setUserId(user.getId());
            createdCredentials.setEnabled(false);
            createdCredentials.setPassword(userCredentials.getPassword());
            createdCredentials.setAdditionalInfo(JacksonUtil.newObjectNode());
            edgeCtx.getUserService().generateUserActivationToken(createdCredentials);
            edgeCtx.getUserService().saveUserCredentials(tenantId, createdCredentials, false);
            return;
        }
        // A password-less message must not blank the stored hash, and there is nothing else to apply.
        if (userCredentials.getPassword() == null) {
            return;
        }
        storedCredentials.setPassword(userCredentials.getPassword());
        edgeCtx.getUserService().saveUserCredentials(tenantId, storedCredentials, false);
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
