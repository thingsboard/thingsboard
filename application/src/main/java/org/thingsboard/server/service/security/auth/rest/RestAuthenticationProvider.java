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
package org.thingsboard.server.service.security.auth.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.security.model.SecuritySettings;
import org.thingsboard.server.common.data.security.model.UserPasswordPolicy;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.settings.SecuritySettingsService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.auth.AbstractAuthenticationProvider;
import org.thingsboard.server.service.security.auth.MfaAuthenticationToken;
import org.thingsboard.server.service.security.auth.MfaConfigurationToken;
import org.thingsboard.server.service.security.auth.mfa.TwoFactorAuthService;
import org.thingsboard.server.service.security.exception.UserPasswordNotValidException;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.security.system.SystemSecurityService;

@Component
@Slf4j
@TbCoreComponent
public class RestAuthenticationProvider extends AbstractAuthenticationProvider {

    private final SystemSecurityService systemSecurityService;
    private final SecuritySettingsService securitySettingsService;
    private final UserService userService;
    private final TwoFactorAuthService twoFactorAuthService;

    @Autowired
    public RestAuthenticationProvider(final UserService userService,
                                      final CustomerService customerService,
                                      final SystemSecurityService systemSecurityService,
                                      SecuritySettingsService securitySettingsService,
                                      TwoFactorAuthService twoFactorAuthService) {
        super(customerService, null);
        this.userService = userService;
        this.systemSecurityService = systemSecurityService;
        this.securitySettingsService = securitySettingsService;
        this.twoFactorAuthService = twoFactorAuthService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Assert.notNull(authentication, "No authentication data provided");

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserPrincipal userPrincipal)) {
            throw new BadCredentialsException("Authentication Failed. Bad user principal.");
        }

        SecurityUser securityUser;
        if (userPrincipal.getType() == UserPrincipal.Type.USER_NAME) {
            String username = userPrincipal.getValue();
            String password = (String) authentication.getCredentials();

            SecuritySettings securitySettings = securitySettingsService.getSecuritySettings();
            UserPasswordPolicy passwordPolicy = securitySettings.getPasswordPolicy();
            if (Boolean.TRUE.equals(passwordPolicy.getForceUserToResetPasswordIfNotValid())) {
                try {
                    systemSecurityService.validatePasswordByPolicy(password, passwordPolicy);
                } catch (DataValidationException e) {
                    throw new UserPasswordNotValidException("The entered password violates our policies. If this is your real password, please reset it.");
                }
            }

            securityUser = authenticateByUsernameAndPassword(authentication, userPrincipal, username, password);
            if (twoFactorAuthService.isTwoFaEnabled(securityUser.getTenantId(), securityUser)) {
                return new MfaAuthenticationToken(securityUser);
            } else if (twoFactorAuthService.isEnforceTwoFaEnabled(securityUser.getTenantId(), securityUser)) {
                return new MfaConfigurationToken(securityUser);
            } else {
                systemSecurityService.logLoginAction(securityUser, authentication.getDetails(), ActionType.LOGIN, null);
            }
        } else {
            String publicId = userPrincipal.getValue();
            securityUser = authenticateByPublicId(userPrincipal, publicId);
        }

        return new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
    }

    private SecurityUser authenticateByUsernameAndPassword(Authentication authentication, UserPrincipal userPrincipal, String username, String password) {
        User user = userService.findUserByEmail(TenantId.SYS_TENANT_ID, username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        try {
            UserCredentials userCredentials = userService.findUserCredentialsByUserId(TenantId.SYS_TENANT_ID, user.getId());
            if (userCredentials == null) {
                throw new UsernameNotFoundException("User credentials not found");
            }

            try {
                systemSecurityService.validateUserCredentials(user.getTenantId(), userCredentials, username, password);
            } catch (LockedException e) {
                systemSecurityService.logLoginAction(user, authentication.getDetails(), ActionType.LOCKOUT, null);
                throw e;
            }

            if (user.getAuthority() == null) {
                throw new InsufficientAuthenticationException("User has no authority assigned");
            }

            return new SecurityUser(user, userCredentials.isEnabled(), userPrincipal);
        } catch (Exception e) {
            systemSecurityService.logLoginAction(user, authentication.getDetails(), ActionType.LOGIN, e);
            throw e;
        }
    }

    private SecurityUser authenticateByPublicId(UserPrincipal userPrincipal, String publicId) {
        return super.authenticateByPublicId(publicId, "Public Id", userPrincipal);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
    }

}
