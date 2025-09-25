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
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.gen.edge.v1.UserCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UserUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Slf4j
public abstract class BaseUserProcessor extends BaseEdgeProcessor {

    @Autowired
    private DataValidator<User> userValidator;

    protected boolean saveOrUpdateUser(TenantId tenantId, UserId userId, UserUpdateMsg userUpdateMsg) {
        boolean isCreated = false;

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
            setCustomerId(isCreated ? null : userById.getCustomerId(), user);

            userValidator.validate(user, User::getTenantId);

            if (isCreated) {
                user.setId(userId);
                String email = user.getEmail();
                User userByEmail = edgeCtx.getUserService().findUserByEmail(tenantId, email);
                if (userByEmail != null && !userByEmail.getId().equals(userId)) {
                    throw new DataValidationException(String.format(
                            "[%s] User with email %s already exists!", tenantId, email
                    ));
                }
            }

            edgeCtx.getUserService().saveUser(tenantId, user, false);
        } catch (Exception e) {
            log.error("[{}] Failed to process user update msg [{}]", tenantId, userUpdateMsg, e);
            throw e;
        }

        return isCreated;
    }

    protected void updateUserCredentials(TenantId tenantId, UserCredentialsUpdateMsg userCredentialsUpdateMsg) {
        UserCredentials userCredentials = JacksonUtil.fromString(userCredentialsUpdateMsg.getEntity(), UserCredentials.class, true);
        if (userCredentials == null) {
            throw new RuntimeException("[{" + tenantId + "}] userCredentialsUpdateMsg {" + userCredentialsUpdateMsg + "} cannot be converted to user credentials");
        }
        User user = edgeCtx.getUserService().findUserById(tenantId, userCredentials.getUserId());
        if (user != null) {
            log.debug("[{}] Updating user credentials for user [{}]. New credentials Id [{}], enabled [{}]",
                    tenantId, user.getName(), userCredentials.getId(), userCredentials.isEnabled());
            try {
                UserCredentials userCredentialsByUserId = edgeCtx.getUserService().findUserCredentialsByUserId(tenantId, user.getId());

                if (userCredentialsByUserId == null) {
                    userCredentialsByUserId = new UserCredentials();
                }

                userCredentialsByUserId.setId(userCredentials.getId());
                userCredentialsByUserId.setEnabled(userCredentials.isEnabled());
                userCredentialsByUserId.setActivateToken(userCredentials.getActivateToken());
                userCredentialsByUserId.setAdditionalInfo(userCredentials.getAdditionalInfo());
                userCredentialsByUserId.setPassword(userCredentials.getPassword());
                userCredentialsByUserId.setResetToken(userCredentials.getResetToken());
                userCredentialsByUserId.setUserId(user.getId());

                edgeCtx.getUserService().saveUserCredentials(tenantId, userCredentialsByUserId, false);
            } catch (Exception e) {
                log.error("[{}] Can't update user credentials for user [{}], userCredentialsUpdateMsg [{}]",
                        tenantId, user.getName(), userCredentialsUpdateMsg, e);
                throw new RuntimeException(e);
            }
        } else {
            log.warn("[{}] Can't find user by id [{}], userCredentialsUpdateMsg [{}]", tenantId, userCredentials.getUserId(), userCredentialsUpdateMsg);
        }
    }

    protected void setCustomerId(CustomerId existingCustomerId, User user) {
        if (user.getAuthority() == Authority.CUSTOMER_USER) {
            if (user.getCustomerId() == null) {
                if (existingCustomerId != null) {
                    user.setCustomerId(existingCustomerId);
                } else {
                    throw new RuntimeException("Customer user should be assigned to customer!");
                }
            }
        } else {
            user.setCustomerId(null);
        }
    }

}
