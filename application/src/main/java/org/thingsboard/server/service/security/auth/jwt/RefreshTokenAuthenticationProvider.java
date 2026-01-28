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
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.model.token.RawAccessJwtToken;
import org.thingsboard.server.service.user.cache.UserAuthDetailsCache;

@Component
public class RefreshTokenAuthenticationProvider extends AbstractAuthenticationProvider {

    private final JwtTokenFactory tokenFactory;
    private final TokenOutdatingService tokenOutdatingService;

    public RefreshTokenAuthenticationProvider(JwtTokenFactory jwtTokenFactory, UserAuthDetailsCache userAuthDetailsCache,
                                              CustomerService customerService, TokenOutdatingService tokenOutdatingService) {
        super(customerService, userAuthDetailsCache);
        this.tokenFactory = jwtTokenFactory;
        this.tokenOutdatingService = tokenOutdatingService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Assert.notNull(authentication, "No authentication data provided");
        RawAccessJwtToken rawAccessToken = (RawAccessJwtToken) authentication.getCredentials();
        SecurityUser unsafeUser = tokenFactory.parseRefreshToken(rawAccessToken.token());
        UserPrincipal principal = unsafeUser.getUserPrincipal();

        SecurityUser securityUser;
        if (principal.getType() == UserPrincipal.Type.USER_NAME) {
            securityUser = authenticateByUserId(TenantId.SYS_TENANT_ID, unsafeUser.getId());
        } else {
            securityUser = authenticateByPublicId(principal.getValue());
        }
        securityUser.setSessionId(unsafeUser.getSessionId());
        if (tokenOutdatingService.isOutdated(rawAccessToken.token(), securityUser.getId())) {
            throw new CredentialsExpiredException("Token is outdated");
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
