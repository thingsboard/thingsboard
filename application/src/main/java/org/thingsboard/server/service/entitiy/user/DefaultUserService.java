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
package org.thingsboard.server.service.entitiy.user;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static org.thingsboard.server.controller.UserController.ACTIVATE_URL_PATTERN;

@Service
@TbCoreComponent
@AllArgsConstructor
@Slf4j
public class DefaultUserService extends AbstractTbEntityService implements TbUserService {

    private final MailService mailService;
    private final SystemSecurityService systemSecurityService;

    @Override
    public User save(TenantId tenantId, CustomerId customerId, User tbUser, boolean sendActivationMail,
                     HttpServletRequest request, SecurityUser user) throws ThingsboardException {
        ActionType actionType = tbUser.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        try {
            boolean sendEmail = tbUser.getId() == null && sendActivationMail;
            User savedUser = checkNotNull(userService.saveUser(tbUser));
            if (sendEmail) {
                UserCredentials userCredentials = userService.findUserCredentialsByUserId(tenantId, savedUser.getId());
                String baseUrl = systemSecurityService.getBaseUrl(tenantId, customerId, request);
                String activateUrl = String.format(ACTIVATE_URL_PATTERN, baseUrl,
                        userCredentials.getActivateToken());
                String email = savedUser.getEmail();
                try {
                    mailService.sendActivationEmail(activateUrl, email);
                } catch (ThingsboardException e) {
                    userService.deleteUser(tenantId, savedUser.getId());
                    throw e;
                }
            }
            notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, customerId, savedUser.getId(),
                    savedUser, user, actionType, true, null);
            return savedUser;
        } catch (Exception e) {
            notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, emptyId(EntityType.USER),
                    tbUser, user, actionType, false, e);
            throw handleException(e);
        }
    }

    @Override
    public void delete(TenantId tenantId, CustomerId customerId, User tbUser, SecurityUser user) throws ThingsboardException {
        UserId userId = tbUser.getId();
        try {
            List<EdgeId> relatedEdgeIds = findRelatedEdgeIds(tenantId, userId);

            userService.deleteUser(tenantId, userId);
            notificationEntityService.notifyDeleteEntity(tenantId, userId, tbUser, customerId,
                    ActionType.DELETED,  relatedEdgeIds, user, userId.toString());
        } catch (Exception e) {
            notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, emptyId(EntityType.USER),
                    null, user, ActionType.DELETED, false, e, userId.toString());
            throw handleException(e);
        }
    }
}
