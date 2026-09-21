// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.service.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.user.UserCredentialsDao;
import org.thingsboard.server.dao.user.UserService;

@Component
public class UserCredentialsDataValidator extends DataValidator<UserCredentials> {

    @Autowired
    private UserCredentialsDao userCredentialsDao;

    @Autowired
    @Lazy
    private UserService userService;

    @Override
    protected void validateCreate(TenantId tenantId, UserCredentials userCredentials) {
        throw new IncorrectParameterException("Creation of new user credentials is prohibited.");
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, UserCredentials userCredentials) {
        if (userCredentials.getUserId() == null) {
            throw new DataValidationException("User credentials should be assigned to user!");
        }
        if (userCredentials.isEnabled()) {
            if (StringUtils.isEmpty(userCredentials.getPassword())) {
                throw new DataValidationException("Enabled user credentials should have password!");
            }
            if (StringUtils.isNotEmpty(userCredentials.getActivateToken())) {
                throw new DataValidationException("Enabled user credentials can't have activate token!");
            }
        }
        UserCredentials existingUserCredentialsEntity = userCredentialsDao.findById(tenantId, userCredentials.getId().getId());
        if (existingUserCredentialsEntity == null) {
            throw new DataValidationException("Unable to update non-existent user credentials!");
        }
        User user = userService.findUserById(tenantId, userCredentials.getUserId());
        if (user == null) {
            throw new DataValidationException("Can't assign user credentials to non-existent user!");
        }
    }
}
