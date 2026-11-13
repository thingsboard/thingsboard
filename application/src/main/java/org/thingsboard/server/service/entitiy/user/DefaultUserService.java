// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.entitiy.user;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.UserActivationLink;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@TbCoreComponent
@AllArgsConstructor
@Slf4j
public class DefaultUserService extends AbstractTbEntityService implements TbUserService {

    private final UserService userService;
    private final MailService mailService;
    private final SystemSecurityService systemSecurityService;

    @Override
    public User save(TenantId tenantId, CustomerId customerId, Authority authority, User tbUser, boolean sendActivationMail,
                     HttpServletRequest request, EntityGroup entityGroup, User user) throws ThingsboardException {
        return save(tenantId, customerId, authority, tbUser,
                sendActivationMail, request, entityGroup != null ? Collections.singletonList(entityGroup) : null, user);
    }

    @Override
    public User save(TenantId tenantId, CustomerId customerId, Authority authority, User tbUser, boolean sendActivationMail,
                     HttpServletRequest request, List<EntityGroup> entityGroups, User user) throws ThingsboardException {
        ActionType actionType = tbUser.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        try {
            boolean sendEmail = tbUser.getId() == null && sendActivationMail;
            User savedUser = checkNotNull(userService.saveUser(tenantId, tbUser));

            // Sys Admins do not have entity groups
            if (!tbUser.isSystemAdmin()) {
                // Add Tenant Admins to 'Tenant Administrators' user group if created by Sys Admin
                if (tbUser.getId() == null && authority == Authority.SYS_ADMIN) {
                    EntityGroup admins = entityGroupService.findOrCreateTenantAdminsGroup(savedUser.getTenantId());
                    entityGroupService.addEntityToEntityGroup(savedUser.getTenantId(), admins.getId(), savedUser.getId());
                    logEntityActionService.logEntityAction(tenantId, savedUser.getId(), savedUser, customerId,
                            ActionType.ADDED_TO_ENTITY_GROUP, user);
                } else if (!CollectionUtils.isEmpty(entityGroups) && tbUser.getId() == null) {
                    for (EntityGroup entityGroup : entityGroups) {
                        entityGroupService.addEntityToEntityGroup(savedUser.getTenantId(), entityGroup.getId(), savedUser.getId());
                        logEntityActionService.logEntityAction(tenantId, savedUser.getId(), savedUser, customerId,
                                ActionType.ADDED_TO_ENTITY_GROUP, user, savedUser.getId().toString(), entityGroup.getId().toString(), entityGroup.getName());
                    }
                }
            }

            if (sendEmail) {
                UserActivationLink activationLink = getActivationLink(tenantId, customerId, authority, savedUser.getId(), request);
                try {
                    mailService.sendActivationEmail(tenantId, activationLink.value(), activationLink.ttlMs(), savedUser.getEmail());
                } catch (ThingsboardException e) {
                    userService.deleteUser(tenantId, savedUser);
                    throw new ThingsboardException("Couldn't send user activation email", ThingsboardErrorCode.GENERAL);
                }
            }
            logEntityActionService.logEntityAction(tenantId, savedUser.getId(), savedUser, customerId, actionType, user);
            return savedUser;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.USER), tbUser, actionType, user, e);
            throw e;
        }
    }

    @Override
    public void delete(TenantId tenantId, CustomerId customerId, User user, User responsibleUser) throws ThingsboardException {
        ActionType actionType = ActionType.DELETED;
        UserId userId = user.getId();

        try {
            userService.deleteUser(tenantId, user);
            logEntityActionService.logEntityAction(tenantId, userId, user, customerId, actionType, responsibleUser, customerId.toString());
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.USER),
                    actionType, responsibleUser, e, userId.toString());
            throw e;
        }
    }

    @Override
    public UserActivationLink getActivationLink(TenantId tenantId, CustomerId customerId, Authority authority, UserId userId, HttpServletRequest request) throws ThingsboardException {
        UserCredentials userCredentials = userService.findUserCredentialsByUserId(tenantId, userId);
        if (!userCredentials.isEnabled() && userCredentials.getActivateToken() != null) {
            userCredentials = userService.checkUserActivationToken(tenantId, userCredentials);
            String baseUrl = systemSecurityService.getBaseUrl(authority, tenantId, customerId, request);
            String link = baseUrl + "/api/noauth/activate?activateToken=" + userCredentials.getActivateToken();
            return new UserActivationLink(link, userCredentials.getActivationTokenTtl());
        } else {
            throw new ThingsboardException("User is already activated!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

}
