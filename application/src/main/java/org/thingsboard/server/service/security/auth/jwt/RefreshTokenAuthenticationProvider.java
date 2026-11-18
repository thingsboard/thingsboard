// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.security.auth.jwt;

import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.service.security.auth.AbstractAuthenticationProvider;
import org.thingsboard.server.service.security.auth.RefreshAuthenticationToken;
import org.thingsboard.server.service.security.auth.TokenOutdatingService;
import org.thingsboard.server.service.security.auth.rest.RestAuthenticationDetails;
import org.thingsboard.server.service.security.exception.IpNotAllowedException;
import org.thingsboard.server.service.security.ip.SysAdminIpPolicy;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.model.token.RawAccessJwtToken;
import org.thingsboard.server.service.security.permission.UserPermissionsService;
import org.thingsboard.server.service.user.cache.UserAuthDetailsCache;

@Component
public class RefreshTokenAuthenticationProvider extends AbstractAuthenticationProvider {

    private final JwtTokenFactory tokenFactory;
    private final TokenOutdatingService tokenOutdatingService;
    private final SysAdminIpPolicy sysAdminIpPolicy;

    public RefreshTokenAuthenticationProvider(JwtTokenFactory jwtTokenFactory, UserAuthDetailsCache userAuthDetailsCache,
                                              UserPermissionsService userPermissionsService, CustomerService customerService,
                                              TokenOutdatingService tokenOutdatingService, SysAdminIpPolicy sysAdminIpPolicy) {
        super(customerService, userAuthDetailsCache, userPermissionsService);
        this.tokenFactory = jwtTokenFactory;
        this.tokenOutdatingService = tokenOutdatingService;
        this.sysAdminIpPolicy = sysAdminIpPolicy;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Assert.notNull(authentication, "No authentication data provided");
        RawAccessJwtToken rawAccessToken = (RawAccessJwtToken) authentication.getCredentials();
        SecurityUser unsafeUser = tokenFactory.parseRefreshToken(rawAccessToken.token());
        UserPrincipal principal = unsafeUser.getUserPrincipal();

        SecurityUser securityUser;
        if (principal.getType() == UserPrincipal.Type.USER_NAME) {
            securityUser = authenticateByUserId(TenantId.SYS_TENANT_ID, unsafeUser.getId(), null, false);
        } else {
            securityUser = authenticateByPublicId(principal.getValue());
        }
        securityUser.setSessionId(unsafeUser.getSessionId());
        if (tokenOutdatingService.isOutdated(rawAccessToken.token(), securityUser.getId())) {
            throw new CredentialsExpiredException("Token is outdated");
        }

        // the refresh success handler responds directly, bypassing post-auth filters, so the
        // sysadmin IP allowlist is enforced here, after the user's real authority is re-loaded
        if (authentication.getDetails() instanceof RestAuthenticationDetails details) {
            if (sysAdminIpPolicy.isBlocked(securityUser.getAuthority(), securityUser.getTenantId(), securityUser.getId(),
                    details.getResolvedAddress(), details.getRawSocketAddress(), "refresh")) {
                throw new IpNotAllowedException();
            }
        } else if (sysAdminIpPolicy.isEnforced() && sysAdminIpPolicy.isSysAdminScoped(securityUser)) {
            // no request context to judge the address by: fail closed for sysadmin-scoped principals
            throw new IpNotAllowedException();
        }

        return new RefreshAuthenticationToken(securityUser);
    }

    private SecurityUser authenticateByPublicId(String publicId) {
        return super.authenticateByPublicId(publicId, "Refresh token", null);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (RefreshAuthenticationToken.class.isAssignableFrom(authentication));
    }

}
