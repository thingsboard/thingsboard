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
package org.thingsboard.server.service.edge.rpc.processor.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.gen.edge.v1.UserCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UserUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Slf4j
public abstract class BaseUserProcessor extends BaseEdgeProcessor {

    @Autowired
    private DataValidator<User> userValidator;

    protected Pair<Boolean, Boolean> saveOrUpdateUser(TenantId tenantId, UserId userId, UserUpdateMsg userUpdateMsg) {
        boolean isCreated = false;
        boolean userEmailUpdated = false;

        try {
            User user = JacksonUtil.fromString(userUpdateMsg.getEntity(), User.class, true);
            if (user == null) {
                throw new RuntimeException("[{" + tenantId + "}] userUpdateMsg {" + userUpdateMsg + "} cannot be converted to user");
            }

            User userById = edgeCtx.getUserService().findUserById(tenantId, userId);
            if (userById == null) {
                isCreated = true;
                user.setId(null);
            } else {
                user.setId(userId);
            }

            String userEmail = user.getEmail();
            User existing = edgeCtx.getUserService().findUserByTenantIdAndEmail(tenantId, user.getEmail());

            if (existing != null && !existing.getId().equals(user.getId())) {
                String[] splitEmail = userEmail.split("@");
                userEmail = splitEmail[0] + "_" + StringUtils.randomAlphanumeric(15) + "@" + splitEmail[1];
                log.warn("[{}] User with email {} already exists. Renaming User email to {}",
                        tenantId, user.getEmail(), userEmail);
                userEmailUpdated = true;
            }
            user.setEmail(userEmail);
            setCustomerId(tenantId, isCreated ? null : userById.getCustomerId(), user, userUpdateMsg);

            userValidator.validate(user, User::getTenantId);

            if (isCreated) {
                user.setId(userId);
            }

            edgeCtx.getUserService().saveUser(tenantId, user, false);
        } catch (Exception e) {
            log.error("[{}] Failed to process user update msg [{}]", tenantId, userUpdateMsg, e);
            throw e;
        }

        return Pair.of(isCreated, userEmailUpdated);
    }

    protected void updateUserCredentials(TenantId tenantId, UserCredentialsUpdateMsg updateMsg) {
        UserCredentials userCredentialsFromUpdateMsg = JacksonUtil.fromString(updateMsg.getEntity(), UserCredentials.class, true);
        if (userCredentialsFromUpdateMsg == null) {
            throw new RuntimeException(String.format("[%s] Failed to parse UserCredentials from updateMsg: %s", tenantId, updateMsg));
        }

        User user = edgeCtx.getUserService().findUserById(tenantId, userCredentialsFromUpdateMsg.getUserId());
        if (user == null) {
            log.warn("[{}] Can't find user by id [{}] skipping credentials update. UserCredentialsUpdateMsg [{}]",
                    tenantId, userCredentialsFromUpdateMsg.getUserId(), updateMsg);
            return;
        }

        log.debug("[{}] Updating user credentials for user [{}]. New credentials Id [{}], enabled [{}]",
                tenantId, user.getName(), userCredentialsFromUpdateMsg.getId(), userCredentialsFromUpdateMsg.isEnabled());

        try {
            UserCredentials existing = edgeCtx.getUserService().findUserCredentialsByUserId(tenantId, user.getId());
            boolean created = existing == null;

            UserCredentials updated = created ? new UserCredentials() : existing;
            updated.setId(userCredentialsFromUpdateMsg.getId());
            updated.setUserId(user.getId());
            updated.setEnabled(userCredentialsFromUpdateMsg.isEnabled());
            updated.setActivateToken(userCredentialsFromUpdateMsg.getActivateToken());
            updated.setAdditionalInfo(userCredentialsFromUpdateMsg.getAdditionalInfo());
            updated.setPassword(userCredentialsFromUpdateMsg.getPassword());
            updated.setResetToken(userCredentialsFromUpdateMsg.getResetToken());


            if (created) {
                edgeCtx.getUserService().saveUserCredentials(tenantId, updated, false);
            } else {
                edgeCtx.getUserService().replaceUserCredentials(tenantId, updated, existing.getId(), false);
            }
        } catch (Exception e) {
            log.error("[{}] Can't update user credentials for user [{}], userCredentialsUpdateMsg [{}]",
                    tenantId, user.getName(), updateMsg, e);
            throw new RuntimeException(e);
        }

    }

    protected abstract void setCustomerId(TenantId tenantId, CustomerId customerId, User user, UserUpdateMsg userUpdateMsg);

}
