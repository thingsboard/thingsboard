// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
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
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.security.model.SecuritySettings;
import org.thingsboard.server.common.data.security.model.UserPasswordPolicy;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.settings.SecuritySettingsService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.auth.AbstractAuthenticationProvider;
import org.thingsboard.server.service.security.auth.MfaAuthenticationToken;
import org.thingsboard.server.service.security.auth.MfaConfigurationToken;
import org.thingsboard.server.service.security.auth.mfa.TwoFactorAuthService;
import org.thingsboard.server.service.security.exception.UserPasswordNotValidException;
import org.thingsboard.server.service.security.ip.SysAdminIpPolicy;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;

import java.net.InetAddress;
import org.thingsboard.server.service.security.permission.UserPermissionsService;
import org.thingsboard.server.service.security.system.SystemSecurityService;

@Component
@Slf4j
@TbCoreComponent
public class RestAuthenticationProvider extends AbstractAuthenticationProvider {

    private final SystemSecurityService systemSecurityService;
    private final SecuritySettingsService securitySettingsService;
    private final UserService userService;
    private final UserPermissionsService userPermissionsService;
    private final TwoFactorAuthService twoFactorAuthService;
    private final SysAdminIpPolicy sysAdminIpPolicy;

    @Autowired
    public RestAuthenticationProvider(final UserService userService, final CustomerService customerService,
                                      final UserPermissionsService userPermissionsService,
                                      final SystemSecurityService systemSecurityService,
                                      SecuritySettingsService securitySettingsService,
                                      TwoFactorAuthService twoFactorAuthService,
                                      SysAdminIpPolicy sysAdminIpPolicy) {
        super(customerService, null, userPermissionsService);
        this.userService = userService;
        this.userPermissionsService = userPermissionsService;
        this.systemSecurityService = systemSecurityService;
        this.securitySettingsService = securitySettingsService;
        this.twoFactorAuthService = twoFactorAuthService;
        this.sysAdminIpPolicy = sysAdminIpPolicy;
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
            // Checked before credential validation, so a blocked address can neither brute-force the
            // password nor lock the account; the generic error keeps the account's role unknowable.
            if (isSysAdminIpBlocked(user, authentication.getDetails())) {
                throw new BadCredentialsException("Authentication Failed. Username or password not valid.");
            }

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

            MergedUserPermissions userPermissions;
            try {
                userPermissions = userPermissionsService.getMergedPermissions(user, false);
            } catch (Exception e) {
                throw new BadCredentialsException("Failed to get user permissions", e);
            }

            return new SecurityUser(user, userCredentials.isEnabled(), userPrincipal, userPermissions);
        } catch (Exception e) {
            systemSecurityService.logLoginAction(user, authentication.getDetails(), ActionType.LOGIN, e);
            throw e;
        }
    }

    private SecurityUser authenticateByPublicId(UserPrincipal userPrincipal, String publicId) {
        return super.authenticateByPublicId(publicId, "Public Id", userPrincipal);
    }

    private boolean isSysAdminIpBlocked(User user, Object details) {
        InetAddress resolved = null;
        InetAddress socket = null;
        if (details instanceof RestAuthenticationDetails restDetails) {
            resolved = restDetails.getResolvedAddress();
            socket = restDetails.getRawSocketAddress();
        }
        return sysAdminIpPolicy.isBlocked(user.getAuthority(), user.getTenantId(), user.getId(), resolved, socket, "login");
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
    }

}
