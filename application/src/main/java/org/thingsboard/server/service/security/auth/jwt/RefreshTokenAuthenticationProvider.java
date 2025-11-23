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
package org.thingsboard.server.service.security.auth.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.UserAuthDetails;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.service.security.auth.RefreshAuthenticationToken;
import org.thingsboard.server.service.security.auth.TokenOutdatingService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.model.token.RawAccessJwtToken;
import org.thingsboard.server.service.user.cache.UserAuthDetailsCache;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RefreshTokenAuthenticationProvider implements AuthenticationProvider {

    private final JwtTokenFactory tokenFactory;
    private final UserAuthDetailsCache userAuthDetailsCache;
    private final CustomerService customerService;
    private final TokenOutdatingService tokenOutdatingService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Assert.notNull(authentication, "No authentication data provided");
        RawAccessJwtToken rawAccessToken = (RawAccessJwtToken) authentication.getCredentials();
        SecurityUser unsafeUser = tokenFactory.parseRefreshToken(rawAccessToken.token());
        UserPrincipal principal = unsafeUser.getUserPrincipal();

        SecurityUser securityUser;
        if (principal.getType() == UserPrincipal.Type.USER_NAME) {
            securityUser = authenticateByUserId(unsafeUser.getId());
        } else {
            securityUser = authenticateByPublicId(principal.getValue());
        }
        securityUser.setSessionId(unsafeUser.getSessionId());
        if (tokenOutdatingService.isOutdated(rawAccessToken.token(), securityUser.getId())) {
            throw new CredentialsExpiredException("Token is outdated");
        }

        return new RefreshAuthenticationToken(securityUser);
    }

    private SecurityUser authenticateByUserId(UserId userId) {
        UserAuthDetails userAuthDetails = userAuthDetailsCache.getUserAuthDetails(TenantId.SYS_TENANT_ID, userId);
        if (userAuthDetails == null) {
            throw new UsernameNotFoundException("User with credentials not found");
        }
        if (!userAuthDetails.credentialsEnabled()) {
            throw new DisabledException("User is not active");
        }

        User user = userAuthDetails.user();
        if (user.getAuthority() == null) {
            throw new InsufficientAuthenticationException("User has no authority assigned");
        }

        UserPrincipal userPrincipal = new UserPrincipal(UserPrincipal.Type.USER_NAME, user.getEmail());
        return new SecurityUser(user, true, userPrincipal);
    }

    private SecurityUser authenticateByPublicId(String publicId) {
        TenantId systemId = TenantId.SYS_TENANT_ID;
        CustomerId customerId;
        try {
            customerId = new CustomerId(UUID.fromString(publicId));
        } catch (Exception e) {
            throw new BadCredentialsException("Refresh token is not valid");
        }
        Customer publicCustomer = customerService.findCustomerById(systemId, customerId);
        if (publicCustomer == null) {
            throw new UsernameNotFoundException("Public entity not found by refresh token");
        }

        if (!publicCustomer.isPublic()) {
            throw new BadCredentialsException("Refresh token is not valid");
        }

        User user = new User(new UserId(EntityId.NULL_UUID));
        user.setTenantId(publicCustomer.getTenantId());
        user.setCustomerId(publicCustomer.getId());
        user.setEmail(publicId);
        user.setAuthority(Authority.CUSTOMER_USER);
        user.setFirstName("Public");
        user.setLastName("Public");

        UserPrincipal userPrincipal = new UserPrincipal(UserPrincipal.Type.PUBLIC_ID, publicId);

        return new SecurityUser(user, true, userPrincipal);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (RefreshAuthenticationToken.class.isAssignableFrom(authentication));
    }

}
